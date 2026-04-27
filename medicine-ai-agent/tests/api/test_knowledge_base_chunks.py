from fastapi.testclient import TestClient

import app.main as main_module
from app.api.routes import knowledge_base as knowledge_base_route
from app.core.security.system_auth.models import SystemAuthPrincipal
from app.main import app


def _mock_system_auth(monkeypatch) -> None:
    """
    功能描述:
        为系统签名接口注入固定认证上下文，避免测试依赖真实签名与 Redis。

    参数说明:
        无。

    返回值:
        None。

    异常说明:
        无。
    """

    async def _fake_verify_system_request(_request) -> SystemAuthPrincipal:
        return SystemAuthPrincipal(
            app_id="biz-server",
            sign_version="v1",
            timestamp=1770000000,
            nonce="nonce-1",
        )

    async def _unexpected_verify_authorization():
        raise AssertionError("allow_system 接口不应触发 verify_authorization")

    monkeypatch.setattr(
        main_module,
        "verify_system_request",
        _fake_verify_system_request,
    )
    monkeypatch.setattr(
        main_module,
        "verify_authorization",
        _unexpected_verify_authorization,
    )


def test_list_document_chunks_uses_page_size_default_50(monkeypatch) -> None:
    """
    测试目的：验证分页查询接口默认 page_size 为 50。
    预期结果：未传 page_size 时 service 收到 50，响应中也返回 50。
    """
    _mock_system_auth(monkeypatch)
    called: dict[str, int] = {}

    def _fake_list_knowledge_chunks(**kwargs):
        called["page_size"] = kwargs["page_size"]
        return [], 0

    monkeypatch.setattr(
        knowledge_base_route,
        "list_knowledge_chunks",
        _fake_list_knowledge_chunks,
    )
    client = TestClient(app)

    response = client.get(
        "/knowledge_base/document/chunks/list",
        params={
            "knowledge_name": "demo_kb",
            "document_id": 1,
            "page": 1,
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert called["page_size"] == 50
    assert body["data"]["page_size"] == 50
    assert body["data"]["has_next"] is False


def test_list_document_chunks_rejects_page_size_over_100(monkeypatch) -> None:
    """
    测试目的：验证分页查询接口限制 page_size 最大值为 100。
    预期结果：当 page_size=101 时返回参数校验错误。
    """
    _mock_system_auth(monkeypatch)
    client = TestClient(app)

    response = client.get(
        "/knowledge_base/document/chunks/list",
        params={
            "knowledge_name": "demo_kb",
            "document_id": 1,
            "page": 1,
            "page_size": 101,
        },
    )

    assert response.status_code == 400
    body = response.json()
    assert body["code"] == 400
    assert body["message"] == "Validation Failed"
    assert any(error["field"] == "page_size" for error in body["errors"])


def test_list_document_chunks_has_next_true_and_uses_chunk_index(monkeypatch) -> None:
    """
    测试目的：验证分页结果 has_next 计算正确，且 rows 使用 chunk_index 字段。
    预期结果：total=120/page=1/page_size=50 时 has_next=true。
    """
    _mock_system_auth(monkeypatch)

    def _fake_list_knowledge_chunks(**_kwargs):
        return [{"chunk_index": 1, "content": "A"}], 120

    monkeypatch.setattr(
        knowledge_base_route,
        "list_knowledge_chunks",
        _fake_list_knowledge_chunks,
    )
    client = TestClient(app)

    response = client.get(
        "/knowledge_base/document/chunks/list",
        params={
            "knowledge_name": "demo_kb",
            "document_id": 1,
            "page": 1,
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["data"]["has_next"] is True
    assert body["data"]["rows"][0]["chunk_index"] == 1
    assert all(not key.endswith("_no") for key in body["data"]["rows"][0].keys())


def test_list_document_chunks_has_next_false_on_last_page(monkeypatch) -> None:
    """
    测试目的：验证最后一页 has_next 为 false。
    预期结果：total=120/page=3/page_size=50 时 has_next=false。
    """
    _mock_system_auth(monkeypatch)

    def _fake_list_knowledge_chunks(**_kwargs):
        return [{"chunk_index": 101, "content": "tail"}], 120

    monkeypatch.setattr(
        knowledge_base_route,
        "list_knowledge_chunks",
        _fake_list_knowledge_chunks,
    )
    client = TestClient(app)

    response = client.get(
        "/knowledge_base/document/chunks/list",
        params={
            "knowledge_name": "demo_kb",
            "document_id": 1,
            "page": 3,
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["data"]["page_size"] == 50
    assert body["data"]["has_next"] is False


def test_delete_document_chunk_route_delegates_to_service(monkeypatch) -> None:
    """
    测试目的：验证批量删除文档接口会透传 knowledge_name/document_ids 到 service。
    预期结果：service 被调用一次，响应返回删除成功。
    """
    _mock_system_auth(monkeypatch)
    captured: dict[str, list[int] | str] = {}

    def _fake_delete_documents(
            *,
            knowledge_name: str,
            document_ids: list[int],
    ) -> None:
        captured["knowledge_name"] = knowledge_name
        captured["document_ids"] = document_ids

    monkeypatch.setattr(
        knowledge_base_route,
        "delete_documents",
        _fake_delete_documents,
    )
    client = TestClient(app)

    response = client.request(
        "DELETE",
        "/knowledge_base/document",
        json={
            "knowledge_name": "demo_kb",
            "document_ids": [7, 8],
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["message"] == "删除成功"
    assert body["data"] == {
        "document_ids": [7, 8],
        "knowledge_name": "demo_kb",
    }
    assert captured == {
        "knowledge_name": "demo_kb",
        "document_ids": [7, 8],
    }


def test_update_document_status_route_delegates_to_service(monkeypatch) -> None:
    """
    测试目的：验证修改状态接口会透传知识库、向量主键和状态值到 service。
    预期结果：service 被调用一次，响应返回更新后的状态信息。
    """
    _mock_system_auth(monkeypatch)
    captured: dict[str, int | str] = {}

    def _fake_update_document_status(
            *,
            knowledge_name: str,
            primary_id: int,
            status: int,
    ) -> int:
        captured["knowledge_name"] = knowledge_name
        captured["primary_id"] = primary_id
        captured["status"] = status
        return 202

    monkeypatch.setattr(
        knowledge_base_route,
        "update_document_status",
        _fake_update_document_status,
    )
    client = TestClient(app)

    response = client.put(
        "/knowledge_base/document/status",
        json={
            "knowledge_name": "demo_kb",
            "vector_id": 101,
            "status": 1,
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["message"] == "更新成功"
    assert body["data"] == {
        "knowledge_name": "demo_kb",
        "vector_id": 202,
        "status": 1,
    }
    assert captured == {
        "knowledge_name": "demo_kb",
        "primary_id": 101,
        "status": 1,
    }


def test_update_document_status_route_rejects_invalid_status(monkeypatch) -> None:
    """
    测试目的：验证修改状态接口仅允许 status=0/1。
    预期结果：非法状态返回 400，且不会触发 service。
    """
    _mock_system_auth(monkeypatch)

    def _unexpected_update_document_status(**_kwargs) -> None:
        raise AssertionError("非法状态不应进入 service")

    monkeypatch.setattr(
        knowledge_base_route,
        "update_document_status",
        _unexpected_update_document_status,
    )
    client = TestClient(app)

    response = client.put(
        "/knowledge_base/document/status",
        json={
            "knowledge_name": "demo_kb",
            "vector_id": 101,
            "status": 2,
        },
    )

    assert response.status_code == 400
    body = response.json()
    assert body["message"] == "Validation Failed"
    assert any(error["field"] == "status" for error in body["errors"])


def test_update_chunk_status_by_vector_id_route_delegates_to_service(monkeypatch) -> None:
    """
    测试目的：验证新接口只透传 vector_id/status，并返回命中的知识库名称。
    预期结果：service 被调用一次，响应返回更新成功。
    """
    _mock_system_auth(monkeypatch)
    captured: dict[str, int] = {}

    def _fake_update_document_status_by_vector_id(
            *,
            primary_id: int,
            status: int,
    ) -> tuple[str, int]:
        captured["primary_id"] = primary_id
        captured["status"] = status
        return "demo_kb", 202

    monkeypatch.setattr(
        knowledge_base_route,
        "update_document_status_by_vector_id",
        _fake_update_document_status_by_vector_id,
    )
    client = TestClient(app)

    response = client.put(
        "/knowledge_base/document/chunk/status",
        json={
            "vector_id": 101,
            "status": 1,
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["message"] == "更新成功"
    assert body["data"] == {
        "knowledge_name": "demo_kb",
        "vector_id": 202,
        "status": 1,
    }
    assert captured == {
        "primary_id": 101,
        "status": 1,
    }


def test_update_chunk_status_by_vector_id_route_rejects_missing_vector_id(monkeypatch) -> None:
    """
    测试目的：验证新接口仅接受 vector_id/status，缺少 vector_id 时返回参数校验错误。
    预期结果：返回 400，且不会进入 service。
    """
    _mock_system_auth(monkeypatch)

    def _unexpected_update_document_status_by_vector_id(**_kwargs) -> tuple[str, int]:
        raise AssertionError("缺少 vector_id 不应进入 service")

    monkeypatch.setattr(
        knowledge_base_route,
        "update_document_status_by_vector_id",
        _unexpected_update_document_status_by_vector_id,
    )
    client = TestClient(app)

    response = client.put(
        "/knowledge_base/document/chunk/status",
        json={
            "status": 1,
        },
    )

    assert response.status_code == 400
    body = response.json()
    assert body["message"] == "Validation Failed"
    assert any(error["field"] == "vector_id" for error in body["errors"])
