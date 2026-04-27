from __future__ import annotations

from pydantic import BaseModel, ConfigDict, Field

from app.schemas.sse_response import AfterSaleStatusValue, OrderStatusValue


class OpenUserOrderListRequest(BaseModel):
    """打开用户订单列表工具参数模型。"""

    model_config = ConfigDict(extra="forbid")

    orderStatus: OrderStatusValue | None = Field(
        default=None,
        description=(
            "订单状态，可选值："
            "PENDING_PAYMENT（待支付）、"
            "PENDING_SHIPMENT（待发货）、"
            "PENDING_RECEIPT（待收货）、"
            "COMPLETED（已完成）、"
            "CANCELLED（已取消）。"
        ),
    )


class OpenUserAfterSaleListRequest(BaseModel):
    """打开用户售后列表工具参数模型。"""

    model_config = ConfigDict(extra="forbid")

    afterSaleStatus: AfterSaleStatusValue | None = Field(
        default=None,
        description=(
            "售后状态，可选值："
            "PENDING（待审核）、"
            "APPROVED（已通过）、"
            "REJECTED（已拒绝）、"
            "PROCESSING（处理中）、"
            "COMPLETED（已完成）、"
            "CANCELLED（已取消）。"
        ),
    )


class OpenUserPatientListRequest(BaseModel):
    """打开用户就诊人列表工具参数模型。"""

    model_config = ConfigDict(extra="forbid")


__all__ = [
    "OpenUserAfterSaleListRequest",
    "OpenUserOrderListRequest",
    "OpenUserPatientListRequest",
]
