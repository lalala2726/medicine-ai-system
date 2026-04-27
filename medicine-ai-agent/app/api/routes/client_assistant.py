from typing import Any

from fastapi import APIRouter, Depends, Header, Path, Query, Request
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.core.security.rate_limit import RateLimitPreset, RateLimitRule, rate_limit
from app.schemas.assistant_run import (
    AssistantRunStopRequest,
    AssistantRunStopResponse,
    AssistantRunSubmitResponse,
)
from app.schemas.admin_assistant_history import ConversationMessagesRequest
from app.schemas.base_request import PageRequest
from app.schemas.client_assistant_submit import ClientAssistantSubmitRequest
from app.schemas.document.conversation import ConversationListItem
from app.schemas.response import ApiResponse, PageResponse
from app.services.client_assistant_service import (
    assistant_chat_stop,
    assistant_chat_stream,
    assistant_chat,
    get_client_chat_capability as get_client_chat_capability_service,
    assistant_message_tts_stream as assistant_message_tts_stream_service,
    conversation_list as conversation_list_service,
    conversation_messages as conversation_messages_service,
    delete_conversation as delete_conversation_service,
    update_conversation_title as update_conversation_title_service,
)

router = APIRouter(prefix="/client/assistant", tags=["客户端助手"])

CHAT_RATE_LIMIT_RULES = (
    RateLimitRule.preset(RateLimitPreset.MINUTE_1, limit=10),
    RateLimitRule.preset(RateLimitPreset.MINUTE_5, limit=30),
    RateLimitRule.preset(RateLimitPreset.HOUR_1, limit=120),
    RateLimitRule.preset(RateLimitPreset.HOUR_24, limit=600),
)

TTS_RATE_LIMIT_RULES = (
    RateLimitRule.preset(RateLimitPreset.MINUTE_1, limit=5),
    RateLimitRule.preset(RateLimitPreset.HOUR_1, limit=60),
    RateLimitRule.preset(RateLimitPreset.HOUR_5, limit=100),
    RateLimitRule.preset(RateLimitPreset.HOUR_24, limit=200),
)

class ConversationListRequest(BaseModel):
    """客户端助手会话列表请求参数。"""

    model_config = ConfigDict(
        extra="forbid",
        json_schema_extra={
            "example": {
                "page_num": 1,
                "page_size": 20,
            }
        },
    )

    page_num: int = Field(default=1, ge=1, description="页号")
    page_size: int = Field(default=20, ge=1, le=100, description="每页大小")


class UpdateConversationTitleRequest(BaseModel):
    """修改客户端助手会话标题请求参数。"""

    model_config = ConfigDict(
        extra="forbid",
        json_schema_extra={
            "example": {
                "title": "退款进度咨询",
            }
        },
    )

    title: str = Field(..., min_length=1, max_length=100, description="会话标题")


class ClientAssistantMessageTtsRequest(BaseModel):
    """客户端助手消息转语音请求参数。"""

    model_config = ConfigDict(extra="forbid")

    message_uuid: str = Field(..., min_length=1, description="AI 消息 UUID")

    @field_validator("message_uuid")
    @classmethod
    def validate_message_uuid(cls, value: str) -> str:
        """
        标准化消息 UUID。

        Args:
            value: 原始消息 UUID。

        Returns:
            str: 去掉首尾空白后的消息 UUID。

        Raises:
            ValueError: 归一化后为空时抛出。
        """

        normalized = value.strip()
        if not normalized:
            raise ValueError("message_uuid 不能为空")
        return normalized


class ClientAssistantChatCapabilityResponse(BaseModel):
    """客户端聊天输入区能力响应。"""

    model_config = ConfigDict(extra="forbid")

    image_upload_enabled: bool = Field(..., description="当前是否允许上传图片")
    image_upload_disabled_message: str | None = Field(
        default=None,
        description="图片上传禁用时的提示文案",
    )
    reasoning_toggle_enabled: bool = Field(..., description="当前是否允许开启深度思考")
    reasoning_toggle_disabled_message: str | None = Field(
        default=None,
        description="深度思考禁用时的提示文案",
    )


@router.get(
    "/chat/capability",
    summary="客户端聊天输入区能力",
    description="返回客户端聊天输入区当前是否允许上传图片、开启深度思考，以及禁用时的提示文案。",
)
async def assistant_chat_capability() -> ApiResponse[ClientAssistantChatCapabilityResponse]:
    """
    读取客户端聊天输入区能力配置。

    Returns:
        ApiResponse[ClientAssistantChatCapabilityResponse]: 客户端聊天输入区能力响应。
    """

    return ApiResponse.success(
        data=ClientAssistantChatCapabilityResponse.model_validate(
            get_client_chat_capability_service(),
        )
    )


