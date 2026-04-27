from __future__ import annotations

from types import SimpleNamespace
from typing import Any

import pytest
from langchain_core.messages import AIMessageChunk
from langchain_core.outputs import ChatGenerationChunk

import app.core.llms.chat_factory as chat_factory
import app.core.llms.common as llm_common_module
import app.core.llms.embedding_factory as embedding_factory
import app.core.llms.provider as provider_module
import app.core.llms.providers.aliyun as aliyun_provider
import app.core.llms.providers.openai as openai_provider
import app.core.llms.providers.volcengine as volcengine_provider
from app.core.llms.provider import LlmProvider


class _FakeChatClient:
    """
    功能描述:
        模拟聊天模型构造对象，用于捕获工厂函数透传参数并断言。

    参数说明:
        **kwargs (Any): 构造时接收的关键字参数。

    返回值:
        None: 仅缓存入参，不执行业务逻辑。

    异常说明:
        无。
    """

    def __init__(self, **kwargs: Any) -> None:
        """
        功能描述:
            初始化模拟对象并保存构造参数。

        参数说明:
            **kwargs (Any): 模型构造参数。

        返回值:
            None

        异常说明:
            无。
        """

        self.kwargs = kwargs


@pytest.fixture(autouse=True)
def _isolate_llm_env_loading(monkeypatch: pytest.MonkeyPatch) -> None:
    """
    功能描述:
        隔离 LLM 配置测试环境，禁止真实 `load_dotenv` 读取本地文件，
        并在每个用例开始前重置加载状态，避免开发机环境干扰断言。

    参数说明:
        monkeypatch (pytest.MonkeyPatch): pytest monkeypatch 工具实例。

    返回值:
        None

    异常说明:
        无。
    """

    monkeypatch.setattr(llm_common_module, "_LLM_ENV_LOADED", False)
    monkeypatch.setattr(llm_common_module, "load_dotenv", lambda **_kwargs: False)


def test_create_chat_model_routes_to_openai_provider(monkeypatch: pytest.MonkeyPatch) -> None:
    """测试目的：验证 create_chat_model 在 OPENAI provider 下路由到 OpenAI 工厂；预期结果：返回 OpenAI 工厂产物且参数原样透传。"""

    captured: dict[str, Any] = {}

    def _fake_openai_chat_model(**kwargs: Any) -> str:
        captured.update(kwargs)
        return "openai-model"

    monkeypatch.setattr(chat_factory, "create_openai_chat_model", _fake_openai_chat_model)

    result = chat_factory.create_chat_model(
        model="gpt-test",
        provider=LlmProvider.OPENAI,
        temperature=0.2,
        think=True,
    )

    assert result == "openai-model"
    assert captured["model"] == "gpt-test"
    assert captured["extra_body"] is None
    assert captured["temperature"] == 0.2


