from __future__ import annotations

from typing import Any

from langgraph.constants import END, START
from langgraph.graph import StateGraph

from app.agent.admin.agent_node import admin_agent
from app.agent.admin.state import AgentState


def build_graph() -> Any:
    """
    功能描述：
        构建管理端单 Agent LangGraph。

    参数说明：
        无。

    返回值：
        Any: 已编译的 LangGraph 实例。

    异常说明：
        无。
    """

    graph = StateGraph(AgentState)
    graph.add_node("admin_agent", admin_agent)
    graph.add_edge(START, "admin_agent")
    graph.add_edge("admin_agent", END)
    return graph.compile()
