from __future__ import annotations

import asyncio
import contextvars
from dataclasses import dataclass
from typing import Any, Callable, Mapping

from langchain_core.messages import HumanMessage

from app.core.agent.agent_tool_trace import extract_text


def _run_async(coro: Any) -> Any:
    """
    在同步上下文安全执行异步协程。

    Args:
        coro: 协程对象。

    Returns:
        Any: 协程返回值。
    """

    try:
        loop = asyncio.get_running_loop()
    except RuntimeError:
        loop = None

    if loop and loop.is_running():
        import concurrent.futures

        current_context = contextvars.copy_context()
        with concurrent.futures.ThreadPoolExecutor(max_workers=1) as pool:
            return pool.submit(current_context.run, asyncio.run, coro).result()

    return asyncio.run(coro)


def _normalize_history_messages(history_messages: list[Any] | str | None) -> list[Any]:
    """
    规范化运行层输入消息。

    Args:
        history_messages: 可传入消息列表或字符串。字符串会自动包装为 `HumanMessage`。

    Returns:
        list[Any]: 标准化后的消息列表。
    """

    if isinstance(history_messages, list):
        return list(history_messages)
    if history_messages is None:
        return []
    return [HumanMessage(content=str(history_messages))]


def _resolve_messages_from_payload(payload: Any) -> list[Any]:
    """
    从 agent 调用返回值中提取消息列表。

    Args:
        payload: `agent.invoke/ainvoke` 原始返回值。

    Returns:
        list[Any]: 消息列表；无法解析时返回空列表。
    """

    if not isinstance(payload, Mapping):
        return []
    raw_messages = payload.get("messages")
    if isinstance(raw_messages, list):
        return list(raw_messages)
    return []


def _resolve_content_from_messages(messages: list[Any]) -> tuple[str, Any]:
    """
    从消息序列中提取最后一条 AI 消息文本及原始 content。

    Args:
        messages: 消息列表。

    Returns:
        tuple[str, Any]:
            - 文本内容（已 strip）；
            - 原始 content。
    """

    for message in reversed(messages):
        if str(getattr(message, "type", "") or "").lower() != "ai":
            continue
        raw_content = getattr(message, "content", None)
        return extract_text(message).strip(), raw_content
    return "", None


def _extract_reasoning_content_from_chunk(message_chunk: Any) -> str:
    """
    从 LangChain 流式消息分片中提取思考文本（阿里云/火山引擎）。

    仅兼容当前主链路真实使用的 `reasoning_content` 字段来源：
    1. 原生属性 `message_chunk.reasoning_content`；
    2. LangChain 封装位置 `message_chunk.additional_kwargs["reasoning_content"]`。

    处理规则：
    1. 自动忽略空字符串；
    2. 两个来源文本相同则去重，避免重复透传；
    3. 保留原有顺序后拼接返回。

    Args:
        message_chunk: 流式消息分片对象（通常为 AIMessageChunk）。

    Returns:
        str: 解析到的思考文本；不存在时返回空字符串。
    """

    parts: list[str] = []

    def _append_part(raw_value: Any) -> None:
        """
        追加单段思考文本（去空、去重）。

        Args:
            raw_value: 待追加的原始字段值。

        Returns:
            None
        """

        if not isinstance(raw_value, str):
            return
        text = raw_value.strip()
        if not text:
            return
        if text in parts:
            return
        parts.append(text)

    _append_part(getattr(message_chunk, "reasoning_content", None))

    additional_kwargs = getattr(message_chunk, "additional_kwargs", None)
    if isinstance(additional_kwargs, Mapping):
        _append_part(additional_kwargs.get("reasoning_content"))

    return "".join(parts)


@dataclass(frozen=True)
class AgentInvokeResult:
    """
    `agent_invoke` 的标准化返回结构。

    Attributes:
        payload: agent 原始返回值。
        messages: 从 payload 提取的消息列表。
        content: 优先最后一条 AI 文本，否则回退 payload 的 `output/text`。
        raw_content: 最后一条 AI 原始 content（若存在）。
    """

    payload: Any
    messages: list[Any]
    content: str
    raw_content: Any


