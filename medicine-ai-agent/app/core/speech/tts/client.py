from __future__ import annotations

import json
import time
import uuid
from collections.abc import AsyncIterator
from dataclasses import dataclass
from typing import Any

import websockets
from loguru import logger

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.core.speech.runtime import (
    register_active_tts_stream,
    unregister_active_tts_stream,
)
from app.core.speech.tts.config import (
    VolcengineTtsConfig,
    build_volcengine_tts_headers,
    is_volcengine_tts_startup_connect_enabled,
    is_volcengine_tts_startup_fail_fast_enabled,
    resolve_volcengine_tts_config,
)
from app.core.speech.tts.text_sanitizer import TtsTextSanitizer
from app.core.speech.volcengine_speech_protocol import (
    EventType,
    MsgType,
    finish_connection,
    finish_session,
    receive_message,
    start_connection,
    start_session,
    task_request,
    wait_for_event,
)
from app.schemas.document.conversation import ConversationDocument, ConversationType
from app.schemas.document.message import MessageRole
from app.services.conversation_service import (
    get_admin_conversation_by_id,
    get_client_conversation_by_id,
)
from app.services.message_service import get_message_by_uuid
from app.services.message_tts_usage_service import add_message_tts_usage

# WebSocket 单帧最大载荷（bytes）。
MAX_WS_MESSAGE_SIZE = 10 * 1024 * 1024
# 文本被截断时的固定前缀模板。
DEFAULT_TTS_TRUNCATION_PREFIX_TEMPLATE = "文本太多了，这边只读前{max_chars}个字符。"

# 不同编码对应的 HTTP 媒体类型映射表。
_AUDIO_MEDIA_TYPE_BY_ENCODING = {
    "mp3": "audio/mpeg",
    "wav": "audio/wav",
    "pcm": "audio/L16",
    "ogg": "audio/ogg",
}

# 仅 TTS 1.0 资源支持 enable_timestamp。
_TIMESTAMP_SUPPORTED_RESOURCE_IDS = {
    "seed-tts-1.0",
    "seed-tts-1.0-concurr",
    "volc.service_type.10029",
    "volc.service_type.10048",
}


@dataclass(frozen=True)
class MessageTtsStream:
    """
    消息转语音的流式输出封装。

    Attributes:
        audio_stream: 音频字节异步迭代器。
        media_type: 音频 MIME 类型。
    """

    audio_stream: AsyncIterator[bytes]
    media_type: str


@dataclass(frozen=True)
class PreparedTtsTextDetail:
    """
    TTS 文本预处理结果。

    Attributes:
        raw_text: 原始文本。
        sanitized_text: 清洗后的文本。
        sent_text: 最终发送给上游 TTS 的文本。
        source_text_chars: 原始文本字符数。
        sanitized_text_chars: 清洗后字符数。
        billable_chars: 按发送文本计费字符数。
        max_text_chars: 正文最大可发送字符数限制，不包含截断提示前缀。
        is_truncated: 是否发生截断。
    """

    raw_text: str
    sanitized_text: str
    sent_text: str
    source_text_chars: int
    sanitized_text_chars: int
    billable_chars: int
    max_text_chars: int
    is_truncated: bool


@dataclass(frozen=True)
class TtsMessageContext:
    """
    消息转语音的已校验消息上下文。

    Attributes:
        message_uuid: 消息 UUID。
        conversation_id: 会话 ID。
        conversation_uuid: 会话 UUID。
        user_id: 用户 ID。
        raw_text: 原始消息文本。
    """

    message_uuid: str
    conversation_id: str
    conversation_uuid: str
    user_id: int
    raw_text: str


@dataclass(frozen=True)
class TtsUsageContext:
    """
    TTS 成功计量落库所需上下文。

    Attributes:
        usage_uuid: 本次调用明细 UUID。
        message_uuid: 消息 UUID。
        conversation_id: 会话 ID。
        conversation_uuid: 会话 UUID。
        user_id: 用户 ID。
        sent_text: 实际送入 TTS 的文本。
        source_text_chars: 原始字符数。
        sanitized_text_chars: 清洗后字符数。
        billable_chars: 计费字符数。
        max_text_chars: 最大字符限制。
        is_truncated: 是否发生截断。
    """

    usage_uuid: str
    message_uuid: str
    conversation_id: str
    conversation_uuid: str
    user_id: int
    sent_text: str
    source_text_chars: int
    sanitized_text_chars: int
    billable_chars: int
    max_text_chars: int
    is_truncated: bool


