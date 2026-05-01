import pytest

from app.core.database.redis import config as redis_config_module
from app.core.exception.exceptions import ServiceException


class _FakeRedis:
    from_url_args: dict[str, object] | None = None

    def __init__(self, **kwargs) -> None:
        self.kwargs = kwargs

    @classmethod
    def from_url(cls, url: str, *, decode_responses: bool = False):
        cls.from_url_args = {
            "url": url,
            "decode_responses": decode_responses,
        }
        return cls(url=url, decode_responses=decode_responses)


@pytest.fixture(autouse=True)
def _clear_cache_between_tests() -> None:
    """每个用例后清理 Redis 配置与连接缓存。"""
    redis_config_module.clear_redis_connection_cache()
    yield
    redis_config_module.clear_redis_connection_cache()


def test_get_redis_settings_prefers_url(monkeypatch: pytest.MonkeyPatch) -> None:
    """验证 REDIS_URL 配置会被优先读取。"""
    monkeypatch.setenv("REDIS_URL", "redis://127.0.0.1:6379/2")

    settings = redis_config_module.get_redis_settings()

    assert settings.url == "redis://127.0.0.1:6379/2"
    assert settings.decode_responses is False


def test_get_redis_settings_raises_on_invalid_port(monkeypatch: pytest.MonkeyPatch) -> None:
    """验证端口配置非法时会抛出业务异常。"""
    monkeypatch.delenv("REDIS_URL", raising=False)
    monkeypatch.setenv("REDIS_PORT", "invalid")

    with pytest.raises(ServiceException, match="REDIS_PORT"):
        redis_config_module.get_redis_settings()


def test_get_redis_connection_uses_from_url(monkeypatch: pytest.MonkeyPatch) -> None:
    """验证 URL 模式会调用 Redis.from_url。"""
    monkeypatch.setenv("REDIS_URL", "redis://127.0.0.1:6379/1")
    monkeypatch.setattr(redis_config_module, "Redis", _FakeRedis)

    connection = redis_config_module.get_redis_connection()

    assert isinstance(connection, _FakeRedis)
    assert _FakeRedis.from_url_args == {
        "url": "redis://127.0.0.1:6379/1",
        "decode_responses": False,
    }


def test_get_redis_connection_uses_host_port(monkeypatch: pytest.MonkeyPatch) -> None:
    """验证非 URL 模式下会按 host/port 构造连接。"""
    monkeypatch.delenv("REDIS_URL", raising=False)
    monkeypatch.setenv("REDIS_HOST", "redis-host")
    monkeypatch.setenv("REDIS_PORT", "6380")
    monkeypatch.setenv("REDIS_DB", "3")
    monkeypatch.setenv("REDIS_SSL", "true")
    monkeypatch.setattr(redis_config_module, "Redis", _FakeRedis)

    connection = redis_config_module.get_redis_connection()

    assert isinstance(connection, _FakeRedis)
    assert connection.kwargs["host"] == "redis-host"
    assert connection.kwargs["port"] == 6380
    assert connection.kwargs["db"] == 3
    assert connection.kwargs["ssl"] is True
    assert connection.kwargs["decode_responses"] is False
