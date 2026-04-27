import datetime

import pytest
from bson import ObjectId
from pydantic import ValidationError

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.schemas.document.message import MessageDocument, MessageRole, MessageStatus
from app.services import message_service as service_module


class _DummyInsertResult:
    def __init__(self, inserted_id: str):
        self.inserted_id = inserted_id


class _DummyUpdateResult:
    def __init__(self, matched_count: int = 1, modified_count: int = 1):
        self.matched_count = matched_count
        self.modified_count = modified_count


class _DummyCursor:
    def __init__(self, rows: list[dict]):
        self._rows = list(rows)
        self.sort_field: str | None = None
        self.sort_direction: int | None = None
        self.skip_value: int = 0
        self.limit_value: int | None = None

    def sort(self, field: str, direction: int):
        self.sort_field = field
        self.sort_direction = direction
        return self

    def limit(self, value: int):
        self.limit_value = value
        return self

    def skip(self, value: int):
        self.skip_value = value
        return self

    def __iter__(self):
        rows = list(self._rows)
        if self.sort_field is not None:
            rows.sort(
                key=lambda item: item.get(self.sort_field),
                reverse=self.sort_direction == -1,
            )
        if self.skip_value:
            rows = rows[self.skip_value:]
        if self.limit_value is not None:
            rows = rows[: self.limit_value]
        return iter(rows)


class _DummyCollection:
    def __init__(self):
        self.last_inserted: dict | None = None
        self.last_find_query: dict | None = None
        self.last_find_one_query: dict | None = None
        self.last_count_query: dict | None = None
        self.last_update_query: dict | None = None
        self.last_update_document: dict | None = None
        self.update_calls: list[tuple[dict, dict]] = []
        self.find_rows: list[dict] = []
        self.find_one_result: dict | None = None
        self.count_documents_result: int = 0
        self.created_indexes: list[dict] = []

    def insert_one(self, document: dict) -> _DummyInsertResult:
        self.last_inserted = document
        return _DummyInsertResult("507f1f77bcf86cd799439011")

    def find(self, query: dict) -> _DummyCursor:
        self.last_find_query = query
        return _DummyCursor(self.find_rows)

    def find_one(self, query: dict) -> dict | None:
        self.last_find_one_query = query
        return self.find_one_result

    def count_documents(self, query: dict) -> int:
        self.last_count_query = query
        return self.count_documents_result

    def update_one(self, query: dict, update: dict) -> _DummyUpdateResult:
        self.last_update_query = query
        self.last_update_document = update
        self.update_calls.append((query, update))
        return _DummyUpdateResult()

    def create_index(self, keys, **kwargs) -> str:
        self.created_indexes.append({"keys": list(keys), "kwargs": kwargs})
        return kwargs.get("name", "idx")


@pytest.fixture(autouse=True)
def _clear_message_service_index_cache():
    service_module._ensure_message_indexes.cache_clear()
    yield
    service_module._ensure_message_indexes.cache_clear()


def test_add_message_inserts_expected_document(monkeypatch):
    """验证 add_message：user 消息入库成功。"""

    collection = _DummyCollection()
    monkeypatch.setattr(
        service_module,
        "get_mongo_database",
        lambda: {"messages": collection},
    )
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "messages")

    result = service_module.add_message(
        conversation_id="507f1f77bcf86cd799439011",
        role=MessageRole.USER,
        content="你好",
        message_uuid="msg-uuid-1",
    )

    assert result == "507f1f77bcf86cd799439011"
    assert collection.last_inserted is not None
    assert collection.last_inserted["uuid"] == "msg-uuid-1"
    assert collection.last_inserted["role"] == MessageRole.USER
    assert collection.last_inserted["status"] == MessageStatus.SUCCESS
    assert collection.last_inserted["content"] == "你好"
    assert collection.last_inserted["conversation_id"] == ObjectId("507f1f77bcf86cd799439011")
    assert "card_uuids" not in collection.last_inserted
    assert collection.last_inserted["history_hidden"] is False
    assert isinstance(collection.last_inserted["created_at"], datetime.datetime)
    assert isinstance(collection.last_inserted["updated_at"], datetime.datetime)
    assert len(collection.created_indexes) == 3


