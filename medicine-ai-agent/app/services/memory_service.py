from __future__ import annotations

import os
from typing import Literal

from langchain_core.messages import AIMessage, HumanMessage, SystemMessage

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.core.llms.provider import LlmProvider, resolve_provider
from app.schemas.document.message import MessageRole, MessageStatus
from app.schemas.memory import Memory
from app.services.conversation_service import get_conversation
from app.services.message_service import list_messages, list_summarizable_tail_messages
from app.services.summary_service import get_conversation_summary
from app.utils.assistant_message_utils import build_user_message_workflow_text

_ASSISTANT_MEMORY_MODE_ENV = "ASSISTANT_MEMORY_MODE"
_ASSISTANT_MEMORY_WINDOW_LIMIT_ENV = "ASSISTANT_MEMORY_WINDOW_LIMIT"
_ASSISTANT_SUMMARY_TRIGGER_WINDOW_ENV = "ASSISTANT_SUMMARY_TRIGGER_WINDOW"
_ASSISTANT_SUMMARY_TAIL_WINDOW_ENV = "ASSISTANT_SUMMARY_TAIL_WINDOW"
_ASSISTANT_SUMMARY_MAX_TOKENS_ENV = "ASSISTANT_SUMMARY_MAX_TOKENS"
_ASSISTANT_SUMMARY_MODEL_ENV = "ASSISTANT_SUMMARY_MODEL"
_DASHSCOPE_SUMMARY_MODEL_ENV = "DASHSCOPE_SUMMARY_MODEL"

_DEFAULT_ASSISTANT_MEMORY_MODE: Literal["window", "summary"] = "window"
_DEFAULT_ASSISTANT_MEMORY_WINDOW_LIMIT = 50
_DEFAULT_ASSISTANT_SUMMARY_TRIGGER_WINDOW = 100
_DEFAULT_ASSISTANT_SUMMARY_TAIL_WINDOW = 20
_DEFAULT_ASSISTANT_SUMMARY_MAX_TOKENS = 2000


def _resolve_positive_int_env(
        *,
        env_name: str,
        default: int,
) -> int:
    """
    功能描述：
        从环境变量读取正整数配置，非法值回退到默认值。

    参数说明：
        env_name (str): 环境变量名称。
        default (int): 默认值，要求为正整数。

    返回值：
        int: 生效的正整数配置值。

    异常说明：
        无（非法环境值会自动回退，不抛异常）。
    """

    raw_value = os.getenv(env_name)
    if raw_value is None:
        return default
    normalized = raw_value.strip()
    if not normalized:
        return default
    try:
        resolved = int(normalized)
    except ValueError:
        return default
    if resolved <= 0:
        return default
    return resolved


def resolve_assistant_memory_mode() -> Literal["window", "summary"]:
    """
    功能描述：
        解析管理助手记忆模式配置。

    参数说明：
        无。

    返回值：
        Literal["window", "summary"]: 生效记忆模式，默认 `window`。

    异常说明：
        无（非法值自动回退到默认模式）。
    """

    raw_mode = (os.getenv(_ASSISTANT_MEMORY_MODE_ENV) or "").strip().lower()
    if raw_mode in {"window", "summary"}:
        return raw_mode
    return _DEFAULT_ASSISTANT_MEMORY_MODE


def resolve_assistant_memory_window_limit() -> int:
    """
    功能描述：
        解析窗口记忆模式的历史条数上限。

    参数说明：
        无。

    返回值：
        int: 生效的窗口大小，默认 `50`。

    异常说明：
        无（非法值自动回退到默认值）。
    """

    return _resolve_positive_int_env(
        env_name=_ASSISTANT_MEMORY_WINDOW_LIMIT_ENV,
        default=_DEFAULT_ASSISTANT_MEMORY_WINDOW_LIMIT,
    )


def resolve_assistant_summary_trigger_window() -> int:
    """
    功能描述：
        解析摘要模式触发阈值（可总结消息条数）。

    参数说明：
        无。

    返回值：
        int: 生效的摘要触发窗口，默认 `100`。

    异常说明：
        无（非法值自动回退到默认值）。
    """

    return _resolve_positive_int_env(
        env_name=_ASSISTANT_SUMMARY_TRIGGER_WINDOW_ENV,
        default=_DEFAULT_ASSISTANT_SUMMARY_TRIGGER_WINDOW,
    )


def resolve_assistant_summary_tail_window() -> int:
    """
    功能描述：
        解析摘要模式下原文尾部保留窗口大小。

    参数说明：
        无。

    返回值：
        int: 生效的尾部窗口大小，默认 `20`。

    异常说明：
        无（非法值自动回退到默认值）。
    """

    return _resolve_positive_int_env(
        env_name=_ASSISTANT_SUMMARY_TAIL_WINDOW_ENV,
        default=_DEFAULT_ASSISTANT_SUMMARY_TAIL_WINDOW,
    )


