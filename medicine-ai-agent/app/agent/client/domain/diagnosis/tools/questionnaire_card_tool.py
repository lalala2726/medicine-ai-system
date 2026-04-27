from __future__ import annotations

from typing import Any

from langchain_core.tools import tool
from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.agent.client.domain.tools.card_tools import build_card_response
from app.core.agent.middleware import (
    tool_call_status,
    tool_thinking_redaction,
    tool_trace_record,
)
from app.core.agent.agent_event_bus import enqueue_final_sse_response
from app.schemas.sse_response import Card
from app.utils.list_utils import TextListUtils

# 诊断问卷卡片类型标识。
CONSULTATION_QUESTIONNAIRE_CARD_TYPE = "consultation-questionnaire-card"
# 诊断问卷卡工具固定成功返回值。
QUESTIONNAIRE_CARD_TOOL_SUCCESS_RESULT = "__SUCCESS__"
# 诊断问卷卡问题数量上限。
MAX_CONSULTATION_QUESTIONNAIRE_COUNT = 5


class ConsultationQuestionnaireQuestionItem(BaseModel):
    """诊断问卷卡中的单个问题项。"""

    model_config = ConfigDict(extra="forbid")

    question: str = Field(..., min_length=1, description="前端展示的问题文本。")
    options: list[str] = Field(
        ...,
        min_length=2,
        max_length=5,
        description="当前问题对应的可点击选项列表。",
    )

    @field_validator("question")
    @classmethod
    def _validate_question(cls, value: str) -> str:
        """校验单个问题文本。

        Args:
            value: 原始问题文本。

        Returns:
            str: 去空白后的有效问题文本。
        """

        normalized_questions = TextListUtils.normalize_unique_required(
            [value],
            field_name="question",
        )
        return normalized_questions[0]

    @field_validator("options")
    @classmethod
    def _validate_options(cls, value: list[str]) -> list[str]:
        """校验单题选项列表。

        Args:
            value: 原始选项列表。

        Returns:
            list[str]: 去空白且不重复后的有效选项列表。
        """

        return TextListUtils.normalize_unique_required(
            value,
            field_name="options",
        )


class ConsultationQuestionnaireCardData(BaseModel):
    """诊断问卷卡片数据。"""

    model_config = ConfigDict(extra="forbid")

    questions: list[ConsultationQuestionnaireQuestionItem] = Field(
        ...,
        min_length=1,
        max_length=MAX_CONSULTATION_QUESTIONNAIRE_COUNT,
        description="诊断问卷问题列表。",
    )


class SendConsultationQuestionnaireCardRequest(BaseModel):
    """发送诊断问卷卡片工具参数。"""

    model_config = ConfigDict(
        extra="forbid",
        json_schema_extra={
            "example": {
                "questions": [
                    {
                        "question": "这两天有发热吗？",
                        "options": ["有", "没有", "不确定"],
                    },
                    {
                        "question": "有没有咳嗽？",
                        "options": ["有", "没有", "偶尔"],
                    },
                ]
            }
        },
    )

    questions: list[ConsultationQuestionnaireQuestionItem] = Field(
        ...,
        min_length=1,
        max_length=MAX_CONSULTATION_QUESTIONNAIRE_COUNT,
        description="节点已经生成好的诊断问卷问题与选项列表。",
    )


def _build_questionnaire_card_data(
        raw_questions: list[dict[str, Any]] | list[ConsultationQuestionnaireQuestionItem],
) -> ConsultationQuestionnaireCardData:
    """将工具原始入参转换为诊断问卷卡片数据。

    Args:
        raw_questions: 工具调用传入的原始问题与选项列表。

    Returns:
        ConsultationQuestionnaireCardData: 校验通过后的诊断问卷卡片数据。
    """

    request = SendConsultationQuestionnaireCardRequest.model_validate(
        {"questions": raw_questions}
    )
    return ConsultationQuestionnaireCardData(questions=request.questions)


@tool(
    args_schema=SendConsultationQuestionnaireCardRequest,
    description=(
            "向前端发送诊断问卷卡。"
            "调用时机：当你需要继续补充病情信息时，"
            "并且已经为每个问题生成好了适合点击的 options 选项时。"
            "调用时必须直接传入结构化的 questions 列表；"
            "每个元素都要包含 question 和 options，question 是自然语言问题，"
            "options 是 2 到 5 个简短中文选项。"
            "即使当前只差 1 个关键问题，也可以发送单题问卷卡，不要直接文字追问。"
    ),
)
@tool_trace_record()
@tool_thinking_redaction(display_name="发送诊断问卷卡")
@tool_call_status(
    tool_name="发送诊断问卷卡",
    start_message="正在整理关键问题并生成问诊卡片",
    error_message="诊断问卷卡发送失败",
    timely_message="诊断问卷卡仍在处理中",
)
def send_consultation_questionnaire_card(
        questions: list[dict[str, Any]] | list[ConsultationQuestionnaireQuestionItem],
) -> str:
    """构建并发送诊断问卷卡。

    Args:
        questions: 节点已经生成好的结构化问题与选项列表。

    Returns:
        str: 固定成功标记字符串。
    """

    card_data = _build_questionnaire_card_data(questions)
    enqueue_final_sse_response(
        build_card_response(
            Card(
                type=CONSULTATION_QUESTIONNAIRE_CARD_TYPE,
                data=card_data.model_dump(mode="json", exclude_none=True),
            ),
            persist_card=True,
        )
    )
    return QUESTIONNAIRE_CARD_TOOL_SUCCESS_RESULT


__all__ = [
    "CONSULTATION_QUESTIONNAIRE_CARD_TYPE",
    "ConsultationQuestionnaireCardData",
    "ConsultationQuestionnaireQuestionItem",
    "MAX_CONSULTATION_QUESTIONNAIRE_COUNT",
    "QUESTIONNAIRE_CARD_TOOL_SUCCESS_RESULT",
    "SendConsultationQuestionnaireCardRequest",
    "send_consultation_questionnaire_card",
]