def test_add_message_persists_thinking_for_assistant(monkeypatch):
    """验证 assistant 消息落库会保存 thinking 字段。"""

    collection = _DummyCollection()
    monkeypatch.setattr(
        service_module,
        "get_mongo_database",
        lambda: {"messages": collection},
    )
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "messages")

    service_module.add_message(
        conversation_id="507f1f77bcf86cd799439011",
        role=MessageRole.AI,
        content="助手回复",
        thinking="这是完整思考内容",
    )

    assert collection.last_inserted is not None
    assert collection.last_inserted["thinking"] == "这是完整思考内容"


def test_add_message_user_ignores_thinking(monkeypatch):
    """验证 user 消息不会保存 thinking 字段。"""

    collection = _DummyCollection()
    monkeypatch.setattr(
        service_module,
        "get_mongo_database",
        lambda: {"messages": collection},
    )
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "messages")

    service_module.add_message(
        conversation_id="507f1f77bcf86cd799439011",
        role=MessageRole.USER,
        content="用户提问",
        thinking="不应保存",
    )

    assert collection.last_inserted is not None
    assert "thinking" not in collection.last_inserted


def test_add_message_persists_cards_for_assistant_with_empty_content(monkeypatch):
    """验证 assistant 消息可在纯卡片场景下落库。"""

    collection = _DummyCollection()
    monkeypatch.setattr(
        service_module,
        "get_mongo_database",
        lambda: {"messages": collection},
    )
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "messages")

    service_module.add_message(
        conversation_id="507f1f77bcf86cd799439011",
        role=MessageRole.AI,
        content="   ",
        cards=[
            {
                "id": "card-1",
                "type": "product-card",
                "data": {
                    "title": "为您推荐以下商品",
                    "products": [],
                },
            }
        ],
    )

    assert collection.last_inserted is not None
    assert collection.last_inserted["content"] == ""
    assert collection.last_inserted["cards"] == [
        {
            "id": "card-1",
            "type": "product-card",
            "data": {
                "title": "为您推荐以下商品",
                "products": [],
            },
        }
    ]
    assert collection.last_inserted["card_uuids"] == ["card-1"]
    assert collection.last_inserted["history_hidden"] is False


def test_add_message_rejects_empty_assistant_content_without_cards(monkeypatch):
    """验证 assistant 消息内容和卡片不能同时为空。"""

    collection = _DummyCollection()
    monkeypatch.setattr(
        service_module,
        "get_mongo_database",
        lambda: {"messages": collection},
    )
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "messages")

    with pytest.raises(ValidationError):
        service_module.add_message(
            conversation_id="507f1f77bcf86cd799439011",
            role=MessageRole.AI,
            content="   ",
        )


def test_add_message_user_ignores_cards(monkeypatch):
    """验证 user 消息不会保存 cards 字段。"""

    collection = _DummyCollection()
    monkeypatch.setattr(
        service_module,
        "get_mongo_database",
        lambda: {"messages": collection},
    )
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "messages")

    service_module.add_message(
        conversation_id="507f1f77bcf86cd799439011",
        role=MessageRole.USER,
        content="用户提问",
        cards=[
            {
                "id": "card-1",
                "type": "product-card",
                "data": {"title": "ignored"},
            }
        ],
    )

    assert collection.last_inserted is not None
    assert "cards" not in collection.last_inserted


