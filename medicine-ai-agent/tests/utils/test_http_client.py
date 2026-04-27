import asyncio
import json

import httpx
import pytest

import app.utils.http_client as http_client_module
from app.core.exception.exceptions import ServiceException
from app.core.security.system_auth.config import clear_system_auth_settings_cache
from app.utils.http_client import HttpClient


class _DummyLogger:
    def __init__(self):
        self.info_logs: list[str] = []
        self.warning_logs: list[str] = []
        self.error_logs: list[str] = []

    @staticmethod
    def _render(message, *args):
        if not args:
            return message
        try:
            return message.format(*args)
        except Exception:
            try:
                return message % args
            except Exception:
                return f"{message} {args}"

    def info(self, message, *args):
        self.info_logs.append(self._render(message, *args))

    def warning(self, message, *args):
        self.warning_logs.append(self._render(message, *args))

    def error(self, message, *args):
        self.error_logs.append(self._render(message, *args))


def _build_response(
        method: str,
        url: str,
        status_code: int = 200,
        body: str = "ok",
        json_body: dict | None = None,
) -> httpx.Response:
    request = httpx.Request(method, url)
    if json_body is not None:
        return httpx.Response(status_code=status_code, request=request, json=json_body)
    return httpx.Response(status_code=status_code, request=request, text=body)


@pytest.fixture(autouse=True)
def _reset_system_signing_env(monkeypatch):
    """清理系统签名相关环境变量，避免受本地环境污染。"""
    monkeypatch.delenv("X_AGENT_KEY", raising=False)
    monkeypatch.delenv("SYSTEM_AUTH_LOCAL_SECRET", raising=False)
    monkeypatch.delenv("SYSTEM_AUTH_ENABLED", raising=False)
    monkeypatch.delenv("SYSTEM_AUTH_CLIENTS_JSON", raising=False)
    monkeypatch.delenv("SYSTEM_AUTH_DEFAULT_SIGN_VERSION", raising=False)
    clear_system_auth_settings_cache()
    yield
    clear_system_auth_settings_cache()


def test_http_client_logs_request_when_enabled(monkeypatch):
    monkeypatch.setenv("HTTP_CLIENT_LOG_ENABLED", "true")
    client = HttpClient(base_url="http://example.com")

    dummy_logger = _DummyLogger()
    monkeypatch.setattr(http_client_module, "logger", dummy_logger)

    async def _fake_send(request: httpx.Request, **_kwargs):
        return _build_response(request.method, str(request.url))

    monkeypatch.setattr(client._client, "send", _fake_send)

    asyncio.run(client.get("/ping", headers={"Authorization": "Bearer test"}))
    asyncio.run(client.close())

    assert any("HTTP request:" in item for item in dummy_logger.info_logs)
    assert any("HTTP response:" in item for item in dummy_logger.info_logs)


def test_http_client_log_switch_takes_effect_without_recreate(monkeypatch):
    monkeypatch.setenv("HTTP_CLIENT_LOG_ENABLED", "false")
    client = HttpClient(base_url="http://example.com")

    dummy_logger = _DummyLogger()
    monkeypatch.setattr(http_client_module, "logger", dummy_logger)

    async def _fake_send(request: httpx.Request, **_kwargs):
        return _build_response(request.method, str(request.url))

    monkeypatch.setattr(client._client, "send", _fake_send)

    asyncio.run(client.get("/first", headers={"Authorization": "Bearer test"}))
    assert dummy_logger.info_logs == []

    monkeypatch.setenv("HTTP_CLIENT_LOG_ENABLED", "true")
    asyncio.run(client.get("/second", headers={"Authorization": "Bearer test"}))
    asyncio.run(client.close())

    assert any("HTTP request:" in item for item in dummy_logger.info_logs)


def test_http_client_logs_block_reason_when_auth_missing(monkeypatch):
    monkeypatch.setenv("HTTP_CLIENT_LOG_ENABLED", "true")
    client = HttpClient(base_url="http://example.com")

    dummy_logger = _DummyLogger()
    monkeypatch.setattr(http_client_module, "logger", dummy_logger)

    with pytest.raises(Exception):
        asyncio.run(client.get("/no-auth"))

    asyncio.run(client.close())
    assert any("HTTP request blocked before send:" in item for item in dummy_logger.warning_logs)


