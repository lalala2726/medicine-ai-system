from __future__ import annotations

from typing import Any, Mapping


def _resolve_model_name_from_response(response: Any, fallback: str = "unknown") -> str:
    """
    从响应元数据解析模型名。

    Args:
        response: 模型响应对象。
        fallback: 兜底模型名。

    Returns:
        str: 解析到的模型名或 fallback。
    """

    response_metadata = getattr(response, "response_metadata", None)
    if isinstance(response_metadata, Mapping):
        for key in ("model_name", "model", "model_id"):
            candidate = response_metadata.get(key)
            if isinstance(candidate, str) and candidate.strip():
                return candidate.strip()
    return fallback


def extract_text(message: Any) -> str:
    """
    从消息对象中提取纯文本内容。

    Args:
        message: LangChain 消息对象（AIMessage/Chunk 等）。

    Returns:
        str: 纯文本内容，无内容时返回空字符串。
    """

    content = getattr(message, "content", "")
    if isinstance(content, str):
        return content
    if isinstance(content, list):
        parts = []
        for item in content:
            if isinstance(item, dict) and isinstance(item.get("text"), str):
                parts.append(item["text"])
            elif isinstance(item, str):
                parts.append(item)
        return "".join(parts)
    return "" if content is None else str(content)


def _is_ai_message(message: Any) -> bool:
    """
    判断消息是否为 AI 消息。

    Args:
        message: 任意消息对象。

    Returns:
        bool: 是 AI 消息返回 True，否则 False。
    """

    return str(getattr(message, "type", "") or "").lower() == "ai"


def resolve_final_messages(payload: Any) -> list[Any]:
    """
    从 agent 执行结果中提取最终消息列表。

    Args:
        payload: 任意可能包含最终消息的载荷，支持：
            - `agent.invoke(...)` 返回值（dict，含 `messages`）；
            - `agent_stream(...)` 返回值（dict，含 `final_messages` 或 `latest_state`）；
            - 直接传入消息列表（list）。

    Returns:
        list[Any]: 最终消息列表；未命中时返回空列表。
    """

    if isinstance(payload, list):
        return list(payload)

    if not isinstance(payload, Mapping):
        return []

    raw_messages = payload.get("messages")
    if isinstance(raw_messages, list):
        return list(raw_messages)

    raw_final_messages = payload.get("final_messages")
    if isinstance(raw_final_messages, list):
        return list(raw_final_messages)

    latest_state = payload.get("latest_state")
    if isinstance(latest_state, Mapping):
        nested_messages = latest_state.get("messages")
        if isinstance(nested_messages, list):
            return list(nested_messages)

    return []


def resolve_final_output_text(
        *,
        payload: Any,
        fallback_text: str = "",
) -> str:
    """
    从最终消息或回退文本中提取最终可持久化回答文本。

    Args:
        payload: 任意可用于提取最终消息的载荷。
        fallback_text: 无法从最终消息提取文本时的兜底文本。

    Returns:
        str: 最终回答文本；无法提取时返回兜底文本。
    """

    final_messages = resolve_final_messages(payload)
    for message in reversed(final_messages):
        if not _is_ai_message(message):
            continue
        text = extract_text(message).strip()
        if text:
            return text
    return str(fallback_text or "").strip()
