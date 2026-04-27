from __future__ import annotations

import hashlib
import os
import socket
import threading
import time
import uuid
from dataclasses import dataclass, field

CUSTOM_EPOCH_MS = 1704067200000  # 2024-01-01T00:00:00Z
NODE_ID_BITS = 10
SEQUENCE_BITS = 12
MAX_NODE_ID = (1 << NODE_ID_BITS) - 1
MAX_SEQUENCE = (1 << SEQUENCE_BITS) - 1


def _resolve_node_id() -> int:
    """基于主机与进程特征生成当前进程的 10 bit 节点标识。"""
    raw = f"{socket.gethostname()}:{uuid.getnode()}:{os.getpid()}".encode("utf-8")
    digest = hashlib.sha1(raw).digest()
    return int.from_bytes(digest[:2], "big") & MAX_NODE_ID


@dataclass
class SnowflakeIdGenerator:
    """简单的 63 bit 正整数雪花 ID 生成器。"""

    node_id: int = field(default_factory=_resolve_node_id)
    _last_timestamp_ms: int = field(default=-1, init=False, repr=False)
    _sequence: int = field(default=0, init=False, repr=False)
    _lock: threading.Lock = field(default_factory=threading.Lock, init=False, repr=False)

    def __post_init__(self) -> None:
        if self.node_id < 0 or self.node_id > MAX_NODE_ID:
            raise ValueError(f"node_id 必须在 0 到 {MAX_NODE_ID} 之间")

    @staticmethod
    def _current_timestamp_ms() -> int:
        return time.time_ns() // 1_000_000

    def _wait_next_timestamp_ms(self, last_timestamp_ms: int) -> int:
        timestamp_ms = self._current_timestamp_ms()
        while timestamp_ms <= last_timestamp_ms:
            time.sleep(0.0001)
            timestamp_ms = self._current_timestamp_ms()
        return timestamp_ms

    def next_id(self) -> int:
        """生成一个新的正整数 `INT64` 雪花 ID。"""
        with self._lock:
            timestamp_ms = self._current_timestamp_ms()
            if timestamp_ms < self._last_timestamp_ms:
                timestamp_ms = self._last_timestamp_ms

            if timestamp_ms == self._last_timestamp_ms:
                self._sequence = (self._sequence + 1) & MAX_SEQUENCE
                if self._sequence == 0:
                    timestamp_ms = self._wait_next_timestamp_ms(self._last_timestamp_ms)
            else:
                self._sequence = 0

            self._last_timestamp_ms = timestamp_ms
            timestamp_part = max(0, timestamp_ms - CUSTOM_EPOCH_MS)
            return (
                    (timestamp_part << (NODE_ID_BITS + SEQUENCE_BITS))
                    | (self.node_id << SEQUENCE_BITS)
                    | self._sequence
            )


_DEFAULT_GENERATOR = SnowflakeIdGenerator()


def generate_snowflake_id() -> int:
    """生成一个新的雪花 ID。"""
    return _DEFAULT_GENERATOR.next_id()
