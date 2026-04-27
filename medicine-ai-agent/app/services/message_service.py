from __future__ import annotations

import datetime
import uuid
from functools import lru_cache
from typing import Annotated, Any

from bson import ObjectId
from pydantic import Field
from pymongo import ASCENDING, DESCENDING

from app.core.codes import ResponseCode
from app.core.database.mongodb import MONGODB_MESSAGES_COLLECTION, get_mongo_database
from app.core.exception.exceptions import ServiceException
from app.schemas.document.message import (
    MessageCard,
    MessageRole,
    MessageCreate,
    MessageDocument,
    MessageStatus,
)


def _resolve_collection_name() -> str:
    """返回 messages 集合固定名称常量。"""

    return MONGODB_MESSAGES_COLLECTION


def _get_collection():
    """获取 messages 集合对象。"""

    db = get_mongo_database()
    return db[_resolve_collection_name()]


@lru_cache(maxsize=1)
def _ensure_message_indexes() -> None:
    """懒初始化 messages 集合常用索引。"""

    collection = _get_collection()
    collection.create_index([("uuid", ASCENDING)], unique=True, name="uniq_uuid")
    collection.create_index(
        [
            ("conversation_id", ASCENDING),
            ("history_hidden", ASCENDING),
            ("created_at", DESCENDING),
        ],
        name="idx_conversation_visible_created_at_desc",
    )
    collection.create_index(
        [
            ("conversation_id", ASCENDING),
            ("card_uuids", ASCENDING),
            ("uuid", ASCENDING),
        ],
        name="idx_conversation_card_uuid_message_uuid",
    )


def _to_object_id(raw_conversation_id: str) -> ObjectId:
    """
    将字符串会话ID转换为 MongoDB ObjectId。

    数据表约束要求 `conversation_id` 为 `objectId`，这里统一做转换与错误拦截。
    """

    try:
        return ObjectId(raw_conversation_id)
    except Exception as exc:  # pragma: no cover - 防御性兜底
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message="conversation_id 格式不正确",
        ) from exc


def _to_message_document(document: dict[str, Any]) -> MessageDocument:
    """
    将 Mongo 原始文档转换为消息模型。

    统一由 schema 做字段归一化（如 ObjectId -> str），避免业务层重复转换逻辑。
    """

    return MessageDocument.model_validate(document)


def _normalize_thinking(thinking: Any) -> str | None:
    """归一化 thinking 文本，空值或空白内容返回 None。"""

    if not isinstance(thinking, str):
        return None
    normalized = thinking.strip()
    return normalized or None


def _normalize_content(content: Any) -> str:
    """
    归一化消息内容。

    用途：
    - 统一处理数据库落库前的 content；
    - 将 `None`、非字符串和纯空白内容统一收敛为空字符串；
    - 让上层由 schema 继续判断“该空字符串是否允许落库”。

    Args:
        content: 原始消息内容，允许为任意类型。

    Returns:
        str: 归一化后的消息内容；无有效文本时返回空字符串。
    """

    if not isinstance(content, str):
        return ""
    if not content.strip():
        return ""
    return content


def _normalize_cards(
        cards: list[MessageCard | dict[str, Any]] | None,
) -> list[MessageCard | dict[str, Any]] | None:
    """
    归一化卡片列表。

    用途：
    - 统一处理落库前的 `cards` 字段；
    - 将空列表与 `None` 统一视为“未传卡片”；
    - 保持非空卡片列表的原始顺序不变。

    Args:
        cards: 原始卡片列表，元素可以是 `MessageCard` 或普通字典。

    Returns:
        list[MessageCard | dict[str, Any]] | None:
            非空时返回原列表；为空时返回 `None`。
    """

    if not cards:
        return None
    return cards


