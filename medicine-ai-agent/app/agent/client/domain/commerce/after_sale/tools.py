"""
客户端 commerce 售后工具。
"""

from __future__ import annotations

from langchain_core.tools import tool

from app.agent.client.domain.commerce.after_sale.schemas import (
    AfterSaleEligibilityRequest,
    AfterSaleNoRequest,
)
from app.core.agent.middleware import tool_thinking_redaction
from app.schemas.http_response import HttpResponse
from app.utils.http_client import HttpClient

@tool(
    args_schema=AfterSaleNoRequest,
    description=(
            "获取售后详情。"
            "调用时机：用户已提供售后单号，想查看售后状态、退款金额、驳回原因或处理时间线时。"
    ),
)
@tool_thinking_redaction(display_name="获取售后详情")
async def get_after_sale_detail(after_sale_no: str) -> dict:
    """
    功能描述：
        获取客户端售后详情。

    参数说明：
        after_sale_no (str): 售后单号。

    返回值：
        dict: 售后详情数据。

    异常说明：
        无；底层 HTTP 异常由上层抛出。
    """

    async with HttpClient() as client:
        response = await client.get(
            url=f"/agent/client/after-sale/{after_sale_no}",
        )
        return HttpResponse.parse_data(response)


@tool(
    args_schema=AfterSaleEligibilityRequest,
    description=(
            "校验售后资格。"
            "调用时机：用户想确认某笔订单或订单项当前能不能申请退款、退货或换货时。"
    ),
)
@tool_thinking_redaction(display_name="校验售后资格")
async def check_after_sale_eligibility(
        order_no: str,
        order_item_id: int | None = None,
) -> dict:
    """
    功能描述：
        校验客户端售后资格。

    参数说明：
        order_no (str): 订单编号。
        order_item_id (int | None): 订单项 ID。

    返回值：
        dict: 售后资格校验结果。

    异常说明：
        无；底层 HTTP 异常由上层抛出。
    """

    async with HttpClient() as client:
        response = await client.get(
            url="/agent/client/after-sale/eligibility",
            params={
                "orderNo": order_no,
                "orderItemId": order_item_id,
            },
        )
        return HttpResponse.parse_data(response)


__all__ = [
    "check_after_sale_eligibility",
    "get_after_sale_detail",
]
