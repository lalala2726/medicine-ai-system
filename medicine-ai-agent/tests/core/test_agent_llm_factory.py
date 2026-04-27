from __future__ import annotations

from typing import Any

import pytest

from app.core.config_sync import AgentChatModelSlot, AgentConfigSnapshot
from app.core.config_sync import llm as llm_factory


def _build_snapshot() -> AgentConfigSnapshot:
    return AgentConfigSnapshot.model_validate(
        {
            "updatedAt": "2026-03-11T14:30:00+08:00",
            "updatedBy": "admin",
            "llm": {
                "providerType": "OpenAI",
                "baseUrl": "https://api.openai.com/v1",
                "apiKey": "sk-runtime",
            },
            "agentConfigs": {
                "knowledgeBase": {
                    "knowledgeNames": ["common_medicine_kb", "otc_guide_kb"],
                    "embeddingDim": 2048,
                    "embeddingModel": "redis-embedding-model",
                    "rankingEnabled": False,
                    "rankingModel": None,
                    "topK": 10,
                },
                "adminAssistant": {
                    "adminNodeModel": {
                        "modelName": "gpt-admin-node-redis",
                        "reasoningEnabled": True,
                    },
                },
                "clientAssistant": {
                    "routeModel": {
                        "modelName": "gpt-client-route-redis",
                        "reasoningEnabled": False,
                    },
                    "serviceNodeModel": {
                        "modelName": "gpt-client-service-redis",
                        "reasoningEnabled": False,
                    },
                    "diagnosisNodeModel": {
                        "modelName": "gpt-client-diagnosis-redis",
                        "reasoningEnabled": False,
                    },
                },
                "commonCapability": {
                    "imageRecognitionModel": {
                        "modelName": "qwen-vl-redis",
                        "reasoningEnabled": True,
                    },
                    "chatHistorySummaryModel": {
                        "modelName": "gpt-summary-redis",
                        "reasoningEnabled": False,
                    },
                    "chatTitleModel": {
                        "modelName": "gpt-title-redis",
                        "reasoningEnabled": False,
                    },
                },
            },
        },
    )


