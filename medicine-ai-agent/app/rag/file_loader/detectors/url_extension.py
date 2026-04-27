from __future__ import annotations

from pathlib import Path
from urllib.parse import urlparse

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.rag.file_loader.detectors.type_mapping import (
    SUPPORTED_URL_EXTENSIONS,
    normalize_extension,
)


def validate_url_extension(url: str) -> str:
    """
    功能描述:
        对导入 URL 执行第一阶段后缀校验，确保下载前即过滤不支持格式。

    参数说明:
        url (str): 文件下载 URL。

    返回值:
        str: 通过校验的标准化后缀（小写且包含点号）。

    异常说明:
        ServiceException:
            - URL 为空时抛出；
            - URL 缺少后缀时抛出；
            - URL 后缀不在支持列表时抛出。
    """
    resolved_url = (url or "").strip()
    if not resolved_url:
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message="文件 URL 不能为空",
        )

    suffix = normalize_extension(Path(urlparse(resolved_url).path).suffix)
    if not suffix:
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message=f"URL 缺少可识别文件后缀: {resolved_url}",
        )
    if suffix not in SUPPORTED_URL_EXTENSIONS:
        allowed = ", ".join(SUPPORTED_URL_EXTENSIONS)
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message=f"不支持的文件后缀: {suffix}，支持: {allowed}",
        )
    return suffix
