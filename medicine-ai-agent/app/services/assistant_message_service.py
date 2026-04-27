from __future__ import annotations

import asyncio
import threading
from typing import Any

from loguru import logger

from app.schemas.assistant_run import AssistantRunStatus
from app.schemas.document.message import MessageCard, MessageRole, MessageStatus
from app.services.memory_summary_service import refresh_conversation_summary_if_needed
from app.services.message_service import add_message, update_assistant_message
from app.utils.assistant_message_utils import resolve_message_status_from_finish_status

# 空回复时写入的统一降级文案。
EMPTY_ASSISTANT_ANSWER_FALLBACK = "服务暂时不可用，请稍后重试。"


def _schedule_background_task(
        *,
        task_name: str,
        func: Any,
        kwargs: dict[str, Any],
) -> None:
    """
    功能描述：
        使用守护线程调度后台任务。

    参数说明：
        task_name (str): 任务名称，用于日志追踪。
        func (Any): 要执行的函数。
        kwargs (dict[str, Any]): 关键字参数。

    返回值：
        None

    异常说明：
        无；后台异常仅记录 warning。
    """

    def _runner() -> None:
        try:
            func(**kwargs)
        except Exception as exc:  # pragma: no cover - 防御性兜底
            logger.opt(exception=exc).warning("Background task failed task_name={task_name}", task_name=task_name)

    thread = threading.Thread(target=_runner, daemon=True)
    thread.start()


def persist_user_message(
        *,
        conversation_id: str,
        content: str,
        cards: list[MessageCard | dict[str, Any]] | None = None,
) -> None:
    """
    功能描述：
        后台持久化 user 消息。

    参数说明：
        conversation_id (str): 会话 ID。
        content (str): 用户消息文本；卡片消息场景允许为空字符串。
        cards (list[MessageCard | dict[str, Any]] | None): 用户消息携带的结构化卡片列表。

    返回值：
        None

    异常说明：
        无。
    """

    add_message(
        conversation_id=conversation_id,
        role="user",
        content=content,
        cards=cards,
    )


def create_placeholder_assistant_message(
        *,
        conversation_id: str,
        message_uuid: str,
) -> None:
    """
    功能描述：
        创建一条 AI 占位消息。

    参数说明：
        conversation_id (str): 会话 ID。
        message_uuid (str): 当前 AI 消息 UUID。

    返回值：
        None

    异常说明：
        无。
    """

    add_message(
        conversation_id=conversation_id,
        role=MessageRole.AI,
        status=MessageStatus.STREAMING,
        content="",
        message_uuid=message_uuid,
    )


def persist_assistant_stream_snapshot(
        *,
        conversation_id: str,
        message_uuid: str,
        answer_text: str,
        thinking_text: str | None,
) -> None:
    """
    功能描述：
        更新 AI 消息的流式快照。

    参数说明：
        conversation_id (str): 会话 ID。
        message_uuid (str): 当前 AI 消息 UUID。
        answer_text (str): 当前聚合回答文本。
        thinking_text (str | None): 当前聚合思考文本。

    返回值：
        None

    异常说明：
        无。
    """

    update_assistant_message(
        conversation_id=conversation_id,
        message_uuid=message_uuid,
        status=MessageStatus.STREAMING,
        content=answer_text,
        thinking=thinking_text,
    )


def persist_assistant_message(
        *,
        conversation_id: str,
        message_uuid: str,
        answer_text: str,
        status: MessageStatus | str,
        thinking_text: str | None = None,
        cards: list[dict[str, Any]] | None = None,
) -> None:
    """
    功能描述：
        持久化 AI 消息并触发追踪与摘要刷新。

    参数说明：
        conversation_id (str): 会话 ID（Mongo ObjectId 字符串）。
        message_uuid (str): 本轮 AI 消息 UUID。
        answer_text (str): AI 最终回复文本。
        status (MessageStatus | str): 消息状态。
        thinking_text (str | None): 可选深度思考文本。
        cards (list[dict[str, Any]] | None): 可选结构化卡片列表。

    返回值：
        None

    异常说明：
        无；数据库与下游异常由后台任务兜底捕获，不向主链路抛出。
    """

    resolved_status = MessageStatus(status)
    update_assistant_message(
        conversation_id=conversation_id,
        message_uuid=message_uuid,
        status=resolved_status,
        content=answer_text,
        thinking=thinking_text,
        cards=cards,
    )
    if resolved_status == MessageStatus.SUCCESS:
        _schedule_background_task(
            task_name="refresh_conversation_summary",
            func=refresh_conversation_summary_if_needed,
            kwargs={
                "conversation_id": conversation_id,
            },
        )


def build_assistant_message_callback(
        *,
        conversation_id: str,
        assistant_message_uuid: str,
):
    """
    功能描述：
        构建“流结束后写入 AI 消息”的异步回调。

    参数说明：
        conversation_id (str): 当前会话 ID（Mongo ObjectId 字符串）。
        assistant_message_uuid (str): 本轮 AI 消息 UUID。

    返回值：
        Callable[..., Awaitable[None]]: 可直接交给 orchestrator 的异步回调函数。

    异常说明：
        无。
    """

    async def _callback(
            answer_text: str,
            thinking_text: str | None = None,
            cards: list[dict[str, Any]] | None = None,
            finish_status: AssistantRunStatus = AssistantRunStatus.SUCCESS,
    ) -> None:
        normalized_answer = str(answer_text or "").strip()
        resolved_cards = cards
        has_cards = bool(resolved_cards)
        resolved_status = resolve_message_status_from_finish_status(
            finish_status=finish_status,
        )
        if not normalized_answer:
            if has_cards or resolved_status == MessageStatus.CANCELLED:
                normalized_answer = ""
            else:
                normalized_answer = EMPTY_ASSISTANT_ANSWER_FALLBACK
                resolved_status = MessageStatus.ERROR

        persist_kwargs = {
            "conversation_id": conversation_id,
            "message_uuid": assistant_message_uuid,
            "answer_text": normalized_answer,
            "thinking_text": thinking_text,
            "cards": resolved_cards,
            "status": resolved_status,
        }
        if has_cards:
            await asyncio.to_thread(
                persist_assistant_message,
                **persist_kwargs,
            )
            return

        _schedule_background_task(
            task_name="persist_assistant_message",
            func=persist_assistant_message,
            kwargs=persist_kwargs,
        )

    return _callback
