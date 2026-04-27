from __future__ import annotations

import asyncio
import threading
import time
import uuid
from dataclasses import dataclass
from typing import Any

from fastapi.responses import StreamingResponse
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage
from loguru import logger

from app.agent.admin.state import ChatHistoryMessage
from app.agent.admin.workflow import build_graph
from app.core.agent.agent_orchestrator import (
    AssistantStreamConfig,
    build_answer_response,
    iterate_assistant_responses,
    serialize_sse,
)
from app.core.agent.run_event_store import (
    AssistantRunEventStore,
    AssistantRunSnapshot,
    LocalRunHandle,
    resolve_assistant_run_snapshot_flush_ms,
    resolve_assistant_run_stream_block_ms,
)
from app.core.codes import ResponseCode
from app.core.config_sync import (
    create_agent_title_llm,
    get_current_agent_config_snapshot,
)
from app.core.exception.exceptions import ServiceException
from app.core.langsmith import build_langsmith_runnable_config
from app.core.security.auth_context import get_user_id
from app.core.speech import build_message_tts_stream
from app.schemas.admin_assistant_history import ConversationMessageResponse
from app.schemas.assistant_run import (
    AssistantRunStatus,
    AssistantRunStopResponse,
    AssistantRunSubmitResponse,
)
from app.schemas.base_request import PageRequest
from app.schemas.document.conversation import ConversationListItem, ConversationType
from app.schemas.document.message import MessageRole, MessageStatus
from app.schemas.sse_response import AssistantResponse, Content, MessageType
from app.services.assistant_message_service import (
    build_assistant_message_callback as _build_assistant_message_callback,
    create_placeholder_assistant_message as _create_placeholder_assistant_message,
    persist_assistant_stream_snapshot as _persist_assistant_stream_snapshot,
    persist_user_message as _persist_user_message,
)
from app.utils.chat_image_utils import (
    EMPTY_IMAGE_QUESTION_TEXT,
    build_user_image_markdown_content,
    normalize_chat_image_urls,
)
from app.services.conversation_service import (
    add_admin_conversation,
    delete_admin_conversation,
    get_admin_conversation,
    list_admin_conversations,
    save_conversation_title,
    update_admin_conversation_title,
)
from app.services.memory_service import load_memory, resolve_assistant_memory_mode
from app.services.message_service import (
    count_messages,
    list_messages,
)
from app.utils.assistant_message_utils import (
    build_conversation_created_event as _build_conversation_created_event,
    build_message_prepared_event as _build_message_prepared_event,
)
from app.utils.prompt_utils import load_managed_prompt

ADMIN_WORKFLOW_NAME = "admin_assistant_graph"
ADMIN_WORKFLOW = build_graph()
STREAM_OUTPUT_NODES = {
    "admin_agent",
}
EMPTY_ASSISTANT_ANSWER_FALLBACK = "服务暂时不可用，请稍后重试。"
RUN_EVENT_STORE = AssistantRunEventStore()
"""助手运行态存储入口，统一负责 Redis 运行态与本机句柄。"""
# 会话标题生成提示词业务键。
GENERATE_TITLE_PROMPT_KEY = "system_generate_title_prompt"
# 会话标题生成提示词本地回退路径。
GENERATE_TITLE_PROMPT_LOCAL_PATH = "_system/generate_title.md"
# 图片发送能力不满足时的统一提示文案。
UNSUPPORTED_IMAGE_UNDERSTANDING_TEXT = "此模型不支持图片理解"
# 管理端聊天界面未选择模型时的统一提示文案。
REQUIRED_CHAT_DISPLAY_MODEL_MESSAGE = "请先选择聊天模型"
# 管理端聊天界面自定义模型不存在时的提示文案模板。
UNKNOWN_CHAT_DISPLAY_MODEL_MESSAGE = "当前选择的模型不存在：%s"
# 管理端聊天界面模型不支持深度思考时的提示文案模板。
UNSUPPORTED_REASONING_MODEL_MESSAGE = "模型不支持深度思考：%s"

STREAMING_STATUS_VISIBLE_SET = {
    MessageStatus.STREAMING,
    MessageStatus.WAITING_INPUT,
    MessageStatus.SUCCESS,
    MessageStatus.CANCELLED,
    MessageStatus.ERROR,
}
"""历史消息列表允许返回的消息状态集合。"""


@dataclass(frozen=True)
class ConversationContext:
    """
    会话准备阶段的统一上下文。

    Attributes:
        conversation_uuid: 会话 UUID。
        conversation_id: Mongo 会话 ID（ObjectId 字符串）。
        assistant_message_uuid: 本轮 AI 回复消息 UUID（在流开始前预生成）。
        history_messages: 会话历史消息（按时间正序）。
        current_question: 当前轮用户问题文本（原始文本）。
        current_image_urls: 当前轮用户上传图片 URL 列表。
        initial_emitted_events: 流开始前要注入的 SSE 事件。
        is_new_conversation: 是否为本次请求创建的新会话。
        override_model_name: 用户手动选择后，经展示模型映射解析出的真实模型名。
        override_reasoning: 用户本次提交显式传入的深度思考开关。
    """

    conversation_uuid: str
    conversation_id: str
    assistant_message_uuid: str
    history_messages: list[ChatHistoryMessage]
    current_question: str
    current_image_urls: list[str]
    initial_emitted_events: tuple[AssistantResponse, ...]
    is_new_conversation: bool
    override_model_name: str | None = None
    override_reasoning: bool | None = None


