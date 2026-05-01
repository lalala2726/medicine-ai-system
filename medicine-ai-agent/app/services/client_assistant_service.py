from __future__ import annotations

import asyncio
import uuid
from dataclasses import dataclass
from typing import Any, Literal

from fastapi.responses import StreamingResponse
from langchain_core.messages import HumanMessage

from app.agent.client.state import CardActionState
from app.agent.client.workflow import build_graph
from app.core.agent.agent_orchestrator import AgentTraceRunConfig, AssistantStreamConfig
from app.core.agent.run_event_store import LocalRunHandle
from app.core.codes import ResponseCode
from app.core.config_sync import AgentChatModelSlot, get_current_agent_config_snapshot
from app.core.exception.exceptions import ServiceException
from app.core.langsmith import build_langsmith_runnable_config
from app.core.security.auth_context import get_user_id
from app.core.speech import build_message_tts_stream
from app.schemas.assistant_run import (
    AssistantRunStatus,
    AssistantRunStopResponse,
    AssistantRunSubmitResponse,
)
from app.schemas.admin_assistant_history import ConversationMessageResponse
from app.schemas.base_request import PageRequest
from app.schemas.client_assistant_submit import (
    ClientAssistantCardActionRequest,
    ClientAssistantAfterSaleCardRequest,
    ClientAssistantConsultProductCardRequest,
    ClientAssistantOrderCardRequest,
    ClientAssistantPatientCardRequest,
    ClientAssistantSubmitRequest,
)
from app.schemas.document.conversation import ConversationListItem, ConversationType
from app.schemas.document.message import MessageCard, MessageRole
from app.agent.services.card_render_service import render_product_card
from app.services.admin_assistant_service import (
    ConversationContext,
    RUN_EVENT_STORE,
    _build_attach_streaming_response,
    _build_background_run_done_callback,
    _map_exception,
    _run_assistant_workflow_in_background,
    _schedule_title_generation,
    _should_stream_token,
)
from app.services.assistant_message_service import (
    build_assistant_message_callback as _build_assistant_message_callback,
    create_placeholder_assistant_message as _create_placeholder_assistant_message,
    persist_user_message as _persist_user_message,
)
from app.services.conversation_service import (
    add_client_conversation,
    delete_client_conversation,
    get_client_conversation,
    list_client_conversations,
    update_client_conversation_title,
)
from app.services.memory_service import load_memory, resolve_assistant_memory_mode
from app.services.message_service import (
    count_messages,
    get_message_card_payload,
    hide_message_card,
    hide_visible_cards_in_conversation,
    list_messages,
)
from app.utils.chat_image_utils import (
    EMPTY_IMAGE_QUESTION_TEXT,
    build_user_image_markdown_content,
    normalize_chat_image_urls,
)
from app.utils.assistant_message_utils import (
    build_user_message_workflow_text,
    build_conversation_created_event as _build_conversation_created_event,
    build_message_prepared_event as _build_message_prepared_event,
    serialize_cards_for_history as _serialize_cards_for_history,
)
from app.agent.client.domain.prescription.protocol import (
    PRESCRIPTION_CONSENT_CARD_SCENE,
    build_prescription_consent_workflow_text,
    resolve_prescription_consent_route_target,
)

CLIENT_WORKFLOW_NAME = "client_assistant_graph"
CLIENT_WORKFLOW = build_graph()
# 图片发送能力不满足时的统一提示文案。
UNSUPPORTED_IMAGE_UNDERSTANDING_TEXT = "此模型不支持图片理解"
# 深度思考能力不满足时的统一提示文案。
UNSUPPORTED_REASONING_TEXT = "当前客户端助手暂未开启深度思考"
# 客户端图片发送要求同时支持图片理解的执行槽位集合。
CLIENT_IMAGE_REQUIRED_VISION_SLOTS = (
    AgentChatModelSlot.CLIENT_SERVICE,
    AgentChatModelSlot.CLIENT_DIAGNOSIS,
)
# 客户端深度思考要求同时支持的执行槽位集合。
CLIENT_REASONING_REQUIRED_SLOTS = (
    AgentChatModelSlot.CLIENT_SERVICE,
    AgentChatModelSlot.CLIENT_DIAGNOSIS,
)
# 同意卡片类型标识。
_CONSENT_CARD_TYPE = "consent-card"
# 选择卡片类型标识。
_SELECTION_CARD_TYPE = "selection-card"
# 商品推荐卡片类型标识。
_PRODUCT_CARD_TYPE = "product-card"
# 商品咨询卡片类型标识。
_CONSULT_PRODUCT_CARD_TYPE = "consult-product-card"
# 商品咨询卡片默认标题文案。
_CONSULT_PRODUCT_CARD_TITLE = "咨询商品"


@dataclass(frozen=True)
class ClientSubmitMessagePayload:
    """
    客户端 submit 消息规范化结果。

    Attributes:
        message_type: submit 消息类型。
        workflow_text: 供标题生成、记忆加载与工作流路由使用的最小号值文本。
        current_question: 当前轮用户原始问题文本（用于节点视觉接线）。
        image_urls: 当前轮用户上传图片 URL 列表。
        title_text: 新会话标题生成使用的文本。
        persist_content: 落库到 user message.content 的文本。
        persist_cards: 落库到 user message.cards 的结构化卡片列表。
        direct_route_target: 当前提交命中的结构化直达节点，可选。
        card_action: 当前提交解析出的卡片点击结构化上下文，可选。
    """

    message_type: Literal["text", "card"]
    workflow_text: str
    current_question: str
    image_urls: list[str]
    title_text: str
    persist_content: str
    persist_cards: list[MessageCard] | None = None
    direct_route_target: str | None = None
    card_action: CardActionState | None = None


