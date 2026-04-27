import datetime
from types import SimpleNamespace

from app.schemas.document.message import MessageRole
from app.services import memory_summary_service as service_module


def _build_message(*, message_id: str, message_uuid: str, role: MessageRole, content: str):
    """测试辅助函数：构造最小消息对象；预期结果：返回含 id/uuid/role/content/cards 字段的命名空间实例。"""

    return SimpleNamespace(
        id=message_id,
        uuid=message_uuid,
        role=role,
        content=content,
        cards=[],
        created_at=datetime.datetime(2026, 1, 1, 10, 0, 0),
    )


def test_refresh_conversation_summary_skips_when_pending_below_trigger(monkeypatch):
    """测试目的：未达到阈值不触发总结；预期结果：不会调用保存摘要。"""

    saved_calls: list[dict] = []
    monkeypatch.setattr(service_module, "get_conversation_summary", lambda *, conversation_id: None)
    monkeypatch.setattr(service_module, "resolve_assistant_summary_trigger_window", lambda: 100)
    monkeypatch.setattr(
        service_module,
        "count_summarizable_messages",
        lambda *, conversation_id, after_message_id: 99,
    )
    monkeypatch.setattr(
        service_module,
        "save_conversation_summary",
        lambda **kwargs: saved_calls.append(kwargs),
    )

    service_module.refresh_conversation_summary_if_needed(conversation_id="507f1f77bcf86cd799439011")

    assert saved_calls == []


def test_refresh_conversation_summary_persists_latest_window_with_cas(monkeypatch):
    """测试目的：达到阈值后按最新窗口总结并CAS写入；预期结果：保存参数包含新游标、版本号与期望游标。"""

    saved_payload: dict = {}
    monkeypatch.setattr(
        service_module,
        "get_conversation_summary",
        lambda *, conversation_id: SimpleNamespace(
            summary_content="旧摘要",
            status="success",
            summary_version=3,
            last_summarized_message_id="507f1f77bcf86cd799439050",
        ),
    )
    monkeypatch.setattr(service_module, "resolve_assistant_summary_trigger_window", lambda: 2)
    monkeypatch.setattr(
        service_module,
        "count_summarizable_messages",
        lambda *, conversation_id, after_message_id: 10,
    )
    monkeypatch.setattr(
        service_module,
        "list_latest_summarizable_messages",
        lambda *, conversation_id, limit, after_message_id: [
            _build_message(
                message_id="507f1f77bcf86cd799439081",
                message_uuid="msg-81",
                role=MessageRole.USER,
                content="用户提问",
            ),
            _build_message(
                message_id="507f1f77bcf86cd799439082",
                message_uuid="msg-82",
                role=MessageRole.AI,
                content="助手回答",
            ),
        ],
    )
    monkeypatch.setattr(service_module, "resolve_agent_summary_model_name", lambda: "summary-model")
    monkeypatch.setattr(
        service_module,
        "_call_summary_llm",
        lambda *, system_prompt, user_prompt: "新摘要",
    )
    monkeypatch.setattr(service_module, "_resolve_summary_budget_max_tokens", lambda: 2000)
    monkeypatch.setattr(
        service_module,
        "_enforce_summary_token_budget",
        lambda *, summary_text, max_tokens, model_name: ("新摘要", 120),
    )
    monkeypatch.setattr(
        service_module,
        "save_conversation_summary",
        lambda **kwargs: saved_payload.update(kwargs) or "507f1f77bcf86cd799439091",
    )

    service_module.refresh_conversation_summary_if_needed(conversation_id="507f1f77bcf86cd799439011")

    assert saved_payload["conversation_id"] == "507f1f77bcf86cd799439011"
    assert saved_payload["summary_content"] == "新摘要"
    assert saved_payload["last_summarized_message_id"] == "507f1f77bcf86cd799439082"
    assert saved_payload["last_summarized_message_uuid"] == "msg-82"
    assert saved_payload["summary_version"] == 4
    assert saved_payload["summary_token_count"] == 120
    assert saved_payload["expected_last_summarized_message_id"] == "507f1f77bcf86cd799439050"


def test_resolve_summary_budget_max_tokens_prefers_agent_config(monkeypatch):
    """测试目的：聊天历史总结预算应读取 `resolve_agent_summary_max_tokens` 结果；预期结果：返回解析函数提供的预算值。"""

    monkeypatch.setattr(service_module, "resolve_agent_summary_max_tokens", lambda: 4096)

    resolved_max_tokens = service_module._resolve_summary_budget_max_tokens()

    assert resolved_max_tokens == 4096


def test_resolve_summary_budget_max_tokens_treats_zero_as_unlimited(monkeypatch):
    """测试目的：聊天历史总结预算为 0 时应视为不限制；预期结果：预算解析返回 None。"""

    monkeypatch.setattr(service_module, "resolve_agent_summary_max_tokens", lambda: None)

    resolved_max_tokens = service_module._resolve_summary_budget_max_tokens()

    assert resolved_max_tokens is None


def test_enforce_summary_token_budget_skips_truncation_when_max_tokens_is_zero(monkeypatch):
    """测试目的：预算为 0 时应视为不限制；预期结果：直接返回原摘要与原 token 数。"""

    # 使用本地桩避免测试环境首次加载 tiktoken 编码时触发外网请求。
    def _fake_count_tokens_with_fallback(*, text: str, model_name: str | None) -> int:
        del model_name
        return len(text)

    monkeypatch.setattr(
        service_module,
        "_count_tokens_with_fallback",
        _fake_count_tokens_with_fallback,
    )

    normalized_summary, token_count = service_module._enforce_summary_token_budget(
        summary_text="这是一个不会被裁剪的摘要文本",
        max_tokens=0,
        model_name=None,
    )

    assert normalized_summary == "这是一个不会被裁剪的摘要文本"
    assert token_count > 0
