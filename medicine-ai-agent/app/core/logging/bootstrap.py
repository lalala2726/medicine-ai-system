from __future__ import annotations

import logging
import os
import sys
from collections.abc import Mapping
from datetime import datetime
from pathlib import Path
from typing import Any

from loguru import logger

from app.core.logging.config import LoggingSettings, get_logging_settings

NORMAL_LOG_TYPE = "info"  # 正常日志目录类型标识。
ERROR_LOG_TYPE = "error"  # 错误日志目录类型标识。
ERROR_LEVEL_NO = logging.ERROR  # 错误级别对应的标准 logging 数值。
AUDIT_SUFFIX_KEY = "audit_suffix"  # 日志格式化阶段使用的附加审计字段键名。
AUDIT_FIELD_ORDER = (
    "event",
    "category",
    "method",
    "path",
    "status",
    "duration_ms",
    "client_ip",
    "user_id",
    "auth_mode",
    "request_id",
)  # 审计字段输出顺序。
STANDARD_LIBRARY_LOGGER_NAMES = (
    "uvicorn",
    "uvicorn.error",
    "fastapi",
    "starlette",
    "asyncio",
    "py.warnings",
)  # 需要统一桥接到 loguru 的标准库 logger 名称。
DISABLED_LOGGER_NAMES = (
    "uvicorn.access",
    "uvicorn.asgi",
)  # 需要显式禁用以避免重复日志的 logger 名称。
LOG_FORMAT = (
    "{time:YYYY-MM-DD HH:mm:ss.SSS} | {level:<8} | pid={process.id} | "
    "{name}:{function}:{line} | {message}{extra[audit_suffix]}\n{exception}"
)  # 控制台与文件统一使用的文本日志格式。


class StandardLoggingInterceptHandler(logging.Handler):
    """将标准库 logging 日志桥接到 loguru。"""

    def emit(self, record: logging.LogRecord) -> None:
        """转发标准库日志记录。

        Args:
            record: 标准库 logging 生成的日志记录对象。

        Returns:
            None: 无返回值。
        """

        try:
            resolved_level: str | int = logger.level(record.levelname).name
        except ValueError:
            resolved_level = record.levelno

        frame = logging.currentframe()
        depth = 2
        while frame is not None and frame.f_code.co_filename == logging.__file__:
            frame = frame.f_back
            depth += 1

        logger.opt(
            depth=depth,
            exception=record.exc_info,
        ).log(resolved_level, record.getMessage())


def _patch_log_record(record: dict[str, Any]) -> None:
    """为日志记录补充统一的审计字段尾缀。

    Args:
        record: loguru 当前待输出的日志记录字典。

    Returns:
        None: 无返回值。
    """

    extra = record["extra"]
    audit_parts: list[str] = []
    for field_name in AUDIT_FIELD_ORDER:
        field_value = extra.get(field_name)
        if field_value is None or field_value == "":
            continue
        audit_parts.append(f"{field_name}={field_value}")
    extra[AUDIT_SUFFIX_KEY] = f" | {' '.join(audit_parts)}" if audit_parts else ""


def _build_log_file_pattern(
        settings: LoggingSettings,
        *,
        log_type: str,
) -> str:
    """构造日志文件路径模板。

    Args:
        settings: 当前日志配置对象。
        log_type: 日志类型，仅允许 `info` 或 `error`。

    Returns:
        str: 交给 loguru 的文件路径模板字符串。
    """

    file_name = f"{settings.app_name}-{settings.port}-pid{os.getpid()}-{log_type}.log"
    return str(
        settings.base_path
        / settings.app_name
        / str(settings.port)
        / log_type
        / "{time:YYYY}"
        / "{time:MM}"
        / "{time:DD}"
        / "{time:HH}"
        / file_name
    )


def _build_hour_directory_parts(value: datetime) -> tuple[str, str, str, str]:
    """构造给定时间对应的小时目录片段。

    Args:
        value: 当前日志记录时间。

    Returns:
        tuple[str, str, str, str]: `(YYYY, MM, DD, HH)` 目录片段。
    """

    return (
        value.strftime("%Y"),
        value.strftime("%m"),
        value.strftime("%d"),
        value.strftime("%H"),
    )


def _extract_hour_directory_parts(file_name: str) -> tuple[str, str, str, str] | None:
    """从当前打开的日志文件路径中提取小时目录片段。

    Args:
        file_name: 当前日志文件完整路径。

    Returns:
        tuple[str, str, str, str] | None: 成功时返回 `(YYYY, MM, DD, HH)`，失败返回 `None`。
    """

    file_path = Path(file_name)
    if len(file_path.parts) < 5:
        return None
    return tuple(file_path.parts[-5:-1])


def _should_rotate_on_hour_boundary(message: Any, file: Any) -> bool:
    """判断文件 sink 是否需要在整小时切换到新目录。

    Args:
        message: loguru 当前消息对象。
        file: loguru 当前写入中的文件对象。

    Returns:
        bool: 当前消息所属小时与文件所在小时不一致时返回 `True`。
    """

    current_hour_parts = _build_hour_directory_parts(message.record["time"])
    opened_file_hour_parts = _extract_hour_directory_parts(str(file.name))
    if opened_file_hour_parts is None:
        return False
    return current_hour_parts != opened_file_hour_parts


