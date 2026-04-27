from __future__ import annotations

from langchain_core.messages import AIMessage, HumanMessage, SystemMessage
from pydantic import BaseModel, ConfigDict, Field


class Memory(BaseModel):
    """业务记忆载体（消息顺序由服务层保证为旧 -> 新）。"""

    model_config = ConfigDict(extra="forbid")

    messages: list[HumanMessage | AIMessage | SystemMessage] = Field(
        default_factory=list,
        description="已排序的会话记忆消息列表（旧 -> 新）",
    )
