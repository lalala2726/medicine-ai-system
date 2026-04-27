"""
统一工具缓存模块。

说明：
1. 所有工具缓存统一使用 Redis Hash，并按会话 UUID 隔离；
2. 缓存策略由 `ToolCacheProfile` 描述，不再为每个领域维护单独实现；
3. 工具函数通过 `@tool_cacheable(...)` 装饰器声明需要缓存，避免手写保存逻辑；
4. 当前默认仅做“成功返回后写缓存 + prompt 复用”，不做工具层直接命中短路。
"""

from __future__ import annotations

import hashlib
import inspect
import json
from contextvars import ContextVar, Token
from dataclasses import dataclass
from datetime import datetime, timezone
from enum import Enum
from functools import wraps
from typing import Any, Callable, ParamSpec, TypeVar

from pydantic import BaseModel

from app.core.database.redis import RedisHashCache

P = ParamSpec("P")
R = TypeVar("R")


class ToolCacheFieldStrategy(str, Enum):
    """
    功能描述：
        工具缓存字段生成策略。

    参数说明：
        无。

    返回值：
        无（枚举定义）。

    异常说明：
        无。
    """

    TOOL_NAME = "tool_name"
    TOOL_NAME_WITH_INPUT_HASH = "tool_name_with_input_hash"


class ToolCacheRenderStrategy(str, Enum):
    """
    功能描述：
        工具缓存 prompt 渲染策略。

    参数说明：
        无。

    返回值：
        无（枚举定义）。

    异常说明：
        无。
    """

    ORDERED_MAP = "ordered_map"
    RECENT_RECORDS = "recent_records"


@dataclass(frozen=True)
class ToolCacheProfile:
    """
    功能描述：
        工具缓存领域配置模型。

    参数说明：
        key_prefix (str): Redis key 前缀。
        ttl_seconds (int): 缓存 TTL，单位秒。
        prompt_title (str): `recent_records` 模式下的标题。
        field_strategy (ToolCacheFieldStrategy): Hash 字段生成策略。
        render_strategy (ToolCacheRenderStrategy): prompt 渲染策略。
        ordered_fields (tuple[str, ...]): `ordered_map` 模式下的字段顺序。
        max_prompt_records (int | None): `recent_records` 模式下的最大记录数。
        cache_none (bool): 是否缓存 `None` 结果。
        cache_empty_list (bool): 是否缓存空列表结果。

    返回值：
        无（数据模型定义）。

    异常说明：
        无。
    """

    key_prefix: str
    ttl_seconds: int
    prompt_title: str
    field_strategy: ToolCacheFieldStrategy
    render_strategy: ToolCacheRenderStrategy
    ordered_fields: tuple[str, ...] = ()
    max_prompt_records: int | None = None
    cache_none: bool = False
    cache_empty_list: bool = False


class ToolCacheEntry(BaseModel):
    """
    功能描述：
        单条工具缓存记录模型。

    参数说明：
        tool_name (str): 工具名称。
        tool_input (Any): 工具入参。
        tool_output (Any): 工具输出。
        updated_at (str): 缓存写入时间（UTC ISO8601）。

    返回值：
        无（数据模型定义）。

    异常说明：
        无。
    """

    tool_name: str
    tool_input: Any
    tool_output: Any
    updated_at: str


# 诊断工具缓存字段：症状候选检索。
DIAGNOSIS_CACHE_FIELD_SEARCH_SYMPTOM_CANDIDATES = "search_symptom_candidates"
# 诊断工具缓存字段：候选疾病召回。
DIAGNOSIS_CACHE_FIELD_QUERY_DISEASE_CANDIDATES = "query_disease_candidates_by_symptoms"
# 诊断工具缓存字段：单疾病详情查询。
DIAGNOSIS_CACHE_FIELD_QUERY_DISEASE_DETAIL = "query_disease_detail"
# 诊断工具缓存字段：批量疾病详情查询。
DIAGNOSIS_CACHE_FIELD_QUERY_DISEASE_DETAILS = "query_disease_details"
# 诊断工具缓存字段：追问症状候选。
DIAGNOSIS_CACHE_FIELD_QUERY_FOLLOWUP_SYMPTOMS = "query_followup_symptom_candidates"
# 诊断工具缓存固定顺序。
DIAGNOSIS_TOOL_CACHE_FIELDS = (
    DIAGNOSIS_CACHE_FIELD_SEARCH_SYMPTOM_CANDIDATES,
    DIAGNOSIS_CACHE_FIELD_QUERY_DISEASE_CANDIDATES,
    DIAGNOSIS_CACHE_FIELD_QUERY_DISEASE_DETAIL,
    DIAGNOSIS_CACHE_FIELD_QUERY_DISEASE_DETAILS,
    DIAGNOSIS_CACHE_FIELD_QUERY_FOLLOWUP_SYMPTOMS,
)

