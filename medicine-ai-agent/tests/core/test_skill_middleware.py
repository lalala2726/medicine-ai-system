from __future__ import annotations

import importlib
from pathlib import Path
from typing import Any

import pytest
from langchain.agents import create_agent as lc_create_agent
from langchain.agents.middleware import ModelRequest, ModelResponse
from langchain_core.language_models import FakeListChatModel
from langchain_core.messages import AIMessage, SystemMessage
from loguru import logger

import app.core.agent.base_prompt_middleware as base_prompt_module
import app.core.agent.skill.discovery.scope as scope_module
from app.core.agent.base_prompt_middleware import BasePromptMiddleware
from app.core.agent.skill import (
    SkillMiddleware,
    create_list_skill_resources_tool,
    create_load_skill_resource_tool,
    create_load_skill_tool,
    discover_skills,
)


class _ToolFriendlyFakeListChatModel(FakeListChatModel):
    """支持工具绑定的测试模型。"""

    def bind_tools(self, tools, *, tool_choice=None, **kwargs):  # type: ignore[no-untyped-def]
        _ = (tools, tool_choice, kwargs)
        return self


def _write_skill(
        root: Path,
        relative_dir: str,
        description: str = "demo desc",
        *,
        skill_name: str = "Demo Skill",
        license_name: str | None = None,
        metadata: dict[str, Any] | None = None,
        include_name: bool = True,
        include_description: bool = True,
) -> None:
    """构造测试用 `SKILL.md` 文件。

    测试目的：
        统一生成不同 frontmatter 组合，降低各用例样板代码。

    预期结果：
        在指定目录下写入可被 discovery/load_skill 识别的技能文件。
    """

    skill_file = root / relative_dir / "SKILL.md"
    skill_file.parent.mkdir(parents=True, exist_ok=True)
    frontmatter_lines = ["---"]
    if include_name:
        frontmatter_lines.append(f"name: {skill_name}")
    if include_description:
        frontmatter_lines.append(f"description: {description}")
    if license_name is not None:
        frontmatter_lines.append(f"license: {license_name}")
    if metadata is not None:
        frontmatter_lines.append("metadata:")
        for key, value in metadata.items():
            frontmatter_lines.append(f'  {key}: "{value}"')
    frontmatter_lines.append("---")

    content = "\n".join(frontmatter_lines) + "\n\n# Steps\n- step 1\n"
    skill_file.write_text(content, encoding="utf-8")


def _flatten_tree_paths(tree: list[dict[str, Any]]) -> list[str]:
    """拍平目录树路径，便于断言。

    测试目的：
        将工具返回的层级树结构转为扁平路径列表，降低断言复杂度。

    预期结果：
        返回包含当前节点与所有子节点 `path` 的稳定列表。
    """

    paths: list[str] = []
    for node in tree:
        paths.append(str(node["path"]))
        children = node.get("children")
        if isinstance(children, list):
            paths.extend(_flatten_tree_paths(children))
    return paths


