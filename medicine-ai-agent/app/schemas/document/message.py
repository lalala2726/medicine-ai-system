from __future__ import annotations

from datetime import datetime
from enum import Enum
from typing import Any

from bson import ObjectId
from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator


class MessageRole(str, Enum):
    """会话消息角色。"""

    USER = "user"
    AI = "ai"


class MessageStatus(str, Enum):
    """会话消息状态。"""

    STREAMING = "streaming"
    WAITING_INPUT = "waiting_input"
    SUCCESS = "success"
    CANCELLED = "cancelled"
    ERROR = "error"


class MessageCard(BaseModel):
    """
    消息内挂载的结构化卡片。

    用途：
    - 作为 AI 历史消息的结构化附加内容；
    - 直接承载最终给前端渲染的卡片数据；
    - 不保存实时 SSE 外壳，仅保留 `id + type + data`。
    """

    model_config = ConfigDict(extra="forbid")

    id: str = Field(..., min_length=1, description="卡片唯一 ID")
    type: str = Field(..., min_length=1, description="卡片类型")
    data: dict[str, Any] = Field(default_factory=dict, description="卡片数据")


class MessageCreate(BaseModel):
    """新增会话消息入参模型（服务层内部使用）。"""

    model_config = ConfigDict(extra="forbid")

    uuid: str = Field(..., min_length=1, description="消息业务唯一ID（UUID）")
    conversation_id: str = Field(..., min_length=1, description="所属会话ID")
    role: MessageRole = Field(..., description="消息角色")
    status: MessageStatus = Field(default=MessageStatus.SUCCESS, description="消息状态")
    content: str = Field(..., description="消息内容")
    thinking: str | None = Field(default=None, description="AI深度思考完整文本")
    cards: list[MessageCard] | None = Field(default=None, description="消息卡片列表")
    card_uuids: list[str] | None = Field(default=None, description="消息卡片 UUID 列表")
    hidden_card_uuids: list[str] | None = Field(default=None, description="已点击后需隐藏的卡片 UUID 列表")
    history_hidden: bool = Field(default=False, description="是否从客户端历史与后续上下文中隐藏")

    @model_validator(mode="after")
    def _validate_content_and_cards(self) -> "MessageCreate":
        """
        校验消息内容与卡片的组合约束。

        规则：
        - user 消息至少需要有非空 content 或至少一张 cards；
        - user 消息仅包含 cards 时，会把 content 统一归一化为空字符串；
        - ai 消息在 `status=streaming` 时允许 `content=""`；
        - ai 消息允许 content 为空字符串，但必须至少有一项 cards；
        - 当 ai 消息仅包含卡片时，会把 content 统一归一化为空字符串。

        Returns:
            MessageCreate: 校验并归一化后的消息创建模型自身。

        Raises:
            ValueError: 当 user/ai 消息内容与 cards 同时为空时抛出。
        """

        normalized_content = self.content.strip()
        has_cards = bool(self.cards)

        if self.role == MessageRole.USER:
            if not normalized_content and has_cards:
                self.content = ""
                return self
            if not normalized_content:
                raise ValueError("用户消息内容和卡片不能同时为空")
            self.content = normalized_content
            return self

        if self.status == MessageStatus.STREAMING:
            self.content = normalized_content
            return self

        if not normalized_content and has_cards:
            self.content = ""
            return self

        if not normalized_content:
            raise ValueError("AI 消息内容和卡片不能同时为空")

        return self


class MessageDocument(BaseModel):
    """MongoDB messages 文档模型。"""

    model_config = ConfigDict(extra="ignore", populate_by_name=True)

    id: str | None = Field(default=None, alias="_id", description="MongoDB 主键")
    uuid: str = Field(..., description="消息业务唯一ID（UUID）")
    conversation_id: str = Field(..., description="所属会话ID")
    role: MessageRole = Field(..., description="消息角色")
    status: MessageStatus = Field(default=MessageStatus.SUCCESS, description="消息状态")
    content: str = Field(..., description="消息内容")
    thinking: str | None = Field(default=None, description="AI深度思考完整文本")
    cards: list[MessageCard] | None = Field(default=None, description="消息卡片列表")
    card_uuids: list[str] | None = Field(default=None, description="消息卡片 UUID 列表")
    hidden_card_uuids: list[str] | None = Field(default=None, description="已点击后需隐藏的卡片 UUID 列表")
    history_hidden: bool = Field(default=False, description="是否从客户端历史与后续上下文中隐藏")
    created_at: datetime = Field(..., description="创建时间")
    updated_at: datetime = Field(..., description="更新时间")

    @field_validator("id", mode="before")
    @classmethod
    def _normalize_message_id(cls, value: Any) -> str | None:
        """把 Mongo ObjectId 统一转换为字符串。"""

        if value is None:
            return None
        if isinstance(value, ObjectId):
            return str(value)
        return str(value)

    @field_validator("conversation_id", mode="before")
    @classmethod
    def _normalize_conversation_id(cls, value: Any) -> str:
        """把 conversation_id 转为字符串，便于接口层直接透传。"""

        if isinstance(value, ObjectId):
            return str(value)
        return str(value)
