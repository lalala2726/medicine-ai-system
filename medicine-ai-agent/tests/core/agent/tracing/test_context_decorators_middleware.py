"""Agent Trace 上下文、装饰器和 middleware 单元测试。"""

from __future__ import annotations

import asyncio
from types import SimpleNamespace
from typing import Any

import pytest
from langchain.messages import ToolMessage

import app.core.agent.tracing.decorators as decorators_module
import app.core.agent.tracing.middleware as tracing_middleware_module
from app.core.agent.tracing.context import (
    bind_trace_state,
    create_trace_state,
    get_current_span_id,
    reset_trace_state,
)
from app.core.agent.tracing.middleware import TraceModelMiddleware
from app.schemas.document.agent_trace import AgentTraceSpanType, AgentTraceStatus


@pytest.fixture
def trace_operations(monkeypatch: pytest.MonkeyPatch) -> list[dict[str, Any]]:
    """捕获 trace 写入事件，避免单元测试触发真实后台 writer 和 MongoDB。"""
    operations: list[dict[str, Any]] = []

    def _capture_trace_operation(operation: dict[str, Any]) -> bool:
        """记录一次 trace 写入操作并返回入队成功。"""
        operations.append(operation)
        return True

    monkeypatch.setattr(decorators_module, "is_agent_trace_enabled", lambda: True)
    monkeypatch.setattr(decorators_module, "enqueue_trace_operation", _capture_trace_operation)
    monkeypatch.setattr(
        tracing_middleware_module,
        "enqueue_trace_operation",
        _capture_trace_operation,
    )
    return operations


def _create_bound_trace_state():
    """
    功能描述：
        创建并绑定测试用 trace 状态。

    参数说明：
        无。

    返回值：
        tuple: trace 状态和 ContextVar 重置 token。
    """

    state = create_trace_state(
        graph_name="admin_assistant_graph",
        conversation_uuid="conversation-1",
        assistant_message_uuid="assistant-message-1",
        user_id=1001,
        conversation_type="admin",
        entrypoint="test",
    )
    token = bind_trace_state(state)
    return state, token


def test_trace_run_and_span_documents_keep_parent_sequence_and_tokens(
        trace_operations: list[dict[str, Any]],
) -> None:
    """验证 run、普通 span 和 root graph span 的父子关系、顺序号、token 汇总稳定。"""
    state = decorators_module.start_trace_run(
        graph_name="admin_assistant_graph",
        conversation_uuid="conversation-1",
        assistant_message_uuid="assistant-message-1",
        user_id=1001,
        conversation_type="admin",
        entrypoint="unit_test",
    )
    assert state is not None

    token = bind_trace_state(state)
    try:
        state.add_token_usage(input_tokens=3, output_tokens=4, total_tokens=7)
        with decorators_module.AgentTraceSpan(
                name="admin_agent",
                span_type=AgentTraceSpanType.NODE,
                input_payload={"question": "查询订单"},
                attributes={"node": "admin"},
        ) as span:
            span.finish(output_payload={"answer": "ok"})

        assert get_current_span_id() == state.root_span_id
        decorators_module.finish_trace_run(
            state=state,
            status=AgentTraceStatus.SUCCESS,
        )
    finally:
        reset_trace_state(token)

    insert_run = trace_operations[0]["document"]
    child_span = trace_operations[1]["document"]
    root_span = trace_operations[2]["document"]
    update_run = trace_operations[3]["updates"]

    assert trace_operations[0]["type"] == "insert_run"
    assert insert_run["trace_id"] == state.trace_id
    assert insert_run["status"] == AgentTraceStatus.RUNNING.value
    assert child_span["parent_span_id"] == state.root_span_id
    assert child_span["span_type"] == AgentTraceSpanType.NODE.value
    assert child_span["sequence"] == 2
    assert root_span["parent_span_id"] is None
    assert root_span["span_type"] == AgentTraceSpanType.GRAPH.value
    assert root_span["sequence"] == 1
    assert root_span["token_usage"] == {
        "input_tokens": 3,
        "output_tokens": 4,
        "total_tokens": 7,
    }
    assert update_run["status"] == AgentTraceStatus.SUCCESS.value
    assert update_run["total_tokens"] == 7


