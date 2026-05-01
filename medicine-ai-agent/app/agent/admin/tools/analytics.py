"""
运营分析领域工具。
"""

from __future__ import annotations

from collections.abc import Mapping

from langchain_core.tools import tool
from pydantic import BaseModel, Field

from app.core.agent.middleware import (
    tool_call_status,
    tool_thinking_redaction,
)
from app.schemas.http_response import HttpResponse
from app.utils.http_client import HttpClient

# 最近天数参数的公共说明。
_DAYS_HELP = (
    "days 为最近天数，默认 30，范围 1-730。"
    "常用值：7 表示近 7 天，15 表示近 15 天，30 表示近 30 天，84 表示近 12 周，365 表示近 12 月。"
)


class AnalyticsDaysRequest(BaseModel):
    """
    功能描述：
        按最近天数查询的运营分析入参模型。

    参数说明：
        days (int): 最近天数。

    返回值：
        无（数据模型定义）。

    异常说明：
        无。
    """

    days: int = Field(default=30, ge=1, le=730, description="最近天数，默认 30，范围 1-730")


class AnalyticsRankRequest(AnalyticsDaysRequest):
    """
    功能描述：
        排行榜查询入参模型。

    参数说明：
        days (int): 最近天数。
        limit (int): 返回数量限制。

    返回值：
        无（数据模型定义）。

    异常说明：
        无。
    """

    limit: int = Field(default=10, ge=1, le=20, description="返回数量限制，默认 10，范围 1-20")


async def request_analytics_data(
        url: str,
        params: Mapping[str, object] | None = None,
) -> dict:
    """
    功能描述：
        发送运营分析查询请求并统一解析响应数据。

    参数说明：
        url (str): 请求路径。
        params (Mapping[str, object] | None): 查询参数。

    返回值：
        dict: 解析后的分析数据。

    异常说明：
        无。
    """

    normalized_params = dict(params or {})
    async with HttpClient() as client:
        response = await client.get(url=url, params=normalized_params)
        return HttpResponse.parse_data(response)


@tool(
    description=(
            "获取实时运营总览。"
            "包括累计成交、今日成交、待发货、待收货、待处理售后和处理中售后等指标。"
    )
)
@tool_thinking_redaction(display_name="实时运营总览")
@tool_call_status(
    tool_name="实时运营总览",
    start_message="正在查询实时运营总览",
    error_message="获取实时运营总览失败",
    timely_message="实时运营总览正在持续处理中",
)
async def analytics_realtime_overview() -> dict:
    """
    功能描述：
        获取实时运营总览。

    参数说明：
        无。

    返回值：
        dict: 实时运营指标数据。

    异常说明：
        无。
    """

    return await request_analytics_data(
        url="/agent/admin/analytics/realtime-overview",
    )


@tool(
    args_schema=AnalyticsDaysRequest,
    description=(
            "获取经营结果汇总。"
            "返回成交金额、支付订单数、净成交额、退款金额、退款率和退货退款件数等指标。"
            f"{_DAYS_HELP}"
    ),
)
@tool_thinking_redaction(display_name="经营结果汇总")
@tool_call_status(
    tool_name="经营结果汇总",
    start_message="正在查询经营结果汇总",
    error_message="获取经营结果汇总失败",
    timely_message="经营结果汇总正在持续处理中",
)
async def analytics_range_summary(days: int = 30) -> dict:
    """
    功能描述：
        获取经营结果汇总。

    参数说明：
        days (int): 最近天数。

    返回值：
        dict: 经营结果汇总数据。

    异常说明：
        无。
    """

    return await request_analytics_data(
        url="/agent/admin/analytics/range-summary",
        params={"days": days},
    )


@tool(
    args_schema=AnalyticsDaysRequest,
    description=(
            "获取支付转化汇总。"
            "返回下单数、已支付数、待支付数、关闭数和支付转化率。"
            f"{_DAYS_HELP}"
    ),
)
@tool_thinking_redaction(display_name="支付转化汇总")
@tool_call_status(
    tool_name="支付转化汇总",
    start_message="正在查询支付转化汇总",
    error_message="获取支付转化汇总失败",
    timely_message="支付转化汇总正在持续处理中",
)
async def analytics_conversion_summary(days: int = 30) -> dict:
    """
    功能描述：
        获取支付转化汇总。

    参数说明：
        days (int): 最近天数。

    返回值：
        dict: 支付转化汇总数据。

    异常说明：
        无。
    """

    return await request_analytics_data(
        url="/agent/admin/analytics/conversion-summary",
        params={"days": days},
    )