def test_add_message_rejects_invalid_conversation_id(monkeypatch):
    """验证 add_message：非法 conversation_id 会返回 BAD_REQUEST。"""

    collection = _DummyCollection()
    monkeypatch.setattr(
        service_module,
        "get_mongo_database",
        lambda: {"messages": collection},
    )
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "messages")

    with pytest.raises(ServiceException) as exc_info:
        service_module.add_message(
            conversation_id="invalid-object-id",
            role=MessageRole.AI,
            content="hello",
        )

    assert exc_info.value.code == ResponseCode.BAD_REQUEST


def test_get_message_by_uuid_returns_typed_model(monkeypatch):
    """验证 get_message_by_uuid：查询结果会被转换为 MessageDocument。"""

    collection = _DummyCollection()
    collection.find_one_result = {
        "_id": ObjectId("507f1f77bcf86cd799439012"),
        "uuid": "msg-1",
        "conversation_id": ObjectId("507f1f77bcf86cd799439011"),
        "role": "ai",
        "content": "hello",
        "thinking": "这是思考内容",
        "cards": [
            {
                "id": "card-1",
                "type": "product-card",
                "data": {
                    "title": "为您推荐以下商品",
                    "products": [],
                },
            }
        ],
        "created_at": datetime.datetime(2026, 1, 1, 10, 0, 0),
        "updated_at": datetime.datetime(2026, 1, 1, 10, 0, 0),
    }
    monkeypatch.setattr(
        service_module,
        "get_mongo_database",
        lambda: {"messages": collection},
    )
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "messages")

    result = service_module.get_message_by_uuid(message_uuid="msg-1")

    assert isinstance(result, MessageDocument)
    assert result is not None
    assert result.id == "507f1f77bcf86cd799439012"
    assert result.conversation_id == "507f1f77bcf86cd799439011"
    assert result.role == MessageRole.AI
    assert result.thinking == "这是思考内容"
    assert result.cards is not None
    assert result.cards[0].model_dump() == {
        "id": "card-1",
        "type": "product-card",
        "data": {
            "title": "为您推荐以下商品",
            "products": [],
        },
    }


def test_list_messages_returns_typed_models(monkeypatch):
    """验证 list_messages：按会话查询并返回类型化消息列表。"""

    collection = _DummyCollection()
    collection.find_rows = [
        {
            "_id": ObjectId("507f1f77bcf86cd799439022"),
            "uuid": "msg-2",
            "conversation_id": ObjectId("507f1f77bcf86cd799439011"),
            "role": "ai",
            "content": "b",
            "thinking": "AI思考",
            "cards": [
                {
                    "id": "card-2",
                    "type": "product-card",
                    "data": {
                        "title": "为您推荐以下商品",
                        "products": [{"id": "1001"}],
                    },
                }
            ],
            "created_at": datetime.datetime(2026, 1, 1, 10, 0, 2),
            "updated_at": datetime.datetime(2026, 1, 1, 10, 0, 2),
        },
        {
            "_id": ObjectId("507f1f77bcf86cd799439021"),
            "uuid": "msg-1",
            "conversation_id": ObjectId("507f1f77bcf86cd799439011"),
            "role": "user",
            "content": "a",
            "created_at": datetime.datetime(2026, 1, 1, 10, 0, 1),
            "updated_at": datetime.datetime(2026, 1, 1, 10, 0, 1),
        },
    ]
    monkeypatch.setattr(
        service_module,
        "get_mongo_database",
        lambda: {"messages": collection},
    )
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "messages")

    result = service_module.list_messages(
        conversation_id="507f1f77bcf86cd799439011",
        limit=2,
        skip=0,
        ascending=True,
    )

    assert collection.last_find_query == {
        "conversation_id": ObjectId("507f1f77bcf86cd799439011"),
    }
    assert len(result) == 2
    assert all(isinstance(item, MessageDocument) for item in result)
    assert result[0].content == "a"
    assert result[0].thinking is None
    assert result[1].content == "b"
    assert result[1].thinking == "AI思考"
    assert result[1].cards is not None
    assert result[1].cards[0].id == "card-2"


