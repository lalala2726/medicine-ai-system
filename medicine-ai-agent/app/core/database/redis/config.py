from __future__ import annotations

import os
from dataclasses import dataclass
from functools import lru_cache
from typing import Optional

from redis import Redis

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException

DEFAULT_REDIS_HOST = "localhost"  # Redis 默认主机
DEFAULT_REDIS_PORT = 6379  # Redis 默认端口
DEFAULT_REDIS_DB = 0  # Redis 默认数据库编号
DEFAULT_DECODE_RESPONSES = False  # 保持 bytes 语义，保障 Lua 与二进制场景


def _parse_int(value: Optional[str], *, name: str, default: int) -> int:
    """解析整数配置值。

    Args:
        value: 环境变量原始字符串。
        name: 配置项名称，用于错误提示。
        default: 未配置时默认值。

    Returns:
        int: 解析后的整数值。

    Raises:
        ServiceException: 配置不是整数时抛出。
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


def _parse_bool(value: Optional[str], *, default: bool) -> bool:
    """解析布尔配置值。

    Args:
        value: 环境变量原始字符串。
        default: 未配置时默认值。

    Returns:
        bool: 解析后的布尔值。
    """
    if value is None:
        return default
    normalized = value.strip().lower()
    if normalized == "":
        return default
    return normalized in {"1", "true", "yes", "on"}


@dataclass(frozen=True)
class RedisSettings:
    """Redis 连接配置模型。"""

    url: str | None
    host: str
    port: int
    db: int
    password: str | None
    ssl: bool
    decode_responses: bool = DEFAULT_DECODE_RESPONSES


@lru_cache(maxsize=1)
def get_redis_settings() -> RedisSettings:
    """读取并缓存 Redis 配置。

    Returns:
        RedisSettings: 解析后的 Redis 配置对象。

    Raises:
        ServiceException: 配置项格式非法时抛出。
    """
    redis_url = (os.getenv("REDIS_URL") or "").strip() or None
    host = (os.getenv("REDIS_HOST") or DEFAULT_REDIS_HOST).strip() or DEFAULT_REDIS_HOST
    port = _parse_int(os.getenv("REDIS_PORT"), name="REDIS_PORT", default=DEFAULT_REDIS_PORT)
    db = _parse_int(os.getenv("REDIS_DB"), name="REDIS_DB", default=DEFAULT_REDIS_DB)
    password = os.getenv("REDIS_PASSWORD")
    ssl_enabled = _parse_bool(os.getenv("REDIS_SSL"), default=False)
    return RedisSettings(
        url=redis_url,
        host=host,
        port=port,
        db=db,
        password=password,
        ssl=ssl_enabled,
        decode_responses=DEFAULT_DECODE_RESPONSES,
    )


@lru_cache(maxsize=1)
def get_redis_connection() -> Redis:
    """创建并缓存 Redis 连接对象。

    Returns:
        Redis: redis-py 连接实例。

    Raises:
        ServiceException: 配置项非法导致连接初始化参数不可用时抛出。
    """
    settings = get_redis_settings()
    if settings.url:
        return Redis.from_url(
            settings.url,
            decode_responses=settings.decode_responses,
        )

    return Redis(
        host=settings.host,
        port=settings.port,
        db=settings.db,
        password=settings.password,
        ssl=settings.ssl,
        decode_responses=settings.decode_responses,
    )


def clear_redis_connection_cache() -> None:
    """清理 Redis 配置与连接缓存（主要用于测试）。

    Returns:
        None: 无返回值。
    """
    get_redis_connection.cache_clear()
    get_redis_settings.cache_clear()
