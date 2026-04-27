from __future__ import annotations

from typing import Any

from langchain_core.messages import HumanMessage

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException

# 聊天图片输入最大张数。
CHAT_IMAGE_MAX_COUNT = 5
# 图片问题为空时的默认提问。
EMPTY_IMAGE_QUESTION_TEXT = "请基于我上传的图片内容进行分析并回答。"


def normalize_chat_image_question(question: str | None) -> str:
    """
    功能描述：
        规范化聊天场景图片问题文本。

    参数说明：
        question (str | None): 原始问题文本。

    返回值：
        str: 去除首尾空白后的问题文本；为空时返回默认图片问题文案。

    异常说明：
        无。
    """

    normalized_question = str(question or "").strip()
    return normalized_question or EMPTY_IMAGE_QUESTION_TEXT


def normalize_chat_image_urls(image_urls: list[str] | None) -> list[str]:
    """
    功能描述：
        规范化并校验聊天场景图片 URL 列表。

    参数说明：
        image_urls (list[str] | None): 前端提交的图片 URL 列表。

    返回值：
        list[str]: 规范化后的图片 URL 列表；当入参为空时返回空列表。

    异常说明：
        ServiceException: 图片数量超过 5 张或包含非法 URL 时抛出 BAD_REQUEST。
    """

    if not image_urls:
        return []

    if len(image_urls) > CHAT_IMAGE_MAX_COUNT:
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message=f"图片最多支持上传 {CHAT_IMAGE_MAX_COUNT} 张",
        )

    normalized_urls: list[str] = []
    for raw_image_url in image_urls:
        normalized_image_url = str(raw_image_url or "").strip()
        if not normalized_image_url:
            raise ServiceException(
                code=ResponseCode.BAD_REQUEST,
                message="图片地址不能为空",
            )

        lower_image_url = normalized_image_url.lower()
        if not lower_image_url.startswith("http://") and not lower_image_url.startswith("https://"):
            raise ServiceException(
                code=ResponseCode.BAD_REQUEST,
                message="图片仅支持 http/https URL",
            )
        normalized_urls.append(normalized_image_url)

    return normalized_urls


def build_user_image_markdown_content(question: str, image_urls: list[str] | None) -> str:
    """
    功能描述：
        将用户问题与图片 URL 组装为可持久化的 Markdown 文本。

    参数说明：
        question (str): 用户问题文本。
        image_urls (list[str] | None): 已校验的图片 URL 列表。

    返回值：
        str: 统一的用户消息文本（问题 + Markdown 图片段）。

    异常说明：
        ServiceException: 当图片 URL 非法时抛出 BAD_REQUEST。
    """

    normalized_question = str(question or "").strip()
    normalized_image_urls = normalize_chat_image_urls(image_urls)
    if not normalized_image_urls:
        return normalized_question

    image_markdown_lines = [
        f"![用户上传图片{index + 1}]({image_url})"
        for index, image_url in enumerate(normalized_image_urls)
    ]
    image_markdown_text = "\n".join(image_markdown_lines)
    if normalized_question:
        return f"{normalized_question}\n\n{image_markdown_text}"
    return image_markdown_text


def build_multimodal_human_content(
        question: str | None,
        image_urls: list[str] | None,
) -> list[dict[str, Any]]:
    """
    功能描述：
        构造原生多模态用户消息 content。

    参数说明：
        question (str | None): 用户问题文本。
        image_urls (list[str] | None): 已校验的图片 URL 列表。

    返回值：
        list[dict[str, Any]]: 可直接写入 HumanMessage.content 的多模态内容数组。

    异常说明：
        ServiceException: 当图片 URL 非法时抛出 BAD_REQUEST。
    """

    normalized_question = normalize_chat_image_question(question)
    normalized_image_urls = normalize_chat_image_urls(image_urls)
    content_blocks: list[dict[str, Any]] = [
        {
            "type": "text",
            "text": normalized_question,
        }
    ]
    for image_url in normalized_image_urls:
        content_blocks.append(
            {
                "type": "image_url",
                "image_url": {"url": image_url},
            }
        )
    return content_blocks


def replace_last_human_message_content(
        history_messages: list[Any],
        message_content: str | list[dict[str, Any]],
) -> list[Any]:
    """
    功能描述：
        替换历史消息数组中最后一条 HumanMessage 内容。

    参数说明：
        history_messages (list[Any]): 原始历史消息数组。
        message_content (str | list[dict[str, Any]]): 需要替换的新消息内容。

    返回值：
        list[Any]: 替换后的历史消息数组。

    异常说明：
        无。
    """

    next_history_messages = list(history_messages)
    replacement_message = HumanMessage(content=message_content)
    for index in range(len(next_history_messages) - 1, -1, -1):
        if isinstance(next_history_messages[index], HumanMessage):
            next_history_messages[index] = replacement_message
            return next_history_messages

    next_history_messages.append(replacement_message)
    return next_history_messages


def build_multimodal_history_messages(
        history_messages: list[Any],
        question: str | None,
        image_urls: list[str] | None,
) -> list[Any]:
    """
    功能描述：
        将当前轮用户图片输入改写为原生多模态 HumanMessage。

    参数说明：
        history_messages (list[Any]): 当前节点历史消息数组。
        question (str | None): 当前轮用户问题文本。
        image_urls (list[str] | None): 当前轮用户上传图片 URL 列表。

    返回值：
        list[Any]: 节点最终使用的历史消息数组。

    异常说明：
        ServiceException: 当图片 URL 非法时抛出 BAD_REQUEST。
    """

    normalized_image_urls = normalize_chat_image_urls(image_urls)
    if not normalized_image_urls:
        return list(history_messages)
    multimodal_content = build_multimodal_human_content(
        question=question,
        image_urls=normalized_image_urls,
    )
    return replace_last_human_message_content(history_messages, multimodal_content)


__all__ = [
    "CHAT_IMAGE_MAX_COUNT",
    "EMPTY_IMAGE_QUESTION_TEXT",
    "build_multimodal_history_messages",
    "build_multimodal_human_content",
    "build_user_image_markdown_content",
    "normalize_chat_image_question",
    "normalize_chat_image_urls",
    "replace_last_human_message_content",
]
