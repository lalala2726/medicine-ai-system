import asyncio
import datetime
import json
from types import SimpleNamespace
from typing import Any

import pytest
from bson import ObjectId
from fastapi.responses import StreamingResponse
from langchain_core.messages import AIMessage, HumanMessage

from app.core.agent.agent_orchestrator import AssistantStreamConfig
from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.core.speech.tts.client import MessageTtsStream
from app.schemas.assistant_run import AssistantRunStatus
from app.schemas.base_request import PageRequest
from app.schemas.document.conversation import ConversationDocument, ConversationListItem, ConversationType
from app.schemas.document.message import MessageRole, MessageStatus
from app.schemas.sse_response import MessageType
from app.services import admin_assistant_service as service_module


class _DummyGraph:
    def __init__(self, final_state: dict | None = None):
        self.final_state = final_state or {}
        self.captured_config = None

    def invoke(self, _state: dict, config: dict | None = None) -> dict:
        self.captured_config = config
        return self.final_state


class _DummyAsyncTask:
    """
    功能描述:
        模拟 `asyncio.create_task` 返回对象，用于同步测试场景下捕获完成回调。

    参数说明:
        coroutine (Any): 被调度的协程对象。

    返回值:
        None

    异常说明:
        无。
    """

    def __init__(self, coroutine: Any) -> None:
        """
        功能描述:
            初始化模拟 task 并关闭未执行协程，避免测试中的未 await 告警。

        参数说明:
            coroutine (Any): 待调度协程。

        返回值:
            None

        异常说明:
            无。
        """

        self.coroutine = coroutine
        self.callbacks: list[Any] = []
        coroutine.close()

    def add_done_callback(self, callback: Any) -> None:
        """
        功能描述:
            记录 task 完成回调。

        参数说明:
            callback (Any): 完成回调函数。

        返回值:
            None

        异常说明:
            无。
        """

        self.callbacks.append(callback)


async def _collect_sse_payloads(response: StreamingResponse) -> list[dict[str, Any]]:
    """
    功能描述:
        消费 StreamingResponse 并提取其中的 SSE `data:` 负载。

    参数说明:
        response (StreamingResponse): 待消费的流式响应对象。

    返回值:
        list[dict[str, Any]]: 解析后的 JSON 负载列表。

    异常说明:
        json.JSONDecodeError: 当 SSE 载荷不是合法 JSON 时抛出。
    """

    payloads: list[dict[str, Any]] = []
    async for chunk in response.body_iterator:
        chunk_text = chunk.decode("utf-8") if isinstance(chunk, bytes) else str(chunk)
        for line in chunk_text.splitlines():
            if not line.startswith("data: "):
                continue
            payloads.append(json.loads(line[len("data: "):]))
    return payloads


def test_assistant_message_tts_stream_returns_chunked_audio_with_expected_headers(monkeypatch):
    captured: dict = {}
    monkeypatch.setattr(service_module, "get_user_id", lambda: 101)

    async def _audio_stream():
        yield b"chunk-a"
        yield b"chunk-b"

    monkeypatch.setattr(
        service_module,
        "build_message_tts_stream",
        lambda **kwargs: (
            captured.update(kwargs),
            MessageTtsStream(audio_stream=_audio_stream(), media_type="audio/mpeg"),
        )[-1],
    )

    response = service_module.assistant_message_tts_stream(
        message_uuid="msg-1",
    )

    assert isinstance(response, StreamingResponse)
    assert response.media_type == "audio/mpeg"
    assert response.headers["cache-control"] == "no-cache"
    assert response.headers["x-accel-buffering"] == "no"
    assert captured == {
        "message_uuid": "msg-1",
        "user_id": 101,
        "conversation_type": ConversationType.ADMIN,
    }


