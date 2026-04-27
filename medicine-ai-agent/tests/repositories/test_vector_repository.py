import pytest
from pymilvus import DataType
from pymilvus.client.types import LoadState

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.repositories import vector_repository as repository_module


class _FakeIndexParams:
    def __init__(self) -> None:
        self.calls: list[dict[str, object]] = []

    def add_index(self, **kwargs) -> None:
        self.calls.append(kwargs)


class _FakeMilvusClient:
    def __init__(self, has_collection_result: bool) -> None:
        self._has_collection_result = has_collection_result
        self.index_params = _FakeIndexParams()
        self.create_collection_calls: list[dict[str, object]] = []
        self.load_collection_calls: list[dict[str, object]] = []
        self.release_collection_calls: list[dict[str, object]] = []
        self.get_load_state_calls: list[dict[str, object]] = []
        self.query_calls: list[dict[str, object]] = []
        self.get_calls: list[dict[str, object]] = []
        self.delete_calls: list[dict[str, object]] = []
        self.insert_calls: list[dict[str, object]] = []
        self.upsert_calls: list[dict[str, object]] = []
        self.list_collections_calls: list[dict[str, object]] = []
        self.rows_result: list[dict] = []
        self.get_result: list[dict] = []
        self.collection_get_results: dict[str, list[dict]] = {}
        self.count_result: list[dict] = [{"count(*)": 0}]
        self.load_state_result: dict = {"state": LoadState.NotLoad}
        self.list_collections_result: list[str] = []
        self.describe_result: dict = {
            "fields": [
                {
                    "name": "id",
                    "is_primary": True,
                    "auto_id": False,
                    "params": {},
                },
                {
                    "name": "embedding",
                    "params": {"dim": 1024},
                }
            ]
        }
        self.insert_result: object = None
        self.upsert_result: object = None

    def has_collection(self, _name: str) -> bool:
        return self._has_collection_result

    def prepare_index_params(self) -> _FakeIndexParams:
        return self.index_params

    def create_collection(self, **kwargs) -> None:
        self.create_collection_calls.append(kwargs)

    def load_collection(self, **kwargs) -> None:
        self.load_collection_calls.append(kwargs)

    def release_collection(self, **kwargs) -> None:
        self.release_collection_calls.append(kwargs)

    def get_load_state(self, **kwargs) -> dict:
        self.get_load_state_calls.append(kwargs)
        return self.load_state_result

    def drop_collection(self, _name: str) -> None:
        return None

    def query(self, **kwargs):
        self.query_calls.append(kwargs)
        output_fields = kwargs.get("output_fields")
        if output_fields == ["count(*)"]:
            return self.count_result
        if output_fields == ["id"]:
            return []
        return self.rows_result

    def get(self, **kwargs):
        self.get_calls.append(kwargs)
        collection_name = kwargs.get("collection_name")
        if collection_name in self.collection_get_results:
            return self.collection_get_results[collection_name]
        return self.get_result

    def list_collections(self, **kwargs):
        self.list_collections_calls.append(kwargs)
        return self.list_collections_result

    def delete(self, **kwargs) -> None:
        self.delete_calls.append(kwargs)

    def insert(self, **kwargs):
        self.insert_calls.append(kwargs)
        return self.insert_result

    def upsert(self, **kwargs):
        self.upsert_calls.append(kwargs)
        return self.upsert_result

    def describe_collection(self, _name: str) -> dict:
        return self.describe_result


