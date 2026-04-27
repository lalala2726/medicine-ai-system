from __future__ import annotations

from langchain_text_splitters import RecursiveCharacterTextSplitter

from app.rag.chunking.types import SplitChunk, build_chunk_stats

# 递归切片默认分隔符顺序：段落 → 换行 → 空格 → 逐字符
_DEFAULT_SEPARATORS: list[str] = ["\n\n", "\n", " ", ""]


def split_by_length(
        text: str,
        chunk_size: int,
        chunk_overlap: int,
) -> list[SplitChunk]:
    """按字符长度对文本执行切片。

    使用 ``RecursiveCharacterTextSplitter`` 保证切片不超过 ``chunk_size``，
    相邻切片间保持 ``chunk_overlap`` 个字符的重叠。

    Args:
        text: 待切片文本。
        chunk_size: 每个切片的目标字符长度。
        chunk_overlap: 相邻切片重叠字符长度。

    Returns:
        切片结果列表；空文本返回空列表。
    """
    if not text or not text.strip():
        return []
    splitter = RecursiveCharacterTextSplitter(
        chunk_size=chunk_size,
        chunk_overlap=chunk_overlap,
        separators=_DEFAULT_SEPARATORS,
    )
    pieces = splitter.split_text(text)
    return [
        SplitChunk(text=piece, stats=build_chunk_stats(piece))
        for piece in pieces
    ]
