from __future__ import annotations

import json
from typing import Any

import pytest

from app.core.config_sync import AgentChatModelSlot
from app.core.config_sync import snapshot as agent_config_module

_KNOWLEDGE_ENABLED_UNSET = object()


class _FakeRedis:
    def __init__(self, return_value: Any = None, error: Exception | None = None) -> None:
        self.return_value = return_value
        self.error = error
        self.get_calls: list[str] = []

    def get(self, key: str) -> Any:
        self.get_calls.append(key)
        if self.error is not None:
            raise self.error
        return self.return_value


@pytest.fixture(autouse=True)
def _clear_agent_config_state(monkeypatch: pytest.MonkeyPatch) -> None:
    for name in [
        "OPENAI_API_KEY",
        "OPENAI_BASE_URL",
        "OPENAI_CHAT_MODEL",
        "OPENAI_EMBEDDING_MODEL",
        "DASHSCOPE_API_KEY",
        "DASHSCOPE_BASE_URL",
        "DASHSCOPE_CHAT_MODEL",
        "DASHSCOPE_EMBEDDING_MODEL",
        "VOLCENGINE_LLM_API_KEY",
        "VOLCENGINE_LLM_BASE_URL",
        "VOLCENGINE_LLM_CHAT_MODEL",
        "VOLCENGINE_LLM_EMBEDDING_MODEL",
        "AGENT_KNOWLEDGE_NAMES",
        "AGENT_KNOWLEDGE_EMBEDDING_DIM",
        "AGENT_KNOWLEDGE_EMBEDDING_MODEL",
        "AGENT_KNOWLEDGE_TOP_K",
        "AGENT_KNOWLEDGE_RANKING_ENABLED",
        "AGENT_KNOWLEDGE_RANKING_MODEL",
    ]:
        monkeypatch.setenv(name, "")
    agent_config_module.clear_agent_config_snapshot_state()
    yield
    agent_config_module.clear_agent_config_snapshot_state()


def _build_snapshot_payload(
        *,
        route_model_name: str = "gpt-4.1-mini",
        knowledge_enabled: bool | None | object = _KNOWLEDGE_ENABLED_UNSET,
) -> dict[str, Any]:
    knowledge_base: dict[str, Any] = {
        "knowledgeNames": [
            "common_medicine_kb",
            "common_medicine_kb",
            "otc_guide_kb",
            "   ",
        ],
        "embeddingDim": 1024,
        "embeddingModel": "text-embedding-v4",
        "rankingEnabled": True,
        "rankingModel": "gpt-4.1-mini",
        "topK": 8,
    }
    if knowledge_enabled is not _KNOWLEDGE_ENABLED_UNSET:
        knowledge_base["enabled"] = knowledge_enabled

    return {
        "updatedAt": "2026-03-11T14:30:00+08:00",
        "updatedBy": "admin",
        "llm": {
            "providerType": "OpenAI",
            "baseUrl": "https://api.openai.com/v1",
            "apiKey": "sk-runtime",
        },
        "agentConfigs": {
            "knowledgeBase": knowledge_base,
            "adminAssistant": {
                "adminNodeModel": {
                    "modelName": route_model_name,
                    "reasoningEnabled": False,
                },
            },
            "clientAssistant": {
                "routeModel": {
                    "modelName": "gpt-client-route",
                    "reasoningEnabled": False,
                },
                "serviceNodeModel": {
                    "modelName": "gpt-client-service",
                    "reasoningEnabled": False,
                },
                "diagnosisNodeModel": {
                    "modelName": "gpt-client-diagnosis",
                    "reasoningEnabled": False,
                },
            },
            "commonCapability": {
                "imageRecognitionModel": {
                    "modelName": "qwen-vl",
                    "reasoningEnabled": True,
                },
                "chatHistorySummaryModel": {
                    "modelName": "gpt-summary",
                    "reasoningEnabled": False,
                },
                "chatTitleModel": {
                    "modelName": "gpt-title",
                    "reasoningEnabled": False,
                },
            },
        },
        "speech": {
            "provider": "volcengine",
            "appId": "speech-app-id",
            "accessToken": "speech-access-token",
            "speechRecognition": {
                "resourceId": "volc.seedasr.sauc.duration",
            },
            "textToSpeech": {
                "resourceId": "seed-tts-2.0",
                "voiceType": "zh_female_xiaohe_uranus_bigtts",
                "maxTextChars": 300,
            },
        },
    }