@dataclass(frozen=True)
class ClientCardActionContext:
    """
    客户端交互卡片点击语义结果。

    Attributes:
        workflow_text: 写入工作流与路由链路的上下文文本。
        direct_route_target: 本次卡片点击命中的直达节点，可选。
        card_action: 本次卡片点击解析出的结构化上下文，可选。
    """

    workflow_text: str
    direct_route_target: str | None = None
    card_action: CardActionState | None = None


def get_client_chat_capability() -> dict[str, Any]:
    """
    读取客户端聊天输入区能力配置。

    Returns:
        dict[str, Any]:
            - `image_upload_enabled`: 当前客户端聊天是否允许上传图片。
            - `image_upload_disabled_message`: 图片上传禁用时的提示文案；允许上传时返回 `None`。
            - `reasoning_toggle_enabled`: 当前是否允许开启深度思考。
            - `reasoning_toggle_disabled_message`: 深度思考禁用时的提示文案；允许开启时返回 `None`。
    """

    config_snapshot = get_current_agent_config_snapshot()
    image_upload_enabled = all(
        config_snapshot.supports_vision_for_chat_slot(slot)
        for slot in CLIENT_IMAGE_REQUIRED_VISION_SLOTS
    )
    reasoning_toggle_enabled = _is_client_reasoning_toggle_available()
    return {
        "image_upload_enabled": image_upload_enabled,
        "image_upload_disabled_message": None if image_upload_enabled else UNSUPPORTED_IMAGE_UNDERSTANDING_TEXT,
        "reasoning_toggle_enabled": reasoning_toggle_enabled,
        "reasoning_toggle_disabled_message": None if reasoning_toggle_enabled else UNSUPPORTED_REASONING_TEXT,
    }


def _merge_runnable_config(
        *,
        run_name: str,
        conversation_uuid: str,
) -> dict[str, Any]:
    """
    功能描述：
        构造 workflow 执行配置，仅保留 LangGraph `thread_id`。

    参数说明：
        run_name (str): workflow 运行名称。
        conversation_uuid (str): 会话 UUID，同时作为 LangGraph `thread_id`。

    返回值：
        dict[str, Any]: 最终 runnable config。

    异常说明：
        无。
    """

    base_config = build_langsmith_runnable_config(
        run_name=run_name,
        tags=["client-assistant", "langgraph"],
        metadata={"entrypoint": "api.client_assistant.chat"},
    ) or {}
    configurable = dict(base_config.get("configurable") or {})
    configurable["thread_id"] = conversation_uuid
    merged_config = dict(base_config)
    merged_config["configurable"] = configurable
    return merged_config


def _invoke_workflow_with_config(
        *,
        workflow: Any,
        workflow_input: Any,
        runnable_config: dict[str, Any] | None,
) -> dict[str, Any]:
    """
    功能描述：
        同步执行 workflow，并在需要时透传 runnable config。

    参数说明：
        workflow (Any): 已编译的 workflow。
        workflow_input (Any): workflow 输入，通常为状态字典。
        runnable_config (dict[str, Any] | None): runnable config。

    返回值：
        dict[str, Any]: workflow 最终返回状态。

    异常说明：
        无；执行异常由调用方感知。
    """

    if runnable_config:
        return workflow.invoke(workflow_input, config=runnable_config)
    return workflow.invoke(workflow_input)


def _build_client_initial_state(
        *,
        conversation_uuid: str,
        assistant_message_uuid: str,
        history_messages: list[Any],
        current_question: str,
        current_image_urls: list[str],
        reasoning_enabled: bool = False,
        direct_route_target: str | None = None,
        card_action: CardActionState | None = None,
) -> dict[str, Any]:
    """
    功能描述：
        构造 client 主图的初始状态。

    参数说明：
        conversation_uuid (str): 当前会话 UUID。
        assistant_message_uuid (str): 当前轮 AI 占位消息 UUID。
        history_messages (list[Any]): 当前会话历史消息列表。
        current_question (str): 当前轮用户原始问题文本。
        current_image_urls (list[str]): 当前轮用户上传图片 URL 列表。
        reasoning_enabled (bool): 当前轮是否开启深度思考。
        direct_route_target (str | None): 当前提交命中的结构化直达节点。
        card_action (CardActionState | None): 当前提交解析出的卡片点击结构化上下文。

    返回值：
        dict[str, Any]: client workflow 初始状态。

    异常说明：
        无。
    """

    base_history = list(history_messages)
    normalized_direct_route_target = str(direct_route_target or "").strip()
    return {
        "conversation_uuid": conversation_uuid,
        "assistant_message_uuid": assistant_message_uuid,
        "routing": (
            {"route_target": normalized_direct_route_target}
            if normalized_direct_route_target
            else {}
        ),
        "card_action": dict(card_action) if isinstance(card_action, dict) else {},
        "context": "",
        "history_messages": base_history,
        "current_question": str(current_question or "").strip(),
        "current_image_urls": list(current_image_urls or []),
        "reasoning_enabled": bool(reasoning_enabled),
        "result": "",
        "messages": list(base_history),
    }


def _load_client_conversation(
        *,
        conversation_uuid: str,
        user_id: int,
) -> str:
    """加载 client 会话并返回 Mongo 会话 ID。"""

    conversation = get_client_conversation(
        conversation_uuid=conversation_uuid,
        user_id=user_id,
    )
    if conversation is None:
        raise ServiceException(code=ResponseCode.NOT_FOUND, message="会话不存在")

    conversation_id = conversation.id
    if conversation_id is None:
        raise ServiceException(code=ResponseCode.DATABASE_ERROR, message="会话数据异常")
    return str(conversation_id)


