from __future__ import annotations

from collections.abc import Callable
from pathlib import Path, PurePosixPath

from langchain_core.tools import tool

from app.core.agent.skill.discovery.scope import normalize_scope
from app.core.agent.middleware.tool_thinking_redaction import (
    tool_thinking_redaction,
)
from app.core.agent.skill.types.models import SkillFileIndex

MAX_RESOURCE_FILE_SIZE = 128 * 1024


def _normalize_resource_path(resource_path: str) -> tuple[Path | None, str | None]:
    """规范化资源相对路径。

    作用：
        对模型传入的 `resource_path` 做安全清洗，只允许相对路径并移除 `./`。
        若路径包含越界片段（如 `..`）则直接拒绝。

    参数：
        resource_path: 原始资源路径字符串。

    返回：
        tuple[Path | None, str | None]:
            - 第一个元素为规范化后的相对路径；
            - 第二个元素为错误提示（成功时为 `None`）。
    """

    raw_path = str(resource_path or "").strip()
    if not raw_path:
        return None, "Resource path cannot be empty."

    posix_path = PurePosixPath(raw_path.replace("\\", "/"))
    if posix_path.is_absolute():
        return None, "Resource path must be a relative path."

    normalized_parts: list[str] = []
    for part in posix_path.parts:
        if part in {"", "."}:
            continue
        if part == "..":
            return None, "Resource path cannot contain parent directory traversal."
        normalized_parts.append(part)

    if not normalized_parts:
        return None, "Resource path cannot be empty."

    return Path(*normalized_parts), None


def _resolve_resource_file(
        *,
        skill_dir: Path,
        normalized_resource_path: Path,
) -> tuple[Path | None, str | None]:
    """解析并校验技能资源文件的绝对路径。

    作用：
        将相对路径解析到技能目录内，并阻断越界访问（含软链逃逸）。

    参数：
        skill_dir: 技能目录（`SKILL.md` 所在目录）。
        normalized_resource_path: 规范化后的相对路径。

    返回：
        tuple[Path | None, str | None]:
            - 第一个元素为安全可读的目标文件路径；
            - 第二个元素为错误提示（成功时为 `None`）。
    """

    try:
        skill_root = skill_dir.resolve()
        candidate = (skill_root / normalized_resource_path).resolve()
    except OSError:
        return None, "Failed to resolve resource path."

    if not candidate.is_relative_to(skill_root):
        return None, "Resource path escapes skill directory."
    if not candidate.exists() or not candidate.is_file():
        return None, "Resource file not found."
    return candidate, None


def _read_utf8_text_file(resource_file: Path) -> tuple[str | None, str | None]:
    """按安全约束读取资源文本文件。

    作用：
        限制读取文件大小并强制 UTF-8 解码，避免大文件与二进制污染上下文。

    参数：
        resource_file: 目标资源文件路径。

    返回：
        tuple[str | None, str | None]:
            - 第一个元素为文件文本内容；
            - 第二个元素为错误提示（成功时为 `None`）。
    """

    try:
        file_size = resource_file.stat().st_size
    except OSError:
        return None, "Failed to read resource file metadata."

    if file_size > MAX_RESOURCE_FILE_SIZE:
        return None, f"Resource file too large (>{MAX_RESOURCE_FILE_SIZE} bytes)."

    try:
        raw_bytes = resource_file.read_bytes()
    except OSError:
        return None, "Failed to read resource file."

    try:
        content = raw_bytes.decode("utf-8")
    except UnicodeDecodeError:
        return None, "Only UTF-8 text files are supported."
    return content, None


