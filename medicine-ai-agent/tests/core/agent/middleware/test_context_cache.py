"""DashScope 显式上下文缓存 middleware 单元测试。"""

from __future__ import annotations

import asyncio
from types import SimpleNamespace
from typing import Any

from langchain_core.messages import SystemMessage

from app.core.agent.middleware.context_cache import (
    DASHSCOPE_EXPLICIT_CACHE_ENABLED_ENV,
    DashScopeExplicitCacheMiddleware,
    has_dashscope_explicit_cache_control,
    is_dashscope_explicit_cache_enabled,
)


class ChatQwen:
    """测试用 ChatQwen 模型桩，类名用于匹配 middleware 的 DashScope 判断。"""


class OtherModel:
    """测试用非 DashScope 模型桩。"""


class _FakeModelRequest:
    """测试用模型请求，模拟 LangChain ModelRequest 的 override 行为。"""

    def __init__(self, *, model: Any, system_message: Any) -> None:
        """
        功能描述：
            初始化测试模型请求。

        参数说明：
            model (Any): 模型对象。
            system_message (Any): 系统消息对象。

        返回值：
            None。
        """

        self.model = model
        self.system_message = system_message

    def override(self, **updates: Any) -> "_FakeModelRequest":
        """
        功能描述：
            返回应用局部字段更新后的新请求。

        参数说明：
            **updates (Any): 需要覆盖的请求字段。

        返回值：
            _FakeModelRequest: 新的测试模型请求。
        """

        return _FakeModelRequest(
            model=updates.get("model", self.model),
            system_message=updates.get("system_message", self.system_message),
        )


def test_is_dashscope_explicit_cache_enabled_reads_truthy_env(
        monkeypatch,
) -> None:
    """验证显式缓存开关只在 truthy 环境变量下开启。"""
    monkeypatch.setenv(DASHSCOPE_EXPLICIT_CACHE_ENABLED_ENV, "true")

    assert is_dashscope_explicit_cache_enabled() is True

    monkeypatch.setenv(DASHSCOPE_EXPLICIT_CACHE_ENABLED_ENV, "false")

    assert is_dashscope_explicit_cache_enabled() is False


def test_has_dashscope_explicit_cache_control_detects_nested_content() -> None:
    """验证缓存标记检测支持 OpenAI 兼容 content block 数组。"""
    content = [
        {"type": "text", "text": "系统提示"},
        {
            "type": "text",
            "text": "可缓存内容",
            "cache_control": {"type": "ephemeral"},
        },
    ]

    assert has_dashscope_explicit_cache_control(content) is True
    assert has_dashscope_explicit_cache_control("纯文本") is False


def test_wrap_model_call_injects_cache_control_for_chatqwen_system_message(
        monkeypatch,
) -> None:
    """验证开启显式缓存后，ChatQwen 系统提示词会被转换成官方 cache_control 结构。"""
    monkeypatch.setenv(DASHSCOPE_EXPLICIT_CACHE_ENABLED_ENV, "true")
    request = _FakeModelRequest(
        model=ChatQwen(),
        system_message=SystemMessage(content="稳定系统提示词"),
    )
    captured_requests: list[_FakeModelRequest] = []

    def _handler(received_request: _FakeModelRequest) -> SimpleNamespace:
        """
        功能描述：
            捕获 middleware 传递给下游的模型请求。

        参数说明：
            received_request (_FakeModelRequest): 注入缓存标记后的请求。

        返回值：
            SimpleNamespace: 测试响应对象。
        """

        captured_requests.append(received_request)
        return SimpleNamespace(ok=True)

    response = DashScopeExplicitCacheMiddleware().wrap_model_call(request, _handler)

    assert response.ok is True
    assert len(captured_requests) == 1
    assert captured_requests[0] is not request
    assert captured_requests[0].system_message.content == [
        {
            "type": "text",
            "text": "稳定系统提示词",
            "cache_control": {"type": "ephemeral"},
        }
    ]


def test_wrap_model_call_keeps_request_when_disabled_or_non_chatqwen(
        monkeypatch,
) -> None:
    """验证显式缓存关闭或非 ChatQwen 模型时不会改写请求。"""
    middleware = DashScopeExplicitCacheMiddleware()
    request = _FakeModelRequest(
        model=ChatQwen(),
        system_message=SystemMessage(content="稳定系统提示词"),
    )
    captured_requests: list[_FakeModelRequest] = []

    def _handler(received_request: _FakeModelRequest) -> SimpleNamespace:
        """
        功能描述：
            捕获 middleware 透传请求。

        参数说明：
            received_request (_FakeModelRequest): middleware 透传的请求。

        返回值：
            SimpleNamespace: 测试响应对象。
        """

        captured_requests.append(received_request)
        return SimpleNamespace(ok=True)

    monkeypatch.setenv(DASHSCOPE_EXPLICIT_CACHE_ENABLED_ENV, "false")
    middleware.wrap_model_call(request, _handler)

    monkeypatch.setenv(DASHSCOPE_EXPLICIT_CACHE_ENABLED_ENV, "true")
    non_chatqwen_request = _FakeModelRequest(
        model=OtherModel(),
        system_message=SystemMessage(content="稳定系统提示词"),
    )
    middleware.wrap_model_call(non_chatqwen_request, _handler)

    assert captured_requests == [request, non_chatqwen_request]


def test_wrap_model_call_does_not_duplicate_existing_cache_control(
        monkeypatch,
) -> None:
    """验证已有 cache_control 的系统消息不会被重复包裹。"""
    monkeypatch.setenv(DASHSCOPE_EXPLICIT_CACHE_ENABLED_ENV, "true")
    system_message = SystemMessage(
        content=[
            {
                "type": "text",
                "text": "稳定系统提示词",
                "cache_control": {"type": "ephemeral"},
            }
        ]
    )
    request = _FakeModelRequest(model=ChatQwen(), system_message=system_message)
    captured_requests: list[_FakeModelRequest] = []

    def _handler(received_request: _FakeModelRequest) -> SimpleNamespace:
        """
        功能描述：
            捕获 middleware 透传请求。

        参数说明：
            received_request (_FakeModelRequest): middleware 透传的请求。

        返回值：
            SimpleNamespace: 测试响应对象。
        """

        captured_requests.append(received_request)
        return SimpleNamespace(ok=True)

    DashScopeExplicitCacheMiddleware().wrap_model_call(request, _handler)

    assert captured_requests == [request]


def test_awrap_model_call_injects_cache_control_for_chatqwen_system_message(
        monkeypatch,
) -> None:
    """验证异步模型调用路径同样会注入显式缓存标记。"""
    monkeypatch.setenv(DASHSCOPE_EXPLICIT_CACHE_ENABLED_ENV, "true")
    request = _FakeModelRequest(
        model=ChatQwen(),
        system_message=SystemMessage(content="异步系统提示词"),
    )
    captured_requests: list[_FakeModelRequest] = []

    async def _handler(received_request: _FakeModelRequest) -> SimpleNamespace:
        """
        功能描述：
            捕获异步 middleware 传递给下游的模型请求。

        参数说明：
            received_request (_FakeModelRequest): 注入缓存标记后的请求。

        返回值：
            SimpleNamespace: 测试响应对象。
        """

        captured_requests.append(received_request)
        return SimpleNamespace(ok=True)

    response = asyncio.run(DashScopeExplicitCacheMiddleware().awrap_model_call(request, _handler))

    assert response.ok is True
    assert captured_requests[0].system_message.content[0]["cache_control"] == {"type": "ephemeral"}
