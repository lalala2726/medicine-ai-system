from __future__ import annotations

from fnmatch import fnmatch

from app.core.database.redis.redis_list_cache import RedisListCache


class _FakeRedis:
    def __init__(self) -> None:
        self.list_store: dict[str, list[object]] = {}
        self.expire_map: dict[str, int] = {}

    def lpush(self, key: str, element: object) -> int:
        bucket = self.list_store.setdefault(key, [])
        bucket.insert(0, element)
        return len(bucket)

    def rpush(self, key: str, element: object) -> int:
        bucket = self.list_store.setdefault(key, [])
        bucket.append(element)
        return len(bucket)

    def lpop(self, key: str):
        bucket = self.list_store.get(key, [])
        return bucket.pop(0) if bucket else None

    def rpop(self, key: str):
        bucket = self.list_store.get(key, [])
        return bucket.pop() if bucket else None

    def lrange(self, key: str, start: int, end: int) -> list[object]:
        bucket = self.list_store.get(key, [])
        real_end = len(bucket) - 1 if end == -1 else end
        return bucket[start:real_end + 1]

    def llen(self, key: str) -> int:
        return len(self.list_store.get(key, []))

    def lrem(self, key: str, count: int, element: object) -> int:
        bucket = self.list_store.get(key, [])
        removed = 0
        if count == 0:
            count = len(bucket)
        index = 0
        while index < len(bucket) and removed < abs(count):
            if bucket[index] == element:
                bucket.pop(index)
                removed += 1
                if count < 0:
                    break
                continue
            index += 1
        return removed

    def ltrim(self, key: str, start: int, end: int) -> None:
        bucket = self.list_store.get(key, [])
        real_end = len(bucket) - 1 if end == -1 else end
        self.list_store[key] = bucket[start:real_end + 1]

    def expire(self, key: str, timeout: int) -> bool:
        if key not in self.list_store:
            return False
        self.expire_map[key] = timeout
        return True

    def scan(self, *, cursor: int, match: str, count: int):
        del cursor, count
        batch = [key.encode("utf-8") for key in self.list_store.keys() if fnmatch(key, match)]
        return 0, batch


def test_redis_list_cache_basic_operations() -> None:
    """验证 List 基础操作。"""
    cache = RedisListCache(redis_client=_FakeRedis())

    assert cache.left_push("list:1", "b") == 1
    assert cache.left_push("list:1", "a") == 2
    assert cache.right_push("list:1", "c") == 3
    assert cache.range("list:1", 0, -1) == ["a", "b", "c"]
    assert cache.size("list:1") == 3
    assert cache.left_pop("list:1") == "a"
    assert cache.right_pop("list:1") == "c"
    assert cache.range("list:1", 0, -1) == ["b"]
    assert cache.expire("list:1", 60) is True


def test_redis_list_cache_scan_keys_with_values() -> None:
    """验证 List 扫描与批量读取。"""
    fake = _FakeRedis()
    cache = RedisListCache(redis_client=fake)
    cache.right_push("queue:a", 1)
    cache.right_push("queue:a", 2)
    cache.right_push("queue:b", 3)
    cache.right_push("other:c", 4)

    assert cache.scan_keys("queue:*") == ["queue:a", "queue:b"]
    assert cache.scan_keys_with_values("queue:*") == {
        "queue:a": [1, 2],
        "queue:b": [3],
    }
