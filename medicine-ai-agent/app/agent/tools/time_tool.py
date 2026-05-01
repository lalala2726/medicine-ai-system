from __future__ import annotations

import datetime
from zoneinfo import ZoneInfo

from langchain_core.tools import tool
from pydantic import BaseModel, Field

from app.core.agent.middleware import tool_call_status, tool_thinking_redaction

# 默认业务时区。当前服务主要面向中国用户，统一返回北京时间。
DEFAULT_AGENT_TIME_ZONE = "Asia/Shanghai"


class CurrentTimeInfo(BaseModel):
    """
    功能描述：
        Agent 当前时间工具返回模型。

    参数说明：
        无。

    返回值：
        无（Pydantic 数据模型定义）。
    """

    timezone: str = Field(description="当前时间所属 IANA 时区")
    iso_datetime: str = Field(description="ISO8601 格式当前时间")
    date: str = Field(description="YYYY-MM-DD 格式日期")
    time: str = Field(description="HH:mm:ss 格式时间")
    weekday: str = Field(description="中文星期")
    display_text: str = Field(description="适合直接给模型阅读的中文当前时间")


def _build_current_time_info(now: datetime.datetime | None = None) -> CurrentTimeInfo:
    """
    功能描述：
        构建当前北京时间信息。

    参数说明：
        now (datetime.datetime | None): 指定时间；为空时使用系统当前时间。

    返回值：
        CurrentTimeInfo: 当前时间结构化信息。
    """

    timezone = ZoneInfo(DEFAULT_AGENT_TIME_ZONE)
    resolved_now = now.astimezone(timezone) if now is not None else datetime.datetime.now(timezone)
    weekday_labels = ("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")
    weekday = weekday_labels[resolved_now.weekday()]
    date_text = resolved_now.strftime("%Y-%m-%d")
    time_text = resolved_now.strftime("%H:%M:%S")
    return CurrentTimeInfo(
        timezone=DEFAULT_AGENT_TIME_ZONE,
        iso_datetime=resolved_now.isoformat(timespec="seconds"),
        date=date_text,
        time=time_text,
        weekday=weekday,
        display_text=f"当前北京时间：{date_text} {time_text}（{weekday}）",
    )


@tool(
    description=(
            "获取当前北京时间。"
            "当用户询问今天、现在、当前日期、当前时间、星期几，"
            "或业务判断依赖当前日期时间时调用。"
            "这是只读工具，不需要用户确认。"
    ),
)
@tool_thinking_redaction(display_name="获取当前时间")
@tool_call_status(
    tool_name="获取当前时间",
    start_message="正在获取当前时间",
    error_message="获取当前时间失败",
    timely_message="当前时间正在持续获取中",
)
def get_current_time() -> CurrentTimeInfo:
    """
    功能描述：
        获取当前北京时间，供 Agent 在需要时间上下文时主动调用。

    参数说明：
        无。

    返回值：
        CurrentTimeInfo: 当前北京时间结构化信息。
    """

    return _build_current_time_info()


__all__ = [
    "CurrentTimeInfo",
    "DEFAULT_AGENT_TIME_ZONE",
    "get_current_time",
]
