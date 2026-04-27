import asyncio
import json
from types import SimpleNamespace
from uuid import UUID

from app.agent.services.card_render_service import ProductCardData, ProductCardProduct
from app.core.agent import agent_orchestrator as orchestrator_module
from app.core.agent.agent_event_bus import emit_answer_delta, enqueue_final_sse_response
from app.schemas.sse_response import (
    Action,
    AssistantResponse,
    Card,
    Content,
    MessageType,
    UserOrderListPayload,
)


def _parse_sse_payload(raw_event: str) -> dict:
    prefix = "data: "
    assert raw_event.startswith(prefix)
    return json.loads(raw_event[len(prefix):].strip())


def _build_action_response(*, message: str, order_status: str | None, priority: int) -> AssistantResponse:
    return AssistantResponse(
        type=MessageType.ACTION,
        content=Content(message=message),
        action=Action(
            type="navigate",
            target="user_order_list",
            payload=UserOrderListPayload(orderStatus=order_status),
            priority=priority,
        ),
    )


def _build_card_response(
        *product_ids: int,
        card_uuid: str = "123e4567-e89b-12d3-a456-426614174000",
        persist_card: bool = False,
) -> AssistantResponse:
    products = [
        ProductCardProduct(
            id=str(product_id),
            name=f"商品{product_id}",
            image=f"https://example.com/{product_id}.png",
            price=f"{index + 1}.00",
        )
        for index, product_id in enumerate(product_ids)
    ]
    return AssistantResponse(
        type=MessageType.CARD,
        content=Content(),
        card=Card(
            type="product-card",
            data=ProductCardData(
                title="为您推荐以下商品",
                products=products,
            ).model_dump(mode="json"),
        ),
        meta={
            "card_uuid": card_uuid,
            "persist_card": persist_card,
        },
    )


def _build_selection_card_response() -> AssistantResponse:
    return AssistantResponse(
        type=MessageType.CARD,
        content=Content(),
        card=Card(
            type="selection-card",
            data={
                "title": "为了更准确判断，你现在有没有发热？",
                "options": ["没有发热", "低烧", "高烧", "不确定"],
            },
        ),
        meta={
            "card_uuid": "123e4567-e89b-12d3-a456-426614174010",
            "persist_card": True,
        },
    )


def _build_consultation_followup_card_response() -> AssistantResponse:
    return AssistantResponse(
        type=MessageType.CARD,
        content=Content(),
        card=Card(
            type="consultation-followup-card",
            data={
                "title": "这次喉咙疼更偏向哪个位置？",
                "description": "结合你刚才补充的情况，我们更偏向咽部刺激或炎症方向，再确认疼痛位置会更稳。",
                "options": ["咽后壁", "扁桃体", "舌根", "不确定"],
                "selectionMode": "multiple",
                "submitText": "发送",
                "allowCustomInput": True,
                "customInputPlaceholder": "补充症状或其他感受",
            },
        ),
        meta={
            "card_uuid": "123e4567-e89b-12d3-a456-426614174012",
            "persist_card": True,
            "message_uuid": "followup-msg-1",
        },
    )


def _build_purchase_card_response() -> AssistantResponse:
    return AssistantResponse(
        type=MessageType.CARD,
        content=Content(),
        card=Card(
            type="product-purchase-card",
            data={
                "items": [
                    {"productId": 101, "quantity": 1},
                    {"productId": 102, "quantity": 1},
                ]
            },
        ),
        meta={
            "card_uuid": "123e4567-e89b-12d3-a456-426614174011",
            "persist_card": True,
        },
    )


def test_serialize_sse_includes_action_payload():
    payload = _build_action_response(
        message="已为你打开待支付订单列表",
        order_status="PENDING_PAYMENT",
        priority=100,
    )

    serialized = orchestrator_module.serialize_sse(payload)
    parsed = _parse_sse_payload(serialized)

    assert parsed["type"] == "action"
    assert parsed["content"]["message"] == "已为你打开待支付订单列表"
    assert parsed["action"] == {
        "type": "navigate",
        "target": "user_order_list",
        "payload": {"orderStatus": "PENDING_PAYMENT"},
        "priority": 100,
    }


