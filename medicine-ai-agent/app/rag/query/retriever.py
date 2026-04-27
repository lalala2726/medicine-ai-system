from __future__ import annotations

import math
from typing import Any

from loguru import logger
from pymilvus import MilvusClient

from app.rag.query.clients import create_rag_embedding_client, create_rag_milvus_client
from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.rag.query.constants import (
    RAG_FILTER_EXPR,
    RAG_MAX_CANDIDATE_POOL,
    RAG_OUTPUT_FIELDS,
    RAG_SEARCH_PARAMS,
)
from app.rag.query.types import KnowledgeSearchHit, KnowledgeSearchRuntimeConfig
from app.rag.query.utils import coerce_optional_int


def extract_search_row_value(row: dict[str, Any], field_name: str) -> Any:
    """从 Milvus 检索结果中提取字段值。

    Args:
        row: 单条 Milvus 检索结果原始字典。
        field_name: 目标字段名。

    Returns:
        命中的字段值；不存在时返回 ``None``。
    """

    if field_name in row:
        return row.get(field_name)
    entity = row.get("entity")
    if isinstance(entity, dict):
        return entity.get(field_name)
    return None


def vector_score_from_row(row: dict[str, Any]) -> float:
    """从 Milvus 检索结果中提取当前用于排序的向量分数。

    Args:
        row: 单条 Milvus 检索结果原始字典。

    Returns:
        当前结果的向量分数；缺失时返回 ``0.0``。
    """

    return float(row.get("distance") or row.get("score") or 0.0)


def search_collection_rows(
        *,
        client: MilvusClient,
        knowledge_name: str,
        query_vector: list[float],
        limit: int,
) -> list[dict[str, Any]] | None:
    """查询单个知识库 collection 并返回原始命中行。

    Args:
        client: 当前查询使用的 Milvus 客户端。
        knowledge_name: 目标知识库 collection 名称。
        query_vector: 当前问题对应的查询向量。
        limit: 单个知识库的召回上限。

    Returns:
        命中行列表；当 collection 不存在时返回 ``None``。
    """

    if not client.has_collection(knowledge_name):
        logger.warning("知识库集合不存在，已跳过：collection={}", knowledge_name)
        return None

    search_results = client.search(
        collection_name=knowledge_name,
        data=[query_vector],
        filter=RAG_FILTER_EXPR,
        limit=limit,
        output_fields=RAG_OUTPUT_FIELDS,
        search_params=RAG_SEARCH_PARAMS,
        anns_field="embedding",
    )
    if not search_results:
        return []
    return list(search_results[0] or [])


def to_knowledge_hit(row: dict[str, Any], *, knowledge_name: str) -> KnowledgeSearchHit:
    """将 Milvus 检索结果行转换为统一的命中结构。

    Args:
        row: 单条 Milvus 检索结果原始字典。
        knowledge_name: 当前命中所属的知识库名称。

    Returns:
        统一结构化后的知识命中对象。
    """

    return KnowledgeSearchHit(
        knowledge_name=knowledge_name,
        content=str(extract_search_row_value(row, "content") or "").strip(),
        score=vector_score_from_row(row),
        document_id=coerce_optional_int(extract_search_row_value(row, "document_id")),
        chunk_index=coerce_optional_int(extract_search_row_value(row, "chunk_index")),
        char_count=coerce_optional_int(extract_search_row_value(row, "char_count")),
    )


def deduplicate_hits(hits: list[KnowledgeSearchHit]) -> list[KnowledgeSearchHit]:
    """按固定业务键去重，保留分数更高的命中。

    Args:
        hits: 原始命中列表。

    Returns:
        去重后的命中列表。
    """

    deduplicated: dict[tuple[str, int | None, int | None, str], KnowledgeSearchHit] = {}
    for hit in hits:
        dedupe_key = (
            hit.knowledge_name,
            hit.document_id,
            hit.chunk_index,
            hit.content,
        )
        existing = deduplicated.get(dedupe_key)
        if existing is None or hit.score > existing.score:
            deduplicated[dedupe_key] = hit
    return list(deduplicated.values())


def sort_hits_desc(hits: list[KnowledgeSearchHit]) -> list[KnowledgeSearchHit]:
    """按分数从高到低排序。

    Args:
        hits: 待排序命中列表。

    Returns:
        按分数降序排列的新列表。
    """

    return sorted(hits, key=lambda item: item.score, reverse=True)


def resolve_vector_candidate_target(*, final_top_k: int, ranking_enabled: bool) -> int:
    """解析当前阶段需要保留的向量候选池规模。

    Args:
        final_top_k: 最终期望返回的命中数量。
        ranking_enabled: 当前阶段是否启用了排序。

    Returns:
        当前阶段需要保留的向量候选池大小。
    """

    if not ranking_enabled:
        return final_top_k
    return min(max(final_top_k * 3, final_top_k), RAG_MAX_CANDIDATE_POOL)


def resolve_recall_per_kb(
        *,
        final_top_k: int,
        knowledge_count: int,
        ranking_enabled: bool,
) -> int:
    """根据最终返回条数与知识库数量解析每个知识库的召回规模。

    Args:
        final_top_k: 最终期望返回的命中数量。
        knowledge_count: 当前参与查询的知识库数量。
        ranking_enabled: 当前阶段是否启用了排序。

    Returns:
        每个知识库应执行的召回上限。
    """

    if knowledge_count <= 0:
        return 1
    candidate_target = resolve_vector_candidate_target(
        final_top_k=final_top_k,
        ranking_enabled=ranking_enabled,
    )
    return max(1, math.ceil(candidate_target / knowledge_count))


def search_knowledge_hits(
        *,
        question: str,
        final_top_k: int,
        runtime_config: KnowledgeSearchRuntimeConfig,
        ranking_enabled: bool,
) -> list[KnowledgeSearchHit]:
    """执行多知识库聚合向量检索。

    Args:
        question: 当前用于向量检索的问题文本。
        final_top_k: 最终期望返回的命中数量。
        runtime_config: 当前知识检索运行时配置。
        ranking_enabled: 当前阶段是否按排序扩充候选池。

    Returns:
        聚合去重并按分数降序裁剪后的候选命中列表。
    """

    client = create_rag_milvus_client(runtime_config=runtime_config)
    embedding_client = create_rag_embedding_client(runtime_config=runtime_config)
    query_vector = embedding_client.embed_query(question)
    recall_per_kb = resolve_recall_per_kb(
        final_top_k=final_top_k,
        knowledge_count=len(runtime_config.knowledge_names),
        ranking_enabled=ranking_enabled,
    )
    candidate_target = resolve_vector_candidate_target(
        final_top_k=final_top_k,
        ranking_enabled=ranking_enabled,
    )

    found_collections: list[str] = []
    aggregated_hits: list[KnowledgeSearchHit] = []
    for knowledge_name in runtime_config.knowledge_names:
        rows = search_collection_rows(
            client=client,
            knowledge_name=knowledge_name,
            query_vector=query_vector,
            limit=recall_per_kb,
        )
        if rows is None:
            continue
        found_collections.append(knowledge_name)
        aggregated_hits.extend(
            to_knowledge_hit(row, knowledge_name=knowledge_name)
            for row in rows
        )

    if not found_collections:
        raise ServiceException(
            code=ResponseCode.NOT_FOUND,
            message=f"知识库集合不存在: collections={runtime_config.knowledge_names}",
        )

    deduplicated_hits = deduplicate_hits(aggregated_hits)
    return sort_hits_desc(deduplicated_hits)[:candidate_target]
