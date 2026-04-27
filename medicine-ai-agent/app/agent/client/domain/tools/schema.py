from __future__ import annotations

from typing import Annotated

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

from app.agent.services.card_render_schema import ProductPurchaseCardRequestItem
from app.utils.list_utils import TextListUtils

# 商品卡工具只接受正整数商品 ID。
ProductIdValue = Annotated[int, Field(ge=1, description="商品 ID，必须为正整数")]


def _normalize_required_text(value: str, *, field_name: str) -> str:
    """去除首尾空白，并确保必填文本不为空。"""

    normalized = value.strip()
    if not normalized:
        raise ValueError(f"{field_name} 不能为空")
    return normalized


def _normalize_optional_text(value: str | None) -> str | None:
    """去除可选文本的首尾空白，空串统一归一化为 None。"""

    if value is None:
        return None
    normalized = value.strip()
    return normalized or None


class ConsentCardAction(BaseModel):
    """同意/拒绝卡片中的按钮数据。"""

    model_config = ConfigDict(extra="forbid")

    label: str = Field(..., min_length=1, description="按钮展示文案")
    value: str = Field(..., min_length=1, description="按钮实际回传值")


class ConsentCardData(BaseModel):
    """同意/拒绝卡片数据。"""

    model_config = ConfigDict(extra="forbid")

    title: str = Field(..., min_length=1, description="卡片标题")
    description: str | None = Field(default=None, description="卡片描述")
    confirm: ConsentCardAction = Field(..., description="同意按钮")
    reject: ConsentCardAction = Field(..., description="拒绝按钮")


class SendConsentCardRequest(BaseModel):
    """发送同意/拒绝卡片工具参数。"""

    model_config = ConfigDict(
        extra="forbid",
        json_schema_extra={
            "example": {
                "title": "是否同意本次处理方案？",
                "description": "同意后将继续下一步处理。",
                "confirm_text": "同意",
                "reject_text": "拒绝",
            }
        },
    )

    title: str = Field(..., min_length=1, description="卡片标题。")
    description: str | None = Field(default=None, description="卡片描述，可选。")
    confirm_text: str = Field(
        default="同意",
        min_length=1,
        description="同意按钮文案；后端会自动将 label 和 value 都设置为该值。",
    )
    reject_text: str = Field(
        default="拒绝",
        min_length=1,
        description="拒绝按钮文案；后端会自动将 label 和 value 都设置为该值。",
    )

    @field_validator("title", "confirm_text", "reject_text")
    @classmethod
    def _validate_required_text(cls, value: str, info) -> str:
        return _normalize_required_text(value, field_name=info.field_name)

    @field_validator("description")
    @classmethod
    def _validate_optional_text(cls, value: str | None) -> str | None:
        return _normalize_optional_text(value)

    @model_validator(mode="after")
    def _validate_buttons_distinct(self) -> "SendConsentCardRequest":
        if self.confirm_text == self.reject_text:
            raise ValueError("confirm_text 和 reject_text 不能相同")
        return self

    def to_card_data(self) -> ConsentCardData:
        return ConsentCardData(
            title=self.title,
            description=self.description,
            confirm=ConsentCardAction(
                label=self.confirm_text,
                value=self.confirm_text,
            ),
            reject=ConsentCardAction(
                label=self.reject_text,
                value=self.reject_text,
            ),
        )


class SelectionCardData(BaseModel):
    """选择卡片数据。"""

    model_config = ConfigDict(extra="forbid")

    title: str = Field(..., min_length=1, description="卡片标题")
    options: list[str] = Field(
        default_factory=list,
        description="选择项列表",
    )


class SendSelectionCardRequest(BaseModel):
    """发送选择卡片工具参数。"""

    model_config = ConfigDict(
        extra="forbid",
        json_schema_extra={
            "example": {
                "title": "请选择您需要处理的问题",
                "options": ["退款", "换货", "维修"],
            }
        },
    )

    title: str = Field(..., min_length=1, description="卡片标题。")
    options: list[str] = Field(
        ...,
        min_length=1,
        description="选项文案列表，会按原顺序直接透传给前端。",
    )

    @field_validator("title")
    @classmethod
    def _validate_required_text(cls, value: str, info) -> str:
        return _normalize_required_text(value, field_name=info.field_name)

    @field_validator("options")
    @classmethod
    def _validate_options(cls, value: list[str]) -> list[str]:
        return TextListUtils.normalize_unique_required(value, field_name="options")

    def to_card_data(self) -> SelectionCardData:
        return SelectionCardData(
            title=self.title,
            options=self.options,
        )


class SendProductCardRequest(BaseModel):
    """发送商品卡片工具参数。"""

    model_config = ConfigDict(extra="forbid")

    productIds: list[ProductIdValue] = Field(
        min_length=1,
        description="需要展示为推荐商品卡片的商品 ID 列表。",
    )


class SendProductPurchaseCardItem(ProductPurchaseCardRequestItem):
    """发送商品购买卡片时的单个商品项参数。"""

    model_config = ConfigDict(
        extra="forbid",
        json_schema_extra={
            "example": {
                "productId": 101,
                "quantity": 2,
            }
        },
    )

    productId: int = Field(
        ...,
        gt=0,
        description="待购买商品的商品 ID，必填，且必须大于 0。",
    )
    quantity: int = Field(
        ...,
        gt=0,
        description="该商品的购买数量，必填，且必须大于 0。",
    )


class SendProductPurchaseCardRequest(BaseModel):
    """发送商品购买卡片工具参数。"""

    model_config = ConfigDict(
        extra="forbid",
        json_schema_extra={
            "example": {
                "items": [
                    {
                        "productId": 101,
                        "quantity": 2,
                    },
                    {
                        "productId": 205,
                        "quantity": 1,
                    },
                ]
            }
        },
    )

    items: list[SendProductPurchaseCardItem] = Field(
        min_length=1,
        description="商品购买项列表，每个元素表示一个待确认购买的商品及其数量。",
    )


__all__ = [
    "ConsentCardAction",
    "ConsentCardData",
    "ProductIdValue",
    "SelectionCardData",
    "SendConsentCardRequest",
    "SendProductCardRequest",
    "SendProductPurchaseCardItem",
    "SendProductPurchaseCardRequest",
    "SendSelectionCardRequest",
]
