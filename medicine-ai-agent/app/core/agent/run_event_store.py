from __future__ import annotations

import asyncio
import json
import os
import time
import uuid
from dataclasses import dataclass
from threading import RLock
from typing import Any

from redis import Redis
from redis.exceptions import RedisError

from app.core.codes import ResponseCode
from app.core.database.redis.config import get_redis_connection
from app.core.exception.exceptions import ServiceException
from app.schemas.assistant_run import AssistantRunStatus
from app.schemas.sse_response import AssistantResponse

DEFAULT_ASSISTANT_RUN_SNAPSHOT_FLUSH_MS = 500
"""Mongo 流式快照默认刷新间隔（毫秒）。"""

DEFAULT_ASSISTANT_RUN_REDIS_TTL_SECONDS = 7200
"""运行中 Redis 元数据默认保留时长（秒）。"""

DEFAULT_ASSISTANT_RUN_TERMINAL_TTL_SECONDS = 900
"""终态 Redis 元数据默认保留时长（秒）。"""

DEFAULT_ASSISTANT_RUN_EVENT_STREAM_MAX_LEN = 2000
"""Redis Stream 默认保留的最大事件条数。"""

DEFAULT_ASSISTANT_RUN_STREAM_BLOCK_MS = 15000
"""attach 读取 Redis Stream 时的默认阻塞时长（毫秒）。"""

RUN_LOCK_KEY_PREFIX = "assistant:run:lock"
"""会话单活锁 Redis key 前缀。"""

RUN_META_KEY_PREFIX = "assistant:run:meta"
"""运行元数据 Redis key 前缀。"""

RUN_SNAPSHOT_KEY_PREFIX = "assistant:run:snapshot"
"""运行快照 Redis key 前缀。"""

RUN_EVENTS_KEY_PREFIX = "assistant:run:events"
"""事件流 Redis key 前缀。"""

_RUN_OWNER_INSTANCE_ID = str(uuid.uuid4())
"""当前进程的运行实例标识。"""


def _decode_redis_value(value: Any) -> str | None:
    """
    功能描述：
        把 Redis 返回值统一解码为字符串。

    参数说明：
        value (Any): Redis 原始返回值，可能是 `bytes`、`str` 或其他类型。

    返回值：
        str | None: 解码后的字符串；传入 `None` 时返回 `None`。

    异常说明：
        无。
    """

    if value is None:
        return None
    if isinstance(value, bytes):
        return value.decode("utf-8")
    return str(value)


def _to_bool(value: Any) -> bool:
    """
    功能描述：
        将 Redis 字段值解析为布尔值。

    参数说明：
        value (Any): 原始值。

    返回值：
        bool: 解析后的布尔结果。

    异常说明：
        无。
    """

    normalized = (_decode_redis_value(value) or "").strip().lower()
    return normalized in {"1", "true", "yes", "on"}


def _parse_positive_int(raw_value: str | None, *, default_value: int) -> int:
    """
    功能描述：
        解析正整数环境变量，非法时回退默认值。

    参数说明：
        raw_value (str | None): 原始环境变量值。
        default_value (int): 默认值。

    返回值：
        int: 生效的正整数配置值。

    异常说明：
        无。
    """

    if raw_value is None or not raw_value.strip():
        return default_value
    try:
        resolved_value = int(raw_value)
    except ValueError:
        return default_value
    return resolved_value if resolved_value > 0 else default_value


def resolve_assistant_run_snapshot_flush_ms() -> int:
    """
    功能描述：
        解析消息流式快照刷库间隔配置。

    参数说明：
        无。

    返回值：
        int: 快照刷库间隔（毫秒）。

    异常说明：
        无。
    """

    return _parse_positive_int(
        os.getenv("ASSISTANT_RUN_SNAPSHOT_FLUSH_MS"),
        default_value=DEFAULT_ASSISTANT_RUN_SNAPSHOT_FLUSH_MS,
    )


def resolve_assistant_run_redis_ttl_seconds() -> int:
    """
    功能描述：
        解析运行中 Redis 元数据保留时长。

    参数说明：
        无。

    返回值：
        int: Redis 运行态 TTL（秒）。

    异常说明：
        无。
    """

    return _parse_positive_int(
        os.getenv("ASSISTANT_RUN_REDIS_TTL_SECONDS"),
        default_value=DEFAULT_ASSISTANT_RUN_REDIS_TTL_SECONDS,
    )


