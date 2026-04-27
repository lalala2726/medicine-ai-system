from __future__ import annotations

import asyncio

import app.main as main_module


def test_prepare_runtime_before_serving_skips_neo4j_probe_when_disabled(
        monkeypatch,
) -> None:
    """测试目的：关闭 Neo4j 启动探活时不应触发校验；预期结果：仅执行配置快照与语音探活。"""

    call_order: list[str] = []

    def _fake_initialize_snapshot() -> None:
        call_order.append("snapshot")

    def _fake_verify_neo4j() -> None:
        call_order.append("neo4j")

    async def _fake_verify_stt() -> None:
        call_order.append("stt")

    async def _fake_verify_tts() -> None:
        call_order.append("tts")

    monkeypatch.setenv("NEO4J_STARTUP_PING_ENABLED", "false")
    monkeypatch.setattr(main_module, "initialize_agent_config_snapshot", _fake_initialize_snapshot)
    monkeypatch.setattr(main_module, "verify_neo4j_connection", _fake_verify_neo4j)
    monkeypatch.setattr(main_module, "verify_volcengine_stt_connection_on_startup", _fake_verify_stt)
    monkeypatch.setattr(main_module, "verify_volcengine_tts_connection_on_startup", _fake_verify_tts)

    asyncio.run(main_module._prepare_runtime_before_serving())

    assert call_order[0] == "snapshot"
    assert set(call_order[1:]) == {"stt", "tts"}
    assert "neo4j" not in call_order


def test_prepare_runtime_before_serving_runs_neo4j_probe_before_speech_probes(
        monkeypatch,
) -> None:
    """测试目的：开启 Neo4j 启动探活时应先校验图数据库；预期结果：neo4j 位于语音探活之前。"""

    call_order: list[str] = []

    def _fake_initialize_snapshot() -> None:
        call_order.append("snapshot")

    def _fake_verify_neo4j() -> None:
        call_order.append("neo4j")

    async def _fake_verify_stt() -> None:
        call_order.append("stt")

    async def _fake_verify_tts() -> None:
        call_order.append("tts")

    monkeypatch.setenv("NEO4J_STARTUP_PING_ENABLED", "true")
    monkeypatch.setattr(main_module, "initialize_agent_config_snapshot", _fake_initialize_snapshot)
    monkeypatch.setattr(main_module, "verify_neo4j_connection", _fake_verify_neo4j)
    monkeypatch.setattr(main_module, "verify_volcengine_stt_connection_on_startup", _fake_verify_stt)
    monkeypatch.setattr(main_module, "verify_volcengine_tts_connection_on_startup", _fake_verify_tts)

    asyncio.run(main_module._prepare_runtime_before_serving())

    assert call_order[0:2] == ["snapshot", "neo4j"]
    assert set(call_order[2:]) == {"stt", "tts"}


def test_lifespan_clears_neo4j_connection_cache_on_shutdown(monkeypatch) -> None:
    """测试目的：应用关闭时必须清理 Neo4j 全局连接；预期结果：lifespan finally 调用清理入口。"""

    clear_calls: list[str] = []

    async def _run_lifespan() -> None:
        async with main_module.lifespan(main_module.app):
            return None

    async def _fake_prepare_runtime() -> None:
        return None

    monkeypatch.setattr(main_module, "_prepare_runtime_before_serving", _fake_prepare_runtime)
    monkeypatch.setattr(main_module, "is_mq_configured", lambda: False)
    monkeypatch.setattr(main_module, "clear_neo4j_connection_cache", lambda: clear_calls.append("clear"))
    monkeypatch.setattr(main_module, "_speech_startup_probe_done", False)

    asyncio.run(_run_lifespan())

    assert clear_calls == ["clear"]
