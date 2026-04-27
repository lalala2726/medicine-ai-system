"""MQ 统一结构化日志。

每条 MQ 链路日志均通过 :func:`mq_log` 输出，格式示例::

    [import] [task_uuid=abc123] [download_done] filename=a.pdf size=10240

日志级别由阶段自动决定：

- **error**: 失败 / 发布失败类阶段
- **warning**: 无效 / 过期 / 重连类阶段
- **info**: 其他正常阶段
"""

from __future__ import annotations

from enum import Enum

from loguru import logger


# 阶段枚举

class ImportStage(str, Enum):
    TASK_RECEIVED = "task_received"
    TASK_INVALID = "task_invalid"
    TASK_STALE_DROPPED = "task_stale_dropped"
    DOWNLOAD_START = "download_start"
    DOWNLOAD_DONE = "download_done"
    PARSE_DONE = "parse_done"
    CHUNK_DONE = "chunk_done"
    EMBED_BATCH = "embed_batch"
    EMBED_DONE = "embed_done"
    INSERT_DONE = "insert_done"
    COMPLETED = "completed"
    FAILED = "failed"
    RESULT_PUBLISHED = "result_published"
    RESULT_PUBLISH_FAILED = "result_publish_failed"
    CONSUMER_CONNECTED = "consumer_connected"
    CONSUMER_RECONNECTING = "consumer_reconnecting"


class ChunkRebuildStage(str, Enum):
    """切片重建链路日志阶段。"""

    TASK_RECEIVED = "task_received"  # 收到命令消息
    TASK_INVALID = "task_invalid"  # 消息校验失败
    TASK_STALE_DROPPED = "task_stale_dropped"  # 版本已过期，丢弃
    REBUILD_START = "rebuild_start"  # 开始重建
    REBUILD_STALE = "rebuild_stale"  # 重建过程中版本已过期
    REBUILD_FAILED = "rebuild_failed"  # 重建业务异常
    COMPLETED = "completed"  # 重建成功
    FAILED = "failed"  # 重建失败（通用）
    RESULT_PUBLISHED = "result_published"  # 结果消息已发布
    RESULT_PUBLISH_FAILED = "result_publish_failed"  # 结果消息发布失败
    CONSUMER_CONNECTED = "consumer_connected"  # 消费者已连接
    CONSUMER_RECONNECTING = "consumer_reconnecting"  # 消费者重连中


class ChunkAddStage(str, Enum):
    """手工新增切片链路日志阶段。"""

    TASK_RECEIVED = "task_received"  # 收到命令消息
    TASK_INVALID = "task_invalid"  # 消息校验失败
    ADD_START = "add_start"  # 开始新增
    ADD_FAILED = "add_failed"  # 新增业务异常
    COMPLETED = "completed"  # 新增成功
    FAILED = "failed"  # 新增失败（通用）
    RESULT_PUBLISHED = "result_published"  # 结果消息已发布
    RESULT_PUBLISH_FAILED = "result_publish_failed"  # 结果消息发布失败
    CONSUMER_CONNECTED = "consumer_connected"  # 消费者已连接
    CONSUMER_RECONNECTING = "consumer_reconnecting"  # 消费者重连中


# ---- 日志级别映射 -----------------------------------------------------------

# 触发 ERROR 级别的阶段集合
_ERROR_STAGES = frozenset({
    ImportStage.FAILED, ImportStage.RESULT_PUBLISH_FAILED,
    ChunkRebuildStage.FAILED, ChunkRebuildStage.REBUILD_FAILED,
    ChunkRebuildStage.RESULT_PUBLISH_FAILED,
    ChunkAddStage.FAILED, ChunkAddStage.ADD_FAILED,
    ChunkAddStage.RESULT_PUBLISH_FAILED,
})

# 触发 WARNING 级别的阶段集合
_WARNING_STAGES = frozenset({
    ImportStage.TASK_INVALID, ImportStage.TASK_STALE_DROPPED,
    ImportStage.CONSUMER_RECONNECTING,
    ChunkRebuildStage.TASK_INVALID, ChunkRebuildStage.TASK_STALE_DROPPED,
    ChunkRebuildStage.REBUILD_STALE, ChunkRebuildStage.CONSUMER_RECONNECTING,
    ChunkAddStage.TASK_INVALID, ChunkAddStage.CONSUMER_RECONNECTING,
})


# ---- 日志输出 ----------------------------------------------------------------


def mq_log(
        pipeline: str,
        stage: ImportStage | ChunkRebuildStage | ChunkAddStage,
        task_uuid: str = "-",
        /,
        **metrics: object,
) -> None:
    """输出 MQ 结构化日志。

    Args:
        pipeline: 链路名称，如 ``"import"``、``"chunk_rebuild"``、``"chunk_add"``。
        stage: 当前阶段枚举值，决定日志级别。
        task_uuid: 任务唯一 ID，缺省为 ``"-"``。
        **metrics: 任意 KV 指标，会以 ``key=value`` 格式追加到日志末尾。
    """
    extra = " ".join(f"{k}={v}" for k, v in metrics.items()) if metrics else ""
    line = f"[{pipeline}] [task_uuid={task_uuid}] [{stage.value}] {extra}".rstrip()

    if stage in _ERROR_STAGES:
        logger.error(line)
    elif stage in _WARNING_STAGES:
        logger.warning(line)
    else:
        logger.info(line)
