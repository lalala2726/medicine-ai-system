from __future__ import annotations

from langchain_core.tools import tool

from app.agent.client.domain.tools.navigation_schema import (
    OpenUserAfterSaleListRequest,
    OpenUserOrderListRequest,
    OpenUserPatientListRequest,
)
from app.core.agent.agent_event_bus import enqueue_final_sse_response
from app.core.agent.middleware import tool_thinking_redaction
from app.schemas.sse_response import (
    Action,
    AfterSaleStatusValue,
    AssistantResponse,
    Content,
    MessageType,
    OrderStatusValue,
    UserAfterSaleListPayload,
    UserOrderListPayload,
    UserPatientListPayload,
)

# 前端动作用统一优先级下发，确保页面跳转类动作优先被消费。
_DEFAULT_ACTION_PRIORITY = 100
# 订单状态码到用户文案的映射，用于生成自然语言反馈。
_ORDER_STATUS_LABELS: dict[str, str] = {
    "PENDING_PAYMENT": "待支付",
    "PENDING_SHIPMENT": "待发货",
    "PENDING_RECEIPT": "待收货",
    "COMPLETED": "已完成",
    "CANCELLED": "已取消",
}
# 售后状态码到用户文案的映射，用于生成自然语言反馈。
_AFTER_SALE_STATUS_LABELS: dict[str, str] = {
    "PENDING": "待审核",
    "APPROVED": "已通过",
    "REJECTED": "已拒绝",
    "PROCESSING": "处理中",
    "COMPLETED": "已完成",
    "CANCELLED": "已取消",
}
# 打开就诊人列表时的固定提示文案。
_OPEN_PATIENT_LIST_MESSAGE = "已为你打开就诊人列表"


def _build_order_list_message(order_status: OrderStatusValue | None) -> str:
    """根据订单状态生成给用户看的打开页面提示语。

    Args:
        order_status: 订单状态筛选值。

    Returns:
        str: 打开订单列表的确认文案。
    """

    if order_status is None:
        return "已为你打开订单列表"
    status_label = _ORDER_STATUS_LABELS.get(order_status, order_status)
    return f"已为你打开{status_label}订单列表"


def _build_after_sale_list_message(
        after_sale_status: AfterSaleStatusValue | None,
) -> str:
    """根据售后状态生成给用户看的打开页面提示语。

    Args:
        after_sale_status: 售后状态筛选值。

    Returns:
        str: 打开售后列表的确认文案。
    """

    if after_sale_status is None:
        return "已为你打开售后列表"
    status_label = _AFTER_SALE_STATUS_LABELS.get(after_sale_status, after_sale_status)
    return f"已为你打开{status_label}售后列表"


def _build_patient_list_message() -> str:
    """构建打开就诊人列表时的提示语。

    Args:
        无。

    Returns:
        str: 打开就诊人列表的确认文案。
    """

    return _OPEN_PATIENT_LIST_MESSAGE


def _build_order_list_action_response(
        order_status: OrderStatusValue | None,
) -> AssistantResponse:
    """构建“打开订单列表”动作响应。

    Args:
        order_status: 订单状态筛选值。

    Returns:
        AssistantResponse: 打开订单列表的动作响应。
    """

    message = _build_order_list_message(order_status)
    return AssistantResponse(
        type=MessageType.ACTION,
        content=Content(message=message),
        action=Action(
            type="navigate",
            target="user_order_list",
            payload=UserOrderListPayload(orderStatus=order_status),
            priority=_DEFAULT_ACTION_PRIORITY,
        ),
    )


def _build_after_sale_list_action_response(
        after_sale_status: AfterSaleStatusValue | None,
) -> AssistantResponse:
    """构建“打开售后列表”动作响应。

    Args:
        after_sale_status: 售后状态筛选值。

    Returns:
        AssistantResponse: 打开售后列表的动作响应。
    """

    message = _build_after_sale_list_message(after_sale_status)
    return AssistantResponse(
        type=MessageType.ACTION,
        content=Content(message=message),
        action=Action(
            type="navigate",
            target="user_after_sale_list",
            payload=UserAfterSaleListPayload(afterSaleStatus=after_sale_status),
            priority=_DEFAULT_ACTION_PRIORITY,
        ),
    )


