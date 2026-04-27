import json
from typing import Any

import pytest

from app.core.codes import ResponseCode
from app.core.config_sync import snapshot as agent_config_module
from app.core.exception.exceptions import ServiceException
from app.core.speech import env_utils as speech_env_utils
from app.core.speech.tts import config as tts_config_module


class _FakeRedis:
    def __init__(self, return_value=None) -> None:
        self.return_value = return_value

    def get(self, _key: str):
        return self.return_value


@pytest.fixture(autouse=True)
def _clear_agent_config_state(monkeypatch: pytest.MonkeyPatch) -> None:
    agent_config_module.clear_agent_config_snapshot_state()
    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: _FakeRedis(return_value=None))
    yield
    agent_config_module.clear_agent_config_snapshot_state()


def _set_shared_env(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("VOLCENGINE_APP_ID", "test-app-id")
    monkeypatch.setenv("VOLCENGINE_ACCESS_TOKEN", "test-access-token")


def _disable_dotenv_lookup(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setattr(speech_env_utils, "_read_dotenv_value", lambda _name: "")


def _build_v4_payload(*, speech: dict[str, Any]) -> bytes:
    return json.dumps(
        {
            "updatedAt": "2026-03-13T10:30:00+08:00",
            "updatedBy": "admin",
            "llm": {
                "providerType": "openai",
                "baseUrl": "https://api.openai.com/v1",
                "apiKey": "sk-runtime",
            },
            "agentConfigs": {},
            "speech": speech,
        }
    ).encode("utf-8")


def test_resolve_volcengine_tts_config_uses_shared_auth_and_defaults(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    _disable_dotenv_lookup(monkeypatch)
    _set_shared_env(monkeypatch)
    monkeypatch.setenv("VOLCENGINE_TTS_RESOURCE_ID", "seed-tts-2.0")
    monkeypatch.setenv("VOLCENGINE_TTS_VOICE_TYPE", "zh_female_xiaohe_uranus_bigtts")
    monkeypatch.delenv("VOLCENGINE_TTS_ENDPOINT", raising=False)
    monkeypatch.delenv("VOLCENGINE_TTS_ENCODING", raising=False)
    monkeypatch.delenv("VOLCENGINE_TTS_SAMPLE_RATE", raising=False)
    monkeypatch.delenv("VOLCENGINE_TTS_MAX_TEXT_CHARS", raising=False)

    config = tts_config_module.resolve_volcengine_tts_config()

    assert config.app_id == "test-app-id"
    assert config.access_token == "test-access-token"
    assert config.endpoint == tts_config_module.DEFAULT_VOLCENGINE_TTS_ENDPOINT
    assert config.encoding == tts_config_module.DEFAULT_VOLCENGINE_TTS_ENCODING
    assert config.sample_rate == tts_config_module.DEFAULT_VOLCENGINE_TTS_SAMPLE_RATE
    assert config.max_text_chars == tts_config_module.DEFAULT_VOLCENGINE_TTS_MAX_TEXT_CHARS
    assert config.resource_id == "seed-tts-2.0"


def test_resolve_volcengine_tts_config_raises_when_shared_env_missing(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    _disable_dotenv_lookup(monkeypatch)
    monkeypatch.delenv("VOLCENGINE_APP_ID", raising=False)
    monkeypatch.delenv("VOLCENGINE_ACCESS_TOKEN", raising=False)
    monkeypatch.setenv("VOLCENGINE_TTS_RESOURCE_ID", "seed-tts-2.0")
    monkeypatch.setenv("VOLCENGINE_TTS_VOICE_TYPE", "zh_female_xiaohe_uranus_bigtts")

    with pytest.raises(ServiceException) as exc_info:
        tts_config_module.resolve_volcengine_tts_config()

    assert exc_info.value.code == ResponseCode.INTERNAL_ERROR.code
    assert "VOLCENGINE_APP_ID" in exc_info.value.message


def test_resolve_volcengine_tts_config_keeps_tts_specific_settings(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    _disable_dotenv_lookup(monkeypatch)
    _set_shared_env(monkeypatch)
    monkeypatch.setenv("VOLCENGINE_TTS_VOICE_TYPE", "S_demo_voice")
    monkeypatch.setenv("VOLCENGINE_TTS_RESOURCE_ID", "seed-tts-2.0")
    monkeypatch.setenv("VOLCENGINE_TTS_ENDPOINT", "wss://tts.example/ws")
    monkeypatch.setenv("VOLCENGINE_TTS_ENCODING", "wav")
    monkeypatch.setenv("VOLCENGINE_TTS_SAMPLE_RATE", "16000")
    monkeypatch.setenv("VOLCENGINE_TTS_MAX_TEXT_CHARS", "500")

    config = tts_config_module.resolve_volcengine_tts_config()

    assert config.endpoint == "wss://tts.example/ws"
    assert config.resource_id == "seed-tts-2.0"
    assert config.encoding == "wav"
    assert config.sample_rate == 16000
    assert config.max_text_chars == 500
    assert config.voice_type == "S_demo_voice"


def test_resolve_volcengine_tts_config_prefers_redis_voice_and_max_text_chars(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    _disable_dotenv_lookup(monkeypatch)
    _set_shared_env(monkeypatch)
    monkeypatch.setenv("VOLCENGINE_TTS_VOICE_TYPE", "env_voice")
    monkeypatch.setenv("VOLCENGINE_TTS_RESOURCE_ID", "env-resource")
    monkeypatch.setenv("VOLCENGINE_TTS_MAX_TEXT_CHARS", "500")
    payload = _build_v4_payload(
        speech={
            "provider": "volcengine",
            "appId": "redis-app-id",
            "accessToken": "redis-access-token",
            "textToSpeech": {
                "resourceId": "redis-resource-id",
                "voiceType": "S_demo_voice",
                "maxTextChars": 256,
            },
        }
    )
    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: _FakeRedis(return_value=payload))
    agent_config_module.initialize_agent_config_snapshot()

    config = tts_config_module.resolve_volcengine_tts_config()

    assert config.app_id == "redis-app-id"
    assert config.access_token == "redis-access-token"
    assert config.voice_type == "S_demo_voice"
    assert config.max_text_chars == 256
    assert config.resource_id == "redis-resource-id"


def test_resolve_volcengine_tts_config_falls_back_to_env_auth_when_redis_auth_is_partial(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    _disable_dotenv_lookup(monkeypatch)
    _set_shared_env(monkeypatch)
    monkeypatch.setenv("VOLCENGINE_TTS_RESOURCE_ID", "env-resource-id")
    monkeypatch.setenv("VOLCENGINE_TTS_VOICE_TYPE", "env_voice")
    payload = _build_v4_payload(
        speech={
            "provider": "volcengine",
            "appId": "redis-only-app-id",
            "textToSpeech": {
                "resourceId": "redis-resource-id",
                "voiceType": "redis_voice",
                "maxTextChars": 128,
            },
        }
    )
    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: _FakeRedis(return_value=payload))
    agent_config_module.initialize_agent_config_snapshot()

    config = tts_config_module.resolve_volcengine_tts_config()

    assert config.app_id == "test-app-id"
    assert config.access_token == "test-access-token"
    assert config.resource_id == "redis-resource-id"
    assert config.voice_type == "redis_voice"
    assert config.max_text_chars == 128


def test_resolve_volcengine_tts_config_falls_back_to_env_tts_config_when_redis_resource_id_missing(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    _disable_dotenv_lookup(monkeypatch)
    _set_shared_env(monkeypatch)
    monkeypatch.setenv("VOLCENGINE_TTS_RESOURCE_ID", "env-resource-id")
    monkeypatch.setenv("VOLCENGINE_TTS_VOICE_TYPE", "env_voice")
    monkeypatch.setenv("VOLCENGINE_TTS_MAX_TEXT_CHARS", "500")
    payload = _build_v4_payload(
        speech={
            "provider": "volcengine",
            "appId": "redis-app-id",
            "accessToken": "redis-access-token",
            "textToSpeech": {
                "voiceType": "redis_voice",
                "maxTextChars": 128,
            },
        }
    )
    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: _FakeRedis(return_value=payload))
    agent_config_module.initialize_agent_config_snapshot()

    config = tts_config_module.resolve_volcengine_tts_config()

    assert config.app_id == "redis-app-id"
    assert config.access_token == "redis-access-token"
    assert config.resource_id == "env-resource-id"
    assert config.voice_type == "env_voice"
    assert config.max_text_chars == 500


def test_resolve_volcengine_tts_config_raises_when_resource_id_missing_in_redis_and_env(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    _disable_dotenv_lookup(monkeypatch)
    _set_shared_env(monkeypatch)
    monkeypatch.delenv("VOLCENGINE_TTS_RESOURCE_ID", raising=False)
    monkeypatch.setenv("VOLCENGINE_TTS_VOICE_TYPE", "env_voice")
    payload = _build_v4_payload(
        speech={
            "provider": "volcengine",
            "appId": "redis-app-id",
            "accessToken": "redis-access-token",
            "textToSpeech": {
                "voiceType": "redis_voice",
                "maxTextChars": 128,
            },
        }
    )
    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: _FakeRedis(return_value=payload))
    agent_config_module.initialize_agent_config_snapshot()

    with pytest.raises(ServiceException) as exc_info:
        tts_config_module.resolve_volcengine_tts_config()

    assert exc_info.value.code == ResponseCode.INTERNAL_ERROR.code
    assert "VOLCENGINE_TTS_RESOURCE_ID" in exc_info.value.message