def test_create_chat_model_uses_env_provider_when_provider_is_none(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：验证未显式传 provider 时可从环境配置读取默认 provider；预期结果：`LLM_PROVIDER=aliyun` 时路由到阿里云工厂。"""

    monkeypatch.setattr(provider_module, "resolve_llm_value", lambda **_kwargs: "aliyun")
    monkeypatch.setattr(chat_factory, "create_aliyun_chat_model", lambda **_kwargs: "aliyun-model")

    result = chat_factory.create_chat_model(model="qwen-test")

    assert result == "aliyun-model"


def test_create_chat_model_defaults_to_openai_when_provider_missing(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：验证未显式传 provider 且环境未配置时默认走 openai；预期结果：路由到 OpenAI 工厂。"""

    monkeypatch.setattr(provider_module, "resolve_llm_value", lambda **_kwargs: None)
    monkeypatch.setattr(chat_factory, "create_openai_chat_model", lambda **_kwargs: "openai-model")

    result = chat_factory.create_chat_model(model="gpt-test")

    assert result == "openai-model"


def test_create_chat_model_explicit_provider_overrides_env_provider(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：验证显式 provider 优先级高于环境配置；预期结果：显式 `openai` 覆盖环境 `aliyun`。"""

    monkeypatch.setattr(provider_module, "resolve_llm_value", lambda **_kwargs: "aliyun")
    monkeypatch.setattr(chat_factory, "create_openai_chat_model", lambda **_kwargs: "openai-model")

    result = chat_factory.create_chat_model(provider=LlmProvider.OPENAI, model="gpt-test")

    assert result == "openai-model"


def test_create_chat_model_accepts_enum_style_provider_from_env(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：验证 `LLM_PROVIDER=LlmProvider.ALIYUN` 可被正确解析；预期结果：未显式传 provider 时路由到阿里云工厂。"""

    monkeypatch.setattr(provider_module, "resolve_llm_value", lambda **_kwargs: "LlmProvider.ALIYUN")
    monkeypatch.setattr(chat_factory, "create_aliyun_chat_model", lambda **_kwargs: "aliyun-model")

    result = chat_factory.create_chat_model(model="qwen-test")

    assert result == "aliyun-model"


def test_create_chat_model_routes_to_aliyun_provider_with_think_enabled(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：验证 create_chat_model 在 ALIYUN provider 下开启 think 时注入 enable_thinking；预期结果：extra_body.enable_thinking=True 且保留其他字段。"""

    captured: dict[str, Any] = {}

    def _fake_aliyun_chat_model(**kwargs: Any) -> str:
        captured.update(kwargs)
        return "aliyun-model"

    monkeypatch.setattr(chat_factory, "create_aliyun_chat_model", _fake_aliyun_chat_model)

    result = chat_factory.create_chat_model(
        model="qwen-test",
        provider=LlmProvider.ALIYUN,
        think=True,
    )

    assert result == "aliyun-model"
    assert captured["model"] == "qwen-test"
    assert captured["extra_body"] == {"enable_thinking": True}


def test_create_chat_model_routes_to_aliyun_provider_with_think_disabled(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：验证 create_chat_model 在 ALIYUN provider 下关闭 think 时显式注入 enable_thinking=False；预期结果：extra_body.enable_thinking=False。"""

    captured: dict[str, Any] = {}

    def _fake_aliyun_chat_model(**kwargs: Any) -> str:
        captured.update(kwargs)
        return "aliyun-model"

    monkeypatch.setattr(chat_factory, "create_aliyun_chat_model", _fake_aliyun_chat_model)

    result = chat_factory.create_chat_model(
        model="qwen-test",
        provider=LlmProvider.ALIYUN,
        extra_body={
            "enable_thinking": True,
        },
        think=False,
    )

    assert result == "aliyun-model"
    assert captured["extra_body"] == {"enable_thinking": False}


def test_create_chat_model_routes_to_volcengine_provider_with_think_enabled(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：验证 create_chat_model 在 VOLCENGINE provider 下开启 think 时注入 thinking.enabled；预期结果：extra_body.thinking.type=enabled。"""

    captured: dict[str, Any] = {}

    def _fake_volcengine_chat_model(**kwargs: Any) -> str:
        captured.update(kwargs)
        return "volcengine-model"

    monkeypatch.setattr(chat_factory, "create_volcengine_chat_model", _fake_volcengine_chat_model)

    result = chat_factory.create_chat_model(
        model="doubao-test",
        provider=LlmProvider.VOLCENGINE,
        think=True,
        temperature=0.1,
    )

    assert result == "volcengine-model"
    assert captured["model"] == "doubao-test"
    assert captured["temperature"] == 0.1
    assert captured["extra_body"] == {"thinking": {"type": "enabled"}}


def test_create_chat_model_routes_to_volcengine_provider_with_think_disabled(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：验证 create_chat_model 在 VOLCENGINE provider 下关闭 think 时显式注入 thinking.disabled；预期结果：extra_body.thinking.type=disabled。"""

    captured: dict[str, Any] = {}

    def _fake_volcengine_chat_model(**kwargs: Any) -> str:
        captured.update(kwargs)
        return "volcengine-model"

    monkeypatch.setattr(chat_factory, "create_volcengine_chat_model", _fake_volcengine_chat_model)

    result = chat_factory.create_chat_model(
        model="doubao-test",
        provider=LlmProvider.VOLCENGINE,
        extra_body={"thinking": {"type": "enabled", "debug": True}},
        think=False,
    )

    assert result == "volcengine-model"
    assert captured["extra_body"] == {
        "thinking": {"type": "disabled", "debug": True},
    }


def test_create_chat_model_accepts_provider_string(monkeypatch: pytest.MonkeyPatch) -> None:
    """测试目的：验证 create_chat_model 支持 provider 字符串输入；预期结果：字符串 `aliyun` 可被识别并正确路由。"""

    monkeypatch.setattr(chat_factory, "create_aliyun_chat_model", lambda **_: "aliyun-model")

    result = chat_factory.create_chat_model(provider="aliyun")

    assert result == "aliyun-model"


def test_create_chat_model_accepts_enum_style_provider_string(monkeypatch: pytest.MonkeyPatch) -> None:
    """测试目的：验证 create_chat_model 支持 `LlmProvider.ALIYUN` 风格字符串；预期结果：可正确解析并路由。"""

    monkeypatch.setattr(chat_factory, "create_aliyun_chat_model", lambda **_: "aliyun-model")

    result = chat_factory.create_chat_model(provider="LlmProvider.ALIYUN")

    assert result == "aliyun-model"


def test_create_chat_model_raises_for_invalid_provider() -> None:
    """测试目的：验证 create_chat_model 对非法 provider 输入报错；预期结果：抛出 ValueError。"""

    with pytest.raises(ValueError):
        chat_factory.create_chat_model(provider="unknown")


def test_create_image_model_routes_to_volcengine_provider(monkeypatch: pytest.MonkeyPatch) -> None:
    """测试目的：验证 create_image_model 在 VOLCENGINE provider 下正确路由；预期结果：返回字节图像工厂产物且注入 thinking.disabled。"""

    captured: dict[str, Any] = {}

    def _fake_volcengine_image_model(**kwargs: Any) -> str:
        captured.update(kwargs)
        return "volcengine-image-model"

    monkeypatch.setattr(chat_factory, "create_volcengine_image_model", _fake_volcengine_image_model)

    result = chat_factory.create_image_model(
        model="doubao-vision-test",
        provider=LlmProvider.VOLCENGINE,
    )

    assert result == "volcengine-image-model"
    assert captured["model"] == "doubao-vision-test"
    assert captured["extra_body"] == {"thinking": {"type": "disabled"}}


def test_create_openai_chat_model_reads_env_and_enables_stream_usage(monkeypatch: pytest.MonkeyPatch) -> None:
    """测试目的：验证 OpenAI 聊天工厂读取 OPENAI 环境变量并默认启用 stream_usage；预期结果：构造参数包含环境值且 `stream_usage=True`。"""

    monkeypatch.setenv("OPENAI_API_KEY", "openai-key")
    monkeypatch.setenv("OPENAI_CHAT_MODEL", "gpt-env")
    monkeypatch.setenv("OPENAI_BASE_URL", "https://openai.example.com/v1")
    monkeypatch.setattr(openai_provider, "ChatOpenAI", _FakeChatClient)

    model = openai_provider.create_openai_chat_model(extra_body={"enable_thinking": True})

    assert isinstance(model, _FakeChatClient)
    assert model.kwargs["model"] == "gpt-env"
    assert model.kwargs["base_url"] == "https://openai.example.com/v1"
    assert model.kwargs["extra_body"] == {"enable_thinking": True}
    assert model.kwargs["stream_usage"] is True


def test_create_openai_chat_model_raises_without_api_key(monkeypatch: pytest.MonkeyPatch) -> None:
    """测试目的：验证 OpenAI 聊天工厂在缺少 API Key 时失败；预期结果：抛出 RuntimeError。"""

    monkeypatch.delenv("OPENAI_API_KEY", raising=False)

    with pytest.raises(RuntimeError, match="OPENAI_API_KEY is not set"):
        openai_provider.create_openai_chat_model(model="gpt-test")


def test_create_openai_chat_model_raises_without_model(monkeypatch: pytest.MonkeyPatch) -> None:
    """测试目的：验证 OpenAI 聊天工厂在未传模型且环境无默认模型时失败；预期结果：抛出 RuntimeError。"""

    monkeypatch.setenv("OPENAI_API_KEY", "openai-key")
    monkeypatch.delenv("OPENAI_CHAT_MODEL", raising=False)

    with pytest.raises(RuntimeError, match="OPENAI_CHAT_MODEL is not set"):
        openai_provider.create_openai_chat_model()


def test_create_openai_image_model_raises_without_model(monkeypatch: pytest.MonkeyPatch) -> None:
    """测试目的：验证 OpenAI 图像工厂在未传模型且环境无默认模型时失败；预期结果：抛出 RuntimeError。"""

    monkeypatch.setenv("OPENAI_API_KEY", "openai-key")
    monkeypatch.delenv("OPENAI_IMAGE_MODEL", raising=False)

    with pytest.raises(RuntimeError, match="OPENAI_IMAGE_MODEL is not set"):
        openai_provider.create_openai_image_model()


def test_create_openai_embedding_model_reads_env(monkeypatch: pytest.MonkeyPatch) -> None:
    """测试目的：验证 OpenAI 嵌入工厂读取 OPENAI 环境变量；预期结果：构造参数包含环境值且维度正确透传。"""

    monkeypatch.setenv("OPENAI_API_KEY", "openai-key")
    monkeypatch.setenv("OPENAI_EMBEDDING_MODEL", "text-embedding-3-large")
    monkeypatch.setenv("OPENAI_BASE_URL", "https://openai.example.com/v1")
    monkeypatch.setattr(openai_provider, "OpenAIEmbeddings", _FakeChatClient)

    model = openai_provider.create_openai_embedding_model(dimensions=1536)

    assert isinstance(model, _FakeChatClient)
    assert model.kwargs["model"] == "text-embedding-3-large"
    assert model.kwargs["base_url"] == "https://openai.example.com/v1"
    assert model.kwargs["dimensions"] == 1536


def test_create_openai_embedding_model_raises_without_model(monkeypatch: pytest.MonkeyPatch) -> None:
    """测试目的：验证 OpenAI 嵌入工厂在未传模型且环境无默认模型时失败；预期结果：抛出 RuntimeError。"""

    monkeypatch.setenv("OPENAI_API_KEY", "openai-key")
    monkeypatch.delenv("OPENAI_EMBEDDING_MODEL", raising=False)

    with pytest.raises(RuntimeError, match="OPENAI_EMBEDDING_MODEL is not set"):
        openai_provider.create_openai_embedding_model()


def test_create_aliyun_chat_model_reads_env_and_enables_stream_usage(monkeypatch: pytest.MonkeyPatch) -> None:
    """测试目的：验证阿里云聊天工厂读取 DASHSCOPE 环境变量并默认启用 stream_usage；预期结果：构造参数包含环境值且 `stream_usage=True`。"""

    monkeypatch.setenv("DASHSCOPE_API_KEY", "dashscope-key")
    monkeypatch.setenv("DASHSCOPE_CHAT_MODEL", "qwen-env")
    monkeypatch.setenv("DASHSCOPE_BASE_URL", "https://dashscope.example.com/v1")
    monkeypatch.setattr(aliyun_provider, "ChatQwen", _FakeChatClient)

    model = aliyun_provider.create_aliyun_chat_model(extra_body={"enable_thinking": True})

    assert isinstance(model, _FakeChatClient)
    assert model.kwargs["model"] == "qwen-env"
    assert model.kwargs["base_url"] == "https://dashscope.example.com/v1"
    assert model.kwargs["extra_body"] == {"enable_thinking": True}
    assert model.kwargs["stream_usage"] is True


def test_create_aliyun_chat_model_raises_without_api_key(monkeypatch: pytest.MonkeyPatch) -> None:
    """测试目的：验证阿里云聊天工厂在缺少 API Key 时失败；预期结果：抛出 RuntimeError。"""

    monkeypatch.delenv("DASHSCOPE_API_KEY", raising=False)

    with pytest.raises(RuntimeError, match="DASHSCOPE_API_KEY is not set"):
        aliyun_provider.create_aliyun_chat_model(model="qwen-test")


def test_create_aliyun_chat_model_raises_without_model(monkeypatch: pytest.MonkeyPatch) -> None:
    """测试目的：验证阿里云聊天工厂在未显式传模型且环境无默认模型时失败；预期结果：抛出 RuntimeError。"""

    monkeypatch.setenv("DASHSCOPE_API_KEY", "dashscope-key")
    monkeypatch.delenv("DASHSCOPE_CHAT_MODEL", raising=False)

    with pytest.raises(RuntimeError, match="DASHSCOPE_CHAT_MODEL is not set"):
        aliyun_provider.create_aliyun_chat_model()


def test_create_aliyun_image_model_raises_without_model(monkeypatch: pytest.MonkeyPatch) -> None:
    """测试目的：验证阿里云图像工厂在未显式传模型且环境无默认模型时失败；预期结果：抛出 RuntimeError。"""

    monkeypatch.setenv("DASHSCOPE_API_KEY", "dashscope-key")
    monkeypatch.delenv("DASHSCOPE_IMAGE_MODEL", raising=False)

    with pytest.raises(RuntimeError, match="DASHSCOPE_IMAGE_MODEL is not set"):
        aliyun_provider.create_aliyun_image_model()


def test_create_aliyun_embedding_model_raises_without_model(monkeypatch: pytest.MonkeyPatch) -> None:
    """测试目的：验证阿里云嵌入工厂在未传模型且环境无默认模型时失败；预期结果：抛出 RuntimeError。"""

    monkeypatch.setenv("DASHSCOPE_API_KEY", "dashscope-key")
    monkeypatch.delenv("DASHSCOPE_EMBEDDING_MODEL", raising=False)

    with pytest.raises(RuntimeError, match="DASHSCOPE_EMBEDDING_MODEL is not set"):
        aliyun_provider.create_aliyun_embedding_model()


def test_create_volcengine_chat_model_reads_env_and_enables_stream_usage(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：验证字节聊天工厂读取 VOLCENGINE_LLM 环境变量并默认启用 stream_usage；预期结果：构造参数包含环境值且 `stream_usage=True`。"""

    monkeypatch.setenv("VOLCENGINE_LLM_API_KEY", "volc-key")
    monkeypatch.setenv("VOLCENGINE_LLM_CHAT_MODEL", "doubao-chat-env")
    monkeypatch.setenv("VOLCENGINE_LLM_BASE_URL", "https://volc.example.com/api/v3")
    monkeypatch.setattr(volcengine_provider, "ChatVolcengine", _FakeChatClient)

    model = volcengine_provider.create_volcengine_chat_model(
        extra_body={"thinking": {"type": "enabled"}},
    )

    assert isinstance(model, _FakeChatClient)
    assert model.kwargs["model"] == "doubao-chat-env"
    assert model.kwargs["base_url"] == "https://volc.example.com/api/v3"
    assert model.kwargs["extra_body"] == {"thinking": {"type": "enabled"}}
    assert model.kwargs["stream_usage"] is True


def test_create_volcengine_chat_model_raises_without_api_key(monkeypatch: pytest.MonkeyPatch) -> None:
    """测试目的：验证字节聊天工厂在缺少 API Key 时失败；预期结果：抛出 RuntimeError。"""

    monkeypatch.delenv("VOLCENGINE_LLM_API_KEY", raising=False)

    with pytest.raises(RuntimeError, match="VOLCENGINE_LLM_API_KEY is not set"):
        volcengine_provider.create_volcengine_chat_model(model="doubao-chat")


def test_create_volcengine_chat_model_raises_when_model_not_provided(monkeypatch: pytest.MonkeyPatch) -> None:
    """测试目的：验证字节聊天工厂在未传模型且环境无默认模型时失败；预期结果：抛出 RuntimeError。"""

    monkeypatch.setenv("VOLCENGINE_LLM_API_KEY", "volc-key")
    monkeypatch.delenv("VOLCENGINE_LLM_CHAT_MODEL", raising=False)

    with pytest.raises(RuntimeError, match="VOLCENGINE_LLM_CHAT_MODEL is not set"):
        volcengine_provider.create_volcengine_chat_model()


def test_create_volcengine_image_model_raises_when_model_not_provided(monkeypatch: pytest.MonkeyPatch) -> None:
    """测试目的：验证字节图像工厂在未传模型且环境无默认模型时失败；预期结果：抛出 RuntimeError。"""

    monkeypatch.setenv("VOLCENGINE_LLM_API_KEY", "volc-key")
    monkeypatch.delenv("VOLCENGINE_LLM_IMAGE_MODEL", raising=False)

    with pytest.raises(RuntimeError, match="VOLCENGINE_LLM_IMAGE_MODEL is not set"):
        volcengine_provider.create_volcengine_image_model()


def test_create_volcengine_image_model_reads_env(monkeypatch: pytest.MonkeyPatch) -> None:
    """测试目的：验证字节图像工厂在环境提供默认模型时可正常创建；预期结果：返回模型实例并使用 `VOLCENGINE_LLM_IMAGE_MODEL`。"""

    monkeypatch.setenv("VOLCENGINE_LLM_API_KEY", "volc-key")
    monkeypatch.setenv("VOLCENGINE_LLM_IMAGE_MODEL", "doubao-image-env")
    monkeypatch.setenv("VOLCENGINE_LLM_BASE_URL", "https://volc.example.com/api/v3")
    monkeypatch.setattr(volcengine_provider, "ChatVolcengine", _FakeChatClient)

    model = volcengine_provider.create_volcengine_image_model(extra_body={"thinking": {"type": "disabled"}})

    assert isinstance(model, _FakeChatClient)
    assert model.kwargs["model"] == "doubao-image-env"
    assert model.kwargs["base_url"] == "https://volc.example.com/api/v3"
    assert model.kwargs["extra_body"] == {"thinking": {"type": "disabled"}}


def test_create_volcengine_embedding_model_raises_when_model_not_provided(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：验证字节嵌入工厂在未传模型且环境无默认模型时失败；预期结果：抛出 RuntimeError。"""

    monkeypatch.setenv("VOLCENGINE_LLM_API_KEY", "volc-key")
    monkeypatch.delenv("VOLCENGINE_LLM_EMBEDDING_MODEL", raising=False)

    with pytest.raises(RuntimeError, match="VOLCENGINE_LLM_EMBEDDING_MODEL is not set"):
        volcengine_provider.create_volcengine_embedding_model()


def test_thinking_resolver_keeps_openai_extra_body() -> None:
    """测试目的：验证 think 归一化在 OPENAI provider 下不注入特殊字段；预期结果：仅保留原 extra_body。"""

    extra = llm_common_module.resolve_provider_extra_body(
        provider=LlmProvider.OPENAI,
        think=True,
    )

    assert extra is None


def test_thinking_resolver_applies_aliyun_rules() -> None:
    """测试目的：验证 think 归一化在 ALIYUN provider 下的开启/关闭规则；预期结果：开启注入 enable_thinking=True，关闭注入 enable_thinking=False。"""

    enabled = llm_common_module.resolve_provider_extra_body(
        provider=LlmProvider.ALIYUN,
        think=True,
    )
    disabled = llm_common_module.resolve_provider_extra_body(
        provider=LlmProvider.ALIYUN,
        extra_body={"enable_thinking": True},
        think=False,
    )

    assert enabled == {"enable_thinking": True}
    assert disabled == {"enable_thinking": False}


def test_thinking_resolver_applies_volcengine_rules() -> None:
    """测试目的：验证 think 归一化在 VOLCENGINE provider 下的开启/关闭规则；预期结果：分别注入 thinking.enabled 与 thinking.disabled 并保留非思考字段。"""

    enabled = llm_common_module.resolve_provider_extra_body(
        provider=LlmProvider.VOLCENGINE,
        think=True,
    )
    disabled = llm_common_module.resolve_provider_extra_body(
        provider=LlmProvider.VOLCENGINE,
        think=False,
    )

    assert enabled == {"thinking": {"type": "enabled"}}
    assert disabled == {"thinking": {"type": "disabled"}}


def test_thinking_resolver_preserves_explicit_disable_for_aliyun() -> None:
    """测试目的：验证阿里云在 think=False 时会显式下发关闭开关；预期结果：返回 enable_thinking=False。"""

    result = llm_common_module.resolve_provider_extra_body(
        provider=LlmProvider.ALIYUN,
        extra_body={"enable_thinking": True},
        think=False,
    )

    assert result == {"enable_thinking": False}


def test_resolve_llm_value_priority_explicit_over_env_and_default(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：验证配置优先级中显式参数最高；预期结果：同键同时存在时返回 explicit 值。"""

    monkeypatch.setenv("LLM_TEST_KEY", "env-value")

    resolved = llm_common_module.resolve_llm_value(
        name="LLM_TEST_KEY",
        explicit="explicit-value",
        default="default-value",
    )

    assert resolved == "explicit-value"


def test_resolve_llm_value_priority_env_over_default(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：验证配置优先级中环境变量高于默认值；预期结果：explicit 缺失时返回 env 值。"""

    monkeypatch.setenv("LLM_TEST_KEY", "env-value")

    resolved = llm_common_module.resolve_llm_value(
        name="LLM_TEST_KEY",
        explicit=None,
        default="default-value",
    )

    assert resolved == "env-value"


def test_resolve_llm_value_returns_default_when_missing(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：验证配置缺失时返回默认值；预期结果：未提供 explicit 且环境无值时返回 default。"""

    monkeypatch.delenv("LLM_TEST_KEY", raising=False)

    resolved = llm_common_module.resolve_llm_value(
        name="LLM_TEST_KEY",
        explicit=None,
        default="default-value",
    )

    assert resolved == "default-value"


def test_chat_volcengine_streaming_injects_reasoning_content(monkeypatch: pytest.MonkeyPatch) -> None:
    """测试目的：验证 ChatVolcengine 流式片段会注入 reasoning_content；预期结果：返回 chunk.message.additional_kwargs 包含 reasoning_content。"""

    def _fake_super_convert(_self, _chunk, _default_chunk_class, _base_generation_info):
        return ChatGenerationChunk(
            message=AIMessageChunk(content=""),
            text="",
        )

    monkeypatch.setattr(
        volcengine_provider.ChatOpenAI,
        "_convert_chunk_to_generation_chunk",
        _fake_super_convert,
    )

    model = volcengine_provider.ChatVolcengine.model_construct()
    result = model._convert_chunk_to_generation_chunk(
        chunk={"choices": [{"delta": {"reasoning_content": "思考分片"}}]},
        default_chunk_class=AIMessageChunk,
        base_generation_info=None,
    )

    assert result is not None
    assert result.message.additional_kwargs["reasoning_content"] == "思考分片"


def test_chat_volcengine_non_streaming_injects_reasoning_content(monkeypatch: pytest.MonkeyPatch) -> None:
    """测试目的：验证 ChatVolcengine 非流式结果会注入 reasoning_content；预期结果：result.generations[0].message.additional_kwargs 包含 reasoning_content。"""

    def _fake_super_create_result(_self, _response, _generation_info=None):
        return SimpleNamespace(
            generations=[SimpleNamespace(message=AIMessageChunk(content=""))],
        )

    monkeypatch.setattr(
        volcengine_provider.ChatOpenAI,
        "_create_chat_result",
        _fake_super_create_result,
    )

    response = SimpleNamespace(
        choices=[SimpleNamespace(message=SimpleNamespace(reasoning_content="思考正文"))],
    )

    model = volcengine_provider.ChatVolcengine.model_construct()
    result = model._create_chat_result(response)

    assert result.generations[0].message.additional_kwargs["reasoning_content"] == "思考正文"


def test_create_embedding_model_routes_to_openai_provider(monkeypatch: pytest.MonkeyPatch) -> None:
    """测试目的：验证 create_embedding_model 在 OPENAI provider 下正确路由；预期结果：调用 OpenAI 嵌入工厂并透传参数。"""

    captured: dict[str, Any] = {}

    def _fake_openai_embedding_model(**kwargs: Any) -> str:
        captured.update(kwargs)
        return "openai-embedding"

    monkeypatch.setattr(embedding_factory, "create_openai_embedding_model", _fake_openai_embedding_model)

    result = embedding_factory.create_embedding_model(
        provider=LlmProvider.OPENAI,
        model="text-embedding-3-large",
        dimensions=1536,
    )

    assert result == "openai-embedding"
    assert captured["model"] == "text-embedding-3-large"
    assert captured["dimensions"] == 1536


def test_create_embedding_model_routes_to_aliyun_provider(monkeypatch: pytest.MonkeyPatch) -> None:
    """测试目的：验证 create_embedding_model 在 ALIYUN provider 下正确路由；预期结果：调用阿里云嵌入工厂并透传参数。"""

    captured: dict[str, Any] = {}

    def _fake_aliyun_embedding_model(**kwargs: Any) -> str:
        captured.update(kwargs)
        return "aliyun-embedding"

    monkeypatch.setattr(embedding_factory, "create_aliyun_embedding_model", _fake_aliyun_embedding_model)

    result = embedding_factory.create_embedding_model(
        provider=LlmProvider.ALIYUN,
        model="text-embedding-v4",
        dimensions=1024,
    )

    assert result == "aliyun-embedding"
    assert captured["model"] == "text-embedding-v4"
    assert captured["dimensions"] == 1024


def test_create_embedding_model_routes_to_volcengine_provider(monkeypatch: pytest.MonkeyPatch) -> None:
    """测试目的：验证 create_embedding_model 在 VOLCENGINE provider 下正确路由；预期结果：调用字节嵌入工厂并透传参数。"""

    captured: dict[str, Any] = {}

    def _fake_volcengine_embedding_model(**kwargs: Any) -> str:
        captured.update(kwargs)
        return "volcengine-embedding"

    monkeypatch.setattr(embedding_factory, "create_volcengine_embedding_model", _fake_volcengine_embedding_model)

    result = embedding_factory.create_embedding_model(
        provider=LlmProvider.VOLCENGINE,
        model="doubao-embedding-test",
        dimensions=1024,
    )

    assert result == "volcengine-embedding"
    assert captured["model"] == "doubao-embedding-test"
    assert captured["dimensions"] == 1024


def test_create_embedding_model_uses_env_provider_when_provider_is_none(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：验证未显式传 embedding provider 时可从环境读取；预期结果：`LLM_PROVIDER=aliyun` 时路由阿里云工厂。"""

    monkeypatch.setenv("LLM_PROVIDER", "aliyun")
    monkeypatch.setattr(embedding_factory, "create_aliyun_embedding_model", lambda **_kwargs: "aliyun-embedding")

    result = embedding_factory.create_embedding_model(model="text-embedding-v4")

    assert result == "aliyun-embedding"


def test_create_embedding_model_defaults_to_openai_when_provider_missing(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：验证 embedding provider 未显式传且环境缺失时默认 openai；预期结果：路由 OpenAI 工厂。"""

    monkeypatch.delenv("LLM_PROVIDER", raising=False)
    monkeypatch.setattr(embedding_factory, "create_openai_embedding_model", lambda **_kwargs: "openai-embedding")

    result = embedding_factory.create_embedding_model(model="text-embedding-3-large")

    assert result == "openai-embedding"


def test_create_embedding_model_validates_dimensions_and_api_key(monkeypatch: pytest.MonkeyPatch) -> None:
    """测试目的：验证嵌入模型工厂参数校验逻辑；预期结果：缺少 API Key 抛 RuntimeError，非法 dimensions 抛 ValueError。"""

    monkeypatch.delenv("DASHSCOPE_API_KEY", raising=False)
    with pytest.raises(RuntimeError, match="DASHSCOPE_API_KEY is not set"):
        embedding_factory.create_embedding_model(
            provider=LlmProvider.ALIYUN,
            model="text-embedding-v4",
        )

    with pytest.raises(ValueError, match="Dimensions must be between 128 and 4096 and a multiple of 2"):
        embedding_factory.create_embedding_model(
            provider=LlmProvider.ALIYUN,
            model="text-embedding-v4",
            api_key="dashscope-key",
            dimensions=127,
        )
