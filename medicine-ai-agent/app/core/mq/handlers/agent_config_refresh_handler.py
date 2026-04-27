"""Agent 配置刷新消费者。"""

from __future__ import annotations

from loguru import logger

from app.core.config_sync import refresh_agent_config_snapshot
from app.core.mq.broker import get_broker
from app.core.mq.models.agent_config_refresh import AgentConfigRefreshMessage
from app.core.mq.topology import agent_config_refresh_exchange, agent_config_refresh_queue
from app.core.speech.runtime import handle_speech_config_refresh as handle_runtime_speech_config_refresh

broker = get_broker()


@broker.subscriber(agent_config_refresh_queue, agent_config_refresh_exchange)
async def handle_agent_config_refresh(msg: AgentConfigRefreshMessage) -> None:
    """消费 Agent 配置刷新消息并尝试更新本地内存快照。

    Args:
        msg: 反序列化后的 Agent 配置刷新消息。
    """

    logger.info(
        "收到 Agent 配置刷新通知：消息类型={}，配置版本={}，redis_key={}，更新人={}",
        msg.message_type,
        msg.config_version,
        msg.redis_key,
        msg.updated_by,
    )
    refresh_result = refresh_agent_config_snapshot(
        redis_key=msg.redis_key,
    )
    if not refresh_result.applied:
        return
    if not refresh_result.speech_changed:
        logger.info("本次 Agent 配置刷新未检测到语音配置变化，跳过语音重连。")
        return
    await handle_runtime_speech_config_refresh()
