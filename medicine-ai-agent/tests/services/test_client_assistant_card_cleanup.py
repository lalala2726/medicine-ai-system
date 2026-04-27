from types import SimpleNamespace

from langchain_core.messages import AIMessage, HumanMessage

from app.services import client_assistant_service as service_module


def test_prepare_existing_conversation_hides_visible_cards_before_loading_memory(monkeypatch):
    calls: list[tuple[str, dict]] = []

    monkeypatch.setattr(
        service_module,
        "_load_client_conversation",
        lambda *, conversation_uuid, user_id: "507f1f77bcf86cd799439011",
    )
    monkeypatch.setattr(
        service_module,
        "_hide_visible_conversation_cards",
        lambda *, conversation_id: calls.append(
            (
                "hide_visible_cards",
                {"conversation_id": conversation_id},
            )
        ),
    )
    monkeypatch.setattr(service_module, "resolve_assistant_memory_mode", lambda: "window")
    monkeypatch.setattr(
        service_module,
        "load_memory",
        lambda *, memory_type, conversation_uuid, user_id, include_history_hidden: (
            calls.append(
                (
                    "load_memory",
                    {
                        "memory_type": memory_type,
                        "conversation_uuid": conversation_uuid,
                        "user_id": user_id,
                        "include_history_hidden": include_history_hidden,
                    },
                )
            ),
            SimpleNamespace(
                messages=[
                    HumanMessage(content="历史问题"),
                    AIMessage(content="历史回答"),
                ]
            ),
        )[-1],
    )

    context = service_module._prepare_existing_conversation(
        conversation_uuid="client-conv-1",
        user_id=100,
        question="本轮问题",
        assistant_message_uuid="assistant-msg-2",
    )

    assert calls == [
        (
            "hide_visible_cards",
            {"conversation_id": "507f1f77bcf86cd799439011"},
        ),
        (
            "load_memory",
            {
                "memory_type": "window",
                "conversation_uuid": "client-conv-1",
                "user_id": 100,
                "include_history_hidden": False,
            },
        ),
    ]
    assert [message.content for message in context.history_messages] == [
        "历史问题",
        "历史回答",
        "本轮问题",
    ]


def test_hide_visible_conversation_cards_delegates_to_message_service(monkeypatch):
    captured: dict[str, str] = {}

    monkeypatch.setattr(
        service_module,
        "hide_visible_cards_in_conversation",
        lambda *, conversation_id: captured.update({"conversation_id": conversation_id}) or 2,
    )

    service_module._hide_visible_conversation_cards(
        conversation_id="507f1f77bcf86cd799439011",
    )

    assert captured == {"conversation_id": "507f1f77bcf86cd799439011"}
