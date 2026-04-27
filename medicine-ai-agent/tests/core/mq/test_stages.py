"""stages 阶段枚举单元测试。"""

from __future__ import annotations

from app.core.mq.models.stages import DocumentChunkResultStage, ImportResultStage


class TestImportResultStage:
    """ImportResultStage 枚举测试。"""

    def test_members_count(self):
        """测试目的：导入结果阶段枚举应包含 4 个成员。
        预期结果：STARTED、PROCESSING、COMPLETED、FAILED 共 4 个。
        """
        assert len(ImportResultStage) == 4

    def test_values_are_uppercase(self):
        """测试目的：所有阶段值应为大写字符串，与 MQ 消息契约一致。
        预期结果：每个成员的 value 均为全大写。
        """
        for member in ImportResultStage:
            assert member.value == member.value.upper()

    def test_string_comparison(self):
        """测试目的：枚举继承 str，应可直接与字符串进行相等比较。
        预期结果：ImportResultStage.STARTED == "STARTED" 为 True。
        """
        assert ImportResultStage.STARTED == "STARTED"
        assert ImportResultStage.FAILED == "FAILED"


class TestDocumentChunkResultStage:
    """DocumentChunkResultStage 枚举测试。"""

    def test_members_count(self):
        """测试目的：切片结果阶段枚举应包含 3 个成员。
        预期结果：STARTED、COMPLETED、FAILED 共 3 个。
        """
        assert len(DocumentChunkResultStage) == 3

    def test_no_processing_stage(self):
        """测试目的：切片链路不含 PROCESSING 阶段（与导入链路不同）。
        预期结果：遍历成员不包含 "PROCESSING"。
        """
        values = {m.value for m in DocumentChunkResultStage}
        assert "PROCESSING" not in values

    def test_string_comparison(self):
        """测试目的：枚举继承 str，应可直接与字符串进行相等比较。
        预期结果：DocumentChunkResultStage.COMPLETED == "COMPLETED" 为 True。
        """
        assert DocumentChunkResultStage.COMPLETED == "COMPLETED"
