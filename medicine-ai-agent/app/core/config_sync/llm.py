from __future__ import annotations

import os
from dataclasses import dataclass
from typing import Any

from langchain_openai import OpenAIEmbeddings

from app.core.config_sync.snapshot import (
    AgentChatModelSlot,
    AgentModelRuntimeConfig,
    AgentModelSlotConfig,
    get_current_agent_config_snapshot,
)
from app.core.llms import ChatModel, create_chat_model, create_embedding_model, create_image_model
from app.core.llms.common import resolve_llm_value
from app.core.llms.provider import LlmProvider, resolve_provider
from app.core.llms.providers.aliyun import DEFAULT_DASHSCOPE_BASE_URL

_IMAGE_MODEL_ENV_NAMES = {
    LlmProvider.ALIYUN: "DASHSCOPE_IMAGE_MODEL",
}
_IMAGE_API_KEY_ENV_NAMES = {
    LlmProvider.ALIYUN: "DASHSCOPE_API_KEY",
}


@dataclass(frozen=True)
class _ResolvedSlotOverrides:
    """槽位解析后的运行时覆盖项。"""

    think: bool
    model_kwargs: dict[str, Any]


@dataclass(frozen=True)
class ResolvedAgentImageRuntime:
    """图片解析直连模型服务所需的运行时信息。"""

    provider: LlmProvider
    model: str
    api_key: str
    base_url: str


def _normalize_max_tokens_limit(value: int | None) -> int | None:
    """归一化最大输出 token 限制。

    Args:
        value: 原始最大 token 值。

    Returns:
        大于 ``0`` 的 token 上限；当值为 ``None``、``0`` 或负数时返回 ``None``，
        表示不限制。
    """

    if value is None or value <= 0:
        return None
    return value


def _resolve_gateway_router_fallback_model_name() -> str | None:
    """解析 Gateway 路由节点的本地环境兜底模型名。

    Returns:
        先尝试读取 provider 专属的 gateway 路由模型配置；未命中时回退到对应
        provider 的通用 chat 模型配置；两者都没有时返回 ``None``。
    """

    resolve_provider(None)
    dedicated_key = "DASHSCOPE_GATEWAY_ROUTER_MODEL"
    fallback_key = "DASHSCOPE_CHAT_MODEL"
    return resolve_llm_value(name=dedicated_key) or resolve_llm_value(name=fallback_key)


def _resolve_image_fallback_model_name(provider: LlmProvider | str | None = None) -> str | None:
    """解析图片识别槽位缺失时的本地环境兜底模型名。"""

    resolve_provider(provider)
    return resolve_llm_value(name="DASHSCOPE_IMAGE_MODEL")


def _resolve_summary_fallback_model_name() -> str | None:
    """解析聊天历史总结任务的本地环境兜底模型名。

    Returns:
        先尝试读取 provider 专属的 summary 模型配置；未命中时回退到全局
        `ASSISTANT_SUMMARY_MODEL`；两者都没有时返回 ``None``。
    """

    resolve_provider(None)
    provider_key = "DASHSCOPE_SUMMARY_MODEL"
    return resolve_llm_value(name=provider_key) or resolve_llm_value(name="ASSISTANT_SUMMARY_MODEL")


def _require_runtime_value(
        value: str | None,
        *,
        provider: LlmProvider,
        env_names: dict[LlmProvider, str],
) -> str:
    """要求运行时关键字段必须存在，否则抛出带环境变量名的错误。"""

    if value:
        return value
    raise RuntimeError(f"{env_names[provider]} is not set")


def _resolve_runtime_provider(runtime_config: AgentModelRuntimeConfig | None) -> LlmProvider:
    """解析当前生效的模型 provider。"""

    return resolve_provider(runtime_config.provider_type if runtime_config is not None else None)


def _resolve_runtime_connection(
        provider: LlmProvider,
        runtime_config: AgentModelRuntimeConfig | None,
) -> tuple[str | None, str | None]:
    """解析当前 provider 生效的 API Key 与 Base URL。"""

    explicit_api_key = runtime_config.api_key if runtime_config is not None else None
    explicit_base_url = runtime_config.base_url if runtime_config is not None else None

    if provider is not LlmProvider.ALIYUN:
        raise ValueError(f"Unsupported provider: {provider}")
    return (
        resolve_llm_value(name="DASHSCOPE_API_KEY", explicit=explicit_api_key),
        resolve_llm_value(
            name="DASHSCOPE_BASE_URL",
            explicit=explicit_base_url,
            default=DEFAULT_DASHSCOPE_BASE_URL,
        ),
    )