def test_invoke_admin_workflow_passes_langsmith_config(monkeypatch):
    """测试目标：校验 workflow invoke 透传 tracing 配置；成功标准：graph.invoke 收到 config。"""

    graph = _DummyGraph(final_state={"results": {"chat": {"content": "ok"}}})
    monkeypatch.setattr(service_module, "ADMIN_WORKFLOW", graph)
    monkeypatch.setattr(
        service_module,
        "build_langsmith_runnable_config",
        lambda **_kwargs: {
            "run_name": "admin_assistant_graph",
            "tags": ["admin-assistant", "langgraph"],
            "metadata": {"entrypoint": "api.admin_assistant.chat"},
        },
    )

    result = service_module._invoke_admin_workflow({"user_input": "hello"})

    assert result["results"]["chat"]["content"] == "ok"
    assert graph.captured_config is not None
    assert graph.captured_config["run_name"] == "admin_assistant_graph"


def test_chat_list_returns_current_user_conversations(monkeypatch):
    """测试目标：会话列表查询透传分页参数；成功标准：仅返回会话 UUID 与标题列表。"""

    captured: dict = {}

    monkeypatch.setattr(service_module, "get_user_id", lambda: 100)
    monkeypatch.setattr(
        service_module,
        "list_admin_conversations",
        lambda *, user_id, page_num, page_size: (
            captured.update(
                {
                    "user_id": user_id,
                    "page_num": page_num,
                    "page_size": page_size,
                }
            ),
            ([ConversationListItem(conversation_uuid="conv-1", title="标题1")], 1),
        )[-1],
    )

    rows, total = service_module.conversation_list(
        page_request=PageRequest(
            page_num=2,
            page_size=20,
        )
    )

    assert captured == {
        "user_id": 100,
        "page_num": 2,
        "page_size": 20,
    }
    assert len(rows) == 1
    assert rows[0].conversation_uuid == "conv-1"
    assert rows[0].title == "标题1"
    assert total == 1


def test_delete_conversation_calls_repository_with_current_user(monkeypatch):
    captured: dict = {}
    monkeypatch.setattr(service_module, "get_user_id", lambda: 101)
    monkeypatch.setattr(
        service_module,
        "delete_admin_conversation",
        lambda *, conversation_uuid, user_id: (
            captured.update({"conversation_uuid": conversation_uuid, "user_id": user_id}),
            True,
        )[-1],
    )

    service_module.delete_conversation(conversation_uuid="conv-1")

    assert captured == {"conversation_uuid": "conv-1", "user_id": 101}


def test_delete_conversation_raises_not_found_when_missing(monkeypatch):
    monkeypatch.setattr(service_module, "get_user_id", lambda: 101)
    monkeypatch.setattr(
        service_module,
        "delete_admin_conversation",
        lambda **_kwargs: False,
    )

    with pytest.raises(ServiceException) as exc_info:
        service_module.delete_conversation(conversation_uuid="missing-conv")
    assert exc_info.value.code == ResponseCode.NOT_FOUND.code


def test_update_conversation_title_returns_normalized_title(monkeypatch):
    captured: dict = {}
    monkeypatch.setattr(service_module, "get_user_id", lambda: 101)
    monkeypatch.setattr(
        service_module,
        "update_admin_conversation_title",
        lambda *, conversation_uuid, user_id, title: (
            captured.update(
                {
                    "conversation_uuid": conversation_uuid,
                    "user_id": user_id,
                    "title": title,
                }
            ),
            True,
        )[-1],
    )

    title = service_module.update_conversation_title(
        conversation_uuid="conv-1",
        title="  新标题  ",
    )

    assert title == "新标题"
    assert captured == {
        "conversation_uuid": "conv-1",
        "user_id": 101,
        "title": "新标题",
    }


def test_update_conversation_title_rejects_blank_title(monkeypatch):
    with pytest.raises(ServiceException) as exc_info:
        service_module.update_conversation_title(
            conversation_uuid="conv-1",
            title="   ",
        )
    assert exc_info.value.code == ResponseCode.BAD_REQUEST.code


