from __future__ import annotations

from typing import Literal, TypeAlias

# 开药推荐确认状态类型。
PrescriptionConsentStatus: TypeAlias = Literal["confirmed", "rejected"]
# 开药推荐同意卡场景标识。
PRESCRIPTION_CONSENT_CARD_SCENE = "prescription_recommendation_consent"
# 开药推荐同意卡确认动作编码。
PRESCRIPTION_CONSENT_CONFIRM_ACTION = "confirm"
# 开药推荐同意卡拒绝动作编码。
PRESCRIPTION_CONSENT_REJECT_ACTION = "reject"
# 开药推荐同意文案。
PRESCRIPTION_CONFIRM_TEXT = "同意推荐药品"
# 开药推荐拒绝文案。
PRESCRIPTION_REJECT_TEXT = "暂不需要"
# 开药推荐同意卡默认说明文案。
PRESCRIPTION_CONSENT_CARD_DESCRIPTION = "当前病情已经基本清楚，是否同意我继续为你推荐药品？"
# 开药推荐同意卡确认后的直达节点。
PRESCRIPTION_CONSENT_CONFIRM_ROUTE_TARGET = "medical_agent"
# 开药推荐同意卡拒绝后的直达节点。
PRESCRIPTION_CONSENT_REJECT_ROUTE_TARGET = "medical_agent"
# 开药推荐同意卡工作流前缀。
PRESCRIPTION_CONSENT_WORKFLOW_PREFIX = "用户点击了开药推荐同意卡"
# 工作流文本中的选择字段前缀。
PRESCRIPTION_SELECTION_LABEL = "选择："


def build_prescription_consent_workflow_text(selected_value: str) -> str:
    """
    功能描述：
        构造开药推荐同意卡提交后的工作流文本。

    参数说明：
        selected_value (str): 用户本次点击的按钮值。

    返回值：
        str: 可直接写入会话历史与路由链路的工作流文本。

    异常说明：
        无。
    """

    normalized_selected_value = str(selected_value or "").strip()
    return (
        f"{PRESCRIPTION_CONSENT_WORKFLOW_PREFIX}，"
        f"{PRESCRIPTION_SELECTION_LABEL}{normalized_selected_value}"
    )


def is_prescription_consent_confirm_action(action: str) -> bool:
    """
    功能描述：
        判断结构化卡片点击动作是否表示同意继续推荐药品。

    参数说明：
        action (str): 当前点击动作编码。

    返回值：
        bool: `True` 表示当前动作为确认继续推荐药品。

    异常说明：
        无。
    """

    return str(action or "").strip() == PRESCRIPTION_CONSENT_CONFIRM_ACTION


def is_prescription_consent_reject_action(action: str) -> bool:
    """
    功能描述：
        判断结构化卡片点击动作是否表示暂不继续推荐药品。

    参数说明：
        action (str): 当前点击动作编码。

    返回值：
        bool: `True` 表示当前动作为拒绝继续推荐药品。

    异常说明：
        无。
    """

    return str(action or "").strip() == PRESCRIPTION_CONSENT_REJECT_ACTION


def resolve_prescription_consent_route_target(action: str) -> str | None:
    """
    功能描述：
        根据开药推荐同意卡点击动作解析直达节点。

    参数说明：
        action (str): 当前点击动作编码。

    返回值：
        str | None: 命中协议动作时返回目标节点；未命中时返回 `None`。

    异常说明：
        无。
    """

    normalized_action = str(action or "").strip()
    if normalized_action == PRESCRIPTION_CONSENT_CONFIRM_ACTION:
        return PRESCRIPTION_CONSENT_CONFIRM_ROUTE_TARGET
    if normalized_action == PRESCRIPTION_CONSENT_REJECT_ACTION:
        return PRESCRIPTION_CONSENT_REJECT_ROUTE_TARGET
    return None


def extract_prescription_selected_value(text: str) -> str:
    """
    功能描述：
        从开药推荐交互工作流文本中提取用户选择值。

    参数说明：
        text (str): 工作流文本。

    返回值：
        str: `选择：` 后面的用户选择文本；未命中时返回空字符串。

    异常说明：
        无。
    """

    normalized_text = str(text or "").strip()
    if PRESCRIPTION_SELECTION_LABEL not in normalized_text:
        return ""
    return normalized_text.split(PRESCRIPTION_SELECTION_LABEL, 1)[1].strip()


def is_prescription_confirm_text(text: str) -> bool:
    """
    功能描述：
        判断当前工作流文本是否表示同意继续推荐药品。

    参数说明：
        text (str): 工作流文本。

    返回值：
        bool: `True` 表示当前选择为同意推荐药品。

    异常说明：
        无。
    """

    return extract_prescription_selected_value(text) == PRESCRIPTION_CONFIRM_TEXT


def is_prescription_reject_text(text: str) -> bool:
    """
    功能描述：
        判断当前工作流文本是否表示暂不继续推荐药品。

    参数说明：
        text (str): 工作流文本。

    返回值：
        bool: `True` 表示当前选择为暂不需要。

    异常说明：
        无。
    """

    return extract_prescription_selected_value(text) == PRESCRIPTION_REJECT_TEXT


def resolve_prescription_consent_status(
        *,
        action: str | None = None,
        text: str | None = None,
) -> PrescriptionConsentStatus | None:
    """
    功能描述：
        统一解析开药推荐确认状态。

    参数说明：
        action (str | None): 结构化卡片点击动作编码。
        text (str | None): 交互工作流文本。

    返回值：
        PrescriptionConsentStatus | None: 命中时返回 `confirmed` 或 `rejected`，否则返回 `None`。

    异常说明：
        无。
    """

    if is_prescription_consent_confirm_action(str(action or "")):
        return "confirmed"
    if is_prescription_consent_reject_action(str(action or "")):
        return "rejected"
    if is_prescription_confirm_text(str(text or "")):
        return "confirmed"
    if is_prescription_reject_text(str(text or "")):
        return "rejected"
    return None


__all__ = [
    "PrescriptionConsentStatus",
    "PRESCRIPTION_CONFIRM_TEXT",
    "PRESCRIPTION_CONSENT_CONFIRM_ACTION",
    "PRESCRIPTION_CONSENT_CONFIRM_ROUTE_TARGET",
    "PRESCRIPTION_CONSENT_CARD_SCENE",
    "PRESCRIPTION_CONSENT_CARD_DESCRIPTION",
    "PRESCRIPTION_CONSENT_REJECT_ACTION",
    "PRESCRIPTION_CONSENT_REJECT_ROUTE_TARGET",
    "PRESCRIPTION_CONSENT_WORKFLOW_PREFIX",
    "PRESCRIPTION_REJECT_TEXT",
    "PRESCRIPTION_SELECTION_LABEL",
    "build_prescription_consent_workflow_text",
    "extract_prescription_selected_value",
    "is_prescription_consent_confirm_action",
    "is_prescription_consent_reject_action",
    "is_prescription_confirm_text",
    "is_prescription_reject_text",
    "resolve_prescription_consent_status",
    "resolve_prescription_consent_route_target",
]
