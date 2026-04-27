from __future__ import annotations

from typing import Any, Sequence

from langchain_core.messages import HumanMessage, SystemMessage
from loguru import logger

from app.core.config_sync import (
    create_agent_summary_llm,
    resolve_agent_summary_max_tokens,
    resolve_agent_summary_model_name,
)
from app.schemas.document.message import MessageDocument, MessageRole
from app.services.memory_service import (
    resolve_assistant_summary_trigger_window,
)
from app.services.message_service import (
    count_summarizable_messages,
    list_latest_summarizable_messages,
)
from app.services.summary_service import (
    get_conversation_summary,
    save_conversation_summary,
)
from app.utils.assistant_message_utils import build_user_message_workflow_text
from app.utils.prompt_utils import load_managed_prompt
from app.utils.token_utills import TokenUtils

# 聊天历史增量摘要提示词业务键。
_SUMMARY_UPDATE_PROMPT_KEY = "system_summary_update_prompt"
# 聊天历史压缩摘要提示词业务键。
_SUMMARY_COMPRESS_PROMPT_KEY = "system_summary_compress_prompt"
# 聊天历史增量摘要提示词本地回退路径。
_SUMMARY_UPDATE_PROMPT_LOCAL_PATH = "_system/summary_update.md"
# 聊天历史压缩摘要提示词本地回退路径。
_SUMMARY_COMPRESS_PROMPT_LOCAL_PATH = "_system/summary_compress.md"


def _load_summary_update_prompt() -> str:
    """读取聊天历史增量摘要提示词。"""

    return load_managed_prompt(
        _SUMMARY_UPDATE_PROMPT_KEY,
        local_prompt_path=_SUMMARY_UPDATE_PROMPT_LOCAL_PATH,
    ).strip()


def _load_summary_compress_prompt() -> str:
    """读取聊天历史压缩摘要提示词。"""

    return load_managed_prompt(
        _SUMMARY_COMPRESS_PROMPT_KEY,
        local_prompt_path=_SUMMARY_COMPRESS_PROMPT_LOCAL_PATH,
    ).strip()


def _extract_message_content_text(raw_content: Any) -> str:
    """
    功能描述：
        归一化 LLM 响应 `content`，提取可用纯文本。

    参数说明：
        raw_content (Any): 模型返回的 `content` 字段，可能是字符串、分片列表或其他结构。

    返回值：
        str: 归一化后的文本内容；无法解析时返回空字符串。

    异常说明：
        无。
    """

    if isinstance(raw_content, str):
        return raw_content.strip()

    if not isinstance(raw_content, list):
        return ""

    pieces: list[str] = []
    for item in raw_content:
        if isinstance(item, str):
            normalized_piece = item.strip()
            if normalized_piece:
                pieces.append(normalized_piece)
            continue
        if isinstance(item, dict):
            candidate = item.get("text")
            if isinstance(candidate, str) and candidate.strip():
                pieces.append(candidate.strip())
    return "\n".join(pieces).strip()


def _call_summary_llm(
        *,
        system_prompt: str,
        user_prompt: str,
) -> str:
    """
    功能描述：
        调用摘要模型，返回单次生成的摘要文本。

    参数说明：
        system_prompt (str): 系统提示词。
        user_prompt (str): 用户输入内容（摘要上下文载荷）。
        model_name (str | None): 用于 token 统计的生效模型名。

    返回值：
        str: 模型生成的摘要文本；空响应返回空字符串。

    异常说明：
        RuntimeError/ValueError:
            模型配置不合法或调用失败时由底层抛出。
    """

    llm_model = create_agent_summary_llm(
        temperature=0.1,
    )
    response = llm_model.invoke(
        [
            SystemMessage(content=system_prompt),
            HumanMessage(content=user_prompt),
        ]
    )
    return _extract_message_content_text(getattr(response, "content", ""))


