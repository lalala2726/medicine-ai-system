import json
from typing import Any

from fastapi.testclient import TestClient

import app.main as main_module
from app.api.routes import knowledge_base as knowledge_base_route
from app.core.security.system_auth.canonical import build_canonical_string
from app.core.security.system_auth.config import clear_system_auth_settings_cache
from app.core.security.system_auth.constants import (
    HEADER_X_AGENT_KEY,
    HEADER_X_AGENT_NONCE,
    HEADER_X_AGENT_SIGNATURE,
    HEADER_X_AGENT_SIGN_VERSION,
    HEADER_X_AGENT_TIMESTAMP,
)
from app.core.security.system_auth.signer import sign_hmac_sha256_base64url
from app.main import app


def _mock_route_data(monkeypatch, rows: list[dict[str, Any]] | None = None, total: int = 1) -> None:
    """模拟切片查询服务返回，避免依赖真实 Milvus。"""
    monkeypatch.setattr(
        knowledge_base_route,
        "list_knowledge_chunks",
        lambda **_kwargs: (rows or [{"chunk_index": 1, "content": "A"}], total),
    )


def _prepare_system_auth_env(monkeypatch) -> None:
    """注入系统签名配置并清理缓存。"""
    monkeypatch.setenv("SYSTEM_AUTH_ENABLED", "true")
    monkeypatch.setenv("SYSTEM_AUTH_MAX_SKEW_SECONDS", "300")
    monkeypatch.setenv("SYSTEM_AUTH_NONCE_TTL_SECONDS", "600")
    monkeypatch.setenv("SYSTEM_AUTH_NONCE_KEY_PREFIX", "system_auth:nonce")
    monkeypatch.setenv(
        "SYSTEM_AUTH_CLIENTS_JSON",
        json.dumps(
            [
                {
                    "app_id": "biz-server",
                    "secret": "secret-1",
                    "enabled": True,
                }
            ]
        ),
    )
    monkeypatch.setenv("SYSTEM_AUTH_DEFAULT_SIGN_VERSION", "v1")
    clear_system_auth_settings_cache()


def _signed_headers(
        *,
        path: str,
        params: dict[str, Any],
        timestamp: int,
        nonce: str,
        secret: str,
) -> dict[str, str]:
    query_pairs = [(str(key), str(value)) for key, value in params.items()]
    canonical = build_canonical_string(
        method="GET",
        path=path,
        query_pairs=query_pairs,
        timestamp=timestamp,
        nonce=nonce,
        body_bytes=b"",
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


def test_chunks_route_allows_valid_system_signature_and_skips_user_auth(monkeypatch) -> None:
    """验证有效系统签名可访问，且不会触发用户鉴权。"""
    _prepare_system_auth_env(monkeypatch)
    _mock_route_data(monkeypatch)
    monkeypatch.setattr(
        "app.core.security.system_auth.verifier.time.time",
        lambda: 1772700050,
    )
    monkeypatch.setattr(
        "app.core.security.system_auth.verifier.reserve_nonce",
        lambda **_kwargs: True,
    )

    async def _unexpected_verify_authorization():
        raise AssertionError("allow_system 路由不应调用 verify_authorization")

    monkeypatch.setattr(main_module, "verify_authorization", _unexpected_verify_authorization)
    client = TestClient(app)
    params = {
        "knowledge_name": "demo_kb",
        "document_id": 1,
        "page": 1,
    }
    headers = _signed_headers(
        path="/knowledge_base/document/chunks/list",
        params=params,
        timestamp=1772700000,
        nonce="nonce-12345678",
        secret="secret-1",
    )

    response = client.get(
        "/knowledge_base/document/chunks/list",
        params=params,
        headers=headers,
    )

    assert response.status_code == 200
    body = response.json()
    assert body["code"] == 200
    assert body["data"]["rows"][0]["chunk_index"] == 1


def test_chunks_route_rejects_request_without_signature_headers(monkeypatch) -> None:
    """验证无签名头请求返回 401。"""
    _prepare_system_auth_env(monkeypatch)
    _mock_route_data(monkeypatch)
    client = TestClient(app)

    response = client.get(
        "/knowledge_base/document/chunks/list",
        params={
            "knowledge_name": "demo_kb",
            "document_id": 1,
            "page": 1,
        },
    )

    assert response.status_code == 401
    body = response.json()
    assert body["code"] == 401


def test_chunks_route_rejects_bad_signature(monkeypatch) -> None:
    """验证签名不匹配返回 401。"""
    _prepare_system_auth_env(monkeypatch)
    _mock_route_data(monkeypatch)
    monkeypatch.setattr(
        "app.core.security.system_auth.verifier.time.time",
        lambda: 1772700050,
    )
    monkeypatch.setattr(
        "app.core.security.system_auth.verifier.reserve_nonce",
        lambda **_kwargs: True,
    )
    client = TestClient(app)
    params = {
        "knowledge_name": "demo_kb",
        "document_id": 1,
        "page": 1,
    }
    headers = _signed_headers(
        path="/knowledge_base/document/chunks/list",
        params=params,
        timestamp=1772700000,
        nonce="nonce-12345678",
        secret="secret-1",
    )
    headers[HEADER_X_AGENT_SIGNATURE] = "bad-signature"

    response = client.get(
        "/knowledge_base/document/chunks/list",
        params=params,
        headers=headers,
    )

    assert response.status_code == 401
    body = response.json()
    assert body["code"] == 401


def test_chunks_route_rejects_replayed_nonce(monkeypatch) -> None:
    """验证 nonce 重放返回 401。"""
    _prepare_system_auth_env(monkeypatch)
    _mock_route_data(monkeypatch)
    monkeypatch.setattr(
        "app.core.security.system_auth.verifier.time.time",
        lambda: 1772700050,
    )
    monkeypatch.setattr(
        "app.core.security.system_auth.verifier.reserve_nonce",
        lambda **_kwargs: False,
    )
    client = TestClient(app)
    params = {
        "knowledge_name": "demo_kb",
        "document_id": 1,
        "page": 1,
    }
    headers = _signed_headers(
        path="/knowledge_base/document/chunks/list",
        params=params,
        timestamp=1772700000,
        nonce="nonce-12345678",
        secret="secret-1",
    )

    response = client.get(
        "/knowledge_base/document/chunks/list",
        params=params,
        headers=headers,
    )

    assert response.status_code == 401
    body = response.json()
    assert body["code"] == 401
