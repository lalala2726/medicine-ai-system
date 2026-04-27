import importlib
import math

import pytest
from fastapi.responses import PlainTextResponse
from redis.exceptions import RedisError
from starlette.requests import Request

from app.core.codes import ResponseCode
from app.core.security.auth_context import reset_current_user, set_current_user
from app.schemas.auth import AuthUser

rate_limit_module = importlib.import_module("app.core.security.rate_limit")


class InMemoryRedisForRateLimit:
    def __init__(self) -> None:
        self._scores: dict[str, list[int]] = {}

    def eval(self, script: str, numkeys: int, *values):
        del script
        keys = list(values[:numkeys])
        args = list(values[numkeys:])
        now_ms = int(args[0])
        rule_count = int(args[2])

        windows_ms: list[int] = []
        limits: list[int] = []
        cursor = 4
        for _ in range(rule_count):
            windows_ms.append(int(args[cursor]))
            limits.append(int(args[cursor + 1]))
            cursor += 2

        blocked = False
        max_retry_ms = 0
        for index in range(rule_count):
            key = keys[index]
            window_ms = windows_ms[index]
            limit = limits[index]
            cutoff = now_ms - window_ms
            existing_scores = [score for score in self._scores.get(key, []) if score > cutoff]
            self._scores[key] = existing_scores
            if len(existing_scores) >= limit:
                blocked = True
                oldest = min(existing_scores) if existing_scores else now_ms
                retry_ms = max(oldest + window_ms - now_ms, 0)
                max_retry_ms = max(max_retry_ms, retry_ms)

        if not blocked:
            for key in keys:
                self._scores.setdefault(key, []).append(now_ms)

        shortest_key = keys[0]
        shortest_window_ms = windows_ms[0]
        shortest_limit = limits[0]
        shortest_cutoff = now_ms - shortest_window_ms
        shortest_scores = [
            score
            for score in self._scores.get(shortest_key, [])
            if score > shortest_cutoff
        ]
        self._scores[shortest_key] = shortest_scores
        shortest_remaining = max(shortest_limit - len(shortest_scores), 0)
        shortest_reset_ms = 0
        if shortest_scores:
            shortest_reset_ms = max(min(shortest_scores) + shortest_window_ms - now_ms, 0)

        return [
            0 if blocked else 1,
            math.ceil(max_retry_ms / 1000),
            shortest_limit,
            shortest_remaining,
            math.ceil(shortest_reset_ms / 1000),
        ]


def _build_request(*, client_host: str | None = "1.1.1.1", x_forwarded_for: str | None = None) -> Request:
    headers: list[tuple[bytes, bytes]] = []
    if x_forwarded_for:
        headers.append((b"x-forwarded-for", x_forwarded_for.encode("utf-8")))
    scope = {
        "type": "http",
        "method": "GET",
        "path": "/unit-test",
        "headers": headers,
    }
    if client_host is not None:
        scope["client"] = (client_host, 12345)
    return Request(scope)


def test_rate_limit_rule_supports_preset_and_custom():
    preset_rule = rate_limit_module.RateLimitRule.preset(
        rate_limit_module.RateLimitPreset.MINUTE_1,
        limit=10,
    )
    custom_rule = rate_limit_module.RateLimitRule.custom(
        seconds=37,
        limit=2,
        label="burst",
    )

    assert preset_rule.window_seconds == 60
    assert preset_rule.limit == 10
    assert custom_rule.window_seconds == 37
    assert custom_rule.limit == 2
    assert custom_rule.label == "burst"


def test_load_rate_limit_lua_script_reads_from_resources(monkeypatch):
    captured: dict[str, object] = {}
    call_count = {"value": 0}

    def _fake_load_resource_text(
            resource_subdir: str,
            name: str,
            *,
            allowed_suffixes=None,
            cache=None,
    ) -> str:
        call_count["value"] += 1
        captured["resource_subdir"] = resource_subdir
        captured["name"] = name
        captured["allowed_suffixes"] = allowed_suffixes
        captured["cache"] = cache
        return "return {1,0,1,1,1}"

    monkeypatch.setattr(rate_limit_module, "load_resource_text", _fake_load_resource_text)
    rate_limit_module._RATE_LIMIT_LUA_SCRIPT_CACHE = None

    first = rate_limit_module._load_rate_limit_lua_script()
    second = rate_limit_module._load_rate_limit_lua_script()

    assert first == "return {1,0,1,1,1}"
    assert second == "return {1,0,1,1,1}"
    assert call_count["value"] == 1
    assert captured["resource_subdir"] == "rate_limit"
    assert captured["name"] == rate_limit_module.RATE_LIMIT_LUA_FILE
    assert captured["allowed_suffixes"] == (".lua",)

    rate_limit_module._RATE_LIMIT_LUA_SCRIPT_CACHE = None