def resolve_assistant_summary_max_tokens() -> int:
    """
    功能描述：
        解析摘要文本最大 token 上限。

    参数说明：
        无。

    返回值：
        int: 生效 token 上限，默认 `2000`。

    异常说明：
        无（非法值自动回退到默认值）。
    """

    return _resolve_positive_int_env(
        env_name=_ASSISTANT_SUMMARY_MAX_TOKENS_ENV,
        default=_DEFAULT_ASSISTANT_SUMMARY_MAX_TOKENS,
    )


def resolve_assistant_summary_model() -> str | None:
    """
    功能描述：
        解析摘要任务专用模型名称，支持按厂商独立配置。

    参数说明：
        无（厂商由 `LLM_PROVIDER` 或默认规则自动解析）。

    返回值：
        str | None:
            生效模型名解析优先级为：
            1. 当前厂商专属环境变量：`DASHSCOPE_SUMMARY_MODEL`
            2. 全局兜底：`ASSISTANT_SUMMARY_MODEL`
            3. 未配置时返回 `None`（调用方回退聊天模型）。

    异常说明：
        ValueError:
            当 `LLM_PROVIDER` 配置非法时，由 `resolve_provider` 抛出。
    """

    def _resolve_optional_env_string(env_name: str) -> str | None:
        """
        功能描述：
            从环境变量读取可选字符串并做空白归一化。

        参数说明：
            env_name (str): 环境变量名称。

        返回值：
            str | None: 非空字符串返回原值（去首尾空白）；空值返回 `None`。

        异常说明：
            无。
        """

        raw_value = os.getenv(env_name)
        if raw_value is None:
            return None
        normalized_value = raw_value.strip()
        return normalized_value or None

    resolved_provider = resolve_provider()
    provider_summary_model_env = {
        LlmProvider.ALIYUN: _DASHSCOPE_SUMMARY_MODEL_ENV,
    }[resolved_provider]
    provider_model_name = _resolve_optional_env_string(provider_summary_model_env)
    if provider_model_name:
        return provider_model_name
    return _resolve_optional_env_string(_ASSISTANT_SUMMARY_MODEL_ENV)


def _resolve_conversation_id(
        *,
        conversation_uuid: str,
        user_id: int,
) -> str:
    """
    功能描述：
        基于会话 UUID 与用户 ID 解析会话主键（Mongo ObjectId 字符串）。

    参数说明：
        conversation_uuid (str): 会话 UUID。
        user_id (int): 当前用户 ID。

    返回值：
        str: 会话 Mongo 主键字符串。

    异常说明：
        ServiceException:
            - BAD_REQUEST: 参数为空或不合法。
            - NOT_FOUND: 会话不存在或无权限。
            - DATABASE_ERROR: 会话文档缺少主键。
    """

    normalized_uuid = conversation_uuid.strip()
    if not normalized_uuid:
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="会话UUID不能为空")
    if user_id <= 0:
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="用户ID不合法")

    conversation = get_conversation(
        conversation_uuid=normalized_uuid,
        user_id=user_id,
    )
    if conversation is None:
        raise ServiceException(code=ResponseCode.NOT_FOUND, message="会话不存在")

    conversation_id = conversation.id
    if conversation_id is None:
        raise ServiceException(code=ResponseCode.DATABASE_ERROR, message="会话数据异常")
    return str(conversation_id)


def _to_chat_memory_messages(
        *,
        include_system_summary: str | None = None,
        documents: list,
) -> list[HumanMessage | AIMessage | SystemMessage]:
    """
    功能描述：
        将消息文档转换为记忆消息列表，支持在开头插入摘要系统消息。

    参数说明：
        include_system_summary (str | None): 摘要文本；为空时不注入系统消息。
        documents (list): 消息文档列表，元素需包含 `role` 与 `content` 字段。

    返回值：
        list[HumanMessage | AIMessage | SystemMessage]: 可直接送入模型的有序消息列表。

    异常说明：
        无。
    """

    result: list[HumanMessage | AIMessage | SystemMessage] = []
    normalized_summary = (include_system_summary or "").strip()
    if normalized_summary:
        result.append(SystemMessage(content=normalized_summary))

    for document in documents:
        if document.role == MessageRole.USER:
            user_content = build_user_message_workflow_text(document.content, document.cards)
            if not user_content:
                continue
            result.append(HumanMessage(content=user_content))
        else:
            result.append(AIMessage(content=document.content))
    return result


