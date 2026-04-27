"""切片重建消息模型。

包含切片重建命令消息（业务 → AI）和重建结果消息（AI → 业务）的 Pydantic 模型。
仅支持单切片重建，通过 Redis ``vector_id`` 粒度的版本检查丢弃过期消息。
"""

from __future__ import annotations

from datetime import datetime, timezone
from typing import Literal

from pydantic import BaseModel, Field, field_validator

from app.core.mq.models.stages import DocumentChunkResultStage

# knowledge_name 合法正则：字母开头，仅含字母、数字、下划线
KNOWLEDGE_NAME_PATTERN = r"^[A-Za-z][A-Za-z0-9_]*$"


class KnowledgeChunkRebuildCommandMessage(BaseModel):
    """切片重建命令消息（业务 → AI）。

    Attributes:
        message_type: 固定为 ``"knowledge_chunk_rebuild_command"``。
        task_uuid: 任务唯一 ID。
        knowledge_name: 知识库名称（Milvus Collection 名）。
        document_id: 文档 ID。
        vector_id: 待重建向量记录 ID。
        version: 消息版本号，用于过期判定。
        content: 新的切片文本内容。
        embedding_model: Embedding 模型名称。
        created_at: 消息创建时间。
    """

    message_type: Literal["knowledge_chunk_rebuild_command"] = "knowledge_chunk_rebuild_command"
    task_uuid: str = Field(..., min_length=1)
    knowledge_name: str = Field(..., pattern=KNOWLEDGE_NAME_PATTERN)
    document_id: int = Field(..., gt=0)
    vector_id: int = Field(..., gt=0)
    version: int = Field(..., gt=0)
    content: str = Field(..., min_length=1)
    embedding_model: str = Field(..., min_length=1)
    created_at: datetime

    @field_validator("content")
    @classmethod
    def validate_content(cls, value: str) -> str:
        """校验 content 字段：strip 后不能为空。"""
        normalized = value.strip()
        if not normalized:
            raise ValueError("content 不能为空")
        return normalized


class KnowledgeChunkRebuildResultMessage(BaseModel):
    """切片重建结果消息（AI → 业务）。

    通过 :meth:`build` 工厂方法创建，自动计算 ``duration_ms``。

    Attributes:
        message_type: 固定为 ``"knowledge_chunk_rebuild_result"``。
        task_uuid: 任务唯一 ID。
        version: 消息版本号。
        stage: 结果阶段。
        message: 人可读描述。
        knowledge_name: 知识库名称。
        document_id: 文档 ID。
        vector_id: 向量记录 ID。
        embedding_model: Embedding 模型名称。
        embedding_dim: Embedding 维度。
        occurred_at: 事件发生时间 (UTC)。
        duration_ms: 耗时（毫秒）。
    """

    message_type: Literal["knowledge_chunk_rebuild_result"] = "knowledge_chunk_rebuild_result"
    task_uuid: str = Field(..., min_length=1)
    version: int = Field(..., gt=0)
    stage: DocumentChunkResultStage
    message: str = Field(..., min_length=1)
    knowledge_name: str = Field(..., min_length=1)
    document_id: int = Field(..., gt=0)
    vector_id: int = Field(..., gt=0)
    embedding_model: str = Field(..., min_length=1)
    embedding_dim: int = Field(default=0, ge=0)
    occurred_at: datetime
    duration_ms: int = Field(default=0, ge=0)

    @classmethod
    def build(
            cls,
            *,
            task_uuid: str,
            version: int,
            stage: DocumentChunkResultStage,
            message: str,
            knowledge_name: str,
            document_id: int,
            vector_id: int,
            embedding_model: str,
            embedding_dim: int = 0,
            started_at: datetime | None = None,
            occurred_at: datetime | None = None,
    ) -> KnowledgeChunkRebuildResultMessage:
        """工厂方法：构建切片重建结果消息并自动计算耗时。

        Args:
            task_uuid: 任务唯一 ID。
            version: 消息版本号。
            stage: 结果阶段。
            message: 人可读描述。
            knowledge_name: 知识库名称。
            document_id: 文档 ID。
            vector_id: 向量记录 ID。
            embedding_model: Embedding 模型名称。
            embedding_dim: Embedding 维度。
            started_at: 任务开始时间。
            occurred_at: 事件发生时间，默认当前 UTC。

        Returns:
            ``KnowledgeChunkRebuildResultMessage`` 实例。
        """
        event_time = (occurred_at or datetime.now(timezone.utc)).astimezone(timezone.utc)
        ref_time = started_at.astimezone(timezone.utc) if started_at else event_time
        duration_ms = int(max(0.0, (event_time - ref_time).total_seconds()) * 1000)
        return cls(
            task_uuid=task_uuid,
            version=version,
            stage=stage,
            message=message,
            knowledge_name=knowledge_name,
            document_id=document_id,
            vector_id=vector_id,
            embedding_model=embedding_model,
            embedding_dim=max(0, embedding_dim),
            occurred_at=event_time,
            duration_ms=duration_ms,
        )
