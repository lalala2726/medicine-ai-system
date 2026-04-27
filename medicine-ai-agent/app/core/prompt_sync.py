from __future__ import annotations

import json
import threading
from dataclasses import dataclass
from datetime import datetime
from enum import Enum
from typing import Any

from loguru import logger

from app.core.database import get_redis_connection
from app.utils.resource_text_utils import load_resource_text

#: Redis 中单条 Agent 提示词运行时配置的 key 模板。
PROMPT_CONFIG_REDIS_KEY_TEMPLATE = "agent:prompt:{}"
#: 当前支持的提示词缓存 schema 版本。
PROMPT_CONFIG_SCHEMA_VERSION = 1
#: 当前纳入 Redis 在线管理的提示词业务键。
MANAGED_PROMPT_KEYS: tuple[str, ...] = (
    "admin_assistant_system_prompt",
    "client_service_node_system_prompt",
    "client_medical_node_system_prompt",
    "client_base_prompt",
    "system_base_prompt",
)

#: 本地提示词文件缓存字典，避免重复磁盘读取。
_LOCAL_PROMPT_CACHE: dict[str, str] = {}


class AgentPromptSource(str, Enum):
    """当前内存提示词快照来源。"""

    REDIS = "redis"
    LOCAL_FALLBACK = "local_fallback"


class AgentPromptLoadReason(str, Enum):
    """提示词快照加载失败原因。"""

    REDIS_KEY_MISSING = "redis_key_missing"
    INVALID_UTF8 = "invalid_utf8"
    UNSUPPORTED_PAYLOAD_TYPE = "unsupported_payload_type"
    REDIS_READ_FAILED = "redis_read_failed"
    INVALID_JSON = "invalid_json"
    INVALID_SCHEMA = "invalid_schema"


@dataclass(frozen=True)
class AgentPromptSnapshot:
    """Agent 提示词运行时快照。"""

    schema_version: int | None
    updated_at: datetime | None
    updated_by: str | None
    prompts: dict[str, str]


@dataclass(frozen=True)
class AgentPromptRefreshResult:
    """提示词快照刷新结果。"""

    applied: bool
    previous_snapshot: AgentPromptSnapshot | None
    current_snapshot: AgentPromptSnapshot | None


@dataclass(frozen=True)
class _RedisPromptRecord:
    """单条 Redis 提示词记录。"""

    schema_version: int | None
    prompt_key: str
    prompt_version: int | None
    updated_at: datetime | None
    updated_by: str | None
    prompt_content: str


class AgentPromptLoadError(RuntimeError):
    """提示词快照加载异常。"""

    def __init__(self, reason: AgentPromptLoadReason, message: str) -> None:
        """初始化加载异常。

        Args:
            reason: 失败原因枚举。
            message: 异常消息。
        """

        super().__init__(message)
        self.reason = reason


#: 保护进程内提示词快照读写的一把锁。
_PROMPT_LOCK = threading.RLock()
#: 当前生效的进程内提示词快照。
_current_prompt_snapshot: AgentPromptSnapshot | None = None
#: 当前生效提示词快照来源。
_current_prompt_source = AgentPromptSource.LOCAL_FALLBACK


def _normalize_optional_str(value: Any) -> str | None:
    """归一化可选字符串值。

    Args:
        value: 原始输入值。

    Returns:
        str | None: 去空白后的字符串；空值返回 ``None``。
    """

    if value is None:
        return None
    normalized = str(value).strip()
    return normalized or None


def _build_prompt_redis_key(prompt_key: str) -> str:
    """按业务键构建 Redis key。

    Args:
        prompt_key: 提示词业务键。

    Returns:
        str: 对应的 Redis key。
    """

    normalized_prompt_key = _normalize_optional_str(prompt_key)
    if normalized_prompt_key is None:
        raise ValueError("prompt_key cannot be empty")
    return PROMPT_CONFIG_REDIS_KEY_TEMPLATE.format(normalized_prompt_key)


