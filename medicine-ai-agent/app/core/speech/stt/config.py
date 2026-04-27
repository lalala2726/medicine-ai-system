from __future__ import annotations

import os
import uuid
from dataclasses import dataclass

from app.core.config_sync.snapshot import get_current_agent_config_snapshot
from app.core.speech.env_utils import (
    parse_positive_int,
    resolve_required_env,
    resolve_volcengine_shared_auth,
)

# 火山实时 STT 默认接入地址（双向流式优化版）。
DEFAULT_VOLCENGINE_STT_ENDPOINT = "wss://openspeech.bytedance.com/api/v3/sauc/bigmodel_async"
# 单次 STT 会话可配置最大时长默认值（秒，默认 10 分钟）。
DEFAULT_VOLCENGINE_STT_MAX_DURATION_SECONDS = 600
# 单次 STT 会话允许配置的最大时长上限（秒，10 分钟）。
MAX_VOLCENGINE_STT_MAX_DURATION_SECONDS = 600


@dataclass(frozen=True)
class VolcengineSttConfig:
    """
    火山实时 STT 运行时配置。

    Attributes:
        endpoint: 上游 STT WebSocket 地址。
        app_id: 火山应用 ID（与 TTS 共用）。
        access_token: 火山访问令牌（与 TTS 共用）。
        resource_id: STT 资源 ID（如 `volc.seedasr.sauc.duration`）。
        max_duration_seconds: 单连接可配置的最大识别时长（秒）。
    """

    endpoint: str
    app_id: str
    access_token: str
    resource_id: str
    max_duration_seconds: int


def resolve_volcengine_stt_config() -> VolcengineSttConfig:
    """
    从环境变量解析实时 STT 配置。

    Returns:
        VolcengineSttConfig: 解析完成且可直接用于建连的配置对象。

    Raises:
        ServiceException: 必填配置缺失或数值配置非法时抛出。
    """

    snapshot = get_current_agent_config_snapshot()
    app_id, access_token = resolve_volcengine_shared_auth(snapshot=snapshot)
    resource_id = snapshot.get_speech_stt_resource_id() or resolve_required_env("VOLCENGINE_STT_RESOURCE_ID")
    endpoint = (os.getenv("VOLCENGINE_STT_ENDPOINT") or DEFAULT_VOLCENGINE_STT_ENDPOINT).strip()
    if not endpoint:
        endpoint = DEFAULT_VOLCENGINE_STT_ENDPOINT
    max_duration_seconds = parse_positive_int(
        value=os.getenv("VOLCENGINE_STT_MAX_DURATION_SECONDS"),
        name="VOLCENGINE_STT_MAX_DURATION_SECONDS",
        default=DEFAULT_VOLCENGINE_STT_MAX_DURATION_SECONDS,
    )
    if max_duration_seconds > MAX_VOLCENGINE_STT_MAX_DURATION_SECONDS:
        max_duration_seconds = MAX_VOLCENGINE_STT_MAX_DURATION_SECONDS
    return VolcengineSttConfig(
        endpoint=endpoint,
        app_id=app_id,
        access_token=access_token,
        resource_id=resource_id,
        max_duration_seconds=max_duration_seconds,
    )


def build_volcengine_stt_headers(
        config: VolcengineSttConfig,
        *,
        connect_id: str | None = None,
) -> dict[str, str]:
    """
    构造连接火山实时 STT 所需 websocket 请求头。

    Args:
        config: STT 配置对象。
        connect_id: 业务侧连接标识；为空时自动生成 UUID。

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


__all__ = [
    "DEFAULT_VOLCENGINE_STT_ENDPOINT",
    "DEFAULT_VOLCENGINE_STT_MAX_DURATION_SECONDS",
    "MAX_VOLCENGINE_STT_MAX_DURATION_SECONDS",
    "VolcengineSttConfig",
    "resolve_volcengine_stt_config",
    "build_volcengine_stt_headers",
]
