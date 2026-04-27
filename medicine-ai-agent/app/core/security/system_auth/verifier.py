from __future__ import annotations

import time

from fastapi import Request

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.core.security.system_auth.canonical import build_request_canonical_string
from app.core.security.system_auth.config import (
    SystemAuthClientConfig,
    SystemAuthSettings,
    get_system_auth_settings,
)
from app.core.security.system_auth.constants import (
    HEADER_X_AGENT_KEY,
    HEADER_X_AGENT_NONCE,
    HEADER_X_AGENT_SIGNATURE,
    HEADER_X_AGENT_SIGN_VERSION,
    HEADER_X_AGENT_TIMESTAMP,
)
from app.core.security.system_auth.models import SystemAuthPrincipal
from app.core.security.system_auth.nonce_store import reserve_nonce
from app.core.security.system_auth.signer import (
    is_signature_equal,
    sign_hmac_sha256_base64url,
)


def _require_header(request: Request, name: str) -> str:
    """读取并校验必填请求头。

    Args:
        request: 当前请求对象。
        name: 请求头名称。

    Returns:
        str: 去空格后的头值。

    Raises:
        ServiceException: 请求头缺失时抛出 401。
    """
    value = (request.headers.get(name) or "").strip()
    if value == "":
        raise ServiceException(
            code=ResponseCode.UNAUTHORIZED,
            message=f"缺少 {name} 请求头",
        )
    return value


def _parse_timestamp(timestamp_text: str) -> int:
    """解析时间戳请求头。

    Args:
        timestamp_text: 时间戳字符串。

    Returns:
        int: 秒级时间戳。

    Raises:
        ServiceException: 非法整数时抛出 401。
    """
    try:
        return int(timestamp_text)
    except ValueError as exc:
        raise ServiceException(
            code=ResponseCode.UNAUTHORIZED,
            message="X-Agent-Timestamp 非法",
        ) from exc


def _validate_timestamp(timestamp: int, settings: SystemAuthSettings) -> None:
    """校验时间戳是否在允许窗口内。

    Args:
        timestamp: 请求时间戳（秒）。
        settings: 系统鉴权配置。

    Returns:
        None

    Raises:
        ServiceException: 超时请求抛出 401。
    """
    now_ts = int(time.time())
    if abs(now_ts - timestamp) > settings.max_skew_seconds:
        raise ServiceException(
            code=ResponseCode.UNAUTHORIZED,
            message="请求时间戳超出允许范围",
        )


def _resolve_client(*, app_id: str, settings: SystemAuthSettings) -> SystemAuthClientConfig:
    """解析并校验调用方配置。

    Args:
        app_id: 调用方标识。
        settings: 系统鉴权配置。

    Returns:
        SystemAuthClientConfig: 调用方配置。

    Raises:
        ServiceException: 未注册或被禁用时抛出 403。
    """
    client = settings.clients.get(app_id)
    if client is None:
        raise ServiceException(
            code=ResponseCode.FORBIDDEN,
            message="系统调用方未注册",
        )
    if not client.enabled:
        raise ServiceException(
            code=ResponseCode.FORBIDDEN,
            message="系统调用方已禁用",
        )
    return client


def _validate_nonce(nonce: str) -> None:
    """校验 nonce 基础格式。

    Args:
        nonce: 请求随机串。

    Returns:
        None

    Raises:
        ServiceException: 长度非法时抛出 401。
    """
    if len(nonce) < 8 or len(nonce) > 128:
        raise ServiceException(
            code=ResponseCode.UNAUTHORIZED,
            message="X-Agent-Nonce 非法",
        )


async def verify_system_request(
        request: Request,
        *,
        settings: SystemAuthSettings | None = None,
) -> SystemAuthPrincipal:
    """执行系统签名认证。

    Args:
        request: 当前请求对象。
        settings: 可选系统鉴权配置，默认读取全局缓存配置。

    Returns:
        SystemAuthPrincipal: 验签通过后的系统主体信息。

    Raises:
        ServiceException:
            - 401: 缺头、签名错误、超时、重放等；
            - 403: 调用方未注册或禁用；
            - 503: 防重放服务不可用。
    """
    resolved_settings = settings or get_system_auth_settings()
    if not resolved_settings.enabled:
        raise ServiceException(
            code=ResponseCode.SERVICE_UNAVAILABLE,
            message="系统签名认证已禁用",
        )

    app_id = _require_header(request, HEADER_X_AGENT_KEY)
    timestamp_text = _require_header(request, HEADER_X_AGENT_TIMESTAMP)
    nonce = _require_header(request, HEADER_X_AGENT_NONCE)
    signature = _require_header(request, HEADER_X_AGENT_SIGNATURE)
    sign_version = _require_header(request, HEADER_X_AGENT_SIGN_VERSION)

    if sign_version != resolved_settings.default_sign_version:
        raise ServiceException(
            code=ResponseCode.UNAUTHORIZED,
            message="不支持的签名版本",
        )

    timestamp = _parse_timestamp(timestamp_text)
    _validate_timestamp(timestamp, resolved_settings)
    _validate_nonce(nonce)
    client = _resolve_client(app_id=app_id, settings=resolved_settings)

    body_bytes = await request.body()
    canonical_string = build_request_canonical_string(
        request=request,
        timestamp=timestamp,
        nonce=nonce,
        body_bytes=body_bytes,
    )
    expected_signature = sign_hmac_sha256_base64url(
        secret=client.secret,
        canonical_string=canonical_string,
    )
    if not is_signature_equal(expected=expected_signature, actual=signature):
        raise ServiceException(
            code=ResponseCode.UNAUTHORIZED,
            message="系统签名校验失败",
        )

    if not reserve_nonce(
            settings=resolved_settings,
            app_id=app_id,
            nonce=nonce,
    ):
        raise ServiceException(
            code=ResponseCode.UNAUTHORIZED,
            message="请求重复，疑似重放攻击",
        )

    return SystemAuthPrincipal(
        app_id=app_id,
        sign_version=sign_version,
        timestamp=timestamp,
        nonce=nonce,
    )
