"""
管理助手流式输出编排层（SSE transport/orchestration）。

这个文件只负责“如何把事件流式推给前端”，包括：
1. workflow 事件消费（messages / values / emitted / error / done）；
2. 事件到 SSE 协议的封包与输出；
3. 结束收尾、尾事件 drain、fallback 文本输出。

这个文件不负责“业务节点如何执行”与“工具如何调用”，例如：
- LLM invoke/tool 调用循环；
- tool 参数编排与工具执行失败重试；
- 具体业务状态字段如何产生。

放置建议：
- 需要复用 SSE 输出流程、事件分发、收尾逻辑的代码放这里；
- 需要做模型调用、工具调用、业务执行策略的代码不要放这里。
"""

import asyncio
import inspect
import json
from contextlib import suppress
from dataclasses import dataclass, field
from typing import Any, AsyncIterable, Awaitable, Callable

from fastapi.concurrency import run_in_threadpool
from fastapi.responses import StreamingResponse
from loguru import logger

from app.core.agent.agent_event_bus import (
    drain_final_sse_responses,
    reset_final_response_queue,
    reset_status_emitter,
    set_final_response_queue,
    set_status_emitter,
)
from app.core.agent.agent_tool_trace import extract_text
from app.core.agent.thinking_redaction import (
    ThinkingRedactionState,
    consume_thinking_text_chunk,
    flush_thinking_text,
)
from app.schemas.assistant_run import AssistantRunStatus
from app.schemas.sse_response import Action, AssistantResponse, Card, Content, MessageType

StreamEvent = tuple[str, Any]
GraphEventPayload = tuple[str, Any]
InitialEmittedEvent = AssistantResponse | dict[str, Any]
OnAnswerCompletedCallback = Callable[
    [str, str | None, list[dict[str, Any]] | None, AssistantRunStatus],
    None | Awaitable[None],
]
BuildInterruptResponsesCallback = Callable[[dict[str, Any]], list[AssistantResponse]]

EVENT_EMITTED = "emitted"
EVENT_GRAPH = "graph"
EVENT_ERROR = "error"
EVENT_DONE = "done"

GRAPH_MODE_MESSAGES = "messages"
GRAPH_MODE_VALUES = "values"


@dataclass(frozen=True)
class AssistantStreamConfig:
    """
    助手流式输出的可配置参数。

    这是对外暴露的配置对象，路由层只需要提供业务相关回调，
    通用流式引擎会根据这些回调完成事件消费、SSE 封包和收尾处理。

    Attributes:
        workflow: 具体的 workflow 对象（通常是 LangGraph 编译后的 graph）。
        build_initial_state: 根据用户问题构造初始状态的函数。
        extract_final_content: 当没有 token 流输出时，从最终状态提取兜底文本。
        should_stream_token: 判定某个 graph 节点 token 是否应输出给前端。
        build_stream_config: 生成 astream 调用配置（可返回 None 表示不传 config）。
        invoke_sync: 无 astream 能力时的同步执行入口（通常内部调用 graph.invoke）。
        map_exception: 将异常映射为前端可读错误文案的函数。
        initial_emitted_events: 流开始前先注入的事件（用于会话创建成功等前置通知）。
        hide_node_types: 对哪些事件类型隐藏 `node` 字段，默认隐藏 function_call。
        stream_modes: astream 订阅模式，默认 messages + values。
        response_headers: StreamingResponse 的响应头，默认包含禁缓存和禁代理缓冲。
        is_cancel_requested: 可选取消检查函数，返回 True 时按 cancelled 收尾。
        cancel_check_interval_ms: 取消检查轮询间隔（毫秒）。
    """

    # 流式执行主体，决定事件从哪里产出。
    workflow: Any
    # 构造 workflow 初始状态：输入是问题字符串，输出是状态字典。
    build_initial_state: Callable[[str], Any]
    # 当没有 token 输出时，从最终状态提取用户可见文本。
    extract_final_content: Callable[[dict[str, Any]], str]
    # token 级过滤器：控制哪些节点 token 可以流向前端。
    should_stream_token: Callable[[str | None, dict[str, Any]], bool]
    # 生成 astream 的 config 参数；为 None 或返回 None 时不传。
    build_stream_config: Callable[[], dict | None] | None
    # 回退执行器：无 astream 时通过该函数同步执行 workflow。
    invoke_sync: Callable[[Any], dict[str, Any]]
    # 异常映射器：把内部异常转成统一错误文案。
    map_exception: Callable[[Exception], str]
    # 可选收尾回调：在流结束时回调完整 answer/thinking 文本、最终卡片与运行终态。
    on_answer_completed: OnAnswerCompletedCallback | None = None
    # 流开始前注入的事件列表（例如会话创建成功事件），会按给定顺序输出。
    initial_emitted_events: tuple[InitialEmittedEvent, ...] = field(
        default_factory=tuple
    )
    # 这些事件类型会隐藏 content.node，避免暴露内部节点标识。
    hide_node_types: set[MessageType] = field(
        default_factory=lambda: {MessageType.FUNCTION_CALL}
    )
    # astream 的 stream_mode 配置，决定消费哪些事件通道。
    stream_modes: tuple[str, ...] = ("messages", "values")
    # SSE 响应头配置，默认关闭缓存并关闭反向代理缓冲。
    response_headers: dict[str, str] = field(
        default_factory=lambda: {
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
        }
    )
    # 可选取消检查器；返回 True 时主流会在最近安全边界停止。
    is_cancel_requested: Callable[[], bool] | None = None
    # 取消轮询间隔，避免 attach/后台 run 在无新事件时无限阻塞。
    cancel_check_interval_ms: int = 200
    # 可选中断响应构造器；当 graph values 中出现 `__interrupt__` 时，用于转换为前端响应。
    build_interrupt_responses: BuildInterruptResponsesCallback | None = None


