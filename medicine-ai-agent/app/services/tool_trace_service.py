from __future__ import annotations

from functools import lru_cache
from typing import Annotated, Any

from pydantic import Field
from pymongo import ASCENDING, DESCENDING

from app.core.database.mongodb import MONGODB_TOOL_TRACES_COLLECTION, get_mongo_database
from app.schemas.document.tool_trace import ToolTraceCreate, ToolTraceDocument, ToolTraceStatus


def _resolve_collection_name() -> str:
    """返回 tool_traces 集合固定名称常量。"""

    return MONGODB_TOOL_TRACES_COLLECTION


def _get_collection():
    """获取 tool_traces 集合对象。"""

    db = get_mongo_database()
    return db[_resolve_collection_name()]


@lru_cache(maxsize=1)
def _ensure_tool_trace_indexes() -> None:
    """懒初始化 tool_traces 集合常用索引。"""

    collection = _get_collection()
    collection.create_index(
        [("conversation_uuid", ASCENDING), ("created_at", DESCENDING)],
        name="idx_tool_trace_conversation_created_at_desc",
    )
    collection.create_index(
        [
            ("conversation_uuid", ASCENDING),
            ("assistant_message_uuid", ASCENDING),
            ("created_at", DESCENDING),
        ],
        name="idx_tool_trace_conversation_message_created_at_desc",
    )
    collection.create_index(
        [("conversation_type", ASCENDING), ("created_at", DESCENDING)],
        name="idx_tool_trace_conversation_type_created_at_desc",
    )


def _to_tool_trace_document(document: dict[str, Any]) -> ToolTraceDocument:
    """将 Mongo 原始文档转换为工具轨迹模型。"""

    return ToolTraceDocument.model_validate(document)


def add_tool_trace(
        *,
        conversation_uuid: Annotated[str, Field(min_length=1)],
        assistant_message_uuid: Annotated[str, Field(min_length=1)],
        conversation_type: str,
        tool_name: Annotated[str, Field(min_length=1)],
        tool_display_name: Annotated[str, Field(min_length=1)],
        status: ToolTraceStatus | str,
        summary_text: Annotated[str, Field(min_length=1)],
        input_payload: Any,
        output_payload: Any | None = None,
        error_payload: dict[str, Any] | None = None,
) -> str:
    """
    新增一条工具调用轨迹。

    Args:
        conversation_uuid: 所属会话 UUID。
        assistant_message_uuid: 所属 AI 回复消息 UUID。
        conversation_type: 会话类型。
        tool_name: 工具原始名称。
        tool_display_name: 工具展示名称。
        status: 工具执行状态。
        summary_text: 给模型消费的稳定中文摘要。
        input_payload: 工具完整输入 JSON 结构。
        output_payload: 工具完整输出 JSON 结构。
        error_payload: 工具失败结构化错误信息。

    Returns:
        str: 新增工具轨迹的 Mongo ObjectId 字符串。
    """

    _ensure_tool_trace_indexes()
    payload = ToolTraceCreate(
        conversation_uuid=conversation_uuid,
        assistant_message_uuid=assistant_message_uuid,
        conversation_type=conversation_type,
        tool_name=tool_name,
        tool_display_name=tool_display_name,
        status=status,
        summary_text=summary_text,
        input_payload=input_payload,
        output_payload=output_payload,
        error_payload=error_payload,
    )
    document = payload.model_dump(mode="python", exclude_none=True)
    result = _get_collection().insert_one(document)
    return str(result.inserted_id)


def list_recent_tool_traces(
        conversation_uuid: Annotated[str, Field(min_length=1)],
        *,
        limit: Annotated[int, Field(ge=1)] = 8,
) -> list[ToolTraceDocument]:
    """
    查询指定会话最近的工具调用轨迹。

    Args:
        conversation_uuid: 所属会话 UUID。
        limit: 返回条数上限。

    Returns:
        list[ToolTraceDocument]: 按创建时间倒序返回的最近轨迹列表。
    """

    _ensure_tool_trace_indexes()
    cursor = (
        _get_collection()
        .find({"conversation_uuid": conversation_uuid})
        .sort("created_at", DESCENDING)
        .limit(limit)
    )
    return [_to_tool_trace_document(item) for item in cursor]
