"""chunk_rebuild_handler 消费者单元测试。"""

from __future__ import annotations

import asyncio
from types import SimpleNamespace
from unittest.mock import AsyncMock, patch

import pytest

from app.core.mq.models.chunk_rebuild_msgs import KnowledgeChunkRebuildCommandMessage
from app.core.mq.models.stages import DocumentChunkResultStage

_HANDLER_MODULE = "app.core.mq.handlers.chunk_rebuild_handler"

_VALID_CMD = KnowledgeChunkRebuildCommandMessage(
    task_uuid="uuid-rb-1",
    knowledge_name="TestKb",
    document_id=1,
    vector_id=100,
    version=5,
    content="重建内容",
    embedding_model="text-embedding-v2",
    created_at="2025-01-01T00:00:00Z",
)


def _success_result():
    return SimpleNamespace(vector_id=888, embedding_dim=1536)


class TestHandleChunkRebuildCommand:
    """handle_chunk_rebuild_command 消费者测试。"""

    def test_stale_message_publishes_failed_and_returns(self):
        """测试目的：版本已过期时应发布 FAILED 并立即返回。
        预期结果：publish_chunk_rebuild_result 被调用 1 次，stage == FAILED。
        """
        with (
            patch(f"{_HANDLER_MODULE}.get_chunk_rebuild_latest_version", return_value=10),
            patch(f"{_HANDLER_MODULE}.publish_chunk_rebuild_result", new_callable=AsyncMock,
                  return_value=True) as mock_pub,
            patch(f"{_HANDLER_MODULE}.mq_log"),
        ):
            from app.core.mq.handlers.chunk_rebuild_handler import handle_chunk_rebuild_command
            asyncio.run(handle_chunk_rebuild_command(_VALID_CMD))
            assert mock_pub.await_count == 1
            msg = mock_pub.call_args_list[0][0][0]
            assert msg.stage == DocumentChunkResultStage.FAILED

    def test_success_flow_publishes_started_and_completed(self):
        """测试目的：业务成功时应发布 STARTED 和 COMPLETED 两条结果。
        预期结果：publish 被调用 2 次。
        """
        with (
            patch(f"{_HANDLER_MODULE}.get_chunk_rebuild_latest_version", return_value=None),
            patch(f"{_HANDLER_MODULE}.publish_chunk_rebuild_result", new_callable=AsyncMock,
                  return_value=True) as mock_pub,
            patch(f"{_HANDLER_MODULE}.mq_log"),
            patch(f"{_HANDLER_MODULE}.asyncio") as mock_asyncio,
        ):
            mock_asyncio.to_thread = AsyncMock(return_value=_success_result())
            from app.core.mq.handlers.chunk_rebuild_handler import handle_chunk_rebuild_command
            asyncio.run(handle_chunk_rebuild_command(_VALID_CMD))
            assert mock_pub.await_count == 2
            stages = [call[0][0].stage for call in mock_pub.call_args_list]
            assert stages == [DocumentChunkResultStage.STARTED, DocumentChunkResultStage.COMPLETED]
            completed_msg = mock_pub.call_args_list[-1][0][0]
            assert completed_msg.vector_id == 888

    def test_business_exception_publishes_failed(self):
        """测试目的：业务抛出普通异常时应发布 FAILED 结果。
        预期结果：最后一条消息 stage == FAILED。
        """
        with (
            patch(f"{_HANDLER_MODULE}.get_chunk_rebuild_latest_version", return_value=None),
            patch(f"{_HANDLER_MODULE}.publish_chunk_rebuild_result", new_callable=AsyncMock,
                  return_value=True) as mock_pub,
            patch(f"{_HANDLER_MODULE}.mq_log"),
            patch(f"{_HANDLER_MODULE}.asyncio") as mock_asyncio,
        ):
            mock_asyncio.to_thread = AsyncMock(side_effect=RuntimeError("rebuild error"))
            from app.core.mq.handlers.chunk_rebuild_handler import handle_chunk_rebuild_command
            asyncio.run(handle_chunk_rebuild_command(_VALID_CMD))
            last_msg = mock_pub.call_args_list[-1][0][0]
            assert last_msg.stage == DocumentChunkResultStage.FAILED

    def test_publish_failure_raises_runtime_error(self):
        """测试目的：结果消息发布失败时应抛出 RuntimeError 触发 NACK。
        预期结果：抛出 RuntimeError。
        """
        with (
            patch(f"{_HANDLER_MODULE}.get_chunk_rebuild_latest_version", return_value=None),
            patch(f"{_HANDLER_MODULE}.publish_chunk_rebuild_result", new_callable=AsyncMock, return_value=False),
            patch(f"{_HANDLER_MODULE}.mq_log"),
        ):
            from app.core.mq.handlers.chunk_rebuild_handler import handle_chunk_rebuild_command
            with pytest.raises(RuntimeError, match="发布失败"):
                asyncio.run(handle_chunk_rebuild_command(_VALID_CMD))

    def test_stale_error_during_rebuild_publishes_failed(self):
        """测试目的：重建过程中抛出异常时应发布 FAILED。
        预期结果：最后一条消息 stage == FAILED。
        """
        with (
            patch(f"{_HANDLER_MODULE}.get_chunk_rebuild_latest_version", return_value=None),
            patch(f"{_HANDLER_MODULE}.publish_chunk_rebuild_result", new_callable=AsyncMock,
                  return_value=True) as mock_pub,
            patch(f"{_HANDLER_MODULE}.mq_log"),
            patch(f"{_HANDLER_MODULE}.asyncio") as mock_asyncio,
        ):
            mock_asyncio.to_thread = AsyncMock(side_effect=ValueError("版本已过期"))
            from app.core.mq.handlers.chunk_rebuild_handler import handle_chunk_rebuild_command
            asyncio.run(handle_chunk_rebuild_command(_VALID_CMD))
            last_msg = mock_pub.call_args_list[-1][0][0]
            assert last_msg.stage == DocumentChunkResultStage.FAILED
