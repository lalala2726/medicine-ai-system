from __future__ import annotations

import os
from urllib.parse import urlparse

from pymilvus import MilvusClient

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException


def _build_milvus_uri() -> str:
    """构建 Milvus 连接 URI。

    优先读取 `MILVUS_URI`，未配置时回退 `MILVUS_HOST + MILVUS_PORT`。

    Returns:
        str: 可用于 `MilvusClient` 初始化的完整 URI。
    """
    uri = os.getenv("MILVUS_URI")
    if uri:
        return uri

    host = os.getenv("MILVUS_HOST", "localhost")
    port = os.getenv("MILVUS_PORT", "19530")
    if host.startswith(("http://", "https://")):
        uri = host
    else:
        uri = f"http://{host}"

    parsed = urlparse(uri)
    if parsed.port is None:
        uri = f"{uri}:{port}"
    return uri


def _parse_milvus_timeout(timeout_value: str | None) -> float | None:
    """解析 Milvus 客户端超时配置。

    Args:
        timeout_value: 环境变量 `MILVUS_TIMEOUT` 原始值。

    Returns:
        float | None: 解析后的超时秒数；未配置时返回 `None`。

    Raises:
        ServiceException: 当 `MILVUS_TIMEOUT` 不是数字时抛出。
    """
    if not timeout_value:
        return None
    try:
        return float(timeout_value)
    except ValueError as exc:
        raise ServiceException(
            code=ResponseCode.INTERNAL_ERROR,
            message="MILVUS_TIMEOUT 必须是数字",
        ) from exc


def get_milvus_connection_args() -> dict[str, str | float]:
    """从环境变量构造可复用的 Milvus 连接参数。

    Returns:
        可直接透传给 ``MilvusClient`` 或 ``langchain_milvus.Milvus`` 的参数字典。

    Raises:
        ServiceException: 当超时配置值非法时抛出。
    """

    connection_args: dict[str, str | float] = {
        "uri": _build_milvus_uri(),
    }
    user = (os.getenv("MILVUS_USER") or os.getenv("MILVUS_USERNAME", "")).strip()
    password = (os.getenv("MILVUS_PASSWORD") or "").strip()
    token = (os.getenv("MILVUS_TOKEN") or "").strip()
    db_name = (os.getenv("MILVUS_DB_NAME") or "").strip()
    timeout = _parse_milvus_timeout(os.getenv("MILVUS_TIMEOUT"))

    if user:
        connection_args["user"] = user
    if password:
        connection_args["password"] = password
    if token:
        connection_args["token"] = token
    if db_name:
        connection_args["db_name"] = db_name
    if timeout is not None:
        connection_args["timeout"] = timeout
    return connection_args


def get_milvus_client() -> MilvusClient:
    """按环境变量配置创建 Milvus 客户端实例。

    Returns:
        MilvusClient: 可执行集合管理与向量写入/检索的客户端对象。

    Raises:
        ServiceException: 配置非法时抛出。
    """
    return MilvusClient(**get_milvus_connection_args())