def agent_invoke(
        agent_instance: Any,
        history_messages: list[Any] | str,
) -> AgentInvokeResult:
    """
    执行 agent 的 invoke 调用（优先异步 ainvoke）。

    Args:
        agent_instance: `create_agent_instance` 返回的 agent。
        history_messages: 输入消息列表。

    Returns:
        AgentInvokeResult: 标准化后的 invoke 结果。
    """

    payload = {"messages": _normalize_history_messages(history_messages)}
    ainvoke = getattr(agent_instance, "ainvoke", None)
    if callable(ainvoke):
        raw_result = _run_async(ainvoke(payload))
    else:
        raw_result = agent_instance.invoke(payload)

    messages = _resolve_messages_from_payload(raw_result)
    content, raw_content = _resolve_content_from_messages(messages)
    if not content and isinstance(raw_result, Mapping):
        content = str(raw_result.get("output") or raw_result.get("text") or "").strip()
        if raw_content is None:
            raw_content = raw_result.get("output") or raw_result.get("text")

    return AgentInvokeResult(
        payload=raw_result,
        messages=messages,
        content=content,
        raw_content=raw_content,
    )


def agent_stream(
        agent_instance: Any,
        history_messages: list[Any] | str,
        on_model_delta: Callable[[str], None] | None = None,
        on_thinking_delta: Callable[[str], None] | None = None,
) -> dict[str, Any]:
    """
    执行 agent 的 astream 调用。

    Args:
        agent_instance: `create_agent_instance` 返回的 agent。
        history_messages: 输入消息列表。
        on_model_delta: 可选回调；当模型节点产出文本分片时触发。
        on_thinking_delta: 可选回调；当模型节点产出思考分片时触发。

    Returns:
        dict[str, Any]:
            - latest_state: 最后一次 values 状态。
            - streamed_text: 全部分片拼接文本。
            - streamed_thinking: 思考分片拼接文本。
            - final_messages: 从 latest_state 解析出的最终消息列表。
    """
    normalized_history_messages = _normalize_history_messages(history_messages)

    async def _collect_stream_events() -> tuple[list[str], list[str], dict[str, Any]]:
        """
        汇总 astream 事件中的回答与思考分片。

        规则：
        1. 回答分片始终取 `extract_text(message_chunk)`；
        2. 思考分片仅在“尚未进入回答阶段”时读取 `message_chunk.reasoning_content`；
        3. 一旦出现回答分片，标记进入回答阶段，后续不再透传思考分片。

        Args:
            无：闭包读取外层 `agent_instance`、`normalized_history_messages` 与回调函数。

        Returns:
            tuple[list[str], list[str], dict[str, Any]]:
                - 回答分片列表；
                - 思考分片列表；
                - 最新 values 状态。
        """

        collected_answer_chunks: list[str] = []
        collected_thinking_chunks: list[str] = []
        collected_latest_state: dict[str, Any] = {}
        is_answering = False

        async for raw_event in agent_instance.astream(
                {"messages": normalized_history_messages},
                stream_mode=["messages", "values"],
        ):
            if not isinstance(raw_event, tuple) or len(raw_event) != 2:
                continue
            mode, payload = raw_event

            if mode == "values":
                if isinstance(payload, Mapping):
                    collected_latest_state = dict(payload)
                continue

            if mode != "messages":
                continue
            if not isinstance(payload, tuple) or len(payload) != 2:
                continue

            message_chunk, metadata = payload
            if not isinstance(metadata, Mapping):
                continue
            if str(metadata.get("langgraph_node") or "") != "model":
                continue

            if not is_answering:
                thinking_delta = _extract_reasoning_content_from_chunk(message_chunk)
                if thinking_delta:
                    collected_thinking_chunks.append(thinking_delta)
                    if on_thinking_delta is not None:
                        on_thinking_delta(thinking_delta)

            delta = extract_text(message_chunk)
            if delta:
                is_answering = True
                collected_answer_chunks.append(delta)
                if on_model_delta is not None:
                    on_model_delta(delta)

        return collected_answer_chunks, collected_thinking_chunks, collected_latest_state

    streamed_chunks, streamed_thinking_chunks, latest_state = _run_async(
        _collect_stream_events()
    )
    final_messages: list[Any] = []
    raw_messages = latest_state.get("messages")
    if isinstance(raw_messages, list):
        final_messages = list(raw_messages)

    return {
        "latest_state": latest_state,
        "streamed_text": "".join(streamed_chunks),
        "streamed_thinking": "".join(streamed_thinking_chunks),
        "final_messages": final_messages,
    }
