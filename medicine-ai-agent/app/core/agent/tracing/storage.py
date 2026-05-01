from __future__ import annotations

from functools import lru_cache
from typing import Any

from pymongo import ASCENDING, DESCENDING, UpdateOne

from app.core.database.mongodb import (
    MONGODB_AGENT_MODEL_TOKEN_USAGE_COLLECTION,
    MONGODB_AGENT_TRACE_RUNS_COLLECTION,
    MONGODB_AGENT_TRACE_SPANS_COLLECTION,
    get_mongo_database,
)


def _get_runs_collection():
    """
    功能描述：
        获取 agent_trace_runs 集合。

    参数说明：
        无。

    返回值：
        Collection: Mongo 集合对象。
    """

    return get_mongo_database()[MONGODB_AGENT_TRACE_RUNS_COLLECTION]


def _get_spans_collection():
    """
    功能描述：
        获取 agent_trace_spans 集合。

    参数说明：
        无。

    返回值：
        Collection: Mongo 集合对象。
    """

    return get_mongo_database()[MONGODB_AGENT_TRACE_SPANS_COLLECTION]


def _get_model_token_usage_collection():
    """
    功能描述：
        获取 agent_model_token_usage 集合。

    参数说明：
        无。

    返回值：
        Collection: Mongo 集合对象。
    """

    return get_mongo_database()[MONGODB_AGENT_MODEL_TOKEN_USAGE_COLLECTION]


@lru_cache(maxsize=1)
def ensure_agent_trace_indexes() -> None:
    """
    功能描述：
        初始化 Agent Trace 所需 Mongo 索引。

    参数说明：
        无。

    返回值：
        None。
    """

    runs = _get_runs_collection()
    runs.create_index([("trace_id", ASCENDING)], name="uk_agent_trace_run_trace_id", unique=True)
    runs.create_index(
        [("conversation_uuid", ASCENDING), ("started_at", DESCENDING)],
        name="idx_agent_trace_run_conversation_started_at_desc",
    )
    runs.create_index(
        [("assistant_message_uuid", ASCENDING)],
        name="idx_agent_trace_run_assistant_message_uuid",
    )
    runs.create_index(
        [("user_id", ASCENDING), ("started_at", DESCENDING)],
        name="idx_agent_trace_run_user_started_at_desc",
    )

    spans = _get_spans_collection()
    spans.create_index(
        [("trace_id", ASCENDING), ("sequence", ASCENDING)],
        name="idx_agent_trace_span_trace_sequence_asc",
    )
    spans.create_index(
        [("trace_id", ASCENDING), ("parent_span_id", ASCENDING)],
        name="idx_agent_trace_span_trace_parent",
    )

    token_usage = _get_model_token_usage_collection()
    token_usage.create_index(
        [("span_id", ASCENDING)],
        name="uk_agent_model_token_usage_span_id",
        unique=True,
    )
    token_usage.create_index(
        [("started_at", DESCENDING)],
        name="idx_agent_model_token_usage_started_at_desc",
    )
    token_usage.create_index(
        [("model_name", ASCENDING), ("started_at", DESCENDING)],
        name="idx_agent_model_token_usage_model_started_at_desc",
    )
    token_usage.create_index(
        [("provider", ASCENDING), ("model_name", ASCENDING), ("started_at", DESCENDING)],
        name="idx_agent_model_token_usage_provider_model_started_at_desc",
    )
    token_usage.create_index(
        [("conversation_type", ASCENDING), ("started_at", DESCENDING)],
        name="idx_agent_model_token_usage_conversation_started_at_desc",
    )
    token_usage.create_index(
        [("status", ASCENDING), ("started_at", DESCENDING)],
        name="idx_agent_model_token_usage_status_started_at_desc",
    )
    token_usage.create_index(
        [("slot", ASCENDING), ("started_at", DESCENDING)],
        name="idx_agent_model_token_usage_slot_started_at_desc",
    )
    token_usage.create_index(
        [("trace_id", ASCENDING)],
        name="idx_agent_model_token_usage_trace_id",
    )


def insert_trace_run(document: dict[str, Any]) -> None:
    """
    功能描述：
        写入 trace run 文档。

    参数说明：
        document (dict[str, Any]): run 文档。

    返回值：
        None。
    """

    ensure_agent_trace_indexes()
    _get_runs_collection().update_one(
        {"trace_id": document["trace_id"]},
        {"$setOnInsert": document},
        upsert=True,
    )


def update_trace_run(trace_id: str, updates: dict[str, Any]) -> None:
    """
    功能描述：
        更新 trace run 汇总字段。

    参数说明：
        trace_id (str): trace ID。
        updates (dict[str, Any]): 需要更新的字段。

    返回值：
        None。
    """

    ensure_agent_trace_indexes()
    _get_runs_collection().update_one(
        {"trace_id": trace_id},
        {"$set": updates},
        upsert=False,
    )


def insert_trace_span(document: dict[str, Any]) -> None:
    """
    功能描述：
        写入 trace span 文档。

    参数说明：
        document (dict[str, Any]): span 文档。

    返回值：
        None。
    """

    ensure_agent_trace_indexes()
    _get_spans_collection().insert_one(document)


def write_trace_operations(operations: list[dict[str, Any]]) -> None:
    """
    功能描述：
        批量写入 trace 操作。

    参数说明：
        operations (list[dict[str, Any]]): writer 队列中的写操作列表。

    返回值：
        None。
    """

    if not operations:
        return

    ensure_agent_trace_indexes()
    span_documents: list[dict[str, Any]] = []
    token_usage_documents: list[dict[str, Any]] = []
    run_operations: list[UpdateOne] = []
    for operation in operations:
        operation_type = operation.get("type")
        if operation_type == "insert_run":
            document = dict(operation.get("document") or {})
            if document:
                run_operations.append(
                    UpdateOne(
                        {"trace_id": document["trace_id"]},
                        {"$setOnInsert": document},
                        upsert=True,
                    )
                )
        elif operation_type == "update_run":
            trace_id = str(operation.get("trace_id") or "").strip()
            set_on_insert = dict(operation.get("set_on_insert") or {})
            updates = dict(operation.get("updates") or {})
            if trace_id and updates:
                update_document: dict[str, Any] = {"$set": updates}
                if set_on_insert:
                    update_document["$setOnInsert"] = set_on_insert
                run_operations.append(
                    UpdateOne(
                        {"trace_id": trace_id},
                        update_document,
                        upsert=True,
                    )
                )
        elif operation_type == "insert_span":
            document = dict(operation.get("document") or {})
            if document:
                span_documents.append(document)
        elif operation_type == "insert_model_token_usage":
            document = dict(operation.get("document") or {})
            if document:
                token_usage_documents.append(document)

    if run_operations:
        _get_runs_collection().bulk_write(run_operations, ordered=False)
    if span_documents:
        _get_spans_collection().insert_many(span_documents, ordered=False)
    if token_usage_documents:
        _get_model_token_usage_collection().insert_many(token_usage_documents, ordered=False)
