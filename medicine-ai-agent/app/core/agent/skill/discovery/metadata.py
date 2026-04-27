from __future__ import annotations

from pathlib import Path
from typing import Any

import yaml
from loguru import logger

from app.core.agent.skill.discovery.scope import _is_path_within_root, normalize_scope
from app.core.agent.skill.types.models import SkillExtraMetadata, SkillFileIndex, SkillMetadata


def _parse_frontmatter(raw_content: str) -> dict[str, Any]:
    """解析 frontmatter 文本块。

    作用：
        使用 YAML 解析 `--- ... ---` 区块，提取技能元数据。

    参数：
        raw_content: frontmatter 原始文本（通常只包含头部块）。

    返回：
        dict[str, Any]: 解析得到的键值对；若格式不合法则返回空字典。
    """

    lines = raw_content.splitlines()
    if not lines or lines[0].strip() != "---":
        return {}

    yaml_lines: list[str] = []
    for line in lines[1:]:
        if line.strip() == "---":
            break
        yaml_lines.append(line)

    yaml_text = "\n".join(yaml_lines).strip()
    if not yaml_text:
        return {}

    try:
        parsed = yaml.safe_load(yaml_text)
    except yaml.YAMLError:
        return {}

    if not isinstance(parsed, dict):
        return {}
    return parsed


def _parse_skill_extra_metadata(raw_metadata: Any, skill_file: Path) -> SkillExtraMetadata | None:
    """解析技能扩展元数据。

    作用：
        将 frontmatter 中的 `metadata` 转为强类型结构，仅保留 `author/version`。

    参数：
        raw_metadata: frontmatter 中的 `metadata` 原始值。
        skill_file: 当前技能文件路径，用于日志输出。

    返回：
        SkillExtraMetadata | None:
            - 合法且至少包含一个字段时返回对象；
            - 不存在、类型错误或无有效字段时返回 `None`。
    """

    if raw_metadata is None:
        return None
    if not isinstance(raw_metadata, dict):
        logger.warning(
            "技能 metadata 字段类型无效（应为对象），已忽略: {}",
            skill_file,
        )
        return None

    parsed: SkillExtraMetadata = {}
    author = str(raw_metadata.get("author") or "").strip()
    if author:
        parsed["author"] = author

    version = str(raw_metadata.get("version") or "").strip()
    if version:
        parsed["version"] = version

    if not parsed:
        logger.warning(
            "技能 metadata 中未包含有效字段（author/version），已忽略: {}",
            skill_file,
        )
        return None
    return parsed


def _read_frontmatter_block(skill_file: Path) -> str:
    """仅读取 `SKILL.md` 的 frontmatter 区块。

    作用：
        只读取文件头部 `--- ... ---` 内容，避免在预加载阶段读取全文。

    参数：
        skill_file: 技能文件路径。

    返回：
        str: frontmatter 原文；无 frontmatter 或读取失败时返回空字符串。
    """

    try:
        with skill_file.open("r", encoding="utf-8") as file_obj:
            first_line = file_obj.readline()
            if first_line.strip() != "---":
                return ""

            chunks = [first_line]
            for line in file_obj:
                chunks.append(line)
                if line.strip() == "---":
                    break
            return "".join(chunks)
    except OSError:
        return ""


