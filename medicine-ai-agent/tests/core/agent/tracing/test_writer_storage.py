"""Agent Trace writer 和 Mongo storage 单元测试。"""

from __future__ import annotations

import queue
from typing import Any

import pytest

import app.core.agent.tracing.storage as storage_module
import app.core.agent.tracing.writer as writer_module
from app.core.agent.tracing.config import AgentTraceSettings


class _AliveThread:
    """用于模拟仍在运行的 writer 线程。"""

    def is_alive(self) -> bool:
        """
        功能描述：
            返回线程运行状态。

        参数说明：
            无。

        返回值：
            bool: 固定返回 True。
        """

        return True


class _FakeUpdateOne:
    """用于替换 pymongo.UpdateOne，方便断言批量写入内容。"""

    def __init__(self, filter_doc: dict[str, Any], update_doc: dict[str, Any], *, upsert: bool) -> None:
        """
        功能描述：
            保存 UpdateOne 初始化参数。

        参数说明：
            filter_doc (dict[str, Any]): Mongo 更新过滤条件。
            update_doc (dict[str, Any]): Mongo 更新内容。
            upsert (bool): 是否开启 upsert。

        返回值：
            None。
        """

        self.filter_doc = filter_doc
        self.update_doc = update_doc
        self.upsert = upsert


class _FakeCollection:
    """用于替换 Mongo collection，记录索引和写入调用。"""

    def __init__(self) -> None:
        """
        功能描述：
            初始化内存记录容器。

        参数说明：
            无。

        返回值：
            None。
        """

        self.indexes: list[dict[str, Any]] = []
        self.bulk_operations: list[_FakeUpdateOne] = []
        self.bulk_ordered: bool | None = None
        self.inserted_many: list[dict[str, Any]] = []
        self.insert_many_ordered: bool | None = None

    def create_index(self, keys: list[tuple[str, int]], *, name: str, **kwargs: Any) -> None:
        """
        功能描述：
            记录索引创建参数。

        参数说明：
            keys (list[tuple[str, int]]): 索引字段。
            name (str): 索引名称。
            **kwargs (Any): 其他索引参数。

        返回值：
            None。
        """

        self.indexes.append({"keys": keys, "name": name, "kwargs": kwargs})

    def bulk_write(self, operations: list[_FakeUpdateOne], *, ordered: bool) -> None:
        """
        功能描述：
            记录 bulk_write 调用。

        参数说明：
            operations (list[_FakeUpdateOne]): 批量更新操作。
            ordered (bool): 是否有序执行。

        返回值：
            None。
        """

        self.bulk_operations.extend(operations)
        self.bulk_ordered = ordered

    def insert_many(self, documents: list[dict[str, Any]], *, ordered: bool) -> None:
        """
        功能描述：
            记录 insert_many 调用。

        参数说明：
            documents (list[dict[str, Any]]): 待插入文档列表。
            ordered (bool): 是否有序执行。

        返回值：
            None。
        """

        self.inserted_many.extend(documents)
        self.insert_many_ordered = ordered


@pytest.fixture(autouse=True)
def _clear_writer_state() -> None:
    """每个用例前后清理 writer 全局状态，避免测试之间互相污染。"""
    writer_module._writer_queue = None
    writer_module._writer_thread = None
    writer_module._writer_settings = None
    yield
    writer_module._writer_queue = None
    writer_module._writer_thread = None
    writer_module._writer_settings = None


def _enabled_settings(*, queue_max_size: int = 2, batch_size: int = 2) -> AgentTraceSettings:
    """
    功能描述：
        构造测试用 trace writer 配置。

    参数说明：
        queue_max_size (int): 队列最大长度。
        batch_size (int): 批量写入条数。

    返回值：
        AgentTraceSettings: 测试配置。
    """

    return AgentTraceSettings(
        enabled=True,
        queue_max_size=queue_max_size,
        batch_size=batch_size,
        flush_interval_ms=1000,
        payload_max_chars=20000,
        message_view_max_messages=30,
    )


