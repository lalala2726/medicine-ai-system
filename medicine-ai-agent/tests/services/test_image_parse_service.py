from __future__ import annotations

from types import SimpleNamespace

import pytest
from langchain_core.messages import HumanMessage

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.services import image_parse_service


def test_parse_drug_images_normalizes_and_parses(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：图片解析应统一走 LangChain 图像模型；预期结果：消息包含规范化图片和文本提示，并返回结构化结果。"""

    captured: dict = {}
    monkeypatch.setattr(
        image_parse_service.FileUtils,
        "image_url_to_base64_data_url",
        lambda _url: "data:image/jpeg;base64,from-url",
    )

    class _FakeLlm:
        def invoke(self, messages):
            captured["messages"] = messages
            return SimpleNamespace(content='{"commonName":"阿司匹林"}')

    def _fake_create_agent_image_llm(**kwargs):
        captured["llm_kwargs"] = kwargs
        return _FakeLlm()

    monkeypatch.setattr(image_parse_service, "create_agent_image_llm", _fake_create_agent_image_llm)
    monkeypatch.setattr(
        image_parse_service,
        "load_managed_prompt",
        lambda prompt_key, local_prompt_path=None: "图片解析提示词",
    )

    result = image_parse_service.parse_drug_images(
        [
            "rawbase64",
            "https://example.com/1.png",
            "data:image/jpeg;base64,abc123",
        ],
    )

    assert result["commonName"] == "阿司匹林"
    assert "warmTips" in result
    assert captured["llm_kwargs"] == {
        "think": False,
        "extra_body": {"response_format": {"type": "json_object"}},
    }
    assert len(captured["messages"]) == 1
    message = captured["messages"][0]
    assert isinstance(message, HumanMessage)
    content = message.content
    assert content[0]["image_url"]["url"] == "data:image/png;base64,rawbase64"
    assert content[1]["image_url"]["url"] == "data:image/jpeg;base64,from-url"
    assert content[2]["image_url"]["url"] == "data:image/jpeg;base64,abc123"
    assert content[3] == {
        "type": "text",
        "text": "图片解析提示词",
    }


def test_parse_drug_images_raises_on_invalid_json(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """测试目的：模型返回非法 JSON 时应抛出业务异常；预期结果：错误码为 INTERNAL_ERROR。"""

    captured: dict = {}

    class _FakeLlm:
        def invoke(self, _messages):
            return SimpleNamespace(content="not-json")

    def _fake_create_agent_image_llm(**kwargs):
        captured["llm_kwargs"] = kwargs
        return _FakeLlm()

    monkeypatch.setattr(image_parse_service, "create_agent_image_llm", _fake_create_agent_image_llm)
    monkeypatch.setattr(
        image_parse_service,
        "load_managed_prompt",
        lambda prompt_key, local_prompt_path=None: "图片解析提示词",
    )

    with pytest.raises(ServiceException) as excinfo:
        image_parse_service.parse_drug_images(["raw"])

    assert excinfo.value.code == ResponseCode.INTERNAL_ERROR
    assert excinfo.value.message == "模型返回非 JSON 内容"
    assert captured["llm_kwargs"] == {
        "think": False,
        "extra_body": {"response_format": {"type": "json_object"}},
    }
