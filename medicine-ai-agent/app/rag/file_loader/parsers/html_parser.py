from __future__ import annotations

from html.parser import HTMLParser
from pathlib import Path

from app.rag.file_loader.parsers.base import BaseParser


class _HTMLTextExtractor(HTMLParser):
    """
    功能描述:
        轻量 HTML 文本抽取器，用于提取标签中的可见文本。

    参数说明:
        无。内部通过 `handle_data` 收集文本片段。

    返回值:
        无。通过 `get_text` 获取合并后的结果。

    异常说明:
        无。解析异常由 HTMLParser 内部容错处理。
    """

    def __init__(self) -> None:
        """
        功能描述:
            初始化 HTML 文本抽取器并创建片段缓冲区。

        参数说明:
            无。

        返回值:
            None: 初始化完成无返回值。

        异常说明:
            无。
        """
        super().__init__()
        self._parts: list[str] = []

    def handle_data(self, data: str) -> None:
        """
        功能描述:
            处理 HTML 标签内文本并过滤纯空白片段。

        参数说明:
            data (str): HTML 标签内文本。

        返回值:
            None: 处理完成无返回值。

        异常说明:
            无。
        """
        if data and data.strip():
            self._parts.append(data.strip())

    def get_text(self) -> str:
        """
        功能描述:
            获取合并后的纯文本结果，并折叠内部冗余空白。

        参数说明:
            无。

        返回值:
            str: 合并后的纯文本。

        异常说明:
            无。
        """
        return " ".join(" ".join(self._parts).split())


class HtmlParser(BaseParser):
    """
    功能描述:
        解析 HTML 文件并抽取正文纯文本。

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
            读取 HTML 文件并提取纯文本内容。

        参数说明:
            file_path (Path): HTML 文件路径。

        返回值:
            str: 提取后的纯文本内容。

        异常说明:
            OSError: 文件不可读时由底层文件系统抛出。
        """
        content = file_path.read_text(encoding="utf-8", errors="ignore")
        extractor = _HTMLTextExtractor()
        extractor.feed(content)
        return extractor.get_text()