def test_agent_trace_records_error_and_restores_parent_span(
        trace_operations: list[dict[str, Any]],
) -> None:
    """验证节点装饰器遇到异常时会写 error span，并恢复父 span 栈。"""
    state, token = _create_bound_trace_state()

    @decorators_module.agent_trace(name="failing_node")
    def _failing_node(value: int) -> None:
        """
        功能描述：
            测试用失败节点。

        参数说明：
            value (int): 用于拼接异常信息的测试值。

        返回值：
            None。
        """

        raise RuntimeError(f"boom-{value}")

    try:
        with pytest.raises(RuntimeError, match="boom-7"):
            _failing_node(7)
        assert get_current_span_id() == state.root_span_id
    finally:
        reset_trace_state(token)

    span_document = trace_operations[0]["document"]

    assert span_document["name"] == "failing_node"
    assert span_document["status"] == AgentTraceStatus.ERROR.value
    assert span_document["error_payload"] == {
        "error_type": "RuntimeError",
        "error_message": "boom-7",
    }
    assert span_document["input_payload"]["args"] == [7]


def test_trace_model_middleware_records_model_payload_and_token_usage(
        trace_operations: list[dict[str, Any]],
) -> None:
    """验证模型 middleware 会记录可见工具、模型属性、输出和 token 汇总。"""
    state, token = _create_bound_trace_state()
    request = SimpleNamespace(
        model=SimpleNamespace(
            model_name="qwen3.5-flash",
            temperature=0.2,
            max_tokens=1024,
            model_kwargs={"top_p": 0.8},
        ),
        model_settings={"stream_usage": True},
        system_message=SimpleNamespace(content="系统提示"),
        messages=[SimpleNamespace(type="human", content="查询订单")],
        tools=[
            SimpleNamespace(
                name="order_list",
                description="查询订单列表",
                args_schema={"type": "object"},
            )
        ],
        tool_choice="auto",
        response_format={"type": "json_object"},
    )
    response_message = SimpleNamespace(
        type="ai",
        content="我需要调用工具",
        response_metadata={"finish_reason": "tool_calls"},
        usage_metadata={
            "input_tokens": 11,
            "output_tokens": 6,
            "total_tokens": 17,
        },
        tool_calls=[{"name": "order_list", "args": {"limit": 10}}],
    )
    response = SimpleNamespace(
        result=[response_message],
        structured_response={"ok": True},
    )

    def _handler(received_request: Any) -> Any:
        """
        功能描述：
            测试用模型处理器。

        参数说明：
            received_request (Any): middleware 透传的模型请求。

        返回值：
            Any: 测试模型响应。
        """

        assert received_request is request
        return response

    try:
        result = TraceModelMiddleware(slot="admin_chat").wrap_model_call(request, _handler)
    finally:
        reset_trace_state(token)

    span_document = trace_operations[0]["document"]

    assert result is response
    assert state.input_tokens == 11
    assert state.output_tokens == 6
    assert state.total_tokens == 17
    assert span_document["name"] == "model"
    assert span_document["span_type"] == AgentTraceSpanType.MODEL.value
    assert span_document["attributes"]["model_name"] == "qwen3.5-flash"
    assert span_document["attributes"]["slot"] == "admin_chat"
    assert span_document["input_payload"]["tools"][0]["name"] == "order_list"
    assert span_document["output_payload"]["finish_reason"] == "tool_calls"
    assert span_document["output_payload"]["tool_calls"] == [
        {"id": "", "name": "order_list", "arguments": {"limit": 10}},
    ]
    assert span_document["token_usage"] == {
        "input_tokens": 11,
        "output_tokens": 6,
        "total_tokens": 17,
    }


