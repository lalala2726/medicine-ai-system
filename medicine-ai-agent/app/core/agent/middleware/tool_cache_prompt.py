from __future__ import annotations

from collections.abc import Awaitable, Callable, Mapping

from langchain.agents.middleware import AgentMiddleware, ModelRequest, ModelResponse
from langchain_core.messages import SystemMessage

from app.core.agent.tool_cache import ToolCacheProfile, render_tool_cache_prompt
from app.utils.prompt_section_utils import find_section_span, normalize_text

# 工具缓存提示词段落开始标记模板，用于定位与替换已注入区块。
_TOOL_CACHE_SECTION_START_TEMPLATE = "<!-- tool_cache_section:{profile_key}:start -->"
# 工具缓存提示词段落结束标记模板，用于定位与替换已注入区块。
_TOOL_CACHE_SECTION_END_TEMPLATE = "<!-- tool_cache_section:{profile_key}:end -->"
# 工具缓存提示词标题前缀。
_TOOL_CACHE_SECTION_HEADING_PREFIX = "## "


class ToolCachePromptMiddleware(AgentMiddleware):
    """工具缓存提示词中间件。

    作用：
        1. 根据 `ToolCacheProfile` 和当前 `conversation_uuid` 渲染工具缓存提示词；
        2. 在模型调用前把缓存段落幂等注入到 `system_message`；
        3. 仅处理提示词注入，不负责工具缓存上下文的 bind/reset 生命周期。
    """

    def __init__(
            self,
            *,
            profile: ToolCacheProfile,
            section_title: str | None = None,
    ) -> None:
        """初始化工具缓存提示词中间件。

        参数：
            profile: 工具缓存 profile，用于读取缓存配置与渲染策略。
            section_title: 注入段落标题；为空时默认使用 `profile.prompt_title`。
        """

        self.profile = profile
        self.section_title = normalize_text(str(section_title or profile.prompt_title or "").strip())
        self._section_start_marker = _TOOL_CACHE_SECTION_START_TEMPLATE.format(
            profile_key=self.profile.key_prefix,
        )
        self._section_end_marker = _TOOL_CACHE_SECTION_END_TEMPLATE.format(
            profile_key=self.profile.key_prefix,
        )

    def _get_conversation_uuid(self, request: ModelRequest) -> str:
        """从请求状态中提取当前会话 UUID。

        参数：
            request: 当前模型请求对象。

        返回：
            str: 当前会话 UUID；不存在时返回空字符串。
        """

        state = request.state
        if not isinstance(state, Mapping):
            return ""
        return str(state.get("conversation_uuid") or "").strip()

    def _build_tool_cache_section(self, rendered_cache_prompt: str) -> str:
        """构建待注入的工具缓存提示词段落。

        参数：
            rendered_cache_prompt: `render_tool_cache_prompt(...)` 返回的缓存提示词文本。

        返回：
            str: 完整的工具缓存段落；当缓存文本为空时返回空字符串。
        """

        normalized_cache_prompt = normalize_text(rendered_cache_prompt)
        if not normalized_cache_prompt:
            return ""

        section_lines: list[str] = [
            self._section_start_marker,
        ]
        if self.section_title:
            section_lines.extend(
                [
                    f"{_TOOL_CACHE_SECTION_HEADING_PREFIX}{self.section_title}",
                    "",
                ]
            )
        section_lines.extend(
            [
                normalized_cache_prompt,
                self._section_end_marker,
            ]
        )
        return normalize_text("\n".join(section_lines))

    @staticmethod
    def _merge_text(
            *,
            before_section: str,
            section_text: str,
            after_section: str,
    ) -> str:
        """合并提示词片段并处理空白边界。

        参数：
            before_section: 目标段落前的文本。
            section_text: 当前要插入的目标段落文本。
            after_section: 目标段落后的文本。

        返回：
            str: 合并后的完整提示词文本。
        """

        normalized_before = before_section.rstrip()
        normalized_section = normalize_text(section_text)
        normalized_after = after_section.lstrip()
        parts = [
            part
            for part in (normalized_before, normalized_section, normalized_after)
            if part
        ]
        return "\n\n".join(parts)

    def _inject_tool_cache_prompt(self, request: ModelRequest) -> ModelRequest:
        """向系统消息注入工具缓存提示词。

        参数：
            request: 当前模型请求对象。

        返回：
            ModelRequest: 注入完成后的请求对象。
        """

        system_message = request.system_message
        current_text = normalize_text(system_message.text) if system_message is not None else ""
        existing_span = find_section_span(
            current_text,
            self._section_start_marker,
            self._section_end_marker,
        )

        conversation_uuid = self._get_conversation_uuid(request)
        if not conversation_uuid:
            if existing_span is None:
                return request
            start_index, end_index = existing_span
            merged_text = self._merge_text(
                before_section=current_text[:start_index],
                section_text="",
                after_section=current_text[end_index:],
            )
            return request.override(system_message=SystemMessage(content=merged_text))

        rendered_cache_prompt = render_tool_cache_prompt(
            self.profile,
            conversation_uuid,
        )
        tool_cache_section = self._build_tool_cache_section(rendered_cache_prompt)

        if not tool_cache_section:
            if existing_span is None:
                return request
            start_index, end_index = existing_span
            merged_text = self._merge_text(
                before_section=current_text[:start_index],
                section_text="",
                after_section=current_text[end_index:],
            )
            return request.override(system_message=SystemMessage(content=merged_text))

        if existing_span is not None:
            start_index, end_index = existing_span
            existing_section = current_text[start_index:end_index]
            if normalize_text(existing_section) == tool_cache_section:
                return request
            merged_text = self._merge_text(
                before_section=current_text[:start_index],
                section_text=tool_cache_section,
                after_section=current_text[end_index:],
            )
            return request.override(system_message=SystemMessage(content=merged_text))

        merged_text = self._merge_text(
            before_section=current_text,
            section_text=tool_cache_section,
            after_section="",
        )
        return request.override(system_message=SystemMessage(content=merged_text))

    def wrap_model_call(
            self,
            request: ModelRequest,
            handler: Callable[[ModelRequest], ModelResponse],
    ) -> ModelResponse:
        """同步包装模型调用，注入工具缓存提示词后继续执行。"""

        modified_request = self._inject_tool_cache_prompt(request)
        return handler(modified_request)

    async def awrap_model_call(
            self,
            request: ModelRequest,
            handler: Callable[[ModelRequest], Awaitable[ModelResponse]],
    ) -> ModelResponse:
        """异步包装模型调用，注入工具缓存提示词后继续执行。"""

        modified_request = self._inject_tool_cache_prompt(request)
        return await handler(modified_request)