def test_initialize_agent_config_snapshot_loads_valid_payload(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：启动时 Redis 有合法配置时应成功加载；预期结果：顶层 llm、agentConfigs 和 speech 均可正常解析。"""

    payload = json.dumps(_build_snapshot_payload()).encode("utf-8")
    fake_redis = _FakeRedis(return_value=payload)
    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: fake_redis)

    snapshot = agent_config_module.initialize_agent_config_snapshot()

    assert snapshot.updated_by == "admin"
    assert snapshot.llm is not None
    assert snapshot.llm.provider_type == "openai"
    assert snapshot.llm.base_url == "https://api.openai.com/v1"
    assert snapshot.llm.api_key == "sk-runtime"
    assert snapshot.agent_configs is not None
    assert snapshot.get_chat_slot(AgentChatModelSlot.ADMIN_NODE) is not None
    assert snapshot.get_chat_slot(AgentChatModelSlot.ADMIN_NODE).model_name == "gpt-4.1-mini"
    assert snapshot.get_chat_slot(AgentChatModelSlot.CLIENT_SERVICE) is not None
    assert snapshot.get_chat_slot(AgentChatModelSlot.CLIENT_SERVICE).model_name == "gpt-client-service"
    assert snapshot.get_chat_slot(AgentChatModelSlot.CLIENT_DIAGNOSIS) is not None
    assert (
            snapshot.get_chat_slot(AgentChatModelSlot.CLIENT_DIAGNOSIS).model_name
            == "gpt-client-diagnosis"
    )
    assert snapshot.get_image_slot() is not None
    assert snapshot.get_image_slot().model_name == "qwen-vl"
    assert snapshot.get_summary_slot() is not None
    assert snapshot.get_summary_slot().model_name == "gpt-summary"
    assert snapshot.get_title_slot() is not None
    assert snapshot.get_title_slot().model_name == "gpt-title"
    assert snapshot.is_knowledge_enabled() is True
    assert snapshot.get_knowledge_names() == ["common_medicine_kb", "otc_guide_kb"]
    assert snapshot.get_knowledge_embedding_model_name() == "text-embedding-v4"
    assert snapshot.get_knowledge_embedding_dim() == 1024
    assert snapshot.is_knowledge_ranking_enabled() is True
    assert snapshot.get_knowledge_ranking_model_name() == "gpt-4.1-mini"
    assert snapshot.get_knowledge_top_k() == 8
    assert snapshot.get_speech_shared_auth() == ("speech-app-id", "speech-access-token")
    assert snapshot.get_speech_stt_resource_id() == "volc.seedasr.sauc.duration"
    assert snapshot.get_speech_tts_resource_id() == "seed-tts-2.0"
    assert snapshot.get_speech_tts_voice_type() == "zh_female_xiaohe_uranus_bigtts"
    assert snapshot.get_speech_tts_max_text_chars() == 300
    assert fake_redis.get_calls == [agent_config_module.AGENT_CONFIG_REDIS_KEY]


def test_initialize_agent_config_snapshot_falls_back_to_local_when_redis_missing(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：首次启动 Redis 无数据时应回退本地快照；预期结果：不抛异常且写入本地兜底标记。"""

    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: _FakeRedis(return_value=None))

    snapshot = agent_config_module.initialize_agent_config_snapshot()

    assert snapshot.updated_by == "local_env_fallback"
    assert snapshot.get_llm_runtime_config() is None
    assert snapshot.agent_configs is not None


