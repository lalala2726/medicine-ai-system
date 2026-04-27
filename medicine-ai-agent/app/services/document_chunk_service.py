from __future__ import annotations

import hashlib
import os
from dataclasses import dataclass
from pathlib import Path
from time import sleep, time
from typing import cast

from loguru import logger
from pymilvus import exceptions as milvus_exceptions

from app.core.codes import ResponseCode
from app.core.config_sync import create_agent_embedding_client
from app.core.database import get_milvus_client
from app.core.exception.exceptions import ServiceException
from app.core.mq.log import ImportStage, mq_log
from app.core.mq.version_store import (
    get_chunk_rebuild_latest_version as get_chunk_edit_latest_version,
)
from app.rag.chunking import SplitChunk, split_excel_text, split_text
from app.rag.file_loader import parse_downloaded_file, validate_url_extension
from app.rag.file_loader.parsers.excel_parser import parse_rows as parse_excel_rows
from app.rag.file_loader.types import FileKind
from app.repositories import vector_repository
from app.schemas.knowledge_import import (
    ImportChunk,
    ImportSingleFileFailedResult,
    ImportSingleFileResult,
    ImportSingleFileSuccessResult,
)
from app.utils.file_utils import FileUtils
from app.utils.snowflake import generate_snowflake_id
from app.utils.token_utills import TokenUtils

# 单切片向量化允许的最大 token 数，超限直接拒绝。
EMBED_MAX_TOKEN_SIZE = 8192

# Embedding 服务单次批量向量化允许的最大文本条数。
EMBED_MAX_BATCH_SIZE = 10

# 文档导入默认切片和入库参数。
DEFAULT_CHUNK_SIZE = 500
DEFAULT_CHUNK_OVERLAP = 0
DEFAULT_VECTOR_BATCH_SIZE = EMBED_MAX_BATCH_SIZE
DEFAULT_INSERT_VERIFY_MAX_RETRIES = 5
DEFAULT_INSERT_VERIFY_INTERVAL_SECONDS = 0.2

# 读取 Milvus 原始向量行时所需的字段列表。
CHUNK_REBUILD_OUTPUT_FIELDS = [
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


@dataclass(frozen=True)
class ChunkRebuildSuccessResult:
    """单切片重建成功结果。"""

    vector_id: int
    embedding_dim: int


class ChunkRebuildMessageStaleError(Exception):
    """表示切片重建任务已被更新版本替代。"""

    def __init__(self, *, vector_id: int, version: int, latest_version: int) -> None:
        self.vector_id = vector_id
        self.version = version
        self.latest_version = latest_version
        super().__init__(
            "切片重建任务已过期，"
            f"vector_id={vector_id}, message_version={version}, latest_version={latest_version}"
        )


@dataclass(frozen=True)
class ChunkAddSuccessResult:
    """手工新增切片成功结果。"""

    vector_id: int
    chunk_index: int
    embedding_dim: int


def create_embedding_client(*, embedding_model: str, embedding_dim: int):
    """创建向量模型客户端。

    Args:
        embedding_model: 向量模型名称。
        embedding_dim: 目标向量维度。

    Returns:
        Any: 可执行 ``embed_documents`` 的 embedding 客户端。

    Raises:
        ServiceException: 模型初始化失败时抛出。
    """
    try:
        return create_agent_embedding_client(
            model=embedding_model,
            dimensions=embedding_dim,
        )
    except Exception as exc:
        raise ServiceException(message=f"初始化向量模型失败: {exc}") from exc


def _resolve_file_kind_text(*, file_kind: FileKind) -> str:
    """将文件类型枚举转换为字符串值。

    Args:
        file_kind: 文件类型枚举。

    Returns:
        str: 文件类型对应的字符串值。
    """
    return cast(str, file_kind.value)


def embed_single_text(*, content: str, embedding_client) -> list[float]:
    """对单条切片内容执行向量化。

    Args:
        content: 待向量化的切片文本。
        embedding_client: embedding 模型客户端。

    Returns:
        list[float]: 生成后的单条向量。

    Raises:
        ServiceException: token 超限、向量化失败或结果为空时抛出。
    """
    token_count = TokenUtils.count_tokens(content)
    if token_count > EMBED_MAX_TOKEN_SIZE:
        raise ServiceException(
            message=(
                "文本超出最大 token 数限制，"
                f"最大 token 数为 {EMBED_MAX_TOKEN_SIZE}, 当前 token 数为 {token_count}"
            ),
        )
    try:
        embeddings = embedding_client.embed_documents([content])
    except Exception as exc:
        raise ServiceException(message=f"嵌入文本失败: {exc}") from exc
    if not embeddings:
        raise ServiceException(message="嵌入结果为空")
    return embeddings[0]


def embed_texts(
        texts: list[str],
        *,
        embedding_client,
) -> list[list[float]]:
    """对文本列表执行向量化。

    Args:
        texts: 待向量化文本列表。
        embedding_client: 向量模型客户端。

    Returns:
        list[list[float]]: 与 ``texts`` 顺序一致的向量列表。

    Raises:
        ServiceException: 文本超过 token 限制或向量化失败时抛出。
    """
    if not texts:
        return []

    token_counts = TokenUtils.count_tokens_list(texts)
    for token_count in token_counts:
        if token_count > EMBED_MAX_TOKEN_SIZE:
            raise ServiceException(
                message=(
                    "文本超出最大 token 数限制，"
                    f"最大 token 数为 {EMBED_MAX_TOKEN_SIZE}, 当前 token 数为 {token_count}"
                ),
            )

    try:
        return embedding_client.embed_documents(texts)
    except Exception as exc:  # pragma: no cover - 依赖外部模型 SDK
        raise ServiceException(message=f"嵌入文本失败: {exc}") from exc


def _split_batches(items: list, batch_size: int) -> list[list]:
    """将列表按批次大小切分。

    Args:
        items: 原始列表。
        batch_size: 单批最大条数。

    Returns:
        list[list]: 二维列表，每个子列表为一个批次。
    """
    result: list[list] = []
    for index in range(0, len(items), batch_size):
        result.append(items[index:index + batch_size])
    return result


def _resolve_vector_batch_size() -> int:
    """读取向量处理批次配置。

    Returns:
        int: 向量处理批次大小。

    Raises:
        ServiceException: 配置值不是正整数或超过 embedding 服务限制时抛出。
    """
    raw_value = (os.getenv("KNOWLEDGE_VECTOR_BATCH_SIZE") or "").strip()
    if not raw_value:
        return DEFAULT_VECTOR_BATCH_SIZE
    try:
        parsed = int(raw_value)
    except ValueError as exc:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message="KNOWLEDGE_VECTOR_BATCH_SIZE 必须是正整数",
        ) from exc
    if parsed <= 0:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message="KNOWLEDGE_VECTOR_BATCH_SIZE 必须大于 0",
        )
    if parsed > EMBED_MAX_BATCH_SIZE:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message=(
                "KNOWLEDGE_VECTOR_BATCH_SIZE 超出 embedding 服务单批上限，"
                f"最大允许值为 {EMBED_MAX_BATCH_SIZE}"
            ),
        )
    return parsed