def test_event_stream_flushes_final_actions_by_priority_before_end():
    class _FakeWorkflow:
        async def astream(self, _state: dict, **_kwargs):
            yield ("messages", (SimpleNamespace(content="你好"), {"langgraph_node": "chat_agent"}))
            enqueue_final_sse_response(
                _build_action_response(
                    message="已为你打开已完成订单列表",
                    order_status="COMPLETED",
                    priority=50,
                )
            )
            enqueue_final_sse_response(
                _build_action_response(
                    message="已为你打开待支付订单列表",
                    order_status="PENDING_PAYMENT",
                    priority=100,
                )
            )
            enqueue_final_sse_response(
                _build_action_response(
                    message="已为你打开待收货订单列表",
                    order_status="PENDING_RECEIPT",
                    priority=50,
                )
            )

    config = orchestrator_module.AssistantStreamConfig(
        workflow=_FakeWorkflow(),
        build_initial_state=lambda _question: {},
        extract_final_content=lambda _state: "",
        should_stream_token=lambda _node, _state: True,
        build_stream_config=None,
        invoke_sync=lambda state: state,
        map_exception=lambda exc: str(exc),
    )

    async def _collect() -> list[str]:
        events: list[str] = []
        async for event in orchestrator_module._event_stream(question="你好", config=config):
            events.append(event)
        return events

    rendered_events = asyncio.run(_collect())
    payloads = [_parse_sse_payload(event) for event in rendered_events]

    assert [payload["type"] for payload in payloads] == [
        "answer",
        "action",
        "action",
        "action",
        "answer",
    ]
    assert payloads[0]["content"]["text"] == "你好"
    assert payloads[1]["action"]["payload"]["orderStatus"] == "PENDING_PAYMENT"
    assert payloads[2]["action"]["payload"]["orderStatus"] == "COMPLETED"
    assert payloads[3]["action"]["payload"]["orderStatus"] == "PENDING_RECEIPT"
    assert payloads[4]["is_end"] is True


def test_serialize_sse_includes_product_card_payload():
    payload = _build_card_response(1001, 1002)

    serialized = orchestrator_module.serialize_sse(payload)
    parsed = _parse_sse_payload(serialized)

    assert parsed["type"] == "card"
    assert parsed["content"] == {}
    assert parsed["card"] == {
        "type": "product-card",
        "data": {
            "title": "为您推荐以下商品",
            "products": [
                {
                    "id": "1001",
                    "name": "商品1001",
                    "image": "https://example.com/1001.png",
                    "price": "1.00",
                },
                {
                    "id": "1002",
                    "name": "商品1002",
                    "image": "https://example.com/1002.png",
                    "price": "2.00",
                },
            ],
        },
    }
    assert parsed["meta"] == {
        "card_uuid": "123e4567-e89b-12d3-a456-426614174000",
        "persist_card": False,
    }


def test_event_stream_flushes_final_card_before_end():
    class _FakeWorkflow:
        async def astream(self, _state: dict, **_kwargs):
            yield ("messages", (SimpleNamespace(content="你好"), {"langgraph_node": "chat_agent"}))
            enqueue_final_sse_response(_build_card_response(1001, 1002))

    config = orchestrator_module.AssistantStreamConfig(
        workflow=_FakeWorkflow(),
        build_initial_state=lambda _question: {},
        extract_final_content=lambda _state: "",
        should_stream_token=lambda _node, _state: True,
        build_stream_config=None,
        invoke_sync=lambda state: state,
        map_exception=lambda exc: str(exc),
    )

    async def _collect() -> list[str]:
        events: list[str] = []
        async for event in orchestrator_module._event_stream(question="你好", config=config):
            events.append(event)
        return events

    rendered_events = asyncio.run(_collect())
    payloads = [_parse_sse_payload(event) for event in rendered_events]

    assert [payload["type"] for payload in payloads] == [
        "answer",
        "card",
        "answer",
    ]
    assert payloads[1]["card"]["data"]["title"] == "为您推荐以下商品"
    assert [item["id"] for item in payloads[1]["card"]["data"]["products"]] == [
        "1001",
        "1002",
    ]
    assert str(UUID(payloads[1]["meta"]["card_uuid"])) == payloads[1]["meta"]["card_uuid"]
    assert payloads[2]["is_end"] is True


