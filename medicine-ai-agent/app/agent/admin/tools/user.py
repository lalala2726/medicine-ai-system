"""
用户领域工具。
"""

from __future__ import annotations

from typing import Optional

from langchain_core.tools import tool
from pydantic import BaseModel, Field

from app.agent.admin.tools.base import validate_context_batch_size
from app.core.agent.middleware import (
    tool_call_status,
    tool_thinking_redaction,
)
from app.core.agent.tool_cache import ADMIN_TOOL_CACHE_PROFILE, tool_cacheable
from app.schemas.http_response import HttpResponse
from app.utils.http_client import HttpClient


class UserListRequest(BaseModel):
    """
    功能描述：
        用户列表查询入参模型。

    参数说明：
        page_num (int): 页码。
        page_size (int): 每页数量。
        id (int | None): 用户 ID。
        username (str | None): 用户名。
        nickname (str | None): 昵称。
        avatar (str | None): 头像 URL。
        roles (str | None): 角色编码。
        status (int | None): 用户状态。
        create_by (str | None): 创建人。

    返回值：
        无（数据模型定义）。

    异常说明：
        无。
    """

    page_num: int = Field(default=1, ge=1, description="页码，从 1 开始")
    page_size: int = Field(default=10, ge=1, le=200, description="每页数量，范围 1-200")
    id: Optional[int] = Field(default=None, ge=1, description="用户 ID，精确匹配")
    username: Optional[str] = Field(default=None, description="用户名，支持模糊查询")
    nickname: Optional[str] = Field(default=None, description="昵称，支持模糊查询")
    avatar: Optional[str] = Field(default=None, description="头像 URL，精确匹配")
    roles: Optional[str] = Field(default=None, description="角色编码，例如 admin")
    status: Optional[int] = Field(default=None, description="用户状态，例如 1 启用、0 禁用")
    create_by: Optional[str] = Field(default=None, description="创建人")


class UserIdRequest(BaseModel):
    """
    功能描述：
        单个用户查询入参模型。

    参数说明：
        user_id (int): 用户 ID。

    返回值：
        无（数据模型定义）。

    异常说明：
        无。
    """

    user_id: int = Field(ge=1, description="用户 ID")


class UserIdsRequest(BaseModel):
    """
    功能描述：
        批量用户查询入参模型。

    参数说明：
        user_ids (list[int]): 用户 ID 数组。

    返回值：
        无（数据模型定义）。

    异常说明：
        无。
    """

    user_ids: list[int] = Field(
        min_length=1,
        description="用户 ID 数组，必须传 JSON 数组",
        examples=[[1001], [1001, 1002]],
    )


class UserIdPageRequest(UserIdRequest):
    """
    功能描述：
        带分页的用户查询入参模型。

    参数说明：
        user_id (int): 用户 ID。
        page_num (int): 页码。
        page_size (int): 每页数量。

    返回值：
        无（数据模型定义）。

    异常说明：
        无。
    """

    page_num: int = Field(default=1, ge=1, description="页码，从 1 开始")
    page_size: int = Field(default=10, ge=1, le=200, description="每页数量，范围 1-200")


def _build_user_list_cache_input(arguments: dict[str, object]) -> dict[str, object]:
    """
    功能描述：
        构造用户列表缓存入参。

    参数说明：
        arguments (dict[str, object]): 绑定后的函数参数映射。

    返回值：
        dict[str, object]: 与真实 HTTP 查询参数一致的结构。

    异常说明：
        无。
    """

    return {
        "pageNum": arguments.get("page_num"),
        "pageSize": arguments.get("page_size"),
        "id": arguments.get("id"),
        "username": arguments.get("username"),
        "nickname": arguments.get("nickname"),
        "avatar": arguments.get("avatar"),
        "roles": arguments.get("roles"),
        "status": arguments.get("status"),
        "createBy": arguments.get("create_by"),
    }


