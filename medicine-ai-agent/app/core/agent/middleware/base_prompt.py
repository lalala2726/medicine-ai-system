from __future__ import annotations

from collections.abc import Awaitable, Callable

from langchain.agents.middleware import AgentMiddleware, ModelRequest, ModelResponse
from langchain_core.messages import SystemMessage

from app.core.agent.skill.prompt.templates import load_skills_system_prompt_template
from app.utils.prompt_section_utils import (
    contains_block,
    find_section_span,
    normalize_text,
    split_template,
)
from app.utils.prompt_utils import load_managed_prompt


class BasePromptMiddleware(AgentMiddleware):
    """基础提示词中间件。

    作用：
        在模型调用前自动注入通用基础提示词，避免各节点手工拼接。

    设计目标：
        1. 幂等：同一次请求重复经过中间件时不重复追加；
        2. 与技能提示词兼容：若已存在技能提示词段落，基础提示词插入到其前方；
        3. 不改动状态与工具，仅处理 `system_message`。
    """

    def __init__(
            self,
            *,
            base_prompt_key: str = "system_base_prompt",
            base_prompt_local_path: str | None = "_system/base_prompt.md",
    ) -> None:
        """初始化基础提示词中间件。

        参数：
            base_prompt_key: 基础提示词业务键。
            base_prompt_local_path: 基础提示词本地回退路径（可选）。
        """

        self.base_prompt_key = base_prompt_key
        self.base_prompt_local_path = base_prompt_local_path

    def _build_base_section(self) -> str:
        """构建基础提示词段落文本。"""

        return normalize_text(
            load_managed_prompt(
                self.base_prompt_key,
                local_prompt_path=self.base_prompt_local_path,
            )
        )

    def _inject_base_prompt(self, request: ModelRequest) -> ModelRequest:
        """向系统消息注入基础提示词。

        功能：
            在不依赖固定标题文案（如 `## 基础系统规则`）的前提下，
            保证基础提示词“只注入一次”，并尽量位于技能提示词前。

        工作原理：
            1. 读取当前 `system_message` 并做文本规范化；
            2. 若基础提示词为空，直接返回（不注入空块）；
            3. 若当前文本已按块边界包含基础提示词，直接返回（幂等）；
            4. 否则尝试通过技能模板前后缀定位技能段：
               - 命中技能段：将基础提示词插入到技能段前；
               - 未命中：将基础提示词追加到末尾；
            5. 最终通过 `request.override(system_message=...)` 返回新请求对象。

        设计目的：
            避免标题文案变更导致判定失效，同时保持基础规则与技能段的相对顺序稳定。
        """

        system_message = request.system_message
        current_text = normalize_text(system_message.text) if system_message is not None else ""

        base_section = self._build_base_section()
        if not base_section:
            return request

        if contains_block(current_text, base_section):
            return request

        if not current_text:
            merged_text = base_section
        else:
            skills_prefix, skills_suffix = split_template(load_skills_system_prompt_template())
            skills_span = find_section_span(current_text, skills_prefix, skills_suffix)
            if skills_span is not None:
                start_index, _ = skills_span
                before_skills = current_text[:start_index].rstrip()
                skills_and_after = current_text[start_index:].lstrip()
                if before_skills:
                    merged_text = f"{before_skills}\n\n{base_section}\n\n{skills_and_after}"
                else:
                    merged_text = f"{base_section}\n\n{skills_and_after}"
            else:
                merged_text = f"{current_text.rstrip()}\n\n{base_section}"

        return request.override(system_message=SystemMessage(content=merged_text))

    def wrap_model_call(
            self,
            request: ModelRequest,
            handler: Callable[[ModelRequest], ModelResponse],
    ) -> ModelResponse:
        """同步包装模型调用，注入基础提示词后继续执行。"""

        modified_request = self._inject_base_prompt(request)
        return handler(modified_request)

    async def awrap_model_call(
            self,
            request: ModelRequest,
            handler: Callable[[ModelRequest], Awaitable[ModelResponse]],
    ) -> ModelResponse:
        """异步包装模型调用，注入基础提示词后继续执行。"""

        modified_request = self._inject_base_prompt(request)
        return await handler(modified_request)