def test_event_stream_keeps_action_priority_ahead_of_card():
    class _FakeWorkflow:
        async def astream(self, _state: dict, **_kwargs):
            yield ("messages", (SimpleNamespace(content="你好"), {"langgraph_node": "chat_agent"}))
            enqueue_final_sse_response(_build_card_response(1001, 1002))
            enqueue_final_sse_response(
                _build_action_response(
                    message="已为你打开待支付订单列表",
                    order_status="PENDING_PAYMENT",
                    priority=100,
                )
            )
            enqueue_final_sse_response(
                _build_action_response(
                    message="已为你打开已完成订单列表",
                    order_status="COMPLETED",
                    priority=50,
                )
            )

    config = orchestrator_module.AssistantStreamConfig(
        workflow=_FakeWorkflow(),
        build_initial_state=lambda _question: {},
        extract_final_content=lambda _state: "",
        should_stream_token=lambda _node, _state: True,
        build_stream_config=None,
        invoke_sync=lambda state: state,
        map_exception=lambda exc: str(exc),
    )

    async def _collect() -> list[str]:
        events: list[str] = []
        async for event in orchestrator_module._event_stream(question="你好", config=config):
            events.append(event)
        return events

    rendered_events = asyncio.run(_collect())
    payloads = [_parse_sse_payload(event) for event in rendered_events]

    assert [payload["type"] for payload in payloads] == [
        "answer",
        "action",
        "action",
        "card",
        "answer",
    ]
    assert payloads[1]["action"]["payload"]["orderStatus"] == "PENDING_PAYMENT"
    assert payloads[2]["action"]["payload"]["orderStatus"] == "COMPLETED"
    assert payloads[3]["card"]["data"]["title"] == "为您推荐以下商品"
    assert [item["id"] for item in payloads[3]["card"]["data"]["products"]] == [
        "1001",
        "1002",
    ]
    assert str(UUID(payloads[3]["meta"]["card_uuid"])) == payloads[3]["meta"]["card_uuid"]


def test_event_stream_passes_final_cards_to_answer_completed_callback():
    captured: dict = {}

    class _FakeWorkflow:
        async def astream(self, _state: dict, **_kwargs):
            yield ("messages", (SimpleNamespace(content="你好"), {"langgraph_node": "chat_agent"}))
            enqueue_final_sse_response(
                _build_card_response(
                    1001,
                    card_uuid="123e4567-e89b-12d3-a456-426614174001",
                    persist_card=True,
                )
            )
            enqueue_final_sse_response(
                _build_action_response(
                    message="已为你打开待支付订单列表",
                    order_status="PENDING_PAYMENT",
                    priority=100,
                )
            )
            enqueue_final_sse_response(
                _build_card_response(
                    1002,
                    card_uuid="123e4567-e89b-12d3-a456-426614174002",
                )
            )

    async def _on_answer_completed(
            answer_text: str,
            thinking_text: str | None,
            cards,
            finish_status,
    ) -> None:
        captured["answer_text"] = answer_text
        captured["thinking_text"] = thinking_text
        captured["cards"] = cards
        captured["finish_status"] = finish_status

    config = orchestrator_module.AssistantStreamConfig(
        workflow=_FakeWorkflow(),
        build_initial_state=lambda _question: {},
        extract_final_content=lambda _state: "",
        should_stream_token=lambda _node, _state: True,
        build_stream_config=None,
        invoke_sync=lambda state: state,
        map_exception=lambda exc: str(exc),
        on_answer_completed=_on_answer_completed,
    )

    async def _collect() -> list[str]:
        events: list[str] = []
        async for event in orchestrator_module._event_stream(question="你好", config=config):
            events.append(event)
        return events

    asyncio.run(_collect())

    assert captured["answer_text"] == "你好"
    assert captured["has_error"] is False
    assert captured["thinking_text"] == ""
    assert captured["cards"] == [
        {
            "id": "123e4567-e89b-12d3-a456-426614174001",
            "type": "product-card",
            "data": {
                "title": "为您推荐以下商品",
                "products": [
                    {
                        "id": "1001",
                        "name": "商品1001",
                        "image": "https://example.com/1001.png",
                        "price": "1.00",
                    }
                ],
            },
        },
    ]


