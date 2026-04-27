from __future__ import annotations

from pymilvus import (
    CollectionSchema,
    DataType,
    FieldSchema,
    MilvusClient,
    exceptions as milvus_exceptions,
)

from app.core.codes import ResponseCode
from app.core.database import get_milvus_client
from app.core.exception.exceptions import ServiceException
from app.utils.snowflake import generate_snowflake_id

DEFAULT_CONTENT_MAX_LENGTH = 65535  # 内容字段最大长度
DEFAULT_SOURCE_HASH_MAX_LENGTH = 64  # 源文本哈希字段最大长度（sha256）
KNOWLEDGE_STATUS_ENABLED = 0  # 知识库启用状态
KNOWLEDGE_STATUS_DISABLED = 1  # 知识库禁用状态
DEFAULT_KNOWLEDGE_STATUS = KNOWLEDGE_STATUS_ENABLED
DEFAULT_VECTOR_INDEX_TYPE = "AUTOINDEX"
DEFAULT_VECTOR_METRIC_TYPE = "COSINE"
COUNT_FALLBACK_LIMIT = 1000
ALLOWED_KNOWLEDGE_STATUS_VALUES = {
    KNOWLEDGE_STATUS_ENABLED,
    KNOWLEDGE_STATUS_DISABLED,
}
DOCUMENT_CHUNK_OUTPUT_FIELDS = [
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


def _validate_knowledge_status(status: int) -> int:
    """
    功能描述:
        校验知识切片状态值，仅允许 0（启用）或 1（禁用）。

    参数说明:
        status (int): 待校验状态值。

    返回值:
        int: 校验通过后的状态值。

    异常说明:
        ServiceException: 状态值不在允许范围时抛出。
    """
    try:
        normalized_status = int(status)
    except (TypeError, ValueError) as exc:
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message="status 只能为 0 或 1",
        ) from exc
    if normalized_status not in ALLOWED_KNOWLEDGE_STATUS_VALUES:
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message="status 只能为 0 或 1",
        )
    return normalized_status


def _build_index_params(client: MilvusClient):
    """
    功能描述:
        构建 Milvus 集合索引参数，包含标量过滤索引与向量检索索引。

    参数说明:
        client (MilvusClient): Milvus 客户端实例。

    返回值:
        Any: Milvus 索引参数对象（由客户端 SDK 提供）。

    异常说明:
        由调用方统一处理底层异常。
    """
    index_params = client.prepare_index_params()
    index_params.add_index(field_name="document_id", index_type="STL_SORT")
    index_params.add_index(
        field_name="embedding",
        index_type=DEFAULT_VECTOR_INDEX_TYPE,
        metric_type=DEFAULT_VECTOR_METRIC_TYPE,
    )
    return index_params


def _field_value(field: dict | object, key: str, default=None):
    """兼容 dict / 对象两种 schema 字段结构读取属性。"""
    if isinstance(field, dict):
        return field.get(key, default)
    return getattr(field, key, default)


def _coerce_bool(value: object) -> bool:
    """将 schema 中可能出现的布尔表达统一转换为 bool。"""
    if isinstance(value, bool):
        return value
    if isinstance(value, str):
        return value.strip().lower() == "true"
    if isinstance(value, (int, float)):
        return bool(value)
    return False


def _get_primary_field(fields: list[dict] | list[object]) -> dict | object | None:
    """从 collection fields 中定位主键字段定义。"""
    for field in fields:
        if _coerce_bool(_field_value(field, "is_primary")):
            return field
        if _coerce_bool(_field_value(field, "primary_key")):
            return field
        if _coerce_bool(_field_value(field, "primaryKey")):
            return field
    for field in fields:
        if _field_value(field, "name") == "id":
            return field
    return None


def _extract_mutation_primary_key(
        mutation_result,
        *,
        fallback_id: int | None = None,
) -> int:
    """从 insert/upsert 结果中提取主键，必要时回退到调用方提供的 ID。"""
    if isinstance(mutation_result, dict):
        ids = mutation_result.get("ids") or mutation_result.get("primary_keys") or []
        if ids:
            return int(ids[0])

    if hasattr(mutation_result, "primary_keys"):
        primary_keys = mutation_result.primary_keys
        if primary_keys:
            return int(primary_keys[0])

    if fallback_id is not None:
        return int(fallback_id)

    raise ServiceException(
        code=ResponseCode.OPERATION_FAILED,
        message="无法从 Milvus 写入结果中提取主键 ID",
    )


