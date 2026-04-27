"""Excel 行级切片。

将 Excel / CSV 的原始行数据按行合并为切片，尽量保证每行完整性。
行内单元格用制表符 ``\\t`` 连接，行与行之间用空格连接。
不使用分段重叠（overlap）。

合并规则：
1. 累积文本长度 + 下一行 ≤ ``max_chunk_size``  → 继续累积
2. 累积文本长度 + 下一行 ≤ ``max_chunk_size + TOLERANCE``  → 容忍并入（保行完整性）
3. 超出容忍范围 → 当前 chunk 保存，新 chunk 开始
4. 单行超过 ``max_chunk_size`` → 按单元格级别拆分
5. 单个单元格超过 ``max_chunk_size`` → 降级 ``RecursiveCharacterTextSplitter`` 切割
"""

from __future__ import annotations

from langchain_text_splitters import RecursiveCharacterTextSplitter

from app.rag.chunking.types import SplitChunk, build_chunk_stats

# 行间连接符
_ROW_SEPARATOR = " "

# 单元格内连接符
_CELL_SEPARATOR = "\t"

# 超过 max_chunk_size 后的容忍余量（字符）
TOLERANCE = 100


def split_excel_rows(
        rows: list[list[str]],
        *,
        max_chunk_size: int = 6000,
) -> list[SplitChunk]:
    """将 Excel / CSV 原始行数据按行合并为切片。

    Args:
        rows: 二维字符串列表，外层为行，内层为单元格。
        max_chunk_size: 单个切片的目标最大字符长度，默认 6000。

    Returns:
        切片结果列表；空输入返回空列表。
    """
    if not rows:
        return []

    chunks: list[SplitChunk] = []
    buffer: str = ""

    for row in rows:
        row_text = _join_row(row)
        if not row_text:
            continue

        # 单行本身就超过 max_chunk_size → 按单元格级别拆分
        if len(row_text) > max_chunk_size:
            # 先把已累积的 buffer 保存
            _flush_buffer(buffer, chunks)
            buffer = ""
            # 对超长行进行单元格级别拆分
            _split_oversize_row(row, max_chunk_size, chunks)
            continue

        if not buffer:
            buffer = row_text
            continue

        merged = buffer + _ROW_SEPARATOR + row_text
        merged_len = len(merged)

        if merged_len <= max_chunk_size:
            # 正常合并
            buffer = merged
        elif merged_len <= max_chunk_size + TOLERANCE:
            # 在容忍范围内，允许并入以保行完整性
            buffer = merged
        else:
            # 超出容忍范围，当前 buffer 保存，row_text 开始新 chunk
            _flush_buffer(buffer, chunks)
            buffer = row_text

    # 最后的 buffer
    _flush_buffer(buffer, chunks)
    return chunks


def _join_row(row: list[str]) -> str:
    """将单行单元格列表用制表符连接并去除首尾空白。"""
    values = [str(cell) if cell is not None else "" for cell in row]
    joined = _CELL_SEPARATOR.join(values).strip()
    return joined


def _flush_buffer(buffer: str, chunks: list[SplitChunk]) -> None:
    """若 buffer 非空则生成一个切片并追加到 chunks。"""
    text = buffer.strip()
    if text:
        chunks.append(SplitChunk(text=text, stats=build_chunk_stats(text)))


def _split_oversize_row(
        row: list[str],
        max_chunk_size: int,
        chunks: list[SplitChunk],
) -> None:
    """按单元格粒度拆分超长行。

    逐个单元格累积，当累积长度超过 ``max_chunk_size`` 时分片。
    如果单个单元格本身超过 ``max_chunk_size``，则降级用
    ``RecursiveCharacterTextSplitter`` 按字符切割。
    """
    buffer = ""
    for cell in row:
        cell_text = str(cell).strip() if cell is not None else ""
        if not cell_text:
            continue

        # 单个单元格超长 → 降级字符切割
        if len(cell_text) > max_chunk_size:
            _flush_buffer(buffer, chunks)
            buffer = ""
            _split_oversize_cell(cell_text, max_chunk_size, chunks)
            continue

        if not buffer:
            buffer = cell_text
            continue

        merged = buffer + _CELL_SEPARATOR + cell_text
        if len(merged) <= max_chunk_size:
            buffer = merged
        else:
            _flush_buffer(buffer, chunks)
            buffer = cell_text

    _flush_buffer(buffer, chunks)


def _split_oversize_cell(
        text: str,
        max_chunk_size: int,
        chunks: list[SplitChunk],
) -> None:
    """对超长单元格文本降级使用字符切割。"""
    splitter = RecursiveCharacterTextSplitter(
        chunk_size=max_chunk_size,
        chunk_overlap=0,
        separators=[" ", ""],
    )
    for piece in splitter.split_text(text):
        piece = piece.strip()
        if piece:
            chunks.append(SplitChunk(text=piece, stats=build_chunk_stats(piece)))