# admin 工具缓存配置。
ADMIN_TOOL_CACHE_PROFILE = ToolCacheProfile(
    key_prefix="admin:tool_cache",
    ttl_seconds=1800,
    prompt_title="已缓存后台工具结果",
    field_strategy=ToolCacheFieldStrategy.TOOL_NAME_WITH_INPUT_HASH,
    render_strategy=ToolCacheRenderStrategy.RECENT_RECORDS,
    max_prompt_records=8,
    cache_none=False,
    cache_empty_list=True,
)
# 诊断工具缓存配置。
DIAGNOSIS_TOOL_CACHE_PROFILE = ToolCacheProfile(
    key_prefix="diagnosis:tool_cache",
    ttl_seconds=1800,
    prompt_title="已缓存诊断工具结果",
    field_strategy=ToolCacheFieldStrategy.TOOL_NAME,
    render_strategy=ToolCacheRenderStrategy.ORDERED_MAP,
    ordered_fields=DIAGNOSIS_TOOL_CACHE_FIELDS,
    max_prompt_records=None,
    cache_none=False,
    cache_empty_list=False,
)
# client commerce 工具缓存配置。
CLIENT_COMMERCE_TOOL_CACHE_PROFILE = ToolCacheProfile(
    key_prefix="client:commerce:tool_cache",
    ttl_seconds=1800,
    prompt_title="已缓存商城工具结果",
    field_strategy=ToolCacheFieldStrategy.TOOL_NAME_WITH_INPUT_HASH,
    render_strategy=ToolCacheRenderStrategy.RECENT_RECORDS,
    max_prompt_records=8,
    cache_none=False,
    cache_empty_list=True,
)

_TOOL_CACHE_CONVERSATION_CONTEXTS: dict[str, ContextVar[str | None]] = {}


def _normalize_required_text(value: str, *, field_name: str) -> str:
    """
    功能描述：
        规范化必填字符串字段。

    参数说明：
        value (str): 原始字段值。
        field_name (str): 字段名称。

    返回值：
        str: 去除首尾空白后的非空字符串。

    异常说明：
        ValueError: 当字段为空时抛出。
    """

    normalized_value = str(value or "").strip()
    if not normalized_value:
        raise ValueError(f"{field_name} 不能为空")
    return normalized_value


def _serialize_cache_value(value: Any) -> Any:
    """
    功能描述：
        将缓存值递归转换为可 JSON 序列化结构。

    参数说明：
        value (Any): 原始缓存值。

    返回值：
        Any: 可 JSON 序列化的结构。

    异常说明：
        无。
    """

    if isinstance(value, BaseModel):
        return value.model_dump(mode="json", exclude_none=True)
    if isinstance(value, list):
        return [_serialize_cache_value(item) for item in value]
    if isinstance(value, tuple):
        return [_serialize_cache_value(item) for item in value]
    if isinstance(value, dict):
        return {
            str(key): _serialize_cache_value(item)
            for key, item in value.items()
        }
    return value


def _decode_redis_text(value: Any, *, field_name: str) -> str:
    """
    功能描述：
        将 Redis 返回值解码为文本。

    参数说明：
        value (Any): Redis 原始字段或字段值。
        field_name (str): 当前字段名称。

    返回值：
        str: 解码后的文本。

    异常说明：
        ValueError: 当解码结果为空时抛出。
    """

    if isinstance(value, bytes):
        return value.decode("utf-8")
    return _normalize_required_text(str(value or ""), field_name=field_name)


def _build_stable_json(value: Any) -> str:
    """
    功能描述：
        构造稳定排序的 JSON 文本，用于生成工具入参哈希。

    参数说明：
        value (Any): 任意待序列化对象。

    返回值：
        str: 稳定排序后的 JSON 文本。

    异常说明：
        无。
    """

    serialized_value = _serialize_cache_value(value)
    return json.dumps(
        serialized_value,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    )


def _get_profile_context_var(profile: ToolCacheProfile) -> ContextVar[str | None]:
    """
    功能描述：
        获取指定 profile 对应的会话上下文变量。

    参数说明：
        profile (ToolCacheProfile): 缓存 profile。

    返回值：
        ContextVar[str | None]: 对应的上下文变量。

    异常说明：
        无。
    """

    context_name = f"tool_cache_conversation_uuid:{profile.key_prefix}"
    context_var = _TOOL_CACHE_CONVERSATION_CONTEXTS.get(context_name)
    if context_var is None:
        context_var = ContextVar(context_name, default=None)
        _TOOL_CACHE_CONVERSATION_CONTEXTS[context_name] = context_var
    return context_var


