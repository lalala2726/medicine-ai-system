"""
客户端 commerce 订单工具。
"""

from __future__ import annotations

from langchain_core.tools import tool

from app.agent.client.domain.commerce.order.schemas import (
    OrderNoRequest,
)
from app.core.agent.middleware import tool_thinking_redaction
from app.core.agent.tool_cache import CLIENT_COMMERCE_TOOL_CACHE_PROFILE, tool_cacheable
from app.schemas.http_response import HttpResponse
from app.utils.http_client import HttpClient

@tool(
    args_schema=OrderNoRequest,
    description=(
            "获取订单详情。"
            "调用时机：用户已经给出订单编号，想查看金额、商品、收货信息、支付状态或物流摘要时。"
    ),
)
@tool_thinking_redaction(display_name="获取订单详情")
@tool_cacheable(
    CLIENT_COMMERCE_TOOL_CACHE_PROFILE,
    tool_name="get_order_detail",
)
async def get_order_detail(order_no: str) -> dict:
    """
    功能描述：
        获取客户端订单详情。

    参数说明：
        order_no (str): 订单编号。

    返回值：
        dict: 订单详情数据。

    异常说明：
        无；底层 HTTP 异常由上层抛出。
    """

    async with HttpClient() as client:
        response = await client.get(
            url=f"/agent/client/order/{order_no}",
        )
        return HttpResponse.parse_data(response)


@tool(
    args_schema=OrderNoRequest,
    description=(
            "获取订单物流。"
            "调用时机：用户已经给出订单编号，想查看是否发货、物流公司、运单号或物流轨迹时。"
    ),
)
@tool_thinking_redaction(display_name="获取订单物流")
@tool_cacheable(
    CLIENT_COMMERCE_TOOL_CACHE_PROFILE,
    tool_name="get_order_shipping",
)
async def get_order_shipping(order_no: str) -> dict:
    """
    功能描述：
        获取客户端订单物流。

    参数说明：
        order_no (str): 订单编号。

    返回值：
        dict: 订单物流数据。

    异常说明：
        无；底层 HTTP 异常由上层抛出。
    """

    async with HttpClient() as client:
        response = await client.get(
            url=f"/agent/client/order/shipping/{order_no}",
        )
        return HttpResponse.parse_data(response)


@tool(
    args_schema=OrderNoRequest,
    description=(
            "获取订单时间线。"
            "调用时机：用户已经给出订单编号，想查看订单从创建到当前状态的关键过程节点时。"
    ),
)
@tool_thinking_redaction(display_name="获取订单时间线")
@tool_cacheable(
    CLIENT_COMMERCE_TOOL_CACHE_PROFILE,
    tool_name="get_order_timeline",
)
async def get_order_timeline(order_no: str) -> dict:
    """
    功能描述：
        获取客户端订单时间线。

    参数说明：
        order_no (str): 订单编号。

    返回值：
        dict: 订单时间线数据。

    异常说明：
        无；底层 HTTP 异常由上层抛出。
    """

    async with HttpClient() as client:
        response = await client.get(
            url=f"/agent/client/order/timeline/{order_no}",
        )
        return HttpResponse.parse_data(response)


@tool(
    args_schema=OrderNoRequest,
    description=(
            "校验是否可取消订单。"
            "调用时机：用户已经给出订单编号，想确认当前订单能不能取消以及原因时。"
    ),
)
@tool_thinking_redaction(display_name="校验订单取消资格")
@tool_cacheable(
    CLIENT_COMMERCE_TOOL_CACHE_PROFILE,
    tool_name="check_order_cancelable",
)
async def check_order_cancelable(order_no: str) -> dict:
    """
    功能描述：
        校验客户端订单是否可取消。

    参数说明：
        order_no (str): 订单编号。

    返回值：
        dict: 订单取消资格校验结果。

    异常说明：
        无；底层 HTTP 异常由上层抛出。
    """

    async with HttpClient() as client:
        response = await client.get(
            url=f"/agent/client/order/cancel-check/{order_no}",
        )
        return HttpResponse.parse_data(response)


__all__ = [
    "check_order_cancelable",
    "get_order_detail",
    "get_order_shipping",
    "get_order_timeline",
]