def _resolve_discovery_scope(
        scope: str | None,
        skill_scope: str | None,
) -> tuple[str, Path, bool]:
    """解析技能发现目标并执行参数互斥校验。

    功能描述：
        根据 `scope/skill_scope` 计算实际扫描目录，并严格约束两者不能同时生效。
        当启用 `skill_scope` 时，后续逻辑应读取该目录下的 `SKILL.md` 单文件；
        当仅传 `scope` 时，保持历史行为：扫描 `<scope>/*/SKILL.md`。

    参数说明：
        scope (str | None): 目录级作用域，历史模式参数，支持空值。
        skill_scope (str | None): 单技能目录作用域，直读模式参数，支持空值。

    返回值：
        tuple[str, Path, bool]:
            - 规范化后的作用域字符串；
            - 对应的绝对目录路径；
            - 是否启用“单技能直读模式”。

    异常说明：
        ValueError: 当 `scope` 与 `skill_scope` 同时非空时抛出。
    """

    normalized_scope_input = str(scope or "").strip()
    normalized_skill_scope_input = str(skill_scope or "").strip()
    if normalized_scope_input and normalized_skill_scope_input:
        raise ValueError("Skill discovery arguments are mutually exclusive: scope and skill_scope.")

    if normalized_skill_scope_input:
        normalized_skill_scope, skill_dir = normalize_scope(skill_scope)
        return normalized_skill_scope, skill_dir, True

    normalized_scope, scope_dir = normalize_scope(scope)
    return normalized_scope, scope_dir, False


def _parse_skill_metadata_from_file(skill_file: Path) -> tuple[str, SkillMetadata] | None:
    """从单个技能文件解析并构建元数据对象。

    功能描述：
        仅读取目标 `SKILL.md` 的 frontmatter，执行必填字段校验后构建
        `SkillMetadata`，供不同发现模式复用。

    参数说明：
        skill_file (Path): 技能文件绝对路径。

    返回值：
        tuple[str, SkillMetadata] | None:
            - 成功时返回 `(skill_name, skill_metadata)`；
            - 解析失败或缺少必填字段时返回 `None`。

    异常说明：
        无；文件读取错误、字段不合法等场景均以日志+`None` 形式处理。
    """

    frontmatter_text = _read_frontmatter_block(skill_file)
    parsed = _parse_frontmatter(frontmatter_text)
    skill_name = str(parsed.get("name") or "").strip()
    if not skill_name:
        logger.error("跳过技能，frontmatter 缺少必填字段 name: {}", skill_file)
        return None

    description = str(parsed.get("description") or "").strip()
    if not description:
        logger.error("跳过技能，frontmatter 缺少必填字段 description: {}", skill_file)
        return None

    skill_metadata: SkillMetadata = {
        "name": skill_name,
        "description": description,
    }
    license_name = str(parsed.get("license") or "").strip()
    if license_name:
        skill_metadata["license"] = license_name

    extra_metadata = _parse_skill_extra_metadata(parsed.get("metadata"), skill_file)
    if extra_metadata is not None:
        skill_metadata["metadata"] = extra_metadata

    return skill_name, skill_metadata


def _build_discovery_result(
        *,
        skill_files: list[Path],
        scope_label: str,
) -> tuple[list[SkillMetadata], SkillFileIndex]:
    """聚合技能文件为稳定有序的发现结果。

    功能描述：
        将技能文件集合转换为 `skills_metadata` 与 `skill_file_index`，并处理
        同名技能覆盖日志与名称排序，保证注入顺序可预期。

    参数说明：
        skill_files (list[Path]): 待解析的技能文件列表。
        scope_label (str): 当前作用域标签，用于重复名称日志提示。

    返回值：
        tuple[list[SkillMetadata], SkillFileIndex]:
            - 对模型可见的技能元数据列表（按名称排序）；
            - `frontmatter.name -> SKILL.md` 文件路径索引。

    异常说明：
        无；单个文件异常会被跳过，不影响其余文件聚合。
    """

    metadata_by_name: dict[str, SkillMetadata] = {}
    skill_file_index: SkillFileIndex = {}
    normalized_scope_label = scope_label or "resources/skills"

    for skill_file in skill_files:
        parsed_result = _parse_skill_metadata_from_file(skill_file)
        if parsed_result is None:
            continue

        skill_name, skill_metadata = parsed_result
        existing = skill_file_index.get(skill_name)
        if existing is not None:
            logger.warning(
                "Duplicate skill name '{}' in scope '{}', override {} -> {}",
                skill_name,
                normalized_scope_label,
                existing,
                skill_file,
            )

        skill_file_index[skill_name] = skill_file
        metadata_by_name[skill_name] = skill_metadata

    deduped_metadata = [metadata_by_name[name] for name in sorted(skill_file_index.keys())]
    return deduped_metadata, skill_file_index


