from __future__ import annotations

from typing import Any, Callable

from fastapi import Request
from fastapi.routing import APIRoute
from starlette.routing import Match

from app.core.security.system_auth.constants import SYSTEM_ENDPOINT_ATTR


def allow_system(func: Callable[..., Any]) -> Callable[..., Any]:
    """标记接口为系统签名访问。

    Args:
        func: 路由函数。

    Returns:
        Callable[..., Any]: 原函数对象。
    """
    setattr(func, SYSTEM_ENDPOINT_ATTR, True)
    return func


def is_system_endpoint(endpoint: Callable[..., Any] | None) -> bool:
    """判断 endpoint 是否标记为系统签名访问。

    Args:
        endpoint: 路由函数对象。

    Returns:
        bool: 命中标记返回 True。
    """
    if endpoint is None:
        return False
    return bool(getattr(endpoint, SYSTEM_ENDPOINT_ATTR, False))


def is_system_request(request: Request) -> bool:
    """判断请求是否命中系统签名接口。

    Args:
        request: 当前请求对象。

    Returns:
        bool: 命中 `@allow_system` 路由返回 True。
    """
    router = getattr(request.app, "router", None)
    routes = getattr(router, "routes", None) if router is not None else None
    if not routes:
        return False
    for route in routes:
        if not isinstance(route, APIRoute):
            continue
        match, _ = route.matches(request.scope)
        if match is Match.FULL:
            return is_system_endpoint(route.endpoint)
    return False
