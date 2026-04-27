from __future__ import annotations

from app.core.codes import ResponseCode
from app.core.database.redis.config import get_redis_connection
from app.core.exception.exceptions import ServiceException
from app.core.security.system_auth.config import SystemAuthSettings


def build_nonce_key(
        *,
        settings: SystemAuthSettings,
        app_id: str,
        nonce: str,
) -> str:
    """构造 nonce 防重放 Redis key。

    Args:
        settings: 系统鉴权配置。
        app_id: 调用方标识。
        nonce: 请求随机串。

    Returns:
        str: Redis key。
    """
    return f"{settings.nonce_key_prefix}:{app_id}:{nonce}"


def reserve_nonce(
        *,
        settings: SystemAuthSettings,
        app_id: str,
        nonce: str,
) -> bool:
    """预占 nonce（SET NX EX），用于防重放。

    Args:
        settings: 系统鉴权配置。
        app_id: 调用方标识。
        nonce: 请求随机串。

    Returns:
        bool: 预占成功返回 True；已存在返回 False。

    Raises:
        ServiceException: Redis 不可用时抛出 503。
    """
    key = build_nonce_key(settings=settings, app_id=app_id, nonce=nonce)
    client = get_redis_connection()
    try:
        reserved = client.set(
            key,
            b"1",
            nx=True,
            ex=settings.nonce_ttl_seconds,
        )
    except Exception as exc:
        raise ServiceException(
            code=ResponseCode.SERVICE_UNAVAILABLE,
            message="系统鉴权防重放服务不可用",
        ) from exc
    return bool(reserved)