@dataclass
class StreamAggregateState:
    """
    流式聚合状态。

    Attributes:
        answer_text: 已聚合回答文本。
        thinking_text: 已聚合思考文本。
        last_flush_monotonic: 上次刷 Mongo 的单调时钟时间。
    """

    answer_text: str = ""
    thinking_text: str = ""
    last_flush_monotonic: float = 0.0


def _invoke_admin_workflow(state: dict[str, Any]) -> dict[str, Any]:
    """
    同步执行管理助手 workflow。

    该函数用于 `astream` 不可用时的回退路径，保证接口始终可返回结果。
    """

    config = build_langsmith_runnable_config(
        run_name=ADMIN_WORKFLOW_NAME,
        tags=["admin-assistant", "langgraph"],
        metadata={"entrypoint": "api.admin_assistant.chat"},
    )
    if config:
        return ADMIN_WORKFLOW.invoke(state, config=config)
    return ADMIN_WORKFLOW.invoke(state)


def _build_stream_config() -> dict | None:
    """
    构建流式执行配置。

    LangSmith tracing 未启用时返回 `None`。
    """

    return build_langsmith_runnable_config(
        run_name=ADMIN_WORKFLOW_NAME,
        tags=["admin-assistant", "langgraph"],
        metadata={"entrypoint": "api.admin_assistant.chat"},
    )


def _build_initial_state(
        question: str,
        *,
        conversation_uuid: str,
        assistant_message_uuid: str,
        history_messages: list[ChatHistoryMessage] | None = None,
        current_question: str = "",
        current_image_urls: list[str] | None = None,
        override_model_name: str | None = None,
        override_reasoning: bool | None = None,
) -> dict[str, Any]:
    """
    构造管理助手的初始状态。

    所有节点共享该状态结构，避免执行过程中出现缺失键导致的分支判断复杂化。

    Args:
        question: 当前用户问题文本；当前实现仅用于接口语义占位，不参与状态构造。
        conversation_uuid: 当前会话 UUID，用于会话级工具缓存隔离。
        assistant_message_uuid: 当前轮 AI 占位消息 UUID。
        history_messages: 会话历史消息列表；为空时默认空列表。
        current_question: 当前轮用户原始问题文本。
        current_image_urls: 当前轮用户上传图片 URL 列表。
        override_model_name: 用户手动选择后，经展示模型映射解析出的真实模型名。
        override_reasoning: 用户本次提交显式传入的深度思考开关。
    Returns:
        dict[str, Any]: LangGraph 初始状态字典，包含会话、历史、已加载工具与消息等字段。
    """

    _ = question
    base_history = list(history_messages or [])

    return {
        "conversation_uuid": str(conversation_uuid).strip(),
        "assistant_message_uuid": str(assistant_message_uuid).strip(),
        "history_messages": base_history,
        "loaded_tool_keys": [],
        "current_question": str(current_question or "").strip(),
        "current_image_urls": list(current_image_urls or []),
        "result": "",
        "override_model_name": override_model_name or None,
        "override_reasoning": override_reasoning,
        # 兼容 MessagesState，保证 astream(messages) 能消费到上下文消息。
        "messages": list(base_history),
    }


def _should_stream_token(stream_node: str | None, latest_state: dict[str, Any]) -> bool:
    """
    判定某个节点 token 是否应该被推送给前端。

    当前规则仅允许 `admin_agent` 节点输出 token。
    """

    _ = latest_state
    return stream_node in STREAM_OUTPUT_NODES


def _map_exception(exc: Exception) -> str:
    """
    将内部异常映射为对用户友好的文案。

    业务异常会保留明确错误信息，未知异常统一为通用降级提示。
    """

    if isinstance(exc, ServiceException):
        return f"处理失败: {exc.message}"
    return EMPTY_ASSISTANT_ANSWER_FALLBACK


def _resolve_admin_submit_model_overrides(
        *,
        custom_model_name: str,
        reasoning_enabled: bool,
        image_urls: list[str] | None,
) -> tuple[str, bool]:
    """
    解析管理端聊天提交中的前端展示模型映射与显式思考开关。

    Args:
        custom_model_name: 前端聊天界面传入的自定义模型名称。
        reasoning_enabled: 前端显式传入的深度思考开关。
        image_urls: 本次提交附带的图片 URL 列表。

    Returns:
        tuple[str, bool]:
            - 第一个元素为真实模型名称。
            - 第二个元素为最终显式思考开关。

    Raises:
        ServiceException: 当自定义模型不存在，或能力与请求不匹配时抛出。
    """

    normalized_custom_model_name = str(custom_model_name or "").strip() or None
    normalized_image_urls = normalize_chat_image_urls(image_urls)
    if normalized_custom_model_name is None:
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message=REQUIRED_CHAT_DISPLAY_MODEL_MESSAGE,
        )

    snapshot = get_current_agent_config_snapshot()
    display_model = snapshot.find_admin_chat_display_model(normalized_custom_model_name)
    if display_model is None or display_model.actual_model_name is None:
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message=UNKNOWN_CHAT_DISPLAY_MODEL_MESSAGE % normalized_custom_model_name,
        )

    if normalized_image_urls and not bool(display_model.support_vision):
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message=UNSUPPORTED_IMAGE_UNDERSTANDING_TEXT,
        )

    if reasoning_enabled and not bool(display_model.support_reasoning):
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message=UNSUPPORTED_REASONING_MODEL_MESSAGE % normalized_custom_model_name,
        )

    return display_model.actual_model_name, reasoning_enabled


