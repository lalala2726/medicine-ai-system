import os
from typing import Any


def _merge_origin_regex_patterns(*patterns: str) -> str:
    """
    合并多个完整的 Origin 正则为一个可供 CORS 使用的模式。

    Args:
        *patterns: 待合并的完整 Origin 正则表达式列表，允许包含 `^` 与 `$` 锚点。

    Returns:
        str: 合并后的完整 Origin 正则表达式。
    """
    normalized_patterns = [
        pattern.removeprefix("^").removesuffix("$")
        for pattern in patterns
        if pattern
    ]
    return rf"^(?:{'|'.join(normalized_patterns)})$"


# 本地开发默认允许的 localhost/localhost 来源。
LOCALHOST_ORIGIN_REGEX = r"^https?://(localhost|127\.0\.0\.1)(:\d+)?$"
# 局域网调试时允许的私有网段来源，例如 192.168.x.x / 10.x.x.x / 172.16.x.x-172.31.x.x。
LOCAL_AREA_NETWORK = (
    r"^https?://(192\.168\.\d+\.\d+|10\.\d+\.\d+\.\d+|172\.(1[6-9]|2\d|3[0-1])\.\d+\.\d+)(:\d+)?$"
)
# 正式环境默认允许 zhangyichuang.com 主域及其任意层级子域名。
ZHANGYICHUANG_ORIGIN_REGEX = r"^https?://(?:[A-Za-z0-9-]+\.)*zhangyichuang\.com(?::\d+)?$"
# 默认 CORS 来源规则：同时允许 localhost、局域网调试来源以及 zhangyichuang.com 域名体系。
DEFAULT_CORS_ALLOW_ORIGIN_REGEX = _merge_origin_regex_patterns(
    LOCALHOST_ORIGIN_REGEX,
    LOCAL_AREA_NETWORK,
    ZHANGYICHUANG_ORIGIN_REGEX,
)


def _parse_csv_env(name: str, default: list[str]) -> list[str]:
    """
    解析逗号分隔的环境变量为字符串列表。

    Args:
        name: 环境变量名称。
        default: 环境变量不存在或解析为空时使用的默认值。

    Returns:
        list[str]: 解析后的非空字符串列表。
    """
    value = os.getenv(name)
    if value is None:
        return default
    items = [item.strip() for item in value.split(",") if item.strip()]
    return items or default


def _parse_bool_env(name: str, default: bool) -> bool:
    """
    解析布尔类型环境变量。

    支持：
    - 真值：`1/true/yes/on`
    - 假值：`0/false/no/off`
    其他非法值回落到默认值。

    Args:
        name: 环境变量名称。
        default: 变量不存在或值非法时使用的默认值。

    Returns:
        bool: 解析后的布尔值。
    """
    value = os.getenv(name)
    if value is None:
        return default
    normalized = value.strip().lower()
    if normalized in {"1", "true", "yes", "on"}:
        return True
    if normalized in {"0", "false", "no", "off"}:
        return False
    return default


def load_cors_config() -> dict[str, Any]:
    """
    加载 FastAPI CORS 中间件配置。

    规则：
    1. 若 `CORS_ALLOW_ORIGINS` 显式配置且非空，则优先使用 origins 列表；
    2. 否则使用 `CORS_ALLOW_ORIGIN_REGEX`（默认允许 localhost、局域网调试来源与 zhangyichuang.com 域名体系）；
    3. methods/headers/credentials 由对应环境变量解析，未配置使用默认值。

    Returns:
        dict[str, Any]: 可直接传给 `CORSMiddleware` 的配置字典。
    """
    allow_origins = _parse_csv_env("CORS_ALLOW_ORIGINS", [])
    allow_origin_regex = os.getenv(
        "CORS_ALLOW_ORIGIN_REGEX",
        DEFAULT_CORS_ALLOW_ORIGIN_REGEX,
    )
    if allow_origins:
        allow_origin_regex = None

    return {
        "allow_origins": allow_origins,
        "allow_origin_regex": allow_origin_regex,
        "allow_methods": _parse_csv_env("CORS_ALLOW_METHODS", ["*"]),
        "allow_headers": _parse_csv_env("CORS_ALLOW_HEADERS", ["*"]),
        "allow_credentials": _parse_bool_env("CORS_ALLOW_CREDENTIALS", True),
    }