def _resolve_summary_fallback_max_tokens() -> int | None:
    """解析聊天历史总结摘要文本预算的本地环境兜底值。

    Returns:
        `ASSISTANT_SUMMARY_MAX_TOKENS` 的正整数值；未配置或非法时返回 ``None``。
    """

    raw_value = (os.getenv("ASSISTANT_SUMMARY_MAX_TOKENS") or "").strip()
    if not raw_value:
        return None
    try:
        resolved = int(raw_value)
    except ValueError:
        return None
    return _normalize_max_tokens_limit(resolved)


def _normalize_chat_slot(slot: AgentChatModelSlot | str) -> AgentChatModelSlot:
    """归一化聊天槽位输入。

    Args:
        slot: 槽位枚举或字符串形式的槽位名。

    Returns:
        归一化后的聊天槽位枚举。
    """

    if isinstance(slot, AgentChatModelSlot):
        return slot
    return AgentChatModelSlot(str(slot).strip())


def _is_gateway_route_slot(slot: AgentChatModelSlot) -> bool:
    """判断当前聊天槽位是否属于 Gateway 路由节点。"""

    return slot in {
        AgentChatModelSlot.CLIENT_ROUTE,
    }


def _resolve_slot_overrides(
        slot_config: AgentModelSlotConfig | None,
        *,
        temperature: float | None,
        think: bool,
        reasoning_override: bool | None,
        max_tokens: int | None,
        kwargs: dict[str, Any],
) -> _ResolvedSlotOverrides:
    """统一解析槽位里的思考开关、温度和 ``max_tokens``。

    Args:
        slot_config: 当前命中的 Redis 槽位配置。
        temperature: 调用方传入的默认温度，Redis 槽位配置存在时会被覆盖。
        think: 调用方传入的默认思考开关。
        reasoning_override: 调用方显式指定的思考开关覆盖；传入后优先级高于 Redis 槽位配置。
        max_tokens: 调用方传入的默认最大输出 token，Redis 槽位配置存在时会被覆盖。
        kwargs: 调用方传入的其余模型构造参数。

    Returns:
        一个包含最终思考开关和透传模型参数的解析结果。
    """

    if reasoning_override is not None:
        resolved_think = reasoning_override
    else:
        resolved_think = (
            slot_config.reasoning_enabled
            if slot_config is not None and slot_config.reasoning_enabled is not None
            else think
        )
    resolved_temperature = (
        slot_config.temperature
        if slot_config is not None and slot_config.temperature is not None
        else temperature
    )
    resolved_max_tokens = _normalize_max_tokens_limit(
        slot_config.max_tokens
        if slot_config is not None and slot_config.max_tokens is not None
        else max_tokens
    )

    model_kwargs = dict(kwargs)
    if resolved_temperature is not None:
        model_kwargs["temperature"] = resolved_temperature
    if resolved_max_tokens is not None:
        model_kwargs["max_tokens"] = resolved_max_tokens

    return _ResolvedSlotOverrides(
        think=bool(resolved_think),
        model_kwargs=model_kwargs,
    )


def _resolve_slot_runtime_model_name(
        slot_config: AgentModelSlotConfig | None,
        *,
        fallback_model: str | None = None,
) -> str | None:
    """解析槽位当前生效的模型名。

    Args:
        slot_config: 当前命中的 Redis 槽位配置。
        fallback_model: Redis 未配置时的本地兜底模型名。

    Returns:
        当前生效模型名；若 Redis 与本地兜底都不存在则返回 ``None``。
    """

    if slot_config is not None and slot_config.model_name is not None:
        return slot_config.model_name
    return fallback_model


def create_agent_chat_llm(
        *,
        slot: AgentChatModelSlot | str,
        model_name: str | None = None,
        temperature: float | None = None,
        think: bool = False,
        reasoning_override: bool | None = None,
        max_tokens: int | None = None,
        extra_body: dict[str, Any] | None = None,
        **kwargs: Any,
) -> ChatModel:
    """按 Agent 槽位创建聊天模型。

    Args:
        slot: 目标聊天槽位。
        model_name: 显式指定的模型名称；传入后优先级高于 Redis 槽位模型名。
        temperature: 调用方传入的默认温度。
        think: 调用方传入的默认思考开关；Redis 槽位有值时会被覆盖。
        reasoning_override: 调用方显式指定的思考开关覆盖；传入后优先级高于 Redis 槽位配置。
        max_tokens: 调用方传入的默认最大输出 token。
        extra_body: 透传到底层模型 SDK 的额外请求体。
        **kwargs: 其余透传到底层 ``create_chat_model`` 的构造参数。

    Returns:
        与底层 ``create_chat_model`` 一致的聊天模型客户端实例。
    """

    normalized_slot = _normalize_chat_slot(slot)
    snapshot = get_current_agent_config_snapshot()
    slot_config = snapshot.get_chat_slot(normalized_slot)
    runtime_config = snapshot.get_llm_runtime_config()
    resolved_overrides = _resolve_slot_overrides(
        slot_config,
        temperature=temperature,
        think=think,
        reasoning_override=reasoning_override,
        max_tokens=max_tokens,
        kwargs=kwargs,
    )

    resolved_model = (
        str(model_name).strip()
        if model_name is not None and str(model_name).strip()
        else _resolve_slot_runtime_model_name(slot_config)
    )
    if resolved_model is None and _is_gateway_route_slot(normalized_slot):
        resolved_model = _resolve_gateway_router_fallback_model_name()

    return create_chat_model(
        model=resolved_model,
        provider=runtime_config.provider_type if runtime_config is not None else None,
        api_key=runtime_config.api_key if runtime_config is not None else None,
        base_url=runtime_config.base_url if runtime_config is not None else None,
        extra_body=extra_body,
        think=resolved_overrides.think,
        **resolved_overrides.model_kwargs,
    )