def test_trace_model_middleware_enqueues_model_token_usage_document_with_cache_metrics(
        trace_operations: list[dict[str, Any]],
) -> None:
    """验证模型 middleware 会把官方缓存 Token 字段写入模型 Token 明细事件。"""
    state, token = _create_bound_trace_state()
    state.register_span_metadata(
        "node-span-1",
        {
            "span_type": AgentTraceSpanType.NODE.value,
            "name": "Admin Assistant Agent Node",
        },
    )
    state.span_stack.append("node-span-1")
    request = SimpleNamespace(
        model=SimpleNamespace(
            model_name="qwen3-coder-plus",
            temperature=0.2,
            max_tokens=2048,
            model_kwargs={},
        ),
        model_settings={},
        system_message=SimpleNamespace(content="系统提示"),
        messages=[SimpleNamespace(type="human", content="分析代码")],
        tools=[],
        tool_choice=None,
        response_format=None,
    )
    response_message = SimpleNamespace(
        type="ai",
        content="分析完成",
        response_metadata={
            "token_usage": {
                "prompt_tokens": 2174,
                "completion_tokens": 88,
                "total_tokens": 2262,
                "prompt_tokens_details": {
                    "cached_tokens": 1605,
                    "cache_creation_input_tokens": 0,
                },
            },
            "finish_reason": "stop",
        },
    )
    response = SimpleNamespace(result=[response_message], structured_response=None)

    def _handler(received_request: Any) -> Any:
        """
        功能描述：
            测试用模型处理器。

        参数说明：
            received_request (Any): middleware 透传的模型请求。

        返回值：
            Any: 测试模型响应。
        """

        assert received_request is request
        return response

    try:
        result = TraceModelMiddleware(slot="adminAssistant.serviceNodeModel").wrap_model_call(
            request,
            _handler,
        )
    finally:
        state.span_stack.remove("node-span-1")
        reset_trace_state(token)

    span_operation = next(
        operation for operation in trace_operations if operation["type"] == "insert_span"
    )
    token_usage_operation = next(
        operation for operation in trace_operations if operation["type"] == "insert_model_token_usage"
    )
    token_usage_document = token_usage_operation["document"]

    assert result is response
    assert span_operation["type"] == "insert_span"
    assert token_usage_operation["type"] == "insert_model_token_usage"
    assert token_usage_document["trace_id"] == state.trace_id
    assert token_usage_document["span_id"] == span_operation["document"]["span_id"]
    assert token_usage_document["conversation_uuid"] == "conversation-1"
    assert token_usage_document["assistant_message_uuid"] == "assistant-message-1"
    assert token_usage_document["user_id"] == 1001
    assert token_usage_document["conversation_type"] == "admin"
    assert token_usage_document["graph_name"] == "admin_assistant_graph"
    assert token_usage_document["entrypoint"] == "test"
    assert token_usage_document["node_name"] == "Admin Assistant Agent Node"
    assert token_usage_document["slot"] == "adminAssistant.serviceNodeModel"
    assert token_usage_document["model_name"] == "qwen3-coder-plus"
    assert token_usage_document["status"] == AgentTraceStatus.SUCCESS.value
    assert token_usage_document["input_tokens"] == 2174
    assert token_usage_document["output_tokens"] == 88
    assert token_usage_document["total_tokens"] == 2262
    assert token_usage_document["cache_read_tokens"] == 1605
    assert token_usage_document["cache_write_tokens"] == 0
    assert token_usage_document["cache_total_tokens"] == 1605
    assert token_usage_document["cache_mode"] == "implicit"
    assert token_usage_document["raw_usage"]["prompt_tokens_details"] == {
        "cached_tokens": 1605,
        "cache_creation_input_tokens": 0,
    }