def _build_patient_list_action_response() -> AssistantResponse:
    """构建“打开就诊人列表”动作响应。

    Args:
        无。

    Returns:
        AssistantResponse: 打开就诊人列表的动作响应。
    """

    message = _build_patient_list_message()
    return AssistantResponse(
        type=MessageType.ACTION,
        content=Content(message=message),
        action=Action(
            type="navigate",
            target="user_patient_list",
            payload=UserPatientListPayload(),
            priority=_DEFAULT_ACTION_PRIORITY,
        ),
    )


@tool(
    args_schema=OpenUserOrderListRequest,
    description=(
            "打开用户订单列表页面。"
            "调用时机："
            "1. 用户明确要求打开、进入或查看订单列表时；"
            "2. 用户有明确订单咨询意图，但还没有主动提供订单号，需要引导用户从弹窗中选择或复制订单号发送给你时。"
            "这是可直接执行的页面动作，不需要先征求用户同意。"
            "如果用户提到了待支付、待发货、待收货、已完成、已取消等状态，"
            "请传入对应的 orderStatus；否则不要传。"
    ),
)
@tool_thinking_redaction(display_name="打开用户订单列表")
async def open_user_order_list(orderStatus: OrderStatusValue | None = None) -> str:
    """将“打开订单列表”动作加入当前请求的最终 SSE 响应队列。

    Args:
        orderStatus: 订单状态筛选值。

    Returns:
        str: 给用户看的打开订单列表提示语。
    """

    response = _build_order_list_action_response(orderStatus)
    enqueue_final_sse_response(response)
    return _build_order_list_message(orderStatus)


@tool(
    args_schema=OpenUserAfterSaleListRequest,
    description=(
            "打开用户售后列表页面。"
            "调用时机："
            "1. 用户明确要求打开、进入或查看售后列表时；"
            "2. 用户有明确售后进度咨询意图，但还没有主动提供售后单号，需要引导用户从弹窗中选择或复制售后单号发送给你时。"
            "这是可直接执行的页面动作，不需要先征求用户同意。"
            "如果用户提到了待审核、已通过、已拒绝、处理中、已完成、已取消等状态，"
            "请传入对应的 afterSaleStatus；否则不要传。"
    ),
)
@tool_thinking_redaction(display_name="打开用户售后列表")
async def open_user_after_sale_list(
        afterSaleStatus: AfterSaleStatusValue | None = None,
) -> str:
    """将“打开售后列表”动作加入当前请求的最终 SSE 响应队列。

    Args:
        afterSaleStatus: 售后状态筛选值。

    Returns:
        str: 给用户看的打开售后列表提示语。
    """

    response = _build_after_sale_list_action_response(afterSaleStatus)
    enqueue_final_sse_response(response)
    return _build_after_sale_list_message(afterSaleStatus)


@tool(
    args_schema=OpenUserPatientListRequest,
    description=(
            "打开用户就诊人列表页面。"
            "调用时机："
            "1. 医疗问诊、病情判断或荐药安全判断需要年龄、性别、过敏史、既往史、慢病、长期用药等基础资料时；"
            "2. 你已经先用一句中文说明需要这些资料的原因，并准备引导用户发送就诊人卡时。"
            "这是可直接执行的页面动作，不需要再次征求用户同意。"
    ),
)
@tool_thinking_redaction(display_name="打开用户就诊人列表")
async def open_user_patient_list() -> str:
    """将“打开就诊人列表”动作加入当前请求的最终 SSE 响应队列。

    Args:
        无。

    Returns:
        str: 给用户看的打开就诊人列表提示语。
    """

    response = _build_patient_list_action_response()
    enqueue_final_sse_response(response)
    return _build_patient_list_message()


__all__ = [
    "OpenUserAfterSaleListRequest",
    "OpenUserOrderListRequest",
    "OpenUserPatientListRequest",
    "open_user_after_sale_list",
    "open_user_order_list",
    "open_user_patient_list",
]