@dataclass
class StreamRuntimeState:
    """
    流式会话运行时状态。

    Attributes:
        latest_state: 最近一次收到的 graph state（通常来自 values 事件）。
        has_streamed_output: 是否已经输出过 token 级 answer。
        has_emitted_error: 是否已经输出过错误 answer。
        thinking_redaction_state: thinking 文本脱敏运行态。
    """

    latest_state: dict[str, Any]
    has_streamed_output: bool = False
    has_emitted_error: bool = False
    aggregated_answer_parts: list[str] = field(default_factory=list)
    aggregated_answer_text: str = ""
    aggregated_thinking_parts: list[str] = field(default_factory=list)
    aggregated_thinking_text: str = ""
    thinking_redaction_state: ThinkingRedactionState = field(default_factory=ThinkingRedactionState)
    active_tool_calls: int = 0
    interrupt_cards: list[dict[str, Any]] = field(default_factory=list)


@dataclass
class EventProcessResult:
    """
    单次事件处理结果。

    Attributes:
        rendered_responses: 当前事件产生的标准响应列表。
        should_break: 主循环是否应在当前事件后进入 done 收尾流程。
    """

    rendered_responses: list[AssistantResponse] = field(default_factory=list)
    should_break: bool = False


def serialize_sse(payload: AssistantResponse) -> str:
    """
    将 AssistantResponse 序列化为 SSE 行文本。

    说明：
    - 采用 `exclude_none=True`，避免输出无意义空字段，减少前端解析噪音。
    - 统一使用 `data: <json>\\n\\n` 格式，符合标准 SSE 协议。
    """

    return (
        f"data: {json.dumps(payload.model_dump(mode='json', exclude_none=True), ensure_ascii=False)}\n\n"
    )


def build_answer_response(
        text: str,
        is_end: bool,
        *,
        state: str | None = None,
        message: str | None = None,
        meta: dict[str, Any] | None = None,
) -> AssistantResponse:
    """
    构造 answer 类型的标准响应对象。

    Args:
        text: 本次要输出的文本片段。
        is_end: 是否为流式结束包。
        state: 可选状态字段。
        message: 可选状态文案。
        meta: 可选元数据。
    """

    return AssistantResponse(
        content=Content(
            text=text,
            state=state,
            message=message,
        ),
        type=MessageType.ANSWER,
        is_end=is_end,
        meta=meta,
    )


def build_answer_sse(text: str, is_end: bool) -> str:
    """
    构造 answer 类型的 SSE 文本。

    Args:
        text: 本次要输出的文本片段。
        is_end: 是否为流式结束包。
    """

    return serialize_sse(build_answer_response(text, is_end))


def _resolve_message_type(raw_type: Any) -> MessageType:
    """
    将输入事件类型解析为 MessageType。

    非法值会回退为 `status`，保证通用流式层具备容错能力，
    不因上游事件类型不规范而中断主流程。
    """

    if isinstance(raw_type, MessageType):
        return raw_type

    normalized_type = str(raw_type or MessageType.STATUS.value)
    try:
        return MessageType(normalized_type)
    except ValueError:
        return MessageType.STATUS


def _resolve_timestamp(raw_timestamp: Any) -> int | None:
    """解析输入时间戳，非法值回退为 None（由模型默认值填充）。"""

    if isinstance(raw_timestamp, int):
        return raw_timestamp
    return None


def _resolve_meta(raw_meta: Any) -> dict[str, Any] | None:
    """解析输入的 meta 字段，仅接受字典类型。"""

    if isinstance(raw_meta, dict):
        return raw_meta
    return None


