"""import_handler 消费者单元测试。"""

from __future__ import annotations

import asyncio
from datetime import datetime, timezone
from types import SimpleNamespace
from unittest.mock import AsyncMock, patch

from app.core.mq.models.import_msgs import KnowledgeImportCommandMessage
from app.core.mq.models.stages import ImportResultStage

_HANDLER_MODULE = "app.core.mq.handlers.import_handler"

_NOW = datetime.now(timezone.utc)

_VALID_CMD = KnowledgeImportCommandMessage(
    task_uuid="uuid-import-1",
    biz_key="bk-100",
    version=5,
    knowledge_name="TestKb",
    document_id=1,
    file_url="https://example.com/a.pdf",
    embedding_model="text-embedding-v2",
    created_at=_NOW,
)


def _success_result():
    return SimpleNamespace(
        status="success",
        file_kind="pdf",
        file_size=1572864,
        chunk_count=10,
        vector_count=10,
        embedding_dim=1536,
        error=None,
    )


def _failure_result():
    return SimpleNamespace(
        status="failed",
        file_size=1,
        chunk_count=0,
        vector_count=0,
        embedding_dim=0,
        error="解析失败",
    )


class TestHandleImportCommand:
    """handle_import_command 消费者测试。"""

    def test_stale_message_returns_early(self):
        """测试目的：版本已过期时应跳过处理，不调用业务逻辑。
        预期结果：不发布任何结果消息。
        """
        with (
            patch(f"{_HANDLER_MODULE}.is_import_stale", return_value=True),
            patch(f"{_HANDLER_MODULE}.publish_import_result", new_callable=AsyncMock) as mock_pub,
            patch(f"{_HANDLER_MODULE}.mq_log"),
        ):
            from app.core.mq.handlers.import_handler import handle_import_command
            asyncio.run(handle_import_command(_VALID_CMD))
            mock_pub.assert_not_awaited()

    def test_success_flow_publishes_three_results(self):
        """测试目的：业务成功时应依次发布 STARTED → PROCESSING → COMPLETED 三条结果。
        预期结果：publish_import_result 被调用 3 次。
        """
        with (
            patch(f"{_HANDLER_MODULE}.is_import_stale", return_value=False),
            patch(f"{_HANDLER_MODULE}.publish_import_result", new_callable=AsyncMock, return_value=True) as mock_pub,
            patch(f"{_HANDLER_MODULE}.mq_log"),
            patch(f"{_HANDLER_MODULE}.asyncio") as mock_asyncio,
        ):
            mock_asyncio.to_thread = AsyncMock(return_value=_success_result())
            from app.core.mq.handlers.import_handler import handle_import_command
            asyncio.run(handle_import_command(_VALID_CMD))
            assert mock_pub.await_count == 3

    def test_success_flow_last_stage_is_completed(self):
        """测试目的：业务成功时最后一次发布的 stage 应为 COMPLETED。
        预期结果：最后一条消息 stage == COMPLETED。
        """
        with (
            patch(f"{_HANDLER_MODULE}.is_import_stale", return_value=False),
            patch(f"{_HANDLER_MODULE}.publish_import_result", new_callable=AsyncMock, return_value=True) as mock_pub,
            patch(f"{_HANDLER_MODULE}.mq_log"),
            patch(f"{_HANDLER_MODULE}.asyncio") as mock_asyncio,
        ):
            mock_asyncio.to_thread = AsyncMock(return_value=_success_result())
            from app.core.mq.handlers.import_handler import handle_import_command
            asyncio.run(handle_import_command(_VALID_CMD))
            last_msg = mock_pub.call_args_list[-1][0][0]
            assert last_msg.stage == ImportResultStage.COMPLETED
            assert last_msg.file_type == "pdf"
            assert last_msg.file_size == 1572864

    def test_business_exception_publishes_failed(self):
        """测试目的：业务逻辑抛出异常时应发布 FAILED 结果。
        预期结果：发布的消息中包含 FAILED stage。
        """
        with (
            patch(f"{_HANDLER_MODULE}.is_import_stale", return_value=False),
            patch(f"{_HANDLER_MODULE}.publish_import_result", new_callable=AsyncMock, return_value=True) as mock_pub,
            patch(f"{_HANDLER_MODULE}.mq_log"),
            patch(f"{_HANDLER_MODULE}.asyncio") as mock_asyncio,
        ):
            mock_asyncio.to_thread = AsyncMock(side_effect=RuntimeError("boom"))
            from app.core.mq.handlers.import_handler import handle_import_command
            asyncio.run(handle_import_command(_VALID_CMD))
            # STARTED + FAILED = 2 calls
            assert mock_pub.await_count == 2
            last_msg = mock_pub.call_args_list[-1][0][0]
            assert last_msg.stage == ImportResultStage.FAILED

    def test_business_failure_result_publishes_failed(self):
        """测试目的：业务返回失败状态时应发布 FAILED 结果。
        预期结果：最后一条消息 stage == FAILED。
        """
        with (
            patch(f"{_HANDLER_MODULE}.is_import_stale", return_value=False),
            patch(f"{_HANDLER_MODULE}.publish_import_result", new_callable=AsyncMock, return_value=True) as mock_pub,
            patch(f"{_HANDLER_MODULE}.mq_log"),
            patch(f"{_HANDLER_MODULE}.asyncio") as mock_asyncio,
        ):
            mock_asyncio.to_thread = AsyncMock(return_value=_failure_result())
            from app.core.mq.handlers.import_handler import handle_import_command
            asyncio.run(handle_import_command(_VALID_CMD))
            last_msg = mock_pub.call_args_list[-1][0][0]
            assert last_msg.stage == ImportResultStage.FAILED
            assert last_msg.file_size == 1
