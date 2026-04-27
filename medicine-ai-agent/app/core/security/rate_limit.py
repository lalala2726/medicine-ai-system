from __future__ import annotations

import inspect
import os
import time
from dataclasses import dataclass
from enum import Enum
from functools import wraps
from typing import Callable, Iterable, Literal
from uuid import uuid4

from fastapi import Request, WebSocket
from fastapi.responses import Response
from loguru import logger
from redis.exceptions import RedisError

from app.core.codes import ResponseCode
from app.core.database.redis.config import get_redis_connection
from app.core.exception.exceptions import ServiceException
from app.core.security.auth_context import get_user_id
from app.utils.resource_text_utils import load_resource_text

SubjectKind = Literal["user_id", "ip"]
Connection = Request | WebSocket
SubjectResolver = Callable[[Connection], str] | Callable[[], str]
SubjectSpec = SubjectKind | SubjectResolver | str

RATE_LIMIT_BACKEND_UNAVAILABLE_MESSAGE = "限流服务不可用，请稍后再试"
DEFAULT_KEY_PREFIX = "rate_limit"
DEFAULT_EXPIRE_PADDING_SECONDS = 60
RATE_LIMIT_LUA_FILE = "sliding_window.lua"
_RATE_LIMIT_LUA_SCRIPT_CACHE: str | None = None


class RateLimitPreset(Enum):
    MINUTE_1 = 60
    MINUTE_5 = 300
    MINUTE_10 = 600
    HOUR_1 = 3600
    HOUR_5 = 18000
    HOUR_24 = 86400

    @property
    def seconds(self) -> int:
        return int(self.value)


@dataclass(frozen=True)
class RateLimitRule:
    window_seconds: int
    limit: int
    label: str | None = None

    def __post_init__(self) -> None:
        if self.window_seconds <= 0:
            raise ValueError("window_seconds must be positive")
        if self.limit <= 0:
            raise ValueError("limit must be positive")

    @classmethod
    def preset(cls, preset: RateLimitPreset, *, limit: int) -> "RateLimitRule":
        return cls(window_seconds=preset.seconds, limit=limit, label=preset.name.lower())

    @classmethod
    def custom(
            cls,
            *,
            seconds: int,
            limit: int,
            label: str | None = None,
    ) -> "RateLimitRule":
        return cls(window_seconds=seconds, limit=limit, label=label)


@dataclass(frozen=True)
class RateLimitCheckResult:
    allowed: bool
    retry_after_seconds: int
    limit: int
    remaining: int
    reset_seconds: int


class RateLimitException(ServiceException):
    def __init__(
            self,
            *,
            message: str,
            code: ResponseCode | int,
            headers: dict[str, str] | None = None,
            data: dict | None = None,
    ) -> None:
        super().__init__(message=message, code=code, data=data)
        self.headers = headers or {}


def _parse_bool(value: str | None, *, default: bool = False) -> bool:
    if value is None:
        return default
    normalized = value.strip().lower()
    if normalized == "":
        return default
    return normalized in {"1", "true", "yes", "on"}


def _to_int(value: object) -> int:
    if isinstance(value, bytes):
        return int(value.decode("utf-8"))
    return int(value)


def _resolve_key_prefix() -> str:
    key_prefix = (os.getenv("RATE_LIMIT_KEY_PREFIX") or "").strip()
    return key_prefix or DEFAULT_KEY_PREFIX


def _load_rate_limit_lua_script() -> str:
    global _RATE_LIMIT_LUA_SCRIPT_CACHE

    cached_script = _RATE_LIMIT_LUA_SCRIPT_CACHE
    if cached_script is not None:
        return cached_script

    try:
        script = load_resource_text(
            "rate_limit",
            RATE_LIMIT_LUA_FILE,
            allowed_suffixes=(".lua",),
        )
    except (FileNotFoundError, ValueError) as exc:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message="rate limit lua script is missing or invalid",
        ) from exc

    _RATE_LIMIT_LUA_SCRIPT_CACHE = script
    return script


def _extract_request(args: tuple, kwargs: dict) -> Connection | None:
    request = kwargs.get("request")
    if isinstance(request, (Request, WebSocket)):
        return request
    for value in kwargs.values():
        if isinstance(value, (Request, WebSocket)):
            return value
    for arg in args:
        if isinstance(arg, (Request, WebSocket)):
            return arg
    return None