def test_trace_model_middleware_enqueues_error_token_usage_document(
        trace_operations: list[dict[str, Any]],
) -> None:
    """验证模型调用异常时仍会写入失败 Token 明细，便于监控失败率。"""
    state, token = _create_bound_trace_state()
    request = SimpleNamespace(
        model=SimpleNamespace(
            model_name="qwen3.5-plus",
            temperature=0.2,
            max_tokens=1024,
            model_kwargs={},
        ),
        model_settings={},
        system_message=SimpleNamespace(
            content=[
                {
                    "type": "text",
                    "text": "系统提示",
                    "cache_control": {"type": "ephemeral"},
                }
            ],
        ),
        messages=[SimpleNamespace(type="human", content="查询订单")],
        tools=[],
        tool_choice=None,
        response_format=None,
    )

    def _handler(received_request: Any) -> Any:
        """
        功能描述：
            测试用失败模型处理器。

        参数说明：
            received_request (Any): middleware 透传的模型请求。

        返回值：
            Any: 本处理器始终抛出异常，不返回正常值。
        """

        assert received_request is request
        raise ValueError("模型调用失败")

    try:
        with pytest.raises(ValueError, match="模型调用失败"):
            TraceModelMiddleware(slot="adminAssistant.serviceNodeModel").wrap_model_call(
                request,
                _handler,
            )
    finally:
        reset_trace_state(token)

    span_operation = next(
        operation for operation in trace_operations if operation["type"] == "insert_span"
    )
    token_usage_operation = next(
        operation for operation in trace_operations if operation["type"] == "insert_model_token_usage"
    )
    token_usage_document = token_usage_operation["document"]

    assert span_operation["type"] == "insert_span"
    assert span_operation["document"]["status"] == AgentTraceStatus.ERROR.value
    assert span_operation["document"]["error_payload"] == {
        "error_type": "ValueError",
        "error_message": "模型调用失败",
    }
    assert token_usage_operation["type"] == "insert_model_token_usage"
    assert token_usage_document["trace_id"] == state.trace_id
    assert token_usage_document["span_id"] == span_operation["document"]["span_id"]
    assert token_usage_document["status"] == AgentTraceStatus.ERROR.value
    assert token_usage_document["error_type"] == "ValueError"
    assert token_usage_document["error_message"] == "模型调用失败"
    assert token_usage_document["input_tokens"] == 0
    assert token_usage_document["output_tokens"] == 0
    assert token_usage_document["total_tokens"] == 0
    assert token_usage_document["cache_mode"] == "explicit"


def test_trace_tool_middleware_records_tool_input_output_and_display_name(
        trace_operations: list[dict[str, Any]],
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """验证工具 middleware 会记录工具参数、展示名和工具返回结果。"""
    state, token = _create_bound_trace_state()
    monkeypatch.setattr(
        tracing_middleware_module,
        "_resolve_tool_display_name",
        lambda tool_name: f"展示-{tool_name}",
    )
    tool_middleware = tracing_middleware_module.build_trace_tool_middleware()
    request = SimpleNamespace(
        tool_call={
            "name": "order_list",
            "id": "tool-call-1",
            "args": {"limit": 10},
        }
    )

    async def _handler(received_request: Any) -> ToolMessage:
        """
        功能描述：
            测试用异步工具处理器。

        参数说明：
            received_request (Any): middleware 透传的工具请求。

        返回值：
            ToolMessage: 工具执行结果。
        """

        assert received_request is request
        return ToolMessage(content='{"ok":true}', tool_call_id="tool-call-1")

    try:
        result = asyncio.run(tool_middleware.awrap_tool_call(request, _handler))
    finally:
        reset_trace_state(token)

    span_document = trace_operations[0]["document"]

    assert isinstance(result, ToolMessage)
    assert span_document["name"] == "order_list"
    assert span_document["span_type"] == AgentTraceSpanType.TOOL.value
    assert span_document["input_payload"] == {
        "tool_name": "order_list",
        "tool_call_id": "tool-call-1",
        "arguments": {"limit": 10},
    }
    assert span_document["attributes"] == {"display_name": "展示-order_list"}
    assert span_document["output_payload"]["content"] == '{"ok":true}'
    assert span_document["output_payload"]["tool_call_id"] == "tool-call-1"
