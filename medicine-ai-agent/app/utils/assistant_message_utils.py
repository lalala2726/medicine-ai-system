from __future__ import annotations

from datetime import date, datetime
from typing import Any

from app.schemas.assistant_run import AssistantRunStatus
from app.schemas.document.message import MessageStatus
from app.schemas.sse_response import AssistantResponse, Content, MessageType

# 订单卡片类型标识。
ORDER_CARD_TYPE = "order-card"
# 售后卡片类型标识。
AFTER_SALE_CARD_TYPE = "after-sale-card"
# 商品卡片类型标识。
PRODUCT_CARD_TYPE = "product-card"
# 商品咨询卡片类型标识。
CONSULT_PRODUCT_CARD_TYPE = "consult-product-card"
# 就诊人卡片类型标识。
PATIENT_CARD_TYPE = "patient-card"
# 就诊人卡片工作流文本前缀。
PATIENT_CARD_WORKFLOW_PREFIX = "用户发送一个就诊人卡片"


def _normalize_patient_profile_text(
        value: Any,
        *,
        empty_text: str = "未提供",
) -> str:
    """
    功能描述：
        标准化就诊人资料中的文本字段，并为缺失值补齐统一占位文案。

    参数说明：
        value (Any): 原始字段值。
        empty_text (str): 缺失值占位文案。

    返回值：
        str: 去掉首尾空白后的字段值；缺失时返回 `empty_text`。

    异常说明：
        无。
    """

    normalized_value = str(value or "").strip()
    return normalized_value or empty_text


def _resolve_patient_gender_text(data: dict[str, Any]) -> str:
    """
    功能描述：
        从就诊人卡片中解析稳定的性别展示文案。

    参数说明：
        data (dict[str, Any]): 就诊人卡 data 字段。

    返回值：
        str: 性别展示文案，无法识别时返回 `未知`。

    异常说明：
        无。
    """

    explicit_gender_text = str(data.get("gender_text") or "").strip()
    if explicit_gender_text:
        return explicit_gender_text

    normalized_gender = str(data.get("gender") or "").strip()
    if normalized_gender == "1":
        return "男"
    if normalized_gender == "2":
        return "女"
    return "未知"


def _resolve_patient_default_text(value: Any) -> str:
    """
    功能描述：
        解析就诊人是否默认的展示文案。

    参数说明：
        value (Any): 原始默认标记。

    返回值：
        str: `是`、`否` 或 `未知`。

    异常说明：
        无。
    """

    if value in {1, "1", True, "true", "True"}:
        return "是"
    if value in {0, "0", False, "false", "False"}:
        return "否"
    return "未知"


def _resolve_patient_age_text(birth_date_text: str) -> str:
    """
    功能描述：
        根据出生日期解析年龄文案。

    参数说明：
        birth_date_text (str): 就诊人出生日期字符串。

    返回值：
        str: 形如 `28岁` 的年龄文案；无法计算时返回 `未知`。

    异常说明：
        无。
    """

    normalized_birth_date = str(birth_date_text or "").strip()
    if not normalized_birth_date:
        return "未知"

    try:
        resolved_birth_date = datetime.strptime(normalized_birth_date[:10], "%Y-%m-%d").date()
    except ValueError:
        return "未知"

    today = date.today()
    age = today.year - resolved_birth_date.year
    if (today.month, today.day) < (resolved_birth_date.month, resolved_birth_date.day):
        age -= 1
    if age < 0:
        return "未知"
    return f"{age}岁"


def _build_patient_card_context_text(data: dict[str, Any]) -> str:
    """
    功能描述：
        将就诊人卡片数据转换为工作流可直接消费的结构化中文上下文。

    参数说明：
        data (dict[str, Any]): 就诊人卡 data 字段。

    返回值：
        str: 完整的就诊人资料上下文文本。

    异常说明：
        无。
    """

    patient_name = _normalize_patient_profile_text(data.get("name"))
    gender_text = _resolve_patient_gender_text(data)
    birth_date_text = _normalize_patient_profile_text(data.get("birth_date"))
    relationship_text = _normalize_patient_profile_text(data.get("relationship"))
    age_text = _resolve_patient_age_text(birth_date_text)
    allergy_text = _normalize_patient_profile_text(data.get("allergy"))
    past_medical_history_text = _normalize_patient_profile_text(data.get("past_medical_history"))
    chronic_disease_text = _normalize_patient_profile_text(data.get("chronic_disease"))
    long_term_medications_text = _normalize_patient_profile_text(data.get("long_term_medications"))
    default_text = _resolve_patient_default_text(data.get("is_default"))

    context_parts = [
        f"姓名: {patient_name}",
        f"性别: {gender_text}",
        f"年龄: {age_text}",
        f"出生日期: {birth_date_text}",
        f"关系: {relationship_text}",
        f"默认就诊人: {default_text}",
        f"过敏史: {allergy_text}",
        f"既往病史: {past_medical_history_text}",
        f"慢性病: {chronic_disease_text}",
        f"长期用药: {long_term_medications_text}",
    ]
    return f"{PATIENT_CARD_WORKFLOW_PREFIX}，" + "，".join(context_parts)


