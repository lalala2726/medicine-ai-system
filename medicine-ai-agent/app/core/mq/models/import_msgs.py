"""知识库导入消息模型。

包含导入命令消息（业务 → AI）和导入结果消息（AI → 业务）的 Pydantic 模型。
JSON 序列化由 FastStream 自动处理。
"""

from __future__ import annotations

from datetime import datetime, timezone
from typing import Any, Literal

from pydantic import BaseModel, Field, model_validator

from app.core.mq.models.stages import ImportResultStage
from app.rag.chunking.types import MAX_CHUNK_OVERLAP, MAX_CHUNK_SIZE, MIN_CHUNK_SIZE
from app.rag.file_loader.types import FileKind

# 当 chunk_size / chunk_overlap 为 null 时的默认值
DEFAULT_COMMAND_CHUNK_SIZE = 500
DEFAULT_COMMAND_CHUNK_OVERLAP = 0


class KnowledgeImportCommandMessage(BaseModel):
    """导入命令消息（业务 → AI）。

    Attributes:
        message_type: 固定为 ``"knowledge_import_command"``。
        task_uuid: 任务唯一 ID。
        biz_key: 业务唯一标识，用于版本检查。
        version: 消息版本号，用于过期判定。
        knowledge_name: 知识库名称（Milvus Collection 名）。
        document_id: 文档 ID。
        file_url: 待导入文件 URL。
        embedding_model: Embedding 模型名称。
        chunk_size: 字符分块大小（100~6000）。
        chunk_overlap: 分段重叠字符长度（0~1000）。
        created_at: 消息创建时间。
    """

    message_type: Literal["knowledge_import_command"] = "knowledge_import_command"
    task_uuid: str = Field(..., min_length=1)
    biz_key: str = Field(..., min_length=1)
    version: int = Field(..., ge=1)
    knowledge_name: str = Field(..., min_length=1)
    document_id: int = Field(..., gt=0)
    file_url: str = Field(..., min_length=1)
    embedding_model: str = Field(..., min_length=1)
    chunk_size: int = Field(default=DEFAULT_COMMAND_CHUNK_SIZE, ge=MIN_CHUNK_SIZE, le=MAX_CHUNK_SIZE)
    chunk_overlap: int = Field(default=DEFAULT_COMMAND_CHUNK_OVERLAP, ge=0, le=MAX_CHUNK_OVERLAP)
    created_at: datetime

    @model_validator(mode="before")
    @classmethod
    def _normalize_nullable_chunk_options(cls, data: Any) -> Any:
        """将业务端发送的 null chunk_size / chunk_overlap 归一为默认值。"""
        if not isinstance(data, dict):
            return data
        payload = dict(data)
        if payload.get("chunk_size") is None:
            payload["chunk_size"] = DEFAULT_COMMAND_CHUNK_SIZE
        if payload.get("chunk_overlap") is None:
            payload["chunk_overlap"] = DEFAULT_COMMAND_CHUNK_OVERLAP
        return payload


class KnowledgeImportResultMessage(BaseModel):
    """导入结果消息（AI → 业务）。

    通过 :meth:`build` 工厂方法创建，自动计算 ``duration_ms``。

    Attributes:
        message_type: 固定为 ``"knowledge_import_result"``。
        task_uuid: 任务唯一 ID。
        biz_key: 业务唯一标识。
        version: 消息版本号。
        stage: 结果阶段。
        message: 人可读描述。
        knowledge_name: 知识库名称。
        document_id: 文档 ID。
        file_url: 文件 URL。
        file_type: 文件类型枚举值。
        file_size: 文件大小（Bytes）。
        chunk_count: 分块数。
        vector_count: 入库向量数。
        embedding_model: Embedding 模型名称。
        embedding_dim: Embedding 维度。
        occurred_at: 事件发生时间 (UTC)。
        duration_ms: 从任务开始到当前阶段的耗时（毫秒）。
    """

    message_type: Literal["knowledge_import_result"] = "knowledge_import_result"
    task_uuid: str = Field(..., min_length=1)
    biz_key: str = Field(..., min_length=1)
    version: int = Field(..., ge=1)
    stage: ImportResultStage
    message: str = Field(..., min_length=1)
    knowledge_name: str = Field(..., min_length=1)
    document_id: int = Field(..., gt=0)
    file_url: str = Field(..., min_length=1)
    file_type: FileKind | None = Field(default=None)
    file_size: int | None = Field(default=None, ge=0)
    chunk_count: int = Field(default=0, ge=0)
    vector_count: int = Field(default=0, ge=0)
    embedding_model: str = Field(..., min_length=1)
    embedding_dim: int = Field(default=0, ge=0)
    occurred_at: datetime
    duration_ms: int = Field(default=0, ge=0)

    @classmethod
    def build(
            cls,
            *,
            task_uuid: str,
            biz_key: str,
            version: int,
            stage: ImportResultStage,
            message: str,
            knowledge_name: str,
            document_id: int,
            file_url: str,
            embedding_model: str,
            file_type: FileKind | None = None,
            file_size: int | None = None,
            chunk_count: int = 0,
            vector_count: int = 0,
            embedding_dim: int = 0,
            started_at: datetime | None = None,
            occurred_at: datetime | None = None,
    ) -> KnowledgeImportResultMessage:
        """工厂方法：构建导入结果消息并自动计算耗时。

        Args:
            task_uuid: 任务唯一 ID。
            biz_key: 业务唯一标识。
            version: 消息版本号。
            stage: 结果阶段。
            message: 人可读描述。
            knowledge_name: 知识库名称。
            document_id: 文档 ID。
            file_url: 文件 URL。
            embedding_model: Embedding 模型名称。
            file_type: 文件类型枚举值。
            file_size: 文件大小（Bytes）。
            chunk_count: 分块数。
            vector_count: 向量数。
            embedding_dim: Embedding 维度。
            started_at: 任务开始时间，用于计算 duration_ms。
            occurred_at: 事件发生时间，默认当前 UTC。

        Returns:
            ``KnowledgeImportResultMessage`` 实例。
        """
        event_time = (occurred_at or datetime.now(timezone.utc)).astimezone(timezone.utc)
        ref_time = started_at.astimezone(timezone.utc) if started_at else event_time
        duration_ms = int(max(0.0, (event_time - ref_time).total_seconds()) * 1000)
        return cls(
            task_uuid=task_uuid,
            biz_key=biz_key,
            version=version,
            stage=stage,
            message=message,
            knowledge_name=knowledge_name,
            document_id=document_id,
            file_url=file_url,
            file_type=file_type,
            file_size=file_size,
            chunk_count=chunk_count,
            vector_count=vector_count,
            embedding_model=embedding_model,
            embedding_dim=max(0, embedding_dim),
            occurred_at=event_time,
            duration_ms=duration_ms,
        )