def _generate_and_save_title(*, conversation_uuid: str, question: str) -> None:
    """
    生成并持久化会话标题。

    该函数运行在后台线程中，避免阻塞 SSE 主链路。
    """

    try:
        title = generate_title(question).strip() or "未知标题"
        save_conversation_title(
            conversation_uuid=conversation_uuid,
            title=title,
        )
    except Exception as exc:  # pragma: no cover - 防御性兜底
        logger.opt(exception=exc).warning(
            "Failed to generate/save conversation title conversation_uuid={conversation_uuid}",
            conversation_uuid=conversation_uuid,
        )


def _schedule_title_generation(*, conversation_uuid: str, question: str) -> None:
    """
    并行调度标题生成任务。

    优先使用当前事件循环把阻塞任务放入线程池；若当前无事件循环，
    则退化为守护线程执行（兼容同步调用/单元测试场景）。
    """

    try:
        loop = asyncio.get_running_loop()
    except RuntimeError:
        thread = threading.Thread(
            target=_generate_and_save_title,
            kwargs={
                "conversation_uuid": conversation_uuid,
                "question": question,
            },
            daemon=True,
        )
        thread.start()
        return

    loop.create_task(
        asyncio.to_thread(
            _generate_and_save_title,
            conversation_uuid=conversation_uuid,
            question=question,
        )
    )


def _load_admin_conversation(
        *,
        conversation_uuid: str,
        user_id: int,
) -> str:
    """
    加载会话并返回 Mongo 会话ID（ObjectId 字符串）。

    Raises:
        ServiceException: 会话不存在、无权限或会话数据异常时抛出。
    """

    conversation = get_admin_conversation(
        conversation_uuid=conversation_uuid,
        user_id=user_id,
    )
    if conversation is None:
        raise ServiceException(code=ResponseCode.NOT_FOUND, message="会话不存在")

    conversation_id = conversation.id
    if conversation_id is None:
        raise ServiceException(code=ResponseCode.DATABASE_ERROR, message="会话数据异常")
    return str(conversation_id)


def _prepare_new_conversation(
        *,
        title_question: str,
        history_question: str,
        current_question: str,
        current_image_urls: list[str],
        user_id: int,
        assistant_message_uuid: str,
) -> ConversationContext:
    """
    准备新会话上下文。

    Args:
        title_question: 当前会话标题生成使用的问题文本。
        history_question: 当前轮写入历史上下文的问题文本。
        current_question: 当前轮用户原始问题文本。
        current_image_urls: 当前轮用户上传图片 URL 列表。
        user_id: 当前用户 ID。

    Returns:
        ConversationContext: 新会话上下文（含会话创建事件）。

    Raises:
        ServiceException: 当会话创建失败时抛出数据库异常。
    """

    conversation_uuid = str(uuid.uuid4())
    conversation_id = add_admin_conversation(
        conversation_uuid=conversation_uuid,
        user_id=user_id,
    )
    if conversation_id is None:
        raise ServiceException(code=ResponseCode.DATABASE_ERROR, message="无法创建会话，请稍后重试。")

    _schedule_title_generation(
        conversation_uuid=conversation_uuid,
        question=title_question,
    )
    return ConversationContext(
        conversation_uuid=conversation_uuid,
        conversation_id=conversation_id,
        assistant_message_uuid=assistant_message_uuid,
        history_messages=[HumanMessage(content=history_question)],
        current_question=current_question,
        current_image_urls=current_image_urls,
        initial_emitted_events=(
            _build_conversation_created_event(
                conversation_uuid=conversation_uuid,
                message_uuid=assistant_message_uuid,
            ),
        ),
        is_new_conversation=True,
    )


def _prepare_existing_conversation(
        *,
        conversation_uuid: str,
        user_id: int,
        history_question: str,
        current_question: str,
        current_image_urls: list[str],
        assistant_message_uuid: str,
) -> ConversationContext:
    """
    准备已存在会话上下文。

    Args:
        conversation_uuid: 会话 UUID。
        user_id: 当前用户 ID（用于会话归属校验）。
        history_question: 当前轮写入历史上下文的问题文本，会追加到历史末尾供模型推理。
        current_question: 当前轮用户原始问题文本。
        current_image_urls: 当前轮用户上传图片 URL 列表。

    Returns:
        ConversationContext: 已存在会话上下文（含消息预创建事件），
            其中 `history_messages` 末尾包含本轮用户问题。

    Raises:
        ServiceException: 当会话不存在、无权限或数据库异常时抛出。
    """

    conversation_id = _load_admin_conversation(
        conversation_uuid=conversation_uuid,
        user_id=user_id,
    )
    memory = load_memory(
        memory_type=resolve_assistant_memory_mode(),
        conversation_uuid=conversation_uuid,
        user_id=user_id,
    )
    history_messages = list(memory.messages)
    # 旧会话场景需显式注入本轮用户输入，避免模型只基于上一轮历史作答。
    history_messages = [*history_messages, HumanMessage(content=history_question)]
    return ConversationContext(
        conversation_uuid=conversation_uuid,
        conversation_id=conversation_id,
        assistant_message_uuid=assistant_message_uuid,
        history_messages=history_messages,
        current_question=current_question,
        current_image_urls=current_image_urls,
        initial_emitted_events=(
            _build_message_prepared_event(
                message_uuid=assistant_message_uuid,
            ),
        ),
        is_new_conversation=False,
    )


