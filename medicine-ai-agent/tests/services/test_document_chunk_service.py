from pathlib import Path

import pytest

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.rag.chunking import ChunkStats, SplitChunk
from app.rag.file_loader.types import FileKind, ParsedDocument
from app.services import document_chunk_service as service_module


def test_import_single_file_succeeds_without_processing_stage_callback(
        monkeypatch: pytest.MonkeyPatch,
        tmp_path: Path,
) -> None:
    """验证 import_single_file 不依赖阶段回调也能完成主流程。"""
    source_path = tmp_path / "demo.txt"
    source_path.write_text("dummy", encoding="utf-8")

    monkeypatch.setenv("KNOWLEDGE_VECTOR_BATCH_SIZE", "2")
    monkeypatch.setattr(
        service_module,
        "_download_file",
        lambda _url: ("demo.txt", source_path),
    )
    monkeypatch.setattr(
        service_module,
        "parse_downloaded_file",
        lambda **_: ParsedDocument(
            file_kind=FileKind.TEXT,
            mime_type="text/plain",
            source_extension=".txt",
            text="第一段\n\n第二段\n\n第三段",
        ),
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "ensure_collection_exists",
        lambda **_: None,
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "get_collection_embedding_dim",
        lambda **_: 1024,
    )
    monkeypatch.setattr(
        service_module,
        "create_embedding_client",
        lambda **_: object(),
    )
    monkeypatch.setattr(
        service_module,
        "embed_texts",
        lambda texts, **_: [[0.1, 0.2] for _ in texts],
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "insert_embeddings",
        lambda **_kwargs: None,
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "count_document_chunks",
        lambda **_kwargs: 3,
    )
    monkeypatch.setattr(
        service_module,
        "_split_parsed_text",
        lambda *_args, **_kwargs: [
            SplitChunk(text="A", stats=ChunkStats(char_count=1)),
            SplitChunk(text="B", stats=ChunkStats(char_count=1)),
            SplitChunk(text="C", stats=ChunkStats(char_count=1)),
        ],
    )

    result = service_module.import_single_file(
        url="https://example.com/demo.txt",
        knowledge_name="demo",
        document_id=2,
        embedding_model="text-embedding-v4",
        chunk_size=200,
        chunk_overlap=50,
    )

    assert result.status == "success"
    assert result.chunk_count == 3
    assert result.vector_count == 3
    assert not source_path.exists()


