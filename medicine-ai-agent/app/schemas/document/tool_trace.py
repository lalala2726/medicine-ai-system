from __future__ import annotations

from datetime import datetime
from enum import Enum
from typing import Any

from bson import ObjectId
from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.schemas.document.conversation import ConversationType


class ToolTraceStatus(str, Enum):
    """工具轨迹状态。"""

    SUCCESS = "success"
    ERROR = "error"


class ToolTraceCreate(BaseModel):
    """新增工具轨迹入参模型（服务层内部使用）。"""

    model_config = ConfigDict(extra="forbid")

    conversation_uuid: str = Field(..., min_length=1, description="所属会话 UUID")
    assistant_message_uuid: str = Field(..., min_length=1, description="所属 AI 回复消息 UUID")
    conversation_type: ConversationType = Field(..., description="会话类型")
    tool_name: str = Field(..., min_length=1, description="工具原始名称")
    tool_display_name: str = Field(..., min_length=1, description="工具展示名称")
    status: ToolTraceStatus = Field(..., description="工具执行状态")
    summary_text: str = Field(..., min_length=1, description="给模型消费的稳定中文摘要")
    input_payload: Any = Field(..., description="工具完整输入 JSON 结构")
    output_payload: Any | None = Field(default=None, description="工具完整输出 JSON 结构")
    error_payload: dict[str, Any] | None = Field(default=None, description="工具失败结构化错误信息")
    created_at: datetime = Field(default_factory=datetime.now, description="创建时间")


class ToolTraceDocument(BaseModel):
    """MongoDB tool_traces 文档模型。"""

    model_config = ConfigDict(extra="ignore", populate_by_name=True)

    id: str | None = Field(default=None, alias="_id", description="MongoDB 主键")
    conversation_uuid: str = Field(..., description="所属会话 UUID")
    assistant_message_uuid: str = Field(..., description="所属 AI 回复消息 UUID")
    conversation_type: ConversationType = Field(..., description="会话类型")
    tool_name: str = Field(..., description="工具原始名称")
    tool_display_name: str = Field(..., description="工具展示名称")
    status: ToolTraceStatus = Field(..., description="工具执行状态")
    summary_text: str = Field(..., description="给模型消费的稳定中文摘要")
    input_payload: Any = Field(..., description="工具完整输入 JSON 结构")
    output_payload: Any | None = Field(default=None, description="工具完整输出 JSON 结构")
    error_payload: dict[str, Any] | None = Field(default=None, description="工具失败结构化错误信息")
    created_at: datetime = Field(..., description="创建时间")

    @field_validator("id", mode="before")
    @classmethod
    def _normalize_tool_trace_id(cls, value: Any) -> str | None:
        """把 Mongo ObjectId 统一转换为字符串。"""

        if value is None:
            return None
        if isinstance(value, ObjectId):
            return str(value)
        return str(value)
