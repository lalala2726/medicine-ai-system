"""version_store 模块单元测试。"""

from __future__ import annotations

from unittest.mock import MagicMock, patch

_MODULE = "app.core.mq.version_store"


def _mock_redis(return_value=None):
    """构造 Redis mock，get() 返回指定值。"""
    conn = MagicMock()
    conn.get.return_value = return_value
    return conn


class TestReadVersion:
    """_read_version 内部函数测试。"""

    def test_returns_int_when_value_is_str(self):
        """测试目的：Redis 返回字符串 '5' 时应转为整型。
        预期结果：返回 5。
        """
        with patch(f"{_MODULE}.get_redis_connection", return_value=_mock_redis("5")):
            from app.core.mq.version_store import _read_version
            assert _read_version("test:key") == 5

    def test_returns_int_when_value_is_bytes(self):
        """测试目的：Redis 返回 bytes b'10' 时应正确 decode 并转整型。
        预期结果：返回 10。
        """
        with patch(f"{_MODULE}.get_redis_connection", return_value=_mock_redis(b"10")):
            from app.core.mq.version_store import _read_version
            assert _read_version("test:key") == 10

    def test_returns_none_when_key_missing(self):
        """测试目的：Redis 返回 None（键不存在）时应返回 None。
        预期结果：返回 None。
        """
        with patch(f"{_MODULE}.get_redis_connection", return_value=_mock_redis(None)):
            from app.core.mq.version_store import _read_version
            assert _read_version("test:key") is None

    def test_returns_none_on_invalid_value(self):
        """测试目的：Redis 返回非数字字符串时应返回 None 而非抛出异常。
        预期结果：返回 None。
        """
        with patch(f"{_MODULE}.get_redis_connection", return_value=_mock_redis("not_a_number")):
            from app.core.mq.version_store import _read_version
            assert _read_version("test:key") is None

    def test_returns_none_on_redis_exception(self):
        """测试目的：Redis 连接异常时应静默返回 None。
        预期结果：返回 None，不抛出异常。
        """
        conn = MagicMock()
        conn.get.side_effect = ConnectionError("redis down")
        with patch(f"{_MODULE}.get_redis_connection", return_value=conn):
            from app.core.mq.version_store import _read_version
            assert _read_version("test:key") is None


class TestIsImportStale:
    """is_import_stale 函数测试。"""

    def test_not_stale_when_no_redis_record(self):
        """测试目的：Redis 无版本记录时应返回 False（不过期）。
        预期结果：返回 False。
        """
        with patch(f"{_MODULE}.get_redis_connection", return_value=_mock_redis(None)):
            from app.core.mq.version_store import is_import_stale
            assert is_import_stale(biz_key="bk-1", version=1) is False

    def test_stale_when_version_less_than_latest(self):
        """测试目的：消息版本低于 Redis 中最新版本时应判定为过期。
        预期结果：返回 True。
        """
        with patch(f"{_MODULE}.get_redis_connection", return_value=_mock_redis("5")):
            from app.core.mq.version_store import is_import_stale
            assert is_import_stale(biz_key="bk-1", version=3) is True

    def test_not_stale_when_version_equals_latest(self):
        """测试目的：消息版本等于 Redis 中最新版本时不应判定为过期。
        预期结果：返回 False。
        """
        with patch(f"{_MODULE}.get_redis_connection", return_value=_mock_redis("5")):
            from app.core.mq.version_store import is_import_stale
            assert is_import_stale(biz_key="bk-1", version=5) is False

    def test_not_stale_when_version_greater_than_latest(self):
        """测试目的：消息版本高于 Redis 中最新版本时不应判定为过期。
        预期结果：返回 False。
        """
        with patch(f"{_MODULE}.get_redis_connection", return_value=_mock_redis("3")):
            from app.core.mq.version_store import is_import_stale
            assert is_import_stale(biz_key="bk-1", version=5) is False

    def test_correct_redis_key_format(self):
        """测试目的：应使用 kb:latest:{biz_key} 格式查询 Redis。
        预期结果：Redis get 调用参数为 'kb:latest:my_biz'。
        """
        conn = _mock_redis(None)
        with patch(f"{_MODULE}.get_redis_connection", return_value=conn):
            from app.core.mq.version_store import is_import_stale
            is_import_stale(biz_key="my_biz", version=1)
            conn.get.assert_called_once_with("kb:latest:my_biz")


class TestIsChunkRebuildStale:
    """is_chunk_rebuild_stale 函数测试。"""

    def test_not_stale_when_no_redis_record(self):
        """测试目的：Redis 无版本记录时应返回 False。
        预期结果：返回 False。
        """
        with patch(f"{_MODULE}.get_redis_connection", return_value=_mock_redis(None)):
            from app.core.mq.version_store import is_chunk_rebuild_stale
            assert is_chunk_rebuild_stale(vector_id=100, version=1) is False

    def test_stale_when_version_less_than_latest(self):
        """测试目的：消息版本低于最新版本时应判定为过期。
        预期结果：返回 True。
        """
        with patch(f"{_MODULE}.get_redis_connection", return_value=_mock_redis("8")):
            from app.core.mq.version_store import is_chunk_rebuild_stale
            assert is_chunk_rebuild_stale(vector_id=100, version=3) is True

    def test_correct_redis_key_format(self):
        """测试目的：应使用 kb:chunk_edit:latest_version:{vector_id} 格式查询 Redis。
        预期结果：Redis get 调用参数正确。
        """
        conn = _mock_redis(None)
        with patch(f"{_MODULE}.get_redis_connection", return_value=conn):
            from app.core.mq.version_store import is_chunk_rebuild_stale
            is_chunk_rebuild_stale(vector_id=42, version=1)
            conn.get.assert_called_once_with("kb:chunk_edit:latest_version:42")


class TestGetChunkRebuildLatestVersion:
    """get_chunk_rebuild_latest_version 函数测试。"""

    def test_returns_version_int(self):
        """测试目的：Redis 有版本号时应返回整型。
        预期结果：返回 12。
        """
        with patch(f"{_MODULE}.get_redis_connection", return_value=_mock_redis("12")):
            from app.core.mq.version_store import get_chunk_rebuild_latest_version
            assert get_chunk_rebuild_latest_version(vector_id=1) == 12

    def test_returns_none_when_key_missing(self):
        """测试目的：Redis 键不存在时应返回 None。
        预期结果：返回 None。
        """
        with patch(f"{_MODULE}.get_redis_connection", return_value=_mock_redis(None)):
            from app.core.mq.version_store import get_chunk_rebuild_latest_version
            assert get_chunk_rebuild_latest_version(vector_id=1) is None
