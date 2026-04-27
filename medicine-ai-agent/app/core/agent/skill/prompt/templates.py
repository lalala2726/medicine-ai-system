from __future__ import annotations

from langchain_core.prompts import SystemMessagePromptTemplate

from app.core.agent.skill.types.models import SkillMetadata
from app.utils.prompt_utils import load_managed_prompt

# 技能系统提示词业务键。
SKILLS_SYSTEM_PROMPT_KEY = "system_skill_prompt"
# 技能系统提示词本地回退路径。
SKILLS_SYSTEM_PROMPT_LOCAL_PATH = "_system/skill_system_prompt.md"


def load_skills_system_prompt_template() -> str:
    """读取技能系统提示词模板。"""

    return load_managed_prompt(
        SKILLS_SYSTEM_PROMPT_KEY,
        local_prompt_path=SKILLS_SYSTEM_PROMPT_LOCAL_PATH,
    ).strip()


# 兼容导出：保留同名常量，避免历史导入点直接失效。
SKILLS_SYSTEM_PROMPT = load_skills_system_prompt_template()


def _format_skills_list(skills_metadata: list[SkillMetadata]) -> str:
    """将技能元数据渲染为提示词列表文本。

    作用：
        将技能元数据按“原始字段”形式渲染为 YAML 风格块，直接提供给模型。

    参数：
        skills_metadata: 技能元数据列表。

    返回：
        str: 可直接插入模板的技能列表文本。
    """

    if not skills_metadata:
        return "- （暂无可用技能）"

    items: list[str] = []
    for item in skills_metadata:
        lines = [
            "```yaml",
            f"name: {item['name']}",
            f"description: {item.get('description') or ''}",
        ]

        license_name = str(item.get("license") or "").strip()
        if license_name:
            lines.append(f"license: {license_name}")

        extra_metadata = item.get("metadata")
        if isinstance(extra_metadata, dict):
            author = str(extra_metadata.get("author") or "").strip()
            version = str(extra_metadata.get("version") or "").strip()
            if author or version:
                lines.append("metadata:")
                if author:
                    lines.append(f"  author: {author}")
                if version:
                    lines.append(f"  version: {version}")

        lines.append("```")
        items.append("\n".join(lines))

    return "\n\n".join(items)


def build_skills_prompt(
        skills_metadata: list[SkillMetadata],
        *,
        system_prompt_template: str | None = None,
) -> str:
    """基于模板构建技能系统提示词段落。

    作用：
        使用 `SystemMessagePromptTemplate` 将 `{skills_list}` 占位符替换为
        当前可用技能列表，生成最终注入模型的文本。

    参数：
        skills_metadata: 技能元数据列表。
        system_prompt_template: 技能系统提示词模板，默认使用内置模板。

    返回：
        str: 渲染后的技能提示词文本。
    """

    template = SystemMessagePromptTemplate.from_template(
        system_prompt_template or load_skills_system_prompt_template()
    )
    formatted = template.format(skills_list=_format_skills_list(skills_metadata))
    return formatted.text
