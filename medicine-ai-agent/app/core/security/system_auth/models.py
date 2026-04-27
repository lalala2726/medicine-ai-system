from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class SystemAuthPrincipal:
    """系统级认证主体信息。

    Args:
        app_id: 系统调用方标识。
        sign_version: 签名版本。
        timestamp: 请求时间戳（秒）。
        nonce: 请求随机串。
    """

    app_id: str
    sign_version: str
    timestamp: int
    nonce: str
