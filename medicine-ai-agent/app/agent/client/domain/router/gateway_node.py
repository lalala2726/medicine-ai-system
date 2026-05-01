from __future__ import annotations

import json
from collections.abc import Mapping
from typing import Any, Literal

from langchain.agents import create_agent
from langchain_core.messages import SystemMessage
from pydantic import BaseModel, Field, ValidationError

from app.agent.client.state import AgentState
from app.core.agent.agent_runtime import agent_invoke
from app.core.agent.middleware import BasePromptMiddleware
from app.core.agent.tracing import TraceModelMiddleware, agent_trace
from app.core.config_sync import AgentChatModelSlot, create_agent_chat_llm
from app.core.langsmith import traceable
from app.utils.prompt_utils import load_managed_prompt

# 客户端路由网关提示词业务键。
_GATEWAY_PROMPT_KEY = "client_gateway_prompt"
# 客户端路由网关提示词本地回退路径。
_GATEWAY_PROMPT_LOCAL_PATH = "client/gateway_prompt.md"
# 客户端路由网关结构化输出配置。
_GATEWAY_JSON_OBJECT_RESPONSE_FORMAT: dict[str, Any] = {
    "response_format": {"type": "json_object"},
}
_ALLOWED_GATEWAY_TARGETS: tuple[str, ...] = (
    "service_agent",
    "medical_agent",
)


class GatewayRoutingSchema(BaseModel):
    """Client gateway 路由结构化输出。"""

    route_target: Literal[
        "service_agent",
        "medical_agent",
    ] = Field(
        description="路由目标，仅允许 service_agent 或 medical_agent。",
    )


def _normalize_route_target(route_target: str) -> str:
    """
    功能描述：
        规范化 client gateway 单一路由目标。

    参数说明：
        route_target (str): 模型返回的原始路由目标。

    返回值：
        str: 规整后的合法路由目标。

    异常说明：
        ValueError: 当路由目标为空或不在允许范围内时抛出。
    """

    normalized_target = str(route_target or "").strip()
    if not normalized_target:
        raise ValueError("route_target cannot be empty")
    if normalized_target not in _ALLOWED_GATEWAY_TARGETS:
        raise ValueError(f"unsupported route_target: {normalized_target}")
    return normalized_target


def _resolve_gateway_routing_result(raw_payload: Any) -> dict[str, Any]:
    """
    功能描述：
        解析 client gateway 的结构化路由结果。

    参数说明：
        raw_payload (Any): `agent_invoke` 返回的原始 payload。

    返回值：
        dict[str, Any]: 仅包含 `route_target` 的标准化路由结果。

    异常说明：
        ValueError: 当 payload 结构非法、JSON 非法或 schema 校验失败时抛出。
    """

    if not isinstance(raw_payload, Mapping):
        raise ValueError("client gateway payload must be a mapping")

    raw_messages = raw_payload.get("messages")
    if not isinstance(raw_messages, list) or not raw_messages:
        raise ValueError("client gateway payload messages cannot be empty")

    last_message = raw_messages[-1]
    raw_content = getattr(last_message, "content", None)
    if not isinstance(raw_content, str):
        raise ValueError("client gateway last message content must be a string")

    try:
        parsed_json = json.loads(raw_content.strip())
    except json.JSONDecodeError as exc:
        raise ValueError("client gateway returned invalid JSON") from exc
    if not isinstance(parsed_json, Mapping):
        raise ValueError("client gateway result must be a JSON object")

    try:
        parsed = GatewayRoutingSchema.model_validate(parsed_json)
    except ValidationError as exc:
        raise ValueError("client gateway result does not match routing schema") from exc

    return {
        "route_target": _normalize_route_target(parsed.route_target),
    }


@traceable(name="Client Assistant Gateway Router Node", run_type="chain")
@agent_trace(name="Client Assistant Gateway Router Node")
def gateway_router(state: AgentState) -> dict[str, Any]:
    """执行 client gateway 路由节点。"""

    llm = create_agent_chat_llm(
        slot=AgentChatModelSlot.CLIENT_ROUTE,
        temperature=0.0,
        think=False,
        extra_body=_GATEWAY_JSON_OBJECT_RESPONSE_FORMAT,
    )
    agent = create_agent(
        model=llm,
        system_prompt=SystemMessage(
            content=load_managed_prompt(
                _GATEWAY_PROMPT_KEY,
                local_prompt_path=_GATEWAY_PROMPT_LOCAL_PATH,
            )
        ),
        middleware=[
            BasePromptMiddleware(),
            TraceModelMiddleware(slot=AgentChatModelSlot.CLIENT_ROUTE.value),
        ],
    )
    history_messages = list(state.get("history_messages") or [])
    result = agent_invoke(agent, history_messages[-20:])
    routing = _resolve_gateway_routing_result(result.payload)
    return {
        "routing": routing,
    }
