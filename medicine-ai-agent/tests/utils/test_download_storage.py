from __future__ import annotations

from datetime import datetime

import app.utils.download_storage as download_storage


def test_resolve_download_root_dir_uses_system_temp_dir(monkeypatch, tmp_path):
    """
    测试目的：验证下载根目录统一解析为系统临时目录。
    预期结果：resolve_download_root_dir 返回 monkeypatch 后的临时目录。
    """
    monkeypatch.setattr(download_storage.tempfile, "gettempdir", lambda: str(tmp_path))
    assert download_storage.resolve_download_root_dir() == tmp_path


def test_build_download_target_path_uses_system_temp_dir_and_preserves_suffix(monkeypatch, tmp_path):
    """
    测试目的：验证目标路径位于系统临时目录下，并保留原始文件后缀。
    预期结果：返回路径位于临时目录，文件名带固定前缀且后缀正确。
    """
    monkeypatch.setattr(download_storage.tempfile, "gettempdir", lambda: str(tmp_path))

    target = download_storage.build_download_target_path(
        "../../unsafe?.txt",
        now=datetime(2026, 3, 3, 10, 20, 30),
    )

    assert target.parent == tmp_path
    assert target.suffix == ".txt"
    assert target.name.startswith("medicine_ai_agent_20260303_")
    assert target.exists()
    target.unlink(missing_ok=True)


def test_safe_filename_sanitizes_path_and_illegal_chars():
    """
    测试目的：验证文件名清洗逻辑可消除路径穿越与非法字符风险。
    预期结果：输出仅保留安全文件名，非法字符被替换；无有效名称时回退默认文件名。
    """

    assert download_storage.safe_filename("../../a/b:c?.txt") == "b_c_.txt"
    assert download_storage.safe_filename("..") == download_storage.DEFAULT_SAFE_FILENAME
