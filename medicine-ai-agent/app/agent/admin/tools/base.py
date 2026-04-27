"""
管理端基础工具与共享辅助函数。

说明：
1. 动态工具加载共享协议已抽到 `app.core.agent.middleware`；
2. 本模块仅保留 admin 专属基础工具与共享辅助函数；
3. 共享的 ID 与业务值规范化函数放在此处，供各领域工具复用。
"""

from __future__ import annotations

from langchain.tools import tool
from pydantic import BaseModel

from app.core.agent.middleware import (
    LoadToolsRequest,
    LoadableToolsCatalog,
    create_list_loadable_tools_tool,
    create_load_tools_tool,
    merge_unique_loaded_tool_keys,
    tool_call_status,
    tool_thinking_redaction,
)
from app.core.security import get_current_user
from app.schemas.auth import AuthUser

# AI 聚合 Context 工具单次批量查询上限。
ADMIN_CONTEXT_BATCH_LIMIT = 20


def format_ids_to_string(ids: list[str]) -> str:
    """
    功能描述：
        将字符串 ID 数组按逗号拼接为后端接口所需路径片段。

    参数说明：
        ids (list[str]): 已完成校验的 ID 数组。

    返回值：
        str: 逗号拼接后的字符串。

    异常说明：
        无。
    """

    return ",".join(str(item) for item in ids)


def format_values_to_path(values: list[str]) -> str:
    """
    功能描述：
        将字符串业务值数组按逗号拼接为后端接口所需路径片段。

    参数说明：
        values (list[str]): 已完成校验的业务值数组。

    返回值：
        str: 逗号拼接后的字符串。

    异常说明：
        无。
    """

    return ",".join(str(item) for item in values)


def normalize_id_list(ids: list[str], *, field_name: str) -> list[str]:
    """
    功能描述：
        规范化并校验批量 ID 数组参数。

    参数说明：
        ids (list[str]): 原始 ID 数组。
        field_name (str): 当前字段名，用于错误提示。

    返回值：
        list[str]: 去空白、去空项后的 ID 数组。

    异常说明：
        ValueError: 当数组为空或全部为空白字符串时抛出。
    """

    normalized = [str(item).strip() for item in ids if str(item).strip()]
    if not normalized:
        raise ValueError(f"{field_name} 必须为非空字符串数组（List[str]）")
    return normalized


def normalize_string_list(values: list[str], *, field_name: str) -> list[str]:
    """
    功能描述：
        规范化并校验批量字符串业务参数。

    参数说明：
        values (list[str]): 原始字符串业务值数组。
        field_name (str): 当前字段名，用于错误提示。

    返回值：
        list[str]: 去空白、去空项后的字符串数组。

    异常说明：
        ValueError: 当数组为空或全部为空白字符串时抛出。
    """

    normalized = [str(item).strip() for item in values if str(item).strip()]
    if not normalized:
        raise ValueError(f"{field_name} 必须为非空字符串数组（List[str]）")
    return normalized


def validate_context_batch_size(values: list[object], *, field_name: str) -> None:
    """
    功能描述：
        校验 AI 聚合 Context 工具的批量查询数量。

    参数说明：
        values (list[object]): 已完成基础规范化的批量查询值。
        field_name (str): 当前字段名，用于错误提示。

    返回值：
        None。

    异常说明：
        ValueError: 当批量数量超过统一上限时抛出。
    """

    if len(values) > ADMIN_CONTEXT_BATCH_LIMIT:
        raise ValueError(f"{field_name} 最多支持 {ADMIN_CONTEXT_BATCH_LIMIT} 个")


class UserInfo(BaseModel):
    """
    功能描述：
        当前登录用户的安全信息模型，仅暴露允许给 Agent 使用的字段。

    参数说明：
        username (str | None): 用户名。
        nickname (str | None): 昵称。

    返回值：
        无（数据模型定义）。

    异常说明：
        无。
    """

    username: str | None = None
    nickname: str | None = None

    @classmethod
    def from_auth_user(cls, auth_user: AuthUser) -> "UserInfo":
        """
        功能描述：
            从认证用户对象构造安全用户信息对象。

        参数说明：
            auth_user (AuthUser): 当前认证用户对象。

        返回值：
            UserInfo: 仅包含非敏感字段的用户信息。

        异常说明：
            无。
        """

        return cls(
            username=auth_user.username,
            nickname=auth_user.nickname,
        )


@tool(
    description=(
            "获取当前聊天用户的基本信息。"
            "适用于确认当前登录人身份、昵称等基础上下文。"
    ),
)
@tool_thinking_redaction(display_name="获取用户信息")
@tool_call_status(
    tool_name="获取用户信息",
    start_message="正在获取用户信息",
    error_message="获取用户信息失败",
    timely_message="用户信息正在持续处理中",
)
def get_safe_user_info() -> UserInfo:
    """
    功能描述：
        获取当前登录用户的非敏感基础信息。

    参数说明：
        无。

    返回值：
        UserInfo: 过滤敏感字段后的用户信息。

    异常说明：
        无。
    """

    auth_user = get_current_user()
    return UserInfo.from_auth_user(auth_user)


__all__ = [
    "ADMIN_CONTEXT_BATCH_LIMIT",
    "LoadableToolsCatalog",
    "LoadToolsRequest",
    "UserInfo",
    "create_list_loadable_tools_tool",
    "create_load_tools_tool",
    "format_ids_to_string",
    "format_values_to_path",
    "get_safe_user_info",
    "merge_unique_loaded_tool_keys",
    "normalize_id_list",
    "normalize_string_list",
    "validate_context_batch_size",
]
