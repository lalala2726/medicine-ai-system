import importlib
import json

from fastapi.responses import StreamingResponse
from fastapi.testclient import TestClient
from redis.exceptions import RedisError

import app.main as main_module
from app.api.routes import client_assistant as assistant_module
from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.main import app
from app.schemas.assistant_run import AssistantRunStatus
from app.schemas.admin_assistant_history import ConversationMessageResponse
from app.schemas.auth import AuthUser
from app.schemas.document.conversation import ConversationListItem

rate_limit_module = importlib.import_module("app.core.security.rate_limit")


def _extract_payloads(response_text: str) -> list[dict]:
    lines = [line for line in response_text.splitlines() if line.startswith("data: ")]
    return [json.loads(line[len("data: "):]) for line in lines]


def _build_streaming_response(text: str) -> StreamingResponse:
    async def _stream():
        yield (
                "data: "
                + json.dumps(
            {
                "content": {"text": text},
                "type": "answer",
                "is_end": False,
                "timestamp": 1,
            },
            ensure_ascii=False,
        )
                + "\n\n"
        )
        yield (
                "data: "
                + json.dumps(
            {
                "content": {"text": ""},
                "type": "answer",
                "is_end": True,
                "timestamp": 2,
            },
            ensure_ascii=False,
        )
                + "\n\n"
        )

    return StreamingResponse(_stream(), media_type="text/event-stream")


def _build_audio_streaming_response() -> StreamingResponse:
    async def _stream():
        yield b"chunk-a"
        yield b"chunk-b"

    return StreamingResponse(
        _stream(),
        media_type="audio/mpeg",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
        },
    )


def _auth_headers() -> dict[str, str]:
    return {"Authorization": "Bearer test-token"}


def _mock_auth(monkeypatch) -> None:
    async def _fake_fetch_current_user() -> AuthUser:
        return AuthUser(
            id=1,
            username="tester",
            roles=[],
            permissions=[],
        )

    monkeypatch.setattr(
        main_module,
        "verify_authorization",
        _fake_fetch_current_user,
    )


def _mock_rate_limit_allow(monkeypatch) -> None:
    def _fake_evaluate_rate_limit(*, scope: str, subject_key: str, rules):
        return rate_limit_module.RateLimitCheckResult(
            allowed=True,
            retry_after_seconds=0,
            limit=10,
            remaining=9,
            reset_seconds=60,
        )

    monkeypatch.setattr(rate_limit_module, "_evaluate_rate_limit", _fake_evaluate_rate_limit)


def _mock_rate_limit_result(
        monkeypatch,
        *,
        allowed: bool,
        retry_after_seconds: int = 0,
        limit: int = 10,
        remaining: int = 9,
        reset_seconds: int = 60,
        exc: Exception | None = None,
) -> None:
    """
    模拟 rate limit 组件的返回结果或异常。

    Args:
        monkeypatch: pytest monkeypatch fixture。
        allowed: 是否允许本次请求。
        retry_after_seconds: 建议重试秒数。
        limit: 当前命中的限流阈值。
        remaining: 当前窗口剩余次数。
        reset_seconds: 当前窗口重置秒数。
        exc: 可选异常；传入后优先抛出异常。

    Returns:
        None
    """

    if exc is not None:
        def _raise_error(*, scope: str, subject_key: str, rules):
            raise exc

        monkeypatch.setattr(rate_limit_module, "_evaluate_rate_limit", _raise_error)
        return

    def _fake_evaluate_rate_limit(*, scope: str, subject_key: str, rules):
        return rate_limit_module.RateLimitCheckResult(
            allowed=allowed,
            retry_after_seconds=retry_after_seconds,
            limit=limit,
            remaining=remaining,
            reset_seconds=reset_seconds,
        )

    monkeypatch.setattr(rate_limit_module, "_evaluate_rate_limit", _fake_evaluate_rate_limit)


