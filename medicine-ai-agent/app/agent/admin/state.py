from __future__ import annotations

from typing import TypeAlias, TypedDict

from langchain_core.messages import AIMessage, HumanMessage, SystemMessage
from langgraph.graph import MessagesState

ChatHistoryMessage: TypeAlias = HumanMessage | AIMessage | SystemMessage
"""对话历史消息类型别名。"""


class AgentState(MessagesState, total=False):
    """
    管理端单 Agent 工作流状态。

    字段说明：
    1. `messages` 由 `MessagesState` 提供，兼容 LangGraph 内部消息流；
    2. `conversation_uuid` 标记当前会话；
    3. `history_messages` 存储外层会话历史；
    4. `assistant_message_uuid` 标记当前轮 AI 占位消息；
    5. `loaded_tool_keys` 用于记录当前一次运行中已加载的业务工具；
    6. `result` 用于外层持久化与流式落库。
    """

    # 当前会话 UUID。
    conversation_uuid: str

    # 当前轮 AI 占位消息 UUID。
    assistant_message_uuid: str

    # 对话历史消息。
    history_messages: list[ChatHistoryMessage]

    # 当前一次运行中已加载的业务工具 key 数组。
    loaded_tool_keys: list[str]

    # 当前轮用户问题文本（用于节点视觉接线）。
    current_question: str

    # 当前轮用户上传图片 URL 列表。
    current_image_urls: list[str]

    # 节点最终输出文本。
    result: str

    # 用户在聊天界面手动选择后，经服务层解析得到的真实模型名称。
    override_model_name: str | None

    # 用户本次提交显式传入的深度思考开关。
    override_reasoning: bool | None