def _prepare_conversation_context(
        *,
        question: str,
        image_urls: list[str] | None,
        user_id: int,
        conversation_uuid: str | None,
        assistant_message_uuid: str,
        override_model_name: str | None = None,
        override_reasoning: bool | None = None,
) -> ConversationContext:
    """
    统一准备会话上下文（新建/加载分支）。

    Args:
        question: 当前用户问题文本。
        image_urls: 当前用户上传图片 URL 列表。
        user_id: 当前用户 ID。
        conversation_uuid: 会话 UUID；为空时创建新会话。
        override_model_name: 用户手动选择的模型名。
        override_reasoning: 用户是否手动开启深度思考。

    Returns:
        ConversationContext: 可直接驱动后续流式响应的会话上下文。

    Raises:
        ServiceException: 当会话创建/加载失败时抛出。
    """

    normalized_question = str(question or "").strip()
    normalized_image_urls = normalize_chat_image_urls(image_urls)
    if not normalized_question and not normalized_image_urls:
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="问题和图片不能同时为空")

    history_question = normalized_question or EMPTY_IMAGE_QUESTION_TEXT
    title_question = normalized_question or "图片咨询"

    if conversation_uuid is None:
        ctx = _prepare_new_conversation(
            title_question=title_question,
            history_question=history_question,
            current_question=normalized_question,
            current_image_urls=normalized_image_urls,
            user_id=user_id,
            assistant_message_uuid=assistant_message_uuid,
        )
    else:
        ctx = _prepare_existing_conversation(
            conversation_uuid=conversation_uuid,
            user_id=user_id,
            history_question=history_question,
            current_question=normalized_question,
            current_image_urls=normalized_image_urls,
            assistant_message_uuid=assistant_message_uuid,
        )
    # 用 dataclasses.replace 补充 override 字段，保持原有 frozen dataclass 不可变语义。
    import dataclasses
    return dataclasses.replace(
        ctx,
        override_model_name=override_model_name or None,
        override_reasoning=override_reasoning,
    )


def _append_stream_response_to_aggregate(
        *,
        aggregate_state: StreamAggregateState,
        response: AssistantResponse,
) -> None:
    """
    将标准流式响应聚合到本地快照状态。

    Args:
        aggregate_state: 当前聚合状态。
        response: 最新响应事件。

    Returns:
        None
    """

    if response.is_end:
        return

    text = response.content.text
    if not isinstance(text, str):
        return

    if response.type == MessageType.ANSWER:
        if response.content.state == "replace":
            aggregate_state.answer_text = text
        else:
            aggregate_state.answer_text += text
    elif response.type == MessageType.THINKING:
        if response.content.state == "replace":
            aggregate_state.thinking_text = text
        else:
            aggregate_state.thinking_text += text


def _build_run_snapshot(
        *,
        aggregate_state: StreamAggregateState,
        assistant_message_uuid: str,
        status: AssistantRunStatus,
        last_event_id: str | None = None,
) -> AssistantRunSnapshot:
    """
    根据当前聚合状态构造运行快照。

    Args:
        aggregate_state: 当前聚合状态。
        assistant_message_uuid: AI 消息 UUID。
        status: 当前运行态状态。
        last_event_id: 最新事件 ID。

    Returns:
        AssistantRunSnapshot: 当前运行快照模型。
    """

    return AssistantRunSnapshot(
        answer_text=aggregate_state.answer_text,
        thinking_text=aggregate_state.thinking_text,
        status=status,
        assistant_message_uuid=assistant_message_uuid,
        last_event_id=last_event_id,
    )


def _build_snapshot_attach_events(
        *,
        conversation_uuid: str,
        snapshot: AssistantRunSnapshot,
) -> tuple[AssistantResponse, ...]:
    """
    为 attach 首次连接构造 replace 语义的快照事件。

    Args:
        conversation_uuid: 会话 UUID。
        snapshot: 当前运行快照。

    Returns:
        tuple[AssistantResponse, ...]: 需要先发送给前端的快照事件。
    """

    events: list[AssistantResponse] = []
    shared_meta = {
        "snapshot": True,
        "replace": True,
        "conversation_uuid": conversation_uuid,
        "message_uuid": snapshot.assistant_message_uuid,
    }
    if snapshot.answer_text:
        events.append(
            AssistantResponse(
                content=Content(
                    text=snapshot.answer_text,
                    state="replace",
                ),
                type=MessageType.ANSWER,
                meta=shared_meta,
            )
        )
    if snapshot.thinking_text:
        events.append(
            AssistantResponse(
                content=Content(
                    text=snapshot.thinking_text,
                    state="replace",
                ),
                type=MessageType.THINKING,
                meta=shared_meta,
            )
        )
    return tuple(events)


def _should_replay_terminal_event_after_snapshot(
        *,
        response: AssistantResponse,
) -> bool:
    """
    判断终态 attach 场景下，某条历史事件是否需要在 snapshot 之后再次补发。

    设计原因：
    1. snapshot 已经用 replace 语义补齐了最新 answer/thinking 文本；
    2. 若再完整回放历史 stream，会导致文本重复；
    3. 但卡片、notice 以及最终 `is_end=true` 结束包仍需要补发，保证刷新后前端状态完整。

    Args:
        response: Redis Stream 中读取到的原始事件。

    Returns:
        bool: `True` 表示该事件需要在 snapshot 之后补发；否则跳过。
    """

    if response.is_end:
        return True
    return response.type not in {
        MessageType.ANSWER,
        MessageType.THINKING,
    }


