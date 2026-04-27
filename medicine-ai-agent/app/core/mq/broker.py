"""RabbitMQ broker 单例与连接配置。

通过环境变量构造 AMQP URL，使用 FastStream ``RabbitBroker`` 管理连接。

环境变量::

    RABBITMQ_HOST      – 主机地址（必填，未设置则视为 MQ 未启用）
    RABBITMQ_PORT      – 端口，默认 5672
    RABBITMQ_USERNAME  – 用户名，默认 guest
    RABBITMQ_PASSWORD  – 密码，默认 guest
    RABBITMQ_VHOST     – Virtual Host，默认 /
"""

from __future__ import annotations

import os
from functools import lru_cache

from loguru import logger


def is_mq_configured() -> bool:
    """检查是否配置了 RabbitMQ 连接。

    Returns:
        当 ``RABBITMQ_HOST`` 环境变量非空时返回 True。
    """
    return bool(os.getenv("RABBITMQ_HOST"))


def _build_amqp_url() -> str:
    """根据环境变量拼接 AMQP 连接 URL。

    Returns:
        格式为 ``amqp://user:pass@host:port/vhost`` 的连接字符串。
    """
    host = os.getenv("RABBITMQ_HOST", "localhost")
    port = os.getenv("RABBITMQ_PORT", "5672")
    username = os.getenv("RABBITMQ_USERNAME", "guest")
    password = os.getenv("RABBITMQ_PASSWORD", "guest")
    vhost = os.getenv("RABBITMQ_VHOST", "/")
    return f"amqp://{username}:{password}@{host}:{port}/{vhost}"


@lru_cache(maxsize=1)
def get_broker():
    """获取全局 ``RabbitBroker`` 单例。

    首次调用时创建实例并缓存，后续调用直接返回缓存实例。
    日志中仅打印 ``host:port/vhost`` 部分，不暴露凭据。

    Returns:
        ``faststream.rabbit.RabbitBroker`` 实例。
    """
    from faststream.rabbit import RabbitBroker

    url = _build_amqp_url()
    logger.info("[mq] 创建 RabbitBroker，连接: {}", url.split("@")[-1])
    return RabbitBroker(url)
