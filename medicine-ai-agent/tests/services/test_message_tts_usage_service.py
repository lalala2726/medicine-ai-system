import datetime

import pytest
from bson import ObjectId
from bson.int64 import Int64

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.services import message_tts_usage_service as service_module


class _DummyInsertResult:
    def __init__(self, inserted_id: str):
        self.inserted_id = inserted_id


class _DummyCollection:
    def __init__(self):
        self.last_inserted: dict | None = None
        self.created_indexes: list[dict] = []

    def insert_one(self, document: dict) -> _DummyInsertResult:
        self.last_inserted = document
        return _DummyInsertResult("507f1f77bcf86cd799439081")

    def create_index(self, keys, name: str | None = None):
        self.created_indexes.append({"keys": keys, "name": name})
        return name or "index"


def _build_payload() -> dict:
    return {
        "message_uuid": "msg-1",
        "conversation_id": "507f1f77bcf86cd799439011",
        "conversation_uuid": "conv-1",
        "user_id": 101,
        "endpoint": "wss://example.com/tts",
        "resource_id": "volc.service_type.10029",
        "voice_type": "zh_female_1",
        "encoding": "mp3",
        "sample_rate": 24000,
        "sent_text": "测试文本",
        "source_text_chars": 4,
        "sanitized_text_chars": 4,
        "max_text_chars": 300,
        "is_truncated": False,
        "audio_chunk_count": 2,
        "audio_bytes": 1024,
        "duration_ms": 321,
    }


def test_add_message_tts_usage_inserts_expected_document(monkeypatch):
    service_module._ensure_message_tts_usage_indexes.cache_clear()
    collection = _DummyCollection()
    monkeypatch.setattr(
        service_module,
        "get_mongo_database",
        lambda: {"message_tts_usages": collection},
    )
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "message_tts_usages")

    result = service_module.add_message_tts_usage(**_build_payload())

    assert result == "507f1f77bcf86cd799439081"
    assert collection.last_inserted is not None
    assert collection.last_inserted["message_uuid"] == "msg-1"
    assert collection.last_inserted["conversation_id"] == ObjectId("507f1f77bcf86cd799439011")
    assert collection.last_inserted["conversation_uuid"] == "conv-1"
    assert collection.last_inserted["user_id"] == Int64(101)
    assert collection.last_inserted["billable_chars"] == len("测试文本")
    assert collection.last_inserted["provider"] == "volcengine"
    assert collection.last_inserted["status"] == "success"
    assert len(collection.created_indexes) == 4
    assert isinstance(collection.last_inserted["created_at"], datetime.datetime)
    assert isinstance(collection.last_inserted["updated_at"], datetime.datetime)


def test_add_message_tts_usage_handles_optional_fields(monkeypatch):
    service_module._ensure_message_tts_usage_indexes.cache_clear()
    collection = _DummyCollection()
    monkeypatch.setattr(
        service_module,
        "get_mongo_database",
        lambda: {"message_tts_usages": collection},
    )
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "message_tts_usages")

    payload = _build_payload()
    payload.update(
        {
            "connect_id": "connect-1",
            "session_id": "session-1",
            "provider_log_id": "log-1",
            "usage_uuid": "usage-1",
        }
    )
    service_module.add_message_tts_usage(**payload)

    assert collection.last_inserted is not None
    assert collection.last_inserted["uuid"] == "usage-1"
    assert collection.last_inserted["connect_id"] == "connect-1"
    assert collection.last_inserted["session_id"] == "session-1"
    assert collection.last_inserted["provider_log_id"] == "log-1"


def test_add_message_tts_usage_rejects_invalid_conversation_id(monkeypatch):
    service_module._ensure_message_tts_usage_indexes.cache_clear()
    collection = _DummyCollection()
    monkeypatch.setattr(
        service_module,
        "get_mongo_database",
        lambda: {"message_tts_usages": collection},
    )
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "message_tts_usages")

    payload = _build_payload()
    payload["conversation_id"] = "invalid-object-id"

    with pytest.raises(ServiceException) as exc_info:
        service_module.add_message_tts_usage(**payload)

    assert exc_info.value.code == ResponseCode.BAD_REQUEST


def test_resolve_collection_name_returns_fixed_constant():
    assert service_module._resolve_collection_name() == "message_tts_usages"
