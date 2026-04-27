from app.core.security.system_auth.signer import (
    is_signature_equal,
    sign_hmac_sha256_base64url,
)


def test_sign_hmac_sha256_base64url_returns_stable_value() -> None:
    """验证签名算法输出稳定。"""
    signature = sign_hmac_sha256_base64url(
        secret="secret-1",
        canonical_string="GET\n/demo\n\n1772700000\nnonce\nhash",
    )
    assert signature == "ZVwlnchQhSsMBJhzJQ1gMiN4upipvspWBNWui74uTOc"


def test_is_signature_equal_supports_secure_compare() -> None:
    """验证签名比较函数的匹配逻辑。"""
    assert is_signature_equal(expected="abc", actual="abc") is True
    assert is_signature_equal(expected="abc", actual="abd") is False