def _build_user_page_cache_input(arguments: dict[str, object]) -> dict[str, object]:
    """
    功能描述：
        构造用户分页子资源缓存入参。

    参数说明：
        arguments (dict[str, object]): 绑定后的函数参数映射。

    返回值：
        dict[str, object]: 当前工具缓存入参。

    异常说明：
        无。
    """

    return {
        "user_id": arguments.get("user_id"),
        "page_num": arguments.get("page_num"),
        "page_size": arguments.get("page_size"),
    }


def _normalize_user_ids(user_ids: list[int]) -> list[int]:
    """
    功能描述：
        规范化并校验用户 ID 数组。

    参数说明：
        user_ids (list[int]): 原始用户 ID 数组。

    返回值：
        list[int]: 去重后的正整数用户 ID 数组。

    异常说明：
        ValueError: 当数组为空或存在非正整数时抛出。
    """

    normalized_ids: list[int] = []
    for user_id in user_ids:
        normalized_id = int(user_id)
        if normalized_id <= 0:
            raise ValueError("user_ids 必须全部为正整数")
        if normalized_id not in normalized_ids:
            normalized_ids.append(normalized_id)
    if not normalized_ids:
        raise ValueError("user_ids 必须为非空正整数数组（List[int]）")
    return normalized_ids


def _format_user_ids_to_path(user_ids: list[int]) -> str:
    """
    功能描述：
        将用户 ID 数组按逗号拼接为后端接口路径片段。

    参数说明：
        user_ids (list[int]): 已完成校验的用户 ID 数组。

    返回值：
        str: 逗号拼接后的用户 ID 字符串。

    异常说明：
        无。
    """

    return ",".join(str(user_id) for user_id in user_ids)


def _build_user_ids_cache_input(arguments: dict[str, object]) -> dict[str, object]:
    """
    功能描述：
        构造批量用户详情与钱包缓存入参。

    参数说明：
        arguments (dict[str, object]): 绑定后的函数参数映射。

    返回值：
        dict[str, object]: 标准化后的用户 ID 数组。

    异常说明：
        ValueError: 当 `user_ids` 非法时抛出。
    """

    raw_user_ids = arguments.get("user_ids")
    normalized_user_ids = _normalize_user_ids(raw_user_ids)
    return {"user_ids": normalized_user_ids}


def _build_user_context_cache_input(arguments: dict[str, object]) -> dict[str, object]:
    """
    功能描述：
        构造用户聚合上下文缓存入参。

    参数说明：
        arguments (dict[str, object]): 绑定后的函数参数映射。

    返回值：
        dict[str, object]: 标准化且数量合法的用户 ID 数组。

    异常说明：
        ValueError: 当 `user_ids` 非法或超过批量上限时抛出。
    """

    cache_input = _build_user_ids_cache_input(arguments)
    validate_context_batch_size(cache_input["user_ids"], field_name="user_ids")
    return cache_input


@tool(
    args_schema=UserListRequest,
    description=(
            "分页查询用户列表，支持按用户 ID、用户名、昵称、角色、状态和创建人筛选。"
            "适用于定位目标用户范围。"
    ),
)
@tool_thinking_redaction(display_name="查询用户列表")
@tool_call_status(
    tool_name="查询用户列表",
    start_message="正在查询用户列表",
    error_message="查询用户列表失败",
    timely_message="用户列表正在持续处理中",
)
@tool_cacheable(
    ADMIN_TOOL_CACHE_PROFILE,
    tool_name="user_list",
    input_builder=_build_user_list_cache_input,
)
async def user_list(
        page_num: int = 1,
        page_size: int = 10,
        id: Optional[int] = None,
        username: Optional[str] = None,
        nickname: Optional[str] = None,
        avatar: Optional[str] = None,
        roles: Optional[str] = None,
        status: Optional[int] = None,
        create_by: Optional[str] = None,
) -> dict:
    """
    功能描述：
        查询用户列表。

    参数说明：
        page_num (int): 页码。
        page_size (int): 每页数量。
        id (Optional[int]): 用户 ID。
        username (Optional[str]): 用户名。
        nickname (Optional[str]): 昵称。
        avatar (Optional[str]): 头像 URL。
        roles (Optional[str]): 角色编码。
        status (Optional[int]): 用户状态。
        create_by (Optional[str]): 创建人。

    返回值：
        dict: 用户列表数据。

    异常说明：
        无。
    """

    async with HttpClient() as client:
        params = {
            "pageNum": page_num,
            "pageSize": page_size,
            "id": id,
            "username": username,
            "nickname": nickname,
            "avatar": avatar,
            "roles": roles,
            "status": status,
            "createBy": create_by,
        }
        response = await client.get(
            url="/agent/admin/user/list",
            params=params,
        )
        return HttpResponse.parse_data(response)