def _normalize_user_card_payload(raw_card: Any) -> dict[str, Any] | None:
    """
    功能描述：
        将不同形态的用户卡片对象统一归一化为字典结构。

    参数说明：
        raw_card (Any): 原始卡片对象，可能是 Pydantic 模型、普通字典或自定义对象。

    返回值：
        dict[str, Any] | None: 归一化后的卡片字典；无法识别时返回 `None`。

    异常说明：
        无。
    """

    if hasattr(raw_card, "model_dump"):
        payload = raw_card.model_dump(mode="json", exclude_none=True)
    elif isinstance(raw_card, dict):
        payload = raw_card
    else:
        payload = {
            "type": getattr(raw_card, "type", None),
            "data": getattr(raw_card, "data", None),
        }

    if not isinstance(payload, dict):
        return None
    return payload


def _extract_product_card_context(data: dict[str, Any]) -> tuple[str, str]:
    """
    功能描述：
        从商品卡片数据中提取商品 ID 与商品名称。

    参数说明：
        data (dict[str, Any]): 商品卡片 data 字段。

    返回值：
        tuple[str, str]: `(product_id, product_name)`；任一字段缺失时返回空字符串。

    异常说明：
        无。
    """

    raw_product = data.get("product")
    if isinstance(raw_product, dict):
        product_id = str(raw_product.get("id") or "").strip()
        product_name = str(raw_product.get("name") or "").strip()
        return product_id, product_name

    raw_products = data.get("products")
    if isinstance(raw_products, list) and raw_products:
        first_product = raw_products[0]
        if isinstance(first_product, dict):
            product_id = str(first_product.get("id") or "").strip()
            product_name = str(first_product.get("name") or "").strip()
            return product_id, product_name

    product_id = str(data.get("product_id") or data.get("productId") or "").strip()
    product_name = str(data.get("product_name") or data.get("productName") or "").strip()
    return product_id, product_name


def extract_user_card_identifier_text(raw_cards: Any) -> str:
    """
    功能描述：
        从用户卡片列表中提取最小号值文本，供记忆与摘要链路复用。

    参数说明：
        raw_cards (Any): 原始卡片列表，元素可以是字典或具备 `type/data` 属性的对象。

    返回值：
        str: 命中的订单编号或售后单号；未命中时返回空字符串。

    异常说明：
        无。
    """

    if not isinstance(raw_cards, list):
        return ""

    for raw_card in raw_cards:
        payload = _normalize_user_card_payload(raw_card)
        if payload is None:
            continue
        card_type = str(payload.get("type") or "").strip()
        data = payload.get("data")
        if not isinstance(data, dict):
            continue
        if card_type == ORDER_CARD_TYPE:
            order_no = str(data.get("order_no") or "").strip()
            if order_no:
                return order_no
        if card_type == AFTER_SALE_CARD_TYPE:
            after_sale_no = str(data.get("after_sale_no") or "").strip()
            if after_sale_no:
                return after_sale_no
        if card_type == PATIENT_CARD_TYPE:
            patient_name = str(data.get("name") or "").strip()
            if patient_name:
                return patient_name
            patient_id = str(data.get("patient_id") or "").strip()
            if patient_id:
                return patient_id

    return ""


def build_user_message_workflow_text(raw_content: Any, raw_cards: Any) -> str:
    """
    功能描述：
        将用户消息文本与结构化卡片上下文合并为工作流可直接消费的完整问题文本。

    参数说明：
        raw_content (Any): 用户消息原始文本内容。
        raw_cards (Any): 用户消息原始卡片列表。

    返回值：
        str: 合并后的工作流输入文本；当消息完全不可用时返回空字符串。

    异常说明：
        无。
    """

    normalized_content = str(raw_content or "").strip()
    if not isinstance(raw_cards, list):
        return normalized_content

    for raw_card in raw_cards:
        payload = _normalize_user_card_payload(raw_card)
        if payload is None:
            continue
        card_type = str(payload.get("type") or "").strip()
        data = payload.get("data")
        if not isinstance(data, dict):
            continue
        if card_type in {PRODUCT_CARD_TYPE, CONSULT_PRODUCT_CARD_TYPE}:
            product_id, product_name = _extract_product_card_context(data)
            context_parts: list[str] = []
            if product_id:
                context_parts.append(f"商品ID: {product_id}")
            if product_name:
                context_parts.append(f"商品名称: {product_name}")
            if not context_parts:
                continue
            context_text = "，".join(context_parts)
            if normalized_content:
                return f"用户发送一个商品咨询卡片，{context_text}，用户问题: {normalized_content}"
            return f"用户发送一个商品咨询卡片，{context_text}"
        if card_type == ORDER_CARD_TYPE:
            order_no = str(data.get("order_no") or "").strip()
            if not order_no:
                continue
            if normalized_content:
                return f"用户发送一个订单卡片，订单编号: {order_no}，用户问题: {normalized_content}"
            return f"用户发送一个订单卡片，订单编号: {order_no}"
        if card_type == AFTER_SALE_CARD_TYPE:
            after_sale_no = str(data.get("after_sale_no") or "").strip()
            if not after_sale_no:
                continue
            if normalized_content:
                return f"用户发送一个售后卡片，售后单号: {after_sale_no}，用户问题: {normalized_content}"
            return f"用户发送一个售后卡片，售后单号: {after_sale_no}"
        if card_type == PATIENT_CARD_TYPE:
            patient_context_text = _build_patient_card_context_text(data)
            if normalized_content:
                return f"{patient_context_text}，用户问题: {normalized_content}"
            return patient_context_text

    return normalized_content


