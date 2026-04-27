import json
from typing import Any

import pytest

from app.core.codes import ResponseCode
from app.core.config_sync import snapshot as agent_config_module
from app.core.exception.exceptions import ServiceException
from app.core.speech import env_utils as speech_env_utils
from app.core.speech.stt import config as stt_config_module


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


def _set_required_env(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("VOLCENGINE_APP_ID", "test-app-id")
    monkeypatch.setenv("VOLCENGINE_ACCESS_TOKEN", "test-access-token")
    monkeypatch.setenv("VOLCENGINE_STT_RESOURCE_ID", "volc.seedasr.sauc.duration")


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


def test_resolve_volcengine_stt_config_uses_defaults(monkeypatch: pytest.MonkeyPatch) -> None:
    _disable_dotenv_lookup(monkeypatch)
    _set_required_env(monkeypatch)
    monkeypatch.delenv("VOLCENGINE_STT_ENDPOINT", raising=False)
    monkeypatch.delenv("VOLCENGINE_STT_MAX_DURATION_SECONDS", raising=False)

    config = stt_config_module.resolve_volcengine_stt_config()

    assert config.endpoint == stt_config_module.DEFAULT_VOLCENGINE_STT_ENDPOINT
    assert config.max_duration_seconds == stt_config_module.DEFAULT_VOLCENGINE_STT_MAX_DURATION_SECONDS
    assert config.app_id == "test-app-id"
    assert config.access_token == "test-access-token"
    assert config.resource_id == "volc.seedasr.sauc.duration"


def test_resolve_volcengine_stt_config_raises_when_required_env_missing(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    _disable_dotenv_lookup(monkeypatch)
    monkeypatch.delenv("VOLCENGINE_APP_ID", raising=False)
    monkeypatch.delenv("VOLCENGINE_ACCESS_TOKEN", raising=False)
    monkeypatch.delenv("VOLCENGINE_STT_RESOURCE_ID", raising=False)

    with pytest.raises(ServiceException) as exc_info:
        stt_config_module.resolve_volcengine_stt_config()

    assert exc_info.value.code == ResponseCode.INTERNAL_ERROR.code
    assert "VOLCENGINE_APP_ID" in exc_info.value.message


def test_resolve_volcengine_stt_config_raises_when_max_duration_invalid(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    _disable_dotenv_lookup(monkeypatch)
    _set_required_env(monkeypatch)
    monkeypatch.setenv("VOLCENGINE_STT_MAX_DURATION_SECONDS", "abc")

    with pytest.raises(ServiceException) as exc_info:
        stt_config_module.resolve_volcengine_stt_config()

    assert exc_info.value.code == ResponseCode.INTERNAL_ERROR.code
    assert "VOLCENGINE_STT_MAX_DURATION_SECONDS" in exc_info.value.message


def test_resolve_volcengine_stt_config_caps_max_duration_to_internal_upper_bound(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    _disable_dotenv_lookup(monkeypatch)
    _set_required_env(monkeypatch)
    monkeypatch.setenv("VOLCENGINE_STT_MAX_DURATION_SECONDS", "1800")

    config = stt_config_module.resolve_volcengine_stt_config()

    assert config.max_duration_seconds == stt_config_module.MAX_VOLCENGINE_STT_MAX_DURATION_SECONDS


def test_resolve_volcengine_stt_config_supports_custom_max_duration_from_env(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    _disable_dotenv_lookup(monkeypatch)
    _set_required_env(monkeypatch)
    monkeypatch.setenv("VOLCENGINE_STT_MAX_DURATION_SECONDS", "300")

    config = stt_config_module.resolve_volcengine_stt_config()

    assert config.max_duration_seconds == 300


def test_build_volcengine_stt_headers_contains_required_fields(monkeypatch: pytest.MonkeyPatch) -> None:
    _disable_dotenv_lookup(monkeypatch)
    _set_required_env(monkeypatch)
    config = stt_config_module.resolve_volcengine_stt_config()

    headers = stt_config_module.build_volcengine_stt_headers(config, connect_id="connect-1")

    assert headers == {
        "X-Api-App-Key": "test-app-id",
        "X-Api-Access-Key": "test-access-token",
        "X-Api-Resource-Id": "volc.seedasr.sauc.duration",
        "X-Api-Connect-Id": "connect-1",
    }


def test_resolve_volcengine_stt_config_prefers_redis_speech_config(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    _disable_dotenv_lookup(monkeypatch)
    _set_required_env(monkeypatch)
    payload = _build_v4_payload(
        speech={
            "provider": "volcengine",
            "appId": "redis-app-id",
            "accessToken": "redis-access-token",
            "speechRecognition": {
                "resourceId": "redis-resource-id",
            },
        }
    )
    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: _FakeRedis(return_value=payload))
    agent_config_module.initialize_agent_config_snapshot()

    config = stt_config_module.resolve_volcengine_stt_config()

    assert config.app_id == "redis-app-id"
    assert config.access_token == "redis-access-token"
    assert config.resource_id == "redis-resource-id"


def test_resolve_volcengine_stt_config_falls_back_to_env_when_redis_auth_is_partial(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    _disable_dotenv_lookup(monkeypatch)
    _set_required_env(monkeypatch)
    payload = _build_v4_payload(
        speech={
            "provider": "volcengine",
            "appId": "redis-only-app-id",
            "speechRecognition": {
                "resourceId": "redis-resource-id",
            },
        }
    )
    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: _FakeRedis(return_value=payload))
    agent_config_module.initialize_agent_config_snapshot()

    config = stt_config_module.resolve_volcengine_stt_config()

    assert config.app_id == "test-app-id"
    assert config.access_token == "test-access-token"
    assert config.resource_id == "redis-resource-id"
