from __future__ import annotations

from pydantic import BaseModel, ConfigDict, Field, field_validator


class AfterSaleNoRequest(BaseModel):
    """
    功能描述：
        按售后单号查询的请求参数模型。

    参数说明：
        after_sale_no (str): 售后单号。

    返回值：
        无（数据模型定义）。

    异常说明：
        ValueError: 当 `after_sale_no` 为空时抛出。
    """

    model_config = ConfigDict(extra="forbid")

    after_sale_no: str = Field(..., min_length=1, description="售后单号")

    @field_validator("after_sale_no")
    @classmethod
    def normalize_after_sale_no(cls, value: str) -> str:
        """
        功能描述：
            规范化售后单号。

        参数说明：
            value (str): 原始售后单号。

        返回值：
            str: 去除首尾空白后的售后单号。

        异常说明：
            ValueError: 当售后单号为空时抛出。
        """

        normalized = value.strip()
        if not normalized:
            raise ValueError("after_sale_no 不能为空")
        return normalized


class AfterSaleEligibilityRequest(BaseModel):
    """
    功能描述：
        售后资格校验请求参数模型。

    参数说明：
        order_no (str): 订单编号。
        order_item_id (int | None): 订单项 ID。

    返回值：
        无（数据模型定义）。

    异常说明：
        ValueError: 当 `order_no` 为空时抛出。
    """

    model_config = ConfigDict(extra="forbid")

    order_no: str = Field(..., min_length=1, description="订单编号")
    order_item_id: int | None = Field(
        default=None,
        ge=1,
        description="订单项 ID，不传表示校验整单",
    )

    @field_validator("order_no")
    @classmethod
    def normalize_order_no(cls, value: str) -> str:
        """
        功能描述：
            规范化订单编号。

        参数说明：
            value (str): 原始订单编号。

        返回值：
            str: 去除首尾空白后的订单编号。

        异常说明：
            ValueError: 当订单编号为空时抛出。
        """

        normalized = value.strip()
        if not normalized:
            raise ValueError("order_no 不能为空")
        return normalized
__all__ = [
    "AfterSaleEligibilityRequest",
    "AfterSaleNoRequest",
]
