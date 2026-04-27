from __future__ import annotations

import inspect
from functools import wraps
from typing import Callable

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.core.security.auth_context import get_current_user
from app.core.security.role_codes import RoleCode

FORBIDDEN_MESSAGE = "无权限访问此接口"
INVALID_PREDICATE_MESSAGE = (
    "pre_authorize 需要可调用对象，请使用 lambda，例如："
    "pre_authorize(lambda: has_role(RoleCode.ADMIN) or "
    'has_permission("system:smart_assistant"))'
)


def has_role(role_code: str | RoleCode) -> bool:
    """
    校验当前用户是否拥有指定角色（严格精确匹配）。

    Args:
        role_code: 目标角色编码，可传字符串或 `RoleCode` 枚举值。

    Returns:
        bool: 命中返回 True，否则返回 False。

    Raises:
        ServiceException: 当当前请求未认证时由用户上下文读取逻辑抛出。
    """
    resolved_role = role_code.value if isinstance(role_code, RoleCode) else role_code
    return resolved_role in get_current_user().roles


def has_permission(permission_code: str) -> bool:
    """
    校验当前用户是否拥有指定权限（严格精确匹配）。

    Args:
        permission_code: 权限编码，例如 `system:smart_assistant`。

    Returns:
        bool: 命中返回 True，否则返回 False。

    Raises:
        ServiceException: 当当前请求未认证时由用户上下文读取逻辑抛出。
    """
    return permission_code in get_current_user().permissions


def _check_predicate(predicate: Callable[[], bool]) -> None:
    """
    执行并校验权限表达式结果。

    约束：
    1. predicate 必须同步执行；
    2. predicate 返回值必须为 bool；
    3. 返回 False 时抛出 FORBIDDEN。

    Args:
        predicate: 权限判断函数，通常是 lambda 组合表达式。

    Returns:
        None

    Raises:
        TypeError: predicate 为异步可等待对象或返回非 bool。
        ServiceException: predicate 返回 False 时抛出无权限异常。
    """
    result = predicate()
    if inspect.isawaitable(result):
        raise TypeError("pre_authorize 的 predicate 必须同步返回 bool")
    if not isinstance(result, bool):
        raise TypeError(
            "pre_authorize 的 predicate 必须返回 bool，"
            "请使用 lambda 包裹组合表达式。",
        )
    if not result:
        raise ServiceException(
            code=ResponseCode.FORBIDDEN,
            message=FORBIDDEN_MESSAGE,
        )


def pre_authorize(predicate: Callable[[], bool]):
    """
    基于上下文用户角色/权限的接口访问控制装饰器。

    该装饰器可用于同步或异步函数，在执行目标函数前先调用 predicate
    完成权限校验。predicate 推荐使用 lambda，内部可组合 `has_role` /
    `has_permission`。

    示例：
        @pre_authorize(lambda: has_role(RoleCode.ADMIN) or has_permission("system:smart_assistant"))

    Args:
        predicate: 同步权限表达式，必须返回 bool。

    Returns:
        Callable: 装饰后的函数。原函数签名通过 `functools.wraps` 保留。

    Raises:
        TypeError: 当 predicate 不可调用时抛出。
    """
    if not callable(predicate):
        raise TypeError(INVALID_PREDICATE_MESSAGE)

    def _decorate(func):
        if inspect.iscoroutinefunction(func):
            @wraps(func)
            async def _async_wrapper(*args, **kwargs):
                _check_predicate(predicate)
                return await func(*args, **kwargs)

            return _async_wrapper

        @wraps(func)
        def _wrapper(*args, **kwargs):
            _check_predicate(predicate)
            return func(*args, **kwargs)

        return _wrapper

    return _decorate


__all__ = ["pre_authorize", "has_role", "has_permission", "RoleCode"]
