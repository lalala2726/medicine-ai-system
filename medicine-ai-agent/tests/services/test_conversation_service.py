import datetime

from bson import ObjectId
from bson.int64 import Int64

from app.schemas.document.conversation import ConversationDocument, ConversationListItem
from app.services import conversation_service as service_module


class _DummyInsertResult:
    def __init__(self, inserted_id: str):
        self.inserted_id = inserted_id


class _DummyUpdateResult:
    def __init__(self, matched_count: int):
        self.matched_count = matched_count


class _DummyDeleteResult:
    def __init__(self, deleted_count: int):
        self.deleted_count = deleted_count


class _DummyCursor:
    def __init__(self, documents: list[dict]):
        self._documents = documents
        self.sort_args: tuple[str, int] | None = None
        self.skip_value = 0
        self.limit_value: int | None = None

    def sort(self, field_name: str, direction: int):
        self.sort_args = (field_name, direction)
        return self

    def skip(self, value: int):
        self.skip_value = value
        return self

    def limit(self, value: int):
        self.limit_value = value
        return self

    def __iter__(self):
        items = list(self._documents)
        if self.skip_value:
            items = items[self.skip_value:]
        if self.limit_value is not None:
            items = items[:self.limit_value]
        return iter(items)


class _DummyCollection:
    def __init__(self):
        self.last_inserted: dict | None = None
        self.last_query: dict | None = None
        self.find_one_result: dict | None = None
        self.last_update_query: dict | None = None
        self.last_update_doc: dict | None = None
        self.last_delete_query: dict | None = None
        self.find_result: list[dict] = []
        self.last_find_query: dict | None = None
        self.last_find_projection: dict | None = None
        self.last_count_query: dict | None = None
        self.last_cursor: _DummyCursor | None = None
        self.update_matched_count: int = 1

    def insert_one(self, document: dict) -> _DummyInsertResult:
        self.last_inserted = document
        return _DummyInsertResult("507f1f77bcf86cd799439011")

    def find_one(self, query: dict) -> dict | None:
        self.last_query = query
        return self.find_one_result

    def update_one(self, query: dict, update_doc: dict):
        self.last_update_query = query
        self.last_update_doc = update_doc
        return _DummyUpdateResult(self.update_matched_count)

    def count_documents(self, query: dict) -> int:
        self.last_count_query = query
        return len(self.find_result)

    def find(self, query: dict, projection: dict):
        self.last_find_query = query
        self.last_find_projection = projection
        self.last_cursor = _DummyCursor(self.find_result)
        return self.last_cursor


def test_add_admin_conversation_uses_int64_user_id(monkeypatch):
    collection = _DummyCollection()
    monkeypatch.setattr(service_module, "get_mongo_database", lambda: {"conversations": collection})

    result = service_module.add_admin_conversation(
        conversation_uuid="conv-1",
        user_id=1,
    )

    assert result == "507f1f77bcf86cd799439011"
    assert collection.last_inserted is not None
    assert collection.last_inserted["uuid"] == "conv-1"
    assert collection.last_inserted["conversation_type"] == "admin"
    assert collection.last_inserted["user_id"] == Int64(1)
    assert isinstance(collection.last_inserted["user_id"], Int64)
    assert collection.last_inserted["title"] == "新聊天"
    assert isinstance(collection.last_inserted["create_time"], datetime.datetime)
    assert isinstance(collection.last_inserted["update_time"], datetime.datetime)
    assert collection.last_inserted["is_deleted"] == 0


def test_get_admin_conversation_uses_int64_user_id_in_query(monkeypatch):
    collection = _DummyCollection()
    collection.find_one_result = {
        "_id": ObjectId("507f1f77bcf86cd799439021"),
        "uuid": "conv-1",
        "conversation_type": "admin",
        "user_id": Int64(1),
        "title": "会话一",
        "create_time": datetime.datetime(2026, 1, 1, 10, 0, 0),
        "update_time": datetime.datetime(2026, 1, 1, 10, 0, 0),
        "is_deleted": 0,
    }
    monkeypatch.setattr(service_module, "get_mongo_database", lambda: {"conversations": collection})

    result = service_module.get_admin_conversation(
        conversation_uuid="conv-1",
        user_id=1,
    )

    assert isinstance(result, ConversationDocument)
    assert result is not None
    assert result.id == "507f1f77bcf86cd799439021"
    assert result.uuid == "conv-1"
    assert result.conversation_type == "admin"
    assert result.user_id == 1
    assert collection.last_query == {
        "uuid": "conv-1",
        "conversation_type": "admin",
        "user_id": Int64(1),
        "is_deleted": {"$ne": 1},
    }


