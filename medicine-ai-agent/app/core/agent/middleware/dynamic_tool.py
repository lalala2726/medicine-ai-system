"""
动态工具加载共享协议模块。

说明：
1. 统一沉淀动态工具加载的请求模型、注册中心、中间件与基础工具工厂；
2. admin 与 client commerce 仅保留各自的工具集合、领域分组与提示文案配置；
3. 该模块不依赖具体业务域，避免重复实现和循环依赖。
"""

from __future__ import annotations

from collections.abc import Awaitable, Callable, Mapping
from dataclasses import dataclass
from typing import Any, Protocol

from langchain.agents.middleware import AgentMiddleware, ModelRequest, ModelResponse
from langchain.messages import ToolMessage
from langchain.tools import ToolRuntime, tool
from langchain_core.tools import BaseTool
from langgraph.types import Command
from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.core.agent.middleware.tool_status import tool_call_status
from app.core.agent.middleware.tool_thinking_redaction import (
    tool_thinking_redaction,
)


@dataclass(frozen=True)
class DynamicToolingTextConfig:
    """
    功能描述：
        动态工具加载文案配置。

    参数说明：
        list_description (str): `list_loadable_tools` 工具描述。
        list_tool_name (str): `list_loadable_tools` 工具展示名。
        list_start_message (str): `list_loadable_tools` 开始提示文案。
        list_error_message (str): `list_loadable_tools` 失败提示文案。
        list_timely_message (str): `list_loadable_tools` 持续处理中提示文案。
        list_usage_tip (str): 工具目录返回中的使用提示。
        load_description (str): `load_tools` 工具描述。
        load_tool_name (str): `load_tools` 工具展示名。
        load_start_message (str): `load_tools` 开始提示文案。
        load_error_message (str): `load_tools` 失败提示文案。
        load_timely_message (str): `load_tools` 持续处理中提示文案。
        load_success_prefix (str): 加载成功后的首行提示前缀。
        load_completion_message (str): 加载成功后的结尾提示文案。

    返回值：
        无（数据模型定义）。

    异常说明：
        无。
    """

    list_description: str
    list_tool_name: str
    list_start_message: str
    list_error_message: str
    list_timely_message: str
    list_usage_tip: str
    load_description: str
    load_tool_name: str
    load_start_message: str
    load_error_message: str
    load_timely_message: str
    load_success_prefix: str
    load_completion_message: str = (
        "这些工具无需用户确认；你可以继续直接调用已加载的实际工具名完成任务。"
    )


class LoadToolsRequest(BaseModel):
    """
    功能描述：
        动态工具加载入参模型。

    参数说明：
        tool_keys (list[str]): 本次需要加载的业务工具 key 数组。

    返回值：
        无（数据模型定义）。

    异常说明：
        ValueError: 当工具数组不合法时抛出。
    """

    model_config = ConfigDict(extra="forbid")

    tool_keys: list[str] = Field(
        min_length=1,
        description="需要加载的业务工具 key 数组，只允许 snake_case 工具名",
    )

    @field_validator("tool_keys")
    @classmethod
    def normalize_tool_keys(cls, value: list[str]) -> list[str]:
        """
        功能描述：
            规范化工具 key 数组并按顺序去重。

        参数说明：
            value (list[str]): 原始工具 key 数组。

        返回值：
            list[str]: 去空白、转小写、顺序去重后的工具 key 数组。

        异常说明：
            ValueError: 当数组为空或包含空值时抛出。
        """

        normalized_tool_keys: list[str] = []
        for raw_tool_key in value:
            tool_key = str(raw_tool_key or "").strip().lower()
            if not tool_key:
                raise ValueError("tool_keys 不能包含空值")
            if tool_key in normalized_tool_keys:
                continue
            normalized_tool_keys.append(tool_key)
        if not normalized_tool_keys:
            raise ValueError("tool_keys 不能为空")
        return normalized_tool_keys


