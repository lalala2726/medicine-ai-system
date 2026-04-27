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


class RedisZSetCache:
    """Redis ZSet 结构操作封装。"""

    def __init__(self, redis_client: Redis | None = None) -> None:
        """初始化 Redis ZSet 操作封装。

        Args:
            redis_client: 可选 Redis 客户端，未传时使用全局连接。
        """
        self._redis = redis_client or get_redis_connection()

    def z_add(self, key: str, member: Any, score: float) -> int:
        """添加有序集合成员。

        Args:
            key: Redis 键。
            member: 成员值。
            score: 成员分值。

        Returns:
            int: 实际新增成员数量（0 或 1）。

        Raises:
            ServiceException: Redis 写入失败时抛出。
        """
        try:
            return int(self._redis.zadd(key, {member: score}))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="zadd", exc=exc) from exc

    def z_add_with_ttl(self, key: str, member: Any, score: float, timeout: int) -> int:
        """添加有序集合成员并设置过期时间（秒）。

        Args:
            key: Redis 键。
            member: 成员值。
            score: 成员分值。
            timeout: 过期秒数。

        Returns:
            int: 实际新增成员数量（0 或 1）。

        Raises:
            ServiceException: Redis 写入或设置过期失败时抛出。
        """
        added = self.z_add(key, member, score)
        self.expire(key, timeout)
        return added

    def z_remove(self, key: str, *members: Any) -> int:
        """删除有序集合成员。

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
            return int(self._redis.zrem(key, *members))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="zrem", exc=exc) from exc

    def z_range(self, key: str, start: int, end: int) -> list[Any]:
        """按排名范围读取成员。

        Args:
            key: Redis 键。
            start: 起始排名（包含）。
            end: 结束排名（包含），`-1` 表示末尾。

        Returns:
            list[Any]: 成员列表。

        Raises:
            ServiceException: Redis 读取失败时抛出。
        """
        try:
            return list(self._redis.zrange(key, start, end))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="zrange", exc=exc) from exc

    def z_range_by_score(self, key: str, min_score: float, max_score: float) -> list[Any]:
        """按分值范围读取成员。

        Args:
            key: Redis 键。
            min_score: 最小分值（包含）。
            max_score: 最大分值（包含）。

        Returns:
            list[Any]: 分值区间内成员列表。

        Raises:
            ServiceException: Redis 读取失败时抛出。
        """
        try:
            return list(self._redis.zrangebyscore(key, min_score, max_score))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="zrangebyscore", exc=exc) from exc

    def z_count(self, key: str, min_score: float, max_score: float) -> int:
        """统计分值区间内成员数。

        Args:
            key: Redis 键。
            min_score: 最小分值（包含）。
            max_score: 最大分值（包含）。

        Returns:
            int: 区间成员数量。

        Raises:
            ServiceException: Redis 统计失败时抛出。
        """
        try:
            return int(self._redis.zcount(key, min_score, max_score))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="zcount", exc=exc) from exc

    def z_card(self, key: str) -> int:
        """读取集合总成员数。

        Args:
            key: Redis 键。

        Returns:
            int: 成员总数。

        Raises:
            ServiceException: Redis 查询失败时抛出。
        """
        try:
            return int(self._redis.zcard(key))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="zcard", exc=exc) from exc

    def get_all_with_score(self, key: str) -> list[tuple[Any, float]]:
        """读取所有成员及分值。

        Args:
            key: Redis 键。

        Returns:
            list[tuple[Any, float]]: 由成员与分值组成的列表。

        Raises:
            ServiceException: Redis 读取失败时抛出。
        """
        try:
            return list(self._redis.zrange(key, 0, -1, withscores=True))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="zrange_withscores", exc=exc) from exc

    def z_score(self, key: str, member: Any) -> float | None:
        """读取成员分值。

        Args:
            key: Redis 键。
            member: 成员值。

        Returns:
            float | None: 成员分值，不存在返回 `None`。

        Raises:
            ServiceException: Redis 查询失败时抛出。
        """
        try:
            score = self._redis.zscore(key, member)
            return float(score) if score is not None else None
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="zscore", exc=exc) from exc

    def z_increment_score(self, key: str, member: Any, delta: float) -> float:
        """对成员分值做增量。

        Args:
            key: Redis 键。
            member: 成员值。
            delta: 分值增量。

        Returns:
            float: 增量后的分值。

        Raises:
            ServiceException: Redis 自增失败时抛出。
        """
        try:
            return float(self._redis.zincrby(key, delta, member))
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="zincrby", exc=exc) from exc

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
        """按模式扫描 ZSet 键列表。

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
    ) -> dict[str, list[tuple[Any, float]]]:
        """按模式扫描 ZSet 键并读取成员及分值。

        Args:
            key_pattern: 键匹配模式。
            count: 每次 SCAN 的建议批次大小。

        Returns:
            dict[str, list[tuple[Any, float]]]: 键与成员分值列表映射。

        Raises:
            ServiceException: Redis 扫描或读取失败时抛出。
        """
        matched_keys = self.scan_keys(key_pattern, count=count)
        if not matched_keys:
            return {}
        result: dict[str, list[tuple[Any, float]]] = {}
        try:
            for key in matched_keys:
                result[key] = list(self._redis.zrange(key, 0, -1, withscores=True))
            return result
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="scan_zset_values", exc=exc) from exc

    def z_scan_with_scores(self, key: str, *, count: int = DEFAULT_SCAN_COUNT) -> list[tuple[Any, float]]:
        """扫描单个 ZSet 成员并携带分值。

        Args:
            key: Redis 键。
            count: 每次 ZSCAN 的建议批次大小。

        Returns:
            list[tuple[Any, float]]: 成员与分值列表。

        Raises:
            ServiceException: Redis 扫描失败时抛出。
        """
        cursor = 0
        result: list[tuple[Any, float]] = []
        try:
            while True:
                cursor, batch = self._redis.zscan(
                    key,
                    cursor=cursor,
                    count=count,
                )
                result.extend((member, float(score)) for member, score in batch)
                if int(cursor) == 0:
                    break
            return result
        except RedisError as exc:
            raise _raise_redis_operation_error(operation="zscan", exc=exc) from exc