def test_build_collection_schema_contains_standard_11_fields() -> None:
    """
    测试目的：验证 Milvus repository 使用标准版 11 字段 schema。
    预期结果：schema 字段顺序、字段类型、向量维度与字符串长度约束均符合约定。
    """
    schema = repository_module._build_collection_schema(
        embedding_dim=1024,
        description="demo",
    )
    field_names = [field.name for field in schema.fields]
    assert field_names == [
        "id",
        "document_id",
        "chunk_index",
        "content",
        "char_count",
        "embedding",
        "chunk_size",
        "chunk_overlap",
        "status",
        "source_hash",
        "created_at_ts",
    ]

    fields = {field.name: field for field in schema.fields}
    assert fields["id"].dtype == DataType.INT64
    assert fields["id"].is_primary is True
    assert fields["id"].auto_id is False
    assert fields["document_id"].dtype == DataType.INT64
    assert fields["chunk_index"].dtype == DataType.INT64
    assert fields["content"].dtype == DataType.VARCHAR
    assert (
            fields["content"].params["max_length"]
            == repository_module.DEFAULT_CONTENT_MAX_LENGTH
    )
    assert fields["char_count"].dtype == DataType.INT32
    assert fields["embedding"].dtype == DataType.FLOAT_VECTOR
    assert fields["embedding"].params["dim"] == 1024
    assert fields["chunk_size"].dtype == DataType.INT32
    assert fields["chunk_overlap"].dtype == DataType.INT32
    assert fields["status"].dtype == DataType.INT32
    assert not fields["status"].params.get("default_value")
    assert fields["source_hash"].dtype == DataType.VARCHAR
    assert (
            fields["source_hash"].params["max_length"]
            == repository_module.DEFAULT_SOURCE_HASH_MAX_LENGTH
    )
    assert fields["created_at_ts"].dtype == DataType.INT64


def test_build_index_params_adds_document_and_embedding_indexes() -> None:
    """
    测试目的：验证索引参数组装包含 document_id 标量索引与 embedding 向量索引。
    预期结果：add_index 被调用两次且参数值符合预期。
    """
    client = _FakeMilvusClient(has_collection_result=False)

    index_params = repository_module._build_index_params(client)

    assert index_params is client.index_params
    assert client.index_params.calls == [
        {"field_name": "document_id", "index_type": "STL_SORT"},
        {
            "field_name": "embedding",
            "index_type": repository_module.DEFAULT_VECTOR_INDEX_TYPE,
            "metric_type": repository_module.DEFAULT_VECTOR_METRIC_TYPE,
        },
    ]


