from __future__ import annotations

import inspect
from functools import wraps
from typing import Any, Callable, ParamSpec, TypeVar

from app.core.agent.middleware.tool_status import resolve_tool_display_name
from app.core.agent.middleware.tool_thinking_redaction import (
    resolve_thinking_tool_display_name,
)
from app.core.agent.tool_trace import (
    bind_tool_trace_arguments,
    save_current_tool_trace_entry,
)
from app.schemas.document.tool_trace import ToolTraceStatus

P = ParamSpec("P")
R = TypeVar("R")
_TOOL_TRACE_RECORD_META_ATTR = "__tool_trace_record_display_name__"


def _resolve_tool_name(tool_obj: Any) -> str:
    """
    功能描述：
        从函数或 Tool 对象中解析真实工具名。

    参数说明：
        tool_obj (Any): 原始函数或 Tool 对象。

    返回值：
        str: 解析出的真实工具名。

    异常说明：
        ValueError: 当无法解析工具名时抛出。
    """

    tool_name = getattr(tool_obj, "name", None)
    if isinstance(tool_name, str) and tool_name.strip():
        return tool_name.strip()

    callable_name = getattr(tool_obj, "__name__", None)
    if isinstance(callable_name, str) and callable_name.strip():
        return callable_name.strip()

    raise ValueError("tool_trace_record 无法解析工具名称，请仅在 @tool 上下紧邻使用。")


def _resolve_tool_display_name_for_trace(
        *,
        tool_name: str,
        explicit_display_name: str | None,
) -> str:
    """
    功能描述：
        解析工具轨迹使用的展示名称。

    参数说明：
        tool_name (str): 工具真实名称。
        explicit_display_name (str | None): 装饰器显式传入的展示名称。

    返回值：
        str: 最终展示名称。

    异常说明：
        无。
    """

    normalized_display_name = str(explicit_display_name or "").strip()
    if normalized_display_name:
        return normalized_display_name

    thinking_display_name = resolve_thinking_tool_display_name(tool_name)
    if thinking_display_name:
        return thinking_display_name

    status_display_name = resolve_tool_display_name(tool_name)
    if status_display_name:
        return status_display_name

    return tool_name


def _build_error_payload(error: Exception) -> dict[str, str]:
    """
    功能描述：
        为失败工具调用构造结构化错误信息。

    参数说明：
        error (Exception): 原始异常对象。

    返回值：
        dict[str, str]: 结构化错误载荷。

    异常说明：
        无。
    """

    error_message = str(error).strip() or repr(error)
    return {
        "error_type": error.__class__.__name__,
        "error_message": error_message,
    }


def _wrap_callable(
        *,
        func: Callable[P, R],
        tool_name: str,
        tool_display_name: str,
) -> Callable[P, R]:
    """
    功能描述：
        为单个工具函数增加工具轨迹记录能力。

    参数说明：
        func (Callable[P, R]): 原始工具函数。
        tool_name (str): 工具真实名称。
        tool_display_name (str): 工具展示名称。

    返回值：
        Callable[P, R]: 记录成功/失败轨迹后的包装函数。

    异常说明：
        无；原始异常会在记录失败轨迹后继续向上抛出。
    """

    if getattr(func, _TOOL_TRACE_RECORD_META_ATTR, None):
        return func

    if inspect.iscoroutinefunction(func):

        @wraps(func)
        async def _async_wrapper(*args: P.args, **kwargs: P.kwargs) -> R:
            tool_input = bind_tool_trace_arguments(func, args, dict(kwargs))
            try:
                result = await func(*args, **kwargs)
            except Exception as exc:
                save_current_tool_trace_entry(
                    tool_name=tool_name,
                    tool_display_name=tool_display_name,
                    status=ToolTraceStatus.ERROR,
                    tool_input=tool_input,
                    tool_output=None,
                    error_payload=_build_error_payload(exc),
                )
                raise
            save_current_tool_trace_entry(
                tool_name=tool_name,
                tool_display_name=tool_display_name,
                status=ToolTraceStatus.SUCCESS,
                tool_input=tool_input,
                tool_output=result,
                error_payload=None,
            )
            return result

        setattr(_async_wrapper, _TOOL_TRACE_RECORD_META_ATTR, tool_display_name)
        return _async_wrapper

    @wraps(func)
    def _sync_wrapper(*args: P.args, **kwargs: P.kwargs) -> R:
        tool_input = bind_tool_trace_arguments(func, args, dict(kwargs))
        try:
            result = func(*args, **kwargs)
        except Exception as exc:
            save_current_tool_trace_entry(
                tool_name=tool_name,
                tool_display_name=tool_display_name,
                status=ToolTraceStatus.ERROR,
                tool_input=tool_input,
                tool_output=None,
                error_payload=_build_error_payload(exc),
            )
            raise
        save_current_tool_trace_entry(
            tool_name=tool_name,
            tool_display_name=tool_display_name,
            status=ToolTraceStatus.SUCCESS,
            tool_input=tool_input,
            tool_output=result,
            error_payload=None,
        )
        return result

    setattr(_sync_wrapper, _TOOL_TRACE_RECORD_META_ATTR, tool_display_name)
    return _sync_wrapper