def resolve_assistant_run_terminal_ttl_seconds() -> int:
    """
    功能描述：
        解析终态 Redis 元数据保留时长。

    参数说明：
        无。

    返回值：
        int: 终态 TTL（秒）。

    异常说明：
        无。
    """

    return _parse_positive_int(
        os.getenv("ASSISTANT_RUN_TERMINAL_TTL_SECONDS"),
        default_value=DEFAULT_ASSISTANT_RUN_TERMINAL_TTL_SECONDS,
    )


def resolve_assistant_run_event_stream_max_len() -> int:
    """
    功能描述：
        解析 Redis Stream 的最大保留长度配置。

    参数说明：
        无。

    返回值：
        int: Redis Stream 最大事件条数。

    异常说明：
        无。
    """

    return _parse_positive_int(
        os.getenv("ASSISTANT_RUN_EVENT_STREAM_MAX_LEN"),
        default_value=DEFAULT_ASSISTANT_RUN_EVENT_STREAM_MAX_LEN,
    )


def resolve_assistant_run_stream_block_ms() -> int:
    """
    功能描述：
        解析 attach 阻塞读取 Redis Stream 的最长等待时间。

    参数说明：
        无。

    返回值：
        int: 阻塞等待时长（毫秒）。

    异常说明：
        无。
    """

    return _parse_positive_int(
        os.getenv("ASSISTANT_RUN_STREAM_BLOCK_MS"),
        default_value=DEFAULT_ASSISTANT_RUN_STREAM_BLOCK_MS,
    )


def _build_lock_key(conversation_uuid: str) -> str:
    """
    功能描述：
        构造会话单活锁 key。

    参数说明：
        conversation_uuid (str): 会话 UUID。

    返回值：
        str: Redis 锁 key。

    异常说明：
        无。
    """

    return f"{RUN_LOCK_KEY_PREFIX}:{conversation_uuid}"


def _build_meta_key(conversation_uuid: str) -> str:
    """
    功能描述：
        构造运行元数据 key。

    参数说明：
        conversation_uuid (str): 会话 UUID。

    返回值：
        str: Redis 元数据 key。

    异常说明：
        无。
    """

    return f"{RUN_META_KEY_PREFIX}:{conversation_uuid}"


def _build_snapshot_key(conversation_uuid: str) -> str:
    """
    功能描述：
        构造运行快照 key。

    参数说明：
        conversation_uuid (str): 会话 UUID。

    返回值：
        str: Redis 快照 key。

    异常说明：
        无。
    """

    return f"{RUN_SNAPSHOT_KEY_PREFIX}:{conversation_uuid}"


def _build_events_key(conversation_uuid: str) -> str:
    """
    功能描述：
        构造事件流 key。

    参数说明：
        conversation_uuid (str): 会话 UUID。

    返回值：
        str: Redis Stream key。

    异常说明：
        无。
    """

    return f"{RUN_EVENTS_KEY_PREFIX}:{conversation_uuid}"


def _raise_redis_unavailable(operation: str, exc: Exception) -> ServiceException:
    """
    功能描述：
        将底层 Redis 异常包装为统一的 503 业务异常。

    参数说明：
        operation (str): 当前执行的 Redis 操作名称。
        exc (Exception): 原始异常对象。

    返回值：
        ServiceException: 统一 503 异常对象。

    异常说明：
        无；当前函数只负责构造异常对象。
    """

    return ServiceException(
        code=ResponseCode.SERVICE_UNAVAILABLE,
        message=f"助手运行态 Redis 不可用: {operation}",
        data={"operation": operation, "detail": str(exc)},
    )


