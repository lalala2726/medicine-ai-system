from __future__ import annotations

import json
import os
from dataclasses import dataclass
from functools import lru_cache
from typing import Any

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.core.security.system_auth.constants import DEFAULT_SIGN_VERSION

DEFAULT_SYSTEM_AUTH_ENABLED = True  # 系统鉴权默认开关
DEFAULT_SYSTEM_AUTH_MAX_SKEW_SECONDS = 300  # 默认时间戳容差（秒）
DEFAULT_SYSTEM_AUTH_NONCE_TTL_SECONDS = 600  # 默认 nonce 过期时间（秒）
DEFAULT_SYSTEM_AUTH_NONCE_KEY_PREFIX = "system_auth:nonce"  # 默认防重放 key 前缀


@dataclass(frozen=True)
class SystemAuthClientConfig:
    """系统调用方配置。

    Args:
        app_id: 调用方标识。
        secret: 调用方签名密钥。
        enabled: 是否启用该调用方。
    """

    app_id: str
    secret: str
    enabled: bool = True


@dataclass(frozen=True)
class SystemAuthSettings:
    """系统签名认证配置。

    Args:
        enabled: 系统鉴权总开关。
        max_skew_seconds: 时间戳容差（秒）。
        nonce_ttl_seconds: nonce TTL（秒）。
        nonce_key_prefix: nonce Redis key 前缀。
        default_sign_version: 默认签名版本。
        local_secret: 当前服务出站签名使用的本地密钥。
        clients: 已注册系统调用方配置映射（key=app_id）。
    """

    enabled: bool
    max_skew_seconds: int
    nonce_ttl_seconds: int
    nonce_key_prefix: str
    default_sign_version: str
    local_secret: str
    clients: dict[str, SystemAuthClientConfig]


def _parse_bool(value: str | None, *, default: bool) -> bool:
    """解析布尔配置值。

    Args:
        value: 原始环境变量字符串。
        default: 默认值。

    Returns:
        bool: 解析后的布尔值。
    """
    if value is None:
        return default
    normalized = value.strip().lower()
    if normalized == "":
        return default
    return normalized in {"1", "true", "yes", "on"}


def _parse_int(value: str | None, *, name: str, default: int) -> int:
    """解析整数配置值。

    Args:
        value: 原始环境变量字符串。
        name: 配置项名称。
        default: 默认值。

    Returns:
        int: 解析后的整数值。

    Raises:
        ServiceException: 非法整数时抛出。
    """
    if value is None or value.strip() == "":
        return default
    try:
        return int(value)
    except ValueError as exc:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message=f"{name} 必须是整数",
        ) from exc


def _parse_clients(raw: str | None) -> dict[str, SystemAuthClientConfig]:
    """解析系统调用方配置 JSON。

    期望格式（数组）：
    [
      {"app_id":"biz-server","secret":"***","enabled":true}
    ]

    Args:
        raw: `SYSTEM_AUTH_CLIENTS_JSON` 原始字符串。

    Returns:
        dict[str, SystemAuthClientConfig]: 客户端配置映射。

    Raises:
        ServiceException: JSON 格式错误或字段非法时抛出。
    """
    if raw is None or raw.strip() == "":
        return {}
    try:
        payload = json.loads(raw)
    except json.JSONDecodeError as exc:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message="SYSTEM_AUTH_CLIENTS_JSON 不是合法 JSON",
        ) from exc

    if not isinstance(payload, list):
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message="SYSTEM_AUTH_CLIENTS_JSON 必须是数组",
        )

    clients: dict[str, SystemAuthClientConfig] = {}
    for item in payload:
        if not isinstance(item, dict):
            raise ServiceException(
                code=ResponseCode.INTERNAL_ERROR,
                message="SYSTEM_AUTH_CLIENTS_JSON 元素必须是对象",
            )
        app_id = str(item.get("app_id") or "").strip()
        secret = str(item.get("secret") or "").strip()
        enabled_raw: Any = item.get("enabled", True)
        enabled = bool(enabled_raw) if isinstance(enabled_raw, bool) else str(enabled_raw).strip().lower() in {
            "1",
            "true",
            "yes",
            "on",
        }
        if app_id == "":
            raise ServiceException(
                code=ResponseCode.INTERNAL_ERROR,
                message="SYSTEM_AUTH_CLIENTS_JSON 中 app_id 不能为空",
            )
        if secret == "":
            raise ServiceException(
                code=ResponseCode.INTERNAL_ERROR,
                message=f"SYSTEM_AUTH_CLIENTS_JSON 中 app_id={app_id} 的 secret 不能为空",
            )
        if app_id in clients:
            raise ServiceException(
                code=ResponseCode.INTERNAL_ERROR,
                message=f"SYSTEM_AUTH_CLIENTS_JSON 存在重复 app_id: {app_id}",
            )
        clients[app_id] = SystemAuthClientConfig(
            app_id=app_id,
            secret=secret,
            enabled=enabled,
        )
    return clients


@lru_cache(maxsize=1)
def get_system_auth_settings() -> SystemAuthSettings:
    """读取并缓存系统鉴权配置。

    Returns:
        SystemAuthSettings: 系统鉴权配置对象。

    Raises:
        ServiceException: 配置项格式非法时抛出。
    """
    enabled = _parse_bool(
        os.getenv("SYSTEM_AUTH_ENABLED"),
        default=DEFAULT_SYSTEM_AUTH_ENABLED,
    )
    max_skew_seconds = _parse_int(
        os.getenv("SYSTEM_AUTH_MAX_SKEW_SECONDS"),
        name="SYSTEM_AUTH_MAX_SKEW_SECONDS",
        default=DEFAULT_SYSTEM_AUTH_MAX_SKEW_SECONDS,
    )
    nonce_ttl_seconds = _parse_int(
        os.getenv("SYSTEM_AUTH_NONCE_TTL_SECONDS"),
        name="SYSTEM_AUTH_NONCE_TTL_SECONDS",
        default=DEFAULT_SYSTEM_AUTH_NONCE_TTL_SECONDS,
    )
    nonce_key_prefix = (
                               os.getenv("SYSTEM_AUTH_NONCE_KEY_PREFIX")
                               or DEFAULT_SYSTEM_AUTH_NONCE_KEY_PREFIX
                       ).strip() or DEFAULT_SYSTEM_AUTH_NONCE_KEY_PREFIX
    default_sign_version = (
                                   os.getenv("SYSTEM_AUTH_DEFAULT_SIGN_VERSION")
                                   or DEFAULT_SIGN_VERSION
                           ).strip() or DEFAULT_SIGN_VERSION
    local_secret = (os.getenv("SYSTEM_AUTH_LOCAL_SECRET") or "").strip()
    clients = _parse_clients(os.getenv("SYSTEM_AUTH_CLIENTS_JSON"))
    return SystemAuthSettings(
        enabled=enabled,
        max_skew_seconds=max_skew_seconds,
        nonce_ttl_seconds=nonce_ttl_seconds,
        nonce_key_prefix=nonce_key_prefix,
        default_sign_version=default_sign_version,
        local_secret=local_secret,
        clients=clients,
    )


def clear_system_auth_settings_cache() -> None:
    """清理系统鉴权配置缓存（测试用）。"""
    get_system_auth_settings.cache_clear()
