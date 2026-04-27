import asyncio
import json
from dataclasses import dataclass

import pytest

import app.core.speech.stt.session as stt_session_module
import app.services.speech_stt_service as speech_stt_service_module
from app.core.exception.exceptions import ServiceException
from app.core.speech.runtime import (
    SPEECH_CONFIG_REFRESH_CLOSE_CODE,
    SPEECH_CONFIG_REFRESH_CLOSE_REASON,
    clear_speech_runtime_state,
)
from app.core.speech.stt.config import VolcengineSttConfig
from app.core.speech.stt.session import AdminAssistantSttSession, SttSessionError
from app.core.speech.volcengine_speech_protocol import (
    CompressionBits,
    MsgType,
    SerializationBits,
    SttServerMessage,
)
from app.schemas.auth import AuthUser
from app.services.speech_stt_service import speech_stt_stream_service


@pytest.fixture(autouse=True)
def _clear_speech_runtime_state() -> None:
    clear_speech_runtime_state()
    yield
    clear_speech_runtime_state()


class _FakeFrontendWebSocket:
    def __init__(self, messages: list[dict]):
        self._queue: asyncio.Queue = asyncio.Queue()
        for message in messages:
            self._queue.put_nowait(message)
        self.accepted = False
        self.closed = False
        self.close_code: int | None = None
        self.close_reason: str | None = None
        self.sent_json: list[dict] = []

    async def accept(self) -> None:
        self.accepted = True

    async def receive(self) -> dict:
        return await self._queue.get()

    async def send_json(self, payload: dict) -> None:
        self.sent_json.append(payload)

    async def close(self, code: int = 1000, reason: str | None = None) -> None:  # noqa: ARG002
        self.closed = True
        self.close_code = code
        self.close_reason = reason


@dataclass
class _FakeSttClient:
    config: VolcengineSttConfig

    def __post_init__(self) -> None:
        self.provider_log_id = "provider-log-1"
        self.connected = False
        self.closed = False
        self.sent_audio: list[tuple[bytes, bool]] = []
        self._response_queue: asyncio.Queue[SttServerMessage] = asyncio.Queue()

    async def connect(self) -> None:
        self.connected = True

    async def close(self) -> None:
        self.closed = True

    async def send_full_client_request(self, *, request, user_id: int | None = None) -> None:  # noqa: ANN001, ARG002
        return None

    async def send_audio_chunk(self, chunk: bytes, *, is_last: bool) -> None:
        self.sent_audio.append((chunk, is_last))
        self._response_queue.put_nowait(
            SttServerMessage(
                message_type=MsgType.FullServerResponse,
                flag=0b0011 if is_last else 0b0001,
                sequence=len(self.sent_audio),
                is_last_package=is_last,
                serialization=SerializationBits.JSON,
                compression=CompressionBits.None_,
                payload=b"",
                payload_json={"result": {"text": "final" if is_last else "partial"}},
                error_code=None,
            )
        )

    async def receive_server_message(self) -> SttServerMessage:
        return await self._response_queue.get()


def _build_config(*, max_duration_seconds: int = 60) -> VolcengineSttConfig:
    return VolcengineSttConfig(
        endpoint="wss://stt.example/ws",
        app_id="app-id",
        access_token="access-token",
        resource_id="volc.seedasr.sauc.duration",
        max_duration_seconds=max_duration_seconds,
    )


def _build_user() -> AuthUser:
    return AuthUser(
        id=1,
        username="tester",
        roles=["SUPER_ADMIN"],
        permissions=["system:smart_assistant"],
    )


def test_stt_session_start_binary_finish_flow() -> None:
    websocket = _FakeFrontendWebSocket(
        messages=[
            {"type": "websocket.receive", "text": json.dumps({"type": "start"})},
            {"type": "websocket.receive", "bytes": b"\x01\x02"},
            {"type": "websocket.receive", "text": json.dumps({"type": "finish"})},
        ]
    )
    fake_client_holder: dict[str, _FakeSttClient] = {}

    def _factory(*, config: VolcengineSttConfig) -> _FakeSttClient:
        client = _FakeSttClient(config=config)
        fake_client_holder["client"] = client
        return client

    session = AdminAssistantSttSession(
        websocket=websocket,  # type: ignore[arg-type]
        user=_build_user(),
        config=_build_config(),
        stt_client_factory=_factory,
    )

    result = asyncio.run(session.run())

    assert result.reason == "client_finish"
    assert websocket.accepted is True
    assert websocket.closed is True
    assert websocket.close_code == 1000
    assert websocket.sent_json[0]["type"] == "started"
    assert websocket.sent_json[0]["max_duration_seconds"] == 60
    assert any(item.get("type") == "transcript" for item in websocket.sent_json)
    assert websocket.sent_json[-1] == {"type": "completed", "reason": "client_finish"}
    assert fake_client_holder["client"].sent_audio == [(b"\x01\x02", False), (b"", True)]