def _extract_card_uuids(
        cards: list[MessageCard | dict[str, Any]] | None,
) -> list[str] | None:
    """从消息卡片列表中提取 card_uuid 集合，兼容历史 `id` 字段。"""

    normalized_cards = _normalize_cards(cards)
    if normalized_cards is None:
        return None

    card_uuids: list[str] = []
    for raw_card in normalized_cards:
        if hasattr(raw_card, "model_dump"):
            payload = raw_card.model_dump(mode="json", exclude_none=True)
        elif isinstance(raw_card, dict):
            payload = raw_card
        else:
            payload = {
                "card_uuid": getattr(raw_card, "card_uuid", None),
                "id": getattr(raw_card, "id", None),
            }
        card_uuid = str(payload.get("card_uuid") or payload.get("id") or "").strip()
        if card_uuid and card_uuid not in card_uuids:
            card_uuids.append(card_uuid)
    return card_uuids or None


def _build_messages_query(
        *,
        conversation_id: Annotated[str, Field(min_length=1)],
        history_hidden: bool | None = None,
        statuses: list[MessageStatus | str] | None = None,
) -> dict[str, Any]:
    """
    构建普通消息查询条件，支持按客户端可见性与状态过滤。

    Args:
        conversation_id: 会话 Mongo ObjectId（字符串形式）。
        history_hidden: 是否按客户端隐藏标记过滤。
        statuses: 可选消息状态白名单；为空时不过滤状态。

    Returns:
        dict[str, Any]: Mongo 查询条件。
    """

    query: dict[str, Any] = {"conversation_id": _to_object_id(conversation_id)}
    if history_hidden is True:
        query["history_hidden"] = True
    elif history_hidden is False:
        query["history_hidden"] = {"$ne": True}
    if statuses:
        query["status"] = {
            "$in": [MessageStatus(status).value for status in statuses]
        }
    return query


def _find_card_message_document(
        *,
        conversation_id: str,
        message_uuid: str,
        card_uuid: str,
) -> dict[str, Any] | None:
    """
    在当前会话中定位指定卡片所属的 AI 消息文档。

    查询策略：
    1. 先按 `message_uuid + card_uuid` 精确匹配；
    2. 若前端传入的 `message_uuid` 不是原始 AI 消息 UUID，再退化为当前会话内按 `card_uuid` 匹配。

    Args:
        conversation_id: 当前会话 Mongo ObjectId（字符串形式）。
        message_uuid: 前端上报的消息 UUID。
        card_uuid: 被点击卡片 UUID。

    Returns:
        dict[str, Any] | None: 命中的原始消息文档；未命中时返回 `None`。
    """

    collection = _get_collection()
    base_card_query = {
        "$or": [
            {"card_uuids": card_uuid},
            {"cards.id": card_uuid},
            {"cards.card_uuid": card_uuid},
        ],
    }
    precise_query = {
        "conversation_id": _to_object_id(conversation_id),
        "uuid": message_uuid,
        **base_card_query,
    }
    document = collection.find_one(precise_query)
    if document is not None:
        return document

    fallback_query = {
        "conversation_id": _to_object_id(conversation_id),
        **base_card_query,
    }
    return collection.find_one(fallback_query)


