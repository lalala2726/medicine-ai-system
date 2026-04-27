"""MQ 结果消息发布函数。

所有 ``publish_*`` 函数返回 ``bool`` 表示是否投递成功，
内部捕获异常并记录日志，不向调用方抛出。
"""

from __future__ import annotations

from loguru import logger
from pydantic import BaseModel

from app.core.mq.broker import get_broker
from app.core.mq.topology import (
    CHUNK_ADD_RESULT_ROUTING_KEY,
    CHUNK_REBUILD_RESULT_ROUTING_KEY,
    IMPORT_RESULT_ROUTING_KEY,
    chunk_add_exchange,
    chunk_rebuild_exchange,
    import_command_queue,
    import_exchange,
)


async def _publish(msg: BaseModel, exchange, routing_key: str, label: str) -> bool:
    """内部通用发布函数。

    Args:
        msg: 待发布的 Pydantic 消息体，FastStream 自动序列化为 JSON。
        exchange: 目标交换机。
        routing_key: 路由键。
        label: 日志标签，用于异常日志定位。

    Returns:
        投递成功返回 True，失败返回 False。
    """
    try:
        await get_broker().publish(
            msg,
            exchange=exchange,
            routing_key=routing_key,
        )
        return True
    except Exception:
        logger.exception("[mq] {} 消息发布失败", label)
        return False


# ---- 导入链路 ----------------------------------------------------------------


async def publish_import_result(msg) -> bool:
    """发布导入结果消息（AI → 业务）。

    Args:
        msg: ``KnowledgeImportResultMessage`` 实例。

    Returns:
        投递成功返回 True。
    """
    return await _publish(msg, import_exchange, IMPORT_RESULT_ROUTING_KEY, "import_result")


async def publish_import_commands(messages: list) -> None:
    """批量发布导入命令消息（业务 → AI）。

    逐条发布，单条失败不影响其他消息。

    Args:
        messages: ``KnowledgeImportCommandMessage`` 列表。
    """
    broker = get_broker()
    for m in messages:
        try:
            await broker.publish(
                m,
                exchange=import_exchange,
                queue=import_command_queue,
                routing_key="knowledge.import.command",
            )
        except Exception:
            logger.exception("[mq] import_command 消息发布失败, task_uuid={}", getattr(m, "task_uuid", "?"))


# ---- 切片重建链路 ------------------------------------------------------------


async def publish_chunk_rebuild_result(msg) -> bool:
    """发布切片重建结果消息（AI → 业务）。

    Args:
        msg: ``KnowledgeChunkRebuildResultMessage`` 实例。

    Returns:
        投递成功返回 True。
    """
    return await _publish(msg, chunk_rebuild_exchange, CHUNK_REBUILD_RESULT_ROUTING_KEY, "chunk_rebuild_result")


# ---- 手工新增切片链路 --------------------------------------------------------


async def publish_chunk_add_result(msg) -> bool:
    """发布手工新增切片结果消息（AI → 业务）。

    Args:
        msg: ``KnowledgeChunkAddResultMessage`` 实例。

    Returns:
        投递成功返回 True。
    """
    return await _publish(msg, chunk_add_exchange, CHUNK_ADD_RESULT_ROUTING_KEY, "chunk_add_result")
