from __future__ import annotations

from typing import Any

from langgraph.constants import END, START
from langgraph.graph import StateGraph

from app.agent.client.domain.medical import medical_agent
from app.agent.client.domain.router import gateway_router
from app.agent.client.domain.service import service_agent
from app.agent.client.state import AgentState
from app.core.agent.tracing import agent_trace


def _resolve_routing_target(state: AgentState) -> str | None:
    """解析状态中的单一路由目标。"""

    routing = state.get("routing")
    if not isinstance(routing, dict):
        return None

    raw_target = routing.get("route_target")
    if not isinstance(raw_target, str):
        return None

    target = str(raw_target or "").strip()
    allowed_targets = {
        "service_agent",
        "medical_agent",
    }
    if not target:
        return None
    if target not in allowed_targets:
        return None
    return target


@agent_trace(name="Client Assistant Entry Router Node")
def _entry_router(_: AgentState) -> dict[str, Any]:
    """client assistant 入口占位节点。"""

    return {}


def _route_from_entry(state: AgentState) -> str:
    """根据预填充直达目标决定是否跳过 gateway_router。"""

    resolved_target = _resolve_routing_target(state)
    if resolved_target is None:
        return "gateway_router"
    return resolved_target


def _route_from_gateway(state: AgentState) -> str:
    """根据 client gateway 结果选择下一个执行节点。"""

    resolved_target = _resolve_routing_target(state)
    if resolved_target is None:
        raise ValueError("gateway_router must provide a valid route_target")
    return resolved_target


def build_graph() -> Any:
    """构建 client assistant graph。"""

    graph = StateGraph(AgentState)

    graph.add_node("entry_router", _entry_router)
    graph.add_node("gateway_router", gateway_router)
    graph.add_node("service_agent", service_agent)
    graph.add_node("medical_agent", medical_agent)

    graph.add_edge(START, "entry_router")
    graph.add_conditional_edges(
        "entry_router",
        _route_from_entry,
        {
            "gateway_router": "gateway_router",
            "service_agent": "service_agent",
            "medical_agent": "medical_agent",
        },
    )
    graph.add_conditional_edges(
        "gateway_router",
        _route_from_gateway,
        {
            "service_agent": "service_agent",
            "medical_agent": "medical_agent",
        },
    )
    graph.add_edge("service_agent", END)
    graph.add_edge("medical_agent", END)

    return graph.compile()
