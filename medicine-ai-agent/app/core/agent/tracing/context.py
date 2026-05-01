from __future__ import annotations

import datetime
import threading
import time
from contextvars import ContextVar, Token
from dataclasses import dataclass, field
from typing import Any

from app.core.agent.tracing.ids import generate_span_id, generate_trace_id
from app.core.agent.tracing.time_utils import utc_now


@dataclass
class AgentTraceState:
    """
    功能描述：
        当前 Agent Trace 的运行时上下文。

    参数说明：
        trace_id (str): 当前 trace ID。
        root_span_id (str): 根 graph span ID。
        graph_name (str): 当前 graph 名称。
        conversation_uuid (str): 会话 UUID。
        assistant_message_uuid (str): 当前 AI 消息 UUID。
        user_id (int | None): 当前用户 ID。
        conversation_type (str): 会话类型。
        entrypoint (str): trace 入口标识。
        span_stack (list[str]): 当前 span 父子栈。
        root_sequence (int): 根 span 固定顺序号。
        sequence (int): 当前 trace 内 span 顺序号。
        input_tokens (int): 累计输入 token。
        output_tokens (int): 累计输出 token。
        total_tokens (int): 累计总 token。
        message_view (dict | None): 顶层消息视图快照。
        span_metadata_by_id (dict[str, dict[str, Any]]): 当前 trace 内 span 运行时元数据。

    返回值：
        无（数据模型）。
    """

    trace_id: str
    root_span_id: str
    graph_name: str
    conversation_uuid: str
    assistant_message_uuid: str
    user_id: int | None
    conversation_type: str
    entrypoint: str
    started_at: datetime.datetime = field(default_factory=utc_now)
    started_monotonic: float = field(default_factory=time.monotonic)
    span_stack: list[str] = field(default_factory=list)
    root_sequence: int = 1
    sequence: int = 1
    input_tokens: int = 0
    output_tokens: int = 0
    total_tokens: int = 0
    message_view: dict | None = None
    span_metadata_by_id: dict[str, dict[str, Any]] = field(default_factory=dict)
    _lock: threading.Lock = field(default_factory=threading.Lock, repr=False)

    def next_sequence(self) -> int:
        """
        功能描述：
            生成当前 trace 内递增的 span 顺序号。

        参数说明：
            无。

        返回值：
            int: 当前 span 顺序号。
        """

        with self._lock:
            self.sequence += 1
            return self.sequence

    def add_token_usage(
            self,
            *,
            input_tokens: int = 0,
            output_tokens: int = 0,
            total_tokens: int = 0,
    ) -> None:
        """
        功能描述：
            累加模型 token 用量。

        参数说明：
            input_tokens (int): 输入 token 数。
            output_tokens (int): 输出 token 数。
            total_tokens (int): 总 token 数。

        返回值：
            None。
        """

        with self._lock:
            self.input_tokens += max(input_tokens, 0)
            self.output_tokens += max(output_tokens, 0)
            self.total_tokens += max(total_tokens, 0)

    def register_span_metadata(self, span_id: str, metadata: dict[str, Any]) -> None:
        """
        功能描述：
            登记当前 trace 内 span 的轻量运行时元数据，供后续子 span 解析父节点信息。

        参数说明：
            span_id (str): Span 唯一标识。
            metadata (dict[str, Any]): Span 元数据。

        返回值：
            None。
        """

        if not span_id:
            return
        with self._lock:
            self.span_metadata_by_id[span_id] = dict(metadata)

    def resolve_span_metadata(self, span_id: str | None) -> dict[str, Any] | None:
        """
        功能描述：
            读取当前 trace 内指定 span 的运行时元数据。

        参数说明：
            span_id (str | None): Span 唯一标识。

        返回值：
            dict[str, Any] | None: Span 元数据；不存在时返回 None。
        """

        if not span_id:
            return None
        with self._lock:
            metadata = self.span_metadata_by_id.get(span_id)
            return dict(metadata) if metadata is not None else None

    def find_nearest_span_metadata(self, span_type: str) -> dict[str, Any] | None:
        """
        功能描述：
            从当前 span 栈向上查找最近的指定类型 span 元数据。

        参数说明：
            span_type (str): 目标 span 类型。

        返回值：
            dict[str, Any] | None: 最近的 span 元数据；不存在时返回 None。
        """

        normalized_span_type = str(span_type or "").strip()
        if not normalized_span_type:
            return None
        with self._lock:
            for span_id in reversed(self.span_stack):
                metadata = self.span_metadata_by_id.get(span_id)
                if metadata and metadata.get("span_type") == normalized_span_type:
                    return dict(metadata)
        return None