def serialize_cards_for_history(
        raw_cards: Any,
        *,
        hidden_card_uuids: list[str] | None = None,
) -> list[dict[str, Any]] | None:
    """
    功能描述：
        将消息文档中的 cards 归一化为历史接口可直接返回的结构。

    参数说明：
        raw_cards (Any): 原始卡片数据，通常来自消息文档中的 `cards` 字段。
        hidden_card_uuids (list[str] | None): 需要从历史中隐藏的卡片 UUID 列表。

    返回值：
        list[dict[str, Any]] | None: 归一化后的卡片列表；没有合法卡片时返回 `None`。

    异常说明：
        无。
    """

    if not isinstance(raw_cards, list) or not raw_cards:
        return None

    hidden_card_uuid_set = {
        str(card_uuid).strip()
        for card_uuid in (hidden_card_uuids or [])
        if str(card_uuid).strip()
    }
    serialized_cards: list[dict[str, Any]] = []
    for raw_card in raw_cards:
        if hasattr(raw_card, "model_dump"):
            payload = raw_card.model_dump(mode="json", exclude_none=True)
        elif isinstance(raw_card, dict):
            payload = raw_card
        else:
            card_id = str(
                getattr(raw_card, "card_uuid", None)
                or getattr(raw_card, "id", "")
                or ""
            ).strip()
            card_type = str(getattr(raw_card, "type", "") or "").strip()
            card_data = getattr(raw_card, "data", None)
            if not card_id or not card_type or not isinstance(card_data, dict):
                continue
            payload = {
                "card_uuid": card_id,
                "type": card_type,
                "data": card_data,
            }

        if not isinstance(payload, dict):
            continue
        card_uuid = str(payload.get("card_uuid") or payload.get("id") or "").strip()
        if not card_uuid:
            continue
        if card_uuid in hidden_card_uuid_set:
            continue
        if not str(payload.get("type") or "").strip():
            continue
        if not isinstance(payload.get("data"), dict):
            continue
        serialized_cards.append(
            {
                "card_uuid": card_uuid,
                "type": payload["type"],
                "data": payload["data"],
            }
        )

    return serialized_cards or None


def build_conversation_created_event(
        *,
        conversation_uuid: str,
        message_uuid: str,
) -> AssistantResponse:
    """
    功能描述：
        构造“会话创建成功”的前置 SSE 事件。

    参数说明：
        conversation_uuid (str): 会话 UUID。
        message_uuid (str): AI 消息 UUID。

    返回值：
        AssistantResponse: 标准 notice 事件。

    异常说明：
        无。
    """

    return AssistantResponse(
        content=Content(
            state="created",
            message="会话创建成功",
        ),
        type=MessageType.NOTICE,
        meta={
            "conversation_uuid": conversation_uuid,
            "message_uuid": message_uuid,
        },
    )


def build_message_prepared_event(
        *,
        message_uuid: str,
) -> AssistantResponse:
    """
    功能描述：
        构造“消息已创建”的前置 SSE 事件。

    参数说明：
        message_uuid (str): AI 消息 UUID。

    返回值：
        AssistantResponse: 标准 notice 事件。

    异常说明：
        无。
    """

    return AssistantResponse(
        type=MessageType.NOTICE,
        meta={
            "message_uuid": message_uuid,
        },
    )


def resolve_message_status_from_finish_status(
        *,
        finish_status: AssistantRunStatus,
) -> MessageStatus:
    """
    功能描述：
        根据运行终态解析消息状态。

    参数说明：
        finish_status (AssistantRunStatus): 运行最终状态。

    返回值：
        MessageStatus: 对应的消息状态枚举。

    异常说明：
        无。
    """

    if finish_status == AssistantRunStatus.CANCELLED:
        return MessageStatus.CANCELLED
    if finish_status == AssistantRunStatus.WAITING_INPUT:
        return MessageStatus.WAITING_INPUT
    if finish_status == AssistantRunStatus.ERROR:
        return MessageStatus.ERROR
    return MessageStatus.SUCCESS
