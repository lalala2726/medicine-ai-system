"""消息队列（MQ）模块聚合导出。

基于 FastStream[rabbit] 实现，提供 RabbitMQ 连接、拓扑、消费、发布等功能。
"""

from app.core.mq.broker import get_broker, is_mq_configured
from app.core.mq.publishers import (
    publish_chunk_add_result,
    publish_chunk_rebuild_result,
    publish_import_commands,
    publish_import_result,
)

__all__ = [
    "get_broker",
    "is_mq_configured",
    "publish_chunk_add_result",
    "publish_chunk_rebuild_result",
    "publish_import_commands",
    "publish_import_result",
]
