import asyncio

import httpx
import pytest

import app.services.auth_service as auth_service
from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.schemas.auth import AuthUser


def _build_ajax_response(payload: dict) -> httpx.Response:
    request = httpx.Request("GET", "http://test/agent/authorization")
    return httpx.Response(status_code=200, request=request, json=payload)


def test_verify_authorization_success_with_user_roles_permissions(monkeypatch):
    fake_response = _build_ajax_response(
        {
            "code": 200,
            "message": "success",
            "data": {
                "user": {
                    "id": 1,
                    "username": "alice",
                    "phoneNumber": "13800000000",
                    "realName": "Alice",
                },
                "roles": ["super_admin"],
                "permissions": ["system:smart_assistant"],
            },
        }
    )

    class FakeHttpClient:
        def __init__(self, **_kwargs):
            pass

        async def __aenter__(self):
            return self

        async def __aexit__(self, *_args):
            return None

        async def get(self, _url):
            return fake_response

    monkeypatch.setattr(auth_service, "HttpClient", FakeHttpClient)

    result = asyncio.run(auth_service.verify_authorization())

    assert isinstance(result, AuthUser)
    assert result.id == 1
    assert result.phone_number == "13800000000"
    assert result.real_name == "Alice"
    assert result.roles == ["super_admin"]
    assert result.permissions == ["system:smart_assistant"]


def test_verify_authorization_defaults_empty_roles_permissions_when_null(monkeypatch):
    fake_response = _build_ajax_response(
        {
            "code": 200,
            "message": "success",
            "data": {
                "user": {
                    "id": 1,
                    "username": "alice",
                    "phoneNumber": "13800000000",
                    "realName": "Alice",
                },
                "roles": None,
                "permissions": None,
            },
        }
    )

    class FakeHttpClient:
        def __init__(self, **_kwargs):
            pass

        async def __aenter__(self):
            return self

        async def __aexit__(self, *_args):
            return None

        async def get(self, _url):
            return fake_response

    monkeypatch.setattr(auth_service, "HttpClient", FakeHttpClient)

    result = asyncio.run(auth_service.verify_authorization())

    assert isinstance(result, AuthUser)
    assert result.id == 1
    assert result.roles == []
    assert result.permissions == []


def test_verify_authorization_maps_401(monkeypatch):
    fake_response = _build_ajax_response(
        {
            "code": 401,
            "message": "token invalid",
            "data": None,
        }
    )

    class FakeHttpClient:
        def __init__(self, **_kwargs):
            pass

        async def __aenter__(self):
            return self

        async def __aexit__(self, *_args):
            return None

        async def get(self, _url):
            return fake_response

    monkeypatch.setattr(auth_service, "HttpClient", FakeHttpClient)

    with pytest.raises(ServiceException) as exc_info:
        asyncio.run(auth_service.verify_authorization())

    assert exc_info.value.code == ResponseCode.UNAUTHORIZED.code
    assert exc_info.value.message == "token invalid"


def test_verify_authorization_keeps_401x_code(monkeypatch):
    fake_response = _build_ajax_response(
        {
            "code": 4011,
            "message": "访问令牌已过期",
            "data": None,
        }
    )

    class FakeHttpClient:
        def __init__(self, **_kwargs):
            pass

        async def __aenter__(self):
            return self

        async def __aexit__(self, *_args):
            return None

        async def get(self, _url):
            return fake_response

    monkeypatch.setattr(auth_service, "HttpClient", FakeHttpClient)

    with pytest.raises(ServiceException) as exc_info:
        asyncio.run(auth_service.verify_authorization())

    assert exc_info.value.code == 4011
    assert exc_info.value.message == "访问令牌已过期"


def test_verify_authorization_maps_upstream_500_to_503(monkeypatch):
    fake_response = _build_ajax_response(
        {
            "code": 500,
            "message": "server error",
            "data": None,
        }
    )

    class FakeHttpClient:
        def __init__(self, **_kwargs):
            pass

        async def __aenter__(self):
            return self

        async def __aexit__(self, *_args):
            return None

        async def get(self, _url):
            return fake_response

    monkeypatch.setattr(auth_service, "HttpClient", FakeHttpClient)

    with pytest.raises(ServiceException) as exc_info:
        asyncio.run(auth_service.verify_authorization())

    assert exc_info.value.code == 503
    assert exc_info.value.message == auth_service.AUTH_SERVICE_UNAVAILABLE_MESSAGE