def _collection_uses_auto_id(client: MilvusClient, knowledge_name: str) -> bool:
    """读取 collection schema，判断主键是否仍使用 auto_id。"""
    try:
        collection_desc = client.describe_collection(knowledge_name)
    except milvus_exceptions.MilvusException as exc:
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message=f"读取知识库 schema 失败: {exc}",
        ) from exc

    fields = collection_desc.get("fields") or []
    primary_field = _get_primary_field(fields)
    if primary_field is None:
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message="知识库 schema 不包含主键字段",
        )

    auto_id = _field_value(primary_field, "auto_id")
    if auto_id is None:
        params = _field_value(primary_field, "params") or {}
        if isinstance(params, dict):
            auto_id = params.get("auto_id")
    return _coerce_bool(auto_id)


def collection_uses_auto_id(knowledge_name: str) -> bool:
    """对外暴露 collection 主键模式，兼容新旧 schema。"""
    client = get_milvus_client()
    return _collection_uses_auto_id(client, knowledge_name)


def _upsert_document_chunk_row(
        *,
        client: MilvusClient,
        knowledge_name: str,
        row: dict,
        error_message: str,
) -> int:
    """执行文档切片 upsert，并返回当前有效主键 ID。"""
    try:
        result = client.upsert(
            collection_name=knowledge_name,
            data=[row],
        )
    except milvus_exceptions.MilvusException as exc:
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message=f"{error_message}: {exc}",
        ) from exc

    fallback_id = int(row.get("id") or 0)
    if fallback_id <= 0:
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message="文档切片缺少有效主键 ID",
        )
    if _collection_uses_auto_id(client, knowledge_name):
        return _extract_mutation_primary_key(result, fallback_id=fallback_id)
    return fallback_id


def _build_collection_schema(embedding_dim: int, description: str) -> CollectionSchema:
    """
    功能描述:
        构建知识库向量集合 schema（标准 11 字段版本）。

    参数说明:
        embedding_dim (int): 向量维度。
        description (str): 集合描述文案。

    返回值:
        CollectionSchema: Milvus 集合 schema 对象。

    异常说明:
        无。
    """
    fields = [
        FieldSchema(
            name="id",
            dtype=DataType.INT64,
            description="主键（雪花 ID）",
            is_primary=True,
            auto_id=False,
        ),
        FieldSchema(
            name="document_id",
            dtype=DataType.INT64,
            description="文档ID",
        ),
        FieldSchema(
            name="chunk_index",
            dtype=DataType.INT64,
            description="文档内切片顺序号（从 1 开始）",
            nullable=True,
        ),
        FieldSchema(
            name="content",
            dtype=DataType.VARCHAR,
            description="chunk 文本内容",
            max_length=DEFAULT_CONTENT_MAX_LENGTH,
        ),
        FieldSchema(
            name="char_count",
            dtype=DataType.INT32,
            description="chunk 字符数量统计",
            nullable=True,
        ),
        FieldSchema(
            name="embedding",
            dtype=DataType.FLOAT_VECTOR,
            description="向量检索字段",
            dim=embedding_dim,
        ),
        FieldSchema(
            name="chunk_size",
            dtype=DataType.INT32,
            description="字符分块大小参数快照",
            nullable=True,
        ),
        FieldSchema(
            name="chunk_overlap",
            dtype=DataType.INT32,
            description="分段重叠字符数快照",
            nullable=True,
        ),
        FieldSchema(
            name="status",
            dtype=DataType.INT32,
            description="知识库状态（0=启用，1=禁用）",
        ),
        FieldSchema(
            name="source_hash",
            dtype=DataType.VARCHAR,
            description="源文本哈希（sha256）",
            max_length=DEFAULT_SOURCE_HASH_MAX_LENGTH,
            nullable=True,
        ),
        FieldSchema(
            name="created_at_ts",
            dtype=DataType.INT64,
            description="切片写入时间戳（毫秒）",
            nullable=True,
        ),
    ]
    return CollectionSchema(fields=fields, description=description or "")


def _extract_count(result: list[dict]) -> int | None:
    """
    功能描述:
        从 Milvus 查询结果中提取 count 聚合值。

    参数说明:
        result (list[dict]): Milvus 查询返回的记录列表。

    返回值:
        int | None:
            - 解析成功返回计数值；
            - 无法解析返回 None。

    异常说明:
        无。
    """
    if not result:
        return 0
    first = result[0]
    if isinstance(first, dict):
        if "count(*)" in first:
            return int(first["count(*)"])
        if "count" in first:
            return int(first["count"])
    return None