def _resolve_action(raw_action: Any) -> Action | None:
    """解析输入的 action 字段，仅接受合法动作对象。"""

    if isinstance(raw_action, Action):
        return raw_action
    if isinstance(raw_action, dict):
        try:
            return Action.model_validate(raw_action)
        except Exception:
            return None
    return None


def _resolve_card(raw_card: Any) -> Card | None:
    """解析输入的 card 字段，仅接受合法卡片对象。"""

    if isinstance(raw_card, Card):
        return raw_card
    if isinstance(raw_card, dict):
        try:
            return Card.model_validate(raw_card)
        except Exception:
            return None
    return None


def build_emitted_response(
        event_payload: dict[str, Any], hide_node_types: set[MessageType]
) -> AssistantResponse | None:
    """
    将状态发射器事件（兼容旧信封与 AssistantResponse 风格）转换为 AssistantResponse。

    Args:
        event_payload: 发射器推送的原始事件。
        hide_node_types: 需要隐藏 `node` 字段的事件类型集合。

    Returns:
        AssistantResponse | None: 可解析时返回标准响应模型；结构不合法时返回 None 并忽略。

    说明：
    - `function_call` 等事件按配置隐藏 `node`，避免把“工具节点内部标识”暴露给前端。
    - 如果事件结构不合法（例如 content 不是字典），选择忽略而非抛错，
      保障主流式链路稳定。
    - 自定义发射事件无论传入何种 `is_end`，都会在这里强制归一为 False，
      保证流结束包仅由收尾流程输出一次。
    """

    if not isinstance(event_payload, dict):
        return None

    content = event_payload.get("content")
    if not isinstance(content, dict):
        return None

    message_type = _resolve_message_type(event_payload.get("type"))

    node = content.get("node")
    if message_type in hide_node_types:
        node = None

    payload_kwargs: dict[str, Any] = {}
    resolved_timestamp = _resolve_timestamp(event_payload.get("timestamp"))
    if resolved_timestamp is not None:
        payload_kwargs["timestamp"] = resolved_timestamp

    # 优先读取新字段 `meta`，兼容读取旧字段 `extra`。
    raw_meta = event_payload.get("meta")
    if raw_meta is None:
        raw_meta = event_payload.get("extra")
    resolved_meta = _resolve_meta(raw_meta)
    if resolved_meta is not None:
        payload_kwargs["meta"] = resolved_meta
    resolved_action = _resolve_action(event_payload.get("action"))
    if resolved_action is not None:
        payload_kwargs["action"] = resolved_action
    resolved_card = _resolve_card(event_payload.get("card"))
    if resolved_card is not None:
        payload_kwargs["card"] = resolved_card

    return AssistantResponse(
        content=Content(
            text=content.get("text"),
            node=node,
            parent_node=content.get("parent_node"),
            state=content.get("state"),
            message=content.get("message"),
            result=content.get("result"),
            name=content.get("name"),
            arguments=content.get("arguments"),
        ),
        type=message_type,
        is_end=False,
        **payload_kwargs,
    )


def build_emitted_sse(
        event_payload: dict[str, Any], hide_node_types: set[MessageType]
) -> str | None:
    """将状态发射器事件转换为 SSE 文本。"""

    payload = build_emitted_response(event_payload, hide_node_types)
    if payload is None:
        return None
    return serialize_sse(payload)


def _normalize_initial_event_payload(event: InitialEmittedEvent) -> dict[str, Any] | None:
    """
    归一化预注入事件负载。

    支持两种输入：
    - AssistantResponse：按统一 JSON 结构导出；
    - dict：直接作为 emitted payload 使用。
    """

    if isinstance(event, AssistantResponse):
        return event.model_dump(mode="json", exclude_none=True)
    if isinstance(event, dict):
        return event
    return None


def _append_answer_text(runtime_state: StreamRuntimeState, text: str) -> str:
    """
    向聚合答案缓冲区追加文本，并对“全量快照重复”做增量裁剪。

    说明：
    - 某些模型/链路会在 messages 事件中重复返回“截至当前的全量文本”，
      若直接拼接会出现同一段内容重复多次；
    - 当新文本以前缀方式包含了已聚合文本时，仅追加新增的 delta。

    Args:
        runtime_state: 流式运行时状态。
        text: 待追加的文本片段。

    Returns:
        str: 实际新增并可对外输出的文本；若无新增则返回空字符串。
    """

    raw_text = str(text or "")
    if not raw_text:
        return ""

    delta_text = raw_text
    existing_text = runtime_state.aggregated_answer_text
    if existing_text and raw_text.startswith(existing_text):
        delta_text = raw_text[len(existing_text):]

    if not delta_text:
        return ""

    runtime_state.aggregated_answer_parts.append(delta_text)
    runtime_state.aggregated_answer_text += delta_text
    return delta_text


