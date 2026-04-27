from __future__ import annotations

import pytest

from app.core import prompt_sync
from app.core.prompt_sync import AgentPromptSnapshot


def _build_snapshot(prompts: dict[str, str]) -> AgentPromptSnapshot:
    """构造测试使用的提示词快照。"""

    return AgentPromptSnapshot(
        schema_version=1,
        updated_at=None,
        updated_by="tester",
        prompts=prompts,
    )


def test_load_managed_prompt_prefers_redis_snapshot_value(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：Redis 快照有值时应优先返回；预期结果：忽略本地回退路径。"""

    monkeypatch.setattr(
        prompt_sync,
        "get_current_agent_prompt_snapshot",
        lambda: _build_snapshot({"client_service_node_system_prompt": "redis prompt"}),
    )
    monkeypatch.setattr(
        prompt_sync,
        "load_resource_text",
        lambda *args, **kwargs: "local prompt",
    )

    result = prompt_sync.load_managed_prompt(
        "client_service_node_system_prompt",
        local_prompt_path="client/service_node_system_prompt.md",
    )

    assert result == "redis prompt"


def test_load_managed_prompt_uses_local_path_when_redis_value_missing(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：Redis 未命中时支持本地回退；预期结果：返回本地提示词内容。"""

    monkeypatch.setattr(
        prompt_sync,
        "get_current_agent_prompt_snapshot",
        lambda: _build_snapshot({}),
    )
    monkeypatch.setattr(
        prompt_sync,
        "load_resource_text",
        lambda *args, **kwargs: "local fallback prompt",
    )

    result = prompt_sync.load_managed_prompt(
        "client_service_node_system_prompt",
        local_prompt_path="client/service_node_system_prompt.md",
    )

    assert result == "local fallback prompt"


def test_load_managed_prompt_returns_empty_when_redis_missing_and_no_local_path(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：未提供本地路径时不抛错；预期结果：Redis 缺失返回空字符串。"""

    monkeypatch.setattr(
        prompt_sync,
        "get_current_agent_prompt_snapshot",
        lambda: _build_snapshot({}),
    )

    result = prompt_sync.load_managed_prompt("client_service_node_system_prompt")

    assert result == ""


def test_load_managed_prompt_returns_empty_when_local_fallback_load_fails(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：本地回退读取失败时保证可继续；预期结果：返回空字符串。"""

    monkeypatch.setattr(
        prompt_sync,
        "get_current_agent_prompt_snapshot",
        lambda: _build_snapshot({}),
    )

    def _raise_file_not_found(*args, **kwargs):
        raise FileNotFoundError("missing file")

    monkeypatch.setattr(
        prompt_sync,
        "load_resource_text",
        _raise_file_not_found,
    )

    result = prompt_sync.load_managed_prompt(
        "client_service_node_system_prompt",
        local_prompt_path="client/service_node_system_prompt.md",
    )

    assert result == ""


def test_load_managed_prompt_raises_when_prompt_key_is_empty() -> None:
    """测试目的：空提示词键应被参数校验拦截；预期结果：抛出 ValueError。"""

    with pytest.raises(ValueError):
        prompt_sync.load_managed_prompt("   ")