def _build_order_submit_message_card(
        *,
        card_request: ClientAssistantOrderCardRequest,
) -> MessageCard:
    """
    将订单卡提交请求转换为可持久化的消息卡片。

    Args:
        card_request: 已通过请求校验的订单卡提交数据。

    Returns:
        MessageCard: 可直接持久化的订单卡片对象。
    """

    return MessageCard.model_validate(
        {
            "id": str(uuid.uuid4()),
            "type": card_request.type,
            "data": card_request.data.model_dump(mode="json"),
        }
    )


def _build_after_sale_submit_message_card(
        *,
        card_request: ClientAssistantAfterSaleCardRequest,
) -> MessageCard:
    """
    将售后卡提交请求转换为可持久化的消息卡片。

    Args:
        card_request: 已通过请求校验的售后卡提交数据。

    Returns:
        MessageCard: 可直接持久化的售后卡片对象。
    """

    return MessageCard.model_validate(
        {
            "id": str(uuid.uuid4()),
            "type": card_request.type,
            "data": card_request.data.model_dump(mode="json"),
        }
    )


def _build_patient_submit_message_card(
        *,
        card_request: ClientAssistantPatientCardRequest,
) -> MessageCard:
    """
    将就诊人卡提交请求转换为可持久化的消息卡片。

    Args:
        card_request: 已通过请求校验的就诊人卡提交数据。

    Returns:
        MessageCard: 可直接持久化的就诊人卡片对象。
    """

    return MessageCard.model_validate(
        {
            "id": str(uuid.uuid4()),
            "type": card_request.type,
            "data": card_request.data.model_dump(mode="json"),
        }
    )


def _build_consult_product_message_card_data(*, rendered_card: Any) -> dict[str, Any]:
    """
    将推荐商品卡数据转换为商品咨询卡数据。

    Args:
        rendered_card: 业务端补全后的推荐商品卡对象。

    Returns:
        dict[str, Any]: 商品咨询卡 data 字段。

    Raises:
        ServiceException: 商品卡片数据不完整时抛出。
    """

    raw_card_data = getattr(rendered_card, "data", None)
    if not isinstance(raw_card_data, dict):
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="商品卡片数据不完整")
    raw_products = raw_card_data.get("products")
    if not isinstance(raw_products, list) or not raw_products:
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="商品卡片数据不完整")
    first_product = raw_products[0]
    if not isinstance(first_product, dict):
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="商品卡片数据不完整")
    product_id = str(first_product.get("id") or "").strip()
    product_name = str(first_product.get("name") or "").strip()
    product_image = str(first_product.get("image") or "").strip()
    product_price = str(first_product.get("price") or "").strip()
    if not product_id or not product_name or not product_image or not product_price:
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="商品卡片数据不完整")
    return {
        "title": _CONSULT_PRODUCT_CARD_TITLE,
        "product": {
            "id": product_id,
            "name": product_name,
            "image": product_image,
            "price": product_price,
        },
    }


async def _build_consult_product_submit_message_card(
        *,
        card_request: ClientAssistantConsultProductCardRequest,
) -> MessageCard:
    """
    将商品咨询卡请求转换为可持久化的商品卡片。

    Args:
        card_request: 已通过请求校验的商品咨询卡提交数据。

    Returns:
        MessageCard: 由业务端补全后的商品卡片对象。

    Raises:
        ServiceException: 商品不存在、已下架或卡片补全失败时抛出。
    """

    rendered_card = await render_product_card([card_request.data.product_id])
    if rendered_card is None or rendered_card.type != _PRODUCT_CARD_TYPE:
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="商品不存在或已下架")
    return MessageCard.model_validate(
        {
            "id": str(uuid.uuid4()),
            "type": _CONSULT_PRODUCT_CARD_TYPE,
            "data": _build_consult_product_message_card_data(rendered_card=rendered_card),
        }
    )


def _resolve_consult_product_conversation_title_text(
        *,
        product_card: MessageCard,
) -> str:
    """
    解析商品咨询新会话标题。

    Args:
        product_card: 已补全完成的商品卡片。

    Returns:
        str: 形如 `商品咨询 XXX` 的标题文本。

    Raises:
        ServiceException: 商品卡片缺少商品名称时抛出。
    """

    raw_product = product_card.data.get("product")
    if not isinstance(raw_product, dict):
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="商品卡片数据不完整")
    product_name = str(raw_product.get("name") or "").strip()
    if not product_name:
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="商品卡片数据不完整")
    return f"商品咨询 {product_name}"


def _resolve_order_card_workflow_text(
        *,
        card_request: ClientAssistantOrderCardRequest,
) -> str:
    """
    提取订单卡提交需要喂给工作流的最小号值文本。

    Args:
        card_request: 已通过请求校验的订单卡提交数据。

    Returns:
        str: 当前订单卡对应的订单编号。
    """

    return card_request.data.order_no


def _resolve_after_sale_card_workflow_text(
        *,
        card_request: ClientAssistantAfterSaleCardRequest,
) -> str:
    """
    提取售后卡提交需要喂给工作流的最小号值文本。

    Args:
        card_request: 已通过请求校验的售后卡提交数据。

    Returns:
        str: 当前售后卡对应的售后单号。
    """

    return card_request.data.after_sale_no


