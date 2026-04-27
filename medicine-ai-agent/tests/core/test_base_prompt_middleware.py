from __future__ import annotations

import asyncio

import pytest
from langchain.agents.middleware import ModelRequest, ModelResponse
from langchain_core.messages import AIMessage, SystemMessage

import app.core.agent.base_prompt_middleware as base_prompt_module
from app.core.agent.base_prompt_middleware import BasePromptMiddleware
from app.core.agent.skill.prompt.templates import build_skills_prompt


def _make_request(system_text: str) -> ModelRequest:
    """构造测试用模型请求。"""

    return ModelRequest(
        model=object(),
        messages=[],
        system_message=SystemMessage(content=system_text),
        tools=[],
        state={},
        runtime=None,
    )


def test_wrap_model_call_appends_base_section_when_no_skills(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """验证无技能段时基础提示词追加到末尾。"""

    monkeypatch.setattr(base_prompt_module, "load_prompt", lambda _path: "通用规则内容")
    middleware = BasePromptMiddleware()
    request = _make_request("角色提示词")
    captured: dict[str, ModelRequest] = {}

    def handler(modified_request: ModelRequest) -> ModelResponse:
        captured["request"] = modified_request
        return ModelResponse(result=[AIMessage(content="ok")])

    middleware.wrap_model_call(request, handler)
    system_text = captured["request"].system_message.text

    assert "角色提示词" in system_text
    assert "通用规则内容" in system_text
    assert system_text.index("角色提示词") < system_text.index("通用规则内容")


def test_wrap_model_call_is_idempotent_when_base_section_exists(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """验证已存在基础提示词内容时不会重复追加。"""

    monkeypatch.setattr(base_prompt_module, "load_prompt", lambda _path: "通用规则内容")
    middleware = BasePromptMiddleware()
    request = _make_request("角色提示词\n\n通用规则内容")
    captured: dict[str, ModelRequest] = {}

    def handler(modified_request: ModelRequest) -> ModelResponse:
        captured["request"] = modified_request
        return ModelResponse(result=[AIMessage(content="ok")])

    middleware.wrap_model_call(request, handler)
    system_text = captured["request"].system_message.text

    assert system_text.count("通用规则内容") == 1


def test_wrap_model_call_inserts_base_before_skills_section(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """验证存在技能段时基础提示词插入到技能段前。"""

    monkeypatch.setattr(base_prompt_module, "load_prompt", lambda _path: "通用规则内容")
    middleware = BasePromptMiddleware()
    skills_section = build_skills_prompt([{"name": "analysis", "description": "analysis desc"}])
    request = _make_request(f"角色提示词\n\n{skills_section}")
    captured: dict[str, ModelRequest] = {}

    def handler(modified_request: ModelRequest) -> ModelResponse:
        captured["request"] = modified_request
        return ModelResponse(result=[AIMessage(content="ok")])

    middleware.wrap_model_call(request, handler)
    system_text = captured["request"].system_message.text

    assert system_text.index("角色提示词") < system_text.index("通用规则内容")
    assert system_text.index("通用规则内容") < system_text.index("name: analysis")


def test_awrap_model_call_behaves_like_sync_version(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """验证异步版本与同步版本注入行为一致。"""

    monkeypatch.setattr(base_prompt_module, "load_prompt", lambda _path: "通用规则内容")
    middleware = BasePromptMiddleware()
    request = _make_request("角色提示词")
    captured: dict[str, ModelRequest] = {}

    async def handler(modified_request: ModelRequest) -> ModelResponse:
        captured["request"] = modified_request
        return ModelResponse(result=[AIMessage(content="ok")])

    asyncio.run(middleware.awrap_model_call(request, handler))
    system_text = captured["request"].system_message.text
    assert "通用规则内容" in system_text