def _build_terminal_attach_end_event(
        *,
        finish_status: AssistantRunStatus,
) -> AssistantResponse:
    """
    为终态 attach 场景构造兜底结束包。

    触发时机：
    - Redis snapshot 已存在；
    - 但事件流中未读到终态 `is_end=true` 包；
    - 需要主动补一个结束包，避免刷新后的前端一直停留在未结束状态。

    Args:
        finish_status: 当前运行终态。

    Returns:
        AssistantResponse: 标准 answer 结束包。
    """

    return build_answer_response(
        "",
        True,
        state=finish_status.value,
        message=(
            "已停止生成"
            if finish_status == AssistantRunStatus.CANCELLED
            else None
        ),
        meta={"run_status": finish_status.value},
    )


async def _flush_stream_snapshot_if_due(
        *,
        conversation_id: str,
        assistant_message_uuid: str,
        aggregate_state: StreamAggregateState,
        force: bool = False,
) -> None:
    """
    按固定节流周期将流式快照刷回 Mongo。

    Args:
        conversation_id: 会话 ID。
        assistant_message_uuid: AI 消息 UUID。
        aggregate_state: 当前聚合状态。
        force: 是否强制立即刷库。

    Returns:
        None
    """

    now_monotonic = time.monotonic()
    flush_interval_seconds = resolve_assistant_run_snapshot_flush_ms() / 1000
    if (
            not force
            and aggregate_state.last_flush_monotonic > 0
            and (now_monotonic - aggregate_state.last_flush_monotonic) < flush_interval_seconds
    ):
        return

    await asyncio.to_thread(
        _persist_assistant_stream_snapshot,
        conversation_id=conversation_id,
        message_uuid=assistant_message_uuid,
        answer_text=aggregate_state.answer_text,
        thinking_text=(aggregate_state.thinking_text or None),
    )
    aggregate_state.last_flush_monotonic = now_monotonic


def _build_background_run_done_callback(
        *,
        conversation_uuid: str,
) -> Any:
    """
    构造后台 run 完成回调，用于记录异常并清理本机句柄。

    Args:
        conversation_uuid: 会话 UUID。

    Returns:
        Callable[[asyncio.Task[Any]], None]: task 完成回调。
    """

    def _callback(task: asyncio.Task[Any]) -> None:
        try:
            task.result()
        except asyncio.CancelledError:
            logger.info(
                "Assistant background run cancelled conversation_uuid={conversation_uuid}",
                conversation_uuid=conversation_uuid,
            )
        except Exception as exc:  # pragma: no cover - 防御性兜底
            logger.opt(exception=exc).error(
                "Assistant background run failed conversation_uuid={conversation_uuid}",
                conversation_uuid=conversation_uuid,
            )

    return _callback


async def _run_assistant_workflow_in_background(
        *,
        question: str,
        context: ConversationContext,
        stream_config: AssistantStreamConfig,
) -> None:
    """
    在后台执行 workflow，并把标准事件写入 Redis 运行态存储。

    Args:
        question: 用户问题文本。
        context: 会话上下文。
        stream_config: orchestrator 流式配置。

    Returns:
        None
    """

    aggregate_state = StreamAggregateState(
        last_flush_monotonic=time.monotonic(),
    )
    final_status = AssistantRunStatus.ERROR
    final_snapshot = _build_run_snapshot(
        aggregate_state=aggregate_state,
        assistant_message_uuid=context.assistant_message_uuid,
        status=AssistantRunStatus.RUNNING,
    )

    try:
        async for response in iterate_assistant_responses(
                question=question,
                config=stream_config,
        ):
            _append_stream_response_to_aggregate(
                aggregate_state=aggregate_state,
                response=response,
            )
            response_meta = response.meta if isinstance(response.meta, dict) else {}
            response_status = AssistantRunStatus.RUNNING
            if response.is_end:
                response_status = AssistantRunStatus(
                    str(response_meta.get("run_status") or AssistantRunStatus.SUCCESS.value)
                )
            current_snapshot = _build_run_snapshot(
                aggregate_state=aggregate_state,
                assistant_message_uuid=context.assistant_message_uuid,
                status=response_status,
            )
            stored_event = await asyncio.to_thread(
                RUN_EVENT_STORE.append_event,
                conversation_uuid=context.conversation_uuid,
                payload=response,
                snapshot=current_snapshot,
            )
            final_snapshot = _build_run_snapshot(
                aggregate_state=aggregate_state,
                assistant_message_uuid=context.assistant_message_uuid,
                status=response_status,
                last_event_id=stored_event.event_id,
            )
            if response_status == AssistantRunStatus.RUNNING:
                await _flush_stream_snapshot_if_due(
                    conversation_id=context.conversation_id,
                    assistant_message_uuid=context.assistant_message_uuid,
                    aggregate_state=aggregate_state,
                )
            else:
                final_status = response_status
                await _flush_stream_snapshot_if_due(
                    conversation_id=context.conversation_id,
                    assistant_message_uuid=context.assistant_message_uuid,
                    aggregate_state=aggregate_state,
                    force=True,
                )
    finally:
        await asyncio.to_thread(
            RUN_EVENT_STORE.finalize_run,
            conversation_uuid=context.conversation_uuid,
            final_status=final_status,
            final_snapshot=final_snapshot,
        )


