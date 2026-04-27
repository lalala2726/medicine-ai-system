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


class RedisSetCache:
    """Redis Set 结构操作封装。"""

    def __init__(self, redis_client: Redis | None = None) -> None:
        """初始化 Redis Set 操作封装。

        Args:
            redis_client: 可选 Redis 客户端，未传时使用全局连接。
        """
        self._redis = redis_client or get_redis_connection()

    def add(self, key: str, member: Any) -> int:
        """添加单个成员。

        Args:
            key: Redis 键。
            member: 集合成员。

        Returns:
            int: 新增成员数量（0 或 1）。

        Raises:
            ServiceException: Redis 写入失败时抛出。
        """
        try:
            return int(self._redis.sadd(key, member))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="sadd", exc=exc) from exc

    def add_all(self, key: str, *members: Any) -> int:
        """批量添加成员。

        Args:
            key: Redis 键。
            *members: 待添加成员列表。

        Returns:
            int: 实际新增成员数量。

        Raises:
            ServiceException: Redis 写入失败时抛出。
        """
        if not members:
            return 0
        try:
            return int(self._redis.sadd(key, *members))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="sadd_many", exc=exc) from exc

    def is_member(self, key: str, member: Any) -> bool:
        """判断成员是否存在。

        Args:
            key: Redis 键。
            member: 待检查成员。

        Returns:
            bool: 成员存在返回 `True`，否则返回 `False`。

        Raises:
            ServiceException: Redis 查询失败时抛出。
        """
        try:
            return bool(self._redis.sismember(key, member))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="sismember", exc=exc) from exc

    def members(self, key: str) -> set[Any]:
        """读取所有成员。

        Args:
            key: Redis 键。

        Returns:
            set[Any]: 成员集合。

        Raises:
            ServiceException: Redis 读取失败时抛出。
        """
        try:
            return set(self._redis.smembers(key))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="smembers", exc=exc) from exc

    def remove(self, key: str, *members: Any) -> int:
        """删除成员。

        Args:
            key: Redis 键。
            *members: 待删除成员列表。

        Returns:
            int: 实际删除成员数量。

        Raises:
            ServiceException: Redis 删除失败时抛出。
        """
        if not members:
            return 0
        try:
            return int(self._redis.srem(key, *members))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="srem", exc=exc) from exc

    def size(self, key: str) -> int:
        """读取集合大小。

        Args:
            key: Redis 键。

        Returns:
            int: 集合成员总数。

        Raises:
            ServiceException: Redis 查询失败时抛出。
        """
        try:
            return int(self._redis.scard(key))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="scard", exc=exc) from exc

    def pop(self, key: str) -> Any:
        """弹出并返回一个成员。

        Args:
            key: Redis 键。

        Returns:
            Any: 弹出的成员，集合为空时返回 `None`。

        Raises:
            ServiceException: Redis 读取失败时抛出。
        """
        try:
            return self._redis.spop(key)
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="spop", exc=exc) from exc

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

    def scan_keys(self, key_pattern: str, *, count: int = DEFAULT_SCAN_COUNT) -> list[str]:
        """按模式扫描 Set 键列表。

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

    def scan_keys_with_values(self, key_pattern: str, *, count: int = DEFAULT_SCAN_COUNT) -> dict[str, set[Any]]:
        """按模式扫描 Set 键并读取成员。

        Args:
            key_pattern: 键匹配模式。
            count: 每次 SCAN 的建议批次大小。

        Returns:
            dict[str, set[Any]]: 键与成员集合映射。

        Raises:
            ServiceException: Redis 扫描或读取失败时抛出。
        """
        matched_keys = self.scan_keys(key_pattern, count=count)
        if not matched_keys:
            return {}
        result: dict[str, set[Any]] = {}
        try:
            for key in matched_keys:
                result[key] = set(self._redis.smembers(key))
            return result
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="scan_set_values", exc=exc) from exc