def test_get_admin_conversation_by_id_scopes_by_object_id_and_user(monkeypatch):
    collection = _DummyCollection()
    collection.find_one_result = {
        "_id": ObjectId("507f1f77bcf86cd799439031"),
        "uuid": "conv-1",
        "conversation_type": "admin",
        "user_id": Int64(9),
        "title": "会话一",
        "create_time": datetime.datetime(2026, 1, 1, 10, 0, 0),
        "update_time": datetime.datetime(2026, 1, 1, 10, 0, 0),
        "is_deleted": 0,
    }
    monkeypatch.setattr(service_module, "get_mongo_database", lambda: {"conversations": collection})

    result = service_module.get_admin_conversation_by_id(
        conversation_id="507f1f77bcf86cd799439031",
        user_id=9,
    )

    assert isinstance(result, ConversationDocument)
    assert result is not None
    assert result.id == "507f1f77bcf86cd799439031"
    assert collection.last_query == {
        "_id": ObjectId("507f1f77bcf86cd799439031"),
        "conversation_type": "admin",
        "user_id": Int64(9),
        "is_deleted": {"$ne": 1},
    }


def test_get_admin_conversation_by_id_returns_none_for_invalid_object_id():
    result = service_module.get_admin_conversation_by_id(
        conversation_id="invalid-object-id",
        user_id=9,
    )
    assert result is None


def test_get_client_conversation_by_id_scopes_by_object_id_and_user(monkeypatch):
    collection = _DummyCollection()
    collection.find_one_result = {
        "_id": ObjectId("507f1f77bcf86cd799439032"),
        "uuid": "client-conv-1",
        "conversation_type": "client",
        "user_id": Int64(10),
        "title": "客户端会话",
        "create_time": datetime.datetime(2026, 1, 1, 10, 0, 0),
        "update_time": datetime.datetime(2026, 1, 1, 10, 0, 0),
        "is_deleted": 0,
    }
    monkeypatch.setattr(service_module, "get_mongo_database", lambda: {"conversations": collection})

    result = service_module.get_client_conversation_by_id(
        conversation_id="507f1f77bcf86cd799439032",
        user_id=10,
    )

    assert isinstance(result, ConversationDocument)
    assert result is not None
    assert result.id == "507f1f77bcf86cd799439032"
    assert collection.last_query == {
        "_id": ObjectId("507f1f77bcf86cd799439032"),
        "conversation_type": "client",
        "user_id": Int64(10),
        "is_deleted": {"$ne": 1},
    }


def test_add_client_conversation_uses_client_type(monkeypatch):
    collection = _DummyCollection()
    monkeypatch.setattr(service_module, "get_mongo_database", lambda: {"conversations": collection})

    service_module.add_client_conversation(
        conversation_uuid="conv-client-1",
        user_id=2,
    )

    assert collection.last_inserted is not None
    assert collection.last_inserted["conversation_type"] == "client"
    assert collection.last_inserted["is_deleted"] == 0


def test_save_conversation_title_updates_title(monkeypatch):
    collection = _DummyCollection()
    monkeypatch.setattr(service_module, "get_mongo_database", lambda: {"conversations": collection})

    service_module.save_conversation_title(
        conversation_uuid="conv-1",
        title="新标题",
    )

    assert collection.last_update_query == {
        "uuid": "conv-1",
        "is_deleted": {"$ne": 1},
    }
    assert collection.last_update_doc is not None
    assert collection.last_update_doc["$set"]["title"] == "新标题"
    assert "update_time" in collection.last_update_doc["$set"]


def test_update_admin_conversation_title_scopes_by_user_and_type(monkeypatch):
    collection = _DummyCollection()
    monkeypatch.setattr(service_module, "get_mongo_database", lambda: {"conversations": collection})

    result = service_module.update_admin_conversation_title(
        conversation_uuid="conv-1",
        user_id=2,
        title="更新标题",
    )

    assert result is True
    assert collection.last_update_query == {
        "uuid": "conv-1",
        "conversation_type": "admin",
        "user_id": Int64(2),
        "is_deleted": {"$ne": 1},
    }
    assert collection.last_update_doc is not None
    assert collection.last_update_doc["$set"]["title"] == "更新标题"
    assert "update_time" in collection.last_update_doc["$set"]


def test_update_client_conversation_title_scopes_by_user_and_type(monkeypatch):
    collection = _DummyCollection()
    monkeypatch.setattr(service_module, "get_mongo_database", lambda: {"conversations": collection})

    result = service_module.update_client_conversation_title(
        conversation_uuid="client-conv-1",
        user_id=6,
        title="客户端标题",
    )

    assert result is True
    assert collection.last_update_query == {
        "uuid": "client-conv-1",
        "conversation_type": "client",
        "user_id": Int64(6),
        "is_deleted": {"$ne": 1},
    }
    assert collection.last_update_doc is not None
    assert collection.last_update_doc["$set"]["title"] == "客户端标题"
    assert "update_time" in collection.last_update_doc["$set"]