def _build_run_stream_config(
        *,
        question: str,
        context: ConversationContext,
        workflow: Any,
        workflow_name: str,
        build_stream_config_func: Any,
        invoke_sync_func: Any,
        map_exception_func: Any,
        should_stream_token_func: Any,
        cancel_event: asyncio.Event,
) -> AssistantStreamConfig:
    """
    构造后台 run 使用的 orchestrator 配置。

    Args:
        question: 用户问题文本。
        context: 当前会话上下文。
        workflow: 已编译 workflow。
        workflow_name: 工作流名称。
        build_stream_config_func: tracing 配置构造函数。
        invoke_sync_func: 同步回退执行函数。
        map_exception_func: 异常映射函数。
        should_stream_token_func: token 输出判定函数。
        cancel_event: 本机取消事件。

    Returns:
        AssistantStreamConfig: 可直接驱动后台 run 的流式配置。
    """

    _ = question
    return AssistantStreamConfig(
        workflow=workflow,
        build_initial_state=lambda q: _build_initial_state(
            q,
            conversation_uuid=context.conversation_uuid,
            assistant_message_uuid=context.assistant_message_uuid,
            history_messages=context.history_messages,
            current_question=context.current_question,
            current_image_urls=context.current_image_urls,
            override_model_name=context.override_model_name,
            override_reasoning=context.override_reasoning,
        ),
        extract_final_content=lambda state: str(state.get("result") or ""),
        should_stream_token=should_stream_token_func,
        build_stream_config=build_stream_config_func,
        invoke_sync=invoke_sync_func,
        map_exception=map_exception_func,
        on_answer_completed=_build_assistant_message_callback(
            conversation_id=context.conversation_id,
            assistant_message_uuid=context.assistant_message_uuid,
        ),
        initial_emitted_events=context.initial_emitted_events,
        is_cancel_requested=lambda: (
                cancel_event.is_set()
                or RUN_EVENT_STORE.is_cancel_requested(
            conversation_uuid=context.conversation_uuid,
        )
        ),
    )


def _build_attach_streaming_response(
        *,
        conversation_uuid: str,
        last_event_id: str | None = None,
) -> StreamingResponse:
    """
    构造 attach SSE 响应。

    Args:
        conversation_uuid: 会话 UUID。
        last_event_id: 客户端已消费到的最后事件 ID。

    Returns:
        StreamingResponse: attach SSE 响应对象。
    """

    normalized_last_event_id = str(last_event_id or "").strip() or None

    async def _stream() -> Any:
        current_event_id = normalized_last_event_id or "0-0"
        if normalized_last_event_id is None:
            snapshot = await asyncio.to_thread(
                RUN_EVENT_STORE.get_snapshot,
                conversation_uuid=conversation_uuid,
            )
            if snapshot is not None:
                for snapshot_event in _build_snapshot_attach_events(
                        conversation_uuid=conversation_uuid,
                        snapshot=snapshot,
                ):
                    yield serialize_sse(snapshot_event)
                if snapshot.status != AssistantRunStatus.RUNNING:
                    terminal_events = await asyncio.to_thread(
                        RUN_EVENT_STORE.read_events,
                        conversation_uuid=conversation_uuid,
                        last_event_id="0-0",
                        block_ms=1,
                    )
                    emitted_end_event = False
                    for event in terminal_events:
                        if not _should_replay_terminal_event_after_snapshot(
                                response=event.payload,
                        ):
                            continue
                        yield serialize_sse(event.payload)
                        if event.payload.is_end:
                            return
                    if not emitted_end_event:
                        yield serialize_sse(
                            _build_terminal_attach_end_event(
                                finish_status=snapshot.status,
                            )
                        )
                        return
                if snapshot.last_event_id is not None:
                    current_event_id = snapshot.last_event_id

        while True:
            meta = await asyncio.to_thread(
                RUN_EVENT_STORE.get_run_meta,
                conversation_uuid=conversation_uuid,
            )
            if meta is None:
                break

            block_ms = (
                resolve_assistant_run_stream_block_ms()
                if meta.status == AssistantRunStatus.RUNNING
                else 1
            )
            events = await asyncio.to_thread(
                RUN_EVENT_STORE.read_events,
                conversation_uuid=conversation_uuid,
                last_event_id=current_event_id,
                block_ms=block_ms,
            )
            if not events:
                if meta.status != AssistantRunStatus.RUNNING:
                    break
                continue

            for event in events:
                current_event_id = event.event_id
                yield serialize_sse(event.payload)
                if event.payload.is_end:
                    return

    return StreamingResponse(
        _stream(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
        },
    )


