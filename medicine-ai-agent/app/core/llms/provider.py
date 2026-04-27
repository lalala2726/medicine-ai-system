from __future__ import annotations

from enum import Enum

from app.core.llms.common import resolve_llm_value


class LlmProvider(str, Enum):
    """
    功能描述:
        定义聊天模型提供商枚举，统一约束工厂函数 `provider` 入参取值，
        避免调用方传入拼写错误的厂商标识。

    参数说明:
        无（枚举类型定义）。

    返回值:
        无（类型定义）。

    异常说明:
        无（异常由枚举解析阶段在调用方触发）。
    """

    ALIYUN = "aliyun"


def normalize_provider(provider: LlmProvider | str) -> LlmProvider:
    """
    功能描述:
        归一化厂商参数，支持 `LlmProvider` 或字符串输入，
        统一转换为 `LlmProvider` 枚举实例。

    参数说明:
        provider (LlmProvider | str): 厂商标识；
            - 当为 `LlmProvider` 时直接返回；
            - 当为字符串时按枚举值解析（大小写不敏感）；
            - 兼容 `LlmProvider.ALIYUN` 这类字符串写法。

    返回值:
        LlmProvider: 归一化后的厂商枚举值。

    异常说明:
        ValueError: 当字符串不在支持范围内时抛出。
    """

    if isinstance(provider, LlmProvider):
        return provider

    raw_provider = str(provider).strip()
    if not raw_provider:
        raise ValueError("LLM provider is empty; allowed values: aliyun")

    # 兼容字符串写法：`LlmProvider.ALIYUN` / `llmprovider.aliyun`
    if "." in raw_provider:
        raw_provider = raw_provider.split(".")[-1]
    normalized = raw_provider.strip().lower()

    try:
        return LlmProvider(normalized)
    except ValueError as exc:
        raise ValueError(
            f"Unsupported LLM provider: {provider}; allowed values: aliyun"
        ) from exc


def resolve_provider(provider: LlmProvider | str | None = None) -> LlmProvider:
    """
    功能描述:
        解析最终 provider，统一遵循优先级：函数参数 > 环境变量（含 python-dotenv 加载结果） > `aliyun`。

    参数说明:
        provider (LlmProvider | str | None): 调用方显式 provider；默认值 `None`。

    返回值:
        LlmProvider: 归一化后的 provider 枚举值。

    异常说明:
        ValueError: 当 provider 字符串不在支持范围内时抛出。
    """

    if provider is not None:
        return normalize_provider(provider)

    configured_provider = resolve_llm_value(name="LLM_PROVIDER")
    if configured_provider:
        return normalize_provider(configured_provider)

    return LlmProvider.ALIYUN