def test_load_admin_conversation_returns_document_id(monkeypatch):
    monkeypatch.setattr(
        service_module,
        "get_admin_conversation",
        lambda *, conversation_uuid, user_id: ConversationDocument(
            _id=ObjectId("507f1f77bcf86cd799439041"),
            uuid=conversation_uuid,
            conversation_type=ConversationType.ADMIN,
            user_id=user_id,
            title="会话标题",
            create_time=datetime.datetime(2026, 1, 1, 10, 0, 0),
            update_time=datetime.datetime(2026, 1, 1, 10, 0, 0),
            is_deleted=0,
        ),
    )

    conversation_id = service_module._load_admin_conversation(
        conversation_uuid="conv-1",
        user_id=101,
    )

    assert conversation_id == "507f1f77bcf86cd799439041"


def test_load_admin_conversation_raises_database_error_when_id_missing(monkeypatch):
    monkeypatch.setattr(
        service_module,
        "get_admin_conversation",
        lambda *, conversation_uuid, user_id: ConversationDocument(
            uuid=conversation_uuid,
            conversation_type=ConversationType.ADMIN,
            user_id=user_id,
            title="会话标题",
            create_time=datetime.datetime(2026, 1, 1, 10, 0, 0),
            update_time=datetime.datetime(2026, 1, 1, 10, 0, 0),
            is_deleted=0,
        ),
    )

    with pytest.raises(ServiceException) as exc_info:
        service_module._load_admin_conversation(
            conversation_uuid="conv-1",
            user_id=101,
        )
    assert exc_info.value.code == ResponseCode.DATABASE_ERROR.code


def test_prepare_new_conversation_returns_context_with_created_event(monkeypatch):
    """测试目标：新会话上下文正确构建；成功标准：包含会话创建事件与首轮问题历史。"""

    scheduled_title_calls: list[dict] = []

    monkeypatch.setattr(service_module.uuid, "uuid4", lambda: "new-conv-uuid")
    monkeypatch.setattr(
        service_module,
        "add_admin_conversation",
        lambda *, conversation_uuid, user_id: f"db-{conversation_uuid}-{user_id}",
    )
    monkeypatch.setattr(
        service_module,
        "_schedule_title_generation",
        lambda **kwargs: scheduled_title_calls.append(kwargs),
    )

    context = service_module._prepare_new_conversation(
        question="新建会话",
        user_id=100,
        assistant_message_uuid="assistant-msg-uuid",
    )

    assert isinstance(context, service_module.ConversationContext)
    assert context.conversation_uuid == "new-conv-uuid"
    assert context.conversation_id == "db-new-conv-uuid-100"
    assert context.assistant_message_uuid == "assistant-msg-uuid"
    assert [message.content for message in context.history_messages] == ["新建会话"]
    assert isinstance(context.history_messages[0], HumanMessage)
    assert context.is_new_conversation is True
    assert len(context.initial_emitted_events) == 1
    session_event = context.initial_emitted_events[0]
    assert session_event.type == MessageType.NOTICE
    assert session_event.content.state == "created"
    assert session_event.meta == {
        "conversation_uuid": "new-conv-uuid",
        "message_uuid": "assistant-msg-uuid",
    }
    assert scheduled_title_calls == [{"conversation_uuid": "new-conv-uuid", "question": "新建会话"}]