def test_stt_session_start_supports_custom_duration_under_server_max() -> None:
    websocket = _FakeFrontendWebSocket(
        messages=[
            {"type": "websocket.receive", "text": json.dumps({"type": "start"})},
            {"type": "websocket.receive", "text": json.dumps({"type": "finish"})},
        ]
    )

    session = AdminAssistantSttSession(
        websocket=websocket,  # type: ignore[arg-type]
        user=_build_user(),
        config=_build_config(max_duration_seconds=120),
        session_duration_seconds=30,
        stt_client_factory=lambda *, config: _FakeSttClient(config=config),
    )

    result = asyncio.run(session.run())

    assert result.reason == "client_finish"
    assert websocket.sent_json[0]["type"] == "started"
    assert websocket.sent_json[0]["max_duration_seconds"] == 30
    assert websocket.sent_json[0]["max_allowed_duration_seconds"] == 120


def test_stt_session_start_rejects_duration_exceeding_server_max() -> None:
    with pytest.raises(SttSessionError) as exc_info:
        AdminAssistantSttSession(
            websocket=_FakeFrontendWebSocket(messages=[]),  # type: ignore[arg-type]
            user=_build_user(),
            config=_build_config(max_duration_seconds=60),
            session_duration_seconds=61,
            stt_client_factory=lambda *, config: _FakeSttClient(config=config),
        )

    assert "session_duration_seconds 不能超过 60 秒" in str(exc_info.value)


def test_stt_session_start_rejects_frontend_duration_override() -> None:
    websocket = _FakeFrontendWebSocket(
        messages=[
            {
                "type": "websocket.receive",
                "text": json.dumps({"type": "start", "max_duration_seconds": 61}),
            },
        ]
    )

    session = AdminAssistantSttSession(
        websocket=websocket,  # type: ignore[arg-type]
        user=_build_user(),
        config=_build_config(max_duration_seconds=60),
        stt_client_factory=lambda *, config: _FakeSttClient(config=config),
    )

    with pytest.raises(SttSessionError) as exc_info:
        asyncio.run(session.run())

    assert "max_duration_seconds 不允许由前端设置" in str(exc_info.value)


def test_stt_session_timeout_active_close() -> None:
    websocket = _FakeFrontendWebSocket(
        messages=[
            {"type": "websocket.receive", "text": json.dumps({"type": "start"})},
        ]
    )
    fake_client_holder: dict[str, _FakeSttClient] = {}

    def _factory(*, config: VolcengineSttConfig) -> _FakeSttClient:
        client = _FakeSttClient(config=config)
        fake_client_holder["client"] = client
        return client

    session = AdminAssistantSttSession(
        websocket=websocket,  # type: ignore[arg-type]
        user=_build_user(),
        config=_build_config(max_duration_seconds=1),
        stt_client_factory=_factory,
    )

    result = asyncio.run(session.run())

    assert result.reason == "timeout"
    assert websocket.closed is True
    assert websocket.close_code == 1000
    assert websocket.sent_json[-1]["type"] == "timeout"
    assert websocket.sent_json[-1]["error_code"] == "stt_timeout"
    assert fake_client_holder["client"].sent_audio[-1] == (b"", True)


def test_stt_session_timeout_when_client_never_sends_start() -> None:
    """测试目的：验证客户端连接后未发送 start 时仍会触发会话超时；预期结果：会话返回 timeout 并关闭连接。"""

    websocket = _FakeFrontendWebSocket(messages=[])

    session = AdminAssistantSttSession(
        websocket=websocket,  # type: ignore[arg-type]
        user=_build_user(),
        config=_build_config(max_duration_seconds=1),
        session_duration_seconds=1,
        stt_client_factory=lambda *, config: _FakeSttClient(config=config),
    )

    result = asyncio.run(session.run())

    assert result.reason == "timeout"
    assert websocket.closed is True
    assert websocket.close_code == 1000
    assert websocket.sent_json[-1]["type"] == "timeout"
    assert websocket.sent_json[-1]["error_code"] == "stt_timeout"


