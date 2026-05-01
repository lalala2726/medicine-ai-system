from typing import Any

from fastapi import APIRouter, Depends, Header, Path, Query, Request
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

from app.core.security.pre_authorize import RoleCode, has_permission, has_role, pre_authorize
from app.core.security.rate_limit import RateLimitPreset, RateLimitRule, rate_limit
from app.schemas.assistant_run import (
    AssistantRunStopRequest,
    AssistantRunStopResponse,
    AssistantRunSubmitResponse,
)
from app.schemas.admin_assistant_history import (
    ConversationMessagesRequest,
)
from app.schemas.base_request import PageRequest
from app.schemas.document.conversation import ConversationListItem
from app.schemas.response import ApiResponse, PageResponse
from app.services.admin_assistant_service import (
    assistant_message_tts_stream as assistant_message_tts_stream_service,
    assistant_chat_stop,
    assistant_chat_stream,
    assistant_chat_submit as assistant_chat_submit_service,
    conversation_list as conversation_list_service,
    conversation_messages as conversation_messages_service,
    delete_conversation as delete_conversation_service,
    update_conversation_title as update_conversation_title_service,
)

router = APIRouter(prefix="/admin/assistant", tags=["智能助手"])

# 管理端智能助手访问权限码，与后台前端 `system:smart_assistant` 保持一致。
ADMIN_ASSISTANT_ACCESS_PERMISSION = "system:smart_assistant"
# 管理端智能助手会话标题修改权限码，用于限制写操作。
ADMIN_ASSISTANT_UPDATE_PERMISSION = "admin:assistant:update"
# 管理端智能助手会话删除权限码，用于限制破坏性操作。
ADMIN_ASSISTANT_DELETE_PERMISSION = "admin:assistant:delete"

# 聊天率限制规则
CHAT_RATE_LIMIT_RULES = (
    RateLimitRule.preset(RateLimitPreset.MINUTE_1, limit=10),
    RateLimitRule.preset(RateLimitPreset.MINUTE_5, limit=30),
    RateLimitRule.preset(RateLimitPreset.HOUR_1, limit=120),
    RateLimitRule.preset(RateLimitPreset.HOUR_24, limit=600),
)

# 语音合成率限制规则
TTS_RATE_LIMIT_RULES = (
    RateLimitRule.preset(RateLimitPreset.MINUTE_1, limit=5),
    RateLimitRule.preset(RateLimitPreset.HOUR_1, limit=60),
    RateLimitRule.preset(RateLimitPreset.HOUR_5, limit=100),
    RateLimitRule.preset(RateLimitPreset.HOUR_24, limit=200),
)


class AssistantRequest(BaseModel):
    """AI助手请求参数。"""

    model_config = ConfigDict(extra="forbid")

    question: str | None = Field(default=None, description="问题")
    image_urls: list[str] | None = Field(
        default=None,
        min_length=1,
        max_length=5,
        description="图片 URL 列表（最多 5 张）",
    )
    conversation_uuid: str | None = Field(
        default=None,
        min_length=1,
        description="会话UUID",
    )
    model_name: str = Field(
        ...,
        min_length=1,
        description="用户手动选择的前端自定义模型名；后端会映射为真实模型名",
    )
    reasoning_enabled: bool = Field(
        ...,
        description="是否开启深度思考；由前端按当前选中模型显式传入",
    )

    @field_validator("question")
    @classmethod
    def validate_question(cls, value: str | None) -> str | None:
        """
        标准化用户问题文本。

        作用：
        1. 去掉首尾空白，避免把仅空格内容传入聊天主流程；
        2. 在路由层提前阻断无效请求，减少下游 service 分支处理复杂度。
        """

        if value is None:
            return None
        normalized = value.strip()
        return normalized or None

    @field_validator("image_urls")
    @classmethod
    def validate_image_urls(cls, value: list[str] | None) -> list[str] | None:
        """
        标准化图片 URL 列表。

        作用：
        1. 去掉每个 URL 的首尾空白，确保后续链路输入稳定；
        2. 校验 URL 不允许为空字符串，数量限制由字段约束负责。
        """

        if value is None:
            return None

        normalized_image_urls: list[str] = []
        for raw_image_url in value:
            normalized_image_url = str(raw_image_url or "").strip()
            if not normalized_image_url:
                raise ValueError("图片地址不能为空")
            normalized_image_urls.append(normalized_image_url)
        return normalized_image_urls

    @field_validator("conversation_uuid")
    @classmethod
    def validate_conversation_uuid(cls, value: str | None) -> str | None:
        """
        标准化会话 UUID。

        作用：
        1. 去掉首尾空白，确保 service 层拿到的 UUID 稳定；
        2. 对于纯空白 UUID，统一视为 None，表示创建新会话。
        """

        if value is None:
            return None
        normalized = value.strip()
        return normalized or None

    @field_validator("model_name")
    @classmethod
    def validate_model_name(cls, value: str) -> str:
        """
        标准化前端展示模型名称。

        作用：
        1. 去掉首尾空白，保证后续映射查找稳定；
        2. 阻断纯空白模型名，避免进入运行时后再报模糊错误。
        """

        normalized = value.strip()
        if not normalized:
            raise ValueError("model_name 不能为空")
        return normalized

    @model_validator(mode="after")
    def validate_payload(self) -> "AssistantRequest":
        """
        校验请求体至少包含问题文本或图片列表。

        Returns:
            AssistantRequest: 校验通过后的请求模型。

        Raises:
            ValueError: 当 `question` 与 `image_urls` 同时为空时抛出。
        """

        if self.question is None and not self.image_urls:
            raise ValueError("问题和图片不能同时为空")
        return self