class LoadableToolsCatalog(BaseModel):
    """
    功能描述：
        可加载业务工具目录模型。

    参数说明：
        exact_tool_names (list[str]): 当前可加载业务工具的精确工具名数组。
        tools_by_domain (dict[str, list[str]]): 按领域分组后的工具名映射。
        supports_multi_load (bool): 是否支持单次同时加载多个工具。
        usage_tip (str): 工具目录使用提示。

    返回值：
        无（数据模型定义）。

    异常说明：
        无。
    """

    exact_tool_names: list[str] = Field(
        ...,
        description="当前可加载业务工具的精确工具名数组",
    )
    tools_by_domain: dict[str, list[str]] = Field(
        ...,
        description="按领域分组后的工具名映射",
    )
    supports_multi_load: bool = Field(
        ...,
        description="是否支持单次同时加载多个工具",
    )
    usage_tip: str = Field(
        ...,
        description="调用加载工具时的使用提示",
    )


class DynamicToolRegistryProtocol(Protocol):
    """
    功能描述：
        动态工具注册中心协议。

    参数说明：
        无。

    返回值：
        无（协议定义）。

    异常说明：
        无。
    """

    def filter_visible_tools(
            self,
            *,
            request_tools: list[Any],
            loaded_tool_keys: list[str] | None,
    ) -> list[Any]:
        """
        功能描述：
            过滤当前请求中的工具列表，仅保留当前可见的管理工具。

        参数说明：
            request_tools (list[Any]): 当前请求中的工具对象数组。
            loaded_tool_keys (list[str] | None): 当前状态中的已加载工具 key 数组。

        返回值：
            list[Any]: 当前请求下模型最终可见的工具对象数组。

        异常说明：
            无。
        """


def normalize_loaded_tool_keys(state: Any) -> list[str] | None:
    """
    功能描述：
        从状态对象中读取并规范化已加载工具数组。

    参数说明：
        state (Any): 请求状态对象或节点状态对象。

    返回值：
        list[str] | None: 合法的工具 key 数组；未命中时返回 `None`。

    异常说明：
        无。
    """

    if not isinstance(state, Mapping):
        return None

    raw_loaded_tool_keys = state.get("loaded_tool_keys")
    if not isinstance(raw_loaded_tool_keys, list):
        return None

    normalized_tool_keys: list[str] = []
    for raw_tool_key in raw_loaded_tool_keys:
        tool_key = str(raw_tool_key or "").strip()
        if not tool_key:
            continue
        if tool_key in normalized_tool_keys:
            continue
        normalized_tool_keys.append(tool_key)
    return normalized_tool_keys


def merge_unique_loaded_tool_keys(
        existing_tool_keys: list[str],
        requested_tool_keys: list[str],
) -> list[str]:
    """
    功能描述：
        合并已加载工具与本次新加载工具，并保持顺序去重。

    参数说明：
        existing_tool_keys (list[str]): 当前状态中已有的工具 key 数组。
        requested_tool_keys (list[str]): 本次新加载的工具 key 数组。

    返回值：
        list[str]: 合并后的稳定顺序工具 key 数组。

    异常说明：
        无。
    """

    merged_tool_keys: list[str] = []
    for raw_tool_key in [*existing_tool_keys, *requested_tool_keys]:
        tool_key = str(raw_tool_key or "").strip()
        if not tool_key:
            continue
        if tool_key in merged_tool_keys:
            continue
        merged_tool_keys.append(tool_key)
    return merged_tool_keys


def extract_loaded_tool_keys_from_stream_result(stream_result: dict[str, Any]) -> list[str]:
    """
    功能描述：
        从单次 agent 流式执行结果中提取最终已加载工具数组。

    参数说明：
        stream_result (dict[str, Any]): `agent_stream` 返回结构。

    返回值：
        list[str]: 规范化后的已加载工具 key 数组。

    异常说明：
        无。
    """

    latest_state = stream_result.get("latest_state")
    normalized_tool_keys = normalize_loaded_tool_keys(latest_state)
    if normalized_tool_keys is None:
        return []
    return normalized_tool_keys