def test_enqueue_trace_operation_returns_false_when_disabled(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """验证 trace 关闭时不会启动 writer，也不会写入队列。"""
    monkeypatch.setattr(
        writer_module,
        "load_agent_trace_settings",
        lambda: AgentTraceSettings(
            enabled=False,
            queue_max_size=1,
            batch_size=1,
            flush_interval_ms=1000,
            payload_max_chars=20000,
            message_view_max_messages=30,
        ),
    )

    queued = writer_module.enqueue_trace_operation({"type": "insert_span"})

    assert queued is False
    assert writer_module._writer_queue is None
    assert writer_module._writer_thread is None


def test_enqueue_trace_operation_drops_new_event_when_queue_is_full(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """验证队列满时会丢弃新事件并返回 False，保证主链路不阻塞。"""
    writer_queue: queue.Queue[dict[str, Any]] = queue.Queue(maxsize=1)
    writer_queue.put_nowait({"type": "insert_run"})
    writer_module._writer_queue = writer_queue
    writer_module._writer_thread = _AliveThread()
    monkeypatch.setattr(writer_module, "load_agent_trace_settings", lambda: _enabled_settings(queue_max_size=1))

    queued = writer_module.enqueue_trace_operation({"type": "insert_span"})

    assert queued is False
    assert writer_queue.qsize() == 1
    assert writer_queue.get_nowait() == {"type": "insert_run"}


def test_writer_loop_flushes_batch_before_stop(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """验证后台 writer 达到批量大小后会刷写，并在 stop 信号后退出。"""
    captured_batches: list[list[dict[str, Any]]] = []

    def _capture_batch(batch: list[dict[str, Any]]) -> None:
        """
        功能描述：
            捕获 writer 写出的批次。

        参数说明：
            batch (list[dict[str, Any]]): 本次待写入操作。

        返回值：
            None。
        """

        captured_batches.append(list(batch))

    operation_one = {"type": "insert_span", "document": {"span_id": "span-1"}}
    operation_two = {"type": "update_run", "trace_id": "trace-1", "updates": {"status": "success"}}
    writer_queue: queue.Queue[dict[str, Any]] = queue.Queue()
    writer_queue.put_nowait(operation_one)
    writer_queue.put_nowait(operation_two)
    writer_queue.put_nowait(writer_module._STOP_SENTINEL)
    monkeypatch.setattr(writer_module, "write_trace_operations", _capture_batch)

    writer_module._writer_loop(_enabled_settings(batch_size=2), writer_queue)

    assert captured_batches == [[operation_one, operation_two]]


def test_write_trace_operations_batches_run_updates_and_span_inserts(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """验证 storage 会把 run、span 和模型 Token 明细分组写入，并创建必要索引。"""
    runs_collection = _FakeCollection()
    spans_collection = _FakeCollection()
    token_usage_collection = _FakeCollection()
    monkeypatch.setattr(storage_module, "_get_runs_collection", lambda: runs_collection)
    monkeypatch.setattr(storage_module, "_get_spans_collection", lambda: spans_collection)
    monkeypatch.setattr(
        storage_module,
        "_get_model_token_usage_collection",
        lambda: token_usage_collection,
    )
    monkeypatch.setattr(storage_module, "UpdateOne", _FakeUpdateOne)
    storage_module.ensure_agent_trace_indexes.cache_clear()

    try:
        storage_module.write_trace_operations(
            [
                {
                    "type": "insert_run",
                    "document": {"trace_id": "trace-1", "status": "running"},
                },
                {
                    "type": "update_run",
                    "trace_id": "trace-1",
                    "set_on_insert": {"conversation_uuid": "conversation-1"},
                    "updates": {"status": "success"},
                },
                {
                    "type": "insert_span",
                    "document": {"trace_id": "trace-1", "span_id": "span-1"},
                },
                {
                    "type": "insert_model_token_usage",
                    "document": {
                        "trace_id": "trace-1",
                        "span_id": "span-1",
                        "input_tokens": 2174,
                        "output_tokens": 88,
                        "total_tokens": 2262,
                        "cache_read_tokens": 1605,
                        "cache_write_tokens": 0,
                        "cache_total_tokens": 1605,
                    },
                },
                {"type": "insert_span", "document": {}},
                {"type": "insert_model_token_usage", "document": {}},
                {"type": "unknown"},
            ]
        )
    finally:
        storage_module.ensure_agent_trace_indexes.cache_clear()

    assert runs_collection.indexes[0]["name"] == "uk_agent_trace_run_trace_id"
    assert runs_collection.indexes[0]["kwargs"] == {"unique": True}
    assert spans_collection.indexes[0]["name"] == "idx_agent_trace_span_trace_sequence_asc"
    assert token_usage_collection.indexes[0]["name"] == "uk_agent_model_token_usage_span_id"
    assert token_usage_collection.indexes[0]["kwargs"] == {"unique": True}
    assert len(runs_collection.bulk_operations) == 2
    assert runs_collection.bulk_ordered is False
    assert runs_collection.bulk_operations[0].filter_doc == {"trace_id": "trace-1"}
    assert runs_collection.bulk_operations[0].update_doc == {
        "$setOnInsert": {"trace_id": "trace-1", "status": "running"},
    }
    assert runs_collection.bulk_operations[0].upsert is True
    assert runs_collection.bulk_operations[1].update_doc == {
        "$set": {"status": "success"},
        "$setOnInsert": {"conversation_uuid": "conversation-1"},
    }
    assert runs_collection.bulk_operations[1].upsert is True
    assert spans_collection.inserted_many == [{"trace_id": "trace-1", "span_id": "span-1"}]
    assert spans_collection.insert_many_ordered is False
    assert token_usage_collection.inserted_many == [
        {
            "trace_id": "trace-1",
            "span_id": "span-1",
            "input_tokens": 2174,
            "output_tokens": 88,
            "total_tokens": 2262,
            "cache_read_tokens": 1605,
            "cache_write_tokens": 0,
            "cache_total_tokens": 1605,
        }
    ]
    assert token_usage_collection.insert_many_ordered is False