def _collect_child_skill_files(scope_dir: Path) -> list[Path]:
    """收集目录模式下的技能文件列表。

    功能描述：
        保持历史扫描策略，仅收集 `<scope>/*/SKILL.md`，不递归深入。

    参数说明：
        scope_dir (Path): 当前扫描目录。

    返回值：
        list[Path]:
            满足安全边界与存在性校验的技能文件路径列表，按目录名稳定排序。

    异常说明：
        无；不合法目录项会被静默跳过。
    """

    skill_files: list[Path] = []
    for child_dir in sorted(scope_dir.iterdir(), key=lambda path: path.name):
        if not child_dir.is_dir():
            continue
        if not _is_path_within_root(child_dir):
            continue

        skill_file = child_dir / "SKILL.md"
        if not skill_file.is_file():
            continue
        if not _is_path_within_root(skill_file):
            continue
        skill_files.append(skill_file)
    return skill_files


def discover_skills_metadata(
        scope: str | None = None,
        *,
        skill_scope: str | None = None,
) -> tuple[list[SkillMetadata], SkillFileIndex]:
    """发现指定作用域下的技能元数据，并建立内部索引。

    功能描述：
        根据入参选择两种发现模式：
        1. `scope` 模式（历史行为）：扫描 `<scope>/*/SKILL.md`（非递归）；
        2. `skill_scope` 模式（新增行为）：仅读取 `<skill_scope>/SKILL.md` 单文件。
        解析 frontmatter 后，返回对模型可见的元数据列表与内部路径索引。

    参数说明：
        scope (str | None): 目录级作用域；为空时扫描技能根目录，默认 `None`。
        skill_scope (str | None): 单技能目录作用域；启用后仅直读目录自身 `SKILL.md`，
            默认 `None`。

    返回值：
        tuple[list[SkillMetadata], SkillFileIndex]:
            - skills_metadata: 对模型暴露的技能元数据（必填字段 + 可选字段）
            - skill_file_index: `frontmatter.name -> SKILL.md` 路径映射

    异常说明：
        ValueError: 当 `scope` 与 `skill_scope` 同时非空时抛出。
    """

    normalized_scope, scope_dir, direct_skill_mode = _resolve_discovery_scope(scope, skill_scope)
    if not scope_dir.exists() or not scope_dir.is_dir():
        return [], {}
    if not _is_path_within_root(scope_dir):
        return [], {}

    if direct_skill_mode:
        skill_file = scope_dir / "SKILL.md"
        if not skill_file.is_file():
            return [], {}
        if not _is_path_within_root(skill_file):
            return [], {}
        return _build_discovery_result(
            skill_files=[skill_file],
            scope_label=normalized_scope,
        )

    return _build_discovery_result(
        skill_files=_collect_child_skill_files(scope_dir),
        scope_label=normalized_scope,
    )


def discover_skills(
        scope: str | None = None,
        *,
        skill_scope: str | None = None,
) -> tuple[list[SkillMetadata], SkillFileIndex]:
    """对外暴露的技能发现入口。

    功能描述：
        统一封装技能元数据发现逻辑，供中间件或测试调用。

    参数说明：
        scope (str | None): 目录级作用域。为空时表示扫描技能根目录，默认 `None`。
        skill_scope (str | None): 单技能目录作用域。启用后仅读取目录自身 `SKILL.md`，
            默认 `None`。

    返回值：
        tuple[list[SkillMetadata], SkillFileIndex]:
            - 技能元数据列表
            - 技能文件索引映射

    异常说明：
        ValueError: 当 `scope` 与 `skill_scope` 同时非空时抛出。
    """

    return discover_skills_metadata(scope, skill_scope=skill_scope)
