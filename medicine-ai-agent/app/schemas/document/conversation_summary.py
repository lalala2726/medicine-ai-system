from __future__ import annotations

from datetime import datetime
from typing import Any, Literal

from bson import ObjectId
from pydantic import BaseModel, ConfigDict, Field, field_validator


class ConversationSummaryUpdateSet(BaseModel):
    """
    功能描述：
        定义 `conversation_summaries` 集合执行 upsert 时 `$set` 段的结构。

    参数说明：
        无（模型字段由调用方赋值）。

    返回值：
        无（Pydantic 数据模型）。

    异常说明：
        当字段缺失或类型不合法时，Pydantic 会抛出校验异常。
    """

    model_config = ConfigDict(extra="forbid")

    summary_content: str = Field(..., min_length=1, description="总结内容")
    last_summarized_message_id: str | None = Field(default=None, description="最后一条已总结消息的Mongo主键字符串")
    last_summarized_message_uuid: str | None = Field(default=None, description="最后一条已总结消息的业务UUID")
    summary_version: int = Field(default=1, ge=1, description="总结版本号（单会话内递增）")
    summary_token_count: int = Field(default=0, ge=0, description="summary_content 的 token 数")
    status: Literal["success", "error"] = Field(default="success", description="总结状态")
    updated_at: datetime = Field(..., description="更新时间")


class ConversationSummarySetOnInsert(BaseModel):
    """
    功能描述：
        定义 `conversation_summaries` 集合执行 upsert 时 `$setOnInsert` 段的结构。

    参数说明：
        无（模型字段由调用方赋值）。

    返回值：
        无（Pydantic 数据模型）。

    异常说明：
        当字段缺失或类型不合法时，Pydantic 会抛出校验异常。
    """

    model_config = ConfigDict(extra="forbid", arbitrary_types_allowed=True)

    conversation_id: ObjectId = Field(..., description="所属会话 ObjectId")
    created_at: datetime = Field(..., description="创建时间")


class ConversationSummaryUpsertPayload(BaseModel):
    """
    功能描述：
        描述 `conversation_summaries` 的 upsert 更新文档完整结构。

    参数说明：
        无（模型字段由调用方赋值）。

    返回值：
        无（Pydantic 数据模型）。

    异常说明：
        当字段缺失或类型不合法时，Pydantic 会抛出校验异常。
    """

    model_config = ConfigDict(extra="forbid", populate_by_name=True)

    set_fields: ConversationSummaryUpdateSet = Field(..., alias="$set")
    set_on_insert_fields: ConversationSummarySetOnInsert = Field(..., alias="$setOnInsert")


class ConversationSummary(BaseModel):
    """
    功能描述：
        定义 `conversation_summaries` 集合在业务层的读取模型。

    参数说明：
        无（模型字段由数据库文档反序列化而来）。

    返回值：
        无（Pydantic 数据模型）。

    异常说明：
        当字段缺失或类型不合法时，Pydantic 会抛出校验异常。
    """

    model_config = ConfigDict(extra="ignore", populate_by_name=True)

    id: str | None = Field(default=None, alias="_id", description="MongoDB 主键")
    conversation_id: str = Field(..., description="所属会话ID")
    summary_content: str = Field(..., description="总结内容")
    last_summarized_message_id: str | None = Field(default=None, description="最后一条已总结消息的Mongo主键字符串")
    last_summarized_message_uuid: str | None = Field(default=None, description="最后一条已总结消息的业务UUID")
    summary_version: int = Field(default=1, ge=1, description="总结版本号（单会话内递增）")
    summary_token_count: int = Field(default=0, ge=0, description="summary_content 的 token 数")
    created_at: datetime = Field(..., description="创建时间")
    updated_at: datetime = Field(..., description="更新时间")
    status: Literal["success", "error"] = Field(default="success", description="总结状态（success/error）")

    @field_validator("id", mode="before")
    @classmethod
    def _normalize_id(cls, value: Any) -> str | None:
        """
        功能描述：
            把 MongoDB `_id` 字段统一归一化为字符串，便于接口层直接消费。

        参数说明：
            value (Any): 原始 `_id` 值，可能为 `ObjectId`、字符串或 `None`。

        返回值：
            str | None: 归一化后的字符串主键；为空时返回 `None`。

        异常说明：
            无。
        """

        if value is None:
            return None
        if isinstance(value, ObjectId):
            return str(value)
        return str(value)

    @field_validator("conversation_id", mode="before")
    @classmethod
    def _normalize_conversation_id(cls, value: Any) -> str:
        """
        功能描述：
            把 `conversation_id` 字段统一归一化为字符串。

        参数说明：
            value (Any): 原始 `conversation_id` 值，可能为 `ObjectId` 或字符串。

        返回值：
            str: 归一化后的会话主键字符串。

        异常说明：
            无。
        """

        if isinstance(value, ObjectId):
            return str(value)
        return str(value)

    @field_validator("last_summarized_message_id", mode="before")
    @classmethod
    def _normalize_last_summarized_message_id(cls, value: Any) -> str | None:
        """
        功能描述：
            把 `last_summarized_message_id` 统一归一化为字符串，兼容历史 ObjectId 类型输入。

        参数说明：
            value (Any): 原始消息主键值，可能为 `ObjectId`、字符串或 `None`。

        返回值：
            str | None: 归一化后的消息主键字符串；为空时返回 `None`。

        异常说明：
            无。
        """

        if value is None:
            return None
        if isinstance(value, ObjectId):
            return str(value)
        return str(value)
