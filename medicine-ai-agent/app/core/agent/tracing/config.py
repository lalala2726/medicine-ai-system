from __future__ import annotations

import os
from dataclasses import dataclass

# Agent Trace 主开关环境变量名。
AGENT_TRACE_ENABLED_ENV_NAME = "AGENT_TRACE_ENABLED"
# Agent Trace 后台队列容量环境变量名。
AGENT_TRACE_QUEUE_MAX_SIZE_ENV_NAME = "AGENT_TRACE_QUEUE_MAX_SIZE"
# Agent Trace 批量写入条数环境变量名。
AGENT_TRACE_BATCH_SIZE_ENV_NAME = "AGENT_TRACE_BATCH_SIZE"
# Agent Trace 批量刷盘间隔环境变量名。
AGENT_TRACE_FLUSH_INTERVAL_MS_ENV_NAME = "AGENT_TRACE_FLUSH_INTERVAL_MS"
# Agent Trace 单个 payload 最大字符数环境变量名。
AGENT_TRACE_PAYLOAD_MAX_CHARS_ENV_NAME = "AGENT_TRACE_PAYLOAD_MAX_CHARS"
# Agent Trace 顶层消息视图最大保存消息数环境变量名。
AGENT_TRACE_MESSAGE_VIEW_MAX_MESSAGES_ENV_NAME = "AGENT_TRACE_MESSAGE_VIEW_MAX_MESSAGES"

DEFAULT_AGENT_TRACE_ENABLED = True
"""Agent Trace 默认开启。"""
DEFAULT_AGENT_TRACE_QUEUE_MAX_SIZE = 5000
"""Agent Trace 默认后台队列容量。"""
DEFAULT_AGENT_TRACE_BATCH_SIZE = 100
"""Agent Trace 默认批量写入条数。"""
DEFAULT_AGENT_TRACE_FLUSH_INTERVAL_MS = 1000
"""Agent Trace 默认批量刷盘间隔（毫秒）。"""
DEFAULT_AGENT_TRACE_PAYLOAD_MAX_CHARS = 20000
"""Agent Trace 默认单个 payload 最大字符数。"""
DEFAULT_AGENT_TRACE_MESSAGE_VIEW_MAX_MESSAGES = 30
"""Agent Trace 顶层消息视图默认保存最近消息条数。"""
_TRUTHY_VALUES = {"1", "true", "yes", "on"}
"""布尔环境变量真值集合。"""
_FALSY_VALUES = {"0", "false", "no", "off"}
"""布尔环境变量假值集合。"""


@dataclass(frozen=True)
class AgentTraceSettings:
    """
    功能描述：
        Agent Trace 运行时配置。

    参数说明：
        enabled (bool): 是否启用 trace 采集。
        queue_max_size (int): 后台队列最大容量。
        batch_size (int): 后台批量写入条数。
        flush_interval_ms (int): 后台批量刷盘间隔（毫秒）。
        payload_max_chars (int): 单个 payload 最大字符数。
        message_view_max_messages (int): 顶层消息视图最大保存消息条数。

    返回值：
        无（数据模型）。
    """

    enabled: bool
    queue_max_size: int
    batch_size: int
    flush_interval_ms: int
    payload_max_chars: int
    message_view_max_messages: int


def _parse_bool(value: str | None, *, default: bool) -> bool:
    """
    功能描述：
        解析布尔环境变量。

    参数说明：
        value (str | None): 环境变量原始值。
        default (bool): 为空或无法识别时使用的默认值。

    返回值：
        bool: 解析后的布尔值。
    """

    if value is None:
        return default
    normalized_value = value.strip().lower()
    if not normalized_value:
        return default
    if normalized_value in _TRUTHY_VALUES:
        return True
    if normalized_value in _FALSY_VALUES:
        return False
    return default


def _parse_positive_int(value: str | None, *, default: int) -> int:
    """
    功能描述：
        解析正整数环境变量。

    参数说明：
        value (str | None): 环境变量原始值。
        default (int): 为空或非法时使用的默认值。

    返回值：
        int: 大于 0 的整数。
    """

    if value is None:
        return default
    normalized_value = value.strip()
    if not normalized_value:
        return default
    try:
        parsed_value = int(normalized_value)
    except ValueError:
        return default
    if parsed_value <= 0:
        return default
    return parsed_value


def load_agent_trace_settings() -> AgentTraceSettings:
    """
    功能描述：
        从环境变量加载 Agent Trace 配置。

    参数说明：
        无。

    返回值：
        AgentTraceSettings: 当前进程生效的 trace 配置。
    """

    return AgentTraceSettings(
        enabled=_parse_bool(
            os.getenv(AGENT_TRACE_ENABLED_ENV_NAME),
            default=DEFAULT_AGENT_TRACE_ENABLED,
        ),
        queue_max_size=_parse_positive_int(
            os.getenv(AGENT_TRACE_QUEUE_MAX_SIZE_ENV_NAME),
            default=DEFAULT_AGENT_TRACE_QUEUE_MAX_SIZE,
        ),
        batch_size=_parse_positive_int(
            os.getenv(AGENT_TRACE_BATCH_SIZE_ENV_NAME),
            default=DEFAULT_AGENT_TRACE_BATCH_SIZE,
        ),
        flush_interval_ms=_parse_positive_int(
            os.getenv(AGENT_TRACE_FLUSH_INTERVAL_MS_ENV_NAME),
            default=DEFAULT_AGENT_TRACE_FLUSH_INTERVAL_MS,
        ),
        payload_max_chars=_parse_positive_int(
            os.getenv(AGENT_TRACE_PAYLOAD_MAX_CHARS_ENV_NAME),
            default=DEFAULT_AGENT_TRACE_PAYLOAD_MAX_CHARS,
        ),
        message_view_max_messages=_parse_positive_int(
            os.getenv(AGENT_TRACE_MESSAGE_VIEW_MAX_MESSAGES_ENV_NAME),
            default=DEFAULT_AGENT_TRACE_MESSAGE_VIEW_MAX_MESSAGES,
        ),
    )


def is_agent_trace_enabled() -> bool:
    """
    功能描述：
        判断当前进程是否启用 Agent Trace。

    参数说明：
        无。

    返回值：
        bool: 启用时返回 True。
    """

    return load_agent_trace_settings().enabled
