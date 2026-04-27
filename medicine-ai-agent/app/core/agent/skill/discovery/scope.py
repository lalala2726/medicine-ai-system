from __future__ import annotations

from pathlib import Path

from app.utils.resource_text_utils import RESOURCES_DIR

SKILLS_ROOT = RESOURCES_DIR / "skills"
_MAX_SCOPE_LEVEL = 3


def normalize_scope(scope: str | None) -> tuple[str, Path]:
    """规范化并校验 Skill 作用域。

    作用：
        将传入的 `scope` 清洗为标准格式，并计算其在技能根目录下的绝对路径，
        同时执行层级限制与路径安全校验。

    参数：
        scope: Skill 作用域，例如 `supervisor` 或 `supervisor/order`。
            当 scope 为空（`None`、空字符串、仅空白）时，默认指向技能根目录。

    返回：
        tuple[str, Path]:
            - 规范化后的 scope 字符串（使用 `/` 分隔）
            - 对应的绝对目录路径

    异常：
        ValueError: 当 scope 层级超限或存在非法路径片段时抛出。
    """

    normalized = str(scope or "").strip().strip("/")
    parts = [part for part in normalized.split("/") if part]
    if len(parts) > _MAX_SCOPE_LEVEL:
        raise ValueError(f"Skill scope depth cannot exceed {_MAX_SCOPE_LEVEL}: {scope}")
    if any(part in {".", ".."} or "\\" in part for part in parts):
        raise ValueError(f"Invalid skill scope: {scope}")

    root = SKILLS_ROOT.resolve()
    scope_dir = (root / Path(*parts)).resolve()
    if not scope_dir.is_relative_to(root):
        raise ValueError(f"Skill scope escapes root path: {scope}")

    return "/".join(parts), scope_dir


def _is_path_within_root(path: Path) -> bool:
    """判断路径是否位于技能根目录之内。

    作用：
        防止路径越界访问，仅允许处理 `SKILLS_ROOT` 范围内的文件。

    参数：
        path: 待校验的路径。

    返回：
        bool: 若路径在技能根目录内返回 `True`，否则返回 `False`。
    """

    try:
        return path.resolve().is_relative_to(SKILLS_ROOT.resolve())
    except OSError:
        return False