def add_message(
        *,
        conversation_id: Annotated[str, Field(min_length=1)],
        role: MessageRole | str,
        status: MessageStatus | str = MessageStatus.SUCCESS,
        content: str,
        thinking: str | None = None,
        cards: list[MessageCard | dict[str, Any]] | None = None,
        message_uuid: str | None = None,
) -> str:
    """
    新增一条会话消息。

    Args:
        conversation_id: 所属会话 Mongo ObjectId（字符串形式）。
        role: 消息角色（user/ai）。
        status: 消息状态（success/error）。
        content: 消息内容。
        thinking: 可选 AI 深度思考完整文本，仅 ai 消息会保存。
        cards: 可选消息卡片列表，支持 user 与 ai 消息持久化。
        message_uuid: 可选消息 UUID，不传时自动生成。

    Returns:
        str: 新增消息的 Mongo ObjectId 字符串。

    Raises:
        ServiceException: 当参数不合法时抛出。

    Note:
        数据库异常会由全局异常处理器统一拦截。
    """

    _ensure_message_indexes()
    normalized_role = MessageRole(role)
    normalized_cards = _normalize_cards(cards)
    payload = MessageCreate(
        uuid=message_uuid or str(uuid.uuid4()),
        conversation_id=conversation_id,
        role=normalized_role,
        status=status,
        content=_normalize_content(content),
        thinking=(
            _normalize_thinking(thinking)
            if normalized_role == MessageRole.AI
            else None
        ),
        cards=normalized_cards,
        card_uuids=_extract_card_uuids(normalized_cards),
        hidden_card_uuids=None,
        history_hidden=False,
    )

    now = datetime.datetime.now()
    # Mongo 写入文档统一由 Pydantic 模型序列化产出。
    document = payload.model_dump()
    if document.get("thinking") is None:
        document.pop("thinking", None)
    if document.get("cards") is None:
        document.pop("cards", None)
    if document.get("card_uuids") is None:
        document.pop("card_uuids", None)
    if document.get("hidden_card_uuids") is None:
        document.pop("hidden_card_uuids", None)
    document["conversation_id"] = _to_object_id(payload.conversation_id)
    document["created_at"] = now
    document["updated_at"] = now

    collection = _get_collection()
    result = collection.insert_one(document)
    return str(result.inserted_id)


def update_assistant_message(
        *,
        conversation_id: Annotated[str, Field(min_length=1)],
        message_uuid: Annotated[str, Field(min_length=1)],
        status: MessageStatus | str,
        content: str,
        thinking: str | None = None,
        cards: list[MessageCard | dict[str, Any]] | None = None,
) -> bool:
    """
    更新一条已存在的 AI 消息。

    Args:
        conversation_id: 所属会话 Mongo ObjectId（字符串形式）。
        message_uuid: 待更新的 AI 消息 UUID。
        status: 消息状态。
        content: 最新消息内容。
        thinking: 最新思考内容。
        cards: 最新卡片列表。

    Returns:
        bool: 命中并完成更新返回 `True`，否则返回 `False`。

    Raises:
        ServiceException: 参数校验失败时抛出。
    """

    normalized_cards = _normalize_cards(cards)
    payload = MessageCreate(
        uuid=message_uuid,
        conversation_id=conversation_id,
        role=MessageRole.AI,
        status=status,
        content=_normalize_content(content),
        thinking=_normalize_thinking(thinking),
        cards=normalized_cards,
        card_uuids=_extract_card_uuids(normalized_cards),
        hidden_card_uuids=None,
        history_hidden=False,
    )

    now = datetime.datetime.now()
    set_document: dict[str, Any] = {
        "status": payload.status,
        "content": payload.content,
        "updated_at": now,
    }
    unset_document: dict[str, Any] = {}

    if payload.thinking is None:
        unset_document["thinking"] = ""
    else:
        set_document["thinking"] = payload.thinking

    if payload.cards is None:
        unset_document["cards"] = ""
        unset_document["card_uuids"] = ""
    else:
        set_document["cards"] = [
            card.model_dump(mode="python")
            if hasattr(card, "model_dump")
            else dict(card)
            for card in payload.cards
        ]
        set_document["card_uuids"] = payload.card_uuids or []

    update_document: dict[str, Any] = {"$set": set_document}
    if unset_document:
        update_document["$unset"] = unset_document

    collection = _get_collection()
    result = collection.update_one(
        {
            "uuid": message_uuid,
            "conversation_id": _to_object_id(conversation_id),
            "role": MessageRole.AI,
        },
        update_document,
    )
    return bool(getattr(result, "matched_count", 0) >= 1)


def get_message_by_uuid(message_uuid: Annotated[str, Field(min_length=1)]) -> MessageDocument | None:
    """
    按消息 UUID 查询单条会话消息。

    Args:
        message_uuid: 消息业务唯一ID。

    Returns:
        MessageDocument | None: 命中返回消息模型，否则返回 None。

    Note:
        数据库异常会由全局异常处理器统一拦截。
    """

    collection = _get_collection()
    document = collection.find_one({"uuid": message_uuid})
    if document is None:
        return None
    return _to_message_document(document)


