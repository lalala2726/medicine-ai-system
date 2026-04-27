from __future__ import annotations

from collections.abc import Callable
from pathlib import Path
from typing import Any

from langchain_core.tools import tool

from app.core.agent.skill.discovery.scope import normalize_scope
from app.core.agent.middleware.tool_thinking_redaction import (
    tool_thinking_redaction,
)
from app.core.agent.skill.types.models import SkillFileIndex, SkillTreeNode, SkillTreeResponse

_MAX_TREE_DEPTH = 3


def _is_path_within_skill_root(path: Path, skill_root: Path) -> bool:
    """判断路径是否位于技能根目录内。

    作用：
        作为目录遍历过程中的安全边界校验，防止路径越界访问。

    参数：
        path: 待校验路径。
        skill_root: 技能根目录（通常是 `SKILL.md` 所在目录）。

    返回：
        bool: 在技能根目录内返回 `True`，否则返回 `False`。
    """

    try:
        return path.resolve().is_relative_to(skill_root.resolve())
    except OSError:
        return False


def _should_skip_entry(path: Path, skill_root: Path) -> bool:
    """判断目录项是否需要跳过。

    作用：
        统一封装目录树遍历过滤策略：隐藏项、软链接、越界路径。

    参数：
        path: 目录项路径。
        skill_root: 技能根目录。

    返回：
        bool: 需要跳过返回 `True`，否则返回 `False`。
    """

    if path.name.startswith("."):
        return True

    try:
        if path.is_symlink():
            return True
    except OSError:
        return True

    return not _is_path_within_skill_root(path, skill_root)


def _relative_path(path: Path, skill_root: Path) -> str:
    """将路径转换为相对技能根目录的 POSIX 路径字符串。

    作用：
        确保工具返回中不包含绝对路径，避免泄露宿主文件系统信息。

    参数：
        path: 目标路径。
        skill_root: 技能根目录。

    返回：
        str: 相对路径字符串。
    """

    return path.resolve().relative_to(skill_root.resolve()).as_posix()


def _build_tree(
        *,
        skill_root: Path,
        current_dir: Path,
        current_depth: int,
        max_depth: int,
) -> list[SkillTreeNode]:
    """构建技能目录树。

    作用：
        以深度优先方式遍历目录，返回稳定排序的结构化节点列表。

    参数：
        skill_root: 技能根目录。
        current_dir: 当前遍历目录。
        current_depth: 当前目录深度（技能根目录为 0）。
        max_depth: 最大展开深度（包含该深度节点）。

    返回：
        list[SkillTreeNode]: 当前目录下的节点列表。
    """

    try:
        entries = list(current_dir.iterdir())
    except OSError:
        return []

    dir_nodes: list[SkillTreeNode] = []
    file_nodes: list[SkillTreeNode] = []

    for entry in entries:
        if _should_skip_entry(entry, skill_root):
            continue

        try:
            relative = _relative_path(entry, skill_root)
        except (OSError, ValueError):
            continue

        try:
            is_dir = entry.is_dir()
            is_file = entry.is_file()
        except OSError:
            continue

        node_depth = current_depth + 1
        if is_dir:
            node: SkillTreeNode = {
                "name": entry.name,
                "type": "dir",
                "path": relative,
                "children": [],
            }
            if node_depth < max_depth:
                node["children"] = _build_tree(
                    skill_root=skill_root,
                    current_dir=entry,
                    current_depth=node_depth,
                    max_depth=max_depth,
                )
            dir_nodes.append(node)
        elif is_file:
            node = {
                "name": entry.name,
                "type": "file",
                "path": relative,
            }
            file_nodes.append(node)

    dir_nodes.sort(key=lambda node: node["name"])
    file_nodes.sort(key=lambda node: node["name"])
    return [*dir_nodes, *file_nodes]


def _build_not_found_response(
        *,
        skill_name: str,
        available_skills: list[str],
        scope_label: str,
) -> SkillTreeResponse:
    """构建技能未命中时的标准响应。

    作用：
        提供一致的错误结构，便于模型按可用技能名重试。

    参数：
        skill_name: 请求的技能名称。
        available_skills: 当前可用技能名称列表。
        scope_label: 作用域文本，用于提示信息。

    返回：
        SkillTreeResponse: 未命中响应结构。
    """

    available_text = ", ".join(available_skills) if available_skills else "(none)"
    return {
        "skill_name": skill_name,
        "max_depth": _MAX_TREE_DEPTH,
        "tree": [],
        "error": (
            f"Skill '{skill_name}' not found under scope '{scope_label}'. "
            f"Available skills: {available_text}"
        ),
        "available_skills": available_skills,
    }


def create_list_skill_resources_tool(
        scope: str | None,
        get_skill_file_index: Callable[[], SkillFileIndex],
):
    """创建 `list_skill_resources` 工具函数。

    作用：
        基于技能名称返回该技能目录的树形结构，帮助模型在路径不确定时定位资源文件。

    参数：
        scope: 技能作用域，用于校验与错误提示。为空时默认技能根目录。
        get_skill_file_index: 获取最新技能索引（`name -> SKILL.md 路径`）的回调。

    返回：
        Callable: 可供模型调用的 `list_skill_resources(skill_name)` 工具。
    """

    normalized_scope, _ = normalize_scope(scope)
    scope_label = normalized_scope or "resources/skills"

    @tool(
        description=(
                "按技能名称列出该技能目录的资源树（最多 3 层）。"
                "当 resource_path 不确定或读取失败时，先调用此工具定位正确路径。"
        )
    )
    @tool_thinking_redaction(display_name="查看技能资源目录")
    def list_skill_resources(skill_name: str) -> dict[str, Any]:
        """按技能名称返回目录树。

        作用：
            返回技能目录下的目录/文件名称结构，不返回文件内容，不返回绝对路径。

        参数：
            skill_name: 技能名称，对应 frontmatter 中的 `name` 字段。

        返回：
            dict[str, Any]:
                - 命中时：返回 `skill_name/max_depth/tree`；
                - 未命中时：返回 `error` 与 `available_skills`。
        """

        normalized_skill_name = str(skill_name or "").strip()
        skill_file_index = get_skill_file_index()
        available_skills = sorted(skill_file_index.keys())

        if not normalized_skill_name:
            return {
                "skill_name": normalized_skill_name,
                "max_depth": _MAX_TREE_DEPTH,
                "tree": [],
                "error": "Skill name cannot be empty.",
                "available_skills": available_skills,
            }

        skill_file = skill_file_index.get(normalized_skill_name)
        if skill_file is None:
            return dict(
                _build_not_found_response(
                    skill_name=normalized_skill_name,
                    available_skills=available_skills,
                    scope_label=scope_label,
                )
            )

        try:
            skill_root = skill_file.parent.resolve()
        except OSError:
            return {
                "skill_name": normalized_skill_name,
                "max_depth": _MAX_TREE_DEPTH,
                "tree": [],
                "error": f"Failed to access skill root for '{normalized_skill_name}'.",
            }

        tree = _build_tree(
            skill_root=skill_root,
            current_dir=skill_root,
            current_depth=0,
            max_depth=_MAX_TREE_DEPTH,
        )
        return {
            "skill_name": normalized_skill_name,
            "max_depth": _MAX_TREE_DEPTH,
            "tree": tree,
        }

    return list_skill_resources
