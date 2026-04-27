from __future__ import annotations

import json
import os
import threading
from dataclasses import dataclass
from datetime import datetime, timezone
from enum import Enum
from pathlib import Path
from typing import Any

from dotenv import dotenv_values
from loguru import logger
from pydantic import BaseModel, ConfigDict, Field, ValidationError, field_validator, model_validator

from app.core.database import get_redis_connection
from app.core.llms.provider import LlmProvider, normalize_provider
from app.core.llms.providers.aliyun import DEFAULT_DASHSCOPE_BASE_URL

#: Redis 中保存 Agent 全量运行时配置的固定 key。
AGENT_CONFIG_REDIS_KEY = "agent:config:all"


class AgentChatModelSlot(str, Enum):
    """Agent 聊天模型槽位。"""

    ADMIN_NODE = "adminAssistant.chatDisplayRuntime"
    CLIENT_ROUTE = "clientAssistant.routeModel"
    CLIENT_SERVICE = "clientAssistant.serviceNodeModel"
    CLIENT_DIAGNOSIS = "clientAssistant.diagnosisNodeModel"


class AgentImageModelSlot(str, Enum):
    """图片识别模型槽位。"""

    RECOGNITION = "imageRecognitionModel"


class AgentEmbeddingModelSlot(str, Enum):
    """向量模型槽位。"""

    EMBEDDING = "embeddingModel"


class KnowledgeBaseScope(str, Enum):
    """知识库配置作用域。"""

    ADMIN = "admin"
    CLIENT = "client"


class AgentConfigSource(str, Enum):
    """当前内存快照来源。"""

    REDIS = "redis"
    LOCAL_FALLBACK = "local_fallback"


class AgentConfigLoadReason(str, Enum):
    """Agent 配置加载失败原因。"""

    REDIS_KEY_MISSING = "redis_key_missing"
    INVALID_UTF8 = "invalid_utf8"
    UNSUPPORTED_PAYLOAD_TYPE = "unsupported_payload_type"
    REDIS_READ_FAILED = "redis_read_failed"
    INVALID_JSON = "invalid_json"
    INVALID_SCHEMA = "invalid_schema"


@dataclass(frozen=True)
class AgentConfigRefreshResult:
    """Agent 配置刷新结果。"""

    applied: bool
    previous_snapshot: "AgentConfigSnapshot | None"
    current_snapshot: "AgentConfigSnapshot | None"
    speech_changed: bool


#: Agent 配置加载失败原因到中文日志文案的映射。
_LOAD_REASON_LABELS: dict[AgentConfigLoadReason, str] = {
    AgentConfigLoadReason.REDIS_KEY_MISSING: "Redis Key 不存在",
    AgentConfigLoadReason.INVALID_UTF8: "配置内容不是合法 UTF-8",
    AgentConfigLoadReason.UNSUPPORTED_PAYLOAD_TYPE: "配置内容类型不支持",
    AgentConfigLoadReason.REDIS_READ_FAILED: "读取 Redis 失败",
    AgentConfigLoadReason.INVALID_JSON: "配置内容不是合法 JSON",
    AgentConfigLoadReason.INVALID_SCHEMA: "配置结构校验失败",
}

#: Agent 配置结构允许的 LLM providerType 列表。
_SUPPORTED_PROVIDER_TYPES = {"aliyun"}
#: 本地 `.env` 中知识库名称列表的回退配置键。
_ENV_AGENT_KNOWLEDGE_NAMES = "AGENT_KNOWLEDGE_NAMES"
#: 本地 `.env` 中知识库向量维度的回退配置键。
_ENV_AGENT_KNOWLEDGE_EMBEDDING_DIM = "AGENT_KNOWLEDGE_EMBEDDING_DIM"
#: 本地 `.env` 中知识库向量模型的回退配置键。
_ENV_AGENT_KNOWLEDGE_EMBEDDING_MODEL = "AGENT_KNOWLEDGE_EMBEDDING_MODEL"
#: 本地 `.env` 中知识库默认返回条数的回退配置键。
_ENV_AGENT_KNOWLEDGE_TOP_K = "AGENT_KNOWLEDGE_TOP_K"
#: 本地 `.env` 中知识库排序开关的回退配置键。
_ENV_AGENT_KNOWLEDGE_RANKING_ENABLED = "AGENT_KNOWLEDGE_RANKING_ENABLED"
#: 本地 `.env` 中知识库排序模型的回退配置键。
_ENV_AGENT_KNOWLEDGE_RANKING_MODEL = "AGENT_KNOWLEDGE_RANKING_MODEL"
#: 项目根目录下的 `.env` 文件路径。
_LOCAL_FALLBACK_DOTENV_FILE = Path(__file__).resolve().parents[3] / ".env"


def _strip_optional_str(value: Any) -> str | None:
    """将可选字符串值规整为去空白后的字符串。

    Args:
        value: 原始输入值，允许为 ``None``、字符串或其他可转字符串对象。

    Returns:
        去除首尾空白后的字符串；若输入为空值或空白字符串则返回 ``None``。
    """

    if value is None:
        return None
    normalized = str(value).strip()
    return normalized or None


def _normalize_optional_positive_int(value: Any) -> int | None:
    """将可选正整数值规整为 ``int``。

    Args:
        value: 原始输入值，允许为 ``None``、数字或可转数字字符串。

    Returns:
        大于 ``0`` 的整数；输入为空、无法解析或小于等于 ``0`` 时返回 ``None``。
    """

    if value is None:
        return None
    if isinstance(value, str):
        normalized = value.strip()
        if not normalized:
            return None
        value = normalized
    try:
        resolved = int(value)
    except (TypeError, ValueError):
        return None
    if resolved <= 0:
        return None
    return resolved