def test_http_client_keeps_raw_response_as_default(monkeypatch):
    client = HttpClient(base_url="http://example.com")

    async def _fake_send(request: httpx.Request, **_kwargs):
        return _build_response(request.method, str(request.url), body="raw-body")

    monkeypatch.setattr(client._client, "send", _fake_send)

    result = asyncio.run(client.get("/raw", headers={"Authorization": "Bearer test"}))
    asyncio.run(client.close())

    assert isinstance(result, httpx.Response)
    assert result.text == "raw-body"


def test_http_client_builds_per_request_timeout_without_passing_kwargs_to_send(monkeypatch):
    client = HttpClient(base_url="http://example.com")
    captured_timeout = None

    async def _fake_send(request: httpx.Request):
        nonlocal captured_timeout
        captured_timeout = request.extensions.get("timeout")
        return _build_response(request.method, str(request.url), body="raw-body")

    monkeypatch.setattr(client._client, "send", _fake_send)

    result = asyncio.run(
        client.get(
            "/raw",
            headers={"Authorization": "Bearer test"},
            timeout=7.5,
        )
    )
    asyncio.run(client.close())

    assert isinstance(result, httpx.Response)
    assert captured_timeout is not None
    assert captured_timeout["connect"] == 7.5
    assert captured_timeout["read"] == 7.5
    assert captured_timeout["write"] == 7.5
    assert captured_timeout["pool"] == 7.5


def test_http_client_returns_json_data_when_response_format_json(monkeypatch):
    client = HttpClient(base_url="http://example.com")

    async def _fake_send(request: httpx.Request, **_kwargs):
        return _build_response(
            request.method,
            str(request.url),
            json_body={"code": 200, "message": "ok", "data": {"id": 1, "name": "alice"}},
        )

    monkeypatch.setattr(client._client, "send", _fake_send)

    result = asyncio.run(
        client.get(
            "/json-data",
            headers={"Authorization": "Bearer test"},
            response_format="json",
        )
    )
    asyncio.run(client.close())

    assert result == {"id": 1, "name": "alice"}


def test_http_client_returns_json_envelope_when_enabled(monkeypatch):
    client = HttpClient(base_url="http://example.com")

    async def _fake_send(request: httpx.Request, **_kwargs):
        return _build_response(
            request.method,
            str(request.url),
            json_body={
                "code": 200,
                "message": "ok",
                "data": {"rows": [1, 2]},
                "timestamp": 1730000000000,
            },
        )

    monkeypatch.setattr(client._client, "send", _fake_send)

    result = asyncio.run(
        client.get(
            "/json-envelope",
            headers={"Authorization": "Bearer test"},
            response_format="json",
            include_envelope=True,
        )
    )
    asyncio.run(client.close())

    assert result == {
        "code": 200,
        "message": "ok",
        "data": {"rows": [1, 2]},
        "timestamp": 1730000000000,
    }


def test_http_client_raises_service_exception_for_non_success_business_code(monkeypatch):
    client = HttpClient(base_url="http://example.com")

    async def _fake_send(request: httpx.Request, **_kwargs):
        return _build_response(
            request.method,
            str(request.url),
            json_body={"code": 500, "message": "biz error", "data": None},
        )

    monkeypatch.setattr(client._client, "send", _fake_send)

    with pytest.raises(ServiceException) as exc_info:
        asyncio.run(
            client.get(
                "/biz-error",
                headers={"Authorization": "Bearer test"},
                response_format="json",
                include_envelope=True,
            )
        )

    asyncio.run(client.close())
    assert exc_info.value.code == 500
    assert exc_info.value.message == "biz error"


