"""导入命令消费 handler。

ACK 策略：始终 ACK（try/except 捕获所有异常后正常返回，不重投）。
处理流程：版本检查 → STARTED → import_single_file → PROCESSING → COMPLETED / FAILED
"""

from __future__ import annotations

import asyncio
from datetime import datetime, timezone

from app.core.mq.broker import get_broker
from app.core.mq.log import ImportStage, mq_log
from app.core.mq.models.import_msgs import (
    KnowledgeImportCommandMessage,
    KnowledgeImportResultMessage,
)
from app.core.mq.models.stages import ImportResultStage
from app.core.mq.publishers import publish_import_result
from app.core.mq.topology import import_command_queue, import_exchange
from app.core.mq.version_store import is_import_stale

broker = get_broker()


@broker.subscriber(import_command_queue, import_exchange)
async def handle_import_command(msg: KnowledgeImportCommandMessage) -> None:
    """消费导入命令消息，执行文件导入流程。

    业务逻辑通过 ``asyncio.to_thread`` 在线程池中执行，避免阻塞事件循环。

    Args:
        msg: 导入命令消息，FastStream 自动反序列化。
    """
    task_uuid = msg.task_uuid
    started_at = datetime.now(timezone.utc)

    mq_log("import", ImportStage.TASK_RECEIVED, task_uuid,
           biz_key=msg.biz_key, version=msg.version, file_url=msg.file_url)

    # 版本检查
    if is_import_stale(biz_key=msg.biz_key, version=msg.version):
        mq_log("import", ImportStage.TASK_STALE_DROPPED, task_uuid,
               biz_key=msg.biz_key, version=msg.version)
        return

    # 公共结果字段
    common = dict(
        task_uuid=task_uuid,
        biz_key=msg.biz_key,
        version=msg.version,
        knowledge_name=msg.knowledge_name,
        document_id=msg.document_id,
        file_url=msg.file_url,
        embedding_model=msg.embedding_model,
        started_at=started_at,
    )

    # 发送 STARTED
    await _publish_result(task_uuid, KnowledgeImportResultMessage.build(
        **common, stage=ImportResultStage.STARTED, message="任务已接收",
    ))

    # 执行业务逻辑
    try:
        from app.services.document_chunk_service import import_single_file

        result = await asyncio.to_thread(
            import_single_file,
            url=msg.file_url,
            knowledge_name=msg.knowledge_name,
            document_id=msg.document_id,
            embedding_model=msg.embedding_model,
            chunk_size=msg.chunk_size,
            chunk_overlap=msg.chunk_overlap,
            task_uuid=task_uuid,
        )
    except Exception as exc:
        mq_log("import", ImportStage.FAILED, task_uuid, error=str(exc))
        await _publish_result(task_uuid, KnowledgeImportResultMessage.build(
            **common, stage=ImportResultStage.FAILED, message=str(exc),
        ))
        return

    # 发送 PROCESSING
    await _publish_result(task_uuid, KnowledgeImportResultMessage.build(
        **common, stage=ImportResultStage.PROCESSING, message="处理中",
    ))

    # 发送最终结果
    if result.status == "success":
        mq_log("import", ImportStage.COMPLETED, task_uuid,
               chunk_count=result.chunk_count, vector_count=result.vector_count)
        await _publish_result(task_uuid, KnowledgeImportResultMessage.build(
            **common,
            stage=ImportResultStage.COMPLETED,
            message="导入完成",
            file_type=result.file_kind,
            file_size=result.file_size,
            chunk_count=result.chunk_count,
            vector_count=result.vector_count,
            embedding_dim=result.embedding_dim,
        ))
    else:
        mq_log("import", ImportStage.FAILED, task_uuid, error=result.error)
        await _publish_result(task_uuid, KnowledgeImportResultMessage.build(
            **common,
            stage=ImportResultStage.FAILED,
            message=result.error,
            file_size=result.file_size,
            embedding_dim=result.embedding_dim,
        ))


async def _publish_result(task_uuid: str, msg: KnowledgeImportResultMessage) -> None:
    """发布导入结果消息并记录发布状态日志。

    Args:
        task_uuid: 任务 ID，用于日志关联。
        msg: 待发布的结果消息。
    """
    ok = await publish_import_result(msg)
    stage = ImportStage.RESULT_PUBLISHED if ok else ImportStage.RESULT_PUBLISH_FAILED
    mq_log("import", stage, task_uuid, result_stage=msg.stage.value)
