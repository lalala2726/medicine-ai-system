from __future__ import annotations

import time
import uuid
from typing import Awaitable, Callable

from fastapi import Request
from loguru import logger
from starlette.responses import Response

from app.core.exception.exception_handlers import ExceptionHandlers
from app.core.exception.exceptions import ServiceException
from app.core.security.anonymous_access import is_anonymous_request
from app.core.security.auth_context import (
    reset_authorization_header,
    reset_current_user,
    set_authorization_header,
    set_current_user,
)
from app.core.security.system_auth import is_system_request, verify_system_request
from app.services.auth_service import verify_authorization

REQUEST_ID_HEADER_NAME = "X-Request-Id"  # 请求追踪 ID 请求头名称。
X_FORWARDED_FOR_HEADER_NAME = "X-Forwarded-For"  # 反向代理透传的原始客户端 IP 请求头名称。
REQUEST_STATE_REQUEST_ID_KEY = "request_id"  # request.state 中的请求追踪 ID 字段名。
REQUEST_STATE_AUTH_MODE_KEY = "auth_mode"  # request.state 中的鉴权模式字段名。
REQUEST_STATE_USER_ID_KEY = "user_id"  # request.state 中的用户 ID 字段名。
AUTH_MODE_BYPASS = "bypass"  # 免鉴权链路使用的审计模式标识。
AUTH_MODE_ANONYMOUS = "anonymous"  # 匿名接口使用的审计模式标识。
AUTH_MODE_SYSTEM = "system"  # 系统签名接口使用的审计模式标识。
AUTH_MODE_USER = "user"  # 用户鉴权接口使用的审计模式标识。
UNKNOWN_CLIENT_IP = "unknown"  # 无法解析客户端 IP 时的占位值。
AUTH_BYPASS_PATHS = {
    "/docs",
    "/redoc",
    "/openapi.json",
    "/docs/oauth2-redirect",
    "/favicon.ico",
}  # 免鉴权路径集合，仅用于文档与基础静态资源。


def _should_skip_authorization(request: Request) -> bool:
    """判断当前请求是否跳过鉴权。

    Args:
        request: 当前 HTTP 请求对象。

    Returns:
        bool: 命中免鉴权条件时返回 `True`。
    """

    if request.method.upper() == "OPTIONS":
        return True
    path = request.url.path
    if path in AUTH_BYPASS_PATHS:
        return True
    if path.startswith("/docs/") or path.startswith("/redoc/"):
        return True
    if is_anonymous_request(request):
        return True
    return False


def _resolve_request_id(request: Request) -> str:
    """解析当前请求的追踪 ID。

    Args:
        request: 当前 HTTP 请求对象。

    Returns:
        str: 请求头中的 `X-Request-Id`；若未传入则返回服务端生成的新 UUID。
    """

    resolved_request_id = (request.headers.get(REQUEST_ID_HEADER_NAME) or "").strip()
    if resolved_request_id != "":
        return resolved_request_id
    return uuid.uuid4().hex


def _resolve_forwarded_client_ip(request: Request) -> str | None:
    """解析反向代理透传的客户端 IP。

    Args:
        request: 当前 HTTP 请求对象。

    Returns:
        str | None: ``X-Forwarded-For`` 中的首个客户端 IP；未携带时返回 ``None``。
    """

    forwarded_for = (request.headers.get(X_FORWARDED_FOR_HEADER_NAME) or "").strip()
    if not forwarded_for:
        return None
    forwarded_client_ip = forwarded_for.split(",", 1)[0].strip()
    return forwarded_client_ip or None


def _resolve_client_ip(request: Request) -> str:
    """解析当前请求的客户端 IP。

    Args:
        request: 当前 HTTP 请求对象。

    Returns:
        str: 客户端 IP；无法解析时返回 `unknown`。
    """

    forwarded_client_ip = _resolve_forwarded_client_ip(request)
    if forwarded_client_ip is not None:
        return forwarded_client_ip

    client = getattr(request, "client", None)
    if client is None or not getattr(client, "host", None):
        return UNKNOWN_CLIENT_IP
    return str(client.host)


def _resolve_auth_mode(request: Request) -> str:
    """解析当前请求的鉴权模式。

    Args:
        request: 当前 HTTP 请求对象。

    Returns:
        str: `bypass`、`anonymous`、`system` 或 `user` 之一。
    """

    if request.method.upper() == "OPTIONS":
        return AUTH_MODE_BYPASS
    path = request.url.path
    if path in AUTH_BYPASS_PATHS or path.startswith("/docs/") or path.startswith("/redoc/"):
        return AUTH_MODE_BYPASS
    if is_anonymous_request(request):
        return AUTH_MODE_ANONYMOUS
    if is_system_request(request):
        return AUTH_MODE_SYSTEM
    return AUTH_MODE_USER


