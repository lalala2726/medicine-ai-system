"""Redis 版本检查：判定 MQ 消息是否过期（stale）。

导入链路按 ``biz_key`` 粒度，切片重建链路按 ``vector_id`` 粒度。
版本号由业务服务在发送命令消息前写入 Redis，
本模块仅健读不写。
"""

from __future__ import annotations

from loguru import logger

from app.core.database import get_redis_connection

# ---- Redis key 前缀 -----------------------------------------------------------

# 导入链路版本键格式: kb:latest:{biz_key}
IMPORT_VERSION_KEY_PREFIX = "kb:latest"
# 切片重建版本键格式: kb:chunk_edit:latest_version:{vector_id}
CHUNK_REBUILD_VERSION_KEY_PREFIX = "kb:chunk_edit:latest_version"


# ---- 内部工具 ----------------------------------------------------------------


def _read_version(key: str) -> int | None:
    """从 Redis GET 读取并转换版本号。

    Args:
        key: Redis 键名。

    Returns:
        整型版本号，或 ``None``（无记录 / 读取失败 / 值非法）。
    """
    try:
        raw = get_redis_connection().get(key)
    except Exception:
        logger.exception("[mq] Redis 读取版本失败: key={}", key)
        return None
    if raw is None:
        return None
    try:
        return int(raw if isinstance(raw, str) else raw.decode())
    except (ValueError, AttributeError):
        logger.warning("[mq] Redis 版本值非法: key={}, raw={}", key, raw)
        return None


# ---- 导入链路 ----------------------------------------------------------------


def is_import_stale(*, biz_key: str, version: int) -> bool:
    """判定导入消息是否已过期。

    当 Redis 中存在更新的版本号时返回 True，此时应丢弃消息。

    Args:
        biz_key: 业务唯一标识。
        version: 消息携带的版本号。

    Returns:
        “已过期”返回 True。
    """
    latest = _read_version(f"{IMPORT_VERSION_KEY_PREFIX}:{biz_key}")
    if latest is None:
        return False
    return version < latest


# ---- 切片重建链路 ------------------------------------------------------------


def get_chunk_rebuild_latest_version(*, vector_id: int) -> int | None:
    """获取切片重建的最新版本号。

    Args:
        vector_id: 向量记录 ID。

    Returns:
        整型版本号，或 ``None``。
    """
    return _read_version(f"{CHUNK_REBUILD_VERSION_KEY_PREFIX}:{vector_id}")


def is_chunk_rebuild_stale(*, vector_id: int, version: int) -> bool:
    """判定切片重建消息是否已过期。

    Args:
        vector_id: 向量记录 ID。
        version: 消息携带的版本号。

    Returns:
        “已过期”返回 True。
    """
    latest = get_chunk_rebuild_latest_version(vector_id=vector_id)
    if latest is None:
        return False
    return version < latest
