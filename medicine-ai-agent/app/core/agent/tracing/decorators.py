from __future__ import annotations

import datetime
import functools
import inspect
import time
from collections.abc import Callable
from typing import Any, ParamSpec, TypeVar

from app.core.agent.tracing.config import is_agent_trace_enabled
from app.core.agent.tracing.context import (
    AgentTraceState,
    create_trace_state,
    get_current_span_id,
    get_current_trace_state,
    pop_span,
    push_span,
)
from app.core.agent.tracing.ids import generate_span_id
from app.core.agent.tracing.serializer import serialize_exception, serialize_value
from app.core.agent.tracing.time_utils import utc_now
from app.core.agent.tracing.writer import enqueue_trace_operation
from app.schemas.document.agent_trace import AgentTraceSpanType, AgentTraceStatus

P = ParamSpec("P")
R = TypeVar("R")


def _now() -> datetime.datetime:
    """
    功能描述：
        获取当前 UTC 时间。

    参数说明：
        无。

    返回值：
        datetime.datetime: 带 UTC 时区信息的当前时间。
    """

    return utc_now()


def _duration_ms(started_monotonic: float) -> int:
    """
    功能描述：
        根据单调时钟计算耗时毫秒。

    参数说明：
        started_monotonic (float): 开始单调时钟。

    返回值：
        int: 耗时毫秒。
    """

    return max(0, int((time.monotonic() - started_monotonic) * 1000))


def start_trace_run(
        *,
        graph_name: str,
        conversation_uuid: str,
        assistant_message_uuid: str,
        user_id: int | None,
        conversation_type: str,
        entrypoint: str,
        message_view: dict[str, Any] | None = None,
) -> AgentTraceState | None:
    """
    功能描述：
        创建 trace run 并投递运行汇总写入事件。

    参数说明：
        graph_name (str): Graph 名称。
        conversation_uuid (str): 会话 UUID。
        assistant_message_uuid (str): 当前 AI 消息 UUID。
        user_id (int | None): 用户 ID。
        conversation_type (str): 会话类型。
        entrypoint (str): 入口标识。
        message_view (dict[str, Any] | None): 顶层消息视图快照。

    返回值：
        AgentTraceState | None: trace 状态；未启用时返回 None。
    """

    if not is_agent_trace_enabled():
        return None
    state = create_trace_state(
        graph_name=graph_name,
        conversation_uuid=conversation_uuid,
        assistant_message_uuid=assistant_message_uuid,
        user_id=user_id,
        conversation_type=conversation_type,
        entrypoint=entrypoint,
        message_view=message_view,
    )
    now = _now()
    enqueue_trace_operation(
        {
            "type": "insert_run",
            "document": {
                "trace_id": state.trace_id,
                "conversation_uuid": conversation_uuid,
                "assistant_message_uuid": assistant_message_uuid,
                "user_id": user_id,
                "conversation_type": conversation_type,
                "graph_name": graph_name,
                "entrypoint": entrypoint,
                "status": AgentTraceStatus.RUNNING.value,
                "started_at": state.started_at,
                "ended_at": None,
                "duration_ms": None,
                "root_span_id": state.root_span_id,
                "input_tokens": 0,
                "output_tokens": 0,
                "total_tokens": 0,
                "error_payload": None,
                "created_at": now,
                "updated_at": now,
            },
        }
    )
    return state


def finish_trace_run(
        *,
        state: AgentTraceState,
        status: AgentTraceStatus | str,
        final_text: str | None = None,
        error_payload: dict[str, Any] | None = None,
) -> None:
    """
    功能描述：
        结束 trace run 并写入根 graph span。

    参数说明：
        state (AgentTraceState): 当前 trace 状态。
        status (AgentTraceStatus | str): 最终状态。
        final_text (str | None): 本轮助手最终输出文本。
        error_payload (dict[str, Any] | None): 错误结构。

    返回值：
        None。
    """

    ended_at = _now()
    duration_ms = _duration_ms(state.started_monotonic)
    normalized_status = AgentTraceStatus(status).value
    root_input_payload: dict[str, Any] = {
        "conversation_uuid": state.conversation_uuid,
        "assistant_message_uuid": state.assistant_message_uuid,
        "conversation_type": state.conversation_type,
    }
    if state.message_view is not None:
        root_input_payload["message_view"] = state.message_view
    enqueue_trace_operation(
        {
            "type": "insert_span",
            "document": {
                "trace_id": state.trace_id,
                "span_id": state.root_span_id,
                "parent_span_id": None,
                "span_type": AgentTraceSpanType.GRAPH.value,
                "name": state.graph_name,
                "status": normalized_status,
                "started_at": state.started_at,
                "ended_at": ended_at,
                "duration_ms": duration_ms,
                "input_payload": root_input_payload,
                "output_payload": {
                    "final_text": final_text,
                    "finish_status": normalized_status,
                },
                "attributes": {
                    "entrypoint": state.entrypoint,
                    "user_id": state.user_id,
                },
                "token_usage": {
                    "input_tokens": state.input_tokens,
                    "output_tokens": state.output_tokens,
                    "total_tokens": state.total_tokens,
                },
                "error_payload": error_payload,
                "sequence": state.root_sequence,
            },
        }
    )
    enqueue_trace_operation(
        {
            "type": "update_run",
            "trace_id": state.trace_id,
            "set_on_insert": {
                "conversation_uuid": state.conversation_uuid,
                "assistant_message_uuid": state.assistant_message_uuid,
                "user_id": state.user_id,
                "conversation_type": state.conversation_type,
                "graph_name": state.graph_name,
                "entrypoint": state.entrypoint,
                "started_at": state.started_at,
                "root_span_id": state.root_span_id,
                "created_at": state.started_at,
            },
            "updates": {
                "status": normalized_status,
                "ended_at": ended_at,
                "duration_ms": duration_ms,
                "input_tokens": state.input_tokens,
                "output_tokens": state.output_tokens,
                "total_tokens": state.total_tokens,
                "error_payload": error_payload,
                "updated_at": ended_at,
            },
        }
    )


