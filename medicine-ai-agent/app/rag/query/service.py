from __future__ import annotations

from app.core.codes import ResponseCode
from app.core.config_sync import KnowledgeBaseScope
from app.core.exception.exceptions import ServiceException
from app.rag.query.formatter import format_knowledge_search_hits
from app.rag.query import ranking as ranking_module
from app.rag.query import retriever as retriever_module
from app.rag.query import rewrite as rewrite_module
from app.rag.query import runtime as runtime_module
from app.rag.query.types import KnowledgeSearchHit, KnowledgeSearchRuntimeConfig
from app.rag.query.utils import normalize_question


def query_knowledge(
        *,
        vector_question: str,
        ranking_question: str,
        top_k: int | None,
        scope: KnowledgeBaseScope = KnowledgeBaseScope.ADMIN,
) -> list[KnowledgeSearchHit]:
    """统一执行知识库查询与可选重排。

    Args:
        vector_question: 用于向量召回的问题文本。
        ranking_question: 用于重排阶段的问题文本。
        top_k: 调用方显式传入的最终返回条数。
        scope: 当前知识库作用域。

    Returns:
        最终返回给上层调用方的知识片段列表。
    """

    final_top_k = runtime_module.resolve_final_top_k(top_k, scope)
    runtime_config = runtime_module.resolve_runtime_config(scope)
    vector_hits = retriever_module.search_knowledge_hits(
        question=vector_question,
        final_top_k=final_top_k,
        runtime_config=runtime_config,
        ranking_enabled=runtime_config.ranking_enabled,
    )
    if not runtime_config.ranking_enabled:
        return vector_hits[:final_top_k]

    return ranking_module.rerank_hits_with_model(
        query=ranking_question,
        hits=vector_hits,
        runtime_config=runtime_config,
        final_top_k=final_top_k,
    )[:final_top_k]


def query_knowledge_with_runtime_config(
        *,
        vector_question: str,
        ranking_question: str,
        top_k: int | None,
        runtime_config: KnowledgeSearchRuntimeConfig,
) -> list[KnowledgeSearchHit]:
    """使用显式运行时配置执行知识库查询与可选重排。

    Args:
        vector_question: 用于向量召回的问题文本。
        ranking_question: 用于重排阶段的问题文本。
        top_k: 调用方显式传入的最终返回条数。
        runtime_config: 当前请求显式指定的运行时配置。

    Returns:
        最终返回给上层调用方的知识片段列表。
    """

    final_top_k = runtime_module.resolve_final_top_k_by_runtime_config(
        explicit_top_k=top_k,
        runtime_config=runtime_config,
    )
    vector_hits = retriever_module.search_knowledge_hits(
        question=vector_question,
        final_top_k=final_top_k,
        runtime_config=runtime_config,
        ranking_enabled=runtime_config.ranking_enabled,
    )
    if not runtime_config.ranking_enabled:
        return vector_hits[:final_top_k]

    return ranking_module.rerank_hits_with_model(
        query=ranking_question,
        hits=vector_hits,
        runtime_config=runtime_config,
        final_top_k=final_top_k,
    )[:final_top_k]


def query_knowledge_by_raw_question(
        *,
        question: str,
        top_k: int | None,
        scope: KnowledgeBaseScope = KnowledgeBaseScope.ADMIN,
) -> list[KnowledgeSearchHit]:
    """使用原始问题直接检索知识库。

    Args:
        question: 用户原始问题。
        top_k: 调用方显式传入的最终返回条数；未传时走 Redis 或默认值。
        scope: 知识库作用域。

    Returns:
        规范化后的知识片段命中列表。

    异常说明：
        ServiceException: 当问题非法或检索失败时抛出。
    """

    normalized_question = normalize_question(question)
    try:
        return query_knowledge(
            vector_question=normalized_question,
            ranking_question=normalized_question,
            top_k=top_k,
            scope=scope,
        )
    except ServiceException:
        raise
    except Exception as exc:
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message=f"知识库检索失败: {exc}",
        ) from exc


def query_knowledge_by_rewritten_question(
        *,
        question: str,
        top_k: int | None,
        scope: KnowledgeBaseScope = KnowledgeBaseScope.ADMIN,
) -> list[KnowledgeSearchHit]:
    """先改写问题，再检索知识库。

    Args:
        question: 用户原始问题。
        top_k: 调用方显式传入的最终返回条数；未传时走 Redis 或默认值。
        scope: 知识库作用域。

    Returns:
        规范化后的知识片段命中列表。

    异常说明：
        ServiceException: 当问题非法或检索失败时抛出。
    """

    normalized_question = normalize_question(question)
    rewritten_question = rewrite_module.rewrite_question_for_knowledge_search(normalized_question)
    try:
        return query_knowledge(
            vector_question=rewritten_question,
            ranking_question=normalized_question,
            top_k=top_k,
            scope=scope,
        )
    except ServiceException:
        raise
    except Exception as exc:
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message=f"知识库检索失败: {exc}",
        ) from exc


def query_knowledge_by_rewritten_question_with_runtime_config(
        *,
        question: str,
        top_k: int | None,
        runtime_config: KnowledgeSearchRuntimeConfig,
) -> list[KnowledgeSearchHit]:
    """先改写问题，再使用显式运行时配置检索知识库。

    Args:
        question: 用户原始问题。
        top_k: 调用方显式传入的最终返回条数。
        runtime_config: 当前请求显式指定的运行时配置。

    Returns:
        规范化后的知识片段命中列表。

    异常说明：
        ServiceException: 当问题非法或检索失败时抛出。
    """

    normalized_question = normalize_question(question)
    rewritten_question = rewrite_module.rewrite_question_for_knowledge_search(normalized_question)
    try:
        return query_knowledge_with_runtime_config(
            vector_question=rewritten_question,
            ranking_question=normalized_question,
            top_k=top_k,
            runtime_config=runtime_config,
        )
    except ServiceException:
        raise
    except Exception as exc:
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message=f"知识库检索失败: {exc}",
        ) from exc


__all__ = [
    "format_knowledge_search_hits",
    "query_knowledge",
    "query_knowledge_with_runtime_config",
    "query_knowledge_by_raw_question",
    "query_knowledge_by_rewritten_question",
    "query_knowledge_by_rewritten_question_with_runtime_config",
]