_TRACE_STATE: ContextVar[AgentTraceState | None] = ContextVar(
    "agent_trace_state",
    default=None,
)
"""当前上下文中的 Agent Trace 状态。"""


def create_trace_state(
        *,
        graph_name: str,
        conversation_uuid: str,
        assistant_message_uuid: str,
        user_id: int | None,
        conversation_type: str,
        entrypoint: str,
        message_view: dict | None = None,
) -> AgentTraceState:
    """
    功能描述：
        创建新的 Agent Trace 状态。

    参数说明：
        graph_name (str): 当前 graph 名称。
        conversation_uuid (str): 会话 UUID。
        assistant_message_uuid (str): 当前 AI 消息 UUID。
        user_id (int | None): 当前用户 ID。
        conversation_type (str): 会话类型。
        entrypoint (str): trace 入口标识。
        message_view (dict | None): 顶层消息视图快照。

    返回值：
        AgentTraceState: 新创建的 trace 状态。
    """

    root_span_id = generate_span_id()
    return AgentTraceState(
        trace_id=generate_trace_id(),
        root_span_id=root_span_id,
        graph_name=graph_name,
        conversation_uuid=conversation_uuid,
        assistant_message_uuid=assistant_message_uuid,
        user_id=user_id,
        conversation_type=conversation_type,
        entrypoint=entrypoint,
        message_view=message_view,
        span_stack=[root_span_id],
    )


def bind_trace_state(state: AgentTraceState) -> Token[AgentTraceState | None]:
    """
    功能描述：
        把 trace 状态绑定到当前上下文。

    参数说明：
        state (AgentTraceState): 当前 trace 状态。

    返回值：
        Token[AgentTraceState | None]: ContextVar 重置令牌。
    """

    return _TRACE_STATE.set(state)


def reset_trace_state(token: Token[AgentTraceState | None]) -> None:
    """
    功能描述：
        重置当前上下文的 trace 状态。

    参数说明：
        token (Token[AgentTraceState | None]): bind 时返回的重置令牌。

    返回值：
        None。
    """

    _TRACE_STATE.reset(token)


def get_current_trace_state() -> AgentTraceState | None:
    """
    功能描述：
        读取当前 trace 状态。

    参数说明：
        无。

    返回值：
        AgentTraceState | None: 当前 trace 状态；未绑定时返回 None。
    """

    return _TRACE_STATE.get()


def get_current_span_id() -> str | None:
    """
    功能描述：
        读取当前 span 栈顶 ID。

    参数说明：
        无。

    返回值：
        str | None: 当前父 span ID；未绑定时返回 None。
    """

    state = get_current_trace_state()
    if state is None or not state.span_stack:
        return None
    return state.span_stack[-1]


def push_span(span_id: str) -> None:
    """
    功能描述：
        把 span 压入当前父子栈。

    参数说明：
        span_id (str): 当前 span ID。

    返回值：
        None。
    """

    state = get_current_trace_state()
    if state is None:
        return
    state.span_stack.append(span_id)


def pop_span(span_id: str) -> None:
    """
    功能描述：
        从当前父子栈弹出 span。

    参数说明：
        span_id (str): 需要弹出的 span ID。

    返回值：
        None。
    """

    state = get_current_trace_state()
    if state is None or not state.span_stack:
        return
    if state.span_stack[-1] == span_id:
        state.span_stack.pop()
