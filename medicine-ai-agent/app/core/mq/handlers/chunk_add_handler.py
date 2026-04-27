"""手工新增切片命令消费 handler。

ACK 策略：``REJECT_ON_ERROR``，结果消息投递成功才正常返回（ACK），
投递失败则 raise 触发 NACK 重投。
"""

from __future__ import annotations

import asyncio
from datetime import datetime, timezone

from faststream import AckPolicy

from app.core.mq.broker import get_broker
from app.core.mq.log import ChunkAddStage, mq_log
from app.core.mq.models.chunk_add_msgs import (
    KnowledgeChunkAddCommandMessage,
    KnowledgeChunkAddResultMessage,
)
from app.core.mq.models.stages import DocumentChunkResultStage
from app.core.mq.publishers import publish_chunk_add_result
from app.core.mq.topology import chunk_add_command_queue, chunk_add_exchange

broker = get_broker()


@broker.subscriber(
    chunk_add_command_queue,
    chunk_add_exchange,
    ack_policy=AckPolicy.REJECT_ON_ERROR,
)
async def handle_chunk_add_command(msg: KnowledgeChunkAddCommandMessage) -> None:
    """消费手工新增切片命令消息。

    流程：STARTED → add_document_chunk → COMPLETED / FAILED。
    无版本检查，新增不涉及覆盖。

    Args:
        msg: 手工新增切片命令消息。
    """
    task_uuid = msg.task_uuid
    started_at = datetime.now(timezone.utc)

    mq_log("chunk_add", ChunkAddStage.TASK_RECEIVED, task_uuid,
           chunk_id=msg.chunk_id, knowledge_name=msg.knowledge_name)

    common = dict(
        task_uuid=task_uuid,
        chunk_id=msg.chunk_id,
        knowledge_name=msg.knowledge_name,
        document_id=msg.document_id,
        embedding_model=msg.embedding_model,
        started_at=started_at,
    )

    # 发送 STARTED
    await _publish_or_raise(task_uuid, KnowledgeChunkAddResultMessage.build(
        **common, stage=DocumentChunkResultStage.STARTED, message="任务已接收",
    ))

    # 执行业务逻辑
    try:
        from app.services.document_chunk_service import add_document_chunk

        result = await asyncio.to_thread(
            add_document_chunk,
            knowledge_name=msg.knowledge_name,
            document_id=msg.document_id,
            content=msg.content,
            embedding_model=msg.embedding_model,
        )
    except Exception as exc:
        mq_log("chunk_add", ChunkAddStage.ADD_FAILED, task_uuid, error=str(exc))
        await _publish_or_raise(task_uuid, KnowledgeChunkAddResultMessage.build(
            **common,
            stage=DocumentChunkResultStage.FAILED,
            message=str(exc),
        ))
        return

    # 发送 COMPLETED
    mq_log("chunk_add", ChunkAddStage.COMPLETED, task_uuid,
           vector_id=result.vector_id, chunk_index=result.chunk_index)
    await _publish_or_raise(task_uuid, KnowledgeChunkAddResultMessage.build(
        **common,
        stage=DocumentChunkResultStage.COMPLETED,
        message="切片新增完成",
        vector_id=result.vector_id,
        chunk_index=result.chunk_index,
        embedding_dim=result.embedding_dim,
    ))


async def _publish_or_raise(task_uuid: str, msg: KnowledgeChunkAddResultMessage) -> None:
    """发布结果消息，投递失败则 raise 触发 NACK 重投。

    Args:
        task_uuid: 任务 ID，用于日志关联。
        msg: 待发布的结果消息。

    Raises:
        RuntimeError: 消息发布失败时抛出，触发 MQ NACK。
    """
    ok = await publish_chunk_add_result(msg)
    stage = ChunkAddStage.RESULT_PUBLISHED if ok else ChunkAddStage.RESULT_PUBLISH_FAILED
    mq_log("chunk_add", stage, task_uuid, result_stage=msg.stage.value)
    if not ok:
        raise RuntimeError(f"切片新增结果消息发布失败: task_uuid={task_uuid}")
