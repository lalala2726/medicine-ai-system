"""
工具调用轨迹模块。

说明：
1. 该模块负责维护工具轨迹的会话级上下文绑定；
2. 显式标注的工具会在调用成功或失败后写入 `tool_traces` 集合；
3. 模型侧只读取最近工具轨迹的摘要文本，不直接消费完整 JSON。
"""

from __future__ import annotations

import dataclasses
import datetime
import enum
import inspect
import json
from contextvars import ContextVar, Token
from dataclasses import dataclass
from typing import Any

from pydantic import BaseModel

from app.schemas.document.conversation import ConversationType
from app.schemas.document.tool_trace import ToolTraceDocument, ToolTraceStatus
from app.services.tool_trace_service import add_tool_trace, list_recent_tool_traces

# 最近工具轨迹默认注入条数。
DEFAULT_TOOL_TRACE_PROMPT_LIMIT = 8
# 工具输入摘要的最大字符数。
_TOOL_TRACE_INPUT_SUMMARY_MAX_LENGTH = 200
# 工具输出摘要的最大字符数。
_TOOL_TRACE_OUTPUT_SUMMARY_MAX_LENGTH = 320
_TOOL_TRACE_CONTEXT: ContextVar["ToolTraceContext | None"] = ContextVar(
    "tool_trace_context",
    default=None,
)


@dataclass(frozen=True)
class ToolTraceContext:
    """
    功能描述：
        当前工具轨迹写入所需的会话上下文。

    参数说明：
        conversation_uuid (str): 所属会话 UUID。
        assistant_message_uuid (str): 所属 AI 回复消息 UUID。
        conversation_type (ConversationType): 会话类型。

    返回值：
        无（数据模型定义）。

    异常说明：
        无。
    """

    conversation_uuid: str
    assistant_message_uuid: str
    conversation_type: ConversationType


def _normalize_required_text(value: str, *, field_name: str) -> str:
    """
    功能描述：
        规范化必填字符串字段。

    参数说明：
        value (str): 原始字段值。
        field_name (str): 字段名称。

    返回值：
        str: 去除首尾空白后的非空字符串。

    异常说明：
        ValueError: 当字段为空时抛出。
    """

    normalized_value = str(value or "").strip()
    if not normalized_value:
        raise ValueError(f"{field_name} 不能为空")
    return normalized_value


def _serialize_trace_value(value: Any) -> Any:
    """
    功能描述：
        将任意工具输入输出递归转换为可 JSON 序列化结构。

    参数说明：
        value (Any): 原始输入输出值。

    返回值：
        Any: 可 JSON 序列化结构。

    异常说明：
        无。
    """

    if isinstance(value, BaseModel):
        return value.model_dump(mode="json", exclude_none=True)
    if dataclasses.is_dataclass(value) and not isinstance(value, type):
        return {
            str(key): _serialize_trace_value(item)
            for key, item in dataclasses.asdict(value).items()
        }
    if isinstance(value, enum.Enum):
        return value.value
    if isinstance(value, datetime.datetime):
        return value.isoformat()
    if isinstance(value, datetime.date):
        return value.isoformat()
    if isinstance(value, list):
        return [_serialize_trace_value(item) for item in value]
    if isinstance(value, tuple):
        return [_serialize_trace_value(item) for item in value]
    if isinstance(value, dict):
        return {
            str(key): _serialize_trace_value(item)
            for key, item in value.items()
        }
    if value is None or isinstance(value, (str, int, float, bool)):
        return value
    return str(value)


def _build_compact_json_text(value: Any, *, max_length: int) -> str:
    """
    功能描述：
        构造给模型阅读的紧凑 JSON 文本，并按上限截断。

    参数说明：
        value (Any): 原始值。
        max_length (int): 最大字符数。

    返回值：
        str: 紧凑 JSON 文本。

    异常说明：
        无。
    """

    serialized_text = json.dumps(
        _serialize_trace_value(value),
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    )
    if len(serialized_text) <= max_length:
        return serialized_text
    truncated_text = serialized_text[:max_length].rstrip()
    return f"{truncated_text}...(已截断)"