def _resolve_patient_card_conversation_title_text(
        *,
        card_request: ClientAssistantPatientCardRequest,
) -> str:
    """
    解析就诊人卡新会话标题。

    Args:
        card_request: 已通过请求校验的就诊人卡提交数据。

    Returns:
        str: 形如 `问诊资料 张三` 的标题文本。
    """

    return f"问诊资料 {card_request.data.name}"


def _build_generic_card_action_workflow_text(
        *,
        card_type: str,
        card_title: str | None,
        selected_text: str,
) -> str:
    """
    构造通用交互卡片点击后的工作流文本。

    Args:
        card_type: 当前卡片类型。
        card_title: 当前卡片标题。
        selected_text: 用户本次提交的文本内容。

    Returns:
        str: 可直接送入工作流的中文语义文本。
    """

    normalized_card_type = str(card_type or "").strip()
    if normalized_card_type == _CONSENT_CARD_TYPE:
        normalized_card_type = "同意卡"
    elif normalized_card_type == _SELECTION_CARD_TYPE:
        normalized_card_type = "选择卡"
    else:
        normalized_card_type = normalized_card_type or "交互卡片"
    normalized_card_title = str(card_title or "").strip()
    normalized_selected_text = str(selected_text or "").strip()
    if normalized_card_title and normalized_selected_text:
        return f"用户点击了卡片《{normalized_card_title}》，选择：{normalized_selected_text}"
    if normalized_card_title:
        return f"用户点击了卡片《{normalized_card_title}》"
    if normalized_selected_text:
        return f"用户点击了{normalized_card_type}，选择：{normalized_selected_text}"
    return f"用户点击了{normalized_card_type}"


def _build_card_action_state(
        *,
        card_type: str,
        card_scene: str,
        action: str,
) -> CardActionState | None:
    """
    功能描述：
        构造卡片点击的结构化工作流上下文字段。

    参数说明：
        card_type (str): 卡片类型标识。
        card_scene (str): 卡片场景标识。
        action (str): 点击动作编码。

    返回值：
        CardActionState | None: 可写入工作流状态的结构化上下文；缺少关键字段时返回 `None`。

    异常说明：
        无。
    """

    normalized_card_type = str(card_type or "").strip()
    normalized_action = str(action or "").strip()
    normalized_card_scene = str(card_scene or "").strip()
    if not normalized_card_type or not normalized_action:
        return None

    card_action_state: CardActionState = {
        "card_type": normalized_card_type,
        "action": normalized_action,
    }
    if normalized_card_scene:
        card_action_state["card_scene"] = normalized_card_scene
    return card_action_state


def _resolve_card_action_direct_route_target(
        *,
        card_type: str,
        card_scene: str,
        action: str,
) -> str | None:
    """
    功能描述：
        解析卡片点击后应直达的工作流节点。

    参数说明：
        card_type (str): 当前卡片类型。
        card_scene (str): 当前卡片场景标识。
        action (str): 当前点击动作编码。

    返回值：
        str | None: 命中结构化协议时返回目标节点；否则返回 `None`。

    异常说明：
        无。
    """

    normalized_card_type = str(card_type or "").strip()
    normalized_card_scene = str(card_scene or "").strip()
    if normalized_card_type != _CONSENT_CARD_TYPE:
        return None
    if normalized_card_scene != PRESCRIPTION_CONSENT_CARD_SCENE:
        return None
    return resolve_prescription_consent_route_target(action)


def _resolve_card_action_context(
        *,
        conversation_id: str,
        card_action: ClientAssistantCardActionRequest,
        content: str,
) -> ClientCardActionContext:
    """
    解析客户端交互卡片点击请求，并转换为工作流上下文文本。

    Args:
        conversation_id: 当前会话 Mongo 主键字符串。
        card_action: 前端上报的卡片点击动作。
        content: 用户本轮提交的文本内容。

    Returns:
        ClientCardActionContext: 已解析完成的卡片点击上下文。

    Raises:
        ServiceException: 当卡片不存在、消息归属不合法或卡片数据无效时抛出。
    """

    card_type = str(card_action.card_type or "").strip()
    action = str(card_action.action or "").strip()
    normalized_card_data: dict[str, Any] = {}
    if card_action.card_scene is not None:
        normalized_card_data["scene"] = card_action.card_scene
    if card_action.card_title is not None:
        normalized_card_data["title"] = card_action.card_title

    try:
        card_payload = get_message_card_payload(
            conversation_id=conversation_id,
            message_uuid=card_action.message_id,
            card_uuid=card_action.card_uuid,
        )
    except ServiceException:
        card_payload = None
    else:
        hide_message_card(
            conversation_id=conversation_id,
            message_uuid=card_action.message_id,
            card_uuid=card_action.card_uuid,
        )
        card_type = str(card_payload.get("type") or card_type).strip()
        raw_card_data = card_payload.get("data")
        if isinstance(raw_card_data, dict):
            normalized_card_data = {
                **dict(raw_card_data),
                **normalized_card_data,
            }

    normalized_content = str(content or "").strip()
    card_scene = str(normalized_card_data.get("scene") or "").strip()
    card_title = str(normalized_card_data.get("title") or "").strip()
    direct_route_target = _resolve_card_action_direct_route_target(
        card_type=card_type,
        card_scene=card_scene,
        action=action,
    )
    card_action_state = _build_card_action_state(
        card_type=card_type,
        card_scene=card_scene,
        action=action,
    ) if direct_route_target else None

    if not card_type:
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="卡片操作无效")

    if card_type == _CONSENT_CARD_TYPE:
        if card_scene == PRESCRIPTION_CONSENT_CARD_SCENE:
            return ClientCardActionContext(
                workflow_text=build_prescription_consent_workflow_text(
                    normalized_content,
                ),
                direct_route_target=direct_route_target,
                card_action=card_action_state,
            )
        return ClientCardActionContext(
            workflow_text=_build_generic_card_action_workflow_text(
                card_type=card_type,
                card_title=card_title,
                selected_text=normalized_content,
            ),
            direct_route_target=direct_route_target,
            card_action=card_action_state,
        )

    if card_type == _SELECTION_CARD_TYPE:
        return ClientCardActionContext(
            workflow_text=_build_generic_card_action_workflow_text(
                card_type=card_type,
                card_title=card_title,
                selected_text=normalized_content,
            ),
            direct_route_target=direct_route_target,
            card_action=card_action_state,
        )

    return ClientCardActionContext(
        workflow_text=_build_generic_card_action_workflow_text(
            card_type=card_type,
            card_title=card_title,
            selected_text=normalized_content,
        ),
        direct_route_target=direct_route_target,
        card_action=card_action_state,
    )