def test_create_agent_chat_llm_prefers_redis_slot_over_local_defaults(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：聊天包装工厂应优先使用 Redis 槽位参数；预期结果：模型名来自槽位，provider/base_url/api_key 来自顶层 llm。"""

    captured: dict[str, Any] = {}
    monkeypatch.setattr(llm_factory, "get_current_agent_config_snapshot", _build_snapshot)
    monkeypatch.setattr(llm_factory, "create_chat_model", lambda **kwargs: captured.update(kwargs) or "llm")

    result = llm_factory.create_agent_chat_llm(
        slot=AgentChatModelSlot.ADMIN_NODE,
        temperature=1.0,
        think=False,
        max_tokens=512,
    )

    assert result == "llm"
    assert captured["model"] == "gpt-admin-node-redis"
    assert captured["provider"] == "openai"
    assert captured["base_url"] == "https://api.openai.com/v1"
    assert captured["api_key"] == "sk-runtime"
    assert captured["temperature"] == 1.0
    assert captured["max_tokens"] == 512
    assert captured["think"] is True


def test_create_agent_chat_llm_uses_gateway_env_fallback_when_slot_missing(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：路由槽位缺失时应回退 gateway 专用环境模型名；预期结果：透传 env 解析出的 model。"""

    captured: dict[str, Any] = {}
    empty_snapshot = AgentConfigSnapshot()
    monkeypatch.setattr(llm_factory, "get_current_agent_config_snapshot", lambda: empty_snapshot)
    monkeypatch.setattr(llm_factory, "_resolve_gateway_router_fallback_model_name", lambda: "gateway-env-model")
    monkeypatch.setattr(llm_factory, "create_chat_model", lambda **kwargs: captured.update(kwargs) or "llm")

    result = llm_factory.create_agent_chat_llm(
        slot=AgentChatModelSlot.CLIENT_ROUTE,
        temperature=0.0,
        think=False,
    )

    assert result == "llm"
    assert captured["model"] == "gateway-env-model"
    assert captured["temperature"] == 0.0
    assert captured["think"] is False


def test_create_agent_chat_llm_treats_zero_max_tokens_as_unlimited(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：本地传入 max_tokens=0 时应视为不限制；预期结果：不会向底层透传 max_tokens。"""

    captured: dict[str, Any] = {}
    empty_snapshot = AgentConfigSnapshot()
    monkeypatch.setattr(llm_factory, "get_current_agent_config_snapshot", lambda: empty_snapshot)
    monkeypatch.setattr(llm_factory, "_resolve_gateway_router_fallback_model_name", lambda: "gateway-env-model")
    monkeypatch.setattr(llm_factory, "create_chat_model", lambda **kwargs: captured.update(kwargs) or "llm")

    result = llm_factory.create_agent_chat_llm(
        slot=AgentChatModelSlot.CLIENT_ROUTE,
        temperature=0.0,
        think=False,
        max_tokens=0,
    )

    assert result == "llm"
    assert "max_tokens" not in captured


def test_create_agent_image_llm_prefers_redis_slot_over_local_defaults(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：图片识别包装工厂应优先使用 Redis 槽位参数；预期结果：模型名来自槽位，provider 来自顶层 llm。"""

    captured: dict[str, Any] = {}
    monkeypatch.setattr(llm_factory, "get_current_agent_config_snapshot", _build_snapshot)
    monkeypatch.setattr(llm_factory, "create_image_model", lambda **kwargs: captured.update(kwargs) or "image-llm")

    result = llm_factory.create_agent_image_llm(
        temperature=1.0,
        think=False,
        max_tokens=256,
    )

    assert result == "image-llm"
    assert captured["model"] == "qwen-vl-redis"
    assert captured["provider"] == "openai"
    assert captured["base_url"] == "https://api.openai.com/v1"
    assert captured["api_key"] == "sk-runtime"
    assert captured["temperature"] == 1.0
    assert captured["max_tokens"] == 256
    assert captured["think"] is True


def test_resolve_agent_image_runtime_prefers_redis_slot_and_runtime_config(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：图片运行时解析应优先读取 Redis 槽位和顶层 llm；预期结果：返回 Redis 模型名与顶层连接信息。"""

    monkeypatch.setattr(llm_factory, "get_current_agent_config_snapshot", _build_snapshot)

    runtime = llm_factory.resolve_agent_image_runtime()

    assert runtime.provider.value == "openai"
    assert runtime.model == "qwen-vl-redis"
    assert runtime.api_key == "sk-runtime"
    assert runtime.base_url == "https://api.openai.com/v1"


def test_resolve_agent_image_runtime_uses_provider_specific_env_fallback_when_slot_missing(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：图片槽位缺失时应按 provider 回退环境模型名；预期结果：返回对应 provider 的 env 模型。"""

    snapshot = AgentConfigSnapshot.model_validate(
        {
            "updatedAt": "2026-03-11T14:30:00+08:00",
            "updatedBy": "admin",
            "llm": {
                "providerType": "aliyun",
                "baseUrl": "https://dashscope.aliyuncs.com/compatible-mode/v1",
                "apiKey": "sk-dashscope",
            },
        },
    )
    monkeypatch.setattr(llm_factory, "get_current_agent_config_snapshot", lambda: snapshot)
    monkeypatch.setattr(llm_factory, "_resolve_image_fallback_model_name", lambda _provider: "qwen-image-env")

    runtime = llm_factory.resolve_agent_image_runtime()

    assert runtime.provider.value == "aliyun"
    assert runtime.model == "qwen-image-env"
    assert runtime.api_key == "sk-dashscope"
    assert runtime.base_url == "https://dashscope.aliyuncs.com/compatible-mode/v1"


def test_resolve_agent_image_runtime_raises_provider_specific_model_error_when_missing(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：图片模型名缺失时应提示当前 provider 对应的环境变量；预期结果：抛出 OPENAI_IMAGE_MODEL 缺失错误。"""

    snapshot = AgentConfigSnapshot.model_validate(
        {
            "updatedAt": "2026-03-11T14:30:00+08:00",
            "updatedBy": "admin",
            "llm": {
                "providerType": "openai",
                "baseUrl": "https://api.openai.com/v1",
                "apiKey": "sk-runtime",
            },
        },
    )
    monkeypatch.setattr(llm_factory, "get_current_agent_config_snapshot", lambda: snapshot)
    monkeypatch.setattr(llm_factory, "_resolve_image_fallback_model_name", lambda _provider: None)

    with pytest.raises(RuntimeError, match="OPENAI_IMAGE_MODEL is not set"):
        llm_factory.resolve_agent_image_runtime()


def test_resolve_agent_image_runtime_raises_provider_specific_api_key_error_when_missing(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：图片 API Key 缺失时应提示当前 provider 对应的环境变量；预期结果：抛出 VOLCENGINE_LLM_API_KEY 缺失错误。"""

    snapshot = AgentConfigSnapshot.model_validate(
        {
            "updatedAt": "2026-03-11T14:30:00+08:00",
            "updatedBy": "admin",
            "llm": {
                "providerType": "volcengine",
                "baseUrl": "https://ark.cn-beijing.volces.com/api/v3",
                "apiKey": "placeholder-runtime-key",
            },
        },
    )
    monkeypatch.setattr(llm_factory, "get_current_agent_config_snapshot", lambda: snapshot)
    monkeypatch.setattr(llm_factory, "_resolve_image_fallback_model_name", lambda _provider: "doubao-image")
    monkeypatch.setattr(
        llm_factory,
        "_resolve_runtime_connection",
        lambda _provider, _runtime_config: (None, "https://ark.cn-beijing.volces.com/api/v3"),
    )

    with pytest.raises(RuntimeError, match="VOLCENGINE_LLM_API_KEY is not set"):
        llm_factory.resolve_agent_image_runtime()


def test_create_agent_summary_llm_prefers_redis_slot_over_local_defaults(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：聊天历史总结包装工厂应优先使用 Redis 槽位参数；预期结果：模型名来自槽位，provider/base_url/api_key 来自顶层 llm。"""

    captured: dict[str, Any] = {}
    monkeypatch.setattr(llm_factory, "get_current_agent_config_snapshot", _build_snapshot)
    monkeypatch.setattr(llm_factory, "create_chat_model", lambda **kwargs: captured.update(kwargs) or "summary-llm")

    result = llm_factory.create_agent_summary_llm(
        temperature=0.9,
        think=True,
        max_tokens=256,
    )

    assert result == "summary-llm"
    assert captured["model"] == "gpt-summary-redis"
    assert captured["provider"] == "openai"
    assert captured["base_url"] == "https://api.openai.com/v1"
    assert captured["api_key"] == "sk-runtime"
    assert captured["temperature"] == 0.9
    assert captured["max_tokens"] == 256
    assert captured["think"] is False


def test_create_agent_title_llm_prefers_redis_slot_over_local_defaults(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：聊天标题包装工厂应优先使用 Redis 槽位参数；预期结果：模型名来自槽位，provider/base_url/api_key 来自顶层 llm。"""

    captured: dict[str, Any] = {}
    monkeypatch.setattr(llm_factory, "get_current_agent_config_snapshot", _build_snapshot)
    monkeypatch.setattr(llm_factory, "create_chat_model", lambda **kwargs: captured.update(kwargs) or "title-llm")

    result = llm_factory.create_agent_title_llm(
        temperature=1.0,
        think=True,
        max_tokens=64,
    )

    assert result == "title-llm"
    assert captured["model"] == "gpt-title-redis"
    assert captured["provider"] == "openai"
    assert captured["base_url"] == "https://api.openai.com/v1"
    assert captured["api_key"] == "sk-runtime"
    assert captured["temperature"] == 1.0
    assert captured["max_tokens"] == 64
    assert captured["think"] is False


def test_resolve_agent_summary_helpers_fall_back_to_env_when_slot_missing(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：聊天历史总结模型名和预算解析在 Redis 缺失时应回退本地环境配置；预期结果：分别返回本地 summary env 模型名和 token 上限。"""

    empty_snapshot = AgentConfigSnapshot()
    monkeypatch.setattr(llm_factory, "get_current_agent_config_snapshot", lambda: empty_snapshot)
    monkeypatch.setattr(llm_factory, "_resolve_summary_fallback_model_name", lambda: "summary-env-model")
    monkeypatch.setattr(llm_factory, "_resolve_summary_fallback_max_tokens", lambda: 2048)

    assert llm_factory.resolve_agent_summary_model_name() == "summary-env-model"
    assert llm_factory.resolve_agent_summary_max_tokens() == 2048


def test_create_agent_summary_llm_treats_zero_max_tokens_as_unlimited(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：聊天历史总结本地传入 max_tokens=0 时应视为不限制；预期结果：不向底层透传 max_tokens。"""

    snapshot = AgentConfigSnapshot.model_validate(
        {
            "updatedAt": "2026-03-11T14:30:00+08:00",
            "updatedBy": "admin",
            "llm": {
                "providerType": "openai",
                "baseUrl": "https://api.openai.com/v1",
                "apiKey": "sk-runtime",
            },
            "agentConfigs": {
                "commonCapability": {
                    "chatHistorySummaryModel": {
                        "modelName": "gpt-summary-redis",
                        "reasoningEnabled": False,
                    },
                },
            },
        },
    )
    captured: dict[str, Any] = {}
    monkeypatch.setattr(llm_factory, "get_current_agent_config_snapshot", lambda: snapshot)
    monkeypatch.setattr(llm_factory, "create_chat_model", lambda **kwargs: captured.update(kwargs) or "summary-llm")

    result = llm_factory.create_agent_summary_llm(max_tokens=0)

    assert result == "summary-llm"
    assert captured["model"] == "gpt-summary-redis"
    assert "max_tokens" not in captured


def test_create_agent_chat_llm_reads_client_slot_from_client_assistant_tree(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：client 槽位应从 clientAssistant 子树读取；预期结果：命中独立 clientAssistant.serviceNodeModel 配置。"""

    captured: dict[str, Any] = {}
    monkeypatch.setattr(llm_factory, "get_current_agent_config_snapshot", _build_snapshot)
    monkeypatch.setattr(llm_factory, "create_chat_model", lambda **kwargs: captured.update(kwargs) or "llm")

    result = llm_factory.create_agent_chat_llm(
        slot=AgentChatModelSlot.CLIENT_SERVICE,
        temperature=1.0,
        think=True,
        max_tokens=512,
    )

    assert result == "llm"
    assert captured["model"] == "gpt-client-service-redis"
    assert captured["temperature"] == 1.0
    assert captured["max_tokens"] == 512
    assert captured["think"] is False


def test_create_agent_embedding_client_prefers_explicit_model_name(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：向量包装工厂应优先使用显式传入模型名；预期结果：model 使用显式值，provider/base_url/api_key 仍来自顶层 llm。"""

    captured: dict[str, Any] = {}
    monkeypatch.setattr(llm_factory, "get_current_agent_config_snapshot", _build_snapshot)
    monkeypatch.setattr(
        llm_factory,
        "create_embedding_model",
        lambda **kwargs: captured.update(kwargs) or "embedding-client",
    )

    result = llm_factory.create_agent_embedding_client(
        model="remote-embedding-model",
        dimensions=1536,
    )

    assert result == "embedding-client"
    assert captured["model"] == "remote-embedding-model"
    assert captured["provider"] == "openai"
    assert captured["base_url"] == "https://api.openai.com/v1"
    assert captured["api_key"] == "sk-runtime"
    assert captured["dimensions"] == 1536


def test_create_agent_embedding_client_uses_redis_model_and_dim_when_not_explicit(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：向量包装工厂在未显式传参时应读取 Redis 知识库配置；预期结果：model 与 dimensions 均来自 knowledgeBase。"""

    captured: dict[str, Any] = {}
    monkeypatch.setattr(llm_factory, "get_current_agent_config_snapshot", _build_snapshot)
    monkeypatch.setattr(
        llm_factory,
        "create_embedding_model",
        lambda **kwargs: captured.update(kwargs) or "embedding-client",
    )

    result = llm_factory.create_agent_embedding_client()

    assert result == "embedding-client"
    assert captured["model"] == "redis-embedding-model"
    assert captured["provider"] == "openai"
    assert captured["base_url"] == "https://api.openai.com/v1"
    assert captured["api_key"] == "sk-runtime"
    assert captured["dimensions"] == 2048
