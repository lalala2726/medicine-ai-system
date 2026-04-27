"""
客户端 service 统一节点实现。
"""

from __future__ import annotations

from typing import Any, cast

from langchain.agents import create_agent
from langchain_core.messages import AIMessage, SystemMessage

from app.agent.client.domain.commerce.after_sale import (
    check_after_sale_eligibility,
    get_after_sale_detail,
)
from app.agent.client.domain.commerce.order import (
    check_order_cancelable,
    get_order_detail,
    get_order_shipping,
    get_order_timeline,
)
from app.agent.client.domain.commerce.product import (
    get_product_details,
    search_products,
)
from app.agent.client.domain.tools import (
    open_user_after_sale_list,
    open_user_order_list,
    send_consent_card,
    send_product_card,
    send_product_purchase_card,
    send_selection_card,
)
from app.agent.client.state import AgentState
from app.agent.tools.rag_query import search_client_knowledge_context
from app.core.agent.agent_event_bus import emit_answer_delta, emit_thinking_delta
from app.core.agent.agent_runtime import agent_stream
from app.core.agent.agent_tool_trace import resolve_final_output_text
from app.core.agent.middleware import (
    BasePromptMiddleware,
    SkillMiddleware,
    ToolCallLimitMiddleware,
    ToolCachePromptMiddleware,
    ToolTracePromptMiddleware,
    build_tool_status_middleware,
)
from app.core.agent.tool_cache import (
    CLIENT_COMMERCE_TOOL_CACHE_PROFILE,
    bind_tool_cache_conversation,
    reset_tool_cache_conversation,
)
from app.core.agent.tool_trace import (
    bind_tool_trace_context,
    reset_tool_trace_context,
)
from app.core.config_sync import (
    AgentChatModelSlot,
    create_agent_chat_llm,
    get_current_agent_config_snapshot,
)
from app.core.langsmith import traceable
from app.schemas.document.conversation import ConversationType
from app.utils.chat_image_utils import (
    build_multimodal_history_messages,
    normalize_chat_image_urls,
)
from app.utils.prompt_utils import append_current_time_to_prompt, load_managed_prompt

# service 节点系统提示词业务键。
_SERVICE_SYSTEM_PROMPT_KEY = "client_service_node_system_prompt"
# service 节点系统提示词本地回退路径。
_SERVICE_SYSTEM_PROMPT_LOCAL_PATH = "client/service_node_system_prompt.md"
# 客户端基础提示词本地回退路径。
_CLIENT_BASE_PROMPT_LOCAL_PATH = "client/_client_base_prompt.md"
# service skill 作用域。
_SERVICE_SKILL_SCOPE = "client_service"
# service 节点状态 schema。
_SERVICE_AGENT_STATE_SCHEMA = cast(Any, AgentState)
# service 节点单轮最多允许的工具调用次数。
_SERVICE_TOOL_CALL_THREAD_LIMIT = 14
# service 节点单次运行最多允许的工具调用次数。
_SERVICE_TOOL_CALL_RUN_LIMIT = 12


@traceable(name="Client Assistant Service Agent Node", run_type="chain")
def service_agent(state: AgentState) -> dict[str, Any]:
    """
    功能描述：
        执行 client service 统一节点，处理所有非医疗问题。

    参数说明：
        state (AgentState): 当前 client agent 工作流状态。

    返回值：
        dict[str, Any]: service 节点输出结果，包含回答文本与消息。

    异常说明：
        不主动吞掉模型或工具异常；异常由上层工作流统一处理。
    """

    conversation_uuid = str(state.get("conversation_uuid") or "").strip()
    assistant_message_uuid = str(state.get("assistant_message_uuid") or "").strip()
    current_question = str(state.get("current_question") or "")
    current_image_urls = normalize_chat_image_urls(state.get("current_image_urls"))
    supports_vision = get_current_agent_config_snapshot().supports_vision_for_chat_slot(
        AgentChatModelSlot.CLIENT_SERVICE
    )
    history_messages = list(state.get("history_messages") or [])
    if supports_vision and current_image_urls:
        history_messages = build_multimodal_history_messages(
            history_messages=history_messages,
            question=current_question,
            image_urls=current_image_urls,
        )

    runtime_tools = [
        search_client_knowledge_context,
        search_products,
        get_product_details,
        get_order_detail,
        get_order_shipping,
        get_order_timeline,
        check_order_cancelable,
        get_after_sale_detail,
        check_after_sale_eligibility,
        open_user_order_list,
        open_user_after_sale_list,
        send_product_card,
        send_product_purchase_card,
        send_consent_card,
        send_selection_card,
    ]
    system_prompt = load_managed_prompt(
        _SERVICE_SYSTEM_PROMPT_KEY,
        local_prompt_path=_SERVICE_SYSTEM_PROMPT_LOCAL_PATH,
    )
    llm = create_agent_chat_llm(
        slot=AgentChatModelSlot.CLIENT_SERVICE,
        temperature=1.0,
        think=False,
        reasoning_override=bool(state.get("reasoning_enabled")),
    )
    agent = create_agent(
        model=llm,
        tools=runtime_tools,
        system_prompt=SystemMessage(
            content=append_current_time_to_prompt(system_prompt)
        ),
        state_schema=_SERVICE_AGENT_STATE_SCHEMA,
        middleware=[
            BasePromptMiddleware(
                base_prompt_key="client_base_prompt",
                base_prompt_local_path=_CLIENT_BASE_PROMPT_LOCAL_PATH,
            ),
            ToolCachePromptMiddleware(profile=CLIENT_COMMERCE_TOOL_CACHE_PROFILE),
            ToolTracePromptMiddleware(),
            SkillMiddleware(scope=_SERVICE_SKILL_SCOPE),
            build_tool_status_middleware(),
            ToolCallLimitMiddleware(
                thread_limit=_SERVICE_TOOL_CALL_THREAD_LIMIT,
                run_limit=_SERVICE_TOOL_CALL_RUN_LIMIT,
            ),
        ],
    )
    cache_token = bind_tool_cache_conversation(
        CLIENT_COMMERCE_TOOL_CACHE_PROFILE,
        conversation_uuid,
    )
    tool_trace_token = bind_tool_trace_context(
        conversation_uuid=conversation_uuid,
        assistant_message_uuid=assistant_message_uuid,
        conversation_type=ConversationType.CLIENT,
    )
    try:
        stream_result = agent_stream(
            agent,
            history_messages,
            on_model_delta=emit_answer_delta,
            on_thinking_delta=emit_thinking_delta,
        )
    finally:
        reset_tool_trace_context(tool_trace_token)
        reset_tool_cache_conversation(
            CLIENT_COMMERCE_TOOL_CACHE_PROFILE,
            cache_token,
        )
    text = resolve_final_output_text(
        payload=stream_result,
        fallback_text=str(stream_result.get("streamed_text") or ""),
    )
    return {
        "result": text,
        "messages": [AIMessage(content=text)],
    }


__all__ = [
    "service_agent",
]