def test_list_messages_supports_skip_with_descending_order(monkeypatch):
    """验证 list_messages：支持 skip 分页，且可按 created_at 倒序读取。"""

    collection = _DummyCollection()
    collection.find_rows = [
        {
            "_id": ObjectId("507f1f77bcf86cd799439020"),
            "uuid": "msg-1",
            "conversation_id": ObjectId("507f1f77bcf86cd799439011"),
            "role": "ai",
            "content": "a",
            "created_at": datetime.datetime(2026, 1, 1, 10, 0, 1),
            "updated_at": datetime.datetime(2026, 1, 1, 10, 0, 1),
        },
        {
            "_id": ObjectId("507f1f77bcf86cd799439021"),
            "uuid": "msg-2",
            "conversation_id": ObjectId("507f1f77bcf86cd799439011"),
            "role": "ai",
            "content": "b",
            "created_at": datetime.datetime(2026, 1, 1, 10, 0, 2),
            "updated_at": datetime.datetime(2026, 1, 1, 10, 0, 2),
        },
        {
            "_id": ObjectId("507f1f77bcf86cd799439022"),
            "uuid": "msg-3",
            "conversation_id": ObjectId("507f1f77bcf86cd799439011"),
            "role": "ai",
            "content": "c",
            "created_at": datetime.datetime(2026, 1, 1, 10, 0, 3),
            "updated_at": datetime.datetime(2026, 1, 1, 10, 0, 3),
        },
    ]
    monkeypatch.setattr(
        service_module,
        "get_mongo_database",
        lambda: {"messages": collection},
    )
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "messages")

    result = service_module.list_messages(
        conversation_id="507f1f77bcf86cd799439011",
        limit=1,
        skip=1,
        ascending=False,
    )

    assert len(result) == 1
    assert result[0].uuid == "msg-2"


def test_list_messages_can_filter_visible_history(monkeypatch):
    collection = _DummyCollection()
    monkeypatch.setattr(
        service_module,
        "get_mongo_database",
        lambda: {"messages": collection},
    )
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "messages")

    service_module.list_messages(
        conversation_id="507f1f77bcf86cd799439011",
        limit=10,
        history_hidden=False,
    )

    assert collection.last_find_query == {
        "conversation_id": ObjectId("507f1f77bcf86cd799439011"),
        "history_hidden": {"$ne": True},
    }


def test_list_messages_can_filter_statuses(monkeypatch):
    """验证普通消息查询支持按状态白名单过滤。"""

    collection = _DummyCollection()
    monkeypatch.setattr(
        service_module,
        "get_mongo_database",
        lambda: {"messages": collection},
    )
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "messages")

    service_module.list_messages(
        conversation_id="507f1f77bcf86cd799439011",
        limit=10,
        statuses=[MessageStatus.STREAMING, MessageStatus.CANCELLED],
    )

    assert collection.last_find_query == {
        "conversation_id": ObjectId("507f1f77bcf86cd799439011"),
        "status": {"$in": ["streaming", "cancelled"]},
    }


def test_update_assistant_message_updates_existing_placeholder(monkeypatch):
    """验证 AI 占位消息可被更新为新的状态与内容。"""

    collection = _DummyCollection()
    monkeypatch.setattr(
        service_module,
        "get_mongo_database",
        lambda: {"messages": collection},
    )
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "messages")

    updated = service_module.update_assistant_message(
        conversation_id="507f1f77bcf86cd799439011",
        message_uuid="msg-1",
        status=MessageStatus.CANCELLED,
        content="部分回答",
        thinking="部分思考",
    )

    assert updated is True
    assert collection.last_update_query == {
        "uuid": "msg-1",
        "conversation_id": ObjectId("507f1f77bcf86cd799439011"),
        "role": MessageRole.AI,
    }
    assert collection.last_update_document is not None
    assert collection.last_update_document["$set"]["status"] == MessageStatus.CANCELLED
    assert collection.last_update_document["$set"]["content"] == "部分回答"
    assert collection.last_update_document["$set"]["thinking"] == "部分思考"


