from __future__ import annotations

import datetime
import time
from collections.abc import Awaitable, Callable, Mapping
from typing import Any

from langchain.agents.middleware import AgentMiddleware, ModelRequest, ModelResponse, ToolCallLimitMiddleware, \
    wrap_tool_call
from langchain.agents.middleware.types import hook_config
from langchain.messages import ToolMessage
from langchain.tools.tool_node import ToolCallRequest
from langgraph.types import Command

from app.core.agent.tracing.context import get_current_trace_state
from app.core.agent.tracing.decorators import AgentTraceSpan
from app.core.agent.tracing.ids import generate_usage_id
from app.core.agent.tracing.serializer import (
    serialize_exception,
    serialize_messages,
    serialize_tools,
    serialize_value,
)
from app.core.agent.tracing.token_usage import (
    extract_finish_reason,
    extract_response_model_token_usage,
    extract_response_raw_usage,
    extract_response_token_usage,
    extract_tool_calls,
)
from app.core.agent.tracing.writer import enqueue_trace_operation
from app.core.llms.provider import resolve_provider
from app.schemas.document.agent_trace import AgentTraceSpanType


def _resolve_model_name(model: Any) -> str:
    """
    功能描述：
        从模型对象解析模型名称。

    参数说明：
        model (Any): LangChain 模型对象。

    返回值：
        str: 模型名称；无法解析时返回类名。
    """

    for attr_name in ("model_name", "model", "model_id"):
        value = getattr(model, attr_name, None)
        if isinstance(value, str) and value.strip():
            return value.strip()
    return model.__class__.__name__


def _resolve_model_provider(model: Any) -> str:
    """
    功能描述：
        解析模型提供商标识。

    参数说明：
        model (Any): LangChain 模型对象。

    返回值：
        str: 模型提供商标识。
    """

    explicit_provider = getattr(model, "provider", None)
    if isinstance(explicit_provider, str) and explicit_provider.strip():
        return explicit_provider.strip()
    return resolve_provider().value


def _resolve_tool_display_name(tool_name: str) -> str:
    """
    功能描述：
        延迟解析工具展示名称，避免 tracing middleware 初始化时触发 agent middleware 循环导入。

    参数说明：
        tool_name (str): 工具注册名称。

    返回值：
        str: 用户可见的工具展示名称。
    """

    from app.core.agent.middleware.tool_status import resolve_tool_display_name

    return resolve_tool_display_name(tool_name)


def _extract_model_settings(request: ModelRequest) -> dict[str, Any]:
    """
    功能描述：
        汇总模型调用设置。

    参数说明：
        request (ModelRequest): 当前模型请求。

    返回值：
        dict[str, Any]: 模型设置摘要。
    """

    settings = dict(getattr(request, "model_settings", None) or {})
    model = request.model
    for attr_name in ("temperature", "max_tokens", "stream_usage"):
        value = getattr(model, attr_name, None)
        if value is not None:
            settings.setdefault(attr_name, value)
    model_kwargs = getattr(model, "model_kwargs", None)
    if isinstance(model_kwargs, Mapping):
        settings.setdefault("model_kwargs", dict(model_kwargs))
    extra_body = getattr(model, "extra_body", None)
    if isinstance(extra_body, Mapping):
        settings.setdefault("extra_body", dict(extra_body))
    return settings


def _build_system_prompt_payload(system_message: Any) -> dict[str, Any]:
    """
    功能描述：
        构造模型详情使用的系统提示词载荷。

    参数说明：
        system_message (Any): LangChain 系统消息对象。

    返回值：
        dict[str, Any]: 系统提示词载荷。
    """

    return {
        "content": serialize_value(_extract_text_from_content(getattr(system_message, "content", None))),
        "render_mode": "markdown",
    }


def _build_model_input_payload(request: ModelRequest) -> dict[str, Any]:
    """
    功能描述：
        构造模型 span 输入载荷。

    参数说明：
        request (ModelRequest): 当前模型请求。

    返回值：
        dict[str, Any]: 模型输入载荷。
    """

    available_tools = serialize_tools(request.tools, display_name_resolver=_resolve_tool_display_name)
    return {
        "system_prompt": _build_system_prompt_payload(request.system_message),
        "messages": serialize_messages(request.messages),
        "tools": available_tools,
        "tool_choice": serialize_value(request.tool_choice),
        "response_format": serialize_value(request.response_format),
    }


