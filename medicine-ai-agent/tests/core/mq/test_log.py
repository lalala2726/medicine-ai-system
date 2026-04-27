"""log 模块单元测试。"""

from __future__ import annotations

import io

from loguru import logger

from app.core.mq.log import (
    ChunkAddStage,
    ChunkRebuildStage,
    ImportStage,
    _ERROR_STAGES,
    _WARNING_STAGES,
    mq_log,
)


def _capture_log(func):
    """捕获 loguru 输出到字符串并返回 (level, message)。"""
    buf = io.StringIO()
    fmt = "{level}|{message}"
    handler_id = logger.add(buf, format=fmt, level="DEBUG")
    try:
        func()
        buf.seek(0)
        line = buf.getvalue().strip()
        if not line:
            return None, ""
        level, _, message = line.partition("|")
        return level, message
    finally:
        logger.remove(handler_id)


# ---- 枚举成员数量 -----------------------------------------------------------


class TestEnumMemberCounts:
    """各阶段枚举成员数量测试。"""

    def test_import_stage_has_16_members(self):
        """测试目的：ImportStage 应包含 16 个成员。
        预期结果：len == 16。
        """
        assert len(ImportStage) == 16

    def test_chunk_rebuild_stage_has_12_members(self):
        """测试目的：ChunkRebuildStage 应包含 12 个成员。
        预期结果：len == 12。
        """
        assert len(ChunkRebuildStage) == 12

    def test_chunk_add_stage_has_10_members(self):
        """测试目的：ChunkAddStage 应包含 10 个成员。
        预期结果：len == 10。
        """
        assert len(ChunkAddStage) == 10


# ---- 日志级别选择 -----------------------------------------------------------


class TestLogLevelSelection:
    """mq_log 日志级别选择测试。"""

    def test_error_level_for_import_failed(self):
        """测试目的：ImportStage.FAILED 应输出 ERROR 级别日志。
        预期结果：日志级别为 ERROR。
        """
        level, _ = _capture_log(lambda: mq_log("import", ImportStage.FAILED, "uuid-1"))
        assert level == "ERROR"

    def test_error_level_for_chunk_rebuild_failed(self):
        """测试目的：ChunkRebuildStage.REBUILD_FAILED 应输出 ERROR 级别日志。
        预期结果：日志级别为 ERROR。
        """
        level, _ = _capture_log(lambda: mq_log("chunk_rebuild", ChunkRebuildStage.REBUILD_FAILED, "uuid-2"))
        assert level == "ERROR"

    def test_error_level_for_chunk_add_result_publish_failed(self):
        """测试目的：ChunkAddStage.RESULT_PUBLISH_FAILED 应输出 ERROR 级别日志。
        预期结果：日志级别为 ERROR。
        """
        level, _ = _capture_log(lambda: mq_log("chunk_add", ChunkAddStage.RESULT_PUBLISH_FAILED, "uuid-3"))
        assert level == "ERROR"

    def test_warning_level_for_import_task_stale(self):
        """测试目的：ImportStage.TASK_STALE_DROPPED 应输出 WARNING 级别日志。
        预期结果：日志级别为 WARNING。
        """
        level, _ = _capture_log(lambda: mq_log("import", ImportStage.TASK_STALE_DROPPED, "uuid-4"))
        assert level == "WARNING"

    def test_warning_level_for_chunk_rebuild_task_invalid(self):
        """测试目的：ChunkRebuildStage.TASK_INVALID 应输出 WARNING 级别日志。
        预期结果：日志级别为 WARNING。
        """
        level, _ = _capture_log(lambda: mq_log("chunk_rebuild", ChunkRebuildStage.TASK_INVALID, "uuid-5"))
        assert level == "WARNING"

    def test_info_level_for_normal_stage(self):
        """测试目的：普通阶段（如 TASK_RECEIVED）应输出 INFO 级别日志。
        预期结果：日志级别为 INFO。
        """
        level, _ = _capture_log(lambda: mq_log("import", ImportStage.TASK_RECEIVED, "uuid-6"))
        assert level == "INFO"

    def test_info_level_for_completed(self):
        """测试目的：COMPLETED 阶段应输出 INFO 级别日志。
        预期结果：日志级别为 INFO。
        """
        level, _ = _capture_log(lambda: mq_log("chunk_add", ChunkAddStage.COMPLETED, "uuid-7"))
        assert level == "INFO"


# ---- 日志格式 ---------------------------------------------------------------


class TestLogFormat:
    """mq_log 输出格式测试。"""

    def test_format_contains_pipeline_and_stage(self):
        """测试目的：日志应包含 [pipeline] 和 [stage.value] 标签。
        预期结果：日志包含 '[import]' 和 '[task_received]'。
        """
        _, message = _capture_log(lambda: mq_log("import", ImportStage.TASK_RECEIVED, "uuid-f"))
        assert "[import]" in message
        assert "[task_received]" in message

    def test_format_contains_task_uuid(self):
        """测试目的：日志应包含 task_uuid 标识。
        预期结果：日志包含 'task_uuid=uuid-g'。
        """
        _, message = _capture_log(lambda: mq_log("import", ImportStage.TASK_RECEIVED, "uuid-g"))
        assert "task_uuid=uuid-g" in message

    def test_default_task_uuid_is_dash(self):
        """测试目的：不传 task_uuid 时默认值应为 '-'。
        预期结果：日志包含 'task_uuid=-'。
        """
        _, message = _capture_log(lambda: mq_log("import", ImportStage.TASK_RECEIVED))
        assert "task_uuid=-" in message

    def test_metrics_appended_as_kv_pairs(self):
        """测试目的：传入 metrics 关键字参数时应以 key=value 格式追加到日志末尾。
        预期结果：日志包含 'size=1024' 和 'name=test'。
        """
        _, message = _capture_log(lambda: mq_log(
            "import", ImportStage.DOWNLOAD_DONE, "uuid-h", size=1024, name="test",
        ))
        assert "size=1024" in message
        assert "name=test" in message

    def test_no_trailing_space_without_metrics(self):
        """测试目的：无 metrics 时日志末尾不应有多余空格。
        预期结果：日志不以空格结尾。
        """
        _, message = _capture_log(lambda: mq_log("import", ImportStage.TASK_RECEIVED, "uuid-i"))
        assert not message.endswith(" ")


# ---- 阶段集合完整性 ---------------------------------------------------------


class TestStageSets:
    """_ERROR_STAGES 和 _WARNING_STAGES 集合完整性测试。"""

    def test_error_stages_are_frozenset(self):
        """测试目的：_ERROR_STAGES 应为 frozenset 类型。
        预期结果：isinstance 检查通过。
        """
        assert isinstance(_ERROR_STAGES, frozenset)

    def test_warning_stages_are_frozenset(self):
        """测试目的：_WARNING_STAGES 应为 frozenset 类型。
        预期结果：isinstance 检查通过。
        """
        assert isinstance(_WARNING_STAGES, frozenset)

    def test_no_overlap_between_error_and_warning(self):
        """测试目的：ERROR 和 WARNING 阶段集合不应有交集。
        预期结果：交集为空。
        """
        assert _ERROR_STAGES & _WARNING_STAGES == set()