def _set_request_audit_context(
        request: Request,
        *,
        request_id: str,
        auth_mode: str,
        user_id: int | None,
) -> None:
    """写入当前请求的审计上下文。

    Args:
        request: 当前 HTTP 请求对象。
        request_id: 当前请求追踪 ID。
        auth_mode: 当前请求鉴权模式。
        user_id: 当前请求用户 ID；无用户时传 `None`。

    Returns:
        None: 无返回值。
    """

    setattr(request.state, REQUEST_STATE_REQUEST_ID_KEY, request_id)
    setattr(request.state, REQUEST_STATE_AUTH_MODE_KEY, auth_mode)
    setattr(request.state, REQUEST_STATE_USER_ID_KEY, user_id)


def _get_request_audit_value(
        request: Request,
        *,
        state_key: str,
        default: str,
) -> str:
    """读取当前请求中的审计上下文字段。

    Args:
        request: 当前 HTTP 请求对象。
        state_key: `request.state` 中的字段名。
        default: 字段缺失时使用的默认值。

    Returns:
        str: 解析后的审计字段字符串。
    """

    state = getattr(request, "state", None)
    resolved_value = getattr(state, state_key, default) if state is not None else default
    if resolved_value is None:
        return default
    return str(resolved_value)


def _log_access(
        request: Request,
        *,
        status_code: int,
        started_monotonic: float,
) -> None:
    """记录当前 HTTP 请求的访问审计日志。

    Args:
        request: 当前 HTTP 请求对象。
        status_code: 最终响应状态码。
        started_monotonic: 请求开始时的单调时钟时间。

    Returns:
        None: 无返回值。
    """

    duration_ms = int((time.perf_counter() - started_monotonic) * 1000)
    logger.bind(
        event="access",
        method=request.method.upper(),
        path=request.url.path,
        status=status_code,
        duration_ms=duration_ms,
        client_ip=_resolve_client_ip(request),
        user_id=_get_request_audit_value(
            request,
            state_key=REQUEST_STATE_USER_ID_KEY,
            default="-",
        ),
        auth_mode=_get_request_audit_value(
            request,
            state_key=REQUEST_STATE_AUTH_MODE_KEY,
            default=AUTH_MODE_BYPASS,
        ),
        request_id=_get_request_audit_value(
            request,
            state_key=REQUEST_STATE_REQUEST_ID_KEY,
            default="-",
        ),
    ).info("HTTP access")


async def authorization_header_middleware(
        request: Request,
        call_next: Callable[[Request], Awaitable[Response]],
) -> Response:
    """统一处理鉴权上下文与访问审计日志。

    Args:
        request: 当前 HTTP 请求对象。
        call_next: FastAPI 下一个请求处理器。

    Returns:
        Response: 当前请求最终响应对象。
    """

    auth_token = set_authorization_header(request.headers.get("Authorization"))
    user_token = set_current_user(None)
    request_id = _resolve_request_id(request)
    auth_mode = _resolve_auth_mode(request)
    request_started_monotonic = time.perf_counter()
    response_status_code = 500
    _set_request_audit_context(
        request,
        request_id=request_id,
        auth_mode=auth_mode,
        user_id=None,
    )
    try:
        if not _should_skip_authorization(request):
            if is_system_request(request):
                try:
                    await verify_system_request(request)
                except ServiceException as exc:
                    response = await ExceptionHandlers.service_exception_handler(request, exc)
                    response_status_code = int(response.status_code)
                    return response
                except Exception as exc:
                    response = await ExceptionHandlers.unhandled_exception_handler(request, exc)
                    response_status_code = int(response.status_code)
                    return response
            else:
                try:
                    current_user = await verify_authorization()
                except ServiceException as exc:
                    response = await ExceptionHandlers.service_exception_handler(request, exc)
                    response_status_code = int(response.status_code)
                    return response
                except Exception as exc:
                    response = await ExceptionHandlers.unhandled_exception_handler(request, exc)
                    response_status_code = int(response.status_code)
                    return response
                reset_current_user(user_token)
                user_token = set_current_user(current_user)
                _set_request_audit_context(
                    request,
                    request_id=request_id,
                    auth_mode=auth_mode,
                    user_id=current_user.id,
                )
        response = await call_next(request)
        response_status_code = int(response.status_code)
        return response
    finally:
        _log_access(
            request,
            status_code=response_status_code,
            started_monotonic=request_started_monotonic,
        )
        reset_current_user(user_token)
        reset_authorization_header(auth_token)


__all__ = ["authorization_header_middleware"]
