from __future__ import annotations

import os
from functools import lru_cache

from pymongo import MongoClient
from pymongo.database import Database
from pymongo.errors import PyMongoError

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException

DEFAULT_MONGODB_URI = "mongodb://localhost:27017"  # MongoDB 默认连接地址
DEFAULT_MONGODB_DB_NAME = "medicine_ai_agent"  # MongoDB 默认数据库名
DEFAULT_MONGODB_TIMEOUT_MS = 3000  # MongoDB 默认超时（毫秒）
MONGODB_CONVERSATIONS_COLLECTION = "conversations"  # MongoDB 会话集合固定名称
MONGODB_MESSAGES_COLLECTION = "messages"  # MongoDB 消息集合固定名称
MONGODB_MESSAGE_TTS_USAGES_COLLECTION = "message_tts_usages"  # MongoDB 语音用量集合固定名称
MONGODB_CONVERSATION_SUMMARIES_COLLECTION = "conversation_summaries"  # MongoDB 会话摘要集合固定名称
MONGODB_TOOL_TRACES_COLLECTION = "tool_traces"  # MongoDB 工具轨迹集合固定名称
DEFAULT_MONGODB_STARTUP_PING_ENABLED = False  # 启动期是否执行 MongoDB ping


def _parse_timeout_ms(value: str | None) -> int:
    """解析并校验 MongoDB 超时配置（毫秒）。

    Args:
        value: 环境变量 `MONGODB_TIMEOUT_MS` 原始值。

    Returns:
        int: 合法超时毫秒数。

    Raises:
        ServiceException: 配置不是正整数时抛出。
    """
    if value is None or value == "":
        return DEFAULT_MONGODB_TIMEOUT_MS
    try:
        timeout_ms = int(value)
    except ValueError as exc:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message="MONGODB_TIMEOUT_MS 必须是整数",
        ) from exc

    if timeout_ms <= 0:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message="MONGODB_TIMEOUT_MS 必须大于 0",
        )
    return timeout_ms


@lru_cache(maxsize=1)
def get_mongo_client() -> MongoClient:
    """创建并缓存 MongoDB 客户端。

    Returns:
        MongoClient: 进程级复用的 MongoDB 客户端。

    Raises:
        ServiceException: 超时配置非法时抛出。
    """
    uri = os.getenv("MONGODB_URI", DEFAULT_MONGODB_URI)
    timeout_ms = _parse_timeout_ms(os.getenv("MONGODB_TIMEOUT_MS"))
    return MongoClient(
        uri,
        serverSelectionTimeoutMS=timeout_ms,
        connectTimeoutMS=timeout_ms,
        socketTimeoutMS=timeout_ms,
    )


def get_mongo_database() -> Database:
    """获取当前业务 MongoDB 数据库实例。

    Returns:
        Database: 业务数据库对象。

    Raises:
        ServiceException: 数据库名为空时抛出。
    """
    db_name = (os.getenv("MONGODB_DB_NAME") or DEFAULT_MONGODB_DB_NAME).strip()
    if not db_name:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message="MONGODB_DB_NAME 不能为空",
        )
    return get_mongo_client()[db_name]


def verify_mongodb_connection() -> None:
    """执行 MongoDB 连通性检查。

    Returns:
        None: 校验成功时无返回值。

    Raises:
        ServiceException: MongoDB 不可达或鉴权失败时抛出。
    """
    try:
        get_mongo_client().admin.command("ping")
    except PyMongoError as exc:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message=f"MongoDB 连接校验失败: {exc}",
        ) from exc