def _build_tool_cache_field(
        profile: ToolCacheProfile,
        tool_name: str,
        tool_input: Any,
) -> str:
    """
    功能描述：
        根据 profile 策略构造 Redis Hash 字段名。

    参数说明：
        profile (ToolCacheProfile): 缓存 profile。
        tool_name (str): 工具名称。
        tool_input (Any): 工具入参。

    返回值：
        str: Redis Hash 字段名。

    异常说明：
        ValueError: 当工具名称为空时抛出。
    """

    normalized_tool_name = _normalize_required_text(tool_name, field_name="tool_name")
    if profile.field_strategy is ToolCacheFieldStrategy.TOOL_NAME:
        return normalized_tool_name

    stable_json = _build_stable_json(tool_input)
    input_hash = hashlib.sha256(stable_json.encode("utf-8")).hexdigest()
    return f"{normalized_tool_name}:{input_hash}"


def build_tool_cache_key(profile: ToolCacheProfile, conversation_uuid: str) -> str:
    """
    功能描述：
        构造指定 profile 的 Redis key。

    参数说明：
        profile (ToolCacheProfile): 缓存 profile。
        conversation_uuid (str): 当前会话 UUID。

    返回值：
        str: Redis key。

    异常说明：
        ValueError: 当会话 UUID 为空时抛出。
    """

    normalized_uuid = _normalize_required_text(
        conversation_uuid,
        field_name="conversation_uuid",
    )
    return f"{profile.key_prefix}:{normalized_uuid}"


def bind_tool_cache_conversation(
        profile: ToolCacheProfile,
        conversation_uuid: str,
) -> Token[str | None]:
    """
    功能描述：
        绑定当前上下文正在使用的会话 UUID。

    参数说明：
        profile (ToolCacheProfile): 缓存 profile。
        conversation_uuid (str): 当前会话 UUID。

    返回值：
        Token[str | None]: `ContextVar` 重置令牌。

    异常说明：
        ValueError: 当会话 UUID 为空时抛出。
    """

    normalized_uuid = _normalize_required_text(
        conversation_uuid,
        field_name="conversation_uuid",
    )
    return _get_profile_context_var(profile).set(normalized_uuid)


def reset_tool_cache_conversation(
        profile: ToolCacheProfile,
        token: Token[str | None],
) -> None:
    """
    功能描述：
        重置当前上下文绑定的会话 UUID。

    参数说明：
        profile (ToolCacheProfile): 缓存 profile。
        token (Token[str | None]): 绑定时返回的重置令牌。

    返回值：
        None。

    异常说明：
        无。
    """

    _get_profile_context_var(profile).reset(token)


def _get_current_tool_cache_conversation_uuid(profile: ToolCacheProfile) -> str:
    """
    功能描述：
        读取当前上下文绑定的会话 UUID。

    参数说明：
        profile (ToolCacheProfile): 缓存 profile。

    返回值：
        str: 当前会话 UUID。

    异常说明：
        ValueError: 当当前上下文未绑定会话 UUID 时抛出。
    """

    conversation_uuid = _get_profile_context_var(profile).get()
    return _normalize_required_text(
        str(conversation_uuid or ""),
        field_name="conversation_uuid",
    )


def load_tool_cache(profile: ToolCacheProfile, conversation_uuid: str) -> dict[str, Any]:
    """
    功能描述：
        读取当前会话的工具缓存。

    参数说明：
        profile (ToolCacheProfile): 缓存 profile。
        conversation_uuid (str): 当前会话 UUID。

    返回值：
        dict[str, Any]: `field -> cache_entry` 的缓存映射。

    异常说明：
        ValueError: 当会话 UUID 为空时抛出。
    """

    cache_key = build_tool_cache_key(profile, conversation_uuid)
    raw_hash = RedisHashCache().h_get_all(cache_key)
    if not raw_hash:
        return {}

    cache_payload: dict[str, Any] = {}
    for raw_field, raw_value in raw_hash.items():
        decoded_field = _decode_redis_text(raw_field, field_name="cache_field")
        cache_payload[decoded_field] = json.loads(
            _decode_redis_text(raw_value, field_name=decoded_field),
        )
    return cache_payload