def test_count_summarizable_messages_applies_summary_filter(monkeypatch):
    """测试目的：统计摘要消息时命中 user/ai 的 success 与 waiting_input；预期结果：查询条件包含 role/status/after 游标。"""

    collection = _DummyCollection()
    collection.count_documents_result = 7
    monkeypatch.setattr(service_module, "get_mongo_database", lambda: {"messages": collection})
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "messages")

    count = service_module.count_summarizable_messages(
        conversation_id="507f1f77bcf86cd799439011",
        after_message_id="507f1f77bcf86cd799439010",
    )

    assert count == 7
    assert collection.last_count_query is not None
    assert collection.last_count_query["conversation_id"] == ObjectId("507f1f77bcf86cd799439011")
    assert collection.last_count_query["status"] == {
        "$in": [
            MessageStatus.SUCCESS.value,
            MessageStatus.WAITING_INPUT.value,
        ]
    }
    assert collection.last_count_query["role"] == {"$in": [MessageRole.USER.value, MessageRole.AI.value]}
    assert collection.last_count_query["_id"] == {"$gt": ObjectId("507f1f77bcf86cd799439010")}


def test_count_messages_counts_only_current_conversation(monkeypatch):
    """测试目的：普通消息总数统计仅按 conversation_id 过滤；预期结果：查询条件不包含其他过滤项。"""

    collection = _DummyCollection()
    collection.count_documents_result = 9
    monkeypatch.setattr(service_module, "get_mongo_database", lambda: {"messages": collection})
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "messages")

    count = service_module.count_messages(
        conversation_id="507f1f77bcf86cd799439011",
    )

    assert count == 9
    assert collection.last_count_query == {
        "conversation_id": ObjectId("507f1f77bcf86cd799439011"),
    }


def test_count_messages_can_filter_visible_history(monkeypatch):
    collection = _DummyCollection()
    collection.count_documents_result = 3
    monkeypatch.setattr(service_module, "get_mongo_database", lambda: {"messages": collection})
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "messages")

    count = service_module.count_messages(
        conversation_id="507f1f77bcf86cd799439011",
        history_hidden=False,
    )

    assert count == 3
    assert collection.last_count_query == {
        "conversation_id": ObjectId("507f1f77bcf86cd799439011"),
        "history_hidden": {"$ne": True},
    }


def test_list_latest_summarizable_messages_returns_ascending(monkeypatch):
    """测试目的：读取最新可总结消息后返回正序；预期结果：输出顺序为旧到新。"""

    collection = _DummyCollection()
    collection.find_rows = [
        {
            "_id": ObjectId("507f1f77bcf86cd799439022"),
            "uuid": "msg-2",
            "conversation_id": ObjectId("507f1f77bcf86cd799439011"),
            "role": "ai",
            "status": "success",
            "content": "B",
            "created_at": datetime.datetime(2026, 1, 1, 10, 0, 2),
            "updated_at": datetime.datetime(2026, 1, 1, 10, 0, 2),
        },
        {
            "_id": ObjectId("507f1f77bcf86cd799439021"),
            "uuid": "msg-1",
            "conversation_id": ObjectId("507f1f77bcf86cd799439011"),
            "role": "user",
            "status": "success",
            "content": "A",
            "created_at": datetime.datetime(2026, 1, 1, 10, 0, 1),
            "updated_at": datetime.datetime(2026, 1, 1, 10, 0, 1),
        },
    ]
    monkeypatch.setattr(service_module, "get_mongo_database", lambda: {"messages": collection})
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "messages")

    documents = service_module.list_latest_summarizable_messages(
        conversation_id="507f1f77bcf86cd799439011",
        limit=2,
        after_message_id=None,
    )

    assert [item.uuid for item in documents] == ["msg-1", "msg-2"]


