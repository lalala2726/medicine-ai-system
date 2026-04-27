from __future__ import annotations

import base64
import mimetypes
import os
from io import BytesIO
from pathlib import Path
from typing import Optional
from urllib.error import URLError
from urllib.parse import unquote, urlparse
from urllib.request import urlopen

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.utils.download_storage import build_download_target_path


class FileUtils:
    """文件下载与编码工具类。"""

    DEFAULT_DOWNLOAD_CHUNK_SIZE = 1024 * 1024  # 1MB
    DEFAULT_TIMEOUT = 30
    DEFAULT_IMAGE_MIME = "image/png"

    @staticmethod
    def resolve_filename_from_url(file_url: str) -> str:
        """从 URL 中解析文件名。"""
        parsed = urlparse(file_url)
        filename = os.path.basename(parsed.path)
        filename = unquote(filename)
        return filename or "downloaded_file"

    @staticmethod
    def _filename_from_content_disposition(
            content_disposition: Optional[str],
    ) -> Optional[str]:
        if not content_disposition:
            return None
        parts = [part.strip() for part in content_disposition.split(";") if part.strip()]
        for part in parts:
            lower_part = part.lower()
            if lower_part.startswith("filename*="):
                value = part.split("=", 1)[1].strip().strip("\"'")
                if value.lower().startswith("utf-8''"):
                    return unquote(value[7:])
                return value
            if lower_part.startswith("filename="):
                return part.split("=", 1)[1].strip().strip("\"'")
        return None

    @staticmethod
    def _normalize_content_type(content_type: Optional[str]) -> Optional[str]:
        if not content_type:
            return None
        return content_type.split(";", 1)[0].strip().lower() or None

    @staticmethod
    def _resolve_filename_from_headers(
            headers,
            fallback_filename: str,
            content_type: Optional[str],
    ) -> str:
        header_filename = FileUtils._filename_from_content_disposition(
            headers.get("Content-Disposition")
        )
        filename = header_filename or fallback_filename
        if not Path(filename).suffix and content_type:
            guessed = mimetypes.guess_extension(content_type)
            if guessed:
                filename = f"{filename}{guessed}"
        return filename

    @staticmethod
    def download_file(
            file_url: str,
            *,
            timeout: int = DEFAULT_TIMEOUT,
            chunk_size: int = DEFAULT_DOWNLOAD_CHUNK_SIZE,
    ) -> tuple[str, Path]:
        """
        功能描述:
            下载远程文件到系统临时目录，并返回解析后的文件名与本地路径。

        参数说明:
            file_url (str): 待下载文件 URL。
            timeout (int): 下载超时时间（秒），默认值为 `DEFAULT_TIMEOUT`。
            chunk_size (int): 流式读取块大小（字节），默认值为 `DEFAULT_DOWNLOAD_CHUNK_SIZE`。

        返回值:
            tuple[str, Path]:
                - 第 1 项: 解析后的文件名（优先响应头，其次 URL 推断）
                - 第 2 项: 本地落盘路径

        异常说明:
            ServiceException:
                - URL 无效或下载失败时抛出；
                - 系统临时目录不可写时由下游抛出；
                - 本地文件写入失败时抛出。
        """
        filename = FileUtils.resolve_filename_from_url(file_url)
        target_path: Path | None = None
        try:
            with urlopen(file_url, timeout=timeout) as response:
                content_type = FileUtils._normalize_content_type(
                    response.headers.get("Content-Type")
                )
                filename = FileUtils._resolve_filename_from_headers(
                    response.headers,
                    filename,
                    content_type,
                )
                target_path = build_download_target_path(filename)
                with target_path.open("wb") as target_file:
                    while True:
                        chunk = response.read(chunk_size)
                        if not chunk:
                            break
                        target_file.write(chunk)
                return filename, target_path
        except ServiceException:
            raise
        except (URLError, ValueError) as exc:
            if target_path and target_path.exists():
                target_path.unlink(missing_ok=True)
            raise ServiceException(
                code=ResponseCode.OPERATION_FAILED,
                message=f"下载文件失败: {file_url}",
            ) from exc
        except OSError as exc:
            if target_path and target_path.exists():
                target_path.unlink(missing_ok=True)
            raise ServiceException(
                code=ResponseCode.OPERATION_FAILED,
                message=f"下载文件失败: {file_url}",
            ) from exc

    @staticmethod
    def download_bytes(
            file_url: str,
            *,
            timeout: int = DEFAULT_TIMEOUT,
            chunk_size: int = DEFAULT_DOWNLOAD_CHUNK_SIZE,
    ) -> tuple[bytes, Optional[str], str]:
        """下载文件并返回字节、Content-Type 与文件名。"""
        filename = FileUtils.resolve_filename_from_url(file_url)
        try:
            with urlopen(file_url, timeout=timeout) as response:
                content_type = FileUtils._normalize_content_type(
                    response.headers.get("Content-Type")
                )
                filename = FileUtils._resolve_filename_from_headers(
                    response.headers,
                    filename,
                    content_type,
                )
                buffer = BytesIO()
                while True:
                    chunk = response.read(chunk_size)
                    if not chunk:
                        break
                    buffer.write(chunk)
                return buffer.getvalue(), content_type, filename
        except (URLError, ValueError) as exc:
            raise ServiceException(
                code=ResponseCode.OPERATION_FAILED,
                message=f"下载文件失败: {file_url}",
            ) from exc

    @staticmethod
    def _resolve_image_mime(
            content_type: Optional[str],
            filename: str,
            default_mime: str,
    ) -> str:
        if content_type and content_type.startswith("image/"):
            return content_type
        guessed = mimetypes.guess_type(filename)[0]
        if guessed and guessed.startswith("image/"):
            return guessed
        return default_mime

    @staticmethod
    def image_url_to_base64_data_url(
            image_url: str,
            *,
            default_mime: str = DEFAULT_IMAGE_MIME,
            timeout: int = DEFAULT_TIMEOUT,
            chunk_size: int = DEFAULT_DOWNLOAD_CHUNK_SIZE,
    ) -> str:
        """下载图片并转成 data URL 格式的 base64 字符串。"""
        data, content_type, filename = FileUtils.download_bytes(
            image_url,
            timeout=timeout,
            chunk_size=chunk_size,
        )
        mime_type = FileUtils._resolve_image_mime(
            content_type,
            filename,
            default_mime,
        )
        encoded = base64.b64encode(data).decode("ascii")
        return f"data:{mime_type};base64,{encoded}"