def _append_thinking_text(runtime_state: StreamRuntimeState, text: str) -> str:
    """
    向聚合思考缓冲区追加文本，并对“全量快照重复”做增量裁剪。

    Args:
        runtime_state: 流式运行时状态。
        text: 待追加的思考文本片段。

    Returns:
        str: 实际新增的文本；若无新增则返回空字符串。
    """

    raw_text = str(text or "")
    if not raw_text:
        return ""

    delta_text = raw_text
    existing_text = runtime_state.aggregated_thinking_text
    if existing_text and raw_text.startswith(existing_text):
        delta_text = raw_text[len(existing_text):]

    if not delta_text:
        return ""

    runtime_state.aggregated_thinking_parts.append(delta_text)
    runtime_state.aggregated_thinking_text += delta_text
    return delta_text


def _redact_thinking_response(
        *,
        runtime_state: StreamRuntimeState,
        response: AssistantResponse,
) -> AssistantResponse | None:
    """
    对 thinking 响应执行增量工具名脱敏。

    Args:
        runtime_state: 流式运行时状态。
        response: 原始 thinking 响应。

    Returns:
        AssistantResponse | None:
            - 返回脱敏后的响应对象；
            - 当本次分片仅包含尚未闭合的英文标识符缓存时返回 `None`。
    """

    if (
            response.type != MessageType.THINKING
            or not isinstance(response.content.text, str)
            or not response.content.text
    ):
        return response

    redacted_text = consume_thinking_text_chunk(
        state=runtime_state.thinking_redaction_state,
        text=response.content.text,
    )
    if not redacted_text:
        return None

    return response.model_copy(
        update={
            "content": response.content.model_copy(update={"text": redacted_text}),
        }
    )


def _append_interrupt_card(
        runtime_state: StreamRuntimeState,
        response: AssistantResponse,
) -> None:
    """
    将即时发出的可持久化卡片写入运行态缓存。

    Args:
        runtime_state: 流式运行时状态。
        response: 当前已标准化的卡片响应。
    """

    if response.type != MessageType.CARD or response.card is None:
        return
    meta = response.meta if isinstance(response.meta, dict) else {}
    if meta.get("persist_card") is not True:
        return
    card_uuid = str(meta.get("card_uuid") or "").strip()
    if not card_uuid:
        return
    if any(item["id"] == card_uuid for item in runtime_state.interrupt_cards):
        return
    runtime_state.interrupt_cards.append(
        {
            "id": card_uuid,
            "type": response.card.type,
            "data": dict(response.card.data),
        }
    )


def _normalize_response_for_runtime(
        *,
        runtime_state: StreamRuntimeState,
        response: AssistantResponse,
) -> AssistantResponse | None:
    """
    统一处理 emitted/interrupt 生成的 AssistantResponse，并同步更新运行态缓存。

    Args:
        runtime_state: 流式运行时状态。
        response: 原始响应对象。

    Returns:
        AssistantResponse | None: 归一化后的响应；无新增内容时返回 `None`。
    """

    normalized_response = _redact_thinking_response(
        runtime_state=runtime_state,
        response=response,
    )
    if normalized_response is None:
        return None
    if (
            normalized_response.type == MessageType.ANSWER
            and isinstance(normalized_response.content.text, str)
            and normalized_response.content.text
    ):
        delta_text = _append_answer_text(runtime_state, normalized_response.content.text)
        if not delta_text:
            return None
        runtime_state.has_streamed_output = True
        normalized_response = normalized_response.model_copy(
            update={
                "content": normalized_response.content.model_copy(update={"text": delta_text}),
            }
        )
    elif (
            normalized_response.type == MessageType.THINKING
            and isinstance(normalized_response.content.text, str)
            and normalized_response.content.text
    ):
        delta_thinking = _append_thinking_text(runtime_state, normalized_response.content.text)
        if not delta_thinking:
            return None
        normalized_response = normalized_response.model_copy(
            update={
                "content": normalized_response.content.model_copy(update={"text": delta_thinking}),
            }
        )

    _append_interrupt_card(runtime_state, normalized_response)
    return normalized_response


def _flush_pending_thinking_response(
        *,
        runtime_state: StreamRuntimeState,
) -> AssistantResponse | None:
    """
    在流式结束前刷新 thinking 脱敏缓冲区。

    Args:
        runtime_state: 当前流式运行时状态。

    Returns:
        AssistantResponse | None: 若存在待刷新的 thinking 文本则返回响应对象，否则返回 `None`。
    """

    flushed_text = flush_thinking_text(state=runtime_state.thinking_redaction_state)
    if not flushed_text:
        return None

    delta_thinking = _append_thinking_text(runtime_state, flushed_text)
    if not delta_thinking:
        return None

    return AssistantResponse(
        type=MessageType.THINKING,
        content=Content(text=delta_thinking),
    )


