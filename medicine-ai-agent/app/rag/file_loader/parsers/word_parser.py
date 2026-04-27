from __future__ import annotations

from pathlib import Path
from typing import Iterable

from docx import Document
from docx.oxml.ns import qn
from docx.table import Table
from docx.text.paragraph import Paragraph

from app.core.exception.exceptions import ServiceException
from app.rag.file_loader.parsers.base import BaseParser


def _iter_block_items(document: Document) -> Iterable[Paragraph | Table]:
    """
    功能描述:
        按文档原始顺序遍历段落与表格节点。

    参数说明:
        document (Document): Word 文档对象。

    返回值:
        Iterable[Paragraph | Table]: 顺序遍历结果。

    异常说明:
        无。
    """
    for child in document.element.body.iterchildren():
        if child.tag == qn("w:p"):
            yield Paragraph(child, document)
        elif child.tag == qn("w:tbl"):
            yield Table(child, document)


def _block_text(block: Paragraph | Table) -> str:
    """
    功能描述:
        统一抽取段落或表格文本，保持结构化换行信息。

    参数说明:
        block (Paragraph | Table): 段落或表格对象。

    返回值:
        str: 抽取到的文本内容。

    异常说明:
        无。
    """
    if isinstance(block, Paragraph):
        return block.text.strip()

    rows: list[str] = []
    for row in block.rows:
        values: list[str] = []
        for cell in row.cells:
            values.append(" ".join(cell.text.split()))
        if any(values):
            rows.append("\t".join(values))
    return "\n".join(rows)


def _parse_docx(file_path: Path) -> str:
    """
    功能描述:
        解析 docx 文件并按内容顺序拼接为单一文本。

    参数说明:
        file_path (Path): docx 文件路径。

    返回值:
        str: 拼接后的完整文本。

    异常说明:
        Exception: 文档读取失败时由 python-docx 抛出。
    """
    document = Document(str(file_path))
    parts: list[str] = []
    for block in _iter_block_items(document):
        text = _block_text(block)
        if text:
            parts.append(text.strip())
    return "\n\n".join(part for part in parts if part)


def _parse_doc(file_path: Path) -> str:
    """
    功能描述:
        使用 unstructured 降级解析 doc 文件并拼接为单一文本。

    参数说明:
        file_path (Path): doc 文件路径。

    返回值:
        str: 拼接后的完整文本。

    异常说明:
        ServiceException:
            - 缺少 unstructured 依赖时抛出；
            - unstructured 解析失败时抛出。
    """
    try:
        from unstructured.partition.auto import partition
    except Exception as exc:
        raise ServiceException("缺少 unstructured 依赖，无法解析 doc 文件") from exc

    try:
        elements = partition(filename=str(file_path))
    except Exception as exc:
        raise ServiceException(f"解析 doc 文件失败: {exc}") from exc

    parts: list[str] = []
    for element in elements:
        text = element.text or ""
        if text:
            parts.append(text.strip())
    return "\n\n".join(part for part in parts if part)


class WordParser(BaseParser):
    """
    功能描述:
        解析 Word 文件，支持 docx 与 doc 两种格式。

    参数说明:
        无。解析参数通过 `parse` 方法传入。

    返回值:
        无。调用 `parse` 时返回文本内容。

    异常说明:
        ServiceException: 文件后缀不支持或降级解析失败时抛出。
    """

    def parse(self, file_path: Path) -> str:
        """
        功能描述:
            根据后缀选择 Word 解析分支。

        参数说明:
            file_path (Path): Word 文件路径。

        返回值:
            str: 拼接后的完整文本。

        异常说明:
            ServiceException: 不支持的 Word 文件格式时抛出。
        """
        suffix = file_path.suffix.lower()
        if suffix == ".docx":
            return _parse_docx(file_path)
        if suffix == ".doc":
            return _parse_doc(file_path)
        raise ServiceException(f"不支持的 Word 格式: {suffix}")