def assistant_chat_submit(
        *,
        question: str,
        image_urls: list[str] | None = None,
        conversation_uuid: str | None = None,
        model_name: str,
        reasoning_enabled: bool,
) -> AssistantRunSubmitResponse:
    """
    提交管理助手聊天请求并创建后台 run。

    Args:
        question: 用户输入问题文本。
        image_urls: 用户上传图片 URL 列表。
        conversation_uuid: 可选会话 UUID；为空时创建新会话。
        model_name: 用户手动选择的前端自定义模型名；后端会映射为真实模型名称。
        reasoning_enabled: 是否开启深度思考；由前端按当前所选模型显式传入。

    Returns:
        AssistantRunSubmitResponse: 新建或已存在运行态的响应。
    """

    resolved_model_name, resolved_reasoning_enabled = _resolve_admin_submit_model_overrides(
        custom_model_name=model_name,
        reasoning_enabled=reasoning_enabled,
        image_urls=image_urls,
    )

    current_user_id = get_user_id()
    assistant_message_uuid = str(uuid.uuid4())
    context = _prepare_conversation_context(
        question=question,
        image_urls=image_urls,
        user_id=current_user_id,
        conversation_uuid=conversation_uuid,
        assistant_message_uuid=assistant_message_uuid,
        override_model_name=resolved_model_name,
        override_reasoning=resolved_reasoning_enabled,
    )

    created_meta = RUN_EVENT_STORE.create_run(
        conversation_uuid=context.conversation_uuid,
        user_id=current_user_id,
        conversation_type=ConversationType.ADMIN.value,
        assistant_message_uuid=context.assistant_message_uuid,
    )
    if created_meta is None:
        active_meta = RUN_EVENT_STORE.get_run_meta(
            conversation_uuid=context.conversation_uuid,
        )
        if active_meta is not None and active_meta.status == AssistantRunStatus.RUNNING:
            raise ServiceException(
                code=ResponseCode.CONFLICT,
                message="当前会话已有正在输出的回答",
                data=AssistantRunSubmitResponse(
                    conversation_uuid=context.conversation_uuid,
                    message_uuid=active_meta.assistant_message_uuid,
                    run_status=active_meta.status,
                ).model_dump(),
            )
        raise ServiceException(
            code=ResponseCode.SERVICE_UNAVAILABLE,
            message="创建助手运行态失败",
        )

    _persist_user_message(
        conversation_id=context.conversation_id,
        content=build_user_image_markdown_content(
            context.current_question,
            context.current_image_urls,
        ),
    )
    _create_placeholder_assistant_message(
        conversation_id=context.conversation_id,
        message_uuid=context.assistant_message_uuid,
    )

    cancel_event = asyncio.Event()
    stream_config = _build_run_stream_config(
        question=context.current_question,
        context=context,
        workflow=ADMIN_WORKFLOW,
        workflow_name=ADMIN_WORKFLOW_NAME,
        build_stream_config_func=_build_stream_config,
        invoke_sync_func=_invoke_admin_workflow,
        map_exception_func=_map_exception,
        should_stream_token_func=_should_stream_token,
        cancel_event=cancel_event,
    )
    background_task = asyncio.create_task(
        _run_assistant_workflow_in_background(
            question=context.current_question,
            context=context,
            stream_config=stream_config,
        )
    )
    background_task.add_done_callback(
        _build_background_run_done_callback(
            conversation_uuid=context.conversation_uuid,
        )
    )
    RUN_EVENT_STORE.register_local_handle(
        conversation_uuid=context.conversation_uuid,
        handle=LocalRunHandle(
            task=background_task,
            cancel_event=cancel_event,
        ),
    )
    return AssistantRunSubmitResponse(
        conversation_uuid=context.conversation_uuid,
        message_uuid=context.assistant_message_uuid,
        run_status=AssistantRunStatus.RUNNING,
    )


def assistant_chat_stream(
        *,
        conversation_uuid: str,
        last_event_id: str | None = None,
) -> StreamingResponse:
    """
    attach 到指定会话当前的流式运行。

    Args:
        conversation_uuid: 会话 UUID。
        last_event_id: 客户端已消费的最后事件 ID。

    Returns:
        StreamingResponse: SSE attach 流。
    """

    current_user_id = get_user_id()
    _load_admin_conversation(
        conversation_uuid=conversation_uuid,
        user_id=current_user_id,
    )
    meta = RUN_EVENT_STORE.get_run_meta(conversation_uuid=conversation_uuid)
    if meta is None:
        raise ServiceException(
            code=ResponseCode.NOT_FOUND,
            message="当前会话没有可连接的流式输出",
        )
    return _build_attach_streaming_response(
        conversation_uuid=conversation_uuid,
        last_event_id=last_event_id,
    )


def assistant_chat_stop(
        *,
        conversation_uuid: str,
) -> AssistantRunStopResponse:
    """
    请求停止指定会话当前的流式运行。

    Args:
        conversation_uuid: 会话 UUID。

    Returns:
        AssistantRunStopResponse: 停止请求响应。
    """

    current_user_id = get_user_id()
    _load_admin_conversation(
        conversation_uuid=conversation_uuid,
        user_id=current_user_id,
    )
    meta = RUN_EVENT_STORE.request_cancel(conversation_uuid=conversation_uuid)
    if meta is None or meta.status != AssistantRunStatus.RUNNING:
        raise ServiceException(
            code=ResponseCode.NOT_FOUND,
            message="当前会话没有运行中的输出",
        )
    return AssistantRunStopResponse(
        conversation_uuid=conversation_uuid,
        message_uuid=meta.assistant_message_uuid,
        run_status=meta.status,
        stop_requested=True,
    )


def assistant_message_tts_stream(
        *,
        message_uuid: str,
) -> StreamingResponse:
    """
    管理助手消息转语音（HTTP chunked audio stream）。

    说明：
    - 先基于 `message_uuid` 做消息存在性、会话归属、消息角色校验；
    - 校验通过后建立上游 Volcengine 双向 TTS websocket；
    - 下游以音频字节流（chunked）持续返回给前端。
    """

    current_user_id = get_user_id()
    tts_stream = build_message_tts_stream(
        message_uuid=message_uuid,
        user_id=current_user_id,
        conversation_type=ConversationType.ADMIN,
    )
    return StreamingResponse(
        tts_stream.audio_stream,
        media_type=tts_stream.media_type,
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
        },
    )


