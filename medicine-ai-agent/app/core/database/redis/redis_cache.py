from __future__ import annotations

import json
from collections.abc import Collection
from typing import Any

from redis import Redis
from redis.exceptions import RedisError

from app.core.codes import ResponseCode
from app.core.database.redis.config import get_redis_connection
from app.core.exception.exceptions import ServiceException

DEFAULT_SCAN_COUNT = 1000  # Redis SCAN 默认批量大小


def _decode_key(key: Any) -> str:
    """将 Redis 返回键统一转换为字符串。

    Args:
        key: Redis 返回的键，可能是 `bytes` 或其他类型。

    Returns:
        str: 统一后的字符串键。
    """
    if isinstance(key, bytes):
        return key.decode("utf-8")
    return str(key)


def _raise_redis_operation_error(*, operation: str, exc: Exception) -> ServiceException:
    """构造统一的 Redis 操作异常。

    Args:
        operation: Redis 操作名称。
        exc: 原始异常对象。

    Returns:
        ServiceException: 统一封装后的业务异常。
    """
    return ServiceException(
        code=ResponseCode.OPERATION_FAILED,
        message=f"Redis {operation} 失败: {exc}",
    )


class RedisCache:
    """Redis Value 结构操作封装。"""

    def __init__(self, redis_client: Redis | None = None) -> None:
        """初始化 Redis Value 操作封装。

        Args:
            redis_client: 可选 Redis 客户端，未传时使用全局连接。
        """
        self._redis = redis_client or get_redis_connection()

    def set_cache_object(self, key: str, value: Any, timeout: int | None = None) -> None:
        """写入普通值。

        Args:
            key: Redis 键。
            value: 写入值。
            timeout: 过期秒数，未传表示不过期。

        Raises:
            ServiceException: Redis 写入失败时抛出。
        """
        try:
            if timeout is None:
                self._redis.set(key, value)
                return
            self._redis.setex(key, timeout, value)
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="set", exc=exc) from exc

    def get_cache_object(self, key: str) -> Any:
        """读取普通值。

        Args:
            key: Redis 键。

        Returns:
            Any: Redis 原始值，不存在时返回 `None`。

        Raises:
            ServiceException: Redis 读取失败时抛出。
        """
        try:
            return self._redis.get(key)
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="get", exc=exc) from exc

    def set_json(self, key: str, value: Any, timeout: int | None = None) -> None:
        """写入 JSON 值（安全 JSON，不携带类型信息）。

        Args:
            key: Redis 键。
            value: 可 JSON 序列化的值。
            timeout: 过期秒数，未传表示不过期。

        Raises:
            ServiceException: JSON 序列化失败或 Redis 写入失败时抛出。
        """
        try:
            payload = json.dumps(value, ensure_ascii=False)
        except (TypeError, ValueError) as exc:
            raise ServiceException(
                code=ResponseCode.BAD_REQUEST,
                message=f"JSON 序列化失败: {exc}",
            ) from exc
        self.set_cache_object(key, payload, timeout=timeout)

    def get_json(self, key: str) -> Any:
        """读取 JSON 值并反序列化。

        Args:
            key: Redis 键。

        Returns:
            Any: 反序列化后的对象，不存在时返回 `None`。

        Raises:
            ServiceException: JSON 反序列化失败或 Redis 读取失败时抛出。
        """
        payload = self.get_cache_object(key)
        if payload is None:
            return None
        text = payload.decode("utf-8") if isinstance(payload, bytes) else str(payload)
        try:
            return json.loads(text)
        except json.JSONDecodeError as exc:
            raise ServiceException(
                code=ResponseCode.OPERATION_FAILED,
                message=f"JSON 反序列化失败: {exc}",
            ) from exc

    def increment(self, key: str) -> int:
        """对键执行自增 1。

        Args:
            key: Redis 键。

        Returns:
            int: 自增后的值。

        Raises:
            ServiceException: Redis 自增失败时抛出。
        """
        try:
            return int(self._redis.incr(key))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="incr", exc=exc) from exc

    def increment_by(self, key: str, delta: int) -> int:
        """对键执行指定增量自增。

        Args:
            key: Redis 键。
            delta: 增量值。

        Returns:
            int: 自增后的值。

        Raises:
            ServiceException: Redis 自增失败时抛出。
        """
        try:
            return int(self._redis.incrby(key, delta))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="incrby", exc=exc) from exc

    def expire(self, key: str, timeout: int) -> bool:
        """设置键过期时间（秒）。

        Args:
            key: Redis 键。
            timeout: 过期秒数。

        Returns:
            bool: 设置成功返回 `True`，否则返回 `False`。

        Raises:
            ServiceException: Redis 设置过期时间失败时抛出。
        """
        try:
            return bool(self._redis.expire(key, timeout))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="expire", exc=exc) from exc

    def delete_object(self, key: str) -> None:
        """删除单个键。

        Args:
            key: Redis 键。

        Raises:
            ServiceException: Redis 删除失败时抛出。
        """
        try:
            self._redis.delete(key)
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="delete", exc=exc) from exc

    def delete_objects(self, keys: Collection[str]) -> int:
        """批量删除键。

        Args:
            keys: 待删除键集合。

        Returns:
            int: 实际删除的键数量。

        Raises:
            ServiceException: Redis 批量删除失败时抛出。
        """
        key_list = list(keys)
        if not key_list:
            return 0
        try:
            return int(self._redis.delete(*key_list))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="delete_many", exc=exc) from exc

    def has_key(self, key: str) -> bool:
        """判断键是否存在。

        Args:
            key: Redis 键。

        Returns:
            bool: 存在返回 `True`，否则返回 `False`。

        Raises:
            ServiceException: Redis 查询失败时抛出。
        """
        try:
            return bool(self._redis.exists(key))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="exists", exc=exc) from exc

    def get_expire(self, key: str) -> int:
        """读取键剩余过期时间（秒）。

        Args:
            key: Redis 键。

        Returns:
            int: 剩余秒数；`-1` 表示未设置过期，`-2` 表示键不存在。

        Raises:
            ServiceException: Redis 查询 TTL 失败时抛出。
        """
        try:
            return int(self._redis.ttl(key))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="ttl", exc=exc) from exc

    def keys(self, key_pattern: str) -> list[str]:
        """按模式读取键列表（直接 KEYS 命令）。

        Args:
            key_pattern: 键匹配模式。

        Returns:
            list[str]: 匹配到的键列表。

        Raises:
            ServiceException: Redis 查询失败时抛出。
        """
        try:
            return [_decode_key(item) for item in self._redis.keys(key_pattern)]
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="keys", exc=exc) from exc

    def scan_keys(self, key_pattern: str, *, count: int = DEFAULT_SCAN_COUNT) -> list[str]:
        """按模式扫描键列表（推荐生产使用）。

        Args:
            key_pattern: 键匹配模式。
            count: 每次 SCAN 的建议批次大小。

        Returns:
            list[str]: 匹配到的键列表。

        Raises:
            ServiceException: Redis 扫描失败时抛出。
        """
        cursor = 0
        keys: list[str] = []
        try:
            while True:
                cursor, batch = self._redis.scan(
                    cursor=cursor,
                    match=key_pattern,
                    count=count,
                )
                keys.extend(_decode_key(item) for item in batch)
                if int(cursor) == 0:
                    break
            return keys
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="scan", exc=exc) from exc

    def scan_keys_with_values(self, key_pattern: str, *, count: int = DEFAULT_SCAN_COUNT) -> dict[str, Any]:
        """按模式扫描键并批量读取值。

        Args:
            key_pattern: 键匹配模式。
            count: 每次 SCAN 的建议批次大小。

        Returns:
            dict[str, Any]: 键与对应值的映射。

        Raises:
            ServiceException: Redis 扫描或批量读取失败时抛出。
        """
        matched_keys = self.scan_keys(key_pattern, count=count)
        if not matched_keys:
            return {}
        try:
            values = self._redis.mget(matched_keys)
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="mget", exc=exc) from exc
        return {
            key: values[index] if index < len(values) else None
            for index, key in enumerate(matched_keys)
        }

    def eval_script(self, script: str, numkeys: int, *values: Any) -> Any:
        """执行 Lua 脚本。

        Args:
            script: Lua 脚本文本。
            numkeys: `values` 中前 `numkeys` 个参数作为 `KEYS`。
            *values: 传给 Lua 的参数，包含 `KEYS` 与 `ARGV`。

        Returns:
            Any: Lua 脚本执行结果。

        Raises:
            ServiceException: Lua 执行失败时抛出。
        """
        try:
            return self._redis.eval(script, numkeys, *values)
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="eval", exc=exc) from exc
