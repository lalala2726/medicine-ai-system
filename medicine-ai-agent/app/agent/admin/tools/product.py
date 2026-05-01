"""
商品领域工具。
"""

from __future__ import annotations

from typing import Optional

from langchain_core.tools import tool
from pydantic import BaseModel, Field

from app.agent.admin.tools.base import format_ids_to_string, normalize_id_list
from app.core.agent.middleware import (
    tool_call_status,
    tool_thinking_redaction,
)
from app.schemas.http_response import HttpResponse
from app.utils.http_client import HttpClient


class ProductListRequest(BaseModel):
    """
    功能描述：
        商品列表查询入参模型。

    参数说明：
        page_num (int | None): 页码。
        page_size (int | None): 每页数量。
        id (int | None): 商品 ID。
        name (str | None): 商品名称关键词。
        category_id (int | None): 分类 ID。
        status (int | None): 上下架状态。
        min_price (float | None): 最低价格。
        max_price (float | None): 最高价格。

    返回值：
        无（数据模型定义）。

    异常说明：
        无。
    """

    page_num: Optional[int] = Field(default=1, description="页码，从 1 开始")
    page_size: Optional[int] = Field(default=10, description="每页数量")
    id: Optional[int] = Field(default=None, description="商品 ID，精确匹配")
    name: Optional[str] = Field(default=None, description="商品名称关键词，支持模糊搜索")
    category_id: Optional[int] = Field(default=None, description="商品分类 ID")
    status: Optional[int] = Field(default=None, description="商品状态，1 表示上架，0 表示下架")
    min_price: Optional[float] = Field(default=None, description="最低价格，单位元")
    max_price: Optional[float] = Field(default=None, description="最高价格，单位元")


class ProductDetailRequest(BaseModel):
    """
    功能描述：
        商品详情查询入参模型。

    参数说明：
        product_id (list[str]): 商品 ID 字符串数组。

    返回值：
        无（数据模型定义）。

    异常说明：
        无。
    """

    product_id: list[str] = Field(
        min_length=1,
        description="商品 ID 字符串数组，必须传 JSON 数组",
        examples=[["2001"], ["2001", "2003"]],
    )


class DrugDetailRequest(BaseModel):
    """
    功能描述：
        药品详情查询入参模型。

    参数说明：
        product_id (list[str]): 药品商品 ID 字符串数组。

    返回值：
        无（数据模型定义）。

    异常说明：
        无。
    """

    product_id: list[str] = Field(
        min_length=1,
        description="药品商品 ID 字符串数组，必须传 JSON 数组",
        examples=[["2001"], ["2001", "2003"]],
    )


@tool(
    args_schema=ProductListRequest,
    description=(
            "查询商品列表，支持按名称、价格区间、分类和状态筛选。"
            "适用于查看商品范围、筛选在售商品或定位候选商品。"
    ),
)
@tool_thinking_redaction(display_name="获取商品列表")
@tool_call_status(
    tool_name="获取商品列表",
    start_message="正在查询商品列表",
    error_message="获取商品列表失败",
    timely_message="商品列表正在持续处理中",
)
async def product_list(
        page_num: int = 1,
        page_size: int = 10,
        id: Optional[int] = None,
        name: Optional[str] = None,
        category_id: Optional[int] = None,
        status: Optional[int] = None,
        min_price: Optional[float] = None,
        max_price: Optional[float] = None,
) -> dict:
    """
    功能描述：
        查询商品列表。

    参数说明：
        page_num (int): 页码。
        page_size (int): 每页数量。
        id (Optional[int]): 商品 ID。
        name (Optional[str]): 商品名称关键词。
        category_id (Optional[int]): 分类 ID。
        status (Optional[int]): 上下架状态。
        min_price (Optional[float]): 最低价格。
        max_price (Optional[float]): 最高价格。

    返回值：
        dict: 商品列表数据。

    异常说明：
        无。
    """

    async with HttpClient() as client:
        params = {
            "pageNum": page_num,
            "pageSize": page_size,
            "id": id,
            "name": name,
            "categoryId": category_id,
            "status": status,
            "minPrice": min_price,
            "maxPrice": max_price,
        }
        response = await client.get(url="/agent/admin/product/list", params=params)
        return HttpResponse.parse_data(response)


@tool(
    args_schema=ProductDetailRequest,
    description=(
            "根据商品 ID 数组查询商品详情。"
            "适用于查看商品基础信息、价格、库存和上下架状态。"
    ),
)
@tool_thinking_redaction(display_name="获取商品详情")
@tool_call_status(
    tool_name="获取商品详情",
    start_message="正在查询商品详情",
    error_message="获取商品详情失败",
    timely_message="商品详情正在持续处理中",
)
async def product_detail(product_id: list[str]) -> dict:
    """
    功能描述：
        根据商品 ID 数组查询商品详情。

    参数说明：
        product_id (list[str]): 商品 ID 字符串数组。

    返回值：
        dict: 商品详情数据。

    异常说明：
        ValueError: 当 `product_id` 非法时抛出。
    """

    normalized_ids = normalize_id_list(product_id, field_name="product_id")
    ids_str = format_ids_to_string(normalized_ids)
    async with HttpClient() as client:
        response = await client.get(url=f"/agent/admin/product/{ids_str}")
        return HttpResponse.parse_data(response)


@tool(
    args_schema=DrugDetailRequest,
    description=(
            "根据药品商品 ID 数组查询药品详情。"
            "适用于查看说明书、适应症、用法用量和注意事项。"
    ),
)
@tool_thinking_redaction(display_name="获取药品详情")
@tool_call_status(
    tool_name="获取药品详情",
    start_message="正在查询药品详情",
    error_message="获取药品详情失败",
    timely_message="药品详情正在持续处理中",
)
async def drug_detail(product_id: list[str]) -> dict:
    """
    功能描述：
        根据药品商品 ID 数组查询药品详情。

    参数说明：
        product_id (list[str]): 药品商品 ID 字符串数组。

    返回值：
        dict: 药品详情数据。

    异常说明：
        ValueError: 当 `product_id` 非法时抛出。
    """

    normalized_ids = normalize_id_list(product_id, field_name="product_id")
    ids_str = format_ids_to_string(normalized_ids)
    async with HttpClient() as client:
        response = await client.get(url=f"/agent/admin/product/drug/{ids_str}")
        return HttpResponse.parse_data(response)


__all__ = [
    "DrugDetailRequest",
    "ProductDetailRequest",
    "ProductListRequest",
    "drug_detail",
    "product_detail",
    "product_list",
]
