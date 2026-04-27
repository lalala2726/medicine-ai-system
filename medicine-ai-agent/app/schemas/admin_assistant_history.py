from __future__ import annotations

from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field


class ConversationMessagesRequest(BaseModel):
    """管理助手历史消息分页请求参数。"""

    model_config = ConfigDict(extra="forbid")

    page_num: int = Field(default=1, ge=1, description="页号")
    page_size: int = Field(default=50, ge=1, le=50, description="每页大小")


class ThoughtStepResponse(BaseModel):
    """思维链子步骤。"""

    model_config = ConfigDict(extra="forbid")

    id: str = Field(..., min_length=1, description="步骤唯一ID")
    message: str = Field(..., min_length=1, description="步骤描述")
    name: str | None = Field(default=None, description="工具名称")
    arguments: str | None = Field(default=None, description="工具参数 JSON 字符串")
    status: Literal["success", "error"] = Field(..., description="步骤状态")


class ThoughtNodeResponse(BaseModel):
    """思维链顶层节点。"""

    model_config = ConfigDict(extra="forbid")

    id: str = Field(..., min_length=1, description="节点唯一ID")
    node: str = Field(..., min_length=1, description="节点标识")
    message: str = Field(..., min_length=1, description="节点描述")
    status: Literal["success", "error"] = Field(..., description="节点状态")
    children: list[ThoughtStepResponse] = Field(default_factory=list, description="子步骤列表")


class ConversationCardResponse(BaseModel):
    """
    历史消息中的结构化卡片。

    用途：
    - 作为历史消息接口中 `cards[]` 的统一元素结构；
    - 与消息文档中的 `MessageCard` 保持一致；
    - 直接承载前端恢复渲染所需的卡片数据。
    """

    model_config = ConfigDict(extra="forbid")

    card_uuid: str = Field(..., min_length=1, description="卡片唯一 UUID")
    type: str = Field(..., min_length=1, description="卡片类型")
    data: dict[str, Any] = Field(default_factory=dict, description="卡片数据")


class ConversationMessageResponse(BaseModel):
    """管理助手历史消息响应项。"""

    model_config = ConfigDict(extra="forbid")

    id: str = Field(..., min_length=1, description="消息唯一ID")
    role: Literal["user", "ai"] = Field(..., description="消息发送方")
    content: str = Field(..., description="消息内容")
    thinking: str | None = Field(default=None, description="AI深度思考完整文本")
    status: Literal["streaming", "waiting_input", "success", "cancelled", "error"] | None = Field(
        default=None,
        description="消息状态",
    )
    cards: list[ConversationCardResponse] | None = Field(
        default=None,
        description="结构化卡片列表",
    )
    thought_chain: list[ThoughtNodeResponse] | None = Field(
        default=None,
        serialization_alias="thoughtChain",
        description="思维链",
    )
