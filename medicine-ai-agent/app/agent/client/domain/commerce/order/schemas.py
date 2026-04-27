from __future__ import annotations

from pydantic import BaseModel, ConfigDict, Field, field_validator


class OrderNoRequest(BaseModel):
    """
    功能描述：
        按订单编号查询的请求参数模型。

    参数说明：
        order_no (str): 订单编号。

    返回值：
        无（数据模型定义）。

    异常说明：
        ValueError: 当 `order_no` 为空时抛出。
    """

    model_config = ConfigDict(extra="forbid")

    order_no: str = Field(..., min_length=1, description="订单编号")

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
    "OrderNoRequest",
]
