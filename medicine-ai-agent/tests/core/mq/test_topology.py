"""topology 模块单元测试。"""

from __future__ import annotations

from faststream.rabbit import ExchangeType, RabbitExchange, RabbitQueue

from app.core.mq.topology import (
    AGENT_CONFIG_REFRESH_ROUTING_KEY,
    CHUNK_ADD_RESULT_ROUTING_KEY,
    CHUNK_REBUILD_RESULT_ROUTING_KEY,
    IMPORT_RESULT_ROUTING_KEY,
    agent_config_refresh_exchange,
    agent_config_refresh_queue,
    chunk_add_command_queue,
    chunk_add_exchange,
    chunk_rebuild_command_queue,
    chunk_rebuild_exchange,
    import_command_queue,
    import_exchange,
)


class TestImportTopology:
    """导入链路拓扑测试。"""

    def test_exchange_name_and_type(self):
        """测试目的：导入交换机名称应为 knowledge.import、类型为 DIRECT、durable。
        预期结果：名称、类型、持久化属性均正确。
        """
        assert import_exchange.name == "knowledge.import"
        assert import_exchange.type == ExchangeType.DIRECT
        assert import_exchange.durable is True

    def test_command_queue_name_and_routing_key(self):
        """测试目的：导入命令队列名称和绑定路由键应与约定一致。
        预期结果：队列名为 knowledge.import.command.q，routing_key 为 knowledge.import.command。
        """
        assert import_command_queue.name == "knowledge.import.command.q"
        assert import_command_queue.routing_key == "knowledge.import.command"
        assert import_command_queue.durable is True

    def test_result_routing_key(self):
        """测试目的：导入结果路由键常量值应正确。
        预期结果：值为 knowledge.import.result。
        """
        assert IMPORT_RESULT_ROUTING_KEY == "knowledge.import.result"


class TestChunkRebuildTopology:
    """切片重建链路拓扑测试。"""

    def test_exchange_name_and_type(self):
        """测试目的：切片重建交换机名称应为 knowledge.chunk_rebuild、DIRECT、durable。
        预期结果：名称、类型、持久化属性均正确。
        """
        assert chunk_rebuild_exchange.name == "knowledge.chunk_rebuild"
        assert chunk_rebuild_exchange.type == ExchangeType.DIRECT
        assert chunk_rebuild_exchange.durable is True

    def test_command_queue(self):
        """测试目的：切片重建命令队列名称和路由键应与约定一致。
        预期结果：队列名含 chunk_rebuild，routing_key 包含 command。
        """
        assert chunk_rebuild_command_queue.name == "knowledge.chunk_rebuild.command.q"
        assert chunk_rebuild_command_queue.routing_key == "knowledge.chunk_rebuild.command"
        assert chunk_rebuild_command_queue.durable is True

    def test_result_routing_key(self):
        """测试目的：切片重建结果路由键常量值应正确。
        预期结果：值为 knowledge.chunk_rebuild.result。
        """
        assert CHUNK_REBUILD_RESULT_ROUTING_KEY == "knowledge.chunk_rebuild.result"


class TestChunkAddTopology:
    """手工新增切片链路拓扑测试。"""

    def test_exchange_name_and_type(self):
        """测试目的：手工新增切片交换机名称应为 knowledge.chunk_add、DIRECT、durable。
        预期结果：名称、类型、持久化属性均正确。
        """
        assert chunk_add_exchange.name == "knowledge.chunk_add"
        assert chunk_add_exchange.type == ExchangeType.DIRECT
        assert chunk_add_exchange.durable is True

    def test_command_queue(self):
        """测试目的：手工新增切片命令队列名称和路由键应与约定一致。
        预期结果：队列名含 chunk_add，routing_key 包含 command。
        """
        assert chunk_add_command_queue.name == "knowledge.chunk_add.command.q"
        assert chunk_add_command_queue.routing_key == "knowledge.chunk_add.command"
        assert chunk_add_command_queue.durable is True

    def test_result_routing_key(self):
        """测试目的：手工新增切片结果路由键常量值应正确。
        预期结果：值为 knowledge.chunk_add.result。
        """
        assert CHUNK_ADD_RESULT_ROUTING_KEY == "knowledge.chunk_add.result"


class TestTopologyTypes:
    """拓扑对象类型验证。"""

    def test_all_exchanges_are_rabbit_exchange(self):
        """测试目的：所有 exchange 变量应为 RabbitExchange 实例。
        预期结果：三个 exchange 均为 RabbitExchange 类型。
        """
        for ex in (
                import_exchange,
                chunk_rebuild_exchange,
                chunk_add_exchange,
                agent_config_refresh_exchange,
        ):
            assert isinstance(ex, RabbitExchange)

    def test_all_queues_are_rabbit_queue(self):
        """测试目的：所有 queue 变量应为 RabbitQueue 实例。
        预期结果：三个 queue 均为 RabbitQueue 类型。
        """
        for q in (
                import_command_queue,
                chunk_rebuild_command_queue,
                chunk_add_command_queue,
                agent_config_refresh_queue,
        ):
            assert isinstance(q, RabbitQueue)


class TestAgentConfigRefreshTopology:
    """Agent 配置刷新链路拓扑测试。"""

    def test_exchange_name_and_type(self):
        """测试目的：配置刷新交换机名称和类型应与约定一致。
        预期结果：exchange 为 agent.config.refresh、DIRECT、durable。
        """
        assert agent_config_refresh_exchange.name == "agent.config.refresh"
        assert agent_config_refresh_exchange.type == ExchangeType.DIRECT
        assert agent_config_refresh_exchange.durable is True

    def test_queue_name_routing_key_and_ttl(self):
        """测试目的：配置刷新队列名称、路由键和 TTL 应正确。
        预期结果：队列名为 agent.config.refresh.q，routing_key 正确，TTL 为 300000。
        """
        assert agent_config_refresh_queue.name == "agent.config.refresh.q"
        assert agent_config_refresh_queue.routing_key == "agent.config.refresh"
        assert agent_config_refresh_queue.durable is True
        ttl_arguments = (
                getattr(agent_config_refresh_queue, "arguments", None)
                or getattr(agent_config_refresh_queue, "queue_arguments", None)
        )
        assert ttl_arguments is not None
        assert ttl_arguments["x-message-ttl"] == 300000

    def test_result_routing_key(self):
        """测试目的：配置刷新 routing key 常量值应正确。
        预期结果：值为 agent.config.refresh。
        """
        assert AGENT_CONFIG_REFRESH_ROUTING_KEY == "agent.config.refresh"
