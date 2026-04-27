"""agent_config_refresh_handler 消费者单元测试。"""

from __future__ import annotations

import asyncio
from unittest.mock import AsyncMock, patch

from app.core.config_sync import AgentConfigRefreshResult
from app.core.mq.models.agent_config_refresh import AgentConfigRefreshMessage

_HANDLER_MODULE = "app.core.mq.handlers.agent_config_refresh_handler"

_VALID_MESSAGE = AgentConfigRefreshMessage(
    redis_key="agent:config:all",
    updated_at="2026-03-11T14:30:00+08:00",
    updated_by="admin",
    created_at="2026-03-11T14:30:01+08:00",
)


def test_handle_agent_config_refresh_passes_redis_key() -> None:
    """测试目的：刷新消费者应把 Redis key 透传给快照刷新逻辑；预期结果：调用参数匹配消息体。"""

    mock_refresh_result = AgentConfigRefreshResult(
        applied=False,
        previous_snapshot=None,
        current_snapshot=None,
        speech_changed=False,
    )

    with patch(f"{_HANDLER_MODULE}.refresh_agent_config_snapshot", return_value=mock_refresh_result) as mock_refresh:
        from app.core.mq.handlers.agent_config_refresh_handler import handle_agent_config_refresh

        asyncio.run(handle_agent_config_refresh(_VALID_MESSAGE))

    mock_refresh.assert_called_once_with(
        redis_key="agent:config:all",
    )


def test_handle_agent_config_refresh_skips_runtime_reconnect_when_speech_unchanged() -> None:
    """测试目的：语音配置未变化时不应触发语音重连；预期结果：只刷新快照，不调用语音运行时协调器。"""

    mock_refresh_result = AgentConfigRefreshResult(
        applied=True,
        previous_snapshot=None,
        current_snapshot=None,
        speech_changed=False,
    )
    runtime_handler = AsyncMock()

    with patch(f"{_HANDLER_MODULE}.refresh_agent_config_snapshot", return_value=mock_refresh_result), patch(
            f"{_HANDLER_MODULE}.handle_runtime_speech_config_refresh",
            runtime_handler,
    ):
        from app.core.mq.handlers.agent_config_refresh_handler import handle_agent_config_refresh

        asyncio.run(handle_agent_config_refresh(_VALID_MESSAGE))

    runtime_handler.assert_not_awaited()


def test_handle_agent_config_refresh_reconnects_when_speech_changed() -> None:
    """测试目的：语音配置变化时应中断旧会话并重探活；预期结果：调用语音运行时协调器。"""

    mock_refresh_result = AgentConfigRefreshResult(
        applied=True,
        previous_snapshot=None,
        current_snapshot=None,
        speech_changed=True,
    )
    runtime_handler = AsyncMock()

    with patch(f"{_HANDLER_MODULE}.refresh_agent_config_snapshot", return_value=mock_refresh_result), patch(
            f"{_HANDLER_MODULE}.handle_runtime_speech_config_refresh",
            runtime_handler,
    ):
        from app.core.mq.handlers.agent_config_refresh_handler import handle_agent_config_refresh

        asyncio.run(handle_agent_config_refresh(_VALID_MESSAGE))

    runtime_handler.assert_awaited_once()
