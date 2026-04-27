from __future__ import annotations

from app.core.codes import ResponseCode
from app.core.config_sync import KnowledgeBaseScope, get_current_agent_config_snapshot
from app.core.exception.exceptions import ServiceException
from app.core.llms.common import resolve_llm_value
from app.core.llms.provider import LlmProvider, resolve_provider
from app.rag.query.constants import RAG_DEFAULT_FINAL_TOP_K, RAG_MAX_FINAL_TOP_K, RAG_MAX_KNOWLEDGE_NAMES
from app.rag.query.types import KnowledgeSearchRuntimeConfig


def _get_scope_label(scope: KnowledgeBaseScope) -> str:
    """返回知识库作用域对应的中文提示前缀。"""

    if scope is KnowledgeBaseScope.CLIENT:
        return "客户端知识库"
    return "知识库"


def normalize_top_k(top_k: int | None) -> int | None:
    """规范化显式传入的最终返回条数。

    Args:
        top_k: 调用方期望返回的命中数量。

    Returns:
        校验通过后的正整数；未传时返回 ``None``。

    Raises:
        ServiceException: 当 ``top_k`` 超出允许范围时抛出。
    """

    if top_k is None:
        return None
    try:
        normalized_top_k = int(top_k)
    except (TypeError, ValueError) as exc:
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message="top_k 必须是 1 到 100 之间的整数",
        ) from exc
    if normalized_top_k <= 0 or normalized_top_k > RAG_MAX_FINAL_TOP_K:
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message="top_k 必须在 1 到 100 之间",
        )
    return normalized_top_k


def _normalize_optional_positive_int(value: object) -> int | None:
    """将可选正整数值规整为 ``int``。

    Args:
        value: 原始输入值，允许为 ``None``、数字或可转数字字符串。

    Returns:
        大于 ``0`` 的整数；输入为空、无法解析或小于等于 ``0`` 时返回 ``None``。
    """

    if value is None:
        return None
    if isinstance(value, str):
        normalized_value = value.strip()
        if not normalized_value:
            return None
        value = normalized_value
    try:
        resolved_value = int(value)
    except (TypeError, ValueError):
        return None
    if resolved_value <= 0:
        return None
    return resolved_value


def resolve_final_top_k(
        explicit_top_k: int | None,
        scope: KnowledgeBaseScope = KnowledgeBaseScope.ADMIN,
) -> int:
    """解析当前请求最终生效的返回条数。

    Args:
        explicit_top_k: 调用方显式传入的最终返回条数。
        scope: 知识库作用域。

    Returns:
        实际生效的最终返回条数。
    """

    normalized_explicit_top_k = normalize_top_k(explicit_top_k)
    if normalized_explicit_top_k is not None:
        return normalized_explicit_top_k

    configured_top_k = get_current_agent_config_snapshot().get_knowledge_top_k(scope)
    if configured_top_k is not None:
        return configured_top_k
    return RAG_DEFAULT_FINAL_TOP_K


def resolve_final_top_k_by_runtime_config(
        explicit_top_k: int | None,
        runtime_config: KnowledgeSearchRuntimeConfig,
) -> int:
    """基于显式运行时配置解析最终返回条数。

    Args:
        explicit_top_k: 调用方显式传入的最终返回条数。
        runtime_config: 当前请求显式指定的运行时配置。

    Returns:
        实际生效的最终返回条数。
    """

    normalized_explicit_top_k = normalize_top_k(explicit_top_k)
    if normalized_explicit_top_k is not None:
        return normalized_explicit_top_k
    if runtime_config.configured_top_k is not None:
        return runtime_config.configured_top_k
    return RAG_DEFAULT_FINAL_TOP_K


def _resolve_provider_embedding_fallback_model_name(provider_type: str) -> str | None:
    """读取当前 provider 对应的 embedding 环境变量模型名。

    Args:
        provider_type: 当前请求生效的 provider 类型。

    Returns:
        当前 provider 对应的 embedding 模型名称；未配置时返回 ``None``。
    """

    resolved_provider = resolve_provider(provider_type)
    if resolved_provider is not LlmProvider.ALIYUN:
        raise ServiceException(
            code=ResponseCode.SERVICE_UNAVAILABLE,
            message=f"不支持的模型提供商: {provider_type}",
        )
    return resolve_llm_value(name="DASHSCOPE_EMBEDDING_MODEL")