def test_client_assistant_submit_route_delegates_to_service(monkeypatch):
    captured: dict = {}
    _mock_auth(monkeypatch)
    _mock_rate_limit_allow(monkeypatch)

    monkeypatch.setattr(
        assistant_module,
        "assistant_chat",
        lambda *, question, conversation_uuid=None: (
            captured.update(
                {
                    "question": question,
                    "conversation_uuid": conversation_uuid,
                }
            ),
            {
                "conversation_uuid": "client-conv-1",
                "message_uuid": "msg-1",
                "run_status": AssistantRunStatus.RUNNING,
            },
        )[-1],
    )
    client = TestClient(app)

    response = client.post(
        "/client/assistant/chat/submit",
        headers=_auth_headers(),
        json={"question": "客户端问题", "conversation_uuid": "client-conv-1"},
    )

    assert response.status_code == 200
    assert response.json()["data"] == {
        "conversation_uuid": "client-conv-1",
        "message_uuid": "msg-1",
        "run_status": "running",
    }
    assert captured == {
        "question": "客户端问题",
        "conversation_uuid": "client-conv-1",
    }


def test_client_assistant_submit_rejects_blank_question(monkeypatch):
    _mock_auth(monkeypatch)
    _mock_rate_limit_allow(monkeypatch)
    client = TestClient(app)

    response = client.post(
        "/client/assistant/chat/submit",
        headers=_auth_headers(),
        json={"question": "   "},
    )

    assert response.status_code == 400


def test_client_assistant_submit_route_accepts_card_action_but_ignores_it(monkeypatch):
    captured: dict = {}
    _mock_auth(monkeypatch)
    _mock_rate_limit_allow(monkeypatch)

    monkeypatch.setattr(
        assistant_module,
        "assistant_chat",
        lambda *, question, conversation_uuid=None: (
            captured.update(
                {
                    "question": question,
                    "conversation_uuid": conversation_uuid,
                }
            ),
            {
                "conversation_uuid": "client-conv-1",
                "message_uuid": "msg-1",
                "run_status": AssistantRunStatus.RUNNING,
            },
        )[-1],
    )
    client = TestClient(app)

    response = client.post(
        "/client/assistant/chat/submit",
        headers=_auth_headers(),
        json={
            "question": "继续处理",
            "conversation_uuid": "client-conv-1",
            "card_action": {
                "type": "click",
                "message_id": "msg-1",
                "card_uuid": "card-1",
            },
        },
    )

    assert response.status_code == 200
    assert response.json()["data"] == {
        "conversation_uuid": "client-conv-1",
        "message_uuid": "msg-1",
        "run_status": "running",
    }
    assert captured == {
        "question": "继续处理",
        "conversation_uuid": "client-conv-1",
    }


def test_client_assistant_submit_accepts_card_action_without_conversation_uuid(monkeypatch):
    captured: dict = {}
    _mock_auth(monkeypatch)
    _mock_rate_limit_allow(monkeypatch)
    monkeypatch.setattr(
        assistant_module,
        "assistant_chat",
        lambda *, question, conversation_uuid=None: (
            captured.update(
                {
                    "question": question,
                    "conversation_uuid": conversation_uuid,
                }
            ),
            {
                "conversation_uuid": "client-conv-2",
                "message_uuid": "msg-2",
                "run_status": AssistantRunStatus.RUNNING,
            },
        )[-1],
    )
    client = TestClient(app)

    response = client.post(
        "/client/assistant/chat/submit",
        headers=_auth_headers(),
        json={
            "question": "继续处理",
            "card_action": {
                "type": "click",
                "message_id": "msg-1",
                "card_uuid": "card-1",
            },
        },
    )

    assert response.status_code == 200
    assert response.json()["data"] == {
        "conversation_uuid": "client-conv-2",
        "message_uuid": "msg-2",
        "run_status": "running",
    }
    assert captured == {
        "question": "继续处理",
        "conversation_uuid": None,
    }