def _update_tool_call_depth(runtime_state: StreamRuntimeState, payload: Any) -> None:
    """
    根据 emitted 的 function_call 事件维护工具调用深度计数。

    Args:
        runtime_state: 流式运行时状态。
        payload: emitted 事件原始负载。

    Returns:
        None
    """

    if not isinstance(payload, dict):
        return
    if str(payload.get("type") or "").strip() != MessageType.FUNCTION_CALL.value:
        return

    content = payload.get("content")
    if not isinstance(content, dict):
        return

    state = str(content.get("state") or "").strip()
    if state == "start":
        runtime_state.active_tool_calls += 1
        return
    if state == "end":
        runtime_state.active_tool_calls = max(0, runtime_state.active_tool_calls - 1)


def handle_graph_message_chunk(
        *,
        chunk: Any,
        runtime_state: StreamRuntimeState,
        should_stream_token: Callable[[str | None, dict[str, Any]], bool],
) -> EventProcessResult:
    """
    处理 graph `messages` 事件。

    Args:
        chunk: graph 透传的 `(message_chunk, metadata)` 二元组。
        runtime_state: 当前运行时状态（用于读取 latest_state 并更新输出标记）。
        should_stream_token: 业务侧 token 输出判定函数。

    Returns:
        EventProcessResult: 包含本次事件生成的 SSE 文本（可能为空）。
    """

    result = EventProcessResult()

    if not isinstance(chunk, tuple) or len(chunk) != 2:
        return result

    message_chunk, metadata = chunk
    stream_node: str | None = None
    if isinstance(metadata, dict):
        stream_node = metadata.get("langgraph_node")

    should_emit = should_stream_token(stream_node, runtime_state.latest_state)
    if not should_emit:
        return result

    # 工具执行期间会触发工具内部 LLM 调用，这些 token 不应直接对前端输出，
    # 否则会与最终 supervisor 汇总结果重复。
    if runtime_state.active_tool_calls > 0:
        return result

    token_text = extract_text(message_chunk)
    if not token_text:
        return result

    delta_text = _append_answer_text(runtime_state, token_text)
    if not delta_text:
        return result

    runtime_state.has_streamed_output = True
    result.rendered_responses.append(build_answer_response(delta_text, False))
    return result


def _process_graph_values_event(chunk: Any, runtime_state: StreamRuntimeState) -> None:
    """
    处理 graph `values` 事件。

    values 事件用于更新最新状态快照，不直接输出给前端。
    """

    if isinstance(chunk, dict):
        merged_state = dict(runtime_state.latest_state or {})
        merged_state.update(chunk)
        runtime_state.latest_state = merged_state


def _process_graph_event(
        payload: GraphEventPayload,
        runtime_state: StreamRuntimeState,
        should_stream_token: Callable[[str | None, dict[str, Any]], bool],
) -> EventProcessResult:
    """
    处理 graph 事件分支，并按 mode 分发到 message/values 处理器。
    """

    result = EventProcessResult()
    if not isinstance(payload, tuple) or len(payload) != 2:
        return result

    mode, chunk = payload
    if mode == GRAPH_MODE_MESSAGES:
        return handle_graph_message_chunk(
            chunk=chunk,
            runtime_state=runtime_state,
            should_stream_token=should_stream_token,
        )
    if mode == GRAPH_MODE_VALUES:
        _process_graph_values_event(chunk, runtime_state)
    return result


def _process_stream_event(
        *,
        event_type: str,
        payload: Any,
        runtime_state: StreamRuntimeState,
        should_stream_token: Callable[[str | None, dict[str, Any]], bool],
        hide_node_types: set[MessageType],
        map_exception: Callable[[Exception], str],
) -> EventProcessResult:
    """
    统一处理单个队列事件。

    该函数是主循环和 drain 流程共享的分发入口，
    用于避免两处重复写 `emitted/graph/error` 分支。
    """

    if event_type == EVENT_EMITTED:
        _update_tool_call_depth(runtime_state, payload)
        emitted_response = build_emitted_response(payload, hide_node_types)
        result = EventProcessResult()
        if emitted_response is None:
            return result

        normalized_response = _normalize_response_for_runtime(
            runtime_state=runtime_state,
            response=emitted_response,
        )
        if normalized_response is None:
            return result

        result.rendered_responses.append(normalized_response)
        return result

    if event_type == EVENT_GRAPH:
        return _process_graph_event(payload, runtime_state, should_stream_token)

    if event_type == EVENT_ERROR:
        runtime_state.has_emitted_error = True
        result = EventProcessResult()
        message = map_exception(payload)
        delta_text = _append_answer_text(runtime_state, message)
        if delta_text:
            result.rendered_responses.append(build_answer_response(delta_text, False))
        return result

    if event_type == EVENT_DONE:
        return EventProcessResult(should_break=True)

    return EventProcessResult()


