"""Agent 提示词刷新消费者。"""

from __future__ import annotations

import asyncio

from loguru import logger

from app.core.mq.broker import get_broker
from app.core.mq.models.agent_prompt_refresh import AgentPromptRefreshMessage
from app.core.mq.topology import agent_prompt_refresh_exchange, agent_prompt_refresh_queue
from app.core.prompt_sync import refresh_agent_prompt_snapshot

broker = get_broker()


@broker.subscriber(agent_prompt_refresh_queue, agent_prompt_refresh_exchange)
async def handle_agent_prompt_refresh(msg: AgentPromptRefreshMessage) -> None:
    """消费 Agent 提示词刷新消息并尝试更新本地内存快照。

    Args:
        msg: 反序列化后的提示词刷新消息。

    Returns:
        None: 无返回值。
    """

    logger.info(
        "收到 Agent 提示词刷新通知：消息类型={}，prompt_key={}，版本={}，redis_key={}，更新人={}",
        msg.message_type,
        msg.prompt_key,
        msg.prompt_version,
        msg.redis_key,
        msg.updated_by,
    )
    refresh_result = await asyncio.to_thread(
        refresh_agent_prompt_snapshot,
        prompt_key=msg.prompt_key,
        redis_key=msg.redis_key,
    )
    if not refresh_result.applied:
        return
