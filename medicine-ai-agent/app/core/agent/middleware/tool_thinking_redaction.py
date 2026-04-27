"""
thinking 工具名脱敏装饰器与注册表。

说明：
1. 该模块只负责维护 `tool_name -> thinking 展示名` 映射，不负责发送工具状态事件；
2. thinking 展示名必须在 `tool_thinking_redaction` 上显式声明，不复用 `tool_call_status` 文案；
3. 若未显式传入 `display_name`，则在装饰阶段直接抛错，防止悄悄回退成英文工具名。
"""

from __future__ import annotations

from typing import Callable, TypeVar

from loguru import logger

_REGISTERED_THINKING_TOOL_DISPLAY_NAMES: dict[str, str] = {}
"""thinking 脱敏注册表：key 为真实工具名，value 为前端可见中文展示名。"""

_TOOL_THINKING_REDACTION_META_ATTR = "__tool_thinking_redaction_display_name__"
"""工具对象上的 thinking 脱敏展示名元数据属性名。"""

_ToolLike = TypeVar("_ToolLike")


def _resolve_tool_name(tool_obj: object) -> str:
    """
    功能描述：
        从工具对象中解析 LangChain 可见的真实工具名。

    参数说明：
        tool_obj (object): 工具函数或 LangChain Tool 对象。

    返回值：
        str: 解析成功时返回真实工具名；失败时返回 `"UNKNOWN_TOOL_NAME"`。

    异常说明：
        无。
    """

    tool_name = getattr(tool_obj, "name", None)
    if isinstance(tool_name, str) and tool_name.strip():
        return tool_name.strip()

    callable_name = getattr(tool_obj, "__name__", None)
    if isinstance(callable_name, str) and callable_name.strip():
        return callable_name.strip()

    logger.warning("tool_thinking_redaction 无法解析工具名称，请检查装饰器顺序。")
    return "UNKNOWN_TOOL_NAME"


def resolve_thinking_tool_display_name(tool_name: str) -> str | None:
    """
    功能描述：
        根据工具原始名称读取已注册的 thinking 脱敏展示名。

    参数说明：
        tool_name (str): 工具真实名称。

    返回值：
        str | None: 命中时返回中文展示名，未注册时返回 `None`。

    异常说明：
        无。
    """

    normalized_tool_name = str(tool_name or "").strip()
    if not normalized_tool_name:
        return None

    display_name = _REGISTERED_THINKING_TOOL_DISPLAY_NAMES.get(normalized_tool_name)
    if not isinstance(display_name, str):
        return None

    normalized_display_name = display_name.strip()
    return normalized_display_name or None


def tool_thinking_redaction(
        *,
        display_name: str,
) -> Callable[[_ToolLike], _ToolLike]:
    """
    功能描述：
        为工具注册 thinking 阶段使用的脱敏展示名。

    参数说明：
        display_name (str): 显式指定的中文展示名。

    返回值：
        Callable: 装饰器函数，返回原工具对象。

    异常说明：
        ValueError:
            - 当工具名称为空时抛出；
            - 当 `display_name` 为空白字符串时抛出。
    """

    def _decorate(tool_obj: _ToolLike) -> _ToolLike:
        """
        功能描述：
            执行 thinking 展示名注册。

        参数说明：
            tool_obj (_ToolLike): 工具函数或 LangChain Tool 对象。

        返回值：
            _ToolLike: 原工具对象，便于继续参与其他装饰器链。

        异常说明：
            ValueError: 当无法解析或确定展示名时抛出。
        """

        resolved_tool_name = _resolve_tool_name(tool_obj)
        if resolved_tool_name == "UNKNOWN_TOOL_NAME":
            raise ValueError("tool_thinking_redaction 无法解析工具名称，请检查装饰器顺序。")

        normalized_display_name = str(display_name or "").strip()
        if not normalized_display_name:
            raise ValueError(
                "tool_thinking_redaction 必须显式提供非空的 display_name。"
            )

        _REGISTERED_THINKING_TOOL_DISPLAY_NAMES[resolved_tool_name] = normalized_display_name
        try:
            setattr(
                tool_obj,
                _TOOL_THINKING_REDACTION_META_ATTR,
                normalized_display_name,
            )
        except Exception:
            logger.warning(
                "工具对象不支持 dynamic 属性写入，将仅使用 thinking 脱敏注册表。tool_name={}",
                resolved_tool_name,
            )
        return tool_obj

    return _decorate


__all__ = [
    "resolve_thinking_tool_display_name",
    "tool_thinking_redaction",
]
