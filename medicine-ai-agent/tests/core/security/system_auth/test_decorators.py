from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.core.security.system_auth.decorators import (
    allow_system,
    is_system_endpoint,
    is_system_request,
)


def test_allow_system_marks_endpoint() -> None:
    """验证 `allow_system` 会为路由函数打标记。"""

    async def _endpoint() -> dict[str, bool]:
        return {"ok": True}

    marked = allow_system(_endpoint)

    assert marked is _endpoint
    assert is_system_endpoint(_endpoint) is True


def test_is_system_endpoint_returns_false_without_marker() -> None:
    """验证未标注函数不会被判定为系统签名接口。"""

    async def _endpoint() -> dict[str, bool]:
        return {"ok": True}

    assert is_system_endpoint(_endpoint) is False


def test_is_system_request_matches_marked_route() -> None:
    """验证请求命中 `@allow_system` 路由时可被正确识别。"""
    app = FastAPI()
    observed: dict[str, bool] = {}

    @app.middleware("http")
    async def _capture_system_status(request, call_next):
        observed[request.url.path] = is_system_request(request)
        return await call_next(request)

    @app.get("/system")
    @allow_system
    async def _system() -> dict[str, bool]:
        return {"ok": True}

    @app.get("/normal")
    async def _normal() -> dict[str, bool]:
        return {"ok": True}

    client = TestClient(app)

    assert client.get("/system").status_code == 200
    assert client.get("/normal").status_code == 200
    assert observed["/system"] is True
    assert observed["/normal"] is False
