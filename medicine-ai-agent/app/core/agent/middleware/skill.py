from __future__ import annotations

from collections.abc import Awaitable, Callable
from typing import Any, NotRequired, TypedDict, cast

from langchain.agents.middleware import AgentMiddleware, ModelRequest, ModelResponse
from langchain_core.messages import SystemMessage

from app.core.agent.skill.discovery.metadata import discover_skills_metadata
from app.core.agent.skill.discovery.scope import normalize_scope
from app.core.agent.skill.prompt.templates import (
    build_skills_prompt,
    load_skills_system_prompt_template,
)
from app.core.agent.skill.tool.list_skill_resources import create_list_skill_resources_tool
from app.core.agent.skill.tool.load_skill import create_load_skill_resource_tool, create_load_skill_tool
from app.core.agent.skill.types.models import SkillFileIndex, SkillMetadata
from app.utils.prompt_section_utils import (
    contains_block,
    find_section_span,
    normalize_text,
    split_template,
)


def _normalize_middleware_scopes(
        scope: str | None,
        skill_scope: str | None,
) -> tuple[str, str | None, str]:
    """规范化 SkillMiddleware 的作用域参数并执行互斥校验。

    功能描述：
        统一处理中间件初始化时的 `scope/skill_scope` 参数，确保二者不能同时生效，
        并返回可直接用于发现逻辑与工具注册的标准化结果。

    参数说明：
        scope (str | None): 历史目录扫描作用域；为空时表示技能根目录。
        skill_scope (str | None): 单技能直读作用域；启用后读取 `<skill_scope>/SKILL.md`。

    返回值：
        tuple[str, str | None, str]:
            - 规范化后的 `scope`（目录扫描模式）；
            - 规范化后的 `skill_scope`（未启用时为 `None`）；
            - 工具展示与错误提示使用的生效作用域标签。

    异常说明：
        ValueError: 当 `scope` 与 `skill_scope` 同时非空时抛出。
    """

    normalized_scope_input = str(scope or "").strip()
    normalized_skill_scope_input = str(skill_scope or "").strip()
    if normalized_scope_input and normalized_skill_scope_input:
        raise ValueError("SkillMiddleware arguments are mutually exclusive: scope and skill_scope.")

    normalized_scope, _ = normalize_scope(scope)
    if normalized_skill_scope_input:
        normalized_skill_scope, _ = normalize_scope(skill_scope)
        return normalized_scope, normalized_skill_scope, normalized_skill_scope

    return normalized_scope, None, normalized_scope


class SkillMiddlewareState(TypedDict, total=False):
    """技能中间件状态结构。

    作用：
        声明运行时需要保留的中间件状态键，避免被 LangChain 状态系统丢弃。

    字段：
        skills_metadata: 预加载后的技能元数据列表。
    """

    skills_metadata: NotRequired[list[SkillMetadata]]


