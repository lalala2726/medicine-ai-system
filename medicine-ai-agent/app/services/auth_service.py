from __future__ import annotations

from typing import Any

import httpx
from loguru import logger
from pydantic import ValidationError

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.schemas.auth import AuthUser, AuthorizationContext
from app.schemas.http_response import HttpResponse
from app.utils.http_client import HttpClient

AUTH_SERVICE_UNAVAILABLE_MESSAGE = "认证服务暂不可用，请稍后重试"  # 认证服务不可用时返回给上游的统一提示文案。
AUTHORIZATION_ENDPOINT_PATH = "/agent/authorization"  # 用户态鉴权上下文查询接口路径。


def _is_auth_failure_code(code: int) -> bool:
    """
    认证失败码判定：
    - 标准 HTTP 401/403
    - 业务扩展码 401x/403x（例如 4011: 访问令牌已过期，4012: 刷新令牌过期）
    """
    if code in {ResponseCode.UNAUTHORIZED.code, ResponseCode.FORBIDDEN.code}:
        return True
    code_text = str(code)
    return code_text.startswith("4011") or code_text.startswith("4012")


def _is_standard_auth_failure_code(code: int) -> bool:
    """标准 HTTP 认证失败码判定。"""

    return code in {ResponseCode.UNAUTHORIZED.code, ResponseCode.FORBIDDEN.code}


def _build_auth_user(payload: Any) -> AuthUser:
    try:
        context = AuthorizationContext.model_validate(payload)
    except ValidationError as exc:
        raise ServiceException(
            code=503,
            message="无法获取当前用户信息～请检查你是否登陆？",
        ) from exc

    return context.to_auth_user()


def _build_authorization_target_url(base_url: str) -> str:
    """拼接认证服务完整请求地址。

    Args:
        base_url: `HttpClient` 当前使用的基础地址。

    Returns:
        str: 认证服务完整请求地址。
    """

    normalized_base_url = base_url.rstrip("/")
    return f"{normalized_base_url}{AUTHORIZATION_ENDPOINT_PATH}"


async def verify_authorization() -> AuthUser:
    """
    使用当前请求 Authorization 调用 Spring `/agent/authorization` 获取用户上下文。

    处理规则：
    - 通过 `/agent/authorization` 获取响应并解析 data。
    - 标准认证失败码（401/403）统一映射为 HTTP 401。
    - 扩展认证失败码（如 4011/4012）保持原业务码透出。
    - 认证服务其他异常（5xx、非 JSON、网络错误、协议不符合约定）统一映射为 HTTP 503。
    - data 需满足 `{user, roles, permissions}` 结构（user 必填；roles/permissions 可空）。
    """
    authorization_target_url = AUTHORIZATION_ENDPOINT_PATH
    try:
        async with HttpClient(timeout=5.0) as client:
            authorization_target_url = _build_authorization_target_url(client.base_url)
            response = await client.get(AUTHORIZATION_ENDPOINT_PATH)
        payload = HttpResponse.parse_data(response)
    except ServiceException as exc:
        if _is_standard_auth_failure_code(exc.code):
            raise ServiceException(
                code=ResponseCode.UNAUTHORIZED,
                message=exc.message,
                data=exc.data,
            ) from exc
        if _is_auth_failure_code(exc.code):
            raise ServiceException(
                code=exc.code,
                message=exc.message,
                data=exc.data,
            ) from exc
        logger.error(
            "Authorization service responded with non-auth failure: target_url={} code={} message={}",
            authorization_target_url,
            exc.code,
            exc.message,
        )
        raise ServiceException(
            code=503,
            message=AUTH_SERVICE_UNAVAILABLE_MESSAGE,
        ) from exc
    except (httpx.TimeoutException, httpx.HTTPError) as exc:
        logger.error(
            "Authorization service request failed: target_url={} error_type={} error={}",
            authorization_target_url,
            type(exc).__name__,
            exc,
        )
        raise ServiceException(
            code=503,
            message=AUTH_SERVICE_UNAVAILABLE_MESSAGE,
        ) from exc

    return _build_auth_user(payload)
