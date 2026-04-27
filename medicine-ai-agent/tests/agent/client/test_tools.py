import asyncio
from uuid import UUID

import pytest
from pydantic import ValidationError

from app.agent.client.domain.after_sale import tools as after_sale_tools_module
from app.agent.client.domain.after_sale.schema import AfterSaleEligibilityRequest
from app.agent.client.domain.tools import card_tools as card_tools_module
from app.agent.client.domain.tools.card_tools import (
    send_consent_card,
    send_product_card,
    send_product_purchase_card,
    send_selection_card,
)
from app.agent.client.domain.tools.schema import (
    OpenUserAfterSaleListRequest,
    OpenUserOrderListRequest,
    SendConsentCardRequest,
    SendProductCardRequest,
    SendProductPurchaseCardItem,
    SendProductPurchaseCardRequest,
    SendSelectionCardRequest,
)
from app.agent.client.domain.tools.action_tools import (
    open_user_after_sale_list,
    open_user_order_list,
)
from app.agent.client.domain.order import tools as order_tools_module
from app.agent.client.domain.product import tools as product_tools_module
from app.agent.client.domain.product.schema import ProductSearchRequest
from app.core.agent.agent_event_bus import (
    drain_final_sse_responses,
    reset_final_response_queue,
    set_final_response_queue,
)
from app.schemas.sse_response import Card, MessageType


def test_open_user_order_list_enqueues_action_with_order_status():
    queue_token = set_final_response_queue()
    try:
        result = asyncio.run(open_user_order_list.ainvoke({"orderStatus": "PENDING_PAYMENT"}))
        queued_responses = drain_final_sse_responses()
    finally:
        reset_final_response_queue(queue_token)

    assert result == "已为你打开待支付订单列表"
    assert len(queued_responses) == 1
    response = queued_responses[0]
    assert response.type == MessageType.ACTION
    assert response.content.message == "已为你打开待支付订单列表"
    assert response.action is not None
    assert response.action.target == "user_order_list"
    assert response.action.payload.orderStatus == "PENDING_PAYMENT"
    assert response.action.priority == 100


def test_open_user_order_list_enqueues_action_without_order_status():
    queue_token = set_final_response_queue()
    try:
        result = asyncio.run(open_user_order_list.ainvoke({}))
        queued_responses = drain_final_sse_responses()
    finally:
        reset_final_response_queue(queue_token)

    assert result == "已为你打开订单列表"
    assert len(queued_responses) == 1
    response = queued_responses[0]
    assert response.action is not None
    assert response.action.target == "user_order_list"
    assert response.action.payload.orderStatus is None


def test_open_user_order_list_request_rejects_invalid_status():
    with pytest.raises(ValidationError):
        OpenUserOrderListRequest.model_validate({"orderStatus": "INVALID"})


def test_open_user_after_sale_list_enqueues_action_with_status():
    queue_token = set_final_response_queue()
    try:
        result = asyncio.run(
            open_user_after_sale_list.ainvoke({"afterSaleStatus": "PENDING"})
        )
        queued_responses = drain_final_sse_responses()
    finally:
        reset_final_response_queue(queue_token)

    assert result == "已为你打开待审核售后列表"
    assert len(queued_responses) == 1
    response = queued_responses[0]
    assert response.type == MessageType.ACTION
    assert response.content.message == "已为你打开待审核售后列表"
    assert response.action is not None
    assert response.action.target == "user_after_sale_list"
    assert response.action.payload.afterSaleStatus == "PENDING"
    assert response.action.priority == 100


def test_open_user_after_sale_list_request_rejects_invalid_status():
    with pytest.raises(ValidationError):
        OpenUserAfterSaleListRequest.model_validate({"afterSaleStatus": "INVALID"})


