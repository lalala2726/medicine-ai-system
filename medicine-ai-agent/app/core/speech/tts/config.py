from __future__ import annotations

import os
import uuid
from dataclasses import dataclass

from app.core.codes import ResponseCode
from app.core.config_sync.snapshot import get_current_agent_config_snapshot
from app.core.exception.exceptions import ServiceException
from app.core.speech.env_utils import (
    parse_positive_int,
    resolve_required_env,
    resolve_volcengine_shared_auth,
)

# 火山双向 TTS 默认接入地址。
DEFAULT_VOLCENGINE_TTS_ENDPOINT = "wss://openspeech.bytedance.com/api/v3/tts/bidirection"
# 默认输出编码。
DEFAULT_VOLCENGINE_TTS_ENCODING = "mp3"
# 默认输出采样率（Hz）。
DEFAULT_VOLCENGINE_TTS_SAMPLE_RATE = 24000
# 单次请求送入 TTS 的最大字符数默认值。
DEFAULT_VOLCENGINE_TTS_MAX_TEXT_CHARS = 300
# 默认开启启动期连通性探活。
DEFAULT_VOLCENGINE_TTS_STARTUP_CONNECT_ENABLED = True
# 默认探活失败不阻断服务启动。
DEFAULT_VOLCENGINE_TTS_STARTUP_FAIL_FAST = False


@dataclass(frozen=True)
class VolcengineTtsConfig:
    """
    火山双向 TTS 运行时配置。

    Attributes:
        endpoint: 上游 TTS WebSocket 地址。
        app_id: 火山应用 ID（与 STT 共用）。
        access_token: 火山访问令牌（与 STT 共用）。
        resource_id: TTS 资源 ID。
        voice_type: 默认音色类型。
        encoding: 输出编码格式。
        sample_rate: 输出采样率。
        max_text_chars: 文本截断上限。
    """

    endpoint: str
    app_id: str
    access_token: str
    resource_id: str
    voice_type: str
    encoding: str
    sample_rate: int
    max_text_chars: int


def _parse_bool(*, value: str | None, default: bool) -> bool:
    """
    解析布尔环境变量。

    Args:
        value: 原始字符串值，可为 `None`。
        default: 无值或空值时的回退值。

    Returns:
        bool: 解析后的布尔值。
    """

    if value is None:
        return default
    normalized = value.strip().lower()
    if normalized == "":
        return default
    return normalized in {"1", "true", "yes", "on"}


