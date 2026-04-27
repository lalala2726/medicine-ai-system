from __future__ import annotations

from datetime import datetime, timezone
from typing import Any, Literal

from app.core.mq.contracts.document.result_stages import ImportResultStage
from pydantic import BaseModel, Field, model_validator

from app.rag.chunking.types import MAX_CHUNK_OVERLAP, MAX_CHUNK_SIZE, MIN_CHUNK_SIZE
from app.rag.file_loader.types import FileKind

# command 默认字符切片大小。
DEFAULT_COMMAND_CHUNK_SIZE = 500
# command 默认重叠字符数。
DEFAULT_COMMAND_CHUNK_OVERLAP = 0


class KnowledgeImportCommandMessage(BaseModel):
    """智能服务消费的导入命令消息模型。

    Attributes:
        message_type: 固定消息类型，值为 ``knowledge_import_command``。
        task_uuid: 业务侧生成的任务唯一标识。
        biz_key: 用于 latest-version 判定的业务键。
        version: 同一 ``biz_key`` 下的递增版本号。
        knowledge_name: 目标知识库名称。
        document_id: 业务文档 ID。
        file_url: 待导入文件的访问地址。
        embedding_model: 本次导入使用的向量模型。
        chunk_size: 字符分块大小（100~6000）。
        chunk_overlap: 分段重叠字符长度（0~1000）。
        created_at: 命令创建时间。
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
        """标准化可空切片参数，避免无关字段导致消息校验失败。"""
        if not isinstance(data, dict):
            return data
        payload = dict(data)
        if payload.get("chunk_size") is None:
            payload["chunk_size"] = DEFAULT_COMMAND_CHUNK_SIZE
        if payload.get("chunk_overlap") is None:
            payload["chunk_overlap"] = DEFAULT_COMMAND_CHUNK_OVERLAP
        return payload

    def to_json_bytes(self) -> bytes:
        """将命令消息序列化为 UTF-8 JSON 字节串。

        Returns:
            bytes: 序列化后的消息体。
        """
        return self.model_dump_json().encode("utf-8")


class KnowledgeImportResultMessage(BaseModel):
    """智能服务发布的导入结果消息模型。

    Attributes:
        message_type: 固定消息类型，值为 ``knowledge_import_result``。
        task_uuid: 对应 command 的任务 ID。
        biz_key: 业务对象唯一键。
        version: 对应的业务版本号。
        stage: 当前结果阶段。
        message: 阶段说明或失败原因。
        knowledge_name: 知识库名称。
        document_id: 业务文档 ID。
        file_url: 导入文件地址。
        filename: 下载后文件名。
        file_type: 文件类型枚举值。
        file_size: 文件大小（Bytes）。
        chunk_count: 切片数量。
        vector_count: 向量数量。
        embedding_model: 实际执行的向量模型。
        embedding_dim: 向量维度。
        occurred_at: 当前结果事件时间。
        duration_ms: 从任务开始到当前事件的耗时。
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
    filename: str | None = None
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
            filename: str | None = None,
            file_type: FileKind | None = None,
            file_size: int | None = None,
            chunk_count: int = 0,
            vector_count: int = 0,
            embedding_dim: int = 0,
            started_at: datetime | None = None,
            occurred_at: datetime | None = None,
    ) -> "KnowledgeImportResultMessage":
        """构建标准化导入结果事件。

        Args:
            task_uuid: 任务唯一标识。
            biz_key: 业务对象唯一键。
            version: 同一 biz_key 下的递增版本号。
            stage: 当前事件阶段。
            message: 阶段消息描述。
            knowledge_name: 知识库名称。
            document_id: 文档 ID。
            file_url: 文件 URL。
            embedding_model: 向量模型名称。
            filename: 可选下载文件名。
            file_type: 文件类型枚举值。
            file_size: 文件大小（Bytes）。
            chunk_count: 成功时切片总数。
            vector_count: 成功时向量总数。
            embedding_dim: 实际向量维度。
            started_at: 可选任务开始时间。
            occurred_at: 可选事件发生时间。

        Returns:
            KnowledgeImportResultMessage: 构建后的结果事件对象。
        """
        event_time = (occurred_at or datetime.now(timezone.utc)).astimezone(timezone.utc)
        normalized_started_at = (
            started_at.astimezone(timezone.utc) if started_at is not None else event_time
        )
        duration_ms = int(max(0.0, (event_time - normalized_started_at).total_seconds()) * 1000)
        return cls(
            task_uuid=task_uuid,
            biz_key=biz_key,
            version=version,
            stage=stage,
            message=message,
            knowledge_name=knowledge_name,
            document_id=document_id,
            file_url=file_url,
            filename=filename,
            file_type=file_type,
            file_size=file_size,
            chunk_count=chunk_count,
            vector_count=vector_count,
            embedding_model=embedding_model,
            embedding_dim=max(0, embedding_dim),
            occurred_at=event_time,
            duration_ms=duration_ms,
        )

    def to_json_bytes(self) -> bytes:
        """将结果消息序列化为 UTF-8 JSON 字节串。

        Returns:
            bytes: 序列化后的消息体。
        """
        return self.model_dump_json().encode("utf-8")
