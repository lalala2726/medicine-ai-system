import pytest

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.core.security.auth_context import (
    get_authorization_header,
    get_current_token,
    get_current_user,
    get_current_user_id,
    get_user,
    get_user_id,
    reset_authorization_header,
    reset_current_user,
    set_authorization_header,
    set_current_user,
)
from app.schemas.auth import AuthUser


def test_current_user_context_set_get_and_reset():
    token = set_current_user(AuthUser(id=11, username="tester"))
    try:
        assert get_current_user().id == 11
        assert get_current_user_id() == 11
        assert get_user().id == 11
        assert get_user_id() == 11
    finally:
        reset_current_user(token)

    with pytest.raises(ServiceException) as exc_info:
        get_current_user()
    assert exc_info.value.code == ResponseCode.UNAUTHORIZED.code


def test_authorization_header_set_get_and_reset():
    token = set_authorization_header("Bearer abc")
    try:
        assert get_authorization_header() == "Bearer abc"
    finally:
        reset_authorization_header(token)

    with pytest.raises(ServiceException) as exc_info:
        get_authorization_header()
    assert exc_info.value.code == ResponseCode.UNAUTHORIZED.code


def test_get_current_token_parses_bearer_token():
    token = set_authorization_header("Bearer token-value")
    try:
        assert get_current_token() == "token-value"
    finally:
        reset_authorization_header(token)


def test_get_current_token_rejects_invalid_authorization_header():
    token = set_authorization_header("Token abc")
    try:
        with pytest.raises(ServiceException) as exc_info:
            get_current_token()
        assert exc_info.value.code == ResponseCode.UNAUTHORIZED.code
    finally:
        reset_authorization_header(token)
