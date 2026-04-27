from __future__ import annotations

import asyncio

import app.core.speech.stt.client as stt_client_module
import app.core.speech.tts.client as tts_client_module
from app.core.speech.runtime import SpeechRuntimeCoordinator


class _FakeParticipant:
    def __init__(self) -> None:
        self.interrupted = False

    async def interrupt_due_to_config_refresh(self) -> None:
        self.interrupted = True


def test_speech_runtime_coordinator_interrupts_active_participants_and_reprobes(monkeypatch) -> None:
    """测试目的：语音配置刷新应先中断活动会话，再基于最新配置重探活；预期结果：中断和 STT/TTS 探活都会执行。"""

    coordinator = SpeechRuntimeCoordinator()
    active_stt = _FakeParticipant()
    active_tts = _FakeParticipant()
    coordinator.register_active_stt_session(active_stt)
    coordinator.register_active_tts_stream(active_tts)
    probe_calls: list[str] = []

    async def _fake_stt_probe() -> None:
        probe_calls.append("stt")

    async def _fake_tts_probe() -> None:
        probe_calls.append("tts")

    monkeypatch.setattr(stt_client_module, "verify_volcengine_stt_connection_on_startup", _fake_stt_probe)
    monkeypatch.setattr(tts_client_module, "verify_volcengine_tts_connection_on_startup", _fake_tts_probe)

    asyncio.run(coordinator.handle_speech_config_refresh())

    assert active_stt.interrupted is True
    assert active_tts.interrupted is True
    assert set(probe_calls) == {"stt", "tts"}
