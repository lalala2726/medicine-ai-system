from __future__ import annotations

from fnmatch import fnmatch

import pytest
from redis.exceptions import RedisError

from app.core.database.redis.redis_cache import RedisCache
from app.core.exception.exceptions import ServiceException


class _FakeRedis:
    def __init__(self) -> None:
        self.store: dict[str, object] = {}
        self.expire_map: dict[str, int] = {}
        self.eval_calls: list[dict[str, object]] = []

    def set(self, key: str, value: object) -> None:
        self.store[key] = value

    def setex(self, key: str, timeout: int, value: object) -> None:
        self.store[key] = value
        self.expire_map[key] = timeout

    def get(self, key: str):
        return self.store.get(key)

    def incr(self, key: str) -> int:
        current = int(self.store.get(key, 0))
        current += 1
        self.store[key] = current
        return current

    def incrby(self, key: str, delta: int) -> int:
        current = int(self.store.get(key, 0))
        current += delta
        self.store[key] = current
        return current

    def expire(self, key: str, timeout: int) -> bool:
        if key not in self.store:
            return False
        self.expire_map[key] = timeout
        return True

    def delete(self, *keys: str) -> int:
        deleted = 0
        for key in keys:
            if key in self.store:
                deleted += 1
                self.store.pop(key, None)
                self.expire_map.pop(key, None)
        return deleted

    def exists(self, key: str) -> int:
        return 1 if key in self.store else 0

    def ttl(self, key: str) -> int:
        return self.expire_map.get(key, -1)

    def keys(self, key_pattern: str) -> list[bytes]:
        return [key.encode("utf-8") for key in self.store.keys() if fnmatch(key, key_pattern)]

    def scan(self, *, cursor: int, match: str, count: int):
        del cursor, count
        batch = [key.encode("utf-8") for key in self.store.keys() if fnmatch(key, match)]
        return 0, batch

    def mget(self, keys: list[str]) -> list[object]:
        return [self.store.get(key) for key in keys]

    def eval(self, script: str, numkeys: int, *values: object):
        self.eval_calls.append(
            {
                "script": script,
                "numkeys": numkeys,
                "values": values,
            }
        )
        return [1, 0, 10, 9, 60]


class _ErrorRedis(_FakeRedis):
    def set(self, key: str, value: object) -> None:
        del key, value
        raise RedisError("mock redis error")


def test_redis_cache_set_get_and_increment() -> None:
    """验证 value 基础读写与自增能力。"""
    client = _FakeRedis()
    cache = RedisCache(redis_client=client)

    cache.set_cache_object("demo:key", b"value")
    assert cache.get_cache_object("demo:key") == b"value"
    assert cache.increment("demo:counter") == 1
    assert cache.increment_by("demo:counter", 2) == 3


def test_redis_cache_json_scan_and_eval() -> None:
    """验证 JSON、扫描和 Lua 执行能力。"""
    client = _FakeRedis()
    cache = RedisCache(redis_client=client)

    cache.set_json("demo:json", {"a": 1}, timeout=30)
    cache.set_cache_object("demo:text", b"text")

    assert cache.get_json("demo:json") == {"a": 1}
    assert cache.scan_keys("demo:*") == ["demo:json", "demo:text"]
    assert cache.scan_keys_with_values("demo:*") == {
        "demo:json": '{"a": 1}',
        "demo:text": b"text",
    }
    assert cache.eval_script("return {1}", 0) == [1, 0, 10, 9, 60]


def test_redis_cache_raises_service_exception_when_redis_error() -> None:
    """验证 Redis 底层异常会包装为 ServiceException。"""
    cache = RedisCache(redis_client=_ErrorRedis())
    with pytest.raises(ServiceException, match="Redis set 失败"):
        cache.set_cache_object("k", "v")