def test_evaluate_rate_limit_allows_then_rejects_on_multi_window(monkeypatch):
    fake_redis = InMemoryRedisForRateLimit()
    clock = {"ms": 0}
    monkeypatch.setattr(rate_limit_module, "get_redis_connection", lambda: fake_redis)
    monkeypatch.setattr(
        rate_limit_module.time,
        "time",
        lambda: clock["ms"] / 1000,
    )

    rules = (
        rate_limit_module.RateLimitRule.custom(seconds=60, limit=2, label="1m"),
        rate_limit_module.RateLimitRule.custom(seconds=300, limit=3, label="5m"),
    )
    normalized_rules = rate_limit_module._normalize_rules(rules)

    first = rate_limit_module._evaluate_rate_limit(
        scope="chat",
        subject_key="user_id:1",
        rules=normalized_rules,
    )
    second = rate_limit_module._evaluate_rate_limit(
        scope="chat",
        subject_key="user_id:1",
        rules=normalized_rules,
    )
    third = rate_limit_module._evaluate_rate_limit(
        scope="chat",
        subject_key="user_id:1",
        rules=normalized_rules,
    )

    assert first.allowed is True
    assert first.limit == 2
    assert first.remaining == 1
    assert second.allowed is True
    assert second.remaining == 0
    assert third.allowed is False
    assert third.retry_after_seconds == 60
    assert third.limit == 2
    assert third.remaining == 0


def test_rate_limit_decorator_applies_shortest_window_headers(monkeypatch):
    monkeypatch.setattr(
        rate_limit_module,
        "_evaluate_rate_limit",
        lambda *, scope, subject_key, rules: rate_limit_module.RateLimitCheckResult(
            allowed=True,
            retry_after_seconds=0,
            limit=10,
            remaining=4,
            reset_seconds=25,
        ),
    )

    @rate_limit_module.rate_limit(
        rules=(rate_limit_module.RateLimitRule.custom(seconds=60, limit=10),),
        subjects=("ip",),
        scope="unit_header",
    )
    def _handler(request: Request):
        return PlainTextResponse("ok")

    response = _handler(_build_request())
    assert response.headers["X-RateLimit-Limit"] == "10"
    assert response.headers["X-RateLimit-Remaining"] == "4"
    assert response.headers["X-RateLimit-Reset"] == "25"
    assert response.headers["Retry-After"] == "0"


def test_rate_limit_decorator_handles_fail_open_and_fail_close(monkeypatch):
    monkeypatch.setattr(
        rate_limit_module,
        "_evaluate_rate_limit",
        lambda *, scope, subject_key, rules: (_ for _ in ()).throw(RedisError("down")),
    )
    called = {"value": False}

    @rate_limit_module.rate_limit(
        rules=(rate_limit_module.RateLimitRule.custom(seconds=60, limit=1),),
        subjects=("ip",),
        scope="unit_fail_open",
        fail_open=True,
    )
    def _fail_open_handler(request: Request):
        called["value"] = True
        return PlainTextResponse("ok")

    response = _fail_open_handler(_build_request())
    assert response.status_code == 200
    assert called["value"] is True

    @rate_limit_module.rate_limit(
        rules=(rate_limit_module.RateLimitRule.custom(seconds=60, limit=1),),
        subjects=("ip",),
        scope="unit_fail_close",
        fail_open=False,
    )
    def _fail_close_handler(request: Request):
        return PlainTextResponse("ok")

    with pytest.raises(rate_limit_module.RateLimitException) as exc_info:
        _fail_close_handler(_build_request())
    assert exc_info.value.code == ResponseCode.SERVICE_UNAVAILABLE.code
    assert exc_info.value.headers["Retry-After"] == "1"


def test_rate_limit_subject_supports_user_id_ip_and_custom(monkeypatch):
    captured: dict[str, str] = {}
    token = set_current_user(AuthUser(id=42, username="tester"))
    monkeypatch.setenv("RATE_LIMIT_TRUST_X_FORWARDED_FOR", "true")

    def _capture_evaluate_rate_limit(*, scope: str, subject_key: str, rules):
        del scope, rules
        captured["subject_key"] = subject_key
        return rate_limit_module.RateLimitCheckResult(
            allowed=True,
            retry_after_seconds=0,
            limit=1,
            remaining=0,
            reset_seconds=60,
        )

    monkeypatch.setattr(rate_limit_module, "_evaluate_rate_limit", _capture_evaluate_rate_limit)

    @rate_limit_module.rate_limit(
        rules=(rate_limit_module.RateLimitRule.custom(seconds=60, limit=1),),
        subjects=("user_id", "ip", lambda request: "tenant-a"),
        scope="unit_subjects",
    )
    def _handler(request: Request):
        return PlainTextResponse("ok")

    try:
        _handler(_build_request(client_host="2.2.2.2", x_forwarded_for="9.9.9.9"))
    finally:
        reset_current_user(token)

    assert captured["subject_key"] == "user_id:42|ip:2.2.2.2|custom2:tenant-a"