def _build_model_attributes(request: ModelRequest, *, slot: str | None) -> dict[str, Any]:
    """
    功能描述：
        构造模型 span 属性。

    参数说明：
        request (ModelRequest): 当前模型请求。
        slot (str | None): 业务模型槽位。

    返回值：
        dict[str, Any]: 模型属性。
    """

    return {
        "provider": _resolve_model_provider(request.model),
        "model_class": request.model.__class__.__name__,
        "model_name": _resolve_model_name(request.model),
        "slot": slot,
        "cache_mode": _resolve_cache_mode(request=request, token_usage={}),
        "model_settings": serialize_value(_extract_model_settings(request)),
    }


def _resolve_cache_mode(*, request: ModelRequest, token_usage: dict[str, int]) -> str:
    """
    功能描述：
        根据请求缓存标记和模型返回的缓存 token 判断缓存模式。

    参数说明：
        request (ModelRequest): 当前模型请求。
        token_usage (dict[str, int]): 标准化 Token 用量。

    返回值：
        str: 缓存模式，取值为 `explicit` / `implicit` / `none`。
    """

    system_message_content = getattr(request.system_message, "content", None)
    if _has_dashscope_explicit_cache_control(system_message_content):
        return "explicit"
    cache_read_tokens = int(token_usage.get("cache_read_tokens") or 0)
    cache_write_tokens = int(token_usage.get("cache_write_tokens") or 0)
    cache_total_tokens = int(token_usage.get("cache_total_tokens") or 0)
    if cache_read_tokens > 0 or cache_write_tokens > 0 or cache_total_tokens > 0:
        return "implicit"
    return "none"


def _has_dashscope_explicit_cache_control(content: Any) -> bool:
    """
    功能描述：
        判断模型请求内容中是否携带 DashScope 显式缓存标记。

    参数说明：
        content (Any): LangChain 消息 content，可能是字符串或 content block 数组。

    返回值：
        bool: 存在 `cache_control` 标记时返回 True。
    """

    if isinstance(content, Mapping):
        cache_control = content.get("cache_control")
        return isinstance(cache_control, Mapping) and bool(cache_control.get("type"))
    if isinstance(content, list | tuple):
        return any(_has_dashscope_explicit_cache_control(item) for item in content)
    return False


def _build_model_token_usage_document(
        *,
        span: AgentTraceSpan,
        request: ModelRequest,
        token_usage: dict[str, int],
        raw_usage: dict[str, Any],
        status: str,
        error_payload: dict[str, Any] | None = None,
) -> dict[str, Any] | None:
    """
    功能描述：
        构造单次模型调用的 Token 明细文档。

    参数说明：
        span (AgentTraceSpan): 当前模型 span。
        request (ModelRequest): 当前模型请求。
        token_usage (dict[str, int]): 标准化 Token 用量。
        raw_usage (dict[str, Any]): 模型供应商原始 usage 结构。
        status (str): 调用状态。
        error_payload (dict[str, Any] | None): 错误结构。

    返回值：
        dict[str, Any] | None: 可写入 Mongo 的 Token 明细文档；无 trace 上下文时返回 None。
    """

    trace_state = span.trace_state
    if trace_state is None:
        return None
    duration_ms = max(0, int((time.monotonic() - span.started_monotonic) * 1000))
    ended_at = span.started_at + datetime.timedelta(milliseconds=duration_ms)
    nearest_node = trace_state.find_nearest_span_metadata(AgentTraceSpanType.NODE.value)
    model_name = _resolve_model_name(request.model)
    model_class = request.model.__class__.__name__
    return {
        "usage_id": generate_usage_id(),
        "trace_id": trace_state.trace_id,
        "span_id": span.span_id,
        "conversation_uuid": trace_state.conversation_uuid,
        "assistant_message_uuid": trace_state.assistant_message_uuid,
        "user_id": trace_state.user_id,
        "conversation_type": trace_state.conversation_type,
        "graph_name": trace_state.graph_name,
        "entrypoint": trace_state.entrypoint,
        "node_name": nearest_node.get("name") if nearest_node else None,
        "slot": span.attributes.get("slot") if isinstance(span.attributes, dict) else None,
        "provider": _resolve_model_provider(request.model),
        "model_name": model_name,
        "model_class": model_class,
        "status": status,
        "error_type": (
            error_payload.get("error_type") or error_payload.get("type")
            if error_payload else None
        ),
        "error_message": (
            error_payload.get("error_message") or error_payload.get("message")
            if error_payload else None
        ),
        "started_at": span.started_at,
        "ended_at": ended_at,
        "duration_ms": duration_ms,
        "input_tokens": int(token_usage.get("input_tokens") or 0),
        "output_tokens": int(token_usage.get("output_tokens") or 0),
        "total_tokens": int(token_usage.get("total_tokens") or 0),
        "cache_read_tokens": int(token_usage.get("cache_read_tokens") or 0),
        "cache_write_tokens": int(token_usage.get("cache_write_tokens") or 0),
        "cache_total_tokens": int(token_usage.get("cache_total_tokens") or 0),
        "cache_mode": _resolve_cache_mode(request=request, token_usage=token_usage),
        "raw_usage": serialize_value(raw_usage),
        "created_at": ended_at,
    }


