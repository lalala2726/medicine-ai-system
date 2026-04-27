from __future__ import annotations

from contextvars import ContextVar, Token
from typing import Optional

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.schemas.auth import AuthUser

_authorization_header: ContextVar[Optional[str]] = ContextVar(
    "authorization_header",
    default=None,
)
_current_user: ContextVar[AuthUser | None] = ContextVar(
    "current_user",
    default=None,
)


def set_authorization_header(value: Optional[str]) -> Token:
    """
    在当前请求上下文中写入 Authorization 头。

    该函数通常在 HTTP 中间件入口调用，用于把请求头写入 `ContextVar`，
    供后续服务与客户端封装按需读取。

    Args:
        value: 当前请求中的 Authorization 原始值，允许为 None。

    Returns:
        Token: `ContextVar.set(...)` 返回的令牌，用于后续 reset。
    """
    return _authorization_header.set(value)


def reset_authorization_header(token: Token) -> None:
    """
    重置 Authorization 上下文，避免跨请求污染。

    Args:
        token: 由 `set_authorization_header` 返回的上下文令牌。

    Returns:
        None
    """
    _authorization_header.reset(token)


def get_authorization_header() -> str:
    """
    获取当前请求上下文中的 Authorization 头。

    Returns:
        str: Authorization 原始值。

    Raises:
        ServiceException: 当上下文中不存在 Authorization 时抛出 UNAUTHORIZED。
    """
    authorization = _authorization_header.get()
    if authorization is None:
        raise ServiceException(
            code=ResponseCode.UNAUTHORIZED,
            message="缺少 Authorization 请求头",
        )
    return authorization


def set_current_user(value: AuthUser | None) -> Token:
    """
    在当前请求上下文中写入认证用户信息。

    Args:
        value: 已认证用户对象；无用户时可传 None。

    Returns:
        Token: `ContextVar.set(...)` 返回的令牌，用于后续 reset。
    """
    return _current_user.set(value)


def reset_current_user(token: Token) -> None:
    """
    重置用户上下文，避免跨请求污染。

    Args:
        token: 由 `set_current_user` 返回的上下文令牌。

    Returns:
        None
    """
    _current_user.reset(token)


def get_current_user() -> AuthUser:
    """
    获取当前请求上下文中的认证用户。

    Returns:
        AuthUser: 当前请求绑定的认证用户。

    Raises:
        ServiceException: 当请求上下文中没有用户信息时抛出 UNAUTHORIZED。
    """
    user = _current_user.get()
    if user is None:
        raise ServiceException(
            code=ResponseCode.UNAUTHORIZED,
            message="当前请求未认证",
        )
    return user


def get_current_user_id() -> int:
    """
    获取当前请求上下文中的用户 ID。

    Returns:
        int: 当前用户 ID。

    Raises:
        ServiceException: 当请求上下文中没有用户信息时透传异常。
    """
    return get_current_user().id


def get_current_token() -> str:
    """
    解析并返回当前请求中的 Bearer token。

    约束格式：`Authorization: Bearer <token>`。

    Returns:
        str: Bearer token 去空格后的值。

    Raises:
        ServiceException: 当 Authorization 头缺失或格式非法时抛出 UNAUTHORIZED。
    """
    authorization = get_authorization_header().strip()
    parts = authorization.split(" ", 1)
    if len(parts) != 2 or parts[0].lower() != "bearer" or not parts[1].strip():
        raise ServiceException(
            code=ResponseCode.UNAUTHORIZED,
            message="无效 Authorization 请求头",
        )
    return parts[1].strip()


def get_user() -> AuthUser:
    """
    `get_current_user` 的简写别名。

    Returns:
        AuthUser: 当前请求上下文中的认证用户。
    """
    return get_current_user()


def get_user_id() -> int:
    """
    `get_current_user_id` 的简写别名。

    Returns:
        int: 当前请求上下文中的用户 ID。
    """
    return get_current_user_id()