def _is_normal_log_record(record: Mapping[str, Any]) -> bool:
    """判断日志记录是否应写入正常日志文件。

    Args:
        record: loguru 日志记录字典。

    Returns:
        bool: 日志级别小于 `ERROR` 时返回 `True`。
    """

    return int(record["level"].no) < ERROR_LEVEL_NO


def _is_error_log_record(record: Mapping[str, Any]) -> bool:
    """判断日志记录是否属于错误日志。

    Args:
        record: loguru 日志记录字典。

    Returns:
        bool: 日志级别大于等于 `ERROR` 时返回 `True`。
    """

    return int(record["level"].no) >= ERROR_LEVEL_NO


def _ensure_log_root_directories(settings: LoggingSettings) -> None:
    """确保日志根目录存在。

    Args:
        settings: 当前日志配置对象。

    Returns:
        None: 无返回值。
    """

    (
            settings.base_path
            / settings.app_name
            / str(settings.port)
            / NORMAL_LOG_TYPE
    ).mkdir(parents=True, exist_ok=True)
    (
            settings.base_path
            / settings.app_name
            / str(settings.port)
            / ERROR_LOG_TYPE
    ).mkdir(parents=True, exist_ok=True)


def _configure_standard_logging_bridge() -> None:
    """接管标准库 logging，并统一桥接到 loguru。

    Args:
        无额外参数。

    Returns:
        None: 无返回值。
    """

    intercept_handler = StandardLoggingInterceptHandler()
    root_logger = logging.getLogger()
    root_logger.handlers.clear()
    root_logger.setLevel(logging.NOTSET)
    root_logger.addHandler(intercept_handler)

    for logger_name in STANDARD_LIBRARY_LOGGER_NAMES:
        current_logger = logging.getLogger(logger_name)
        current_logger.handlers.clear()
        current_logger.propagate = True
        current_logger.disabled = False
        current_logger.setLevel(logging.NOTSET)

    for logger_name in DISABLED_LOGGER_NAMES:
        current_logger = logging.getLogger(logger_name)
        current_logger.handlers.clear()
        current_logger.propagate = False
        current_logger.disabled = True

    logging.captureWarnings(True)


def _register_console_sink(settings: LoggingSettings) -> None:
    """注册控制台日志 sink。

    正常日志写入 stdout，错误日志写入 stderr。这样 IDE 或终端不会把 INFO/WARNING 等
    正常日志误染成红色，同时仍保留错误日志走 stderr 的标准语义。

    Args:
        settings: 当前日志配置对象。

    Returns:
        None: 无返回值。
    """

    logger.add(
        sys.stdout,
        level=settings.level,
        format=LOG_FORMAT,
        filter=_is_normal_log_record,
        backtrace=False,
        diagnose=False,
        catch=False,
    )
    logger.add(
        sys.stderr,
        level=settings.level,
        format=LOG_FORMAT,
        filter=_is_error_log_record,
        backtrace=False,
        diagnose=False,
        catch=False,
    )


def _register_normal_file_sink(settings: LoggingSettings) -> None:
    """注册正常日志文件 sink。

    Args:
        settings: 当前日志配置对象。

    Returns:
        None: 无返回值。
    """

    logger.add(
        _build_log_file_pattern(settings, log_type=NORMAL_LOG_TYPE),
        level=settings.level,
        format=LOG_FORMAT,
        filter=_is_normal_log_record,
        rotation=_should_rotate_on_hour_boundary,
        retention=f"{settings.max_history_days} days",
        encoding="utf-8",
        enqueue=True,
        backtrace=False,
        diagnose=False,
        catch=False,
    )


def _register_error_file_sink(settings: LoggingSettings) -> None:
    """注册错误日志文件 sink。

    Args:
        settings: 当前日志配置对象。

    Returns:
        None: 无返回值。
    """

    logger.add(
        _build_log_file_pattern(settings, log_type=ERROR_LOG_TYPE),
        level="ERROR",
        format=LOG_FORMAT,
        rotation=_should_rotate_on_hour_boundary,
        retention=f"{settings.max_history_days} days",
        encoding="utf-8",
        enqueue=True,
        backtrace=False,
        diagnose=False,
        catch=False,
    )


def initialize_logging() -> None:
    """初始化当前服务的统一日志体系。

    Args:
        无额外参数；日志配置由环境变量解析得到。

    Returns:
        None: 无返回值。
    """

    settings = get_logging_settings()
    _ensure_log_root_directories(settings)

    logger.remove()
    logger.configure(
        extra={AUDIT_SUFFIX_KEY: ""},
        patcher=_patch_log_record,
    )

    _register_console_sink(settings)
    _register_normal_file_sink(settings)
    _register_error_file_sink(settings)
    _configure_standard_logging_bridge()

    logger.bind(
        event="logging_startup",
        request_id="-",
        user_id="-",
        auth_mode="-",
    ).info(
        "logging initialized base_path={} port={} level={} retention_days={}",
        settings.base_path,
        settings.port,
        settings.level,
        settings.max_history_days,
    )
