from __future__ import annotations

from fnmatch import fnmatch

from app.core.database.redis.redis_zset_cache import RedisZSetCache


class _FakeRedis:
    def __init__(self) -> None:
        self.zset_store: dict[str, dict[object, float]] = {}
        self.expire_map: dict[str, int] = {}

    def zadd(self, key: str, mapping: dict[object, float]) -> int:
        bucket = self.zset_store.setdefault(key, {})
        added = 0
        for member, score in mapping.items():
            if member not in bucket:
                added += 1
            bucket[member] = float(score)
        return added

    def zrem(self, key: str, *members: object) -> int:
        bucket = self.zset_store.get(key, {})
        removed = 0
        for member in members:
            if member in bucket:
                removed += 1
                bucket.pop(member, None)
        return removed

    def zrange(self, key: str, start: int, end: int, *, withscores: bool = False):
        bucket = self.zset_store.get(key, {})
        ordered = sorted(bucket.items(), key=lambda item: (item[1], str(item[0])))
        real_end = len(ordered) - 1 if end == -1 else end
        sliced = ordered[start:real_end + 1]
        if withscores:
            return [(member, score) for member, score in sliced]
        return [member for member, _ in sliced]

    def zrangebyscore(self, key: str, min_score: float, max_score: float):
        bucket = self.zset_store.get(key, {})
        ordered = sorted(bucket.items(), key=lambda item: (item[1], str(item[0])))
        return [
            member
            for member, score in ordered
            if float(min_score) <= score <= float(max_score)
        ]

    def zcount(self, key: str, min_score: float, max_score: float) -> int:
        return len(self.zrangebyscore(key, min_score, max_score))

    def zcard(self, key: str) -> int:
        return len(self.zset_store.get(key, {}))

    def zscore(self, key: str, member: object):
        return self.zset_store.get(key, {}).get(member)

    def zincrby(self, key: str, delta: float, member: object) -> float:
        bucket = self.zset_store.setdefault(key, {})
        new_score = float(bucket.get(member, 0.0)) + float(delta)
        bucket[member] = new_score
        return new_score

    def expire(self, key: str, timeout: int) -> bool:
        if key not in self.zset_store:
            return False
        self.expire_map[key] = timeout
        return True

    def scan(self, *, cursor: int, match: str, count: int):
        del cursor, count
        batch = [key.encode("utf-8") for key in self.zset_store.keys() if fnmatch(key, match)]
        return 0, batch

    def zscan(self, key: str, *, cursor: int, count: int):
        del cursor, count
        bucket = self.zset_store.get(key, {})
        batch = sorted(bucket.items(), key=lambda item: (item[1], str(item[0])))
        return 0, batch


def test_redis_zset_cache_basic_operations() -> None:
    """验证 ZSet 基础操作。"""
    cache = RedisZSetCache(redis_client=_FakeRedis())

    assert cache.z_add("rank:1", "u1", 10.0) == 1
    assert cache.z_add_with_ttl("rank:1", "u2", 20.0, timeout=60) == 1
    assert cache.z_card("rank:1") == 2
    assert cache.z_range("rank:1", 0, -1) == ["u1", "u2"]
    assert cache.z_score("rank:1", "u1") == 10.0
    assert cache.z_increment_score("rank:1", "u1", 5.0) == 15.0
    assert cache.z_count("rank:1", 10.0, 30.0) == 2
    assert cache.get_all_with_score("rank:1") == [("u1", 15.0), ("u2", 20.0)]
    assert cache.z_remove("rank:1", "u2") == 1


def test_redis_zset_cache_scan_and_zscan() -> None:
    """验证 ZSet 键扫描与成员扫描。"""
    fake = _FakeRedis()
    cache = RedisZSetCache(redis_client=fake)
    cache.z_add("rank:daily", "u1", 1.0)
    cache.z_add("rank:daily", "u2", 2.0)
    cache.z_add("rank:weekly", "u3", 3.0)
    cache.z_add("other:x", "u4", 4.0)

    assert cache.scan_keys("rank:*") == ["rank:daily", "rank:weekly"]
    assert cache.scan_keys_with_values("rank:*") == {
        "rank:daily": [("u1", 1.0), ("u2", 2.0)],
        "rank:weekly": [("u3", 3.0)],
    }
    assert cache.z_scan_with_scores("rank:daily") == [("u1", 1.0), ("u2", 2.0)]
