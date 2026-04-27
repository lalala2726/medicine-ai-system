from __future__ import annotations

import datetime
import uuid
from functools import lru_cache
from typing import Annotated

from bson import ObjectId
from pydantic import Field
from pymongo import DESCENDING

from app.core.codes import ResponseCode
from app.core.database.mongodb import (
    get_mongo_database,
    MONGODB_MESSAGE_TTS_USAGES_COLLECTION,
)
from app.core.exception.exceptions import ServiceException
from app.schemas.document.message_tts_usage import (
    MessageTtsUsageCreate,
    TtsUsageProvider,
    TtsUsageStatus,
)


def _resolve_collection_name() -> str:
    """返回 message_tts_usages 集合固定名称常量。"""

    return MONGODB_MESSAGE_TTS_USAGES_COLLECTION


def _to_object_id(raw_conversation_id: str) -> ObjectId:
    """将字符串会话ID转换为 MongoDB ObjectId。"""

    try:
        return ObjectId(raw_conversation_id)
    except Exception as exc:  # pragma: no cover - 防御性兜底
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message="conversation_id 格式不正确",
        ) from exc


@lru_cache(maxsize=1)
def _ensure_message_tts_usage_indexes() -> None:
    """
    确保 `message_tts_usages` 统计查询索引存在。

    索引策略：
    1. message_uuid + created_at(desc)
    2. user_id + created_at(desc)
    3. conversation_uuid + created_at(desc)
    4. created_at(desc)
    """

    db = get_mongo_database()
    collection = db[_resolve_collection_name()]
    collection.create_index(
        [("message_uuid", 1), ("created_at", DESCENDING)],
        name="idx_message_uuid_created_at_desc",
    )
    collection.create_index(
        [("user_id", 1), ("created_at", DESCENDING)],
        name="idx_user_id_created_at_desc",
    )
    collection.create_index(
        [("conversation_uuid", 1), ("created_at", DESCENDING)],
        name="idx_conversation_uuid_created_at_desc",
    )
    collection.create_index(
        [("created_at", DESCENDING)],
        name="idx_created_at_desc",
    )


def add_message_tts_usage(
        *,
        message_uuid: Annotated[str, Field(min_length=1)],
        conversation_id: Annotated[str, Field(min_length=1)],
        conversation_uuid: Annotated[str, Field(min_length=1)],
        user_id: Annotated[int, Field(ge=1)],
        endpoint: Annotated[str, Field(min_length=1)],
        resource_id: Annotated[str, Field(min_length=1)],
        voice_type: Annotated[str, Field(min_length=1)],
        encoding: Annotated[str, Field(min_length=1)],
        sample_rate: Annotated[int, Field(ge=1)],
        sent_text: Annotated[str, Field(min_length=1)],
        source_text_chars: Annotated[int, Field(ge=0)],
        sanitized_text_chars: Annotated[int, Field(ge=0)],
        max_text_chars: Annotated[int, Field(ge=1)],
        is_truncated: bool,
        audio_chunk_count: Annotated[int, Field(ge=0)],
        audio_bytes: Annotated[int, Field(ge=0)],
        duration_ms: Annotated[int, Field(ge=0)],
        connect_id: str | None = None,
        session_id: str | None = None,
        provider_log_id: str | None = None,
        provider: TtsUsageProvider | str = TtsUsageProvider.VOLCENGINE,
        status: TtsUsageStatus | str = TtsUsageStatus.SUCCESS,
        usage_uuid: str | None = None,
) -> str:
    """
    新增一条 TTS 调用成功明细。

    说明：
    - `billable_chars` 按最终发送给第三方的文本 `sent_text` 长度计算；
    - 仅负责落库，不附加业务权限/归属判断。
    """

    _ensure_message_tts_usage_indexes()

    payload = MessageTtsUsageCreate(
        uuid=usage_uuid or str(uuid.uuid4()),
        message_uuid=message_uuid,
        conversation_id=conversation_id,
        conversation_uuid=conversation_uuid,
        user_id=user_id,
        provider=provider,
        status=status,
        endpoint=endpoint,
        resource_id=resource_id,
        voice_type=voice_type,
        encoding=encoding,
        sample_rate=sample_rate,
        sent_text=sent_text,
        billable_chars=len(sent_text),
        source_text_chars=source_text_chars,
        sanitized_text_chars=sanitized_text_chars,
        max_text_chars=max_text_chars,
        is_truncated=is_truncated,
        audio_chunk_count=audio_chunk_count,
        audio_bytes=audio_bytes,
        duration_ms=duration_ms,
        connect_id=connect_id,
        session_id=session_id,
        provider_log_id=provider_log_id,
    )

    now = datetime.datetime.now()
    document = payload.model_dump()
    if document.get("connect_id") is None:
        document.pop("connect_id", None)
    if document.get("session_id") is None:
        document.pop("session_id", None)
    if document.get("provider_log_id") is None:
        document.pop("provider_log_id", None)
    document["conversation_id"] = _to_object_id(payload.conversation_id)
    document["created_at"] = now
    document["updated_at"] = now

    db = get_mongo_database()
    collection = db[_resolve_collection_name()]
    result = collection.insert_one(document)
    return str(result.inserted_id)