async def _resolve_submit_message_payload(
        *,
        submit_message: ClientAssistantSubmitRequest,
        user_id: int,
) -> ClientSubmitMessagePayload:
    """
    规范化客户端 submit 消息，并直接复用前端提交的完整卡片数据。

    Args:
        submit_message: 通过路由层校验后的 submit 请求模型。

    Returns:
        ClientSubmitMessagePayload: 规范化后的用户消息载荷。
    """

    normalized_image_urls = normalize_chat_image_urls(submit_message.image_urls)

    if submit_message.message_type == "text":
        content = str(submit_message.content or "").strip()
        if submit_message.card_action is not None:
            if submit_message.conversation_uuid is None:
                raise ServiceException(code=ResponseCode.BAD_REQUEST, message="卡片操作缺少会话UUID")
            conversation_id = _load_client_conversation(
                conversation_uuid=submit_message.conversation_uuid,
                user_id=user_id,
            )
            card_action_context = _resolve_card_action_context(
                conversation_id=conversation_id,
                card_action=submit_message.card_action,
                content=content,
            )
            return ClientSubmitMessagePayload(
                message_type="text",
                workflow_text=card_action_context.workflow_text,
                current_question=card_action_context.workflow_text,
                image_urls=[],
                title_text=card_action_context.workflow_text,
                persist_content=content,
                persist_cards=None,
                direct_route_target=card_action_context.direct_route_target,
                card_action=card_action_context.card_action,
            )

        if submit_message.card is not None:
            product_card = await _build_consult_product_submit_message_card(card_request=submit_message.card)
            product_workflow_text = build_user_message_workflow_text(content, [product_card])
            return ClientSubmitMessagePayload(
                message_type="text",
                workflow_text=product_workflow_text,
                current_question=product_workflow_text,
                image_urls=[],
                title_text=_resolve_consult_product_conversation_title_text(product_card=product_card),
                persist_content=content,
                persist_cards=[product_card],
                direct_route_target=None,
                card_action=None,
            )

        workflow_text = build_user_message_workflow_text(content, None)
        if not workflow_text and normalized_image_urls:
            workflow_text = EMPTY_IMAGE_QUESTION_TEXT

        return ClientSubmitMessagePayload(
            message_type="text",
            workflow_text=workflow_text,
            current_question=content or EMPTY_IMAGE_QUESTION_TEXT,
            image_urls=normalized_image_urls,
            title_text=content or ("图片咨询" if normalized_image_urls else ""),
            persist_content=build_user_image_markdown_content(content, normalized_image_urls),
            persist_cards=None,
            direct_route_target=None,
            card_action=None,
        )

    if submit_message.card.type == "order-card":
        order_card = _build_order_submit_message_card(card_request=submit_message.card)
        order_workflow_text = build_user_message_workflow_text("", [order_card])
        return ClientSubmitMessagePayload(
            message_type="card",
            workflow_text=order_workflow_text,
            current_question=order_workflow_text,
            image_urls=[],
            title_text=_resolve_order_card_workflow_text(card_request=submit_message.card),
            persist_content="",
            persist_cards=[order_card],
            direct_route_target=None,
            card_action=None,
        )

    if submit_message.card.type == "after-sale-card":
        after_sale_card = _build_after_sale_submit_message_card(card_request=submit_message.card)
        after_sale_workflow_text = build_user_message_workflow_text("", [after_sale_card])
        return ClientSubmitMessagePayload(
            message_type="card",
            workflow_text=after_sale_workflow_text,
            current_question=after_sale_workflow_text,
            image_urls=[],
            title_text=_resolve_after_sale_card_workflow_text(card_request=submit_message.card),
            persist_content="",
            persist_cards=[after_sale_card],
            direct_route_target=None,
            card_action=None,
        )

    patient_card = _build_patient_submit_message_card(card_request=submit_message.card)
    patient_workflow_text = build_user_message_workflow_text("", [patient_card])
    return ClientSubmitMessagePayload(
        message_type="card",
        workflow_text=patient_workflow_text,
        current_question=patient_workflow_text,
        image_urls=[],
        title_text=_resolve_patient_card_conversation_title_text(card_request=submit_message.card),
        persist_content="",
        persist_cards=[patient_card],
        direct_route_target=None,
        card_action=None,
    )


