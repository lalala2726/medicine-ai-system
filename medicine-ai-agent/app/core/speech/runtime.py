from __future__ import annotations

import asyncio
import threading
from typing import Protocol

from loguru import logger

SPEECH_CONFIG_REFRESH_CLOSE_CODE = 1012
SPEECH_CONFIG_REFRESH_CLOSE_REASON = "语音配置已刷新，需要重新建立会话"


class SpeechRefreshParticipant(Protocol):
    """支持被语音配置刷新中断的活动会话。"""

    async def interrupt_due_to_config_refresh(self) -> None:
        """在语音配置刷新时中断当前活动会话。"""


class SpeechRuntimeCoordinator:
    """进程内语音运行时协调器。"""

    def __init__(self) -> None:
        self._registry_lock = threading.Lock()
        self._active_stt_sessions: set[SpeechRefreshParticipant] = set()
        self._active_tts_streams: set[SpeechRefreshParticipant] = set()
        self._refresh_lock: asyncio.Lock | None = None
        self._refresh_lock_loop: asyncio.AbstractEventLoop | None = None

    def register_active_stt_session(self, session: SpeechRefreshParticipant) -> None:
        with self._registry_lock:
            self._active_stt_sessions.add(session)

    def unregister_active_stt_session(self, session: SpeechRefreshParticipant) -> None:
        with self._registry_lock:
            self._active_stt_sessions.discard(session)

    def register_active_tts_stream(self, stream: SpeechRefreshParticipant) -> None:
        with self._registry_lock:
            self._active_tts_streams.add(stream)

    def unregister_active_tts_stream(self, stream: SpeechRefreshParticipant) -> None:
        with self._registry_lock:
            self._active_tts_streams.discard(stream)

    async def handle_speech_config_refresh(self) -> None:
        """串行处理中断旧语音会话并基于新配置重新探活。"""

        async with self._get_refresh_lock():
            stt_sessions, tts_streams = self._snapshot_active_participants()
            logger.info(
                "检测到语音配置变更，开始中断活动语音会话 stt_count={} tts_count={}",
                len(stt_sessions),
                len(tts_streams),
            )
            await self._interrupt_active_participants(stt_sessions, tts_streams)
            await self._reprobe_speech_connections()

    def clear(self) -> None:
        with self._registry_lock:
            self._active_stt_sessions.clear()
            self._active_tts_streams.clear()

    def get_active_counts(self) -> tuple[int, int]:
        with self._registry_lock:
            return len(self._active_stt_sessions), len(self._active_tts_streams)

    def _snapshot_active_participants(
            self,
    ) -> tuple[list[SpeechRefreshParticipant], list[SpeechRefreshParticipant]]:
        with self._registry_lock:
            return list(self._active_stt_sessions), list(self._active_tts_streams)

    def _get_refresh_lock(self) -> asyncio.Lock:
        loop = asyncio.get_running_loop()
        with self._registry_lock:
            if self._refresh_lock is None or self._refresh_lock_loop is not loop:
                self._refresh_lock = asyncio.Lock()
                self._refresh_lock_loop = loop
            return self._refresh_lock

    async def _interrupt_active_participants(
            self,
            stt_sessions: list[SpeechRefreshParticipant],
            tts_streams: list[SpeechRefreshParticipant],
    ) -> None:
        participants = [*stt_sessions, *tts_streams]
        if not participants:
            logger.info("当前没有活动语音会话需要中断。")
            return

        results = await asyncio.gather(
            *(participant.interrupt_due_to_config_refresh() for participant in participants),
            return_exceptions=True,
        )
        for result in results:
            if isinstance(result, Exception):
                logger.opt(exception=result).warning("中断活动语音会话时发生异常")

    async def _reprobe_speech_connections(self) -> None:
        from app.core.speech.stt.client import verify_volcengine_stt_connection_on_startup
        from app.core.speech.tts.client import verify_volcengine_tts_connection_on_startup

        logger.info("开始基于最新语音配置重新探活 STT/TTS 连接。")
        results = await asyncio.gather(
            verify_volcengine_stt_connection_on_startup(),
            verify_volcengine_tts_connection_on_startup(),
            return_exceptions=True,
        )
        for channel, result in zip(("stt", "tts"), results, strict=False):
            if isinstance(result, Exception):
                logger.opt(exception=result).warning(
                    "语音配置刷新后重探活失败 channel={channel}",
                    channel=channel,
                )
        logger.info("语音配置刷新后的重探活流程结束。")


_speech_runtime_coordinator = SpeechRuntimeCoordinator()


def register_active_stt_session(session: SpeechRefreshParticipant) -> None:
    _speech_runtime_coordinator.register_active_stt_session(session)


def unregister_active_stt_session(session: SpeechRefreshParticipant) -> None:
    _speech_runtime_coordinator.unregister_active_stt_session(session)


def register_active_tts_stream(stream: SpeechRefreshParticipant) -> None:
    _speech_runtime_coordinator.register_active_tts_stream(stream)


def unregister_active_tts_stream(stream: SpeechRefreshParticipant) -> None:
    _speech_runtime_coordinator.unregister_active_tts_stream(stream)


async def handle_speech_config_refresh() -> None:
    await _speech_runtime_coordinator.handle_speech_config_refresh()


def clear_speech_runtime_state() -> None:
    _speech_runtime_coordinator.clear()


def get_active_speech_runtime_counts() -> tuple[int, int]:
    return _speech_runtime_coordinator.get_active_counts()


__all__ = [
    "SPEECH_CONFIG_REFRESH_CLOSE_CODE",
    "SPEECH_CONFIG_REFRESH_CLOSE_REASON",
    "SpeechRuntimeCoordinator",
    "clear_speech_runtime_state",
    "get_active_speech_runtime_counts",
    "handle_speech_config_refresh",
    "register_active_stt_session",
    "register_active_tts_stream",
    "unregister_active_stt_session",
    "unregister_active_tts_stream",
]