def test_event_stream_consultation_collecting_flushes_selection_card_after_emitted_answer():
    class _FakeWorkflow:
        async def astream(self, _state: dict, **_kwargs):
            emit_answer_delta("先别太担心，这更像是常见上呼吸道不适。")
            emit_answer_delta("\n\n我还想确认一下，你有没有发热？")
            enqueue_final_sse_response(_build_selection_card_response())
            yield ("values", {"result": "这段兜底文本不应该再次输出"})

    config = orchestrator_module.AssistantStreamConfig(
        workflow=_FakeWorkflow(),
        build_initial_state=lambda _question: {},
        extract_final_content=lambda _state: "这段兜底文本不应该再次输出",
        should_stream_token=lambda _node, _state: True,
        build_stream_config=None,
        invoke_sync=lambda state: state,
        map_exception=lambda exc: str(exc),
    )

    async def _collect() -> list[str]:
        events: list[str] = []
        async for event in orchestrator_module._event_stream(question="我咳嗽流鼻涕", config=config):
            events.append(event)
        return events

    rendered_events = asyncio.run(_collect())
    payloads = [_parse_sse_payload(event) for event in rendered_events]

    assert [payload["type"] for payload in payloads] == [
        "answer",
        "answer",
        "card",
        "answer",
    ]
    assert payloads[0]["content"]["text"] == "先别太担心，这更像是常见上呼吸道不适。"
    assert payloads[1]["content"]["text"] == "\n\n我还想确认一下，你有没有发热？"
    assert payloads[2]["card"]["type"] == "selection-card"
    assert payloads[2]["card"]["data"]["title"] == "为了更准确判断，你现在有没有发热？"
    assert all("这段兜底文本不应该再次输出" not in json.dumps(payload, ensure_ascii=False) for payload in payloads)
    assert payloads[3]["is_end"] is True


def test_event_stream_consultation_interrupt_emits_followup_card_and_waiting_input_end():
    class _FakeWorkflow:
        async def astream(self, _state: dict, **_kwargs):
            emit_answer_delta("先别太担心，这更像是常见咽部不适。")
            yield ("values", {"__interrupt__": [object()], "result": "这段兜底文本不应该再次输出"})

    config = orchestrator_module.AssistantStreamConfig(
        workflow=_FakeWorkflow(),
        build_initial_state=lambda _question: {},
        extract_final_content=lambda _state: "这段兜底文本不应该再次输出",
        should_stream_token=lambda _node, _state: True,
        build_stream_config=None,
        invoke_sync=lambda state: state,
        map_exception=lambda exc: str(exc),
        build_interrupt_responses=lambda _state: [_build_consultation_followup_card_response()],
    )

    async def _collect() -> list[str]:
        events: list[str] = []
        async for event in orchestrator_module._event_stream(question="我喉咙疼", config=config):
            events.append(event)
        return events

    rendered_events = asyncio.run(_collect())
    payloads = [_parse_sse_payload(event) for event in rendered_events]

    assert [payload["type"] for payload in payloads] == ["answer", "card", "answer"]
    assert payloads[0]["content"]["text"] == "先别太担心，这更像是常见咽部不适。"
    assert payloads[1]["card"]["type"] == "consultation-followup-card"
    assert payloads[1]["meta"]["message_uuid"] == "followup-msg-1"
    assert payloads[2]["content"]["state"] == "waiting_input"
    assert payloads[2]["meta"]["run_status"] == "waiting_input"
    assert payloads[2]["is_end"] is True