@tool(
    args_schema=UserIdsRequest,
    description=(
            "根据用户 ID 数组批量查询 AI 聚合用户上下文。"
            "当用户问这个用户整体情况、钱包状态、账号是否异常、能否操作钱包时优先使用。"
            "一次最多 20 个用户 ID；多个用户 ID 必须一次性传入 user_ids 数组。"
            "只有用户明确要求钱包流水、消费分页或完整用户详情时，才改用 user_wallet_flow、user_consume_info、user_detail 或 user_wallet。"
    ),
)
@tool_thinking_redaction(display_name="查询用户上下文")
@tool_call_status(
    tool_name="查询用户上下文",
    start_message="正在查询用户上下文",
    error_message="查询用户上下文失败",
    timely_message="用户上下文正在持续处理中",
)
@tool_cacheable(
    ADMIN_TOOL_CACHE_PROFILE,
    tool_name="user_context",
    input_builder=_build_user_context_cache_input,
)
async def user_context(user_ids: list[int]) -> dict:
    """
    功能描述：
        根据用户 ID 数组批量查询 AI 聚合用户上下文。

    参数说明：
        user_ids (list[int]): 用户 ID 数组，最多 20 个。

    返回值：
        dict: 按用户 ID 分组的用户上下文数据。

    异常说明：
        ValueError: 当 `user_ids` 非法或超过批量上限时抛出。
    """

    normalized_user_ids = _normalize_user_ids(user_ids)
    validate_context_batch_size(normalized_user_ids, field_name="user_ids")
    user_ids_str = _format_user_ids_to_path(normalized_user_ids)
    async with HttpClient() as client:
        response = await client.get(url=f"/agent/admin/user/context/{user_ids_str}")
        return HttpResponse.parse_data(response)


@tool(
    args_schema=UserIdsRequest,
    description=(
            "根据用户 ID 数组批量查询用户详情。"
            "适用于查看一个或多个用户资料、角色和账号基础信息。"
    ),
)
@tool_thinking_redaction(display_name="查询用户详情")
@tool_call_status(
    tool_name="查询用户详情",
    start_message="正在查询用户详情",
    error_message="查询用户详情失败",
    timely_message="用户详情正在持续处理中",
)
@tool_cacheable(
    ADMIN_TOOL_CACHE_PROFILE,
    tool_name="user_detail",
    input_builder=_build_user_ids_cache_input,
)
async def user_detail(user_ids: list[int]) -> dict:
    """
    功能描述：
        根据用户 ID 数组批量查询用户详情。

    参数说明：
        user_ids (list[int]): 用户 ID 数组。

    返回值：
        dict: 用户详情数组。

    异常说明：
        ValueError: 当 `user_ids` 非法时抛出。
    """

    normalized_user_ids = _normalize_user_ids(user_ids)
    user_ids_str = _format_user_ids_to_path(normalized_user_ids)
    async with HttpClient() as client:
        response = await client.get(url=f"/agent/admin/user/{user_ids_str}/detail")
        return HttpResponse.parse_data(response)