def _format_messages_for_summary(messages: Sequence[MessageDocument]) -> str:
    """
    功能描述：
        将消息文档序列化为摘要模型可消费的对话文本。

    参数说明：
        messages (Sequence[MessageDocument]): 待总结消息，要求按时间正序输入。

    返回值：
        str: 结构化对话文本，每行格式为 `用户/助手: 内容`。

    异常说明：
        无。
    """

    formatted_lines: list[str] = []
    for message in messages:
        speaker = "用户" if message.role == MessageRole.USER else "助手"
        content = (
            build_user_message_workflow_text(message.content, message.cards)
            if message.role == MessageRole.USER
            else str(message.content or "").strip()
        )
        if not content:
            continue
        formatted_lines.append(f"{speaker}: {content}")
    return "\n".join(formatted_lines).strip()


def _count_tokens_with_fallback(
        *,
        text: str,
        model_name: str | None,
) -> int:
    """
    功能描述：
        统计文本 token 数，优先按模型编码统计，失败时回退默认编码。

    参数说明：
        text (str): 目标文本。
        model_name (str | None): 模型名，用于推断编码器。

    返回值：
        int: token 统计结果。

    异常说明：
        ServiceException:
            - 当 `tiktoken` 不可用且无法统计时由底层抛出。
    """

    try:
        return TokenUtils.count_tokens(text, model_name=model_name)
    except Exception:
        return TokenUtils.count_tokens(text)


def _truncate_text_by_tokens(
        *,
        text: str,
        max_tokens: int | None,
        model_name: str | None,
) -> str:
    """
    功能描述：
        按 token 数对文本执行硬截断，确保输出不超过上限。

    参数说明：
        text (str): 原始文本。
        max_tokens (int | None): 最大 token 数；``None``、``0`` 或负数表示不限制。
        model_name (str | None): 模型名，用于推断编码器。

    返回值：
        str: 截断后的文本。

    异常说明：
        ServiceException:
            - `tiktoken` 不可用且无法获取编码器时由底层抛出。
    """

    if max_tokens is None or max_tokens <= 0:
        return text
    try:
        encoder = TokenUtils.get_encoder(model_name=model_name)
    except Exception:
        encoder = TokenUtils.get_encoder()
    tokens = encoder.encode(text)
    if len(tokens) <= max_tokens:
        return text
    return encoder.decode(tokens[:max_tokens]).strip()


def _enforce_summary_token_budget(
        *,
        summary_text: str,
        max_tokens: int | None,
        model_name: str | None,
) -> tuple[str, int]:
    """
    功能描述：
        对摘要文本执行 token 预算控制：
        1) 超限先走“摘要再压缩”；
        2) 仍超限执行 token 级硬截断。

    参数说明：
        summary_text (str): 原始摘要文本。
        max_tokens (int | None): 摘要 token 上限；``None``、``0`` 或负数表示不限制。
        model_name (str | None): 摘要模型名。

    返回值：
        tuple[str, int]:
            - 生效摘要文本；
            - 生效摘要 token 数。

    异常说明：
        RuntimeError/ValueError:
            压缩模型调用失败时由底层抛出。
    """

    normalized_summary = summary_text.strip()
    if not normalized_summary:
        return "", 0

    token_count = _count_tokens_with_fallback(
        text=normalized_summary,
        model_name=model_name,
    )
    if max_tokens is None or max_tokens <= 0:
        return normalized_summary, token_count
    if token_count <= max_tokens:
        return normalized_summary, token_count

    compressed_candidate = _call_summary_llm(
        system_prompt=_load_summary_compress_prompt(),
        user_prompt=normalized_summary,
    ).strip()
    if compressed_candidate:
        normalized_summary = compressed_candidate
    token_count = _count_tokens_with_fallback(
        text=normalized_summary,
        model_name=model_name,
    )
    if token_count <= max_tokens:
        return normalized_summary, token_count

    truncated_summary = _truncate_text_by_tokens(
        text=normalized_summary,
        max_tokens=max_tokens,
        model_name=model_name,
    )
    truncated_token_count = _count_tokens_with_fallback(
        text=truncated_summary,
        model_name=model_name,
    )
    return truncated_summary, truncated_token_count