def resolve_runtime_config(
        scope: KnowledgeBaseScope = KnowledgeBaseScope.ADMIN,
) -> KnowledgeSearchRuntimeConfig:
    """解析当前知识检索链所需的运行时配置。

    Args:
        scope: 知识库作用域。

    Returns:
        当前请求生效的知识检索运行时配置。

    Raises:
        ServiceException: 当缺少必要知识库配置时抛出。
    """

    snapshot = get_current_agent_config_snapshot()
    scope_label = _get_scope_label(scope)
    if not snapshot.is_knowledge_enabled(scope):
        raise ServiceException(
            code=ResponseCode.SERVICE_UNAVAILABLE,
            message=f"{scope_label}检索未启用",
        )

    runtime_config = snapshot.get_llm_runtime_config()
    if runtime_config is None:
        raise ServiceException(
            code=ResponseCode.SERVICE_UNAVAILABLE,
            message=f"{scope_label}检索缺少 llm 运行时配置",
        )
    if (
            runtime_config.provider_type is None
            or runtime_config.base_url is None
            or runtime_config.api_key is None
    ):
        raise ServiceException(
            code=ResponseCode.SERVICE_UNAVAILABLE,
            message=f"{scope_label}检索的 llm 运行时配置不完整",
        )

    knowledge_names = snapshot.get_knowledge_names(scope)
    if not knowledge_names:
        raise ServiceException(
            code=ResponseCode.SERVICE_UNAVAILABLE,
            message=f"{scope_label}检索未配置 knowledgeNames",
        )
    if len(knowledge_names) > RAG_MAX_KNOWLEDGE_NAMES:
        raise ServiceException(
            code=ResponseCode.SERVICE_UNAVAILABLE,
            message=f"{scope_label}数量不能超过 {RAG_MAX_KNOWLEDGE_NAMES} 个",
        )

    embedding_model_name = (
            snapshot.get_knowledge_embedding_model_name(scope)
            or _resolve_provider_embedding_fallback_model_name(runtime_config.provider_type)
    )
    if embedding_model_name is None:
        raise ServiceException(
            code=ResponseCode.SERVICE_UNAVAILABLE,
            message=f"{scope_label}检索未配置 embeddingModel",
        )

    embedding_dim = snapshot.get_knowledge_embedding_dim(scope) or 1024
    return KnowledgeSearchRuntimeConfig(
        provider_type=runtime_config.provider_type,
        llm_base_url=runtime_config.base_url,
        llm_api_key=runtime_config.api_key,
        knowledge_names=knowledge_names,
        embedding_model_name=embedding_model_name,
        embedding_dim=embedding_dim,
        ranking_enabled=snapshot.is_knowledge_ranking_enabled(scope),
        ranking_model_name=snapshot.get_knowledge_ranking_model_name(scope),
        configured_top_k=snapshot.get_knowledge_top_k(scope),
    )


def resolve_manual_runtime_config(
        *,
        knowledge_names: list[str],
        embedding_model: str,
        embedding_dim: int,
        ranking_enabled: bool,
        ranking_model: str | None,
        configured_top_k: int | None,
) -> KnowledgeSearchRuntimeConfig:
    """解析管理端手动知识检索使用的运行时配置。

    Args:
        knowledge_names: 本次检索显式指定的知识库名称列表。
        embedding_model: 本次检索显式指定的向量模型名称。
        embedding_dim: 本次检索显式指定的向量维度。
        ranking_enabled: 本次检索是否启用重排。
        ranking_model: 本次检索显式指定的重排模型名称。
        configured_top_k: 本次检索显式指定的默认返回条数。

    Returns:
        当前请求生效的知识检索运行时配置。

    Raises:
        ServiceException: 当缺少必要运行时配置时抛出。
    """

    snapshot = get_current_agent_config_snapshot()
    runtime_config = snapshot.get_llm_runtime_config()
    if runtime_config is None:
        raise ServiceException(
            code=ResponseCode.SERVICE_UNAVAILABLE,
            message="知识库检索缺少 llm 运行时配置",
        )
    if (
            runtime_config.provider_type is None
            or runtime_config.base_url is None
            or runtime_config.api_key is None
    ):
        raise ServiceException(
            code=ResponseCode.SERVICE_UNAVAILABLE,
            message="知识库检索的 llm 运行时配置不完整",
        )

    normalized_knowledge_names = _normalize_manual_knowledge_names(knowledge_names)
    normalized_embedding_model = _normalize_manual_embedding_model(embedding_model)
    normalized_embedding_dim = _normalize_manual_embedding_dim(embedding_dim)
    normalized_ranking_model = _normalize_manual_ranking_model(
        ranking_enabled=ranking_enabled,
        ranking_model=ranking_model,
    )
    normalized_configured_top_k = normalize_top_k(configured_top_k)
    return KnowledgeSearchRuntimeConfig(
        provider_type=runtime_config.provider_type,
        llm_base_url=runtime_config.base_url,
        llm_api_key=runtime_config.api_key,
        knowledge_names=normalized_knowledge_names,
        embedding_model_name=normalized_embedding_model,
        embedding_dim=normalized_embedding_dim,
        ranking_enabled=ranking_enabled,
        ranking_model_name=normalized_ranking_model,
        configured_top_k=normalized_configured_top_k,
    )