def list_messages(
        *,
        conversation_id: Annotated[str, Field(min_length=1)],
        limit: Annotated[int, Field(ge=1)] = 50,
        skip: Annotated[int, Field(ge=0)] = 0,
        ascending: bool = True,
        history_hidden: bool | None = None,
        statuses: list[MessageStatus | str] | None = None,
) -> list[MessageDocument]:
    """
    查询某个会话下的消息列表。

    Args:
        conversation_id: 所属会话 Mongo ObjectId（字符串形式）。
        limit: 返回条数上限，默认 50。
        skip: 跳过条数，默认 0。
        ascending: 是否按创建时间升序，默认 True（旧到新）。
        history_hidden: 是否按客户端可见性过滤。
        statuses: 可选消息状态白名单。

    Returns:
        list[MessageDocument]: 消息文档模型列表。
            仅负责数据库模型输出，不做上层会话消息格式转换。

    Note:
        数据库异常会由全局异常处理器统一拦截。
    """

    sort_direction = ASCENDING if ascending else DESCENDING
    query = _build_messages_query(
        conversation_id=conversation_id,
        history_hidden=history_hidden,
        statuses=statuses,
    )
    collection = _get_collection()
    cursor = collection.find(query).sort("created_at", sort_direction).skip(skip).limit(limit)
    return [_to_message_document(item) for item in cursor]


def count_messages(
        *,
        conversation_id: Annotated[str, Field(min_length=1)],
        history_hidden: bool | None = None,
        statuses: list[MessageStatus | str] | None = None,
) -> int:
    """
    统计某个会话下的消息总数。

    Args:
        conversation_id: 所属会话 Mongo ObjectId（字符串形式）。

    Returns:
        int: 当前会话命中的消息总数。

    Note:
        仅按 `conversation_id` 统计，不附加其他角色、状态或摘要过滤条件。
    """

    query = _build_messages_query(
        conversation_id=conversation_id,
        history_hidden=history_hidden,
        statuses=statuses,
    )
    collection = _get_collection()
    return int(collection.count_documents(query))


def _build_summarizable_messages_query(
        *,
        conversation_id: Annotated[str, Field(min_length=1)],
        after_message_id: str | None = None,
        history_hidden: bool | None = None,
) -> dict[str, Any]:
    """
    功能描述：
        构建“可参与摘要”的消息查询条件。

    参数说明：
        conversation_id (str): 会话 Mongo ObjectId（字符串形式）。
        after_message_id (str | None): 可选消息游标，仅查询 `_id` 大于该值的消息。

    返回值：
        dict[str, Any]: MongoDB 查询条件。

    异常说明：
        ServiceException:
            - BAD_REQUEST: `conversation_id` 或 `after_message_id` 不是合法 ObjectId。
    """

    query: dict[str, Any] = {
        "conversation_id": _to_object_id(conversation_id),
        "role": {
            "$in": [
                MessageRole.USER.value,
                MessageRole.AI.value,
            ]
        },
        "status": {
            "$in": [
                MessageStatus.SUCCESS.value,
                MessageStatus.WAITING_INPUT.value,
            ]
        },
    }
    normalized_after_message_id = (after_message_id or "").strip()
    if normalized_after_message_id:
        try:
            query["_id"] = {"$gt": ObjectId(normalized_after_message_id)}
        except Exception as exc:
            raise ServiceException(
                code=ResponseCode.BAD_REQUEST,
                message="after_message_id 格式不正确",
            ) from exc
    if history_hidden is True:
        query["history_hidden"] = True
    elif history_hidden is False:
        query["history_hidden"] = {"$ne": True}
    return query