class _ActiveTtsStreamHandle:
    """运行中的 TTS 流句柄，用于配置刷新时中断上游连接。"""

    def __init__(self, *, connect_id: str, session_id: str) -> None:
        self.connect_id = connect_id
        self.session_id = session_id
        self._websocket: Any | None = None
        self._interrupted = False

    @property
    def interrupted(self) -> bool:
        return self._interrupted

    def attach_websocket(self, websocket: Any) -> None:
        self._websocket = websocket

    async def interrupt_due_to_config_refresh(self) -> None:
        if self._interrupted:
            return
        self._interrupted = True
        websocket = self._websocket
        if websocket is None:
            return
        try:
            await websocket.close()
        except Exception:
            pass


def _supports_timestamp_for_resource(resource_id: str) -> bool:
    """
    判断当前资源是否支持 `enable_timestamp`。

    火山双向 TTS 文档说明该参数仅适用于 TTS 1.0 资源。
    """

    normalized = resource_id.strip().lower()
    if not normalized:
        return False
    return normalized in _TIMESTAMP_SUPPORTED_RESOURCE_IDS


def resolve_audio_media_type(encoding: str) -> str:
    """
    根据音频编码推导 HTTP `Content-Type`。

    Args:
        encoding: 音频编码名称（如 mp3/wav/pcm/ogg）。

    Returns:
        str: 对应的媒体类型；未知编码返回 `application/octet-stream`。
    """

    normalized = encoding.strip().lower()
    if not normalized:
        return "application/octet-stream"
    return _AUDIO_MEDIA_TYPE_BY_ENCODING.get(normalized, "application/octet-stream")


def prepare_tts_text(
        *,
        raw_text: str,
        max_text_chars: int,
) -> PreparedTtsTextDetail:
    """
    对待合成文本执行发送前处理。

    处理规则：
    1. 先做白名单清洗，仅保留可播报文本；
    2. 清洗后为空则拒绝转语音；
    3. 超过 `max_text_chars` 时，只保留前 `max_text_chars` 个正文字符；
    4. 截断提示前缀单独追加，不占用正文字符额度。

    Args:
        raw_text: 原始消息文本。
        max_text_chars: 允许送入 TTS 的最大字符数。

    Returns:
        PreparedTtsTextDetail: 预处理细节与最终发送文本。

    Raises:
        ServiceException: 清洗后文本为空时抛出。
    """

    sanitized_text = TtsTextSanitizer.sanitize_text(raw_text)
    if not sanitized_text:
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message="消息清洗后为空，无法转语音",
        )

    source_text_chars = len(raw_text)
    sanitized_text_chars = len(sanitized_text)
    if sanitized_text_chars <= max_text_chars:
        return PreparedTtsTextDetail(
            raw_text=raw_text,
            sanitized_text=sanitized_text,
            sent_text=sanitized_text,
            source_text_chars=source_text_chars,
            sanitized_text_chars=sanitized_text_chars,
            billable_chars=len(sanitized_text),
            max_text_chars=max_text_chars,
            is_truncated=False,
        )

    truncated_body_text = sanitized_text[:max_text_chars]
    truncation_prefix = DEFAULT_TTS_TRUNCATION_PREFIX_TEMPLATE.format(max_chars=max_text_chars).strip()
    if truncation_prefix:
        final_text = f"{truncation_prefix}\n{truncated_body_text}"
    else:
        final_text = truncated_body_text

    logger.info(
        "Volcengine TTS input text truncated max_chars={max_chars} source_chars={source_chars} body_chars={body_chars} final_chars={final_chars}",
        max_chars=max_text_chars,
        source_chars=sanitized_text_chars,
        body_chars=len(truncated_body_text),
        final_chars=len(final_text),
    )
    return PreparedTtsTextDetail(
        raw_text=raw_text,
        sanitized_text=sanitized_text,
        sent_text=final_text,
        source_text_chars=source_text_chars,
        sanitized_text_chars=sanitized_text_chars,
        billable_chars=len(final_text),
        max_text_chars=max_text_chars,
        is_truncated=True,
    )