def test_hide_message_card_updates_hidden_card_uuids_and_history_hidden(monkeypatch):
    collection = _DummyCollection()
    collection.find_one_result = {
        "_id": ObjectId("507f1f77bcf86cd799439099"),
        "uuid": "msg-1",
        "conversation_id": ObjectId("507f1f77bcf86cd799439011"),
        "role": "ai",
        "status": "success",
        "content": "",
        "cards": [
            {
                "id": "card-1",
                "type": "selection-card",
                "data": {"title": "请选择", "options": ["A", "B"]},
            }
        ],
        "created_at": datetime.datetime(2026, 1, 1, 10, 0, 1),
        "updated_at": datetime.datetime(2026, 1, 1, 10, 0, 1),
    }
    monkeypatch.setattr(service_module, "get_mongo_database", lambda: {"messages": collection})
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "messages")

    service_module.hide_message_card(
        conversation_id="507f1f77bcf86cd799439011",
        message_uuid="msg-1",
        card_uuid="card-1",
    )

    assert collection.last_find_one_query == {
        "conversation_id": ObjectId("507f1f77bcf86cd799439011"),
        "uuid": "msg-1",
        "$or": [
            {"card_uuids": "card-1"},
            {"cards.id": "card-1"},
            {"cards.card_uuid": "card-1"},
        ],
    }
    assert collection.last_update_query == {"_id": ObjectId("507f1f77bcf86cd799439099")}
    assert collection.last_update_document is not None
    assert collection.last_update_document["$set"]["card_uuids"] == ["card-1"]
    assert collection.last_update_document["$set"]["hidden_card_uuids"] == ["card-1"]
    assert collection.last_update_document["$set"]["history_hidden"] is True


def test_hide_message_card_is_idempotent_for_repeated_click(monkeypatch):
    collection = _DummyCollection()
    collection.find_one_result = {
        "_id": ObjectId("507f1f77bcf86cd799439099"),
        "uuid": "msg-1",
        "conversation_id": ObjectId("507f1f77bcf86cd799439011"),
        "role": "ai",
        "status": "success",
        "content": "还有文本",
        "hidden_card_uuids": ["card-1"],
        "card_uuids": ["card-1", "card-2"],
        "cards": [
            {
                "id": "card-1",
                "type": "consent-card",
                "data": {"title": "是否同意"},
            },
            {
                "id": "card-2",
                "type": "selection-card",
                "data": {"title": "请选择", "options": ["A", "B"]},
            },
        ],
        "created_at": datetime.datetime(2026, 1, 1, 10, 0, 1),
        "updated_at": datetime.datetime(2026, 1, 1, 10, 0, 1),
    }
    monkeypatch.setattr(service_module, "get_mongo_database", lambda: {"messages": collection})
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "messages")

    service_module.hide_message_card(
        conversation_id="507f1f77bcf86cd799439011",
        message_uuid="msg-1",
        card_uuid="card-1",
    )

    assert collection.last_update_document is not None
    assert collection.last_update_document["$set"]["hidden_card_uuids"] == ["card-1"]
    assert collection.last_update_document["$set"]["history_hidden"] is False


def test_hide_message_card_rejects_invalid_target(monkeypatch):
    collection = _DummyCollection()
    collection.find_one_result = None
    monkeypatch.setattr(service_module, "get_mongo_database", lambda: {"messages": collection})
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "messages")

    with pytest.raises(ServiceException) as exc_info:
        service_module.hide_message_card(
            conversation_id="507f1f77bcf86cd799439011",
            message_uuid="msg-1",
            card_uuid="card-1",
        )

    assert exc_info.value.code == ResponseCode.BAD_REQUEST


