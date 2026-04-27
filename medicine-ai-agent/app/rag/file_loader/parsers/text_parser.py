from __future__ import annotations

from pathlib import Path

from app.rag.file_loader.parsers.base import BaseParser


class TextParser(BaseParser):
    """
    功能描述:
        解析纯文本和 Markdown 文件并返回完整文本内容。

    参数说明:
        无。解析参数通过 `parse` 方法传入。

    返回值:
        无。调用 `parse` 时返回文本内容。

    异常说明:
        无。文件读取异常由底层 I/O 抛出。
    """

    def parse(self, file_path: Path) -> str:
        """
        功能描述:
            读取文本文件并返回完整文本。

        参数说明:
            file_path (Path): 文本文件路径。

        返回值:
            str: 文件文本内容。

        异常说明:
            OSError: 文件不可读时由底层文件系统抛出。
        """
        return file_path.read_text(encoding="utf-8", errors="ignore")
