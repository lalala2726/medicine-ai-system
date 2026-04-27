from __future__ import annotations

import os
import re
import tempfile
from datetime import datetime
from pathlib import Path

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException

DEFAULT_SAFE_FILENAME = "downloaded_file"
_INVALID_FILENAME_PATTERN = re.compile(r'[<>:"|?*\x00-\x1f]')


def safe_filename(filename: str) -> str:
    """
    功能描述:
        对下载文件名进行安全化处理，防止路径穿越与非法字符导致落盘风险。

    参数说明:
        filename (str): 原始文件名，可能来自 URL 或响应头。

    返回值:
        str: 安全化后的文件名；当输入为空或非法时返回默认值 `downloaded_file`。

    异常说明:
        无。该函数不会主动抛出异常。
    """

    resolved = (filename or "").strip()
    basename = Path(resolved).name
    sanitized = basename.replace("/", "_").replace("\\", "_")
    sanitized = _INVALID_FILENAME_PATTERN.sub("_", sanitized)
    sanitized = sanitized.strip().strip(".")
    if not sanitized:
        return DEFAULT_SAFE_FILENAME
    return sanitized


def _ensure_writable_dir(path: Path) -> None:
    """
    功能描述:
        确保目标目录存在且可写，供下载文件落盘前执行目录可用性校验。

    参数说明:
        path (Path): 待校验目录路径。

    返回值:
        None: 校验成功时无返回值。

    异常说明:
        ServiceException:
            - 目录创建失败时抛出；
            - 目录不是有效目录或不可写时抛出。
    """

    try:
        path.mkdir(parents=True, exist_ok=True)
    except OSError as exc:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message=f"无法创建下载目录: {path}",
        ) from exc

    if not path.is_dir():
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message=f"下载目录不是有效目录: {path}",
        )
    if not os.access(path, os.W_OK | os.X_OK):
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message=f"下载目录不可写: {path}",
        )


def resolve_download_root_dir() -> Path:
    """
    功能描述:
        返回系统临时目录并完成可写性校验。

    参数说明:
        无。

    返回值:
        Path: 生效的系统临时目录绝对路径。

    异常说明:
        ServiceException:
            - 系统临时目录不可访问或不可写时抛出。
    """
    root_dir = Path(tempfile.gettempdir()).expanduser()
    _ensure_writable_dir(root_dir)
    return root_dir


def build_download_target_path(
        filename: str,
        now: datetime | None = None,
) -> Path:
    """
    功能描述:
        基于系统临时目录构造唯一下载文件路径，并保留原始文件后缀。

    参数说明:
        filename (str): 原始文件名。
        now (datetime | None): 时间戳注入点，默认值为 None；仅用于生成可读前缀。

    返回值:
        Path: 最终下载落盘路径（已预留唯一临时文件名）。

    异常说明:
        ServiceException:
            - 系统临时目录不可访问时抛出；
            - 创建临时文件失败时抛出。
    """
    root_dir = resolve_download_root_dir()
    resolved_now = now or datetime.now()
    resolved_filename = safe_filename(filename)
    suffix = Path(resolved_filename).suffix
    prefix = f"medicine_ai_agent_{resolved_now:%Y%m%d}_"

    try:
        fd, raw_path = tempfile.mkstemp(
            dir=root_dir,
            prefix=prefix,
            suffix=suffix,
        )
    except OSError as exc:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message=f"无法创建临时下载文件: {root_dir}",
        ) from exc

    os.close(fd)
    return Path(raw_path)


__all__ = [
    "build_download_target_path",
    "resolve_download_root_dir",
    "safe_filename",
]
