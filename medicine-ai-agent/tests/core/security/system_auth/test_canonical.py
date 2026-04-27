from app.core.security.system_auth.canonical import (
    build_canonical_string,
    normalize_query_pairs,
    sha256_hex,
)


def test_sha256_hex_returns_lowercase_digest() -> None:
    """验证 SHA256 摘要输出为小写十六进制。"""
    assert (
            sha256_hex(b"abc")
            == "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
    )


def test_normalize_query_pairs_sorts_key_and_value_with_duplicates() -> None:
    """验证 query 参数归一化会排序并保留重复键。"""
    query = [
        ("b", "2"),
        ("a", "3"),
        ("a", "1"),
        ("b", "1"),
    ]
    assert normalize_query_pairs(query) == "a=1&a=3&b=1&b=2"


def test_build_canonical_string_uses_six_lines_and_body_hash() -> None:
    """验证 canonical string 由六段组成且最后一段为 body SHA256。"""
    canonical = build_canonical_string(
        method="post",
        path="/knowledge_base/document/chunks/list",
        query_pairs=[("page", "1"), ("page_size", "50")],
        timestamp=1772700000,
        nonce="nonce-123",
        body_bytes=b'{"a":1}',
    )
    lines = canonical.split("\n")
    assert len(lines) == 6
    assert lines[0] == "POST"
    assert lines[1] == "/knowledge_base/document/chunks/list"
    assert lines[2] == "page=1&page_size=50"
    assert lines[3] == "1772700000"
    assert lines[4] == "nonce-123"
    assert lines[5] == "015abd7f5cc57a2dd94b7590f04ad8084273905ee33ec5cebeae62276a97f862"