def _normalize_message_uuid(message_uuid: str) -> str:
    """
    标准化并校验消息 UUID。

    Args:
        message_uuid: 原始消息 UUID。

    Returns:
        str: 去空白后的消息 UUID。

    Raises:
        ServiceException: 为空字符串时抛出。
    """

    normalized = message_uuid.strip()
    if normalized:
        return normalized
    raise ServiceException(
        code=ResponseCode.BAD_REQUEST,
        message="message_uuid 不能为空",
    )


def _load_message_context_for_tts(
        *,
        message_uuid: str,
        user_id: int,
        conversation_type: ConversationType,
) -> TtsMessageContext:
    """
    加载并校验待合成消息文本。

    校验规则：
    1. 消息存在；
    2. 消息所属会话属于当前用户，且必须匹配指定会话类型；
    3. 仅允许 `role=ai`；
    4. 文本内容非空。

    Args:
        message_uuid: 消息 UUID。
        user_id: 当前用户 ID。
        conversation_type: 目标会话类型（`admin` 或 `client`）。

    Returns:
        TtsMessageContext: 校验通过后的消息上下文。

    Raises:
        ServiceException: 消息不存在、无权限或文本不符合要求时抛出。
    """

    message = get_message_by_uuid(message_uuid)
    if message is None:
        raise ServiceException(
            code=ResponseCode.NOT_FOUND,
            message="消息不存在",
        )

    conversation = _load_conversation_document_for_tts(
        conversation_id=message.conversation_id,
        user_id=user_id,
        conversation_type=conversation_type,
    )
    if conversation is None:
        raise ServiceException(
            code=ResponseCode.NOT_FOUND,
            message="消息不存在或无权限访问",
        )

    if message.role != MessageRole.AI:
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message="仅支持 AI 消息转语音",
        )

    raw_text = str(message.content or "")
    if not raw_text.strip():
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message="消息内容为空，无法转语音",
        )

    return TtsMessageContext(
        message_uuid=message_uuid,
        conversation_id=message.conversation_id,
        conversation_uuid=conversation.uuid,
        user_id=conversation.user_id,
        raw_text=raw_text,
    )


def _load_conversation_document_for_tts(
        *,
        conversation_id: str,
        user_id: int,
        conversation_type: ConversationType,
) -> ConversationDocument | None:
    """
    按会话类型加载 TTS 目标消息所属会话。

    Args:
        conversation_id: 会话 Mongo ObjectId（字符串）。
        user_id: 当前用户 ID。
        conversation_type: 目标会话类型（`admin` 或 `client`）。

    Returns:
        ConversationDocument | None: 命中返回会话模型，否则返回 `None`。

    Raises:
        ValueError: 会话类型不受支持时抛出。
    """

    if conversation_type == ConversationType.ADMIN:
        return get_admin_conversation_by_id(
            conversation_id=conversation_id,
            user_id=user_id,
        )
    if conversation_type == ConversationType.CLIENT:
        return get_client_conversation_by_id(
            conversation_id=conversation_id,
            user_id=user_id,
        )
    raise ValueError(f"unsupported conversation_type for TTS: {conversation_type!r}")


def build_message_tts_stream(
        *,
        message_uuid: str,
        user_id: int,
        conversation_type: ConversationType,
) -> MessageTtsStream:
    """
    按 `message_uuid` 构建双向 TTS 音频流。

    说明：
    - 在返回流之前会先完成消息与权限校验；
    - 音色、编码、采样率全部由服务端环境变量控制；
    - 返回的异步迭代器会边接收上游音频边向下游输出分片。

    Args:
        message_uuid: 目标消息 UUID。
        user_id: 当前请求用户 ID。
        conversation_type: 目标会话类型（`admin` 或 `client`）。

    Returns:
        MessageTtsStream: 包含音频流与媒体类型的封装对象。
    """

    normalized_message_uuid = _normalize_message_uuid(message_uuid)
    message_context = _load_message_context_for_tts(
        message_uuid=normalized_message_uuid,
        user_id=user_id,
        conversation_type=conversation_type,
    )
    config = resolve_volcengine_tts_config()
    prepared_text_detail = prepare_tts_text(
        raw_text=message_context.raw_text,
        max_text_chars=config.max_text_chars,
    )
    usage_context = TtsUsageContext(
        usage_uuid=str(uuid.uuid4()),
        message_uuid=message_context.message_uuid,
        conversation_id=message_context.conversation_id,
        conversation_uuid=message_context.conversation_uuid,
        user_id=message_context.user_id,
        sent_text=prepared_text_detail.sent_text,
        source_text_chars=prepared_text_detail.source_text_chars,
        sanitized_text_chars=prepared_text_detail.sanitized_text_chars,
        billable_chars=prepared_text_detail.billable_chars,
        max_text_chars=prepared_text_detail.max_text_chars,
        is_truncated=prepared_text_detail.is_truncated,
    )
    return MessageTtsStream(
        audio_stream=_stream_tts_audio(
            usage_context=usage_context,
            config=config,
        ),
        media_type=resolve_audio_media_type(config.encoding),
    )