def count_summarizable_messages(
        *,
        conversation_id: Annotated[str, Field(min_length=1)],
        after_message_id: str | None = None,
        history_hidden: bool | None = None,
) -> int:
    """
    功能描述：
        统计某会话中可参与摘要的消息数量。

    参数说明：
        conversation_id (str): 会话 Mongo ObjectId（字符串形式）。
        after_message_id (str | None): 可选消息游标，仅统计 `_id` 更大的消息。

    返回值：
        int: 命中的消息条数。

    异常说明：
        ServiceException:
            - BAD_REQUEST: ID 不是合法 ObjectId。
    """

    query = _build_summarizable_messages_query(
        conversation_id=conversation_id,
        after_message_id=after_message_id,
        history_hidden=history_hidden,
    )
    collection = _get_collection()
    return int(collection.count_documents(query))


def list_summarizable_messages(
        *,
        conversation_id: Annotated[str, Field(min_length=1)],
        limit: Annotated[int, Field(ge=1)] = 50,
        after_message_id: str | None = None,
        ascending: bool = True,
        history_hidden: bool | None = None,
) -> list[MessageDocument]:
    """
    功能描述：
        查询某会话中可参与摘要的消息列表。

    参数说明：
        conversation_id (str): 会话 Mongo ObjectId（字符串形式）。
        limit (int): 返回条数上限。
        after_message_id (str | None): 可选消息游标，仅查询 `_id` 更大的消息。
        ascending (bool): 是否按创建时间升序返回；`True` 表示旧到新。

    返回值：
        list[MessageDocument]: 满足摘要条件的消息列表。

    异常说明：
        ServiceException:
            - BAD_REQUEST: ID 不是合法 ObjectId。
    """

    query = _build_summarizable_messages_query(
        conversation_id=conversation_id,
        after_message_id=after_message_id,
        history_hidden=history_hidden,
    )
    sort_direction = ASCENDING if ascending else DESCENDING
    collection = _get_collection()
    cursor = collection.find(query).sort("created_at", sort_direction).limit(limit)
    return [_to_message_document(item) for item in cursor]


def list_latest_summarizable_messages(
        *,
        conversation_id: Annotated[str, Field(min_length=1)],
        limit: Annotated[int, Field(ge=1)] = 100,
        after_message_id: str | None = None,
        history_hidden: bool | None = None,
) -> list[MessageDocument]:
    """
    功能描述：
        获取“最新 N 条可参与摘要消息”，并按时间正序返回。

    参数说明：
        conversation_id (str): 会话 Mongo ObjectId（字符串形式）。
        limit (int): 需要的最新消息数量。
        after_message_id (str | None): 可选消息游标，仅查询 `_id` 更大的消息。

    返回值：
        list[MessageDocument]: 最新 N 条消息（旧到新）。

    异常说明：
        ServiceException:
            - BAD_REQUEST: ID 不是合法 ObjectId。
    """

    latest_desc = list_summarizable_messages(
        conversation_id=conversation_id,
        limit=limit,
        after_message_id=after_message_id,
        ascending=False,
        history_hidden=history_hidden,
    )
    return list(reversed(latest_desc))


def list_summarizable_tail_messages(
        *,
        conversation_id: Annotated[str, Field(min_length=1)],
        limit: Annotated[int, Field(ge=1)] = 20,
        history_hidden: bool | None = None,
) -> list[MessageDocument]:
    """
    功能描述：
        获取会话中可参与摘要的“尾部消息窗口”，并按时间正序返回。

    参数说明：
        conversation_id (str): 会话 Mongo ObjectId（字符串形式）。
        limit (int): 尾部窗口大小。

    返回值：
        list[MessageDocument]: 尾部消息列表（旧到新）。

    异常说明：
        ServiceException:
            - BAD_REQUEST: ID 不是合法 ObjectId。
    """

    return list_latest_summarizable_messages(
        conversation_id=conversation_id,
        limit=limit,
        after_message_id=None,
        history_hidden=history_hidden,
    )


def _normalize_card_uuid_list(card_uuids: list[str] | None) -> list[str]:
    """归一化 card_uuid 列表，去空并保持原有顺序去重。"""

    normalized_card_uuids: list[str] = []
    for raw_card_uuid in card_uuids or []:
        normalized_card_uuid = str(raw_card_uuid or "").strip()
        if not normalized_card_uuid:
            continue
        if normalized_card_uuid in normalized_card_uuids:
            continue
        normalized_card_uuids.append(normalized_card_uuid)
    return normalized_card_uuids


