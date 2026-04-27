from fastapi.testclient import TestClient

import app.main as main_module
from app.main import app
from app.schemas.auth import AuthUser


def _auth_headers() -> dict[str, str]:
    return {"Authorization": "Bearer test-token"}


def _mock_auth(monkeypatch) -> None:
    async def _fake_fetch_current_user() -> AuthUser:
        return AuthUser(id=1, username="tester")

    monkeypatch.setattr(
        main_module,
        "verify_authorization",
        _fake_fetch_current_user,
    )


def test_cors_preflight_allows_localhost_origin():
    client = TestClient(app)

    response = client.options(
        "/image_parse/drug",
        headers={
            "Origin": "http://localhost:3000",
            "Access-Control-Request-Method": "POST",
        },
    )

    assert response.status_code == 200
    assert response.headers["access-control-allow-origin"] == "http://localhost:3000"
    assert response.headers["access-control-allow-credentials"] == "true"


def test_cors_simple_request_allows_127_origin(monkeypatch):
    _mock_auth(monkeypatch)
    client = TestClient(app)

    response = client.post(
        "/image_parse/drug",
        headers={"Origin": "http://localhost:5173", **_auth_headers()},
        json={"images": []},
    )

    assert response.status_code == 400
    assert response.headers["access-control-allow-origin"] == "http://localhost:5173"
    assert response.headers["access-control-allow-credentials"] == "true"


def test_cors_preflight_allows_local_area_network_origin():
    """测试目的：默认 CORS 允许局域网地址发起预检请求。"""
    client = TestClient(app)

    response = client.options(
        "/image_parse/drug",
        headers={
            "Origin": "http://192.168.31.8:3000",
            "Access-Control-Request-Method": "POST",
        },
    )

    assert response.status_code == 200
    assert response.headers["access-control-allow-origin"] == "http://192.168.31.8:3000"
    assert response.headers["access-control-allow-credentials"] == "true"


def test_cors_preflight_allows_zhangyichuang_origin():
    """测试目的：默认 CORS 允许主域名 zhangyichuang.com 发起预检请求。"""
    client = TestClient(app)

    response = client.options(
        "/image_parse/drug",
        headers={
            "Origin": "https://zhangyichuang.com",
            "Access-Control-Request-Method": "POST",
        },
    )

    assert response.status_code == 200
    assert response.headers["access-control-allow-origin"] == "https://zhangyichuang.com"
    assert response.headers["access-control-allow-credentials"] == "true"


def test_cors_preflight_allows_zhangyichuang_subdomain_origin():
    """测试目的：默认 CORS 允许 zhangyichuang.com 任意层级子域名发起预检请求。"""
    client = TestClient(app)

    response = client.options(
        "/image_parse/drug",
        headers={
            "Origin": "https://admin.api.zhangyichuang.com:8443",
            "Access-Control-Request-Method": "POST",
        },
    )

    assert response.status_code == 200
    assert response.headers["access-control-allow-origin"] == "https://admin.api.zhangyichuang.com:8443"
    assert response.headers["access-control-allow-credentials"] == "true"


def test_cors_preflight_rejects_non_local_origin():
    client = TestClient(app)

    response = client.options(
        "/image_parse/drug",
        headers={
            "Origin": "http://evil.com",
            "Access-Control-Request-Method": "POST",
        },
    )

    assert response.status_code == 400
    assert "access-control-allow-origin" not in response.headers


def test_cors_preflight_rejects_fake_zhangyichuang_suffix_origin():
    """测试目的：默认 CORS 不应错误放行伪造的后缀域名。"""
    client = TestClient(app)

    response = client.options(
        "/image_parse/drug",
        headers={
            "Origin": "https://zhangyichuang.com.evil.com",
            "Access-Control-Request-Method": "POST",
        },
    )

    assert response.status_code == 400
    assert "access-control-allow-origin" not in response.headers