def _normalize_optional_non_negative_float(value: Any) -> float | None:
    """将可选非负浮点值规整为 ``float``。

    Args:
        value: 原始输入值，允许为 ``None``、数字或可转数字字符串。

    Returns:
        float | None: 大于等于 ``0`` 的浮点值；输入为空时返回 ``None``。

    Raises:
        ValueError: 当输入不是数字或小于 ``0`` 时抛出。
    """

    if value is None:
        return None
    if isinstance(value, str):
        normalized = value.strip()
        if not normalized:
            return None
        value = normalized
    try:
        resolved = float(value)
    except (TypeError, ValueError) as exc:
        raise ValueError("value must be a non-negative number") from exc
    if resolved < 0:
        raise ValueError("value must be a non-negative number")
    return resolved


def _normalize_string_list(value: Any) -> list[str] | None:
    """将字符串列表规整为去重且保持顺序的结果。

    Args:
        value: 原始输入值，允许为 ``None`` 或可迭代对象。

    Returns:
        去空白、去重、保序后的字符串列表；若输入为空则返回 ``None``。

    Raises:
        ValueError: 当输入不是列表类结构时抛出。
    """

    if value is None:
        return None
    if not isinstance(value, (list, tuple)):
        raise ValueError("knowledgeNames must be a list")

    seen: set[str] = set()
    normalized_values: list[str] = []
    for item in value:
        normalized = _strip_optional_str(item)
        if normalized is None or normalized in seen:
            continue
        seen.add(normalized)
        normalized_values.append(normalized)
    return normalized_values or None


def _normalize_optional_bool(value: Any) -> bool | None:
    """将可选布尔值规整为 ``bool``。

    Args:
        value: 原始输入值，允许为 ``None``、布尔值或布尔语义字符串。

    Returns:
        解析成功时返回 ``True`` 或 ``False``；未配置或无法识别时返回 ``None``。
    """

    if value is None:
        return None
    if isinstance(value, bool):
        return value
    normalized = str(value).strip().lower()
    if not normalized:
        return None
    if normalized in {"1", "true", "yes", "on"}:
        return True
    if normalized in {"0", "false", "no", "off"}:
        return False
    return None


def _read_local_fallback_env_value(name: str) -> str | None:
    """读取本地兜底配置值且不污染进程环境。

    Args:
        name: 目标环境变量名。

    Returns:
        显式环境变量或 `.env` 中的非空字符串；均不存在时返回 ``None``。
    """

    env_value = (os.getenv(name) or "").strip()
    if env_value:
        return env_value

    try:
        dotenv_value = dotenv_values(_LOCAL_FALLBACK_DOTENV_FILE).get(name)
    except Exception:
        return None
    normalized = str(dotenv_value or "").strip()
    return normalized or None


def _clone_snapshot(snapshot: "AgentConfigSnapshot | None") -> "AgentConfigSnapshot | None":
    """深拷贝当前快照对象。"""

    if snapshot is None:
        return None
    return snapshot.model_copy(deep=True)


def _normalize_speech_config(snapshot: "AgentConfigSnapshot | None") -> dict[str, Any] | None:
    """提取用于语音配置比对的归一化 speech 子树。"""

    if snapshot is None or snapshot.speech is None:
        return None
    return snapshot.speech.model_dump(
        mode="json",
        by_alias=False,
        exclude_none=False,
    )


def _resolve_local_fallback_provider() -> LlmProvider:
    """解析本地 `.env` 回退场景使用的 provider。"""

    configured_provider = _read_local_fallback_env_value("LLM_PROVIDER")
    if configured_provider:
        return normalize_provider(configured_provider)
    return LlmProvider.ALIYUN


def _resolve_local_fallback_runtime_values(provider: LlmProvider) -> tuple[str | None, str | None]:
    """读取本地 `.env` 中当前 provider 对应的运行时连接信息。"""

    if provider is not LlmProvider.ALIYUN:
        raise ValueError(f"Unsupported provider: {provider}")
    return (
        _read_local_fallback_env_value("DASHSCOPE_API_KEY"),
        _read_local_fallback_env_value("DASHSCOPE_BASE_URL") or DEFAULT_DASHSCOPE_BASE_URL,
    )


def _resolve_local_fallback_embedding_model_name(provider: LlmProvider) -> str | None:
    """读取本地 `.env` 中当前 provider 对应的 embedding 模型名称。"""

    explicit_model = _read_local_fallback_env_value(_ENV_AGENT_KNOWLEDGE_EMBEDDING_MODEL)
    if explicit_model:
        return explicit_model
    if provider is not LlmProvider.ALIYUN:
        raise ValueError(f"Unsupported provider: {provider}")
    return _read_local_fallback_env_value("DASHSCOPE_EMBEDDING_MODEL")


def _resolve_local_fallback_llm_runtime_config() -> AgentModelRuntimeConfig | None:
    """构造本地 `.env` 回退场景的顶层 `llm` 配置。"""

    provider = _resolve_local_fallback_provider()
    api_key, base_url = _resolve_local_fallback_runtime_values(provider)
    if not api_key or not base_url:
        return None
    return AgentModelRuntimeConfig(
        providerType=provider.value,
        baseUrl=base_url,
        apiKey=api_key,
    )


def _resolve_local_fallback_knowledge_base_config() -> KnowledgeBaseAgentConfig | None:
    """构造本地 `.env` 回退场景的 `knowledgeBase` 配置。"""

    provider = _resolve_local_fallback_provider()
    raw_knowledge_names = _read_local_fallback_env_value(_ENV_AGENT_KNOWLEDGE_NAMES)
    knowledge_names = _normalize_string_list(
        raw_knowledge_names.split(",") if raw_knowledge_names else None
    )
    if not knowledge_names:
        return None

    ranking_enabled = _normalize_optional_bool(
        _read_local_fallback_env_value(_ENV_AGENT_KNOWLEDGE_RANKING_ENABLED)
    )
    ranking_model = _read_local_fallback_env_value(_ENV_AGENT_KNOWLEDGE_RANKING_MODEL)

    try:
        return KnowledgeBaseAgentConfig(
            enabled=True,
            knowledgeNames=knowledge_names,
            embeddingDim=(
                    _normalize_optional_positive_int(
                        _read_local_fallback_env_value(_ENV_AGENT_KNOWLEDGE_EMBEDDING_DIM)
                    )
                    or 1024
            ),
            embeddingModel=_resolve_local_fallback_embedding_model_name(provider),
            rankingEnabled=bool(ranking_enabled),
            rankingModel=ranking_model,
            topK=_normalize_optional_positive_int(_read_local_fallback_env_value(_ENV_AGENT_KNOWLEDGE_TOP_K)),
        )
    except ValidationError as exc:
        logger.warning(
            "本地 .env 知识库兜底配置无效，已忽略：error_count={}",
            len(exc.errors()),
        )
        return None


