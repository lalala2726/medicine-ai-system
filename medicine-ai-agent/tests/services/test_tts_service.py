import asyncio
import json
from types import SimpleNamespace

import pytest

import app.core.speech.tts.client as service_module
from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.core.speech.runtime import clear_speech_runtime_state
from app.core.speech.tts.config import VolcengineTtsConfig
from app.core.speech.volcengine_speech_protocol import EventType, Message, MsgType
from app.schemas.document.conversation import ConversationType
from app.schemas.document.message import MessageRole


@pytest.fixture(autouse=True)
def _clear_speech_runtime_state_fixture():
    clear_speech_runtime_state()
    yield
    clear_speech_runtime_state()


class _DummyWebSocket:
    def __init__(self):
        self.sent_frames: list[bytes] = []
        self.closed = False
        self.closed_event: asyncio.Event | None = None

    async def send(self, frame: bytes):
        self.sent_frames.append(frame)

    async def close(self):
        self.closed = True
        if self.closed_event is not None:
            self.closed_event.set()


def _build_config() -> VolcengineTtsConfig:
    return VolcengineTtsConfig(
        endpoint="wss://example.com/tts",
        app_id="app-id",
        access_token="token",
        resource_id="volc.service_type.10029",
        voice_type="zh_female_1",
        encoding="mp3",
        sample_rate=24000,
        max_text_chars=300,
    )


async def _collect_stream(stream):
    chunks: list[bytes] = []
    async for item in stream:
        chunks.append(item)
    return chunks


def test_build_message_tts_stream_raises_not_found_when_message_missing(monkeypatch):
    monkeypatch.setattr(service_module, "get_message_by_uuid", lambda _uuid: None)

    with pytest.raises(ServiceException) as exc_info:
        service_module.build_message_tts_stream(
            message_uuid="msg-1",
            user_id=1,
            conversation_type=ConversationType.ADMIN,
        )

    assert exc_info.value.code == ResponseCode.NOT_FOUND.code


def test_build_message_tts_stream_raises_not_found_when_message_not_owned(monkeypatch):
    monkeypatch.setattr(
        service_module,
        "get_message_by_uuid",
        lambda _uuid: SimpleNamespace(
            conversation_id="507f1f77bcf86cd799439011",
            role=MessageRole.AI,
            content="hello",
        ),
    )
    monkeypatch.setattr(
        service_module,
        "get_admin_conversation_by_id",
        lambda **_kwargs: None,
    )

    with pytest.raises(ServiceException) as exc_info:
        service_module.build_message_tts_stream(
            message_uuid="msg-2",
            user_id=2,
            conversation_type=ConversationType.ADMIN,
        )

    assert exc_info.value.code == ResponseCode.NOT_FOUND.code


def test_build_message_tts_stream_raises_bad_request_when_message_role_not_ai(monkeypatch):
    monkeypatch.setattr(
        service_module,
        "get_message_by_uuid",
        lambda _uuid: SimpleNamespace(
            conversation_id="507f1f77bcf86cd799439011",
            role=MessageRole.USER,
            content="user text",
        ),
    )
    monkeypatch.setattr(
        service_module,
        "get_admin_conversation_by_id",
        lambda **_kwargs: SimpleNamespace(uuid="conv-3", user_id=3),
    )

    with pytest.raises(ServiceException) as exc_info:
        service_module.build_message_tts_stream(
            message_uuid="msg-3",
            user_id=3,
            conversation_type=ConversationType.ADMIN,
        )

    assert exc_info.value.code == ResponseCode.BAD_REQUEST.code


def test_load_message_context_for_tts_supports_client_conversation(monkeypatch):
    monkeypatch.setattr(
        service_module,
        "get_message_by_uuid",
        lambda _uuid: SimpleNamespace(
            conversation_id="507f1f77bcf86cd799439012",
            role=MessageRole.AI,
            content="client hello",
        ),
    )
    monkeypatch.setattr(
        service_module,
        "get_client_conversation_by_id",
        lambda **_kwargs: SimpleNamespace(uuid="client-conv-1", user_id=8),
    )

    context = service_module._load_message_context_for_tts(
        message_uuid="msg-client-1",
        user_id=8,
        conversation_type=ConversationType.CLIENT,
    )

    assert context.message_uuid == "msg-client-1"
    assert context.conversation_uuid == "client-conv-1"
    assert context.user_id == 8
    assert context.raw_text == "client hello"