@tool(
    args_schema=AnalyticsDaysRequest,
    description=(
            "获取履约时效汇总。"
            "返回平均发货时长、平均收货时长、超时未发货订单数和超时未收货订单数。"
            f"{_DAYS_HELP}"
    ),
)
@tool_thinking_redaction(display_name="履约时效汇总")
@tool_call_status(
    tool_name="履约时效汇总",
    start_message="正在查询履约时效汇总",
    error_message="获取履约时效汇总失败",
    timely_message="履约时效汇总正在持续处理中",
)
async def analytics_fulfillment_summary(days: int = 30) -> dict:
    """
    功能描述：
        获取履约时效汇总。

    参数说明：
        days (int): 最近天数。

    返回值：
        dict: 履约时效汇总数据。

    异常说明：
        无。
    """

    return await request_analytics_data(
        url="/agent/admin/analytics/fulfillment-summary",
        params={"days": days},
    )


@tool(
    args_schema=AnalyticsDaysRequest,
    description=(
            "获取售后处理时效汇总。"
            "返回平均审核时长、平均完结时长、超 24 小时未审核数和超 72 小时未完结数。"
            f"{_DAYS_HELP}"
    ),
)
@tool_thinking_redaction(display_name="售后处理时效汇总")
@tool_call_status(
    tool_name="售后处理时效汇总",
    start_message="正在查询售后处理时效汇总",
    error_message="获取售后处理时效汇总失败",
    timely_message="售后处理时效汇总正在持续处理中",
)
async def analytics_after_sale_efficiency_summary(days: int = 30) -> dict:
    """
    功能描述：
        获取售后处理时效汇总。

    参数说明：
        days (int): 最近天数。

    返回值：
        dict: 售后处理时效汇总数据。

    异常说明：
        无。
    """

    return await request_analytics_data(
        url="/agent/admin/analytics/after-sale-efficiency-summary",
        params={"days": days},
    )


@tool(
    args_schema=AnalyticsDaysRequest,
    description=(
            "获取售后状态分布。"
            "返回待审核、已通过、处理中和已完成等状态的数量分布。"
            f"{_DAYS_HELP}"
    ),
)
@tool_thinking_redaction(display_name="售后状态分布")
@tool_call_status(
    tool_name="售后状态分布",
    start_message="正在查询售后状态分布",
    error_message="获取售后状态分布失败",
    timely_message="售后状态分布正在持续处理中",
)
async def analytics_after_sale_status_distribution(days: int = 30) -> dict:
    """
    功能描述：
        获取售后状态分布。

    参数说明：
        days (int): 最近天数。

    返回值：
        dict: 售后状态分布数据。

    异常说明：
        无。
    """

    return await request_analytics_data(
        url="/agent/admin/analytics/after-sale-status-distribution",
        params={"days": days},
    )


@tool(
    args_schema=AnalyticsDaysRequest,
    description=(
            "获取售后原因分布。"
            "返回商品损坏、描述不符和不想要了等原因的数量分布。"
            f"{_DAYS_HELP}"
    ),
)
@tool_thinking_redaction(display_name="售后原因分布")
@tool_call_status(
    tool_name="售后原因分布",
    start_message="正在查询售后原因分布",
    error_message="获取售后原因分布失败",
    timely_message="售后原因分布正在持续处理中",
)
async def analytics_after_sale_reason_distribution(days: int = 30) -> dict:
    """
    功能描述：
        获取售后原因分布。

    参数说明：
        days (int): 最近天数。

    返回值：
        dict: 售后原因分布数据。

    异常说明：
        无。
    """

    return await request_analytics_data(
        url="/agent/admin/analytics/after-sale-reason-distribution",
        params={"days": days},
    )