def _build_source_hash(text: str) -> str:
    """计算原始文本的 sha256 哈希。

    Args:
        text: 原始文本。

    Returns:
        str: 十六进制哈希字符串。
    """
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def _download_file(url: str) -> tuple[str, Path]:
    """下载文件并返回文件名与本地路径。

    Args:
        url: 远程文件 URL。

    Returns:
        tuple[str, Path]: ``(filename, file_path)``。

    Raises:
        ServiceException: 下载失败或下载目录配置异常时抛出。
    """
    return FileUtils.download_file(url)


def _validate_file_not_empty(file_path: Path) -> None:
    """校验文件非空。

    Args:
        file_path: 本地文件路径。

    Raises:
        ServiceException: 文件大小为 0 时抛出。
    """
    if file_path.stat().st_size == 0:
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message="文件为空，无法导入",
        )


def _split_parsed_text(
        text: str,
        chunk_size: int,
        chunk_overlap: int,
) -> list[SplitChunk]:
    """对解析后的文本执行字符长度切片。

    Args:
        text: 完整文本。
        chunk_size: 字符分块大小。
        chunk_overlap: 分段重叠字符数。

    Returns:
        list[SplitChunk]: 切片结果列表；空白文本返回空列表。
    """
    if not text.strip():
        return []
    return split_text(text, chunk_size, chunk_overlap)


def _ensure_document_visible_after_insert(
        *,
        knowledge_name: str,
        document_id: int,
        expected_count: int,
) -> None:
    """校验文档切片写入后在向量库中可见。

    Args:
        knowledge_name: 知识库名称。
        document_id: 文档 ID。
        expected_count: 期望最小切片数量。

    Raises:
        ServiceException: 在重试窗口内仍未查询到期望数量时抛出。
    """
    if expected_count <= 0:
        return

    observed_count = 0
    for attempt in range(DEFAULT_INSERT_VERIFY_MAX_RETRIES):
        observed_count = vector_repository.count_document_chunks(
            knowledge_name=knowledge_name,
            document_id=document_id,
        )
        if observed_count >= expected_count:
            return
        if attempt < DEFAULT_INSERT_VERIFY_MAX_RETRIES - 1:
            sleep(DEFAULT_INSERT_VERIFY_INTERVAL_SECONDS)

    raise ServiceException(
        code=ResponseCode.OPERATION_FAILED,
        message=(
            "切片写入校验失败: "
            f"document_id={document_id}, expected={expected_count}, observed={observed_count}"
        ),
    )


