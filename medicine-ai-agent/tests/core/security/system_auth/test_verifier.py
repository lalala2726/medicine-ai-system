import asyncio
from collections.abc import Iterable

import pytest
from starlette.requests import Request

from app.core.exception.exceptions import ServiceException
from app.core.security.system_auth.canonical import build_canonical_string
from app.core.security.system_auth.config import SystemAuthClientConfig, SystemAuthSettings
from app.core.security.system_auth.constants import (
    HEADER_X_AGENT_KEY,
    HEADER_X_AGENT_NONCE,
    HEADER_X_AGENT_SIGNATURE,
    HEADER_X_AGENT_SIGN_VERSION,
    HEADER_X_AGENT_TIMESTAMP,
)
from app.core.security.system_auth.signer import sign_hmac_sha256_base64url
from app.core.security.system_auth.verifier import verify_system_request


def _build_request(
        *,
        method: str,
        path: str,
        headers: dict[str, str],
        query_string: str = "",
        body: bytes = b"",
) -> Request:
    scope = {
        "type": "http",
        "method": method.upper(),
        "path": path,
        "query_string": query_string.encode("utf-8"),
        "headers": [
            (key.lower().encode("utf-8"), value.encode("utf-8"))
            for key, value in headers.items()
        ],
    }
    sent = {"done": False}

    async def _receive():
        if sent["done"]:
            return {"type": "http.request", "body": b"", "more_body": False}
        sent["done"] = True
        return {"type": "http.request", "body": body, "more_body": False}

    return Request(scope, _receive)


def _settings(*, client_enabled: bool = True) -> SystemAuthSettings:
    return SystemAuthSettings(
        enabled=True,
        max_skew_seconds=300,
        nonce_ttl_seconds=600,
        nonce_key_prefix="system_auth:nonce",
        default_sign_version="v1",
        local_secret="local-secret",
        clients={
            "biz-server": SystemAuthClientConfig(
                app_id="biz-server",
                secret="secret-1",
                enabled=client_enabled,
            )
        },
    )


def _signed_headers(
        *,
        method: str,
        path: str,
        query_pairs: Iterable[tuple[str, str]],
        body: bytes,
        timestamp: int,
        nonce: str,
        secret: str,
) -> dict[str, str]:
    canonical = build_canonical_string(
        method=method,
        path=path,
        query_pairs=query_pairs,
        timestamp=timestamp,
        nonce=nonce,
        body_bytes=body,
    )
    signature = sign_hmac_sha256_base64url(
        secret=secret,
        canonical_string=canonical,
    )
    return {
        HEADER_X_AGENT_KEY: "biz-server",
        HEADER_X_AGENT_TIMESTAMP: str(timestamp),
        HEADER_X_AGENT_NONCE: nonce,
        HEADER_X_AGENT_SIGNATURE: signature,
        HEADER_X_AGENT_SIGN_VERSION: "v1",
    }


def test_verify_system_request_success(monkeypatch) -> None:
    """验证合法签名请求可通过系统鉴权。"""
    monkeypatch.setattr(
        "app.core.security.system_auth.verifier.time.time",
        lambda: 1772700050,
    )
    monkeypatch.setattr(
        "app.core.security.system_auth.verifier.reserve_nonce",
        lambda **_kwargs: True,
    )
    headers = _signed_headers(
        method="GET",
        path="/knowledge_base/document/chunks/list",
        query_pairs=[("document_id", "1"), ("knowledge_name", "demo"), ("page", "1")],
        body=b"",
        timestamp=1772700000,
        nonce="nonce-12345678",
        secret="secret-1",
    )
    request = _build_request(
        method="GET",
        path="/knowledge_base/document/chunks/list",
        headers=headers,
        query_string="document_id=1&knowledge_name=demo&page=1",
    )

    principal = asyncio.run(verify_system_request(request, settings=_settings()))

    assert principal.app_id == "biz-server"
    assert principal.sign_version == "v1"


def test_verify_system_request_rejects_missing_headers() -> None:
    """验证缺少签名头时返回 401。"""
    request = _build_request(
        method="GET",
        path="/knowledge_base/document/chunks/list",
        headers={},
    )
    with pytest.raises(ServiceException) as exc_info:
        asyncio.run(verify_system_request(request, settings=_settings()))
    assert int(exc_info.value.code) == 401


def test_verify_system_request_rejects_bad_signature(monkeypatch) -> None:
    """验证签名不匹配返回 401。"""
    monkeypatch.setattr(
        "app.core.security.system_auth.verifier.time.time",
        lambda: 1772700050,
    )
    headers = _signed_headers(
        method="GET",
        path="/knowledge_base/document/chunks/list",
        query_pairs=[],
        body=b"",
        timestamp=1772700000,
        nonce="nonce-12345678",
        secret="secret-1",
    )
    headers[HEADER_X_AGENT_SIGNATURE] = "bad-signature"
    request = _build_request(
        method="GET",
        path="/knowledge_base/document/chunks/list",
        headers=headers,
    )
    with pytest.raises(ServiceException) as exc_info:
        asyncio.run(verify_system_request(request, settings=_settings()))
    assert int(exc_info.value.code) == 401


def test_verify_system_request_rejects_expired_timestamp(monkeypatch) -> None:
    """验证超出时间窗口的请求返回 401。"""
    monkeypatch.setattr(
        "app.core.security.system_auth.verifier.time.time",
        lambda: 1772700900,
    )
    headers = _signed_headers(
        method="GET",
        path="/knowledge_base/document/chunks/list",
        query_pairs=[],
        body=b"",
        timestamp=1772700000,
        nonce="nonce-12345678",
        secret="secret-1",
    )
    request = _build_request(
        method="GET",
        path="/knowledge_base/document/chunks/list",
        headers=headers,
    )
    with pytest.raises(ServiceException) as exc_info:
        asyncio.run(verify_system_request(request, settings=_settings()))
    assert int(exc_info.value.code) == 401


def test_verify_system_request_rejects_replay(monkeypatch) -> None:
    """验证 nonce 重放会返回 401。"""
    monkeypatch.setattr(
        "app.core.security.system_auth.verifier.time.time",
        lambda: 1772700050,
    )
    monkeypatch.setattr(
        "app.core.security.system_auth.verifier.reserve_nonce",
        lambda **_kwargs: False,
    )
    headers = _signed_headers(
        method="GET",
        path="/knowledge_base/document/chunks/list",
        query_pairs=[],
        body=b"",
        timestamp=1772700000,
        nonce="nonce-12345678",
        secret="secret-1",
    )
    request = _build_request(
        method="GET",
        path="/knowledge_base/document/chunks/list",
        headers=headers,
    )
    with pytest.raises(ServiceException) as exc_info:
        asyncio.run(verify_system_request(request, settings=_settings()))
    assert int(exc_info.value.code) == 401


def test_verify_system_request_rejects_disabled_client(monkeypatch) -> None:
    """验证禁用客户端返回 403。"""
    monkeypatch.setattr(
        "app.core.security.system_auth.verifier.time.time",
        lambda: 1772700050,
    )
    headers = _signed_headers(
        method="GET",
        path="/knowledge_base/document/chunks/list",
        query_pairs=[],
        body=b"",
        timestamp=1772700000,
        nonce="nonce-12345678",
        secret="secret-1",
    )
    request = _build_request(
        method="GET",
        path="/knowledge_base/document/chunks/list",
        headers=headers,
    )
    with pytest.raises(ServiceException) as exc_info:
        asyncio.run(verify_system_request(request, settings=_settings(client_enabled=False)))
    assert int(exc_info.value.code) == 403
