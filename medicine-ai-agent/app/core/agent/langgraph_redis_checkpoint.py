from __future__ import annotations

import os
import pickle
import time
from collections.abc import AsyncIterator, Iterator, Sequence
from typing import Any

from langchain_core.runnables import RunnableConfig
from langgraph.checkpoint.base import (
    WRITES_IDX_MAP,
    BaseCheckpointSaver,
    ChannelVersions,
    Checkpoint,
    CheckpointMetadata,
    CheckpointTuple,
    get_checkpoint_id,
    get_checkpoint_metadata,
)
from redis import Redis
from redis.exceptions import RedisError

from app.core.codes import ResponseCode
from app.core.database.redis.config import get_redis_connection
from app.core.exception.exceptions import ServiceException

DEFAULT_LANGGRAPH_CHECKPOINT_KEY_PREFIX = "langgraph:checkpoint"
"""LangGraph checkpoint Redis key 前缀。"""

DEFAULT_LANGGRAPH_CHECKPOINT_TTL_SECONDS = 7200
"""LangGraph checkpoint 默认保留时长（秒）。"""


def _resolve_checkpoint_ttl_seconds() -> int:
    """
    功能描述：
        解析 LangGraph checkpoint 的 Redis TTL。

    参数说明：
        无。

    返回值：
        int: 最终生效的 TTL 秒数。

    异常说明：
        无；非法值时回退默认值。
    """

    raw_value = os.getenv("ASSISTANT_RUN_REDIS_TTL_SECONDS")
    if raw_value is None or not raw_value.strip():
        return DEFAULT_LANGGRAPH_CHECKPOINT_TTL_SECONDS
    try:
        resolved_value = int(raw_value)
    except ValueError:
        return DEFAULT_LANGGRAPH_CHECKPOINT_TTL_SECONDS
    if resolved_value <= 0:
        return DEFAULT_LANGGRAPH_CHECKPOINT_TTL_SECONDS
    return resolved_value


def _raise_redis_unavailable(operation: str, exc: Exception) -> ServiceException:
    """
    功能描述：
        将 Redis 异常统一包装为服务不可用异常。

    参数说明：
        operation (str): 当前执行的 Redis 操作名称。
        exc (Exception): 原始异常对象。

    返回值：
        ServiceException: 统一包装后的业务异常。

    异常说明：
        无；当前函数只负责构造异常对象。
    """

    return ServiceException(
        code=ResponseCode.SERVICE_UNAVAILABLE,
        message=f"LangGraph checkpoint Redis 不可用: {operation}",
        data={"operation": operation, "detail": str(exc)},
    )


def _serialize_pickle(value: Any) -> bytes:
    """
    功能描述：
        将 Python 对象序列化为二进制字节串。

    参数说明：
        value (Any): 待序列化对象。

    返回值：
        bytes: pickle 序列化后的二进制内容。

    异常说明：
        无；由调用方统一处理底层异常。
    """

    return pickle.dumps(value, protocol=pickle.HIGHEST_PROTOCOL)


def _deserialize_pickle(raw_value: bytes | None) -> Any:
    """
    功能描述：
        将 Redis 中的二进制内容反序列化为 Python 对象。

    参数说明：
        raw_value (bytes | None): Redis 返回的原始字节串。

    返回值：
        Any: 反序列化后的对象；原始值为空时返回 `None`。

    异常说明：
        无；由调用方统一处理底层异常。
    """

    if raw_value is None:
        return None
    return pickle.loads(raw_value)


def _normalize_checkpoint_ns(config: RunnableConfig) -> str:
    """
    功能描述：
        从 RunnableConfig 中提取 checkpoint namespace。

    参数说明：
        config (RunnableConfig): LangGraph runnable config。

    返回值：
        str: 标准化后的 checkpoint namespace；未传时返回空字符串。

    异常说明：
        无。
    """

    return str(config["configurable"].get("checkpoint_ns", "") or "")


def _build_index_key(*, key_prefix: str, thread_id: str, checkpoint_ns: str) -> str:
    """构造 checkpoint 索引 key。"""

    return f"{key_prefix}:index:{thread_id}:{checkpoint_ns}"