def _decorate_tool_object(
        *,
        tool_obj: Any,
        explicit_display_name: str | None,
) -> Any:
    """
    功能描述：
        为 `@tool` 生成的 Tool 对象补齐工具轨迹记录。

    参数说明：
        tool_obj (Any): LangChain Tool 对象。
        explicit_display_name (str | None): 显式展示名称。

    返回值：
        Any: 原 Tool 对象。

    异常说明：
        ValueError: 当对象不是可包装的 Tool，或缺少底层执行函数时抛出。
    """

    tool_name = _resolve_tool_name(tool_obj)
    tool_display_name = _resolve_tool_display_name_for_trace(
        tool_name=tool_name,
        explicit_display_name=explicit_display_name,
    )
    wrapped = False

    tool_func = getattr(tool_obj, "func", None)
    if callable(tool_func):
        setattr(
            tool_obj,
            "func",
            _wrap_callable(
                func=tool_func,
                tool_name=tool_name,
                tool_display_name=tool_display_name,
            ),
        )
        wrapped = True

    tool_coroutine = getattr(tool_obj, "coroutine", None)
    if callable(tool_coroutine):
        setattr(
            tool_obj,
            "coroutine",
            _wrap_callable(
                func=tool_coroutine,
                tool_name=tool_name,
                tool_display_name=tool_display_name,
            ),
        )
        wrapped = True

    if not wrapped:
        raise ValueError("tool_trace_record 只能用于带 func/coroutine 的 @tool 对象。")
    setattr(tool_obj, _TOOL_TRACE_RECORD_META_ATTR, tool_display_name)
    return tool_obj


def tool_trace_record(
        *,
        display_name: str | None = None,
) -> Callable[[Any], Any]:
    """
    功能描述：
        为显式标注的工具记录输入、输出与失败轨迹。

    参数说明：
        display_name (str | None): 可选工具展示名称；为空时按已有工具中文名解析。

    返回值：
        Callable[[Any], Any]: 既支持包原函数，也支持包 `@tool` 产物的装饰器。

    异常说明：
        ValueError: 当目标既不是函数也不是 `@tool` 产物时抛出。
    """

    def _decorate(target: Any) -> Any:
        if callable(target) and not hasattr(target, "func") and not hasattr(target, "coroutine"):
            tool_name = _resolve_tool_name(target)
            tool_display_name = _resolve_tool_display_name_for_trace(
                tool_name=tool_name,
                explicit_display_name=display_name,
            )
            return _wrap_callable(
                func=target,
                tool_name=tool_name,
                tool_display_name=tool_display_name,
            )

        if hasattr(target, "name") and (hasattr(target, "func") or hasattr(target, "coroutine")):
            return _decorate_tool_object(
                tool_obj=target,
                explicit_display_name=display_name,
            )

        raise ValueError("tool_trace_record 只能用于 @tool 上下紧邻的装饰器位置。")

    return _decorate


__all__ = [
    "tool_trace_record",
]