class DynamicToolMiddleware(AgentMiddleware):
    """
    功能描述：
        根据状态中的 `loaded_tool_keys` 动态过滤管理工具。

    参数说明：
        registry (DynamicToolRegistryProtocol): 动态工具注册中心。

    返回值：
        无（中间件对象）。

    异常说明：
        无。
    """

    def __init__(self, *, registry: DynamicToolRegistryProtocol) -> None:
        """
        功能描述：
            初始化动态工具中间件。

        参数说明：
            registry (DynamicToolRegistryProtocol): 动态工具注册中心。

        返回值：
            None。

        异常说明：
            无。
        """

        self._registry = registry

    def _filter_request_tools(self, request: ModelRequest) -> ModelRequest:
        """
        功能描述：
            过滤当前模型请求中的工具列表。

        参数说明：
            request (ModelRequest): 当前模型请求对象。

        返回值：
            ModelRequest: 已应用动态工具过滤后的请求对象。

        异常说明：
            无。
        """

        request_tools = list(request.tools)
        state_dict = request.state if isinstance(request.state, Mapping) else {}
        loaded_tool_keys = normalize_loaded_tool_keys(state_dict)
        visible_tools = self._registry.filter_visible_tools(
            request_tools=request_tools,
            loaded_tool_keys=loaded_tool_keys,
        )
        return request.override(tools=visible_tools)

    def wrap_model_call(
            self,
            request: ModelRequest,
            handler: Callable[[ModelRequest], ModelResponse],
    ) -> ModelResponse:
        """
        功能描述：
            在同步模型调用前执行工具过滤。

        参数说明：
            request (ModelRequest): 当前模型请求对象。
            handler (Callable[[ModelRequest], ModelResponse]): 下游处理器。

        返回值：
            ModelResponse: 下游模型响应。

        异常说明：
            无。
        """

        filtered_request = self._filter_request_tools(request)
        return handler(filtered_request)

    async def awrap_model_call(
            self,
            request: ModelRequest,
            handler: Callable[[ModelRequest], Awaitable[ModelResponse]],
    ) -> ModelResponse:
        """
        功能描述：
            在异步模型调用前执行工具过滤。

        参数说明：
            request (ModelRequest): 当前模型请求对象。
            handler (Callable[[ModelRequest], Awaitable[ModelResponse]]): 下游处理器。

        返回值：
            ModelResponse: 下游模型响应。

        异常说明：
            无。
        """

        filtered_request = self._filter_request_tools(request)
        return await handler(filtered_request)