def save_tool_cache_entry(
        profile: ToolCacheProfile,
        conversation_uuid: str,
        tool_name: str,
        tool_input: Any,
        tool_output: Any,
) -> None:
    """
    功能描述：
        保存单条工具缓存记录。

    参数说明：
        profile (ToolCacheProfile): 缓存 profile。
        conversation_uuid (str): 当前会话 UUID。
        tool_name (str): 工具名称。
        tool_input (Any): 工具入参。
        tool_output (Any): 工具输出。

    返回值：
        None。

    异常说明：
        ValueError: 当会话 UUID 或工具名称为空时抛出。
    """

    cache_key = build_tool_cache_key(profile, conversation_uuid)
    normalized_tool_name = _normalize_required_text(tool_name, field_name="tool_name")
    normalized_tool_input = _serialize_cache_value(tool_input)
    normalized_tool_output = _serialize_cache_value(tool_output)
    cache_field = _build_tool_cache_field(
        profile,
        normalized_tool_name,
        normalized_tool_input,
    )
    cache_entry = ToolCacheEntry(
        tool_name=normalized_tool_name,
        tool_input=normalized_tool_input,
        tool_output=normalized_tool_output,
        updated_at=datetime.now(timezone.utc).isoformat(),
    )
    redis_hash_cache = RedisHashCache()
    redis_hash_cache.h_put(
        cache_key,
        cache_field,
        json.dumps(cache_entry.model_dump(mode="json"), ensure_ascii=False),
    )
    redis_hash_cache.expire(cache_key, profile.ttl_seconds)


def save_current_tool_cache_entry(
        profile: ToolCacheProfile,
        tool_name: str,
        tool_input: Any,
        tool_output: Any,
) -> None:
    """
    功能描述：
        基于当前上下文会话保存工具缓存记录。

    参数说明：
        profile (ToolCacheProfile): 缓存 profile。
        tool_name (str): 工具名称。
        tool_input (Any): 工具入参。
        tool_output (Any): 工具输出。

    返回值：
        None。

    异常说明：
        ValueError: 当当前上下文未绑定会话 UUID 时抛出。
    """

    conversation_uuid = _get_current_tool_cache_conversation_uuid(profile)
    save_tool_cache_entry(
        profile=profile,
        conversation_uuid=conversation_uuid,
        tool_name=tool_name,
        tool_input=tool_input,
        tool_output=tool_output,
    )


def render_tool_cache_prompt(
        profile: ToolCacheProfile,
        conversation_uuid: str,
) -> str:
    """
    功能描述：
        根据 profile 渲染工具缓存 prompt 片段。

    参数说明：
        profile (ToolCacheProfile): 缓存 profile。
        conversation_uuid (str): 当前会话 UUID。

    返回值：
        str: JSON code block 形式的缓存片段；无缓存时返回空字符串。

    异常说明：
        ValueError: 当会话 UUID 为空时抛出。
    """

    cache_payload = load_tool_cache(profile, conversation_uuid)
    if not cache_payload:
        return ""

    if profile.render_strategy is ToolCacheRenderStrategy.ORDERED_MAP:
        rendered_payload: dict[str, Any] = {}
        for field_name in profile.ordered_fields:
            if field_name in cache_payload:
                rendered_payload[field_name] = cache_payload[field_name]
        for field_name, field_value in cache_payload.items():
            if field_name in rendered_payload:
                continue
            rendered_payload[field_name] = field_value
        rendered_json = json.dumps(rendered_payload, ensure_ascii=False, indent=2)
        return f"```json\n{rendered_json}\n```"

    sorted_entries = sorted(
        cache_payload.values(),
        key=lambda item: str(item.get("updated_at") or ""),
        reverse=True,
    )
    max_prompt_records = profile.max_prompt_records or len(sorted_entries)
    rendered_payload = {
        "title": profile.prompt_title,
        "records": sorted_entries[:max_prompt_records],
    }
    rendered_json = json.dumps(rendered_payload, ensure_ascii=False, indent=2)
    return f"```json\n{rendered_json}\n```"


def _should_cache_result(
        profile: ToolCacheProfile,
        tool_output: Any,
        should_cache: Callable[[Any], bool] | None,
) -> bool:
    """
    功能描述：
        判断当前工具返回值是否需要写入缓存。

    参数说明：
        profile (ToolCacheProfile): 缓存 profile。
        tool_output (Any): 工具输出。
        should_cache (Callable[[Any], bool] | None): 显式缓存判断函数。

    返回值：
        bool: `True` 表示写缓存，`False` 表示跳过。

    异常说明：
        无。
    """

    if should_cache is not None:
        return bool(should_cache(tool_output))
    if tool_output is None and not profile.cache_none:
        return False
    if isinstance(tool_output, list) and not tool_output and not profile.cache_empty_list:
        return False
    return True