class AssistantMessageTtsRequest(BaseModel):
    """管理助手消息转语音请求参数。"""

    model_config = ConfigDict(extra="forbid")

    message_uuid: str = Field(..., min_length=1, description="消息 UUID")

    @field_validator("message_uuid")
    @classmethod
    def validate_message_uuid(cls, value: str) -> str:
        normalized = value.strip()
        if not normalized:
            raise ValueError("message_uuid 不能为空")
        return normalized


class ConversationListRequest(BaseModel):
    """智能助手会话列表请求参数。"""
    model_config = ConfigDict(extra="forbid")

    page_num: int = Field(default=1, ge=1, description="页号")
    page_size: int = Field(default=20, ge=1, le=100, description="每页大小")


class UpdateConversationTitleRequest(BaseModel):
    """修改会话标题请求参数。"""

    model_config = ConfigDict(extra="forbid")

    title: str = Field(..., min_length=1, max_length=100, description="会话标题")


@router.post("/chat/submit", summary="管理助手提交对话")
@rate_limit(
    rules=CHAT_RATE_LIMIT_RULES,
    subjects=("user_id",),
    scope="admin_assistant_chat",
    fail_open=False,
)
@pre_authorize(
    lambda: has_role(RoleCode.SUPER_ADMIN) or has_permission(ADMIN_ASSISTANT_ACCESS_PERMISSION)
)
async def assistant_submit(
        _request: Request,
        request: AssistantRequest,
) -> ApiResponse[AssistantRunSubmitResponse]:
    """
    管理助手聊天提交入口。

    Args:
        _request: FastAPI 原始请求对象（当前实现仅用于依赖注入与中间件链路）。
        request: 聊天请求体，包含问题与可选会话 UUID。

    Returns:
        ApiResponse[AssistantRunSubmitResponse]: 运行态响应对象。
    """

    return ApiResponse.success(
        data=assistant_chat_submit_service(
            question=request.question or "",
            image_urls=request.image_urls,
            conversation_uuid=request.conversation_uuid,
            model_name=request.model_name,
            reasoning_enabled=request.reasoning_enabled,
        )
    )


@router.get("/chat/stream", summary="管理助手 attach 流式输出")
@pre_authorize(
    lambda: has_role(RoleCode.SUPER_ADMIN) or has_permission(ADMIN_ASSISTANT_ACCESS_PERMISSION)
)
async def assistant_stream(
        conversation_uuid: str = Query(..., min_length=1, description="会话UUID"),
        last_event_id: str | None = Header(
            default=None,
            alias="Last-Event-ID",
            description="客户端已消费的最后事件 ID",
        ),
) -> StreamingResponse:
    """
    attach 到指定会话当前的后台流式运行。

    Args:
        conversation_uuid: 会话 UUID。
        last_event_id: 客户端已消费到的最后事件 ID。

    Returns:
        StreamingResponse: SSE attach 流。
    """

    return assistant_chat_stream(
        conversation_uuid=conversation_uuid,
        last_event_id=last_event_id,
    )