def test_http_client_injects_system_signature_headers_when_agent_key_enabled(monkeypatch):
    """验证系统签名模式会自动附加签名相关请求头。"""
    monkeypatch.setenv("X_AGENT_KEY", "agent-server")
    monkeypatch.setenv("SYSTEM_AUTH_LOCAL_SECRET", "local-secret-1")
    monkeypatch.setenv("SYSTEM_AUTH_ENABLED", "true")
    monkeypatch.setenv(
        "SYSTEM_AUTH_CLIENTS_JSON",
        json.dumps(
            [
                {
                    "app_id": "biz-server",
                    "secret": "remote-secret-1",
                    "enabled": True,
                }
            ]
        ),
    )
    monkeypatch.setenv("SYSTEM_AUTH_DEFAULT_SIGN_VERSION", "v1")
    clear_system_auth_settings_cache()
    client = HttpClient(base_url="http://example.com")
    captured: dict[str, str] = {}

    async def _fake_send(request: httpx.Request, **_kwargs):
        captured.update(dict(request.headers))
        return _build_response(request.method, str(request.url), body="ok")

    monkeypatch.setattr(client._client, "send", _fake_send)
    asyncio.run(client.get("/signed"))
    asyncio.run(client.close())

    normalized_headers = {key.lower(): value for key, value in captured.items()}
    assert normalized_headers.get("x-agent-key") == "agent-server"
    assert normalized_headers.get("x-agent-sign-version") == "v1"
    assert normalized_headers.get("x-agent-timestamp")
    assert normalized_headers.get("x-agent-nonce")
    assert normalized_headers.get("x-agent-signature")


def test_http_client_skips_system_signature_when_authorization_exists(monkeypatch):
    """验证自动模式下存在用户 Authorization 时不附加系统签名头。"""
    monkeypatch.setenv("X_AGENT_KEY", "agent-server")
    monkeypatch.setenv("SYSTEM_AUTH_LOCAL_SECRET", "local-secret-1")
    monkeypatch.setenv("SYSTEM_AUTH_ENABLED", "true")
    monkeypatch.setenv(
        "SYSTEM_AUTH_CLIENTS_JSON",
        json.dumps(
            [
                {
                    "app_id": "biz-server",
                    "secret": "remote-secret-1",
                    "enabled": True,
                }
            ]
        ),
    )
    clear_system_auth_settings_cache()
    client = HttpClient(base_url="http://example.com")
    captured: dict[str, str] = {}

    async def _fake_send(request: httpx.Request, **_kwargs):
        captured.update(dict(request.headers))
        return _build_response(request.method, str(request.url), body="ok")

    monkeypatch.setattr(client._client, "send", _fake_send)
    asyncio.run(client.get("/user-call", headers={"Authorization": "Bearer user-token"}))
    asyncio.run(client.close())

    normalized_headers = {key.lower(): value for key, value in captured.items()}
    assert normalized_headers.get("authorization") == "Bearer user-token"
    assert "x-agent-key" not in normalized_headers
    assert "x-agent-signature" not in normalized_headers


def test_http_client_can_force_system_signature_when_authorization_exists(monkeypatch):
    """验证可通过请求参数强制附加系统签名头。"""
    monkeypatch.setenv("X_AGENT_KEY", "agent-server")
    monkeypatch.setenv("SYSTEM_AUTH_LOCAL_SECRET", "local-secret-1")
    monkeypatch.setenv("SYSTEM_AUTH_ENABLED", "true")
    monkeypatch.setenv(
        "SYSTEM_AUTH_CLIENTS_JSON",
        json.dumps(
            [
                {
                    "app_id": "biz-server",
                    "secret": "remote-secret-1",
                    "enabled": True,
                }
            ]
        ),
    )
    clear_system_auth_settings_cache()
    client = HttpClient(base_url="http://example.com")
    captured: dict[str, str] = {}

    async def _fake_send(request: httpx.Request, **_kwargs):
        captured.update(dict(request.headers))
        return _build_response(request.method, str(request.url), body="ok")

    monkeypatch.setattr(client._client, "send", _fake_send)
    asyncio.run(
        client.get(
            "/force-signed",
            headers={"Authorization": "Bearer user-token"},
            use_system_signature=True,
        )
    )
    asyncio.run(client.close())

    normalized_headers = {key.lower(): value for key, value in captured.items()}
    assert normalized_headers.get("authorization") == "Bearer user-token"
    assert normalized_headers.get("x-agent-key") == "agent-server"
    assert normalized_headers.get("x-agent-signature")


def test_http_client_rejects_signed_request_when_local_secret_missing(monkeypatch):
    """验证缺少本地签名密钥时禁止发送系统签名请求。"""
    monkeypatch.setenv("X_AGENT_KEY", "agent-server")
    monkeypatch.setenv("SYSTEM_AUTH_ENABLED", "true")
    client = HttpClient(base_url="http://example.com")

    with pytest.raises(ServiceException) as exc_info:
        asyncio.run(client.get("/signed"))

    asyncio.run(client.close())
    assert exc_info.value.message == "系统签名请求缺少 SYSTEM_AUTH_LOCAL_SECRET 配置"