def import_single_file(
        url: str,
        knowledge_name: str,
        document_id: int,
        embedding_model: str,
        chunk_size: int = DEFAULT_CHUNK_SIZE,
        chunk_overlap: int = DEFAULT_CHUNK_OVERLAP,
        task_uuid: str = "-",
) -> ImportSingleFileResult:
    """执行单个文件的完整导入流程：校验、下载、解析、切片、向量化、入库。

    Excel/CSV 文件使用行级合并切片（无重叠），其余文件使用字符长度切片。

    Args:
        url: 远程文件 URL。
        knowledge_name: 知识库名称。
        document_id: 文档 ID。
        embedding_model: 向量模型名称。
        chunk_size: 字符分块大小。
        chunk_overlap: 分段重叠字符数。
        task_uuid: 导入任务唯一标识，用于日志关联。

    Returns:
        ImportSingleFileResult: 成功时返回 ``ImportSingleFileSuccessResult``，
        失败时返回 ``ImportSingleFileFailedResult``。

    Raises:
        ServiceException: 参数校验失败时直接抛出。
    """
    vector_batch_size = _resolve_vector_batch_size()
    filename: str | None = None
    file_path: Path | None = None
    file_size: int | None = None
    embedding_dim = 0

    try:
        source_extension = validate_url_extension(url)

        vector_repository.ensure_collection_exists(knowledge_name=knowledge_name)
        embedding_dim = vector_repository.get_collection_embedding_dim(
            knowledge_name=knowledge_name,
        )
        embedding_client = create_embedding_client(
            embedding_model=embedding_model,
            embedding_dim=embedding_dim,
        )

        mq_log("import", ImportStage.DOWNLOAD_START, task_uuid, url=url)
        filename, file_path = _download_file(url)
        _validate_file_not_empty(file_path)
        file_size = file_path.stat().st_size
        mq_log("import", ImportStage.DOWNLOAD_DONE, task_uuid, filename=filename, size=file_size)

        parsed_document = parse_downloaded_file(
            file_path=file_path,
            source_url=url,
        )
        parsed_text = parsed_document.text or ""
        parsed_file_kind_text = _resolve_file_kind_text(file_kind=parsed_document.file_kind)
        mq_log(
            "import",
            ImportStage.PARSE_DONE,
            task_uuid,
            filename=filename,
            file_kind=parsed_file_kind_text,
            text_length=len(parsed_text),
        )

        # Excel/CSV → 行级合并切片；其余 → 字符长度切片
        if parsed_document.file_kind == FileKind.EXCEL:
            rows = parse_excel_rows(file_path)
            chunks = split_excel_text(rows, max_chunk_size=chunk_size)
        else:
            chunks = _split_parsed_text(parsed_text, chunk_size, chunk_overlap)
        mq_log(
            "import",
            ImportStage.CHUNK_DONE,
            task_uuid,
            chunk_count=len(chunks),
            chunk_size=chunk_size,
        )

        source_hash = _build_source_hash(parsed_text)
        vector_count = 0
        insert_batches = 0
        batch_chunks_list = _split_batches(chunks, vector_batch_size)
        total_batches = len(batch_chunks_list)

        for batch_index, batch_chunks in enumerate(batch_chunks_list, start=1):
            batch_texts = [chunk.text for chunk in batch_chunks]
            batch_embeddings = embed_texts(
                batch_texts,
                embedding_client=embedding_client,
            )
            mq_log(
                "import",
                ImportStage.EMBED_BATCH,
                task_uuid,
                batch=f"{batch_index}/{total_batches}",
                texts=len(batch_texts),
            )

            vector_repository.insert_embeddings(
                knowledge_name=knowledge_name,
                document_id=document_id,
                embeddings=batch_embeddings,
                texts=batch_texts,
                start_chunk_index=vector_count + 1,
                chunk_size=chunk_size,
                chunk_overlap=chunk_overlap,
                source_hash=source_hash,
                char_counts=[chunk.stats.char_count for chunk in batch_chunks],
                created_at_ts=int(time() * 1000),
            )
            vector_count += len(batch_chunks)
            insert_batches += 1

        _ensure_document_visible_after_insert(
            knowledge_name=knowledge_name,
            document_id=document_id,
            expected_count=vector_count,
        )

        mq_log(
            "import",
            ImportStage.INSERT_DONE,
            task_uuid,
            vector_count=vector_count,
            insert_batches=insert_batches,
        )
        mq_log(
            "import",
            ImportStage.COMPLETED,
            task_uuid,
            filename=filename,
            chunk_count=len(chunks),
            vector_count=vector_count,
        )

        return ImportSingleFileSuccessResult(
            file_url=url,
            filename=filename,
            file_size=file_size,
            source_extension=source_extension,
            file_kind=parsed_file_kind_text,
            mime_type=parsed_document.mime_type,
            text_length=len(parsed_text),
            chunk_count=len(chunks),
            vector_count=vector_count,
            insert_batches=insert_batches,
            embedding_model=embedding_model,
            embedding_dim=embedding_dim,
            chunks=[ImportChunk.from_split_chunk(chunk) for chunk in chunks],
        )
    except Exception as exc:
        mq_log(
            "import",
            ImportStage.FAILED,
            task_uuid,
            url=url,
            filename=filename,
            error=str(exc),
        )
        return ImportSingleFileFailedResult(
            file_url=url,
            filename=filename,
            file_size=file_size,
            error=str(exc),
            embedding_model=embedding_model,
            embedding_dim=embedding_dim,
        )
    finally:
        if file_path is not None:
            try:
                file_path.unlink(missing_ok=True)
            except OSError as exc:
                logger.warning(
                    "Failed to cleanup downloaded temp file: task_uuid={} path={} error={}",
                    task_uuid,
                    file_path,
                    exc,
                )