def _bind_tool_arguments(
        func: Callable[..., Any],
        args: tuple[Any, ...],
        kwargs: dict[str, Any],
) -> dict[str, Any]:
    """
    功能描述：
        根据工具函数签名绑定本次调用的参数，并补齐默认值。

    参数说明：
        func (Callable[..., Any]): 原始工具函数。
        args (tuple[Any, ...]): 位置参数。
        kwargs (dict[str, Any]): 关键字参数。

    返回值：
        dict[str, Any]: 规范化后的参数映射。

    异常说明：
        TypeError: 当参数绑定失败时抛出。
    """

    signature = inspect.signature(func)
    bound_arguments = signature.bind_partial(*args, **kwargs)
    bound_arguments.apply_defaults()
    return dict(bound_arguments.arguments)


def tool_cacheable(
        profile: ToolCacheProfile,
        *,
        tool_name: str | None = None,
        input_builder: Callable[[dict[str, Any]], Any] | None = None,
        should_cache: Callable[[Any], bool] | None = None,
) -> Callable[[Callable[P, R]], Callable[P, R]]:
    """
    功能描述：
        为工具函数增加“成功返回后写入当前会话缓存”的能力。

    参数说明：
        profile (ToolCacheProfile): 缓存 profile。
        tool_name (str | None): 缓存时使用的工具名；为空时回退函数名。
        input_builder (Callable[[dict[str, Any]], Any] | None):
            根据绑定后的参数映射构造缓存入参；为空时直接使用参数映射。
        should_cache (Callable[[Any], bool] | None):
            自定义缓存判定函数；为空时按 profile 默认策略判断。

    返回值：
        Callable[[Callable[P, R]], Callable[P, R]]: 装饰器函数。

    异常说明：
        无；工具函数自身异常保持原样抛出。
    """

    def _decorate(func: Callable[P, R]) -> Callable[P, R]:
        resolved_tool_name = _normalize_required_text(
            tool_name or func.__name__,
            field_name="tool_name",
        )

        if inspect.iscoroutinefunction(func):

            @wraps(func)
            async def _async_wrapper(*args: P.args, **kwargs: P.kwargs) -> R:
                result = await func(*args, **kwargs)
                if _should_cache_result(profile, result, should_cache):
                    argument_mapping = _bind_tool_arguments(
                        func,
                        args,
                        dict(kwargs),
                    )
                    cache_input = (
                        input_builder(argument_mapping)
                        if input_builder is not None
                        else argument_mapping
                    )
                    save_current_tool_cache_entry(
                        profile=profile,
                        tool_name=resolved_tool_name,
                        tool_input=cache_input,
                        tool_output=result,
                    )
                return result

            return _async_wrapper

        @wraps(func)
        def _sync_wrapper(*args: P.args, **kwargs: P.kwargs) -> R:
            result = func(*args, **kwargs)
            if _should_cache_result(profile, result, should_cache):
                argument_mapping = _bind_tool_arguments(
                    func,
                    args,
                    dict(kwargs),
                )
                cache_input = (
                    input_builder(argument_mapping)
                    if input_builder is not None
                    else argument_mapping
                )
                save_current_tool_cache_entry(
                    profile=profile,
                    tool_name=resolved_tool_name,
                    tool_input=cache_input,
                    tool_output=result,
                )
            return result

        return _sync_wrapper

    return _decorate


__all__ = [
    "ADMIN_TOOL_CACHE_PROFILE",
    "CLIENT_COMMERCE_TOOL_CACHE_PROFILE",
    "DIAGNOSIS_CACHE_FIELD_QUERY_DISEASE_CANDIDATES",
    "DIAGNOSIS_CACHE_FIELD_QUERY_DISEASE_DETAIL",
    "DIAGNOSIS_CACHE_FIELD_QUERY_DISEASE_DETAILS",
    "DIAGNOSIS_CACHE_FIELD_QUERY_FOLLOWUP_SYMPTOMS",
    "DIAGNOSIS_CACHE_FIELD_SEARCH_SYMPTOM_CANDIDATES",
    "DIAGNOSIS_TOOL_CACHE_FIELDS",
    "DIAGNOSIS_TOOL_CACHE_PROFILE",
    "ToolCacheEntry",
    "ToolCacheFieldStrategy",
    "ToolCacheProfile",
    "ToolCacheRenderStrategy",
    "bind_tool_cache_conversation",
    "build_tool_cache_key",
    "load_tool_cache",
    "render_tool_cache_prompt",
    "reset_tool_cache_conversation",
    "save_current_tool_cache_entry",
    "save_tool_cache_entry",
    "tool_cacheable",
]