def _resolve_message_card_uuids(message_document: MessageDocument) -> list[str]:
    """从消息文档中提取完整卡片 UUID 列表，兼容旧数据缺失 `card_uuids` 的情况。"""

    extracted_card_uuids = _extract_card_uuids(message_document.cards)
    if extracted_card_uuids:
        return extracted_card_uuids
    return _normalize_card_uuid_list(message_document.card_uuids)


def _resolve_message_card_visibility(
        *,
        message_document: MessageDocument,
        additional_hidden_card_uuids: list[str] | None = None,
) -> tuple[list[str], list[str], bool]:
    """计算消息卡片的最终可见性状态。"""

    all_card_uuids = _resolve_message_card_uuids(message_document)
    hidden_card_uuids = _normalize_card_uuid_list(message_document.hidden_card_uuids)

    for raw_card_uuid in additional_hidden_card_uuids or []:
        normalized_card_uuid = str(raw_card_uuid or "").strip()
        if not normalized_card_uuid:
            continue
        if normalized_card_uuid not in all_card_uuids:
            continue
        if normalized_card_uuid in hidden_card_uuids:
            continue
        hidden_card_uuids.append(normalized_card_uuid)

    normalized_content = message_document.content.strip()
    normalized_thinking = (message_document.thinking or "").strip()
    history_hidden = (
            bool(all_card_uuids)
            and not normalized_content
            and not normalized_thinking
            and all(card_uuid in hidden_card_uuids for card_uuid in all_card_uuids)
    )
    return all_card_uuids, hidden_card_uuids, history_hidden


def _build_message_update_query(
        *,
        conversation_id: str,
        message_document: MessageDocument,
) -> dict[str, Any]:
    """构造单条消息更新条件，优先按 `_id` 精确命中。"""

    if message_document.id:
        return {"_id": ObjectId(message_document.id)}
    return {
        "conversation_id": _to_object_id(conversation_id),
        "uuid": message_document.uuid,
    }


def hide_visible_cards_in_conversation(
        *,
        conversation_id: Annotated[str, Field(min_length=1)],
) -> int:
    """
    批量隐藏某个会话下当前仍对客户端可见的 AI 卡片。

    Returns:
        int: 本次实际更新的消息条数。
    """

    _ensure_message_indexes()
    collection = _get_collection()
    query = {
        "conversation_id": _to_object_id(conversation_id),
        "history_hidden": {"$ne": True},
        "$or": [
            {"card_uuids.0": {"$exists": True}},
            {"cards.0": {"$exists": True}},
        ],
    }

    updated_message_count = 0
    for document in collection.find(query):
        message_document = _to_message_document(document)
        if message_document.role != MessageRole.AI:
            continue

        all_card_uuids = _resolve_message_card_uuids(message_document)
        if not all_card_uuids:
            continue

        current_hidden_card_uuids = _normalize_card_uuid_list(message_document.hidden_card_uuids)
        visible_card_uuids = [
            card_uuid
            for card_uuid in all_card_uuids
            if card_uuid not in current_hidden_card_uuids
        ]
        resolved_card_uuids, resolved_hidden_card_uuids, history_hidden = (
            _resolve_message_card_visibility(
                message_document=message_document,
                additional_hidden_card_uuids=visible_card_uuids,
            )
        )
        current_card_uuids = _normalize_card_uuid_list(message_document.card_uuids)
        if (
                current_card_uuids == resolved_card_uuids
                and current_hidden_card_uuids == resolved_hidden_card_uuids
                and bool(message_document.history_hidden) == history_hidden
        ):
            continue

        collection.update_one(
            _build_message_update_query(
                conversation_id=conversation_id,
                message_document=message_document,
            ),
            {
                "$set": {
                    "card_uuids": resolved_card_uuids,
                    "hidden_card_uuids": resolved_hidden_card_uuids,
                    "history_hidden": history_hidden,
                    "updated_at": datetime.datetime.now(),
                }
            },
        )
        updated_message_count += 1

    return updated_message_count


