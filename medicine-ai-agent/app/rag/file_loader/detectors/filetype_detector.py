from __future__ import annotations

from importlib import import_module
from pathlib import Path

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.rag.file_loader.detectors.type_mapping import (
    file_kind_from_extension,
    file_kind_from_mime,
    normalize_extension,
)
from app.rag.file_loader.types import FileKind


def _load_filetype_module():
    """
    功能描述:
        动态加载 filetype 模块，避免在模块导入阶段形成硬依赖。

    参数说明:
        无。

    返回值:
        module: filetype 模块对象。

    异常说明:
        ServiceException: 未安装 filetype 时抛出。
    """
    try:
        return import_module("filetype")
    except Exception as exc:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message="缺少 filetype 依赖，无法识别文件类型，请先安装 filetype",
        ) from exc


def _detect_mime_type(file_path: Path) -> str | None:
    """
    功能描述:
        基于 filetype 读取文件头并识别 MIME 类型。

    参数说明:
        file_path (Path): 待识别文件路径。

    返回值:
        str | None: 识别出的 MIME 类型，识别不到时返回 None。

    异常说明:
        ServiceException: filetype 调用异常时抛出。
    """
    filetype_module = _load_filetype_module()
    try:
        kind = filetype_module.guess(str(file_path))
    except Exception as exc:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message="文件类型识别失败，请确认 filetype 依赖可用",
        ) from exc

    mime_type = getattr(kind, "mime", None) if kind else None
    normalized = (mime_type or "").strip().lower()
    return normalized or None


def detect_file_kind(file_path: Path) -> tuple[FileKind, str | None]:
    """
    功能描述:
        执行下载后二次类型识别，优先依据文件头 MIME，兜底依据文件后缀。

    参数说明:
        file_path (Path): 已下载文件路径。

    返回值:
        tuple[FileKind, str | None]:
            - 第 1 项: 识别后的文件类型；
            - 第 2 项: 识别出的 MIME 类型（可能为 None）。

    异常说明:
        ServiceException:
            - 文件不存在时抛出；
            - 文件类型无法识别或不受支持时抛出。
    """
    if not file_path.exists() or not file_path.is_file():
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message=f"文件不存在或不是有效文件: {file_path}",
        )

    mime_type = _detect_mime_type(file_path)
    file_kind = file_kind_from_mime(mime_type)
    if file_kind:
        return file_kind, mime_type

    file_kind = file_kind_from_extension(normalize_extension(file_path.suffix))
    if file_kind:
        return file_kind, mime_type

    raise ServiceException(
        code=ResponseCode.BAD_REQUEST,
        message=f"无法识别或不支持的文件类型: {file_path.suffix or 'unknown'}",
    )
