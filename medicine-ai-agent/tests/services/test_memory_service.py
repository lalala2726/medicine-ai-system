from types import SimpleNamespace

import pytest
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.schemas.document.message import MessageRole, MessageStatus
from app.schemas.memory import Memory
from app.services import memory_service as service_module


def test_load_memory_by_window_returns_ordered_memory(monkeypatch):
    """测试目的：窗口模式按旧到新返回消息；预期结果：输出为 Human -> AI 顺序且不含系统消息。"""

    captured: dict = {}

    monkeypatch.setattr(
        service_module,
        "get_conversation",
        lambda *, conversation_uuid, user_id: (
            captured.update(
                {
                    "conversation_uuid": conversation_uuid,
                    "user_id": user_id,
                }
            ),
            SimpleNamespace(id="conv-object-id"),
        )[-1],
    )
    monkeypatch.setattr(
        service_module,
        "list_messages",
        lambda *, conversation_id, limit, ascending, history_hidden=None, statuses=None: (
            captured.update(
                {
                    "conversation_id": conversation_id,
                    "limit": limit,
                    "ascending": ascending,
                    "history_hidden": history_hidden,
                    "statuses": statuses,
                }
            ),
            [
                SimpleNamespace(role=MessageRole.AI, content="A2"),
                SimpleNamespace(role=MessageRole.USER, content="Q1"),
            ],
        )[-1],
    )

    result = service_module.load_memory_by_window(
        conversation_uuid="conv-1",
        user_id=100,
        limit=2,
    )

    assert isinstance(result, Memory)
    assert captured == {
        "conversation_uuid": "conv-1",
        "user_id": 100,
        "conversation_id": "conv-object-id",
        "limit": 2,
        "ascending": False,
        "history_hidden": None,
        "statuses": [MessageStatus.SUCCESS, MessageStatus.WAITING_INPUT],
    }
    assert [message.type for message in result.messages] == ["human", "ai"]
    assert [message.content for message in result.messages] == ["Q1", "A2"]
    assert all(message.type != "system" for message in result.messages)


def test_load_memory_by_window_raises_not_found_when_conversation_missing(monkeypatch):
    """测试目的：会话不存在时返回业务异常；预期结果：抛出 NOT_FOUND。"""

    monkeypatch.setattr(service_module, "get_conversation", lambda **_kwargs: None)

    with pytest.raises(ServiceException) as exc_info:
        service_module.load_memory_by_window(
            conversation_uuid="conv-missing",
            user_id=100,
            limit=10,
        )

    assert exc_info.value.code == ResponseCode.NOT_FOUND


def test_load_memory_by_window_raises_database_error_when_conversation_id_missing(monkeypatch):
    """测试目的：会话主键缺失时兜底；预期结果：抛出 DATABASE_ERROR。"""

    monkeypatch.setattr(
        service_module,
        "get_conversation",
        lambda **_kwargs: SimpleNamespace(id=None),
    )

    with pytest.raises(ServiceException) as exc_info:
        service_module.load_memory_by_window(
            conversation_uuid="conv-1",
            user_id=100,
            limit=10,
        )

    assert exc_info.value.code == ResponseCode.DATABASE_ERROR


def test_load_memory_uses_env_mode_and_dispatches_window(monkeypatch):
    """测试目的：load_memory 由环境模式驱动；预期结果：ASSISTANT_MEMORY_MODE=window 时调用窗口加载器。"""

    monkeypatch.setattr(
        service_module,
        "load_memory_by_window",
        lambda *, conversation_uuid, user_id, limit, include_history_hidden: Memory(messages=[]),
    )
    monkeypatch.setenv("ASSISTANT_MEMORY_MODE", "window")

    result = service_module.load_memory(
        memory_type="summary",
        conversation_uuid="conv-1",
        user_id=100,
        limit=20,
    )

    assert isinstance(result, Memory)


def test_load_memory_by_summary_returns_summary_and_tail(monkeypatch):
    """测试目的：summary 模式返回摘要+尾部窗口；预期结果：首条为 SystemMessage，后续为尾部原文消息。"""

    monkeypatch.setattr(
        service_module,
        "get_conversation",
        lambda **_kwargs: SimpleNamespace(id="507f1f77bcf86cd799439011"),
    )
    monkeypatch.setattr(
        service_module,
        "get_conversation_summary",
        lambda *, conversation_id: SimpleNamespace(
            summary_content="历史摘要",
            status="success",
        ),
    )
    monkeypatch.setattr(
        service_module,
        "list_summarizable_tail_messages",
        lambda *, conversation_id, limit, history_hidden=None: [
            SimpleNamespace(role=MessageRole.USER, content="用户问题"),
            SimpleNamespace(role=MessageRole.AI, content="助手回答"),
        ],
    )
    monkeypatch.setattr(service_module, "resolve_assistant_summary_tail_window", lambda: 20)

    memory = service_module.load_memory_by_summary(
        conversation_uuid="conv-1",
        user_id=100,
    )

    assert isinstance(memory, Memory)
    assert len(memory.messages) == 3
    assert isinstance(memory.messages[0], SystemMessage)
    assert isinstance(memory.messages[1], HumanMessage)
    assert isinstance(memory.messages[2], AIMessage)
    assert [item.content for item in memory.messages] == [
        "历史摘要",
        "用户问题",
        "助手回答",
    ]


def test_load_memory_by_window_can_exclude_hidden_history(monkeypatch):
    captured: dict = {}

    monkeypatch.setattr(
        service_module,
        "get_conversation",
        lambda **_kwargs: SimpleNamespace(id="conv-object-id"),
    )
    monkeypatch.setattr(
        service_module,
        "list_messages",
        lambda *, conversation_id, limit, ascending, history_hidden=None, statuses=None: (
            captured.update({"history_hidden": history_hidden, "statuses": statuses}),
            [],
        )[-1],
    )

    service_module.load_memory_by_window(
        conversation_uuid="conv-1",
        user_id=100,
        limit=2,
        include_history_hidden=False,
    )

    assert captured == {
        "history_hidden": False,
        "statuses": [MessageStatus.SUCCESS, MessageStatus.WAITING_INPUT],
    }


def test_resolve_assistant_summary_model_prefers_provider_specific_env(monkeypatch):
    """测试目的：摘要模型按厂商独立配置优先；预期结果：命中厂商专属变量时覆盖全局兜底。"""

    monkeypatch.setenv("LLM_PROVIDER", "openai")
    monkeypatch.setenv("ASSISTANT_SUMMARY_MODEL", "global-summary-model")
    monkeypatch.setenv("OPENAI_SUMMARY_MODEL", "openai-summary-model")

    resolved_model = service_module.resolve_assistant_summary_model()

    assert resolved_model == "openai-summary-model"


def test_resolve_assistant_summary_model_falls_back_to_global_env(monkeypatch):
    """测试目的：厂商专属未配置时回退全局；预期结果：返回 ASSISTANT_SUMMARY_MODEL。"""

    monkeypatch.setenv("LLM_PROVIDER", "volcengine")
    monkeypatch.delenv("VOLCENGINE_LLM_SUMMARY_MODEL", raising=False)
    monkeypatch.setenv("ASSISTANT_SUMMARY_MODEL", "global-summary-model")

    resolved_model = service_module.resolve_assistant_summary_model()

    assert resolved_model == "global-summary-model"
