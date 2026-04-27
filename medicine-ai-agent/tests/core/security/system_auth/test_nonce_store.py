import pytest

from app.core.exception.exceptions import ServiceException
from app.core.security.system_auth.config import SystemAuthSettings
from app.core.security.system_auth.nonce_store import build_nonce_key, reserve_nonce


class _FakeRedis:
    def __init__(self, *, set_result: object = True, raise_error: bool = False) -> None:
        self.set_result = set_result
        self.raise_error = raise_error
        self.calls: list[dict[str, object]] = []

    def set(self, key, value, nx, ex):
        self.calls.append(
            {
                "key": key,
                "value": value,
                "nx": nx,
                "ex": ex,
            }
        )
        if self.raise_error:
            raise RuntimeError("redis down")
        return self.set_result


def _settings() -> SystemAuthSettings:
    return SystemAuthSettings(
        enabled=True,
        max_skew_seconds=300,
        nonce_ttl_seconds=600,
        nonce_key_prefix="system_auth:nonce",
        default_sign_version="v1",
        local_secret="local-secret",
        clients={},
    )


def test_build_nonce_key_uses_prefix_app_id_and_nonce() -> None:
    """验证 nonce key 拼接规则。"""
    key = build_nonce_key(
        settings=_settings(),
        app_id="biz-server",
        nonce="abc-123",
    )
    assert key == "system_auth:nonce:biz-server:abc-123"


def test_reserve_nonce_returns_false_when_nonce_exists(monkeypatch) -> None:
    """验证重复 nonce 会返回 False。"""
    fake = _FakeRedis(set_result=False)
    monkeypatch.setattr(
        "app.core.security.system_auth.nonce_store.get_redis_connection",
        lambda: fake,
    )
    reserved = reserve_nonce(
        settings=_settings(),
        app_id="biz-server",
        nonce="nonce-1",
    )
    assert reserved is False
    assert fake.calls[0]["nx"] is True
    assert fake.calls[0]["ex"] == 600


def test_reserve_nonce_raises_503_when_redis_unavailable(monkeypatch) -> None:
    """验证 Redis 异常时按 503 失败关闭。"""
    fake = _FakeRedis(raise_error=True)
    monkeypatch.setattr(
        "app.core.security.system_auth.nonce_store.get_redis_connection",
        lambda: fake,
    )
    with pytest.raises(ServiceException) as exc_info:
        reserve_nonce(
            settings=_settings(),
            app_id="biz-server",
            nonce="nonce-2",
        )
    assert int(exc_info.value.code) == 503