def _is_managed_prompt_key(prompt_key: str) -> bool:
    """判断是否为受 Redis 在线管理的提示词键。

    Args:
        prompt_key: 提示词业务键。

    Returns:
        bool: ``True`` 表示当前 key 应从 Redis 读取。
    """

    return prompt_key in MANAGED_PROMPT_KEYS


def _decode_redis_payload(*, raw_payload: Any, redis_key: str) -> str:
    """将 Redis 原始返回值解码为 JSON 文本。

    Args:
        raw_payload: Redis ``GET`` 返回的原始值。
        redis_key: 当前读取的 Redis key。

    Returns:
        str: 解码后的 JSON 字符串。

    Raises:
        AgentPromptLoadError: 当 payload 不合法时抛出。
    """

    if raw_payload is None:
        raise AgentPromptLoadError(
            AgentPromptLoadReason.REDIS_KEY_MISSING,
            f"Redis key {redis_key} is missing",
        )
    if isinstance(raw_payload, bytes):
        try:
            return raw_payload.decode("utf-8")
        except UnicodeDecodeError as exc:
            raise AgentPromptLoadError(
                AgentPromptLoadReason.INVALID_UTF8,
                "Agent prompt payload is not valid utf-8",
            ) from exc
    if isinstance(raw_payload, str):
        return raw_payload
    raise AgentPromptLoadError(
        AgentPromptLoadReason.UNSUPPORTED_PAYLOAD_TYPE,
        f"Unsupported prompt payload type: {type(raw_payload)!r}",
    )


def _unwrap_payload_root(data: Any) -> dict[str, Any]:
    """提取提示词缓存实际根对象。

    Args:
        data: ``json.loads`` 后的对象。

    Returns:
        dict[str, Any]: 供解析的实际缓存根对象。

    Raises:
        AgentPromptLoadError: 当结构非法时抛出。
    """

    if not isinstance(data, dict):
        raise AgentPromptLoadError(
            AgentPromptLoadReason.INVALID_SCHEMA,
            "Prompt payload root must be a JSON object",
        )
    wrapped_data = data.get("data")
    if wrapped_data is None:
        return data
    if not isinstance(wrapped_data, dict):
        raise AgentPromptLoadError(
            AgentPromptLoadReason.INVALID_SCHEMA,
            "Wrapped prompt payload data must be a JSON object",
        )
    return wrapped_data


def _parse_updated_at(value: Any) -> datetime | None:
    """解析更新时间字段。

    Args:
        value: 原始 ``updatedAt`` 值。

    Returns:
        datetime | None: 解析成功返回时间对象，失败返回 ``None``。
    """

    normalized_value = _normalize_optional_str(value)
    if normalized_value is None:
        return None
    try:
        return datetime.fromisoformat(normalized_value)
    except ValueError:
        return None


def _parse_optional_int(value: Any) -> int | None:
    """解析可选整数值。

    Args:
        value: 原始输入值。

    Returns:
        int | None: 解析成功返回整数，失败返回 ``None``。
    """

    if value is None:
        return None
    normalized_value = str(value).strip()
    if not normalized_value:
        return None
    try:
        return int(normalized_value)
    except (TypeError, ValueError):
        return None