def _build_checkpoint_key(
        *,
        key_prefix: str,
        thread_id: str,
        checkpoint_ns: str,
        checkpoint_id: str,
) -> str:
    """构造单个 checkpoint 数据 key。"""

    return f"{key_prefix}:data:{thread_id}:{checkpoint_ns}:{checkpoint_id}"


def _build_writes_key(
        *,
        key_prefix: str,
        thread_id: str,
        checkpoint_ns: str,
        checkpoint_id: str,
) -> str:
    """构造单个 checkpoint 的 pending writes key。"""

    return f"{key_prefix}:writes:{thread_id}:{checkpoint_ns}:{checkpoint_id}"


def _build_blob_key(
        *,
        key_prefix: str,
        thread_id: str,
        checkpoint_ns: str,
        channel: str,
        version: str | int | float,
) -> str:
    """构造 channel blob key。"""

    return f"{key_prefix}:blob:{thread_id}:{checkpoint_ns}:{channel}:{version}"


class RedisCheckpointSaver(BaseCheckpointSaver[str]):
    """
    LangGraph Redis checkpoint saver。

    说明：
    - 仅依赖当前项目已有的 `redis-py` 连接；
    - 支持 `interrupt()` 所需的 `get_tuple/put/put_writes/delete_thread`；
    - 数据内部使用 pickle 持久化，业务侧不直接读取这些 key。
    """

    def __init__(
            self,
            *,
            redis_client: Redis | None = None,
            key_prefix: str = DEFAULT_LANGGRAPH_CHECKPOINT_KEY_PREFIX,
            ttl_seconds: int | None = None,
            serde: Any | None = None,
    ) -> None:
        """
        功能描述：
            初始化 Redis checkpoint saver。

        参数说明：
            redis_client (Redis | None): 可选 Redis 连接；为空时复用全局连接。
            key_prefix (str): Redis key 前缀。
            ttl_seconds (int | None): 数据保留秒数；为空时读取默认配置。
            serde (Any | None): LangGraph serializer，可选。

        返回值：
            无。

        异常说明：
            无。
        """

        super().__init__(serde=serde)
        self._redis = redis_client or get_redis_connection()
        self._key_prefix = key_prefix
        self._ttl_seconds = ttl_seconds or _resolve_checkpoint_ttl_seconds()

    def _load_blobs(
            self,
            *,
            thread_id: str,
            checkpoint_ns: str,
            versions: ChannelVersions,
    ) -> dict[str, Any]:
        """
        功能描述：
            根据 channel_versions 读取 checkpoint channel_values。

        参数说明：
            thread_id (str): thread_id。
            checkpoint_ns (str): checkpoint namespace。
            versions (ChannelVersions): channel 版本映射。

        返回值：
            dict[str, Any]: 反序列化后的 channel_values。

        异常说明：
            ServiceException: Redis 不可用时抛出。
        """

        if not versions:
            return {}

        pipeline = self._redis.pipeline()
        ordered_channels: list[tuple[str, str | int | float]] = []
        for channel_name, version in versions.items():
            ordered_channels.append((channel_name, version))
            pipeline.get(
                _build_blob_key(
                    key_prefix=self._key_prefix,
                    thread_id=thread_id,
                    checkpoint_ns=checkpoint_ns,
                    channel=channel_name,
                    version=version,
                )
            )

        try:
            raw_blobs = pipeline.execute()
        except RedisError as exc:
            raise _raise_redis_unavailable("checkpoint.load_blobs", exc) from exc

        channel_values: dict[str, Any] = {}
        for (channel_name, _version), raw_blob in zip(ordered_channels, raw_blobs, strict=False):
            typed_blob = _deserialize_pickle(raw_blob)
            if not typed_blob:
                continue
            if typed_blob[0] == "empty":
                continue
            channel_values[channel_name] = self.serde.loads_typed(typed_blob)
        return channel_values

    def _load_pending_writes(
            self,
            *,
            thread_id: str,
            checkpoint_ns: str,
            checkpoint_id: str,
    ) -> list[tuple[str, str, Any]]:
        """
        功能描述：
            读取单个 checkpoint 下的 pending writes。

        参数说明：
            thread_id (str): thread_id。
            checkpoint_ns (str): checkpoint namespace。
            checkpoint_id (str): checkpoint_id。

        返回值：
            list[tuple[str, str, Any]]: LangGraph 期望格式的 pending writes。

        异常说明：
            ServiceException: Redis 不可用时抛出。
        """

        try:
            raw_writes = self._redis.hgetall(
                _build_writes_key(
                    key_prefix=self._key_prefix,
                    thread_id=thread_id,
                    checkpoint_ns=checkpoint_ns,
                    checkpoint_id=checkpoint_id,
                )
            )
        except RedisError as exc:
            raise _raise_redis_unavailable("checkpoint.load_writes", exc) from exc

        if not raw_writes:
            return []

        normalized_writes: list[tuple[int, tuple[str, str, Any]]] = []
        for raw_payload in raw_writes.values():
            write_payload = _deserialize_pickle(raw_payload)
            if not isinstance(write_payload, tuple) or len(write_payload) != 5:
                continue
            task_id, channel_name, typed_value, _task_path, write_index = write_payload
            normalized_writes.append(
                (
                    int(write_index),
                    (
                        str(task_id),
                        str(channel_name),
                        self.serde.loads_typed(typed_value),
                    ),
                )
            )

        normalized_writes.sort(key=lambda item: item[0])
        return [item[1] for item in normalized_writes]

    def _get_latest_checkpoint_id(
            self,
            *,
            thread_id: str,
            checkpoint_ns: str,
    ) -> str | None:
        """
        功能描述：
            获取某个 thread/namespace 下最新的 checkpoint_id。

        参数说明：
            thread_id (str): thread_id。
            checkpoint_ns (str): checkpoint namespace。

        返回值：
            str | None: 最新 checkpoint_id；不存在时返回 `None`。

        异常说明：
            ServiceException: Redis 不可用时抛出。
        """

        try:
            raw_checkpoint_ids = self._redis.zrevrange(
                _build_index_key(
                    key_prefix=self._key_prefix,
                    thread_id=thread_id,
                    checkpoint_ns=checkpoint_ns,
                ),
                0,
                0,
            )
        except RedisError as exc:
            raise _raise_redis_unavailable("checkpoint.latest", exc) from exc

        if not raw_checkpoint_ids:
            return None
        raw_checkpoint_id = raw_checkpoint_ids[0]
        if isinstance(raw_checkpoint_id, bytes):
            return raw_checkpoint_id.decode("utf-8")
        return str(raw_checkpoint_id)

    def get_tuple(self, config: RunnableConfig) -> CheckpointTuple | None:
        """
        功能描述：
            读取指定 config 对应的 checkpoint tuple。

        参数说明：
            config (RunnableConfig): LangGraph runnable config。

        返回值：
            CheckpointTuple | None: 命中时返回 checkpoint tuple，否则返回 `None`。

        异常说明：
            ServiceException: Redis 不可用时抛出。
        """

        thread_id = str(config["configurable"]["thread_id"])
        checkpoint_ns = _normalize_checkpoint_ns(config)
        checkpoint_id = get_checkpoint_id(config) or self._get_latest_checkpoint_id(
            thread_id=thread_id,
            checkpoint_ns=checkpoint_ns,
        )
        if not checkpoint_id:
            return None

        checkpoint_key = _build_checkpoint_key(
            key_prefix=self._key_prefix,
            thread_id=thread_id,
            checkpoint_ns=checkpoint_ns,
            checkpoint_id=checkpoint_id,
        )
        try:
            raw_payload = self._redis.get(checkpoint_key)
        except RedisError as exc:
            raise _raise_redis_unavailable("checkpoint.get", exc) from exc

        checkpoint_payload = _deserialize_pickle(raw_payload)
        if not isinstance(checkpoint_payload, dict):
            return None

        typed_checkpoint = checkpoint_payload.get("checkpoint")
        typed_metadata = checkpoint_payload.get("metadata")
        parent_checkpoint_id = checkpoint_payload.get("parent_checkpoint_id")
        if typed_checkpoint is None or typed_metadata is None:
            return None

        checkpoint = self.serde.loads_typed(typed_checkpoint)
        metadata = self.serde.loads_typed(typed_metadata)
        return CheckpointTuple(
            config={
                "configurable": {
                    "thread_id": thread_id,
                    "checkpoint_ns": checkpoint_ns,
                    "checkpoint_id": checkpoint_id,
                }
            },
            checkpoint={
                **checkpoint,
                "channel_values": self._load_blobs(
                    thread_id=thread_id,
                    checkpoint_ns=checkpoint_ns,
                    versions=checkpoint["channel_versions"],
                ),
            },
            metadata=metadata,
            pending_writes=self._load_pending_writes(
                thread_id=thread_id,
                checkpoint_ns=checkpoint_ns,
                checkpoint_id=checkpoint_id,
            ),
            parent_config=(
                {
                    "configurable": {
                        "thread_id": thread_id,
                        "checkpoint_ns": checkpoint_ns,
                        "checkpoint_id": str(parent_checkpoint_id),
                    }
                }
                if parent_checkpoint_id
                else None
            ),
        )

    def list(
            self,
            config: RunnableConfig | None,
            *,
            filter: dict[str, Any] | None = None,
            before: RunnableConfig | None = None,
            limit: int | None = None,
    ) -> Iterator[CheckpointTuple]:
        """
        功能描述：
            按条件遍历 checkpoint tuple。

        参数说明：
            config (RunnableConfig | None): 基础查询 config；当前实现要求提供 thread_id。
            filter (dict[str, Any] | None): metadata 过滤条件。
            before (RunnableConfig | None): checkpoint_id 上界过滤。
            limit (int | None): 最大返回数量。

        返回值：
            Iterator[CheckpointTuple]: 匹配结果迭代器。

        异常说明：
            ServiceException: Redis 不可用时抛出。
        """

        if config is None:
            return iter(())

        thread_id = str(config["configurable"]["thread_id"])
        checkpoint_ns = _normalize_checkpoint_ns(config)
        before_checkpoint_id = get_checkpoint_id(before) if before is not None else None
        index_key = _build_index_key(
            key_prefix=self._key_prefix,
            thread_id=thread_id,
            checkpoint_ns=checkpoint_ns,
        )

        try:
            raw_checkpoint_ids = self._redis.zrevrange(index_key, 0, -1)
        except RedisError as exc:
            raise _raise_redis_unavailable("checkpoint.list", exc) from exc

        remaining = limit
        checkpoint_tuples: list[CheckpointTuple] = []
        for raw_checkpoint_id in raw_checkpoint_ids:
            checkpoint_id = (
                raw_checkpoint_id.decode("utf-8")
                if isinstance(raw_checkpoint_id, bytes)
                else str(raw_checkpoint_id)
            )
            if before_checkpoint_id and checkpoint_id >= before_checkpoint_id:
                continue

            checkpoint_tuple = self.get_tuple(
                {
                    "configurable": {
                        "thread_id": thread_id,
                        "checkpoint_ns": checkpoint_ns,
                        "checkpoint_id": checkpoint_id,
                    }
                }
            )
            if checkpoint_tuple is None:
                continue

            if filter and not all(
                    checkpoint_tuple.metadata.get(filter_key) == filter_value
                    for filter_key, filter_value in filter.items()
            ):
                continue

            checkpoint_tuples.append(checkpoint_tuple)
            if remaining is not None:
                remaining -= 1
                if remaining <= 0:
                    break

        return iter(checkpoint_tuples)

    def put(
            self,
            config: RunnableConfig,
            checkpoint: Checkpoint,
            metadata: CheckpointMetadata,
            new_versions: ChannelVersions,
    ) -> RunnableConfig:
        """
        功能描述：
            保存 checkpoint 与其 metadata。

        参数说明：
            config (RunnableConfig): LangGraph runnable config。
            checkpoint (Checkpoint): 待保存 checkpoint。
            metadata (CheckpointMetadata): checkpoint metadata。
            new_versions (ChannelVersions): 本次新增 channel version。

        返回值：
            RunnableConfig: 指向新 checkpoint_id 的 config。

        异常说明：
            ServiceException: Redis 不可用时抛出。
        """

        thread_id = str(config["configurable"]["thread_id"])
        checkpoint_ns = _normalize_checkpoint_ns(config)
        checkpoint_id = str(checkpoint["id"])
        checkpoint_copy = checkpoint.copy()
        channel_values = dict(checkpoint_copy.pop("channel_values", {}))
        checkpoint_key = _build_checkpoint_key(
            key_prefix=self._key_prefix,
            thread_id=thread_id,
            checkpoint_ns=checkpoint_ns,
            checkpoint_id=checkpoint_id,
        )
        index_key = _build_index_key(
            key_prefix=self._key_prefix,
            thread_id=thread_id,
            checkpoint_ns=checkpoint_ns,
        )

        pipeline = self._redis.pipeline()
        for channel_name, version in new_versions.items():
            typed_blob = (
                self.serde.dumps_typed(channel_values[channel_name])
                if channel_name in channel_values
                else ("empty", b"")
            )
            pipeline.set(
                _build_blob_key(
                    key_prefix=self._key_prefix,
                    thread_id=thread_id,
                    checkpoint_ns=checkpoint_ns,
                    channel=channel_name,
                    version=version,
                ),
                _serialize_pickle(typed_blob),
                ex=self._ttl_seconds,
            )

        checkpoint_payload = {
            "checkpoint": self.serde.dumps_typed(checkpoint_copy),
            "metadata": self.serde.dumps_typed(get_checkpoint_metadata(config, metadata)),
            "parent_checkpoint_id": config["configurable"].get("checkpoint_id"),
        }
        pipeline.set(
            checkpoint_key,
            _serialize_pickle(checkpoint_payload),
            ex=self._ttl_seconds,
        )
        pipeline.zadd(index_key, {checkpoint_id: float(time.time())})
        pipeline.expire(index_key, self._ttl_seconds)

        try:
            pipeline.execute()
        except RedisError as exc:
            raise _raise_redis_unavailable("checkpoint.put", exc) from exc

        return {
            "configurable": {
                "thread_id": thread_id,
                "checkpoint_ns": checkpoint_ns,
                "checkpoint_id": checkpoint_id,
            }
        }

    def put_writes(
            self,
            config: RunnableConfig,
            writes: Sequence[tuple[str, Any]],
            task_id: str,
            task_path: str = "",
    ) -> None:
        """
        功能描述：
            保存单个 checkpoint 的 pending writes。

        参数说明：
            config (RunnableConfig): LangGraph runnable config。
            writes (Sequence[tuple[str, Any]]): writes 列表。
            task_id (str): task_id。
            task_path (str): task_path。

        返回值：
            无。

        异常说明：
            ServiceException: Redis 不可用时抛出。
        """

        thread_id = str(config["configurable"]["thread_id"])
        checkpoint_ns = _normalize_checkpoint_ns(config)
        checkpoint_id = str(config["configurable"]["checkpoint_id"])
        writes_key = _build_writes_key(
            key_prefix=self._key_prefix,
            thread_id=thread_id,
            checkpoint_ns=checkpoint_ns,
            checkpoint_id=checkpoint_id,
        )

        try:
            existing_fields = {
                (
                    raw_field.decode("utf-8")
                    if isinstance(raw_field, bytes)
                    else str(raw_field)
                )
                for raw_field in self._redis.hkeys(writes_key)
            }
        except RedisError as exc:
            raise _raise_redis_unavailable("checkpoint.put_writes.keys", exc) from exc

        pipeline = self._redis.pipeline()
        for index, (channel_name, value) in enumerate(writes):
            write_index = WRITES_IDX_MAP.get(channel_name, index)
            field_name = f"{task_id}:{write_index}"
            if write_index >= 0 and field_name in existing_fields:
                continue

            pipeline.hset(
                writes_key,
                field_name,
                _serialize_pickle(
                    (
                        task_id,
                        channel_name,
                        self.serde.dumps_typed(value),
                        task_path,
                        write_index,
                    )
                ),
            )
        pipeline.expire(writes_key, self._ttl_seconds)

        try:
            pipeline.execute()
        except RedisError as exc:
            raise _raise_redis_unavailable("checkpoint.put_writes", exc) from exc

    def delete_thread(self, thread_id: str) -> None:
        """
        功能描述：
            删除某个 thread_id 下的所有 checkpoint 数据。

        参数说明：
            thread_id (str): thread_id。

        返回值：
            无。

        异常说明：
            ServiceException: Redis 不可用时抛出。
        """

        key_pattern = f"{self._key_prefix}:*:{thread_id}:*"
        try:
            matched_keys = list(self._redis.scan_iter(match=key_pattern))
            if matched_keys:
                self._redis.delete(*matched_keys)
        except RedisError as exc:
            raise _raise_redis_unavailable("checkpoint.delete_thread", exc) from exc

    def delete_for_runs(self, run_ids: Sequence[str]) -> None:
        """当前项目未使用 run_id 级 checkpoint 删除，先保持空实现。"""

        _ = run_ids

    def copy_thread(self, source_thread_id: str, target_thread_id: str) -> None:
        """当前项目未使用 thread 拷贝，先保持空实现。"""

        _ = source_thread_id
        _ = target_thread_id

    def prune(self, thread_ids: Sequence[str], *, strategy: str = "keep_latest") -> None:
        """当前项目未使用 checkpoint 裁剪，先保持空实现。"""

        _ = thread_ids
        _ = strategy

    async def aget_tuple(self, config: RunnableConfig) -> CheckpointTuple | None:
        """异步读取 checkpoint tuple。"""

        return self.get_tuple(config)

    async def alist(
            self,
            config: RunnableConfig | None,
            *,
            filter: dict[str, Any] | None = None,
            before: RunnableConfig | None = None,
            limit: int | None = None,
    ) -> AsyncIterator[CheckpointTuple]:
        """异步遍历 checkpoint tuple。"""

        for item in self.list(config, filter=filter, before=before, limit=limit):
            yield item

    async def aput(
            self,
            config: RunnableConfig,
            checkpoint: Checkpoint,
            metadata: CheckpointMetadata,
            new_versions: ChannelVersions,
    ) -> RunnableConfig:
        """异步保存 checkpoint。"""

        return self.put(config, checkpoint, metadata, new_versions)

    async def aput_writes(
            self,
            config: RunnableConfig,
            writes: Sequence[tuple[str, Any]],
            task_id: str,
            task_path: str = "",
    ) -> None:
        """异步保存 pending writes。"""

        self.put_writes(config, writes, task_id, task_path)

    async def adelete_thread(self, thread_id: str) -> None:
        """异步删除 thread checkpoint 数据。"""

        self.delete_thread(thread_id)

    async def adelete_for_runs(self, run_ids: Sequence[str]) -> None:
        """异步删除 run_id 对应 checkpoint 数据。"""

        self.delete_for_runs(run_ids)

    async def acopy_thread(self, source_thread_id: str, target_thread_id: str) -> None:
        """异步拷贝 thread checkpoint 数据。"""

        self.copy_thread(source_thread_id, target_thread_id)

    async def aprune(self, thread_ids: Sequence[str], *, strategy: str = "keep_latest") -> None:
        """异步裁剪 checkpoint 数据。"""

        self.prune(thread_ids, strategy=strategy)

    def get_next_version(self, current: str | None, channel: None) -> str:
        """
        功能描述：
            生成下一个递增的 channel version。

        参数说明：
            current (str | None): 当前版本号。
            channel (None): LangGraph 保留参数，当前未使用。

        返回值：
            str: 单调递增的字符串版本号。

        异常说明：
            无。
        """

        _ = channel
        if current is None:
            current_version = 0
        elif isinstance(current, int):
            current_version = current
        else:
            current_version = int(str(current).split(".")[0])
        next_version = current_version + 1
        return f"{next_version:032}.{time.time_ns():020}"


_REDIS_CHECKPOINT_SAVER = RedisCheckpointSaver()
"""项目级共享的 LangGraph Redis checkpoint saver。"""

__all__ = [
    "DEFAULT_LANGGRAPH_CHECKPOINT_KEY_PREFIX",
    "DEFAULT_LANGGRAPH_CHECKPOINT_TTL_SECONDS",
    "RedisCheckpointSaver",
    "_REDIS_CHECKPOINT_SAVER",
]
