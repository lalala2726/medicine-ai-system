from __future__ import annotations

from typing import Any

from langchain_openai import OpenAIEmbeddings

from app.core.llms.provider import LlmProvider, resolve_provider
from app.core.llms.providers import (
    create_aliyun_embedding_model,
)


def _validate_embedding_dimensions(dimensions: int | None) -> None:
    """
    功能描述:
        校验嵌入模型维度配置，确保与当前向量服务支持范围一致。

    参数说明:
        dimensions (int | None): 向量维度；默认值由调用方决定。
            当为 `None` 时表示不显式下发维度参数，直接跳过校验。

    返回值:
        None

    异常说明:
        ValueError: 当维度不在 [128, 4096] 或不是 2 的倍数时抛出。
    """

    if dimensions is None:
        return
    if dimensions < 128 or dimensions > 4096 or dimensions % 2 != 0:
        raise ValueError("Dimensions must be between 128 and 4096 and a multiple of 2")


def create_embedding_model(
        *,
        provider: LlmProvider | str | None = None,
        model: str | None = None,
        api_key: str | None = None,
        base_url: str | None = None,
        dimensions: int | None = 1024,
        **kwargs: Any,
) -> OpenAIEmbeddings:
    """
    功能描述:
        按 provider 创建向量嵌入模型客户端。

    参数说明:
        provider (LlmProvider | str | None): 模型厂商；默认值 `None`。
            优先使用显式入参；未传时读取 `LLM_PROVIDER`，再回退到 `aliyun`。
        model (str | None): 嵌入模型名称；默认值 `None`。
            模型名需由显式参数或对应 provider 的环境变量提供。
        api_key (str | None): 覆盖厂商 API 密钥；默认值 `None`。
        base_url (str | None): 覆盖厂商 API Base URL；默认值 `None`。
        dimensions (int | None): 向量维度；默认值 `1024`。
            当不为 `None` 时需满足范围 [128, 4096] 且为偶数。
        **kwargs (Any): 其余透传 `OpenAIEmbeddings` 构造参数。

    返回值:
        OpenAIEmbeddings: 根据 provider 创建的向量嵌入客户端实例。

    异常说明:
        ValueError: 当 `provider` 不合法或 `dimensions` 不合法时抛出。
        RuntimeError: 当 provider 对应的必填密钥/模型名缺失时由下层抛出。
    """

    _validate_embedding_dimensions(dimensions)
    resolved_provider = resolve_provider(provider)

    if resolved_provider is not LlmProvider.ALIYUN:
        raise ValueError(f"Unsupported provider: {resolved_provider}")

    return create_aliyun_embedding_model(
        model=model,
        api_key=api_key,
        base_url=base_url,
        dimensions=dimensions,
        **kwargs,
    )