def test_create_collection_raises_when_collection_exists(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """
    测试目的：验证集合已存在时 repository 会抛出统一业务异常。
    预期结果：抛出 ServiceException，错误文案包含 knowledge 已存在。
    """
    client = _FakeMilvusClient(has_collection_result=True)
    monkeypatch.setattr(repository_module, "get_milvus_client", lambda: client)

    with pytest.raises(ServiceException, match="knowledge 已存在"):
        repository_module.create_collection(
            knowledge_name="demo_kb",
            embedding_dim=1024,
            description="demo",
        )


def test_load_collection_state_returns_normalized_state(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """
    测试目的：验证加载集合会调用 load + get_load_state 并返回规范化状态。
    预期结果：返回包含 knowledge_name/load_state，且调用参数正确。
    """
    client = _FakeMilvusClient(has_collection_result=True)
    client.load_state_result = {"state": LoadState.Loaded}
    monkeypatch.setattr(repository_module, "get_milvus_client", lambda: client)

    result = repository_module.load_collection_state("demo_kb")

    assert result == {
        "knowledge_name": "demo_kb",
        "load_state": "Loaded",
    }
    assert client.load_collection_calls == [{"collection_name": "demo_kb"}]
    assert client.get_load_state_calls == [{"collection_name": "demo_kb"}]


def test_load_collection_state_keeps_progress_when_loading(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """
    测试目的：验证加载中状态会透传 progress 字段。
    预期结果：返回 load_state=Loading 且 progress 保持原值。
    """
    client = _FakeMilvusClient(has_collection_result=True)
    client.load_state_result = {"state": LoadState.Loading, "progress": 37}
    monkeypatch.setattr(repository_module, "get_milvus_client", lambda: client)

    result = repository_module.load_collection_state("demo_kb")

    assert result["load_state"] == "Loading"
    assert result["progress"] == 37


def test_release_collection_state_returns_normalized_state(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """
    测试目的：验证释放集合会调用 release + get_load_state 并返回规范化状态。
    预期结果：返回包含 knowledge_name/load_state，且调用参数正确。
    """
    client = _FakeMilvusClient(has_collection_result=True)
    client.load_state_result = {"state": LoadState.NotLoad}
    monkeypatch.setattr(repository_module, "get_milvus_client", lambda: client)

    result = repository_module.release_collection_state("demo_kb")

    assert result == {
        "knowledge_name": "demo_kb",
        "load_state": "NotLoad",
    }
    assert client.release_collection_calls == [{"collection_name": "demo_kb"}]
    assert client.get_load_state_calls == [{"collection_name": "demo_kb"}]


def test_load_collection_state_raises_not_found_when_collection_missing(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """
    测试目的：验证加载不存在集合时抛出 NOT_FOUND 业务异常。
    预期结果：错误码为 404，且不会调用 load/get_load_state。
    """
    client = _FakeMilvusClient(has_collection_result=False)
    monkeypatch.setattr(repository_module, "get_milvus_client", lambda: client)

    with pytest.raises(ServiceException) as exc_info:
        repository_module.load_collection_state("missing_kb")

    assert int(exc_info.value.code) == ResponseCode.NOT_FOUND.code
    assert client.load_collection_calls == []
    assert client.get_load_state_calls == []


def test_release_collection_state_raises_not_found_when_collection_missing(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """
    测试目的：验证释放不存在集合时抛出 NOT_FOUND 业务异常。
    预期结果：错误码为 404，且不会调用 release/get_load_state。
    """
    client = _FakeMilvusClient(has_collection_result=False)
    monkeypatch.setattr(repository_module, "get_milvus_client", lambda: client)

    with pytest.raises(ServiceException) as exc_info:
        repository_module.release_collection_state("missing_kb")

    assert int(exc_info.value.code) == ResponseCode.NOT_FOUND.code
    assert client.release_collection_calls == []
    assert client.get_load_state_calls == []


def test_list_document_chunks_queries_with_expected_filter_and_fields(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """
    测试目的：验证分页查询按 document_id 过滤并返回约定输出字段。
    预期结果：返回 rows 与 total 正确，且 query 参数包含正确 filter/limit/offset/output_fields。
    """
    client = _FakeMilvusClient(has_collection_result=True)
    client.count_result = [{"count(*)": 3}]
    client.rows_result = [
        {
            "id": 1,
            "document_id": 10,
            "chunk_index": 2,
            "content": "demo-2",
        },
        {
            "id": 2,
            "document_id": 10,
            "chunk_index": 1,
            "content": "demo",
        }
    ]
    monkeypatch.setattr(repository_module, "get_milvus_client", lambda: client)

    rows, total = repository_module.list_document_chunks(
        knowledge_name="demo_kb",
        document_id=10,
        page_num=2,
        page_size=5,
    )

    assert total == 3
    assert rows == [
        {
            "id": 2,
            "document_id": 10,
            "chunk_index": 1,
            "content": "demo",
        },
        {
            "id": 1,
            "document_id": 10,
            "chunk_index": 2,
            "content": "demo-2",
        },
    ]
    assert len(client.query_calls) == 2
    count_query = client.query_calls[0]
    rows_query = client.query_calls[1]
    assert count_query["filter"] == "document_id == 10"
    assert rows_query["filter"] == "document_id == 10"
    assert rows_query["limit"] == 5
    assert rows_query["offset"] == 5
    assert rows_query["output_fields"] == [
        "id",
        "document_id",
        "chunk_index",
        "content",
        "char_count",
        "chunk_size",
        "chunk_overlap",
        "status",
        "source_hash",
        "created_at_ts",
    ]


def test_delete_document_chunks_calls_milvus_delete_with_document_ids_filter(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """
    测试目的：验证批量删除文档切片时会按 document_id 列表过滤调用 Milvus delete。
    预期结果：delete 被调用一次且过滤表达式为 document_id in 指定列表。
    """
    client = _FakeMilvusClient(has_collection_result=True)
    monkeypatch.setattr(repository_module, "get_milvus_client", lambda: client)

    repository_module.delete_document_chunks(
        knowledge_name="demo_kb",
        document_ids=[42, 43],
    )

    assert len(client.delete_calls) == 1
    payload = client.delete_calls[0]
    assert payload["collection_name"] == "demo_kb"
    assert payload["filter"] == "document_id in [42, 43]"


def test_count_document_chunks_supports_optional_status_filter(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """
    测试目的：验证切片计数支持按 document_id 统计，并可附加 status 过滤。
    预期结果：返回 count(*) 值，且查询过滤表达式符合预期。
    """
    client = _FakeMilvusClient(has_collection_result=True)
    client.count_result = [{"count(*)": 7}]
    monkeypatch.setattr(repository_module, "get_milvus_client", lambda: client)

    total_all = repository_module.count_document_chunks(
        knowledge_name="demo_kb",
        document_id=10,
    )
    total_enabled = repository_module.count_document_chunks(
        knowledge_name="demo_kb",
        document_id=10,
        status=0,
    )

    assert total_all == 7
    assert total_enabled == 7
    assert client.query_calls[0]["filter"] == "document_id == 10"
    assert client.query_calls[1]["filter"] == "document_id == 10 and status == 0"


def test_update_document_chunk_status_reads_then_upserts(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """
    测试目的：验证按向量主键更新状态时，会先读取原记录，再以 upsert 覆盖 status。
    预期结果：get/upsert 各调用一次，且仅 status 被更新为目标值。
    """
    client = _FakeMilvusClient(has_collection_result=True)
    client.get_result = [
        {
            "id": 101,
            "document_id": 42,
            "chunk_index": 3,
            "content": "demo",
            "char_count": 4,
            "embedding": [0.1, 0.2],
            "chunk_size": 500,
            "chunk_overlap": 0,
            "status": repository_module.KNOWLEDGE_STATUS_ENABLED,
            "source_hash": "hash-1",
            "created_at_ts": 1234567890,
        }
    ]
    monkeypatch.setattr(repository_module, "get_milvus_client", lambda: client)

    current_vector_id = repository_module.update_document_chunk_status(
        knowledge_name="demo_kb",
        primary_id=101,
        status=repository_module.KNOWLEDGE_STATUS_DISABLED,
    )

    assert client.get_calls == [
        {
            "collection_name": "demo_kb",
            "ids": 101,
            "output_fields": repository_module.DOCUMENT_CHUNK_OUTPUT_FIELDS,
        }
    ]
    assert len(client.upsert_calls) == 1
    payload = client.upsert_calls[0]
    assert payload["collection_name"] == "demo_kb"
    row = payload["data"][0]
    assert current_vector_id == 101
    assert row["id"] == 101
    assert row["document_id"] == 42
    assert row["status"] == repository_module.KNOWLEDGE_STATUS_DISABLED
    assert row["embedding"] == [0.1, 0.2]


def test_update_document_chunk_status_rejects_invalid_status() -> None:
    """
    测试目的：验证状态只允许 0 或 1。
    预期结果：传入非法值时抛出统一业务异常。
    """
    with pytest.raises(ServiceException, match="status 只能为 0 或 1"):
        repository_module.update_document_chunk_status(
            knowledge_name="demo_kb",
            primary_id=101,
            status=2,
        )


def test_update_document_chunk_status_raises_when_vector_row_missing(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """
    测试目的：验证目标向量主键不存在时返回 404 业务异常。
    预期结果：get 命中空列表时抛出 ServiceException，code=404。
    """
    client = _FakeMilvusClient(has_collection_result=True)
    monkeypatch.setattr(repository_module, "get_milvus_client", lambda: client)

    with pytest.raises(ServiceException) as exc_info:
        repository_module.update_document_chunk_status(
            knowledge_name="demo_kb",
            primary_id=101,
            status=repository_module.KNOWLEDGE_STATUS_ENABLED,
        )

    assert exc_info.value.code == ResponseCode.NOT_FOUND.code
    assert exc_info.value.message == "向量记录不存在"


def test_update_document_chunk_status_by_primary_id_updates_matched_collection(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """
    测试目的：验证 repository 可遍历集合并更新命中的唯一向量记录。
    预期结果：返回命中的集合名，且 upsert 写回正确状态。
    """
    client = _FakeMilvusClient(has_collection_result=True)
    client.list_collections_result = ["kb_a", "kb_b"]
    client.collection_get_results = {
        "kb_a": [],
        "kb_b": [
            {
                "id": 101,
                "document_id": 42,
                "chunk_index": 3,
                "content": "demo",
                "char_count": 4,
                "embedding": [0.1, 0.2],
                "chunk_size": 500,
                "chunk_overlap": 0,
                "status": repository_module.KNOWLEDGE_STATUS_ENABLED,
                "source_hash": "hash-1",
                "created_at_ts": 1234567890,
            }
        ],
    }
    monkeypatch.setattr(repository_module, "get_milvus_client", lambda: client)

    knowledge_name, current_vector_id = repository_module.update_document_chunk_status_by_primary_id(
        primary_id=101,
        status=repository_module.KNOWLEDGE_STATUS_DISABLED,
    )

    assert knowledge_name == "kb_b"
    assert current_vector_id == 101
    assert client.list_collections_calls == [{}]
    assert len(client.upsert_calls) == 1
    payload = client.upsert_calls[0]
    assert payload["collection_name"] == "kb_b"
    assert payload["data"][0]["status"] == repository_module.KNOWLEDGE_STATUS_DISABLED


def test_update_document_chunk_status_by_primary_id_raises_when_missing(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """
    测试目的：验证主键在所有集合中都不存在时返回 404。
    预期结果：抛出 ServiceException，code=404。
    """
    client = _FakeMilvusClient(has_collection_result=True)
    client.list_collections_result = ["kb_a", "kb_b"]
    client.collection_get_results = {
        "kb_a": [],
        "kb_b": [],
    }
    monkeypatch.setattr(repository_module, "get_milvus_client", lambda: client)

    with pytest.raises(ServiceException) as exc_info:
        repository_module.update_document_chunk_status_by_primary_id(
            primary_id=101,
            status=repository_module.KNOWLEDGE_STATUS_ENABLED,
        )

    assert exc_info.value.code == ResponseCode.NOT_FOUND.code
    assert exc_info.value.message == "向量记录不存在"


def test_update_document_chunk_status_by_primary_id_raises_when_multiple_matches(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """
    测试目的：验证同一主键命中多个集合时不会盲目更新第一个集合。
    预期结果：抛出业务异常，提示命中多个知识库。
    """
    client = _FakeMilvusClient(has_collection_result=True)
    row = {
        "id": 101,
        "document_id": 42,
        "chunk_index": 3,
        "content": "demo",
        "char_count": 4,
        "embedding": [0.1, 0.2],
        "chunk_size": 500,
        "chunk_overlap": 0,
        "status": repository_module.KNOWLEDGE_STATUS_ENABLED,
        "source_hash": "hash-1",
        "created_at_ts": 1234567890,
    }
    client.list_collections_result = ["kb_a", "kb_b"]
    client.collection_get_results = {
        "kb_a": [row],
        "kb_b": [row],
    }
    monkeypatch.setattr(repository_module, "get_milvus_client", lambda: client)

    with pytest.raises(ServiceException, match="向量主键命中多个知识库"):
        repository_module.update_document_chunk_status_by_primary_id(
            primary_id=101,
            status=repository_module.KNOWLEDGE_STATUS_DISABLED,
        )

    assert client.upsert_calls == []


def test_get_collection_embedding_dim_reads_dim_from_schema(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """
    测试目的：验证 repository 可从 collection schema 中读取 embedding 字段 dim。
    预期结果：返回整型维度值，且与 schema 配置一致。
    """
    client = _FakeMilvusClient(has_collection_result=True)
    client.describe_result = {
        "fields": [
            {"name": "id", "params": {}},
            {"name": "embedding", "params": {"dim": 1536}},
        ]
    }
    monkeypatch.setattr(repository_module, "get_milvus_client", lambda: client)

    dim = repository_module.get_collection_embedding_dim("demo_kb")

    assert dim == 1536


def test_get_collection_embedding_dim_raises_when_embedding_field_missing(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """
    测试目的：验证 schema 中不存在 embedding 字段时会抛出业务异常。
    预期结果：调用 get_collection_embedding_dim 抛出 ServiceException。
    """
    client = _FakeMilvusClient(has_collection_result=True)
    client.describe_result = {"fields": [{"name": "id", "params": {}}]}
    monkeypatch.setattr(repository_module, "get_milvus_client", lambda: client)

    with pytest.raises(ServiceException, match="embedding 字段"):
        repository_module.get_collection_embedding_dim("demo_kb")


def test_insert_embeddings_builds_full_payload_fields(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """
    测试目的：验证向量写入会补齐 chunk_index/char_count/策略快照/时间戳等字段。
    预期结果：insert 数据包含完整业务字段，且 chunk_index 从 start_chunk_index 开始递增。
    """
    client = _FakeMilvusClient(has_collection_result=True)
    monkeypatch.setattr(repository_module, "get_milvus_client", lambda: client)

    repository_module.insert_embeddings(
        knowledge_name="demo_kb",
        document_id=99,
        embeddings=[[0.1, 0.2], [0.3, 0.4]],
        texts=["A", "BC"],
        start_chunk_index=7,
        chunk_size=500,
        chunk_overlap=0,
        source_hash="hash-1",
        char_counts=[1, 2],
        created_at_ts=1234567890,
    )

    assert len(client.insert_calls) == 1
    payload = client.insert_calls[0]
    assert payload["collection_name"] == "demo_kb"
    rows = payload["data"]
    assert len(rows) == 2
    assert isinstance(rows[0]["id"], int)
    assert isinstance(rows[1]["id"], int)
    assert rows[0]["id"] != rows[1]["id"]
    assert rows[0]["chunk_index"] == 7
    assert rows[1]["chunk_index"] == 8
    assert rows[0]["char_count"] == 1
    assert rows[1]["char_count"] == 2
    assert rows[0]["chunk_size"] == 500
    assert rows[0]["chunk_overlap"] == 0
    assert rows[0]["status"] == repository_module.DEFAULT_KNOWLEDGE_STATUS
    assert rows[1]["status"] == repository_module.DEFAULT_KNOWLEDGE_STATUS
    assert rows[0]["source_hash"] == "hash-1"
    assert rows[0]["created_at_ts"] == 1234567890


def test_collection_uses_auto_id_reads_schema_flag(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """
    测试目的：验证 repository 可识别旧 collection 的 auto_id 主键模式。
    预期结果：schema 主键带 auto_id=true 时返回 True。
    """
    client = _FakeMilvusClient(has_collection_result=True)
    client.describe_result = {
        "fields": [
            {
                "name": "id",
                "is_primary": True,
                "auto_id": True,
                "params": {},
            }
        ]
    }
    monkeypatch.setattr(repository_module, "get_milvus_client", lambda: client)

    assert repository_module.collection_uses_auto_id("demo_kb") is True


def test_insert_embeddings_omits_manual_id_for_auto_id_collection(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """
    测试目的：验证旧 auto_id collection 写入时不会携带自定义主键字段。
    预期结果：insert payload 中不包含 id。
    """
    client = _FakeMilvusClient(has_collection_result=True)
    client.describe_result = {
        "fields": [
            {
                "name": "id",
                "is_primary": True,
                "auto_id": True,
                "params": {},
            },
            {
                "name": "embedding",
                "params": {"dim": 1024},
            },
        ]
    }
    monkeypatch.setattr(repository_module, "get_milvus_client", lambda: client)

    repository_module.insert_embeddings(
        knowledge_name="demo_kb",
        document_id=99,
        embeddings=[[0.1, 0.2]],
        texts=["A"],
        created_at_ts=1234567890,
    )

    rows = client.insert_calls[0]["data"]
    assert "id" not in rows[0]


def test_update_document_chunk_status_returns_new_primary_id_for_auto_id_collection(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """
    测试目的：验证旧 auto_id collection 更新状态后会返回 Milvus 新主键。
    预期结果：返回 upsert 结果中的 ids[0]。
    """
    client = _FakeMilvusClient(has_collection_result=True)
    client.describe_result = {
        "fields": [
            {
                "name": "id",
                "is_primary": True,
                "auto_id": True,
                "params": {},
            }
        ]
    }
    client.get_result = [
        {
            "id": 101,
            "document_id": 42,
            "chunk_index": 3,
            "content": "demo",
            "char_count": 4,
            "embedding": [0.1, 0.2],
            "chunk_size": 500,
            "chunk_overlap": 0,
            "status": repository_module.KNOWLEDGE_STATUS_ENABLED,
            "source_hash": "hash-1",
            "created_at_ts": 1234567890,
        }
    ]
    client.upsert_result = {"ids": [202]}
    monkeypatch.setattr(repository_module, "get_milvus_client", lambda: client)

    current_vector_id = repository_module.update_document_chunk_status(
        knowledge_name="demo_kb",
        primary_id=101,
        status=repository_module.KNOWLEDGE_STATUS_DISABLED,
    )

    assert current_vector_id == 202