def test_load_message_context_for_tts_rejects_unowned_client_conversation(monkeypatch):
    monkeypatch.setattr(
        service_module,
        "get_message_by_uuid",
        lambda _uuid: SimpleNamespace(
            conversation_id="507f1f77bcf86cd799439013",
            role=MessageRole.AI,
            content="client hello",
        ),
    )
    monkeypatch.setattr(
        service_module,
        "get_client_conversation_by_id",
        lambda **_kwargs: None,
    )

    with pytest.raises(ServiceException) as exc_info:
        service_module._load_message_context_for_tts(
            message_uuid="msg-client-2",
            user_id=9,
            conversation_type=ConversationType.CLIENT,
        )

    assert exc_info.value.code == ResponseCode.NOT_FOUND.code


def test_prepare_tts_text_truncates_with_prefix():
    config = _build_config()
    config = VolcengineTtsConfig(
        endpoint=config.endpoint,
        app_id=config.app_id,
        access_token=config.access_token,
        resource_id=config.resource_id,
        voice_type=config.voice_type,
        encoding=config.encoding,
        sample_rate=config.sample_rate,
        max_text_chars=10,
    )

    raw_text = "\n".join(
        [
            "###第一行内容",
            "**第二行内容很长很长**",
            "第三行内容 https://example.com",
        ],
    )
    prepared = service_module.prepare_tts_text(
        raw_text=raw_text,
        max_text_chars=config.max_text_chars,
    )

    expected_prefix = service_module.DEFAULT_TTS_TRUNCATION_PREFIX_TEMPLATE.format(max_chars=10).strip()
    assert prepared.sent_text.startswith(expected_prefix)
    truncated_body = prepared.sent_text[len(expected_prefix):].lstrip("\n")
    assert len(truncated_body) <= config.max_text_chars
    assert "https://" not in prepared.sent_text.lower()
    assert "#" not in prepared.sent_text
    assert "*" not in prepared.sent_text
    assert "第三行内容" not in truncated_body
    assert prepared.is_truncated is True
    assert prepared.billable_chars == len(prepared.sent_text)


def test_prepare_tts_text_keeps_prefix_outside_body_char_budget():
    config = VolcengineTtsConfig(
        endpoint="wss://example.com/tts",
        app_id="app-id",
        access_token="token",
        resource_id="volc.service_type.10029",
        voice_type="zh_female_1",
        encoding="mp3",
        sample_rate=24000,
        max_text_chars=5,
    )

    prepared = service_module.prepare_tts_text(
        raw_text="第一行很长很长\n第二行",
        max_text_chars=config.max_text_chars,
    )

    expected_prefix = service_module.DEFAULT_TTS_TRUNCATION_PREFIX_TEMPLATE.format(max_chars=5).strip()
    assert prepared.sent_text.startswith(expected_prefix)
    truncated_body = prepared.sent_text[len(expected_prefix):].lstrip("\n")
    assert truncated_body == "第一行很长"
    assert len(truncated_body) == config.max_text_chars
    assert len(prepared.sent_text) > config.max_text_chars


def test_prepare_tts_text_raises_when_sanitized_empty():
    config = _build_config()
    raw_text = "```json\n{\"a\":1}\n```\nhttps://example.com\n"

    with pytest.raises(ServiceException) as exc_info:
        service_module.prepare_tts_text(
            raw_text=raw_text,
            max_text_chars=config.max_text_chars,
        )

    assert exc_info.value.code == ResponseCode.BAD_REQUEST.code
    assert "清洗后为空" in exc_info.value.message


