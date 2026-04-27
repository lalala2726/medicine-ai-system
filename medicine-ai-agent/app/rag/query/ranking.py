from __future__ import annotations

from typing import Any

import httpx

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.rag.query.constants import (
    QWEN3_RERANK_MODEL_NAME,
    RAG_QWEN3_RERANK_ENDPOINT,
    RAG_RERANK_PROVIDER_TYPE,
    RAG_RERANK_REQUEST_TIMEOUT_SECONDS,
)
from app.rag.query.types import KnowledgeSearchHit, KnowledgeSearchRuntimeConfig


def validate_rerank_runtime_config(*, runtime_config: KnowledgeSearchRuntimeConfig) -> str:
    """校验并返回当前请求生效的重排模型名称。

    Args:
        runtime_config: 当前知识检索运行时配置。

    Returns:
        当前请求允许使用的重排模型名称。

    异常说明：
        ServiceException: 当模型提供商或重排模型配置非法时抛出。
    """

    provider_type = runtime_config.provider_type.strip().lower()
    if provider_type != RAG_RERANK_PROVIDER_TYPE:
        raise ServiceException(
            code=ResponseCode.SERVICE_UNAVAILABLE,
            message=f"知识库重排暂不支持当前模型提供商: {runtime_config.provider_type}",
        )

    model_name = runtime_config.ranking_model_name
    if model_name is None:
        raise ServiceException(
            code=ResponseCode.SERVICE_UNAVAILABLE,
            message="知识库重排未配置 rankingModel",
        )
    if model_name != QWEN3_RERANK_MODEL_NAME:
        raise ServiceException(
            code=ResponseCode.SERVICE_UNAVAILABLE,
            message=f"知识库重排当前仅支持模型: {QWEN3_RERANK_MODEL_NAME}",
        )
    return model_name


def build_rerank_request_payload(
        *,
        query: str,
        documents: list[str],
        top_n: int,
) -> dict[str, Any]:
    """构造 `qwen3-rerank` 请求体。

    Args:
        query: 当前重排查询文本。
        documents: 当前候选文档列表。
        top_n: 当前期望返回的结果数量。

    Returns:
        `qwen3-rerank` 兼容接口请求体。
    """

    return {
        "model": QWEN3_RERANK_MODEL_NAME,
        "query": query,
        "documents": documents,
        "top_n": top_n,
    }


def parse_rerank_results(response_payload: Any) -> list[dict[str, Any]]:
    """解析重排接口响应中的 `results` 列表。

    Args:
        response_payload: 重排接口原始 JSON 响应。

    Returns:
        响应中的结果数组。

    结构说明:
        当前实现同时兼容以下两类官方返回结构：
        1. 兼容接口 `/compatible-api/v1/reranks` 的顶层 `results`；
        2. 服务接口 / SDK 风格响应中的 `output.results`。

    异常说明：
        ServiceException: 当响应结构不合法时抛出。
    """

    if not isinstance(response_payload, dict):
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message="重排接口响应格式错误：根节点必须为对象",
        )

    if response_payload.get("code"):
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message=f"重排接口返回业务错误: code={response_payload.get('code')}, "
                    f"message={response_payload.get('message')}",
        )

    direct_results = response_payload.get("results")
    if isinstance(direct_results, list):
        return [item for item in direct_results if isinstance(item, dict)]

    output = response_payload.get("output")
    if isinstance(output, dict):
        nested_results = output.get("results")
        if isinstance(nested_results, list):
            return [item for item in nested_results if isinstance(item, dict)]

    response_keys = sorted(str(key) for key in response_payload.keys())
    output_keys = (
        sorted(str(key) for key in output.keys())
        if isinstance(output, dict)
        else None
    )
    raise ServiceException(
        code=ResponseCode.OPERATION_FAILED,
        message=(
            "重排接口响应格式错误：未找到 `results` 列表。"
            f" top_level_keys={response_keys}, output_keys={output_keys}"
        ),
    )


def parse_rerank_indexes(*, results: list[dict[str, Any]], max_index: int) -> list[int]:
    """从重排结果中提取合法且去重的文档索引。

    Args:
        results: 重排结果列表。
        max_index: 当前候选列表最大合法索引上限（不包含）。

    Returns:
        合法、去重后的排序索引列表。

    异常说明：
        ServiceException: 当响应中没有合法索引时抛出。
    """

    indexes: list[int] = []
    seen_indexes: set[int] = set()
    for item in results:
        raw_index = item.get("index")
        try:
            resolved_index = int(raw_index)
        except (TypeError, ValueError):
            continue
        if resolved_index < 0 or resolved_index >= max_index:
            continue
        if resolved_index in seen_indexes:
            continue
        seen_indexes.add(resolved_index)
        indexes.append(resolved_index)

    if not indexes:
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message="重排接口未返回有效排序结果",
        )
    return indexes


def rerank_hits_with_model(
        *,
        query: str,
        hits: list[KnowledgeSearchHit],
        runtime_config: KnowledgeSearchRuntimeConfig,
        final_top_k: int,
) -> list[KnowledgeSearchHit]:
    """使用 `qwen3-rerank` 对候选知识片段进行二次排序。

    Args:
        query: 排序阶段使用的用户问题。
        hits: 待排序候选片段列表。
        runtime_config: 当前知识检索运行时配置。
        final_top_k: 最终期望返回的命中数量。

    Returns:
        重排后的命中列表。

    异常说明：
        ServiceException: 当重排请求失败或响应不合法时抛出。
    """

    if not hits:
        return []

    validate_rerank_runtime_config(runtime_config=runtime_config)
    payload = build_rerank_request_payload(
        query=query,
        documents=[item.content for item in hits],
        top_n=final_top_k,
    )
    headers = {
        "Authorization": f"Bearer {runtime_config.llm_api_key}",
        "Content-Type": "application/json",
    }
    try:
        response = httpx.post(
            RAG_QWEN3_RERANK_ENDPOINT,
            json=payload,
            headers=headers,
            timeout=RAG_RERANK_REQUEST_TIMEOUT_SECONDS,
        )
    except httpx.HTTPError as exc:
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message=f"重排接口请求失败: {exc}",
        ) from exc

    if response.status_code != 200:
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message=f"重排接口请求失败: status={response.status_code}, body={response.text}",
        )

    try:
        response_payload = response.json()
    except ValueError as exc:
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message=f"重排接口返回非 JSON 响应: {response.text}",
        ) from exc

    results = parse_rerank_results(response_payload)
    sorted_indexes = parse_rerank_indexes(
        results=results,
        max_index=len(hits),
    )
    return [hits[index] for index in sorted_indexes[:final_top_k]]