def _count_by_filter(client: MilvusClient, knowledge_name: str, filter_expr: str) -> int:
    """
    功能描述:
        按过滤条件统计集合记录数，优先使用 count 聚合，失败时回退分页计数。

    参数说明:
        client (MilvusClient): Milvus 客户端实例。
        knowledge_name (str): 集合名称。
        filter_expr (str): 过滤表达式。

    返回值:
        int: 命中记录总数。

    异常说明:
        ServiceException: 底层查询失败时抛出。
    """
    try:
        result = client.query(
            collection_name=knowledge_name,
            filter=filter_expr,
            output_fields=["count(*)"],
        )
        count_value = _extract_count(result)
        if count_value is not None:
            return count_value
    except milvus_exceptions.MilvusException as exc:
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message=f"查询知识库失败: {exc}",
        ) from exc

    total = 0
    offset = 0
    while True:
        try:
            batch = client.query(
                collection_name=knowledge_name,
                filter=filter_expr,
                output_fields=["id"],
                limit=COUNT_FALLBACK_LIMIT,
                offset=offset,
            )
        except milvus_exceptions.MilvusException as exc:
            raise ServiceException(
                code=ResponseCode.OPERATION_FAILED,
                message=f"查询知识库失败: {exc}",
            ) from exc
        if not batch:
            break
        batch_len = len(batch)
        total += batch_len
        if batch_len < COUNT_FALLBACK_LIMIT:
            break
        offset += batch_len
    return total


def create_collection(knowledge_name: str, embedding_dim: int, description: str) -> None:
    """
    功能描述:
        创建知识库对应的 Milvus 集合并初始化索引。

    参数说明:
        knowledge_name (str): 集合名称。
        embedding_dim (int): 向量维度。
        description (str): 集合描述。

    返回值:
        None: 创建成功无返回值。

    异常说明:
        ServiceException:
            - 集合已存在时抛出；
            - Milvus 创建失败时抛出。
    """
    client = get_milvus_client()
    try:
        if client.has_collection(knowledge_name):
            raise ServiceException(
                code=ResponseCode.OPERATION_FAILED,
                message="knowledge 已存在",
            )
        schema = _build_collection_schema(embedding_dim, description)
        index_params = _build_index_params(client)
        client.create_collection(
            collection_name=knowledge_name,
            schema=schema,
            index_params=index_params,
        )
    except milvus_exceptions.MilvusException as exc:
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message=f"创建 knowledge 失败: {exc}",
        ) from exc


def delete_collection(knowledge_name: str) -> None:
    """
    功能描述:
        删除知识库对应的 Milvus 集合。

    参数说明:
        knowledge_name (str): 集合名称。

    返回值:
        None: 删除成功无返回值。

    异常说明:
        ServiceException:
            - 集合不存在时抛出；
            - Milvus 删除失败时抛出。
    """
    client = get_milvus_client()
    try:
        if not client.has_collection(knowledge_name):
            raise ServiceException(
                code=ResponseCode.NOT_FOUND,
                message="knowledge 不存在",
            )
        client.drop_collection(knowledge_name)
    except milvus_exceptions.MilvusException as exc:
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message=f"删除 knowledge 失败: {exc}",
        ) from exc


def ensure_collection_exists(knowledge_name: str) -> None:
    """
    功能描述:
        校验指定知识库集合是否存在。

    参数说明:
        knowledge_name (str): 集合名称。

    返回值:
        None: 集合存在时无返回值。

    异常说明:
        ServiceException:
            - 集合不存在时抛出；
            - Milvus 查询失败时抛出。
    """
    client = get_milvus_client()
    try:
        if not client.has_collection(knowledge_name):
            raise ServiceException(
                code=ResponseCode.NOT_FOUND,
                message="知识库不存在",
            )
    except milvus_exceptions.MilvusException as exc:
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message=f"查询知识库失败: {exc}",
        ) from exc


