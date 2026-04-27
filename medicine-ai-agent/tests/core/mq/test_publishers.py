"""publishers 模块单元测试。"""

from __future__ import annotations

import asyncio
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

_MODULE = "app.core.mq.publishers"


@pytest.fixture()
def mock_broker():
    """构造 broker mock，publish 默认成功。"""
    broker = MagicMock()
    broker.publish = AsyncMock()
    return broker


# ---- _publish 内部函数 -------------------------------------------------------


class TestPublishInternal:
    """_publish 内部通用发布函数测试。"""

    def test_returns_true_on_success(self, mock_broker):
        """测试目的：publish 正常完成时应返回 True。
        预期结果：返回 True。
        """
        with patch(f"{_MODULE}.get_broker", return_value=mock_broker):
            from app.core.mq.publishers import _publish
            result = asyncio.run(_publish(MagicMock(), MagicMock(), "rk", "label"))
            assert result is True

    def test_returns_false_on_exception(self, mock_broker):
        """测试目的：publish 抛出异常时应捕获并返回 False。
        预期结果：返回 False，不抛出异常。
        """
        mock_broker.publish = AsyncMock(side_effect=RuntimeError("connection lost"))
        with patch(f"{_MODULE}.get_broker", return_value=mock_broker):
            from app.core.mq.publishers import _publish
            result = asyncio.run(_publish(MagicMock(), MagicMock(), "rk", "label"))
            assert result is False

    def test_calls_broker_publish_with_correct_args(self, mock_broker):
        """测试目的：应使用正确的 exchange 和 routing_key 调用 broker.publish。
        预期结果：broker.publish 被调用一次，参数匹配。
        """
        msg = MagicMock()
        exchange = MagicMock()
        with patch(f"{_MODULE}.get_broker", return_value=mock_broker):
            from app.core.mq.publishers import _publish
            asyncio.run(_publish(msg, exchange, "test.routing.key", "test_label"))
            mock_broker.publish.assert_awaited_once_with(
                msg, exchange=exchange, routing_key="test.routing.key",
            )


# ---- publish_import_result ---------------------------------------------------


class TestPublishImportResult:
    """publish_import_result 函数测试。"""

    def test_returns_true_on_success(self, mock_broker):
        """测试目的：正常发布时应返回 True。
        预期结果：返回 True。
        """
        with patch(f"{_MODULE}.get_broker", return_value=mock_broker):
            from app.core.mq.publishers import publish_import_result
            result = asyncio.run(publish_import_result(MagicMock()))
            assert result is True

    def test_uses_import_exchange(self, mock_broker):
        """测试目的：应使用 import_exchange 和 IMPORT_RESULT_ROUTING_KEY。
        预期结果：broker.publish 调用参数包含正确的 exchange 和 routing_key。
        """
        with patch(f"{_MODULE}.get_broker", return_value=mock_broker):
            from app.core.mq.publishers import publish_import_result
            from app.core.mq.topology import IMPORT_RESULT_ROUTING_KEY, import_exchange
            msg = MagicMock()
            asyncio.run(publish_import_result(msg))
            mock_broker.publish.assert_awaited_once_with(
                msg, exchange=import_exchange, routing_key=IMPORT_RESULT_ROUTING_KEY,
            )


# ---- publish_chunk_rebuild_result --------------------------------------------


class TestPublishChunkRebuildResult:
    """publish_chunk_rebuild_result 函数测试。"""

    def test_returns_true_on_success(self, mock_broker):
        """测试目的：正常发布时应返回 True。
        预期结果：返回 True。
        """
        with patch(f"{_MODULE}.get_broker", return_value=mock_broker):
            from app.core.mq.publishers import publish_chunk_rebuild_result
            result = asyncio.run(publish_chunk_rebuild_result(MagicMock()))
            assert result is True


# ---- publish_chunk_add_result ------------------------------------------------


class TestPublishChunkAddResult:
    """publish_chunk_add_result 函数测试。"""

    def test_returns_true_on_success(self, mock_broker):
        """测试目的：正常发布时应返回 True。
        预期结果：返回 True。
        """
        with patch(f"{_MODULE}.get_broker", return_value=mock_broker):
            from app.core.mq.publishers import publish_chunk_add_result
            result = asyncio.run(publish_chunk_add_result(MagicMock()))
            assert result is True

    def test_uses_chunk_add_exchange(self, mock_broker):
        """测试目的：应使用 chunk_add_exchange 和 CHUNK_ADD_RESULT_ROUTING_KEY。
        预期结果：broker.publish 调用参数包含正确的 exchange 和 routing_key。
        """
        with patch(f"{_MODULE}.get_broker", return_value=mock_broker):
            from app.core.mq.publishers import publish_chunk_add_result
            from app.core.mq.topology import CHUNK_ADD_RESULT_ROUTING_KEY, chunk_add_exchange
            msg = MagicMock()
            asyncio.run(publish_chunk_add_result(msg))
            mock_broker.publish.assert_awaited_once_with(
                msg, exchange=chunk_add_exchange, routing_key=CHUNK_ADD_RESULT_ROUTING_KEY,
            )


# ---- publish_import_commands (批量) ------------------------------------------


class TestPublishImportCommands:
    """publish_import_commands 批量发布函数测试。"""

    def test_publishes_each_message(self, mock_broker):
        """测试目的：应逐条发布列表中的每条消息。
        预期结果：broker.publish 被调用 3 次。
        """
        messages = [MagicMock() for _ in range(3)]
        with patch(f"{_MODULE}.get_broker", return_value=mock_broker):
            from app.core.mq.publishers import publish_import_commands
            asyncio.run(publish_import_commands(messages))
            assert mock_broker.publish.await_count == 3

    def test_single_failure_does_not_block_others(self, mock_broker):
        """测试目的：单条发布失败时不应阻断后续消息。
        预期结果：broker.publish 被调用 3 次（第 2 条失败但第 3 条仍发布）。
        """
        mock_broker.publish = AsyncMock(side_effect=[None, RuntimeError("fail"), None])
        messages = [MagicMock() for _ in range(3)]
        with patch(f"{_MODULE}.get_broker", return_value=mock_broker):
            from app.core.mq.publishers import publish_import_commands
            asyncio.run(publish_import_commands(messages))
            assert mock_broker.publish.await_count == 3
