import importlib
import json

from fastapi.responses import StreamingResponse
from fastapi.testclient import TestClient

import app.main as main_module
from app.api.routes import admin_assistant as admin_route_module
from app.api.routes import client_assistant as client_route_module
from app.main import app
from app.schemas.auth import AuthUser
from app.schemas.assistant_run import AssistantRunStatus
from app.core.security.role_codes import RoleCode

rate_limit_module = importlib.import_module("app.core.security.rate_limit")


def _auth_headers() -> dict[str, str]:
    """构造测试用认证头。"""

    return {"Authorization": "Bearer test-token"}


def _mock_auth(
        monkeypatch,
        *,
        roles: list[str] | None = None,
        permissions: list[str] | None = None,
) -> None:
    """为 HTTP 接口注入固定认证用户。"""

    resolved_roles = [RoleCode.SUPER_ADMIN.value] if roles is None else roles
    resolved_permissions = [] if permissions is None else permissions

    async def _fake_fetch_current_user() -> AuthUser:
        return AuthUser(
            id=1,
            username="tester",
            roles=resolved_roles,
            permissions=resolved_permissions,
        )

    monkeypatch.setattr(main_module, "verify_authorization", _fake_fetch_current_user)


def _mock_rate_limit_allow(monkeypatch) -> None:
    """放通聊天 submit 限流。"""

    def _fake_evaluate_rate_limit(*, scope: str, subject_key: str, rules):
        return rate_limit_module.RateLimitCheckResult(
            allowed=True,
            retry_after_seconds=0,
            limit=10,
            remaining=9,
            reset_seconds=60,
        )

    monkeypatch.setattr(rate_limit_module, "_evaluate_rate_limit", _fake_evaluate_rate_limit)


def _build_streaming_response(text: str) -> StreamingResponse:
    """构造简单 SSE 响应。"""

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


def test_admin_submit_route_delegates_to_service(monkeypatch) -> None:
    """验证管理端 submit 路由会透传参数并包装 ApiResponse。"""

    captured: dict[str, str | None] = {}
    _mock_auth(monkeypatch)
    _mock_rate_limit_allow(monkeypatch)
    monkeypatch.setattr(
        admin_route_module,
        "assistant_chat_submit",
        lambda *, question, conversation_uuid=None: (
            captured.update(
                {
                    "question": question,
                    "conversation_uuid": conversation_uuid,
                }
            ),
            {
                "conversation_uuid": "conv-1",
                "message_uuid": "msg-1",
                "run_status": AssistantRunStatus.RUNNING,
            },
        )[-1],
    )

    client = TestClient(app)
    response = client.post(
        "/admin/assistant/chat/submit",
        headers=_auth_headers(),
        json={"question": "你好", "conversation_uuid": "conv-1"},
    )

    assert response.status_code == 200
    assert response.json()["data"] == {
        "conversation_uuid": "conv-1",
        "message_uuid": "msg-1",
        "run_status": "running",
    }
    assert captured == {
        "question": "你好",
        "conversation_uuid": "conv-1",
    }


def test_admin_stream_route_delegates_to_service(monkeypatch) -> None:
    """验证管理端 stream 路由会透传会话与 Last-Event-ID。"""

    captured: dict[str, str | None] = {}
    _mock_auth(monkeypatch)
    monkeypatch.setattr(
        admin_route_module,
        "assistant_chat_stream",
        lambda *, conversation_uuid, last_event_id=None: (
            captured.update(
                {
                    "conversation_uuid": conversation_uuid,
                    "last_event_id": last_event_id,
                }
            ),
            _build_streaming_response("管理端续流"),
        )[-1],
    )

    client = TestClient(app)
    response = client.get(
        "/admin/assistant/chat/stream",
        headers={
            **_auth_headers(),
            "Last-Event-ID": "12-0",
        },
        params={"conversation_uuid": "conv-1"},
    )

    assert response.status_code == 200
    assert response.headers["content-type"].startswith("text/event-stream")
    assert captured == {
        "conversation_uuid": "conv-1",
        "last_event_id": "12-0",
    }
    assert "管理端续流" in response.text