class AgentModelRuntimeConfig(BaseModel):
    """Redis 顶层 `llm` 运行时配置。"""

    model_config = ConfigDict(populate_by_name=True, extra="forbid")

    provider_type: str | None = Field(default=None, alias="providerType")
    base_url: str | None = Field(default=None, alias="baseUrl")
    api_key: str | None = Field(default=None, alias="apiKey")

    @field_validator("provider_type", mode="before")
    @classmethod
    def _normalize_provider_type(cls, value: Any) -> str | None:
        """归一化顶层 `providerType` 字段。

        Args:
            value: Redis 中的原始 providerType 值。

        Returns:
            规整后的 providerType；未提供时返回 ``None``。

        Raises:
            ValueError: 当 providerType 不在支持范围内时抛出。
        """

        normalized = _strip_optional_str(value)
        if normalized is None:
            return None
        lowered = normalized.lower()
        if lowered not in _SUPPORTED_PROVIDER_TYPES:
            raise ValueError(f"Unsupported agent config providerType: {normalized}")
        return lowered

    @field_validator("base_url", "api_key", mode="before")
    @classmethod
    def _normalize_optional_str(cls, value: Any) -> str | None:
        """归一化顶层运行时配置中的可选字符串字段。"""

        return _strip_optional_str(value)

    @model_validator(mode="after")
    def _validate_runtime_shape(self) -> AgentModelRuntimeConfig:
        """校验顶层 `llm` 配置是否完整。

        Returns:
            AgentModelRuntimeConfig: 当前模型实例。

        Raises:
            ValueError: 当 `llm` 对象存在但字段不完整时抛出。
        """

        has_any_runtime_value = any(
            value is not None
            for value in (self.provider_type, self.base_url, self.api_key)
        )
        if has_any_runtime_value and (
                self.provider_type is None or self.base_url is None or self.api_key is None
        ):
            raise ValueError("Agent runtime config requires providerType, baseUrl and apiKey")
        return self


class AgentModelSlotConfig(BaseModel):
    """业务槽位配置。"""

    model_config = ConfigDict(populate_by_name=True, extra="forbid")

    model_name: str | None = Field(default=None, alias="modelName")
    temperature: float | None = None
    max_tokens: int | None = Field(default=None, alias="maxTokens")
    reasoning_enabled: bool | None = Field(default=None, alias="reasoningEnabled")
    support_reasoning: bool | None = Field(default=None, alias="supportReasoning")
    support_vision: bool | None = Field(default=None, alias="supportVision")

    @field_validator("model_name", mode="before")
    @classmethod
    def _normalize_model_name(cls, value: Any) -> str | None:
        """归一化槽位模型名称。"""

        return _strip_optional_str(value)

    @field_validator("temperature", mode="before")
    @classmethod
    def _normalize_temperature(cls, value: Any) -> float | None:
        """归一化槽位模型温度配置。

        Args:
            value: Redis 中的原始 ``temperature`` 值。

        Returns:
            float | None: 非负温度值；未配置时返回 ``None``。
        """

        return _normalize_optional_non_negative_float(value)

    @field_validator("max_tokens", mode="before")
    @classmethod
    def _normalize_max_tokens(cls, value: Any) -> int | None:
        """归一化槽位模型最大输出 token 配置。

        Args:
            value: Redis 中的原始 ``maxTokens`` 值。

        Returns:
            int | None: 正整数 token 上限；未配置或小于等于 ``0`` 时返回 ``None``。
        """

        return _normalize_optional_positive_int(value)

    @field_validator("support_reasoning", "support_vision", mode="before")
    @classmethod
    def _normalize_support_capability(cls, value: Any) -> bool | None:
        """归一化槽位能力标记。"""

        return _normalize_optional_bool(value)


class AdminAssistantChatDisplayModelConfig(BaseModel):
    """管理端聊天界面展示模型配置。"""

    model_config = ConfigDict(populate_by_name=True, extra="forbid")

    custom_model_name: str | None = Field(default=None, alias="customModelName")
    actual_model_name: str | None = Field(default=None, alias="actualModelName")
    description: str | None = None
    support_reasoning: bool | None = Field(default=None, alias="supportReasoning")
    support_vision: bool | None = Field(default=None, alias="supportVision")

    @field_validator("custom_model_name", "actual_model_name", "description", mode="before")
    @classmethod
    def _normalize_optional_str(cls, value: Any) -> str | None:
        """归一化聊天展示模型配置中的可选字符串字段。"""

        return _strip_optional_str(value)

    @field_validator("support_reasoning", "support_vision", mode="before")
    @classmethod
    def _normalize_optional_bool_field(cls, value: Any) -> bool | None:
        """归一化聊天展示模型配置中的可选布尔字段。"""

        return _normalize_optional_bool(value)


