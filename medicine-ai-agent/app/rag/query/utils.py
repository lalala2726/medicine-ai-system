from __future__ import annotations

from typing import Any

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException


def normalize_question(question: str) -> str:
    """规范化并校验知识库检索问题。

    Args:
        question: 用户传入的原始问题文本。

    Returns:
        去除首尾空白后的问题文本。

    Raises:
        ServiceException: 当问题在去空白后为空时抛出。
    """

    normalized_question = str(question or "").strip()
    if not normalized_question:
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message="question 不能为空",
        )
    return normalized_question


def coerce_optional_int(value: Any) -> int | None:
    """将元信息中的值规整为可选整数。

    Args:
        value: 待转换的原始值。

    Returns:
        转换成功时返回整数；无法转换时返回 ``None``。
    """

    if value is None:
        return None
    try:
        return int(value)
    except (TypeError, ValueError):
        return None


def extract_message_content_text(content: Any) -> str:
    """从 LangChain 消息内容中提取纯文本。

    Args:
        content: LangChain 消息的 ``content`` 字段。

    Returns:
        规整后的纯文本内容；无法提取时返回空字符串。
    """

    if isinstance(content, str):
        return content.strip()
    if not isinstance(content, list):
        return ""

    pieces: list[str] = []
    for item in content:
        if isinstance(item, str) and item.strip():
            pieces.append(item.strip())
            continue
        if isinstance(item, dict):
            candidate = item.get("text")
            if isinstance(candidate, str) and candidate.strip():
                pieces.append(candidate.strip())
    return "\n".join(pieces).strip()