def test_client_assistant_submit_rejects_blank_card_action_fields(monkeypatch):
    _mock_auth(monkeypatch)
    _mock_rate_limit_allow(monkeypatch)
    client = TestClient(app)

    response = client.post(
        "/client/assistant/chat/submit",
        headers=_auth_headers(),
        json={
            "question": "继续处理",
            "conversation_uuid": "client-conv-1",
            "card_action": {
                "type": "click",
                "message_id": "   ",
                "card_uuid": "card-1",
            },
        },
    )

    assert response.status_code == 400


def test_client_assistant_message_tts_stream_route_delegates_to_service(monkeypatch):
    captured: dict = {}
    _mock_auth(monkeypatch)
    _mock_rate_limit_allow(monkeypatch)

    def _fake_tts_stream(*, message_uuid: str):
        captured["message_uuid"] = message_uuid
        return _build_audio_streaming_response()

    monkeypatch.setattr(
        assistant_module,
        "assistant_message_tts_stream_service",
        _fake_tts_stream,
    )
    client = TestClient(app)

    response = client.post(
        "/client/assistant/message/tts/stream",
        headers=_auth_headers(),
        json={"message_uuid": "  msg-tts-1  "},
    )

    assert response.status_code == 200
    assert response.headers["content-type"].startswith("audio/mpeg")
    assert response.headers["cache-control"] == "no-cache"
    assert response.headers["x-accel-buffering"] == "no"
    assert response.content == b"chunk-achunk-b"
    assert captured == {"message_uuid": "msg-tts-1"}


def test_client_assistant_message_tts_stream_route_rejects_blank_message_uuid(monkeypatch):
    called = {"value": False}
    _mock_auth(monkeypatch)
    _mock_rate_limit_allow(monkeypatch)

    def _fake_tts_stream(*, message_uuid: str):
        called["value"] = True
        return _build_audio_streaming_response()

    monkeypatch.setattr(
        assistant_module,
        "assistant_message_tts_stream_service",
        _fake_tts_stream,
    )
    client = TestClient(app)

    response = client.post(
        "/client/assistant/message/tts/stream",
        headers=_auth_headers(),
        json={"message_uuid": "   "},
    )

    assert response.status_code == 400
    assert response.json()["message"] == "Validation Failed"
    assert called["value"] is False


def test_client_assistant_message_tts_stream_route_rejects_extra_fields(monkeypatch):
    called = {"value": False}
    _mock_auth(monkeypatch)
    _mock_rate_limit_allow(monkeypatch)

    def _fake_tts_stream(*, message_uuid: str):
        called["value"] = True
        return _build_audio_streaming_response()

    monkeypatch.setattr(
        assistant_module,
        "assistant_message_tts_stream_service",
        _fake_tts_stream,
    )
    client = TestClient(app)

    response = client.post(
        "/client/assistant/message/tts/stream",
        headers=_auth_headers(),
        json={
            "message_uuid": "msg-tts-1",
            "voice_type": "not-allowed",
        },
    )

    assert response.status_code == 400
    body = response.json()
    assert body["message"] == "Validation Failed"
    assert any(
        item["field"] == "voice_type" and item["type"] == "extra_forbidden"
        for item in body["errors"]
    )
    assert called["value"] is False


def test_client_assistant_message_tts_stream_route_requires_authentication(monkeypatch):
    async def _fake_verify_authorization() -> AuthUser:
        raise ServiceException(code=ResponseCode.UNAUTHORIZED, message="未认证")

    monkeypatch.setattr(main_module, "verify_authorization", _fake_verify_authorization)
    client = TestClient(app)

    response = client.post(
        "/client/assistant/message/tts/stream",
        json={"message_uuid": "msg-tts-1"},
    )

    assert response.status_code == ResponseCode.UNAUTHORIZED.code


