from __future__ import annotations

import asyncio
import json
import time
from dataclasses import dataclass
from typing import Any

from fastapi import WebSocket
from loguru import logger

from app.core.speech.runtime import (
    SPEECH_CONFIG_REFRESH_CLOSE_CODE,
    SPEECH_CONFIG_REFRESH_CLOSE_REASON,
    register_active_stt_session,
    unregister_active_stt_session,
)
from app.core.speech.stt.client import SttStartRequest, VolcengineSttClient
from app.core.speech.stt.config import VolcengineSttConfig
from app.core.speech.volcengine_speech_protocol import MsgType, SttServerMessage
from app.schemas.auth import AuthUser

# 前端单个音频二进制包大小上限（bytes）。
MAX_FRONTEND_AUDIO_CHUNK_SIZE = 512 * 1024
# 收到 finish 后等待上游最终结果的最大时长（秒）。
FINISH_WAIT_MAX_SECONDS = 8
# 前端未显式指定时的会话默认识别时长（秒）。
DEFAULT_STT_SESSION_DURATION_SECONDS = 60


class SttSessionError(RuntimeError):
    """前后端 STT 会话控制异常。"""


@dataclass(frozen=True)
class SttSessionCloseResult:
    """
    STT 会话结束状态。

    Attributes:
        reason: 会话结束原因（如 `client_finish` / `timeout` / `upstream_end`）。
    """

    reason: str