def test_send_product_card_enqueues_card_response(monkeypatch):
    async def _fake_render_product_card(_product_ids: list[int]) -> Card:
        return Card(
            type="product-card",
            data={
                "title": "为您推荐以下商品",
                "products": [
                    {
                        "id": "1001",
                        "name": "感冒灵颗粒",
                        "image": "https://example.com/1001.png",
                        "price": "29.90",
                    },
                    {
                        "id": "1002",
                        "name": "板蓝根颗粒",
                        "image": "https://example.com/1002.png",
                        "price": "19.80",
                    },
                ],
            },
        )

    monkeypatch.setattr(card_tools_module, "render_product_card", _fake_render_product_card)
    queue_token = set_final_response_queue()
    try:
        result = asyncio.run(send_product_card.ainvoke({"productIds": [1001, 1002]}))
        queued_responses = drain_final_sse_responses()
    finally:
        reset_final_response_queue(queue_token)

    assert result == "__SUCCESS__"
    assert len(queued_responses) == 1
    response = queued_responses[0]
    assert response.type == MessageType.CARD
    assert response.card is not None
    assert response.card.type == "product-card"
    assert response.card.data["title"] == "为您推荐以下商品"
    assert response.card.data["products"] == [
        {
            "id": "1001",
            "name": "感冒灵颗粒",
            "image": "https://example.com/1001.png",
            "price": "29.90",
        },
        {
            "id": "1002",
            "name": "板蓝根颗粒",
            "image": "https://example.com/1002.png",
            "price": "19.80",
        },
    ]
    assert response.content.model_dump(exclude_none=True) == {}
    assert response.meta is not None
    assert "card_uuid" in response.meta
    assert str(UUID(response.meta["card_uuid"])) == response.meta["card_uuid"]
    assert response.meta["persist_card"] is True


def test_send_product_card_skips_empty_card(monkeypatch):
    async def _fake_render_product_card(_product_ids: list[int]) -> None:
        return None

    monkeypatch.setattr(card_tools_module, "render_product_card", _fake_render_product_card)
    queue_token = set_final_response_queue()
    try:
        result = asyncio.run(send_product_card.ainvoke({"productIds": [1001, 1002]}))
        queued_responses = drain_final_sse_responses()
    finally:
        reset_final_response_queue(queue_token)

    assert result == "__ERROR__:未从业务端获取到商品信息无法发送商品卡片"
    assert queued_responses == []


def test_send_product_card_request_rejects_empty_product_ids():
    with pytest.raises(ValidationError):
        SendProductCardRequest.model_validate({"productIds": []})


def test_send_product_card_request_rejects_non_positive_product_id():
    with pytest.raises(ValidationError):
        SendProductCardRequest.model_validate({"productIds": [0]})


def test_send_product_purchase_card_enqueues_card_response(monkeypatch):
    async def _fake_render_product_purchase_card(_items: list[dict]) -> Card:
        return Card(
            type="product-purchase-card",
            data={
                "title": "请确认要购买的商品",
                "products": [
                    {
                        "id": "1001",
                        "name": "感冒灵颗粒",
                        "image": "https://example.com/1001.png",
                        "price": "29.90",
                        "quantity": 2,
                    },
                    {
                        "id": "1002",
                        "name": "板蓝根颗粒",
                        "image": "https://example.com/1002.png",
                        "price": "19.80",
                        "quantity": 1,
                    },
                ],
                "total_price": "79.60",
            },
        )

    monkeypatch.setattr(
        card_tools_module,
        "render_product_purchase_card",
        _fake_render_product_purchase_card,
    )
    queue_token = set_final_response_queue()
    try:
        result = asyncio.run(
            send_product_purchase_card.ainvoke(
                {
                    "items": [
                        {"productId": 1001, "quantity": 2},
                        {"productId": 1002, "quantity": 1},
                    ]
                }
            )
        )
        queued_responses = drain_final_sse_responses()
    finally:
        reset_final_response_queue(queue_token)

    assert result == "__SUCCESS__"
    assert len(queued_responses) == 1
    response = queued_responses[0]
    assert response.type == MessageType.CARD
    assert response.card is not None
    assert response.card.type == "product-purchase-card"
    assert response.card.data == {
        "title": "请确认要购买的商品",
        "products": [
            {
                "id": "1001",
                "name": "感冒灵颗粒",
                "image": "https://example.com/1001.png",
                "price": "29.90",
                "quantity": 2,
            },
            {
                "id": "1002",
                "name": "板蓝根颗粒",
                "image": "https://example.com/1002.png",
                "price": "19.80",
                "quantity": 1,
            },
        ],
        "total_price": "79.60",
    }
    assert response.content.model_dump(exclude_none=True) == {}
    assert response.meta is not None
    assert "card_uuid" in response.meta
    assert str(UUID(response.meta["card_uuid"])) == response.meta["card_uuid"]
    assert response.meta["persist_card"] is True


