import pytest

from app.core.database.neo4j import config as neo4j_config_module
from app.core.database.neo4j.client import clear_neo4j_connection_cache
from app.core.exception.exceptions import ServiceException


class _FakeDriver:
    def __init__(self) -> None:
        self.closed = False
        self.verify_kwargs: dict[str, object] | None = None

    def verify_connectivity(self, **kwargs) -> None:
        self.verify_kwargs = kwargs

    def close(self) -> None:
        self.closed = True


class _FakeGraphDatabase:
    calls: list[dict[str, object]] = []
    next_driver: _FakeDriver | None = None

    @classmethod
    def driver(cls, uri: str, *, auth=None, **config):
        driver = cls.next_driver or _FakeDriver()
        cls.calls.append(
            {
                "uri": uri,
                "auth": auth,
                "config": config,
                "driver": driver,
            }
        )
        return driver


@pytest.fixture(autouse=True)
def _clear_cache_between_tests() -> None:
    """每个用例前后清理 Neo4j 配置与连接缓存。"""
    clear_neo4j_connection_cache()
    _FakeGraphDatabase.calls.clear()
    _FakeGraphDatabase.next_driver = None
    yield
    clear_neo4j_connection_cache()
    _FakeGraphDatabase.calls.clear()
    _FakeGraphDatabase.next_driver = None


def test_get_neo4j_settings_uses_defaults(monkeypatch: pytest.MonkeyPatch) -> None:
    """验证默认 Neo4j 配置会被正确加载。"""
    monkeypatch.delenv("NEO4J_URI", raising=False)
    monkeypatch.delenv("NEO4J_USER", raising=False)
    monkeypatch.delenv("NEO4J_DATABASE", raising=False)
    monkeypatch.delenv("NEO4J_TIMEOUT_SECONDS", raising=False)
    monkeypatch.delenv("NEO4J_STARTUP_PING_ENABLED", raising=False)
    monkeypatch.setenv("NEO4J_PASSWORD", "secret")

    settings = neo4j_config_module.get_neo4j_settings()

    assert settings.uri == neo4j_config_module.DEFAULT_NEO4J_URI
    assert settings.user == neo4j_config_module.DEFAULT_NEO4J_USER
    assert settings.database == neo4j_config_module.DEFAULT_NEO4J_DATABASE
    assert settings.timeout_seconds == neo4j_config_module.DEFAULT_NEO4J_TIMEOUT_SECONDS
    assert settings.startup_ping_enabled is False


def test_get_neo4j_settings_reads_env_values(monkeypatch: pytest.MonkeyPatch) -> None:
    """验证显式环境变量会覆盖默认 Neo4j 配置。"""
    monkeypatch.setenv("NEO4J_URI", "bolt://127.0.0.1:17687")
    monkeypatch.setenv("NEO4J_USER", "graph_user")
    monkeypatch.setenv("NEO4J_PASSWORD", "graph_password")
    monkeypatch.setenv("NEO4J_DATABASE", "medicine_graph")
    monkeypatch.setenv("NEO4J_TIMEOUT_SECONDS", "8.5")
    monkeypatch.setenv("NEO4J_STARTUP_PING_ENABLED", "true")

    settings = neo4j_config_module.get_neo4j_settings()

    assert settings.uri == "bolt://127.0.0.1:17687"
    assert settings.user == "graph_user"
    assert settings.password == "graph_password"
    assert settings.database == "medicine_graph"
    assert settings.timeout_seconds == 8.5
    assert settings.startup_ping_enabled is True


def test_get_neo4j_settings_raises_on_invalid_timeout(monkeypatch: pytest.MonkeyPatch) -> None:
    """验证非法超时配置会抛出业务异常。"""
    monkeypatch.setenv("NEO4J_PASSWORD", "secret")
    monkeypatch.setenv("NEO4J_TIMEOUT_SECONDS", "invalid")

    with pytest.raises(ServiceException, match="NEO4J_TIMEOUT_SECONDS"):
        neo4j_config_module.get_neo4j_settings()


def test_get_neo4j_settings_raises_when_password_missing(monkeypatch: pytest.MonkeyPatch) -> None:
    """验证缺失密码时会拒绝构造 Neo4j 配置。"""
    monkeypatch.delenv("NEO4J_PASSWORD", raising=False)

    with pytest.raises(ServiceException, match="NEO4J_PASSWORD"):
        neo4j_config_module.get_neo4j_settings()


def test_get_neo4j_settings_raises_when_database_blank(monkeypatch: pytest.MonkeyPatch) -> None:
    """验证数据库名为空白字符串时会抛出业务异常。"""
    monkeypatch.setenv("NEO4J_PASSWORD", "secret")
    monkeypatch.setenv("NEO4J_DATABASE", "   ")

    with pytest.raises(ServiceException, match="NEO4J_DATABASE"):
        neo4j_config_module.get_neo4j_settings()


def test_get_neo4j_driver_builds_and_caches_driver(monkeypatch: pytest.MonkeyPatch) -> None:
    """验证 Neo4j Driver 会按配置构造并命中缓存。"""
    monkeypatch.delenv("NEO4J_URI", raising=False)
    monkeypatch.delenv("NEO4J_USER", raising=False)
    monkeypatch.setenv("NEO4J_PASSWORD", "secret")
    monkeypatch.setenv("NEO4J_TIMEOUT_SECONDS", "4.5")
    monkeypatch.setattr(neo4j_config_module, "GraphDatabase", _FakeGraphDatabase)

    driver_one = neo4j_config_module.get_neo4j_driver()
    driver_two = neo4j_config_module.get_neo4j_driver()

    assert driver_one is driver_two
    assert len(_FakeGraphDatabase.calls) == 1
    assert _FakeGraphDatabase.calls[0]["uri"] == neo4j_config_module.DEFAULT_NEO4J_URI
    assert _FakeGraphDatabase.calls[0]["auth"] == (neo4j_config_module.DEFAULT_NEO4J_USER, "secret")
    assert _FakeGraphDatabase.calls[0]["config"] == {"connection_timeout": 4.5}


def test_verify_neo4j_connection_calls_driver_verify_connectivity(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """验证 Neo4j 连通性校验会调用底层 Driver 的 verify_connectivity。"""
    fake_driver = _FakeDriver()
    _FakeGraphDatabase.next_driver = fake_driver
    monkeypatch.setenv("NEO4J_PASSWORD", "secret")
    monkeypatch.setenv("NEO4J_DATABASE", "medicine_graph")
    monkeypatch.setattr(neo4j_config_module, "GraphDatabase", _FakeGraphDatabase)

    neo4j_config_module.verify_neo4j_connection()

    assert fake_driver.verify_kwargs == {"database": "medicine_graph"}


def test_clear_neo4j_connection_cache_closes_cached_driver(monkeypatch: pytest.MonkeyPatch) -> None:
    """验证清理 Neo4j 连接缓存时会关闭已缓存的 Driver。"""
    fake_driver = _FakeDriver()
    _FakeGraphDatabase.next_driver = fake_driver
    monkeypatch.setenv("NEO4J_PASSWORD", "secret")
    monkeypatch.setattr(neo4j_config_module, "GraphDatabase", _FakeGraphDatabase)

    neo4j_config_module.get_neo4j_driver()
    clear_neo4j_connection_cache()

    assert fake_driver.closed is True