def test_build_start_session_payload_keeps_enable_timestamp_for_tts_1_resource():
    payload = json.loads(
        service_module._build_start_session_payload(config=_build_config()).decode("utf-8")
    )

    assert payload["req_params"]["audio_params"] == {
        "format": "mp3",
        "sample_rate": 24000,
        "enable_timestamp": True,
    }


def test_build_start_session_payload_omits_enable_timestamp_for_tts_2_resource():
    config = VolcengineTtsConfig(
        endpoint="wss://example.com/tts",
        app_id="app-id",
        access_token="token",
        resource_id="seed-tts-2.0",
        voice_type="zh_female_xiaohe_uranus_bigtts",
        encoding="mp3",
        sample_rate=24000,
        max_text_chars=300,
    )

    payload = json.loads(
        service_module._build_start_session_payload(config=config).decode("utf-8")
    )

    assert payload["req_params"]["audio_params"] == {
        "format": "mp3",
        "sample_rate": 24000,
    }


def test_build_task_request_payload_only_sends_text():
    payload = json.loads(
        service_module._build_task_request_payload(text="测试文本").decode("utf-8")
    )

    assert payload["req_params"] == {
        "text": "测试文本",
    }


def test_stream_message_tts_yields_audio_chunks_in_order(monkeypatch):
    monkeypatch.setattr(
        service_module,
        "get_message_by_uuid",
        lambda _uuid: SimpleNamespace(
            conversation_id="507f1f77bcf86cd799439011",
            role=MessageRole.AI,
            content="测试文本",
        ),
    )
    monkeypatch.setattr(
        service_module,
        "get_admin_conversation_by_id",
        lambda **_kwargs: SimpleNamespace(uuid="conv-4", user_id=4),
    )
    monkeypatch.setattr(
        service_module,
        "resolve_volcengine_tts_config",
        lambda **_kwargs: _build_config(),
    )
    monkeypatch.setattr(
        service_module,
        "build_volcengine_tts_headers",
        lambda *_args, **_kwargs: {"X-Api-App-Key": "app-id"},
    )
    usage_calls: list[dict] = []
    monkeypatch.setattr(
        service_module,
        "add_message_tts_usage",
        lambda **kwargs: usage_calls.append(kwargs) or "507f1f77bcf86cd799439071",
    )

    dummy_ws = _DummyWebSocket()
    dummy_ws.response = SimpleNamespace(headers={"x-tt-logid": "log-success"})

    async def _fake_connect(*_args, **_kwargs):
        return dummy_ws

    monkeypatch.setattr(service_module.websockets, "connect", _fake_connect)

    async def _noop(*_args, **_kwargs):
        return None

    monkeypatch.setattr(service_module, "start_connection", _noop)
    monkeypatch.setattr(service_module, "start_session", _noop)
    monkeypatch.setattr(service_module, "task_request", _noop)
    monkeypatch.setattr(service_module, "finish_session", _noop)
    monkeypatch.setattr(service_module, "finish_connection", _noop)
    monkeypatch.setattr(service_module, "wait_for_event", _noop)

    received_messages = [
        Message(type=MsgType.AudioOnlyServer, payload=b"chunk-1"),
        Message(type=MsgType.AudioOnlyServer, payload=b"chunk-2"),
        Message(type=MsgType.FullServerResponse, event=EventType.SessionFinished, payload=b"{}"),
    ]

    async def _fake_receive_message(*_args, **_kwargs):
        return received_messages.pop(0)

    monkeypatch.setattr(service_module, "receive_message", _fake_receive_message)

    stream = service_module.build_message_tts_stream(
        message_uuid="msg-4",
        user_id=4,
        conversation_type=ConversationType.ADMIN,
    )
    chunks = asyncio.run(_collect_stream(stream.audio_stream))

    assert chunks == [b"chunk-1", b"chunk-2"]
    assert dummy_ws.closed is True
    assert len(usage_calls) == 1
    assert usage_calls[0]["message_uuid"] == "msg-4"
    assert usage_calls[0]["conversation_uuid"] == "conv-4"
    assert usage_calls[0]["user_id"] == 4
    assert usage_calls[0]["sent_text"] == "测试文本"
    assert usage_calls[0]["source_text_chars"] == len("测试文本")
    assert usage_calls[0]["sanitized_text_chars"] == len("测试文本")
    assert usage_calls[0]["is_truncated"] is False
    assert usage_calls[0]["audio_chunk_count"] == 2
    assert usage_calls[0]["audio_bytes"] == len(b"chunk-1") + len(b"chunk-2")
    assert usage_calls[0]["provider_log_id"] == "log-success"


