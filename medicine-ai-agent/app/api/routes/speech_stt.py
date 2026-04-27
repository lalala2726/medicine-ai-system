from __future__ import annotations

from typing import Literal

from fastapi import APIRouter, WebSocket

from app.core.codes import ResponseCode
from app.core.security.auth_context import (
    reset_authorization_header,
    reset_current_user,
    set_authorization_header,
    set_current_user,
)
from app.core.security.rate_limit import (
    RateLimitException,
    RateLimitPreset,
    RateLimitRule,
    check_rate_limit,
)
from app.services.auth_service import verify_authorization
from app.services.speech_stt_service import speech_stt_stream_service

router = APIRouter(prefix="/ws/speech/stt", tags=["语音识别"])

# 语音识别 WebSocket 的固定限流规则集合。
STT_RATE_LIMIT_RULES = (
    RateLimitRule.preset(RateLimitPreset.MINUTE_1, limit=5),
    RateLimitRule.preset(RateLimitPreset.HOUR_1, limit=60),
    RateLimitRule.preset(RateLimitPreset.HOUR_5, limit=100),
    RateLimitRule.preset(RateLimitPreset.HOUR_24, limit=200),
)
# 语音识别限流主体字段，当前按登录用户维度限制。
STT_RATE_LIMIT_SUBJECTS: tuple[Literal["user_id"], ...] = ("user_id",)
# 语音识别限流作用域名称。
STT_RATE_LIMIT_SCOPE = "speech_stt_stream"


def _resolve_query_authorization(websocket: WebSocket) -> str | None:
    """
    从 WebSocket query 参数解析 Bearer Authorization。

    Args:
        websocket: 当前握手中的 WebSocket 连接对象。

    Returns:
        str | None: 标准 `Bearer <token>` Authorization；未提供 token 时返回 None。
    """

    raw_token = (
            websocket.query_params.get("access_token")
            or websocket.query_params.get("token")
            or ""
    ).strip()
    if not raw_token:
        return None
    if raw_token.lower().startswith("bearer "):
        return raw_token
    return f"Bearer {raw_token}"


@router.websocket("/stream")
async def speech_stt_stream(websocket: WebSocket) -> None:
    """
    语音识别 STT WebSocket 接口。

    认证规则：
    - 从 query 参数读取 token（`access_token` 或 `token`）；
    - 鉴权成功后转发到 STT 服务。
    """

    auth_token = set_authorization_header(_resolve_query_authorization(websocket))
    user_token = set_current_user(None)
    try:
        try:
            current_user = await verify_authorization()
        except Exception:
            await websocket.close(code=1008, reason="unauthorized")
            return

        # STT 是客户端与管理端共用入口，只校验登录态；资源控制交给用户维度限流。
        reset_current_user(user_token)
        user_token = set_current_user(current_user)

        try:
            check_rate_limit(
                scope=STT_RATE_LIMIT_SCOPE,
                rules=STT_RATE_LIMIT_RULES,
                subjects=STT_RATE_LIMIT_SUBJECTS,
                fail_open=False,
                request=websocket,
            )
        except RateLimitException as exc:
            close_code = (
                1013
                if exc.code == ResponseCode.TOO_MANY_REQUESTS.code
                else 1011
            )
            await websocket.close(code=close_code, reason=exc.message)
            return

        await speech_stt_stream_service(
            websocket=websocket,
            user=current_user,
            session_duration_seconds=60
        )
    finally:
        reset_current_user(user_token)
        reset_authorization_header(auth_token)


__all__ = ["router"]