def create_load_skill_tool(
        scope: str | None,
        get_skill_file_index: Callable[[], SkillFileIndex],
):
    """创建 `load_skill` 工具函数。

    作用：
        在固定 `scope` 下基于预构建索引提供技能全文懒加载能力。
        该函数返回一个可注册到 LangChain 的工具对象。

    参数：
        scope: 技能作用域，用于错误提示与作用域校验。为空时默认技能根目录。
        get_skill_file_index: 获取最新技能索引的回调函数，返回 `name -> 文件路径`。

    返回：
        Callable: 可供模型调用的 `load_skill(skill_name)` 工具。
    """

    normalized_scope, _ = normalize_scope(scope)
    scope_label = normalized_scope or "resources/skills"

    @tool(
        description=(
                "按技能名称加载完整 SKILL.md 内容。"
                "当任务命中某个技能并需要详细说明时调用。"
        )
    )
    @tool_thinking_redaction(display_name="加载技能")
    def load_skill(skill_name: str) -> str:
        """按技能名称读取并返回完整技能文件内容。

        作用：
            仅在模型显式调用时读取对应 `SKILL.md` 全文，实现渐进式加载。

        参数：
            skill_name: 技能名称，对应 frontmatter 中的 `name` 字段。

        返回：
            str: 命中时返回 `Loaded skill: ...` 与全文；未命中返回可用技能列表提示。
        """

        normalized_skill_name = str(skill_name or "").strip()
        if not normalized_skill_name:
            return "Skill name cannot be empty."

        skill_file_index = get_skill_file_index()
        selected_file = skill_file_index.get(normalized_skill_name)
        if selected_file is None:
            available_names = sorted(skill_file_index.keys())
            available_text = ", ".join(available_names) if available_names else "(none)"
            return (
                f"Skill '{normalized_skill_name}' not found under scope '{scope_label}'. "
                f"Available skills: {available_text}"
            )

        try:
            full_content = selected_file.read_text(encoding="utf-8")
        except OSError:
            return f"Failed to read skill file for '{normalized_skill_name}'."
        return f"Loaded skill: {normalized_skill_name}\n\n{full_content}"

    return load_skill


def create_load_skill_resource_tool(
        scope: str | None,
        get_skill_file_index: Callable[[], SkillFileIndex],
):
    """创建 `load_skill_resource` 工具函数。

    作用：
        在技能目录内按相对路径按需读取资源文件内容。
        该工具支持读取 `SKILL.md` 同级文件与任意子目录文件，但禁止越界访问。

    参数：
        scope: 技能作用域，用于错误提示与作用域校验。为空时默认技能根目录。
        get_skill_file_index: 获取最新技能索引的回调函数，返回 `name -> 文件路径`。

    返回：
        Callable: 可供模型调用的 `load_skill_resource(skill_name, resource_path)` 工具。
    """

    normalized_scope, _ = normalize_scope(scope)
    scope_label = normalized_scope or "resources/skills"

    @tool(
        description=(
                "读取技能目录内的资源文件（仅相对路径、最大 128KB、仅 UTF-8 文本）。"
                "当技能说明提示去读取 references/scripts 等文件时调用。"
        )
    )
    @tool_thinking_redaction(display_name="加载技能资源")
    def load_skill_resource(skill_name: str, resource_path: str) -> str:
        """按技能名称与相对路径读取资源全文。

        作用：
            基于技能索引定位技能目录后，安全读取资源文件内容并返回。

        参数：
            skill_name: 技能名称，对应 frontmatter 中的 `name` 字段。
            resource_path: 相对技能目录的路径，例如 `evaluation.md` 或 `./references/evaluation.md`。

        返回：
            str: 成功返回资源全文；失败返回明确错误信息。
        """

        normalized_skill_name = str(skill_name or "").strip()
        if not normalized_skill_name:
            return "Skill name cannot be empty."

        normalized_resource_path, path_error = _normalize_resource_path(resource_path)
        if path_error is not None:
            return path_error

        skill_file_index = get_skill_file_index()
        selected_skill_file = skill_file_index.get(normalized_skill_name)
        if selected_skill_file is None:
            available_names = sorted(skill_file_index.keys())
            available_text = ", ".join(available_names) if available_names else "(none)"
            return (
                f"Skill '{normalized_skill_name}' not found under scope '{scope_label}'. "
                f"Available skills: {available_text}"
            )

        resource_file, resolve_error = _resolve_resource_file(
            skill_dir=selected_skill_file.parent,
            normalized_resource_path=normalized_resource_path,
        )
        if resolve_error is not None:
            return resolve_error

        assert resource_file is not None
        content, read_error = _read_utf8_text_file(resource_file)
        if read_error is not None:
            return read_error

        assert content is not None
        normalized_path_str = normalized_resource_path.as_posix()
        return (
            f"Loaded skill resource: {normalized_skill_name} ({normalized_path_str})\n\n"
            f"{content}"
        )

    return load_skill_resource
