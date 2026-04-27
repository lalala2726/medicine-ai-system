from __future__ import annotations

import os
from dataclasses import dataclass
from functools import lru_cache

from neo4j import Driver, GraphDatabase
from neo4j.exceptions import DriverError, Neo4jError

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException

DEFAULT_NEO4J_URI = "bolt://localhost:7687"  # Neo4j 默认连接地址
DEFAULT_NEO4J_USER = "neo4j"  # Neo4j 默认用户名
DEFAULT_NEO4J_DATABASE = "neo4j"  # Neo4j 默认数据库名称
DEFAULT_NEO4J_TIMEOUT_SECONDS = 3.0  # Neo4j 默认连接超时（秒）
DEFAULT_NEO4J_STARTUP_PING_ENABLED = False  # 启动时是否执行 Neo4j 连通性校验

_CACHED_NEO4J_DRIVER: Driver | None = None  # 进程级缓存的 Neo4j Driver 引用，便于统一关闭连接。


@dataclass(frozen=True)
class Neo4jSettings:
    """Neo4j 连接配置模型。"""

    uri: str
    user: str
    password: str
    database: str
    timeout_seconds: float
    startup_ping_enabled: bool


def _parse_bool(value: str | None, *, default: bool) -> bool:
    """解析布尔配置值。

    Args:
        value: 环境变量原始字符串。
        default: 未配置时使用的默认布尔值。

    Returns:
        bool: 解析后的布尔值。
    """
    if value is None:
        return default
    normalized = value.strip().lower()
    if normalized == "":
        return default
    return normalized in {"1", "true", "yes", "on"}


def _parse_timeout_seconds(value: str | None) -> float:
    """解析并校验 Neo4j 超时配置。

    Args:
        value: 环境变量 `NEO4J_TIMEOUT_SECONDS` 原始值。

    Returns:
        float: 合法的超时秒数。

    Raises:
        ServiceException: 超时配置为空、非数字或小于等于 0 时抛出。
    """
    if value is None or value.strip() == "":
        return DEFAULT_NEO4J_TIMEOUT_SECONDS
    try:
        timeout_seconds = float(value)
    except ValueError as exc:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message="NEO4J_TIMEOUT_SECONDS 必须是数字",
        ) from exc

    if timeout_seconds <= 0:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message="NEO4J_TIMEOUT_SECONDS 必须大于 0",
        )
    return timeout_seconds


def is_neo4j_startup_ping_enabled() -> bool:
    """读取 Neo4j 启动探活开关。

    Args:
        无额外参数；直接读取环境变量 `NEO4J_STARTUP_PING_ENABLED`。

    Returns:
        bool: 是否在服务启动阶段执行 Neo4j 连通性校验。
    """
    return _parse_bool(
        os.getenv("NEO4J_STARTUP_PING_ENABLED"),
        default=DEFAULT_NEO4J_STARTUP_PING_ENABLED,
    )


@lru_cache(maxsize=1)
def get_neo4j_settings() -> Neo4jSettings:
    """读取并缓存 Neo4j 配置。

    Args:
        无额外参数；配置来源为当前进程环境变量。

    Returns:
        Neo4jSettings: 解析后的 Neo4j 配置对象。

    Raises:
        ServiceException: 必填配置缺失或配置格式非法时抛出。
    """
    uri = (os.getenv("NEO4J_URI") or DEFAULT_NEO4J_URI).strip()
    user = (os.getenv("NEO4J_USER") or DEFAULT_NEO4J_USER).strip()
    password = (os.getenv("NEO4J_PASSWORD") or "").strip()
    database = (os.getenv("NEO4J_DATABASE") or DEFAULT_NEO4J_DATABASE).strip()

    if not uri:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message="NEO4J_URI 不能为空",
        )
    if not user:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message="NEO4J_USER 不能为空",
        )
    if not password:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message="NEO4J_PASSWORD 不能为空",
        )
    if not database:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message="NEO4J_DATABASE 不能为空",
        )

    return Neo4jSettings(
        uri=uri,
        user=user,
        password=password,
        database=database,
        timeout_seconds=_parse_timeout_seconds(os.getenv("NEO4J_TIMEOUT_SECONDS")),
        startup_ping_enabled=is_neo4j_startup_ping_enabled(),
    )


def _remember_cached_neo4j_driver(driver: Driver) -> None:
    """记录当前缓存的 Neo4j Driver。

    Args:
        driver: 已成功创建的 Neo4j Driver 实例。

    Returns:
        None: 无返回值。
    """
    global _CACHED_NEO4J_DRIVER
    _CACHED_NEO4J_DRIVER = driver


@lru_cache(maxsize=1)
def get_neo4j_driver() -> Driver:
    """创建并缓存 Neo4j Driver。

    Args:
        无额外参数；连接参数来自 `get_neo4j_settings()`。

    Returns:
        Driver: 进程级复用的 Neo4j Driver。

    Raises:
        ServiceException: 配置非法或 Driver 初始化失败时抛出。
    """
    settings = get_neo4j_settings()
    try:
        driver = GraphDatabase.driver(
            settings.uri,
            auth=(settings.user, settings.password),
            connection_timeout=settings.timeout_seconds,
        )
    except (DriverError, Neo4jError, TypeError, ValueError) as exc:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message="Neo4j 客户端初始化失败",
        ) from exc

    _remember_cached_neo4j_driver(driver)
    return driver


def verify_neo4j_connection() -> None:
    """执行 Neo4j 连通性校验。

    Args:
        无额外参数；校验目标取自当前环境配置。

    Returns:
        None: 校验成功时无返回值。

    Raises:
        ServiceException: Neo4j 不可达、认证失败或数据库不存在时抛出。
    """
    settings = get_neo4j_settings()
    try:
        get_neo4j_driver().verify_connectivity(database=settings.database)
    except (DriverError, Neo4jError) as exc:
        raise ServiceException(
            code=ResponseCode.DATABASE_ERROR,
            message="Neo4j 连接校验失败",
        ) from exc


def _clear_neo4j_driver_cache() -> None:
    """清理 Neo4j Driver 与配置缓存。

    Args:
        无额外参数。

    Returns:
        None: 无返回值。
    """
    global _CACHED_NEO4J_DRIVER
    cached_driver = _CACHED_NEO4J_DRIVER
    _CACHED_NEO4J_DRIVER = None
    if cached_driver is not None:
        try:
            cached_driver.close()
        except (DriverError, Neo4jError):
            pass

    get_neo4j_driver.cache_clear()
    get_neo4j_settings.cache_clear()