class ManagedDynamicToolRegistry:
    """
    功能描述：
        动态工具注册中心共享基类。

    参数说明：
        business_tools_by_domain (dict[str, tuple[BaseTool, ...]]): 按领域分组的业务工具映射。
        extra_base_tools (tuple[BaseTool, ...]): 额外始终可见的基础工具数组。
        text_config (DynamicToolingTextConfig): 动态工具文案配置。

    返回值：
        无（注册中心对象）。

    异常说明：
        ValueError: 当工具 key 重复或缺失时抛出。
    """

    def __init__(
            self,
            *,
            business_tools_by_domain: dict[str, tuple[BaseTool, ...]],
            extra_base_tools: tuple[BaseTool, ...] = (),
            text_config: DynamicToolingTextConfig,
    ) -> None:
        """
        功能描述：
            初始化动态工具注册中心并构建工具索引。

        参数说明：
            business_tools_by_domain (dict[str, tuple[BaseTool, ...]]): 按领域分组的业务工具映射。
            extra_base_tools (tuple[BaseTool, ...]): 额外始终可见的基础工具数组。
            text_config (DynamicToolingTextConfig): 动态工具文案配置。

        返回值：
            None。

        异常说明：
            ValueError: 当工具 key 重复或非法时抛出。
        """

        self._text_config = text_config
        self._business_tools_by_domain = dict(business_tools_by_domain)
        self._business_tools: tuple[BaseTool, ...] = tuple(
            tool_obj
            for domain_tools in self._business_tools_by_domain.values()
            for tool_obj in domain_tools
        )
        self._list_loadable_tools = create_list_loadable_tools_tool(
            get_tool_catalog=self.get_business_tool_catalog,
            text_config=self._text_config,
        )
        self._load_tools = create_load_tools_tool(
            get_allowed_tool_keys=self.get_business_tool_key_set,
            text_config=self._text_config,
        )
        self._base_tools: tuple[BaseTool, ...] = (
            self._list_loadable_tools,
            self._load_tools,
            *extra_base_tools,
        )
        self._managed_tools: tuple[BaseTool, ...] = (
            *self._base_tools,
            *self._business_tools,
        )
        self._tool_by_key = self._build_tool_index(self._managed_tools)

    @staticmethod
    def _build_tool_index(tools: tuple[BaseTool, ...]) -> dict[str, BaseTool]:
        """
        功能描述：
            根据工具对象数组构建 `tool_key -> tool` 索引。

        参数说明：
            tools (tuple[BaseTool, ...]): 参与注册的工具数组。

        返回值：
            dict[str, BaseTool]: 工具索引字典。

        异常说明：
            ValueError: 当工具 key 缺失或重复时抛出。
        """

        tool_index: dict[str, BaseTool] = {}
        for tool_obj in tools:
            tool_key = str(getattr(tool_obj, "name", "") or "").strip()
            if not tool_key:
                raise ValueError("工具缺少 name，无法注册")
            if tool_key in tool_index:
                raise ValueError(f"工具 key 重复：{tool_key}")
            tool_index[tool_key] = tool_obj
        return tool_index

    @property
    def all_tools(self) -> list[BaseTool]:
        """
        功能描述：
            返回 `create_agent` 使用的全量已注册工具列表。

        参数说明：
            无。

        返回值：
            list[BaseTool]: 全量工具列表。

        异常说明：
            无。
        """

        return list(self._managed_tools)

    @property
    def base_tools(self) -> list[BaseTool]:
        """
        功能描述：
            返回默认直接暴露给模型的基础工具列表。

        参数说明：
            无。

        返回值：
            list[BaseTool]: 基础工具列表。

        异常说明：
            无。
        """

        return list(self._base_tools)

    def get_business_tool_key_set(self) -> set[str]:
        """
        功能描述：
            返回业务工具 key 集合。

        参数说明：
            无。

        返回值：
            set[str]: 业务工具 key 集合。

        异常说明：
            无。
        """

        return {
            str(tool_obj.name).strip()
            for tool_obj in self._business_tools
        }

    def get_business_tool_catalog(self) -> dict[str, list[str]]:
        """
        功能描述：
            返回按领域分组的业务工具目录。

        参数说明：
            无。

        返回值：
            dict[str, list[str]]: 按领域分组的业务工具精确名称映射。

        异常说明：
            无。
        """

        business_tool_catalog: dict[str, list[str]] = {}
        for domain_name, domain_tools in self._business_tools_by_domain.items():
            business_tool_catalog[domain_name] = [
                str(tool_obj.name).strip()
                for tool_obj in domain_tools
                if str(tool_obj.name).strip()
            ]
        return business_tool_catalog

    def get_base_tool_key_set(self) -> set[str]:
        """
        功能描述：
            返回基础工具 key 集合。

        参数说明：
            无。

        返回值：
            set[str]: 基础工具 key 集合。

        异常说明：
            无。
        """

        return {
            str(tool_obj.name).strip()
            for tool_obj in self._base_tools
        }

    def get_managed_tool_key_set(self) -> set[str]:
        """
        功能描述：
            返回注册中心管理的全部工具 key 集合。

        参数说明：
            无。

        返回值：
            set[str]: 全量工具 key 集合。

        异常说明：
            无。
        """

        return set(self._tool_by_key.keys())

    def resolve_visible_tool_key_set(self, loaded_tool_keys: list[str] | None) -> set[str]:
        """
        功能描述：
            计算当前模型可见的管理工具 key 集合。

        参数说明：
            loaded_tool_keys (list[str] | None): 当前状态中的已加载工具 key 数组。

        返回值：
            set[str]: 当前应暴露给模型的管理工具 key 集合。

        异常说明：
            无。
        """

        visible_tool_keys = self.get_base_tool_key_set()
        business_tool_keys = self.get_business_tool_key_set()
        if not loaded_tool_keys:
            return visible_tool_keys

        for raw_tool_key in loaded_tool_keys:
            tool_key = str(raw_tool_key or "").strip()
            if not tool_key:
                continue
            if tool_key not in business_tool_keys:
                continue
            visible_tool_keys.add(tool_key)
        return visible_tool_keys

    def filter_visible_tools(
            self,
            *,
            request_tools: list[Any],
            loaded_tool_keys: list[str] | None,
    ) -> list[Any]:
        """
        功能描述：
            过滤当前请求中的工具列表，仅保留当前可见的管理工具。

        参数说明：
            request_tools (list[Any]): 当前请求中的工具对象数组。
            loaded_tool_keys (list[str] | None): 当前状态中的已加载工具 key 数组。

        返回值：
            list[Any]:
                保留当前可见的管理工具，并始终保留非注册中心管理工具。

        异常说明：
            无。
        """

        visible_tool_keys = self.resolve_visible_tool_key_set(loaded_tool_keys)
        managed_tool_keys = self.get_managed_tool_key_set()
        visible_tools: list[Any] = []
        for tool_obj in request_tools:
            tool_key = str(getattr(tool_obj, "name", "") or "").strip()
            if not tool_key:
                visible_tools.append(tool_obj)
                continue
            if tool_key not in managed_tool_keys:
                visible_tools.append(tool_obj)
                continue
            if tool_key in visible_tool_keys:
                visible_tools.append(tool_obj)
        return visible_tools