class KnowledgeBaseAgentConfig(BaseModel):
    """知识库配置。"""

    model_config = ConfigDict(populate_by_name=True, extra="forbid")

    enabled: bool | None = None
    knowledge_names: list[str] | None = Field(default=None, alias="knowledgeNames")
    embedding_dim: int | None = Field(default=None, alias="embeddingDim")
    embedding_model: str | None = Field(default=None, alias="embeddingModel")
    ranking_enabled: bool = Field(default=False, alias="rankingEnabled")
    ranking_model: str | None = Field(default=None, alias="rankingModel")
    top_k: int | None = Field(default=None, alias="topK")

    @field_validator("enabled", mode="before")
    @classmethod
    def _normalize_enabled(cls, value: Any) -> bool | None:
        """归一化知识库总开关。"""

        return _normalize_optional_bool(value)

    @field_validator("knowledge_names", mode="before")
    @classmethod
    def _normalize_knowledge_names(cls, value: Any) -> list[str] | None:
        """归一化知识库名称列表。"""

        return _normalize_string_list(value)

    @field_validator("embedding_model", "ranking_model", mode="before")
    @classmethod
    def _normalize_optional_model_name(cls, value: Any) -> str | None:
        """归一化知识库模型名称字符串。"""

        if value is None:
            return None
        if not isinstance(value, str):
            raise ValueError("knowledgeBase model name must be a string")
        return _strip_optional_str(value)

    @field_validator("embedding_dim", "top_k", mode="before")
    @classmethod
    def _normalize_optional_positive_number(cls, value: Any) -> int | None:
        """归一化知识库配置中的可选正整数。"""

        return _normalize_optional_positive_int(value)

    @model_validator(mode="after")
    def _validate_ranking_config(self) -> KnowledgeBaseAgentConfig:
        """校验知识库排序相关字段组合是否合法。"""

        if self.top_k is not None and self.top_k > 100:
            raise ValueError("knowledgeBase.topK must be between 1 and 100")
        if self.enabled is False:
            return self
        if not self.ranking_enabled and self.ranking_model is not None:
            raise ValueError("knowledgeBase.rankingModel must be null when rankingEnabled is false")
        if self.ranking_enabled and self.ranking_model is None:
            raise ValueError("knowledgeBase.rankingModel is required when rankingEnabled is true")
        return self

    def has_legacy_enabled_content(self) -> bool:
        """判断历史配置是否可视为已启用知识库。"""

        return any(
            (
                bool(self.knowledge_names),
                self.embedding_model is not None,
                self.embedding_dim is not None,
                self.top_k is not None,
                bool(self.ranking_enabled),
                self.ranking_model is not None,
            )
        )


class AdminAssistantAgentConfig(BaseModel):
    """管理助手配置。"""

    model_config = ConfigDict(populate_by_name=True, extra="forbid")

    chat_display_models: list[AdminAssistantChatDisplayModelConfig] | None = Field(
        default=None,
        alias="chatDisplayModels",
    )


class ClientAssistantAgentConfig(BaseModel):
    """客户端助手配置。"""

    model_config = ConfigDict(populate_by_name=True, extra="forbid")

    route_model: AgentModelSlotConfig | None = Field(default=None, alias="routeModel")
    service_node_model: AgentModelSlotConfig | None = Field(
        default=None,
        alias="serviceNodeModel",
    )
    diagnosis_node_model: AgentModelSlotConfig | None = Field(
        default=None,
        alias="diagnosisNodeModel",
    )
    reasoning_enabled: bool | None = Field(default=None, alias="reasoningEnabled")

    @field_validator("reasoning_enabled", mode="before")
    @classmethod
    def _normalize_reasoning_enabled(cls, value: Any) -> bool | None:
        """归一化客户端统一深度思考开关。"""

        return _normalize_optional_bool(value)


class CommonCapabilityAgentConfig(BaseModel):
    """通用能力配置。"""

    model_config = ConfigDict(populate_by_name=True, extra="forbid")

    image_recognition_model: AgentModelSlotConfig | None = Field(
        default=None,
        alias="imageRecognitionModel",
    )
    chat_history_summary_model: AgentModelSlotConfig | None = Field(
        default=None,
        alias="chatHistorySummaryModel",
    )
    chat_title_model: AgentModelSlotConfig | None = Field(
        default=None,
        alias="chatTitleModel",
    )


class AgentConfigsConfig(BaseModel):
    """顶层 `agentConfigs` 容器。"""

    model_config = ConfigDict(populate_by_name=True, extra="forbid")

    knowledge_base: KnowledgeBaseAgentConfig | None = Field(default=None, alias="knowledgeBase")
    client_knowledge_base: KnowledgeBaseAgentConfig | None = Field(
        default=None,
        alias="clientKnowledgeBase",
    )
    admin_assistant: AdminAssistantAgentConfig | None = Field(default=None, alias="adminAssistant")
    client_assistant: ClientAssistantAgentConfig | None = Field(default=None, alias="clientAssistant")
    common_capability: CommonCapabilityAgentConfig | None = Field(
        default=None,
        alias="commonCapability",
    )


class SpeechRecognitionAgentConfig(BaseModel):
    """语音识别配置。"""

    model_config = ConfigDict(populate_by_name=True, extra="forbid")

    resource_id: str | None = Field(default=None, alias="resourceId")

    @field_validator("resource_id", mode="before")
    @classmethod
    def _normalize_resource_id(cls, value: Any) -> str | None:
        """归一化语音识别资源 ID。"""

        return _strip_optional_str(value)


class TextToSpeechAgentConfig(BaseModel):
    """文本转语音配置。"""

    model_config = ConfigDict(populate_by_name=True, extra="forbid")

    resource_id: str | None = Field(default=None, alias="resourceId")
    voice_type: str | None = Field(default=None, alias="voiceType")
    max_text_chars: int | None = Field(default=None, alias="maxTextChars")

    @field_validator("resource_id", "voice_type", mode="before")
    @classmethod
    def _normalize_optional_str(cls, value: Any) -> str | None:
        """归一化文本转语音可选字符串字段。"""

        return _strip_optional_str(value)

    @field_validator("max_text_chars", mode="before")
    @classmethod
    def _normalize_max_text_chars(cls, value: Any) -> int | None:
        """归一化文本转语音最大字符数。"""

        return _normalize_optional_positive_int(value)


