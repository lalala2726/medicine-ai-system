"""
thinking 文本中的工具名脱敏工具。

说明：
1. 仅处理思维链 `thinking` 文本，不处理 answer/status/function_call 等其他输出；
2. 仅按“完整英文标识符”做替换，不做模糊匹配；
3. 通过增量状态保留尚未结束的英文标识符，保证流式分片场景下不会提前误替换。
"""

from __future__ import annotations

from dataclasses import dataclass

from app.core.agent.middleware.tool_thinking_redaction import (
    resolve_thinking_tool_display_name,
)

# `tool:search_knowledge_context` 这类前缀形式不参与脱敏时使用的前缀标识。
_TOOL_PREFIX_IDENTIFIER = "tool"
# `tool:search_knowledge_context` 这类前缀形式不参与脱敏时使用的分隔符。
_TOOL_PREFIX_DELIMITER = ":"


@dataclass
class ThinkingRedactionState:
    """
    功能描述：
        thinking 文本增量脱敏运行态。

    属性说明：
        pending_identifier (str): 当前尚未遇到右边界的英文标识符片段。
        last_source_identifier (str | None): 最近一次已完成输出的原始英文标识符。
        last_source_delimiter (str | None): 最近一次输出的非标识符分隔符。
    """

    pending_identifier: str = ""
    last_source_identifier: str | None = None
    last_source_delimiter: str | None = None


def _is_ascii_identifier_char(char: str) -> bool:
    """
    功能描述：
        判断单个字符是否属于英文标识符字符。

    参数说明：
        char (str): 待判断的单字符文本。

    返回值：
        bool: 字符属于 `[A-Za-z0-9_]` 时返回 True。

    异常说明：
        无。
    """

    if len(char) != 1:
        return False
    return char.isascii() and (char.isalnum() or char == "_")


def _contains_ascii_identifier_candidate(text: str) -> bool:
    """
    功能描述：
        判断文本中是否存在英文标识符候选字符。

    参数说明：
        text (str): 待检查的文本。

    返回值：
        bool: 文本中存在 `[A-Za-z0-9_]` 字符时返回 True。

    异常说明：
        无。
    """

    return any(_is_ascii_identifier_char(char) for char in text)


def _should_skip_prefixed_tool_name(state: ThinkingRedactionState) -> bool:
    """
    功能描述：
        判断当前英文标识符是否紧跟在 `tool:` 前缀之后。

    参数说明：
        state (ThinkingRedactionState): 当前 thinking 脱敏运行态。

    返回值：
        bool: 紧跟 `tool:` 前缀时返回 True，否则返回 False。

    异常说明：
        无。
    """

    return (
            state.last_source_identifier == _TOOL_PREFIX_IDENTIFIER
            and state.last_source_delimiter == _TOOL_PREFIX_DELIMITER
    )


def _finalize_pending_identifier(state: ThinkingRedactionState) -> str:
    """
    功能描述：
        结束当前待处理英文标识符，并返回脱敏后的最终文本。

    参数说明：
        state (ThinkingRedactionState): 当前 thinking 脱敏运行态。

    返回值：
        str: 已完成右边界确认的最终文本；命中映射时返回中文展示名。

    异常说明：
        无。
    """

    raw_identifier = state.pending_identifier
    state.pending_identifier = ""

    if _should_skip_prefixed_tool_name(state):
        resolved_text = raw_identifier
    else:
        resolved_text = (
                resolve_thinking_tool_display_name(raw_identifier) or raw_identifier
        )

    state.last_source_identifier = raw_identifier
    state.last_source_delimiter = None
    return resolved_text


def consume_thinking_text_chunk(
        *,
        state: ThinkingRedactionState,
        text: str,
) -> str:
    """
    功能描述：
        增量消费一段 thinking 文本，并返回当前已经确认安全可输出的脱敏结果。

    参数说明：
        state (ThinkingRedactionState): 当前 thinking 脱敏运行态。
        text (str): 本次新增的 thinking 文本分片。

    返回值：
        str: 当前分片中已可安全输出的脱敏文本；若全部仍需等待右边界确认则返回空字符串。

    异常说明：
        无。
    """

    raw_text = str(text or "")
    if not raw_text:
        return ""

    if not state.pending_identifier and not _contains_ascii_identifier_candidate(raw_text):
        state.last_source_delimiter = raw_text[-1]
        return raw_text

    rendered_parts: list[str] = []
    for char in raw_text:
        if _is_ascii_identifier_char(char):
            state.pending_identifier += char
            continue

        if state.pending_identifier:
            rendered_parts.append(_finalize_pending_identifier(state))

        rendered_parts.append(char)
        state.last_source_delimiter = char

    return "".join(rendered_parts)


def flush_thinking_text(*, state: ThinkingRedactionState) -> str:
    """
    功能描述：
        在流式结束时强制输出仍缓存在运行态中的最后一段英文标识符。

    参数说明：
        state (ThinkingRedactionState): 当前 thinking 脱敏运行态。

    返回值：
        str: 最终待输出的脱敏文本；若不存在待刷新的缓存则返回空字符串。

    异常说明：
        无。
    """

    if not state.pending_identifier:
        return ""
    return _finalize_pending_identifier(state)


__all__ = [
    "ThinkingRedactionState",
    "consume_thinking_text_chunk",
    "flush_thinking_text",
]
