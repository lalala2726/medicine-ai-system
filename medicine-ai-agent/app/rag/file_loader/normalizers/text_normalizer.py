from __future__ import annotations

import re

from app.rag.file_loader.types import FileKind

_ZERO_WIDTH_PATTERN = re.compile(r"[\u200b\u200c\u200d\ufeff]")
_HORIZONTAL_SPACE_PATTERN = re.compile(r"[ \t]+")
_TRAILING_SPACE_PATTERN = re.compile(r"[ \t]+$")


def _normalize_newlines(text: str) -> str:
    """
    功能描述:
        统一文本换行符为 `\\n`，减少不同系统换行差异导致的切片噪声。

    参数说明:
        text (str): 原始文本。

    返回值:
        str: 换行统一后的文本。

    异常说明:
        无。
    """
    return text.replace("\r\n", "\n").replace("\r", "\n")


def _strip_zero_width(text: str) -> str:
    """
    功能描述:
        去除零宽字符和 BOM 等不可见噪声字符，避免无意义 token 消耗。

    参数说明:
        text (str): 原始文本。

    返回值:
        str: 去除不可见字符后的文本。

    异常说明:
        无。
    """
    return _ZERO_WIDTH_PATTERN.sub("", text)


def _compress_blank_lines(lines: list[str], *, max_blank_lines: int = 2) -> list[str]:
    """
    功能描述:
        压缩连续空行数量，保留必要段落边界并避免无效 token 浪费。

    参数说明:
        lines (list[str]): 按行拆分的文本。
        max_blank_lines (int): 连续空行最大保留数量，默认值为 2。

    返回值:
        list[str]: 压缩空行后的行列表。

    异常说明:
        无。
    """
    compressed: list[str] = []
    blank_count = 0
    for line in lines:
        if line.strip():
            blank_count = 0
            compressed.append(line)
            continue
        blank_count += 1
        if blank_count <= max_blank_lines:
            compressed.append("")
    return compressed


def _normalize_markdown(text: str) -> str:
    """
    功能描述:
        对 Markdown 执行保守清洗，保留标题/列表/代码块结构，仅处理明显噪声。

    参数说明:
        text (str): Markdown 原始文本。

    返回值:
        str: 清洗后的 Markdown 文本。

    异常说明:
        无。
    """
    lines = _normalize_newlines(_strip_zero_width(text)).split("\n")
    cleaned: list[str] = []
    in_code_block = False
    for line in lines:
        stripped = line.strip()
        if stripped.startswith("```") or stripped.startswith("~~~"):
            in_code_block = not in_code_block
            cleaned.append(stripped)
            continue
        if in_code_block:
            cleaned.append(line.rstrip("\n"))
        else:
            cleaned.append(_TRAILING_SPACE_PATTERN.sub("", line))
    return "\n".join(_compress_blank_lines(cleaned)).strip()


def _normalize_excel(text: str) -> str:
    """
    功能描述:
        对 Excel 文本执行行列保留清洗：保留行边界与制表符列分隔，压缩单元格冗余空白。

    参数说明:
        text (str): Excel 解析后的原始文本。

    返回值:
        str: 清洗后的 Excel 文本。

    异常说明:
        无。
    """
    lines = _normalize_newlines(_strip_zero_width(text)).split("\n")
    cleaned_rows: list[str] = []
    for line in lines:
        if not line.strip():
            cleaned_rows.append("")
            continue
        cells = line.split("\t")
        normalized_cells = [
            _HORIZONTAL_SPACE_PATTERN.sub(" ", cell).strip() for cell in cells
        ]
        cleaned_rows.append("\t".join(normalized_cells).strip())
    return "\n".join(_compress_blank_lines(cleaned_rows)).strip()


def _normalize_general(text: str) -> str:
    """
    功能描述:
        对通用文本执行结构保留清洗：压缩行内空白、去行尾空白并保留段落换行。

    参数说明:
        text (str): 通用原始文本。

    返回值:
        str: 清洗后的通用文本。

    异常说明:
        无。
    """
    lines = _normalize_newlines(_strip_zero_width(text)).split("\n")
    cleaned: list[str] = []
    for line in lines:
        if not line.strip():
            cleaned.append("")
            continue
        normalized_line = _HORIZONTAL_SPACE_PATTERN.sub(" ", line).strip()
        cleaned.append(_TRAILING_SPACE_PATTERN.sub("", normalized_line))
    return "\n".join(_compress_blank_lines(cleaned)).strip()


def normalize_text(text: str, file_kind: FileKind) -> str:
    """
    功能描述:
        按文件类型执行文本标准化清洗，减少无效 token 消耗并尽量保留语义结构。

    参数说明:
        text (str): 待清洗文本。
        file_kind (FileKind): 文件类型。

    返回值:
        str: 清洗后的文本。

    异常说明:
        无。未知类型会回退通用清洗策略。
    """
    if not text:
        return ""
    if file_kind == FileKind.MARKDOWN:
        return _normalize_markdown(text)
    if file_kind == FileKind.EXCEL:
        return _normalize_excel(text)
    return _normalize_general(text)