def test_verify_authorization_maps_network_error_to_503(monkeypatch):
    class FakeHttpClient:
        def __init__(self, **_kwargs):
            pass

        async def __aenter__(self):
            return self

        async def __aexit__(self, *_args):
            return None

        async def get(self, _url):
            raise httpx.ConnectError("connect failed")

    monkeypatch.setattr(auth_service, "HttpClient", FakeHttpClient)

    with pytest.raises(ServiceException) as exc_info:
        asyncio.run(auth_service.verify_authorization())

    assert exc_info.value.code == 503


def test_verify_authorization_maps_non_json_response_to_503(monkeypatch):
    request = httpx.Request("GET", "http://test/agent/authorization")
    fake_response = httpx.Response(
        status_code=500,
        request=request,
        text="<!doctype html><html><body><h1>HTTP Status 500</h1></body></html>",
        headers={"content-type": "text/html"},
    )

    class FakeHttpClient:
        def __init__(self, **_kwargs):
            pass

        async def __aenter__(self):
            return self

        async def __aexit__(self, *_args):
            return None

        async def get(self, _url):
            return fake_response

    monkeypatch.setattr(auth_service, "HttpClient", FakeHttpClient)

    with pytest.raises(ServiceException) as exc_info:
        asyncio.run(auth_service.verify_authorization())

    assert exc_info.value.code == ResponseCode.SERVICE_UNAVAILABLE.code
    assert exc_info.value.message == auth_service.AUTH_SERVICE_UNAVAILABLE_MESSAGE


def test_verify_authorization_maps_invalid_roles_to_503(monkeypatch):
    fake_response = _build_ajax_response(
        {
            "code": 200,
            "message": "success",
            "data": {
                "user": {
                    "id": 1,
                    "username": "alice",
                },
                "roles": "super_admin",
                "permissions": ["system:smart_assistant"],
            },
        }
    )

    class FakeHttpClient:
        def __init__(self, **_kwargs):
            pass

        async def __aenter__(self):
            return self

        async def __aexit__(self, *_args):
            return None

        async def get(self, _url):
            return fake_response

    monkeypatch.setattr(auth_service, "HttpClient", FakeHttpClient)

    with pytest.raises(ServiceException) as exc_info:
        asyncio.run(auth_service.verify_authorization())

    assert exc_info.value.code == 503


def test_verify_authorization_maps_null_user_to_503(monkeypatch):
    fake_response = _build_ajax_response(
        {
            "code": 200,
            "message": "success",
            "data": {
                "user": None,
                "roles": None,
                "permissions": None,
            },
        }
    )

    class FakeHttpClient:
        def __init__(self, **_kwargs):
            pass

        async def __aenter__(self):
            return self

        async def __aexit__(self, *_args):
            return None

        async def get(self, _url):
            return fake_response

    monkeypatch.setattr(auth_service, "HttpClient", FakeHttpClient)

    with pytest.raises(ServiceException) as exc_info:
        asyncio.run(auth_service.verify_authorization())

    assert exc_info.value.code == 503


def test_verify_authorization_maps_invalid_data_to_503(monkeypatch):
    fake_response = _build_ajax_response(
        {
            "code": 200,
            "message": "success",
            "data": {
                "username": "alice",
            },
        }
    )

    class FakeHttpClient:
        def __init__(self, **_kwargs):
            pass

        async def __aenter__(self):
            return self

        async def __aexit__(self, *_args):
            return None

        async def get(self, _url):
            return fake_response

    monkeypatch.setattr(auth_service, "HttpClient", FakeHttpClient)

    with pytest.raises(ServiceException) as exc_info:
        asyncio.run(auth_service.verify_authorization())

    assert exc_info.value.code == 503