def test_stream_message_tts_stops_gracefully_when_upstream_interrupted(monkeypatch):
    monkeypatch.setattr(
        service_module,
        "get_message_by_uuid",
        lambda _uuid: SimpleNamespace(
            conversation_id="507f1f77bcf86cd799439011",
            role=MessageRole.AI,
            content="测试文本",
        ),
    )
    monkeypatch.setattr(
        service_module,
        "get_admin_conversation_by_id",
        lambda **_kwargs: SimpleNamespace(uuid="conv-5", user_id=5),
    )
    monkeypatch.setattr(
        service_module,
        "resolve_volcengine_tts_config",
        lambda **_kwargs: _build_config(),
    )
    monkeypatch.setattr(
        service_module,
        "build_volcengine_tts_headers",
        lambda *_args, **_kwargs: {"X-Api-App-Key": "app-id"},
    )
    usage_calls: list[dict] = []
    monkeypatch.setattr(
        service_module,
        "add_message_tts_usage",
        lambda **kwargs: usage_calls.append(kwargs) or "507f1f77bcf86cd799439072",
    )

    dummy_ws = _DummyWebSocket()

    async def _fake_connect(*_args, **_kwargs):
        return dummy_ws

    monkeypatch.setattr(service_module.websockets, "connect", _fake_connect)

    async def _noop(*_args, **_kwargs):
        return None

    monkeypatch.setattr(service_module, "start_connection", _noop)
    monkeypatch.setattr(service_module, "start_session", _noop)
    monkeypatch.setattr(service_module, "task_request", _noop)
    monkeypatch.setattr(service_module, "finish_session", _noop)
    monkeypatch.setattr(service_module, "finish_connection", _noop)
    monkeypatch.setattr(service_module, "wait_for_event", _noop)

    async def _raise_on_receive(*_args, **_kwargs):
        raise RuntimeError("connection closed")

    monkeypatch.setattr(service_module, "receive_message", _raise_on_receive)

    stream = service_module.build_message_tts_stream(
        message_uuid="msg-5",
        user_id=5,
        conversation_type=ConversationType.ADMIN,
    )
    chunks = asyncio.run(_collect_stream(stream.audio_stream))

    assert chunks == []
    assert dummy_ws.closed is True
    assert usage_calls == []