class SpeechAgentConfig(BaseModel):
    """语音配置。"""

    model_config = ConfigDict(populate_by_name=True, extra="forbid")

    provider: str | None = None
    app_id: str | None = Field(default=None, alias="appId")
    access_token: str | None = Field(default=None, alias="accessToken")
    speech_recognition: SpeechRecognitionAgentConfig | None = Field(
        default=None,
        alias="speechRecognition",
    )
    text_to_speech: TextToSpeechAgentConfig | None = Field(
        default=None,
        alias="textToSpeech",
    )

    @field_validator("provider", "app_id", "access_token", mode="before")
    @classmethod
    def _normalize_optional_str(cls, value: Any) -> str | None:
        """归一化语音配置中的可选字符串字段。"""

        return _strip_optional_str(value)


class AgentConfigSnapshot(BaseModel):
    """Agent 运行时配置快照。"""

    model_config = ConfigDict(populate_by_name=True, extra="forbid")

    updated_at: datetime | None = Field(default=None, alias="updatedAt")
    updated_by: str | None = Field(default=None, alias="updatedBy")
    llm: AgentModelRuntimeConfig | None = None
    agent_configs: AgentConfigsConfig | None = Field(default=None, alias="agentConfigs")
    speech: SpeechAgentConfig | None = None

    @field_validator("updated_by", mode="before")
    @classmethod
    def _normalize_updated_by(cls, value: Any) -> str | None:
        """归一化更新人字段。"""

        return _strip_optional_str(value)

    def get_llm_runtime_config(self) -> AgentModelRuntimeConfig | None:
        """读取顶层 LLM 运行时配置。

        Returns:
            Redis 顶层 `llm` 配置；本地兜底或未配置时返回 ``None``。
        """

        return self.llm

    def get_chat_slot(self, slot: AgentChatModelSlot) -> AgentModelSlotConfig | None:
        """根据聊天槽位枚举读取对应业务槽位配置。

        Args:
            slot: 目标聊天槽位。

        Returns:
            命中的槽位配置；若当前快照未配置对应槽位则返回 ``None``。
        """

        agent_configs = self.agent_configs
        if agent_configs is None:
            return None
        if slot is AgentChatModelSlot.ADMIN_NODE:
            admin_display_models = self.get_admin_chat_display_models()
            display_model = admin_display_models[0] if admin_display_models else None
            if display_model is None or display_model.actual_model_name is None:
                return None
            return AgentModelSlotConfig(
                modelName=display_model.actual_model_name,
                reasoningEnabled=False,
                supportReasoning=display_model.support_reasoning,
                supportVision=display_model.support_vision,
            )
        if slot is AgentChatModelSlot.CLIENT_ROUTE:
            client_assistant = agent_configs.client_assistant
            return None if client_assistant is None else client_assistant.route_model
        if slot is AgentChatModelSlot.CLIENT_SERVICE:
            client_assistant = agent_configs.client_assistant
            return None if client_assistant is None else client_assistant.service_node_model
        if slot is AgentChatModelSlot.CLIENT_DIAGNOSIS:
            client_assistant = agent_configs.client_assistant
            return None if client_assistant is None else client_assistant.diagnosis_node_model
        return None

    def supports_vision_for_chat_slot(self, slot: AgentChatModelSlot) -> bool:
        """判断指定聊天槽位是否支持图片理解。"""

        slot_config = self.get_chat_slot(slot)
        return bool(slot_config.support_vision) if slot_config is not None else False

    def supports_reasoning_for_chat_slot(self, slot: AgentChatModelSlot) -> bool:
        """判断指定聊天槽位是否支持深度思考。"""

        slot_config = self.get_chat_slot(slot)
        return bool(slot_config.support_reasoning) if slot_config is not None else False

    def is_client_reasoning_enabled(self) -> bool:
        """判断客户端统一深度思考开关是否已开启。"""

        agent_configs = self.agent_configs
        if agent_configs is None or agent_configs.client_assistant is None:
            return False
        return bool(agent_configs.client_assistant.reasoning_enabled)

    def get_admin_chat_display_models(self) -> list[AdminAssistantChatDisplayModelConfig]:
        """读取管理端聊天界面展示模型列表。"""

        agent_configs = self.agent_configs
        if agent_configs is None or agent_configs.admin_assistant is None:
            return []
        return list(agent_configs.admin_assistant.chat_display_models or [])

    def find_admin_chat_display_model(
            self,
            custom_model_name: str | None,
    ) -> AdminAssistantChatDisplayModelConfig | None:
        """根据自定义模型名称查找管理端聊天界面展示模型配置。"""

        normalized_custom_model_name = _strip_optional_str(custom_model_name)
        if normalized_custom_model_name is None:
            return None
        for config in self.get_admin_chat_display_models():
            if config.custom_model_name == normalized_custom_model_name:
                return config
        return None

    def find_admin_chat_display_model_by_actual_model_name(
            self,
            actual_model_name: str | None,
    ) -> AdminAssistantChatDisplayModelConfig | None:
        """根据真实模型名称查找管理端聊天界面展示模型配置。"""

        normalized_actual_model_name = _strip_optional_str(actual_model_name)
        if normalized_actual_model_name is None:
            return None
        for config in self.get_admin_chat_display_models():
            if config.actual_model_name == normalized_actual_model_name:
                return config
        return None

    def supports_vision_for_admin_actual_model_name(self, actual_model_name: str | None) -> bool | None:
        """判断指定真实模型名称在管理端聊天映射中是否支持图片理解。"""

        config = self.find_admin_chat_display_model_by_actual_model_name(actual_model_name)
        if config is None:
            return None
        return bool(config.support_vision)

    def get_image_slot(self) -> AgentModelSlotConfig | None:
        """读取图片识别槽位配置。

        Returns:
            图片识别模型槽位配置；未配置时返回 ``None``。
        """

        agent_configs = self.agent_configs
        if agent_configs is None or agent_configs.common_capability is None:
            return None
        return agent_configs.common_capability.image_recognition_model

    def get_embedding_slot(self) -> AgentModelSlotConfig | None:
        """读取知识库 embedding 槽位配置。

        Returns:
            知识库 embedding 模型槽位配置；未配置时返回 ``None``。
        """

        model_name = self.get_knowledge_embedding_model_name()
        if model_name is None:
            return None
        return AgentModelSlotConfig(modelName=model_name)

    def _get_scoped_knowledge_base(
            self,
            scope: KnowledgeBaseScope = KnowledgeBaseScope.ADMIN,
    ) -> KnowledgeBaseAgentConfig | None:
        """根据作用域读取对应的知识库配置节点。

        Args:
            scope: 知识库作用域。

        Returns:
            作用域对应的知识库配置；未配置时返回 ``None``。
        """

        agent_configs = self.agent_configs
        if agent_configs is None:
            return None
        if scope is KnowledgeBaseScope.CLIENT:
            return agent_configs.client_knowledge_base
        return agent_configs.knowledge_base

    def is_knowledge_enabled(self, scope: KnowledgeBaseScope = KnowledgeBaseScope.ADMIN) -> bool:
        """判断当前知识库能力是否启用。

        Args:
            scope: 知识库作用域。

        Returns:
            当 Redis 显式配置 `enabled` 时以其为准；否则按历史字段兼容推断。
        """

        knowledge_base = self._get_scoped_knowledge_base(scope)
        if knowledge_base is None:
            return False
        if knowledge_base.enabled is not None:
            return knowledge_base.enabled
        return knowledge_base.has_legacy_enabled_content()

    def get_knowledge_names(self, scope: KnowledgeBaseScope = KnowledgeBaseScope.ADMIN) -> list[str]:
        """读取当前允许访问的知识库名称列表。

        Args:
            scope: 知识库作用域。

        Returns:
            去重且保序后的知识库名称列表；未配置时返回空列表。
        """

        knowledge_base = self._get_scoped_knowledge_base(scope)
        if knowledge_base is None:
            return []
        return list(knowledge_base.knowledge_names or [])

    def get_knowledge_embedding_model_name(
            self,
            scope: KnowledgeBaseScope = KnowledgeBaseScope.ADMIN,
    ) -> str | None:
        """读取知识库统一的向量模型名称。

        Args:
            scope: 知识库作用域。

        Returns:
            当前知识库统一 embedding 模型名；未配置时返回 ``None``。
        """

        knowledge_base = self._get_scoped_knowledge_base(scope)
        if knowledge_base is None:
            return None
        return knowledge_base.embedding_model

    def get_knowledge_embedding_dim(
            self,
            scope: KnowledgeBaseScope = KnowledgeBaseScope.ADMIN,
    ) -> int | None:
        """读取知识库统一的向量维度。

        Args:
            scope: 知识库作用域。

        Returns:
            当前知识库统一 embedding 维度；未配置时返回 ``None``。
        """

        knowledge_base = self._get_scoped_knowledge_base(scope)
        if knowledge_base is None:
            return None
        return knowledge_base.embedding_dim

    def is_knowledge_ranking_enabled(
            self,
            scope: KnowledgeBaseScope = KnowledgeBaseScope.ADMIN,
    ) -> bool:
        """读取知识库排序开关。

        Args:
            scope: 知识库作用域。

        Returns:
            当前 Redis 是否显式启用了排序。
        """

        knowledge_base = self._get_scoped_knowledge_base(scope)
        if knowledge_base is None:
            return False
        return bool(knowledge_base.ranking_enabled)

    def get_knowledge_ranking_model_name(
            self,
            scope: KnowledgeBaseScope = KnowledgeBaseScope.ADMIN,
    ) -> str | None:
        """读取知识库排序模型名称。

        Args:
            scope: 知识库作用域。

        Returns:
            当前知识库排序模型名称；未配置时返回 ``None``。
        """

        knowledge_base = self._get_scoped_knowledge_base(scope)
        if knowledge_base is None:
            return None
        return knowledge_base.ranking_model

    def get_knowledge_top_k(self, scope: KnowledgeBaseScope = KnowledgeBaseScope.ADMIN) -> int | None:
        """读取知识库最终返回条数配置。

        Args:
            scope: 知识库作用域。

        Returns:
            Redis 中配置的最终返回条数；未配置或非法时返回 ``None``。
        """

        knowledge_base = self._get_scoped_knowledge_base(scope)
        if knowledge_base is None:
            return None
        return knowledge_base.top_k

    def get_summary_slot(self) -> AgentModelSlotConfig | None:
        """读取聊天历史总结槽位配置。

        Returns:
            聊天历史总结模型槽位配置；未配置时返回 ``None``。
        """

        agent_configs = self.agent_configs
        if agent_configs is None or agent_configs.common_capability is None:
            return None
        return agent_configs.common_capability.chat_history_summary_model

    def get_title_slot(self) -> AgentModelSlotConfig | None:
        """读取聊天标题生成槽位配置。

        Returns:
            聊天标题生成模型槽位配置；未配置时返回 ``None``。
        """

        agent_configs = self.agent_configs
        if agent_configs is None or agent_configs.common_capability is None:
            return None
        return agent_configs.common_capability.chat_title_model

    def get_speech_shared_auth(self) -> tuple[str, str] | None:
        """读取语音共享鉴权配置。

        Returns:
            当 Redis 中 `appId/accessToken` 同时存在时返回鉴权二元组；
            任一缺失时返回 ``None``，由调用方继续回退环境变量。
        """

        speech = self.speech
        if speech is None:
            return None
        if speech.app_id is None or speech.access_token is None:
            return None
        return speech.app_id, speech.access_token

    def get_speech_stt_resource_id(self) -> str | None:
        """读取语音识别资源 ID。

        Returns:
            语音识别 `resourceId`；未配置时返回 ``None``。
        """

        speech = self.speech
        if speech is None or speech.speech_recognition is None:
            return None
        return speech.speech_recognition.resource_id

    def get_speech_tts_voice_type(self) -> str | None:
        """读取文本转语音音色类型。

        Returns:
            文本转语音 `voiceType`；未配置时返回 ``None``。
        """

        speech = self.speech
        if speech is None or speech.text_to_speech is None:
            return None
        return speech.text_to_speech.voice_type

    def get_speech_tts_resource_id(self) -> str | None:
        """读取文本转语音资源 ID。

        Returns:
            文本转语音 `resourceId`；未配置时返回 ``None``。
        """

        speech = self.speech
        if speech is None or speech.text_to_speech is None:
            return None
        return speech.text_to_speech.resource_id

    def get_speech_tts_max_text_chars(self) -> int | None:
        """读取文本转语音最大字符数限制。

        Returns:
            文本转语音 `maxTextChars`；未配置或非法时返回 ``None``。
        """

        speech = self.speech
        if speech is None or speech.text_to_speech is None:
            return None
        return speech.text_to_speech.max_text_chars


