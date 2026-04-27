from __future__ import annotations

import os
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException

DEFAULT_LOG_BASE_PATH = "logs"  # Python 服务日志根目录默认值。
DEFAULT_LOG_LEVEL = "INFO"  # Python 服务默认日志级别。
DEFAULT_LOG_MAX_HISTORY_DAYS = 30  # Python 服务日志默认保留天数。
DEFAULT_MEDICINE_AGENT_PORT = 8000  # Python 服务默认监听端口。
DEFAULT_LOG_APP_NAME = "medicine-ai-agent"  # Python 服务日志目录中的固定应用名。
SUPPORTED_LOG_LEVELS = frozenset({
    "TRACE",
    "DEBUG",
    "INFO",
    "SUCCESS",
    "WARNING",
    "ERROR",
    "CRITICAL",
})  # 当前服务允许配置的日志级别集合。


@dataclass(frozen=True)
class LoggingSettings:
    """日志配置模型。

    Args:
        base_path: 日志根目录。
        level: 全局日志级别。
        max_history_days: 日志文件保留天数。
        port: 服务端口，用于日志目录分层。
        app_name: 日志目录中的应用名称。
    """

    base_path: Path
    level: str
    max_history_days: int
    port: int
    app_name: str


def _parse_positive_int(
        value: str | None,
        *,
        name: str,
        default: int,
) -> int:
    """解析正整数配置值。

    Args:
        value: 原始环境变量字符串。
        name: 当前配置项名称。
        default: 未配置时使用的默认值。

    Returns:
        int: 解析后的正整数值。

    Raises:
        ServiceException: 配置非整数或小于等于 0 时抛出。
    """

    if value is None or value.strip() == "":
        return default
    try:
        resolved_value = int(value)
    except ValueError as exc:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message=f"{name} 必须是整数",
        ) from exc
    if resolved_value <= 0:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message=f"{name} 必须大于 0",
        )
    return resolved_value


def _parse_log_level(value: str | None) -> str:
    """解析日志级别配置。

    Args:
        value: 原始环境变量字符串。

    Returns:
        str: 规范化后的日志级别名称。

    Raises:
        ServiceException: 日志级别不在允许范围内时抛出。
    """

    if value is None or value.strip() == "":
        return DEFAULT_LOG_LEVEL
    resolved_level = value.strip().upper()
    if resolved_level not in SUPPORTED_LOG_LEVELS:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message="MEDICINE_LOG_LEVEL 配置非法",
        )
    return resolved_level


def _parse_log_base_path(value: str | None) -> Path:
    """解析日志根目录配置。

    Args:
        value: 原始环境变量字符串。

    Returns:
        Path: 解析后的日志根目录路径。
    """

    resolved_path = (value or DEFAULT_LOG_BASE_PATH).strip() or DEFAULT_LOG_BASE_PATH
    return Path(resolved_path).expanduser()


@lru_cache(maxsize=1)
def get_logging_settings() -> LoggingSettings:
    """读取并缓存日志配置。

    Args:
        无额外参数；配置来源为当前进程环境变量。

    Returns:
        LoggingSettings: 解析后的日志配置对象。

    Raises:
        ServiceException: 配置格式非法时抛出。
    """

    return LoggingSettings(
        base_path=_parse_log_base_path(os.getenv("MEDICINE_LOG_BASE_PATH")),
        level=_parse_log_level(os.getenv("MEDICINE_LOG_LEVEL")),
        max_history_days=_parse_positive_int(
            os.getenv("MEDICINE_LOG_MAX_HISTORY_DAYS"),
            name="MEDICINE_LOG_MAX_HISTORY_DAYS",
            default=DEFAULT_LOG_MAX_HISTORY_DAYS,
        ),
        port=_parse_positive_int(
            os.getenv("MEDICINE_AGENT_PORT"),
            name="MEDICINE_AGENT_PORT",
            default=DEFAULT_MEDICINE_AGENT_PORT,
        ),
        app_name=DEFAULT_LOG_APP_NAME,
    )


def clear_logging_settings_cache() -> None:
    """清理日志配置缓存。

    Args:
        无额外参数。

    Returns:
        None: 无返回值。
    """

    get_logging_settings.cache_clear()