def hide_message_card(
        *,
        conversation_id: Annotated[str, Field(min_length=1)],
        message_uuid: Annotated[str, Field(min_length=1)],
        card_uuid: Annotated[str, Field(min_length=1)],
) -> None:
    """
    标记某条 AI 消息中的卡片已点击，后续不再出现在客户端历史与记忆中。

    说明：
    - 仅允许处理当前会话下的 AI 消息；
    - `card_uuid` 不存在、消息不属于该会话、或消息不是 AI 消息时统一返回 BAD_REQUEST；
    - 重复点击同一卡片视为幂等成功。
    """

    normalized_message_uuid = message_uuid.strip()
    normalized_card_uuid = card_uuid.strip()
    if not normalized_message_uuid or not normalized_card_uuid:
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="卡片操作参数不合法")

    _ensure_message_indexes()
    collection = _get_collection()
    document = _find_card_message_document(
        conversation_id=conversation_id,
        message_uuid=normalized_message_uuid,
        card_uuid=normalized_card_uuid,
    )
    if document is None:
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="卡片操作无效")

    message_document = _to_message_document(document)
    if message_document.role != MessageRole.AI:
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="卡片操作无效")

    all_card_uuids = _resolve_message_card_uuids(message_document)
    if not all_card_uuids or normalized_card_uuid not in all_card_uuids:
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="卡片操作无效")

    resolved_card_uuids, resolved_hidden_card_uuids, history_hidden = _resolve_message_card_visibility(
        message_document=message_document,
        additional_hidden_card_uuids=[normalized_card_uuid],
    )
    collection.update_one(
        _build_message_update_query(
            conversation_id=conversation_id,
            message_document=message_document,
        ),
        {
            "$set": {
                "card_uuids": resolved_card_uuids,
                "hidden_card_uuids": resolved_hidden_card_uuids,
                "history_hidden": history_hidden,
                "updated_at": datetime.datetime.now(),
            }
        },
    )


def get_message_card_payload(
        *,
        conversation_id: Annotated[str, Field(min_length=1)],
        message_uuid: Annotated[str, Field(min_length=1)],
        card_uuid: Annotated[str, Field(min_length=1)],
) -> dict[str, Any]:
    """
    读取指定 AI 消息中的单张卡片原始载荷。

    说明：
    - 仅允许读取当前会话下的 AI 消息卡片；
    - `card_uuid` 不存在、消息不属于该会话、或消息不是 AI 消息时统一返回 BAD_REQUEST；
    - 返回值保持 `{type, data}` 原始结构，供上层拼接交互语义。
    """

    normalized_message_uuid = message_uuid.strip()
    normalized_card_uuid = card_uuid.strip()
    if not normalized_message_uuid or not normalized_card_uuid:
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="卡片操作参数不合法")

    _ensure_message_indexes()
    document = _find_card_message_document(
        conversation_id=conversation_id,
        message_uuid=normalized_message_uuid,
        card_uuid=normalized_card_uuid,
    )
    if document is None:
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="卡片操作无效")

    raw_role = str(document.get("role") or "").strip()
    if raw_role != MessageRole.AI.value:
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="卡片操作无效")

    raw_cards = document.get("cards")
    if not isinstance(raw_cards, list):
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="卡片操作无效")

    for raw_card in raw_cards:
        if not isinstance(raw_card, dict):
            continue
        current_card_uuid = str(raw_card.get("id") or raw_card.get("card_uuid") or "").strip()
        if current_card_uuid != normalized_card_uuid:
            continue
        card_type = str(raw_card.get("type") or "").strip()
        card_data = raw_card.get("data")
        if not card_type or not isinstance(card_data, dict):
            raise ServiceException(code=ResponseCode.BAD_REQUEST, message="卡片操作无效")
        return {
            "type": card_type,
            "data": dict(card_data),
        }

    raise ServiceException(code=ResponseCode.BAD_REQUEST, message="卡片操作无效")
