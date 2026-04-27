from unittest.mock import MagicMock

from langchain_core.messages import AIMessage, SystemMessage, HumanMessage

from app.services.admin_assistant_service import generate_title


def test_generate_title_with_valid_input(monkeypatch):
    """测试目的：验证标题生成主流程模型参数透传正确；预期结果：返回模型标题且 create_agent_title_llm 收到默认温度配置。"""
    mock_llm = MagicMock()
    mock_llm.invoke.return_value = AIMessage(content="感冒灵销量查询")

    captured_kwargs = {}

    def _fake_create_title_llm(**kwargs):
        captured_kwargs.update(kwargs)
        return mock_llm

    monkeypatch.setattr("app.services.admin_assistant_service.create_agent_title_llm", _fake_create_title_llm)
    monkeypatch.setattr(
        "app.services.admin_assistant_service.load_managed_prompt",
        lambda prompt_key, local_prompt_path=None: "# 模拟提示词",
    )

    title = generate_title("帮我查查感冒灵卖了多少")

    assert title == "感冒灵销量查询"
    assert "model" not in captured_kwargs
    assert captured_kwargs["temperature"] == 1.0
    mock_llm.invoke.assert_called_once()
    messages = mock_llm.invoke.call_args[0][0]
    assert isinstance(messages[0], SystemMessage)
    assert isinstance(messages[1], HumanMessage)
    assert messages[1].content == "帮我查查感冒灵卖了多少"


def test_generate_title_empty_input():
    """测试目的：验证空输入走兜底标题逻辑；预期结果：返回“未知标题”。"""
    assert generate_title("") == "未知标题"
    assert generate_title(None) == "未知标题"


def test_generate_title_llm_returns_empty(monkeypatch):
    """测试目的：验证模型返回空文本时走兜底标题；预期结果：返回“未知标题”。"""
    mock_llm = MagicMock()
    mock_llm.invoke.return_value = AIMessage(content="")

    monkeypatch.setattr("app.services.admin_assistant_service.create_agent_title_llm", lambda **kwargs: mock_llm)
    monkeypatch.setattr(
        "app.services.admin_assistant_service.load_managed_prompt",
        lambda prompt_key, local_prompt_path=None: "# 模拟提示词",
    )

    title = generate_title("测试输入")
    assert title == "未知标题"


def test_generate_title_strips_whitespace(monkeypatch):
    """测试目的：验证标题结果会去除首尾空白；预期结果：返回去空格后的标题文本。"""
    mock_llm = MagicMock()
    mock_llm.invoke.return_value = AIMessage(content="  有空格的标题  ")

    monkeypatch.setattr("app.services.admin_assistant_service.create_agent_title_llm", lambda **kwargs: mock_llm)
    monkeypatch.setattr(
        "app.services.admin_assistant_service.load_managed_prompt",
        lambda prompt_key, local_prompt_path=None: "# 模拟提示词",
    )

    title = generate_title("测试输入")
    assert title == "有空格的标题"
