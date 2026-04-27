from __future__ import annotations

from datetime import datetime, timedelta, timezone

from app.core.prompt_sync import load_managed_prompt as load_managed_prompt_by_key
from app.utils.resource_text_utils import (
    RESOURCES_DIR,
    load_resource_text,
    load_resource_text_from_root,
)

PROMPT_DIR = RESOURCES_DIR / "prompt"
_DEFAULT_PROMPT_DIR = PROMPT_DIR.resolve()
_PROMPT_CACHE: dict[str, str] = {}
_UTC_PLUS_8 = timezone(timedelta(hours=8))


class PromptUtils:
    """Prompt 读取工具。"""

    @staticmethod
    def load_prompt(name: str) -> str:
        """按相对路径读取提示词文件。

        作用：
            从 `resources/prompt` 目录安全读取 markdown 提示词，并做内存缓存。

        参数：
            name: 提示词相对路径，必须包含 `.md` 后缀。

        返回：
            str: 提示词文本内容。
        """

        resolved_prompt_dir = PROMPT_DIR.resolve()
        if resolved_prompt_dir == _DEFAULT_PROMPT_DIR:
            return load_resource_text(
                "prompt",
                name,
                allowed_suffixes=(".md",),
                cache=_PROMPT_CACHE,
            )

        # 兼容单测里 monkeypatch PROMPT_DIR 的场景。
        return load_resource_text_from_root(
            resolved_prompt_dir,
            name,
            allowed_suffixes=(".md",),
            cache=_PROMPT_CACHE,
        )


def load_prompt(name: str) -> str:
    """按相对路径读取 `resources/prompt` 下的 markdown 提示词。"""

    return PromptUtils.load_prompt(name)


def load_managed_prompt(prompt_key: str, local_prompt_path: str | None = None) -> str:
    """按统一业务键读取 Agent 提示词。

    Args:
        prompt_key: 提示词业务键。
        local_prompt_path: 可选本地提示词文件路径（相对 ``resources/prompt``）。

    Returns:
        str: 提示词正文。Redis 未命中且未提供本地路径时返回空字符串。
    """

    return load_managed_prompt_by_key(
        prompt_key,
        local_prompt_path=local_prompt_path,
    )


def append_current_time_to_prompt(prompt: str, now: datetime | None = None) -> str:
    """向提示词末尾追加当前时间说明。

    Args:
        prompt: 原始提示词文本。
        now: 可选时间注入点；为空时使用当前 UTC 时间。

    Returns:
        str: 追加了 `当前时间：YYYY-MM-DD HH:MM UTC+8` 的提示词文本。
    """

    current_time = now or datetime.now(timezone.utc)
    if current_time.tzinfo is None:
        current_time = current_time.replace(tzinfo=timezone.utc)
    formatted_time = current_time.astimezone(_UTC_PLUS_8).strftime("%Y-%m-%d %H:%M UTC+8")
    normalized_prompt = prompt.rstrip()
    if not normalized_prompt:
        return f"当前时间：{formatted_time}"
    return f"{normalized_prompt}\n\n当前时间：{formatted_time}"
