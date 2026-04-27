from __future__ import annotations

from pathlib import Path, PurePosixPath

RESOURCES_DIR = Path(__file__).resolve().parents[2] / "resources"


def _normalize_relative_path(value: str, *, field_name: str) -> str:
    raw_value = str(value or "").strip()
    if not raw_value:
        raise ValueError(f"{field_name} cannot be empty")

    posix_path = PurePosixPath(raw_value.replace("\\", "/"))
    if posix_path.is_absolute():
        raise ValueError(f"{field_name} must be a relative path")

    normalized_parts: list[str] = []
    for part in posix_path.parts:
        if part in {"", "."}:
            continue
        if part == "..":
            raise ValueError(f"{field_name} cannot contain parent traversal '..'")
        normalized_parts.append(part)

    if not normalized_parts:
        raise ValueError(f"{field_name} cannot be empty")

    return "/".join(normalized_parts)


def _validate_allowed_suffixes(
        *,
        normalized_name: str,
        allowed_suffixes: tuple[str, ...] | None,
) -> None:
    if not allowed_suffixes:
        return

    normalized_suffixes: tuple[str, ...] = tuple(
        suffix if suffix.startswith(".") else f".{suffix}"
        for suffix in allowed_suffixes
    )
    if not normalized_name.endswith(normalized_suffixes):
        raise ValueError(
            f"Resource path must include one of suffixes: {', '.join(normalized_suffixes)}"
        )


def load_resource_text_from_root(
        resource_root: Path,
        name: str,
        *,
        allowed_suffixes: tuple[str, ...] | None = None,
        cache: dict[str, str] | None = None,
) -> str:
    normalized_name = _normalize_relative_path(name, field_name="Resource name")
    _validate_allowed_suffixes(
        normalized_name=normalized_name,
        allowed_suffixes=allowed_suffixes,
    )

    if cache is not None:
        cached = cache.get(normalized_name)
        if cached is not None:
            return cached

    resolved_root = resource_root.resolve()
    file_path = (resolved_root / Path(*normalized_name.split("/"))).resolve()
    if not file_path.is_relative_to(resolved_root):
        raise ValueError("Resource path escapes resource root")
    if not file_path.exists() or not file_path.is_file():
        raise FileNotFoundError(f"Resource file not found: {file_path}")

    text = file_path.read_text(encoding="utf-8")
    if cache is not None:
        cache[normalized_name] = text
    return text


def load_resource_text(
        resource_subdir: str,
        name: str,
        *,
        allowed_suffixes: tuple[str, ...] | None = None,
        cache: dict[str, str] | None = None,
) -> str:
    normalized_subdir = _normalize_relative_path(
        resource_subdir,
        field_name="Resource subdir",
    )
    resources_root = RESOURCES_DIR.resolve()
    resolved_subdir = (resources_root / Path(*normalized_subdir.split("/"))).resolve()
    if not resolved_subdir.is_relative_to(resources_root):
        raise ValueError("Resource subdir escapes resources root")

    return load_resource_text_from_root(
        resolved_subdir,
        name,
        allowed_suffixes=allowed_suffixes,
        cache=cache,
    )


__all__ = [
    "RESOURCES_DIR",
    "load_resource_text",
    "load_resource_text_from_root",
]