def _build_start_session_payload(*, config: VolcengineTtsConfig) -> bytes:
    """
    构造 `StartSession` 事件请求体。

    Args:
        config: TTS 配置对象。

    Returns:
        bytes: JSON 编码后的请求载荷。
    """

    audio_params: dict[str, Any] = {
        "format": config.encoding,
        "sample_rate": config.sample_rate,
    }
    if _supports_timestamp_for_resource(config.resource_id):
        audio_params["enable_timestamp"] = True

    payload = {
        "event": EventType.StartSession,
        "namespace": "BidirectionalTTS",
        "user": {
            "uid": str(uuid.uuid4()),
        },
        "req_params": {
            "speaker": config.voice_type,
            "audio_params": audio_params,
            "additions": json.dumps(
                {
                    "disable_markdown_filter": False,
                }
            ),
        },
    }
    return json.dumps(payload, ensure_ascii=False).encode("utf-8")


def _build_task_request_payload(*, text: str) -> bytes:
    """
    构造 `TaskRequest` 事件请求体。

    Args:
        text: 待合成文本。

    Returns:
        bytes: JSON 编码后的请求载荷。
    """

    payload = {
        "event": EventType.TaskRequest,
        "namespace": "BidirectionalTTS",
        "req_params": {
            "text": text,
        },
    }
    return json.dumps(payload, ensure_ascii=False).encode("utf-8")


def _decode_payload(payload: bytes) -> str:
    """
    将二进制载荷安全解码为文本，仅用于日志输出。

    Args:
        payload: 原始二进制载荷。

    Returns:
        str: UTF-8 解码结果；解码失败时为空字符串。
    """

    try:
        return payload.decode("utf-8")
    except Exception:
        return ""


def _extract_log_id(websocket: object) -> str:
    """
    提取握手响应中的 `x-tt-logid`，便于启动日志追踪。

    不同 `websockets` 版本的响应对象结构略有差异，因此这里做防御式读取。

    Args:
        websocket: websockets 客户端对象。

    Returns:
        str: `x-tt-logid`；提取失败时为空字符串。
    """

    try:
        response = getattr(websocket, "response", None)
        if response is None:
            return ""
        headers = getattr(response, "headers", None)
        if headers is None:
            return ""
        getter = getattr(headers, "get", None)
        if callable(getter):
            return str(getter("x-tt-logid") or "")
        return ""
    except Exception:
        return ""


