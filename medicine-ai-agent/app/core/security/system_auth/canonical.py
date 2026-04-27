from __future__ import annotations

import hashlib
from collections.abc import Iterable
from urllib.parse import urlencode

from fastapi import Request


def sha256_hex(data: bytes) -> str:
    """计算字节串 SHA256 十六进制摘要。

    Args:
        data: 原始字节串。

    Returns:
        str: 小写十六进制摘要。
    """
    return hashlib.sha256(data).hexdigest()


def normalize_query_pairs(query_pairs: Iterable[tuple[str, str]]) -> str:
    """归一化 query 参数用于签名。

    规则：
    1. 保留重复参数；
    2. 先按 key 升序，再按 value 升序；
    3. 使用 URL 编码后拼接为 query 字符串。

    Args:
        query_pairs: query 参数键值对迭代器。

    Returns:
        str: 归一化后的 query 字符串。
    """
    sorted_pairs = sorted(
        [(str(key), str(value)) for key, value in query_pairs],
        key=lambda item: (item[0], item[1]),
    )
    if not sorted_pairs:
        return ""
    return urlencode(sorted_pairs, doseq=True, safe="-_.~")


def build_canonical_string(
        *,
        method: str,
        path: str,
        query_pairs: Iterable[tuple[str, str]],
        timestamp: int,
        nonce: str,
        body_bytes: bytes,
) -> str:
    """构造签名 canonical string。

    格式：
    METHOD\\nPATH\\nSORTED_QUERY\\nTIMESTAMP\\nNONCE\\nBODY_SHA256

    Args:
        method: 请求方法。
        path: URL 路径。
        query_pairs: query 参数键值对。
        timestamp: 秒级时间戳。
        nonce: 随机串。
        body_bytes: 请求体原始字节串。

    Returns:
        str: canonical string。
    """
    method_text = method.upper().strip()
    path_text = path.strip() or "/"
    query_text = normalize_query_pairs(query_pairs)
    body_sha256 = sha256_hex(body_bytes)
    return "\n".join(
        [
            method_text,
            path_text,
            query_text,
            str(timestamp),
            nonce,
            body_sha256,
        ]
    )


def build_request_canonical_string(
        *,
        request: Request,
        timestamp: int,
        nonce: str,
        body_bytes: bytes,
) -> str:
    """基于 FastAPI 请求对象构造 canonical string。

    Args:
        request: FastAPI 请求对象。
        timestamp: 秒级时间戳。
        nonce: 随机串。
        body_bytes: 请求体原始字节串。

    Returns:
        str: canonical string。
    """
    query_pairs = [(key, value) for key, value in request.query_params.multi_items()]
    return build_canonical_string(
        method=request.method,
        path=request.url.path,
        query_pairs=query_pairs,
        timestamp=timestamp,
        nonce=nonce,
        body_bytes=body_bytes,
    )
