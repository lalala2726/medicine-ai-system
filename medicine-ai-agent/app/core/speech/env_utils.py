from __future__ import annotations

import os
from pathlib import Path

from dotenv import dotenv_values

from app.core.codes import ResponseCode
from app.core.config_sync.snapshot import AgentConfigSnapshot, get_current_agent_config_snapshot
from app.core.exception.exceptions import ServiceException

# STT/TTS 共用的火山 AppId 配置键名。
VOLCENGINE_APP_ID_ENV = "VOLCENGINE_APP_ID"
# STT/TTS 共用的火山 AccessToken 配置键名。
VOLCENGINE_ACCESS_TOKEN_ENV = "VOLCENGINE_ACCESS_TOKEN"

# 项目根目录 `.env` 文件路径（用于配置优先级解析）。
DOTENV_FILE = Path(__file__).resolve().parents[3] / ".env"


def _read_dotenv_value(name: str) -> str:
    """
    从项目根 `.env` 中读取指定配置项。

    Args:
        name: 目标环境变量名。

    Returns:
        str: 读取到的去空白值；未配置或读取失败时返回空字符串。
    """

    if not DOTENV_FILE.exists():
        return ""
    try:
        values = dotenv_values(DOTENV_FILE)
    except Exception:
        return ""
    raw = values.get(name)
    if raw is None:
        return ""
    value = str(raw).strip()
    return value


def resolve_required_env(name: str) -> str:
    """
    读取必填配置，优先 `.env`，其次系统环境变量。

    Args:
        name: 必填配置键名。

    Returns:
        str: 解析后的非空配置值。

    Raises:
        ServiceException: 当 `.env` 与系统环境变量都未提供有效值时抛出。
    """

    dotenv_value = _read_dotenv_value(name)
    if dotenv_value:
        return dotenv_value
    env_value = (os.getenv(name) or "").strip()
    if env_value:
        return env_value
    raise ServiceException(
        code=ResponseCode.INTERNAL_ERROR,
        message=f"{name} is not set",
    )


def resolve_optional_env(name: str) -> str | None:
    """
    读取可选配置，优先 `.env`，其次系统环境变量。

    Args:
        name: 配置键名。

    Returns:
        str | None: 解析后的非空配置值；未配置时返回 ``None``。
    """

    dotenv_value = _read_dotenv_value(name)
    if dotenv_value:
        return dotenv_value
    env_value = (os.getenv(name) or "").strip()
    return env_value or None


def _resolve_current_agent_snapshot(snapshot: AgentConfigSnapshot | None) -> AgentConfigSnapshot:
    """解析当前调用应使用的 Agent 快照。

    Args:
        snapshot: 调用方已持有的快照；为空时自动读取当前生效快照。

    Returns:
        AgentConfigSnapshot: 当前应使用的 Agent 配置快照。
    """

    if snapshot is not None:
        return snapshot
    return get_current_agent_config_snapshot()


def resolve_volcengine_shared_auth_from_env() -> tuple[str, str]:
    """
    仅从环境变量解析 STT/TTS 共用的火山鉴权配置。

    Returns:
        tuple[str, str]:
            - 第 1 项: `VOLCENGINE_APP_ID`
            - 第 2 项: `VOLCENGINE_ACCESS_TOKEN`
    """

    app_id = resolve_required_env(VOLCENGINE_APP_ID_ENV)
    access_token = resolve_required_env(VOLCENGINE_ACCESS_TOKEN_ENV)
    return app_id, access_token


def resolve_volcengine_shared_auth(
        *,
        snapshot: AgentConfigSnapshot | None = None,
) -> tuple[str, str]:
    """
    解析 STT/TTS 共用的火山鉴权配置。

    规则：
    - Redis 中 `appId/accessToken` 同时存在时优先使用 Redis；
    - Redis 仅存在半套鉴权时不与环境变量混用，直接回退环境变量。

    Args:
        snapshot: 当前生效的 Agent 配置快照；为空时自动读取。

    Returns:
        tuple[str, str]:
            - 第 1 项: `VOLCENGINE_APP_ID`
            - 第 2 项: `VOLCENGINE_ACCESS_TOKEN`
    """

    current_snapshot = _resolve_current_agent_snapshot(snapshot)
    shared_auth = current_snapshot.get_speech_shared_auth()
    if shared_auth is not None:
        return shared_auth
    return resolve_volcengine_shared_auth_from_env()


def parse_positive_int(*, value: str | None, name: str, default: int) -> int:
    """
    解析正整数字符串配置，支持默认值回退。

    Args:
        value: 原始配置值（通常来自环境变量），可为 `None`。
        name: 配置键名，用于异常文案定位。
        default: 空值时使用的默认值。

    Returns:
        int: 解析得到的正整数。

    Raises:
        ServiceException:
            - 值不是整数时抛出 `{name} must be an integer`
            - 值小于等于 0 时抛出 `{name} must be greater than 0`
    """

    if value is None or value.strip() == "":
        return default
    try:
        resolved = int(value)
    except ValueError as exc:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message=f"{name} must be an integer",
        ) from exc
    if resolved <= 0:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message=f"{name} must be greater than 0",
        )
    return resolved


__all__ = [
    "VOLCENGINE_ACCESS_TOKEN_ENV",
    "VOLCENGINE_APP_ID_ENV",
    "parse_positive_int",
    "resolve_optional_env",
    "resolve_required_env",
    "resolve_volcengine_shared_auth",
    "resolve_volcengine_shared_auth_from_env",
]