@router.post("/chat/stop", summary="管理助手停止当前输出")
@pre_authorize(
    lambda: has_role(RoleCode.SUPER_ADMIN) or has_permission(ADMIN_ASSISTANT_ACCESS_PERMISSION)
)
async def assistant_stop(
        request: AssistantRunStopRequest,
) -> ApiResponse[AssistantRunStopResponse]:
    """
    请求停止指定会话当前的后台 run。

    Args:
        request: 停止请求体，仅包含 `conversation_uuid`。

    Returns:
        ApiResponse[AssistantRunStopResponse]: 停止请求响应。
    """

    return ApiResponse.success(
        data=assistant_chat_stop(
            conversation_uuid=request.conversation_uuid,
        )
    )


@router.post("/message/tts/stream", summary="管理助手消息转语音（流式）")
@rate_limit(
    rules=TTS_RATE_LIMIT_RULES,
    subjects=("user_id",),
    scope="admin_assistant_tts",
    fail_open=False,
)
@pre_authorize(
    lambda: has_role(RoleCode.SUPER_ADMIN) or has_permission(ADMIN_ASSISTANT_ACCESS_PERMISSION)
)
async def assistant_message_tts_stream(
        _request: Request,
        request: AssistantMessageTtsRequest,
) -> StreamingResponse:
    """根据消息 UUID 生成语音并以 HTTP chunked 流式返回音频数据。"""

    return assistant_message_tts_stream_service(
        message_uuid=request.message_uuid,
    )


@router.get("/conversation/list", summary="管理助手会话列表")
@pre_authorize(
    lambda: has_role(RoleCode.SUPER_ADMIN) or has_permission(ADMIN_ASSISTANT_ACCESS_PERMISSION)
)
async def conversation_list(
        request: ConversationListRequest = Depends(),
) -> ApiResponse[PageResponse[ConversationListItem]]:
    """
    分页查询管理助手会话列表（仅返回会话 UUID 与标题）。
    """

    rows, total = conversation_list_service(
        page_request=PageRequest(
            page_num=request.page_num,
            page_size=request.page_size,
        )
    )
    return ApiResponse.page(
        rows=rows,
        total=total,
        page_num=request.page_num,
        page_size=request.page_size,
    )


@router.delete("/conversation/{conversation_uuid}", summary="删除管理助手会话")
@pre_authorize(
    lambda: has_role(RoleCode.SUPER_ADMIN) or has_permission(ADMIN_ASSISTANT_DELETE_PERMISSION)
)
async def delete_conversation(
        conversation_uuid: str = Path(..., min_length=1, description="会话UUID"),
) -> ApiResponse[dict[str, str]]:
    """删除管理助手会话"""
    delete_conversation_service(conversation_uuid=conversation_uuid)
    return ApiResponse.success(
        data={"conversation_uuid": conversation_uuid},
        message="删除成功",
    )


@router.put("/conversation/{conversation_uuid}", summary="修改管理助手会话标题")
@pre_authorize(
    lambda: has_role(RoleCode.SUPER_ADMIN) or has_permission(ADMIN_ASSISTANT_UPDATE_PERMISSION)
)
async def update_conversation_title(
        request: UpdateConversationTitleRequest,
        conversation_uuid: str = Path(..., min_length=1, description="会话UUID"),
) -> ApiResponse[dict[str, str]]:
    """
    修改管理助手会话标题。
    """

    normalized_title = update_conversation_title_service(
        conversation_uuid=conversation_uuid,
        title=request.title,
    )
    return ApiResponse.success(
        data={
            "conversation_uuid": conversation_uuid,
            "title": normalized_title,
        },
        message="修改成功",
    )


@router.get("/history/{conversation_uuid}", summary="管理助手历史消息")
@pre_authorize(
    lambda: has_role(RoleCode.SUPER_ADMIN) or has_permission(ADMIN_ASSISTANT_ACCESS_PERMISSION)
)
async def conversation_messages(
        conversation_uuid: str = Path(..., min_length=1, description="会话UUID"),
        request: ConversationMessagesRequest = Depends(),
) -> ApiResponse[PageResponse[dict[str, Any]]]:
    """
    分页查询管理助手历史消息。
    """

    messages, total = conversation_messages_service(
        conversation_uuid=conversation_uuid,
        page_request=PageRequest(
            page_num=request.page_num,
            page_size=request.page_size,
        ),
    )
    serialized_messages = [
        message.model_dump(by_alias=True, exclude_none=True)
        for message in messages
    ]
    return ApiResponse.page(
        rows=serialized_messages,
        total=total,
        page_num=request.page_num,
        page_size=request.page_size,
    )