class AgentConfigLoadError(RuntimeError):
    """Redis 配置加载失败。"""

    def __init__(self, reason: AgentConfigLoadReason, message: str) -> None:
        """初始化加载异常。

        Args:
            reason: 失败原因枚举，供日志记录与测试断言使用。
            message: 脱敏后的异常消息，不包含 Redis 原始配置内容。
        """

        super().__init__(message)
        self.reason = reason


#: 保护进程内 Agent 配置快照读写的一把锁。
_CONFIG_LOCK = threading.RLock()
#: 当前生效的进程内配置快照。
_current_snapshot: AgentConfigSnapshot | None = None
#: 当前生效快照来源，便于日志和调试定位。
_current_source = AgentConfigSource.LOCAL_FALLBACK


def _build_local_fallback_snapshot() -> AgentConfigSnapshot:
    """构造本地 `.env` 兜底场景使用的快照。

    Returns:
        基于本地环境变量组装的 Agent 配置快照；无可用配置时返回空兜底快照。
    """

    payload: dict[str, Any] = {
        "updatedAt": datetime.now(timezone.utc),
        "updatedBy": "local_env_fallback",
        "agentConfigs": {},
    }
    llm_runtime_config = _resolve_local_fallback_llm_runtime_config()
    if llm_runtime_config is not None:
        payload["llm"] = llm_runtime_config.model_dump(by_alias=True, exclude_none=True)
    knowledge_base_config = _resolve_local_fallback_knowledge_base_config()
    if knowledge_base_config is not None:
        payload["agentConfigs"]["knowledgeBase"] = knowledge_base_config.model_dump(
            by_alias=True,
            exclude_none=True,
        )

    try:
        return AgentConfigSnapshot.model_validate(payload)
    except ValidationError as exc:
        logger.warning(
            "本地 .env Agent 配置兜底快照无效，已回退为空快照：error_count={}",
            len(exc.errors()),
        )
        return AgentConfigSnapshot(
            updatedAt=datetime.now(timezone.utc),
            updatedBy="local_env_fallback",
        )


