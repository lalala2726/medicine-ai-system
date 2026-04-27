import pytest

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.services import document_chunk_service as service_module


class _FakeMilvusClient:
    def __init__(self) -> None:
        self.get_calls: list[dict[str, object]] = []
        self.upsert_calls: list[dict[str, object]] = []
        self.get_result: list[dict] = []
        self.upsert_result: object = None

    def get(self, **kwargs):
        self.get_calls.append(kwargs)
        return self.get_result

    def upsert(self, **kwargs):
        self.upsert_calls.append(kwargs)
        return self.upsert_result


def test_rebuild_document_chunk_raises_when_vector_row_missing(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """验证向量记录不存在时会抛出 404 业务异常且不执行 upsert。"""
    client = _FakeMilvusClient()
    monkeypatch.setattr(service_module.vector_repository, "ensure_collection_exists", lambda **_: None)
    monkeypatch.setattr(service_module.vector_repository, "get_collection_embedding_dim", lambda **_: 1024)
    monkeypatch.setattr(service_module.vector_repository, "collection_uses_auto_id", lambda **_: False)
    monkeypatch.setattr(service_module, "create_embedding_client", lambda **_: object())
    monkeypatch.setattr(service_module, "embed_single_text", lambda **_: [0.1, 0.2])
    monkeypatch.setattr(service_module, "get_milvus_client", lambda: client)

    with pytest.raises(ServiceException) as exc_info:
        service_module.rebuild_document_chunk(
            knowledge_name="demo_kb",
            document_id=7,
            vector_id=101,
            version=3,
            content="updated chunk",
            embedding_model="text-embedding-v4",
        )

    assert exc_info.value.code == ResponseCode.NOT_FOUND.code
    assert exc_info.value.message == "向量记录不存在"
    assert client.upsert_calls == []


def test_rebuild_document_chunk_raises_when_document_id_mismatches(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """验证向量记录归属的 document_id 不匹配时拒绝更新。"""
    client = _FakeMilvusClient()
    client.get_result = [{"id": 101, "document_id": 9}]
    monkeypatch.setattr(service_module.vector_repository, "ensure_collection_exists", lambda **_: None)
    monkeypatch.setattr(service_module.vector_repository, "get_collection_embedding_dim", lambda **_: 1024)
    monkeypatch.setattr(service_module.vector_repository, "collection_uses_auto_id", lambda **_: False)
    monkeypatch.setattr(service_module, "create_embedding_client", lambda **_: object())
    monkeypatch.setattr(service_module, "embed_single_text", lambda **_: [0.1, 0.2])
    monkeypatch.setattr(service_module, "get_milvus_client", lambda: client)

    with pytest.raises(ServiceException, match="向量记录与文档ID不匹配"):
        service_module.rebuild_document_chunk(
            knowledge_name="demo_kb",
            document_id=7,
            vector_id=101,
            version=3,
            content="updated chunk",
            embedding_model="text-embedding-v4",
        )

    assert client.upsert_calls == []


def test_rebuild_document_chunk_propagates_model_initialization_failure(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """验证向量模型初始化失败时不会触发 Milvus 写入。"""
    client = _FakeMilvusClient()
    monkeypatch.setattr(service_module.vector_repository, "ensure_collection_exists", lambda **_: None)
    monkeypatch.setattr(service_module.vector_repository, "get_collection_embedding_dim", lambda **_: 1024)
    monkeypatch.setattr(service_module.vector_repository, "collection_uses_auto_id", lambda **_: False)
    monkeypatch.setattr(service_module, "get_milvus_client", lambda: client)

    def _raise_create_error(**_kwargs):
        raise ServiceException(message="初始化向量模型失败: mock init error")

    monkeypatch.setattr(service_module, "create_embedding_client", _raise_create_error)

    with pytest.raises(ServiceException, match="初始化向量模型失败"):
        service_module.rebuild_document_chunk(
            knowledge_name="demo_kb",
            document_id=7,
            vector_id=101,
            version=3,
            content="updated chunk",
            embedding_model="text-embedding-v4",
        )

    assert client.upsert_calls == []


def test_rebuild_document_chunk_does_not_upsert_when_embedding_fails(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """验证 embedding 失败时不会触发 Milvus upsert。"""
    client = _FakeMilvusClient()
    client.get_result = [{
        "id": 101,
        "document_id": 7,
        "chunk_index": 2,
        "content": "old",
        "char_count": 3,
        "embedding": [0.1, 0.2],
        "chunk_size": 500,
        "chunk_overlap": 0,
        "status": 0,
        "source_hash": "hash",
        "created_at_ts": 123,
    }]
    monkeypatch.setattr(service_module.vector_repository, "ensure_collection_exists", lambda **_: None)
    monkeypatch.setattr(service_module.vector_repository, "get_collection_embedding_dim", lambda **_: 1024)
    monkeypatch.setattr(service_module.vector_repository, "collection_uses_auto_id", lambda **_: False)
    monkeypatch.setattr(service_module, "create_embedding_client", lambda **_: object())
    monkeypatch.setattr(service_module, "get_milvus_client", lambda: client)

    def _raise_embed_error(**_kwargs):
        raise ServiceException(message="嵌入文本失败: mock error")

    monkeypatch.setattr(service_module, "embed_single_text", _raise_embed_error)

    with pytest.raises(ServiceException, match="嵌入文本失败"):
        service_module.rebuild_document_chunk(
            knowledge_name="demo_kb",
            document_id=7,
            vector_id=101,
            version=3,
            content="updated chunk",
            embedding_model="text-embedding-v4",
        )

    assert client.upsert_calls == []


def test_rebuild_document_chunk_upserts_once_and_preserves_metadata(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """验证成功重建时只 upsert 一次，并保留非内容类 metadata。"""
    client = _FakeMilvusClient()
    client.get_result = [{
        "id": 101,
        "document_id": 7,
        "chunk_index": 2,
        "content": "old",
        "char_count": 3,
        "embedding": [0.1, 0.2],
        "chunk_size": 500,
        "chunk_overlap": 0,
        "status": 0,
        "source_hash": "hash-1",
        "created_at_ts": 123,
    }]
    monkeypatch.setattr(service_module.vector_repository, "ensure_collection_exists", lambda **_: None)
    monkeypatch.setattr(service_module.vector_repository, "get_collection_embedding_dim", lambda **_: 1024)
    monkeypatch.setattr(service_module.vector_repository, "collection_uses_auto_id", lambda **_: False)
    monkeypatch.setattr(service_module, "create_embedding_client", lambda **_: object())
    monkeypatch.setattr(service_module, "embed_single_text", lambda **_: [0.5, 0.6])
    monkeypatch.setattr(service_module, "get_chunk_edit_latest_version", lambda **_: 3)
    monkeypatch.setattr(service_module, "get_milvus_client", lambda: client)

    result = service_module.rebuild_document_chunk(
        knowledge_name="demo_kb",
        document_id=7,
        vector_id=101,
        version=3,
        content="updated chunk",
        embedding_model="text-embedding-v4",
    )

    assert result.vector_id == 101
    assert result.embedding_dim == 1024
    assert client.get_calls == [{
        "collection_name": "demo_kb",
        "ids": 101,
        "output_fields": service_module.CHUNK_REBUILD_OUTPUT_FIELDS,
    }]
    assert len(client.upsert_calls) == 1
    payload = client.upsert_calls[0]
    assert payload["collection_name"] == "demo_kb"
    row = payload["data"][0]
    assert row["id"] == 101
    assert row["document_id"] == 7
    assert row["chunk_index"] == 2
    assert row["chunk_size"] == 500
    assert row["chunk_overlap"] == 0
    assert row["status"] == 0
    assert row["content"] == "updated chunk"
    assert row["char_count"] == len("updated chunk")
    assert row["embedding"] == [0.5, 0.6]
    assert row["source_hash"] is None
    assert isinstance(row["created_at_ts"], int)
    assert row["created_at_ts"] > 0


def test_rebuild_document_chunk_rejects_stale_version_before_upsert(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """验证写入前若发现消息已过期则不会执行 upsert。"""
    client = _FakeMilvusClient()
    client.get_result = [{
        "id": 101,
        "document_id": 7,
        "chunk_index": 2,
        "content": "old",
        "char_count": 3,
        "embedding": [0.1, 0.2],
        "chunk_size": 500,
        "chunk_overlap": 0,
        "status": 0,
        "source_hash": "hash-1",
        "created_at_ts": 123,
    }]
    monkeypatch.setattr(service_module.vector_repository, "ensure_collection_exists", lambda **_: None)
    monkeypatch.setattr(service_module.vector_repository, "get_collection_embedding_dim", lambda **_: 1024)
    monkeypatch.setattr(service_module.vector_repository, "collection_uses_auto_id", lambda **_: False)
    monkeypatch.setattr(service_module, "create_embedding_client", lambda **_: object())
    monkeypatch.setattr(service_module, "embed_single_text", lambda **_: [0.5, 0.6])
    monkeypatch.setattr(service_module, "get_chunk_edit_latest_version", lambda **_: 6)
    monkeypatch.setattr(service_module, "get_milvus_client", lambda: client)

    with pytest.raises(service_module.ChunkRebuildMessageStaleError, match="message_version=5"):
        service_module.rebuild_document_chunk(
            knowledge_name="demo_kb",
            document_id=7,
            vector_id=101,
            version=5,
            content="updated chunk",
            embedding_model="text-embedding-v4",
        )

    assert client.upsert_calls == []


def test_rebuild_document_chunk_returns_new_primary_id_for_auto_id_collection(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """验证旧 auto_id collection 重建后会返回 Milvus 新主键。"""
    client = _FakeMilvusClient()
    client.get_result = [{
        "id": 101,
        "document_id": 7,
        "chunk_index": 2,
        "content": "old",
        "char_count": 3,
        "embedding": [0.1, 0.2],
        "chunk_size": 500,
        "chunk_overlap": 0,
        "status": 0,
        "source_hash": "hash-1",
        "created_at_ts": 123,
    }]
    client.upsert_result = {"ids": [303]}
    monkeypatch.setattr(service_module.vector_repository, "ensure_collection_exists", lambda **_: None)
    monkeypatch.setattr(service_module.vector_repository, "get_collection_embedding_dim", lambda **_: 1024)
    monkeypatch.setattr(service_module.vector_repository, "collection_uses_auto_id", lambda **_: True)
    monkeypatch.setattr(service_module, "create_embedding_client", lambda **_: object())
    monkeypatch.setattr(service_module, "embed_single_text", lambda **_: [0.5, 0.6])
    monkeypatch.setattr(service_module, "get_chunk_edit_latest_version", lambda **_: 3)
    monkeypatch.setattr(service_module, "get_milvus_client", lambda: client)

    result = service_module.rebuild_document_chunk(
        knowledge_name="demo_kb",
        document_id=7,
        vector_id=101,
        version=3,
        content="updated chunk",
        embedding_model="text-embedding-v4",
    )

    assert result.vector_id == 303
    assert result.embedding_dim == 1024