def test_initialize_agent_config_snapshot_uses_env_fallback_when_redis_missing(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：Redis 缺失时应支持从本地环境变量组装知识库查询配置；预期结果：顶层 llm 与 knowledgeBase getter 都能读取到 env 兜底值。"""

    monkeypatch.setenv("LLM_PROVIDER", "aliyun")
    monkeypatch.setenv("DASHSCOPE_API_KEY", "sk-local")
    monkeypatch.setenv("DASHSCOPE_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1")
    monkeypatch.setenv("DASHSCOPE_CHAT_MODEL", "qwen-flash")
    monkeypatch.setenv("DASHSCOPE_EMBEDDING_MODEL", "text-embedding-v4")
    monkeypatch.setenv("AGENT_KNOWLEDGE_NAMES", "common_medicine_kb, otc_guide_kb")
    monkeypatch.setenv("AGENT_KNOWLEDGE_EMBEDDING_DIM", "1024")
    monkeypatch.setenv("AGENT_KNOWLEDGE_TOP_K", "6")
    monkeypatch.setenv("AGENT_KNOWLEDGE_RANKING_ENABLED", "true")
    monkeypatch.setenv("AGENT_KNOWLEDGE_RANKING_MODEL", "qwen-flash")
    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: _FakeRedis(return_value=None))

    snapshot = agent_config_module.initialize_agent_config_snapshot()

    assert snapshot.updated_by == "local_env_fallback"
    assert snapshot.get_llm_runtime_config() is not None
    assert snapshot.get_llm_runtime_config().provider_type == "aliyun"
    assert snapshot.get_llm_runtime_config().base_url == "https://dashscope.aliyuncs.com/compatible-mode/v1"
    assert snapshot.get_llm_runtime_config().api_key == "sk-local"
    assert snapshot.is_knowledge_enabled() is True
    assert snapshot.get_knowledge_names() == ["common_medicine_kb", "otc_guide_kb"]
    assert snapshot.get_knowledge_embedding_model_name() == "text-embedding-v4"
    assert snapshot.get_knowledge_embedding_dim() == 1024
    assert snapshot.get_knowledge_top_k() == 6
    assert snapshot.is_knowledge_ranking_enabled() is True
    assert snapshot.get_knowledge_ranking_model_name() == "qwen-flash"


def test_initialize_agent_config_snapshot_respects_explicit_knowledge_enabled_true(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：Redis 显式开启知识库时应直接以 enabled=true 为准；预期结果：知识库启用且原始配置可读。"""

    payload = json.dumps(_build_snapshot_payload(knowledge_enabled=True)).encode("utf-8")
    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: _FakeRedis(return_value=payload))

    snapshot = agent_config_module.initialize_agent_config_snapshot()

    assert snapshot.is_knowledge_enabled() is True
    assert snapshot.get_knowledge_names() == ["common_medicine_kb", "otc_guide_kb"]
    assert snapshot.get_knowledge_embedding_model_name() == "text-embedding-v4"


def test_initialize_agent_config_snapshot_respects_explicit_knowledge_enabled_false(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：Redis 显式关闭知识库时应以 enabled=false 为准；预期结果：运行时关闭但原始配置保留。"""

    payload = json.dumps(_build_snapshot_payload(knowledge_enabled=False)).encode("utf-8")
    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: _FakeRedis(return_value=payload))

    snapshot = agent_config_module.initialize_agent_config_snapshot()

    assert snapshot.updated_by == "admin"
    assert snapshot.is_knowledge_enabled() is False
    assert snapshot.get_knowledge_names() == ["common_medicine_kb", "otc_guide_kb"]
    assert snapshot.get_knowledge_embedding_model_name() == "text-embedding-v4"
    assert snapshot.get_knowledge_top_k() == 8


def test_initialize_agent_config_snapshot_treats_empty_legacy_knowledge_config_as_disabled(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：历史配置未带 enabled 且 knowledgeBase 为空时应视为关闭；预期结果：快照正常加载但知识库未启用。"""

    payload_dict = _build_snapshot_payload()
    payload_dict["agentConfigs"]["knowledgeBase"] = {}
    payload = json.dumps(payload_dict).encode("utf-8")
    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: _FakeRedis(return_value=payload))

    snapshot = agent_config_module.initialize_agent_config_snapshot()

    assert snapshot.updated_by == "admin"
    assert snapshot.is_knowledge_enabled() is False
    assert snapshot.get_knowledge_names() == []
    assert snapshot.get_knowledge_embedding_model_name() is None


