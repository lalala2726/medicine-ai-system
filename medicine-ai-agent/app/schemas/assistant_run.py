from __future__ import annotations

from enum import Enum

from pydantic import BaseModel, ConfigDict, Field


class AssistantRunStatus(str, Enum):
    """助手运行态状态。"""

    RUNNING = "running"
    WAITING_INPUT = "waiting_input"
    SUCCESS = "success"
    CANCELLED = "cancelled"
    ERROR = "error"


class AssistantRunSubmitResponse(BaseModel):
    """提交聊天请求后的运行态响应。"""

    model_config = ConfigDict(extra="forbid")

    conversation_uuid: str = Field(..., min_length=1, description="会话 UUID")
    message_uuid: str = Field(..., min_length=1, description="当前 AI 消息 UUID")
    run_status: AssistantRunStatus = Field(..., description="当前运行态状态")


class AssistantRunStopRequest(BaseModel):
    """停止当前会话运行请求。"""

    model_config = ConfigDict(extra="forbid")

    conversation_uuid: str = Field(..., min_length=1, description="会话 UUID")


class AssistantRunStopResponse(BaseModel):
    """停止当前会话运行响应。"""

    model_config = ConfigDict(extra="forbid")

    conversation_uuid: str = Field(..., min_length=1, description="会话 UUID")
    message_uuid: str = Field(..., min_length=1, description="当前 AI 消息 UUID")
    run_status: AssistantRunStatus = Field(..., description="当前运行态状态")
    stop_requested: bool = Field(default=True, description="是否已接受停止请求")
