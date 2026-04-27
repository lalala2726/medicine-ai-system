"""broker 模块单元测试。"""

from __future__ import annotations

from unittest.mock import patch


class TestIsMqConfigured:
    """is_mq_configured 函数测试。"""

    def test_returns_true_when_host_is_set(self):
        """测试目的：RABBITMQ_HOST 已配置时应返回 True。
        预期结果：返回 True。
        """
        with patch.dict("os.environ", {"RABBITMQ_HOST": "192.168.1.1"}):
            from app.core.mq.broker import is_mq_configured

            assert is_mq_configured() is True

    def test_returns_false_when_host_is_empty(self):
        """测试目的：RABBITMQ_HOST 为空字符串时应返回 False。
        预期结果：返回 False。
        """
        with patch.dict("os.environ", {"RABBITMQ_HOST": ""}, clear=False):
            from app.core.mq.broker import is_mq_configured

            assert is_mq_configured() is False

    def test_returns_false_when_host_is_not_set(self):
        """测试目的：RABBITMQ_HOST 未设置时应返回 False。
        预期结果：返回 False。
        """
        with patch.dict("os.environ", {}, clear=True):
            from app.core.mq.broker import is_mq_configured

            assert is_mq_configured() is False


class TestBuildAmqpUrl:
    """_build_amqp_url 函数测试。"""

    def test_builds_url_with_all_env_vars(self):
        """测试目的：所有环境变量齐全时应正确拼接 AMQP URL。
        预期结果：URL 格式为 amqp://user:pass@host:port/vhost。
        """
        env = {
            "RABBITMQ_HOST": "10.0.0.1",
            "RABBITMQ_PORT": "5673",
            "RABBITMQ_USERNAME": "admin",
            "RABBITMQ_PASSWORD": "secret",
            "RABBITMQ_VHOST": "/test",
        }
        with patch.dict("os.environ", env, clear=True):
            from app.core.mq.broker import _build_amqp_url

            url = _build_amqp_url()
            assert url == "amqp://admin:secret@10.0.0.1:5673//test"

    def test_builds_url_with_defaults(self):
        """测试目的：仅设置 HOST 时其他参数应使用默认值。
        预期结果：port=5672, username=guest, password=guest, vhost=/。
        """
        with patch.dict("os.environ", {"RABBITMQ_HOST": "myhost"}, clear=True):
            from app.core.mq.broker import _build_amqp_url

            url = _build_amqp_url()
            assert url == "amqp://guest:guest@myhost:5672//"

    def test_default_host_is_localhost(self):
        """测试目的：HOST 未设置时默认应为 localhost。
        预期结果：URL 包含 @localhost:。
        """
        with patch.dict("os.environ", {}, clear=True):
            from app.core.mq.broker import _build_amqp_url

            url = _build_amqp_url()
            assert "@localhost:" in url