def test_initialize_agent_config_snapshot_accepts_wrapped_data_root(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：Redis 可能带有 Java 序列化包装层；预期结果：应读取 `data` 内层配置。"""

    payload_dict = {
        "@class": "java.util.LinkedHashMap",
        "data": _build_snapshot_payload(),
    }
    payload = json.dumps(payload_dict).encode("utf-8")
    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: _FakeRedis(return_value=payload))

    snapshot = agent_config_module.initialize_agent_config_snapshot()

    assert snapshot.updated_by == "admin"
    assert snapshot.llm is not None
    assert snapshot.llm.provider_type == "openai"
    assert snapshot.get_speech_shared_auth() == ("speech-app-id", "speech-access-token")


def test_initialize_agent_config_snapshot_rejects_old_v1_slot_runtime_fields(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：当前结构不兼容槽位内旧运行时字段；预期结果：初始化回退本地快照。"""

    payload_dict = _build_snapshot_payload()
    payload_dict["agentConfigs"]["adminAssistant"]["adminNodeModel"]["model"] = {
        "provider": "openai",
        "model": "legacy-model",
    }
    payload = json.dumps(payload_dict).encode("utf-8")
    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: _FakeRedis(return_value=payload))

    snapshot = agent_config_module.initialize_agent_config_snapshot()

    assert snapshot.updated_by == "local_env_fallback"
    assert snapshot.get_chat_slot(AgentChatModelSlot.ADMIN_NODE) is None


def test_initialize_agent_config_snapshot_rejects_non_string_embedding_model(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：知识库 embeddingModel 已改为纯字符串；预期结果：旧对象结构会触发本地回退。"""

    payload_dict = _build_snapshot_payload()
    payload_dict["agentConfigs"]["knowledgeBase"]["embeddingModel"] = {
        "modelName": "legacy-embedding",
    }
    payload = json.dumps(payload_dict).encode("utf-8")
    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: _FakeRedis(return_value=payload))

    snapshot = agent_config_module.initialize_agent_config_snapshot()

    assert snapshot.updated_by == "local_env_fallback"
    assert snapshot.get_knowledge_embedding_model_name() is None


def test_initialize_agent_config_snapshot_rejects_ranking_model_when_disabled(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：rankingEnabled=false 时 rankingModel 必须为 null；预期结果：非法组合会触发本地回退。"""

    payload_dict = _build_snapshot_payload()
    payload_dict["agentConfigs"]["knowledgeBase"]["rankingEnabled"] = False
    payload_dict["agentConfigs"]["knowledgeBase"]["rankingModel"] = "gpt-4.1-mini"
    payload = json.dumps(payload_dict).encode("utf-8")
    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: _FakeRedis(return_value=payload))

    snapshot = agent_config_module.initialize_agent_config_snapshot()

    assert snapshot.updated_by == "local_env_fallback"
    assert snapshot.is_knowledge_ranking_enabled() is False