def _load_prompt_record_from_redis(*, prompt_key: str, redis_key: str) -> _RedisPromptRecord:
    """从 Redis 读取并反序列化单条提示词记录。

    Args:
        prompt_key: 目标提示词业务键。
        redis_key: 目标 Redis key。

    Returns:
        _RedisPromptRecord: 归一化后的单条提示词记录。

    Raises:
        AgentPromptLoadError: 当读取、反序列化或结构校验失败时抛出。
    """

    try:
        raw_payload = get_redis_connection().get(redis_key)
    except Exception as exc:
        raise AgentPromptLoadError(
            AgentPromptLoadReason.REDIS_READ_FAILED,
            f"Failed to read redis key {redis_key}",
        ) from exc
    payload = _decode_redis_payload(
        raw_payload=raw_payload,
        redis_key=redis_key,
    )
    try:
        data = json.loads(payload)
    except json.JSONDecodeError as exc:
        raise AgentPromptLoadError(
            AgentPromptLoadReason.INVALID_JSON,
            "Prompt payload is not valid JSON",
        ) from exc
    root = _unwrap_payload_root(data)
    payload_prompt_key = _normalize_optional_str(root.get("promptKey"))
    if payload_prompt_key is not None and payload_prompt_key != prompt_key:
        raise AgentPromptLoadError(
            AgentPromptLoadReason.INVALID_SCHEMA,
            f"Prompt payload key mismatch: expected {prompt_key}, got {payload_prompt_key}",
        )
    prompt_content = _normalize_optional_str(root.get("promptContent"))
    if prompt_content is None:
        raise AgentPromptLoadError(
            AgentPromptLoadReason.INVALID_SCHEMA,
            "Prompt payload promptContent must be a non-empty string",
        )
    return _RedisPromptRecord(
        schema_version=_parse_optional_int(root.get("schemaVersion")),
        prompt_key=prompt_key,
        prompt_version=_parse_optional_int(root.get("promptVersion")),
        updated_at=_parse_updated_at(root.get("updatedAt")),
        updated_by=_normalize_optional_str(root.get("updatedBy")),
        prompt_content=prompt_content,
    )


def _build_local_fallback_snapshot() -> AgentPromptSnapshot:
    """构造空的本地回退快照。

    Returns:
        AgentPromptSnapshot: 空快照对象。
    """

    return AgentPromptSnapshot(
        schema_version=PROMPT_CONFIG_SCHEMA_VERSION,
        updated_at=None,
        updated_by="local_file_fallback",
        prompts={},
    )


def _copy_snapshot(snapshot: AgentPromptSnapshot | None) -> AgentPromptSnapshot | None:
    """复制提示词快照对象。

    Args:
        snapshot: 原始快照对象。

    Returns:
        AgentPromptSnapshot | None: 深拷贝快照；空值返回 ``None``。
    """

    if snapshot is None:
        return None
    return AgentPromptSnapshot(
        schema_version=snapshot.schema_version,
        updated_at=snapshot.updated_at,
        updated_by=snapshot.updated_by,
        prompts=dict(snapshot.prompts),
    )


def _set_current_snapshot(snapshot: AgentPromptSnapshot, *, source: AgentPromptSource) -> None:
    """原子更新当前生效提示词快照。

    Args:
        snapshot: 新快照对象。
        source: 快照来源。
    """

    global _current_prompt_snapshot, _current_prompt_source
    _current_prompt_snapshot = snapshot
    _current_prompt_source = source


def _apply_prompt_record_to_snapshot(
        *,
        snapshot: AgentPromptSnapshot,
        prompt_key: str,
        prompt_record: _RedisPromptRecord | None,
) -> AgentPromptSnapshot:
    """将单条提示词记录合并到快照中。

    Args:
        snapshot: 原始提示词快照。
        prompt_key: 当前处理的提示词业务键。
        prompt_record: 新的 Redis 记录；为空表示删除该 key。

    Returns:
        AgentPromptSnapshot: 合并后的提示词快照。
    """

    next_prompts = dict(snapshot.prompts)
    if prompt_record is None:
        next_prompts.pop(prompt_key, None)
        return AgentPromptSnapshot(
            schema_version=snapshot.schema_version,
            updated_at=snapshot.updated_at,
            updated_by=snapshot.updated_by,
            prompts=next_prompts,
        )
    next_prompts[prompt_key] = prompt_record.prompt_content
    return AgentPromptSnapshot(
        schema_version=prompt_record.schema_version or snapshot.schema_version,
        updated_at=prompt_record.updated_at,
        updated_by=prompt_record.updated_by,
        prompts=next_prompts,
    )