@dataclass(frozen=True)
class AssistantRunMeta:
    """
    功能描述：
        助手运行态元数据模型。

    Attributes:
        run_id: 运行唯一标识。
        conversation_uuid: 会话 UUID。
        user_id: 用户 ID。
        conversation_type: 会话类型。
        assistant_message_uuid: 当前 AI 消息 UUID。
        status: 当前运行态状态。
        owner_instance_id: 创建该 run 的实例标识。
        last_event_id: 最新 Redis Stream 事件 ID。
        cancel_requested: 是否已请求取消。
        started_at: 运行开始时间戳（毫秒）。
        updated_at: 最近更新时间戳（毫秒）。
    """

    run_id: str
    conversation_uuid: str
    user_id: int
    conversation_type: str
    assistant_message_uuid: str
    status: AssistantRunStatus
    owner_instance_id: str
    last_event_id: str | None
    cancel_requested: bool
    started_at: int
    updated_at: int


@dataclass(frozen=True)
class AssistantRunSnapshot:
    """
    功能描述：
        助手运行时快照模型。

    Attributes:
        answer_text: 当前已聚合的回答文本。
        thinking_text: 当前已聚合的思考文本。
        status: 当前运行态状态。
        assistant_message_uuid: 当前 AI 消息 UUID。
        last_event_id: 快照已覆盖到的最新事件 ID。
    """

    answer_text: str
    thinking_text: str
    status: AssistantRunStatus
    assistant_message_uuid: str
    last_event_id: str | None


@dataclass(frozen=True)
class AssistantRunEvent:
    """
    功能描述：
        Redis Stream 中单条助手事件模型。

    Attributes:
        event_id: Redis Stream 事件 ID。
        payload: 标准 AssistantResponse 负载。
    """

    event_id: str
    payload: AssistantResponse


@dataclass
class LocalRunHandle:
    """
    功能描述：
        当前进程内的运行句柄。

    Attributes:
        task: 后台执行 run 的 asyncio 任务。
        cancel_event: 本机立即取消标记。
    """

    task: asyncio.Task[Any]
    cancel_event: asyncio.Event


