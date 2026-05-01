from __future__ import annotations

import queue
import threading
import time
from typing import Any

from loguru import logger

from app.core.agent.tracing.config import AgentTraceSettings, load_agent_trace_settings
from app.core.agent.tracing.storage import write_trace_operations

_STOP_SENTINEL = {"type": "__stop__"}
"""后台 writer 停止信号。"""
_writer_lock = threading.Lock()
"""保护 writer 启停状态的进程级锁。"""
_writer_queue: queue.Queue[dict[str, Any]] | None = None
"""Agent Trace 后台写入队列。"""
_writer_thread: threading.Thread | None = None
"""Agent Trace 后台写入线程。"""
_writer_settings: AgentTraceSettings | None = None
"""后台 writer 当前使用的配置。"""


def _flush_batch(batch: list[dict[str, Any]]) -> None:
    """
    功能描述：
        将一批 trace 操作写入 Mongo。

    参数说明：
        batch (list[dict[str, Any]]): 待写入操作。

    返回值：
        None。
    """

    if not batch:
        return
    try:
        write_trace_operations(batch)
    except Exception as exc:  # pragma: no cover - 写库失败不能影响主链路
        logger.opt(exception=exc).warning("Agent trace batch write failed. size={}", len(batch))


def _writer_loop(settings: AgentTraceSettings, writer_queue: queue.Queue[dict[str, Any]]) -> None:
    """
    功能描述：
        Agent Trace 后台 writer 主循环。

    参数说明：
        settings (AgentTraceSettings): writer 配置。
        writer_queue (queue.Queue[dict[str, Any]]): 后台队列。

    返回值：
        None。
    """

    batch: list[dict[str, Any]] = []
    flush_interval_seconds = settings.flush_interval_ms / 1000
    next_flush_at = time.monotonic() + flush_interval_seconds

    while True:
        timeout = max(0.01, next_flush_at - time.monotonic())
        try:
            operation = writer_queue.get(timeout=timeout)
        except queue.Empty:
            _flush_batch(batch)
            batch = []
            next_flush_at = time.monotonic() + flush_interval_seconds
            continue

        if operation is _STOP_SENTINEL or operation.get("type") == _STOP_SENTINEL["type"]:
            _flush_batch(batch)
            batch = []
            break

        batch.append(operation)
        if len(batch) >= settings.batch_size:
            _flush_batch(batch)
            batch = []
            next_flush_at = time.monotonic() + flush_interval_seconds


def start_agent_trace_writer() -> None:
    """
    功能描述：
        启动 Agent Trace 后台 writer。

    参数说明：
        无。

    返回值：
        None。
    """

    global _writer_queue, _writer_settings, _writer_thread
    settings = load_agent_trace_settings()
    if not settings.enabled:
        return
    with _writer_lock:
        if _writer_thread is not None and _writer_thread.is_alive():
            return
        _writer_settings = settings
        _writer_queue = queue.Queue(maxsize=settings.queue_max_size)
        _writer_thread = threading.Thread(
            target=_writer_loop,
            args=(settings, _writer_queue),
            name="agent-trace-writer",
            daemon=True,
        )
        _writer_thread.start()


def stop_agent_trace_writer() -> None:
    """
    功能描述：
        停止 Agent Trace 后台 writer 并尽量刷完队列。

    参数说明：
        无。

    返回值：
        None。
    """

    global _writer_queue, _writer_settings, _writer_thread
    with _writer_lock:
        writer_thread = _writer_thread
        writer_queue = _writer_queue
        if writer_thread is None or writer_queue is None:
            return
        try:
            writer_queue.put(_STOP_SENTINEL, timeout=1)
        except queue.Full:
            logger.warning("Agent trace writer queue full when stopping; remaining items may be dropped.")
        writer_thread.join(timeout=5)
        _writer_thread = None
        _writer_queue = None
        _writer_settings = None


def enqueue_trace_operation(operation: dict[str, Any]) -> bool:
    """
    功能描述：
        非阻塞投递 trace 写入操作。

    参数说明：
        operation (dict[str, Any]): 写入操作。

    返回值：
        bool: 投递成功返回 True；未启用或队列满返回 False。
    """

    if not load_agent_trace_settings().enabled:
        return False
    if _writer_queue is None or _writer_thread is None or not _writer_thread.is_alive():
        start_agent_trace_writer()
    writer_queue = _writer_queue
    if writer_queue is None:
        return False
    try:
        writer_queue.put_nowait(operation)
        return True
    except queue.Full:
        logger.warning("Agent trace writer queue full; dropping trace operation type={}", operation.get("type"))
        return False
