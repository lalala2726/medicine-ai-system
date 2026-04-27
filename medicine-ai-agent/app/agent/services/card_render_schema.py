from __future__ import annotations

from decimal import Decimal

from pydantic import BaseModel, ConfigDict, Field

_DEFAULT_PRODUCT_CARD_TITLE = "为您推荐以下商品"
_DEFAULT_PRODUCT_PURCHASE_CARD_TITLE = "请确认要购买的商品"


class PurchaseCardFieldMeta(BaseModel):
    """商品卡片接口返回的字段说明元数据。"""

    model_config = ConfigDict(extra="ignore")

    entityDescription: str | None = Field(default=None, description="实体说明")
    fieldDescriptions: dict[str, str] = Field(
        default_factory=dict,
        description="字段语义说明",
    )


class BaseProductCardItem(BaseModel):
    """商品卡片上游接口中的公共商品字段。"""

    model_config = ConfigDict(extra="ignore")

    id: str = Field(..., description="商品 ID")
    name: str = Field(..., description="商品名称")
    image: str = Field(..., description="商品主图")
    price: Decimal = Field(default=Decimal("0.00"), description="商品销售价")


class ProductCardItem(BaseProductCardItem):
    """推荐商品卡接口中的单个商品项。"""

    model_config = ConfigDict(extra="ignore")

    spec: str | None = Field(default=None, description="商品规格")
    efficacy: str | None = Field(default=None, description="功效/适应症")
    prescription: bool | None = Field(default=None, description="是否处方药")
    stock: int | None = Field(default=None, description="库存")


class ProductCardResponseData(BaseModel):
    """推荐商品卡接口业务数据。"""

    model_config = ConfigDict(extra="ignore")

    totalPrice: Decimal = Field(default=Decimal("0.00"), description="商品总价")
    items: list[ProductCardItem] = Field(
        default_factory=list,
        description="商品卡片列表",
    )
    meta: PurchaseCardFieldMeta | None = Field(
        default=None,
        description="字段语义元数据",
    )


class ProductCardProduct(BaseModel):
    """前端推荐商品卡片中的单个商品项。"""

    model_config = ConfigDict(extra="forbid")

    id: str = Field(..., description="商品 ID")
    name: str = Field(..., description="商品名称")
    image: str = Field(..., description="商品主图")
    price: str = Field(..., description="商品销售价")


class ProductCardData(BaseModel):
    """前端推荐商品卡片数据。"""

    model_config = ConfigDict(extra="forbid")

    title: str = Field(
        default=_DEFAULT_PRODUCT_CARD_TITLE,
        description="商品卡片标题",
    )
    products: list[ProductCardProduct] = Field(
        default_factory=list,
        description="商品展示列表",
    )


class ProductPurchaseCardRequestItem(BaseModel):
    """商品购买卡片请求中的单个购买项。"""

    model_config = ConfigDict(extra="forbid")

    productId: int = Field(..., gt=0, description="商品 ID，必须大于 0")
    quantity: int = Field(..., gt=0, description="购买数量，必须大于 0")


class ProductPurchaseCardItem(BaseProductCardItem):
    """商品购买卡片接口中的单个商品项。"""

    model_config = ConfigDict(extra="ignore")

    quantity: int = Field(..., gt=0, description="购买数量")
    spec: str | None = Field(default=None, description="商品规格")
    efficacy: str | None = Field(default=None, description="功效/适应症")
    prescription: bool | None = Field(default=None, description="是否处方药")
    stock: int | None = Field(default=None, description="库存")


class ProductPurchaseCardResponseData(BaseModel):
    """商品购买卡片接口业务数据。"""

    model_config = ConfigDict(extra="ignore")

    totalPrice: Decimal = Field(default=Decimal("0.00"), description="商品总价")
    items: list[ProductPurchaseCardItem] = Field(
        default_factory=list,
        description="商品购买卡片列表",
    )
    meta: PurchaseCardFieldMeta | None = Field(
        default=None,
        description="字段语义元数据",
    )


class ProductPurchaseCardProduct(BaseModel):
    """前端商品购买卡片中的单个商品项。"""

    model_config = ConfigDict(extra="forbid")

    id: str = Field(..., description="商品 ID")
    name: str = Field(..., description="商品名称")
    image: str = Field(..., description="商品主图")
    price: str = Field(..., description="商品销售价")
    quantity: int = Field(..., gt=0, description="购买数量")


class ProductPurchaseCardData(BaseModel):
    """前端商品购买卡片数据。"""

    model_config = ConfigDict(extra="forbid")

    title: str = Field(
        default=_DEFAULT_PRODUCT_PURCHASE_CARD_TITLE,
        description="商品购买卡片标题",
    )
    products: list[ProductPurchaseCardProduct] = Field(
        default_factory=list,
        description="商品购买展示列表",
    )
    total_price: str = Field(default="0.00", description="当前展示商品总价")


__all__ = [
    "BaseProductCardItem",
    "ProductCardData",
    "ProductCardItem",
    "ProductCardProduct",
    "ProductCardResponseData",
    "ProductPurchaseCardData",
    "ProductPurchaseCardItem",
    "ProductPurchaseCardProduct",
    "ProductPurchaseCardRequestItem",
    "ProductPurchaseCardResponseData",
    "PurchaseCardFieldMeta",
]