def test_initialize_agent_config_snapshot_rejects_missing_ranking_model_when_enabled(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：rankingEnabled=true 时 rankingModel 必填；预期结果：非法组合会触发本地回退。"""

    payload_dict = _build_snapshot_payload()
    payload_dict["agentConfigs"]["knowledgeBase"]["rankingEnabled"] = True
    payload_dict["agentConfigs"]["knowledgeBase"]["rankingModel"] = None
    payload = json.dumps(payload_dict).encode("utf-8")
    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: _FakeRedis(return_value=payload))

    snapshot = agent_config_module.initialize_agent_config_snapshot()

    assert snapshot.updated_by == "local_env_fallback"
    assert snapshot.get_knowledge_ranking_model_name() is None


def test_initialize_agent_config_snapshot_allows_incomplete_ranking_when_knowledge_disabled(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：enabled=false 时应允许保留不完整知识库配置；预期结果：快照加载成功且知识库保持关闭。"""

    payload_dict = _build_snapshot_payload(knowledge_enabled=False)
    payload_dict["agentConfigs"]["knowledgeBase"]["rankingEnabled"] = True
    payload_dict["agentConfigs"]["knowledgeBase"]["rankingModel"] = None
    payload = json.dumps(payload_dict).encode("utf-8")
    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: _FakeRedis(return_value=payload))

    snapshot = agent_config_module.initialize_agent_config_snapshot()

    assert snapshot.updated_by == "admin"
    assert snapshot.is_knowledge_enabled() is False
    assert snapshot.is_knowledge_ranking_enabled() is True
    assert snapshot.get_knowledge_ranking_model_name() is None


def test_initialize_agent_config_snapshot_accepts_top_k_zero_as_unconfigured(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：topK 为 0 时应视为未配置；预期结果：getter 返回 None。"""

    payload_dict = _build_snapshot_payload()
    payload_dict["agentConfigs"]["knowledgeBase"]["topK"] = 0
    payload = json.dumps(payload_dict).encode("utf-8")
    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: _FakeRedis(return_value=payload))

    snapshot = agent_config_module.initialize_agent_config_snapshot()

    assert snapshot.updated_by == "admin"
    assert snapshot.get_knowledge_top_k() is None


def test_initialize_agent_config_snapshot_rejects_unsupported_provider_type(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：providerType 仅允许文档规定值；预期结果：非法 providerType 会触发本地回退。"""

    payload_dict = _build_snapshot_payload()
    payload_dict["llm"]["providerType"] = "Open AI"
    payload = json.dumps(payload_dict).encode("utf-8")
    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: _FakeRedis(return_value=payload))

    snapshot = agent_config_module.initialize_agent_config_snapshot()

    assert snapshot.updated_by == "local_env_fallback"
    assert snapshot.get_llm_runtime_config() is None


def test_refresh_agent_config_snapshot_keeps_previous_redis_snapshot_on_failure(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：已有成功 Redis 快照后刷新失败不应回退覆盖；预期结果：继续保留旧快照内容。"""

    initial_payload = json.dumps(_build_snapshot_payload(route_model_name="gpt-old")).encode("utf-8")
    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: _FakeRedis(return_value=initial_payload))
    agent_config_module.initialize_agent_config_snapshot()

    monkeypatch.setattr(
        agent_config_module,
        "get_redis_connection",
        lambda: _FakeRedis(error=RuntimeError("redis down")),
    )

    refresh_result = agent_config_module.refresh_agent_config_snapshot(
        redis_key=agent_config_module.AGENT_CONFIG_REDIS_KEY,
    )

    current_snapshot = agent_config_module.get_current_agent_config_snapshot()
    assert refresh_result.applied is False
    assert refresh_result.speech_changed is False
    assert refresh_result.current_snapshot is not None
    assert current_snapshot.get_chat_slot(AgentChatModelSlot.ADMIN_NODE) is not None
    assert current_snapshot.get_chat_slot(AgentChatModelSlot.ADMIN_NODE).model_name == "gpt-old"


def test_refresh_agent_config_snapshot_always_reload_when_notification_arrives(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：收到通知后不再进行版本门控；预期结果：即使内容相同也会重新读取 Redis。"""

    initial_payload = json.dumps(_build_snapshot_payload(route_model_name="gpt-route-same")).encode("utf-8")
    initial_redis = _FakeRedis(return_value=initial_payload)
    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: initial_redis)
    agent_config_module.initialize_agent_config_snapshot()

    reloaded_payload = json.dumps(_build_snapshot_payload(route_model_name="gpt-route-same")).encode("utf-8")
    reloaded_redis = _FakeRedis(return_value=reloaded_payload)
    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: reloaded_redis)

    refresh_result = agent_config_module.refresh_agent_config_snapshot(
        redis_key=agent_config_module.AGENT_CONFIG_REDIS_KEY,
    )

    assert refresh_result.applied is True
    assert refresh_result.speech_changed is False
    assert reloaded_redis.get_calls == [agent_config_module.AGENT_CONFIG_REDIS_KEY]


