import asyncio

from app.core.agent.run_event_store import (
    AssistantRunEventStore,
    AssistantRunSnapshot,
    LocalRunHandle,
)
from app.schemas.assistant_run import AssistantRunStatus
from app.schemas.sse_response import AssistantResponse, Content, MessageType


class _FakePipeline:
    """简易 Redis pipeline stub。"""

    def __init__(self, redis_client: "_FakeRedis") -> None:
        self._redis = redis_client
        self._operations: list[tuple[str, tuple, dict]] = []

    def delete(self, *args, **kwargs):
        self._operations.append(("delete", args, kwargs))
        return self

    def hset(self, *args, **kwargs):
        self._operations.append(("hset", args, kwargs))
        return self

    def expire(self, *args, **kwargs):
        self._operations.append(("expire", args, kwargs))
        return self

    def set(self, *args, **kwargs):
        self._operations.append(("set", args, kwargs))
        return self

    def execute(self):
        for method_name, args, kwargs in self._operations:
            getattr(self._redis, method_name)(*args, **kwargs)
        return True


class _FakeRedis:
    """最小可用 Redis stub，覆盖 run_event_store 所需命令。"""

    def __init__(self) -> None:
        self.values: dict[str, str] = {}
        self.hashes: dict[str, dict[str, str]] = {}
        self.streams: dict[str, list[tuple[str, dict[str, str]]]] = {}
        self.expirations: dict[str, int] = {}
        self.stream_counter = 0

    def pipeline(self) -> _FakePipeline:
        return _FakePipeline(self)

    def set(self, key: str, value: str, nx: bool = False, ex: int | None = None):
        if nx and key in self.values:
            return False
        self.values[key] = value
        if ex is not None:
            self.expirations[key] = ex
        return True

    def get(self, key: str):
        return self.values.get(key)

    def delete(self, key: str) -> int:
        removed = 0
        if key in self.values:
            self.values.pop(key, None)
            removed += 1
        if key in self.hashes:
            self.hashes.pop(key, None)
            removed += 1
        if key in self.streams:
            self.streams.pop(key, None)
            removed += 1
        return removed

    def hset(self, key: str, mapping: dict[str, str] | None = None, **kwargs):
        target = self.hashes.setdefault(key, {})
        if mapping is not None:
            for field, value in mapping.items():
                target[str(field)] = str(value)
        for field, value in kwargs.items():
            target[str(field)] = str(value)
        return 1

    def hgetall(self, key: str) -> dict[str, str]:
        return dict(self.hashes.get(key, {}))

    def expire(self, key: str, timeout: int) -> bool:
        self.expirations[key] = timeout
        return True

    def xadd(
            self,
            key: str,
            fields: dict[str, str],
            maxlen: int | None = None,
            approximate: bool = True,
    ) -> str:
        _ = approximate
        self.stream_counter += 1
        event_id = f"{self.stream_counter}-0"
        items = self.streams.setdefault(key, [])
        items.append((event_id, dict(fields)))
        if maxlen is not None and len(items) > maxlen:
            self.streams[key] = items[-maxlen:]
        return event_id

    def xread(self, streams: dict[str, str], block: int | None = None):
        _ = block
        result: list[tuple[str, list[tuple[str, dict[str, str]]]]] = []
        for stream_key, last_event_id in streams.items():
            items = self.streams.get(stream_key, [])
            filtered_items = [
                (event_id, payload)
                for event_id, payload in items
                if event_id > last_event_id
            ]
            if filtered_items:
                result.append((stream_key, filtered_items))
        return result


class _DummyTask:
    """本机句柄测试用任务 stub。"""

    def result(self) -> None:
        return None


