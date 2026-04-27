import pytest

from app.core.exception.exceptions import ServiceException
from app.services import knowledge_base_service as service_module


def test_delete_documents_checks_collection_then_deletes(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """
    测试目的：验证批量删除文档前会先校验知识库存在，再删除这些文档全部切片。
    预期结果：repository 两个调用均被触发，参数正确。
    """
    calls: list[tuple[str, str, list[int] | None]] = []

    def _fake_ensure_collection_exists(*, knowledge_name: str) -> None:
        calls.append(("ensure", knowledge_name, None))

    def _fake_delete_document_chunks(
            *,
            knowledge_name: str,
            document_ids: list[int],
    ) -> None:
        calls.append(("delete", knowledge_name, document_ids))

    monkeypatch.setattr(
        service_module.vector_repository,
        "ensure_collection_exists",
        _fake_ensure_collection_exists,
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "delete_document_chunks",
        _fake_delete_document_chunks,
    )

    service_module.delete_documents(
        knowledge_name="demo_kb",
        document_ids=[9, 10],
    )

    assert calls == [
        ("ensure", "demo_kb", None),
        ("delete", "demo_kb", [9, 10]),
    ]


def test_update_document_status_checks_collection_then_updates(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """
    测试目的：验证修改文档状态前会校验知识库存在，并透传主键和状态值。
    预期结果：repository update 被调用一次且参数正确。
    """
    calls: list[tuple[str, str, int, int] | tuple[str, str]] = []

    def _fake_ensure_collection_exists(*, knowledge_name: str) -> None:
        calls.append(("ensure", knowledge_name))

    def _fake_update_document_chunk_status(
            *,
            knowledge_name: str,
            primary_id: int,
            status: int,
    ) -> int:
        calls.append(("update", knowledge_name, primary_id, status))
        return 202

    monkeypatch.setattr(
        service_module.vector_repository,
        "ensure_collection_exists",
        _fake_ensure_collection_exists,
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "update_document_chunk_status",
        _fake_update_document_chunk_status,
    )

    current_vector_id = service_module.update_document_status(
        knowledge_name="demo_kb",
        primary_id=101,
        status=1,
    )

    assert current_vector_id == 202
    assert calls == [
        ("ensure", "demo_kb"),
        ("update", "demo_kb", 101, 1),
    ]


def test_update_document_status_propagates_invalid_status(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """
    测试目的：验证底层状态校验失败时，service 不吞掉业务异常。
    预期结果：ServiceException 向上抛出。
    """
    monkeypatch.setattr(
        service_module.vector_repository,
        "ensure_collection_exists",
        lambda **_kwargs: None,
    )

    def _raise_invalid_status(**_kwargs) -> None:
        raise ServiceException(message="status 只能为 0 或 1")

    monkeypatch.setattr(
        service_module.vector_repository,
        "update_document_chunk_status",
        _raise_invalid_status,
    )

    with pytest.raises(ServiceException, match="status 只能为 0 或 1"):
        service_module.update_document_status(
            knowledge_name="demo_kb",
            primary_id=101,
            status=2,
        )


def test_update_document_status_by_vector_id_delegates_to_repository(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """
    测试目的：验证按主键更新状态时会透传主键和状态，并返回命中的知识库名称。
    预期结果：repository 被调用一次，返回值原样透传。
    """
    captured: dict[str, int] = {}

    def _fake_update_document_chunk_status_by_primary_id(
            *,
            primary_id: int,
            status: int,
    ) -> tuple[str, int]:
        captured["primary_id"] = primary_id
        captured["status"] = status
        return "demo_kb", 202

    monkeypatch.setattr(
        service_module.vector_repository,
        "update_document_chunk_status_by_primary_id",
        _fake_update_document_chunk_status_by_primary_id,
    )

    knowledge_name, current_vector_id = service_module.update_document_status_by_vector_id(
        primary_id=101,
        status=1,
    )

    assert knowledge_name == "demo_kb"
    assert current_vector_id == 202
    assert captured == {
        "primary_id": 101,
        "status": 1,
    }
