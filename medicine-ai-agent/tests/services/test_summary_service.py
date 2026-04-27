import datetime

import pytest
from bson import ObjectId

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.schemas.document.conversation_summary import (
    ConversationSummary,
    ConversationSummarySetOnInsert,
    ConversationSummaryUpsertPayload,
    ConversationSummaryUpdateSet,
)
from app.services import summary_service as service_module


class _DummyCollection:
    def __init__(self):
        self.last_find_one_query: dict | None = None
        self.last_find_one_projection: dict | None = None
        self.last_find_one_and_update_query: dict | None = None
        self.last_find_one_and_update_doc: dict | None = None
        self.last_find_one_and_update_upsert: bool | None = None
        self.find_one_result: dict | None = None
        self.find_one_and_update_result: dict | None = None

    def find_one(self, query: dict, projection: dict | None = None) -> dict | None:
        self.last_find_one_query = query
        self.last_find_one_projection = projection
        return self.find_one_result

    def find_one_and_update(
            self,
            query: dict,
            update_doc: dict,
            upsert: bool,
            return_document: object,
    ) -> dict | None:
        self.last_find_one_and_update_query = query
        self.last_find_one_and_update_doc = update_doc
        self.last_find_one_and_update_upsert = upsert
        _ = return_document
        return self.find_one_and_update_result


def test_save_conversation_summary_upserts_when_document_missing(monkeypatch):
    """测试目的：首次写入摘要时执行 upsert；预期结果：返回文档ID且写入新游标字段。"""

    collection = _DummyCollection()
    collection.find_one_result = None
    collection.find_one_and_update_result = {"_id": ObjectId("507f1f77bcf86cd799439031")}
    monkeypatch.setattr(
        service_module,
        "get_mongo_database",
        lambda: {"conversation_summaries": collection},
    )
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "conversation_summaries")

    result = service_module.save_conversation_summary(
        conversation_id="507f1f77bcf86cd799439011",
        summary_content="这是总结",
        last_summarized_message_id="507f1f77bcf86cd799439021",
        last_summarized_message_uuid="msg-2",
        summary_version=2,
        summary_token_count=128,
        status="success",
        expected_last_summarized_message_id=None,
    )

    assert result == "507f1f77bcf86cd799439031"
    assert collection.last_find_one_query == {"conversation_id": ObjectId("507f1f77bcf86cd799439011")}
    assert collection.last_find_one_and_update_query == {"conversation_id": ObjectId("507f1f77bcf86cd799439011")}
    assert collection.last_find_one_and_update_upsert is True
    update_doc = collection.last_find_one_and_update_doc
    assert update_doc is not None
    assert update_doc["$set"]["summary_content"] == "这是总结"
    assert update_doc["$set"]["last_summarized_message_id"] == "507f1f77bcf86cd799439021"
    assert update_doc["$set"]["last_summarized_message_uuid"] == "msg-2"
    assert update_doc["$set"]["summary_version"] == 2
    assert update_doc["$set"]["summary_token_count"] == 128

    expected_update_doc = ConversationSummaryUpsertPayload(
        set_fields=ConversationSummaryUpdateSet(
            summary_content="这是总结",
            last_summarized_message_id="507f1f77bcf86cd799439021",
            last_summarized_message_uuid="msg-2",
            summary_version=2,
            summary_token_count=128,
            status="success",
            updated_at=update_doc["$set"]["updated_at"],
        ),
        set_on_insert_fields=ConversationSummarySetOnInsert(
            conversation_id=ObjectId("507f1f77bcf86cd799439011"),
            created_at=update_doc["$setOnInsert"]["created_at"],
        ),
    ).model_dump(by_alias=True, mode="python")
    assert update_doc == expected_update_doc


