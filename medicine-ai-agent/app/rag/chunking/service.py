from __future__ import annotations

import logging

from app.rag.chunking.strategies.character_splitter import split_by_length
from app.rag.chunking.strategies.excel_row_splitter import split_excel_rows
from app.rag.chunking.types import SplitChunk

_CHUNK_OVERSIZE_WARNING_PREFIX = "Created a chunk of size"


class _LangChainChunkWarningFilter(logging.Filter):
    """过滤 LangChain Text Splitter 的超长 chunk 提示日志。"""

    def filter(self, record: logging.LogRecord) -> bool:
        return not record.getMessage().startswith(_CHUNK_OVERSIZE_WARNING_PREFIX)


def _suppress_langchain_chunk_size_warning() -> None:
    logger = logging.getLogger("langchain_text_splitters.base")
    if any(isinstance(item, _LangChainChunkWarningFilter) for item in logger.filters):
        return
    logger.addFilter(_LangChainChunkWarningFilter())


_suppress_langchain_chunk_size_warning()


def split_text(
        text: str,
        chunk_size: int,
        chunk_overlap: int,
) -> list[SplitChunk]:
    """按字符长度对普通文本执行切片。

    Args:
        text: 待切片文本。
        chunk_size: 每个切片的目标字符长度。
        chunk_overlap: 相邻切片重叠字符长度。

    Returns:
        切片结果列表；空文本返回空列表。
    """
    if not text:
        return []
    return split_by_length(text, chunk_size, chunk_overlap)


def split_excel_text(
        rows: list[list[str]],
        *,
        max_chunk_size: int = 6000,
) -> list[SplitChunk]:
    """将 Excel / CSV 原始行数据按行合并为切片。

    Args:
        rows: 二维字符串列表，外层为行，内层为单元格。
        max_chunk_size: 单个切片目标最大字符长度，默认 6000。

    Returns:
        切片结果列表；空输入返回空列表。
    """
    if not rows:
        return []
    return split_excel_rows(rows, max_chunk_size=max_chunk_size)
