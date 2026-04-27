from __future__ import annotations

from typing import Any, Callable

from fastapi import Request
from fastapi.routing import APIRoute
from starlette.routing import Match

ANONYMOUS_ENDPOINT_ATTR = "__allow_anonymous__"


def allow_anonymous(func: Callable[..., Any]) -> Callable[..., Any]:
    """
    标记接口为匿名可访问。

    该装饰器只负责打标记，不参与鉴权决策；鉴权由中间件统一判断。
    """
    setattr(func, ANONYMOUS_ENDPOINT_ATTR, True)
    return func


def is_anonymous_endpoint(endpoint: Callable[..., Any] | None) -> bool:
    """判断 endpoint 是否标记为匿名访问。"""
    if endpoint is None:
        return False
    return bool(getattr(endpoint, ANONYMOUS_ENDPOINT_ATTR, False))


def is_anonymous_request(request: Request) -> bool:
    """
    判断当前请求是否命中匿名 endpoint。

    通过 FastAPI 路由匹配检查 `Match.FULL` 的 APIRoute，再读取 endpoint 标记。
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
            return is_anonymous_endpoint(route.endpoint)
    return False


__all__ = [
    "allow_anonymous",
    "is_anonymous_endpoint",
    "is_anonymous_request",
]
