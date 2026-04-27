from __future__ import annotations

from pathlib import Path

from loguru import logger

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.rag.file_loader.detectors.filetype_detector import detect_file_kind
from app.rag.file_loader.detectors.type_mapping import file_kind_from_extension
from app.rag.file_loader.detectors.url_extension import validate_url_extension
from app.rag.file_loader.normalizers.text_normalizer import normalize_text
from app.rag.file_loader.registry import get_parser
from app.rag.file_loader.types import ParseOptions, ParsedDocument


def parse_downloaded_file(
        file_path: Path,
        source_url: str,
        options: ParseOptions | None = None,
) -> ParsedDocument:
    """
    功能描述:
        解析已下载文件的统一入口。内部完成 URL 后缀校验、魔数识别、解析器分发与文本清洗。

    参数说明:
        file_path (Path): 已下载的本地文件路径。
        source_url (str): 文件来源 URL，用于第一阶段后缀校验。
        options (ParseOptions | None): 解析行为配置，默认值为 None；为空时使用默认配置。

    返回值:
        ParsedDocument: 标准化文件解析结果。

    异常说明:
        ServiceException:
            - URL 后缀不支持时由 URL 校验逻辑抛出；
            - 魔数识别失败或文件类型不支持时由识别逻辑抛出；
            - 具体解析器失败时由解析器实现抛出。
    """
    resolved_options = options or ParseOptions()

    source_extension = validate_url_extension(source_url)
    source_file_kind = file_kind_from_extension(source_extension)
    detected_file_kind, mime_type = detect_file_kind(file_path)
    if source_file_kind and source_file_kind != detected_file_kind:
        logger.warning(
            "URL 后缀与真实类型不一致，将按真实类型解析：url={}, source_extension={}, source_kind={}, detected_kind={}, mime_type={}",
            source_url,
            source_extension,
            source_file_kind.value,
            detected_file_kind.value,
            mime_type,
        )

    parser = get_parser(detected_file_kind)
    try:
        parsed_text = parser.parse(file_path)
    except ServiceException:
        raise
    except Exception as exc:
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message=f"文件解析失败: {file_path.name}",
        ) from exc
    normalized_text = parsed_text or ""
    if resolved_options.normalize_text:
        normalized_text = normalize_text(normalized_text, detected_file_kind)

    return ParsedDocument(
        file_kind=detected_file_kind,
        mime_type=mime_type,
        source_extension=source_extension,
        text=normalized_text,
    )
