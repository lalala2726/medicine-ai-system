from __future__ import annotations

from datetime import datetime
from enum import Enum
from typing import Any

from bson import ObjectId
from bson.int64 import Int64
from pydantic import BaseModel, ConfigDict, Field, field_serializer, field_validator


class TtsUsageProvider(str, Enum):
    """TTS 服务提供商。"""

    VOLCENGINE = "volcengine"


class TtsUsageStatus(str, Enum):
    """TTS 调用结果状态。"""

    SUCCESS = "success"


class MessageTtsUsageCreate(BaseModel):
    """新增 TTS 调用明细入参模型（服务层内部使用）。"""

    model_config = ConfigDict(extra="forbid", arbitrary_types_allowed=True)

    uuid: str = Field(..., min_length=1, description="TTS 调用唯一 ID")
    message_uuid: str = Field(..., min_length=1, description="关联 AI 消息 UUID")
    conversation_id: str = Field(..., min_length=1, description="所属会话 ID")
    conversation_uuid: str = Field(..., min_length=1, description="所属会话 UUID")
    user_id: int = Field(..., ge=1, description="用户 ID（int64）")
    provider: TtsUsageProvider = Field(default=TtsUsageProvider.VOLCENGINE, description="TTS 提供商")
    status: TtsUsageStatus = Field(default=TtsUsageStatus.SUCCESS, description="调用状态")
    endpoint: str = Field(..., min_length=1, description="请求 endpoint")
    resource_id: str = Field(..., min_length=1, description="资源 ID")
    voice_type: str = Field(..., min_length=1, description="音色")
    encoding: str = Field(..., min_length=1, description="音频编码")
    sample_rate: int = Field(..., ge=1, description="采样率")
    sent_text: str = Field(..., min_length=1, description="实际发送给第三方的文本")
    billable_chars: int = Field(..., ge=0, description="计费字符数")
    source_text_chars: int = Field(..., ge=0, description="原始文本字符数")
    sanitized_text_chars: int = Field(..., ge=0, description="清洗后文本字符数")
    max_text_chars: int = Field(..., ge=1, description="最大可发送字符数")
    is_truncated: bool = Field(default=False, description="是否发生截断")
    audio_chunk_count: int = Field(..., ge=0, description="音频分片数")
    audio_bytes: int = Field(..., ge=0, description="音频字节数")
    duration_ms: int = Field(..., ge=0, description="调用耗时毫秒")
    connect_id: str | None = Field(default=None, description="WebSocket connect_id")
    session_id: str | None = Field(default=None, description="TTS session_id")
    provider_log_id: str | None = Field(default=None, description="第三方响应日志 ID")

    @field_serializer("user_id", when_used="always")
    def _serialize_user_id(self, value: int) -> Int64:
        """确保写入 Mongo 时 user_id 为 bson int64。"""

        return Int64(value)


class MessageTtsUsageDocument(BaseModel):
    """MongoDB message_tts_usages 文档模型。"""

    model_config = ConfigDict(extra="ignore", populate_by_name=True)

    id: str | None = Field(default=None, alias="_id", description="MongoDB 主键")
    uuid: str = Field(..., description="TTS 调用唯一 ID")
    message_uuid: str = Field(..., description="关联 AI 消息 UUID")
    conversation_id: str = Field(..., description="所属会话 ID")
    conversation_uuid: str = Field(..., description="所属会话 UUID")
    user_id: int = Field(..., description="用户 ID（int64）")
    provider: TtsUsageProvider = Field(default=TtsUsageProvider.VOLCENGINE, description="TTS 提供商")
    status: TtsUsageStatus = Field(default=TtsUsageStatus.SUCCESS, description="调用状态")
    endpoint: str = Field(..., description="请求 endpoint")
    resource_id: str = Field(..., description="资源 ID")
    voice_type: str = Field(..., description="音色")
    encoding: str = Field(..., description="音频编码")
    sample_rate: int = Field(..., description="采样率")
    sent_text: str = Field(..., description="实际发送给第三方的文本")
    billable_chars: int = Field(..., description="计费字符数")
    source_text_chars: int = Field(..., description="原始文本字符数")
    sanitized_text_chars: int = Field(..., description="清洗后文本字符数")
    max_text_chars: int = Field(..., description="最大可发送字符数")
    is_truncated: bool = Field(default=False, description="是否发生截断")
    audio_chunk_count: int = Field(..., description="音频分片数")
    audio_bytes: int = Field(..., description="音频字节数")
    duration_ms: int = Field(..., description="调用耗时毫秒")
    connect_id: str | None = Field(default=None, description="WebSocket connect_id")
    session_id: str | None = Field(default=None, description="TTS session_id")
    provider_log_id: str | None = Field(default=None, description="第三方响应日志 ID")
    created_at: datetime = Field(..., description="创建时间")
    updated_at: datetime = Field(..., description="更新时间")

    @field_validator("id", mode="before")
    @classmethod
    def _normalize_id(cls, value: Any) -> str | None:
        """把 Mongo ObjectId 统一转换为字符串。"""

        if value is None:
            return None
        if isinstance(value, ObjectId):
            return str(value)
        return str(value)

    @field_validator("conversation_id", mode="before")
    @classmethod
    def _normalize_conversation_id(cls, value: Any) -> str:
        """把 conversation_id 转为字符串。"""

        if isinstance(value, ObjectId):
            return str(value)
        return str(value)
