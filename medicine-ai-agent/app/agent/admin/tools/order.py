"""
订单领域工具。
"""

from __future__ import annotations

from typing import Optional

from langchain_core.tools import tool
from pydantic import BaseModel, Field

from app.agent.admin.tools.base import (
    format_values_to_path,
    normalize_string_list,
    validate_context_batch_size,
)
from app.core.agent.middleware import (
    tool_call_status,
    tool_thinking_redaction,
)
from app.schemas.http_response import HttpResponse
from app.utils.http_client import HttpClient


class OrderListRequest(BaseModel):
    """
    功能描述：
        订单列表查询入参模型。

    参数说明：
        page_num (int | None): 页码。
        page_size (int | None): 每页数量。
        order_no (str | None): 订单号。
        pay_type (str | None): 支付方式。
        order_status (str | None): 订单状态。
        delivery_type (str | None): 配送方式。
        receiver_name (str | None): 收货人姓名。
        receiver_phone (str | None): 收货人手机号。

    返回值：
        无（数据模型定义）。

    异常说明：
        无。
    """

    page_num: Optional[int] = Field(default=1, description="页码，从 1 开始，默认为 1")
    page_size: Optional[int] = Field(default=10, description="每页数量，默认为 10")
    order_no: Optional[str] = Field(default=None, description="订单编号，精确匹配")
    pay_type: Optional[str] = Field(default=None, description="支付方式编码，例如 wechat、alipay")
    order_status: Optional[str] = Field(default=None, description="订单状态编码，例如 pending、paid、shipped")
    delivery_type: Optional[str] = Field(default=None, description="配送方式编码，例如 express、pickup")
    receiver_name: Optional[str] = Field(default=None, description="收货人姓名，支持模糊搜索")
    receiver_phone: Optional[str] = Field(default=None, description="收货人手机号，精确匹配")


class OrderNosRequest(BaseModel):
    """
    功能描述：
        订单编号批量查询入参模型。

    参数说明：
        order_nos (list[str]): 订单编号字符串数组。

    返回值：
        无（数据模型定义）。

    异常说明：
        无。
    """

    order_nos: list[str] = Field(
        min_length=1,
        description="订单编号字符串数组，必须传 JSON 数组",
        examples=[["O20260101"], ["O20260101", "O20260102"]],
    )


@tool(
    args_schema=OrderListRequest,
    description=(
            "分页查询订单列表，支持按订单号、状态、收货人和支付方式等条件筛选。"
            "适用于查订单列表、筛选订单或定位订单范围。"
    ),
)
@tool_thinking_redaction(display_name="获取订单列表")
@tool_call_status(
    tool_name="获取订单列表",
    start_message="正在查询订单列表",
    error_message="获取订单列表失败",
    timely_message="订单列表正在持续处理中",
)
async def order_list(
        page_num: int = 1,
        page_size: int = 10,
        order_no: Optional[str] = None,
        pay_type: Optional[str] = None,
        order_status: Optional[str] = None,
        delivery_type: Optional[str] = None,
        receiver_name: Optional[str] = None,
        receiver_phone: Optional[str] = None,
) -> dict:
    """
    功能描述：
        查询订单列表。

    参数说明：
        page_num (int): 页码。
        page_size (int): 每页数量。
        order_no (Optional[str]): 订单号。
        pay_type (Optional[str]): 支付方式。
        order_status (Optional[str]): 订单状态。
        delivery_type (Optional[str]): 配送方式。
        receiver_name (Optional[str]): 收货人姓名。
        receiver_phone (Optional[str]): 收货人手机号。

    返回值：
        dict: 订单列表数据。

    异常说明：
        无。
    """

    async with HttpClient() as client:
        params = {
            "pageNum": page_num,
            "pageSize": page_size,
            "orderNo": order_no,
            "payType": pay_type,
            "orderStatus": order_status,
            "deliveryType": delivery_type,
            "receiverName": receiver_name,
            "receiverPhone": receiver_phone,
        }
        response = await client.get(
            url="/agent/admin/order/list",
            params=params,
        )
        return HttpResponse.parse_data(response)


@tool(
    args_schema=OrderNosRequest,
    description=(
            "根据订单编号数组批量查询 AI 聚合订单上下文。"
            "返回订单状态、完整收货地址、完整订单时间线、发货物流完整信息、金额摘要、商品摘要和 AI 决策提示。"
            "当用户问订单现在什么情况、帮我看这些订单、为什么没发货、物流到哪、收货地址、能不能退款、下一步怎么处理时必须优先使用。"
            "一次最多 20 个订单号；多个订单号必须一次性传入 order_nos 数组。"
            "只有用户明确要求完整商品明细或支付明细时，才额外使用 order_detail。"
    ),
)
@tool_thinking_redaction(display_name="获取订单上下文")
@tool_call_status(
    tool_name="获取订单上下文",
    start_message="正在查询订单上下文",
    error_message="获取订单上下文失败",
    timely_message="订单上下文正在持续处理中",
)
async def order_context(order_nos: list[str]) -> dict:
    """
    功能描述：
        根据订单编号数组批量查询 AI 聚合订单上下文。

    参数说明：
        order_nos (list[str]): 订单编号字符串数组，最多 20 个。

    返回值：
        dict: 按订单编号分组的订单上下文数据。

    异常说明：
        ValueError: 当 `order_nos` 非法或超过批量上限时抛出。
    """

    normalized_order_nos = normalize_string_list(order_nos, field_name="order_nos")
    validate_context_batch_size(normalized_order_nos, field_name="order_nos")
    order_nos_str = format_values_to_path(normalized_order_nos)
    async with HttpClient() as client:
        response = await client.get(url=f"/agent/admin/order/context/{order_nos_str}")
        return HttpResponse.parse_data(response)


@tool(
    args_schema=OrderNosRequest,
    description=(
            "根据订单编号数组查询订单详情。"
            "仅适用于用户明确要求完整商品明细、支付明细等非上下文摘要字段；订单状态、收货地址、时间线和物流信息优先使用 order_context。"
    ),
)
@tool_thinking_redaction(display_name="获取订单详情")
@tool_call_status(
    tool_name="获取订单详情",
    start_message="正在查询订单详情",
    error_message="获取订单详情失败",
    timely_message="订单详情正在持续处理中",
)
async def order_detail(order_nos: list[str]) -> dict:
    """
    功能描述：
        根据订单编号数组查询订单详情。

    参数说明：
        order_nos (list[str]): 订单编号字符串数组。

    返回值：
        dict: 订单详情数据。

    异常说明：
        ValueError: 当 `order_nos` 非法时抛出。
    """

    normalized_order_nos = normalize_string_list(order_nos, field_name="order_nos")
    order_nos_str = format_values_to_path(normalized_order_nos)
    async with HttpClient() as client:
        response = await client.get(url=f"/agent/admin/order/{order_nos_str}")
        return HttpResponse.parse_data(response)


__all__ = [
    "OrderListRequest",
    "OrderNosRequest",
    "order_context",
    "order_detail",
    "order_list",
]