def test_client_assistant_tts_route_returns_429_when_rate_limited(monkeypatch):
    _mock_auth(monkeypatch)
    _mock_rate_limit_result(
        monkeypatch,
        allowed=False,
        retry_after_seconds=9,
        limit=5,
        remaining=0,
        reset_seconds=9,
    )
    called = {"value": False}

    def _fake_tts_stream(*, message_uuid: str):
        called["value"] = True
        return _build_audio_streaming_response()

    monkeypatch.setattr(
        assistant_module,
        "assistant_message_tts_stream_service",
        _fake_tts_stream,
    )
    client = TestClient(app)

    response = client.post(
        "/client/assistant/message/tts/stream",
        headers=_auth_headers(),
        json={"message_uuid": "msg-tts-1"},
    )

    assert response.status_code == ResponseCode.TOO_MANY_REQUESTS.code
    assert response.headers["retry-after"] == "9"
    body = response.json()
    assert body["code"] == ResponseCode.TOO_MANY_REQUESTS.code
    assert body["message"] == "访问 /client/assistant/message/tts/stream 过于频繁，请在 9 秒后再试"
    assert called["value"] is False


def test_client_assistant_tts_route_returns_503_when_redis_unavailable(monkeypatch):
    _mock_auth(monkeypatch)
    _mock_rate_limit_result(monkeypatch, allowed=True, exc=RedisError("redis down"))
    called = {"value": False}

    def _fake_tts_stream(*, message_uuid: str):
        called["value"] = True
        return _build_audio_streaming_response()

    monkeypatch.setattr(
        assistant_module,
        "assistant_message_tts_stream_service",
        _fake_tts_stream,
    )
    client = TestClient(app)

    response = client.post(
        "/client/assistant/message/tts/stream",
        headers=_auth_headers(),
        json={"message_uuid": "msg-tts-1"},
    )

    assert response.status_code == ResponseCode.SERVICE_UNAVAILABLE.code
    assert response.headers["retry-after"] == "1"
    body = response.json()
    assert body["code"] == ResponseCode.SERVICE_UNAVAILABLE.code
    assert body["message"] == "限流服务不可用，请稍后再试"
    assert called["value"] is False


def test_client_voice_tts_rate_limit_rules_match_expected() -> None:
    assert [
               (rule.window_seconds, rule.limit)
               for rule in assistant_module.TTS_RATE_LIMIT_RULES
           ] == [
               (60, 5),
               (3600, 60),
               (18000, 100),
               (86400, 200),
           ]


def test_client_conversation_list_route_returns_page(monkeypatch):
    _mock_auth(monkeypatch)
    monkeypatch.setattr(
        assistant_module,
        "conversation_list_service",
        lambda *, page_request: (
            [ConversationListItem(conversation_uuid="client-conv-1", title="标题1")],
            1,
        ),
    )
    client = TestClient(app)

    response = client.get(
        "/client/assistant/conversation/list?page_num=1&page_size=20",
        headers=_auth_headers(),
    )

    assert response.status_code == 200
    body = response.json()
    assert body["data"]["rows"] == [
        {"conversation_uuid": "client-conv-1", "title": "标题1"}
    ]
    assert body["data"]["total"] == 1


