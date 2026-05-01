from __future__ import annotations

import dataclasses
import datetime
import enum
import json
from collections.abc import Callable
from typing import Any, Mapping

from bson import ObjectId
from langchain_core.messages import AIMessage, HumanMessage
from pydantic import BaseModel

from app.core.agent.tracing.config import load_agent_trace_settings


def _truncate_text(value: str, *, max_chars: int) -> str:
    """
    功能描述：
        按最大字符数截断文本。

    参数说明：
        value (str): 原始文本。
        max_chars (int): 最大字符数。

    返回值：
        str: 截断后的文本。
    """

    if len(value) <= max_chars:
        return value
    return f"{value[:max_chars]}...(已截断，原始长度={len(value)})"


def serialize_value(value: Any, *, max_chars: int | None = None) -> Any:
    """
    功能描述：
        将任意 Python 值转换成可保存到 Mongo 的 JSON 友好结构。

    参数说明：
        value (Any): 原始值。
        max_chars (int | None): 单个字符串最大字符数；为空时读取 trace 配置。

    返回值：
        Any: 可 JSON 化的结构。
    """

    resolved_max_chars = max_chars or load_agent_trace_settings().payload_max_chars
    if isinstance(value, BaseModel):
        return serialize_value(value.model_dump(mode="json", exclude_none=True), max_chars=resolved_max_chars)
    if dataclasses.is_dataclass(value) and not isinstance(value, type):
        return serialize_value(dataclasses.asdict(value), max_chars=resolved_max_chars)
    if isinstance(value, enum.Enum):
        return value.value
    if isinstance(value, ObjectId):
        return str(value)
    if isinstance(value, datetime.datetime):
        return value.isoformat()
    if isinstance(value, datetime.date):
        return value.isoformat()
    if isinstance(value, str):
        return _truncate_text(value, max_chars=resolved_max_chars)
    if value is None or isinstance(value, (int, float, bool)):
        return value
    if isinstance(value, list):
        return [serialize_value(item, max_chars=resolved_max_chars) for item in value]
    if isinstance(value, tuple):
        return [serialize_value(item, max_chars=resolved_max_chars) for item in value]
    if isinstance(value, Mapping):
        return {
            str(key): serialize_value(item, max_chars=resolved_max_chars)
            for key, item in value.items()
        }
    return _truncate_text(str(value), max_chars=resolved_max_chars)


def serialize_message(message: Any) -> dict[str, Any]:
    """
    功能描述：
        序列化 LangChain 消息对象。

    参数说明：
        message (Any): LangChain 消息对象或兼容对象。

    返回值：
        dict[str, Any]: 消息序列化结构。
    """

    payload: dict[str, Any] = {
        "type": str(getattr(message, "type", "") or message.__class__.__name__),
        "content": serialize_value(getattr(message, "content", None)),
    }
    name = getattr(message, "name", None)
    if name:
        payload["name"] = str(name)
    message_id = getattr(message, "id", None)
    if message_id:
        payload["id"] = str(message_id)
    tool_call_id = getattr(message, "tool_call_id", None)
    if tool_call_id:
        payload["tool_call_id"] = str(tool_call_id)
    additional_kwargs = getattr(message, "additional_kwargs", None)
    if additional_kwargs:
        payload["additional_kwargs"] = serialize_value(additional_kwargs)
    response_metadata = getattr(message, "response_metadata", None)
    if response_metadata:
        payload["response_metadata"] = serialize_value(response_metadata)
    tool_calls = getattr(message, "tool_calls", None)
    if tool_calls:
        payload["tool_calls"] = serialize_value(tool_calls)
    usage_metadata = getattr(message, "usage_metadata", None)
    if usage_metadata:
        payload["usage_metadata"] = serialize_value(usage_metadata)
    return payload


def serialize_messages(messages: list[Any] | tuple[Any, ...] | None) -> list[dict[str, Any]]:
    """
    功能描述：
        批量序列化 LangChain 消息列表。

    参数说明：
        messages (list[Any] | tuple[Any, ...] | None): 原始消息列表。

    返回值：
        list[dict[str, Any]]: 序列化后的消息列表。
    """

    return [serialize_message(message) for message in list(messages or [])]


def _extract_full_text_content(content: Any) -> str | None:
    """
    功能描述：
        提取 Trace 消息视图使用的完整文本内容，不做字符截断。

    参数说明：
        content (Any): LangChain 消息 content 字段。

    返回值：
        str | None: 完整文本；非文本或空白文本返回 None。
    """

    if not isinstance(content, str):
        return None
    if not content.strip():
        return None
    return content


