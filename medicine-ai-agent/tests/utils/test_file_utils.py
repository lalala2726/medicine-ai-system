from __future__ import annotations

import app.utils.download_storage as download_storage
import app.utils.file_utils as file_utils_module
from app.utils.file_utils import FileUtils


class _FakeResponse:
    def __init__(self, payload: bytes, headers: dict[str, str]) -> None:
        self._payload = payload
        self._offset = 0
        self.headers = headers

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb) -> bool:
        return False

    def read(self, size: int = -1) -> bytes:
        if self._offset >= len(self._payload):
            return b""
        if size < 0:
            size = len(self._payload) - self._offset
        chunk = self._payload[self._offset:self._offset + size]
        self._offset += len(chunk)
        return chunk


def test_download_file_writes_to_system_temp_path(monkeypatch, tmp_path):
    """
    测试目的：验证 download_file 使用系统临时目录目标路径写入文件且返回正确文件名。
    预期结果：文件写入成功，返回路径位于临时目录，文件内容与响应体一致。
    """
    monkeypatch.setattr(download_storage.tempfile, "gettempdir", lambda: str(tmp_path))
    monkeypatch.setattr(
        file_utils_module,
        "urlopen",
        lambda _url, timeout: _FakeResponse(
            b"hello world",
            {
                "Content-Type": "text/plain",
                "Content-Disposition": "attachment; filename*=UTF-8''doc.txt",
            },
        ),
    )

    filename, saved_path = FileUtils.download_file("http://example.com/download")

    assert filename == "doc.txt"
    assert saved_path.parent == tmp_path
    assert saved_path.suffix == ".txt"
    assert saved_path.read_bytes() == b"hello world"
    saved_path.unlink(missing_ok=True)


def test_download_file_uses_system_temp_dir_without_extra_config(monkeypatch, tmp_path):
    """
    测试目的：验证下载流程默认使用系统临时目录，无需额外下载目录配置。
    预期结果：download_file 成功返回，并将文件写入系统临时目录。
    """
    monkeypatch.setattr(download_storage.tempfile, "gettempdir", lambda: str(tmp_path))
    monkeypatch.setattr(
        file_utils_module,
        "urlopen",
        lambda _url, timeout: _FakeResponse(
            b"payload",
            {
                "Content-Type": "text/plain",
                "Content-Disposition": "attachment; filename=test.txt",
            },
        ),
    )

    filename, saved_path = FileUtils.download_file("http://example.com/download")

    assert filename == "test.txt"
    assert saved_path.parent == tmp_path
    assert saved_path.read_bytes() == b"payload"
    saved_path.unlink(missing_ok=True)
