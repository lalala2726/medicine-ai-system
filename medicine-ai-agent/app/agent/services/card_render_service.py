from __future__ import annotations

from decimal import Decimal, InvalidOperation, ROUND_HALF_UP
from typing import Any

from app.agent.services.card_render_schema import (
    ProductCardData,
    ProductCardItem,
    ProductCardProduct,
    ProductCardResponseData,
    ProductPurchaseCardData,
    ProductPurchaseCardItem,
    ProductPurchaseCardProduct,
    ProductPurchaseCardRequestItem,
    ProductPurchaseCardResponseData,
)
from app.schemas.sse_response import Card
from app.utils.http_client import HttpClient

# 统一金额展示精度，所有卡片价格最终都会量化为两位小数字符串。
_MONEY_PRECISION = Decimal("0.00")


def _format_money(value: Decimal | int | float | str) -> str:
    """
    功能描述：
        将不同来源的价格值统一格式化为两位小数字符串，供前端卡片展示使用。

    参数说明：
        value (Decimal | int | float | str): 原始价格值，支持 Decimal、整数、浮点数或字符串。

    返回值：
        str: 两位小数的金额字符串；当输入无法转换为数字时，返回 `"0.00"`。

    异常说明：
        无。非法数值会被内部兜底为 0，不向外抛出异常。
    """

    try:
        normalized_value = Decimal(str(value))
    except (InvalidOperation, TypeError, ValueError):
        normalized_value = Decimal("0")
    return str(normalized_value.quantize(_MONEY_PRECISION, rounding=ROUND_HALF_UP))


def _build_product_card_url(product_ids: list[int]) -> str:
    """
    功能描述：
        根据商品 ID 列表拼接推荐商品卡片补全接口地址。

    参数说明：
        product_ids (list[int]): 需要查询的商品 ID 列表，顺序与调用方传入顺序保持一致。

    返回值：
        str: 业务端推荐商品卡片接口相对路径，例如
        `"/agent/client/card/product/101,205,309"`。

    异常说明：
        无。
    """

    normalized_ids = ",".join(str(product_id) for product_id in product_ids)
    return f"/agent/client/card/product/{normalized_ids}"


def _map_product_items(
        *,
        product_ids: list[int],
        items: list[ProductCardItem],
) -> list[ProductCardProduct]:
    """
    功能描述：
        将业务端返回的推荐商品明细映射为前端商品卡片结构，并按请求商品 ID 顺序输出。

    参数说明：
        product_ids (list[int]): 原始请求的商品 ID 列表，用于控制前端卡片商品排序。
        items (list[ProductCardItem]): 业务端返回的商品明细列表。

    返回值：
        list[ProductCardProduct]: 可直接写入前端商品卡片的商品列表。
        若某个商品 ID 在业务端返回中缺失，则该商品会被跳过。

    异常说明：
        无。
    """

    items_by_id: dict[str, ProductCardItem] = {}
    for item in items:
        item_id = item.id.strip()
        if item_id and item_id not in items_by_id:
            items_by_id[item_id] = item

    products: list[ProductCardProduct] = []
    for product_id in product_ids:
        item = items_by_id.get(str(product_id))
        if item is None:
            continue
        products.append(
            ProductCardProduct(
                id=item.id,
                name=item.name,
                image=item.image,
                price=_format_money(item.price),
            )
        )
    return products


def _normalize_purchase_card_request_items(
        items: list[ProductPurchaseCardRequestItem | dict[str, Any]],
) -> list[ProductPurchaseCardRequestItem]:
    """
    功能描述：
        统一规范商品购买卡片请求项，兼容已经构造好的模型对象和原始字典输入。

    参数说明：
        items (list[ProductPurchaseCardRequestItem | dict[str, Any]]):
            商品购买项列表；元素可以是 `ProductPurchaseCardRequestItem`
            或待校验的原始字典。

    返回值：
        list[ProductPurchaseCardRequestItem]: 经过 Pydantic 校验后的标准购买项列表。

    异常说明：
        pydantic.ValidationError: 当字典结构缺字段、字段类型错误或数值不满足约束时抛出。
    """

    return [
        item
        if isinstance(item, ProductPurchaseCardRequestItem)
        else ProductPurchaseCardRequestItem.model_validate(item)
        for item in items
    ]


