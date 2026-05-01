from __future__ import annotations

from datetime import datetime
from enum import Enum
from typing import Any

from bson import ObjectId
from pydantic import BaseModel, ConfigDict, Field, field_validator


class AgentTraceStatus(str, Enum):
    """Agent Trace 运行状态。"""

    RUNNING = "running"
    SUCCESS = "success"
    ERROR = "error"
    CANCELLED = "cancelled"


class AgentTraceSpanType(str, Enum):
    """Agent Trace span 类型。"""

    GRAPH = "graph"
    NODE = "node"
    MIDDLEWARE = "middleware"
    MODEL = "model"
    TOOL = "tool"


class AgentTraceRunDocument(BaseModel):
    """
    功能描述：
        Agent Trace 运行汇总 Mongo 文档。

    参数说明：
        字段与 `agent_trace_runs` 集合一一对应。

    返回值：
        无（文档模型）。
    """

    model_config = ConfigDict(extra="ignore", populate_by_name=True)

    id: str | None = Field(default=None, alias="_id", description="MongoDB 主键")
    trace_id: str = Field(..., min_length=1, description="Trace 唯一标识")
    conversation_uuid: str = Field(..., min_length=1, description="会话 UUID")
    assistant_message_uuid: str = Field(..., min_length=1, description="AI 消息 UUID")
    user_id: int | None = Field(default=None, description="用户 ID")
    conversation_type: str = Field(..., min_length=1, description="会话类型")
    graph_name: str = Field(..., min_length=1, description="Graph 名称")
    entrypoint: str = Field(..., min_length=1, description="入口标识")
    status: AgentTraceStatus = Field(..., description="运行状态")
    started_at: datetime = Field(..., description="开始时间")
    ended_at: datetime | None = Field(default=None, description="结束时间")
    duration_ms: int | None = Field(default=None, description="耗时毫秒")
    root_span_id: str = Field(..., min_length=1, description="根 span ID")
    input_tokens: int = Field(default=0, ge=0, description="输入 token")
    output_tokens: int = Field(default=0, ge=0, description="输出 token")
    total_tokens: int = Field(default=0, ge=0, description="总 token")
    error_payload: dict[str, Any] | None = Field(default=None, description="错误结构")
    created_at: datetime = Field(..., description="创建时间")
    updated_at: datetime = Field(..., description="更新时间")

    @field_validator("id", mode="before")
    @classmethod
    def _normalize_id(cls, value: Any) -> str | None:
        """
        功能描述：
            把 Mongo ObjectId 转成字符串。

        参数说明：
            value (Any): 原始主键。

        返回值：
            str | None: 字符串主键。
        """

        if value is None:
            return None
        if isinstance(value, ObjectId):
            return str(value)
        return str(value)


class AgentTraceSpanDocument(BaseModel):
    """
    功能描述：
        Agent Trace span 明细 Mongo 文档。

    参数说明：
        字段与 `agent_trace_spans` 集合一一对应。

    返回值：
        无（文档模型）。
    """

    model_config = ConfigDict(extra="ignore", populate_by_name=True)

    id: str | None = Field(default=None, alias="_id", description="MongoDB 主键")
    trace_id: str = Field(..., min_length=1, description="Trace 唯一标识")
    span_id: str = Field(..., min_length=1, description="Span 唯一标识")
    parent_span_id: str | None = Field(default=None, description="父 span ID")
    span_type: AgentTraceSpanType = Field(..., description="Span 类型")
    name: str = Field(..., min_length=1, description="Span 名称")
    status: AgentTraceStatus = Field(..., description="Span 状态")
    started_at: datetime = Field(..., description="开始时间")
    ended_at: datetime = Field(..., description="结束时间")
    duration_ms: int = Field(..., ge=0, description="耗时毫秒")
    input_payload: Any | None = Field(default=None, description="输入载荷")
    output_payload: Any | None = Field(default=None, description="输出载荷")
    attributes: dict[str, Any] | None = Field(default=None, description="附加属性")
    token_usage: dict[str, int] | None = Field(default=None, description="Token 用量")
    error_payload: dict[str, Any] | None = Field(default=None, description="错误结构")
    sequence: int = Field(..., ge=1, description="Trace 内顺序号")

    @field_validator("id", mode="before")
    @classmethod
    def _normalize_id(cls, value: Any) -> str | None:
        """
        功能描述：
            把 Mongo ObjectId 转成字符串。

        参数说明：
            value (Any): 原始主键。

        返回值：
            str | None: 字符串主键。
        """

        if value is None:
            return None
        if isinstance(value, ObjectId):
            return str(value)
        return str(value)
