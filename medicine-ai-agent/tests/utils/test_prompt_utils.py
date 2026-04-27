from __future__ import annotations

from datetime import datetime, timezone

from app.utils.prompt_utils import append_current_time_to_prompt


def test_append_current_time_to_prompt_uses_exact_utc_plus_8_format() -> None:
    result = append_current_time_to_prompt(
        "角色提示词",
        datetime(2026, 3, 5, 1, 2, tzinfo=timezone.utc),
    )

    assert result == "角色提示词\n\n当前时间：2026-03-05 09:02 UTC+8"


def test_append_current_time_to_prompt_supports_empty_prompt() -> None:
    result = append_current_time_to_prompt(
        "",
        datetime(2026, 12, 1, 16, 7, tzinfo=timezone.utc),
    )

    assert result == "当前时间：2026-12-02 00:07 UTC+8"