def test_prepare_existing_conversation_returns_context_with_history(monkeypatch):
    """测试目标：旧会话上下文正确构建；成功标准：加载会话并返回历史窗口。"""

    captured: dict = {}
    expected_history = [HumanMessage(content="历史问题"), AIMessage(content="历史回答")]

    monkeypatch.setattr(
        service_module,
        "_load_admin_conversation",
        lambda *, conversation_uuid, user_id: (
            captured.update({"conversation_uuid": conversation_uuid, "user_id": user_id}),
            "507f1f77bcf86cd799439011",
        )[-1],
    )
    monkeypatch.setattr(
        service_module,
        "resolve_assistant_memory_mode",
        lambda: "summary",
    )
    monkeypatch.setattr(
        service_module,
        "load_memory",
        lambda *, memory_type, conversation_uuid, user_id: (
            captured.update(
                {
                    "memory_type": memory_type,
                    "memory_conversation_uuid": conversation_uuid,
                    "memory_user_id": user_id,
                }
            ),
            SimpleNamespace(messages=expected_history),
        )[-1],
    )

    context = service_module._prepare_existing_conversation(
        conversation_uuid="conv-1",
        user_id=100,
        question="本轮问题",
        assistant_message_uuid="assistant-msg-uuid",
    )

    assert isinstance(context, service_module.ConversationContext)
    assert context.conversation_uuid == "conv-1"
    assert context.conversation_id == "507f1f77bcf86cd799439011"
    assert context.assistant_message_uuid == "assistant-msg-uuid"
    assert [message.content for message in context.history_messages] == [
        "历史问题",
        "历史回答",
        "本轮问题",
    ]
    assert isinstance(context.history_messages[-1], HumanMessage)
    assert len(context.initial_emitted_events) == 1
    session_event = context.initial_emitted_events[0]
    assert session_event.type == MessageType.NOTICE
    assert session_event.content.state is None
    assert session_event.meta == {"message_uuid": "assistant-msg-uuid"}
    assert context.is_new_conversation is False
    assert captured == {
        "conversation_uuid": "conv-1",
        "user_id": 100,
        "memory_type": "summary",
        "memory_conversation_uuid": "conv-1",
        "memory_user_id": 100,
    }


def test_prepare_conversation_context_routes_by_conversation_uuid(monkeypatch):
    """测试目标：会话准备总入口分发正确；成功标准：按 UUID 是否为空路由到对应分支。"""

    call_order: list[tuple[str, dict]] = []
    new_context = service_module.ConversationContext(
        conversation_uuid="new-conv",
        conversation_id="new-id",
        assistant_message_uuid="msg-new",
        history_messages=[],
        initial_emitted_events=(),
        is_new_conversation=True,
    )
    existing_context = service_module.ConversationContext(
        conversation_uuid="conv-1",
        conversation_id="old-id",
        assistant_message_uuid="msg-old",
        history_messages=[HumanMessage(content="历史")],
        initial_emitted_events=(),
        is_new_conversation=False,
    )

    monkeypatch.setattr(
        service_module,
        "_prepare_new_conversation",
        lambda *, question, user_id, assistant_message_uuid: (
            call_order.append(
                (
                    "new",
                    {
                        "question": question,
                        "user_id": user_id,
                        "assistant_message_uuid": assistant_message_uuid,
                    },
                )
            ),
            new_context,
        )[-1],
    )
    monkeypatch.setattr(
        service_module,
        "_prepare_existing_conversation",
        lambda *, conversation_uuid, user_id, question, assistant_message_uuid: (
            call_order.append(
                (
                    "existing",
                    {
                        "conversation_uuid": conversation_uuid,
                        "user_id": user_id,
                        "question": question,
                        "assistant_message_uuid": assistant_message_uuid,
                    },
                )
            ),
            existing_context,
        )[-1],
    )

    context_new = service_module._prepare_conversation_context(
        question="问题A",
        user_id=100,
        conversation_uuid=None,
        assistant_message_uuid="msg-a",
    )
    context_existing = service_module._prepare_conversation_context(
        question="问题B",
        user_id=101,
        conversation_uuid="conv-1",
        assistant_message_uuid="msg-b",
    )

    assert context_new is new_context
    assert context_existing is existing_context
    assert call_order == [
        ("new", {"question": "问题A", "user_id": 100, "assistant_message_uuid": "msg-a"}),
        (
            "existing",
            {
                "conversation_uuid": "conv-1",
                "user_id": 101,
                "question": "问题B",
                "assistant_message_uuid": "msg-b",
            },
        ),
    ]