def test_event_stream_consultation_diagnosis_flushes_purchase_card_after_emitted_answer():
    class _FakeWorkflow:
        async def astream(self, _state: dict, **_kwargs):
            emit_answer_delta("结合你的情况，更像是常见感冒方向。")
            emit_answer_delta("\n\n可考虑的药品有：感冒灵颗粒、板蓝根颗粒。")
            enqueue_final_sse_response(_build_purchase_card_response())
            yield ("values", {"result": "这段兜底文本不应该再次输出"})

    config = orchestrator_module.AssistantStreamConfig(
        workflow=_FakeWorkflow(),
        build_initial_state=lambda _question: {},
        extract_final_content=lambda _state: "这段兜底文本不应该再次输出",
        should_stream_token=lambda _node, _state: True,
        build_stream_config=None,
        invoke_sync=lambda state: state,
        map_exception=lambda exc: str(exc),
    )

    async def _collect() -> list[str]:
        events: list[str] = []
        async for event in orchestrator_module._event_stream(question="我是不是感冒了", config=config):
            events.append(event)
        return events

    rendered_events = asyncio.run(_collect())
    payloads = [_parse_sse_payload(event) for event in rendered_events]

    assert [payload["type"] for payload in payloads] == [
        "answer",
        "answer",
        "card",
        "answer",
    ]
    assert payloads[0]["content"]["text"] == "结合你的情况，更像是常见感冒方向。"
    assert payloads[1]["content"]["text"] == "\n\n可考虑的药品有：感冒灵颗粒、板蓝根颗粒。"
    assert payloads[2]["card"]["type"] == "product-purchase-card"
    assert payloads[2]["card"]["data"]["items"] == [
        {"productId": 101, "quantity": 1},
        {"productId": 102, "quantity": 1},
    ]
    assert all("这段兜底文本不应该再次输出" not in json.dumps(payload, ensure_ascii=False) for payload in payloads)
    assert payloads[3]["is_end"] is True


def test_extract_persistable_cards_ignores_cards_with_persist_flag_false():
    cards = orchestrator_module._extract_persistable_cards(
        [
            _build_card_response(
                1001,
                card_uuid="123e4567-e89b-12d3-a456-426614174001",
            )
        ]
    )

    assert cards is None


def test_extract_persistable_cards_ignores_cards_without_persist_flag():
    cards = orchestrator_module._extract_persistable_cards(
        [
            AssistantResponse(
                type=MessageType.CARD,
                content=Content(),
                card=Card(
                    type="product-card",
                    data=ProductCardData(
                        title="为您推荐以下商品",
                        products=[],
                    ).model_dump(mode="json"),
                ),
                meta={"card_uuid": "123e4567-e89b-12d3-a456-426614174001"},
            )
        ]
    )

    assert cards is None


def test_extract_persistable_cards_ignores_cards_without_card_uuid():
    cards = orchestrator_module._extract_persistable_cards(
        [
            AssistantResponse(
                type=MessageType.CARD,
                content=Content(),
                card=Card(
                    type="product-card",
                    data=ProductCardData(
                        title="为您推荐以下商品",
                        products=[],
                    ).model_dump(mode="json"),
                ),
                meta={"persist_card": True},
            )
        ]
    )

    assert cards is None