@tool(
    args_schema=AnalyticsRankRequest,
    description=(
            "获取热销商品排行。"
            "按销量返回商品名称、商品图、销量和成交金额。"
            f"{_DAYS_HELP}"
    ),
)
@tool_thinking_redaction(display_name="热销商品排行")
@tool_call_status(
    tool_name="热销商品排行",
    start_message="正在查询热销商品排行",
    error_message="获取热销商品排行失败",
    timely_message="热销商品排行正在持续处理中",
)
async def analytics_top_selling_products(days: int = 30, limit: int = 10) -> dict:
    """
    功能描述：
        获取热销商品排行。

    参数说明：
        days (int): 最近天数。
        limit (int): 返回数量限制。

    返回值：
        dict: 热销商品排行数据。

    异常说明：
        无。
    """

    return await request_analytics_data(
        url="/agent/admin/analytics/top-selling-products",
        params={"days": days, "limit": limit},
    )


@tool(
    args_schema=AnalyticsRankRequest,
    description=(
            "获取退货退款风险商品排行。"
            "返回商品销量、退货退款件数、退货退款率和退款金额。"
            f"{_DAYS_HELP}"
    ),
)
@tool_thinking_redaction(display_name="退货退款风险商品排行")
@tool_call_status(
    tool_name="退货退款风险商品排行",
    start_message="正在查询退货退款风险商品排行",
    error_message="获取退货退款风险商品排行失败",
    timely_message="退货退款风险商品排行正在持续处理中",
)
async def analytics_return_refund_risk_products(days: int = 30, limit: int = 10) -> dict:
    """
    功能描述：
        获取退货退款风险商品排行。

    参数说明：
        days (int): 最近天数。
        limit (int): 返回数量限制。

    返回值：
        dict: 风险商品排行数据。

    异常说明：
        无。
    """

    return await request_analytics_data(
        url="/agent/admin/analytics/return-refund-risk-products",
        params={"days": days, "limit": limit},
    )


@tool(
    args_schema=AnalyticsDaysRequest,
    description=(
            "获取成交趋势。"
            "返回完整时间轴上的成交金额和支付订单数趋势点位。"
            f"{_DAYS_HELP}"
    ),
)
@tool_thinking_redaction(display_name="成交趋势")
@tool_call_status(
    tool_name="成交趋势",
    start_message="正在查询成交趋势",
    error_message="获取成交趋势失败",
    timely_message="成交趋势正在持续处理中",
)
async def analytics_sales_trend(days: int = 30) -> dict:
    """
    功能描述：
        获取成交趋势。

    参数说明：
        days (int): 最近天数。

    返回值：
        dict: 成交趋势数据。

    异常说明：
        无。
    """

    return await request_analytics_data(
        url="/agent/admin/analytics/sales-trend",
        params={"days": days},
    )


@tool(
    args_schema=AnalyticsDaysRequest,
    description=(
            "获取售后趋势。"
            "返回完整时间轴上的退款金额和售后申请数趋势点位。"
            f"{_DAYS_HELP}"
    ),
)
@tool_thinking_redaction(display_name="售后趋势")
@tool_call_status(
    tool_name="售后趋势",
    start_message="正在查询售后趋势",
    error_message="获取售后趋势失败",
    timely_message="售后趋势正在持续处理中",
)
async def analytics_after_sale_trend(days: int = 30) -> dict:
    """
    功能描述：
        获取售后趋势。

    参数说明：
        days (int): 最近天数。

    返回值：
        dict: 售后趋势数据。

    异常说明：
        无。
    """

    return await request_analytics_data(
        url="/agent/admin/analytics/after-sale-trend",
        params={"days": days},
    )


__all__ = [
    "AnalyticsDaysRequest",
    "AnalyticsRankRequest",
    "analytics_after_sale_efficiency_summary",
    "analytics_after_sale_reason_distribution",
    "analytics_after_sale_status_distribution",
    "analytics_after_sale_trend",
    "analytics_conversion_summary",
    "analytics_fulfillment_summary",
    "analytics_range_summary",
    "analytics_realtime_overview",
    "analytics_return_refund_risk_products",
    "analytics_sales_trend",
    "analytics_top_selling_products",
]
