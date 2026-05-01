from __future__ import annotations

from app.core.agent.tracing.writer import start_agent_trace_writer, stop_agent_trace_writer


def start_agent_tracing() -> None:
    """
    功能描述：
        启动 Agent Trace 运行时组件。

    参数说明：
        无。

    返回值：
        None。
    """

    start_agent_trace_writer()


def stop_agent_tracing() -> None:
    """
    功能描述：
        停止 Agent Trace 运行时组件。

    参数说明：
        无。

    返回值：
        None。
    """

    stop_agent_trace_writer()
