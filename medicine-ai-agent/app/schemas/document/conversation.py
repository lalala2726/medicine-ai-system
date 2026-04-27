from __future__ import annotations

from datetime import datetime
from enum import Enum
from typing import Any

from bson import ObjectId
from bson.int64 import Int64
from pydantic import BaseModel, ConfigDict, Field, field_serializer, field_validator


class ConversationType(str, Enum):
    """会话类型。"""

    CLIENT = "client"
    ADMIN = "admin"


class ConversationCreate(BaseModel):
    """新增会话入参模型（服务层内部使用）。"""

    model_config = ConfigDict(extra="forbid", arbitrary_types_allowed=True)

    uuid: str = Field(..., min_length=1, description="会话业务唯一ID")
    conversation_type: ConversationType = Field(..., description="会话类型")
    user_id: int = Field(..., ge=1, description="用户ID（int64）")
    title: str = Field(default="新聊天", description="会话标题")
    create_time: datetime = Field(default_factory=datetime.now, description="创建时间")
    update_time: datetime = Field(default_factory=datetime.now, description="更新时间")
    is_deleted: int = Field(default=0, ge=0, le=1, description="逻辑删除标记：0未删除，1已删除")

    @field_serializer("user_id", when_used="always")
    def _serialize_user_id(self, value: int) -> Int64:
        """确保写入 Mongo 时 user_id 为 bson int64。"""

        return Int64(value)


class ConversationListItem(BaseModel):
    """会话列表响应项。"""

    model_config = ConfigDict(extra="forbid")

    conversation_uuid: str = Field(..., min_length=1, description="会话 UUID")
    title: str = Field(..., min_length=1, description="会话标题")


class ConversationUpdateSet(BaseModel):
    """会话更新 `$set` 负载。"""

    model_config = ConfigDict(extra="forbid")

    update_time: datetime = Field(..., description="更新时间")
    title: str | None = Field(default=None, min_length=1, description="会话标题")
    is_deleted: int | None = Field(default=None, ge=0, le=1, description="逻辑删除标记")


class ConversationDocument(BaseModel):
    """MongoDB conversations 文档模型。"""

    model_config = ConfigDict(extra="ignore", populate_by_name=True)

    id: str | None = Field(default=None, alias="_id", description="MongoDB 主键")
    uuid: str = Field(..., description="会话业务唯一ID")
    conversation_type: ConversationType = Field(..., description="会话类型")
    user_id: int = Field(..., description="用户ID（int64）")
    title: str | None = Field(default=None, description="会话标题")
    create_time: datetime = Field(..., description="创建时间")
    update_time: datetime = Field(..., description="更新时间")
    is_deleted: int = Field(default=0, ge=0, le=1, description="逻辑删除标记：0未删除，1已删除")
    metadata: dict[str, Any] | None = Field(default=None, description="扩展信息")

    @field_validator("id", mode="before")
    @classmethod
    def _normalize_object_id(cls, value: Any) -> str | None:
        """把 Mongo ObjectId 统一转换为字符串，避免上层重复处理。"""

        if value is None:
            return None
        if isinstance(value, ObjectId):
            return str(value)
        return str(value)