def test_stream_message_tts_interrupts_on_config_refresh(monkeypatch):
    monkeypatch.setattr(
        service_module,
        "get_message_by_uuid",
        lambda _uuid: SimpleNamespace(
            conversation_id="507f1f77bcf86cd799439011",
            role=MessageRole.AI,
            content="测试文本",
        ),
    )
    monkeypatch.setattr(
        service_module,
        "get_admin_conversation_by_id",
        lambda **_kwargs: SimpleNamespace(uuid="conv-refresh", user_id=7),
    )
    monkeypatch.setattr(
        service_module,
        "resolve_volcengine_tts_config",
        lambda **_kwargs: _build_config(),
    )
    monkeypatch.setattr(
        service_module,
        "build_volcengine_tts_headers",
        lambda *_args, **_kwargs: {"X-Api-App-Key": "app-id"},
    )
    usage_calls: list[dict] = []
    monkeypatch.setattr(
        service_module,
        "add_message_tts_usage",
        lambda **kwargs: usage_calls.append(kwargs) or "507f1f77bcf86cd799439074",
    )

    registered_handles: list[object] = []
    unregistered_handles: list[object] = []
    monkeypatch.setattr(
        service_module,
        "register_active_tts_stream",
        lambda handle: registered_handles.append(handle),
    )
    monkeypatch.setattr(
        service_module,
        "unregister_active_tts_stream",
        lambda handle: unregistered_handles.append(handle),
    )

    dummy_ws = _DummyWebSocket()
    dummy_ws.closed_event = asyncio.Event()

    async def _fake_connect(*_args, **_kwargs):
        return dummy_ws

    monkeypatch.setattr(service_module.websockets, "connect", _fake_connect)

    async def _noop(*_args, **_kwargs):
        return None

    monkeypatch.setattr(service_module, "start_connection", _noop)
    monkeypatch.setattr(service_module, "start_session", _noop)
    monkeypatch.setattr(service_module, "task_request", _noop)
    monkeypatch.setattr(service_module, "finish_session", _noop)
    monkeypatch.setattr(service_module, "finish_connection", _noop)
    monkeypatch.setattr(service_module, "wait_for_event", _noop)

    async def _wait_until_closed(*_args, **_kwargs):
        await dummy_ws.closed_event.wait()
        raise RuntimeError("connection closed")

    monkeypatch.setattr(service_module, "receive_message", _wait_until_closed)

    stream = service_module.build_message_tts_stream(
        message_uuid="msg-refresh",
        user_id=7,
        conversation_type=ConversationType.ADMIN,
    )

    async def _run_stream_with_refresh() -> list[bytes]:
        consume_task = asyncio.create_task(_collect_stream(stream.audio_stream))
        await asyncio.sleep(0)
        await asyncio.sleep(0)
        assert len(registered_handles) == 1
        handle = registered_handles[0]
        await handle.interrupt_due_to_config_refresh()
        return await consume_task

    chunks = asyncio.run(_run_stream_with_refresh())

    assert chunks == []
    assert dummy_ws.closed is True
    assert usage_calls == []
    assert len(unregistered_handles) == 1
    assert unregistered_handles[0] is registered_handles[0]


def test_stream_message_tts_persists_usage_with_truncated_flag(monkeypatch):
    config = _build_config()
    config = VolcengineTtsConfig(
        endpoint=config.endpoint,
        app_id=config.app_id,
        access_token=config.access_token,
        resource_id=config.resource_id,
        voice_type=config.voice_type,
        encoding=config.encoding,
        sample_rate=config.sample_rate,
        max_text_chars=5,
    )
    monkeypatch.setattr(
        service_module,
        "get_message_by_uuid",
        lambda _uuid: SimpleNamespace(
            conversation_id="507f1f77bcf86cd799439011",
            role=MessageRole.AI,
            content="这是很长很长的文本，用于触发截断。",
        ),
    )
    monkeypatch.setattr(
        service_module,
        "get_admin_conversation_by_id",
        lambda **_kwargs: SimpleNamespace(uuid="conv-6", user_id=6),
    )
    monkeypatch.setattr(
        service_module,
        "resolve_volcengine_tts_config",
        lambda **_kwargs: config,
    )
    monkeypatch.setattr(
        service_module,
        "build_volcengine_tts_headers",
        lambda *_args, **_kwargs: {"X-Api-App-Key": "app-id"},
    )
    usage_calls: list[dict] = []
    monkeypatch.setattr(
        service_module,
        "add_message_tts_usage",
        lambda **kwargs: usage_calls.append(kwargs) or "507f1f77bcf86cd799439073",
    )

    dummy_ws = _DummyWebSocket()

    async def _fake_connect(*_args, **_kwargs):
        return dummy_ws

    monkeypatch.setattr(service_module.websockets, "connect", _fake_connect)

    async def _noop(*_args, **_kwargs):
        return None

    monkeypatch.setattr(service_module, "start_connection", _noop)
    monkeypatch.setattr(service_module, "start_session", _noop)
    monkeypatch.setattr(service_module, "task_request", _noop)
    monkeypatch.setattr(service_module, "finish_session", _noop)
    monkeypatch.setattr(service_module, "finish_connection", _noop)
    monkeypatch.setattr(service_module, "wait_for_event", _noop)

    received_messages = [
        Message(type=MsgType.AudioOnlyServer, payload=b"chunk"),
        Message(type=MsgType.FullServerResponse, event=EventType.SessionFinished, payload=b"{}"),
    ]

    async def _fake_receive_message(*_args, **_kwargs):
        return received_messages.pop(0)

    monkeypatch.setattr(service_module, "receive_message", _fake_receive_message)

    stream = service_module.build_message_tts_stream(
        message_uuid="msg-6",
        user_id=6,
        conversation_type=ConversationType.ADMIN,
    )
    chunks = asyncio.run(_collect_stream(stream.audio_stream))

    assert chunks == [b"chunk"]
    assert len(usage_calls) == 1
    assert usage_calls[0]["message_uuid"] == "msg-6"
    assert usage_calls[0]["is_truncated"] is True
    assert usage_calls[0]["max_text_chars"] == 5
    expected_prefix = service_module.DEFAULT_TTS_TRUNCATION_PREFIX_TEMPLATE.format(max_chars=5).strip()
    assert usage_calls[0]["sent_text"].startswith(expected_prefix[: len(usage_calls[0]["sent_text"])])
    truncated_body = usage_calls[0]["sent_text"][len(expected_prefix):].lstrip("\n")
    assert len(truncated_body) <= 5
    assert usage_calls[0]["sanitized_text_chars"] >= 5