def test_run_event_store_create_run_enforces_single_active_run() -> None:
    """验证同一会话只能创建一个运行中的 active run。"""

    store = AssistantRunEventStore(redis_client=_FakeRedis())

    first_meta = store.create_run(
        conversation_uuid="conv-1",
        user_id=1,
        conversation_type="client",
        assistant_message_uuid="msg-1",
    )
    second_meta = store.create_run(
        conversation_uuid="conv-1",
        user_id=1,
        conversation_type="client",
        assistant_message_uuid="msg-2",
    )

    assert first_meta is not None
    assert first_meta.assistant_message_uuid == "msg-1"
    assert first_meta.status == AssistantRunStatus.RUNNING
    assert second_meta is None


def test_run_event_store_append_and_read_events_updates_snapshot_and_meta() -> None:
    """验证事件写入后，meta 与 snapshot 都会推进到最新事件。"""

    store = AssistantRunEventStore(redis_client=_FakeRedis())
    store.create_run(
        conversation_uuid="conv-1",
        user_id=1,
        conversation_type="admin",
        assistant_message_uuid="msg-1",
    )

    event = store.append_event(
        conversation_uuid="conv-1",
        payload=AssistantResponse(
            content=Content(text="你好"),
            type=MessageType.ANSWER,
        ),
        snapshot=AssistantRunSnapshot(
            answer_text="你好",
            thinking_text="",
            status=AssistantRunStatus.RUNNING,
            assistant_message_uuid="msg-1",
            last_event_id=None,
        ),
    )

    meta = store.get_run_meta(conversation_uuid="conv-1")
    snapshot = store.get_snapshot(conversation_uuid="conv-1")
    events = store.read_events(
        conversation_uuid="conv-1",
        last_event_id="0-0",
        block_ms=1,
    )

    assert meta is not None
    assert meta.last_event_id == event.event_id
    assert snapshot is not None
    assert snapshot.last_event_id == event.event_id
    assert snapshot.answer_text == "你好"
    assert len(events) == 1
    assert events[0].payload.content.text == "你好"


def test_run_event_store_request_cancel_marks_meta_and_local_handle() -> None:
    """验证 stop 请求会同时标记 Redis 元数据与本机取消句柄。"""

    store = AssistantRunEventStore(redis_client=_FakeRedis())
    store.create_run(
        conversation_uuid="conv-1",
        user_id=1,
        conversation_type="client",
        assistant_message_uuid="msg-1",
    )
    cancel_event = asyncio.Event()
    store.register_local_handle(
        conversation_uuid="conv-1",
        handle=LocalRunHandle(
            task=_DummyTask(),  # type: ignore[arg-type]
            cancel_event=cancel_event,
        ),
    )

    meta = store.request_cancel(conversation_uuid="conv-1")

    assert meta is not None
    assert store.is_cancel_requested(conversation_uuid="conv-1") is True
    assert cancel_event.is_set() is True


def test_run_event_store_finalize_run_releases_lock_and_keeps_terminal_snapshot() -> None:
    """验证 run 终态收尾会释放单活锁并保留终态快照。"""

    fake_redis = _FakeRedis()
    store = AssistantRunEventStore(redis_client=fake_redis)
    store.create_run(
        conversation_uuid="conv-1",
        user_id=1,
        conversation_type="admin",
        assistant_message_uuid="msg-1",
    )

    store.finalize_run(
        conversation_uuid="conv-1",
        final_status=AssistantRunStatus.CANCELLED,
        final_snapshot=AssistantRunSnapshot(
            answer_text="部分输出",
            thinking_text="部分思考",
            status=AssistantRunStatus.CANCELLED,
            assistant_message_uuid="msg-1",
            last_event_id="3-0",
        ),
    )

    meta = store.get_run_meta(conversation_uuid="conv-1")
    snapshot = store.get_snapshot(conversation_uuid="conv-1")

    assert fake_redis.get("assistant:run:lock:conv-1") is None
    assert meta is not None
    assert meta.status == AssistantRunStatus.CANCELLED
    assert snapshot is not None
    assert snapshot.status == AssistantRunStatus.CANCELLED
    assert snapshot.answer_text == "部分输出"
