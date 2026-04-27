from __future__ import annotations

import pytest

import app.utils.resource_text_utils as resource_text_module
from app.utils.resource_text_utils import (
    load_resource_text,
    load_resource_text_from_root,
)


@pytest.fixture(autouse=True)
def reset_resources_dir(monkeypatch, tmp_path):
    monkeypatch.setattr(resource_text_module, "RESOURCES_DIR", tmp_path)


def test_load_resource_text_reads_nested_file(tmp_path):
    target_file = tmp_path / "prompt" / "admin" / "chat.md"
    target_file.parent.mkdir(parents=True, exist_ok=True)
    target_file.write_text("hello", encoding="utf-8")

    assert load_resource_text("prompt", "admin/chat.md") == "hello"


def test_load_resource_text_raises_for_empty_name():
    with pytest.raises(ValueError):
        load_resource_text("prompt", "   ")


def test_load_resource_text_rejects_absolute_path(tmp_path):
    absolute_path = str((tmp_path / "prompt" / "a.md").resolve())
    with pytest.raises(ValueError):
        load_resource_text("prompt", absolute_path)


def test_load_resource_text_rejects_parent_traversal():
    with pytest.raises(ValueError):
        load_resource_text("prompt", "../secret.md")


def test_load_resource_text_rejects_symlink_escape(tmp_path):
    prompt_root = tmp_path / "prompt"
    prompt_root.mkdir(parents=True, exist_ok=True)

    outside_file = tmp_path / "outside.md"
    outside_file.write_text("outside", encoding="utf-8")

    link_file = prompt_root / "system" / "leak.md"
    link_file.parent.mkdir(parents=True, exist_ok=True)
    try:
        link_file.symlink_to(outside_file)
    except OSError:
        pytest.skip("当前环境不支持创建符号链接")

    with pytest.raises(ValueError):
        load_resource_text("prompt", "system/leak.md")


def test_load_resource_text_validates_allowed_suffixes(tmp_path):
    lua_file = tmp_path / "rate_limit" / "sliding_window.lua"
    lua_file.parent.mkdir(parents=True, exist_ok=True)
    lua_file.write_text("-- lua", encoding="utf-8")

    assert load_resource_text(
        "rate_limit",
        "sliding_window.lua",
        allowed_suffixes=(".lua",),
    ) == "-- lua"

    with pytest.raises(ValueError):
        load_resource_text(
            "rate_limit",
            "sliding_window.lua",
            allowed_suffixes=(".md",),
        )


def test_load_resource_text_uses_cache(tmp_path):
    prompt_file = tmp_path / "prompt" / "admin_demo_prompt.md"
    prompt_file.parent.mkdir(parents=True, exist_ok=True)
    prompt_file.write_text("v1", encoding="utf-8")
    cache: dict[str, str] = {}

    assert load_resource_text(
        "prompt",
        "admin_demo_prompt.md",
        allowed_suffixes=(".md",),
        cache=cache,
    ) == "v1"

    prompt_file.write_text("v2", encoding="utf-8")
    assert load_resource_text(
        "prompt",
        "admin_demo_prompt.md",
        allowed_suffixes=(".md",),
        cache=cache,
    ) == "v1"


def test_load_resource_text_from_root_reads_text(tmp_path):
    custom_root = tmp_path / "custom"
    custom_file = custom_root / "foo.md"
    custom_file.parent.mkdir(parents=True, exist_ok=True)
    custom_file.write_text("ok", encoding="utf-8")

    assert load_resource_text_from_root(
        custom_root,
        "foo.md",
        allowed_suffixes=(".md",),
    ) == "ok"


def test_load_resource_text_rejects_invalid_resource_subdir():
    with pytest.raises(ValueError):
        load_resource_text("../prompt", "x.md")
