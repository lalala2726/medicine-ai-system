from __future__ import annotations

from pathlib import Path

from pypdf import PdfReader

from app.rag.file_loader.parsers.base import BaseParser


class PdfParser(BaseParser):
    """
    功能描述:
        解析 PDF 文件并返回拼接后的完整文本。

    参数说明:
        无。解析参数通过 `parse` 方法传入。

    返回值:
        无。调用 `parse` 时返回文本内容。

    异常说明:
        无。底层解析异常由 pypdf 抛出。
    """

    def parse(self, file_path: Path) -> str:
        """
        功能描述:
            打开 PDF 文件并按顺序提取文本后拼接为单一字符串。

        参数说明:
            file_path (Path): PDF 文件路径。

        返回值:
            str: 拼接后的完整文本内容。

        异常说明:
            Exception: PDF 结构损坏或读取失败时由底层库抛出。
        """
        reader = PdfReader(str(file_path))
        parts = [(page.extract_text() or "").strip() for page in reader.pages]
        return "\n\n".join(part for part in parts if part)