def _prepare_new_conversation(
        *,
        workflow_text: str,
        title_text: str,
        current_question: str,
        current_image_urls: list[str],
        user_id: int,
        assistant_message_uuid: str,
) -> ConversationContext:
    """准备新的 client 会话上下文。"""

    conversation_uuid = str(uuid.uuid4())
    conversation_id = add_client_conversation(
        conversation_uuid=conversation_uuid,
        user_id=user_id,
    )
    if conversation_id is None:
        raise ServiceException(code=ResponseCode.DATABASE_ERROR, message="无法创建会话，请稍后重试。")

    _schedule_title_generation(
        conversation_uuid=conversation_uuid,
        question=title_text,
    )
    return ConversationContext(
        conversation_uuid=conversation_uuid,
        conversation_id=conversation_id,
        assistant_message_uuid=assistant_message_uuid,
        history_messages=[HumanMessage(content=workflow_text)],
        current_question=current_question,
        current_image_urls=current_image_urls,
        initial_emitted_events=(
            _build_conversation_created_event(
                conversation_uuid=conversation_uuid,
                message_uuid=assistant_message_uuid,
            ),
        ),
        is_new_conversation=True,
    )


def _prepare_existing_conversation(
        *,
        conversation_uuid: str,
        user_id: int,
        workflow_text: str,
        current_question: str,
        current_image_urls: list[str],
        assistant_message_uuid: str,
) -> ConversationContext:
    """准备已存在的 client 会话上下文。"""

    conversation_id = _load_client_conversation(
        conversation_uuid=conversation_uuid,
        user_id=user_id,
    )
    _hide_visible_conversation_cards(
        conversation_id=conversation_id,
    )
    memory = load_memory(
        memory_type=resolve_assistant_memory_mode(),
        conversation_uuid=conversation_uuid,
        user_id=user_id,
        include_history_hidden=False,
    )
    history_messages = [*list(memory.messages), HumanMessage(content=workflow_text)]
    return ConversationContext(
        conversation_uuid=conversation_uuid,
        conversation_id=conversation_id,
        assistant_message_uuid=assistant_message_uuid,
        history_messages=history_messages,
        current_question=current_question,
        current_image_urls=current_image_urls,
        initial_emitted_events=(
            _build_message_prepared_event(
                message_uuid=assistant_message_uuid,
            ),
        ),
        is_new_conversation=False,
    )


def _prepare_conversation_context(
        *,
        workflow_text: str,
        title_text: str,
        current_question: str,
        current_image_urls: list[str],
        user_id: int,
        conversation_uuid: str | None,
        assistant_message_uuid: str,
) -> ConversationContext:
    """统一准备 client 会话上下文。"""

    if conversation_uuid is None:
        return _prepare_new_conversation(
            workflow_text=workflow_text,
            title_text=title_text,
            current_question=current_question,
            current_image_urls=current_image_urls,
            user_id=user_id,
            assistant_message_uuid=assistant_message_uuid,
        )
    return _prepare_existing_conversation(
        conversation_uuid=conversation_uuid,
        user_id=user_id,
        workflow_text=workflow_text,
        current_question=current_question,
        current_image_urls=current_image_urls,
        assistant_message_uuid=assistant_message_uuid,
    )


def _hide_visible_conversation_cards(
        *,
        conversation_id: str,
) -> None:
    """在加载旧会话 memory 前批量隐藏当前仍可见的 AI 卡片。"""

    hide_visible_cards_in_conversation(
        conversation_id=conversation_id,
    )


def _ensure_client_submit_image_vision_supported(*, image_urls: list[str] | None) -> None:
    """
    校验客户端聊天提交的图片能力是否满足。

    Args:
        image_urls: 本次提交附带的图片 URL 列表。

    Returns:
        None

    Raises:
        ServiceException: 当请求携带图片且任一客户端执行槽位不支持图片理解时抛出。
    """

    normalized_image_urls = normalize_chat_image_urls(image_urls)
    if not normalized_image_urls:
        return

    config_snapshot = get_current_agent_config_snapshot()
    supports_all_slots = all(
        config_snapshot.supports_vision_for_chat_slot(slot)
        for slot in CLIENT_IMAGE_REQUIRED_VISION_SLOTS
    )
    if supports_all_slots:
        return

    raise ServiceException(
        code=ResponseCode.BAD_REQUEST,
        message=UNSUPPORTED_IMAGE_UNDERSTANDING_TEXT,
    )


def _ensure_client_submit_reasoning_supported(*, reasoning_enabled: bool) -> None:
    """
    校验客户端聊天提交的深度思考能力是否满足。

    Args:
        reasoning_enabled: 当前轮是否尝试开启深度思考。

    Returns:
        None

    Raises:
        ServiceException: 当请求开启深度思考且任一执行槽位不支持时抛出。
    """

    if not reasoning_enabled:
        return

    if _is_client_reasoning_toggle_available():
        return

    raise ServiceException(
        code=ResponseCode.BAD_REQUEST,
        message=UNSUPPORTED_REASONING_TEXT,
    )


def _is_client_reasoning_toggle_available() -> bool:
    """
    判断客户端聊天输入框当前是否允许展示深度思考开关。

    Returns:
        bool: 仅当管理端已开启统一深度思考，且服务节点与诊断节点模型都支持深度思考时返回 `True`。
    """

    config_snapshot = get_current_agent_config_snapshot()
    if not config_snapshot.is_client_reasoning_enabled():
        return False
    return all(
        config_snapshot.supports_reasoning_for_chat_slot(slot)
        for slot in CLIENT_REASONING_REQUIRED_SLOTS
    )