async def drain_pending_events(
        *,
        queue: asyncio.Queue[StreamEvent],
        runtime_state: StreamRuntimeState,
        should_stream_token: Callable[[str | None, dict[str, Any]], bool],
        hide_node_types: set[MessageType],
        map_exception: Callable[[Exception], str],
) -> EventProcessResult:
    """
    在 `done` 事件后，尽可能消费并输出队列里的尾部事件。

    说明：
    - 该函数只负责“清空尾部队列并生成可输出事件”，不决定主流程是否结束。
    - 运行时状态（latest_state、错误标记、token 标记）通过 `runtime_state` 原地更新。
    """

    result = EventProcessResult()

    while True:
        try:
            pending_type, pending_payload = queue.get_nowait()
        except asyncio.QueueEmpty:
            break

        pending_result = _process_stream_event(
            event_type=pending_type,
            payload=pending_payload,
            runtime_state=runtime_state,
            should_stream_token=should_stream_token,
            hide_node_types=hide_node_types,
            map_exception=map_exception,
        )
        for rendered_item in pending_result.rendered_responses:
            result.rendered_responses.append(rendered_item)

    return result


def _build_stream_kwargs(config: AssistantStreamConfig) -> dict[str, Any]:
    """
    组装 workflow `astream` 所需参数。

    stream_mode 固定来源于配置；
    config.build_stream_config 返回值为空时不注入，保持调用参数干净。
    """

    stream_kwargs: dict[str, Any] = {"stream_mode": list(config.stream_modes)}
    if config.build_stream_config is not None:
        stream_config = config.build_stream_config()
        if stream_config:
            stream_kwargs["config"] = stream_config
    return stream_kwargs


async def _produce_workflow_events(
        *,
        queue: asyncio.Queue[StreamEvent],
        state: dict[str, Any],
        runtime_state: StreamRuntimeState,
        config: AssistantStreamConfig,
) -> None:
    """
    生产 workflow 事件并写入队列。

    流程：
    1. 优先走 `workflow.astream`（messages + values）
    2. 若无 astream 则走同步 invoke 回退
    3. 任意异常统一写入 `error` 事件
    4. 最终一定写入 `done`，驱动消费方进入收尾流程
    """

    try:
        if hasattr(config.workflow, "astream"):
            stream_kwargs = _build_stream_kwargs(config)
            async for mode, chunk in config.workflow.astream(state, **stream_kwargs):
                await queue.put((EVENT_GRAPH, (mode, chunk)))
        else:
            runtime_state.latest_state = await run_in_threadpool(config.invoke_sync, state)
    except asyncio.CancelledError:
        raise
    except Exception as exc:
        logger.opt(exception=exc).error("Assistant workflow execution failed")
        await queue.put((EVENT_ERROR, exc))
    finally:
        await queue.put((EVENT_DONE, None))


async def _finalize_stream(
        emitter_token: Any,
        producer_task: asyncio.Task[Any],
        *,
        finish_status: AssistantRunStatus,
) -> AssistantResponse:
    """
    统一收尾逻辑：重置 emitter、取消后台任务、输出结束包。

    为什么必须输出结束包：
    - 前端依赖 `is_end=true` 作为流完成信号；
    - 即使中途报错，也要给出可确定结束点，避免客户端悬挂等待。
    """

    reset_status_emitter(emitter_token)
    if not producer_task.done():
        producer_task.cancel()
        with suppress(asyncio.CancelledError):
            await producer_task
    return build_answer_response(
        "",
        True,
        state=finish_status.value,
        message=(
            "已停止生成"
            if finish_status == AssistantRunStatus.CANCELLED
            else None
        ),
        meta={"run_status": finish_status.value},
    )


def _drain_final_sse_responses() -> list[AssistantResponse]:
    """
    取出所有最终 SSE 响应对象。

    用途：
    - 在流式主体输出完成后，统一获取“流尾响应队列”中的内容；
    - 保留事件总线已经排好的最终发送顺序；
    - 为后续 SSE 序列化和历史卡片持久化提供同一份源数据。

    Returns:
        list[AssistantResponse]: 按最终发送顺序返回的响应对象列表。
    """

    return drain_final_sse_responses()