def _resolve_trace_message_role(message: Any) -> str | None:
    """
    功能描述：
        解析 Trace 消息视图可保存的消息角色。

    参数说明：
        message (Any): LangChain 消息对象或兼容消息对象。

    返回值：
        str | None: `user` / `ai`；非可读历史消息返回 None。
    """

    if isinstance(message, HumanMessage):
        return "user"
    if isinstance(message, AIMessage):
        return "ai"
    message_type = str(getattr(message, "type", "") or "").strip().lower()
    if message_type in {"human", "user"}:
        return "user"
    if message_type in {"ai", "assistant"}:
        return "ai"
    return None


def serialize_trace_message_view(messages: list[Any] | tuple[Any, ...] | None) -> dict[str, Any]:
    """
    功能描述：
        序列化顶层 Trace 消息视图快照，只保留最近 N 条完整 user/ai 文本消息。

    参数说明：
        messages (list[Any] | tuple[Any, ...] | None): 当前会话历史消息列表。

    返回值：
        dict[str, Any]: 可直接写入 root graph span 的消息视图结构，包含保存条数和省略条数。
    """

    settings = load_agent_trace_settings()
    max_messages = settings.message_view_max_messages
    view_messages: list[dict[str, Any]] = []
    for index, message in enumerate(list(messages or [])):
        role = _resolve_trace_message_role(message)
        if role is None:
            continue
        content = _extract_full_text_content(getattr(message, "content", None))
        if content is None:
            continue
        view_messages.append(
            {
                "role": role,
                "content": content,
                "index": index,
            }
        )
    total_message_count = len(view_messages)
    saved_messages = view_messages[-max_messages:]
    return {
        "messages": saved_messages,
        "total_message_count": total_message_count,
        "saved_message_count": len(saved_messages),
        "omitted_message_count": max(0, total_message_count - len(saved_messages)),
    }


def _serialize_tool_args_schema(tool: Any) -> Any:
    """
    功能描述：
        序列化工具入参结构，优先输出标准 JSON Schema。

    参数说明：
        tool (Any): LangChain 工具对象。

    返回值：
        Any: JSON Schema 或可序列化的工具入参结构。
    """

    args_schema = getattr(tool, "args_schema", None)
    try:
        if isinstance(args_schema, type) and issubclass(args_schema, BaseModel):
            return serialize_value(args_schema.model_json_schema())
        if hasattr(args_schema, "model_json_schema"):
            return serialize_value(args_schema.model_json_schema())
    except Exception as exc:  # noqa: BLE001
        return {
            "schema_error": exc.__class__.__name__,
            "schema_error_message": serialize_value(str(exc)),
            "schema_repr": serialize_value(args_schema),
        }
    if args_schema is not None:
        return serialize_value(args_schema)
    return serialize_value(getattr(tool, "args", None))


def serialize_tools(
        tools: list[Any] | tuple[Any, ...] | None,
        *,
        display_name_resolver: Callable[[str], str] | None = None,
) -> list[dict[str, Any]]:
    """
    功能描述：
        序列化模型当前可见工具列表。

    参数说明：
        tools (list[Any] | tuple[Any, ...] | None): 工具对象列表。
        display_name_resolver (Callable[[str], str] | None): 工具展示名称解析函数。

    返回值：
        list[dict[str, Any]]: 工具摘要列表。
    """

    serialized_tools: list[dict[str, Any]] = []
    for tool in list(tools or []):
        tool_name = str(getattr(tool, "name", "") or "")
        display_name = display_name_resolver(tool_name) if display_name_resolver and tool_name else tool_name
        serialized_tools.append(
            {
                "name": tool_name,
                "display_name": display_name,
                "description": serialize_value(getattr(tool, "description", None)),
                "args_schema": _serialize_tool_args_schema(tool),
            }
        )
    return serialized_tools


def serialize_exception(error: BaseException) -> dict[str, Any]:
    """
    功能描述：
        序列化异常信息。

    参数说明：
        error (BaseException): 原始异常对象。

    返回值：
        dict[str, Any]: 异常结构。
    """

    return {
        "error_type": error.__class__.__name__,
        "error_message": serialize_value(str(error).strip() or repr(error)),
    }


def to_json_text(value: Any) -> str:
    """
    功能描述：
        将任意值转换成紧凑 JSON 文本，主要用于日志诊断。

    参数说明：
        value (Any): 原始值。

    返回值：
        str: JSON 文本。
    """

    return json.dumps(serialize_value(value), ensure_ascii=False, default=str, separators=(",", ":"))
