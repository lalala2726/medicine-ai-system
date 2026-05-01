from __future__ import annotations

import os
from collections.abc import Awaitable, Callable, Mapping
from typing import Any

from langchain.agents.middleware import AgentMiddleware, ModelRequest, ModelResponse
from langchain_core.messages import SystemMessage

# DashScope 显式上下文缓存开关环境变量名称。
DASHSCOPE_EXPLICIT_CACHE_ENABLED_ENV = "DASHSCOPE_EXPLICIT_CACHE_ENABLED"
# DashScope 显式缓存标记类型，当前官方仅支持 ephemeral。
_DASHSCOPE_CACHE_CONTROL_TYPE = "ephemeral"
# OpenAI 兼容协议文本块类型。
_OPENAI_TEXT_CONTENT_TYPE = "text"


def _is_truthy_env(value: str | None) -> bool:
    """
    功能描述：
        判断环境变量文本是否表示开启。

    参数说明：
        value (str | None): 环境变量原始文本。

    返回值：
        bool: 文本为 1/true/yes/on 时返回 True。
    """

    return str(value or "").strip().lower() in {"1", "true", "yes", "on"}


def is_dashscope_explicit_cache_enabled() -> bool:
    """
    功能描述：
        判断是否开启 DashScope 显式上下文缓存。

    参数说明：
        无。

    返回值：
        bool: `DASHSCOPE_EXPLICIT_CACHE_ENABLED=true` 时返回 True。
    """

    return _is_truthy_env(os.getenv(DASHSCOPE_EXPLICIT_CACHE_ENABLED_ENV))


def has_dashscope_explicit_cache_control(content: Any) -> bool:
    """
    功能描述：
        判断消息内容中是否已经存在 DashScope 显式缓存标记。

    参数说明：
        content (Any): LangChain 消息 content，可能是字符串或 content block 数组。

    返回值：
        bool: 存在 `cache_control` 标记时返回 True。
    """

    if isinstance(content, Mapping):
        cache_control = content.get("cache_control")
        return isinstance(cache_control, Mapping) and bool(cache_control.get("type"))
    if isinstance(content, list | tuple):
        return any(has_dashscope_explicit_cache_control(item) for item in content)
    return False


def _is_dashscope_chat_model(model: Any) -> bool:
    """
    功能描述：
        判断当前 LangChain 模型是否为项目封装的 DashScope ChatQwen。

    参数说明：
        model (Any): LangChain 模型对象。

    返回值：
        bool: 模型类名为 ChatQwen 时返回 True。
    """

    return model.__class__.__name__ == "ChatQwen"


def _extract_cacheable_text(content: Any) -> str:
    """
    功能描述：
        从 system message content 中提取可放入缓存标记的纯文本。

    参数说明：
        content (Any): LangChain 系统消息内容。

    返回值：
        str: 可缓存文本；无法提取时返回空字符串。
    """

    if isinstance(content, str):
        return content.strip()
    if not isinstance(content, list | tuple):
        return ""
    text_parts: list[str] = []
    for item in content:
        if isinstance(item, str) and item.strip():
            text_parts.append(item.strip())
            continue
        if not isinstance(item, Mapping):
            return ""
        item_type = str(item.get("type") or _OPENAI_TEXT_CONTENT_TYPE)
        if item_type != _OPENAI_TEXT_CONTENT_TYPE:
            return ""
        text = item.get("text")
        if isinstance(text, str) and text.strip():
            text_parts.append(text.strip())
    return "\n".join(text_parts).strip()


def _build_cached_system_message(system_message: SystemMessage) -> SystemMessage:
    """
    功能描述：
        为系统消息添加 OpenAI 兼容的 DashScope 显式缓存标记。

    参数说明：
        system_message (SystemMessage): 当前系统消息。

    返回值：
        SystemMessage: 带缓存标记的新系统消息；无法处理时返回原消息。
    """

    content = getattr(system_message, "content", None)
    if has_dashscope_explicit_cache_control(content):
        return system_message
    cacheable_text = _extract_cacheable_text(content)
    if not cacheable_text:
        return system_message
    cached_content = [
        {
            "type": _OPENAI_TEXT_CONTENT_TYPE,
            "text": cacheable_text,
            "cache_control": {"type": _DASHSCOPE_CACHE_CONTROL_TYPE},
        }
    ]
    return SystemMessage(
        content=cached_content,
        additional_kwargs=dict(getattr(system_message, "additional_kwargs", None) or {}),
        response_metadata=dict(getattr(system_message, "response_metadata", None) or {}),
        name=getattr(system_message, "name", None),
        id=getattr(system_message, "id", None),
    )


class DashScopeExplicitCacheMiddleware(AgentMiddleware):
    """
    功能描述：
        在指定聊天链路的 DashScope 模型调用前，为系统提示词添加显式上下文缓存标记。

    参数说明：
        无。

    返回值：
        无（middleware 对象）。
    """

    def _inject_cache_control(self, request: ModelRequest) -> ModelRequest:
        """
        功能描述：
            对符合条件的模型请求注入显式缓存标记。

        参数说明：
            request (ModelRequest): 当前模型请求。

        返回值：
            ModelRequest: 注入缓存标记后的请求；不满足条件时返回原请求。
        """

        if not is_dashscope_explicit_cache_enabled():
            return request
        if not _is_dashscope_chat_model(request.model):
            return request
        system_message = request.system_message
        if not isinstance(system_message, SystemMessage):
            return request
        cached_system_message = _build_cached_system_message(system_message)
        if cached_system_message is system_message:
            return request
        return request.override(system_message=cached_system_message)

    def wrap_model_call(
            self,
            request: ModelRequest,
            handler: Callable[[ModelRequest], ModelResponse],
    ) -> ModelResponse:
        """
        功能描述：
            同步模型调用前注入 DashScope 显式缓存标记。

        参数说明：
            request (ModelRequest): 当前模型请求。
            handler (Callable[[ModelRequest], ModelResponse]): 下游模型处理器。

        返回值：
            ModelResponse: 下游模型响应。
        """

        return handler(self._inject_cache_control(request))

    async def awrap_model_call(
            self,
            request: ModelRequest,
            handler: Callable[[ModelRequest], Awaitable[ModelResponse]],
    ) -> ModelResponse:
        """
        功能描述：
            异步模型调用前注入 DashScope 显式缓存标记。

        参数说明：
            request (ModelRequest): 当前模型请求。
            handler (Callable[[ModelRequest], Awaitable[ModelResponse]]): 下游模型处理器。

        返回值：
            ModelResponse: 下游模型响应。
        """

        return await handler(self._inject_cache_control(request))
