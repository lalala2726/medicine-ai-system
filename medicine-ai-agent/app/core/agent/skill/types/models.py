from __future__ import annotations

from pathlib import Path
from typing import Literal
from typing import NotRequired, TypeAlias, TypedDict


class SkillExtraMetadata(TypedDict):
    """技能扩展元数据。

    作用：
        约束 `metadata` 字段结构，避免使用无约束字典。

    字段：
        author: 技能作者或维护者。
        version: 技能版本号。
    """

    author: NotRequired[str]
    version: NotRequired[str]


class SkillMetadata(TypedDict):
    """技能元数据。

    作用：
        表示在预加载阶段暴露给模型的最小技能信息。

    字段：
        name: 技能唯一标识，供 `load_skill` 调用使用。
        description: 技能描述信息，用于系统提示词展示。
        license: 技能许可协议（可选）。
        metadata: 扩展元数据（可选）。
    """

    name: str
    description: str
    license: NotRequired[str]
    metadata: NotRequired[SkillExtraMetadata]


class SkillTreeNode(TypedDict):
    """技能目录树节点。

    作用：
        表示 `list_skill_resources` 工具返回的目录/文件节点。

    字段：
        name: 节点名称（文件名或目录名）。
        type: 节点类型，`dir` 或 `file`。
        path: 相对技能根目录的路径（不包含绝对路径）。
        children: 子节点列表，仅目录节点可包含该字段。
    """

    name: str
    type: Literal["dir", "file"]
    path: str
    children: NotRequired[list["SkillTreeNode"]]


class SkillTreeResponse(TypedDict):
    """技能目录树工具响应结构。

    作用：
        统一约束 `list_skill_resources` 返回 JSON 的字段格式。

    字段：
        skill_name: 请求的技能名称。
        max_depth: 目录树最大展开层级。
        tree: 目录树节点列表。
        error: 错误信息（可选）。
        available_skills: 可用技能名称列表（可选）。
    """

    skill_name: str
    max_depth: int
    tree: list[SkillTreeNode]
    error: NotRequired[str]
    available_skills: NotRequired[list[str]]


SkillFileIndex: TypeAlias = dict[str, Path]
