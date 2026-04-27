from __future__ import annotations

from typing import Any

from langchain_core.tools import tool
from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.agent.client.domain.prescription.protocol import (
    PRESCRIPTION_CONSENT_CONFIRM_ACTION,
    PRESCRIPTION_CONSENT_CARD_DESCRIPTION,
    PRESCRIPTION_CONFIRM_TEXT,
    PRESCRIPTION_CONSENT_CARD_SCENE,
    PRESCRIPTION_CONSENT_REJECT_ACTION,
    PRESCRIPTION_REJECT_TEXT,
)
from app.agent.client.domain.tools.card_tools import build_card_response
from app.core.agent.agent_event_bus import enqueue_final_sse_response
from app.core.agent.middleware import (
    tool_call_status,
    tool_thinking_redaction,
    tool_trace_record,
)
from app.schemas.sse_response import Card
from app.utils.http_client import HttpClient

# 开药域工具固定成功返回值。
PRESCRIPTION_TOOL_SUCCESS_RESULT = "__SUCCESS__"
# 商品标签目录接口固定路径。
PRODUCT_SEARCH_TAG_FILTERS_URL = "/agent/client/product/search/tag-filters"


class SendPrescriptionConsentCardRequest(BaseModel):
    """发送开药推荐同意卡工具入参。"""

    model_config = ConfigDict(
        extra="forbid",
        json_schema_extra={
            "example": {
                "title": "当前更像：上呼吸道感染",
                "description": "当前病情已经基本清楚，是否同意我继续为你推荐药品？",
            }
        },
    )

    title: str = Field(
        ...,
        min_length=1,
        description="卡片标题，必须直接写当前已经收敛出的病情信息。",
    )
    description: str | None = Field(
        default=None,
        description="卡片补充说明；为空时默认使用继续推荐药品的确认文案。",
    )

    @field_validator("title", "description")
    @classmethod
    def _normalize_text(cls, value: str | None) -> str | None:
        """
        功能描述：
            统一清洗标题与说明文本。

        参数说明：
            value (str | None): 原始文本。

        返回值：
            str | None: 去除首尾空白后的文本；空白值返回 `None`。

        异常说明：
            无。
        """

        if value is None:
            return None
        normalized_value = value.strip()
        return normalized_value or None


async def _fetch_product_search_tag_filters() -> list[dict[str, Any]]:
    """
    功能描述：
        读取系统当前启用的商品搜索标签目录。

    参数说明：
        无。

    返回值：
        list[dict[str, Any]]: 标签类型分组列表。

    异常说明：
        Exception: HTTP 请求异常会直接向上抛出，由调用方感知。
    """

    async with HttpClient() as client:
        payload = await client.get(
            PRODUCT_SEARCH_TAG_FILTERS_URL,
            response_format="json",
        )
    if not isinstance(payload, list):
        return []
    return [dict(item) for item in payload if isinstance(item, dict)]


def _extract_tag_names(raw_options: Any) -> list[str]:
    """
    功能描述：
        从标签选项列表中提取标签名称集合。

    参数说明：
        raw_options (Any): 原始标签选项列表。

    返回值：
        list[str]: 当前标签分组下的标签名称列表。

    异常说明：
        无。
    """

    if not isinstance(raw_options, list):
        return []

    tag_names: list[str] = []
    for raw_option in raw_options:
        if not isinstance(raw_option, dict):
            continue
        tag_name = str(raw_option.get("tagName") or "").strip()
        if not tag_name or tag_name in tag_names:
            continue
        tag_names.append(tag_name)
    return tag_names


async def build_product_search_tag_prompt_text() -> str:
    """
    功能描述：
        把系统商品标签目录渲染成开药节点系统提示词附加段。

    参数说明：
        无。

    返回值：
        str: 面向模型的标签目录提示词片段；标签为空时返回空字符串。

    异常说明：
        Exception: HTTP 请求异常会直接向上抛出，由调用方感知。
    """

    filter_groups = await _fetch_product_search_tag_filters()
    if not filter_groups:
        return ""

    rendered_lines: list[str] = [
        "## 系统商品标签目录",
        "",
        "以下是系统当前真实存在的商品标签目录。组织搜索词时优先使用这些标签，不要编造系统不存在的标签名称，也不要把这些标签直接作为用户交互选项输出给用户。",
    ]
    for filter_group in filter_groups:
        type_name = str(filter_group.get("typeName") or "").strip()
        type_code = str(filter_group.get("typeCode") or "").strip()
        tag_names = _extract_tag_names(filter_group.get("options"))
        if not type_name or not tag_names:
            continue
        if type_code:
            rendered_lines.append(
                f"- {type_name}（{type_code}）：{'、'.join(tag_names)}"
            )
        else:
            rendered_lines.append(f"- {type_name}：{'、'.join(tag_names)}")

    if len(rendered_lines) == 3:
        return ""
    rendered_lines.extend(
        [
            "",
            "搜索时优先把疾病方向、图谱里的药物线索和上述标签组合成更精确的关键词；标签只用于内部检索，不要再向用户发标签选择卡。",
        ]
    )
    return "\n".join(rendered_lines)


@tool(
    args_schema=SendPrescriptionConsentCardRequest,
    description=(
            "向前端发送推荐药品同意卡。"
            "调用时机：当前仍处于纯问诊路径，但诊断方向已经基本收敛，"
            "需要先确认用户是否同意继续推荐药品时。"
    ),
)
@tool_trace_record()
@tool_thinking_redaction(display_name="发送推荐药品同意卡")
@tool_call_status(
    tool_name="发送推荐药品同意卡",
    start_message="正在生成推荐药品确认卡片",
    error_message="推荐药品同意卡发送失败",
    timely_message="推荐药品同意卡仍在处理中",
)
def send_prescription_consent_card(
        title: str,
        description: str | None = None,
) -> str:
    """
    功能描述：
        发送开药推荐同意卡。

    参数说明：
        title (str): 卡片标题，必须是当前已经收敛出的病情信息。
        description (str | None): 卡片补充说明；为空时使用默认确认文案。

    返回值：
        str: 固定成功标记字符串。

    异常说明：
        无。
    """

    request = SendPrescriptionConsentCardRequest(
        title=title,
        description=description,
    )
    enqueue_final_sse_response(
        build_card_response(
            Card(
                type="consent-card",
                data={
                    "scene": PRESCRIPTION_CONSENT_CARD_SCENE,
                    "title": request.title,
                    "description": request.description or PRESCRIPTION_CONSENT_CARD_DESCRIPTION,
                    "confirm": {
                        "action": PRESCRIPTION_CONSENT_CONFIRM_ACTION,
                        "label": PRESCRIPTION_CONFIRM_TEXT,
                        "value": PRESCRIPTION_CONFIRM_TEXT,
                    },
                    "reject": {
                        "action": PRESCRIPTION_CONSENT_REJECT_ACTION,
                        "label": PRESCRIPTION_REJECT_TEXT,
                        "value": PRESCRIPTION_REJECT_TEXT,
                    },
                },
            ),
            persist_card=True,
        )
    )
    return PRESCRIPTION_TOOL_SUCCESS_RESULT


__all__ = [
    "PRESCRIPTION_TOOL_SUCCESS_RESULT",
    "PRODUCT_SEARCH_TAG_FILTERS_URL",
    "SendPrescriptionConsentCardRequest",
    "build_product_search_tag_prompt_text",
    "send_prescription_consent_card",
]