@router.post(
    "/chat/submit",
    summary="客户端助手提交对话",
    description=(
            "客户端 AI 助手聊天提交接口。"
            "提交成功后会返回 `conversation_uuid`、`message_uuid` 与 `run_status`，"
            "前端随后再通过 `/chat/stream` attach 到同一个后台 run。"
    ),
    response_description="运行态响应对象。",
)
@rate_limit(
    rules=CHAT_RATE_LIMIT_RULES,
    subjects=("user_id",),
    scope="client_assistant_chat",
    fail_open=False,
)
async def assistant_submit(
        _request: Request,
        request: ClientAssistantSubmitRequest,
) -> ApiResponse[AssistantRunSubmitResponse]:
    """
    客户端助手聊天提交入口。

    对接要点：
    1. 这是提交接口，只负责创建后台 run；
    2. 成功后前端应改为调用 `GET /chat/stream` 建立 SSE attach；
    3. `conversation_uuid` 仍然是控制当前 run 的外部主键；
    4. `message_uuid` 用于标识当前 AI 消息实体。
    """

    return ApiResponse.success(
        data=await assistant_chat(
            submit_message=request,
        )
    )


@router.get(
    "/chat/stream",
    summary="客户端助手 attach 流式输出",
    description=(
            "attach 到指定会话当前仍在运行的后台消息流。"
            "支持可选 `Last-Event-ID` 断点补发；未传时会先发送 replace 语义的快照事件。"
    ),
    response_description="SSE 流式事件；前端需按 `data: <json>\\n\\n` 解析。",
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
    attach 到客户端助手当前后台 run。

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


@router.post(
    "/chat/stop",
    summary="客户端助手停止当前输出",
    description="停止指定会话当前正在运行的后台 AI 输出。",
)
async def assistant_stop(
        request: AssistantRunStopRequest,
) -> ApiResponse[AssistantRunStopResponse]:
    """
    请求停止客户端助手当前后台 run。

    Args:
        request: 停止请求体。

    Returns:
        ApiResponse[AssistantRunStopResponse]: 停止响应。
    """

    return ApiResponse.success(
        data=assistant_chat_stop(
            conversation_uuid=request.conversation_uuid,
        )
    )


@router.post(
    "/message/tts/stream",
    summary="客户端助手消息转语音（流式）",
    description=(
            "根据客户端助手 AI 消息 `message_uuid` 生成语音，并以 HTTP chunked 方式持续返回音频字节流。"
            "该接口不是 SSE，也不是 WebSocket。"
    ),
    response_description="音频字节流；前端可直接用 `audio.src` / `Blob` / `MediaSource` 等方式播放。",
)
@rate_limit(
    rules=TTS_RATE_LIMIT_RULES,
    subjects=("user_id",),
    scope="client_assistant_tts",
    fail_open=False,
)
async def assistant_message_tts_stream(
        _request: Request,
        request: ClientAssistantMessageTtsRequest,
) -> StreamingResponse:
    """
    根据消息 UUID 生成客户端助手语音，并以 HTTP chunked 流式返回音频数据。

    Args:
        _request: FastAPI 原始请求对象（当前实现仅用于依赖注入与中间件链路）。
        request: 转语音请求体，仅包含 `message_uuid`。

    Returns:
        StreamingResponse: 音频流响应对象。
    """

    return assistant_message_tts_stream_service(
        message_uuid=request.message_uuid,
    )


@router.get(
    "/conversation/list",
    summary="客户端助手会话列表",
    description="分页查询当前登录用户的客户端助手会话列表，仅返回 `conversation_uuid` 和 `title`。",
)
async def conversation_list(
        request: ConversationListRequest = Depends(),
) -> ApiResponse[PageResponse[ConversationListItem]]:
    """分页查询客户端助手会话列表。"""

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


@router.get(
    "/history/{conversation_uuid}",
    summary="客户端助手历史消息",
    description=(
            "分页查询指定客户端助手会话的历史消息。"
            "返回结果按时间正序排列，可直接用于前端会话回放。"
    ),
)
async def conversation_messages(
        conversation_uuid: str = Path(..., min_length=1, description="会话UUID"),
        request: ConversationMessagesRequest = Depends(),
) -> ApiResponse[PageResponse[dict[str, Any]]]:
    """分页查询客户端助手历史消息。"""

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


@router.delete(
    "/conversation/{conversation_uuid}",
    summary="删除客户端助手会话",
    description="逻辑删除当前登录用户的客户端助手会话；需要携带 `Authorization: Bearer <token>`。",
)
async def delete_conversation(
        conversation_uuid: str = Path(..., min_length=1, description="会话UUID"),
) -> ApiResponse[dict[str, str]]:
    """删除客户端助手会话。"""

    delete_conversation_service(conversation_uuid=conversation_uuid)
    return ApiResponse.success(
        data={"conversation_uuid": conversation_uuid},
        message="删除成功",
    )


@router.put(
    "/conversation/{conversation_uuid}",
    summary="修改客户端助手会话标题",
    description=(
            "修改当前登录用户的客户端助手会话标题；"
            "请求体需传入 `title`，最大长度 100，接口需要 `Authorization: Bearer <token>`。"
    ),
)
async def update_conversation_title(
        request: UpdateConversationTitleRequest,
        conversation_uuid: str = Path(..., min_length=1, description="会话UUID"),
) -> ApiResponse[dict[str, str]]:
    """修改客户端助手会话标题。"""

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
