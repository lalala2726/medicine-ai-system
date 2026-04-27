import asyncio
from types import SimpleNamespace

import pytest

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.core.speech.stt import client as stt_client_module
from app.core.speech.stt.config import VolcengineSttConfig


class _DummyWebSocket:
    def __init__(self) -> None:
        self.closed = False
        self.response = None

    async def close(self) -> None:
        self.closed = True


class _DummyConnectError(Exception):
    def __init__(self, response) -> None:
        super().__init__("connect failed")
        self.response = response


def _build_config() -> VolcengineSttConfig:
    return VolcengineSttConfig(
        endpoint="wss://stt.example/ws",
        app_id="app-id",
        access_token="access-token",
        resource_id="volc.seedasr.sauc.duration",
        max_duration_seconds=60,
    )


def test_verify_volcengine_stt_connection_on_startup_success(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setattr(stt_client_module, "resolve_volcengine_stt_config", lambda: _build_config())
    dummy_ws = _DummyWebSocket()
    dummy_ws.response = SimpleNamespace(headers={"x-tt-logid": "log-1"})

    async def _fake_connect(*_args, **_kwargs):
        return dummy_ws

    monkeypatch.setattr(stt_client_module.websockets, "connect", _fake_connect)

    asyncio.run(stt_client_module.verify_volcengine_stt_connection_on_startup())

    assert dummy_ws.closed is True


def test_verify_volcengine_stt_connection_on_startup_skips_when_config_invalid(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    monkeypatch.setattr(
        stt_client_module,
        "resolve_volcengine_stt_config",
        lambda: (_ for _ in ()).throw(
            ServiceException(
                code=ResponseCode.INTERNAL_ERROR,
                message="VOLCENGINE_APP_ID is not set",
            )
        ),
    )
    called = {"connect": False}

    async def _fake_connect(*_args, **_kwargs):
        called["connect"] = True
        return _DummyWebSocket()

    monkeypatch.setattr(stt_client_module.websockets, "connect", _fake_connect)

    asyncio.run(stt_client_module.verify_volcengine_stt_connection_on_startup())

    assert called["connect"] is False


def test_verify_volcengine_stt_connection_on_startup_handles_connect_error(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    monkeypatch.setattr(stt_client_module, "resolve_volcengine_stt_config", lambda: _build_config())

    async def _fake_connect(*_args, **_kwargs):
        raise RuntimeError("connect failed")

    monkeypatch.setattr(stt_client_module.websockets, "connect", _fake_connect)

    asyncio.run(stt_client_module.verify_volcengine_stt_connection_on_startup())


def test_extract_startup_connect_error_detail_with_response() -> None:
    exc = _DummyConnectError(
        SimpleNamespace(
            status_code=400,
            reason_phrase="Bad Request",
            body=b'{"message":"invalid resource id"}',
        )
    )

    status, reason, body = stt_client_module._extract_startup_connect_error_detail(exc)  # noqa: SLF001

    assert status == 400
    assert reason == "Bad Request"
    assert body == '{"message":"invalid resource id"}'