def _build_snapshot_from_redis() -> AgentPromptSnapshot:
    """从 Redis 批量构建托管提示词快照。

    Returns:
        AgentPromptSnapshot: 初始化后的托管提示词快照。
    """

    snapshot = _build_local_fallback_snapshot()
    for prompt_key in MANAGED_PROMPT_KEYS:
        redis_key = _build_prompt_redis_key(prompt_key)
        try:
            prompt_record = _load_prompt_record_from_redis(
                prompt_key=prompt_key,
                redis_key=redis_key,
            )
        except AgentPromptLoadError as exc:
            if exc.reason != AgentPromptLoadReason.REDIS_KEY_MISSING:
                logger.warning(
                    "Agent 托管提示词初始化失败：prompt_key={}，redis_key={}，错误原因={}",
                    prompt_key,
                    redis_key,
                    exc.reason.value,
                )
            continue
        snapshot = _apply_prompt_record_to_snapshot(
            snapshot=snapshot,
            prompt_key=prompt_key,
            prompt_record=prompt_record,
        )
    return snapshot


def initialize_agent_prompt_snapshot() -> AgentPromptSnapshot:
    """初始化进程内提示词快照。

    Returns:
        AgentPromptSnapshot: 初始化后生效的提示词快照副本。
    """

    with _PROMPT_LOCK:
        if _current_prompt_snapshot is not None:
            return _copy_snapshot(_current_prompt_snapshot) or _build_local_fallback_snapshot()

    snapshot = _build_snapshot_from_redis()
    source = AgentPromptSource.REDIS if snapshot.prompts else AgentPromptSource.LOCAL_FALLBACK
    with _PROMPT_LOCK:
        _set_current_snapshot(snapshot, source=source)
    logger.info(
        "Agent 提示词初始化完成：来源={}，managed_prompt_count={}",
        source.value,
        len(snapshot.prompts),
    )
    return _copy_snapshot(snapshot) or _build_local_fallback_snapshot()


def get_current_agent_prompt_snapshot() -> AgentPromptSnapshot:
    """读取当前生效提示词快照。

    Returns:
        AgentPromptSnapshot: 当前快照副本。
    """

    with _PROMPT_LOCK:
        if _current_prompt_snapshot is not None:
            return _copy_snapshot(_current_prompt_snapshot) or _build_local_fallback_snapshot()
    return initialize_agent_prompt_snapshot()


def refresh_agent_prompt_snapshot(*, prompt_key: str, redis_key: str) -> AgentPromptRefreshResult:
    """刷新进程内的单条托管提示词快照。

    Args:
        prompt_key: 需要重新读取的提示词业务键。
        redis_key: 需要重新读取的 Redis key。

    Returns:
        AgentPromptRefreshResult: 结构化刷新结果。
    """

    normalized_prompt_key = _normalize_optional_str(prompt_key)
    if normalized_prompt_key is None:
        raise ValueError("prompt_key cannot be empty")
    with _PROMPT_LOCK:
        previous_snapshot = _copy_snapshot(_current_prompt_snapshot)
        base_snapshot = previous_snapshot or _build_local_fallback_snapshot()
    if not _is_managed_prompt_key(normalized_prompt_key):
        logger.info("忽略未托管的提示词刷新消息：prompt_key={}，redis_key={}", normalized_prompt_key, redis_key)
        return AgentPromptRefreshResult(
            applied=False,
            previous_snapshot=previous_snapshot,
            current_snapshot=previous_snapshot,
        )
    try:
        prompt_record = _load_prompt_record_from_redis(
            prompt_key=normalized_prompt_key,
            redis_key=redis_key,
        )
    except AgentPromptLoadError as exc:
        if exc.reason != AgentPromptLoadReason.REDIS_KEY_MISSING:
            logger.warning(
                "Agent 提示词刷新失败，继续保留当前快照：prompt_key={}，redis_key={}，错误原因={}",
                normalized_prompt_key,
                redis_key,
                exc.reason.value,
            )
            return AgentPromptRefreshResult(
                applied=False,
                previous_snapshot=previous_snapshot,
                current_snapshot=previous_snapshot,
            )
        next_snapshot = _apply_prompt_record_to_snapshot(
            snapshot=base_snapshot,
            prompt_key=normalized_prompt_key,
            prompt_record=None,
        )
    else:
        next_snapshot = _apply_prompt_record_to_snapshot(
            snapshot=base_snapshot,
            prompt_key=normalized_prompt_key,
            prompt_record=prompt_record,
        )
    next_source = AgentPromptSource.REDIS if next_snapshot.prompts else AgentPromptSource.LOCAL_FALLBACK
    with _PROMPT_LOCK:
        _set_current_snapshot(next_snapshot, source=next_source)
    current_snapshot = _copy_snapshot(next_snapshot)
    logger.info(
        "Agent 提示词刷新已生效：prompt_key={}，redis_key={}，managed_prompt_count={}",
        normalized_prompt_key,
        redis_key,
        len(next_snapshot.prompts),
    )
    return AgentPromptRefreshResult(
        applied=True,
        previous_snapshot=previous_snapshot,
        current_snapshot=current_snapshot,
    )