def test_import_single_file_rejects_url_without_supported_suffix(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """验证不支持的 URL 后缀会在下载前被拒绝。"""
    called = {"download": False}

    def _fake_download(_url: str):
        called["download"] = True
        return "a.txt", Path("/tmp/a.txt")

    monkeypatch.setattr(service_module, "_download_file", _fake_download)
    monkeypatch.setattr(
        service_module.vector_repository,
        "ensure_collection_exists",
        lambda **_: None,
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "get_collection_embedding_dim",
        lambda **_: 1024,
    )
    monkeypatch.setattr(
        service_module,
        "create_embedding_client",
        lambda **_: object(),
    )

    result = service_module.import_single_file(
        url="https://example.com/file.bin",
        knowledge_name="demo",
        document_id=1,
        embedding_model="text-embedding-v4",
        chunk_size=200,
        chunk_overlap=50,
    )

    assert result.status == "failed"
    assert result.file_url == "https://example.com/file.bin"
    assert called["download"] is False


def test_import_single_file_rejects_html_url_suffix(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """验证 HTML 文件后缀不在导入支持列表内。"""
    called = {"download": False}

    def _fake_download(_url: str):
        called["download"] = True
        return "a.html", Path("/tmp/a.html")

    monkeypatch.setattr(service_module, "_download_file", _fake_download)
    monkeypatch.setattr(
        service_module.vector_repository,
        "ensure_collection_exists",
        lambda **_: None,
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "get_collection_embedding_dim",
        lambda **_: 1024,
    )
    monkeypatch.setattr(
        service_module,
        "create_embedding_client",
        lambda **_: object(),
    )

    result = service_module.import_single_file(
        url="https://example.com/file.html",
        knowledge_name="demo",
        document_id=1,
        embedding_model="text-embedding-v4",
        chunk_size=200,
        chunk_overlap=50,
    )

    assert result.status == "failed"
    assert result.file_url == "https://example.com/file.html"
    assert "不支持的文件后缀" in result.error
    assert called["download"] is False


def test_import_single_file_runs_vectorization_and_insert_batches(
        monkeypatch: pytest.MonkeyPatch,
        tmp_path: Path,
) -> None:
    """验证导入主流程可完成下载、解析、切片、向量化与入库。"""
    source_path = tmp_path / "demo.txt"
    source_path.write_text("dummy", encoding="utf-8")
    insert_calls: list[dict] = []

    monkeypatch.setenv("KNOWLEDGE_VECTOR_BATCH_SIZE", "2")
    monkeypatch.setattr(
        service_module,
        "_download_file",
        lambda _url: ("demo.txt", source_path),
    )
    monkeypatch.setattr(
        service_module,
        "parse_downloaded_file",
        lambda **_: ParsedDocument(
            file_kind=FileKind.TEXT,
            mime_type="text/plain",
            source_extension=".txt",
            text="第一段\n\n第二段\n\n第三段",
        ),
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "ensure_collection_exists",
        lambda **_: None,
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "get_collection_embedding_dim",
        lambda **_: 1024,
    )
    monkeypatch.setattr(
        service_module,
        "create_embedding_client",
        lambda **_: object(),
    )
    monkeypatch.setattr(
        service_module,
        "embed_texts",
        lambda texts, **_: [[0.1, 0.2] for _ in texts],
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "insert_embeddings",
        lambda **kwargs: insert_calls.append(kwargs),
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "count_document_chunks",
        lambda **_kwargs: 3,
    )
    monkeypatch.setattr(
        service_module,
        "_split_parsed_text",
        lambda *_args, **_kwargs: [
            SplitChunk(text="A", stats=ChunkStats(char_count=1)),
            SplitChunk(text="B", stats=ChunkStats(char_count=1)),
            SplitChunk(text="C", stats=ChunkStats(char_count=1)),
        ],
    )

    result = service_module.import_single_file(
        url="https://example.com/demo.txt",
        knowledge_name="demo",
        document_id=2,
        embedding_model="text-embedding-v4",
        chunk_size=200,
        chunk_overlap=50,
    )

    assert result.status == "success"
    first = result
    assert first.filename == "demo.txt"
    assert first.file_size == 5
    assert first.file_kind == "text"
    assert first.mime_type == "text/plain"
    assert first.source_extension == ".txt"
    assert first.chunk_count == 3
    assert first.vector_count == 3
    assert first.insert_batches == 2
    assert first.embedding_model == "text-embedding-v4"
    assert first.embedding_dim == 1024
    assert len(insert_calls) == 2
    assert insert_calls[0]["start_chunk_index"] == 1
    assert insert_calls[1]["start_chunk_index"] == 3
    assert insert_calls[0]["chunk_size"] == 200
    assert insert_calls[0]["chunk_overlap"] == 50
    assert not source_path.exists()


def test_import_single_file_allows_missing_mime_type_for_markdown(
        monkeypatch: pytest.MonkeyPatch,
        tmp_path: Path,
) -> None:
    """验证 markdown 文件在 MIME 未识别时仍可成功导入。"""
    source_path = tmp_path / "demo.md"
    source_path.write_text("# 标题\n\n内容", encoding="utf-8")

    monkeypatch.setenv("KNOWLEDGE_VECTOR_BATCH_SIZE", "2")
    monkeypatch.setattr(
        service_module,
        "_download_file",
        lambda _url: ("demo.md", source_path),
    )
    monkeypatch.setattr(
        service_module,
        "parse_downloaded_file",
        lambda **_: ParsedDocument(
            file_kind=FileKind.MARKDOWN,
            mime_type=None,
            source_extension=".md",
            text="# 标题\n\n内容",
        ),
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "ensure_collection_exists",
        lambda **_: None,
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "get_collection_embedding_dim",
        lambda **_: 1024,
    )
    monkeypatch.setattr(
        service_module,
        "create_embedding_client",
        lambda **_: object(),
    )
    monkeypatch.setattr(
        service_module,
        "embed_texts",
        lambda texts, **_: [[0.1, 0.2] for _ in texts],
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "insert_embeddings",
        lambda **_kwargs: None,
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "count_document_chunks",
        lambda **_kwargs: 1,
    )
    monkeypatch.setattr(
        service_module,
        "_split_parsed_text",
        lambda *_args, **_kwargs: [
            SplitChunk(text="markdown", stats=ChunkStats(char_count=8)),
        ],
    )

    result = service_module.import_single_file(
        url="https://example.com/demo.md",
        knowledge_name="demo",
        document_id=10,
        embedding_model="text-embedding-v4",
        chunk_size=200,
        chunk_overlap=50,
    )

    assert result.status == "success"
    assert result.file_kind == "markdown"
    assert result.mime_type is None
    assert result.chunk_count == 1
    assert not source_path.exists()


def test_import_single_file_cleans_downloaded_file_on_parse_failure(
        monkeypatch: pytest.MonkeyPatch,
        tmp_path: Path,
) -> None:
    """验证解析失败时仍会清理已下载临时文件。"""
    source_path = tmp_path / "demo.txt"
    source_path.write_text("dummy", encoding="utf-8")

    monkeypatch.setattr(
        service_module,
        "_download_file",
        lambda _url: ("demo.txt", source_path),
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "ensure_collection_exists",
        lambda **_: None,
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "get_collection_embedding_dim",
        lambda **_: 1024,
    )
    monkeypatch.setattr(
        service_module,
        "create_embedding_client",
        lambda **_: object(),
    )
    monkeypatch.setattr(
        service_module,
        "parse_downloaded_file",
        lambda **_: (_raise_parse_error()),
    )

    result = service_module.import_single_file(
        url="https://example.com/demo.txt",
        knowledge_name="demo",
        document_id=3,
        embedding_model="text-embedding-v4",
        chunk_size=10,
        chunk_overlap=20,
    )

    assert result.status == "failed"
    assert result.file_url == "https://example.com/demo.txt"
    assert result.file_size == 5
    assert "mock parse error" in result.error
    assert not source_path.exists()


def test_import_single_file_batches_are_strictly_serial(
        monkeypatch: pytest.MonkeyPatch,
        tmp_path: Path,
) -> None:
    """验证向量化与入库按批次严格串行执行。"""
    source_path = tmp_path / "demo.txt"
    source_path.write_text("dummy", encoding="utf-8")
    trace: list[str] = []

    monkeypatch.setenv("KNOWLEDGE_VECTOR_BATCH_SIZE", "2")
    monkeypatch.setattr(
        service_module,
        "_download_file",
        lambda _url: ("demo.txt", source_path),
    )
    monkeypatch.setattr(
        service_module,
        "parse_downloaded_file",
        lambda **_: ParsedDocument(
            file_kind=FileKind.TEXT,
            mime_type="text/plain",
            source_extension=".txt",
            text="T",
        ),
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "ensure_collection_exists",
        lambda **_: None,
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "get_collection_embedding_dim",
        lambda **_: 1024,
    )
    monkeypatch.setattr(
        service_module,
        "create_embedding_client",
        lambda **_: object(),
    )

    def _fake_embed(texts: list[str], **_kwargs) -> list[list[float]]:
        trace.append(f"embed-{len(texts)}")
        return [[0.1, 0.2] for _ in texts]

    def _fake_insert(**kwargs) -> None:
        trace.append(f"insert-{len(kwargs['texts'])}")

    monkeypatch.setattr(service_module, "embed_texts", _fake_embed)
    monkeypatch.setattr(
        service_module.vector_repository,
        "insert_embeddings",
        _fake_insert,
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "count_document_chunks",
        lambda **_kwargs: 3,
    )
    monkeypatch.setattr(
        service_module,
        "_split_parsed_text",
        lambda *_args, **_kwargs: [
            SplitChunk(text="A", stats=ChunkStats(char_count=1)),
            SplitChunk(text="B", stats=ChunkStats(char_count=1)),
            SplitChunk(text="C", stats=ChunkStats(char_count=1)),
        ],
    )

    result = service_module.import_single_file(
        url="https://example.com/demo.txt",
        knowledge_name="demo",
        document_id=6,
        embedding_model="text-embedding-v4",
        chunk_size=100,
        chunk_overlap=50,
    )

    assert result.status == "success"
    assert trace == ["embed-2", "insert-2", "embed-1", "insert-1"]
    assert not source_path.exists()


def test_import_single_file_fails_when_insert_visibility_check_not_passed(
        monkeypatch: pytest.MonkeyPatch,
        tmp_path: Path,
) -> None:
    """验证写入可见性校验失败时返回 failed，避免误发 completed。"""
    source_path = tmp_path / "demo.txt"
    source_path.write_text("dummy", encoding="utf-8")

    monkeypatch.setenv("KNOWLEDGE_VECTOR_BATCH_SIZE", "2")
    monkeypatch.setattr(
        service_module,
        "DEFAULT_INSERT_VERIFY_MAX_RETRIES",
        2,
    )
    monkeypatch.setattr(
        service_module,
        "DEFAULT_INSERT_VERIFY_INTERVAL_SECONDS",
        0.0,
    )
    monkeypatch.setattr(
        service_module,
        "_download_file",
        lambda _url: ("demo.txt", source_path),
    )
    monkeypatch.setattr(
        service_module,
        "parse_downloaded_file",
        lambda **_: ParsedDocument(
            file_kind=FileKind.TEXT,
            mime_type="text/plain",
            source_extension=".txt",
            text="第一段\n\n第二段",
        ),
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "ensure_collection_exists",
        lambda **_: None,
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "get_collection_embedding_dim",
        lambda **_: 1024,
    )
    monkeypatch.setattr(
        service_module,
        "create_embedding_client",
        lambda **_: object(),
    )
    monkeypatch.setattr(
        service_module,
        "embed_texts",
        lambda texts, **_: [[0.1, 0.2] for _ in texts],
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "insert_embeddings",
        lambda **_kwargs: None,
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "count_document_chunks",
        lambda **_kwargs: 0,
    )
    monkeypatch.setattr(
        service_module,
        "_split_parsed_text",
        lambda *_args, **_kwargs: [
            SplitChunk(text="A", stats=ChunkStats(char_count=1)),
            SplitChunk(text="B", stats=ChunkStats(char_count=1)),
        ],
    )

    result = service_module.import_single_file(
        url="https://example.com/demo.txt",
        knowledge_name="demo",
        document_id=9,
        embedding_model="text-embedding-v4",
        chunk_size=200,
        chunk_overlap=50,
    )

    assert result.status == "failed"
    assert "切片写入校验失败" in result.error
    assert not source_path.exists()


class _DummyLogger:
    def __init__(self) -> None:
        self.warning_logs: list[str] = []

    @staticmethod
    def _render(message, *args):
        if not args:
            return message
        try:
            return message.format(*args)
        except Exception:
            return f"{message} {args}"

    def warning(self, message, *args):
        self.warning_logs.append(self._render(message, *args))


def test_import_single_file_cleanup_failure_does_not_override_result(
        monkeypatch: pytest.MonkeyPatch,
        tmp_path: Path,
) -> None:
    """验证删除临时文件失败时不覆盖主流程结果，仅记录 warning。"""
    source_path = tmp_path / "demo.txt"
    source_path.write_text("dummy", encoding="utf-8")
    dummy_logger = _DummyLogger()
    original_unlink = Path.unlink

    def _fake_unlink(self: Path, missing_ok: bool = False) -> None:
        if self == source_path:
            raise OSError("cleanup failed")
        original_unlink(self, missing_ok=missing_ok)

    monkeypatch.setenv("KNOWLEDGE_VECTOR_BATCH_SIZE", "2")
    monkeypatch.setattr(service_module, "logger", dummy_logger)
    monkeypatch.setattr(Path, "unlink", _fake_unlink)
    monkeypatch.setattr(
        service_module,
        "_download_file",
        lambda _url: ("demo.txt", source_path),
    )
    monkeypatch.setattr(
        service_module,
        "parse_downloaded_file",
        lambda **_: ParsedDocument(
            file_kind=FileKind.TEXT,
            mime_type="text/plain",
            source_extension=".txt",
            text="第一段",
        ),
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "ensure_collection_exists",
        lambda **_: None,
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "get_collection_embedding_dim",
        lambda **_: 1024,
    )
    monkeypatch.setattr(
        service_module,
        "create_embedding_client",
        lambda **_: object(),
    )
    monkeypatch.setattr(
        service_module,
        "embed_texts",
        lambda texts, **_: [[0.1, 0.2] for _ in texts],
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "insert_embeddings",
        lambda **_kwargs: None,
    )
    monkeypatch.setattr(
        service_module.vector_repository,
        "count_document_chunks",
        lambda **_kwargs: 1,
    )
    monkeypatch.setattr(
        service_module,
        "_split_parsed_text",
        lambda *_args, **_kwargs: [
            SplitChunk(text="A", stats=ChunkStats(char_count=1)),
        ],
    )

    result = service_module.import_single_file(
        url="https://example.com/demo.txt",
        knowledge_name="demo",
        document_id=11,
        embedding_model="text-embedding-v4",
        chunk_size=100,
        chunk_overlap=50,
    )

    assert result.status == "success"
    assert source_path.exists()
    assert any("Failed to cleanup downloaded temp file:" in item for item in dummy_logger.warning_logs)


def _raise_parse_error() -> ParsedDocument:
    """用于失败路径测试，抛出解析异常。"""
    raise ServiceException(code=ResponseCode.OPERATION_FAILED, message="mock parse error")
