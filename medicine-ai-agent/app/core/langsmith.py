import os
from collections.abc import Callable
from typing import Any, TypeVar

try:
    from langsmith import traceable as _traceable
except Exception:  # pragma: no cover - 可选依赖导入失败时退化为 no-op
    _traceable = None

CallableType = TypeVar("CallableType", bound=Callable[..., Any])
"""无副作用装饰器返回的可调用对象类型。"""
LANGSMITH_TRACING_ENV_NAME = "LANGSMITH_TRACING"
"""LangSmith tracing 主开关环境变量名。"""
_TRUTHY_ENV_VALUES = {"1", "true", "yes", "on"}
"""环境变量真值集合。"""


def _is_truthy(value: str | None) -> bool:
    """
    功能描述：
        判断字符串环境变量是否表示布尔真值。

    参数说明：
        value (str | None): 环境变量原始值。

    返回值：
        bool: 命中真值集合时返回 `True`，否则返回 `False`。
    """

    if value is None:
        return False
    return value.strip().lower() in _TRUTHY_ENV_VALUES


def is_langsmith_enabled() -> bool:
    """
    功能描述：
        判断当前环境是否启用 LangSmith tracing。

        开关规则：
        1. `LANGSMITH_TRACING=true` 时启用；
        2. 其余情况默认关闭。

    返回值：
        bool: 当前环境是否启用 LangSmith tracing。
    """

    return _is_truthy(os.getenv(LANGSMITH_TRACING_ENV_NAME))


def build_langsmith_runnable_config(
        run_name: str,
        tags: list[str] | None = None,
        metadata: dict[str, Any] | None = None,
) -> dict[str, Any] | None:
    """
    功能描述：
        按当前环境配置构造 LangSmith runnable config。

    参数说明：
        run_name (str): workflow 运行名称。
        tags (list[str] | None): tracing 标签列表。
        metadata (dict[str, Any] | None): tracing 元数据。

    返回值：
        dict[str, Any] | None:
            启用 tracing 时返回 runnable config；
            未启用 tracing 时返回 `None`。
    """

    if not is_langsmith_enabled():
        return None

    config: dict[str, Any] = {
        "run_name": run_name,
    }
    if tags:
        config["tags"] = tags
    if metadata:
        config["metadata"] = metadata
    return config


def traceable(*args, **kwargs):
    """
    功能描述：
        提供统一的 LangSmith traceable 装饰器入口。

    参数说明：
        *args: LangSmith 装饰器位置参数。
        **kwargs: LangSmith 装饰器关键字参数。

    返回值：
        Callable[[CallableType], CallableType]:
            安装了 LangSmith 时返回官方装饰器；
            未安装时返回无副作用装饰器。

    设计说明：
        不在装饰器创建阶段读取 `LANGSMITH_TRACING`，避免模块导入早于
        `load_dotenv()` 时把 tracing 开关错误固化为关闭状态。
    """

    if _traceable is not None:
        return _traceable(*args, **kwargs)

    def _decorator(func: CallableType) -> CallableType:
        """
        功能描述：
            直接返回原函数，不附加任何 tracing 行为。

        参数说明：
            func (CallableType): 待装饰函数。

        返回值：
            CallableType: 原函数本身。
        """

        return func

    return _decorator