def _normalize_load_state(result: dict) -> dict:
    """
    功能描述:
        规范化 Milvus `get_load_state` 返回结果，统一输出可序列化字段。

    参数说明:
        result (dict): SDK 返回的 load_state 结果字典。

    返回值:
        dict: 标准化后的状态字典，包含 `load_state`，可选 `progress`。

    异常说明:
        无。
    """
    state_value = result.get("state")
    if hasattr(state_value, "name"):
        load_state = str(state_value.name)
    elif isinstance(state_value, str):
        load_state = state_value
    else:
        load_state = str(state_value)

    normalized = {"load_state": load_state}
    if "progress" in result:
        normalized["progress"] = result["progress"]
    return normalized


def load_collection_state(knowledge_name: str) -> dict:
    """
    功能描述:
        加载指定集合到查询节点，并返回当前加载状态。

    参数说明:
        knowledge_name (str): 集合名称。

    返回值:
        dict: 包含 `knowledge_name` 与 `load_state`，必要时包含 `progress`。

    异常说明:
        ServiceException:
            - 集合不存在时抛出；
            - Milvus 调用失败时抛出。
    """
    client = get_milvus_client()
    try:
        if not client.has_collection(knowledge_name):
            raise ServiceException(
                code=ResponseCode.NOT_FOUND,
                message="知识库不存在",
            )
        client.load_collection(collection_name=knowledge_name)
        state_result = client.get_load_state(collection_name=knowledge_name)
    except milvus_exceptions.MilvusException as exc:
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message=f"启用知识库失败: {exc}",
        ) from exc

    return {
        "knowledge_name": knowledge_name,
        **_normalize_load_state(state_result),
    }


def release_collection_state(knowledge_name: str) -> dict:
    """
    功能描述:
        释放指定集合在查询节点中的加载状态，并返回当前加载状态。

    参数说明:
        knowledge_name (str): 集合名称。

    返回值:
        dict: 包含 `knowledge_name` 与 `load_state`，必要时包含 `progress`。

    异常说明:
        ServiceException:
            - 集合不存在时抛出；
            - Milvus 调用失败时抛出。
    """
    client = get_milvus_client()
    try:
        if not client.has_collection(knowledge_name):
            raise ServiceException(
                code=ResponseCode.NOT_FOUND,
                message="知识库不存在",
            )
        client.release_collection(collection_name=knowledge_name)
        state_result = client.get_load_state(collection_name=knowledge_name)
    except milvus_exceptions.MilvusException as exc:
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message=f"关闭知识库失败: {exc}",
        ) from exc

    return {
        "knowledge_name": knowledge_name,
        **_normalize_load_state(state_result),
    }


def insert_embeddings(
        knowledge_name: str,
        document_id: int,
        embeddings: list[list[float]],
        texts: list[str],
        *,
        start_chunk_index: int = 1,
        chunk_size: int | None = None,
        chunk_overlap: int | None = None,
        source_hash: str | None = None,
        char_counts: list[int] | None = None,
        created_at_ts: int | None = None,
) -> None:
    """
    功能描述:
        将文档向量批量写入 Milvus。

    参数说明:
        knowledge_name (str): 集合名称。
        document_id (int): 文档 ID。
        embeddings (list[list[float]]): 向量列表。
        texts (list[str]): 文本列表。
        start_chunk_index (int): 本批首个 chunk 序号（从 1 开始）。
        chunk_size (int | None): 字符分块大小快照，默认值为 None。
        chunk_overlap (int | None): 重叠字符数快照，默认值为 None。
        source_hash (str | None): 源文本哈希，默认值为 None。
        char_counts (list[int] | None): 每条文本字符数列表，默认值为 None。
        created_at_ts (int | None): 写入时间戳（毫秒），默认值为 None。

    返回值:
        None: 写入成功无返回值。

    异常说明:
        ServiceException:
            - 向量数量与文本数量不一致时抛出；
            - Milvus 写入失败时抛出。
    """
    if len(embeddings) != len(texts):
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message="向量数量与文本数量不一致",
        )
    if char_counts is not None and len(char_counts) != len(texts):
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message="字符统计数量与文本数量不一致",
        )
    if not embeddings:
        return

    client = get_milvus_client()
    use_auto_id = _collection_uses_auto_id(client, knowledge_name)
    data = [
        {
            "document_id": document_id,
            "chunk_index": start_chunk_index + index,
            "embedding": embedding,
            "content": text,
            "char_count": (
                char_counts[index]
                if char_counts is not None
                else len(text)
            ),
            "chunk_size": chunk_size,
            "chunk_overlap": chunk_overlap,
            "status": DEFAULT_KNOWLEDGE_STATUS,
            "source_hash": source_hash,
            "created_at_ts": created_at_ts,
        }
        for index, (embedding, text) in enumerate(
            zip(embeddings, texts, strict=True)
        )
    ]
    if not use_auto_id:
        for row in data:
            row["id"] = generate_snowflake_id()

    try:
        client.insert(collection_name=knowledge_name, data=data)
    except milvus_exceptions.MilvusException as exc:
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message=f"写入向量失败: {exc}",
        ) from exc