async def verify_volcengine_tts_connection_on_startup() -> None:
    """
    启动阶段执行一次火山双向 TTS 连接探活，并打印关键信息到控制台。

    行为说明：
    1. `VOLCENGINE_TTS_STARTUP_CONNECT_ENABLED=false` 时直接跳过；
    2. 配置不完整时打印提示并跳过；
    3. 配置完整时执行 websocket 握手与 `StartConnection/FinishConnection`；
    4. 失败时默认仅告警；若 `VOLCENGINE_TTS_STARTUP_FAIL_FAST=true` 则中断启动。

    Returns:
        None
    """

    if not is_volcengine_tts_startup_connect_enabled():
        logger.info("文本转语音启动探活已禁用。")
        return

    try:
        config = resolve_volcengine_tts_config()
    except ServiceException as exc:
        logger.warning(
            "文本转语音启动探活配置不完整，跳过连接: {message}",
            message=exc.message,
        )
        if is_volcengine_tts_startup_fail_fast_enabled():
            raise
        return

    websocket = None
    connect_id = str(uuid.uuid4())
    headers = build_volcengine_tts_headers(config, connect_id=connect_id)
    started_at = time.monotonic()

    logger.info(
        "文本转语音启动探活开始 endpoint={endpoint} connect_id={connect_id} resource_id={resource_id} voice_type={voice_type} encoding={encoding} sample_rate={sample_rate}",
        endpoint=config.endpoint,
        connect_id=connect_id,
        resource_id=config.resource_id,
        voice_type=config.voice_type,
        encoding=config.encoding,
        sample_rate=config.sample_rate,
    )

    try:
        websocket = await websockets.connect(
            config.endpoint,
            additional_headers=headers,
            max_size=MAX_WS_MESSAGE_SIZE,
        )
        log_id = _extract_log_id(websocket)
        logger.info(
            "文本转语音已连接远程服务 connect_id={connect_id} log_id={log_id}",
            connect_id=connect_id,
            log_id=log_id or "-",
        )

        await start_connection(websocket)
        await wait_for_event(
            websocket,
            msg_type=MsgType.FullServerResponse,
            event_type=EventType.ConnectionStarted,
        )

        elapsed_ms = int((time.monotonic() - started_at) * 1000)
        logger.info(
            "文本转语音连接成功 connect_id={connect_id} elapsed_ms={elapsed_ms}",
            connect_id=connect_id,
            elapsed_ms=elapsed_ms,
        )

        await finish_connection(websocket)
        await wait_for_event(
            websocket,
            msg_type=MsgType.FullServerResponse,
            event_type=EventType.ConnectionFinished,
        )
        logger.info(
            "文本转语音启动探活连接已关闭 connect_id={connect_id}",
            connect_id=connect_id,
        )
    except Exception as exc:
        logger.opt(exception=exc).warning(
            "文本转语音启动探活失败 connect_id={connect_id}",
            connect_id=connect_id,
        )
        if is_volcengine_tts_startup_fail_fast_enabled():
            raise ServiceException(
                code=ResponseCode.INTERNAL_ERROR,
                message="Volcengine TTS 启动连接失败",
            ) from exc
    finally:
        if websocket is not None:
            try:
                await websocket.close()
            except Exception:
                pass


def _persist_tts_usage_on_success(
        *,
        usage_context: TtsUsageContext,
        config: VolcengineTtsConfig,
        connect_id: str,
        session_id: str,
        provider_log_id: str | None,
        audio_chunk_count: int,
        audio_bytes: int,
        duration_ms: int,
) -> None:
    """
    持久化成功 TTS 调用明细，失败仅记录告警日志。

    Args:
        usage_context: 调用上下文与计费信息。
        config: TTS 配置对象。
        connect_id: 上游连接 ID。
        session_id: 上游会话 ID。
        provider_log_id: 上游日志 ID。
        audio_chunk_count: 音频分片数量。
        audio_bytes: 音频总字节数。
        duration_ms: 本次调用耗时（毫秒）。

    Returns:
        None
    """

    try:
        add_message_tts_usage(
            usage_uuid=usage_context.usage_uuid,
            message_uuid=usage_context.message_uuid,
            conversation_id=usage_context.conversation_id,
            conversation_uuid=usage_context.conversation_uuid,
            user_id=usage_context.user_id,
            endpoint=config.endpoint,
            resource_id=config.resource_id,
            voice_type=config.voice_type,
            encoding=config.encoding,
            sample_rate=config.sample_rate,
            sent_text=usage_context.sent_text,
            source_text_chars=usage_context.source_text_chars,
            sanitized_text_chars=usage_context.sanitized_text_chars,
            max_text_chars=usage_context.max_text_chars,
            is_truncated=usage_context.is_truncated,
            audio_chunk_count=audio_chunk_count,
            audio_bytes=audio_bytes,
            duration_ms=duration_ms,
            connect_id=connect_id,
            session_id=session_id,
            provider_log_id=provider_log_id,
        )
    except Exception as exc:  # pragma: no cover - 防御性兜底
        logger.opt(exception=exc).warning(
            "Persist message_tts_usage failed message_uuid={message_uuid} usage_uuid={usage_uuid}",
            message_uuid=usage_context.message_uuid,
            usage_uuid=usage_context.usage_uuid,
        )


