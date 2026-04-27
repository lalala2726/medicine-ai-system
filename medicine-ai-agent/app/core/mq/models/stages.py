"""结果事件阶段枚举。

用于结果消息的 ``stage`` 字段，表示当前任务进度。
"""

from __future__ import annotations

from enum import Enum


class ImportResultStage(str, Enum):
    """导入链路结果阶段。"""

    STARTED = "STARTED"  # 任务已接收
    PROCESSING = "PROCESSING"  # 处理中（粗粒度进度）
    COMPLETED = "COMPLETED"  # 导入成功
    FAILED = "FAILED"  # 导入失败


class DocumentChunkResultStage(str, Enum):
    """切片链路（重建、新增）结果阶段。"""

    STARTED = "STARTED"  # 任务已接收
    COMPLETED = "COMPLETED"  # 成功
    FAILED = "FAILED"  # 失败