def test_stt_session_returns_error_when_upstream_reports_error() -> None:
    websocket = _FakeFrontendWebSocket(
        messages=[
            {"type": "websocket.receive", "text": json.dumps({"type": "start"})},
        ]
    )

    class _ErrorSttClient(_FakeSttClient):
        async def send_full_client_request(self, *, request,
                                           user_id: int | None = None) -> None:  # noqa: ANN001, ARG002
            self._response_queue.put_nowait(
                SttServerMessage(
                    message_type=MsgType.Error,
                    flag=0,
                    sequence=None,
                    is_last_package=True,
                    serialization=SerializationBits.JSON,
                    compression=CompressionBits.None_,
                    payload=b"",
                    payload_json={"message": "provider bad request"},
                    error_code=45000001,
                )
            )

    session = AdminAssistantSttSession(
        websocket=websocket,  # type: ignore[arg-type]
        user=_build_user(),
        config=_build_config(),
        stt_client_factory=lambda *, config: _ErrorSttClient(config=config),
    )

    result = asyncio.run(session.run())

    assert result.reason == "upstream_end"
    assert websocket.closed is True
    assert websocket.close_code == 1011
    assert any(
        item.get("type") == "error" and "provider bad request" in item.get("message", "")
        for item in websocket.sent_json
    )


def test_stt_session_interrupts_on_config_refresh(monkeypatch: pytest.MonkeyPatch) -> None:
    """测试目的：语音配置刷新时应主动中断进行中的 STT 会话；预期结果：返回 config_refresh 并以 1012 关闭前端连接。"""

    websocket = _FakeFrontendWebSocket(
        messages=[
            {"type": "websocket.receive", "text": json.dumps({"type": "start"})},
        ]
    )
    fake_client_holder: dict[str, _FakeSttClient] = {}
    registered_sessions: list[AdminAssistantSttSession] = []
    unregistered_sessions: list[AdminAssistantSttSession] = []

    def _factory(*, config: VolcengineSttConfig) -> _FakeSttClient:
        client = _FakeSttClient(config=config)
        fake_client_holder["client"] = client
        return client

    monkeypatch.setattr(
        stt_session_module,
        "register_active_stt_session",
        lambda session: registered_sessions.append(session),
    )
    monkeypatch.setattr(
        stt_session_module,
        "unregister_active_stt_session",
        lambda session: unregistered_sessions.append(session),
    )

    session = AdminAssistantSttSession(
        websocket=websocket,  # type: ignore[arg-type]
        user=_build_user(),
        config=_build_config(),
        stt_client_factory=_factory,
    )

    async def _run_and_interrupt() -> tuple[str, bool]:
        task = asyncio.create_task(session.run())
        await asyncio.sleep(0)
        await asyncio.sleep(0)
        await session.interrupt_due_to_config_refresh()
        result = await task
        return result.reason, fake_client_holder["client"].closed

    reason, client_closed = asyncio.run(_run_and_interrupt())

    assert reason == "config_refresh"
    assert client_closed is True
    assert websocket.closed is True
    assert websocket.close_code == SPEECH_CONFIG_REFRESH_CLOSE_CODE
    assert websocket.close_reason == SPEECH_CONFIG_REFRESH_CLOSE_REASON
    assert registered_sessions == [session]
    assert unregistered_sessions == [session]


def test_speech_stt_service_closes_when_stt_config_invalid(monkeypatch: pytest.MonkeyPatch) -> None:
    websocket = _FakeFrontendWebSocket(messages=[])

    def _raise_config_error() -> VolcengineSttConfig:
        raise ServiceException(message="VOLCENGINE_APP_ID is not set")

    monkeypatch.setattr(
        speech_stt_service_module,
        "resolve_volcengine_stt_config",
        _raise_config_error,
    )

    asyncio.run(
        speech_stt_stream_service(
            websocket=websocket,  # type: ignore[arg-type]
            user=_build_user(),
        )
    )

    assert websocket.closed is True
    assert websocket.close_code == 1011


def test_speech_stt_service_supports_business_level_session_duration(monkeypatch: pytest.MonkeyPatch) -> None:
    websocket = _FakeFrontendWebSocket(messages=[])
    captured: dict[str, int | None] = {"session_duration_seconds": None}

    monkeypatch.setattr(
        speech_stt_service_module,
        "resolve_volcengine_stt_config",
        lambda: _build_config(max_duration_seconds=600),
    )

    class _FakeSession:
        def __init__(
                self,
                *,
                websocket,  # noqa: ANN001, ARG002
                user,  # noqa: ANN001, ARG002
                config,  # noqa: ANN001, ARG002
                session_duration_seconds: int | None = None,
        ) -> None:
            captured["session_duration_seconds"] = session_duration_seconds

        async def run(self) -> None:
            return None

    monkeypatch.setattr(
        speech_stt_service_module,
        "AdminAssistantSttSession",
        _FakeSession,
    )

    asyncio.run(
        speech_stt_stream_service(
            websocket=websocket,  # type: ignore[arg-type]
            user=_build_user(),
            session_duration_seconds=30,
        )
    )

    assert captured["session_duration_seconds"] == 30
