"""
售后领域工具。
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
from app.core.agent.tool_cache import ADMIN_TOOL_CACHE_PROFILE, tool_cacheable
from app.schemas.http_response import HttpResponse
from app.utils.http_client import HttpClient


class AfterSaleListRequest(BaseModel):
    """
    功能描述：
        售后列表查询入参模型。

    参数说明：
        page_num (int): 页码。
        page_size (int): 每页数量。
        after_sale_type (str | None): 售后类型。
        after_sale_status (str | None): 售后状态。
        order_no (str | None): 订单号。
        user_id (int | None): 用户 ID。
        apply_reason (str | None): 申请原因。

    返回值：
        无（数据模型定义）。

    异常说明：
        无。
    """

    page_num: int = Field(default=1, ge=1, description="页码，从 1 开始")
    page_size: int = Field(default=10, ge=1, le=200, description="每页数量，范围 1-200")
    after_sale_type: Optional[str] = Field(default=None, description="售后类型，例如 REFUND_ONLY")
    after_sale_status: Optional[str] = Field(default=None, description="售后状态，例如 PENDING、APPROVED")
    order_no: Optional[str] = Field(default=None, description="订单编号，精确匹配")
    user_id: Optional[int] = Field(default=None, ge=1, description="用户 ID")
    apply_reason: Optional[str] = Field(default=None, description="申请原因，例如 DAMAGED")


class AfterSaleNosRequest(BaseModel):
    """
    功能描述：
        售后详情批量查询入参模型。

    参数说明：
        after_sale_nos (list[str]): 售后单号字符串数组。

    返回值：
        无（数据模型定义）。

    异常说明：
        无。
    """

    after_sale_nos: list[str] = Field(
        min_length=1,
        description="售后单号字符串数组，必须传 JSON 数组",
        examples=[["AS20260101"], ["AS20260101", "AS20260102"]],
    )


def _build_after_sale_list_cache_input(arguments: dict[str, object]) -> dict[str, object]:
    """
    功能描述：
        构造售后列表缓存入参。

    参数说明：
        arguments (dict[str, object]): 绑定后的函数参数映射。

    返回值：
        dict[str, object]: 与真实 HTTP 查询参数一致的结构。

    异常说明：
        无。
    """

    return {
        "pageNum": arguments.get("page_num"),
        "pageSize": arguments.get("page_size"),
        "afterSaleType": arguments.get("after_sale_type"),
        "afterSaleStatus": arguments.get("after_sale_status"),
        "orderNo": arguments.get("order_no"),
        "userId": arguments.get("user_id"),
        "applyReason": arguments.get("apply_reason"),
    }


def _build_after_sale_detail_cache_input(arguments: dict[str, object]) -> dict[str, object]:
    """
    功能描述：
        构造售后详情缓存入参。

    参数说明：
        arguments (dict[str, object]): 绑定后的函数参数映射。

    返回值：
        dict[str, object]: 标准化后的售后单号数组。

    异常说明：
        ValueError: 当 `after_sale_nos` 非法时抛出。
    """

    raw_after_sale_nos = arguments.get("after_sale_nos")
    normalized_after_sale_nos = normalize_string_list(raw_after_sale_nos, field_name="after_sale_nos")
    return {"after_sale_nos": normalized_after_sale_nos}


def _build_after_sale_context_cache_input(arguments: dict[str, object]) -> dict[str, object]:
    """
    功能描述：
        构造售后聚合上下文缓存入参。

    参数说明：
        arguments (dict[str, object]): 绑定后的函数参数映射。

    返回值：
        dict[str, object]: 标准化且数量合法的售后单号数组。

    异常说明：
        ValueError: 当 `after_sale_nos` 非法或超过批量上限时抛出。
    """

    cache_input = _build_after_sale_detail_cache_input(arguments)
    validate_context_batch_size(cache_input["after_sale_nos"], field_name="after_sale_nos")
    return cache_input


@tool(
    args_schema=AfterSaleListRequest,
    description=(
            "分页查询售后申请列表，支持按售后类型、状态、订单号、用户 ID 和申请原因筛选。"
            "适用于定位售后单范围和查看待处理售后。"
    ),
)
@tool_thinking_redaction(display_name="查询售后列表")
@tool_call_status(
    tool_name="查询售后列表",
    start_message="正在查询售后列表",
    error_message="查询售后列表失败",
    timely_message="售后列表正在持续处理中",
)
@tool_cacheable(
    ADMIN_TOOL_CACHE_PROFILE,
    tool_name="after_sale_list",
    input_builder=_build_after_sale_list_cache_input,
)
async def after_sale_list(
        page_num: int = 1,
        page_size: int = 10,
        after_sale_type: Optional[str] = None,
        after_sale_status: Optional[str] = None,
        order_no: Optional[str] = None,
        user_id: Optional[int] = None,
        apply_reason: Optional[str] = None,
) -> dict:
    """
    功能描述：
        查询售后申请列表。

    参数说明：
        page_num (int): 页码。
        page_size (int): 每页数量。
        after_sale_type (Optional[str]): 售后类型。
        after_sale_status (Optional[str]): 售后状态。
        order_no (Optional[str]): 订单号。
        user_id (Optional[int]): 用户 ID。
        apply_reason (Optional[str]): 申请原因。

    返回值：
        dict: 售后列表数据。

    异常说明：
        无。
    """

    async with HttpClient() as client:
        params = {
            "pageNum": page_num,
            "pageSize": page_size,
            "afterSaleType": after_sale_type,
            "afterSaleStatus": after_sale_status,
            "orderNo": order_no,
            "userId": user_id,
            "applyReason": apply_reason,
        }
        response = await client.get(
            url="/agent/admin/after-sale/list",
            params=params,
        )
        return HttpResponse.parse_data(response)


@tool(
    args_schema=AfterSaleNosRequest,
    description=(
            "根据售后单号数组批量查询 AI 聚合售后上下文。"
            "当用户问售后进度、为什么拒绝、下一步怎么处理、售后当前状态时优先使用。"
            "一次最多 20 个售后单号；多个售后单号必须一次性传入 after_sale_nos 数组。"
            "只有用户明确要求完整售后详情、完整凭证或完整处理时间线时，才改用 after_sale_detail。"
    ),
)
@tool_thinking_redaction(display_name="查询售后上下文")
@tool_call_status(
    tool_name="查询售后上下文",
    start_message="正在查询售后上下文",
    error_message="查询售后上下文失败",
    timely_message="售后上下文正在持续处理中",
)
@tool_cacheable(
    ADMIN_TOOL_CACHE_PROFILE,
    tool_name="after_sale_context",
    input_builder=_build_after_sale_context_cache_input,
)
async def after_sale_context(after_sale_nos: list[str]) -> dict:
    """
    功能描述：
        根据售后单号数组批量查询 AI 聚合售后上下文。

    参数说明：
        after_sale_nos (list[str]): 售后单号字符串数组，最多 20 个。

    返回值：
        dict: 按售后单号分组的售后上下文数据。

    异常说明：
        ValueError: 当 `after_sale_nos` 非法或超过批量上限时抛出。
    """

    normalized_after_sale_nos = normalize_string_list(after_sale_nos, field_name="after_sale_nos")
    validate_context_batch_size(normalized_after_sale_nos, field_name="after_sale_nos")
    after_sale_nos_str = format_values_to_path(normalized_after_sale_nos)
    async with HttpClient() as client:
        response = await client.get(
            url=f"/agent/admin/after-sale/context/{after_sale_nos_str}",
        )
        return HttpResponse.parse_data(response)


@tool(
    args_schema=AfterSaleNosRequest,
    description=(
            "根据售后单号数组查询售后详情。"
            "适用于查看处理进度、原因、处理结果和关联订单信息。"
    ),
)
@tool_thinking_redaction(display_name="查询售后详情")
@tool_call_status(
    tool_name="查询售后详情",
    start_message="正在查询售后详情",
    error_message="查询售后详情失败",
    timely_message="售后详情正在持续处理中",
)
@tool_cacheable(
    ADMIN_TOOL_CACHE_PROFILE,
    tool_name="after_sale_detail",
    input_builder=_build_after_sale_detail_cache_input,
)
async def after_sale_detail(after_sale_nos: list[str]) -> dict:
    """
    功能描述：
        根据售后单号数组查询售后详情。

    参数说明：
        after_sale_nos (list[str]): 售后单号字符串数组。

    返回值：
        dict: 售后详情数组。

    异常说明：
        ValueError: 当 `after_sale_nos` 非法时抛出。
    """

    normalized_after_sale_nos = normalize_string_list(after_sale_nos, field_name="after_sale_nos")
    after_sale_nos_str = format_values_to_path(normalized_after_sale_nos)
    async with HttpClient() as client:
        response = await client.get(
            url=f"/agent/admin/after-sale/detail/{after_sale_nos_str}",
        )
        return HttpResponse.parse_data(response)


__all__ = [
    "AfterSaleListRequest",
    "AfterSaleNosRequest",
    "after_sale_context",
    "after_sale_detail",
    "after_sale_list",
]