class SkillMiddleware(AgentMiddleware):
    """技能中间件。

    作用：
        1. 在 `before_agent/abefore_agent` 阶段预加载技能元数据；
        2. 在模型调用前向系统提示词注入技能列表与使用说明；
        3. 注册技能工具，支持按名称懒加载技能文件与资源文件。
    """

    state_schema = SkillMiddlewareState

    def __init__(
            self,
            scope: str | None = None,
            system_prompt_template: str | None = None,
            *,
            skill_scope: str | None = None,
    ):
        """初始化技能中间件。

        功能描述：
            初始化技能发现参数、提示词模板与三类技能工具，并对入参执行互斥校验。

        参数说明：
            scope (str | None): 目录扫描作用域，例如 `supervisor`、`supervisor/a`。
                为空时默认扫描技能根目录，默认 `None`。
            system_prompt_template (str | None): 技能提示词模板，为空时动态读取统一管理模板。
            skill_scope (str | None): 单技能直读作用域，例如 `chart`；
                启用后仅读取 `<skill_scope>/SKILL.md`，默认 `None`。

        返回值：
            None。

        异常说明：
            ValueError: 当 `scope` 与 `skill_scope` 同时非空时抛出。
        """

        normalized_scope, normalized_skill_scope, effective_scope = _normalize_middleware_scopes(
            scope,
            skill_scope,
        )
        self.scope = normalized_scope
        self.skill_scope = normalized_skill_scope
        self._effective_scope = effective_scope
        self._system_prompt_template = system_prompt_template
        self._skill_file_index: SkillFileIndex = {}
        self._load_skill_tool = create_load_skill_tool(
            self._effective_scope,
            get_skill_file_index=lambda: self._skill_file_index,
        )
        self._load_skill_resource_tool = create_load_skill_resource_tool(
            self._effective_scope,
            get_skill_file_index=lambda: self._skill_file_index,
        )
        self._list_skill_resources_tool = create_list_skill_resources_tool(
            self._effective_scope,
            get_skill_file_index=lambda: self._skill_file_index,
        )
        self.tools = [
            self._load_skill_tool,
            self._load_skill_resource_tool,
            self._list_skill_resources_tool,
        ]

    def _build_skills_section(self, skills_metadata: list[SkillMetadata]) -> str:
        """构建技能提示词段落。

        参数：
            skills_metadata: 技能元数据列表（必填字段 + 可选字段）。

        返回：
            str: 渲染后的技能系统提示词文本。
        """

        return build_skills_prompt(
            skills_metadata,
            system_prompt_template=self._system_prompt_template,
        )

    def _inject_skills_prompt(self, request: ModelRequest) -> ModelRequest:
        """向请求的系统消息注入技能提示词。

        参数：
            request: 当前模型请求对象。

        返回：
            ModelRequest: 注入后（或原样）的模型请求对象。

        工作原理：
            1. 从 `request.state` 读取 `skills_metadata`，渲染出本次应注入的技能段；
            2. 通过模板占位符 `{skills_list}` 拆出前后缀，并在现有 `system_text`
               中定位已存在技能段；
            3. 定位命中时：
               - 若旧段与新段规范化后相同：不变更（幂等）；
               - 若不同：执行“替换”而非追加，保证只有一份技能段；
            4. 定位未命中时：
               - 若按块边界已包含相同技能段：不追加；
               - 否则在末尾追加技能段；
            5. 通过 `request.override(system_message=...)` 返回更新后的请求。

        设计目的：
            去除对固定标题文案的耦合，避免模板标题改动导致重复注入。
        """

        # 元数据合法性由文件发现阶段统一校验，这里不重复做字段级校验。
        state_dict = cast(dict[str, Any], cast(object, request.state))
        raw_metadata = state_dict.get("skills_metadata", [])
        skills_metadata = raw_metadata if isinstance(raw_metadata, list) else []

        skills_section = normalize_text(self._build_skills_section(skills_metadata))
        if not skills_section:
            return request

        system_message = request.system_message
        system_text = normalize_text(system_message.text) if system_message is not None else ""

        template_for_match = self._system_prompt_template or load_skills_system_prompt_template()
        prefix, suffix = split_template(template_for_match)
        skills_span = find_section_span(system_text, prefix, suffix)
        if skills_span is not None:
            start_index, end_index = skills_span
            existing_section = system_text[start_index:end_index]
            if normalize_text(existing_section) == skills_section:
                return request

            before_section = system_text[:start_index].rstrip()
            after_section = system_text[end_index:].lstrip()
            if before_section and after_section:
                merged_text = f"{before_section}\n\n{skills_section}\n\n{after_section}"
            elif before_section:
                merged_text = f"{before_section}\n\n{skills_section}"
            elif after_section:
                merged_text = f"{skills_section}\n\n{after_section}"
            else:
                merged_text = skills_section
            return request.override(system_message=SystemMessage(content=merged_text))

        if contains_block(system_text, skills_section):
            return request

        merged_text = f"{system_text.rstrip()}\n\n{skills_section}" if system_text else skills_section
        return request.override(system_message=SystemMessage(content=merged_text))

    def before_agent(self, state: dict[str, Any], runtime: Any) -> dict[str, Any] | None:
        """同步阶段预加载技能元数据并刷新内部索引。

        参数：
            state: 运行时状态字典。
            runtime: 运行时上下文（此处不直接使用）。

        返回：
            dict[str, Any] | None:
                - 当 `state` 中尚无 `skills_metadata` 时，返回增量状态；
                - 若已存在则返回 `None`（幂等）。
        """

        _ = runtime
        skills_metadata, skill_file_index = discover_skills_metadata(
            self.scope,
            skill_scope=self.skill_scope,
        )
        self._skill_file_index = skill_file_index
        if "skills_metadata" in state:
            return None
        return {"skills_metadata": skills_metadata}

    async def abefore_agent(self, state: dict[str, Any], runtime: Any) -> dict[str, Any] | None:

        """异步阶段预加载技能元数据。

        参数：
            state: 运行时状态字典。
            runtime: 运行时上下文。

        返回：
            dict[str, Any] | None: 与 `before_agent` 保持一致。
        """

        return self.before_agent(state, runtime)

    def wrap_model_call(
            self,
            request: ModelRequest,
            handler: Callable[[ModelRequest], ModelResponse],
    ):
        """同步包装模型调用，注入技能提示词后继续执行。

        参数：
            request: 当前模型请求。
            handler: 下游模型调用处理器。

        返回：
            ModelResponse: 下游处理器返回的模型响应。
        """

        modified_request = self._inject_skills_prompt(request)
        return handler(modified_request)

    async def awrap_model_call(
            self,
            request: ModelRequest,
            handler: Callable[[ModelRequest], Awaitable[ModelResponse]],
    ):
        """异步包装模型调用，注入技能提示词后继续执行。

        参数：
            request: 当前模型请求。
            handler: 下游异步模型调用处理器。

        返回：
            ModelResponse: 下游处理器返回的模型响应。
        """

        modified_request = self._inject_skills_prompt(request)
        return await handler(modified_request)