def resolve_volcengine_tts_config() -> VolcengineTtsConfig:
    """
    从 Redis/环境变量解析火山双向 TTS 配置。

    说明：
    - 前端仅传 `message_uuid`，音色/编码/采样率全部由服务端配置控制；
    - 文本转语音 `resourceId` 为必填项，优先读取 Redis，缺失时回退 `VOLCENGINE_TTS_RESOURCE_ID`；
    - 文本最大字符数由 `speech.textToSpeech.maxTextChars` 或 `VOLCENGINE_TTS_MAX_TEXT_CHARS` 控制。

    Returns:
        VolcengineTtsConfig: 可直接用于 TTS 建连与请求发送的配置对象。

    Raises:
        ServiceException: 必填项缺失或配置值非法时抛出。
    """

    snapshot = get_current_agent_config_snapshot()
    app_id, access_token = resolve_volcengine_shared_auth(snapshot=snapshot)
    endpoint = (os.getenv("VOLCENGINE_TTS_ENDPOINT") or DEFAULT_VOLCENGINE_TTS_ENDPOINT).strip()
    if not endpoint:
        endpoint = DEFAULT_VOLCENGINE_TTS_ENDPOINT

    redis_resource_id = snapshot.get_speech_tts_resource_id()
    redis_voice_type = snapshot.get_speech_tts_voice_type()
    redis_max_text_chars = snapshot.get_speech_tts_max_text_chars()
    if redis_resource_id is not None:
        resolved_resource_id = redis_resource_id
        resolved_voice_type = (redis_voice_type or "").strip()
        if not resolved_voice_type:
            raise ServiceException(
                code=ResponseCode.INTERNAL_ERROR,
                message="Redis 中 TTS 配置不完整: speech.textToSpeech.voiceType 缺失或为空",
            )
        if redis_max_text_chars is None:
            raise ServiceException(
                code=ResponseCode.INTERNAL_ERROR,
                message="Redis 中 TTS 配置不完整: speech.textToSpeech.maxTextChars 缺失",
            )
        resolved_max_text_chars = redis_max_text_chars
    else:
        resolved_resource_id = resolve_required_env("VOLCENGINE_TTS_RESOURCE_ID")
        resolved_voice_type = resolve_required_env("VOLCENGINE_TTS_VOICE_TYPE")
        resolved_max_text_chars = parse_positive_int(
            value=os.getenv("VOLCENGINE_TTS_MAX_TEXT_CHARS"),
            name="VOLCENGINE_TTS_MAX_TEXT_CHARS",
            default=DEFAULT_VOLCENGINE_TTS_MAX_TEXT_CHARS,
        )

    resolved_voice_type = resolved_voice_type.strip()
    if not resolved_voice_type:
        raise ServiceException(
            code=ResponseCode.BAD_REQUEST,
            message="voice_type 不能为空",
        )

    resolved_encoding = (
            os.getenv("VOLCENGINE_TTS_ENCODING")
            or DEFAULT_VOLCENGINE_TTS_ENCODING
    ).strip().lower()
    if not resolved_encoding:
        resolved_encoding = DEFAULT_VOLCENGINE_TTS_ENCODING

    resolved_sample_rate = parse_positive_int(
        value=os.getenv("VOLCENGINE_TTS_SAMPLE_RATE"),
        name="VOLCENGINE_TTS_SAMPLE_RATE",
        default=DEFAULT_VOLCENGINE_TTS_SAMPLE_RATE,
    )

    return VolcengineTtsConfig(
        endpoint=endpoint,
        app_id=app_id,
        access_token=access_token,
        resource_id=resolved_resource_id,
        voice_type=resolved_voice_type,
        encoding=resolved_encoding,
        sample_rate=resolved_sample_rate,
        max_text_chars=resolved_max_text_chars,
    )


def is_volcengine_tts_startup_connect_enabled() -> bool:
    """
    判断是否在服务启动阶段执行 TTS 连接探活。

    Returns:
        bool: `True` 表示启用启动探活。
    """

    return _parse_bool(
        value=os.getenv("VOLCENGINE_TTS_STARTUP_CONNECT_ENABLED"),
        default=DEFAULT_VOLCENGINE_TTS_STARTUP_CONNECT_ENABLED,
    )


def is_volcengine_tts_startup_fail_fast_enabled() -> bool:
    """
    判断启动探活失败时是否中断服务启动。

    Returns:
        bool: `True` 表示探活失败即抛错阻断启动。
    """

    return _parse_bool(
        value=os.getenv("VOLCENGINE_TTS_STARTUP_FAIL_FAST"),
        default=DEFAULT_VOLCENGINE_TTS_STARTUP_FAIL_FAST,
    )


def build_volcengine_tts_headers(
        config: VolcengineTtsConfig,
        *,
        connect_id: str | None = None,
) -> dict[str, str]:
    """
    构造连接火山双向 TTS 所需的 WebSocket 请求头。

    Args:
        config: TTS 配置对象。
        connect_id: 业务连接标识；为空时自动生成 UUID。

    Returns:
        dict[str, str]: 可直接传给 websocket 客户端的请求头。
    """

    resolved_connect_id = (connect_id or str(uuid.uuid4())).strip()
    if not resolved_connect_id:
        resolved_connect_id = str(uuid.uuid4())

    return {
        "X-Api-App-Key": config.app_id,
        "X-Api-Access-Key": config.access_token,
        "X-Api-Resource-Id": config.resource_id,
        "X-Api-Connect-Id": resolved_connect_id,
    }