def create_list_loadable_tools_tool(
        *,
        get_tool_catalog: Callable[[], dict[str, list[str]]],
        text_config: DynamicToolingTextConfig,
) -> Any:
    """
    功能描述：
        创建查看可加载业务工具目录的基础工具。

    参数说明：
        get_tool_catalog (Callable[[], dict[str, list[str]]]): 返回按领域分组的业务工具目录回调函数。
        text_config (DynamicToolingTextConfig): 动态工具文案配置。

    返回值：
        Any: LangChain Tool 对象，请求名固定为 `list_loadable_tools`。

    异常说明：
        无。
    """

    @tool(description=text_config.list_description)
    @tool_thinking_redaction(display_name=text_config.list_tool_name)
    @tool_call_status(
        tool_name=text_config.list_tool_name,
        start_message=text_config.list_start_message,
        error_message=text_config.list_error_message,
        timely_message=text_config.list_timely_message,
    )
    def list_loadable_tools() -> LoadableToolsCatalog:
        """
        功能描述：
            返回当前允许加载的业务工具精确名称目录。

        参数说明：
            无。

        返回值：
            LoadableToolsCatalog: 可加载工具目录结构。

        异常说明：
            无。
        """

        tools_by_domain = get_tool_catalog()
        exact_tool_names: list[str] = []
        for domain_tool_names in tools_by_domain.values():
            for raw_tool_name in domain_tool_names:
                tool_name = str(raw_tool_name or "").strip()
                if not tool_name:
                    continue
                if tool_name in exact_tool_names:
                    continue
                exact_tool_names.append(tool_name)

        return LoadableToolsCatalog(
            exact_tool_names=exact_tool_names,
            tools_by_domain=tools_by_domain,
            supports_multi_load=True,
            usage_tip=text_config.list_usage_tip,
        )

    return list_loadable_tools