def load_memory(
        *,
        memory_type: Literal["window", "summary"],
        conversation_uuid: str,
        user_id: int,
        limit: int = _DEFAULT_ASSISTANT_MEMORY_WINDOW_LIMIT,
        include_history_hidden: bool = True,
) -> Memory:
    """
    功能描述：
        加载会话记忆，并根据全局环境变量决定最终使用 `window` 或 `summary` 模式。

    参数说明：
        memory_type (Literal["window", "summary"]): 兼容保留参数；实际模式由
            `ASSISTANT_MEMORY_MODE` 决定。
        conversation_uuid (str): 会话 UUID。
        user_id (int): 用户 ID。
        limit (int): 兼容保留窗口大小参数；最终窗口大小优先使用环境配置。

    返回值：
        Memory: 记忆消息列表（按旧到新顺序）。

    异常说明：
        ServiceException:
            - BAD_REQUEST: 入参不合法。
            - NOT_FOUND: 会话不存在。
            - DATABASE_ERROR: 会话数据异常。
        ValueError:
            - 当模式值异常时抛出（理论上不会发生，已由配置解析兜底）。
    """

    _ = memory_type
    effective_mode = resolve_assistant_memory_mode()
    configured_window_limit = resolve_assistant_memory_window_limit()
    # 保留显式入参优先级：仅当调用方传入非默认值时覆盖环境配置。
    effective_window_limit = (
        limit
        if limit > 0 and limit != _DEFAULT_ASSISTANT_MEMORY_WINDOW_LIMIT
        else configured_window_limit
    )
    match effective_mode:
        case "window":
            return load_memory_by_window(
                conversation_uuid=conversation_uuid,
                user_id=user_id,
                limit=effective_window_limit,
                include_history_hidden=include_history_hidden,
            )
        case "summary":
            return load_memory_by_summary(
                conversation_uuid=conversation_uuid,
                user_id=user_id,
                include_history_hidden=include_history_hidden,
            )
        case _:
            raise ValueError(f"Invalid memory type: {effective_mode}")


def load_memory_by_window(
        *,
        conversation_uuid: str,
        user_id: int,
        limit: int = _DEFAULT_ASSISTANT_MEMORY_WINDOW_LIMIT,
        include_history_hidden: bool = True,
) -> Memory:
    """
    功能描述：
        加载窗口记忆（按时间正序返回）。

    参数说明：
        conversation_uuid (str): 会话 UUID。
        user_id (int): 用户 ID。
        limit (int): 窗口大小。

    返回值：
        Memory: 仅包含 `HumanMessage/AIMessage` 的记忆对象。

    异常说明：
        ServiceException:
            - BAD_REQUEST: 参数不合法。
            - NOT_FOUND: 会话不存在。
            - DATABASE_ERROR: 会话数据异常。
    """

    if limit <= 0:
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="窗口大小不合法")

    conversation_id = _resolve_conversation_id(
        conversation_uuid=conversation_uuid,
        user_id=user_id,
    )

    message_documents = list_messages(
        conversation_id=str(conversation_id),
        limit=limit,
        ascending=False,
        history_hidden=(None if include_history_hidden else False),
        statuses=[MessageStatus.SUCCESS, MessageStatus.WAITING_INPUT],
    )

    history_messages = _to_chat_memory_messages(
        documents=list(reversed(message_documents)),
    )
    return Memory.model_construct(messages=history_messages)


def load_memory_by_summary(
        *,
        conversation_uuid: str,
        user_id: int,
        include_history_hidden: bool = True,
) -> Memory:
    """
    功能描述：
        加载摘要记忆模式的上下文，返回“最新摘要 + 原文尾部窗口”。

    参数说明：
        conversation_uuid (str): 会话 UUID。
        user_id (int): 用户 ID。

    返回值：
        Memory:
            按顺序返回：
            1. 可选 `SystemMessage(summary_content)`；
            2. 尾部窗口内的原始 `HumanMessage/AIMessage`。

    异常说明：
        ServiceException:
            - BAD_REQUEST: 参数不合法。
            - NOT_FOUND: 会话不存在。
            - DATABASE_ERROR: 会话数据异常。
    """

    conversation_id = _resolve_conversation_id(
        conversation_uuid=conversation_uuid,
        user_id=user_id,
    )
    summary_document = get_conversation_summary(conversation_id=conversation_id)
    summary_content: str | None = None
    if (
            summary_document is not None
            and summary_document.status == "success"
            and summary_document.summary_content.strip()
    ):
        summary_content = summary_document.summary_content

    tail_documents = list_summarizable_tail_messages(
        conversation_id=conversation_id,
        limit=resolve_assistant_summary_tail_window(),
        history_hidden=(None if include_history_hidden else False),
    )
    memory_messages = _to_chat_memory_messages(
        include_system_summary=summary_content,
        documents=tail_documents,
    )
    return Memory.model_construct(messages=memory_messages)
