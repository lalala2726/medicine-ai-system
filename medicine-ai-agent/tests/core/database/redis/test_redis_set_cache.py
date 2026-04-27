from __future__ import annotations

from fnmatch import fnmatch

from app.core.database.redis.redis_set_cache import RedisSetCache


class _FakeRedis:
    def __init__(self) -> None:
        self.set_store: dict[str, set[object]] = {}
        self.expire_map: dict[str, int] = {}

    def sadd(self, key: str, *members: object) -> int:
        bucket = self.set_store.setdefault(key, set())
        before = len(bucket)
        bucket.update(members)
        return len(bucket) - before

    def sismember(self, key: str, member: object) -> bool:
        return member in self.set_store.get(key, set())

    def smembers(self, key: str) -> set[object]:
        return set(self.set_store.get(key, set()))

    def srem(self, key: str, *members: object) -> int:
        bucket = self.set_store.get(key, set())
        removed = 0
        for member in members:
            if member in bucket:
                removed += 1
                bucket.remove(member)
        return removed

    def scard(self, key: str) -> int:
        return len(self.set_store.get(key, set()))

    def spop(self, key: str):
        bucket = self.set_store.get(key, set())
        if not bucket:
            return None
        value = next(iter(bucket))
        bucket.remove(value)
        return value

    def expire(self, key: str, timeout: int) -> bool:
        if key not in self.set_store:
            return False
        self.expire_map[key] = timeout
        return True

    def scan(self, *, cursor: int, match: str, count: int):
        del cursor, count
        batch = [key.encode("utf-8") for key in self.set_store.keys() if fnmatch(key, match)]
        return 0, batch


def test_redis_set_cache_basic_operations() -> None:
    """验证 Set 基础操作。"""
    cache = RedisSetCache(redis_client=_FakeRedis())

    assert cache.add("set:1", "a") == 1
    assert cache.add_all("set:1", "b", "c") == 2
    assert cache.is_member("set:1", "b") is True
    assert cache.size("set:1") == 3
    assert cache.remove("set:1", "a") == 1
    assert cache.expire("set:1", 120) is True
    popped = cache.pop("set:1")
    assert popped in {"b", "c"}


def test_redis_set_cache_scan_keys_with_values() -> None:
    """验证 Set 扫描与批量读取。"""
    fake = _FakeRedis()
    cache = RedisSetCache(redis_client=fake)
    cache.add_all("perm:admin", "read", "write")
    cache.add("perm:user", "read")
    cache.add("other:x", "none")

    assert cache.scan_keys("perm:*") == ["perm:admin", "perm:user"]
    values = cache.scan_keys_with_values("perm:*")
    assert values["perm:admin"] == {"read", "write"}
    assert values["perm:user"] == {"read"}