def create_agent_image_llm(
        *,
        temperature: float | None = None,
        think: bool = False,
        reasoning_override: bool | None = None,
        max_tokens: int | None = None,
        extra_body: dict[str, Any] | None = None,
        **kwargs: Any,
) -> ChatModel:
    """按图片识别槽位创建图像理解模型。

    Args:
        temperature: 调用方传入的默认温度。
        think: 调用方传入的默认思考开关；Redis 槽位有值时会被覆盖。
        reasoning_override: 调用方显式指定的思考开关覆盖；传入后优先级高于 Redis 槽位配置。
        max_tokens: 调用方传入的默认最大输出 token。
        extra_body: 透传到底层模型 SDK 的额外请求体。
        **kwargs: 其余透传到底层 ``create_image_model`` 的构造参数。

    Returns:
        与底层 ``create_image_model`` 一致的图像理解模型客户端实例。
    """

    snapshot = get_current_agent_config_snapshot()
    slot_config = snapshot.get_image_slot()
    runtime_config = snapshot.get_llm_runtime_config()
    resolved_overrides = _resolve_slot_overrides(
        slot_config,
        temperature=temperature,
        think=think,
        reasoning_override=reasoning_override,
        max_tokens=max_tokens,
        kwargs=kwargs,
    )

    return create_image_model(
        model=_resolve_slot_runtime_model_name(slot_config),
        provider=runtime_config.provider_type if runtime_config is not None else None,
        api_key=runtime_config.api_key if runtime_config is not None else None,
        base_url=runtime_config.base_url if runtime_config is not None else None,
        extra_body=extra_body,
        think=resolved_overrides.think,
        **resolved_overrides.model_kwargs,
    )


def resolve_agent_image_runtime() -> ResolvedAgentImageRuntime:
    """解析图片识别当前生效的直连运行时配置。"""

    snapshot = get_current_agent_config_snapshot()
    slot_config = snapshot.get_image_slot()
    runtime_config = snapshot.get_llm_runtime_config()

    resolved_provider = _resolve_runtime_provider(runtime_config)
    resolved_model = _require_runtime_value(
        _resolve_slot_runtime_model_name(
            slot_config,
            fallback_model=_resolve_image_fallback_model_name(resolved_provider),
        ),
        provider=resolved_provider,
        env_names=_IMAGE_MODEL_ENV_NAMES,
    )

    resolved_api_key, resolved_base_url = _resolve_runtime_connection(
        resolved_provider,
        runtime_config,
    )
    resolved_api_key = _require_runtime_value(
        resolved_api_key,
        provider=resolved_provider,
        env_names=_IMAGE_API_KEY_ENV_NAMES,
    )
    if not resolved_base_url:
        raise RuntimeError("LLM base URL is not set")

    return ResolvedAgentImageRuntime(
        provider=resolved_provider,
        model=resolved_model,
        api_key=resolved_api_key,
        base_url=resolved_base_url,
    )