def _extract_persistable_cards(
        final_responses: list[AssistantResponse],
) -> list[dict[str, Any]] | None:
    """
    从最终 SSE 响应中提取可落库的卡片。

    仅提取 `type=card` 的响应，且要求：
    - 存在合法 `card_uuid`；
    - `meta.persist_card is True`。
    action 等其他最终响应不会进入消息持久化结构。

    Args:
        final_responses: 流尾阶段准备发送给前端的最终 SSE 响应列表。

    Returns:
        list[dict[str, Any]] | None:
            可持久化的卡片列表，结构固定为 `id + type + data`；
            若不存在合法卡片则返回 `None`。
    """

    cards: list[dict[str, Any]] = []
    for response in final_responses:
        if response.type != MessageType.CARD or response.card is None:
            continue

        meta = response.meta if isinstance(response.meta, dict) else {}
        if meta.get("persist_card") is not True:
            continue

        card_id = str(meta.get("card_uuid") or "").strip()
        if not card_id:
            continue

        cards.append(
            {
                "id": card_id,
                "type": response.card.type,
                "data": dict(response.card.data),
            }
        )

    return cards or None


def _merge_persistable_cards(
        emitted_cards: list[dict[str, Any]],
        final_cards: list[dict[str, Any]] | None,
) -> list[dict[str, Any]] | None:
    """
    合并即时 emitted 卡片与流尾最终卡片，并按 card_uuid 去重。

    Args:
        emitted_cards: 即时发出的可持久化卡片列表。
        final_cards: 流尾阶段提取出的可持久化卡片列表。

    Returns:
        list[dict[str, Any]] | None: 去重后的卡片列表；为空时返回 `None`。
    """

    merged_cards: list[dict[str, Any]] = []
    seen_card_ids: set[str] = set()
    for source_cards in (emitted_cards, final_cards or []):
        for item in source_cards:
            card_id = str(item.get("id") or "").strip()
            if not card_id or card_id in seen_card_ids:
                continue
            seen_card_ids.add(card_id)
            merged_cards.append(dict(item))
    return merged_cards or None


async def _invoke_answer_completed_callback(
        callback: OnAnswerCompletedCallback | None,
        answer_text: str,
        thinking_text: str,
        final_cards: list[dict[str, Any]] | None,
        finish_status: AssistantRunStatus,
) -> None:
    """
    执行“回答完成”回调。

    允许回调为同步函数或异步函数；回调异常会被调用方兜底，不影响主流输出。

    Args:
        callback: 结束回调函数。
        answer_text: 聚合后的完整回答文本。
        thinking_text: 聚合后的完整思考文本。
        final_cards: 流尾阶段已过滤出的可持久化卡片列表。
        finish_status: 当前运行的最终状态。
    """

    if callback is None:
        return

    normalized_thinking_text = thinking_text.strip() or None
    callback_result = callback(
        answer_text,
        normalized_thinking_text,
        final_cards,
        finish_status,
    )
    if inspect.isawaitable(callback_result):
        await callback_result