def _enqueue_model_token_usage_document(document: dict[str, Any] | None) -> None:
    """
    功能描述：
        投递模型 Token 明细写入操作。

    参数说明：
        document (dict[str, Any] | None): Token 明细文档。

    返回值：
        None。
    """

    if not document:
        return
    enqueue_trace_operation(
        {
            "type": "insert_model_token_usage",
            "document": document,
        }
    )


def _extract_text_from_content(content: Any) -> str | None:
    """
    功能描述：
        从模型消息 content 中提取可展示文本。

    参数说明：
        content (Any): 模型消息内容，可能是字符串或多模态片段数组。

    返回值：
        str | None: 文本内容；没有文本时返回 None。
    """

    if isinstance(content, str):
        normalized_content = content.strip()
        return normalized_content or None
    if isinstance(content, list):
        text_parts: list[str] = []
        for item in content:
            if isinstance(item, str) and item.strip():
                text_parts.append(item.strip())
                continue
            if not isinstance(item, Mapping):
                continue
            item_text = item.get("text") or item.get("content")
            if isinstance(item_text, str) and item_text.strip():
                text_parts.append(item_text.strip())
        if text_parts:
            return "\n".join(text_parts)
    return None


def _extract_final_text(messages: list[Any]) -> str | None:
    """
    功能描述：
        从模型响应消息中提取最终自然语言文本。

    参数说明：
        messages (list[Any]): 模型响应消息列表。

    返回值：
        str | None: 最终文本；不存在时返回 None。
    """

    for message in reversed(messages):
        text = _extract_text_from_content(getattr(message, "content", None))
        if text:
            return text
    return None


def _normalize_tool_call(tool_call: Any) -> dict[str, Any] | None:
    """
    功能描述：
        将模型输出中的工具调用规范化为稳定结构。

    参数说明：
        tool_call (Any): 原始工具调用对象。

    返回值：
        dict[str, Any] | None: 规范化工具调用；无有效内容时返回 None。
    """

    if isinstance(tool_call, Mapping):
        tool_call_id = str(tool_call.get("id") or tool_call.get("tool_call_id") or "").strip()
        tool_name = str(tool_call.get("name") or "").strip()
        arguments = tool_call.get("args") if "args" in tool_call else tool_call.get("arguments")
    else:
        tool_call_id = str(getattr(tool_call, "id", "") or getattr(tool_call, "tool_call_id", "") or "").strip()
        tool_name = str(getattr(tool_call, "name", "") or "").strip()
        arguments = getattr(tool_call, "args", None)
    if not tool_call_id and not tool_name and arguments is None:
        return None
    return {
        "id": tool_call_id,
        "name": tool_name,
        "arguments": serialize_value(arguments),
    }


def _normalize_tool_calls(tool_calls: list[Any]) -> list[dict[str, Any]]:
    """
    功能描述：
        批量规范化模型输出中的工具调用。

    参数说明：
        tool_calls (list[Any]): 原始工具调用列表。

    返回值：
        list[dict[str, Any]]: 规范化后的工具调用列表。
    """

    normalized_tool_calls: list[dict[str, Any]] = []
    for tool_call in tool_calls:
        normalized_tool_call = _normalize_tool_call(tool_call)
        if normalized_tool_call is not None:
            normalized_tool_calls.append(normalized_tool_call)
    return normalized_tool_calls