def test_verify_volcengine_tts_connection_on_startup_success(monkeypatch):
    monkeypatch.setattr(
        service_module,
        "is_volcengine_tts_startup_connect_enabled",
        lambda: True,
    )
    monkeypatch.setattr(
        service_module,
        "is_volcengine_tts_startup_fail_fast_enabled",
        lambda: False,
    )
    monkeypatch.setattr(
        service_module,
        "resolve_volcengine_tts_config",
        lambda: _build_config(),
    )
    monkeypatch.setattr(
        service_module,
        "build_volcengine_tts_headers",
        lambda *_args, **_kwargs: {"X-Api-App-Key": "app-id"},
    )

    dummy_ws = _DummyWebSocket()
    dummy_ws.response = SimpleNamespace(headers={"x-tt-logid": "log-1"})

    async def _fake_connect(*_args, **_kwargs):
        return dummy_ws

    call_events: list[tuple[str, object]] = []

    async def _fake_start_connection(*_args, **_kwargs):
        call_events.append(("start_connection", None))

    async def _fake_finish_connection(*_args, **_kwargs):
        call_events.append(("finish_connection", None))

    async def _fake_wait_for_event(*_args, **kwargs):
        call_events.append(("wait_for_event", kwargs.get("event_type")))
        return Message(type=MsgType.FullServerResponse, event=kwargs.get("event_type"), payload=b"{}")

    monkeypatch.setattr(service_module.websockets, "connect", _fake_connect)
    monkeypatch.setattr(service_module, "start_connection", _fake_start_connection)
    monkeypatch.setattr(service_module, "finish_connection", _fake_finish_connection)
    monkeypatch.setattr(service_module, "wait_for_event", _fake_wait_for_event)

    asyncio.run(service_module.verify_volcengine_tts_connection_on_startup())

    assert ("start_connection", None) in call_events
    assert ("finish_connection", None) in call_events
    assert ("wait_for_event", EventType.ConnectionStarted) in call_events
    assert ("wait_for_event", EventType.ConnectionFinished) in call_events
    assert dummy_ws.closed is True


def test_verify_volcengine_tts_connection_on_startup_skips_when_disabled(monkeypatch):
    monkeypatch.setattr(
        service_module,
        "is_volcengine_tts_startup_connect_enabled",
        lambda: False,
    )

    called = {"connect": False}

    async def _fake_connect(*_args, **_kwargs):
        called["connect"] = True
        return _DummyWebSocket()

    monkeypatch.setattr(service_module.websockets, "connect", _fake_connect)

    asyncio.run(service_module.verify_volcengine_tts_connection_on_startup())

    assert called["connect"] is False
