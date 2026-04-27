from __future__ import annotations

from typing import Any

from langchain_core.messages import AIMessage, HumanMessage

from app.core.agent.agent_runtime import agent_stream


class _FakeChunk:
    """
    模拟最小消息分片对象。

    Attributes:
        content: 回答分片文本。
        reasoning_content: 思考分片文本。
        additional_kwargs: 扩展字段容器，可用于模拟 LangChain 封装字段。
    """

    def __init__(
            self,
            *,
            content: str = "",
            reasoning_content: str | None = None,
            additional_kwargs: dict[str, Any] | None = None,
    ) -> None:
        """
        初始化测试分片对象。

        Args:
            content: 回答文本分片，默认空字符串。
            reasoning_content: 思考文本分片，默认 `None`。
            additional_kwargs: 扩展字段字典，默认 `None`。

        Returns:
            None
        """

        self.content = content
        self.reasoning_content = reasoning_content
        self.additional_kwargs = dict(additional_kwargs or {})


class _FakeStreamAgent:
    """
    用于测试 `agent_stream` 的最小化假 Agent。

    Attributes:
        events: 预定义流事件序列，按 `astream` 顺序依次产出。
    """

    def __init__(self, events: list[tuple[str, Any]]) -> None:
        """
        初始化测试 Agent。

        Args:
            events: 流式事件序列，每一项是 `(mode, payload)`。

        Returns:
            None
        """

        self.events = events

    async def astream(self, _payload: dict[str, Any], stream_mode: list[str]):  # type: ignore[no-untyped-def]
        """
        按预定义顺序异步产出事件。

        Args:
            _payload: 调用方输入负载（测试中不使用）。
            stream_mode: 订阅模式列表（测试中仅用于断言非空）。

        Yields:
            tuple[str, Any]: 与 `events` 对齐的流事件。
        """

        assert stream_mode
        for event in self.events:
            yield event


def test_agent_stream_emits_answer_and_thinking_deltas():
    """测试目的：验证 `reasoning_content` 可透传 thinking；预期结果：先收到 thinking 再收到 answer 且有聚合返回。"""

    model_deltas: list[str] = []
    thinking_deltas: list[str] = []
    events = [
        ("messages", (_FakeChunk(reasoning_content="思考分片"), {"langgraph_node": "model"})),
        ("messages", (_FakeChunk(content="答案分片"), {"langgraph_node": "model"})),
        ("values", {"messages": [AIMessage(content="最终答案")]}),
    ]
    agent = _FakeStreamAgent(events)

    result = agent_stream(
        agent,
        [HumanMessage(content="用户问题")],
        on_model_delta=model_deltas.append,
        on_thinking_delta=thinking_deltas.append,
    )

    assert model_deltas == ["答案分片"]
    assert thinking_deltas == ["思考分片"]
    assert result["streamed_text"] == "答案分片"
    assert result["streamed_thinking"] == "思考分片"
    assert len(result["final_messages"]) == 1


def test_agent_stream_stops_forwarding_thinking_after_answer_started():
    """测试目的：验证进入回答阶段后不再透传 thinking；预期结果：answer 后续 chunk 的 reasoning_content 被忽略。"""

    model_deltas: list[str] = []
    thinking_deltas: list[str] = []
    events = [
        ("messages", (_FakeChunk(reasoning_content="思考1"), {"langgraph_node": "model"})),
        ("messages", (_FakeChunk(content="答案1"), {"langgraph_node": "model"})),
        ("messages", (_FakeChunk(content="答案2", reasoning_content="思考2"), {"langgraph_node": "model"})),
        ("values", {"messages": [AIMessage(content="最终答案")]}),
    ]
    agent = _FakeStreamAgent(events)

    result = agent_stream(
        agent,
        [HumanMessage(content="用户问题")],
        on_model_delta=model_deltas.append,
        on_thinking_delta=thinking_deltas.append,
    )

    assert thinking_deltas == ["思考1"]
    assert model_deltas == ["答案1", "答案2"]
    assert result["streamed_thinking"] == "思考1"
    assert result["streamed_text"] == "答案1答案2"


def test_agent_stream_does_not_emit_thinking_when_reasoning_content_absent():
    """测试目的：验证无 reasoning_content 时不触发 thinking 回调；预期结果：thinking 回调为空但 answer 正常输出。"""

    model_deltas: list[str] = []
    thinking_deltas: list[str] = []
    events = [
        ("messages", (_FakeChunk(content="答案分片"), {"langgraph_node": "model"})),
        ("values", {"messages": [AIMessage(content="最终答案")]}),
    ]
    agent = _FakeStreamAgent(events)

    result = agent_stream(
        agent,
        [HumanMessage(content="用户问题")],
        on_model_delta=model_deltas.append,
        on_thinking_delta=thinking_deltas.append,
    )

    assert model_deltas == ["答案分片"]
    assert thinking_deltas == []
    assert result["streamed_text"] == "答案分片"
    assert result["streamed_thinking"] == ""


def test_agent_stream_emits_thinking_from_additional_kwargs_reasoning_content():
    """测试目的：验证 LangChain 包装字段 additional_kwargs.reasoning_content 可透传；预期结果：thinking 回调收到分片并写入 streamed_thinking。"""

    model_deltas: list[str] = []
    thinking_deltas: list[str] = []
    events = [
        (
            "messages",
            (
                _FakeChunk(additional_kwargs={"reasoning_content": "思考-扩展字段"}),
                {"langgraph_node": "model"},
            ),
        ),
        ("messages", (_FakeChunk(content="答案分片"), {"langgraph_node": "model"})),
        ("values", {"messages": [AIMessage(content="最终答案")]}),
    ]
    agent = _FakeStreamAgent(events)

    result = agent_stream(
        agent,
        [HumanMessage(content="用户问题")],
        on_model_delta=model_deltas.append,
        on_thinking_delta=thinking_deltas.append,
    )

    assert thinking_deltas == ["思考-扩展字段"]
    assert model_deltas == ["答案分片"]
    assert result["streamed_thinking"] == "思考-扩展字段"
    assert result["streamed_text"] == "答案分片"


def test_agent_stream_deduplicates_reasoning_content_between_two_sources():
    """测试目的：验证 reasoning_content 在属性与 additional_kwargs 同时出现时不会重复透传；预期结果：thinking 仅收到一次该分片。"""

    model_deltas: list[str] = []
    thinking_deltas: list[str] = []
    events = [
        (
            "messages",
            (
                _FakeChunk(
                    reasoning_content="思考去重分片",
                    additional_kwargs={"reasoning_content": "思考去重分片"},
                ),
                {"langgraph_node": "model"},
            ),
        ),
        ("messages", (_FakeChunk(content="答案分片"), {"langgraph_node": "model"})),
        ("values", {"messages": [AIMessage(content="最终答案")]}),
    ]
    agent = _FakeStreamAgent(events)

    result = agent_stream(
        agent,
        [HumanMessage(content="用户问题")],
        on_model_delta=model_deltas.append,
        on_thinking_delta=thinking_deltas.append,
    )

    assert thinking_deltas == ["思考去重分片"]
    assert model_deltas == ["答案分片"]
    assert result["streamed_thinking"] == "思考去重分片"
    assert result["streamed_text"] == "答案分片"