def test_discover_skills_only_reads_immediate_children_for_scope_supervisor(
        monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    """验证 scope=supervisor 只扫描直系子目录。

    测试目的：
        防止递归扫描把更深层级技能误加载。

    预期结果：
        仅返回 `supervisor/*/SKILL.md` 中的 a/b/c。
    """

    skills_root = tmp_path / "skills"
    _write_skill(skills_root, "supervisor/a", "desc a", skill_name="a")
    _write_skill(skills_root, "supervisor/b", "desc b", skill_name="b")
    _write_skill(skills_root, "supervisor/c", "desc c", skill_name="c")
    _write_skill(skills_root, "supervisor/c/a", "nested should be ignored", skill_name="c-a")
    _write_skill(skills_root, "supervisor/d/w", "deep should be ignored", skill_name="d-w")
    monkeypatch.setattr(scope_module, "SKILLS_ROOT", skills_root)

    metadata, _ = discover_skills("supervisor")

    assert [item["name"] for item in metadata] == ["a", "b", "c"]


def test_discover_skills_only_reads_immediate_children_for_nested_scope(
        monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    """验证嵌套 scope 仍只扫描当前层级的直系子目录。

    测试目的：
        保证 `scope=supervisor/a` 时只读取 `supervisor/a/*/SKILL.md`。

    预期结果：
        返回 b/c，忽略 `supervisor/a/c/s` 等更深目录。
    """

    skills_root = tmp_path / "skills"
    _write_skill(skills_root, "supervisor/a/b", "desc b", skill_name="b")
    _write_skill(skills_root, "supervisor/a/c", "desc c", skill_name="c")
    _write_skill(skills_root, "supervisor/a/c/s", "nested should be ignored", skill_name="c-s")
    monkeypatch.setattr(scope_module, "SKILLS_ROOT", skills_root)

    metadata, _ = discover_skills("supervisor/a")

    assert [item["name"] for item in metadata] == ["b", "c"]


def test_discover_skills_raises_when_scope_depth_exceeds_three_levels() -> None:
    """验证 scope 层级超限时会被拒绝。

    测试目的：
        确认最大 3 级作用域限制生效。

    预期结果：
        传入四级 scope 时抛出 `ValueError`。
    """

    with pytest.raises(ValueError):
        discover_skills("supervisor/a/c/d")


def test_discover_skills_uses_root_directory_when_scope_is_empty(
        monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    """验证空 scope 默认扫描技能根目录。

    测试目的：
        确认 `scope` 为空时不报错，并按根目录规则扫描 `resources/skills/*/SKILL.md`。

    预期结果：
        返回根目录直系技能，忽略更深层级的技能文件。
    """

    skills_root = tmp_path / "skills"
    _write_skill(skills_root, "a", "desc a", skill_name="a")
    _write_skill(skills_root, "b", "desc b", skill_name="b")
    _write_skill(skills_root, "supervisor/analysis", "nested ignored", skill_name="analysis")
    monkeypatch.setattr(scope_module, "SKILLS_ROOT", skills_root)

    metadata, _ = discover_skills("")

    assert [item["name"] for item in metadata] == ["a", "b"]


def test_before_agent_is_idempotent_when_skills_metadata_already_exists() -> None:
    """验证 `before_agent` 的幂等行为。

    测试目的：
        避免已存在 `skills_metadata` 时重复覆盖状态。

    预期结果：
        `before_agent` 返回 `None`。
    """

    middleware = SkillMiddleware(scope="supervisor")
    state = {"skills_metadata": [{"name": "existing", "description": "cached"}]}

    result = middleware.before_agent(state, runtime=None)

    assert result is None


def test_skill_middleware_defaults_to_root_scope_when_scope_missing(
        monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    """验证 `SkillMiddleware()` 默认使用根目录 scope。

    测试目的：
        确认中间件在未显式传 scope 时也能发现技能并写入元数据。

    预期结果：
        `before_agent` 返回根目录直系技能元数据。
    """

    skills_root = tmp_path / "skills"
    _write_skill(skills_root, "root-skill", "root desc", skill_name="root-skill")
    monkeypatch.setattr(scope_module, "SKILLS_ROOT", skills_root)

    middleware = SkillMiddleware()
    result = middleware.before_agent({}, runtime=None)

    assert result is not None
    assert result["skills_metadata"] == [
        {"name": "root-skill", "description": "root desc"},
    ]


def test_discover_skills_finds_repo_chart_skill_in_direct_skill_mode() -> None:
    """验证真实仓库中的 chart skill 能被 direct-skill 模式发现。

    测试目的：
        覆盖默认 `SKILLS_ROOT` 的真实路径解析，防止根目录偏移后导致
        `skill_scope=\"chart\"` 返回空列表。

    预期结果：
        `discover_skills(skill_scope=\"chart\")` 返回名为 `chart` 的技能元数据。
    """

    metadata, _ = discover_skills(skill_scope="chart")

    assert metadata == [
        {
            "name": "chart",
            "description": "图表模板技能，提供 18 种图表类型的模板规范、字段说明与标准输出格式，基于 GPT-Vis (AntV) 规范。",
            "license": "Apache-2.0",
            "metadata": {"author": "Chuang", "version": "1.0"},
        }
    ]


def test_before_agent_returns_metadata_without_path(
        monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    """验证预加载只返回元数据，不暴露路径/正文。

    测试目的：
        确认 `before_agent` 仅注入前置元数据，并携带可选字段。

    预期结果：
        返回 `name/description/license/metadata`，且不含 `path/content`。
    """

    skills_root = tmp_path / "skills"
    _write_skill(
        skills_root,
        "supervisor/analysis",
        "analysis desc",
        skill_name="analysis",
        license_name="Apache-2.0",
        metadata={"author": "example-org", "version": "1.0"},
    )
    monkeypatch.setattr(scope_module, "SKILLS_ROOT", skills_root)
    middleware = SkillMiddleware(scope="supervisor")

    result = middleware.before_agent({}, runtime=None)

    assert result is not None
    metadata = result["skills_metadata"]
    assert metadata == [
        {
            "name": "analysis",
            "description": "analysis desc",
            "license": "Apache-2.0",
            "metadata": {"author": "example-org", "version": "1.0"},
        }
    ]
    assert "path" not in metadata[0]
    assert "content" not in metadata[0]


def test_discover_skills_only_keeps_author_and_version_in_metadata(
        monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    """验证 metadata 仅保留强约束字段。

    测试目的：
        确保 `metadata` 只透传 `author/version`，忽略其他键。

    预期结果：
        `channel` 被过滤，仅剩 `author/version`。
    """

    skills_root = tmp_path / "skills"
    _write_skill(
        skills_root,
        "supervisor/analysis",
        "analysis desc",
        skill_name="analysis",
        metadata={"author": "example-org", "version": "1.0", "channel": "dev"},
    )
    monkeypatch.setattr(scope_module, "SKILLS_ROOT", skills_root)

    metadata, _ = discover_skills("supervisor")

    assert metadata == [
        {
            "name": "analysis",
            "description": "analysis desc",
            "metadata": {"author": "example-org", "version": "1.0"},
        }
    ]


def test_discover_skills_skips_missing_required_fields_and_logs_error(
        monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    """验证缺少必填字段时跳过并记录错误日志。

    测试目的：
        确认 `name/description` 必填校验与错误可观测性。

    预期结果：
        相关技能不被加载，日志中包含错误级别与具体文件路径。
    """

    skills_root = tmp_path / "skills"
    _write_skill(
        skills_root,
        "supervisor/missing_name",
        "desc",
        include_name=False,
    )
    _write_skill(
        skills_root,
        "supervisor/missing_description",
        include_description=False,
    )
    monkeypatch.setattr(scope_module, "SKILLS_ROOT", skills_root)

    logs: list[str] = []
    log_id = logger.add(logs.append, format="{level}|{message}")
    try:
        metadata, _ = discover_skills("supervisor")
    finally:
        logger.remove(log_id)

    assert metadata == []
    all_logs = "\n".join(logs)
    assert "ERROR|跳过技能，frontmatter 缺少必填字段 name" in all_logs
    assert "ERROR|跳过技能，frontmatter 缺少必填字段 description" in all_logs
    assert "missing_name/SKILL.md" in all_logs
    assert "missing_description/SKILL.md" in all_logs


def test_load_skill_returns_full_skill_content(
        monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    """验证 `load_skill` 可按名称返回全文。

    测试目的：
        确认技能正文采取按需懒加载策略且内容完整。

    预期结果：
        返回结果包含 `Loaded skill` 标记、frontmatter 与正文片段。
    """

    skills_root = tmp_path / "skills"
    _write_skill(skills_root, "supervisor/analysis", "analysis desc", skill_name="analysis")
    monkeypatch.setattr(scope_module, "SKILLS_ROOT", skills_root)

    _, skill_file_index = discover_skills("supervisor")
    load_skill = create_load_skill_tool("supervisor", lambda: skill_file_index)
    content = load_skill.invoke({"skill_name": "analysis"})

    assert "Loaded skill: analysis" in content
    assert "description: analysis desc" in content
    assert "# Steps" in content


def test_load_skill_returns_available_names_when_target_not_found(
        monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    """验证 `load_skill` 未命中时的提示信息。

    测试目的：
        确保调用错误时能给出可用技能名，便于模型重试。

    预期结果：
        返回 `not found`，并列出可用技能 `analysis`。
    """

    skills_root = tmp_path / "skills"
    _write_skill(skills_root, "supervisor/analysis", "analysis desc", skill_name="analysis")
    monkeypatch.setattr(scope_module, "SKILLS_ROOT", skills_root)

    _, skill_file_index = discover_skills("supervisor")
    load_skill = create_load_skill_tool("supervisor", lambda: skill_file_index)
    content = load_skill.invoke({"skill_name": "missing"})

    assert "not found" in content.lower()
    assert "analysis" in content


def test_load_skill_uses_frontmatter_name_not_directory_name(
        monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    """验证 `load_skill` 以 frontmatter.name 作为唯一键。

    测试目的：
        防止目录名与技能名不一致时发生错误映射。

    预期结果：
        `analysis` 可加载，`analysis_dir` 未命中。
    """

    skills_root = tmp_path / "skills"
    _write_skill(
        skills_root,
        "supervisor/analysis_dir",
        "analysis desc",
        skill_name="analysis",
    )
    monkeypatch.setattr(scope_module, "SKILLS_ROOT", skills_root)

    _, skill_file_index = discover_skills("supervisor")
    load_skill = create_load_skill_tool("supervisor", lambda: skill_file_index)

    ok_content = load_skill.invoke({"skill_name": "analysis"})
    missing_content = load_skill.invoke({"skill_name": "analysis_dir"})

    assert "Loaded skill: analysis" in ok_content
    assert "not found" in missing_content.lower()


def test_load_skill_resource_reads_file_in_same_directory_when_only_filename_given(
        monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    """验证仅传文件名时读取 `SKILL.md` 同级文件。

    测试目的：
        覆盖你要求的行为：`evaluation.md` 表示技能目录（`SKILL.md` 同级）下的文件。

    预期结果：
        `load_skill_resource("analysis", "evaluation.md")` 成功返回该文件全文。
    """

    skills_root = tmp_path / "skills"
    _write_skill(skills_root, "supervisor/analysis", "analysis desc", skill_name="analysis")
    evaluation_file = skills_root / "supervisor/analysis/evaluation.md"
    evaluation_file.write_text("# 同级评估\n- item", encoding="utf-8")
    monkeypatch.setattr(scope_module, "SKILLS_ROOT", skills_root)

    _, skill_file_index = discover_skills("supervisor")
    load_resource = create_load_skill_resource_tool("supervisor", lambda: skill_file_index)
    content = load_resource.invoke(
        {"skill_name": "analysis", "resource_path": "evaluation.md"}
    )

    assert "Loaded skill resource: analysis (evaluation.md)" in content
    assert "# 同级评估" in content


def test_load_skill_resource_reads_nested_relative_file(
        monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    """验证可读取技能目录内的相对路径文件。

    测试目的：
        确认 `./references/evaluation.md` 这类子目录路径可正常读取。

    预期结果：
        返回对应资源文件全文。
    """

    skills_root = tmp_path / "skills"
    _write_skill(skills_root, "supervisor/analysis", "analysis desc", skill_name="analysis")
    nested_file = skills_root / "supervisor/analysis/references/evaluation.md"
    nested_file.parent.mkdir(parents=True, exist_ok=True)
    nested_file.write_text("nested resource", encoding="utf-8")
    monkeypatch.setattr(scope_module, "SKILLS_ROOT", skills_root)

    _, skill_file_index = discover_skills("supervisor")
    load_resource = create_load_skill_resource_tool("supervisor", lambda: skill_file_index)
    content = load_resource.invoke(
        {"skill_name": "analysis", "resource_path": "./references/evaluation.md"}
    )

    assert "Loaded skill resource: analysis (references/evaluation.md)" in content
    assert "nested resource" in content


def test_load_skill_resource_rejects_absolute_path(
        monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    """验证拒绝绝对路径。

    测试目的：
        防止工具访问任意系统路径。

    预期结果：
        传入绝对路径时返回“必须是相对路径”错误。
    """

    skills_root = tmp_path / "skills"
    _write_skill(skills_root, "supervisor/analysis", "analysis desc", skill_name="analysis")
    monkeypatch.setattr(scope_module, "SKILLS_ROOT", skills_root)

    _, skill_file_index = discover_skills("supervisor")
    load_resource = create_load_skill_resource_tool("supervisor", lambda: skill_file_index)
    content = load_resource.invoke(
        {"skill_name": "analysis", "resource_path": str((tmp_path / "x.md").resolve())}
    )

    assert "relative path" in content.lower()


def test_load_skill_resource_rejects_parent_directory_traversal(
        monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    """验证拒绝父目录穿越。

    测试目的：
        防止通过 `../` 跳出技能目录边界。

    预期结果：
        返回路径穿越错误，不读取任何文件。
    """

    skills_root = tmp_path / "skills"
    _write_skill(skills_root, "supervisor/analysis", "analysis desc", skill_name="analysis")
    monkeypatch.setattr(scope_module, "SKILLS_ROOT", skills_root)

    _, skill_file_index = discover_skills("supervisor")
    load_resource = create_load_skill_resource_tool("supervisor", lambda: skill_file_index)
    content = load_resource.invoke(
        {"skill_name": "analysis", "resource_path": "../outside.txt"}
    )

    assert "parent directory traversal" in content.lower()


def test_load_skill_resource_blocks_symlink_escape(
        monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    """验证符号链接越界会被阻断。

    测试目的：
        防止在技能目录内通过软链读取技能目录之外的文件。

    预期结果：
        访问软链目标时返回越界错误。
    """

    skills_root = tmp_path / "skills"
    _write_skill(skills_root, "supervisor/analysis", "analysis desc", skill_name="analysis")
    outside_file = tmp_path / "outside.txt"
    outside_file.write_text("escape", encoding="utf-8")

    symlink_path = skills_root / "supervisor/analysis/escape.md"
    try:
        symlink_path.symlink_to(outside_file)
    except OSError:
        pytest.skip("当前环境不支持创建符号链接")
    monkeypatch.setattr(scope_module, "SKILLS_ROOT", skills_root)

    _, skill_file_index = discover_skills("supervisor")
    load_resource = create_load_skill_resource_tool("supervisor", lambda: skill_file_index)
    content = load_resource.invoke(
        {"skill_name": "analysis", "resource_path": "escape.md"}
    )

    assert "escapes skill directory" in content.lower()


def test_load_skill_resource_rejects_large_file_over_128kb(
        monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    """验证超过 128KB 文件会被拒绝。

    测试目的：
        避免单次加载超大文本挤占上下文窗口。

    预期结果：
        返回文件过大错误。
    """

    skills_root = tmp_path / "skills"
    _write_skill(skills_root, "supervisor/analysis", "analysis desc", skill_name="analysis")
    large_file = skills_root / "supervisor/analysis/large.txt"
    large_file.write_text("a" * (128 * 1024 + 1), encoding="utf-8")
    monkeypatch.setattr(scope_module, "SKILLS_ROOT", skills_root)

    _, skill_file_index = discover_skills("supervisor")
    load_resource = create_load_skill_resource_tool("supervisor", lambda: skill_file_index)
    content = load_resource.invoke({"skill_name": "analysis", "resource_path": "large.txt"})

    assert "too large" in content.lower()


def test_load_skill_resource_rejects_non_utf8_file(
        monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    """验证仅允许 UTF-8 文本文件。

    测试目的：
        防止模型读取不可解码的二进制/非 UTF-8 文件。

    预期结果：
        返回“Only UTF-8 text files are supported.”。
    """

    skills_root = tmp_path / "skills"
    _write_skill(skills_root, "supervisor/analysis", "analysis desc", skill_name="analysis")
    binary_file = skills_root / "supervisor/analysis/binary.bin"
    binary_file.write_bytes(b"\xff\xfe\x00\x80")
    monkeypatch.setattr(scope_module, "SKILLS_ROOT", skills_root)

    _, skill_file_index = discover_skills("supervisor")
    load_resource = create_load_skill_resource_tool("supervisor", lambda: skill_file_index)
    content = load_resource.invoke({"skill_name": "analysis", "resource_path": "binary.bin"})

    assert "utf-8" in content.lower()


def test_load_skill_resource_returns_available_names_when_skill_not_found(
        monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    """验证技能名未命中时返回可用技能列表。

    测试目的：
        让模型在调用错误时能快速回退到正确 skill 名。

    预期结果：
        返回 not found，并列出 available skills。
    """

    skills_root = tmp_path / "skills"
    _write_skill(skills_root, "supervisor/analysis", "analysis desc", skill_name="analysis")
    monkeypatch.setattr(scope_module, "SKILLS_ROOT", skills_root)

    _, skill_file_index = discover_skills("supervisor")
    load_resource = create_load_skill_resource_tool("supervisor", lambda: skill_file_index)
    content = load_resource.invoke(
        {"skill_name": "missing", "resource_path": "evaluation.md"}
    )

    assert "not found" in content.lower()
    assert "analysis" in content


def test_list_skill_resources_returns_tree_with_relative_paths(
        monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    """验证目录树工具返回结构化树形结果。

    测试目的：
        确认工具只返回目录/文件结构，路径为相对路径，且顺序稳定（目录优先、名称排序）。

    预期结果：
        返回 `skill_name/max_depth/tree`，包含 `scripts/references/assets/SKILL.md` 等节点。
    """

    skills_root = tmp_path / "skills"
    _write_skill(skills_root, "supervisor/analysis", "analysis desc", skill_name="analysis")
    (skills_root / "supervisor/analysis/scripts").mkdir(parents=True, exist_ok=True)
    (skills_root / "supervisor/analysis/scripts/main.py").write_text("print('ok')", encoding="utf-8")
    (skills_root / "supervisor/analysis/references").mkdir(parents=True, exist_ok=True)
    (skills_root / "supervisor/analysis/references/evaluation.md").write_text("# eval", encoding="utf-8")
    (skills_root / "supervisor/analysis/assets").mkdir(parents=True, exist_ok=True)
    (skills_root / "supervisor/analysis/assets/logo.txt").write_text("logo", encoding="utf-8")
    monkeypatch.setattr(scope_module, "SKILLS_ROOT", skills_root)

    _, skill_file_index = discover_skills("supervisor")
    list_tool = create_list_skill_resources_tool("supervisor", lambda: skill_file_index)
    result = list_tool.invoke({"skill_name": "analysis"})

    assert result["skill_name"] == "analysis"
    assert result["max_depth"] == 3
    tree = result["tree"]
    assert isinstance(tree, list)

    root_names = [node["name"] for node in tree]
    assert root_names == ["assets", "references", "scripts", "SKILL.md"]

    flattened_paths = _flatten_tree_paths(tree)
    assert "assets" in flattened_paths
    assert "assets/logo.txt" in flattened_paths
    assert "references" in flattened_paths
    assert "references/evaluation.md" in flattened_paths
    assert "scripts" in flattened_paths
    assert "scripts/main.py" in flattened_paths
    assert "SKILL.md" in flattened_paths
    assert all(not str(path).startswith("/") for path in flattened_paths)


def test_list_skill_resources_respects_max_depth_three(
        monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    """验证目录树工具的最大层级限制。

    测试目的：
        防止工具递归过深，保持输出可控与稳定。

    预期结果：
        深度 4 的文件不会被展开到结果中。
    """

    skills_root = tmp_path / "skills"
    _write_skill(skills_root, "supervisor/analysis", "analysis desc", skill_name="analysis")
    deep_file = skills_root / "supervisor/analysis/l1/l2/l3/l4.txt"
    deep_file.parent.mkdir(parents=True, exist_ok=True)
    deep_file.write_text("too deep", encoding="utf-8")
    monkeypatch.setattr(scope_module, "SKILLS_ROOT", skills_root)

    _, skill_file_index = discover_skills("supervisor")
    list_tool = create_list_skill_resources_tool("supervisor", lambda: skill_file_index)
    result = list_tool.invoke({"skill_name": "analysis"})

    flattened_paths = _flatten_tree_paths(result["tree"])
    assert "l1" in flattened_paths
    assert "l1/l2" in flattened_paths
    assert "l1/l2/l3" in flattened_paths
    assert "l1/l2/l3/l4.txt" not in flattened_paths


def test_list_skill_resources_filters_hidden_entries(
        monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    """验证目录树工具默认过滤隐藏文件与隐藏目录。

    测试目的：
        避免将不应暴露给模型的隐藏资源加入目录树。

    预期结果：
        `.secret`、`.hidden` 不出现在树结构中。
    """

    skills_root = tmp_path / "skills"
    _write_skill(skills_root, "supervisor/analysis", "analysis desc", skill_name="analysis")
    (skills_root / "supervisor/analysis/.secret").write_text("hidden", encoding="utf-8")
    hidden_dir = skills_root / "supervisor/analysis/.hidden"
    hidden_dir.mkdir(parents=True, exist_ok=True)
    (hidden_dir / "a.txt").write_text("x", encoding="utf-8")
    monkeypatch.setattr(scope_module, "SKILLS_ROOT", skills_root)

    _, skill_file_index = discover_skills("supervisor")
    list_tool = create_list_skill_resources_tool("supervisor", lambda: skill_file_index)
    result = list_tool.invoke({"skill_name": "analysis"})

    flattened_paths = _flatten_tree_paths(result["tree"])
    assert ".secret" not in flattened_paths
    assert ".hidden" not in flattened_paths
    assert ".hidden/a.txt" not in flattened_paths


def test_list_skill_resources_skips_symlink_outside_skill_root(
        monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    """验证目录树工具会忽略软链目录，防止越界读取。

    测试目的：
        防止通过符号链接把技能目录外内容暴露给模型。

    预期结果：
        软链节点不出现在目录树结果中。
    """

    skills_root = tmp_path / "skills"
    _write_skill(skills_root, "supervisor/analysis", "analysis desc", skill_name="analysis")
    outside_dir = tmp_path / "outside"
    outside_dir.mkdir(parents=True, exist_ok=True)
    (outside_dir / "escape.txt").write_text("escape", encoding="utf-8")

    link_path = skills_root / "supervisor/analysis/outside_link"
    try:
        link_path.symlink_to(outside_dir, target_is_directory=True)
    except OSError:
        pytest.skip("当前环境不支持创建符号链接")

    monkeypatch.setattr(scope_module, "SKILLS_ROOT", skills_root)
    _, skill_file_index = discover_skills("supervisor")
    list_tool = create_list_skill_resources_tool("supervisor", lambda: skill_file_index)
    result = list_tool.invoke({"skill_name": "analysis"})

    flattened_paths = _flatten_tree_paths(result["tree"])
    assert "outside_link" not in flattened_paths


def test_list_skill_resources_returns_available_skills_when_not_found(
        monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    """验证目录树工具未命中时返回可用技能名。

    测试目的：
        保证模型输入错误技能名时可以根据返回列表快速重试。

    预期结果：
        响应包含 `error` 与 `available_skills`。
    """

    skills_root = tmp_path / "skills"
    _write_skill(skills_root, "supervisor/analysis", "analysis desc", skill_name="analysis")
    monkeypatch.setattr(scope_module, "SKILLS_ROOT", skills_root)

    _, skill_file_index = discover_skills("supervisor")
    list_tool = create_list_skill_resources_tool("supervisor", lambda: skill_file_index)
    result = list_tool.invoke({"skill_name": "missing"})

    assert "error" in result
    assert "not found" in result["error"].lower()
    assert result["available_skills"] == ["analysis"]


def test_before_agent_preloads_metadata_but_not_full_content(
        monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    """验证预加载不读取正文，工具调用时才读取。

    测试目的：
        保证渐进式加载：`before_agent` 只处理元数据。

    预期结果：
        `before_agent` 阶段读取次数为 0；调用 `load_skill` 后读取次数为 1。
    """

    skills_root = tmp_path / "skills"
    _write_skill(skills_root, "supervisor/analysis", "analysis desc", skill_name="analysis")
    monkeypatch.setattr(scope_module, "SKILLS_ROOT", skills_root)

    read_calls: list[Path] = []
    original_read_text = Path.read_text

    def _spy_read_text(path_obj: Path, *args, **kwargs):  # type: ignore[no-untyped-def]
        read_calls.append(path_obj)
        return original_read_text(path_obj, *args, **kwargs)

    monkeypatch.setattr(Path, "read_text", _spy_read_text)
    middleware = SkillMiddleware(scope="supervisor")
    middleware.before_agent({}, runtime=None)
    assert read_calls == []

    content = middleware.tools[0].invoke({"skill_name": "analysis"})
    assert "Loaded skill: analysis" in content
    assert len(read_calls) == 1


def test_skill_middleware_registers_list_skill_resources_tool() -> None:
    """验证中间件工具注册包含目录树工具。

    测试目的：
        防止重构后漏注册 `list_skill_resources`，导致模型无法探查技能目录。

    预期结果：
        `SkillMiddleware.tools` 同时包含 `load_skill`、`load_skill_resource`、`list_skill_resources`。
    """

    middleware = SkillMiddleware(scope="supervisor")
    tool_names = [tool.name for tool in middleware.tools]

    assert "load_skill" in tool_names
    assert "load_skill_resource" in tool_names
    assert "list_skill_resources" in tool_names


def test_wrap_model_call_injects_skills_system_prompt_without_paths() -> None:
    """验证系统提示词注入内容正确。

    测试目的：
        确认注入文本包含技能元数据与调用说明，但不泄露路径或全文。

    预期结果：
        包含 `name/description/license/metadata` 与技能工具调用说明，
        不包含技能绝对路径信息（如 `resources/skills`）。
    """

    middleware = SkillMiddleware(scope="supervisor")
    request = ModelRequest(
        model=object(),
        messages=[],
        system_message=SystemMessage(content="base prompt"),
        tools=[],
        state={
            "skills_metadata": [
                {
                    "name": "analysis",
                    "description": "analysis desc",
                    "license": "Apache-2.0",
                    "metadata": {"author": "example-org", "version": "1.0"},
                }
            ]
        },
        runtime=None,
    )

    captured_request: dict[str, ModelRequest] = {}

    def handler(modified_request: ModelRequest) -> ModelResponse:
        captured_request["request"] = modified_request
        return ModelResponse(result=[AIMessage(content="ok")])

    middleware.wrap_model_call(request, handler)
    system_text = captured_request["request"].system_message.text

    assert "## 技能系统" in system_text
    assert "调用流程（渐进式降级）" in system_text
    assert "字段含义说明" in system_text
    assert "name: analysis" in system_text
    assert "description: analysis desc" in system_text
    assert "license: Apache-2.0" in system_text
    assert "metadata:" in system_text
    assert "author: example-org" in system_text
    assert "version: 1.0" in system_text
    assert "load_skill(\"<skill_name>\")" in system_text
    assert "load_skill_resource(\"<skill_name>\", \"<relative_path>\")" in system_text
    assert "list_skill_resources(\"<skill_name>\")" in system_text
    assert "resources/skills" not in system_text


def test_base_and_skill_middlewares_keep_expected_order_and_idempotency(
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    """验证 BasePromptMiddleware 与 SkillMiddleware 组合时顺序正确且不重复注入。

    测试目的：
        确保中间件组合后保持 `角色 -> base -> skills`，并且重复执行不会产生重复段落。

    预期结果：
        首次注入后顺序正确；再次注入时 base/skills 段均只出现一次。
    """

    monkeypatch.setattr(base_prompt_module, "load_prompt", lambda _path: "BASE RULES")
    base_middleware = BasePromptMiddleware()
    skill_middleware = SkillMiddleware(scope="supervisor")
    request = ModelRequest(
        model=object(),
        messages=[],
        system_message=SystemMessage(content="ROLE PROMPT"),
        tools=[],
        state={
            "skills_metadata": [
                {
                    "name": "analysis",
                    "description": "analysis desc",
                }
            ]
        },
        runtime=None,
    )

    def _run_chain(input_request: ModelRequest) -> ModelRequest:
        captured: dict[str, ModelRequest] = {}

        def final_handler(modified_request: ModelRequest) -> ModelResponse:
            captured["request"] = modified_request
            return ModelResponse(result=[AIMessage(content="ok")])

        base_middleware.wrap_model_call(
            input_request,
            lambda req: skill_middleware.wrap_model_call(req, final_handler),
        )
        return captured["request"]

    first_result = _run_chain(request)
    first_text = first_result.system_message.text
    assert first_text.index("ROLE PROMPT") < first_text.index("BASE RULES")
    assert first_text.index("BASE RULES") < first_text.index("name: analysis")

    second_result = _run_chain(first_result)
    second_text = second_result.system_message.text
    assert second_text.count("BASE RULES") == 1
    assert second_text.count("调用流程（渐进式降级）") == 1


def test_wrap_model_call_replaces_existing_skills_section_when_metadata_changes() -> None:
    """验证技能元数据变化时替换已有技能段而不是追加。"""

    middleware = SkillMiddleware(
        scope="supervisor",
        system_prompt_template="SKILL PREFIX\n\n{skills_list}\n\nSKILL SUFFIX",
    )
    first_request = ModelRequest(
        model=object(),
        messages=[],
        system_message=SystemMessage(content="base prompt"),
        tools=[],
        state={
            "skills_metadata": [
                {
                    "name": "analysis",
                    "description": "old desc",
                }
            ]
        },
        runtime=None,
    )
    captured_first: dict[str, ModelRequest] = {}

    def _first_handler(modified_request: ModelRequest) -> ModelResponse:
        captured_first["request"] = modified_request
        return ModelResponse(result=[AIMessage(content="ok")])

    middleware.wrap_model_call(first_request, _first_handler)
    first_text = captured_first["request"].system_message.text
    assert "description: old desc" in first_text

    second_request = ModelRequest(
        model=object(),
        messages=[],
        system_message=SystemMessage(content=first_text),
        tools=[],
        state={
            "skills_metadata": [
                {
                    "name": "analysis",
                    "description": "new desc",
                },
                {
                    "name": "report",
                    "description": "report desc",
                },
            ]
        },
        runtime=None,
    )
    captured_second: dict[str, ModelRequest] = {}

    def _second_handler(modified_request: ModelRequest) -> ModelResponse:
        captured_second["request"] = modified_request
        return ModelResponse(result=[AIMessage(content="ok")])

    middleware.wrap_model_call(second_request, _second_handler)
    second_text = captured_second["request"].system_message.text

    assert second_text.count("SKILL PREFIX") == 1
    assert second_text.count("SKILL SUFFIX") == 1
    assert "description: old desc" not in second_text
    assert "description: new desc" in second_text
    assert "name: report" in second_text


def test_runtime_state_preserves_skills_metadata_for_prompt_injection(
        monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    """验证真实运行链路中 `skills_metadata` 不会被状态系统丢弃。

    测试目的：
        覆盖 `before_agent -> wrap_model_call` 的实际集成路径，确保可用技能可注入提示词。

    预期结果：
        `_build_skills_section` 收到非空技能元数据，且包含预加载的技能名称。
    """

    skills_root = tmp_path / "skills"
    _write_skill(skills_root, "supervisor/analysis", "analysis desc", skill_name="analysis")
    monkeypatch.setattr(scope_module, "SKILLS_ROOT", skills_root)

    middleware = SkillMiddleware(scope="supervisor")
    captured_metadata: list[list[dict[str, Any]]] = []
    original_build = middleware._build_skills_section

    def _spy_build(skills_metadata):  # type: ignore[no-untyped-def]
        captured_metadata.append(list(skills_metadata))
        return original_build(skills_metadata)

    monkeypatch.setattr(middleware, "_build_skills_section", _spy_build)

    agent = lc_create_agent(
        model=_ToolFriendlyFakeListChatModel(responses=["ok"]),
        middleware=[middleware],
    )
    agent.invoke({"messages": [{"role": "user", "content": "hi"}]})

    assert captured_metadata
    assert captured_metadata[0]
    assert captured_metadata[0][0]["name"] == "analysis"


def test_old_skill_module_path_removed() -> None:
    """验证旧模块入口已移除。

    测试目的：
        防止调用方继续依赖已废弃的旧导入路径。

    预期结果：
        导入 `app.core.skill` 抛出 `ModuleNotFoundError`。
    """

    with pytest.raises(ModuleNotFoundError):
        importlib.import_module("app.core.skill")
