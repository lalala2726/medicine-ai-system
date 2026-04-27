from __future__ import annotations

import threading
from typing import Any

from loguru import logger
from pymilvus import MilvusClient

from app.core.database.milvus import get_milvus_connection_args
from app.core.llms import create_embedding_model
from app.rag.query.types import KnowledgeSearchRuntimeConfig

#: 控制 RAG 运行时配置日志仅打印一次，避免重复刷屏。
_RAG_RUNTIME_CONFIG_LOGGED = False
#: RAG Milvus 客户端缓存锁，保护多线程查询下的客户端复用。
_RAG_MILVUS_CLIENT_LOCK = threading.RLock()
#: 按 Milvus 连接参数缓存 RAG 查询客户端，避免每次检索都新建连接。
_RAG_MILVUS_CLIENT_CACHE: dict[tuple[tuple[str, str | float], ...], MilvusClient] = {}


def _build_milvus_client_cache_key(
        connection_args: dict[str, str | float],
) -> tuple[tuple[str, str | float], ...]:
    """构造 Milvus 客户端缓存键。

    Args:
        connection_args: 当前 Milvus 客户端连接参数。

    Returns:
        tuple[tuple[str, str | float], ...]: 可哈希且顺序稳定的缓存键。
    """

    return tuple(sorted(connection_args.items(), key=lambda item: item[0]))


def create_rag_embedding_client(*, runtime_config: KnowledgeSearchRuntimeConfig) -> Any:
    """构造 RAG 查询专用的向量模型客户端。

    Args:
        runtime_config: 当前请求生效的知识检索运行时配置。

    Returns:
        可执行 ``embed_query`` 的向量模型客户端实例。
    """

    return create_embedding_model(
        provider=runtime_config.provider_type,
        model=runtime_config.embedding_model_name,
        api_key=runtime_config.llm_api_key,
        base_url=runtime_config.llm_base_url,
        dimensions=runtime_config.embedding_dim,
    )


def log_rag_runtime_config_once(
        *,
        runtime_config: KnowledgeSearchRuntimeConfig,
        connection_args: dict[str, str | float],
) -> None:
    """记录当前 RAG 查询链的关键运行时配置。

    Args:
        runtime_config: 当前请求生效的知识检索运行时配置。
        connection_args: 当前 Milvus 客户端使用的连接参数。

    Returns:
        无。
    """

    global _RAG_RUNTIME_CONFIG_LOGGED
    if _RAG_RUNTIME_CONFIG_LOGGED:
        return

    logger.info(
        "RAG 查询配置已生效：provider={}，embedding_model={}，embedding_dimensions={}，"
        "llm_base_url={}，milvus_uri={}，milvus_db_name={}，knowledge_names={}，"
        "ranking_enabled={}，ranking_model={}，configured_top_k={}",
        runtime_config.provider_type,
        runtime_config.embedding_model_name,
        runtime_config.embedding_dim,
        runtime_config.llm_base_url,
        connection_args.get("uri"),
        connection_args.get("db_name"),
        runtime_config.knowledge_names,
        runtime_config.ranking_enabled,
        runtime_config.ranking_model_name,
        runtime_config.configured_top_k,
    )
    _RAG_RUNTIME_CONFIG_LOGGED = True


def create_rag_milvus_client(*, runtime_config: KnowledgeSearchRuntimeConfig) -> MilvusClient:
    """构造 RAG 查询专用的 Milvus 客户端。

    Args:
        runtime_config: 当前请求生效的知识检索运行时配置。

    Returns:
        已绑定当前环境连接参数的 ``MilvusClient`` 实例。
    """

    connection_args = dict(get_milvus_connection_args())
    log_rag_runtime_config_once(
        runtime_config=runtime_config,
        connection_args=connection_args,
    )
    cache_key = _build_milvus_client_cache_key(connection_args)
    with _RAG_MILVUS_CLIENT_LOCK:
        cached_client = _RAG_MILVUS_CLIENT_CACHE.get(cache_key)
        if cached_client is not None:
            return cached_client
        client = MilvusClient(**connection_args)
        _RAG_MILVUS_CLIENT_CACHE[cache_key] = client
        return client