def _build_summary_text(
        *,
        tool_display_name: str,
        status: ToolTraceStatus,
        tool_input: Any,
        tool_output: Any | None,
        error_payload: dict[str, Any] | None,
) -> str:
    """
    功能描述：
        为单次工具调用构造稳定中文摘要。

    参数说明：
        tool_display_name (str): 工具展示名称。
        status (ToolTraceStatus): 工具执行状态。
        tool_input (Any): 工具输入。
        tool_output (Any | None): 工具输出。
        error_payload (dict[str, Any] | None): 失败结构化错误信息。

    返回值：
        str: 给模型消费的稳定中文摘要。

    异常说明：
        无。
    """

    input_text = _build_compact_json_text(
        tool_input,
        max_length=_TOOL_TRACE_INPUT_SUMMARY_MAX_LENGTH,
    )
    if status == ToolTraceStatus.SUCCESS:
        output_text = _build_compact_json_text(
            tool_output,
            max_length=_TOOL_TRACE_OUTPUT_SUMMARY_MAX_LENGTH,
        )
        return f"工具调用：{tool_display_name}。输入：{input_text}。结果：{output_text}。"

    error_message = ""
    if isinstance(error_payload, dict):
        error_message = str(error_payload.get("error_message") or "").strip()
    if not error_message:
        error_message = "未知错误"
    return f"工具调用：{tool_display_name}。输入：{input_text}。结果：调用失败，错误={error_message}。"


def bind_tool_trace_context(
        *,
        conversation_uuid: str,
        assistant_message_uuid: str,
        conversation_type: ConversationType | str,
) -> Token[ToolTraceContext | None]:
    """
    功能描述：
        绑定当前工具轨迹写入上下文。

    参数说明：
        conversation_uuid (str): 所属会话 UUID。
        assistant_message_uuid (str): 所属 AI 回复消息 UUID。
        conversation_type (ConversationType | str): 会话类型。

    返回值：
        Token[ToolTraceContext | None]: `ContextVar` 重置令牌。

    异常说明：
        ValueError: 当任一上下文字段为空时抛出。
    """

    context = ToolTraceContext(
        conversation_uuid=_normalize_required_text(
            conversation_uuid,
            field_name="conversation_uuid",
        ),
        assistant_message_uuid=_normalize_required_text(
            assistant_message_uuid,
            field_name="assistant_message_uuid",
        ),
        conversation_type=ConversationType(conversation_type),
    )
    return _TOOL_TRACE_CONTEXT.set(context)


def reset_tool_trace_context(token: Token[ToolTraceContext | None]) -> None:
    """
    功能描述：
        重置当前工具轨迹上下文。

    参数说明：
        token (Token[ToolTraceContext | None]): 绑定时返回的重置令牌。

    返回值：
        None

    异常说明：
        无。
    """

    _TOOL_TRACE_CONTEXT.reset(token)


def _get_current_tool_trace_context() -> ToolTraceContext:
    """
    功能描述：
        读取当前上下文绑定的工具轨迹信息。

    参数说明：
        无。

    返回值：
        ToolTraceContext: 当前工具轨迹上下文。

    异常说明：
        ValueError: 当当前上下文未绑定工具轨迹时抛出。
    """

    context = _TOOL_TRACE_CONTEXT.get()
    if context is None:
        raise ValueError("tool_trace_context 未绑定")
    return context


