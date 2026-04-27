from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.core.security.anonymous_access import (
    allow_anonymous,
    is_anonymous_endpoint,
    is_anonymous_request,
)


def test_allow_anonymous_marks_endpoint() -> None:
    async def _endpoint() -> dict[str, bool]:
        return {"ok": True}

    marked = allow_anonymous(_endpoint)

    assert marked is _endpoint
    assert is_anonymous_endpoint(_endpoint) is True


def test_is_anonymous_endpoint_returns_false_without_marker() -> None:
    async def _endpoint() -> dict[str, bool]:
        return {"ok": True}

    assert is_anonymous_endpoint(_endpoint) is False


def test_is_anonymous_request_matches_marked_route() -> None:
    app = FastAPI()
    observed: dict[str, bool] = {}

    @app.middleware("http")
    async def _capture_anonymous_status(request, call_next):
        observed[request.url.path] = is_anonymous_request(request)
        return await call_next(request)

    @app.get("/public")
    @allow_anonymous
    async def _public() -> dict[str, bool]:
        return {"ok": True}

    @app.get("/private")
    async def _private() -> dict[str, bool]:
        return {"ok": True}

    client = TestClient(app)

    assert client.get("/public").status_code == 200
    assert client.get("/private").status_code == 200
    assert observed["/public"] is True
    assert observed["/private"] is False