def get_collection_embedding_dim(knowledge_name: str) -> int:
    """
    功能描述:
        读取集合中 embedding 字段的向量维度配置。

    参数说明:
        knowledge_name (str): 集合名称。

    返回值:
        int: embedding 字段维度值。

    异常说明:
        ServiceException:
            - 集合描述失败时抛出；
            - 未找到 embedding 字段或维度非法时抛出。
    """
    client = get_milvus_client()
    try:
        collection_desc = client.describe_collection(knowledge_name)
    except milvus_exceptions.MilvusException as exc:
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message=f"读取知识库 schema 失败: {exc}",
        ) from exc

    fields = collection_desc.get("fields") or []
    for field in fields:
        if field.get("name") != "embedding":
            continue
        params = field.get("params") or {}
        dim_value = params.get("dim")
        if dim_value is None:
            dim_value = field.get("dim")
        if dim_value is None:
            raise ServiceException(
                code=ResponseCode.OPERATION_FAILED,
                message="知识库 embedding 字段缺少 dim 配置",
            )
        try:
            embedding_dim = int(dim_value)
        except (TypeError, ValueError) as exc:
            raise ServiceException(
                code=ResponseCode.OPERATION_FAILED,
                message=f"知识库 embedding 维度非法: {dim_value}",
            ) from exc
        if embedding_dim <= 0:
            raise ServiceException(
                code=ResponseCode.OPERATION_FAILED,
                message=f"知识库 embedding 维度非法: {embedding_dim}",
            )
        return embedding_dim

    raise ServiceException(
        code=ResponseCode.OPERATION_FAILED,
        message="知识库 schema 不包含 embedding 字段",
    )


def list_document_chunks(
        knowledge_name: str,
        document_id: int,
        page_num: int,
        page_size: int,
) -> tuple[list[dict], int]:
    """
    功能描述:
        分页查询指定文档切片记录。

    参数说明:
        knowledge_name (str): 集合名称。
        document_id (int): 文档 ID。
        page_num (int): 页码（从 1 开始）。
        page_size (int): 每页数量。

    返回值:
        tuple[list[dict], int]:
            - 第 1 项: 当前页切片列表；
            - 第 2 项: 总条数。

    异常说明:
        ServiceException:
            - page_num/page_size 非法时抛出；
            - Milvus 查询失败时抛出。
    """
    if page_num <= 0 or page_size <= 0:
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message="page_num 和 page_size 必须大于 0",
        )

    client = get_milvus_client()
    filter_expr = f"document_id == {document_id}"
    total = _count_by_filter(client, knowledge_name, filter_expr)
    offset = (page_num - 1) * page_size
    try:
        rows = client.query(
            collection_name=knowledge_name,
            filter=filter_expr,
            output_fields=[
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
            ],
            limit=page_size,
            offset=offset,
        )
    except milvus_exceptions.MilvusException as exc:
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message=f"查询知识库失败: {exc}",
        ) from exc
    sorted_rows = sorted(
        rows or [],
        key=lambda row: int(row.get("chunk_index") or 0),
    )
    return sorted_rows, total


def count_document_chunks(
        knowledge_name: str,
        document_id: int,
        *,
        status: int | None = None,
) -> int:
    """
    功能描述:
        统计指定文档在集合中的切片数量。

    参数说明:
        knowledge_name (str): 集合名称。
        document_id (int): 文档 ID。
        status (int | None): 可选状态过滤；传 None 表示不过滤状态。

    返回值:
        int: 命中切片总数。

    异常说明:
        ServiceException: Milvus 查询失败时抛出。
    """
    client = get_milvus_client()
    filter_expr = f"document_id == {document_id}"
    if status is not None:
        filter_expr = f"{filter_expr} and status == {int(status)}"
    return _count_by_filter(client, knowledge_name, filter_expr)