async def assistant_chat(
        *,
        submit_message: ClientAssistantSubmitRequest,
) -> AssistantRunSubmitResponse:
    """客户端助手聊天提交入口（创建后台 run 并返回运行态）。"""

    _ensure_client_submit_image_vision_supported(image_urls=submit_message.image_urls)
    _ensure_client_submit_reasoning_supported(reasoning_enabled=submit_message.reasoning_enabled)

    current_user_id = get_user_id()
    prepared_message = await _resolve_submit_message_payload(
        submit_message=submit_message,
        user_id=current_user_id,
    )
    assistant_message_uuid = str(uuid.uuid4())
    context = _prepare_conversation_context(
        workflow_text=prepared_message.workflow_text,
        title_text=prepared_message.title_text,
        current_question=prepared_message.current_question,
        current_image_urls=prepared_message.image_urls,
        user_id=current_user_id,
        conversation_uuid=submit_message.conversation_uuid,
        assistant_message_uuid=assistant_message_uuid,
    )

    created_meta = RUN_EVENT_STORE.create_run(
        conversation_uuid=context.conversation_uuid,
        user_id=current_user_id,
        conversation_type=ConversationType.CLIENT.value,
        assistant_message_uuid=context.assistant_message_uuid,
    )
    if created_meta is None:
        active_meta = RUN_EVENT_STORE.get_run_meta(
            conversation_uuid=context.conversation_uuid,
        )
        if active_meta is not None and active_meta.status == AssistantRunStatus.RUNNING:
            raise ServiceException(
                code=ResponseCode.CONFLICT,
                message="当前会话已有正在输出的回答",
                data=AssistantRunSubmitResponse(
                    conversation_uuid=context.conversation_uuid,
                    message_uuid=active_meta.assistant_message_uuid,
                    run_status=active_meta.status,
                ).model_dump(),
            )
        raise ServiceException(
            code=ResponseCode.SERVICE_UNAVAILABLE,
            message="创建助手运行态失败",
        )

    _persist_user_message(
        conversation_id=context.conversation_id,
        content=prepared_message.persist_content,
        cards=prepared_message.persist_cards,
    )
    _create_placeholder_assistant_message(
        conversation_id=context.conversation_id,
        message_uuid=context.assistant_message_uuid,
    )

    cancel_event = asyncio.Event()
    resolved_workflow = CLIENT_WORKFLOW
    resolved_run_name = CLIENT_WORKFLOW_NAME
    runnable_config = _merge_runnable_config(
        run_name=resolved_run_name,
        conversation_uuid=context.conversation_uuid,
    )
    stream_config = AssistantStreamConfig(
        workflow=resolved_workflow,
        build_initial_state=(
            lambda _question: _build_client_initial_state(
                conversation_uuid=context.conversation_uuid,
                assistant_message_uuid=context.assistant_message_uuid,
                history_messages=context.history_messages,
                current_question=context.current_question,
                current_image_urls=context.current_image_urls,
                reasoning_enabled=submit_message.reasoning_enabled,
                direct_route_target=prepared_message.direct_route_target,
                card_action=prepared_message.card_action,
            )
        ),
        extract_final_content=(lambda state: str(state.get("result") or "")),
        should_stream_token=_should_stream_token,
        build_stream_config=lambda: runnable_config,
        invoke_sync=lambda workflow_input: _invoke_workflow_with_config(
            workflow=resolved_workflow,
            workflow_input=workflow_input,
            runnable_config=runnable_config,
        ),
        map_exception=_map_exception,
        on_answer_completed=_build_assistant_message_callback(
            conversation_id=context.conversation_id,
            assistant_message_uuid=context.assistant_message_uuid,
        ),
        initial_emitted_events=context.initial_emitted_events,
        is_cancel_requested=lambda: (
                cancel_event.is_set()
                or RUN_EVENT_STORE.is_cancel_requested(
            conversation_uuid=context.conversation_uuid,
        )
        ),
        trace_config=AgentTraceRunConfig(
            graph_name=resolved_run_name,
            conversation_uuid=context.conversation_uuid,
            assistant_message_uuid=context.assistant_message_uuid,
            user_id=current_user_id,
            conversation_type=ConversationType.CLIENT.value,
            entrypoint="api.client_assistant.chat",
        ),
    )
    background_task = asyncio.create_task(
        _run_assistant_workflow_in_background(
            question=prepared_message.current_question,
            context=context,
            stream_config=stream_config,
        )
    )
    background_task.add_done_callback(
        _build_background_run_done_callback(
            conversation_uuid=context.conversation_uuid,
        )
    )
    RUN_EVENT_STORE.register_local_handle(
        conversation_uuid=context.conversation_uuid,
        handle=LocalRunHandle(
            task=background_task,
            cancel_event=cancel_event,
        ),
    )
    return AssistantRunSubmitResponse(
        conversation_uuid=context.conversation_uuid,
        message_uuid=context.assistant_message_uuid,
        run_status=AssistantRunStatus.RUNNING,
    )


def assistant_chat_stream(
        *,
        conversation_uuid: str,
        last_event_id: str | None = None,
) -> StreamingResponse:
    """attach 到客户端助手当前的流式 run。"""

    current_user_id = get_user_id()
    _load_client_conversation(
        conversation_uuid=conversation_uuid,
        user_id=current_user_id,
    )
    meta = RUN_EVENT_STORE.get_run_meta(conversation_uuid=conversation_uuid)
    if meta is None:
        raise ServiceException(
            code=ResponseCode.NOT_FOUND,
            message="当前会话没有可连接的流式输出",
        )
    return _build_attach_streaming_response(
        conversation_uuid=conversation_uuid,
        last_event_id=last_event_id,
    )


