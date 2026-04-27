"""手工新增切片消息模型。

包含手工新增切片命令消息（业务 → AI）和结果消息（AI → 业务）的 Pydantic 模型。
"""

from __future__ import annotations

from datetime import datetime, timezone
from typing import Literal

from pydantic import BaseModel, Field, field_validator

from app.core.mq.models.stages import DocumentChunkResultStage

# knowledge_name 合法正则：字母开头，仅含字母、数字、下划线
KNOWLEDGE_NAME_PATTERN = r"^[A-Za-z][A-Za-z0-9_]*$"


class KnowledgeChunkAddCommandMessage(BaseModel):
    """手工新增切片命令消息（业务 → AI）。

    Attributes:
        message_type: 固定为 ``"knowledge_chunk_add_command"``。
        task_uuid: 任务唯一 ID。
        chunk_id: 业务侧切片 ID，需在结果消息中回传。
        knowledge_name: 知识库名称（Milvus Collection 名）。
        document_id: 文档 ID。
        content: 切片文本内容。
        embedding_model: Embedding 模型名称。
        created_at: 消息创建时间。
    """

    message_type: Literal["knowledge_chunk_add_command"] = "knowledge_chunk_add_command"
    task_uuid: str = Field(..., min_length=1)
    chunk_id: int = Field(..., gt=0)
    knowledge_name: str = Field(..., pattern=KNOWLEDGE_NAME_PATTERN)
    document_id: int = Field(..., gt=0)
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


class KnowledgeChunkAddResultMessage(BaseModel):
    """手工新增切片结果消息（AI → 业务）。

    通过 :meth:`build` 工厂方法创建，自动计算 ``duration_ms``。

    Attributes:
        message_type: 固定为 ``"knowledge_chunk_add_result"``。
        task_uuid: 任务唯一 ID。
        chunk_id: 业务侧切片 ID（回传）。
        stage: 结果阶段。
        message: 人可读描述。
        knowledge_name: 知识库名称。
        document_id: 文档 ID。
        vector_id: 新插入的向量记录 ID（成功时填充）。
        chunk_index: 切片序号（成功时填充）。
        embedding_model: Embedding 模型名称。
        embedding_dim: Embedding 维度。
        occurred_at: 事件发生时间 (UTC)。
        duration_ms: 耗时（毫秒）。
    """

    message_type: Literal["knowledge_chunk_add_result"] = "knowledge_chunk_add_result"
    task_uuid: str = Field(..., min_length=1)
    chunk_id: int = Field(..., gt=0)
    stage: DocumentChunkResultStage
    message: str = Field(..., min_length=1)
    knowledge_name: str = Field(..., min_length=1)
    document_id: int = Field(..., gt=0)
    vector_id: int | None = Field(default=None)
    chunk_index: int | None = Field(default=None)
    embedding_model: str = Field(..., min_length=1)
    embedding_dim: int = Field(default=0, ge=0)
    occurred_at: datetime
    duration_ms: int = Field(default=0, ge=0)

    @classmethod
    def build(
            cls,
            *,
            task_uuid: str,
            chunk_id: int,
            stage: DocumentChunkResultStage,
            message: str,
            knowledge_name: str,
            document_id: int,
            embedding_model: str,
            vector_id: int | None = None,
            chunk_index: int | None = None,
            embedding_dim: int = 0,
            started_at: datetime | None = None,
            occurred_at: datetime | None = None,
    ) -> KnowledgeChunkAddResultMessage:
        """工厂方法：构建手工新增切片结果消息并自动计算耗时。

        Args:
            task_uuid: 任务唯一 ID。
            chunk_id: 业务侧切片 ID。
            stage: 结果阶段。
            message: 人可读描述。
            knowledge_name: 知识库名称。
            document_id: 文档 ID。
            embedding_model: Embedding 模型名称。
            vector_id: 新插入向量 ID（成功时传入）。
            chunk_index: 切片序号（成功时传入）。
            embedding_dim: Embedding 维度。
            started_at: 任务开始时间。
            occurred_at: 事件发生时间，默认当前 UTC。

        Returns:
            ``KnowledgeChunkAddResultMessage`` 实例。
        """
        event_time = (occurred_at or datetime.now(timezone.utc)).astimezone(timezone.utc)
        ref_time = started_at.astimezone(timezone.utc) if started_at else event_time
        duration_ms = int(max(0.0, (event_time - ref_time).total_seconds()) * 1000)
        return cls(
            task_uuid=task_uuid,
            chunk_id=chunk_id,
            stage=stage,
            message=message,
            knowledge_name=knowledge_name,
            document_id=document_id,
            vector_id=vector_id,
            chunk_index=chunk_index,
            embedding_model=embedding_model,
            embedding_dim=max(0, embedding_dim),
            occurred_at=event_time,
            duration_ms=duration_ms,
        )
