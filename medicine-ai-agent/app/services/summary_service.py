from __future__ import annotations

import datetime
from typing import Annotated, Literal

from bson import ObjectId
from pydantic import Field
from pymongo import ReturnDocument

from app.core.codes import ResponseCode
from app.core.database.mongodb import (
    get_mongo_database,
    MONGODB_CONVERSATION_SUMMARIES_COLLECTION,
)
from app.core.exception.exceptions import ServiceException
from app.schemas.document.conversation_summary import (
    ConversationSummary,
    ConversationSummarySetOnInsert,
    ConversationSummaryUpsertPayload,
    ConversationSummaryUpdateSet,
)


def _resolve_collection_name() -> str:
    """
    功能描述：
        返回 `conversation_summaries` 集合固定名称常量。

    参数说明：
        无。

    返回值：
        str: 实际生效的集合名。

    异常说明：
        无。
    """

    return MONGODB_CONVERSATION_SUMMARIES_COLLECTION


def _to_object_id(raw_conversation_id: str) -> ObjectId:
    """
    功能描述：
        将字符串会话 ID 转换为 MongoDB `ObjectId`。

    参数说明：
        raw_conversation_id (str): 会话主键字符串。

    返回值：
        ObjectId: 转换后的 MongoDB 主键对象。

    异常说明：
        ServiceException:
            - BAD_REQUEST: 当会话 ID 不是合法 `ObjectId` 字符串时抛出。
    """

    try:
        return ObjectId(raw_conversation_id)
    except Exception as exc:  # pragma: no cover - 防御性兜底
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message="conversation_id 格式不正确",
        ) from exc


def _normalize_optional_string(value: str | None) -> str | None:
    """
    功能描述：
        归一化可选字符串参数，统一去除首尾空白并过滤空串。

    参数说明：
        value (str | None): 原始字符串值。

    返回值：
        str | None: 归一化后的非空字符串；空值或空串返回 `None`。

    异常说明：
        无。
    """

    if value is None:
        return None
    normalized = value.strip()
    return normalized or None


def save_conversation_summary(
        *,
        conversation_id: Annotated[str, Field(min_length=1)],
        summary_content: Annotated[str, Field(min_length=1)],
        last_summarized_message_id: str | None = None,
        last_summarized_message_uuid: str | None = None,
        summary_version: int = 1,
        summary_token_count: int = 0,
        status: Literal["success", "error"] = "success",
        expected_last_summarized_message_id: str | None = None,
) -> str | None:
    """
    功能描述：
        保存会话摘要文档（按 `conversation_id` 单文档 upsert），并提供游标级 CAS 保护。

    参数说明：
        conversation_id (str): 会话 Mongo 主键（ObjectId 字符串）。
        summary_content (str): 新摘要内容，不能为空。
        last_summarized_message_id (str | None): 本次摘要覆盖到的最后一条消息 `_id`。
        last_summarized_message_uuid (str | None): 本次摘要覆盖到的最后一条消息业务 UUID。
        summary_version (int): 摘要版本号，要求 >= 1。
        summary_token_count (int): 摘要 token 数，要求 >= 0。
        status (Literal["success", "error"]): 摘要状态。
        expected_last_summarized_message_id (str | None):
            CAS 期望游标值。仅当库中当前游标与该值一致时才允许更新；
            不一致则返回 `None` 表示跳过写入。

    返回值：
        str | None:
            - 成功写入时返回摘要文档 `_id` 字符串；
            - CAS 未命中时返回 `None`。

    异常说明：
        ServiceException:
            - BAD_REQUEST: `conversation_id` 不是合法 ObjectId。
            - DATABASE_ERROR: 数据库返回结果不合法。

    Note:
        数据库异常会由全局异常处理器统一拦截。
    """

    now = datetime.datetime.now()
    normalized_summary = summary_content.strip()
    if not normalized_summary:
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="summary_content 不能为空")

    if summary_version < 1:
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="summary_version 必须大于等于 1")
    if summary_token_count < 0:
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="summary_token_count 不能为负数")

    normalized_last_message_id = _normalize_optional_string(last_summarized_message_id)
    normalized_last_message_uuid = _normalize_optional_string(last_summarized_message_uuid)
    normalized_expected_last_message_id = _normalize_optional_string(expected_last_summarized_message_id)

    db = get_mongo_database()
    collection = db[_resolve_collection_name()]

    conversation_object_id = _to_object_id(conversation_id)
    current_document = collection.find_one(
        {"conversation_id": conversation_object_id},
        {
            "_id": 1,
            "last_summarized_message_id": 1,
        },
    )
    if current_document is None:
        if normalized_expected_last_message_id is not None:
            return None
        query: dict[str, object] = {"conversation_id": conversation_object_id}
        upsert = True
    else:
        current_last_message_id = _normalize_optional_string(current_document.get("last_summarized_message_id"))
        if current_last_message_id != normalized_expected_last_message_id:
            return None
        query = {"_id": current_document["_id"]}
        upsert = False

    update_payload = ConversationSummaryUpsertPayload.model_validate(
        {
            "$set": ConversationSummaryUpdateSet(
                summary_content=normalized_summary,
                last_summarized_message_id=normalized_last_message_id,
                last_summarized_message_uuid=normalized_last_message_uuid,
                summary_version=summary_version,
                summary_token_count=summary_token_count,
                status=status,
                updated_at=now,
            ),
            "$setOnInsert": ConversationSummarySetOnInsert(
                conversation_id=conversation_object_id,
                created_at=now,
            ),
        }
    )
    update_doc = update_payload.model_dump(by_alias=True, mode="python")

    document = collection.find_one_and_update(
        query,
        update_doc,
        upsert=upsert,
        return_document=ReturnDocument.AFTER,
    )

    if document is None:
        return None
    if not isinstance(document, dict) or "_id" not in document:
        raise ServiceException(code=ResponseCode.DATABASE_ERROR, message="数据库错误")
    return str(document["_id"])


def get_conversation_summary(
        *,
        conversation_id: Annotated[str, Field(min_length=1)],
) -> ConversationSummary | None:
    """
    功能描述：
        按 `conversation_id` 查询会话摘要文档。

    参数说明：
        conversation_id (str): 会话 Mongo 主键（ObjectId 字符串）。

    返回值：
        ConversationSummary | None:
            命中返回摘要文档模型；未命中返回 `None`。

    异常说明：
        ServiceException:
            - BAD_REQUEST: `conversation_id` 不是合法 ObjectId。

    Note:
        数据库异常会由全局异常处理器统一拦截。
    """

    db = get_mongo_database()
    collection = db[_resolve_collection_name()]
    query = {"conversation_id": _to_object_id(conversation_id)}

    document = collection.find_one(query)
    if document is None:
        return None
    return ConversationSummary.model_validate(document)
