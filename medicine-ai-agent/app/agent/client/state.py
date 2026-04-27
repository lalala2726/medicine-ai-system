from __future__ import annotations

from typing import TypeAlias, TypedDict

from langchain_core.messages import AIMessage, HumanMessage, SystemMessage
from langgraph.graph import MessagesState

ChatHistoryMessage: TypeAlias = HumanMessage | AIMessage | SystemMessage


class GatewayRoutingState(TypedDict):
    """Gateway 路由结果结构。"""

    route_target: str


class CardActionState(TypedDict, total=False):
    """交互卡片点击后的结构化上下文。"""

    card_type: str
    card_scene: str
    action: str


class AgentState(MessagesState, total=False):
    """
    Client agent 工作流状态。

    字段说明：
    1. `messages` 由 `MessagesState` 提供，兼容 LangGraph 内部消息流；
    2. `conversation_uuid` 用于会话级工具缓存隔离；
    3. `routing` 存储 gateway 路由结果或入口直达目标；
    4. `card_action` 存储交互卡片点击后的结构化上下文；
    5. `assistant_message_uuid` 标记当前轮 AI 占位消息；
    6. `history_messages/result` 用于外层持久化与流式落库。
    """

    conversation_uuid: str
    assistant_message_uuid: str
    routing: GatewayRoutingState
    card_action: CardActionState
    context: str
    history_messages: list[ChatHistoryMessage]
    # 当前轮用户问题文本（用于节点视觉接线）。
    current_question: str
    # 当前轮用户上传图片 URL 列表。
    current_image_urls: list[str]
    # 当前轮是否开启深度思考。
    reasoning_enabled: bool
    result: str