def _resolve_ip(request: Connection | None) -> str:
    if request is not None and request.client is not None and request.client.host:
        return request.client.host

    if request is not None and _parse_bool(
            os.getenv("RATE_LIMIT_TRUST_X_FORWARDED_FOR"),
            default=False,
    ):
        x_forwarded_for = request.headers.get("x-forwarded-for", "")
        if x_forwarded_for:
            return x_forwarded_for.split(",")[0].strip()

    return "unknown"


def _call_subject_resolver(resolver: SubjectResolver, request: Connection | None) -> str:
    signature = inspect.signature(resolver)
    if len(signature.parameters) == 0:
        return str(resolver())
    if request is None:
        raise ValueError("custom subject resolver requires Request argument")
    return str(resolver(request))


def _normalize_subject_value(value: str) -> str:
    normalized = value.strip().replace("|", "_")
    if not normalized:
        raise ValueError("subject value cannot be empty")
    return normalized


def _resolve_subject_key(
        *,
        subjects: tuple[SubjectSpec, ...],
        request: Connection | None,
) -> str:
    parts: list[str] = []
    for index, subject in enumerate(subjects):
        if isinstance(subject, str):
            if subject == "user_id":
                parts.append(f"user_id:{get_user_id()}")
                continue
            if subject == "ip":
                parts.append(f"ip:{_normalize_subject_value(_resolve_ip(request))}")
                continue
            raise ValueError("subject string must be 'user_id' or 'ip'")
        if not callable(subject):
            raise ValueError("subject must be a supported string or callable")

        resolved_value = _normalize_subject_value(_call_subject_resolver(subject, request))
        parts.append(f"custom{index}:{resolved_value}")
    return "|".join(parts)


def _build_rate_limit_headers(result: RateLimitCheckResult) -> dict[str, str]:
    return {
        "X-RateLimit-Limit": str(max(result.limit, 0)),
        "X-RateLimit-Remaining": str(max(result.remaining, 0)),
        "X-RateLimit-Reset": str(max(result.reset_seconds, 0)),
        "Retry-After": str(max(result.retry_after_seconds, 0)),
    }


def _resolve_request_path(request: Connection | None) -> str:
    if request is None:
        return "当前接口"
    return request.url.path or "当前接口"


def _build_rate_limit_exceeded_message(
        *,
        request: Connection | None,
        retry_after_seconds: int,
) -> str:
    path = _resolve_request_path(request)
    wait_seconds = max(int(retry_after_seconds), 1)
    return f"访问 {path} 过于频繁，请在 {wait_seconds} 秒后再试"


def _normalize_rules(rules: Iterable[RateLimitRule]) -> tuple[RateLimitRule, ...]:
    merged: dict[int, RateLimitRule] = {}
    for rule in rules:
        existing = merged.get(rule.window_seconds)
        if existing is None or rule.limit < existing.limit:
            merged[rule.window_seconds] = rule
    if not merged:
        raise ValueError("rate_limit requires at least one rule")
    return tuple(sorted(merged.values(), key=lambda item: item.window_seconds))


def _evaluate_rate_limit(
        *,
        scope: str,
        subject_key: str,
        rules: tuple[RateLimitRule, ...],
) -> RateLimitCheckResult:
    now_ms = int(time.time() * 1000)
    request_member = f"{uuid4().hex}:{now_ms}"
    key_prefix = _resolve_key_prefix()
    keys = [
        f"{key_prefix}:{scope}:{subject_key}:{rule.window_seconds}"
        for rule in rules
    ]

    args: list[str] = [
        str(now_ms),
        request_member,
        str(len(rules)),
        str(DEFAULT_EXPIRE_PADDING_SECONDS),
    ]
    for rule in rules:
        args.extend([str(rule.window_seconds * 1000), str(rule.limit)])

    redis_client = get_redis_connection()
    raw_result = redis_client.eval(
        _load_rate_limit_lua_script(),
        len(keys),
        *keys,
        *args,
    )
    if not isinstance(raw_result, list) or len(raw_result) < 5:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message="rate limit backend returned invalid result",
        )

    allowed = _to_int(raw_result[0]) == 1
    retry_after_seconds = max(_to_int(raw_result[1]), 0)
    shortest_limit = max(_to_int(raw_result[2]), 0)
    shortest_remaining = max(_to_int(raw_result[3]), 0)
    shortest_reset = max(_to_int(raw_result[4]), 0)
    return RateLimitCheckResult(
        allowed=allowed,
        retry_after_seconds=retry_after_seconds,
        limit=shortest_limit,
        remaining=shortest_remaining,
        reset_seconds=shortest_reset,
    )