def test_assistant_chat_submit_creates_run_and_placeholder_message(monkeypatch):
    """测试目标：提交接口创建运行态并注册后台任务；成功标准：run、user 消息和 AI 占位消息均被正确创建。"""

    captured: dict = {}
    persist_calls: list[dict] = []
    placeholder_calls: list[dict] = []
    create_run_calls: list[dict] = []
    register_calls: list[dict] = []
    prepared_context = service_module.ConversationContext(
        conversation_uuid="conv-1",
        conversation_id="507f1f77bcf86cd799439011",
        assistant_message_uuid="assistant-msg-uuid",
        history_messages=[HumanMessage(content="历史问题"), AIMessage(content="历史回答"),
                          HumanMessage(content="代理测试")],
        initial_emitted_events=(),
        is_new_conversation=False,
    )

    monkeypatch.setattr(service_module, "get_user_id", lambda: 100)
    monkeypatch.setattr(service_module.uuid, "uuid4", lambda: "assistant-msg-uuid")
    monkeypatch.setattr(
        service_module,
        "_prepare_conversation_context",
        lambda **kwargs: (captured.setdefault("prepare_kwargs", kwargs), prepared_context)[1],
    )
    monkeypatch.setattr(
        service_module.RUN_EVENT_STORE,
        "create_run",
        lambda **kwargs: create_run_calls.append(kwargs) or object(),
    )
    monkeypatch.setattr(
        service_module,
        "_persist_user_message",
        lambda **kwargs: persist_calls.append(kwargs),
    )
    monkeypatch.setattr(
        service_module,
        "_create_placeholder_assistant_message",
        lambda **kwargs: placeholder_calls.append(kwargs),
    )
    monkeypatch.setattr(
        service_module,
        "_build_run_stream_config",
        lambda **kwargs: (
            captured.setdefault("build_run_stream_config_kwargs", kwargs),
            SimpleNamespace(name="stream-config"),
        )[1],
    )
    monkeypatch.setattr(
        service_module.asyncio,
        "create_task",
        lambda coroutine: captured.setdefault("background_task", _DummyAsyncTask(coroutine)),
    )
    monkeypatch.setattr(
        service_module.RUN_EVENT_STORE,
        "register_local_handle",
        lambda **kwargs: register_calls.append(kwargs),
    )

    response = service_module.assistant_chat_submit(
        question="代理测试",
        conversation_uuid="conv-1",
    )

    assert response.conversation_uuid == "conv-1"
    assert response.message_uuid == "assistant-msg-uuid"
    assert response.run_status == AssistantRunStatus.RUNNING
    assert captured["prepare_kwargs"] == {
        "question": "代理测试",
        "user_id": 100,
        "conversation_uuid": "conv-1",
        "assistant_message_uuid": "assistant-msg-uuid",
    }
    assert create_run_calls == [
        {
            "conversation_uuid": "conv-1",
            "user_id": 100,
            "conversation_type": ConversationType.ADMIN.value,
            "assistant_message_uuid": "assistant-msg-uuid",
        }
    ]
    assert persist_calls == [
        {
            "conversation_id": "507f1f77bcf86cd799439011",
            "question": "代理测试",
        }
    ]
    assert placeholder_calls == [
        {
            "conversation_id": "507f1f77bcf86cd799439011",
            "message_uuid": "assistant-msg-uuid",
        }
    ]
    assert captured["build_run_stream_config_kwargs"]["question"] == "代理测试"
    assert captured["build_run_stream_config_kwargs"]["context"] is prepared_context
    assert register_calls and register_calls[-1]["conversation_uuid"] == "conv-1"
    assert register_calls[-1]["handle"].task is captured["background_task"]