def _ensure_latest_chunk_edit_version(*, vector_id: int, version: int) -> None:
    """在写入前确认当前任务仍然是该切片的最新版本。

    Args:
        vector_id: Milvus 向量主键 ID。
        version: 当前任务版本号。

    Raises:
        ChunkRebuildMessageStaleError: 当前任务已落后于 Redis 最新版本时抛出。
    """
    latest_version = get_chunk_edit_latest_version(vector_id=vector_id)
    if latest_version is None or version >= latest_version:
        return
    raise ChunkRebuildMessageStaleError(
        vector_id=vector_id,
        version=version,
        latest_version=latest_version,
    )


def _get_next_chunk_index(client, knowledge_name: str, document_id: int) -> int:
    """计算该文档下一个可用的 ``chunk_index``。

    Args:
        client: Milvus 客户端实例。
        knowledge_name: 集合名称。
        document_id: 文档 ID。

    Returns:
        int: 下一个可用的 ``chunk_index``，从 1 开始。

    Raises:
        ServiceException: 查询文档切片失败时抛出。
    """
    try:
        rows = client.query(
            collection_name=knowledge_name,
            filter=f"document_id == {document_id}",
            output_fields=["chunk_index"],
            limit=16384,
        )
    except milvus_exceptions.MilvusException as exc:
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message=f"查询文档切片失败: {exc}",
        ) from exc

    if not rows:
        return 1

    max_index = max(int(row.get("chunk_index") or 0) for row in rows)
    return max_index + 1


def _extract_primary_key(
        insert_result,
        *,
        fallback_id: int | None = None,
) -> int:
    """从 Milvus insert/upsert 结果中提取主键 ID。

    Args:
        insert_result: Milvus insert/upsert 返回的结果对象。
        fallback_id: 当 SDK 未显式返回主键时的兜底 ID。

    Returns:
        int: 新插入记录的主键 ID。

    Raises:
        ServiceException: 无法提取主键 ID 时抛出。
    """
    if isinstance(insert_result, dict):
        ids = insert_result.get("ids") or insert_result.get("primary_keys") or []
        if ids:
            return int(ids[0])

    if hasattr(insert_result, "primary_keys"):
        pks = insert_result.primary_keys
        if pks:
            return int(pks[0])

    if fallback_id is not None:
        return int(fallback_id)

    raise ServiceException(
        code=ResponseCode.OPERATION_FAILED,
        message="无法从 Milvus 写入结果中提取主键 ID",
    )


