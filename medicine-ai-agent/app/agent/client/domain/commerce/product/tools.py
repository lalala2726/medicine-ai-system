"""
客户端 commerce 商品工具。
"""

from __future__ import annotations

from langchain_core.tools import tool

from app.agent.client.domain.commerce.product.schemas import (
    ProductIdsRequest,
    ProductSearchRequest,
)
from app.core.agent.middleware import (
    tool_call_status,
    tool_thinking_redaction,
)
from app.schemas.http_response import HttpResponse
from app.utils.http_client import HttpClient

# 商品统一详情接口路径。
PRODUCT_DETAILS_URL = "/agent/client/product/details"


@tool(
    args_schema=ProductSearchRequest,
    description=(
            "搜索商品。"
            "调用时机：用户想找某类商品、按用途挑选商品、按关键词搜索商品时。"
    ),
)
@tool_thinking_redaction(display_name="商品搜索")
@tool_call_status(
    tool_name="商品搜索",
    start_message="正在搜索候选药品",
    error_message="商品搜索失败",
    timely_message="商品搜索仍在处理中",
)
async def search_products(
        keyword: str | None = None,
        category_name: str | None = None,
        usage: str | None = None,
        tag_names: list[str] | None = None,
        page_num: int = 1,
        page_size: int = 10,
) -> dict:
    """
    功能描述：
        搜索客户端商品摘要列表。

    参数说明：
        keyword (str | None): 商品关键词。
        category_name (str | None): 商品分类名称。
        usage (str | None): 商品用途或适用场景。
        tag_names (list[str] | None): 商品标签名称列表，辅助筛选。
        page_num (int): 页码。
        page_size (int): 每页数量。

    返回值：
        dict: 商品搜索结果分页数据；`rows` 仅包含供筛选候选药品的摘要字段。

    异常说明：
        无；底层 HTTP 异常由上层抛出。
    """

    params: dict = {
        "keyword": keyword,
        "categoryName": category_name,
        "usage": usage,
        "pageNum": page_num,
        "pageSize": page_size,
    }
    # Spring 接收 List 参数需要重复 key 形式: tagNames=A&tagNames=B
    query_params: list[tuple[str, str | int]] = [
        (k, v) for k, v in params.items() if v is not None
    ]
    if tag_names:
        for name in tag_names:
            query_params.append(("tagNames", name))

    async with HttpClient() as client:
        response = await client.get(
            url="/agent/client/product/search",
            params=query_params,
        )
        return HttpResponse.parse_data(response)


@tool(
    args_schema=ProductIdsRequest,
    description=(
            "批量获取统一药品详情。"
            "调用时机：已经通过搜索确定候选药品，或用户明确给出商品 ID，"
            "需要进一步查询价格、库存、禁忌、说明书等完整药品资料时。"
    ),
)
@tool_thinking_redaction(display_name="药品详情查询")
@tool_call_status(
    tool_name="药品详情查询",
    start_message="正在查询药品详情",
    error_message="药品详情查询失败",
    timely_message="药品详情仍在处理中",
)
async def get_product_details(product_ids: list[int]) -> list[dict]:
    """
    功能描述：
        批量获取客户端统一药品详情。

    参数说明：
        product_ids (list[int]): 商品 ID 列表。

    返回值：
        list[dict]: 统一药品详情列表。

    异常说明：
        无；底层 HTTP 异常由上层抛出。
    """

    request = ProductIdsRequest.model_validate({"product_ids": product_ids})
    async with HttpClient() as client:
        response = await client.post(
            url=PRODUCT_DETAILS_URL,
            json={
                "productIds": request.product_ids,
            },
        )
        return HttpResponse.parse_data(response)


__all__ = [
    "PRODUCT_DETAILS_URL",
    "get_product_details",
    "search_products",
]