class AssistantRunEventStore:
    """
    功能描述：
        Redis 助手运行态存储入口。

    设计目标：
        1. 统一封装会话单活锁、运行元数据、事件流与快照；
        2. 对 service 层暴露稳定接口，避免上层散落 Redis key 细节；
        3. 在同一进程内维护本机 run 句柄，支持快速取消。
    """

    _local_handles: dict[str, LocalRunHandle] = {}
    _local_handles_lock = RLock()

    def __init__(self, redis_client: Redis | None = None) -> None:
        """
        功能描述：
            初始化运行态存储实例。

        参数说明：
            redis_client (Redis | None): 可选 Redis 客户端；为空时使用全局连接。

        返回值：
            无。

        异常说明：
            无。
        """

        self._redis = redis_client or get_redis_connection()

    @property
    def owner_instance_id(self) -> str:
        """
        功能描述：
            返回当前进程的 owner 实例标识。

        参数说明：
            无。

        返回值：
            str: 当前实例 ID。

        异常说明：
            无。
        """

        return _RUN_OWNER_INSTANCE_ID

    def create_run(
            self,
            *,
            conversation_uuid: str,
            user_id: int,
            conversation_type: str,
            assistant_message_uuid: str,
    ) -> AssistantRunMeta | None:
        """
        功能描述：
            创建新的会话运行态，并获取单会话单活锁。

        参数说明：
            conversation_uuid (str): 会话 UUID。
            user_id (int): 用户 ID。
            conversation_type (str): 会话类型。
            assistant_message_uuid (str): 当前 AI 消息 UUID。

        返回值：
            AssistantRunMeta | None:
                获取锁成功时返回新建元数据；若已有运行中任务则返回 `None`。

        异常说明：
            ServiceException:
                - Redis 不可用时抛出 `SERVICE_UNAVAILABLE`。
        """

        now_ms = int(time.time() * 1000)
        run_id = str(uuid.uuid4())
        lock_key = _build_lock_key(conversation_uuid)
        meta_key = _build_meta_key(conversation_uuid)
        snapshot_key = _build_snapshot_key(conversation_uuid)
        events_key = _build_events_key(conversation_uuid)
        run_ttl_seconds = resolve_assistant_run_redis_ttl_seconds()

        try:
            lock_acquired = bool(
                self._redis.set(
                    lock_key,
                    run_id,
                    nx=True,
                    ex=run_ttl_seconds,
                )
            )
            if not lock_acquired:
                return None

            meta = AssistantRunMeta(
                run_id=run_id,
                conversation_uuid=conversation_uuid,
                user_id=user_id,
                conversation_type=conversation_type,
                assistant_message_uuid=assistant_message_uuid,
                status=AssistantRunStatus.RUNNING,
                owner_instance_id=self.owner_instance_id,
                last_event_id=None,
                cancel_requested=False,
                started_at=now_ms,
                updated_at=now_ms,
            )
            snapshot = AssistantRunSnapshot(
                answer_text="",
                thinking_text="",
                status=AssistantRunStatus.RUNNING,
                assistant_message_uuid=assistant_message_uuid,
                last_event_id=None,
            )

            pipeline = self._redis.pipeline()
            pipeline.delete(events_key)
            pipeline.hset(meta_key, mapping=self._serialize_meta(meta))
            pipeline.expire(meta_key, run_ttl_seconds)
            pipeline.set(snapshot_key, self._serialize_snapshot(snapshot), ex=run_ttl_seconds)
            pipeline.expire(lock_key, run_ttl_seconds)
            pipeline.execute()
            return meta
        except RedisError as exc:
            raise _raise_redis_unavailable("create_run", exc) from exc

    def get_run_meta(self, *, conversation_uuid: str) -> AssistantRunMeta | None:
        """
        功能描述：
            读取会话运行元数据。

        参数说明：
            conversation_uuid (str): 会话 UUID。

        返回值：
            AssistantRunMeta | None: 命中返回运行元数据，未命中返回 `None`。

        异常说明：
            ServiceException:
                - Redis 不可用时抛出 `SERVICE_UNAVAILABLE`。
        """

        try:
            raw_meta = self._redis.hgetall(_build_meta_key(conversation_uuid))
        except RedisError as exc:
            raise _raise_redis_unavailable("get_run_meta", exc) from exc
        if not raw_meta:
            return None
        return self._deserialize_meta(
            conversation_uuid=conversation_uuid,
            raw_meta=raw_meta,
        )

    def get_snapshot(self, *, conversation_uuid: str) -> AssistantRunSnapshot | None:
        """
        功能描述：
            读取运行快照。

        参数说明：
            conversation_uuid (str): 会话 UUID。

        返回值：
            AssistantRunSnapshot | None: 命中返回快照，否则返回 `None`。

        异常说明：
            ServiceException:
                - Redis 不可用或快照反序列化失败时抛出。
        """

        try:
            raw_snapshot = self._redis.get(_build_snapshot_key(conversation_uuid))
        except RedisError as exc:
            raise _raise_redis_unavailable("get_snapshot", exc) from exc
        if raw_snapshot is None:
            return None
        return self._deserialize_snapshot(raw_snapshot)

    def append_event(
            self,
            *,
            conversation_uuid: str,
            payload: AssistantResponse,
            snapshot: AssistantRunSnapshot,
    ) -> AssistantRunEvent:
        """
        功能描述：
            向 Redis Stream 写入一条事件，并同步更新 meta/snapshot。

        参数说明：
            conversation_uuid (str): 会话 UUID。
            payload (AssistantResponse): 要写入的标准事件负载。
            snapshot (AssistantRunSnapshot): 事件写入后的最新快照。

        返回值：
            AssistantRunEvent: 实际写入后的事件 ID 与负载。

        异常说明：
            ServiceException:
                - Redis 不可用时抛出 `SERVICE_UNAVAILABLE`。
        """

        now_ms = int(time.time() * 1000)
        events_key = _build_events_key(conversation_uuid)
        meta_key = _build_meta_key(conversation_uuid)
        snapshot_key = _build_snapshot_key(conversation_uuid)
        event_stream_max_len = resolve_assistant_run_event_stream_max_len()
        run_ttl_seconds = resolve_assistant_run_redis_ttl_seconds()

        try:
            event_id = self._redis.xadd(
                events_key,
                {
                    "payload": json.dumps(
                        payload.model_dump(mode="json", exclude_none=True),
                        ensure_ascii=False,
                    )
                },
                maxlen=event_stream_max_len,
                approximate=True,
            )
            normalized_event_id = _decode_redis_value(event_id) or "0-0"
            refreshed_snapshot = AssistantRunSnapshot(
                answer_text=snapshot.answer_text,
                thinking_text=snapshot.thinking_text,
                status=snapshot.status,
                assistant_message_uuid=snapshot.assistant_message_uuid,
                last_event_id=normalized_event_id,
            )
            pipeline = self._redis.pipeline()
            pipeline.hset(
                meta_key,
                mapping={
                    "last_event_id": normalized_event_id,
                    "status": snapshot.status.value,
                    "updated_at": now_ms,
                },
            )
            pipeline.expire(meta_key, run_ttl_seconds)
            pipeline.set(
                snapshot_key,
                self._serialize_snapshot(refreshed_snapshot),
                ex=run_ttl_seconds,
            )
            pipeline.expire(events_key, run_ttl_seconds)
            pipeline.execute()
            return AssistantRunEvent(
                event_id=normalized_event_id,
                payload=payload,
            )
        except RedisError as exc:
            raise _raise_redis_unavailable("append_event", exc) from exc

    def finalize_run(
            self,
            *,
            conversation_uuid: str,
            final_status: AssistantRunStatus,
            final_snapshot: AssistantRunSnapshot,
    ) -> None:
        """
        功能描述：
            将运行态切换到终态，并释放单活锁。

        参数说明：
            conversation_uuid (str): 会话 UUID。
            final_status (AssistantRunStatus): 最终运行态状态。
            final_snapshot (AssistantRunSnapshot): 终态快照。

        返回值：
            无。

        异常说明：
            ServiceException:
                - Redis 不可用时抛出 `SERVICE_UNAVAILABLE`。
        """

        now_ms = int(time.time() * 1000)
        lock_key = _build_lock_key(conversation_uuid)
        meta_key = _build_meta_key(conversation_uuid)
        snapshot_key = _build_snapshot_key(conversation_uuid)
        events_key = _build_events_key(conversation_uuid)
        terminal_ttl_seconds = resolve_assistant_run_terminal_ttl_seconds()

        try:
            pipeline = self._redis.pipeline()
            pipeline.delete(lock_key)
            pipeline.hset(
                meta_key,
                mapping={
                    "status": final_status.value,
                    "updated_at": now_ms,
                    "cancel_requested": "1"
                    if final_status == AssistantRunStatus.CANCELLED
                    else "0",
                },
            )
            pipeline.expire(meta_key, terminal_ttl_seconds)
            pipeline.set(
                snapshot_key,
                self._serialize_snapshot(
                    AssistantRunSnapshot(
                        answer_text=final_snapshot.answer_text,
                        thinking_text=final_snapshot.thinking_text,
                        status=final_status,
                        assistant_message_uuid=final_snapshot.assistant_message_uuid,
                        last_event_id=final_snapshot.last_event_id,
                    )
                ),
                ex=terminal_ttl_seconds,
            )
            pipeline.expire(events_key, terminal_ttl_seconds)
            pipeline.execute()
        except RedisError as exc:
            raise _raise_redis_unavailable("finalize_run", exc) from exc
        finally:
            self.remove_local_handle(conversation_uuid=conversation_uuid)

    def request_cancel(self, *, conversation_uuid: str) -> AssistantRunMeta | None:
        """
        功能描述：
            标记指定会话 run 为取消中，并通知本机句柄。

        参数说明：
            conversation_uuid (str): 会话 UUID。

        返回值：
            AssistantRunMeta | None:
                命中时返回更新后的运行元数据；未命中返回 `None`。

        异常说明：
            ServiceException:
                - Redis 不可用时抛出 `SERVICE_UNAVAILABLE`。
        """

        meta = self.get_run_meta(conversation_uuid=conversation_uuid)
        if meta is None:
            return None

        try:
            self._redis.hset(
                _build_meta_key(conversation_uuid),
                mapping={
                    "cancel_requested": "1",
                    "updated_at": int(time.time() * 1000),
                },
            )
        except RedisError as exc:
            raise _raise_redis_unavailable("request_cancel", exc) from exc

        handle = self.get_local_handle(conversation_uuid=conversation_uuid)
        if handle is not None:
            handle.cancel_event.set()
        return self.get_run_meta(conversation_uuid=conversation_uuid)

    def is_cancel_requested(self, *, conversation_uuid: str) -> bool:
        """
        功能描述：
            判断指定会话是否已收到取消请求。

        参数说明：
            conversation_uuid (str): 会话 UUID。

        返回值：
            bool: 已取消返回 `True`，否则返回 `False`。

        异常说明：
            ServiceException:
                - Redis 不可用时抛出 `SERVICE_UNAVAILABLE`。
        """

        meta = self.get_run_meta(conversation_uuid=conversation_uuid)
        if meta is None:
            return False
        return meta.cancel_requested

    def read_events(
            self,
            *,
            conversation_uuid: str,
            last_event_id: str,
            block_ms: int | None = None,
    ) -> list[AssistantRunEvent]:
        """
        功能描述：
            从 Redis Stream 读取指定事件 ID 之后的新事件。

        参数说明：
            conversation_uuid (str): 会话 UUID。
            last_event_id (str): 上次消费完成的事件 ID。
            block_ms (int | None): 可选阻塞时长（毫秒）。

        返回值：
            list[AssistantRunEvent]: 读取到的事件列表，按 Redis Stream 顺序返回。

        异常说明：
            ServiceException:
                - Redis 不可用时抛出 `SERVICE_UNAVAILABLE`。
        """

        stream_key = _build_events_key(conversation_uuid)
        try:
            result = self._redis.xread(
                streams={stream_key: last_event_id},
                block=block_ms,
            )
        except RedisError as exc:
            raise _raise_redis_unavailable("read_events", exc) from exc
        if not result:
            return []

        parsed_events: list[AssistantRunEvent] = []
        for _stream_name, stream_items in result:
            for raw_event_id, field_map in stream_items:
                payload_text = _decode_redis_value(field_map.get(b"payload") if isinstance(field_map, dict) else None)
                if payload_text is None and isinstance(field_map, dict):
                    payload_text = _decode_redis_value(field_map.get("payload"))
                if not payload_text:
                    continue
                try:
                    payload_dict = json.loads(payload_text)
                    payload = AssistantResponse.model_validate(payload_dict)
                except Exception:
                    continue
                parsed_events.append(
                    AssistantRunEvent(
                        event_id=_decode_redis_value(raw_event_id) or "0-0",
                        payload=payload,
                    )
                )
        return parsed_events

    def register_local_handle(
            self,
            *,
            conversation_uuid: str,
            handle: LocalRunHandle,
    ) -> None:
        """
        功能描述：
            注册当前进程内的运行句柄。

        参数说明：
            conversation_uuid (str): 会话 UUID。
            handle (LocalRunHandle): 本机运行句柄。

        返回值：
            无。

        异常说明：
            无。
        """

        with self._local_handles_lock:
            self._local_handles[conversation_uuid] = handle

    def get_local_handle(self, *, conversation_uuid: str) -> LocalRunHandle | None:
        """
        功能描述：
            获取当前进程内保存的运行句柄。

        参数说明：
            conversation_uuid (str): 会话 UUID。

        返回值：
            LocalRunHandle | None: 命中返回句柄，否则返回 `None`。

        异常说明：
            无。
        """

        with self._local_handles_lock:
            return self._local_handles.get(conversation_uuid)

    def remove_local_handle(self, *, conversation_uuid: str) -> None:
        """
        功能描述：
            删除当前进程内保存的运行句柄。

        参数说明：
            conversation_uuid (str): 会话 UUID。

        返回值：
            无。

        异常说明：
            无。
        """

        with self._local_handles_lock:
            self._local_handles.pop(conversation_uuid, None)

    @staticmethod
    def _serialize_meta(meta: AssistantRunMeta) -> dict[str, str]:
        """
        功能描述：
            将运行元数据序列化为 Redis Hash 字段。

        参数说明：
            meta (AssistantRunMeta): 运行元数据模型。

        返回值：
            dict[str, str]: 可直接写入 Redis Hash 的字段映射。

        异常说明：
            无。
        """

        return {
            "run_id": meta.run_id,
            "user_id": str(meta.user_id),
            "conversation_type": meta.conversation_type,
            "assistant_message_uuid": meta.assistant_message_uuid,
            "status": meta.status.value,
            "owner_instance_id": meta.owner_instance_id,
            "last_event_id": meta.last_event_id or "",
            "cancel_requested": "1" if meta.cancel_requested else "0",
            "started_at": str(meta.started_at),
            "updated_at": str(meta.updated_at),
        }

    @staticmethod
    def _deserialize_meta(
            *,
            conversation_uuid: str,
            raw_meta: dict[Any, Any],
    ) -> AssistantRunMeta:
        """
        功能描述：
            将 Redis Hash 字段还原为运行元数据模型。

        参数说明：
            conversation_uuid (str): 会话 UUID。
            raw_meta (dict[Any, Any]): Redis 原始字段映射。

        返回值：
            AssistantRunMeta: 反序列化后的运行元数据。

        异常说明：
            ServiceException:
                - 字段缺失或非法时抛出 `SERVICE_UNAVAILABLE`。
        """

        normalized_fields = {
            _decode_redis_value(key) or "": _decode_redis_value(value)
            for key, value in raw_meta.items()
        }
        try:
            return AssistantRunMeta(
                run_id=normalized_fields["run_id"] or "",
                conversation_uuid=conversation_uuid,
                user_id=int(normalized_fields["user_id"] or "0"),
                conversation_type=normalized_fields["conversation_type"] or "",
                assistant_message_uuid=normalized_fields["assistant_message_uuid"] or "",
                status=AssistantRunStatus(normalized_fields["status"] or AssistantRunStatus.RUNNING.value),
                owner_instance_id=normalized_fields["owner_instance_id"] or "",
                last_event_id=(normalized_fields.get("last_event_id") or "") or None,
                cancel_requested=_to_bool(normalized_fields.get("cancel_requested")),
                started_at=int(normalized_fields["started_at"] or "0"),
                updated_at=int(normalized_fields["updated_at"] or "0"),
            )
        except Exception as exc:
            raise ServiceException(
                code=ResponseCode.SERVICE_UNAVAILABLE,
                message="助手运行态元数据损坏",
                data={"conversation_uuid": conversation_uuid, "detail": str(exc)},
            ) from exc

    @staticmethod
    def _serialize_snapshot(snapshot: AssistantRunSnapshot) -> str:
        """
        功能描述：
            将运行快照序列化为 JSON 字符串。

        参数说明：
            snapshot (AssistantRunSnapshot): 快照模型。

        返回值：
            str: JSON 字符串。

        异常说明：
            无。
        """

        return json.dumps(
            {
                "answer_text": snapshot.answer_text,
                "thinking_text": snapshot.thinking_text,
                "status": snapshot.status.value,
                "assistant_message_uuid": snapshot.assistant_message_uuid,
                "last_event_id": snapshot.last_event_id,
            },
            ensure_ascii=False,
        )

    @staticmethod
    def _deserialize_snapshot(raw_snapshot: Any) -> AssistantRunSnapshot:
        """
        功能描述：
            将 Redis 中的 JSON 快照还原为模型对象。

        参数说明：
            raw_snapshot (Any): Redis 返回的原始值。

        返回值：
            AssistantRunSnapshot: 反序列化后的快照对象。

        异常说明：
            ServiceException:
                - JSON 非法或字段缺失时抛出 `SERVICE_UNAVAILABLE`。
        """

        snapshot_text = _decode_redis_value(raw_snapshot) or ""
        try:
            payload = json.loads(snapshot_text)
            return AssistantRunSnapshot(
                answer_text=str(payload.get("answer_text") or ""),
                thinking_text=str(payload.get("thinking_text") or ""),
                status=AssistantRunStatus(
                    str(payload.get("status") or AssistantRunStatus.RUNNING.value)
                ),
                assistant_message_uuid=str(payload.get("assistant_message_uuid") or ""),
                last_event_id=(
                        str(payload.get("last_event_id") or "").strip()
                        or None
                ),
            )
        except Exception as exc:
            raise ServiceException(
                code=ResponseCode.SERVICE_UNAVAILABLE,
                message="助手运行态快照损坏",
                data={"detail": str(exc)},
            ) from exc
