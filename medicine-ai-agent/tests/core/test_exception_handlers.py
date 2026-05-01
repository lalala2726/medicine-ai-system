import json

import anyio
from fastapi.exceptions import RequestValidationError
from starlette.exceptions import HTTPException as StarletteHTTPException
from starlette.requests import Request
from starlette.websockets import WebSocket

from app.core.codes import ResponseCode
from app.core.exception.exception_handlers import ExceptionHandlers
from app.core.exception.exceptions import ServiceException


def _build_request(path: str) -> Request:
    return Request({"type": "http", "method": "POST", "path": path, "headers": []})


def _build_websocket(path: str) -> WebSocket:
    async def _receive() -> dict:
        return {"type": "websocket.disconnect", "code": 1000}

    async def _send(_message: dict) -> None:
        return None

    return WebSocket(
        {
            "type": "websocket",
            "path": path,
            "headers": [],
            "query_string": b"",
            "client": ("127.0.0.1", 12345),
            "server": ("testserver", 80),
            "scheme": "ws",
            "subprotocols": [],
        },
        receive=_receive,
        send=_send,
    )


def _install_log_spy(monkeypatch) -> list[dict]:
    calls: list[dict] = []

    def _fake_log_exception(*, request, exc, category) -> None:
        calls.append(
            {
                "request": request,
                "exc": exc,
                "category": category,
            }
        )

    monkeypatch.setattr(
        ExceptionHandlers,
        "_log_exception",
        staticmethod(_fake_log_exception),
    )
    return calls


def test_request_validation_exception_handler():
    exc = RequestValidationError(
        [
            {
                "type": "missing",
                "loc": ("body", "image_urls"),
                "msg": "Field required",
                "input": {"imageUrl": ["https://example.com/a.png"]},
            }
        ]
    )

    response = anyio.run(ExceptionHandlers.request_validation_exception_handler, None, exc)

    assert response.status_code == ResponseCode.BAD_REQUEST
    body = json.loads(response.body)
    assert body["code"] == ResponseCode.BAD_REQUEST
    assert body["message"] == "Validation Failed"
    assert body["errors"][0]["field"] == "image_urls"
    assert body["errors"][0]["message"] == "Field required"
    assert body["errors"][0]["type"] == "missing"


def test_service_exception_handler_known_code():
    exc = ServiceException(message="bad", code=ResponseCode.BAD_REQUEST, data={"a": 1})

    response = anyio.run(ExceptionHandlers.service_exception_handler, None, exc)

    assert response.status_code == ResponseCode.BAD_REQUEST
    body = json.loads(response.body)
    assert body["code"] == ResponseCode.BAD_REQUEST
    assert body["message"] == "bad"
    assert body["data"] == {"a": 1}


def test_service_exception_handler_passes_exception_headers():
    exc = ServiceException(message="limited", code=ResponseCode.TOO_MANY_REQUESTS)
    exc.headers = {
        "Retry-After": "12",
        "X-RateLimit-Limit": "10",
    }

    response = anyio.run(ExceptionHandlers.service_exception_handler, None, exc)

    assert response.status_code == ResponseCode.TOO_MANY_REQUESTS
    assert response.headers["retry-after"] == "12"
    assert response.headers["x-ratelimit-limit"] == "10"


def test_service_exception_handler_unknown_code():
    exc = ServiceException(message="custom", code=422)

    response = anyio.run(ExceptionHandlers.service_exception_handler, None, exc)

    assert response.status_code == 422
    body = json.loads(response.body)
    assert body["code"] == 422
    assert body["message"] == "custom"


def test_service_exception_handler_non_http_business_code_falls_back_status():
    exc = ServiceException(message="token expired", code=4011)

    response = anyio.run(ExceptionHandlers.service_exception_handler, None, exc)

    assert response.status_code == ResponseCode.BAD_REQUEST
    body = json.loads(response.body)
    assert body["code"] == 4011
    assert body["message"] == "token expired"


def test_http_exception_handler_known_code():
    exc = StarletteHTTPException(status_code=404, detail="not found")

    response = anyio.run(
        ExceptionHandlers.http_exception_handler,
        _build_request("/missing"),
        exc,
    )

    assert response.status_code == 404
    body = json.loads(response.body)
    assert body["code"] == 404
    assert body["message"] == "路由 /missing 不存在"


def test_http_exception_handler_unknown_code():
    exc = StarletteHTTPException(status_code=418, detail="teapot")

    response = anyio.run(
        ExceptionHandlers.http_exception_handler,
        _build_request("/teapot"),
        exc,
    )

    assert response.status_code == 418
    body = json.loads(response.body)
    assert body["code"] == 418
    assert body["message"] == "teapot"


def test_unhandled_exception_handler():
    response = anyio.run(ExceptionHandlers.unhandled_exception_handler, None, Exception("boom"))

    assert response.status_code == ResponseCode.INTERNAL_ERROR
    body = json.loads(response.body)
    assert body["code"] == ResponseCode.INTERNAL_ERROR
    assert body["message"] == ResponseCode.INTERNAL_ERROR.message


def test_build_request_context_handles_none_request():
    context = ExceptionHandlers._build_request_context(None)
    assert context == "method=unknown path=unknown client=unknown"


def test_build_request_context_handles_websocket():
    websocket = _build_websocket("/ws/speech/stt/stream")
    context = ExceptionHandlers._build_request_context(websocket)
    assert context == "method=websocket path=/ws/speech/stt/stream client=127.0.0.1:12345"


def test_request_validation_exception_handler_logs_detail(monkeypatch):
    calls = _install_log_spy(monkeypatch)
    exc = RequestValidationError(
        [
            {
                "type": "missing",
                "loc": ("body", "question"),
                "msg": "Field required",
                "input": {},
            }
        ]
    )

    response = anyio.run(
        ExceptionHandlers.request_validation_exception_handler,
        None,
        exc,
    )

    assert response.status_code == ResponseCode.BAD_REQUEST
    assert len(calls) == 1
    assert calls[0]["category"] == "validation"
    assert calls[0]["request"] is None
    assert calls[0]["exc"] is exc


def test_service_exception_handler_logs_detail(monkeypatch):
    calls = _install_log_spy(monkeypatch)
    exc = ServiceException(message="bad", code=ResponseCode.BAD_REQUEST)

    response = anyio.run(ExceptionHandlers.service_exception_handler, None, exc)

    assert response.status_code == ResponseCode.BAD_REQUEST
    assert len(calls) == 1
    assert calls[0]["category"] == "service"
    assert calls[0]["request"] is None
    assert calls[0]["exc"] is exc


def test_http_exception_handler_logs_detail(monkeypatch):
    calls = _install_log_spy(monkeypatch)
    request = _build_request("/missing")
    exc = StarletteHTTPException(status_code=404, detail="not found")

    response = anyio.run(ExceptionHandlers.http_exception_handler, request, exc)

    assert response.status_code == 404
    assert len(calls) == 1
    assert calls[0]["category"] == "http"
    assert calls[0]["request"] is request
    assert calls[0]["exc"] is exc


def test_unhandled_exception_handler_logs_detail(monkeypatch):
    calls = _install_log_spy(monkeypatch)
    exc = Exception("boom")

    response = anyio.run(ExceptionHandlers.unhandled_exception_handler, None, exc)

    assert response.status_code == ResponseCode.INTERNAL_ERROR
    assert len(calls) == 1
    assert calls[0]["category"] == "unhandled"
    assert calls[0]["request"] is None
    assert calls[0]["exc"] is exc