def load_managed_prompt(prompt_key: str, local_prompt_path: str | None = None) -> str:
    """按统一业务键读取 Agent 提示词正文。

    读取规则：
        1. 托管提示词键：只读取 Redis 运行时缓存；
        2. 非托管提示词键：继续使用本地提示词文件。

    Args:
        prompt_key: 提示词业务键。
        local_prompt_path: 可选本地提示词相对路径（基于 ``resources/prompt``）。

    Returns:
        str: 最终生效提示词正文。

    Raises:
        ValueError: 提示词键为空时抛出。
    """

    normalized_prompt_key = _normalize_optional_str(prompt_key)
    if normalized_prompt_key is None:
        raise ValueError("prompt_key cannot be empty")
    if _is_managed_prompt_key(normalized_prompt_key):
        snapshot = get_current_agent_prompt_snapshot()
        managed_prompt = snapshot.prompts.get(normalized_prompt_key)
        return managed_prompt or ""

    normalized_local_prompt_path = _normalize_optional_str(local_prompt_path)
    if normalized_local_prompt_path is None:
        return ""
    try:
        return load_resource_text(
            "prompt",
            normalized_local_prompt_path,
            allowed_suffixes=(".md",),
            cache=_LOCAL_PROMPT_CACHE,
        )
    except Exception as exc:
        logger.warning(
            "Agent 提示词本地回退加载失败：prompt_key={}，local_prompt_path={}，error={}",
            normalized_prompt_key,
            normalized_local_prompt_path,
            repr(exc),
        )
        return ""


def clear_agent_prompt_snapshot_state() -> None:
    """清理进程内提示词快照状态。

    Returns:
        None: 无返回值。
    """

    global _current_prompt_snapshot, _current_prompt_source
    with _PROMPT_LOCK:
        _current_prompt_snapshot = None
        _current_prompt_source = AgentPromptSource.LOCAL_FALLBACK


__all__ = [
    "PROMPT_CONFIG_REDIS_KEY_TEMPLATE",
    "PROMPT_CONFIG_SCHEMA_VERSION",
    "MANAGED_PROMPT_KEYS",
    "AgentPromptLoadError",
    "AgentPromptLoadReason",
    "AgentPromptRefreshResult",
    "AgentPromptSnapshot",
    "AgentPromptSource",
    "clear_agent_prompt_snapshot_state",
    "get_current_agent_prompt_snapshot",
    "initialize_agent_prompt_snapshot",
    "load_managed_prompt",
    "refresh_agent_prompt_snapshot",
]