def test_save_conversation_summary_returns_none_on_cas_mismatch(monkeypatch):
    """测试目的：CAS 游标不匹配时跳过写入；预期结果：返回 None 且不执行 find_one_and_update。"""

    collection = _DummyCollection()
    collection.find_one_result = {
        "_id": ObjectId("507f1f77bcf86cd799439032"),
        "last_summarized_message_id": "507f1f77bcf86cd799439099",
    }
    monkeypatch.setattr(
        service_module,
        "get_mongo_database",
        lambda: {"conversation_summaries": collection},
    )
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "conversation_summaries")

    result = service_module.save_conversation_summary(
        conversation_id="507f1f77bcf86cd799439011",
        summary_content="新的总结",
        expected_last_summarized_message_id="507f1f77bcf86cd799439088",
    )

    assert result is None
    assert collection.last_find_one_and_update_query is None
    assert collection.last_find_one_and_update_doc is None


def test_save_conversation_summary_updates_when_cas_matches(monkeypatch):
    """测试目的：CAS 游标匹配时允许覆盖更新；预期结果：按 `_id` 更新且 upsert=False。"""

    collection = _DummyCollection()
    collection.find_one_result = {
        "_id": ObjectId("507f1f77bcf86cd799439033"),
        "last_summarized_message_id": "507f1f77bcf86cd799439022",
    }
    collection.find_one_and_update_result = {"_id": ObjectId("507f1f77bcf86cd799439033")}
    monkeypatch.setattr(
        service_module,
        "get_mongo_database",
        lambda: {"conversation_summaries": collection},
    )
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "conversation_summaries")

    result = service_module.save_conversation_summary(
        conversation_id="507f1f77bcf86cd799439011",
        summary_content="新的总结",
        last_summarized_message_id="507f1f77bcf86cd799439044",
        summary_version=3,
        summary_token_count=256,
        expected_last_summarized_message_id="507f1f77bcf86cd799439022",
    )

    assert result == "507f1f77bcf86cd799439033"
    assert collection.last_find_one_and_update_query == {"_id": ObjectId("507f1f77bcf86cd799439033")}
    assert collection.last_find_one_and_update_upsert is False


def test_save_conversation_summary_rejects_invalid_conversation_id(monkeypatch):
    """测试目的：非法 conversation_id 被拒绝；预期结果：抛 BAD_REQUEST。"""

    collection = _DummyCollection()
    monkeypatch.setattr(
        service_module,
        "get_mongo_database",
        lambda: {"conversation_summaries": collection},
    )
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "conversation_summaries")

    with pytest.raises(ServiceException) as exc_info:
        service_module.save_conversation_summary(
            conversation_id="invalid-object-id",
            summary_content="summary",
        )

    assert exc_info.value.code == ResponseCode.BAD_REQUEST


def test_get_conversation_summary_returns_typed_model(monkeypatch):
    """测试目的：查询结果转换为 ConversationSummary；预期结果：新游标字段正确反序列化。"""

    collection = _DummyCollection()
    collection.find_one_result = {
        "_id": ObjectId("507f1f77bcf86cd799439034"),
        "conversation_id": ObjectId("507f1f77bcf86cd799439011"),
        "summary_content": "总结内容",
        "last_summarized_message_id": ObjectId("507f1f77bcf86cd799439055"),
        "last_summarized_message_uuid": "msg-9",
        "summary_version": 5,
        "summary_token_count": 512,
        "created_at": datetime.datetime(2026, 1, 1, 10, 0, 0),
        "updated_at": datetime.datetime(2026, 1, 1, 11, 0, 0),
        "status": "success",
    }
    monkeypatch.setattr(
        service_module,
        "get_mongo_database",
        lambda: {"conversation_summaries": collection},
    )
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "conversation_summaries")

    result = service_module.get_conversation_summary(conversation_id="507f1f77bcf86cd799439011")

    assert isinstance(result, ConversationSummary)
    assert result is not None
    assert result.id == "507f1f77bcf86cd799439034"
    assert result.conversation_id == "507f1f77bcf86cd799439011"
    assert result.last_summarized_message_id == "507f1f77bcf86cd799439055"
    assert result.last_summarized_message_uuid == "msg-9"
    assert result.summary_version == 5
    assert result.summary_token_count == 512
