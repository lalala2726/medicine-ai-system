from __future__ import annotations

from dataclasses import dataclass
from enum import Enum


class FileKind(str, Enum):
    """
    功能描述:
        定义导入链路支持的标准文件类型枚举，统一 URL 预检、魔数识别与解析器分发。

    参数说明:
        无。枚举成员在类中固定定义。

    返回值:
        无。调用方通过枚举成员的 value 进行传输与日志输出。

    异常说明:
        无。非法值由调用方在转换阶段处理。
    """

    TEXT = "text"
    MARKDOWN = "markdown"
    PDF = "pdf"
    WORD = "word"
    EXCEL = "excel"
    PPT = "ppt"


@dataclass
class ParsedDocument:
    """
    功能描述:
        表示文件解析后的统一输出结果，包含类型识别与单一文本内容。

    参数说明:
        file_kind (FileKind): 下载后识别出的文件类型。
        mime_type (str | None): 魔数识别得到的 MIME 类型，默认值为 None。
        source_extension (str | None): URL 推断得到的后缀，默认值为 None。
        text (str): 解析并清洗后的文本内容，默认值为空字符串。

    返回值:
        无。该类用于承载文件级解析结果。

    异常说明:
        无。
    """

    file_kind: FileKind
    mime_type: str | None = None
    source_extension: str | None = None
    text: str = ""

    def to_dict(self) -> dict:
        """
        功能描述:
            将文件解析结果转换为字典结构，便于序列化输出。

        参数说明:
            无。

        返回值:
            dict: 包含 file_kind、mime_type、source_extension、text 的字典。

        异常说明:
            无。
        """
        return {
            "file_kind": self.file_kind.value,
            "mime_type": self.mime_type,
            "source_extension": self.source_extension,
            "text": self.text,
        }


@dataclass
class ParseOptions:
    """
    功能描述:
        定义文件解析阶段可选行为参数，供单入口 service 使用。

    参数说明:
        normalize_text (bool): 是否执行文本标准化清洗，默认值为 True。

    返回值:
        无。该类用于承载解析行为配置。

    异常说明:
        无。
    """

    normalize_text: bool = True
