from __future__ import annotations

import base64
import hashlib
import hmac


def sign_hmac_sha256_base64url(*, secret: str, canonical_string: str) -> str:
    """使用 HMAC-SHA256 对 canonical string 签名并输出 Base64URL。

    Args:
        secret: 签名密钥。
        canonical_string: 待签名原文。

    Returns:
        str: Base64URL 签名值（去除尾部 `=`）。
    """
    digest = hmac.new(
        secret.encode("utf-8"),
        canonical_string.encode("utf-8"),
        hashlib.sha256,
    ).digest()
    return base64.urlsafe_b64encode(digest).decode("utf-8").rstrip("=")


def is_signature_equal(*, expected: str, actual: str) -> bool:
    """安全比较签名字符串。

    Args:
        expected: 期望签名。
        actual: 实际签名。

    Returns:
        bool: 匹配返回 True，否则 False。
    """
    return hmac.compare_digest(expected, actual)
