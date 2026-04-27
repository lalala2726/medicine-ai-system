from __future__ import annotations

from typing import Any

from langchain_openai import ChatOpenAI, OpenAIEmbeddings
from pydantic import SecretStr

from app.core.llms.common import prepare_chat_client_kwargs, resolve_llm_value

DEFAULT_OPENAI_BASE_URL = "https://api.openai.com/v1"


def create_openai_chat_model(
        *,
        model: str | None = None,
        api_key: str | None = None,
        base_url: str | None = None,
        extra_body: dict[str, Any] | None = None,
        **kwargs: Any,
) -> ChatOpenAI:
    """
    功能描述:
        创建 OpenAI 兼容聊天模型客户端，并透传调用方提供的扩展参数。

    参数说明:
        model (str | None): 模型名称；默认值 `None`。
            为 `None` 时读取 `OPENAI_CHAT_MODEL`，若仍为空则报错。
        api_key (str | None): OpenAI API 密钥；默认值 `None`。
            为 `None` 时读取环境变量 `OPENAI_API_KEY`。
        base_url (str | None): OpenAI API 基础地址；默认值 `None`。
            为 `None` 时读取 `OPENAI_BASE_URL`，若未配置则使用 `https://api.openai.com/v1`。
        extra_body (dict[str, Any] | None): 透传给模型服务端的扩展字段；默认值 `None`。
        **kwargs (Any): 其余透传 `ChatOpenAI` 构造参数。

    返回值:
        ChatOpenAI: 可直接用于 `invoke/stream` 的聊天模型实例。

    异常说明:
        RuntimeError:
            - 当未提供 `api_key` 且环境变量 `OPENAI_API_KEY` 未设置时抛出；
            - 当未传 `model` 且环境变量 `OPENAI_CHAT_MODEL` 未设置时抛出。
    """

    key = resolve_llm_value(name="OPENAI_API_KEY", explicit=api_key)
    if not key:
        raise RuntimeError("OPENAI_API_KEY is not set")

    resolved_model = resolve_llm_value(name="OPENAI_CHAT_MODEL", explicit=model)
    if not resolved_model:
        raise RuntimeError("OPENAI_CHAT_MODEL is not set")
    resolved_base_url = resolve_llm_value(
        name="OPENAI_BASE_URL",
        explicit=base_url,
        default=DEFAULT_OPENAI_BASE_URL,
    )

    prepared_kwargs = prepare_chat_client_kwargs(extra_body=extra_body, **kwargs)

    return ChatOpenAI(
        model=resolved_model,
        api_key=SecretStr(key),
        base_url=resolved_base_url,
        **prepared_kwargs,
    )


def create_openai_image_model(
        *,
        model: str | None = None,
        api_key: str | None = None,
        base_url: str | None = None,
        extra_body: dict[str, Any] | None = None,
        **kwargs: Any,
) -> ChatOpenAI:
    """
    功能描述:
        创建 OpenAI 兼容图像理解模型客户端。

    参数说明:
        model (str | None): 图像模型名称；默认值 `None`。
            为 `None` 时读取 `OPENAI_IMAGE_MODEL`，若仍为空则报错。
        api_key (str | None): OpenAI API 密钥；默认值 `None`。
            为 `None` 时读取环境变量 `OPENAI_API_KEY`。
        base_url (str | None): OpenAI API 基础地址；默认值 `None`。
            为 `None` 时读取 `OPENAI_BASE_URL`，若未配置则使用 `https://api.openai.com/v1`。
        extra_body (dict[str, Any] | None): 透传扩展字段；默认值 `None`。
        **kwargs (Any): 其余透传 `ChatOpenAI` 构造参数。

    返回值:
        ChatOpenAI: 图像理解可用的聊天模型实例。

    异常说明:
        RuntimeError:
            - 当未提供 `api_key` 且环境变量 `OPENAI_API_KEY` 未设置时抛出；
            - 当未传 `model` 且环境变量 `OPENAI_IMAGE_MODEL` 未设置时抛出。
    """

    resolved_model = resolve_llm_value(name="OPENAI_IMAGE_MODEL", explicit=model)
    if not resolved_model:
        raise RuntimeError("OPENAI_IMAGE_MODEL is not set")

    return create_openai_chat_model(
        model=resolved_model,
        api_key=api_key,
        base_url=base_url,
        extra_body=extra_body,
        **kwargs,
    )


def create_openai_embedding_model(
        *,
        model: str | None = None,
        api_key: str | None = None,
        base_url: str | None = None,
        dimensions: int | None = 1024,
        **kwargs: Any,
) -> OpenAIEmbeddings:
    """
    功能描述:
        创建 OpenAI 兼容向量嵌入模型客户端。

    参数说明:
        model (str | None): 嵌入模型名称；默认值 `None`。
            为 `None` 时读取 `OPENAI_EMBEDDING_MODEL`，若仍为空则报错。
        api_key (str | None): OpenAI API 密钥；默认值 `None`。
            为 `None` 时读取环境变量 `OPENAI_API_KEY`。
        base_url (str | None): OpenAI API 基础地址；默认值 `None`。
            为 `None` 时读取 `OPENAI_BASE_URL`，若未配置则使用 `https://api.openai.com/v1`。
        dimensions (int | None): 向量维度；默认值 `1024`。
            传 `None` 时不向底层客户端设置该参数。
        **kwargs (Any): 其余透传 `OpenAIEmbeddings` 构造参数。

    返回值:
        OpenAIEmbeddings: 可直接用于文本向量化的嵌入模型实例。

    异常说明:
        RuntimeError:
            - 当未提供 `api_key` 且环境变量 `OPENAI_API_KEY` 未设置时抛出；
            - 当未传 `model` 且环境变量 `OPENAI_EMBEDDING_MODEL` 未设置时抛出。
    """

    key = resolve_llm_value(name="OPENAI_API_KEY", explicit=api_key)
    if not key:
        raise RuntimeError("OPENAI_API_KEY is not set")

    resolved_model = resolve_llm_value(name="OPENAI_EMBEDDING_MODEL", explicit=model)
    if not resolved_model:
        raise RuntimeError("OPENAI_EMBEDDING_MODEL is not set")

    resolved_base_url = resolve_llm_value(
        name="OPENAI_BASE_URL",
        explicit=base_url,
        default=DEFAULT_OPENAI_BASE_URL,
    )

    embedding_kwargs = dict(kwargs)
    if dimensions is not None:
        embedding_kwargs["dimensions"] = dimensions

    return OpenAIEmbeddings(
        model=resolved_model,
        check_embedding_ctx_length=False,
        api_key=SecretStr(key),
        base_url=resolved_base_url,
        **embedding_kwargs,
    )