def test_client_history_route_returns_serialized_messages(monkeypatch):
    _mock_auth(monkeypatch)
    monkeypatch.setattr(
        assistant_module,
        "conversation_messages_service",
        lambda *, conversation_uuid, page_request: (
            [
                ConversationMessageResponse(
                    id="msg-1",
                    role="user",
                    content="你好",
                ),
                ConversationMessageResponse(
                    id="msg-2",
                    role="ai",
                    content="",
                    status="success",
                    cards=[
                        {
                            "card_uuid": "card-1",
                            "type": "product-card",
                            "data": {
                                "title": "为您推荐以下商品",
                                "products": [{"id": "1001", "name": "商品1001"}],
                            },
                        },
                        {
                            "card_uuid": "card-2",
                            "type": "product-purchase-card",
                            "data": {
                                "title": "请确认要购买的商品",
                                "products": [
                                    {
                                        "id": "1002",
                                        "name": "商品1002",
                                        "price": "19.90",
                                        "quantity": 2,
                                    }
                                ],
                                "total_price": "39.80",
                            },
                        }
                    ],
                )
            ],
            2,
        ),
    )
    client = TestClient(app)

    response = client.get(
        "/client/assistant/history/client-conv-1?page_num=1&page_size=20",
        headers=_auth_headers(),
    )

    assert response.status_code == 200
    body = response.json()
    assert body["data"]["rows"] == [
        {"id": "msg-1", "role": "user", "content": "你好"},
        {
            "id": "msg-2",
            "role": "ai",
            "content": "",
            "status": "success",
            "cards": [
                {
                    "card_uuid": "card-1",
                    "type": "product-card",
                    "data": {
                        "title": "为您推荐以下商品",
                        "products": [{"id": "1001", "name": "商品1001"}],
                    },
                },
                {
                    "card_uuid": "card-2",
                    "type": "product-purchase-card",
                    "data": {
                        "title": "请确认要购买的商品",
                        "products": [
                            {
                                "id": "1002",
                                "name": "商品1002",
                                "price": "19.90",
                                "quantity": 2,
                            }
                        ],
                        "total_price": "39.80",
                    },
                }
            ],
        },
    ]
    assert body["data"]["total"] == 2


def test_delete_client_conversation_route_delegates_to_service(monkeypatch):
    captured: dict = {}
    _mock_auth(monkeypatch)

    def _fake_delete_conversation(*, conversation_uuid: str):
        captured["conversation_uuid"] = conversation_uuid

    monkeypatch.setattr(
        assistant_module,
        "delete_conversation_service",
        _fake_delete_conversation,
    )
    client = TestClient(app)

    response = client.delete(
        "/client/assistant/conversation/client-conv-1",
        headers=_auth_headers(),
    )

    assert response.status_code == 200
    body = response.json()
    assert body["code"] == 200
    assert body["message"] == "删除成功"
    assert body["data"] == {"conversation_uuid": "client-conv-1"}
    assert captured == {"conversation_uuid": "client-conv-1"}


def test_update_client_conversation_title_route_delegates_to_service(monkeypatch):
    captured: dict = {}
    _mock_auth(monkeypatch)

    def _fake_update_title(*, conversation_uuid: str, title: str) -> str:
        captured["conversation_uuid"] = conversation_uuid
        captured["title"] = title
        return "新标题"

    monkeypatch.setattr(
        assistant_module,
        "update_conversation_title_service",
        _fake_update_title,
    )
    client = TestClient(app)

    response = client.put(
        "/client/assistant/conversation/client-conv-1",
        headers=_auth_headers(),
        json={"title": "  新标题  "},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["code"] == 200
    assert body["message"] == "修改成功"
    assert body["data"] == {
        "conversation_uuid": "client-conv-1",
        "title": "新标题",
    }
    assert captured == {"conversation_uuid": "client-conv-1", "title": "  新标题  "}


def test_delete_client_conversation_requires_auth(monkeypatch):
    called = {"value": False}

    def _fake_delete_conversation(*, conversation_uuid: str):
        called["value"] = True

    monkeypatch.setattr(
        assistant_module,
        "delete_conversation_service",
        _fake_delete_conversation,
    )
    client = TestClient(app)

    response = client.delete("/client/assistant/conversation/client-conv-1")

    assert response.status_code == 401
    body = response.json()
    assert body["code"] == 401
    assert called["value"] is False


def test_update_client_conversation_title_requires_auth(monkeypatch):
    called = {"value": False}

    def _fake_update_title(*, conversation_uuid: str, title: str) -> str:
        called["value"] = True
        return "不会执行"

    monkeypatch.setattr(
        assistant_module,
        "update_conversation_title_service",
        _fake_update_title,
    )
    client = TestClient(app)

    response = client.put(
        "/client/assistant/conversation/client-conv-1",
        json={"title": "新标题"},
    )

    assert response.status_code == 401
    body = response.json()
    assert body["code"] == 401
    assert called["value"] is False