def _map_product_purchase_items(
        *,
        request_items: list[ProductPurchaseCardRequestItem],
        items: list[ProductPurchaseCardItem],
) -> tuple[list[ProductPurchaseCardProduct], str]:
    """
    功能描述：
        将业务端返回的购买商品明细映射为前端购买确认卡片结构，并计算展示总价。

    参数说明：
        request_items (list[ProductPurchaseCardRequestItem]): 原始购买请求项列表，
            用于保留前端展示顺序和购买数量。
        items (list[ProductPurchaseCardItem]): 业务端返回的商品明细列表。

    返回值：
        tuple[list[ProductPurchaseCardProduct], str]:
            第一个元素为前端购买卡片商品列表，
            第二个元素为基于成功匹配商品计算出的两位小数字符串总价。
            对于业务端未返回的商品，将跳过且不计入总价。

    异常说明：
        无。
    """

    items_by_id: dict[str, ProductPurchaseCardItem] = {}
    for item in items:
        item_id = item.id.strip()
        if item_id and item_id not in items_by_id:
            items_by_id[item_id] = item

    products: list[ProductPurchaseCardProduct] = []
    total_price = Decimal("0")
    for request_item in request_items:
        item = items_by_id.get(str(request_item.productId))
        if item is None:
            continue
        products.append(
            ProductPurchaseCardProduct(
                id=item.id,
                name=item.name,
                image=item.image,
                price=_format_money(item.price),
                quantity=request_item.quantity,
            )
        )
        total_price += item.price * request_item.quantity

    return products, _format_money(total_price)


async def render_product_card(product_ids: list[int]) -> Card | None:
    """
    功能描述：
        请求业务端补全推荐商品信息，并渲染为前端可直接消费的推荐商品卡片。

    参数说明：
        product_ids (list[int]): 需要展示的商品 ID 列表。

    返回值：
        Card | None:
            当成功获取到至少一个有效商品时，返回 `type="product-card"` 的卡片对象；
            当入参为空或业务端未返回任何可展示商品时，返回 `None`。

    异常说明：
        pydantic.ValidationError: 当业务端返回结构不符合 `ProductCardResponseData` 约束时抛出。
        Exception: `HttpClient` 请求异常等底层错误会继续向上抛出，由调用方处理。
    """

    if not product_ids:
        return None

    async with HttpClient() as client:
        # TODO(Chuang): 业务端卡片接口完成系统内部认证改造后，这里补充 use_system_signature=True。
        payload: Any = await client.get(
            _build_product_card_url(product_ids),
            response_format="json",
        )

    response_data = ProductCardResponseData.model_validate(payload)
    products = _map_product_items(
        product_ids=product_ids,
        items=response_data.items,
    )
    if not products:
        return None

    card_data = ProductCardData(products=products)
    return Card(
        type="product-card",
        data=card_data.model_dump(mode="json"),
    )


async def render_product_purchase_card(
        items: list[ProductPurchaseCardRequestItem | dict[str, Any]],
) -> Card | None:
    """
    功能描述：
        请求业务端补全购买商品信息，并渲染为前端可直接消费的购买确认卡片。

    参数说明：
        items (list[ProductPurchaseCardRequestItem | dict[str, Any]]):
            商品购买项列表，包含商品 ID 与购买数量；支持传入模型对象或原始字典。

    返回值：
        Card | None:
            当成功获取到至少一个可展示商品时，返回 `type="product-purchase-card"` 的卡片对象；
            当入参为空或业务端未返回任何可展示商品时，返回 `None`。

    异常说明：
        pydantic.ValidationError:
            当请求项校验失败，或业务端返回结构不符合 `ProductPurchaseCardResponseData`
            约束时抛出。
        Exception: `HttpClient` 请求异常等底层错误会继续向上抛出，由调用方处理。
    """

    request_items = _normalize_purchase_card_request_items(items)
    if not request_items:
        return None

    async with HttpClient() as client:
        # TODO(Chuang): 业务端卡片接口完成系统内部认证改造后，这里补充 use_system_signature=True。
        payload: Any = await client.post(
            "/agent/client/card/purchase_cards",
            json={
                "items": [
                    item.model_dump(mode="json")
                    for item in request_items
                ]
            },
            response_format="json",
        )

    response_data = ProductPurchaseCardResponseData.model_validate(payload)
    products, total_price = _map_product_purchase_items(
        request_items=request_items,
        items=response_data.items,
    )
    if not products:
        return None

    card_data = ProductPurchaseCardData(
        products=products,
        total_price=total_price,
    )
    return Card(
        type="product-purchase-card",
        data=card_data.model_dump(mode="json"),
    )


__all__ = [
    "render_product_card",
    "render_product_purchase_card",
]