class AgentTraceSpan:
    """
    功能描述：
        单个 Agent Trace span 上下文。

    参数说明：
        name (str): Span 名称。
        span_type (AgentTraceSpanType | str): Span 类型。
        input_payload (Any | None): 输入载荷。
        attributes (dict[str, Any] | None): 附加属性。

    返回值：
        无（上下文对象）。
    """

    def __init__(
            self,
            *,
            name: str,
            span_type: AgentTraceSpanType | str,
            input_payload: Any | None = None,
            attributes: dict[str, Any] | None = None,
    ) -> None:
        self.name = name
        self.span_type = AgentTraceSpanType(span_type)
        self.input_payload = input_payload
        self.attributes = attributes
        self.trace_state = get_current_trace_state()
        self.parent_span_id = get_current_span_id()
        self.span_id = generate_span_id()
        self.started_at = _now()
        self.started_monotonic = time.monotonic()
        self.enabled = self.trace_state is not None and is_agent_trace_enabled()
        self.finished = False
        if self.enabled and self.trace_state is not None:
            self.trace_state.register_span_metadata(
                self.span_id,
                {
                    "span_id": self.span_id,
                    "parent_span_id": self.parent_span_id,
                    "span_type": self.span_type.value,
                    "name": self.name,
                    "started_at": self.started_at,
                },
            )

    def __enter__(self) -> "AgentTraceSpan":
        """
        功能描述：
            进入 span 上下文。

        参数说明：
            无。

        返回值：
            AgentTraceSpan: 当前 span。
        """

        if self.enabled:
            push_span(self.span_id)
        return self

    def __exit__(self, exc_type: Any, exc: BaseException | None, traceback: Any) -> None:
        """
        功能描述：
            退出 span 上下文并写入 span 文档。

        参数说明：
            exc_type (Any): 异常类型。
            exc (BaseException | None): 异常对象。
            traceback (Any): 异常堆栈。

        返回值：
            None。
        """

        _ = exc_type, traceback
        if self.enabled:
            if not self.finished:
                self.finish(error=exc)
            pop_span(self.span_id)

    def finish(
            self,
            *,
            output_payload: Any | None = None,
            attributes: dict[str, Any] | None = None,
            token_usage: dict[str, int] | None = None,
            error: BaseException | None = None,
    ) -> None:
        """
        功能描述：
            写入当前 span。

        参数说明：
            output_payload (Any | None): 输出载荷。
            attributes (dict[str, Any] | None): 本次额外属性。
            token_usage (dict[str, int] | None): Token 用量。
            error (BaseException | None): 异常对象。

        返回值：
            None。
        """

        if not self.enabled or self.trace_state is None or self.finished:
            return
        self.finished = True
        merged_attributes = dict(self.attributes or {})
        if attributes:
            merged_attributes.update(attributes)
        error_payload = serialize_exception(error) if error is not None else None
        status = AgentTraceStatus.ERROR if error is not None else AgentTraceStatus.SUCCESS
        enqueue_trace_operation(
            {
                "type": "insert_span",
                "document": {
                    "trace_id": self.trace_state.trace_id,
                    "span_id": self.span_id,
                    "parent_span_id": self.parent_span_id,
                    "span_type": self.span_type.value,
                    "name": self.name,
                    "status": status.value,
                    "started_at": self.started_at,
                    "ended_at": _now(),
                    "duration_ms": _duration_ms(self.started_monotonic),
                    "input_payload": serialize_value(self.input_payload),
                    "output_payload": serialize_value(output_payload),
                    "attributes": serialize_value(merged_attributes) if merged_attributes else None,
                    "token_usage": token_usage,
                    "error_payload": error_payload,
                    "sequence": self.trace_state.next_sequence(),
                },
            }
        )


def agent_trace(
        *,
        name: str,
        span_type: AgentTraceSpanType | str = AgentTraceSpanType.NODE,
        attributes: dict[str, Any] | None = None,
) -> Callable[[Callable[P, R]], Callable[P, R]]:
    """
    功能描述：
        为节点或普通函数记录 Agent Trace span。

    参数说明：
        name (str): Span 名称。
        span_type (AgentTraceSpanType | str): Span 类型。
        attributes (dict[str, Any] | None): 静态附加属性。

    返回值：
        Callable[[Callable[P, R]], Callable[P, R]]: 装饰器函数。
    """

    def _decorate(func: Callable[P, R]) -> Callable[P, R]:
        if inspect.iscoroutinefunction(func):
            @functools.wraps(func)
            async def _async_wrapper(*args: P.args, **kwargs: P.kwargs) -> R:
                with AgentTraceSpan(
                        name=name,
                        span_type=span_type,
                        input_payload={"args": args, "kwargs": kwargs},
                        attributes=attributes,
                ) as span:
                    result = await func(*args, **kwargs)
                    span.finish(output_payload=result)
                    return result

            return _async_wrapper

        @functools.wraps(func)
        def _sync_wrapper(*args: P.args, **kwargs: P.kwargs) -> R:
            with AgentTraceSpan(
                    name=name,
                    span_type=span_type,
                    input_payload={"args": args, "kwargs": kwargs},
                    attributes=attributes,
            ) as span:
                result = func(*args, **kwargs)
                span.finish(output_payload=result)
                return result

        return _sync_wrapper

    return _decorate
