from __future__ import annotations

from typing import Literal, TypeAlias

from pydantic import BaseModel, Field

from app.rag.chunking import SplitChunk


class ImportChunkStats(BaseModel):
    """导入切片统计信息。"""

    char_count: int = Field(default=0, ge=0)


class ImportChunk(BaseModel):
    """导入切片内容。"""

    text: str
    stats: ImportChunkStats

    @classmethod
    def from_split_chunk(cls, chunk: SplitChunk) -> "ImportChunk":
        """将 chunking 层对象转换为导入结果切片对象。"""
        return cls(
            text=chunk.text,
            stats=ImportChunkStats(char_count=chunk.stats.char_count),
        )


class ImportSingleFileSuccessResult(BaseModel):
    """单文件导入成功结果。"""

    status: Literal["success"] = "success"
    file_url: str = Field(..., min_length=1)
    filename: str | None = None
    file_size: int = Field(..., ge=0)
    source_extension: str = Field(..., min_length=1)
    file_kind: str = Field(..., min_length=1)
    mime_type: str | None = Field(default=None, min_length=1)
    text_length: int = Field(..., ge=0)
    chunk_count: int = Field(..., ge=0)
    vector_count: int = Field(..., ge=0)
    insert_batches: int = Field(..., ge=0)
    embedding_model: str = Field(..., min_length=1)
    embedding_dim: int = Field(..., ge=0)
    chunks: list[ImportChunk] = Field(default_factory=list)


class ImportSingleFileFailedResult(BaseModel):
    """单文件导入失败结果。"""

    status: Literal["failed"] = "failed"
    file_url: str = Field(..., min_length=1)
    filename: str | None = None
    file_size: int | None = Field(default=None, ge=0)
    error: str = Field(..., min_length=1)
    embedding_model: str = Field(..., min_length=1)
    embedding_dim: int = Field(..., ge=0)


ImportSingleFileResult: TypeAlias = (
        ImportSingleFileSuccessResult | ImportSingleFileFailedResult
)
