import pytest

from app.services import document_chunk_service as service_module


class _FakeMilvusClient:
    def __init__(self) -> None:
        self.query_calls: list[dict[str, object]] = []
        self.insert_calls: list[dict[str, object]] = []
        self.query_result: list[dict] = []
        self.insert_result: object = None

    def query(self, **kwargs):
        self.query_calls.append(kwargs)
        return self.query_result

    def insert(self, **kwargs):
        self.insert_calls.append(kwargs)
        return self.insert_result


def test_add_document_chunk_uses_snowflake_id_for_manual_primary_key(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """验证新 schema 下新增切片会写入稳定雪花主键。"""
    client = _FakeMilvusClient()
    monkeypatch.setattr(service_module.vector_repository, "ensure_collection_exists", lambda **_: None)
    monkeypatch.setattr(service_module.vector_repository, "get_collection_embedding_dim", lambda **_: 1024)
    monkeypatch.setattr(service_module.vector_repository, "collection_uses_auto_id", lambda **_: False)
    monkeypatch.setattr(service_module, "create_embedding_client", lambda **_: object())
    monkeypatch.setattr(service_module, "embed_single_text", lambda **_: [0.1, 0.2])
    monkeypatch.setattr(service_module, "generate_snowflake_id", lambda: 555)
    monkeypatch.setattr(service_module, "get_milvus_client", lambda: client)

    result = service_module.add_document_chunk(
        knowledge_name="demo_kb",
        document_id=7,
        content="new chunk",
        embedding_model="text-embedding-v4",
    )

    assert result.vector_id == 555
    assert result.chunk_index == 1
    assert result.embedding_dim == 1024
    row = client.insert_calls[0]["data"][0]
    assert row["id"] == 555
    assert row["chunk_index"] == 1
    assert row["content"] == "new chunk"


def test_add_document_chunk_keeps_auto_id_compatibility(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """验证旧 auto_id collection 新增切片时仍使用 Milvus 返回主键。"""
    client = _FakeMilvusClient()
    client.query_result = [{"chunk_index": 2}]
    client.insert_result = {"ids": [909]}
    monkeypatch.setattr(service_module.vector_repository, "ensure_collection_exists", lambda **_: None)
    monkeypatch.setattr(service_module.vector_repository, "get_collection_embedding_dim", lambda **_: 1024)
    monkeypatch.setattr(service_module.vector_repository, "collection_uses_auto_id", lambda **_: True)
    monkeypatch.setattr(service_module, "create_embedding_client", lambda **_: object())
    monkeypatch.setattr(service_module, "embed_single_text", lambda **_: [0.1, 0.2])
    monkeypatch.setattr(service_module, "generate_snowflake_id", lambda: (_ for _ in ()).throw(
        AssertionError("auto_id collection 不应生成自定义主键")
    ))
    monkeypatch.setattr(service_module, "get_milvus_client", lambda: client)

    result = service_module.add_document_chunk(
        knowledge_name="demo_kb",
        document_id=7,
        content="new chunk",
        embedding_model="text-embedding-v4",
    )

    assert result.vector_id == 909
    assert result.chunk_index == 3
    row = client.insert_calls[0]["data"][0]
    assert "id" not in row
    assert row["chunk_index"] == 3