def save_current_tool_trace_entry(
        *,
        tool_name: str,
        tool_display_name: str,
        status: ToolTraceStatus | str,
        tool_input: Any,
        tool_output: Any | None,
        error_payload: dict[str, Any] | None = None,
) -> str:
    """
    功能描述：
        基于当前上下文保存单条工具轨迹。

    参数说明：
        tool_name (str): 工具原始名称。
        tool_display_name (str): 工具展示名称。
        status (ToolTraceStatus | str): 工具执行状态。
        tool_input (Any): 工具输入。
        tool_output (Any | None): 工具输出。
        error_payload (dict[str, Any] | None): 工具失败结构化错误信息。

    返回值：
        str: 新增工具轨迹的 Mongo ObjectId 字符串。

    异常说明：
        ValueError: 当上下文未绑定或关键字段为空时抛出。
    """

    context = _get_current_tool_trace_context()
    normalized_status = ToolTraceStatus(status)
    normalized_input = _serialize_trace_value(tool_input)
    normalized_output = _serialize_trace_value(tool_output)
    normalized_error_payload = (
        {
            str(key): _serialize_trace_value(value)
            for key, value in error_payload.items()
        }
        if isinstance(error_payload, dict)
        else None
    )
    summary_text = _build_summary_text(
        tool_display_name=_normalize_required_text(
            tool_display_name,
            field_name="tool_display_name",
        ),
        status=normalized_status,
        tool_input=normalized_input,
        tool_output=normalized_output,
        error_payload=normalized_error_payload,
    )
    return add_tool_trace(
        conversation_uuid=context.conversation_uuid,
        assistant_message_uuid=context.assistant_message_uuid,
        conversation_type=context.conversation_type.value,
        tool_name=_normalize_required_text(tool_name, field_name="tool_name"),
        tool_display_name=tool_display_name,
        status=normalized_status,
        summary_text=summary_text,
        input_payload=normalized_input,
        output_payload=normalized_output,
        error_payload=normalized_error_payload,
    )


def render_tool_trace_prompt(
        *,
        conversation_uuid: str,
        limit: int = DEFAULT_TOOL_TRACE_PROMPT_LIMIT,
) -> str:
    """
    功能描述：
        渲染当前会话最近工具轨迹的模型上下文片段。

    参数说明：
        conversation_uuid (str): 所属会话 UUID。
        limit (int): 最大读取条数。

    返回值：
        str: 工具轨迹提示词正文；没有轨迹时返回空字符串。

    异常说明：
        ValueError: 当会话 UUID 为空时抛出。
    """

    normalized_conversation_uuid = _normalize_required_text(
        conversation_uuid,
        field_name="conversation_uuid",
    )
    recent_traces = list_recent_tool_traces(
        normalized_conversation_uuid,
        limit=limit,
    )
    if not recent_traces:
        return ""

    prompt_lines: list[str] = []
    ordered_traces: list[ToolTraceDocument] = list(reversed(recent_traces))
    for index, trace_document in enumerate(ordered_traces, start=1):
        status_text = "成功" if trace_document.status == ToolTraceStatus.SUCCESS else "失败"
        prompt_lines.append(f"{index}. [{status_text}] {trace_document.summary_text}")
    return "\n".join(prompt_lines).strip()


def bind_tool_trace_arguments(
        func: Any,
        args: tuple[Any, ...],
        kwargs: dict[str, Any],
) -> dict[str, Any]:
    """
    功能描述：
        根据工具函数签名绑定当前调用的业务参数，并剔除运行时注入字段。

    参数说明：
        func (Any): 原始工具函数。
        args (tuple[Any, ...]): 位置参数。
        kwargs (dict[str, Any]): 关键字参数。

    返回值：
        dict[str, Any]: 仅包含业务参数的映射。

    异常说明：
        TypeError: 当参数绑定失败时抛出。
    """

    signature = inspect.signature(func)
    bound_arguments = signature.bind_partial(*args, **kwargs)
    bound_arguments.apply_defaults()
    excluded_parameter_names = {"runtime", "callbacks", "config", "run_manager"}
    return {
        key: value
        for key, value in dict(bound_arguments.arguments).items()
        if key not in excluded_parameter_names
    }


__all__ = [
    "DEFAULT_TOOL_TRACE_PROMPT_LIMIT",
    "ToolTraceContext",
    "bind_tool_trace_arguments",
    "bind_tool_trace_context",
    "render_tool_trace_prompt",
    "reset_tool_trace_context",
    "save_current_tool_trace_entry",
]
