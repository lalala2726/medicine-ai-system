from __future__ import annotations

from dataclasses import dataclass
from typing import Any

# 切片参数约束常量
MIN_CHUNK_SIZE = 100
MAX_CHUNK_SIZE = 6000
MIN_CHUNK_OVERLAP = 0
MAX_CHUNK_OVERLAP = 1000


@dataclass
class SplitConfig:
    """文本切片通用配置。

    Attributes:
        chunk_size: 每个分块的目标字符长度，取值范围 [100, 6000]，默认 500。
        chunk_overlap: 相邻分块重叠字符长度，取值范围 [0, 1000]，默认 0。
    """

    chunk_size: int = 500
    chunk_overlap: int = 0


@dataclass
class ChunkStats:
    """单个切片的文本统计信息。"""

    char_count: int = 0

    def to_dict(self) -> dict[str, int]:
        return {"char_count": self.char_count}


def build_chunk_stats(text: str) -> ChunkStats:
    """基于切片文本构建统计信息。"""
    return ChunkStats(char_count=len(text or ""))


@dataclass
class SplitChunk:
    """单个切片结果。"""

    text: str
    stats: ChunkStats

    def to_dict(self) -> dict[str, Any]:
        return {
            "text": self.text,
            "stats": self.stats.to_dict(),
        }