def test_hide_visible_cards_in_conversation_hides_all_visible_cards(monkeypatch):
    collection = _DummyCollection()
    collection.find_rows = [
        {
            "_id": ObjectId("507f1f77bcf86cd799439099"),
            "uuid": "msg-card-only",
            "conversation_id": ObjectId("507f1f77bcf86cd799439011"),
            "role": "ai",
            "status": "success",
            "content": "",
            "cards": [
                {
                    "id": "card-1",
                    "type": "selection-card",
                    "data": {"title": "请选择", "options": ["A", "B"]},
                },
                {
                    "id": "card-2",
                    "type": "consent-card",
                    "data": {"title": "是否同意"},
                },
            ],
            "created_at": datetime.datetime(2026, 1, 1, 10, 0, 1),
            "updated_at": datetime.datetime(2026, 1, 1, 10, 0, 1),
        },
        {
            "_id": ObjectId("507f1f77bcf86cd799439100"),
            "uuid": "msg-with-content",
            "conversation_id": ObjectId("507f1f77bcf86cd799439011"),
            "role": "ai",
            "status": "success",
            "content": "这是带正文的推荐说明",
            "hidden_card_uuids": ["card-3"],
            "cards": [
                {
                    "id": "card-3",
                    "type": "product-card",
                    "data": {"title": "旧卡片"},
                },
                {
                    "id": "card-4",
                    "type": "product-purchase-card",
                    "data": {"title": "新卡片"},
                },
            ],
            "created_at": datetime.datetime(2026, 1, 1, 10, 0, 2),
            "updated_at": datetime.datetime(2026, 1, 1, 10, 0, 2),
        },
    ]
    monkeypatch.setattr(service_module, "get_mongo_database", lambda: {"messages": collection})
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "messages")

    updated_count = service_module.hide_visible_cards_in_conversation(
        conversation_id="507f1f77bcf86cd799439011",
    )

    assert updated_count == 2
    assert collection.last_find_query == {
        "conversation_id": ObjectId("507f1f77bcf86cd799439011"),
        "history_hidden": {"$ne": True},
        "$or": [
            {"card_uuids.0": {"$exists": True}},
            {"cards.0": {"$exists": True}},
        ],
    }
    assert collection.update_calls[0][1]["$set"]["card_uuids"] == ["card-1", "card-2"]
    assert collection.update_calls[0][1]["$set"]["hidden_card_uuids"] == ["card-1", "card-2"]
    assert collection.update_calls[0][1]["$set"]["history_hidden"] is True
    assert collection.update_calls[1][1]["$set"]["card_uuids"] == ["card-3", "card-4"]
    assert collection.update_calls[1][1]["$set"]["hidden_card_uuids"] == ["card-3", "card-4"]
    assert collection.update_calls[1][1]["$set"]["history_hidden"] is False


def test_hide_visible_cards_in_conversation_is_idempotent_when_all_cards_hidden(monkeypatch):
    collection = _DummyCollection()
    collection.find_rows = [
        {
            "_id": ObjectId("507f1f77bcf86cd799439099"),
            "uuid": "msg-1",
            "conversation_id": ObjectId("507f1f77bcf86cd799439011"),
            "role": "ai",
            "status": "success",
            "content": "",
            "card_uuids": ["card-1"],
            "hidden_card_uuids": ["card-1"],
            "history_hidden": True,
            "cards": [
                {
                    "id": "card-1",
                    "type": "selection-card",
                    "data": {"title": "请选择", "options": ["A", "B"]},
                }
            ],
            "created_at": datetime.datetime(2026, 1, 1, 10, 0, 1),
            "updated_at": datetime.datetime(2026, 1, 1, 10, 0, 1),
        }
    ]
    monkeypatch.setattr(service_module, "get_mongo_database", lambda: {"messages": collection})
    monkeypatch.setattr(service_module, "_resolve_collection_name", lambda: "messages")

    updated_count = service_module.hide_visible_cards_in_conversation(
        conversation_id="507f1f77bcf86cd799439011",
    )

    assert updated_count == 0
    assert collection.update_calls == []