def _build_model_output_payload(response: ModelResponse) -> dict[str, Any]:
    """
    功能描述：
        构造模型 span 输出载荷。

    参数说明：
        response (ModelResponse): 模型响应。

    返回值：
        dict[str, Any]: 模型输出载荷。
    """

    result_messages = list(response.result or [])
    normalized_tool_calls = _normalize_tool_calls(extract_tool_calls(result_messages))
    return {
        "messages": serialize_messages(result_messages),
        "structured_response": serialize_value(response.structured_response),
        "finish_reason": extract_finish_reason(result_messages),
        "tool_calls": normalized_tool_calls,
        "final_text": serialize_value(_extract_final_text(result_messages)),
    }


class TraceModelMiddleware(AgentMiddleware):
    """
    功能描述：
        记录模型调用输入、输出、工具列表与 token 用量的 middleware。

    参数说明：
        slot (str | None): 当前业务模型槽位。

    返回值：
        无（middleware 对象）。
    """

    def __init__(self, *, slot: str | None = None) -> None:
        self.slot = slot

    def wrap_model_call(
            self,
            request: ModelRequest,
            handler: Callable[[ModelRequest], ModelResponse],
    ) -> ModelResponse:
        """
        功能描述：
            同步记录模型调用 span。

        参数说明：
            request (ModelRequest): 当前模型请求。
            handler (Callable[[ModelRequest], ModelResponse]): 下游模型处理器。

        返回值：
            ModelResponse: 模型响应。
        """

        with AgentTraceSpan(
                name="model",
                span_type=AgentTraceSpanType.MODEL,
                input_payload=_build_model_input_payload(request),
                attributes=_build_model_attributes(request, slot=self.slot),
        ) as span:
            try:
                response = handler(request)
            except Exception as exc:
                error_payload = serialize_exception(exc)
                _enqueue_model_token_usage_document(
                    _build_model_token_usage_document(
                        span=span,
                        request=request,
                        token_usage={},
                        raw_usage={},
                        status="error",
                        error_payload=error_payload,
                    )
                )
                raise
            token_usage = extract_response_token_usage(response.result)
            model_token_usage = extract_response_model_token_usage(response.result)
            raw_usage = extract_response_raw_usage(response.result)
            trace_state = get_current_trace_state()
            if trace_state is not None:
                trace_state.add_token_usage(**token_usage)
            span.finish(
                output_payload=_build_model_output_payload(response),
                token_usage=token_usage,
            )
            _enqueue_model_token_usage_document(
                _build_model_token_usage_document(
                    span=span,
                    request=request,
                    token_usage=model_token_usage,
                    raw_usage=raw_usage,
                    status="success",
                )
            )
            return response

    async def awrap_model_call(
            self,
            request: ModelRequest,
            handler: Callable[[ModelRequest], Awaitable[ModelResponse]],
    ) -> ModelResponse:
        """
        功能描述：
            异步记录模型调用 span。

        参数说明：
            request (ModelRequest): 当前模型请求。
            handler (Callable[[ModelRequest], Awaitable[ModelResponse]]): 下游模型处理器。

        返回值：
            ModelResponse: 模型响应。
        """

        with AgentTraceSpan(
                name="model",
                span_type=AgentTraceSpanType.MODEL,
                input_payload=_build_model_input_payload(request),
                attributes=_build_model_attributes(request, slot=self.slot),
        ) as span:
            try:
                response = await handler(request)
            except Exception as exc:
                error_payload = serialize_exception(exc)
                _enqueue_model_token_usage_document(
                    _build_model_token_usage_document(
                        span=span,
                        request=request,
                        token_usage={},
                        raw_usage={},
                        status="error",
                        error_payload=error_payload,
                    )
                )
                raise
            token_usage = extract_response_token_usage(response.result)
            model_token_usage = extract_response_model_token_usage(response.result)
            raw_usage = extract_response_raw_usage(response.result)
            trace_state = get_current_trace_state()
            if trace_state is not None:
                trace_state.add_token_usage(**token_usage)
            span.finish(
                output_payload=_build_model_output_payload(response),
                token_usage=token_usage,
            )
            _enqueue_model_token_usage_document(
                _build_model_token_usage_document(
                    span=span,
                    request=request,
                    token_usage=model_token_usage,
                    raw_usage=raw_usage,
                    status="success",
                )
            )
            return response