def test_persist_failure_only_logs_warning(monkeypatch):
    """测试目标：后台任务失败仅日志；成功标准：异常不抛出且 warning 被记录。"""

    warning_calls: list[dict] = []

    class _DummyLogger:
        def warning(self, message: str, **kwargs):
            warning_calls.append({"message": message, "kwargs": kwargs})

    monkeypatch.setattr(service_module.logger, "opt", lambda **_kwargs: _DummyLogger())

    class _ImmediateThread:
        def __init__(self, target, daemon=True):
            self._target = target
            self.daemon = daemon

        def start(self):
            self._target()

    monkeypatch.setattr(service_module.threading, "Thread", _ImmediateThread)

    service_module._schedule_background_task(
        task_name="broken_task",
        func=lambda **_kwargs: (_ for _ in ()).throw(RuntimeError("boom")),
        kwargs={},
    )

    assert warning_calls
    assert warning_calls[0]["kwargs"]["task_name"] == "broken_task"


def test_assistant_chat_submit_uses_new_conversation_context(monkeypatch):
    """测试目标：新会话提交路径沿用准备后的新会话上下文；成功标准：返回值与运行态、占位消息均使用新会话标识。"""

    create_run_calls: list[dict] = []
    persist_calls: list[dict] = []
    placeholder_calls: list[dict] = []
    register_calls: list[dict] = []
    prepared_context = service_module.ConversationContext(
        conversation_uuid="new-conv-uuid",
        conversation_id="db-new-conv-uuid-100",
        assistant_message_uuid="assistant-msg-uuid",
        history_messages=[HumanMessage(content="新建会话")],
        initial_emitted_events=(),
        is_new_conversation=True,
    )

    monkeypatch.setattr(service_module, "get_user_id", lambda: 100)
    monkeypatch.setattr(service_module.uuid, "uuid4", lambda: "assistant-msg-uuid")
    monkeypatch.setattr(
        service_module,
        "_prepare_conversation_context",
        lambda **_kwargs: prepared_context,
    )
    monkeypatch.setattr(
        service_module.RUN_EVENT_STORE,
        "create_run",
        lambda **kwargs: create_run_calls.append(kwargs) or object(),
    )
    monkeypatch.setattr(
        service_module,
        "_persist_user_message",
        lambda **kwargs: persist_calls.append(kwargs),
    )
    monkeypatch.setattr(
        service_module,
        "_create_placeholder_assistant_message",
        lambda **kwargs: placeholder_calls.append(kwargs),
    )
    monkeypatch.setattr(
        service_module,
        "_build_run_stream_config",
        lambda **_kwargs: SimpleNamespace(name="stream-config"),
    )
    monkeypatch.setattr(
        service_module.asyncio,
        "create_task",
        lambda coroutine: _DummyAsyncTask(coroutine),
    )
    monkeypatch.setattr(
        service_module.RUN_EVENT_STORE,
        "register_local_handle",
        lambda **kwargs: register_calls.append(kwargs),
    )

    response = service_module.assistant_chat_submit(question="新建会话")

    assert response.conversation_uuid == "new-conv-uuid"
    assert response.message_uuid == "assistant-msg-uuid"
    assert create_run_calls[-1]["conversation_uuid"] == "new-conv-uuid"
    assert persist_calls == [
        {
            "conversation_id": "db-new-conv-uuid-100",
            "question": "新建会话",
        }
    ]
    assert placeholder_calls == [
        {
            "conversation_id": "db-new-conv-uuid-100",
            "message_uuid": "assistant-msg-uuid",
        }
    ]
    assert register_calls[-1]["conversation_uuid"] == "new-conv-uuid"