def load_history(
        *,
        conversation_id: str,
        limit: int = 50,
) -> list[ChatHistoryMessage]:
    """
    加载会话历史消息（最近窗口）。

    读取策略：
    1. 先按创建时间倒序读取最近 N 条；
    2. 再反转为正序，保证喂给模型时上下文顺序正确。
    """

    message_documents = list_messages(
        conversation_id=conversation_id,
        limit=limit,
        ascending=False,
    )
    history_messages: list[ChatHistoryMessage] = [
        HumanMessage(content=document.content)
        if document.role == MessageRole.USER
        else AIMessage(content=document.content)
        for document in message_documents
    ]
    return list(reversed(history_messages))


def conversation_list(
        *,
        page_request: PageRequest,
) -> tuple[list[ConversationListItem], int]:
    """
    分页查询当前用户的管理助手会话列表。

    仅返回会话 UUID 与标题，不返回会话内部主键等冗余字段。

    Args:
        page_request: 分页请求参数。

    Returns:
        tuple[list[ConversationListItem], int]:
            - rows: 当前页会话列表项模型。
            - total: 会话总数。
    """

    current_user_id = get_user_id()
    return list_admin_conversations(
        user_id=current_user_id,
        page_num=page_request.page_num,
        page_size=page_request.page_size,
    )


def conversation_messages(
        *,
        conversation_uuid: str,
        page_request: PageRequest,
) -> tuple[list[ConversationMessageResponse], int]:
    """
    分页查询当前用户某个管理助手会话的历史消息与总数。

    分页规则：
    1. page_num=1 返回最近一页（最新 50 条）；
    2. 每页内部按时间升序返回，便于前端直接渲染。
    """

    normalized_uuid = conversation_uuid.strip()
    if not normalized_uuid:
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="会话UUID不能为空")

    current_user_id = get_user_id()
    conversation_id = _load_admin_conversation(
        conversation_uuid=normalized_uuid,
        user_id=current_user_id,
    )

    skip = (page_request.page_num - 1) * page_request.page_size
    total = count_messages(conversation_id=conversation_id)
    message_documents = list_messages(
        conversation_id=conversation_id,
        limit=page_request.page_size,
        skip=skip,
        ascending=False,
    )

    result: list[ConversationMessageResponse] = []
    for document in reversed(message_documents):
        role = "user" if document.role == MessageRole.USER else "ai"
        payload: dict[str, Any] = {
            "id": document.uuid,
            "role": role,
            "content": document.content,
        }
        if role == "ai":
            payload["status"] = document.status.value
            raw_thinking = getattr(document, "thinking", None)
            if isinstance(raw_thinking, str) and raw_thinking.strip():
                payload["thinking"] = raw_thinking
        result.append(ConversationMessageResponse.model_validate(payload))
    return result, total


def delete_conversation(
        *,
        conversation_uuid: str,
) -> None:
    """
    删除当前用户的管理助手会话。

    Args:
        conversation_uuid: 会话 UUID。

    Raises:
        ServiceException:
            - BAD_REQUEST: 会话 UUID 为空；
            - NOT_FOUND: 会话不存在或无权限；
            - DATABASE_ERROR: 数据库异常。
    """

    normalized_uuid = conversation_uuid.strip()
    if not normalized_uuid:
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="会话UUID不能为空")

    current_user_id = get_user_id()
    deleted = delete_admin_conversation(
        conversation_uuid=normalized_uuid,
        user_id=current_user_id,
    )
    if not deleted:
        raise ServiceException(code=ResponseCode.NOT_FOUND, message="会话不存在")


def update_conversation_title(
        *,
        conversation_uuid: str,
        title: str,
) -> str:
    """
    更新当前用户管理助手会话标题。

    Args:
        conversation_uuid: 会话 UUID。
        title: 新标题。

    Returns:
        str: 归一化后的标题（strip 后）。

    Raises:
        ServiceException:
            - BAD_REQUEST: 会话 UUID 或标题为空；
            - NOT_FOUND: 会话不存在或无权限；
            - DATABASE_ERROR: 数据库异常。
    """

    normalized_uuid = conversation_uuid.strip()
    if not normalized_uuid:
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="会话UUID不能为空")

    normalized_title = title.strip()
    if not normalized_title:
        raise ServiceException(code=ResponseCode.BAD_REQUEST, message="会话标题不能为空")

    current_user_id = get_user_id()
    updated = update_admin_conversation_title(
        conversation_uuid=normalized_uuid,
        user_id=current_user_id,
        title=normalized_title,
    )
    if not updated:
        raise ServiceException(code=ResponseCode.NOT_FOUND, message="会话不存在")

    return normalized_title


def generate_title(question: str) -> str:
    """根据用户输入生成标题。"""

    system_prompt = load_managed_prompt(
        GENERATE_TITLE_PROMPT_KEY,
        local_prompt_path=GENERATE_TITLE_PROMPT_LOCAL_PATH,
    ).strip()
    if not question:
        return "未知标题"

    llm_model = create_agent_title_llm(
        temperature=1.0
    )
    messages = [
        SystemMessage(content=system_prompt),
        HumanMessage(content=question),
    ]
    response = llm_model.invoke(
        messages
    )
    content = getattr(response, "content", "")
    if isinstance(content, str) and content.strip():
        return content.strip()
    return "未知标题"