def rebuild_document_chunk(
        *,
        knowledge_name: str,
        document_id: int,
        vector_id: int,
        version: int,
        content: str,
        embedding_model: str,
) -> ChunkRebuildSuccessResult:
    """按向量主键重建单个文档切片的内容与向量。

    Args:
        knowledge_name: 目标知识库名称。
        document_id: 业务文档 ID。
        vector_id: Milvus 向量主键 ID。
        version: 当前切片编辑版本号。
        content: 新的切片内容。
        embedding_model: 本次任务使用的向量模型名称。

    Returns:
        ChunkRebuildSuccessResult: 重建成功后的摘要结果。

    Raises:
        ServiceException: collection 不存在、向量记录不存在、字段校验失败、
            embedding 失败或 Milvus 写入失败时抛出。
        ChunkRebuildMessageStaleError: 写入前发现当前任务已被更新版本替代时抛出。
    """
    vector_repository.ensure_collection_exists(knowledge_name=knowledge_name)
    embedding_dim = vector_repository.get_collection_embedding_dim(
        knowledge_name=knowledge_name,
    )
    embedding_client = create_embedding_client(
        embedding_model=embedding_model,
        embedding_dim=embedding_dim,
    )
    client = get_milvus_client()
    use_auto_id = vector_repository.collection_uses_auto_id(
        knowledge_name=knowledge_name,
    )

    try:
        rows = client.get(
            collection_name=knowledge_name,
            ids=vector_id,
            output_fields=CHUNK_REBUILD_OUTPUT_FIELDS,
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
    persisted_document_id = int(persisted_row.get("document_id") or 0)
    if persisted_document_id != document_id:
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message="向量记录与文档ID不匹配",
        )

    embedding = embed_single_text(
        content=content,
        embedding_client=embedding_client,
    )

    updated_row = dict(persisted_row)
    updated_row["id"] = int(updated_row.get("id") or vector_id)
    updated_row["document_id"] = persisted_document_id
    updated_row["content"] = content
    updated_row["char_count"] = len(content)
    updated_row["embedding"] = embedding
    updated_row["source_hash"] = None
    updated_row["created_at_ts"] = int(time() * 1000)

    _ensure_latest_chunk_edit_version(vector_id=vector_id, version=version)

    try:
        upsert_result = client.upsert(
            collection_name=knowledge_name,
            data=[updated_row],
        )
    except milvus_exceptions.MilvusException as exc:
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message=f"更新文档切片失败: {exc}",
        ) from exc

    current_vector_id = (
        _extract_primary_key(
            upsert_result,
            fallback_id=int(updated_row["id"]),
        )
        if use_auto_id
        else int(updated_row["id"])
    )

    return ChunkRebuildSuccessResult(
        vector_id=current_vector_id,
        embedding_dim=embedding_dim,
    )


def add_document_chunk(
        *,
        knowledge_name: str,
        document_id: int,
        content: str,
        embedding_model: str,
) -> ChunkAddSuccessResult:
    """向量化并插入一条新的文档切片。

    Args:
        knowledge_name: 目标知识库名称。
        document_id: 业务文档 ID。
        content: 新增切片内容。
        embedding_model: 本次任务使用的向量模型名称。

    Returns:
        ChunkAddSuccessResult: 新增成功后的摘要结果，包含向量主键和切片序号。

    Raises:
        ServiceException: collection 不存在、embedding 失败或 Milvus 写入失败时抛出。
    """
    vector_repository.ensure_collection_exists(knowledge_name=knowledge_name)
    embedding_dim = vector_repository.get_collection_embedding_dim(
        knowledge_name=knowledge_name,
    )
    embedding_client = create_embedding_client(
        embedding_model=embedding_model,
        embedding_dim=embedding_dim,
    )
    client = get_milvus_client()
    use_auto_id = vector_repository.collection_uses_auto_id(
        knowledge_name=knowledge_name,
    )

    embedding = embed_single_text(
        content=content,
        embedding_client=embedding_client,
    )

    chunk_index = _get_next_chunk_index(client, knowledge_name, document_id)

    new_row = {
        "document_id": document_id,
        "chunk_index": chunk_index,
        "content": content,
        "char_count": len(content),
        "embedding": embedding,
        "chunk_size": None,
        "chunk_overlap": None,
        "status": 0,
        "source_hash": None,
        "created_at_ts": int(time() * 1000),
    }
    generated_vector_id: int | None = None
    if not use_auto_id:
        generated_vector_id = generate_snowflake_id()
        new_row["id"] = generated_vector_id

    try:
        insert_result = client.insert(
            collection_name=knowledge_name,
            data=[new_row],
        )
    except milvus_exceptions.MilvusException as exc:
        raise ServiceException(
            code=ResponseCode.OPERATION_FAILED,
            message=f"插入文档切片失败: {exc}",
        ) from exc

    vector_id = _extract_primary_key(
        insert_result,
        fallback_id=generated_vector_id,
    )

    return ChunkAddSuccessResult(
        vector_id=vector_id,
        chunk_index=chunk_index,
        embedding_dim=embedding_dim,
    )