def _normalize_manual_knowledge_names(knowledge_names: list[str]) -> list[str]:
    """规范化手动检索传入的知识库名称列表。

    Args:
        knowledge_names: 原始知识库名称列表。

    Returns:
        去空白、去重且保持顺序的知识库名称列表。

    Raises:
        ServiceException: 当知识库名称列表为空或超出限制时抛出。
    """

    normalized_knowledge_names: list[str] = []
    seen_names: set[str] = set()
    for knowledge_name in knowledge_names:
        normalized_knowledge_name = str(knowledge_name or "").strip()
        if not normalized_knowledge_name or normalized_knowledge_name in seen_names:
            continue
        seen_names.add(normalized_knowledge_name)
        normalized_knowledge_names.append(normalized_knowledge_name)

    if not normalized_knowledge_names:
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message="knowledge_names 不能为空",
        )
    if len(normalized_knowledge_names) > RAG_MAX_KNOWLEDGE_NAMES:
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message=f"knowledge_names 数量不能超过 {RAG_MAX_KNOWLEDGE_NAMES} 个",
        )
    return normalized_knowledge_names


def _normalize_manual_embedding_model(embedding_model: str) -> str:
    """规范化手动检索传入的向量模型名称。

    Args:
        embedding_model: 原始向量模型名称。

    Returns:
        去空白后的向量模型名称。

    Raises:
        ServiceException: 当向量模型名称为空时抛出。
    """

    normalized_embedding_model = str(embedding_model or "").strip()
    if not normalized_embedding_model:
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message="embedding_model 不能为空",
        )
    return normalized_embedding_model


def _normalize_manual_embedding_dim(embedding_dim: int) -> int:
    """规范化手动检索传入的向量维度。

    Args:
        embedding_dim: 原始向量维度。

    Returns:
        校验通过后的向量维度。

    Raises:
        ServiceException: 当向量维度非法时抛出。
    """

    normalized_embedding_dim = _normalize_optional_positive_int(embedding_dim)
    if normalized_embedding_dim is None:
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message="embedding_dim 必须是正整数",
        )
    return normalized_embedding_dim


def _normalize_manual_ranking_model(
        *,
        ranking_enabled: bool,
        ranking_model: str | None,
) -> str | None:
    """规范化手动检索传入的重排模型名称。

    Args:
        ranking_enabled: 当前请求是否启用重排。
        ranking_model: 原始重排模型名称。

    Returns:
        校验通过后的重排模型名称；未开启重排时返回 ``None``。

    Raises:
        ServiceException: 当重排开关与模型配置不匹配时抛出。
    """

    normalized_ranking_model = str(ranking_model or "").strip() or None
    if not ranking_enabled:
        if normalized_ranking_model is not None:
            raise ServiceException(
                code=ResponseCode.BAD_REQUEST,
                message="ranking_enabled=false 时不允许传入 ranking_model",
            )
        return None
    if normalized_ranking_model is None:
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message="ranking_enabled=true 时必须传入 ranking_model",
        )
    return normalized_ranking_model
