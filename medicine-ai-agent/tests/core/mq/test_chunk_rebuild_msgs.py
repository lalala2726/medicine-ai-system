"""切片重建消息模型单元测试。"""

from __future__ import annotations

from datetime import datetime, timezone

import pytest
from pydantic import ValidationError

from app.core.mq.models.chunk_rebuild_msgs import (
    KnowledgeChunkRebuildCommandMessage,
    KnowledgeChunkRebuildResultMessage,
)
from app.core.mq.models.stages import DocumentChunkResultStage

_NOW = datetime.now(timezone.utc)

_VALID_COMMAND_DATA = dict(
    task_uuid="uuid-r-001",
    knowledge_name="TestKb",
    document_id=5,
    vector_id=100,
    version=3,
    content="一段有效文本",
    embedding_model="text-embedding-v2",
    created_at=_NOW.isoformat(),
)

_VALID_RESULT_KWARGS = dict(
    task_uuid="uuid-r-001",
    version=3,
    stage=DocumentChunkResultStage.COMPLETED,
    message="切片重建完成",
    knowledge_name="TestKb",
    document_id=5,
    vector_id=100,
    embedding_model="text-embedding-v2",
)


class TestChunkRebuildCommandMessage:
    """KnowledgeChunkRebuildCommandMessage 测试。"""

    def test_valid_payload_creates_model(self):
        """测试目的：提供完整有效字段时应成功创建模型实例。
        预期结果：message_type 为 knowledge_chunk_rebuild_command，各字段正确。
        """
        msg = KnowledgeChunkRebuildCommandMessage(**_VALID_COMMAND_DATA)
        assert msg.message_type == "knowledge_chunk_rebuild_command"
        assert msg.vector_id == 100

    def test_content_gets_stripped(self):
        """测试目的：content 前后有空白时应被 strip 处理。
        预期结果：msg.content 不含首尾空白。
        """
        data = {**_VALID_COMMAND_DATA, "content": "  有效文本  "}
        msg = KnowledgeChunkRebuildCommandMessage(**data)
        assert msg.content == "有效文本"

    def test_whitespace_only_content_raises_error(self):
        """测试目的：content 仅含空白字符时应校验失败。
        预期结果：抛出 ValidationError。
        """
        data = {**_VALID_COMMAND_DATA, "content": "   "}
        with pytest.raises(ValidationError):
            KnowledgeChunkRebuildCommandMessage(**data)

    def test_invalid_knowledge_name_raises_error(self):
        """测试目的：knowledge_name 不符合正则（数字开头）时应校验失败。
        预期结果：抛出 ValidationError。
        """
        data = {**_VALID_COMMAND_DATA, "knowledge_name": "123invalid"}
        with pytest.raises(ValidationError):
            KnowledgeChunkRebuildCommandMessage(**data)

    def test_valid_knowledge_name_with_underscore(self):
        """测试目的：knowledge_name 含下划线时应通过校验。
        预期结果：模型创建成功。
        """
        data = {**_VALID_COMMAND_DATA, "knowledge_name": "My_Knowledge_Base01"}
        msg = KnowledgeChunkRebuildCommandMessage(**data)
        assert msg.knowledge_name == "My_Knowledge_Base01"

    def test_zero_vector_id_raises_error(self):
        """测试目的：vector_id <= 0 时应校验失败。
        预期结果：抛出 ValidationError。
        """
        data = {**_VALID_COMMAND_DATA, "vector_id": 0}
        with pytest.raises(ValidationError):
            KnowledgeChunkRebuildCommandMessage(**data)

    def test_zero_version_raises_error(self):
        """测试目的：version <= 0 时应校验失败。
        预期结果：抛出 ValidationError。
        """
        data = {**_VALID_COMMAND_DATA, "version": 0}
        with pytest.raises(ValidationError):
            KnowledgeChunkRebuildCommandMessage(**data)


class TestChunkRebuildResultMessage:
    """KnowledgeChunkRebuildResultMessage 测试。"""

    def test_build_creates_valid_instance(self):
        """测试目的：build 工厂方法应创建有效实例。
        预期结果：message_type 正确，stage 为 COMPLETED。
        """
        msg = KnowledgeChunkRebuildResultMessage.build(**_VALID_RESULT_KWARGS)
        assert msg.message_type == "knowledge_chunk_rebuild_result"
        assert msg.stage == DocumentChunkResultStage.COMPLETED

    def test_build_calculates_duration(self):
        """测试目的：传入 started_at 和 occurred_at 时应计算正确的 duration_ms。
        预期结果：duration_ms == 3000（3 秒）。
        """
        started = datetime(2025, 6, 1, 12, 0, 0, tzinfo=timezone.utc)
        occurred = datetime(2025, 6, 1, 12, 0, 3, tzinfo=timezone.utc)
        msg = KnowledgeChunkRebuildResultMessage.build(
            **_VALID_RESULT_KWARGS, started_at=started, occurred_at=occurred,
        )
        assert msg.duration_ms == 3000

    def test_build_embedding_dim_negative_clamped_to_zero(self):
        """测试目的：传入负数 embedding_dim 时应被截断为 0。
        预期结果：msg.embedding_dim == 0。
        """
        msg = KnowledgeChunkRebuildResultMessage.build(
            **_VALID_RESULT_KWARGS, embedding_dim=-1,
        )
        assert msg.embedding_dim == 0