class AdminAssistantSttSession:
    """管理助手 STT WebSocket 桥接会话。"""

    def __init__(
            self,
            *,
            websocket: WebSocket,
            user: AuthUser,
            config: VolcengineSttConfig,
            session_duration_seconds: int | None = None,
            stt_client_factory: Any = VolcengineSttClient,
    ) -> None:
        """
        初始化前后端 STT 桥接会话。

        Args:
            websocket: 下游前端 WebSocket 连接。
            user: 当前认证用户信息。
            config: STT 配置对象。
            session_duration_seconds: 本次会话识别时长（秒），由业务代码传入；
                为空时默认 60 秒，且不会超过 `config.max_duration_seconds`。
            stt_client_factory: 上游 STT 客户端工厂，默认 `VolcengineSttClient`。
        """

        self._websocket = websocket
        self._user = user
        self._config = config
        self._stt_client = stt_client_factory(config=config)

        self._started = False
        self._finish_requested = False
        self._session_duration_seconds = self._resolve_session_duration_seconds(
            session_duration_seconds
        )
        self._deadline_at: float | None = None
        self._upstream_task: asyncio.Task[None] | None = None
        self._upstream_ended = asyncio.Event()
        self._upstream_error: str | None = None
        self._close_sent = False
        self._config_refresh_requested = asyncio.Event()
        self._registered_in_runtime = False

    async def run(self) -> SttSessionCloseResult | None:
        """
        驱动 STT 会话主循环。

        Returns:
            SttSessionCloseResult: 会话结束原因。
        """

        await self._websocket.accept()
        register_active_stt_session(self)
        self._registered_in_runtime = True
        # 接入后立即启动会话超时计时，避免客户端长期空闲不发 start 占用连接资源。
        self._deadline_at = time.monotonic() + float(self._session_duration_seconds)
        try:
            while True:
                wait_timeout = self._remaining_timeout_seconds()
                ready = await self._wait_for_frontend_or_upstream(wait_timeout)
                if ready["type"] == "timeout":
                    await self._handle_timeout()
                    return SttSessionCloseResult(reason="timeout")
                if ready["type"] == "config_refresh":
                    return SttSessionCloseResult(reason="config_refresh")
                if ready["type"] == "upstream":
                    await self._handle_upstream_completion()
                    return SttSessionCloseResult(
                        reason="client_finish" if self._finish_requested else "upstream_end"
                    )
                message = ready["message"]
                message_type = message.get("type")
                if message_type == "websocket.disconnect":
                    return SttSessionCloseResult(reason="client_disconnect")
                if message_type != "websocket.receive":
                    continue
                if message.get("text") is not None:
                    should_close = await self._handle_frontend_text(message["text"])
                    if should_close:
                        return SttSessionCloseResult(reason="client_finish")
                    continue
                if message.get("bytes") is not None:
                    await self._handle_frontend_audio(message["bytes"])
                    continue
        except Exception:
            if self._config_refresh_requested.is_set():
                return SttSessionCloseResult(reason="config_refresh")
            raise
        finally:
            if self._registered_in_runtime:
                unregister_active_stt_session(self)
                self._registered_in_runtime = False
            await self._cleanup()

    async def interrupt_due_to_config_refresh(self) -> None:
        """在语音配置刷新时主动中断当前 STT 会话。"""

        if self._config_refresh_requested.is_set():
            return
        self._config_refresh_requested.set()
        await self._stt_client.close()
        await self._close_websocket(
            code=SPEECH_CONFIG_REFRESH_CLOSE_CODE,
            reason=SPEECH_CONFIG_REFRESH_CLOSE_REASON,
        )

    async def _handle_frontend_text(self, text_payload: str) -> bool:
        """
        处理前端文本控制指令（start/finish）。

        Args:
            text_payload: 文本帧内容（JSON）。

        Returns:
            bool: `True` 表示会话应立即结束。
        """

        payload = self._parse_json_payload(text_payload)
        message_type = payload.get("type")

        if message_type == "start":
            if self._started:
                raise SttSessionError("重复发送 start")
            await self._start_stt_stream(payload)
            return False

        if message_type == "finish":
            if not self._started:
                raise SttSessionError("请先发送 start")
            if self._finish_requested:
                raise SttSessionError("重复发送 finish")
            self._finish_requested = True
            await self._stt_client.send_audio_chunk(b"", is_last=True)
            await self._wait_upstream_until_done()
            await self._send_json(
                {
                    "type": "completed",
                    "reason": "client_finish",
                }
            )
            await self._close_websocket(code=1000)
            return True

        raise SttSessionError(f"不支持的消息类型: {message_type!r}")

    async def _handle_frontend_audio(self, audio_payload: bytes) -> None:
        """
        处理前端音频二进制分包。

        Args:
            audio_payload: 音频字节。

        Returns:
            None
        """

        if not self._started:
            raise SttSessionError("请先发送 start")
        if self._finish_requested:
            raise SttSessionError("finish 后不允许继续发送音频")
        if len(audio_payload) > MAX_FRONTEND_AUDIO_CHUNK_SIZE:
            raise SttSessionError("音频分片过大")
        await self._stt_client.send_audio_chunk(audio_payload, is_last=False)

    async def _start_stt_stream(self, payload: dict[str, Any]) -> None:
        """
        启动上游 STT 识别流并回发 started 事件。

        Args:
            payload: 前端 `start` 控制包 JSON。

        Returns:
            None
        """

        if "max_duration_seconds" in payload:
            raise SttSessionError("max_duration_seconds 不允许由前端设置")

        request = self._build_start_request(payload.get("request"))
        await self._stt_client.connect()
        await self._stt_client.send_full_client_request(
            request=request,
            user_id=self._user.id,
        )
        self._started = True
        self._deadline_at = time.monotonic() + float(self._session_duration_seconds)
        self._upstream_task = asyncio.create_task(self._forward_upstream_messages())
        await self._send_json(
            {
                "type": "started",
                "max_duration_seconds": self._session_duration_seconds,
                "max_allowed_duration_seconds": self._config.max_duration_seconds,
                "provider_log_id": self._stt_client.provider_log_id,
            }
        )

    async def _forward_upstream_messages(self) -> None:
        """
        后台任务：持续消费上游消息并转发给前端。

        Returns:
            None
        """

        try:
            while True:
                message = await self._stt_client.receive_server_message()
                await self._forward_single_upstream_message(message)
                if message.message_type == MsgType.Error:
                    break
                if message.is_last_package:
                    break
        except Exception as exc:
            self._upstream_error = str(exc)
            if self._config_refresh_requested.is_set():
                logger.info("stt upstream interrupted by speech config refresh")
            else:
                logger.opt(exception=exc).warning("stt upstream receive failed")
        finally:
            self._upstream_ended.set()

    async def _forward_single_upstream_message(self, message: SttServerMessage) -> None:
        """
        转发单条上游消息到前端协议。

        Args:
            message: 上游解析后的 STT 消息。

        Returns:
            None
        """

        if message.message_type == MsgType.Error:
            payload = message.payload_json if isinstance(message.payload_json, dict) else {}
            message_text = str(payload.get("message") or payload.get("error") or "语音识别服务异常")
            self._upstream_error = message_text
            await self._send_json(
                {
                    "type": "error",
                    "message": message_text,
                }
            )
            return

        if message.message_type != MsgType.FullServerResponse:
            return

        payload = message.payload_json if isinstance(message.payload_json, dict) else {}
        result = payload.get("result")
        if not isinstance(result, dict):
            result = {}
        await self._send_json(
            {
                "type": "transcript",
                "is_final": bool(message.is_last_package),
                "sequence": int(message.sequence or 0),
                "result": result,
            }
        )

    async def _handle_upstream_completion(self) -> None:
        """
        处理上游识别结束事件并完成前端收口。

        Returns:
            None
        """

        if self._upstream_task is not None:
            task = self._upstream_task
            self._upstream_task = None
            try:
                await task
            except Exception as exc:
                self._upstream_error = str(exc)

        if self._upstream_error:
            await self._send_json({"type": "error", "message": self._upstream_error})
            await self._close_websocket(code=1011)
            return

        await self._send_json(
            {
                "type": "completed",
                "reason": "client_finish" if self._finish_requested else "upstream_end",
            }
        )
        await self._close_websocket(code=1000)

    async def _wait_upstream_until_done(self) -> None:
        """
        在 finish 后等待上游最终包。

        Returns:
            None

        Raises:
            SttSessionError: 等待超时或上游返回错误。
        """

        timeout = self._remaining_timeout_seconds()
        if timeout is None:
            timeout = FINISH_WAIT_MAX_SECONDS
        timeout = min(max(timeout, 0.1), FINISH_WAIT_MAX_SECONDS)
        try:
            await asyncio.wait_for(self._upstream_ended.wait(), timeout=timeout)
        except asyncio.TimeoutError as exc:
            raise SttSessionError("等待识别结果超时") from exc
        upstream_task = self._upstream_task
        self._upstream_task = None
        if upstream_task is not None:
            try:
                await upstream_task
            except Exception as exc:
                self._upstream_error = str(exc)
        if self._upstream_error:
            raise SttSessionError(self._upstream_error)

    async def _handle_timeout(self) -> None:
        """
        处理会话超时并主动关闭连接。

        Returns:
            None
        """

        if self._started and not self._finish_requested:
            try:
                await self._stt_client.send_audio_chunk(b"", is_last=True)
            except Exception:
                pass
        await self._send_json(
            {
                "type": "timeout",
                "error_code": "stt_timeout",
                "message": f"识别已超过 {self._session_duration_seconds} 秒，连接已关闭",
            }
        )
        await self._close_websocket(code=1000)

    async def _wait_for_frontend_or_upstream(self, timeout: float | None) -> dict[str, Any]:
        """
        等待前端消息或上游任务完成（谁先到先处理）。

        Args:
            timeout: 本轮等待超时时间（秒）；`None` 表示不设上限。

        Returns:
            dict[str, Any]: 描述就绪事件的结构化结果。
        """

        frontend_task = asyncio.create_task(self._websocket.receive())
        config_refresh_task = asyncio.create_task(self._config_refresh_requested.wait())
        wait_tasks: set[asyncio.Task[Any]] = {frontend_task, config_refresh_task}
        upstream_task = self._upstream_task
        if upstream_task is not None:
            wait_tasks.add(upstream_task)

        done, _ = await asyncio.wait(
            wait_tasks,
            timeout=timeout,
            return_when=asyncio.FIRST_COMPLETED,
        )
        if not done:
            await self._cancel_task(frontend_task)
            await self._cancel_task(config_refresh_task)
            return {"type": "timeout"}

        if config_refresh_task in done:
            await self._cancel_task(frontend_task)
            return {"type": "config_refresh"}

        if upstream_task is not None and upstream_task in done:
            await self._cancel_task(frontend_task)
            await self._cancel_task(config_refresh_task)
            return {"type": "upstream"}

        message = await frontend_task
        await self._cancel_task(config_refresh_task)
        return {"type": "frontend", "message": message}

    async def _cleanup(self) -> None:
        """
        会话结束后清理任务与连接资源。

        Returns:
            None
        """

        upstream_task = self._upstream_task
        self._upstream_task = None
        if upstream_task is not None and not upstream_task.done():
            upstream_task.cancel()
            try:
                await upstream_task
            except asyncio.CancelledError:
                pass
            except Exception:
                pass
        await self._stt_client.close()

    def _remaining_timeout_seconds(self) -> float | None:
        """
        计算当前距离会话超时还剩余的秒数。

        Returns:
            float | None: 剩余秒数；若尚未初始化截止时间则返回 `None`。
        """

        if self._deadline_at is None:
            return None
        return max(self._deadline_at - time.monotonic(), 0.0)

    async def _send_json(self, payload: dict[str, Any]) -> None:
        """
        向前端发送 JSON 事件。

        Args:
            payload: 事件载荷。

        Returns:
            None
        """

        if self._config_refresh_requested.is_set():
            return
        await self._websocket.send_json(payload)

    async def _close_websocket(self, *, code: int, reason: str | None = None) -> None:
        """
        幂等关闭前端 WebSocket。

        Args:
            code: 关闭状态码。
            reason: 关闭原因。

        Returns:
            None
        """

        if self._close_sent:
            return
        self._close_sent = True
        try:
            await self._websocket.close(code=code, reason=reason)
        except Exception:
            pass

    @staticmethod
    async def _cancel_task(task: asyncio.Task[Any]) -> None:
        """取消并回收后台任务。"""

        if task.done():
            try:
                await task
            except asyncio.CancelledError:
                pass
            except Exception:
                pass
            return
        task.cancel()
        try:
            await task
        except asyncio.CancelledError:
            pass
        except Exception:
            pass

    @staticmethod
    def _parse_json_payload(text_payload: str) -> dict[str, Any]:
        """
        解析并校验文本控制消息 JSON。

        Args:
            text_payload: 前端文本帧内容。

        Returns:
            dict[str, Any]: 解析后的 JSON 对象。

        Raises:
            SttSessionError: 非法 JSON 或非 object 结构。
        """

        try:
            parsed = json.loads(text_payload)
        except json.JSONDecodeError as exc:
            raise SttSessionError("控制消息必须为 JSON") from exc
        if not isinstance(parsed, dict):
            raise SttSessionError("控制消息必须为 JSON object")
        return parsed

    def _resolve_session_duration_seconds(self, value: int | None) -> int:
        """
        解析本次会话识别时长（秒），仅接受业务代码传入值。

        规则：
        1. 未传则使用默认值 60 秒；
        2. 任何传入值都必须为正整数；
        3. 传入值不能超过服务端配置上限 `config.max_duration_seconds`。
        """

        if value is None:
            return min(
                DEFAULT_STT_SESSION_DURATION_SECONDS,
                self._config.max_duration_seconds,
            )
        if isinstance(value, bool):
            raise SttSessionError("session_duration_seconds 必须是正整数（秒）")
        try:
            resolved = int(value)
        except (TypeError, ValueError) as exc:
            raise SttSessionError("session_duration_seconds 必须是正整数（秒）") from exc
        if resolved <= 0:
            raise SttSessionError("session_duration_seconds 必须大于 0")
        if resolved > self._config.max_duration_seconds:
            raise SttSessionError(
                f"session_duration_seconds 不能超过 {self._config.max_duration_seconds} 秒"
            )
        return resolved

    @staticmethod
    def _build_start_request(request_payload: Any) -> SttStartRequest:
        """
        把前端 start.request 载荷转换为 `SttStartRequest`。

        Args:
            request_payload: 前端请求参数对象。

        Returns:
            SttStartRequest: 规范化后的上游请求参数。

        Raises:
            SttSessionError: 字段类型或取值不符合要求。
        """

        if request_payload is None:
            request_payload = {}
        if not isinstance(request_payload, dict):
            raise SttSessionError("start.request 必须是 object")

        enable_itn = request_payload.get("enable_itn", True)
        enable_punc = request_payload.get("enable_punc", True)
        show_utterances = request_payload.get("show_utterances", True)
        result_type = request_payload.get("result_type", "single")

        if not isinstance(enable_itn, bool):
            raise SttSessionError("enable_itn 必须是布尔值")
        if not isinstance(enable_punc, bool):
            raise SttSessionError("enable_punc 必须是布尔值")
        if not isinstance(show_utterances, bool):
            raise SttSessionError("show_utterances 必须是布尔值")
        if result_type not in {"single", "full"}:
            raise SttSessionError("result_type 仅支持 single/full")

        return SttStartRequest(
            enable_itn=enable_itn,
            enable_punc=enable_punc,
            show_utterances=show_utterances,
            result_type=result_type,
        )


__all__ = [
    "AdminAssistantSttSession",
    "DEFAULT_STT_SESSION_DURATION_SECONDS",
    "MAX_FRONTEND_AUDIO_CHUNK_SIZE",
    "SttSessionCloseResult",
    "SttSessionError",
]
