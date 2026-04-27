"""切片重建命令消费 handler。

ACK 策略：``REJECT_ON_ERROR``，结果消息投递成功才正常返回（ACK），
投递失败则 raise 触发 NACK 重投。
"""

from __future__ import annotations

import asyncio
from datetime import datetime, timezone

from faststream import AckPolicy

from app.core.mq.broker import get_broker
from app.core.mq.log import ChunkRebuildStage, mq_log
from app.core.mq.models.chunk_rebuild_msgs import (
    KnowledgeChunkRebuildCommandMessage,
    KnowledgeChunkRebuildResultMessage,
)
from app.core.mq.models.stages import DocumentChunkResultStage
from app.core.mq.publishers import publish_chunk_rebuild_result
from app.core.mq.topology import chunk_rebuild_command_queue, chunk_rebuild_exchange
from app.core.mq.version_store import get_chunk_rebuild_latest_version

broker = get_broker()


@broker.subscriber(
    chunk_rebuild_command_queue,
    chunk_rebuild_exchange,
    ack_policy=AckPolicy.REJECT_ON_ERROR,
)
async def handle_chunk_rebuild_command(msg: KnowledgeChunkRebuildCommandMessage) -> None:
    """消费切片重建命令消息。

    流程：版本检查 → STARTED → rebuild_document_chunk → COMPLETED / FAILED。
    具备 ``ChunkRebuildMessageStaleError`` 专属处理，标记为版本过期。

    Args:
        msg: 切片重建命令消息。
    """
    task_uuid = msg.task_uuid
    started_at = datetime.now(timezone.utc)

    mq_log("chunk_rebuild", ChunkRebuildStage.TASK_RECEIVED, task_uuid,
           vector_id=msg.vector_id, version=msg.version)

    common = dict(
        task_uuid=task_uuid,
        version=msg.version,
        knowledge_name=msg.knowledge_name,
        document_id=msg.document_id,
        vector_id=msg.vector_id,
        embedding_model=msg.embedding_model,
        started_at=started_at,
    )

    # 消费前版本检查 ──────────────────────────────────────
    latest = get_chunk_rebuild_latest_version(vector_id=msg.vector_id)
    if latest is not None and msg.version < latest:
        mq_log("chunk_rebuild", ChunkRebuildStage.TASK_STALE_DROPPED, task_uuid,
               vector_id=msg.vector_id, version=msg.version, latest=latest)
        await _publish_or_raise(task_uuid, KnowledgeChunkRebuildResultMessage.build(
            **common,
            stage=DocumentChunkResultStage.FAILED,
            message=f"版本已过期 (version={msg.version}, latest={latest})",
        ))
        return

    # 发送 STARTED
    await _publish_or_raise(task_uuid, KnowledgeChunkRebuildResultMessage.build(
        **common, stage=DocumentChunkResultStage.STARTED, message="任务已接收",
    ))

    # 执行业务逻辑
    try:
        from app.services.document_chunk_service import (
            ChunkRebuildMessageStaleError,
            rebuild_document_chunk,
        )

        result = await asyncio.to_thread(
            rebuild_document_chunk,
            knowledge_name=msg.knowledge_name,
            document_id=msg.document_id,
            vector_id=msg.vector_id,
            version=msg.version,
            content=msg.content,
            embedding_model=msg.embedding_model,
        )
    except ChunkRebuildMessageStaleError as exc:
        mq_log("chunk_rebuild", ChunkRebuildStage.REBUILD_STALE, task_uuid,
               vector_id=msg.vector_id, version=msg.version)
        await _publish_or_raise(task_uuid, KnowledgeChunkRebuildResultMessage.build(
            **common,
            stage=DocumentChunkResultStage.FAILED,
            message=str(exc),
        ))
        return
    except Exception as exc:
        mq_log("chunk_rebuild", ChunkRebuildStage.REBUILD_FAILED, task_uuid, error=str(exc))
        await _publish_or_raise(task_uuid, KnowledgeChunkRebuildResultMessage.build(
            **common,
            stage=DocumentChunkResultStage.FAILED,
            message=str(exc),
        ))
        return

    # 发送 COMPLETED
    mq_log("chunk_rebuild", ChunkRebuildStage.COMPLETED, task_uuid)
    await _publish_or_raise(task_uuid, KnowledgeChunkRebuildResultMessage.build(
        **{**common, "vector_id": result.vector_id},
        stage=DocumentChunkResultStage.COMPLETED,
        message="切片重建完成",
        embedding_dim=result.embedding_dim,
    ))


async def _publish_or_raise(task_uuid: str, msg: KnowledgeChunkRebuildResultMessage) -> None:
    """发布结果消息，投递失败则 raise 触发 NACK 重投。

    Args:
        task_uuid: 任务 ID，用于日志关联。
        msg: 待发布的结果消息。

    Raises:
        RuntimeError: 消息发布失败时抛出，触发 MQ NACK。
    """
    ok = await publish_chunk_rebuild_result(msg)
    stage = ChunkRebuildStage.RESULT_PUBLISHED if ok else ChunkRebuildStage.RESULT_PUBLISH_FAILED
    mq_log("chunk_rebuild", stage, task_uuid, result_stage=msg.stage.value)
    if not ok:
        raise RuntimeError(f"切片重建结果消息发布失败: task_uuid={task_uuid}")
