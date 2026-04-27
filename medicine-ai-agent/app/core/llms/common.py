from __future__ import annotations

import os
from typing import TYPE_CHECKING, Any

from dotenv import load_dotenv

if TYPE_CHECKING:
    from app.core.llms.provider import LlmProvider

_LLM_ENV_LOADED = False


def ensure_llm_env_loaded() -> None:
    """
    功能描述:
        加载 LLM 相关环境变量，统一采用 `python-dotenv` 语义，
        并保证在进程内只执行一次，避免重复加载带来的开销。

    参数说明:
        无。

    返回值:
        None

    异常说明:
        无（`load_dotenv` 默认容错；未找到 `.env` 时不会抛出异常）。
    """

    global _LLM_ENV_LOADED
    if _LLM_ENV_LOADED:
        return
    load_dotenv(override=False)
    _LLM_ENV_LOADED = True


def resolve_llm_value(
        *,
        name: str,
        explicit: str | None = None,
        default: str | None = None,
) -> str | None:
    """
    功能描述:
        解析 LLM 配置值，统一遵循优先级：函数显式参数 > 已加载环境变量 > 默认值。

    参数说明:
        name (str): 配置键名。
        explicit (str | None): 调用方显式传入值；默认值 `None`。
        default (str | None): 当显式与环境均缺失时的兜底值；默认值 `None`。

    返回值:
        str | None:
            - 命中显式参数或环境变量时，返回去空白后的字符串；
            - 均未命中时返回 `default`（可为 `None`）。

    异常说明:
        无。
    """

    explicit_value = (explicit or "").strip()
    if explicit_value:
        return explicit_value

    ensure_llm_env_loaded()
    env_value = (os.getenv(name) or "").strip()
    if env_value:
        return env_value

    return default


def resolve_provider_extra_body(
        *,
        provider: LlmProvider | str,
        extra_body: dict[str, Any] | None = None,
        think: bool = False,
) -> dict[str, Any] | None:
    """
    功能描述:
        按 provider 统一归一化深度思考请求参数，输出可直接透传给模型客户端
        的 `extra_body`，并保证不修改调用方传入对象。

    参数说明:
        provider (LlmProvider | str): 模型提供商标识；
            当前仅支持 `aliyun`。
        extra_body (dict[str, Any] | None): 调用方传入的扩展请求体；默认值 `None`。
        think (bool): 是否开启深度思考；默认值 `False`。
            - `True`：显式开启阿里云百联思考；
            - `False`：显式关闭阿里云百联思考。

    返回值:
        dict[str, Any] | None:
            - 返回归一化后的 `extra_body`；
            - 当归一化后为空对象时返回 `None`。

    异常说明:
        ValueError: 当 provider 取值不受支持时，由 `normalize_provider` 抛出。
    """

    from app.core.llms.provider import LlmProvider, normalize_provider

    resolved_provider = normalize_provider(provider)
    resolved_extra_body = dict(extra_body or {})

    if resolved_provider is LlmProvider.ALIYUN:
        # 阿里云结构化输出链路显式绑定思考开关，避免服务端默认策略带来行为差异。
        resolved_extra_body["enable_thinking"] = bool(think)

    return resolved_extra_body or None


def prepare_chat_client_kwargs(
        *,
        extra_body: dict[str, Any] | None = None,
        **kwargs: Any,
) -> dict[str, Any]:
    """
    功能描述:
        统一准备聊天客户端构造参数，负责复制并规整 `model_kwargs/extra_body`，
        同时默认开启 `stream_usage=True`，避免 provider 实现重复代码。

    参数说明:
        extra_body (dict[str, Any] | None): 透传给模型服务端的扩展请求体；默认值 `None`。
        **kwargs (Any): 调用方传入的其余构造参数。

    返回值:
        dict[str, Any]: 规整后的构造参数副本，可直接用于模型客户端初始化。

    异常说明:
        无。
    """

    prepared_kwargs = dict(kwargs)

    raw_model_kwargs = prepared_kwargs.pop("model_kwargs", None)
    model_kwargs = dict(raw_model_kwargs or {})
    if model_kwargs:
        prepared_kwargs["model_kwargs"] = model_kwargs

    resolved_extra_body = dict(extra_body or {})
    if resolved_extra_body:
        prepared_kwargs["extra_body"] = resolved_extra_body

    prepared_kwargs.setdefault("stream_usage", True)
    return prepared_kwargs
