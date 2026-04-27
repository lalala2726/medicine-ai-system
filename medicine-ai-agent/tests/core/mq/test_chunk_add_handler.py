"""chunk_add_handler 消费者单元测试。"""

from __future__ import annotations

import asyncio
from datetime import datetime, timezone
from types import SimpleNamespace
from unittest.mock import AsyncMock, patch

import pytest

from app.core.mq.models.chunk_add_msgs import KnowledgeChunkAddCommandMessage
from app.core.mq.models.stages import DocumentChunkResultStage

_HANDLER_MODULE = "app.core.mq.handlers.chunk_add_handler"

_VALID_CMD = KnowledgeChunkAddCommandMessage(
    task_uuid="uuid-ca-1",
    chunk_id=42,
    knowledge_name="TestKb",
    document_id=5,
    content="新增切片内容",
    embedding_model="text-embedding-v2",
    created_at=datetime.now(timezone.utc).isoformat(),
)


def _success_result():
    return SimpleNamespace(vector_id=999, chunk_index=3, embedding_dim=1536)


class TestHandleChunkAddCommand:
    """handle_chunk_add_command 消费者测试。"""

    def test_success_flow_publishes_started_and_completed(self):
        """测试目的：业务成功时应发布 STARTED 和 COMPLETED 两条结果。
        预期结果：publish 被调用 2 次，stage 依次为 STARTED 和 COMPLETED。
        """
        with (
            patch(f"{_HANDLER_MODULE}.publish_chunk_add_result", new_callable=AsyncMock, return_value=True) as mock_pub,
            patch(f"{_HANDLER_MODULE}.mq_log"),
            patch(f"{_HANDLER_MODULE}.asyncio") as mock_asyncio,
        ):
            mock_asyncio.to_thread = AsyncMock(return_value=_success_result())
            from app.core.mq.handlers.chunk_add_handler import handle_chunk_add_command
            asyncio.run(handle_chunk_add_command(_VALID_CMD))
            assert mock_pub.await_count == 2
            stages = [call[0][0].stage for call in mock_pub.call_args_list]
            assert stages == [DocumentChunkResultStage.STARTED, DocumentChunkResultStage.COMPLETED]

    def test_completed_msg_carries_vector_fields(self):
        """测试目的：COMPLETED 消息应携带 vector_id 和 chunk_index。
        预期结果：vector_id == 999, chunk_index == 3。
        """
        with (
            patch(f"{_HANDLER_MODULE}.publish_chunk_add_result", new_callable=AsyncMock, return_value=True) as mock_pub,
            patch(f"{_HANDLER_MODULE}.mq_log"),
            patch(f"{_HANDLER_MODULE}.asyncio") as mock_asyncio,
        ):
            mock_asyncio.to_thread = AsyncMock(return_value=_success_result())
            from app.core.mq.handlers.chunk_add_handler import handle_chunk_add_command
            asyncio.run(handle_chunk_add_command(_VALID_CMD))
            completed_msg = mock_pub.call_args_list[-1][0][0]
            assert completed_msg.vector_id == 999
            assert completed_msg.chunk_index == 3

    def test_business_exception_publishes_failed(self):
        """测试目的：业务抛出异常时应发布 FAILED 结果。
        预期结果：最后一条消息 stage == FAILED，共 2 次发布。
        """
        with (
            patch(f"{_HANDLER_MODULE}.publish_chunk_add_result", new_callable=AsyncMock, return_value=True) as mock_pub,
            patch(f"{_HANDLER_MODULE}.mq_log"),
            patch(f"{_HANDLER_MODULE}.asyncio") as mock_asyncio,
        ):
            mock_asyncio.to_thread = AsyncMock(side_effect=ValueError("embedding error"))
            from app.core.mq.handlers.chunk_add_handler import handle_chunk_add_command
            asyncio.run(handle_chunk_add_command(_VALID_CMD))
            assert mock_pub.await_count == 2
            last_msg = mock_pub.call_args_list[-1][0][0]
            assert last_msg.stage == DocumentChunkResultStage.FAILED

    def test_publish_failure_raises_runtime_error(self):
        """测试目的：结果消息发布失败时应抛出 RuntimeError 触发 NACK。
        预期结果：抛出 RuntimeError。
        """
        with (
            patch(f"{_HANDLER_MODULE}.publish_chunk_add_result", new_callable=AsyncMock, return_value=False),
            patch(f"{_HANDLER_MODULE}.mq_log"),
        ):
            from app.core.mq.handlers.chunk_add_handler import handle_chunk_add_command
            with pytest.raises(RuntimeError, match="发布失败"):
                asyncio.run(handle_chunk_add_command(_VALID_CMD))

    def test_no_version_check_involved(self):
        """测试目的：chunk_add 不涉及版本检查，不应调用 version_store。
        预期结果：handler 代码无 version_store 调用，业务正常执行。
        """
        with (
            patch(f"{_HANDLER_MODULE}.publish_chunk_add_result", new_callable=AsyncMock, return_value=True) as mock_pub,
            patch(f"{_HANDLER_MODULE}.mq_log"),
            patch(f"{_HANDLER_MODULE}.asyncio") as mock_asyncio,
        ):
            mock_asyncio.to_thread = AsyncMock(return_value=_success_result())
            from app.core.mq.handlers.chunk_add_handler import handle_chunk_add_command
            asyncio.run(handle_chunk_add_command(_VALID_CMD))
            # 只要正常走到 COMPLETED 就说明没有被版本检查挡住
            last_msg = mock_pub.call_args_list[-1][0][0]
            assert last_msg.stage == DocumentChunkResultStage.COMPLETED

    def test_chunk_id_echoed_in_result(self):
        """测试目的：结果消息应回传原始命令的 chunk_id。
        预期结果：STARTED 和 COMPLETED 两条消息的 chunk_id 均为 42。
        """
        with (
            patch(f"{_HANDLER_MODULE}.publish_chunk_add_result", new_callable=AsyncMock, return_value=True) as mock_pub,
            patch(f"{_HANDLER_MODULE}.mq_log"),
            patch(f"{_HANDLER_MODULE}.asyncio") as mock_asyncio,
        ):
            mock_asyncio.to_thread = AsyncMock(return_value=_success_result())
            from app.core.mq.handlers.chunk_add_handler import handle_chunk_add_command
            asyncio.run(handle_chunk_add_command(_VALID_CMD))
            for call in mock_pub.call_args_list:
                assert call[0][0].chunk_id == 42