def create_load_tools_tool(
        *,
        get_allowed_tool_keys: Callable[[], set[str]],
        text_config: DynamicToolingTextConfig,
) -> Any:
    """
    功能描述：
        创建运行时工具动态加载工具。

    参数说明：
        get_allowed_tool_keys (Callable[[], set[str]]): 返回当前允许加载的业务工具 key 集合的回调函数。
        text_config (DynamicToolingTextConfig): 动态工具文案配置。

    返回值：
        Any: LangChain Tool 对象，请求名固定为 `load_tools`。

    异常说明：
        ValueError: 当加载的工具 key 不存在于允许集合中时由内部函数抛出。
    """

    @tool(description=text_config.load_description)
    @tool_thinking_redaction(display_name=text_config.load_tool_name)
    @tool_call_status(
        tool_name=text_config.load_tool_name,
        start_message=text_config.load_start_message,
        error_message=text_config.load_error_message,
        timely_message=text_config.load_timely_message,
    )
    def load_tools(
            tool_keys: list[str],
            runtime: ToolRuntime[None, Any],
    ) -> Command:
        """
        功能描述：
            为当前 agent 运行加载额外业务工具，并把加载结果写入状态。

        参数说明：
            tool_keys (list[str]): 需要加载的业务工具 key 数组。
            runtime (ToolRuntime[None, Any]): 当前工具运行时上下文。

        返回值：
            Command:
                更新 `loaded_tool_keys` 与一条 ToolMessage，
                使后续模型调用可以看到本次已加载的业务工具。

        异常说明：
            ValueError:
                - 当 `tool_keys` 入参结构非法时由模型校验抛出；
                - 当加载了未注册的业务工具 key 时抛出；
                - 当状态中的 `loaded_tool_keys` 结构非法时不会抛错，仅按空数组处理。
        """

        validated_request = LoadToolsRequest.model_validate(
            {
                "tool_keys": tool_keys,
            }
        )
        normalized_tool_keys = validated_request.tool_keys
        allowed_tool_keys = get_allowed_tool_keys()
        unresolved_tool_keys = [
            tool_key
            for tool_key in normalized_tool_keys
            if tool_key not in allowed_tool_keys
        ]
        if unresolved_tool_keys:
            raise ValueError(
                "不允许加载以下工具: " + ", ".join(unresolved_tool_keys)
            )

        current_state = runtime.state if isinstance(runtime.state, Mapping) else {}
        current_loaded_tool_keys = normalize_loaded_tool_keys(current_state) or []
        merged_tool_keys = merge_unique_loaded_tool_keys(
            existing_tool_keys=current_loaded_tool_keys,
            requested_tool_keys=normalized_tool_keys,
        )

        tool_message_lines = [
            text_config.load_success_prefix + ", ".join(normalized_tool_keys),
            text_config.load_completion_message,
        ]
        tool_message = ToolMessage(
            content="\n".join(tool_message_lines),
            tool_call_id=runtime.tool_call_id,
        )
        return Command(
            update={
                "messages": [tool_message],
                "loaded_tool_keys": merged_tool_keys,
            }
        )

    return load_tools


__all__ = [
    "DynamicToolMiddleware",
    "DynamicToolRegistryProtocol",
    "DynamicToolingTextConfig",
    "LoadToolsRequest",
    "LoadableToolsCatalog",
    "ManagedDynamicToolRegistry",
    "create_list_loadable_tools_tool",
    "create_load_tools_tool",
    "extract_loaded_tool_keys_from_stream_result",
    "merge_unique_loaded_tool_keys",
    "normalize_loaded_tool_keys",
]
