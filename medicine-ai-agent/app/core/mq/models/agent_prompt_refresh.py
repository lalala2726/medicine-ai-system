"""Agent 提示词刷新消息模型。"""

from __future__ import annotations

from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field


class AgentPromptRefreshMessage(BaseModel):
    """业务服务发送的 Agent 提示词刷新通知。

    Attributes:
        message_type: 固定消息类型，值为 ``agent_prompt_refresh``。
        prompt_key: 本次更新的提示词业务键。
        prompt_version: 本次更新后的版本号。
        redis_key: 提示词配置所在的 Redis key。
        updated_at: 本次配置更新时间。
        updated_by: 本次配置更新人。
        created_at: MQ 消息创建时间。
    """

    message_type: Literal["agent_prompt_refresh"] = "agent_prompt_refresh"
    prompt_key: str = Field(..., min_length=1)
    prompt_version: int | None = None
    redis_key: str = Field(..., min_length=1)
    updated_at: datetime
    updated_by: str = Field(..., min_length=1)
    created_at: datetime
