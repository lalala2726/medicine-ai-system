from __future__ import annotations

import time
from typing import Any, Literal, cast

from langchain.agents import create_agent
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage

from app.agent.client.domain.commerce.product import (
    get_product_details,
    search_products,
)
from app.agent.client.domain.diagnosis.tools import (
    query_disease_candidates_by_symptoms,
    query_disease_detail,
    query_disease_details,
    query_followup_symptom_candidates,
    search_symptom_candidates,
    send_consultation_questionnaire_card,
)
from app.agent.client.domain.tools.navigation_tools import open_user_patient_list
from app.agent.client.domain.prescription.tools import (
    build_product_search_tag_prompt_text,
    send_prescription_consent_card,
)
from app.agent.client.domain.prescription.protocol import (
    PRESCRIPTION_CONSENT_CARD_SCENE,
    PrescriptionConsentStatus,
    resolve_prescription_consent_status,
)
from app.agent.client.domain.tools.card_tools import (
    send_product_card,
    send_product_purchase_card,
)
from app.agent.client.state import AgentState
from app.core.agent.agent_event_bus import emit_answer_delta, emit_thinking_delta
from app.core.agent.agent_runtime import _run_async, agent_stream
from app.core.agent.agent_tool_trace import resolve_final_output_text
from app.core.agent.middleware import (
    BasePromptMiddleware,
    SkillMiddleware,
    ToolCallLimitMiddleware,
    ToolTracePromptMiddleware,
    build_tool_status_middleware,
)
from app.core.agent.tool_cache import (
    CLIENT_COMMERCE_TOOL_CACHE_PROFILE,
    DIAGNOSIS_TOOL_CACHE_PROFILE,
    bind_tool_cache_conversation,
    render_tool_cache_prompt,
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
from app.utils.assistant_message_utils import PATIENT_CARD_WORKFLOW_PREFIX
from app.utils.prompt_utils import append_current_time_to_prompt, load_managed_prompt

# 医疗节点系统提示词业务键。
_MEDICAL_SYSTEM_PROMPT_KEY = "client_medical_node_system_prompt"
# 医疗节点系统提示词本地回退路径。
_MEDICAL_SYSTEM_PROMPT_LOCAL_PATH = "client/medical_node_system_prompt.md"
# 医疗 skill 作用域。
_MEDICAL_SKILL_SCOPE = "client_medical"
# 医疗节点状态 schema。
_MEDICAL_AGENT_STATE_SCHEMA = cast(Any, AgentState)
# 医疗节点工具调用单轮上限。
_MEDICAL_TOOL_CALL_THREAD_LIMIT = 28
# 医疗节点工具调用单次运行上限。
_MEDICAL_TOOL_CALL_RUN_LIMIT = 24
# 标签目录缓存时长（秒）。
_TAG_CACHE_TTL = 3600.0
# 就诊人资料状态：已提供。
_PATIENT_INFO_STATUS_PROVIDED = "provided"
# 就诊人资料状态：已拒绝。
_PATIENT_INFO_STATUS_REJECTED = "rejected"
# 就诊人资料状态：未知。
_PATIENT_INFO_STATUS_UNKNOWN = "unknown"
# 判断就诊人资料相关语义时使用的关键词。
_PATIENT_INFO_CONTEXT_KEYWORDS: tuple[str, ...] = (
    "就诊人",
    "问诊资料",
    "患者资料",
    "患者基础信息",
    "基础信息",
    "年龄",
    "性别",
    "出生日期",
    "过敏史",
    "既往病史",
    "慢性病",
    "长期用药",
)
# 判断用户明确拒绝提供就诊人资料时使用的关键词。
_PATIENT_INFO_REJECTION_KEYWORDS: tuple[str, ...] = (
    "先不提供",
    "暂不提供",
    "不提供",
    "先不发",
    "暂时不发",
    "暂不发",
    "不用发",
    "不发了",
    "不发",
    "不想发",
    "不想提供",
    "不用提供",
    "先不给",
    "暂时不给",
)

# 就诊人资料状态类型。
PatientInfoStatus = Literal["provided", "rejected", "unknown"]

# 标签目录内存缓存，避免每轮都请求一次商品标签目录。
_tag_cache: str | None = None
# 标签目录缓存写入时间戳。
_tag_cache_ts: float = 0.0


async def _get_cached_tag_filters() -> str:
    """
    功能描述：
        带内存缓存地读取商品标签目录提示词片段。

    参数说明：
        无。

    返回值：
        str: 面向模型的商品标签目录提示词；目录为空时返回空字符串。

    异常说明：
        Exception: 首次读取或缓存过期时的 HTTP 异常会直接向上抛出。
    """

    global _tag_cache, _tag_cache_ts
    now = time.time()
    if _tag_cache is not None and (now - _tag_cache_ts) < _TAG_CACHE_TTL:
        return _tag_cache

    resolved_prompt = await build_product_search_tag_prompt_text()
    _tag_cache = resolved_prompt
    _tag_cache_ts = now
    return resolved_prompt


def _append_prompt_section(
        *,
        base_prompt: str,
        section_title: str,
        section_content: str,
) -> str:
    """
    功能描述：
        在基础提示词尾部追加一个非空段落。

    参数说明：
        base_prompt (str): 当前基础提示词。
        section_title (str): 段落标题。
        section_content (str): 段落正文。

    返回值：
        str: 拼接后的完整提示词文本。

    异常说明：
        无。
    """

    normalized_section_content = str(section_content or "").strip()
    if not normalized_section_content:
        return base_prompt

    normalized_base_prompt = str(base_prompt or "").rstrip()
    return (
        f"{normalized_base_prompt}\n\n"
        f"## {section_title}\n\n"
        f"{normalized_section_content}"
    )


def _stringify_message_content(content: Any) -> str:
    """
    功能描述：
        将消息内容规范化为可匹配的纯文本。

    参数说明：
        content (Any): 原始消息内容，可能是字符串或多模态列表。

    返回值：
        str: 规范化后的文本内容。

    异常说明：
        无。
    """

    if isinstance(content, str):
        return content.strip()
    if not isinstance(content, list):
        return ""

    text_parts: list[str] = []
    for item in content:
        if not isinstance(item, dict):
            continue
        if item.get("type") != "text":
            continue
        text_value = str(item.get("text") or "").strip()
        if text_value:
            text_parts.append(text_value)
    return "\n".join(text_parts).strip()


def _resolve_prescription_consent_from_state(state: AgentState) -> PrescriptionConsentStatus | None:
    """
    功能描述：
        从当前状态中解析最近一次开药确认状态。

    参数说明：
        state (AgentState): 当前 client agent 工作流状态。

    返回值：
        PrescriptionConsentStatus | None: 命中时返回 `confirmed` 或 `rejected`，否则返回 `None`。

    异常说明：
        无。
    """

    card_action = state.get("card_action")
    if isinstance(card_action, dict):
        card_scene = str(card_action.get("card_scene") or "").strip()
        if card_scene == PRESCRIPTION_CONSENT_CARD_SCENE:
            resolved_status = resolve_prescription_consent_status(
                action=str(card_action.get("action") or ""),
            )
            if resolved_status is not None:
                return resolved_status

    current_question = str(state.get("current_question") or "").strip()
    current_status = resolve_prescription_consent_status(text=current_question)
    if current_status is not None:
        return current_status

    history_messages = list(state.get("history_messages") or [])
    for message in reversed(history_messages):
        if not isinstance(message, HumanMessage):
            continue
        message_text = _stringify_message_content(message.content)
        if not message_text:
            continue
        resolved_status = resolve_prescription_consent_status(text=message_text)
        if resolved_status is not None:
            return resolved_status
    return None


def _build_prescription_consent_prompt(
        prescription_consent_status: PrescriptionConsentStatus | None,
) -> str:
    """
    功能描述：
        构造开药确认状态提示词片段。

    参数说明：
        prescription_consent_status (PrescriptionConsentStatus | None): 最近一次开药确认状态。

    返回值：
        str: 供系统提示词追加的交互状态文本；无状态时返回空字符串。

    异常说明：
        无。
    """

    if prescription_consent_status == "confirmed":
        return "- 最近一次开药确认状态：用户已经确认继续推荐药品；请直接按当前真实病情和资料继续判断是否需要进入药品推荐。"
    if prescription_consent_status == "rejected":
        return "- 最近一次开药确认状态：用户刚表示暂不继续推荐药品；除非用户这轮再次明确要药，否则不要主动推荐药品或发送购买卡。"
    return ""


def _contains_patient_info_keyword(text: str) -> bool:
    """
    功能描述：
        判断文本中是否出现就诊人资料相关关键词。

    参数说明：
        text (str): 待判断文本。

    返回值：
        bool: 命中任一关键词时返回 `True`。

    异常说明：
        无。
    """

    normalized_text = str(text or "").replace(" ", "").strip()
    if not normalized_text:
        return False
    return any(keyword in normalized_text for keyword in _PATIENT_INFO_CONTEXT_KEYWORDS)


def _contains_patient_rejection_keyword(text: str) -> bool:
    """
    功能描述：
        判断文本中是否出现明确拒绝提供就诊人资料的语义关键词。

    参数说明：
        text (str): 待判断文本。

    返回值：
        bool: 命中拒绝关键词时返回 `True`。

    异常说明：
        无。
    """

    normalized_text = str(text or "").replace(" ", "").strip()
    if not normalized_text:
        return False
    return any(keyword in normalized_text for keyword in _PATIENT_INFO_REJECTION_KEYWORDS)


def _find_previous_ai_message_text(history_messages: list[Any], start_index: int) -> str:
    """
    功能描述：
        从历史消息中找到指定用户消息之前最近一条 AI 消息文本。

    参数说明：
        history_messages (list[Any]): 当前会话历史消息列表。
        start_index (int): 当前用户消息下标。

    返回值：
        str: 最近一条 AI 消息文本；未找到时返回空字符串。

    异常说明：
        无。
    """

    for index in range(start_index - 1, -1, -1):
        current_message = history_messages[index]
        if not isinstance(current_message, AIMessage):
            continue
        return _stringify_message_content(current_message.content)
    return ""


def _is_patient_info_rejection_text(message_text: str, previous_ai_text: str = "") -> bool:
    """
    功能描述：
        判断一条用户文本是否明确表达了“不想提供就诊人资料”。

    参数说明：
        message_text (str): 用户消息文本。
        previous_ai_text (str): 该消息前最近一条 AI 消息文本。

    返回值：
        bool: 命中明确拒绝语义时返回 `True`。

    异常说明：
        无。
    """

    normalized_message_text = str(message_text or "").replace(" ", "").strip()
    if not normalized_message_text:
        return False
    if not _contains_patient_rejection_keyword(normalized_message_text):
        return False
    if _contains_patient_info_keyword(normalized_message_text):
        return True
    return _contains_patient_info_keyword(previous_ai_text)


def _resolve_patient_info_status_from_state(state: AgentState) -> PatientInfoStatus:
    """
    功能描述：
        结合最近一次相关用户输入，解析当前会话的就诊人资料状态。

    参数说明：
        state (AgentState): 当前 client agent 工作流状态。

    返回值：
        PatientInfoStatus: `provided`、`rejected` 或 `unknown`。

    异常说明：
        无。
    """

    history_messages = list(state.get("history_messages") or [])
    for index in range(len(history_messages) - 1, -1, -1):
        current_message = history_messages[index]
        if not isinstance(current_message, HumanMessage):
            continue
        message_text = _stringify_message_content(current_message.content)
        if not message_text:
            continue
        if message_text.startswith(PATIENT_CARD_WORKFLOW_PREFIX):
            return _PATIENT_INFO_STATUS_PROVIDED
        previous_ai_text = _find_previous_ai_message_text(history_messages, index)
        if _is_patient_info_rejection_text(message_text, previous_ai_text):
            return _PATIENT_INFO_STATUS_REJECTED

    current_question = str(state.get("current_question") or "").strip()
    if current_question.startswith(PATIENT_CARD_WORKFLOW_PREFIX):
        return _PATIENT_INFO_STATUS_PROVIDED
    if _is_patient_info_rejection_text(current_question):
        return _PATIENT_INFO_STATUS_REJECTED
    return _PATIENT_INFO_STATUS_UNKNOWN


def _build_patient_info_prompt(patient_info_status: PatientInfoStatus) -> str:
    """
    功能描述：
        构造就诊人资料状态对应的系统提示词片段。

    参数说明：
        patient_info_status (PatientInfoStatus): 最近一次就诊人资料状态。

    返回值：
        str: 当前状态对应的提示词片段。

    异常说明：
        无。
    """

    if patient_info_status == _PATIENT_INFO_STATUS_PROVIDED:
        return "- 最近一次就诊人资料状态：用户已经发送就诊人卡；后续判断优先结合其中的年龄、性别、过敏史、既往病史、慢性病和长期用药信息，不要重复索取。"
    if patient_info_status == _PATIENT_INFO_STATUS_REJECTED:
        return "- 最近一次就诊人资料状态：用户刚明确表示暂不提供；本会话内不要再次主动索取，也不要再次调用打开就诊人列表动作，除非用户自己发送就诊人卡。你仍需基于现有信息继续判断，并说明建议边界。"
    return "- 最近一次就诊人资料状态：未知。只有当年龄、性别、过敏史、既往病史、慢性病、长期用药会显著影响病情判断或荐药安全时，先用一句简短中文说明原因，再调用打开就诊人列表动作；纯疾病解释类问题不要主动索取。"


def _build_medical_history_messages(state: AgentState) -> list[Any]:
    """
    功能描述：
        构造医疗节点使用的历史消息列表。

    参数说明：
        state (AgentState): 当前 client agent 工作流状态。

    返回值：
        list[Any]: 已按视觉能力补齐后的历史消息列表。

    异常说明：
        无。
    """

    current_question = str(state.get("current_question") or "")
    current_image_urls = normalize_chat_image_urls(state.get("current_image_urls"))
    supports_vision = get_current_agent_config_snapshot().supports_vision_for_chat_slot(
        AgentChatModelSlot.CLIENT_DIAGNOSIS
    )
    history_messages = list(state.get("history_messages") or [])
    if supports_vision and current_image_urls:
        return build_multimodal_history_messages(
            history_messages=history_messages,
            question=current_question,
            image_urls=current_image_urls,
        )
    return history_messages


def _build_medical_runtime_tools() -> list[Any]:
    """
    功能描述：
        构造医疗节点运行时工具集合。

    参数说明：
        无。

    返回值：
        list[Any]: 医疗节点允许调用的工具列表。

    异常说明：
        无。
    """

    return [
        search_symptom_candidates,
        query_disease_candidates_by_symptoms,
        query_disease_detail,
        query_disease_details,
        query_followup_symptom_candidates,
        send_consultation_questionnaire_card,
        send_prescription_consent_card,
        open_user_patient_list,
        search_products,
        get_product_details,
        send_product_card,
        send_product_purchase_card,
    ]


def _build_medical_system_prompt(
        *,
        conversation_uuid: str,
        prescription_consent_status: PrescriptionConsentStatus | None,
        patient_info_status: PatientInfoStatus,
) -> str:
    """
    功能描述：
        构造医疗节点系统提示词。

    参数说明：
        conversation_uuid (str): 当前会话 UUID。
        prescription_consent_status (PrescriptionConsentStatus | None): 最近一次开药确认状态。

    返回值：
        str: 拼接缓存与标签目录后的完整提示词。

    异常说明：
        Exception: 读取商品标签目录失败时直接向上抛出。
    """

    final_prompt = load_managed_prompt(
        _MEDICAL_SYSTEM_PROMPT_KEY,
        local_prompt_path=_MEDICAL_SYSTEM_PROMPT_LOCAL_PATH,
    )
    diagnosis_cache_prompt = render_tool_cache_prompt(
        DIAGNOSIS_TOOL_CACHE_PROFILE,
        conversation_uuid,
    )
    commerce_cache_prompt = render_tool_cache_prompt(
        CLIENT_COMMERCE_TOOL_CACHE_PROFILE,
        conversation_uuid,
    )
    tag_prompt_text = _run_async(_get_cached_tag_filters())

    final_prompt = _append_prompt_section(
        base_prompt=final_prompt,
        section_title="当前交互状态",
        section_content=_build_prescription_consent_prompt(prescription_consent_status),
    )
    final_prompt = _append_prompt_section(
        base_prompt=final_prompt,
        section_title="当前就诊人资料状态",
        section_content=_build_patient_info_prompt(patient_info_status),
    )
    final_prompt = _append_prompt_section(
        base_prompt=final_prompt,
        section_title="当前已知疾病资料",
        section_content=diagnosis_cache_prompt,
    )
    final_prompt = _append_prompt_section(
        base_prompt=final_prompt,
        section_title="当前已知药品资料",
        section_content=commerce_cache_prompt,
    )
    if tag_prompt_text:
        final_prompt = f"{final_prompt.rstrip()}\n\n{tag_prompt_text}"
    return append_current_time_to_prompt(final_prompt)


@traceable(name="Client Assistant Medical Agent Node", run_type="chain")
def medical_agent(state: AgentState) -> dict[str, Any]:
    """
    功能描述：
        执行客户端统一医疗节点。

    参数说明：
        state (AgentState): 当前 client agent 工作流状态。

    返回值：
        dict[str, Any]: 医疗节点最终输出结果与消息列表。

    异常说明：
        不主动吞掉模型或工具异常；异常由上层工作流统一处理。
    """

    conversation_uuid = str(state.get("conversation_uuid") or "").strip()
    assistant_message_uuid = str(state.get("assistant_message_uuid") or "").strip()
    history_messages = _build_medical_history_messages(state)
    prescription_consent_status = _resolve_prescription_consent_from_state(state)
    patient_info_status = _resolve_patient_info_status_from_state(state)
    runtime_tools = _build_medical_runtime_tools()
    llm = create_agent_chat_llm(
        slot=AgentChatModelSlot.CLIENT_DIAGNOSIS,
        temperature=1.0,
        think=False,
        reasoning_override=bool(state.get("reasoning_enabled")),
    )
    agent = create_agent(
        model=llm,
        tools=runtime_tools,
        system_prompt=SystemMessage(
            content=_build_medical_system_prompt(
                conversation_uuid=conversation_uuid,
                prescription_consent_status=prescription_consent_status,
                patient_info_status=patient_info_status,
            )
        ),
        state_schema=_MEDICAL_AGENT_STATE_SCHEMA,
        middleware=[
            BasePromptMiddleware(),
            ToolTracePromptMiddleware(),
            SkillMiddleware(scope=_MEDICAL_SKILL_SCOPE),
            build_tool_status_middleware(),
            ToolCallLimitMiddleware(
                thread_limit=_MEDICAL_TOOL_CALL_THREAD_LIMIT,
                run_limit=_MEDICAL_TOOL_CALL_RUN_LIMIT,
            ),
        ],
    )
    diagnosis_cache_token = bind_tool_cache_conversation(
        DIAGNOSIS_TOOL_CACHE_PROFILE,
        conversation_uuid,
    )
    commerce_cache_token = bind_tool_cache_conversation(
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
            commerce_cache_token,
        )
        reset_tool_cache_conversation(
            DIAGNOSIS_TOOL_CACHE_PROFILE,
            diagnosis_cache_token,
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
    "medical_agent",
]
