from __future__ import annotations

from app.rag.file_loader.types import FileKind

EXTENSION_TO_FILE_KIND: dict[str, FileKind] = {
    ".txt": FileKind.TEXT,
    ".md": FileKind.MARKDOWN,
    ".pdf": FileKind.PDF,
    ".docx": FileKind.WORD,
    ".doc": FileKind.WORD,
    ".xlsx": FileKind.EXCEL,
    ".xls": FileKind.EXCEL,
    ".csv": FileKind.EXCEL,
    ".pptx": FileKind.PPT,
    ".ppt": FileKind.PPT,
}

MIME_TO_FILE_KIND: dict[str, FileKind] = {
    "text/plain": FileKind.TEXT,
    "text/markdown": FileKind.MARKDOWN,
    "text/x-markdown": FileKind.MARKDOWN,
    "application/pdf": FileKind.PDF,
    "application/msword": FileKind.WORD,
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document": FileKind.WORD,
    "application/vnd.ms-excel": FileKind.EXCEL,
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet": FileKind.EXCEL,
    "text/csv": FileKind.EXCEL,
    "application/vnd.ms-powerpoint": FileKind.PPT,
    "application/vnd.openxmlformats-officedocument.presentationml.presentation": FileKind.PPT,
}


def normalize_extension(extension: str | None) -> str:
    """
    功能描述:
        标准化扩展名格式，统一小写并确保以点号开头。

    参数说明:
        extension (str | None): 原始扩展名。

    返回值:
        str: 标准化后的扩展名；输入为空时返回空字符串。

    异常说明:
        无。
    """
    if not extension:
        return ""
    lowered = extension.strip().lower()
    if not lowered:
        return ""
    if lowered.startswith("."):
        return lowered
    return f".{lowered}"


def file_kind_from_extension(extension: str | None) -> FileKind | None:
    """
    功能描述:
        根据扩展名映射文件类型。

    参数说明:
        extension (str | None): 原始扩展名。

    返回值:
        FileKind | None: 命中映射时返回对应文件类型，否则返回 None。

    异常说明:
        无。
    """
    return EXTENSION_TO_FILE_KIND.get(normalize_extension(extension))


def file_kind_from_mime(mime_type: str | None) -> FileKind | None:
    """
    功能描述:
        根据 MIME 类型映射文件类型。

    参数说明:
        mime_type (str | None): MIME 类型字符串。

    返回值:
        FileKind | None: 命中映射时返回对应文件类型，否则返回 None。

    异常说明:
        无。
    """
    normalized_mime = (mime_type or "").strip().lower()
    if not normalized_mime:
        return None
    return MIME_TO_FILE_KIND.get(normalized_mime)


SUPPORTED_URL_EXTENSIONS = tuple(sorted(EXTENSION_TO_FILE_KIND.keys()))