def _get_load_reason_label(reason: AgentConfigLoadReason) -> str:
    """返回加载失败原因对应的中文日志文案。

    Args:
        reason: 加载失败原因枚举。

    Returns:
        适合写入日志的中文原因描述。
    """

    return _LOAD_REASON_LABELS.get(reason, "未知错误")


def _decode_redis_payload(*, raw_payload: Any, redis_key: str) -> str:
    """将 Redis 原始返回值解码为 JSON 文本。

    Args:
        raw_payload: Redis ``GET`` 返回的原始值。
        redis_key: 当前读取的 Redis key，用于错误提示。

    Returns:
        解码后的 JSON 字符串。

    Raises:
        AgentConfigLoadError: 当 key 不存在、编码非法或 payload 类型不支持时抛出。
    """

    if raw_payload is None:
        raise AgentConfigLoadError(
            AgentConfigLoadReason.REDIS_KEY_MISSING,
            f"Redis key {redis_key} is missing",
        )
    if isinstance(raw_payload, bytes):
        try:
            return raw_payload.decode("utf-8")
        except UnicodeDecodeError as exc:
            raise AgentConfigLoadError(
                AgentConfigLoadReason.INVALID_UTF8,
                "Agent config payload is not valid utf-8",
            ) from exc
    if isinstance(raw_payload, str):
        return raw_payload
    raise AgentConfigLoadError(
        AgentConfigLoadReason.UNSUPPORTED_PAYLOAD_TYPE,
        f"Unsupported agent config payload type: {type(raw_payload)!r}",
    )


def _unwrap_snapshot_payload_root(*, data: Any) -> dict[str, Any]:
    """提取 Agent 配置实际根对象。

    Args:
        data: `json.loads` 后的原始对象。

    Returns:
        供 `AgentConfigSnapshot` 校验的实际配置根对象。

    Raises:
        AgentConfigLoadError: 当根对象不是 JSON object，或包装层 `data` 不是对象时抛出。
    """

    if not isinstance(data, dict):
        raise AgentConfigLoadError(
            AgentConfigLoadReason.INVALID_SCHEMA,
            "Agent config payload root must be a JSON object",
        )
    wrapped_data = data.get("data")
    if wrapped_data is None:
        return data
    if not isinstance(wrapped_data, dict):
        raise AgentConfigLoadError(
            AgentConfigLoadReason.INVALID_SCHEMA,
            "Wrapped agent config payload data must be a JSON object",
        )
    return wrapped_data


def _ensure_snapshot_structure(snapshot: AgentConfigSnapshot) -> AgentConfigSnapshot:
    """校验快照是否满足 Redis 结构要求。

    Args:
        snapshot: 已通过 Pydantic 基础校验的快照对象。

    Returns:
        AgentConfigSnapshot: 原样返回当前快照对象。

    Raises:
        AgentConfigLoadError: 当快照未满足必填结构时抛出。
    """
    if snapshot.llm is None:
        raise AgentConfigLoadError(
            AgentConfigLoadReason.INVALID_SCHEMA,
            "Agent config payload llm is required",
        )
    if snapshot.agent_configs is None:
        raise AgentConfigLoadError(
            AgentConfigLoadReason.INVALID_SCHEMA,
            "Agent config payload agentConfigs is required",
        )
    runtime_config = snapshot.get_llm_runtime_config()
    if runtime_config is None:
        raise AgentConfigLoadError(
            AgentConfigLoadReason.INVALID_SCHEMA,
            "Agent config payload llm is required",
        )
    if (
            runtime_config.provider_type is None
            or runtime_config.base_url is None
            or runtime_config.api_key is None
    ):
        raise AgentConfigLoadError(
            AgentConfigLoadReason.INVALID_SCHEMA,
            "Agent config payload llm is incomplete",
        )
    return snapshot