def delete_document_chunks(knowledge_name: str, document_ids: list[int]) -> None:
    """
    功能描述:
        批量删除指定文档在集合中的全部切片记录。

    参数说明:
        knowledge_name (str): 集合名称。
        document_ids (list[int]): 文档 ID 列表。

    返回值:
        None: 删除成功无返回值。

    异常说明:
        ServiceException: Milvus 删除失败时抛出。
    """
    client = get_milvus_client()
    joined_ids = ", ".join(str(document_id) for document_id in document_ids)
    filter_expr = f"document_id in [{joined_ids}]"
    try:
        client.delete(
            collection_name=knowledge_name,
            filter=filter_expr,
        )
    except milvus_exceptions.MilvusException as exc:
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message=f"删除文档切片失败: {exc}",
        ) from exc


def update_document_chunk_status(
        knowledge_name: str,
        primary_id: int,
        status: int,
) -> int:
    """
    功能描述:
        按向量主键更新指定切片记录的状态字段。

    参数说明:
        knowledge_name (str): 集合名称。
        primary_id (int): Milvus 主键 ID。
        status (int): 状态值，仅允许 0 或 1。

    返回值:
        int: 更新后的当前有效主键 ID。

    异常说明:
        ServiceException:
            - 状态值非法时抛出；
            - 记录不存在时抛出；
            - Milvus 读写失败时抛出。
    """
    normalized_status = _validate_knowledge_status(status)
    client = get_milvus_client()
    try:
        rows = client.get(
            collection_name=knowledge_name,
            ids=primary_id,
            output_fields=DOCUMENT_CHUNK_OUTPUT_FIELDS,
        )
    except milvus_exceptions.MilvusException as exc:
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message=f"读取文档切片失败: {exc}",
        ) from exc

    if not rows:
        raise ServiceException(
            code=ResponseCode.NOT_FOUND,
            message="向量记录不存在",
        )

    persisted_row = dict(rows[0])
    persisted_row["id"] = int(persisted_row.get("id") or primary_id)
    persisted_row["status"] = normalized_status

    return _upsert_document_chunk_row(
        client=client,
        knowledge_name=knowledge_name,
        row=persisted_row,
        error_message="更新文档状态失败",
    )


def update_document_chunk_status_by_primary_id(
        primary_id: int,
        status: int,
) -> tuple[str, int]:
    """
    功能描述:
        遍历知识库集合，按向量主键更新指定切片记录的状态字段。

    参数说明:
        primary_id (int): Milvus 主键 ID。
        status (int): 状态值，仅允许 0 或 1。

    返回值:
        tuple[str, int]:
            - 第 1 项: 命中并完成更新的集合名称；
            - 第 2 项: 更新后的当前有效主键 ID。

    异常说明:
        ServiceException:
            - 状态值非法时抛出；
            - 记录不存在时抛出；
            - 同一主键命中多个集合时抛出；
            - Milvus 读写失败时抛出。
    """
    normalized_status = _validate_knowledge_status(status)
    client = get_milvus_client()

    try:
        collection_names = list(client.list_collections())
    except milvus_exceptions.MilvusException as exc:
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message=f"读取知识库列表失败: {exc}",
        ) from exc

    matched_rows: list[tuple[str, dict]] = []
    for knowledge_name in collection_names:
        try:
            rows = client.get(
                collection_name=knowledge_name,
                ids=primary_id,
                output_fields=DOCUMENT_CHUNK_OUTPUT_FIELDS,
            )
        except milvus_exceptions.MilvusException as exc:
            raise ServiceException(
                code=ResponseCode.OPERATION_FAILED,
                message=f"读取文档切片失败: {exc}",
            ) from exc
        if rows:
            matched_rows.append((knowledge_name, dict(rows[0])))

    if not matched_rows:
        raise ServiceException(
            code=ResponseCode.NOT_FOUND,
            message="向量记录不存在",
        )
    if len(matched_rows) > 1:
        collection_names = ", ".join(name for name, _ in matched_rows)
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message=f"向量主键命中多个知识库: {collection_names}",
        )

    knowledge_name, persisted_row = matched_rows[0]
    persisted_row["id"] = int(persisted_row.get("id") or primary_id)
    persisted_row["status"] = normalized_status

    current_primary_id = _upsert_document_chunk_row(
        client=client,
        knowledge_name=knowledge_name,
        row=persisted_row,
        error_message="更新文档状态失败",
    )

    return knowledge_name, current_primary_id