def _build_summary_update_payload(
        *,
        previous_summary: str | None,
        recent_dialogue_text: str,
        recent_dialogue_count: int,
) -> str:
    """
    功能描述：
        构造“增量摘要更新”提示词输入载荷。

    参数说明：
        previous_summary (str | None): 当前会话已存在摘要；为空表示首次摘要。
        recent_dialogue_text (str): 本次参与摘要的最新对话片段。
        recent_dialogue_count (int): 本次参与摘要的消息条数。

    返回值：
        str: 供摘要模型消费的输入文本。

    异常说明：
        无。
    """

    normalized_previous_summary = (previous_summary or "").strip() or "无"
    return (
        "请基于已有摘要与最新对话片段，生成新的单条会话摘要。\n"
        "要求：保留关键事实、上下文约束、已确认结论与待办事项；禁止编造。\n\n"
        f"[已有摘要]\n{normalized_previous_summary}\n\n"
        f"[最新对话片段，共 {recent_dialogue_count} 条]\n{recent_dialogue_text}"
    )


def _resolve_summary_budget_max_tokens() -> int | None:
    """
    功能描述：
        解析聊天历史总结的摘要文本 token 预算。

    参数说明：
        无。

    返回值：
        int | None: 生效的摘要 token 上限；优先取 Redis 槽位，未配置时回退本地环境值；
            ``None`` 表示不限制。

    异常说明：
        无。
    """

    return resolve_agent_summary_max_tokens()


def refresh_conversation_summary_if_needed(
        *,
        conversation_id: str,
) -> None:
    """
    功能描述：
        按阈值刷新会话摘要（异步任务入口）。

    执行规则：
        1. 仅统计 `user/ai + success` 消息；
        2. 当未总结消息数达到阈值时触发；
        3. 若积压超阈值，仅总结“最新阈值窗口”并直接推进游标（更早未总结内容永久丢弃）；
        4. 保存时使用 CAS（期望游标）防并发覆盖；
        5. 摘要文本执行 token 预算控制后落库。

    参数说明：
        conversation_id (str): 会话 Mongo ObjectId 字符串。

    返回值：
        None

    异常说明：
        无（函数内部吞并异常并记录 warning，避免影响主链路）。
    """

    normalized_conversation_id = conversation_id.strip()
    if not normalized_conversation_id:
        return

    try:
        summary_document = get_conversation_summary(conversation_id=normalized_conversation_id)
        expected_cursor = (
            summary_document.last_summarized_message_id
            if summary_document is not None
            else None
        )
        trigger_window = resolve_assistant_summary_trigger_window()
        pending_count = count_summarizable_messages(
            conversation_id=normalized_conversation_id,
            after_message_id=expected_cursor,
        )
        if pending_count < trigger_window:
            return

        latest_messages = list_latest_summarizable_messages(
            conversation_id=normalized_conversation_id,
            limit=trigger_window,
            after_message_id=expected_cursor,
        )
        if not latest_messages:
            return

        recent_dialogue_text = _format_messages_for_summary(latest_messages)
        if not recent_dialogue_text:
            return

        previous_summary = (
            summary_document.summary_content
            if summary_document is not None and summary_document.status == "success"
            else None
        )
        summary_model_name = resolve_agent_summary_model_name()
        update_payload = _build_summary_update_payload(
            previous_summary=previous_summary,
            recent_dialogue_text=recent_dialogue_text,
            recent_dialogue_count=len(latest_messages),
        )
        generated_summary = _call_summary_llm(
            system_prompt=_load_summary_update_prompt(),
            user_prompt=update_payload,
        )
        normalized_summary, summary_token_count = _enforce_summary_token_budget(
            summary_text=generated_summary,
            max_tokens=_resolve_summary_budget_max_tokens(),
            model_name=summary_model_name,
        )
        if not normalized_summary:
            return

        last_message = latest_messages[-1]
        if last_message.id is None:
            return
        next_summary_version = (
            summary_document.summary_version + 1
            if summary_document is not None
            else 1
        )
        save_conversation_summary(
            conversation_id=normalized_conversation_id,
            summary_content=normalized_summary,
            last_summarized_message_id=last_message.id,
            last_summarized_message_uuid=last_message.uuid,
            summary_version=next_summary_version,
            summary_token_count=summary_token_count,
            status="success",
            expected_last_summarized_message_id=expected_cursor,
        )
    except Exception as exc:  # pragma: no cover - 防御性兜底
        logger.opt(exception=exc).warning(
            "refresh_conversation_summary_if_needed failed conversation_id={conversation_id}",
            conversation_id=normalized_conversation_id,
        )
