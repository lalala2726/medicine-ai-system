"""手工新增切片消息模型单元测试。"""

from __future__ import annotations

from datetime import datetime, timezone

import pytest
from pydantic import ValidationError

from app.core.mq.models.chunk_add_msgs import (
    KnowledgeChunkAddCommandMessage,
    KnowledgeChunkAddResultMessage,
)
from app.core.mq.models.stages import DocumentChunkResultStage

_NOW = datetime.now(timezone.utc)

_VALID_COMMAND_DATA = dict(
    task_uuid="uuid-a-001",
    chunk_id=42,
    knowledge_name="TestKb",
    document_id=5,
    content="一段切片文本",
    embedding_model="text-embedding-v2",
    created_at=_NOW.isoformat(),
)

_VALID_RESULT_KWARGS = dict(
    task_uuid="uuid-a-001",
    chunk_id=42,
    stage=DocumentChunkResultStage.COMPLETED,
    message="切片新增完成",
    knowledge_name="TestKb",
    document_id=5,
    embedding_model="text-embedding-v2",
)


class TestChunkAddCommandMessage:
    """KnowledgeChunkAddCommandMessage 测试。"""

    def test_valid_payload_creates_model(self):
        """测试目的：提供完整有效字段时应成功创建模型实例。
        预期结果：message_type 为 knowledge_chunk_add_command，chunk_id 正确。
        """
        msg = KnowledgeChunkAddCommandMessage(**_VALID_COMMAND_DATA)
        assert msg.message_type == "knowledge_chunk_add_command"
        assert msg.chunk_id == 42

    def test_content_strip(self):
        """测试目的：content 前后有空白时应被 strip。
        预期结果：msg.content 不含首尾空白。
        """
        data = {**_VALID_COMMAND_DATA, "content": "\n  有效文本\t "}
        msg = KnowledgeChunkAddCommandMessage(**data)
        assert msg.content == "有效文本"

    def test_whitespace_only_content_raises_error(self):
        """测试目的：content 仅含空白字符时应校验失败。
        预期结果：抛出 ValidationError。
        """
        data = {**_VALID_COMMAND_DATA, "content": "\t\n "}
        with pytest.raises(ValidationError):
            KnowledgeChunkAddCommandMessage(**data)

    def test_zero_chunk_id_raises_error(self):
        """测试目的：chunk_id <= 0 时应校验失败。
        预期结果：抛出 ValidationError。
        """
        data = {**_VALID_COMMAND_DATA, "chunk_id": 0}
        with pytest.raises(ValidationError):
            KnowledgeChunkAddCommandMessage(**data)

    def test_invalid_knowledge_name_with_hyphen_raises_error(self):
        """测试目的：knowledge_name 含连字符时应校验失败（仅允许下划线）。
        预期结果：抛出 ValidationError。
        """
        data = {**_VALID_COMMAND_DATA, "knowledge_name": "test-kb"}
        with pytest.raises(ValidationError):
            KnowledgeChunkAddCommandMessage(**data)

    def test_invalid_knowledge_name_with_chinese_raises_error(self):
        """测试目的：knowledge_name 含中文字符时应校验失败。
        预期结果：抛出 ValidationError。
        """
        data = {**_VALID_COMMAND_DATA, "knowledge_name": "测试知识库"}
        with pytest.raises(ValidationError):
            KnowledgeChunkAddCommandMessage(**data)


class TestChunkAddResultMessage:
    """KnowledgeChunkAddResultMessage 测试。"""

    def test_build_creates_valid_instance(self):
        """测试目的：build 工厂方法应创建有效实例。
        预期结果：message_type 正确，chunk_id 回传正确。
        """
        msg = KnowledgeChunkAddResultMessage.build(**_VALID_RESULT_KWARGS)
        assert msg.message_type == "knowledge_chunk_add_result"
        assert msg.chunk_id == 42

    def test_build_with_optional_vector_fields(self):
        """测试目的：传入 vector_id 和 chunk_index 时应保留在实例中。
        预期结果：msg.vector_id 和 msg.chunk_index 与传入值一致。
        """
        msg = KnowledgeChunkAddResultMessage.build(
            **_VALID_RESULT_KWARGS, vector_id=999, chunk_index=3,
        )
        assert msg.vector_id == 999
        assert msg.chunk_index == 3

    def test_build_defaults_vector_fields_to_none(self):
        """测试目的：不传 vector_id / chunk_index 时应默认为 None。
        预期结果：msg.vector_id 和 msg.chunk_index 均为 None。
        """
        msg = KnowledgeChunkAddResultMessage.build(**_VALID_RESULT_KWARGS)
        assert msg.vector_id is None
        assert msg.chunk_index is None

    def test_build_calculates_duration(self):
        """测试目的：传入 started_at 和 occurred_at 时应正确计算 duration_ms。
        预期结果：duration_ms == 1500（1.5 秒）。
        """
        started = datetime(2025, 1, 1, 0, 0, 0, tzinfo=timezone.utc)
        occurred = datetime(2025, 1, 1, 0, 0, 1, 500_000, tzinfo=timezone.utc)
        msg = KnowledgeChunkAddResultMessage.build(
            **_VALID_RESULT_KWARGS, started_at=started, occurred_at=occurred,
        )
        assert msg.duration_ms == 1500

    def test_build_negative_embedding_dim_clamped(self):
        """测试目的：传入负数 embedding_dim 时应被截断为 0。
        预期结果：msg.embedding_dim == 0。
        """
        msg = KnowledgeChunkAddResultMessage.build(
            **_VALID_RESULT_KWARGS, embedding_dim=-5,
        )
        assert msg.embedding_dim == 0
