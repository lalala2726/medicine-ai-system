import importlib
import json

import pytest
from fastapi.responses import StreamingResponse
from fastapi.testclient import TestClient
from redis.exceptions import RedisError
from starlette.websockets import WebSocketDisconnect

import app.main as main_module
from app.api.routes import admin_assistant as assistant_module
from app.api.routes import speech_stt as speech_stt_module
from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.core.security.auth_context import get_authorization_header
from app.core.security.role_codes import RoleCode
from app.main import app
from app.schemas.assistant_run import AssistantRunStatus, AssistantRunSubmitResponse
from app.schemas.admin_assistant_history import ConversationMessageResponse, ThoughtNodeResponse
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


def _build_notice_then_answer_streaming_response(
        *,
        notice_meta: dict[str, str],
        answer_text: str,
        notice_content: dict[str, str] | None = None,
) -> StreamingResponse:
    async def _stream():
        notice_payload = {
            "type": "notice",
            "is_end": False,
            "timestamp": 1,
            "meta": notice_meta,
        }
        if notice_content is not None:
            notice_payload["content"] = notice_content
        yield (
                "data: "
                + json.dumps(
            notice_payload,
            ensure_ascii=False,
        )
                + "\n\n"
        )
        yield (
                "data: "
                + json.dumps(
            {
                "content": {"text": answer_text},
                "type": "answer",
                "is_end": False,
                "timestamp": 2,
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
                "timestamp": 3,
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


def _mock_auth(
        monkeypatch,
        *,
        roles: list[str] | None = None,
        permissions: list[str] | None = None,
) -> None:
    resolved_roles = [RoleCode.SUPER_ADMIN.value] if roles is None else roles
    resolved_permissions = [] if permissions is None else permissions

    async def _fake_fetch_current_user() -> AuthUser:
        return AuthUser(
            id=1,
            username="tester",
            roles=resolved_roles,
            permissions=resolved_permissions,
        )

    monkeypatch.setattr(
        main_module,
        "verify_authorization",
        _fake_fetch_current_user,
    )


def _mock_ws_auth(
        monkeypatch,
        *,
        roles: list[str] | None = None,
        permissions: list[str] | None = None,
) -> None:
    resolved_roles = [RoleCode.SUPER_ADMIN.value] if roles is None else roles
    resolved_permissions = [] if permissions is None else permissions

    async def _fake_fetch_current_user() -> AuthUser:
        return AuthUser(
            id=1,
            username="tester",
            roles=resolved_roles,
            permissions=resolved_permissions,
        )

    monkeypatch.setattr(
        speech_stt_module,
        "verify_authorization",
        _fake_fetch_current_user,
    )


def _mock_verify_authorization_counter(
        monkeypatch,
        *,
        raise_on_call: bool = False,
) -> dict[str, int]:
    called = {"count": 0}

    async def _fake_verify_authorization() -> AuthUser:
        called["count"] += 1
        if raise_on_call:
            raise AssertionError("verify_authorization should not be called")
        return AuthUser(
            id=1,
            username="tester",
            roles=[RoleCode.SUPER_ADMIN.value],
            permissions=[],
        )

    monkeypatch.setattr(
        main_module,
        "verify_authorization",
        _fake_verify_authorization,
    )
    return called


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


@pytest.fixture(autouse=True)
def _default_rate_limit_allow(monkeypatch):
    _mock_rate_limit_result(monkeypatch, allowed=True)


def test_assistant_route_delegates_to_service(monkeypatch):
    captured: dict = {}
    _mock_auth(monkeypatch)

    def _fake_assistant_chat_submit(*, question: str, conversation_uuid: str | None = None):
        captured["question"] = question
        captured["conversation_uuid"] = conversation_uuid
        return AssistantRunSubmitResponse(
            conversation_uuid="conv-1",
            message_uuid="msg-1",
            run_status=AssistantRunStatus.RUNNING,
        )

    monkeypatch.setattr(assistant_module, "assistant_chat_submit", _fake_assistant_chat_submit)
    client = TestClient(app)

    response = client.post(
        "/admin/assistant/chat/submit",
        headers=_auth_headers(),
        json={"question": "代理测试", "conversation_uuid": "conv-1"},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["data"] == {
        "conversation_uuid": "conv-1",
        "message_uuid": "msg-1",
        "run_status": "running",
    }
    assert captured == {
        "question": "代理测试",
        "conversation_uuid": "conv-1",
    }


def test_assistant_request_defaults_conversation_uuid_to_none(monkeypatch):
    captured: dict = {}
    _mock_auth(monkeypatch)

    def _fake_assistant_chat_submit(*, question: str, conversation_uuid: str | None = None):
        captured["question"] = question
        captured["conversation_uuid"] = conversation_uuid
        return AssistantRunSubmitResponse(
            conversation_uuid="conv-new",
            message_uuid="msg-new",
            run_status=AssistantRunStatus.RUNNING,
        )

    monkeypatch.setattr(assistant_module, "assistant_chat_submit", _fake_assistant_chat_submit)
    client = TestClient(app)

    response = client.post(
        "/admin/assistant/chat/submit",
        headers=_auth_headers(),
        json={"question": "hi"},
    )

    assert response.status_code == 200
    assert captured["question"] == "hi"
    assert captured["conversation_uuid"] is None


def test_assistant_submit_route_returns_new_conversation_run_meta(monkeypatch):
    captured: dict = {}
    _mock_auth(monkeypatch)

    def _fake_assistant_chat_submit(*, question: str, conversation_uuid: str | None = None):
        captured["question"] = question
        captured["conversation_uuid"] = conversation_uuid
        return AssistantRunSubmitResponse(
            conversation_uuid="conv-new-1",
            message_uuid="msg-new-1",
            run_status=AssistantRunStatus.RUNNING,
        )

    monkeypatch.setattr(assistant_module, "assistant_chat_submit", _fake_assistant_chat_submit)
    client = TestClient(app)

    response = client.post(
        "/admin/assistant/chat/submit",
        headers=_auth_headers(),
        json={"question": "新会话"},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["data"] == {
        "conversation_uuid": "conv-new-1",
        "message_uuid": "msg-new-1",
        "run_status": "running",
    }
    assert captured["question"] == "新会话"
    assert captured["conversation_uuid"] is None


def test_assistant_submit_route_returns_existing_conversation_run_meta(monkeypatch):
    captured: dict = {}
    _mock_auth(monkeypatch)

    def _fake_assistant_chat_submit(*, question: str, conversation_uuid: str | None = None):
        captured["question"] = question
        captured["conversation_uuid"] = conversation_uuid
        return AssistantRunSubmitResponse(
            conversation_uuid="conv-1",
            message_uuid="msg-old-1",
            run_status=AssistantRunStatus.RUNNING,
        )

    monkeypatch.setattr(assistant_module, "assistant_chat_submit", _fake_assistant_chat_submit)
    client = TestClient(app)

    response = client.post(
        "/admin/assistant/chat/submit",
        headers=_auth_headers(),
        json={"question": "旧会话", "conversation_uuid": "conv-1"},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["data"] == {
        "conversation_uuid": "conv-1",
        "message_uuid": "msg-old-1",
        "run_status": "running",
    }
    assert captured == {
        "question": "旧会话",
        "conversation_uuid": "conv-1",
    }


def test_assistant_request_normalizes_question_and_conversation_uuid(monkeypatch):
    captured: dict = {}
    _mock_auth(monkeypatch)

    def _fake_assistant_chat_submit(*, question: str, conversation_uuid: str | None = None):
        captured["question"] = question
        captured["conversation_uuid"] = conversation_uuid
        return AssistantRunSubmitResponse(
            conversation_uuid="conv-1",
            message_uuid="msg-1",
            run_status=AssistantRunStatus.RUNNING,
        )

    monkeypatch.setattr(assistant_module, "assistant_chat_submit", _fake_assistant_chat_submit)
    client = TestClient(app)

    response = client.post(
        "/admin/assistant/chat/submit",
        headers=_auth_headers(),
        json={"question": "  请帮我查订单  ", "conversation_uuid": "  conv-1  "},
    )

    assert response.status_code == 200
    assert captured["question"] == "请帮我查订单"
    assert captured["conversation_uuid"] == "conv-1"


def test_assistant_request_rejects_blank_question_after_trim(monkeypatch):
    called = {"value": False}
    _mock_auth(monkeypatch)

    def _fake_assistant_chat_submit(*, question: str, conversation_uuid: str | None = None):
        called["value"] = True
        return AssistantRunSubmitResponse(
            conversation_uuid="conv-1",
            message_uuid="msg-1",
            run_status=AssistantRunStatus.RUNNING,
        )

    monkeypatch.setattr(assistant_module, "assistant_chat_submit", _fake_assistant_chat_submit)
    client = TestClient(app)

    response = client.post(
        "/admin/assistant/chat/submit",
        headers=_auth_headers(),
        json={"question": "   ", "conversation_uuid": "conv-1"},
    )

    assert response.status_code == 400
    body = response.json()
    assert body["code"] == 400
    assert body["message"] == "Validation Failed"
    assert called["value"] is False


def test_assistant_request_rejects_legacy_conversion_uuid(monkeypatch):
    called = {"value": False}
    _mock_auth(monkeypatch)

    def _fake_assistant_chat_submit(*, question: str, conversation_uuid: str | None = None):
        called["value"] = True
        return AssistantRunSubmitResponse(
            conversation_uuid="conv-1",
            message_uuid="msg-1",
            run_status=AssistantRunStatus.RUNNING,
        )

    monkeypatch.setattr(assistant_module, "assistant_chat_submit", _fake_assistant_chat_submit)
    client = TestClient(app)

    response = client.post(
        "/admin/assistant/chat/submit",
        headers=_auth_headers(),
        json={"question": "hi", "conversion_uuid": "legacy"},
    )

    assert response.status_code == 400
    body = response.json()
    assert body["code"] == 400
    assert body["message"] == "Validation Failed"
    assert any(
        item["field"] == "conversion_uuid" and item["type"] == "extra_forbidden"
        for item in body["errors"]
    )
    assert called["value"] is False


def test_conversation_list_route_delegates_to_service(monkeypatch):
    captured: dict = {}
    _mock_auth(monkeypatch)

    def _fake_chat_list(*, page_request):
        captured["page_num"] = page_request.page_num
        captured["page_size"] = page_request.page_size
        return ([ConversationListItem(conversation_uuid="conv-1", title="会话1")], 3)

    monkeypatch.setattr(
        assistant_module,
        "conversation_list_service",
        _fake_chat_list,
    )
    client = TestClient(app)

    response = client.get(
        "/admin/assistant/conversation/list",
        headers=_auth_headers(),
        params={"page_num": 2, "page_size": 5},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["code"] == 200
    assert body["data"]["rows"] == [{"conversation_uuid": "conv-1", "title": "会话1"}]
    assert body["data"]["total"] == 3
    assert body["data"]["page_num"] == 2
    assert body["data"]["page_size"] == 5
    assert captured == {"page_num": 2, "page_size": 5}


def test_conversation_list_route_uses_default_pagination(monkeypatch):
    captured: dict = {}
    _mock_auth(monkeypatch)

    def _fake_chat_list(*, page_request):
        captured["page_num"] = page_request.page_num
        captured["page_size"] = page_request.page_size
        return ([], 0)

    monkeypatch.setattr(
        assistant_module,
        "conversation_list_service",
        _fake_chat_list,
    )
    client = TestClient(app)

    response = client.get(
        "/admin/assistant/conversation/list",
        headers=_auth_headers(),
    )

    assert response.status_code == 200
    body = response.json()
    assert body["code"] == 200
    assert body["data"]["rows"] == []
    assert body["data"]["total"] == 0
    assert body["data"]["page_num"] == 1
    assert body["data"]["page_size"] == 20
    assert captured == {"page_num": 1, "page_size": 20}


def test_conversation_messages_route_delegates_to_service(monkeypatch):
    captured: dict = {}
    _mock_auth(monkeypatch)

    def _fake_conversation_messages(*, conversation_uuid: str, page_request):
        captured["conversation_uuid"] = conversation_uuid
        captured["page_num"] = page_request.page_num
        captured["page_size"] = page_request.page_size
        return (
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
                    thinking="这是完整思考文本",
                    status="success",
                    thought_chain=[
                        ThoughtNodeResponse(
                            id="node-1",
                            node="planner",
                            message="planner",
                            status="success",
                            children=[],
                        )
                    ],
                ),
            ],
            2,
        )

    monkeypatch.setattr(
        assistant_module,
        "conversation_messages_service",
        _fake_conversation_messages,
    )
    client = TestClient(app)

    response = client.get(
        "/admin/assistant/history/conv-1",
        headers=_auth_headers(),
        params={"page_num": 1, "page_size": 50},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["code"] == 200
    assert body["data"]["total"] == 2
    assert body["data"]["page_num"] == 1
    assert body["data"]["page_size"] == 50
    assert body["data"]["rows"][0] == {"id": "msg-1", "role": "user", "content": "你好"}
    assert body["data"]["rows"][1]["id"] == "msg-2"
    assert body["data"]["rows"][1]["role"] == "ai"
    assert body["data"]["rows"][1]["status"] == "success"
    assert body["data"]["rows"][1]["content"] == ""
    assert body["data"]["rows"][1]["thinking"] == "这是完整思考文本"
    assert "cards" not in body["data"]["rows"][1]
    assert body["data"]["rows"][1]["thoughtChain"][0]["node"] == "planner"
    assert "thinking" not in body["data"]["rows"][0]
    assert captured == {"conversation_uuid": "conv-1", "page_num": 1, "page_size": 50}


def test_conversation_messages_route_uses_default_pagination(monkeypatch):
    captured: dict = {}
    _mock_auth(monkeypatch)

    def _fake_conversation_messages(*, conversation_uuid: str, page_request):
        captured["conversation_uuid"] = conversation_uuid
        captured["page_num"] = page_request.page_num
        captured["page_size"] = page_request.page_size
        return ([], 0)

    monkeypatch.setattr(
        assistant_module,
        "conversation_messages_service",
        _fake_conversation_messages,
    )
    client = TestClient(app)

    response = client.get(
        "/admin/assistant/history/conv-1",
        headers=_auth_headers(),
    )

    assert response.status_code == 200
    body = response.json()
    assert body["code"] == 200
    assert body["data"] == {
        "rows": [],
        "total": 0,
        "page_num": 1,
        "page_size": 50,
    }
    assert captured == {"conversation_uuid": "conv-1", "page_num": 1, "page_size": 50}


def test_conversation_messages_route_rejects_page_size_above_50(monkeypatch):
    called = {"value": False}
    _mock_auth(monkeypatch)

    def _fake_conversation_messages(*, conversation_uuid: str, page_request):
        called["value"] = True
        return ([], 0)

    monkeypatch.setattr(
        assistant_module,
        "conversation_messages_service",
        _fake_conversation_messages,
    )
    client = TestClient(app)

    response = client.get(
        "/admin/assistant/history/conv-1",
        headers=_auth_headers(),
        params={"page_size": 51},
    )

    assert response.status_code == 400
    body = response.json()
    assert body["code"] == 400
    assert body["message"] == "Validation Failed"
    assert called["value"] is False


def test_conversation_messages_route_forbidden_without_role_or_permission(monkeypatch):
    called = {"value": False}
    _mock_auth(monkeypatch, roles=[], permissions=[])

    def _fake_conversation_messages(*, conversation_uuid: str, page_request):
        called["value"] = True
        return ([], 0)

    monkeypatch.setattr(
        assistant_module,
        "conversation_messages_service",
        _fake_conversation_messages,
    )
    client = TestClient(app)

    response = client.get(
        "/admin/assistant/history/conv-1",
        headers=_auth_headers(),
    )

    assert response.status_code == 403
    body = response.json()
    assert body["code"] == 403
    assert body["message"] == "无权限访问此接口"
    assert called["value"] is False


def test_conversation_messages_route_returns_404_when_conversation_missing(monkeypatch):
    _mock_auth(monkeypatch)

    def _fake_conversation_messages(*, conversation_uuid: str, page_request):
        raise ServiceException(code=ResponseCode.NOT_FOUND, message="会话不存在")

    monkeypatch.setattr(
        assistant_module,
        "conversation_messages_service",
        _fake_conversation_messages,
    )
    client = TestClient(app)

    response = client.get(
        "/admin/assistant/history/missing",
        headers=_auth_headers(),
    )

    assert response.status_code == 404
    body = response.json()
    assert body["code"] == 404
    assert body["message"] == "会话不存在"


def test_assistant_route_allows_permission_without_admin_role(monkeypatch):
    captured: dict = {}
    _mock_auth(
        monkeypatch,
        roles=[],
        permissions=["system:smart_assistant"],
    )

    def _fake_assistant_chat_submit(*, question: str, conversation_uuid: str | None = None):
        captured["question"] = question
        captured["conversation_uuid"] = conversation_uuid
        return AssistantRunSubmitResponse(
            conversation_uuid="conv-1",
            message_uuid="msg-1",
            run_status=AssistantRunStatus.RUNNING,
        )

    monkeypatch.setattr(assistant_module, "assistant_chat_submit", _fake_assistant_chat_submit)
    client = TestClient(app)

    response = client.post(
        "/admin/assistant/chat/submit",
        headers=_auth_headers(),
        json={"question": "权限测试"},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["data"]["message_uuid"] == "msg-1"
    assert captured["question"] == "权限测试"


def test_assistant_route_forbidden_without_role_or_permission(monkeypatch):
    called = {"value": False}
    _mock_auth(monkeypatch, roles=[], permissions=[])

    def _fake_assistant_chat_submit(*, question: str, conversation_uuid: str | None = None):
        called["value"] = True
        return AssistantRunSubmitResponse(
            conversation_uuid="conv-1",
            message_uuid="msg-1",
            run_status=AssistantRunStatus.RUNNING,
        )

    monkeypatch.setattr(assistant_module, "assistant_chat_submit", _fake_assistant_chat_submit)
    client = TestClient(app)

    response = client.post(
        "/admin/assistant/chat/submit",
        headers=_auth_headers(),
        json={"question": "forbidden"},
    )

    assert response.status_code == 403
    body = response.json()
    assert body["code"] == 403
    assert body["message"] == "无权限访问此接口"
    assert called["value"] is False


def test_conversation_list_forbidden_without_role_or_permission(monkeypatch):
    called = {"value": False}
    _mock_auth(monkeypatch, roles=[], permissions=[])

    def _fake_chat_list(*, page_request):
        called["value"] = True
        return ([], 0)

    monkeypatch.setattr(
        assistant_module,
        "conversation_list_service",
        _fake_chat_list,
    )
    client = TestClient(app)

    response = client.get(
        "/admin/assistant/conversation/list",
        headers=_auth_headers(),
    )

    assert response.status_code == 403
    body = response.json()
    assert body["code"] == 403
    assert body["message"] == "无权限访问此接口"
    assert called["value"] is False


def test_delete_conversation_route_delegates_to_service(monkeypatch):
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
        "/admin/assistant/conversation/conv-1",
        headers=_auth_headers(),
    )

    assert response.status_code == 200
    body = response.json()
    assert body["code"] == 200
    assert body["message"] == "删除成功"
    assert body["data"] == {"conversation_uuid": "conv-1"}
    assert captured == {"conversation_uuid": "conv-1"}


def test_update_conversation_title_route_delegates_to_service(monkeypatch):
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
        "/admin/assistant/conversation/conv-1",
        headers=_auth_headers(),
        json={"title": "  新标题  "},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["code"] == 200
    assert body["message"] == "修改成功"
    assert body["data"] == {"conversation_uuid": "conv-1", "title": "新标题"}
    assert captured == {"conversation_uuid": "conv-1", "title": "  新标题  "}


def test_delete_conversation_forbidden_without_role_or_permission(monkeypatch):
    called = {"value": False}
    _mock_auth(monkeypatch, roles=[], permissions=[])

    def _fake_delete_conversation(*, conversation_uuid: str):
        called["value"] = True

    monkeypatch.setattr(
        assistant_module,
        "delete_conversation_service",
        _fake_delete_conversation,
    )
    client = TestClient(app)

    response = client.delete(
        "/admin/assistant/conversation/conv-1",
        headers=_auth_headers(),
    )

    assert response.status_code == 403
    body = response.json()
    assert body["code"] == 403
    assert body["message"] == "无权限访问此接口"
    assert called["value"] is False


def test_update_conversation_title_forbidden_without_role_or_permission(monkeypatch):
    called = {"value": False}
    _mock_auth(monkeypatch, roles=[], permissions=[])

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
        "/admin/assistant/conversation/conv-1",
        headers=_auth_headers(),
        json={"title": "新标题"},
    )

    assert response.status_code == 403
    body = response.json()
    assert body["code"] == 403
    assert body["message"] == "无权限访问此接口"
    assert called["value"] is False


def test_assistant_message_tts_stream_route_delegates_to_service(monkeypatch):
    captured: dict = {}
    _mock_auth(monkeypatch)

    def _fake_tts_stream(
            *,
            message_uuid: str,
    ):
        captured["message_uuid"] = message_uuid
        return _build_audio_streaming_response()

    monkeypatch.setattr(
        assistant_module,
        "assistant_message_tts_stream_service",
        _fake_tts_stream,
    )
    client = TestClient(app)

    response = client.post(
        "/admin/assistant/message/tts/stream",
        headers=_auth_headers(),
        json={
            "message_uuid": "  msg-1  ",
        },
    )

    assert response.status_code == 200
    assert response.headers["content-type"].startswith("audio/mpeg")
    assert response.headers["cache-control"] == "no-cache"
    assert response.headers["x-accel-buffering"] == "no"
    assert response.content == b"chunk-achunk-b"
    assert captured == {
        "message_uuid": "msg-1",
    }


def test_assistant_message_tts_stream_route_rejects_blank_message_uuid(monkeypatch):
    called = {"value": False}
    _mock_auth(monkeypatch)

    def _fake_tts_stream(
            *,
            message_uuid: str,
    ):
        called["value"] = True
        return _build_audio_streaming_response()

    monkeypatch.setattr(
        assistant_module,
        "assistant_message_tts_stream_service",
        _fake_tts_stream,
    )
    client = TestClient(app)

    response = client.post(
        "/admin/assistant/message/tts/stream",
        headers=_auth_headers(),
        json={
            "message_uuid": "   ",
        },
    )

    assert response.status_code == 400
    body = response.json()
    assert body["code"] == 400
    assert body["message"] == "Validation Failed"
    assert called["value"] is False


def test_assistant_message_tts_stream_route_forbidden_without_permission(monkeypatch):
    called = {"value": False}
    _mock_auth(monkeypatch, roles=[], permissions=[])

    def _fake_tts_stream(
            *,
            message_uuid: str,
    ):
        called["value"] = True
        return _build_audio_streaming_response()

    monkeypatch.setattr(
        assistant_module,
        "assistant_message_tts_stream_service",
        _fake_tts_stream,
    )
    client = TestClient(app)

    response = client.post(
        "/admin/assistant/message/tts/stream",
        headers=_auth_headers(),
        json={"message_uuid": "msg-1"},
    )

    assert response.status_code == 403
    body = response.json()
    assert body["code"] == 403
    assert body["message"] == "无权限访问此接口"
    assert called["value"] is False


def test_assistant_message_tts_stream_route_rejects_extra_fields(monkeypatch):
    called = {"value": False}
    _mock_auth(monkeypatch)

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
        "/admin/assistant/message/tts/stream",
        headers=_auth_headers(),
        json={
            "message_uuid": "msg-1",
            "voice_type": "not-allowed",
        },
    )

    assert response.status_code == 400
    body = response.json()
    assert body["code"] == 400
    assert body["message"] == "Validation Failed"
    assert any(
        item["field"] == "voice_type" and item["type"] == "extra_forbidden"
        for item in body["errors"]
    )
    assert called["value"] is False


def test_non_anonymous_route_still_requires_authentication(monkeypatch):
    async def _fake_verify_authorization() -> AuthUser:
        raise ServiceException(code=ResponseCode.UNAUTHORIZED, message="未认证")

    monkeypatch.setattr(main_module, "verify_authorization", _fake_verify_authorization)
    client = TestClient(app)

    response = client.post(
        "/admin/assistant/chat/submit",
        json={"question": "未认证访问"},
    )

    assert response.status_code == ResponseCode.UNAUTHORIZED.code


def test_assistant_chat_route_sets_rate_limit_headers_on_success(monkeypatch):
    _mock_auth(monkeypatch)
    _mock_rate_limit_result(
        monkeypatch,
        allowed=True,
        retry_after_seconds=0,
        limit=10,
        remaining=8,
        reset_seconds=42,
    )

    def _fake_assistant_chat_submit(*, question: str, conversation_uuid: str | None = None):
        return AssistantRunSubmitResponse(
            conversation_uuid="conv-1",
            message_uuid="msg-1",
            run_status=AssistantRunStatus.RUNNING,
        )

    monkeypatch.setattr(assistant_module, "assistant_chat_submit", _fake_assistant_chat_submit)
    client = TestClient(app)

    response = client.post(
        "/admin/assistant/chat/submit",
        headers=_auth_headers(),
        json={"question": "头信息测试"},
    )

    assert response.status_code == 200
    assert "x-ratelimit-limit" not in response.headers
    assert "x-ratelimit-remaining" not in response.headers
    assert "x-ratelimit-reset" not in response.headers
    assert "retry-after" not in response.headers


def test_assistant_chat_route_returns_429_when_rate_limited(monkeypatch):
    _mock_auth(monkeypatch)
    _mock_rate_limit_result(
        monkeypatch,
        allowed=False,
        retry_after_seconds=17,
        limit=10,
        remaining=0,
        reset_seconds=17,
    )
    called = {"value": False}

    def _fake_assistant_chat_submit(*, question: str, conversation_uuid: str | None = None):
        called["value"] = True
        return AssistantRunSubmitResponse(
            conversation_uuid="conv-1",
            message_uuid="msg-1",
            run_status=AssistantRunStatus.RUNNING,
        )

    monkeypatch.setattr(assistant_module, "assistant_chat_submit", _fake_assistant_chat_submit)
    client = TestClient(app)

    response = client.post(
        "/admin/assistant/chat/submit",
        headers=_auth_headers(),
        json={"question": "触发限流"},
    )

    assert response.status_code == ResponseCode.TOO_MANY_REQUESTS.code
    assert response.headers["retry-after"] == "17"
    assert response.headers["x-ratelimit-limit"] == "10"
    assert response.headers["x-ratelimit-remaining"] == "0"
    body = response.json()
    assert body["code"] == ResponseCode.TOO_MANY_REQUESTS.code
    assert body["message"] == "访问 /admin/assistant/chat/submit 过于频繁，请在 17 秒后再试"
    assert called["value"] is False
    assert "data" not in body


def test_assistant_tts_route_returns_429_when_rate_limited(monkeypatch):
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
        "/admin/assistant/message/tts/stream",
        headers=_auth_headers(),
        json={"message_uuid": "msg-1"},
    )

    assert response.status_code == ResponseCode.TOO_MANY_REQUESTS.code
    assert response.headers["retry-after"] == "9"
    body = response.json()
    assert body["code"] == ResponseCode.TOO_MANY_REQUESTS.code
    assert body["message"] == "访问 /admin/assistant/message/tts/stream 过于频繁，请在 9 秒后再试"
    assert called["value"] is False
    assert "data" not in body


def test_assistant_chat_route_returns_503_when_redis_unavailable(monkeypatch):
    _mock_auth(monkeypatch)
    _mock_rate_limit_result(monkeypatch, allowed=True, exc=RedisError("redis down"))
    called = {"value": False}

    def _fake_assistant_chat_submit(*, question: str, conversation_uuid: str | None = None):
        called["value"] = True
        return AssistantRunSubmitResponse(
            conversation_uuid="conv-1",
            message_uuid="msg-1",
            run_status=AssistantRunStatus.RUNNING,
        )

    monkeypatch.setattr(assistant_module, "assistant_chat_submit", _fake_assistant_chat_submit)
    client = TestClient(app)

    response = client.post(
        "/admin/assistant/chat/submit",
        headers=_auth_headers(),
        json={"question": "redis 故障"},
    )

    assert response.status_code == ResponseCode.SERVICE_UNAVAILABLE.code
    assert response.headers["retry-after"] == "1"
    body = response.json()
    assert body["code"] == ResponseCode.SERVICE_UNAVAILABLE.code
    assert body["message"] == "限流服务不可用，请稍后再试"
    assert called["value"] is False
    assert "data" not in body


def test_assistant_tts_route_returns_503_when_redis_unavailable(monkeypatch):
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
        "/admin/assistant/message/tts/stream",
        headers=_auth_headers(),
        json={"message_uuid": "msg-1"},
    )

    assert response.status_code == ResponseCode.SERVICE_UNAVAILABLE.code
    assert response.headers["retry-after"] == "1"
    body = response.json()
    assert body["code"] == ResponseCode.SERVICE_UNAVAILABLE.code
    assert body["message"] == "限流服务不可用，请稍后再试"
    assert called["value"] is False
    assert "data" not in body


def test_voice_tts_rate_limit_rules_match_expected() -> None:
    assert [
               (rule.window_seconds, rule.limit)
               for rule in assistant_module.TTS_RATE_LIMIT_RULES
           ] == [
               (60, 5),
               (3600, 60),
               (18000, 100),
               (86400, 200),
           ]


def test_speech_stt_rate_limit_rules_match_expected() -> None:
    assert [
               (rule.window_seconds, rule.limit)
               for rule in speech_stt_module.STT_RATE_LIMIT_RULES
           ] == [
               (60, 5),
               (3600, 60),
               (18000, 100),
               (86400, 200),
           ]


def test_assistant_stt_websocket_rejects_without_authorization(monkeypatch):
    called = {"value": False}

    async def _fake_verify_authorization() -> AuthUser:
        called["value"] = True
        raise ServiceException(code=ResponseCode.UNAUTHORIZED, message="未认证")

    monkeypatch.setattr(speech_stt_module, "verify_authorization", _fake_verify_authorization)

    async def _fake_stt_service(*, websocket, user, session_duration_seconds):  # noqa: ARG001
        raise AssertionError("service should not be called")

    monkeypatch.setattr(
        speech_stt_module,
        "speech_stt_stream_service",
        _fake_stt_service,
    )

    client = TestClient(app)

    with pytest.raises(WebSocketDisconnect) as exc_info:
        with client.websocket_connect("/ws/speech/stt/stream"):
            pass

    assert exc_info.value.code == 1008
    assert called["value"] is True


def test_assistant_stt_websocket_rejects_when_rate_limited(monkeypatch):
    _mock_ws_auth(monkeypatch)
    _mock_rate_limit_result(
        monkeypatch,
        allowed=False,
        retry_after_seconds=15,
        limit=5,
        remaining=0,
        reset_seconds=15,
    )
    called = {"value": False}

    async def _fake_stt_service(*, websocket, user, session_duration_seconds):  # noqa: ARG001
        called["value"] = True
        raise AssertionError("service should not be called")

    monkeypatch.setattr(
        speech_stt_module,
        "speech_stt_stream_service",
        _fake_stt_service,
    )

    client = TestClient(app)
    with pytest.raises(WebSocketDisconnect) as exc_info:
        with client.websocket_connect(
                "/ws/speech/stt/stream?access_token=test-token",
        ):
            pass

    assert exc_info.value.code == 1013
    assert called["value"] is False


def test_assistant_stt_websocket_rejects_when_rate_limit_backend_unavailable(monkeypatch):
    _mock_ws_auth(monkeypatch)
    _mock_rate_limit_result(monkeypatch, allowed=True, exc=RedisError("redis down"))
    called = {"value": False}

    async def _fake_stt_service(*, websocket, user, session_duration_seconds):  # noqa: ARG001
        called["value"] = True
        raise AssertionError("service should not be called")

    monkeypatch.setattr(
        speech_stt_module,
        "speech_stt_stream_service",
        _fake_stt_service,
    )

    client = TestClient(app)
    with pytest.raises(WebSocketDisconnect) as exc_info:
        with client.websocket_connect(
                "/ws/speech/stt/stream?access_token=test-token",
        ):
            pass

    assert exc_info.value.code == 1011
    assert called["value"] is False


def test_assistant_stt_websocket_rejects_without_permission(monkeypatch):
    _mock_ws_auth(monkeypatch, roles=[], permissions=[])
    called = {"value": False}

    async def _fake_stt_service(*, websocket, user, session_duration_seconds):  # noqa: ARG001
        called["value"] = True
        raise AssertionError("service should not be called")

    monkeypatch.setattr(
        speech_stt_module,
        "speech_stt_stream_service",
        _fake_stt_service,
    )

    client = TestClient(app)
    with pytest.raises(WebSocketDisconnect) as exc_info:
        with client.websocket_connect(
                "/ws/speech/stt/stream?access_token=test-token",
        ):
            pass

    assert exc_info.value.code == 1008
    assert called["value"] is False


def test_assistant_stt_websocket_route_delegates_to_service(monkeypatch):
    _mock_ws_auth(monkeypatch)
    captured: dict = {}

    async def _fake_stt_service(*, websocket, user, session_duration_seconds):
        captured["user_id"] = user.id
        captured["session_duration_seconds"] = session_duration_seconds
        await websocket.accept()
        await websocket.send_json(
            {
                "type": "started",
                "max_duration_seconds": session_duration_seconds,
            }
        )
        await websocket.send_json({"type": "completed", "reason": "client_finish"})
        await websocket.close(code=1000)

    monkeypatch.setattr(
        speech_stt_module,
        "speech_stt_stream_service",
        _fake_stt_service,
    )

    client = TestClient(app)
    with client.websocket_connect(
            "/ws/speech/stt/stream?access_token=test-token",
    ) as websocket:
        started = websocket.receive_json()
        completed = websocket.receive_json()

    assert started["type"] == "started"
    assert started["max_duration_seconds"] == 60
    assert completed == {"type": "completed", "reason": "client_finish"}
    assert captured == {"user_id": 1, "session_duration_seconds": 60}


def test_assistant_stt_websocket_supports_token_query_key(monkeypatch):
    _mock_ws_auth(monkeypatch)
    captured: dict = {}

    async def _fake_stt_service(*, websocket, user, session_duration_seconds):
        captured["user_id"] = user.id
        captured["session_duration_seconds"] = session_duration_seconds
        await websocket.accept()
        await websocket.send_json(
            {
                "type": "started",
                "max_duration_seconds": session_duration_seconds,
            }
        )
        await websocket.send_json({"type": "completed", "reason": "client_finish"})
        await websocket.close(code=1000)

    monkeypatch.setattr(
        speech_stt_module,
        "speech_stt_stream_service",
        _fake_stt_service,
    )

    client = TestClient(app)
    with client.websocket_connect(
            "/ws/speech/stt/stream?token=test-token",
    ) as websocket:
        started = websocket.receive_json()
        completed = websocket.receive_json()

    assert started["type"] == "started"
    assert started["max_duration_seconds"] == 60
    assert completed == {"type": "completed", "reason": "client_finish"}
    assert captured == {"user_id": 1, "session_duration_seconds": 60}


def test_assistant_stt_websocket_route_can_return_timeout_event(monkeypatch):
    _mock_ws_auth(monkeypatch)

    async def _fake_stt_service(*, websocket, user, session_duration_seconds):  # noqa: ARG001
        await websocket.accept()
        await websocket.send_json(
            {
                "type": "timeout",
                "message": "识别已超过 60 秒，连接已关闭",
            }
        )
        await websocket.close(code=1000)

    monkeypatch.setattr(
        speech_stt_module,
        "speech_stt_stream_service",
        _fake_stt_service,
    )

    client = TestClient(app)
    with client.websocket_connect(
            "/ws/speech/stt/stream?access_token=test-token",
    ) as websocket:
        timeout_payload = websocket.receive_json()

    assert timeout_payload["type"] == "timeout"
    assert "60 秒" in timeout_payload["message"]


def test_assistant_stt_websocket_route_can_return_protocol_error_event(monkeypatch):
    _mock_ws_auth(monkeypatch)

    async def _fake_stt_service(*, websocket, user, session_duration_seconds):  # noqa: ARG001
        await websocket.accept()
        await websocket.send_json({"type": "error", "message": "请先发送 start"})
        await websocket.close(code=1008)

    monkeypatch.setattr(
        speech_stt_module,
        "speech_stt_stream_service",
        _fake_stt_service,
    )

    client = TestClient(app)
    with client.websocket_connect(
            "/ws/speech/stt/stream?access_token=test-token",
    ) as websocket:
        payload = websocket.receive_json()

    assert payload == {"type": "error", "message": "请先发送 start"}


def test_assistant_stt_websocket_query_token_forwarded_as_bearer(monkeypatch):
    observed = {"authorization": None}

    async def _fake_verify_authorization() -> AuthUser:
        observed["authorization"] = get_authorization_header()
        return AuthUser(
            id=1,
            username="tester",
            roles=[RoleCode.SUPER_ADMIN.value],
            permissions=[],
        )

    monkeypatch.setattr(speech_stt_module, "verify_authorization", _fake_verify_authorization)

    async def _fake_stt_service(*, websocket, user, session_duration_seconds):  # noqa: ARG001
        await websocket.accept()
        await websocket.send_json({"type": "completed", "reason": "client_finish"})
        await websocket.close(code=1000)

    monkeypatch.setattr(
        speech_stt_module,
        "speech_stt_stream_service",
        _fake_stt_service,
    )

    client = TestClient(app)
    with client.websocket_connect("/ws/speech/stt/stream?access_token=test-token") as websocket:
        payload = websocket.receive_json()

    assert payload == {"type": "completed", "reason": "client_finish"}
    assert observed["authorization"] == "Bearer test-token"
