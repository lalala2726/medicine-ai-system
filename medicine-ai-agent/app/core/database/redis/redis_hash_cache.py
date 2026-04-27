from __future__ import annotations

from typing import Any

from redis import Redis
from redis.exceptions import RedisError

from app.core.database.redis.config import get_redis_connection
from app.core.database.redis.redis_cache import (
    DEFAULT_SCAN_COUNT,
    _decode_key,
    _raise_redis_operation_error,
)


class RedisHashCache:
    """Redis Hash 结构操作封装。"""

    def __init__(self, redis_client: Redis | None = None) -> None:
        """初始化 Redis Hash 操作封装。

        Args:
            redis_client: 可选 Redis 客户端，未传时使用全局连接。
        """
        self._redis = redis_client or get_redis_connection()

    def h_put(self, key: str, field: Any, value: Any) -> None:
        """写入单个 Hash 字段。

        Args:
            key: Redis 键。
            field: Hash 字段名。
            value: 字段值。

        Raises:
            ServiceException: Redis 写入失败时抛出。
        """
        try:
            self._redis.hset(key, field, value)
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="hset", exc=exc) from exc

    def h_put_all(self, key: str, field_map: dict[Any, Any]) -> None:
        """批量写入 Hash 字段。

        Args:
            key: Redis 键。
            field_map: 字段映射。

        Raises:
            ServiceException: Redis 写入失败时抛出。
        """
        try:
            self._redis.hset(key, mapping=field_map)
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="hset_many", exc=exc) from exc

    def h_get(self, key: str, field: Any) -> Any:
        """读取单个 Hash 字段。

        Args:
            key: Redis 键。
            field: Hash 字段名。

        Returns:
            Any: 字段值，不存在时返回 `None`。

        Raises:
            ServiceException: Redis 读取失败时抛出。
        """
        try:
            return self._redis.hget(key, field)
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="hget", exc=exc) from exc

    def h_get_all(self, key: str) -> dict[Any, Any]:
        """读取整个 Hash。

        Args:
            key: Redis 键。

        Returns:
            dict[Any, Any]: 全量字段映射。

        Raises:
            ServiceException: Redis 读取失败时抛出。
        """
        try:
            return dict(self._redis.hgetall(key))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="hgetall", exc=exc) from exc

    def h_remove(self, key: str, *fields: Any) -> int:
        """删除一个或多个 Hash 字段。

        Args:
            key: Redis 键。
            *fields: 要删除的字段列表。

        Returns:
            int: 实际删除的字段数。

        Raises:
            ServiceException: Redis 删除失败时抛出。
        """
        if not fields:
            return 0
        try:
            return int(self._redis.hdel(key, *fields))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="hdel", exc=exc) from exc

    def h_exists(self, key: str, field: Any) -> bool:
        """判断 Hash 字段是否存在。

        Args:
            key: Redis 键。
            field: Hash 字段名。

        Returns:
            bool: 存在返回 `True`，否则返回 `False`。

        Raises:
            ServiceException: Redis 查询失败时抛出。
        """
        try:
            return bool(self._redis.hexists(key, field))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="hexists", exc=exc) from exc

    def h_size(self, key: str) -> int:
        """读取 Hash 字段数。

        Args:
            key: Redis 键。

        Returns:
            int: 字段总数。

        Raises:
            ServiceException: Redis 查询失败时抛出。
        """
        try:
            return int(self._redis.hlen(key))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="hlen", exc=exc) from exc

    def h_increment(self, key: str, field: Any, delta: int) -> int:
        """对 Hash 数值字段做增量。

        Args:
            key: Redis 键。
            field: Hash 字段名。
            delta: 增量值。

        Returns:
            int: 增量后的字段值。

        Raises:
            ServiceException: Redis 自增失败时抛出。
        """
        try:
            return int(self._redis.hincrby(key, field, delta))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="hincrby", exc=exc) from exc

    def expire(self, key: str, timeout: int) -> bool:
        """设置键过期时间（秒）。

        Args:
            key: Redis 键。
            timeout: 过期秒数。

        Returns:
            bool: 设置成功返回 `True`，否则返回 `False`。

        Raises:
            ServiceException: Redis 设置过期失败时抛出。
        """
        try:
            return bool(self._redis.expire(key, timeout))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="expire", exc=exc) from exc

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

    def scan_keys(self, key_pattern: str, *, count: int = DEFAULT_SCAN_COUNT) -> list[str]:
        """按模式扫描 Hash 键列表。

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

    def scan_keys_with_values(
            self,
            key_pattern: str,
            *,
            count: int = DEFAULT_SCAN_COUNT,
    ) -> dict[str, dict[Any, Any]]:
        """按模式扫描 Hash 键并读取每个 Hash 全量字段。

        Args:
            key_pattern: 键匹配模式。
            count: 每次 SCAN 的建议批次大小。

        Returns:
            dict[str, dict[Any, Any]]: 键到 Hash 字段映射的映射。

        Raises:
            ServiceException: Redis 扫描或读取失败时抛出。
        """
        matched_keys = self.scan_keys(key_pattern, count=count)
        if not matched_keys:
            return {}
        result: dict[str, dict[Any, Any]] = {}
        try:
            for key in matched_keys:
                result[key] = dict(self._redis.hgetall(key))
            return result
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="scan_hash_values", exc=exc) from exc