def create_agent_summary_llm(
        *,
        temperature: float | None = None,
        think: bool = False,
        reasoning_override: bool | None = None,
        max_tokens: int | None = None,
        extra_body: dict[str, Any] | None = None,
        **kwargs: Any,
) -> ChatModel:
    """按聊天历史总结槽位创建聊天模型。

    Args:
        temperature: 调用方传入的默认温度。
        think: 调用方传入的默认思考开关；Redis 槽位有值时会被覆盖。
        reasoning_override: 调用方显式指定的思考开关覆盖；传入后优先级高于 Redis 槽位配置。
        max_tokens: 调用方传入的默认最大输出 token。
        extra_body: 透传到底层模型 SDK 的额外请求体。
        **kwargs: 其余透传到底层 ``create_chat_model`` 的构造参数。

    Returns:
        与底层 ``create_chat_model`` 一致的聊天模型客户端实例。
    """

    snapshot = get_current_agent_config_snapshot()
    slot_config = snapshot.get_summary_slot()
    runtime_config = snapshot.get_llm_runtime_config()
    resolved_overrides = _resolve_slot_overrides(
        slot_config,
        temperature=temperature,
        think=think,
        reasoning_override=reasoning_override,
        max_tokens=max_tokens,
        kwargs=kwargs,
    )

    return create_chat_model(
        model=_resolve_slot_runtime_model_name(
            slot_config,
            fallback_model=_resolve_summary_fallback_model_name(),
        ),
        provider=runtime_config.provider_type if runtime_config is not None else None,
        api_key=runtime_config.api_key if runtime_config is not None else None,
        base_url=runtime_config.base_url if runtime_config is not None else None,
        extra_body=extra_body,
        think=resolved_overrides.think,
        **resolved_overrides.model_kwargs,
    )


def create_agent_title_llm(
        *,
        temperature: float | None = None,
        think: bool = False,
        reasoning_override: bool | None = None,
        max_tokens: int | None = None,
        extra_body: dict[str, Any] | None = None,
        **kwargs: Any,
) -> ChatModel:
    """按聊天标题生成槽位创建聊天模型。

    Args:
        temperature: 调用方传入的默认温度。
        think: 调用方传入的默认思考开关；Redis 槽位有值时会被覆盖。
        reasoning_override: 调用方显式指定的思考开关覆盖；传入后优先级高于 Redis 槽位配置。
        max_tokens: 调用方传入的默认最大输出 token。
        extra_body: 透传到底层模型 SDK 的额外请求体。
        **kwargs: 其余透传到底层 ``create_chat_model`` 的构造参数。

    Returns:
        与底层 ``create_chat_model`` 一致的聊天模型客户端实例。
    """

    snapshot = get_current_agent_config_snapshot()
    slot_config = snapshot.get_title_slot()
    runtime_config = snapshot.get_llm_runtime_config()
    resolved_overrides = _resolve_slot_overrides(
        slot_config,
        temperature=temperature,
        think=think,
        reasoning_override=reasoning_override,
        max_tokens=max_tokens,
        kwargs=kwargs,
    )

    return create_chat_model(
        model=_resolve_slot_runtime_model_name(slot_config),
        provider=runtime_config.provider_type if runtime_config is not None else None,
        api_key=runtime_config.api_key if runtime_config is not None else None,
        base_url=runtime_config.base_url if runtime_config is not None else None,
        extra_body=extra_body,
        think=resolved_overrides.think,
        **resolved_overrides.model_kwargs,
    )


def resolve_agent_summary_model_name() -> str | None:
    """解析聊天历史总结当前生效的模型名。

    Returns:
        优先返回 Redis 中总结槽位绑定的模型名；未命中时回退本地 summary 环境配置。
    """

    snapshot = get_current_agent_config_snapshot()
    return _resolve_slot_runtime_model_name(
        snapshot.get_summary_slot(),
        fallback_model=_resolve_summary_fallback_model_name(),
    )


def resolve_agent_summary_max_tokens() -> int | None:
    """解析聊天历史总结当前生效的摘要文本预算上限。

    Returns:
        返回本地 `ASSISTANT_SUMMARY_MAX_TOKENS` 的正整数值；
        未配置或非法时返回 ``None``。
    """

    return _resolve_summary_fallback_max_tokens()


def create_agent_embedding_client(
        *,
        model: str | None = None,
        dimensions: int | None = None,
        **kwargs: Any,
) -> OpenAIEmbeddings:
    """按 Agent 配置创建向量模型客户端。

    Args:
        model: 显式传入的 embedding 模型名，优先级高于 Redis 槽位。
        dimensions: 显式传入的向量维度，优先级高于 Redis 配置。
        **kwargs: 其余透传到底层 ``create_embedding_model`` 的构造参数。

    Returns:
        与底层 ``create_embedding_model`` 一致的 embedding 客户端实例。
    """

    snapshot = get_current_agent_config_snapshot()
    runtime_config = snapshot.get_llm_runtime_config()
    resolved_dimensions = dimensions or snapshot.get_knowledge_embedding_dim() or 1024

    resolved_model = model or snapshot.get_knowledge_embedding_model_name()
    return create_embedding_model(
        model=resolved_model,
        provider=runtime_config.provider_type if runtime_config is not None else None,
        api_key=runtime_config.api_key if runtime_config is not None else None,
        base_url=runtime_config.base_url if runtime_config is not None else None,
        dimensions=resolved_dimensions,
        **kwargs,
    )