def test_send_product_purchase_card_skips_empty_card(monkeypatch):
    async def _fake_render_product_purchase_card(_items: list[dict]) -> None:
        return None

    monkeypatch.setattr(
        card_tools_module,
        "render_product_purchase_card",
        _fake_render_product_purchase_card,
    )
    queue_token = set_final_response_queue()
    try:
        result = asyncio.run(
            send_product_purchase_card.ainvoke(
                {"items": [{"productId": 1001, "quantity": 2}]}
            )
        )
        queued_responses = drain_final_sse_responses()
    finally:
        reset_final_response_queue(queue_token)

    assert result == "__ERROR__:未从业务端获取到商品购买信息无法发送商品购买卡片"
    assert queued_responses == []


def test_send_product_purchase_card_request_rejects_empty_items():
    with pytest.raises(ValidationError):
        SendProductPurchaseCardRequest.model_validate({"items": []})


def test_send_product_purchase_card_request_rejects_non_positive_product_id():
    with pytest.raises(ValidationError):
        SendProductPurchaseCardRequest.model_validate(
            {"items": [{"productId": 0, "quantity": 1}]}
        )


def test_send_product_purchase_card_request_rejects_non_positive_quantity():
    with pytest.raises(ValidationError):
        SendProductPurchaseCardRequest.model_validate(
            {"items": [{"productId": 1001, "quantity": 0}]}
        )


def test_send_product_purchase_card_request_schema_contains_field_descriptions():
    schema = SendProductPurchaseCardRequest.model_json_schema()

    assert schema["example"] == {
        "items": [
            {"productId": 101, "quantity": 2},
            {"productId": 205, "quantity": 1},
        ]
    }
    assert (
            schema["properties"]["items"]["description"]
            == "商品购买项列表，每个元素表示一个待确认购买的商品及其数量。"
    )
    item_schema = schema["$defs"][SendProductPurchaseCardItem.__name__]
    assert item_schema["properties"]["productId"]["description"] == "待购买商品的商品 ID，必填，且必须大于 0。"
    assert item_schema["properties"]["quantity"]["description"] == "该商品的购买数量，必填，且必须大于 0。"


def test_send_consent_card_enqueues_default_card_response():
    queue_token = set_final_response_queue()
    try:
        result = asyncio.run(
            send_consent_card.ainvoke(
                {
                    "title": "是否同意本次处理方案？",
                    "description": "同意后将继续下一步处理。",
                }
            )
        )
        queued_responses = drain_final_sse_responses()
    finally:
        reset_final_response_queue(queue_token)

    assert result == "__SUCCESS__"
    assert len(queued_responses) == 1
    response = queued_responses[0]
    assert response.type == MessageType.CARD
    assert response.card is not None
    assert response.card.type == "consent-card"
    assert response.card.data == {
        "title": "是否同意本次处理方案？",
        "description": "同意后将继续下一步处理。",
        "confirm": {
            "label": "同意",
            "value": "同意",
        },
        "reject": {
            "label": "拒绝",
            "value": "拒绝",
        },
    }
    assert response.meta is not None
    assert str(UUID(response.meta["card_uuid"])) == response.meta["card_uuid"]
    assert response.meta["persist_card"] is True


def test_send_consent_card_enqueues_custom_text_response():
    queue_token = set_final_response_queue()
    try:
        result = asyncio.run(
            send_consent_card.ainvoke(
                {
                    "title": "是否继续处理？",
                    "confirm_text": "继续处理",
                    "reject_text": "稍后再说",
                }
            )
        )
        queued_responses = drain_final_sse_responses()
    finally:
        reset_final_response_queue(queue_token)

    assert result == "__SUCCESS__"
    response = queued_responses[0]
    assert response.card is not None
    assert response.card.data["confirm"] == {
        "label": "继续处理",
        "value": "继续处理",
    }
    assert response.card.data["reject"] == {
        "label": "稍后再说",
        "value": "稍后再说",
    }


def test_send_consent_card_request_rejects_same_confirm_and_reject_text():
    with pytest.raises(ValidationError):
        SendConsentCardRequest.model_validate(
            {
                "title": "是否继续处理？",
                "confirm_text": "同意",
                "reject_text": " 同意 ",
            }
        )


def test_send_selection_card_enqueues_card_response():
    queue_token = set_final_response_queue()
    try:
        result = asyncio.run(
            send_selection_card.ainvoke(
                {
                    "title": "请选择您需要处理的问题",
                    "options": ["退款", "换货"],
                }
            )
        )
        queued_responses = drain_final_sse_responses()
    finally:
        reset_final_response_queue(queue_token)

    assert result == "__SUCCESS__"
    assert len(queued_responses) == 1
    response = queued_responses[0]
    assert response.type == MessageType.CARD
    assert response.card is not None
    assert response.card.type == "selection-card"
    assert response.card.data == {
        "title": "请选择您需要处理的问题",
        "options": ["退款", "换货"],
    }
    assert response.meta is not None
    assert str(UUID(response.meta["card_uuid"])) == response.meta["card_uuid"]
    assert response.meta["persist_card"] is True


