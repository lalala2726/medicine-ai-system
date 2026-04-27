from __future__ import annotations

from collections.abc import Awaitable, Callable, Mapping

from langchain.agents.middleware import AgentMiddleware, ModelRequest, ModelResponse
from langchain_core.messages import SystemMessage

from app.core.agent.tool_trace import (
    DEFAULT_TOOL_TRACE_PROMPT_LIMIT,
    render_tool_trace_prompt,
)
from app.utils.prompt_section_utils import find_section_span, normalize_text

# 工具轨迹提示词段落开始标记模板。
_TOOL_TRACE_SECTION_START_TEMPLATE = "<!-- tool_trace_section:{section_key}:start -->"
# 工具轨迹提示词段落结束标记模板。
_TOOL_TRACE_SECTION_END_TEMPLATE = "<!-- tool_trace_section:{section_key}:end -->"
# 工具轨迹提示词标题前缀。
_TOOL_TRACE_SECTION_HEADING_PREFIX = "## "


class ToolTracePromptMiddleware(AgentMiddleware):
    """工具轨迹提示词中间件。

    作用：
        1. 每次模型调用前按会话 UUID 读取最近工具轨迹；
        2. 将轨迹摘要幂等注入到系统提示词；
        3. 仅负责提示词注入，不负责轨迹上下文的 bind/reset 生命周期。
    """

    def __init__(
            self,
            *,
            section_key: str = "recent",
            section_title: str = "最近工具调用轨迹",
            limit: int = DEFAULT_TOOL_TRACE_PROMPT_LIMIT,
    ) -> None:
        """初始化工具轨迹提示词中间件。

        Args:
            section_key: 段落唯一键，用于生成区块标记。
            section_title: 注入段落标题。
            limit: 每次读取的最近轨迹条数。
        """

        self.section_key = normalize_text(str(section_key or "").strip())
        self.section_title = normalize_text(str(section_title or "").strip())
        self.limit = limit
        self._section_start_marker = _TOOL_TRACE_SECTION_START_TEMPLATE.format(
            section_key=self.section_key or "recent",
        )
        self._section_end_marker = _TOOL_TRACE_SECTION_END_TEMPLATE.format(
            section_key=self.section_key or "recent",
        )

    def _get_conversation_uuid(self, request: ModelRequest) -> str:
        """从请求状态中提取当前会话 UUID。

        Args:
            request: 当前模型请求对象。

        Returns:
            str: 当前会话 UUID；不存在时返回空字符串。
        """

        state = request.state
        if not isinstance(state, Mapping):
            return ""
        return str(state.get("conversation_uuid") or "").strip()

    def _build_tool_trace_section(self, rendered_trace_prompt: str) -> str:
        """构建待注入的工具轨迹提示词段落。

        Args:
            rendered_trace_prompt: `render_tool_trace_prompt(...)` 返回的轨迹摘要正文。

        Returns:
            str: 完整的工具轨迹段落；没有正文时返回空字符串。
        """

        normalized_trace_prompt = normalize_text(rendered_trace_prompt)
        if not normalized_trace_prompt:
            return ""

        section_lines: list[str] = [self._section_start_marker]
        if self.section_title:
            section_lines.extend(
                [
                    f"{_TOOL_TRACE_SECTION_HEADING_PREFIX}{self.section_title}",
                    "",
                ]
            )
        section_lines.extend(
            [
                normalized_trace_prompt,
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

        Args:
            before_section: 目标段落前的文本。
            section_text: 当前要插入的目标段落文本。
            after_section: 目标段落后的文本。

        Returns:
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

    def _inject_tool_trace_prompt(self, request: ModelRequest) -> ModelRequest:
        """向系统消息注入工具轨迹提示词。

        Args:
            request: 当前模型请求对象。

        Returns:
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

        rendered_trace_prompt = render_tool_trace_prompt(
            conversation_uuid=conversation_uuid,
            limit=self.limit,
        )
        tool_trace_section = self._build_tool_trace_section(rendered_trace_prompt)
        if not tool_trace_section:
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
            if normalize_text(existing_section) == tool_trace_section:
                return request
            merged_text = self._merge_text(
                before_section=current_text[:start_index],
                section_text=tool_trace_section,
                after_section=current_text[end_index:],
            )
            return request.override(system_message=SystemMessage(content=merged_text))

        merged_text = self._merge_text(
            before_section=current_text,
            section_text=tool_trace_section,
            after_section="",
        )
        return request.override(system_message=SystemMessage(content=merged_text))

    def wrap_model_call(
            self,
            request: ModelRequest,
            handler: Callable[[ModelRequest], ModelResponse],
    ) -> ModelResponse:
        """同步包装模型调用，注入工具轨迹提示词后继续执行。"""

        modified_request = self._inject_tool_trace_prompt(request)
        return handler(modified_request)

    async def awrap_model_call(
            self,
            request: ModelRequest,
            handler: Callable[[ModelRequest], Awaitable[ModelResponse]],
    ) -> ModelResponse:
        """异步包装模型调用，注入工具轨迹提示词后继续执行。"""

        modified_request = self._inject_tool_trace_prompt(request)
        return await handler(modified_request)