async def iterate_assistant_responses(
        *,
        question: str,
        config: AssistantStreamConfig,
) -> AsyncIterable[AssistantResponse]:
    """
    核心响应流生成器。

    这是通用流式引擎的主循环：
    - 从 workflow 与状态发射器接收事件
    - 按事件类型分发处理
    - 在 done 后 drain 尾事件
    - 必要时输出 fallback answer
    - 最终输出结束包
    """

    state = config.build_initial_state(question)
    runtime_state = StreamRuntimeState(latest_state=state if isinstance(state, dict) else {})
    final_responses: list[AssistantResponse] = []
    finish_status = AssistantRunStatus.SUCCESS

    queue: asyncio.Queue[StreamEvent] = asyncio.Queue()
    loop = asyncio.get_running_loop()

    def _event_emitter(event: dict[str, Any]) -> None:
        loop.call_soon_threadsafe(queue.put_nowait, (EVENT_EMITTED, event))

    emitter_token = set_status_emitter(_event_emitter)
    final_response_queue_token = set_final_response_queue()

    # 先把前置事件写入队列，确保“会话创建成功”等通知优先于图执行事件输出。
    for initial_event in config.initial_emitted_events:
        initial_payload = _normalize_initial_event_payload(initial_event)
        if initial_payload is not None:
            queue.put_nowait((EVENT_EMITTED, initial_payload))

    producer_task = asyncio.create_task(
        _produce_workflow_events(
            queue=queue,
            state=state,
            runtime_state=runtime_state,
            config=config,
        )
    )

    try:
        while True:
            if config.is_cancel_requested is not None and config.is_cancel_requested():
                finish_status = AssistantRunStatus.CANCELLED
                if not producer_task.done():
                    producer_task.cancel()
                    with suppress(asyncio.CancelledError):
                        await producer_task
                await asyncio.sleep(0)
                drained_result = await drain_pending_events(
                    queue=queue,
                    runtime_state=runtime_state,
                    should_stream_token=config.should_stream_token,
                    hide_node_types=config.hide_node_types,
                    map_exception=config.map_exception,
                )
                for drained_item in drained_result.rendered_responses:
                    yield drained_item
                break

            wait_timeout_seconds = None
            if config.is_cancel_requested is not None:
                wait_timeout_seconds = max(config.cancel_check_interval_ms, 1) / 1000
            try:
                if wait_timeout_seconds is None:
                    event_type, payload = await queue.get()
                else:
                    event_type, payload = await asyncio.wait_for(
                        queue.get(),
                        timeout=wait_timeout_seconds,
                    )
            except asyncio.TimeoutError:
                continue

            event_result = _process_stream_event(
                event_type=event_type,
                payload=payload,
                runtime_state=runtime_state,
                should_stream_token=config.should_stream_token,
                hide_node_types=config.hide_node_types,
                map_exception=config.map_exception,
            )
            if event_type == EVENT_ERROR:
                finish_status = AssistantRunStatus.ERROR

            for rendered_item in event_result.rendered_responses:
                yield rendered_item

            if event_result.should_break:
                # 这里先让出一次调度，再 drain 队列，是为了捕获 done 前后竞态写入的尾事件。
                await asyncio.sleep(0)
                drained_result = await drain_pending_events(
                    queue=queue,
                    runtime_state=runtime_state,
                    should_stream_token=config.should_stream_token,
                    hide_node_types=config.hide_node_types,
                    map_exception=config.map_exception,
                )
                for drained_item in drained_result.rendered_responses:
                    yield drained_item
                break

        flushed_thinking_response = _flush_pending_thinking_response(
            runtime_state=runtime_state,
        )
        if flushed_thinking_response is not None:
            yield flushed_thinking_response

        interrupt_responses: list[AssistantResponse] = []
        if (
                finish_status == AssistantRunStatus.SUCCESS
                and config.build_interrupt_responses is not None
                and isinstance(runtime_state.latest_state, dict)
        ):
            interrupt_responses = list(
                config.build_interrupt_responses(runtime_state.latest_state) or []
            )
            if interrupt_responses:
                finish_status = AssistantRunStatus.WAITING_INPUT
                for interrupt_response in interrupt_responses:
                    normalized_response = _normalize_response_for_runtime(
                        runtime_state=runtime_state,
                        response=interrupt_response,
                    )
                    if normalized_response is not None:
                        yield normalized_response

        # 当没有 token 输出且没有错误时，回退到业务侧最终内容提取。
        # 若业务侧返回空字符串，则视为“无兜底内容”，不输出额外 answer 包。
        if (
                finish_status == AssistantRunStatus.SUCCESS
                and not runtime_state.has_emitted_error
                and not runtime_state.has_streamed_output
        ):
            fallback_text = config.extract_final_content(runtime_state.latest_state)
            if isinstance(fallback_text, str) and fallback_text:
                delta_text = _append_answer_text(runtime_state, fallback_text)
                if delta_text:
                    yield build_answer_response(delta_text, False)

        if finish_status not in {AssistantRunStatus.CANCELLED, AssistantRunStatus.WAITING_INPUT}:
            final_responses = _drain_final_sse_responses()
            for final_response in final_responses:
                yield final_response
    finally:
        try:
            await _invoke_answer_completed_callback(
                config.on_answer_completed,
                runtime_state.aggregated_answer_text,
                runtime_state.aggregated_thinking_text,
                _merge_persistable_cards(
                    runtime_state.interrupt_cards,
                    _extract_persistable_cards(final_responses),
                ),
                finish_status,
            )
        except Exception as exc:  # pragma: no cover - 防御性兜底
            logger.opt(exception=exc).warning("Assistant stream finalize callback failed")
        reset_final_response_queue(final_response_queue_token)
        end_event = await _finalize_stream(
            emitter_token,
            producer_task,
            finish_status=finish_status,
        )
        yield end_event


async def _event_stream(
        *,
        question: str,
        config: AssistantStreamConfig,
) -> AsyncIterable[str]:
    """
    核心 SSE 文本流生成器。

    Args:
        question: 用户问题文本。
        config: 助手流式配置。

    Returns:
        AsyncIterable[str]: 标准 SSE 文本迭代器。
    """

    async for payload in iterate_assistant_responses(
            question=question,
            config=config,
    ):
        yield serialize_sse(payload)


def create_streaming_response(
        question: str, config: AssistantStreamConfig
) -> StreamingResponse:
    """
    对外统一入口：创建 FastAPI StreamingResponse。

    业务路由仅需组装 `AssistantStreamConfig` 并传入问题文本，
    无需关心队列管理、状态事件透传和收尾细节。
    """

    return StreamingResponse(
        _event_stream(question=question, config=config),
        media_type="text/event-stream",
        headers=config.response_headers,
    )