def test_delete_admin_conversation_scopes_by_user_and_type(monkeypatch):
    collection = _DummyCollection()
    monkeypatch.setattr(service_module, "get_mongo_database", lambda: {"conversations": collection})

    result = service_module.delete_admin_conversation(
        conversation_uuid="conv-2",
        user_id=3,
    )

    assert result is True
    assert collection.last_update_query == {
        "uuid": "conv-2",
        "conversation_type": "admin",
        "user_id": Int64(3),
        "is_deleted": {"$ne": 1},
    }
    assert collection.last_update_doc is not None
    assert collection.last_update_doc["$set"]["is_deleted"] == 1
    assert "update_time" in collection.last_update_doc["$set"]


def test_delete_client_conversation_scopes_by_user_and_type(monkeypatch):
    collection = _DummyCollection()
    monkeypatch.setattr(service_module, "get_mongo_database", lambda: {"conversations": collection})

    result = service_module.delete_client_conversation(
        conversation_uuid="client-conv-2",
        user_id=7,
    )

    assert result is True
    assert collection.last_update_query == {
        "uuid": "client-conv-2",
        "conversation_type": "client",
        "user_id": Int64(7),
        "is_deleted": {"$ne": 1},
    }
    assert collection.last_update_doc is not None
    assert collection.last_update_doc["$set"]["is_deleted"] == 1
    assert "update_time" in collection.last_update_doc["$set"]


def test_list_admin_conversations_returns_uuid_and_title(monkeypatch):
    collection = _DummyCollection()
    collection.find_result = [
        {"uuid": "conv-1", "title": "会话一"},
        {"uuid": "conv-2", "title": ""},
        {"uuid": "", "title": "应忽略"},
    ]
    monkeypatch.setattr(service_module, "get_mongo_database", lambda: {"conversations": collection})

    rows, total = service_module.list_admin_conversations(
        user_id=1,
        page_num=1,
        page_size=2,
    )

    assert collection.last_count_query == {
        "conversation_type": "admin",
        "user_id": Int64(1),
        "is_deleted": {"$ne": 1},
    }
    assert collection.last_find_query == {
        "conversation_type": "admin",
        "user_id": Int64(1),
        "is_deleted": {"$ne": 1},
    }
    assert collection.last_find_projection == {
        "_id": 0,
        "uuid": 1,
        "title": 1,
    }
    assert collection.last_cursor is not None
    assert collection.last_cursor.sort_args == ("update_time", -1)
    assert collection.last_cursor.skip_value == 0
    assert collection.last_cursor.limit_value == 2
    assert total == 3
    assert all(isinstance(item, ConversationListItem) for item in rows)
    assert [item.model_dump() for item in rows] == [
        {"conversation_uuid": "conv-1", "title": "会话一"},
        {"conversation_uuid": "conv-2", "title": "新聊天"},
    ]


def test_list_client_conversations_returns_uuid_and_title(monkeypatch):
    collection = _DummyCollection()
    collection.find_result = [
        {"uuid": "client-conv-1", "title": "客户端会话"},
        {"uuid": "client-conv-2", "title": ""},
    ]
    monkeypatch.setattr(service_module, "get_mongo_database", lambda: {"conversations": collection})

    rows, total = service_module.list_client_conversations(
        user_id=8,
        page_num=1,
        page_size=20,
    )

    assert collection.last_count_query == {
        "conversation_type": "client",
        "user_id": Int64(8),
        "is_deleted": {"$ne": 1},
    }
    assert collection.last_find_query == {
        "conversation_type": "client",
        "user_id": Int64(8),
        "is_deleted": {"$ne": 1},
    }
    assert total == 2
    assert [item.model_dump() for item in rows] == [
        {"conversation_uuid": "client-conv-1", "title": "客户端会话"},
        {"conversation_uuid": "client-conv-2", "title": "新聊天"},
    ]


def test_delete_admin_conversation_returns_false_when_not_matched(monkeypatch):
    collection = _DummyCollection()
    collection.update_matched_count = 0
    monkeypatch.setattr(service_module, "get_mongo_database", lambda: {"conversations": collection})

    result = service_module.delete_admin_conversation(
        conversation_uuid="conv-3",
        user_id=4,
    )

    assert result is False


def test_update_client_conversation_title_returns_false_when_not_matched(monkeypatch):
    collection = _DummyCollection()
    collection.update_matched_count = 0
    monkeypatch.setattr(service_module, "get_mongo_database", lambda: {"conversations": collection})

    result = service_module.update_client_conversation_title(
        conversation_uuid="client-conv-3",
        user_id=10,
        title="未命中标题",
    )

    assert result is False


def test_delete_client_conversation_returns_false_when_not_matched(monkeypatch):
    collection = _DummyCollection()
    collection.update_matched_count = 0
    monkeypatch.setattr(service_module, "get_mongo_database", lambda: {"conversations": collection})

    result = service_module.delete_client_conversation(
        conversation_uuid="client-conv-4",
        user_id=11,
    )

    assert result is False