def test_refresh_agent_config_snapshot_applies_redis_payload(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：收到刷新通知后应替换本地快照；预期结果：当前快照更新为 Redis 最新内容。"""

    initial_payload = json.dumps(_build_snapshot_payload(route_model_name="gpt-old")).encode("utf-8")
    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: _FakeRedis(return_value=initial_payload))
    agent_config_module.initialize_agent_config_snapshot()

    refreshed_payload = json.dumps(_build_snapshot_payload(route_model_name="gpt-new")).encode("utf-8")
    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: _FakeRedis(return_value=refreshed_payload))

    refresh_result = agent_config_module.refresh_agent_config_snapshot(
        redis_key=agent_config_module.AGENT_CONFIG_REDIS_KEY,
    )

    current_snapshot = agent_config_module.get_current_agent_config_snapshot()
    assert refresh_result.applied is True
    assert refresh_result.speech_changed is False
    assert current_snapshot.get_chat_slot(AgentChatModelSlot.ADMIN_NODE) is not None
    assert current_snapshot.get_chat_slot(AgentChatModelSlot.ADMIN_NODE).model_name == "gpt-new"


def test_initialize_agent_config_snapshot_accepts_partial_speech_auth_without_failing(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：Redis 里语音鉴权只给半套时不应拖垮整份快照；预期结果：快照可成功加载，但共享鉴权 getter 返回空。"""

    payload_dict = _build_snapshot_payload()
    payload_dict["speech"]["accessToken"] = None
    payload = json.dumps(payload_dict).encode("utf-8")
    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: _FakeRedis(return_value=payload))

    snapshot = agent_config_module.initialize_agent_config_snapshot()

    assert snapshot.speech is not None
    assert snapshot.speech.app_id == "speech-app-id"
    assert snapshot.get_speech_shared_auth() is None


def test_refresh_agent_config_snapshot_applies_latest_speech_values(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：收到刷新通知后语音配置也应同步更新；预期结果：当前快照可读到新的音色与最大字符数。"""

    initial_payload = json.dumps(_build_snapshot_payload()).encode("utf-8")
    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: _FakeRedis(return_value=initial_payload))
    agent_config_module.initialize_agent_config_snapshot()

    refreshed_payload_dict = _build_snapshot_payload()
    refreshed_payload_dict["speech"]["textToSpeech"]["resourceId"] = "seed-tts-3.0"
    refreshed_payload_dict["speech"]["textToSpeech"]["voiceType"] = "S_demo_voice"
    refreshed_payload_dict["speech"]["textToSpeech"]["maxTextChars"] = 512
    refreshed_payload = json.dumps(refreshed_payload_dict).encode("utf-8")
    monkeypatch.setattr(agent_config_module, "get_redis_connection", lambda: _FakeRedis(return_value=refreshed_payload))

    refresh_result = agent_config_module.refresh_agent_config_snapshot(
        redis_key=agent_config_module.AGENT_CONFIG_REDIS_KEY,
    )

    current_snapshot = agent_config_module.get_current_agent_config_snapshot()
    assert refresh_result.applied is True
    assert refresh_result.speech_changed is True
    assert refresh_result.previous_snapshot is not None
    assert refresh_result.current_snapshot is not None
    assert current_snapshot.get_speech_tts_resource_id() == "seed-tts-3.0"
    assert current_snapshot.get_speech_tts_voice_type() == "S_demo_voice"
    assert current_snapshot.get_speech_tts_max_text_chars() == 512