def _apply_headers_to_response(
        *,
        response_obj: object,
        kwargs: dict,
        headers: dict[str, str],
) -> None:
    if not headers:
        return
    if isinstance(response_obj, Response):
        for header, value in headers.items():
            response_obj.headers[header] = value
        return
    maybe_response = kwargs.get("response")
    if isinstance(maybe_response, Response):
        for header, value in headers.items():
            maybe_response.headers[header] = value


def _check_rate_limit(
        *,
        scope: str,
        subjects: tuple[SubjectSpec, ...],
        rules: tuple[RateLimitRule, ...],
        fail_open: bool,
        args: tuple,
        kwargs: dict,
) -> dict[str, str]:
    request = _extract_request(args, kwargs)
    try:
        subject_key = _resolve_subject_key(subjects=subjects, request=request)
    except ServiceException:
        raise
    except Exception as exc:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message="invalid rate limit subject config",
        ) from exc

    try:
        result = _evaluate_rate_limit(
            scope=scope,
            subject_key=subject_key,
            rules=rules,
        )
    except RedisError as exc:
        if fail_open:
            logger.warning(
                "rate_limit backend unavailable, request allowed scope={} subject={} error={}",
                scope,
                subject_key,
                str(exc),
            )
            return {}
        raise RateLimitException(
            message=RATE_LIMIT_BACKEND_UNAVAILABLE_MESSAGE,
            code=ResponseCode.SERVICE_UNAVAILABLE,
            headers={"Retry-After": "1"},
        ) from exc

    headers = _build_rate_limit_headers(result)
    if result.allowed:
        return headers

    raise RateLimitException(
        message=_build_rate_limit_exceeded_message(
            request=request,
            retry_after_seconds=result.retry_after_seconds,
        ),
        code=ResponseCode.TOO_MANY_REQUESTS,
        headers=headers,
    )


def check_rate_limit(
        *,
        scope: str,
        rules: Iterable[RateLimitRule],
        subjects: tuple[SubjectSpec, ...] = ("user_id",),
        fail_open: bool = True,
        request: Connection | None = None,
) -> dict[str, str]:
    """执行一次限流检查并返回限流响应头。"""

    resolved_rules = _normalize_rules(tuple(rules))
    if not subjects:
        raise ValueError("rate_limit requires at least one subject")

    return _check_rate_limit(
        scope=scope,
        subjects=tuple(subjects),
        rules=resolved_rules,
        fail_open=fail_open,
        args=(request,) if request is not None else (),
        kwargs={"request": request} if request is not None else {},
    )


def rate_limit(
        *,
        rules: Iterable[RateLimitRule],
        subjects: tuple[SubjectSpec, ...] = ("user_id",),
        scope: str | None = None,
        fail_open: bool = True,
):
    resolved_rules = _normalize_rules(tuple(rules))
    if not subjects:
        raise ValueError("rate_limit requires at least one subject")
    resolved_subjects = tuple(subjects)

    def _decorate(func):
        resolved_scope = (scope or "").strip()
        if not resolved_scope:
            resolved_scope = f"{func.__module__}.{func.__qualname__}"

        if inspect.iscoroutinefunction(func):
            @wraps(func)
            async def _async_wrapper(*args, **kwargs):
                headers = _check_rate_limit(
                    scope=resolved_scope,
                    subjects=resolved_subjects,
                    rules=resolved_rules,
                    fail_open=fail_open,
                    args=args,
                    kwargs=kwargs,
                )
                response_obj = await func(*args, **kwargs)
                _apply_headers_to_response(
                    response_obj=response_obj,
                    kwargs=kwargs,
                    headers=headers,
                )
                return response_obj

            return _async_wrapper

        @wraps(func)
        def _wrapper(*args, **kwargs):
            headers = _check_rate_limit(
                scope=resolved_scope,
                subjects=resolved_subjects,
                rules=resolved_rules,
                fail_open=fail_open,
                args=args,
                kwargs=kwargs,
            )
            response_obj = func(*args, **kwargs)
            _apply_headers_to_response(
                response_obj=response_obj,
                kwargs=kwargs,
                headers=headers,
            )
            return response_obj

        return _wrapper

    return _decorate


__all__ = [
    "RateLimitPreset",
    "RateLimitRule",
    "RateLimitCheckResult",
    "RateLimitException",
    "check_rate_limit",
    "rate_limit",
]
