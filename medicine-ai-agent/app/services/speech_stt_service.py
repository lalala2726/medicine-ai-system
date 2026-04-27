from __future__ import annotations

from fastapi import WebSocket
from loguru import logger

from app.core.exception.exceptions import ServiceException
from app.core.speech.stt.config import resolve_volcengine_stt_config
from app.core.speech.stt.session import AdminAssistantSttSession, SttSessionError
from app.schemas.auth import AuthUser


async def speech_stt_stream_service(
        *,
        websocket: WebSocket,
        user: AuthUser,
        session_duration_seconds: int | None = None,
) -> None:
    """
    管理助手实时语音识别 websocket 服务入口。

    行为：
    1. 接收前端 start/binary/finish；
    2. 转发到火山实时 STT 并回传 transcript；
    3. 到达最大时长或完成后由后端主动关闭连接。

    Args:
        websocket: 前端 websocket 连接对象。
        user: 当前认证用户。
        session_duration_seconds: 业务代码指定的本次会话时长（秒）；
            为空时使用默认 60 秒，且不超过配置上限。
    """

    try:
        config = resolve_volcengine_stt_config()
        session = AdminAssistantSttSession(
            websocket=websocket,
            user=user,
            config=config,
            session_duration_seconds=session_duration_seconds,
        )
        await session.run()
    except SttSessionError as exc:
        logger.warning("stt session rejected: {}", str(exc))
        try:
            await websocket.send_json(
                {
                    "type": "error",
                    "message": str(exc),
                }
            )
        except Exception:
            pass
        try:
            await websocket.close(code=1008, reason=str(exc))
        except Exception:
            pass
    except ServiceException as exc:
        logger.warning("stt service unavailable: {}", exc.message)
        try:
            await websocket.close(code=1011, reason="stt_unavailable")
        except Exception:
            pass
    except Exception as exc:
        logger.opt(exception=exc).warning("stt session failed")
        try:
            await websocket.send_json(
                {
                    "type": "error",
                    "message": "语音识别服务异常",
                }
            )
        except Exception:
            pass
        try:
            await websocket.close(code=1011, reason="stt_error")
        except Exception:
            pass


__all__ = ["speech_stt_stream_service"]
