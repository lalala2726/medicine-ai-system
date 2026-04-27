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


class RedisListCache:
    """Redis List 结构操作封装。"""

    def __init__(self, redis_client: Redis | None = None) -> None:
        """初始化 Redis List 操作封装。

        Args:
            redis_client: 可选 Redis 客户端，未传时使用全局连接。
        """
        self._redis = redis_client or get_redis_connection()

    def left_push(self, key: str, element: Any) -> int:
        """从左侧入队。

        Args:
            key: Redis 键。
            element: 待写入元素。

        Returns:
            int: 写入后列表长度。

        Raises:
            ServiceException: Redis 写入失败时抛出。
        """
        try:
            return int(self._redis.lpush(key, element))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="lpush", exc=exc) from exc

    def right_push(self, key: str, element: Any) -> int:
        """从右侧入队。

        Args:
            key: Redis 键。
            element: 待写入元素。

        Returns:
            int: 写入后列表长度。

        Raises:
            ServiceException: Redis 写入失败时抛出。
        """
        try:
            return int(self._redis.rpush(key, element))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="rpush", exc=exc) from exc

    def left_pop(self, key: str) -> Any:
        """从左侧出队。

        Args:
            key: Redis 键。

        Returns:
            Any: 出队元素，列表为空时返回 `None`。

        Raises:
            ServiceException: Redis 读取失败时抛出。
        """
        try:
            return self._redis.lpop(key)
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="lpop", exc=exc) from exc

    def right_pop(self, key: str) -> Any:
        """从右侧出队。

        Args:
            key: Redis 键。

        Returns:
            Any: 出队元素，列表为空时返回 `None`。

        Raises:
            ServiceException: Redis 读取失败时抛出。
        """
        try:
            return self._redis.rpop(key)
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="rpop", exc=exc) from exc

    def range(self, key: str, start: int, end: int) -> list[Any]:
        """读取指定区间列表元素。

        Args:
            key: Redis 键。
            start: 起始下标（包含）。
            end: 结束下标（包含），传 `-1` 表示末尾。

        Returns:
            list[Any]: 区间元素列表。

        Raises:
            ServiceException: Redis 读取失败时抛出。
        """
        try:
            return list(self._redis.lrange(key, start, end))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="lrange", exc=exc) from exc

    def size(self, key: str) -> int:
        """读取列表长度。

        Args:
            key: Redis 键。

        Returns:
            int: 列表长度。

        Raises:
            ServiceException: Redis 查询失败时抛出。
        """
        try:
            return int(self._redis.llen(key))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="llen", exc=exc) from exc

    def remove(self, key: str, count: int, element: Any) -> int:
        """删除列表中匹配元素。

        Args:
            key: Redis 键。
            count: 删除数量语义同 Redis `LREM`。
            element: 待删除元素。

        Returns:
            int: 实际删除数量。

        Raises:
            ServiceException: Redis 删除失败时抛出。
        """
        try:
            return int(self._redis.lrem(key, count, element))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="lrem", exc=exc) from exc

    def trim(self, key: str, start: int, end: int) -> None:
        """修剪列表区间。

        Args:
            key: Redis 键。
            start: 起始下标（包含）。
            end: 结束下标（包含），传 `-1` 表示末尾。

        Raises:
            ServiceException: Redis 修剪失败时抛出。
        """
        try:
            self._redis.ltrim(key, start, end)
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="ltrim", exc=exc) from exc

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
        """按模式扫描 List 键列表。

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

    def scan_keys_with_values(self, key_pattern: str, *, count: int = DEFAULT_SCAN_COUNT) -> dict[str, list[Any]]:
        """按模式扫描 List 键并读取完整列表。

        Args:
            key_pattern: 键匹配模式。
            count: 每次 SCAN 的建议批次大小。

        Returns:
            dict[str, list[Any]]: 键与完整列表数据映射。

        Raises:
            ServiceException: Redis 扫描或读取失败时抛出。
        """
        matched_keys = self.scan_keys(key_pattern, count=count)
        if not matched_keys:
            return {}
        result: dict[str, list[Any]] = {}
        try:
            for key in matched_keys:
                result[key] = list(self._redis.lrange(key, 0, -1))
            return result
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="scan_list_values", exc=exc) from exc