def _parse_tool_request(request: ToolCallRequest) -> tuple[str, str, Any]:
    """
    功能描述：
        从工具请求中解析名称、调用 ID 和参数。

    参数说明：
        request (ToolCallRequest): 当前工具调用请求。

    返回值：
        tuple[str, str, Any]: 工具名、tool_call_id、工具参数。
    """

    tool_call = request.tool_call if isinstance(request.tool_call, Mapping) else {}
    tool_name = str(tool_call.get("name") or "").strip()
    tool_call_id = str(tool_call.get("id") or "").strip()
    tool_args = tool_call.get("args")
    return tool_name, tool_call_id, tool_args


def build_trace_tool_middleware():
    """
    功能描述：
        构建工具调用 trace middleware。

    参数说明：
        无。

    返回值：
        Any: 可传给 `create_agent(..., middleware=[...])` 的工具拦截器。
    """

    @wrap_tool_call
    async def _trace_tool_middleware(
            request: ToolCallRequest,
            handler: Callable[[ToolCallRequest], Awaitable[ToolMessage | Command | None]],
    ) -> ToolMessage | Command | None:
        """
        功能描述：
            记录单次工具调用输入、输出和异常。

        参数说明：
            request (ToolCallRequest): 当前工具调用请求。
            handler (Callable[[ToolCallRequest], Awaitable[ToolMessage | Command | None]]): 下游工具处理器。

        返回值：
            ToolMessage | Command | None: 工具执行结果。
        """

        tool_name, tool_call_id, tool_args = _parse_tool_request(request)
        display_name = _resolve_tool_display_name(tool_name)
        with AgentTraceSpan(
                name=tool_name or "unknown_tool",
                span_type=AgentTraceSpanType.TOOL,
                input_payload={
                    "tool_name": tool_name,
                    "tool_call_id": tool_call_id,
                    "arguments": tool_args,
                },
                attributes={
                    "display_name": display_name,
                },
        ) as span:
            try:
                result = await handler(request)
            except Exception as exc:
                span.finish(
                    output_payload=None,
                    attributes={"error_payload": serialize_exception(exc)},
                    error=exc,
                )
                raise
            span.finish(output_payload=serialize_value(result))
            return result

    return _trace_tool_middleware


class TracedToolCallLimitMiddleware(ToolCallLimitMiddleware):
    """
    功能描述：
        带 trace span 的 ToolCallLimitMiddleware。

    参数说明：
        继承第三方 ToolCallLimitMiddleware 的初始化参数。

    返回值：
        无（middleware 对象）。
    """

    @hook_config(can_jump_to=["end"])
    def after_model(self, state: Any, runtime: Any) -> dict[str, Any] | None:
        """
        功能描述：
            同步记录 after_model 阶段并执行工具调用次数限制。

        参数说明：
            state (Any): 当前 agent 状态。
            runtime (Any): LangGraph 运行时。

        返回值：
            dict[str, Any] | None: 状态更新。
        """

        with AgentTraceSpan(
                name=f"{self.name}.after_model",
                span_type=AgentTraceSpanType.MIDDLEWARE,
                input_payload={
                    "thread_limit": self.thread_limit,
                    "run_limit": self.run_limit,
                    "tool_name": self.tool_name,
                },
        ) as span:
            result = super().after_model(state, runtime)
            span.finish(output_payload=result)
            return result

    @hook_config(can_jump_to=["end"])
    async def aafter_model(self, state: Any, runtime: Any) -> dict[str, Any] | None:
        """
        功能描述：
            异步记录 after_model 阶段并执行工具调用次数限制。

        参数说明：
            state (Any): 当前 agent 状态。
            runtime (Any): LangGraph 运行时。

        返回值：
            dict[str, Any] | None: 状态更新。
        """

        return self.after_model(state, runtime)