def assistant_chat_stop(
        *,
        conversation_uuid: str,
) -> AssistantRunStopResponse:
    """停止客户端助手当前会话的流式 run。"""

    current_user_id = get_user_id()
    _load_client_conversation(
        conversation_uuid=conversation_uuid,
        user_id=current_user_id,
    )
    meta = RUN_EVENT_STORE.request_cancel(conversation_uuid=conversation_uuid)
    if meta is None or meta.status != AssistantRunStatus.RUNNING:
        raise ServiceException(
            code=ResponseCode.NOT_FOUND,
            message="当前会话没有运行中的输出",
        )
    return AssistantRunStopResponse(
        conversation_uuid=conversation_uuid,
        message_uuid=meta.assistant_message_uuid,
        run_status=meta.status,
        stop_requested=True,
    )


def assistant_message_tts_stream(
        *,
        message_uuid: str,
) -> StreamingResponse:
    """
    客户端助手消息转语音（HTTP chunked audio stream）。

    说明：
    - 先基于 `message_uuid` 校验消息存在性、client 会话归属与消息角色；
    - 校验通过后建立上游 Volcengine 双向 TTS websocket；
    - 下游以音频字节流（chunked）持续返回给前端。

    Args:
        message_uuid: 目标 AI 消息 UUID。

    Returns:
        StreamingResponse: 下游音频流响应对象。
    """

    current_user_id = get_user_id()
    tts_stream = build_message_tts_stream(
        message_uuid=message_uuid,
        user_id=current_user_id,
        conversation_type=ConversationType.CLIENT,
    )
    return StreamingResponse(
        tts_stream.audio_stream,
        media_type=tts_stream.media_type,
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
        },
    )


def conversation_list(
        *,
        page_request: PageRequest,
) -> tuple[list[ConversationListItem], int]:
    """分页查询当前用户的 client 会话列表。"""

    current_user_id = get_user_id()
    return list_client_conversations(
        user_id=current_user_id,
        page_num=page_request.page_num,
        page_size=page_request.page_size,
    )


def conversation_messages(
        *,
        conversation_uuid: str,
        page_request: PageRequest,
) -> tuple[list[ConversationMessageResponse], int]:
    """分页查询当前用户某个 client 会话的历史消息。"""

    normalized_uuid = conversation_uuid.strip()
    if not normalized_uuid:
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="会话UUID不能为空")

    current_user_id = get_user_id()
    conversation_id = _load_client_conversation(
        conversation_uuid=normalized_uuid,
        user_id=current_user_id,
    )

    skip = (page_request.page_num - 1) * page_request.page_size
    total = count_messages(
        conversation_id=conversation_id,
        history_hidden=False,
    )
    message_documents = list_messages(
        conversation_id=conversation_id,
        limit=page_request.page_size,
        skip=skip,
        ascending=False,
        history_hidden=False,
    )

    result: list[ConversationMessageResponse] = []
    for document in reversed(message_documents):
        role = "user" if document.role == MessageRole.USER else "ai"
        raw_thinking = getattr(document, "thinking", None)
        normalized_thinking = (
            raw_thinking.strip()
            if isinstance(raw_thinking, str) and raw_thinking.strip()
            else None
        )
        serialized_cards = _serialize_cards_for_history(
            getattr(document, "cards", None),
            hidden_card_uuids=getattr(document, "hidden_card_uuids", None),
        )
        if role == "ai":
            if not document.content.strip() and normalized_thinking is None and serialized_cards is None:
                continue
        elif not document.content.strip() and serialized_cards is None:
            continue
        payload: dict[str, Any] = {
            "id": document.uuid,
            "role": role,
            "content": document.content,
        }
        if serialized_cards is not None:
            payload["cards"] = serialized_cards
        if role == "ai":
            payload["status"] = document.status.value
            if normalized_thinking is not None:
                payload["thinking"] = normalized_thinking
        result.append(ConversationMessageResponse.model_validate(payload))
    return result, total


def delete_conversation(
        *,
        conversation_uuid: str,
) -> None:
    """
    删除当前用户的客户端助手会话。

    Args:
        conversation_uuid: 会话 UUID。

    Raises:
        ServiceException:
            - BAD_REQUEST: 会话 UUID 为空；
            - NOT_FOUND: 会话不存在或无权限；
            - DATABASE_ERROR: 数据库异常。
    """

    normalized_uuid = conversation_uuid.strip()
    if not normalized_uuid:
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="会话UUID不能为空")

    current_user_id = get_user_id()
    deleted = delete_client_conversation(
        conversation_uuid=normalized_uuid,
        user_id=current_user_id,
    )
    if not deleted:
        raise ServiceException(code=ResponseCode.NOT_FOUND, message="会话不存在")


def update_conversation_title(
        *,
        conversation_uuid: str,
        title: str,
) -> str:
    """
    更新当前用户客户端助手会话标题。

    Args:
        conversation_uuid: 会话 UUID。
        title: 新标题。

    Returns:
        str: 归一化后的标题（strip 后）。

    Raises:
        ServiceException:
            - BAD_REQUEST: 会话 UUID 或标题为空；
            - NOT_FOUND: 会话不存在或无权限；
            - DATABASE_ERROR: 数据库异常。
    """

    normalized_uuid = conversation_uuid.strip()
    if not normalized_uuid:
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="会话UUID不能为空")

    normalized_title = title.strip()
    if not normalized_title:
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="会话标题不能为空")

    current_user_id = get_user_id()
    updated = update_client_conversation_title(
        conversation_uuid=normalized_uuid,
        user_id=current_user_id,
        title=normalized_title,
    )
    if not updated:
        raise ServiceException(code=ResponseCode.NOT_FOUND, message="会话不存在")

    return normalized_title