def test_send_selection_card_request_rejects_removed_fields():
    with pytest.raises(ValidationError):
        SendSelectionCardRequest.model_validate(
            {
                "title": "请选择处理方式",
                "options": ["退款"],
                "extraField": "unexpected",
            }
        )


def test_send_selection_card_request_rejects_empty_options():
    with pytest.raises(ValidationError):
        SendSelectionCardRequest.model_validate(
            {
                "title": "请选择处理方式",
                "options": [],
            }
        )


def test_send_selection_card_request_rejects_blank_option():
    with pytest.raises(ValidationError):
        SendSelectionCardRequest.model_validate(
            {
                "title": "请选择处理方式",
                "options": ["退款", "   "],
            }
        )


def test_send_selection_card_request_rejects_duplicate_option_after_strip():
    with pytest.raises(ValidationError):
        SendSelectionCardRequest.model_validate(
            {
                "title": "请选择处理方式",
                "options": ["退款", " 退款 "],
            }
        )


def test_product_search_request_requires_query():
    with pytest.raises(ValidationError):
        ProductSearchRequest.model_validate({})


def test_after_sale_eligibility_request_rejects_blank_order_no():
    with pytest.raises(ValidationError):
        AfterSaleEligibilityRequest.model_validate({"order_no": "   "})


def test_get_order_detail_calls_client_order_detail_endpoint(monkeypatch):
    captured: dict = {}

    class _FakeHttpClient:
        async def __aenter__(self):
            return self

        async def __aexit__(self, exc_type, exc, tb):
            return None

        async def get(self, *, url, params=None):
            captured["url"] = url
            captured["params"] = params
            return {"ok": True}

    monkeypatch.setattr(order_tools_module, "HttpClient", _FakeHttpClient)
    monkeypatch.setattr(order_tools_module.HttpResponse, "parse_data", lambda response: response)

    result = asyncio.run(
        order_tools_module.get_order_detail.ainvoke({"order_no": "O202603160001"})
    )

    assert captured == {
        "url": "/agent/client/order/O202603160001",
        "params": None,
    }
    assert result == {"ok": True}


def test_search_products_calls_client_search_endpoint(monkeypatch):
    captured: dict = {}

    class _FakeHttpClient:
        async def __aenter__(self):
            return self

        async def __aexit__(self, exc_type, exc, tb):
            return None

        async def get(self, *, url, params=None):
            captured["url"] = url
            captured["params"] = params
            return {"rows": []}

    monkeypatch.setattr(product_tools_module, "HttpClient", _FakeHttpClient)
    monkeypatch.setattr(
        product_tools_module.HttpResponse,
        "parse_data",
        lambda response: response,
    )

    result = asyncio.run(
        product_tools_module.search_products.ainvoke({"keyword": "维生素"})
    )

    assert captured == {
        "url": "/agent/client/product/search",
        "params": {
            "keyword": "维生素",
            "categoryName": None,
            "usage": None,
            "pageNum": 1,
            "pageSize": 10,
        },
    }
    assert result == {"rows": []}


def test_check_after_sale_eligibility_calls_client_endpoint(monkeypatch):
    captured: dict = {}

    class _FakeHttpClient:
        async def __aenter__(self):
            return self

        async def __aexit__(self, exc_type, exc, tb):
            return None

        async def get(self, *, url, params=None):
            captured["url"] = url
            captured["params"] = params
            return {"eligible": True}

    monkeypatch.setattr(after_sale_tools_module, "HttpClient", _FakeHttpClient)
    monkeypatch.setattr(
        after_sale_tools_module.HttpResponse,
        "parse_data",
        lambda response: response,
    )

    result = asyncio.run(
        after_sale_tools_module.check_after_sale_eligibility.ainvoke(
            {"order_no": "O202603160001", "order_item_id": 12}
        )
    )

    assert captured == {
        "url": "/agent/client/after-sale/eligibility",
        "params": {
            "orderNo": "O202603160001",
            "orderItemId": 12,
        },
    }
    assert result == {"eligible": True}
