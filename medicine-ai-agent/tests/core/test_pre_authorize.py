import asyncio

import pytest

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.core.security.auth_context import reset_current_user, set_current_user
from app.core.security.pre_authorize import RoleCode, has_permission, has_role, pre_authorize
from app.schemas.auth import AuthUser


def test_pre_authorize_allows_when_has_role():
    token = set_current_user(
        AuthUser(id=1, username="tester", roles=[RoleCode.ADMIN.value])
    )
    try:
        called = {"value": False}

        @pre_authorize(lambda: has_role(RoleCode.ADMIN))
        async def _handler() -> str:
            called["value"] = True
            return "ok"

        result = asyncio.run(_handler())
        assert result == "ok"
        assert called["value"] is True
    finally:
        reset_current_user(token)


def test_pre_authorize_allows_when_has_permission():
    token = set_current_user(
        AuthUser(
            id=1,
            username="tester",
            permissions=["system:smart_assistant"],
        )
    )
    try:
        called = {"value": False}

        @pre_authorize(lambda: has_permission("system:smart_assistant"))
        def _handler() -> str:
            called["value"] = True
            return "ok"

        result = _handler()
        assert result == "ok"
        assert called["value"] is True
    finally:
        reset_current_user(token)


def test_pre_authorize_or_expression_allows_when_any_match():
    token = set_current_user(
        AuthUser(
            id=1,
            username="tester",
            permissions=["system:smart_assistant"],
        )
    )
    try:
        @pre_authorize(
            lambda: has_role(RoleCode.ADMIN)
                    or has_permission("system:smart_assistant")
        )
        def _handler() -> str:
            return "ok"

        assert _handler() == "ok"
    finally:
        reset_current_user(token)


def test_pre_authorize_and_expression_rejects_when_condition_not_met():
    token = set_current_user(
        AuthUser(
            id=1,
            username="tester",
            roles=[RoleCode.ADMIN.value],
            permissions=[],
        )
    )
    try:
        @pre_authorize(
            lambda: has_role(RoleCode.ADMIN)
                    and has_permission("system:smart_assistant")
        )
        def _handler() -> str:
            return "ok"

        with pytest.raises(ServiceException) as exc_info:
            _handler()
        assert exc_info.value.code == ResponseCode.FORBIDDEN.code
    finally:
        reset_current_user(token)


def test_pre_authorize_raises_unauthorized_without_user_context():
    @pre_authorize(lambda: has_role(RoleCode.ADMIN))
    def _handler() -> str:
        return "ok"

    with pytest.raises(ServiceException) as exc_info:
        _handler()
    assert exc_info.value.code == ResponseCode.UNAUTHORIZED.code


def test_pre_authorize_requires_callable_predicate():
    with pytest.raises(TypeError):
        pre_authorize(True)  # type: ignore[arg-type]
