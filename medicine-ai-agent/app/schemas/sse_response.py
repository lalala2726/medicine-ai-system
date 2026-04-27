import time
from enum import Enum
from typing import Any, Literal

from pydantic import AliasChoices, BaseModel, Field


class Content(BaseModel):
    text: str | None = Field(default=None, description="文本")
    node: str | None = Field(default=None, description="节点")
    parent_node: str | None = Field(default=None, description="所属节点")
    state: str | None = Field(default=None, description="状态")
    message: str | None = Field(default=None, description="消息")
    result: str | None = Field(default=None, description="结果")
    name: str | None = Field(default=None, description="名称")
    arguments: str | None = Field(default=None, description="参数")


OrderStatusValue = Literal[
    "PENDING_PAYMENT",
    "PENDING_SHIPMENT",
    "PENDING_RECEIPT",
    "COMPLETED",
    "CANCELLED",
]
AfterSaleStatusValue = Literal[
    "PENDING",
    "APPROVED",
    "REJECTED",
    "PROCESSING",
    "COMPLETED",
    "CANCELLED",
]


class UserOrderListPayload(BaseModel):
    orderStatus: OrderStatusValue | None = Field(
        default=None,
        description="订单状态过滤",
    )


class UserAfterSaleListPayload(BaseModel):
    afterSaleStatus: AfterSaleStatusValue | None = Field(
        default=None,
        description="售后状态过滤",
    )


class UserPatientListPayload(BaseModel):
    """打开用户就诊人列表时的动作参数。"""


class Action(BaseModel):
    type: Literal["navigate"] = Field(default="navigate", description="动作类型")
    target: Literal["user_order_list", "user_after_sale_list", "user_patient_list"] = Field(
        default="user_order_list",
        description="动作目标",
    )
    payload: UserOrderListPayload | UserAfterSaleListPayload | UserPatientListPayload = Field(
        default_factory=UserOrderListPayload,
        description="动作参数",
    )
    priority: int = Field(default=0, description="优先级，数值越大越先发送")


class Card(BaseModel):
    """前端卡片通用传输结构。"""

    type: str = Field(..., min_length=1, description="卡片类型")
    data: dict[str, Any] = Field(
        default_factory=dict,
        description="卡片数据",
    )


class MessageType(str, Enum):
    ANSWER = "answer"
    THINKING = "thinking"
    FUNCTION_CALL = "function_call"
    TOOL_RESPONSE = "tool_response"
    STATUS = "status"
    NOTICE = "notice"
    ACTION = "action"
    CARD = "card"


class AssistantResponse(BaseModel):
    """AI助手SSE响应参数"""

    content: Content = Field(default_factory=Content, description="内容")
    type: MessageType = Field(default=MessageType.ANSWER, description="类型")
    action: Action | None = Field(default=None, description="前端动作")
    card: Card | None = Field(default=None, description="前端卡片")
    meta: dict[str, Any] | None = Field(
        default=None,
        validation_alias=AliasChoices("meta", "extra"),
        description="元数据（如会话ID、追踪ID等扩展信息）",
    )
    is_end: bool = Field(default=False, description="是否结束")
    timestamp: int = Field(
        default_factory=lambda: int(time.time() * 1000), description="时间戳（毫秒）"
    )
