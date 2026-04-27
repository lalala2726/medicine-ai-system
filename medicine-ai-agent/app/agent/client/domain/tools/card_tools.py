from __future__ import annotations

import uuid

from langchain_core.tools import tool
from loguru import logger

from app.agent.client.domain.tools.schema import (
    SendConsentCardRequest,
    SendProductCardRequest,
    SendProductPurchaseCardItem,
    SendProductPurchaseCardRequest,
    SendSelectionCardRequest,
)
from app.agent.services.card_render_service import (
    render_product_card,
    render_product_purchase_card,
)
from app.core.agent.agent_event_bus import enqueue_final_sse_response
from app.core.agent.middleware import (
    tool_call_status,
    tool_thinking_redaction,
    tool_trace_record,
)
from app.schemas.sse_response import Card, AssistantResponse, MessageType

# 通用成功返回值，供工具调用链判断执行成功。
_SUCCESS_RESULT = "__SUCCESS__"
# 商品卡片在业务端未返回有效数据时使用的错误消息。
_PRODUCT_CARD_EMPTY_ERROR = "__ERROR__:未从业务端获取到商品信息无法发送商品卡片"
# 商品购买卡片在业务端未返回有效数据时使用的错误消息。
_PRODUCT_PURCHASE_CARD_EMPTY_ERROR = "__ERROR__:未从业务端获取到商品购买信息无法发送商品购买卡片"
# 前端同意卡片类型标识。
_CONSENT_CARD_TYPE = "consent-card"
# 前端选择卡片类型标识。
_SELECTION_CARD_TYPE = "selection-card"


def build_card_response(
        card: Card,
        *,
        persist_card: bool = False,
) -> AssistantResponse:
    """构建通用卡片 SSE 响应，并标记该卡片是否需要随消息持久化。"""

    return AssistantResponse(
        type=MessageType.CARD,
        card=card,
        meta={
            "card_uuid": str(uuid.uuid4()),
            "persist_card": persist_card,
        },
    )


def _enqueue_card_response(
        card: Card,
        *,
        persist_card: bool,
) -> None:
    """构建卡片响应并放入当前请求的最终 SSE 队列。"""

    enqueue_final_sse_response(
        build_card_response(card, persist_card=persist_card)
    )


@tool(
    args_schema=SendProductCardRequest,
    description=(
            "向前端发送商品卡片。"
            "调用时机：已经筛出要推荐的商品后，希望在本轮回答文本结束后展示商品卡片时。"
    ),
)
@tool_trace_record()
@tool_thinking_redaction(display_name="发送商品卡片")
@tool_call_status(
    tool_name="发送商品卡片",
    start_message="正在生成商品卡片",
    error_message="商品卡片发送失败",
    timely_message="商品卡片仍在处理中",
)
async def send_product_card(productIds: list[int]) -> str:
    """渲染商品卡片并在本轮回答结束时发送给前端。"""

    try:
        card = await render_product_card(productIds)
    except Exception as exc:
        logger.opt(exception=exc).error(
            "send_product_card failed product_ids={}",
            productIds,
        )
        raise

    if card is None:
        return _PRODUCT_CARD_EMPTY_ERROR

    _enqueue_card_response(card, persist_card=True)
    return _SUCCESS_RESULT


@tool(
    args_schema=SendProductPurchaseCardRequest,
    description=(
            "向前端发送商品购买卡片。"
            "调用时机：已经明确用户准备购买哪些商品以及对应数量后，希望在本轮回答文本结束后展示购买确认卡片时。"
    ),
)
@tool_trace_record()
@tool_thinking_redaction(display_name="发送商品购买卡片")
@tool_call_status(
    tool_name="发送商品购买卡片",
    start_message="正在生成商品购买卡片",
    error_message="商品购买卡片发送失败",
    timely_message="商品购买卡片仍在处理中",
)
async def send_product_purchase_card(
        items: list[SendProductPurchaseCardItem],
) -> str:
    """渲染商品购买确认卡片并在本轮回答结束时发送给前端。"""

    serialized_items = [item.model_dump(mode="json") for item in items]

    try:
        card = await render_product_purchase_card(items)
    except Exception as exc:
        logger.opt(exception=exc).error(
            "send_product_purchase_card failed items={}",
            serialized_items,
        )
        raise

    if card is None:
        return _PRODUCT_PURCHASE_CARD_EMPTY_ERROR

    _enqueue_card_response(card, persist_card=True)
    return _SUCCESS_RESULT


@tool(
    args_schema=SendConsentCardRequest,
    description=(
            "向前端发送同意/拒绝交互卡片。"
            "调用时机：需要用户确认是否同意某个方案、操作或下一步处理时。"
    ),
)
@tool_trace_record()
@tool_thinking_redaction(display_name="发送同意卡片")
@tool_call_status(
    tool_name="发送同意卡片",
    start_message="正在生成同意卡片",
    error_message="同意卡片发送失败",
    timely_message="同意卡片仍在处理中",
)
async def send_consent_card(
        title: str,
        description: str | None = None,
        confirm_text: str = "同意",
        reject_text: str = "拒绝",
) -> str:
    """构建同意/拒绝交互卡片并在本轮回答结束时发送给前端。"""

    request = SendConsentCardRequest(
        title=title,
        description=description,
        confirm_text=confirm_text,
        reject_text=reject_text,
    )
    _enqueue_card_response(
        Card(
            type=_CONSENT_CARD_TYPE,
            data=request.to_card_data().model_dump(mode="json", exclude_none=True),
        ),
        persist_card=True,
    )
    return _SUCCESS_RESULT


@tool(
    args_schema=SendSelectionCardRequest,
    description=(
            "向前端发送选择交互卡片。"
            "调用时机：需要用户从一个或多个预设选项中做选择时。"
    ),
)
@tool_trace_record()
@tool_thinking_redaction(display_name="发送选择卡片")
@tool_call_status(
    tool_name="发送选择卡片",
    start_message="正在生成选择卡片",
    error_message="选择卡片发送失败",
    timely_message="选择卡片仍在处理中",
)
async def send_selection_card(
        title: str,
        options: list[str],
) -> str:
    """构建选择交互卡片并在本轮回答结束时发送给前端。"""

    request = SendSelectionCardRequest(
        title=title,
        options=options,
    )
    _enqueue_card_response(
        Card(
            type=_SELECTION_CARD_TYPE,
            data=request.to_card_data().model_dump(mode="json", exclude_none=True),
        ),
        persist_card=True,
    )
    return _SUCCESS_RESULT


__all__ = [
    "send_consent_card",
    "send_product_card",
    "send_product_purchase_card",
    "send_selection_card",
]