def test_admin_stop_route_delegates_to_service(monkeypatch) -> None:
    """验证管理端 stop 路由会返回停止响应。"""

    _mock_auth(monkeypatch)
    monkeypatch.setattr(
        admin_route_module,
        "assistant_chat_stop",
        lambda *, conversation_uuid: {
            "conversation_uuid": conversation_uuid,
            "message_uuid": "msg-1",
            "run_status": AssistantRunStatus.RUNNING,
            "stop_requested": True,
        },
    )

    client = TestClient(app)
    response = client.post(
        "/admin/assistant/chat/stop",
        headers=_auth_headers(),
        json={"conversation_uuid": "conv-1"},
    )

    assert response.status_code == 200
    assert response.json()["data"] == {
        "conversation_uuid": "conv-1",
        "message_uuid": "msg-1",
        "run_status": "running",
        "stop_requested": True,
    }


def test_client_submit_route_delegates_to_service(monkeypatch) -> None:
    """验证客户端 submit 路由仅透传 question/conversation_uuid 并返回运行态。"""

    captured: dict[str, object] = {}
    _mock_auth(monkeypatch, roles=[])
    _mock_rate_limit_allow(monkeypatch)
    monkeypatch.setattr(
        client_route_module,
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
                "message_id": "msg-2",
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


def test_client_submit_route_accepts_card_action_but_ignores_it(monkeypatch) -> None:
    """验证客户端 submit 路由仍可接受 card_action，但不会再传给 service。"""

    captured: dict[str, object] = {}
    _mock_auth(monkeypatch, roles=[])
    _mock_rate_limit_allow(monkeypatch)
    monkeypatch.setattr(
        client_route_module,
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
            "card_action": {
                "type": "click",
                "message_id": "msg-2",
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
        "conversation_uuid": None,
    }


def test_client_stream_route_delegates_to_service(monkeypatch) -> None:
    """验证客户端 stream 路由会 attach 到 service 返回的 SSE。"""

    captured: dict[str, str | None] = {}
    _mock_auth(monkeypatch, roles=[])
    monkeypatch.setattr(
        client_route_module,
        "assistant_chat_stream",
        lambda *, conversation_uuid, last_event_id=None: (
            captured.update(
                {
                    "conversation_uuid": conversation_uuid,
                    "last_event_id": last_event_id,
                }
            ),
            _build_streaming_response("客户端续流"),
        )[-1],
    )

    client = TestClient(app)
    response = client.get(
        "/client/assistant/chat/stream",
        headers={
            **_auth_headers(),
            "Last-Event-ID": "3-0",
        },
        params={"conversation_uuid": "client-conv-1"},
    )

    assert response.status_code == 200
    assert response.headers["content-type"].startswith("text/event-stream")
    assert captured == {
        "conversation_uuid": "client-conv-1",
        "last_event_id": "3-0",
    }
    assert "客户端续流" in response.text


def test_client_stop_route_delegates_to_service(monkeypatch) -> None:
    """验证客户端 stop 路由会返回标准停止响应。"""

    _mock_auth(monkeypatch, roles=[])
    monkeypatch.setattr(
        client_route_module,
        "assistant_chat_stop",
        lambda *, conversation_uuid: {
            "conversation_uuid": conversation_uuid,
            "message_uuid": "msg-9",
            "run_status": AssistantRunStatus.RUNNING,
            "stop_requested": True,
        },
    )

    client = TestClient(app)
    response = client.post(
        "/client/assistant/chat/stop",
        headers=_auth_headers(),
        json={"conversation_uuid": "client-conv-1"},
    )

    assert response.status_code == 200
    assert response.json()["data"] == {
        "conversation_uuid": "client-conv-1",
        "message_uuid": "msg-9",
        "run_status": "running",
        "stop_requested": True,
    }