def test_build_attach_streaming_response_replays_terminal_card_and_end_after_snapshot(monkeypatch):
    """测试目标：终态 attach 在发送 snapshot 后仍会补发卡片与结束包；成功标准：刷新后前端可重新拿到 card 和 is_end=true。"""

    monkeypatch.setattr(
        service_module,
        "RUN_EVENT_STORE",
        SimpleNamespace(
            get_snapshot=lambda *, conversation_uuid: service_module.AssistantRunSnapshot(
                answer_text="阶段性回复",
                thinking_text="",
                status=AssistantRunStatus.WAITING_INPUT,
                assistant_message_uuid="msg-1",
                last_event_id="3-0",
            ),
            read_events=lambda *, conversation_uuid, last_event_id, block_ms: [
                SimpleNamespace(
                    event_id="1-0",
                    payload=service_module.AssistantResponse(
                        content=service_module.Content(text="阶段性回复"),
                        type=service_module.MessageType.ANSWER,
                    ),
                ),
                SimpleNamespace(
                    event_id="2-0",
                    payload=service_module.AssistantResponse(
                        type=service_module.MessageType.CARD,
                        card={
                            "type": "consultation-followup-card",
                            "data": {
                                "title": "现在体温是多少？",
                                "options": ["未测量", "37.3以下", "37.3以上"],
                            },
                        },
                        meta={"message_uuid": "msg-1"},
                    ),
                ),
                SimpleNamespace(
                    event_id="3-0",
                    payload=service_module.build_answer_response(
                        "",
                        True,
                        state=AssistantRunStatus.WAITING_INPUT.value,
                        meta={"run_status": AssistantRunStatus.WAITING_INPUT.value},
                    ),
                ),
            ],
            get_run_meta=lambda *, conversation_uuid: None,
        ),
    )

    response = service_module._build_attach_streaming_response(
        conversation_uuid="conv-1",
    )
    payloads = asyncio.run(_collect_sse_payloads(response))

    assert payloads[0]["type"] == "answer"
    assert payloads[0]["content"]["text"] == "阶段性回复"
    assert payloads[0]["content"]["state"] == "replace"
    assert payloads[0]["is_end"] is False
    assert payloads[1]["type"] == "card"
    assert payloads[1]["card"]["type"] == "consultation-followup-card"
    assert payloads[2]["is_end"] is True
    assert payloads[2]["meta"]["run_status"] == "waiting_input"


def test_load_history_reads_latest_window_and_returns_chronological(monkeypatch):
    """测试目标：历史读取顺序正确；成功标准：倒序读后返回正序窗口。"""

    captured: dict = {}

    def _fake_list_messages(*, conversation_id: str, limit: int, ascending: bool):
        captured["conversation_id"] = conversation_id
        captured["limit"] = limit
        captured["ascending"] = ascending
        return [
            type("Doc", (), {"role": MessageRole.USER, "content": "Q2"})(),
            type("Doc", (), {"role": MessageRole.AI, "content": "A1"})(),
            type("Doc", (), {"role": MessageRole.USER, "content": "Q1"})(),
        ]

    monkeypatch.setattr(service_module, "list_messages", _fake_list_messages)

    history_messages = service_module.load_history(
        conversation_id="507f1f77bcf86cd799439011",
        limit=50,
    )

    assert captured == {
        "conversation_id": "507f1f77bcf86cd799439011",
        "limit": 50,
        "ascending": False,
    }
    assert isinstance(history_messages[0], HumanMessage)
    assert isinstance(history_messages[1], AIMessage)
    assert isinstance(history_messages[2], HumanMessage)
    assert [message.content for message in history_messages] == ["Q1", "A1", "Q2"]


