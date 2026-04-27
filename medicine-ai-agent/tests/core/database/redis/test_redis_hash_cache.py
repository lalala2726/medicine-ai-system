from __future__ import annotations

from fnmatch import fnmatch

from app.core.database.redis.redis_hash_cache import RedisHashCache


class _FakeRedis:
    def __init__(self) -> None:
        self.hash_store: dict[str, dict[object, object]] = {}
        self.expire_map: dict[str, int] = {}

    def hset(self, key: str, field=None, value=None, mapping=None):
        bucket = self.hash_store.setdefault(key, {})
        if mapping is not None:
            bucket.update(mapping)
            return len(mapping)
        bucket[field] = value
        return 1

    def hget(self, key: str, field: object):
        return self.hash_store.get(key, {}).get(field)

    def hgetall(self, key: str):
        return dict(self.hash_store.get(key, {}))

    def hdel(self, key: str, *fields: object) -> int:
        bucket = self.hash_store.get(key, {})
        deleted = 0
        for field in fields:
            if field in bucket:
                deleted += 1
                bucket.pop(field, None)
        return deleted

    def hexists(self, key: str, field: object) -> bool:
        return field in self.hash_store.get(key, {})

    def hlen(self, key: str) -> int:
        return len(self.hash_store.get(key, {}))

    def hincrby(self, key: str, field: object, delta: int) -> int:
        bucket = self.hash_store.setdefault(key, {})
        current = int(bucket.get(field, 0))
        current += delta
        bucket[field] = current
        return current

    def expire(self, key: str, timeout: int) -> bool:
        if key not in self.hash_store:
            return False
        self.expire_map[key] = timeout
        return True

    def ttl(self, key: str) -> int:
        return self.expire_map.get(key, -1)

    def scan(self, *, cursor: int, match: str, count: int):
        del cursor, count
        batch = [key.encode("utf-8") for key in self.hash_store.keys() if fnmatch(key, match)]
        return 0, batch


def test_redis_hash_cache_basic_operations() -> None:
    """验证 Hash 基础操作。"""
    cache = RedisHashCache(redis_client=_FakeRedis())

    cache.h_put("hash:1", "a", 1)
    cache.h_put_all("hash:1", {"b": 2})
    assert cache.h_get("hash:1", "a") == 1
    assert cache.h_get_all("hash:1") == {"a": 1, "b": 2}
    assert cache.h_exists("hash:1", "b") is True
    assert cache.h_size("hash:1") == 2
    assert cache.h_increment("hash:1", "c", 3) == 3
    assert cache.h_remove("hash:1", "a") == 1
    assert cache.expire("hash:1", 120) is True
    assert cache.get_expire("hash:1") == 120


def test_redis_hash_cache_scan_keys_with_values() -> None:
    """验证 Hash 扫描与批量读取。"""
    fake = _FakeRedis()
    cache = RedisHashCache(redis_client=fake)
    cache.h_put("hash:one", "k1", "v1")
    cache.h_put("hash:two", "k2", "v2")
    cache.h_put("other:three", "k3", "v3")

    assert cache.scan_keys("hash:*") == ["hash:one", "hash:two"]
    assert cache.scan_keys_with_values("hash:*") == {
        "hash:one": {"k1": "v1"},
        "hash:two": {"k2": "v2"},
    }
