from __future__ import annotations

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.rag.file_loader.parsers import (
    BaseParser,
    ExcelParser,
    PdfParser,
    PptParser,
    TextParser,
    WordParser,
)
from app.rag.file_loader.types import FileKind

_PARSER_REGISTRY: dict[FileKind, BaseParser] = {
    FileKind.TEXT: TextParser(),
    FileKind.MARKDOWN: TextParser(),
    FileKind.PDF: PdfParser(),
    FileKind.WORD: WordParser(),
    FileKind.EXCEL: ExcelParser(),
    FileKind.PPT: PptParser(),
}


def get_parser(file_kind: FileKind) -> BaseParser:
    """
    功能描述:
        根据文件类型获取对应解析器实例。

    参数说明:
        file_kind (FileKind): 目标文件类型。

    返回值:
        BaseParser: 对应解析器实例。

    异常说明:
        ServiceException: 传入类型未注册对应解析器时抛出。
    """
    parser = _PARSER_REGISTRY.get(file_kind)
    if parser is None:
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message=f"不支持的文件类型: {file_kind.value}",
        )
    return parser
