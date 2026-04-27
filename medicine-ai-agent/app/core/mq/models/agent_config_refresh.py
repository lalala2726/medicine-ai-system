"""Agent 配置刷新消息模型。"""

from __future__ import annotations

from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field


class AgentConfigRefreshMessage(BaseModel):
    """业务服务发送的 Agent 配置刷新通知。

    Attributes:
        message_type: 固定消息类型，值为 ``agent_config_refresh``。
        config_version: 当前 Redis 配置版本号，仅作为日志元信息，可选。
        redis_key: 配置所在的 Redis key。
        updated_at: 本次配置更新时间。
        updated_by: 本次配置更新人。
        created_at: MQ 消息创建时间。
    """

    message_type: Literal["agent_config_refresh"] = "agent_config_refresh"
    config_version: int | None = None
    redis_key: str = Field(..., min_length=1)
    updated_at: datetime
    updated_by: str = Field(..., min_length=1)
    created_at: datetime