@tool(
    args_schema=UserIdsRequest,
    description=(
            "根据用户 ID 数组批量查询钱包信息。"
            "适用于查看一个或多个用户的钱包余额和可用状态。"
    ),
)
@tool_thinking_redaction(display_name="查询用户钱包")
@tool_call_status(
    tool_name="查询用户钱包",
    start_message="正在查询用户钱包",
    error_message="查询用户钱包失败",
    timely_message="用户钱包正在持续处理中",
)
@tool_cacheable(
    ADMIN_TOOL_CACHE_PROFILE,
    tool_name="user_wallet",
    input_builder=_build_user_ids_cache_input,
)
async def user_wallet(user_ids: list[int]) -> dict:
    """
    功能描述：
        根据用户 ID 数组批量查询钱包信息。

    参数说明：
        user_ids (list[int]): 用户 ID 数组。

    返回值：
        dict: 用户钱包数组。

    异常说明：
        ValueError: 当 `user_ids` 非法时抛出。
    """

    normalized_user_ids = _normalize_user_ids(user_ids)
    user_ids_str = _format_user_ids_to_path(normalized_user_ids)
    async with HttpClient() as client:
        response = await client.get(url=f"/agent/admin/user/{user_ids_str}/wallet")
        return HttpResponse.parse_data(response)


@tool(
    args_schema=UserIdPageRequest,
    description=(
            "根据用户 ID 分页查询钱包流水。"
            "适用于查看用户钱包变动明细。"
    ),
)
@tool_thinking_redaction(display_name="查询用户钱包流水")
@tool_call_status(
    tool_name="查询用户钱包流水",
    start_message="正在查询用户钱包流水",
    error_message="查询用户钱包流水失败",
    timely_message="用户钱包流水正在持续处理中",
)
@tool_cacheable(
    ADMIN_TOOL_CACHE_PROFILE,
    tool_name="user_wallet_flow",
    input_builder=_build_user_page_cache_input,
)
async def user_wallet_flow(
        user_id: int,
        page_num: int = 1,
        page_size: int = 10,
) -> dict:
    """
    功能描述：
        根据用户 ID 分页查询钱包流水。

    参数说明：
        user_id (int): 用户 ID。
        page_num (int): 页码。
        page_size (int): 每页数量。

    返回值：
        dict: 用户钱包流水数据。

    异常说明：
        无。
    """

    async with HttpClient() as client:
        params = {
            "pageNum": page_num,
            "pageSize": page_size,
        }
        response = await client.get(
            url=f"/agent/admin/user/{user_id}/wallet_flow",
            params=params,
        )
        return HttpResponse.parse_data(response)


@tool(
    args_schema=UserIdPageRequest,
    description=(
            "根据用户 ID 分页查询消费信息。"
            "适用于查看用户消费记录和消费明细。"
    ),
)
@tool_thinking_redaction(display_name="查询用户消费信息")
@tool_call_status(
    tool_name="查询用户消费信息",
    start_message="正在查询用户消费信息",
    error_message="查询用户消费信息失败",
    timely_message="用户消费信息正在持续处理中",
)
@tool_cacheable(
    ADMIN_TOOL_CACHE_PROFILE,
    tool_name="user_consume_info",
    input_builder=_build_user_page_cache_input,
)
async def user_consume_info(
        user_id: int,
        page_num: int = 1,
        page_size: int = 10,
) -> dict:
    """
    功能描述：
        根据用户 ID 分页查询消费信息。

    参数说明：
        user_id (int): 用户 ID。
        page_num (int): 页码。
        page_size (int): 每页数量。

    返回值：
        dict: 用户消费信息数据。

    异常说明：
        无。
    """

    async with HttpClient() as client:
        params = {
            "pageNum": page_num,
            "pageSize": page_size,
        }
        response = await client.get(
            url=f"/agent/admin/user/{user_id}/consume_info",
            params=params,
        )
        return HttpResponse.parse_data(response)


__all__ = [
    "UserIdPageRequest",
    "UserIdRequest",
    "UserIdsRequest",
    "UserListRequest",
    "user_consume_info",
    "user_context",
    "user_detail",
    "user_list",
    "user_wallet",
    "user_wallet_flow",
]