async def _stream_tts_audio(
        *,
        usage_context: TtsUsageContext,
        config: VolcengineTtsConfig,
) -> AsyncIterator[bytes]:
    """
    建立到火山双向 TTS 的 WebSocket 并流式产出音频字节。

    生命周期：
    1. StartConnection -> ConnectionStarted
    2. StartSession -> SessionStarted
    3. TaskRequest + FinishSession
    4. 持续读取 `AudioOnlyServer` 直到 `SessionFinished`
    5. 成功结束后写入 `message_tts_usages`
    6. FinishConnection 并关闭 websocket

    Args:
        usage_context: 调用上下文与计费信息。
        config: TTS 配置对象。

    Yields:
        bytes: 上游返回的音频二进制分片。
    """

    websocket = None
    session_id = str(uuid.uuid4())
    connect_id = str(uuid.uuid4())
    provider_log_id = ""
    headers = build_volcengine_tts_headers(config, connect_id=connect_id)
    started_at = time.monotonic()
    audio_chunk_count = 0
    audio_bytes = 0
    is_session_finished = False
    runtime_handle = _ActiveTtsStreamHandle(
        connect_id=connect_id,
        session_id=session_id,
    )
    registered_in_runtime = False

    try:
        websocket = await websockets.connect(
            config.endpoint,
            additional_headers=headers,
            max_size=MAX_WS_MESSAGE_SIZE,
        )
        runtime_handle.attach_websocket(websocket)
        register_active_tts_stream(runtime_handle)
        registered_in_runtime = True
        provider_log_id = _extract_log_id(websocket)

        await start_connection(websocket)
        await wait_for_event(
            websocket,
            msg_type=MsgType.FullServerResponse,
            event_type=EventType.ConnectionStarted,
        )

        await start_session(
            websocket,
            _build_start_session_payload(config=config),
            session_id,
        )
        await wait_for_event(
            websocket,
            msg_type=MsgType.FullServerResponse,
            event_type=EventType.SessionStarted,
        )

        await task_request(
            websocket,
            _build_task_request_payload(text=usage_context.sent_text),
            session_id,
        )
        await finish_session(websocket, session_id)

        while True:
            message = await receive_message(websocket)
            if runtime_handle.interrupted:
                logger.info(
                    "Volcengine TTS stream interrupted by speech config refresh connect_id={connect_id} session_id={session_id}",
                    connect_id=connect_id,
                    session_id=session_id,
                )
                break
            if message.type == MsgType.AudioOnlyServer:
                if message.payload:
                    audio_chunk_count += 1
                    audio_bytes += len(message.payload)
                    yield message.payload
                continue

            if message.type == MsgType.FullServerResponse:
                if message.event == EventType.SessionFinished:
                    is_session_finished = True
                    break
                if message.event == EventType.SessionFailed:
                    logger.warning(
                        "Volcengine TTS session failed event={event} payload={payload}",
                        event=int(message.event),
                        payload=_decode_payload(message.payload),
                    )
                    break
                continue

            if message.type == MsgType.Error:
                logger.warning(
                    "Volcengine TTS protocol error code={code} payload={payload}",
                    code=message.error_code,
                    payload=_decode_payload(message.payload),
                )
                break

    except Exception as exc:
        if runtime_handle.interrupted:
            logger.info(
                "Volcengine TTS stream interrupted by speech config refresh connect_id={connect_id} session_id={session_id}",
                connect_id=connect_id,
                session_id=session_id,
            )
        else:
            logger.opt(exception=exc).warning(
                "Volcengine TTS streaming interrupted connect_id={connect_id} session_id={session_id}",
                connect_id=connect_id,
                session_id=session_id,
            )
    finally:
        if registered_in_runtime:
            unregister_active_tts_stream(runtime_handle)
        duration_ms = int((time.monotonic() - started_at) * 1000)
        if is_session_finished:
            _persist_tts_usage_on_success(
                usage_context=usage_context,
                config=config,
                connect_id=connect_id,
                session_id=session_id,
                provider_log_id=provider_log_id or None,
                audio_chunk_count=audio_chunk_count,
                audio_bytes=audio_bytes,
                duration_ms=duration_ms,
            )
        if websocket is not None:
            try:
                if not runtime_handle.interrupted:
                    await finish_connection(websocket)
                    await wait_for_event(
                        websocket,
                        msg_type=MsgType.FullServerResponse,
                        event_type=EventType.ConnectionFinished,
                    )
            except Exception:
                pass
            try:
                await websocket.close()
            except Exception:
                pass