def _load_snapshot_from_redis(*, redis_key: str) -> AgentConfigSnapshot:
    """从 Redis 读取并反序列化 Agent 配置快照。

    Args:
        redis_key: 要读取的 Redis key。

    Returns:
        通过 schema 校验后的 Agent 配置快照。

    Raises:
        AgentConfigLoadError: 当 Redis 读取、JSON 解析或 schema 校验失败时抛出。
    """

    try:
        raw_payload = get_redis_connection().get(redis_key)
    except Exception as exc:
        raise AgentConfigLoadError(
            AgentConfigLoadReason.REDIS_READ_FAILED,
            f"Failed to read redis key {redis_key}",
        ) from exc

    payload = _decode_redis_payload(raw_payload=raw_payload, redis_key=redis_key)
    try:
        data = json.loads(payload)
    except json.JSONDecodeError as exc:
        raise AgentConfigLoadError(
            AgentConfigLoadReason.INVALID_JSON,
            "Agent config payload is not valid JSON",
        ) from exc

    effective_root = _unwrap_snapshot_payload_root(data=data)
    try:
        snapshot = AgentConfigSnapshot.model_validate(effective_root)
    except ValidationError as exc:
        raise AgentConfigLoadError(
            AgentConfigLoadReason.INVALID_SCHEMA,
            "Agent config payload schema is invalid",
        ) from exc
    return _ensure_snapshot_structure(snapshot)


def _set_current_snapshot(snapshot: AgentConfigSnapshot, *, source: AgentConfigSource) -> None:
    """原子更新进程内当前快照状态。

    Args:
        snapshot: 要写入的新快照。
        source: 该快照的来源。
    """

    global _current_snapshot, _current_source
    _current_snapshot = snapshot
    _current_source = source


def initialize_agent_config_snapshot() -> AgentConfigSnapshot:
    """初始化进程内 Agent 配置快照。

    启动时优先尝试从 Redis 读取；若 Redis 不可用、key 缺失或数据非法，
    则回退到本地 `.env` 兜底快照。

    Returns:
        当前初始化后生效的 Agent 配置快照副本。
    """

    with _CONFIG_LOCK:
        if _current_snapshot is not None:
            return _current_snapshot.model_copy(deep=True)

    try:
        snapshot = _load_snapshot_from_redis(redis_key=AGENT_CONFIG_REDIS_KEY)
    except AgentConfigLoadError as exc:
        snapshot = _build_local_fallback_snapshot()
        with _CONFIG_LOCK:
            _set_current_snapshot(snapshot, source=AgentConfigSource.LOCAL_FALLBACK)
        logger.warning(
            "Agent 配置初始化完成：来源={}，redis_key={}，错误原因={}",
            AgentConfigSource.LOCAL_FALLBACK.value,
            AGENT_CONFIG_REDIS_KEY,
            _get_load_reason_label(exc.reason),
        )
        return snapshot.model_copy(deep=True)

    with _CONFIG_LOCK:
        _set_current_snapshot(snapshot, source=AgentConfigSource.REDIS)
    logger.info(
        "Agent 配置初始化完成：来源={}，redis_key={}",
        AgentConfigSource.REDIS.value,
        AGENT_CONFIG_REDIS_KEY,
    )
    return snapshot.model_copy(deep=True)


def get_current_agent_config_snapshot() -> AgentConfigSnapshot:
    """读取当前生效的 Agent 配置快照。

    Returns:
        当前生效快照的深拷贝副本。
    """

    with _CONFIG_LOCK:
        if _current_snapshot is not None:
            return _current_snapshot.model_copy(deep=True)
    return initialize_agent_config_snapshot()


def refresh_agent_config_snapshot(*, redis_key: str) -> AgentConfigRefreshResult:
    """在收到 MQ 刷新通知后重新拉取 Redis 配置并更新本地快照。

    Args:
        redis_key: 需要重新读取的 Redis key。

    Returns:
        AgentConfigRefreshResult: 结构化刷新结果，包含语音配置是否变化。
    """

    with _CONFIG_LOCK:
        previous_snapshot = _clone_snapshot(_current_snapshot)

    try:
        snapshot = _load_snapshot_from_redis(redis_key=redis_key)
    except AgentConfigLoadError as exc:
        logger.warning(
            "Agent 配置刷新失败，继续保留当前快照：redis_key={}，错误原因={}",
            redis_key,
            _get_load_reason_label(exc.reason),
        )
        return AgentConfigRefreshResult(
            applied=False,
            previous_snapshot=previous_snapshot,
            current_snapshot=previous_snapshot,
            speech_changed=False,
        )

    with _CONFIG_LOCK:
        _set_current_snapshot(snapshot, source=AgentConfigSource.REDIS)
    current_snapshot = snapshot.model_copy(deep=True)
    speech_changed = _normalize_speech_config(previous_snapshot) != _normalize_speech_config(current_snapshot)
    logger.info(
        "Agent 配置刷新已生效：来源={}，redis_key={}，speech_changed={}",
        AgentConfigSource.REDIS.value,
        redis_key,
        speech_changed,
    )
    return AgentConfigRefreshResult(
        applied=True,
        previous_snapshot=previous_snapshot,
        current_snapshot=current_snapshot,
        speech_changed=speech_changed,
    )


def clear_agent_config_snapshot_state() -> None:
    """清理进程内 Agent 配置快照状态。

    该函数主要供测试使用，用于隔离用例之间的模块级全局状态。
    """

    global _current_snapshot, _current_source
    with _CONFIG_LOCK:
        _current_snapshot = None
        _current_source = AgentConfigSource.LOCAL_FALLBACK