def test_conversation_messages_returns_latest_page_in_chronological_order(monkeypatch):
    """测试目标：历史消息按页读取最新窗口，返回时按时间升序。"""

    monkeypatch.setattr(service_module, "get_user_id", lambda: 100)
    monkeypatch.setattr(
        service_module,
        "_load_admin_conversation",
        lambda *, conversation_uuid, user_id: "507f1f77bcf86cd799439011",
    )
    mock_docs = [
        {
            "uuid": "msg-ai-2",
            "role": MessageRole.AI,
            "status": MessageStatus.SUCCESS,
            "content": "AI第二条",
        },
        {
            "uuid": "msg-user-1",
            "role": MessageRole.USER,
            "status": MessageStatus.SUCCESS,
            "content": "用户第一条",
        },
    ]
    monkeypatch.setattr(
        service_module,
        "count_messages",
        lambda **_kwargs: 2,
    )
    monkeypatch.setattr(
        service_module,
        "list_messages",
        lambda **_kwargs: [
            type("Doc", (), item)() for item in mock_docs
        ],
    )

    rows, total = service_module.conversation_messages(
        conversation_uuid="conv-1",
        page_request=PageRequest(page_num=1, page_size=50),
    )

    assert total == 2
    assert [item.id for item in rows] == ["msg-user-1", "msg-ai-2"]
    assert rows[0].role == "user"
    assert rows[0].status is None
    assert rows[0].thinking is None
    assert rows[1].role == "ai"
    assert rows[1].status == "success"
    assert rows[1].thinking is None
    assert rows[1].thought_chain is None


def test_conversation_messages_returns_thinking_for_ai_when_present(monkeypatch):
    """测试目标：AI 历史消息存在思考文本时返回 thinking 字段；成功标准：结果包含完整 thinking 文本。"""

    monkeypatch.setattr(service_module, "get_user_id", lambda: 100)
    monkeypatch.setattr(
        service_module,
        "_load_admin_conversation",
        lambda *, conversation_uuid, user_id: "507f1f77bcf86cd799439011",
    )
    mock_docs = [
        {
            "uuid": "msg-user-1",
            "role": MessageRole.USER,
            "status": MessageStatus.SUCCESS,
            "content": "用户第一条",
        },
        {
            "uuid": "msg-ai-2",
            "role": MessageRole.AI,
            "status": MessageStatus.SUCCESS,
            "content": "AI第二条",
            "thinking": "这是完整思考文本",
        },
    ]
    monkeypatch.setattr(
        service_module,
        "count_messages",
        lambda **_kwargs: 2,
    )
    monkeypatch.setattr(
        service_module,
        "list_messages",
        lambda **_kwargs: [
            type("Doc", (), item)() for item in reversed(mock_docs)
        ],
    )

    rows, total = service_module.conversation_messages(
        conversation_uuid="conv-1",
        page_request=PageRequest(page_num=1, page_size=50),
    )

    assert total == 2
    assert [item.id for item in rows] == ["msg-user-1", "msg-ai-2"]
    assert rows[0].thinking is None
    assert rows[1].thinking == "这是完整思考文本"


def test_conversation_messages_ignores_cards_for_admin_history(monkeypatch):
    """测试目标：admin 历史消息不返回 cards；成功标准：即使底层文档存在 cards 也不透出。"""

    monkeypatch.setattr(service_module, "get_user_id", lambda: 100)
    monkeypatch.setattr(
        service_module,
        "_load_admin_conversation",
        lambda *, conversation_uuid, user_id: "507f1f77bcf86cd799439011",
    )
    monkeypatch.setattr(
        service_module,
        "count_messages",
        lambda **_kwargs: 1,
    )
    monkeypatch.setattr(
        service_module,
        "list_messages",
        lambda **_kwargs: [
            type(
                "Doc",
                (),
                {
                    "uuid": "msg-ai-1",
                    "role": MessageRole.AI,
                    "status": MessageStatus.SUCCESS,
                    "content": "",
                    "cards": [
                        {
                            "id": "card-1",
                            "type": "product-card",
                            "data": {
                                "title": "为您推荐以下商品",
                                "products": [{"id": "1001", "name": "商品1001"}],
                            },
                        }
                    ],
                },
            )()
        ],
    )

    rows, total = service_module.conversation_messages(
        conversation_uuid="conv-1",
        page_request=PageRequest(page_num=1, page_size=50),
    )

    assert total == 1
    assert rows[0].id == "msg-ai-1"
    assert rows[0].content == ""
    assert rows[0].cards is None
