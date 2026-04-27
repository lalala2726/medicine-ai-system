#!/usr/bin/env python3
"""导入 MongoDB 地区数据。

该脚本读取当前目录下的 `regions.jsonl.gz`，默认导入到
`mongodb://root:123456@localhost:27017/medicine?authSource=admin` 的 `regions` 集合。
"""

from __future__ import annotations

import gzip
import os
from pathlib import Path

from bson import json_util
from pymongo import ASCENDING, MongoClient

# 当前脚本所在目录。
BASE_DIR = Path(__file__).resolve().parent
# 默认地区数据文件路径。
DEFAULT_DATA_FILE = BASE_DIR / "regions.jsonl.gz"
# 默认索引元数据文件路径。
DEFAULT_INDEX_FILE = BASE_DIR / "regions.indexes.json"
# 默认目标 MongoDB 主机。
DEFAULT_TARGET_HOST = "localhost"
# 默认目标 MongoDB 端口。
DEFAULT_TARGET_PORT = "27017"
# 默认目标 MongoDB 认证库。
DEFAULT_AUTH_DB = "admin"
# 默认目标业务库。
DEFAULT_TARGET_DB = "medicine"
# 默认目标地区集合。
DEFAULT_TARGET_COLLECTION = "regions"
# 默认目标 MongoDB 用户名。
DEFAULT_TARGET_USERNAME = "root"
# 默认目标 MongoDB 密码。
DEFAULT_TARGET_PASSWORD = "123456"


def env(name: str, default: str) -> str:
    """读取环境变量。

    Args:
        name: 环境变量名称。
        default: 环境变量不存在或为空字符串时使用的默认值。

    Returns:
        解析后的环境变量值。
    """

    value = os.getenv(name, "").strip()
    return value or default


def build_mongodb_uri() -> str:
    """构建目标 MongoDB 连接串。

    Returns:
        可直接传给 `MongoClient` 的连接串。
    """

    host = env("TARGET_HOST", DEFAULT_TARGET_HOST)
    port = env("TARGET_PORT", DEFAULT_TARGET_PORT)
    username = env("TARGET_USERNAME", DEFAULT_TARGET_USERNAME)
    password = env("TARGET_PASSWORD", DEFAULT_TARGET_PASSWORD)
    auth_db = env("TARGET_AUTH_DB", DEFAULT_AUTH_DB)
    target_db = env("TARGET_DB", DEFAULT_TARGET_DB)
    return f"mongodb://{username}:{password}@{host}:{port}/{target_db}?authSource={auth_db}"


def load_documents(data_file: Path) -> list[dict]:
    """从 gzip JSON Lines 文件中读取地区文档。

    Args:
        data_file: 地区数据文件路径。

    Returns:
        MongoDB 文档列表。
    """

    documents: list[dict] = []
    with gzip.open(data_file, "rt", encoding="utf-8") as file:
        for line in file:
            normalized_line = line.strip()
            if normalized_line:
                documents.append(json_util.loads(normalized_line))
    return documents


def create_basic_indexes(collection) -> None:
    """创建地区查询常用索引。

    Args:
        collection: 目标 MongoDB collection 对象。

    Returns:
        None。
    """

    collection.create_index([("id", ASCENDING)], name="idx_region_id")
    collection.create_index([("parent_id", ASCENDING)], name="idx_region_parent_id")
    collection.create_index([("level", ASCENDING)], name="idx_region_level")
    collection.create_index([("name", ASCENDING)], name="idx_region_name")
    collection.create_index([("pinyin", ASCENDING)], name="idx_region_pinyin")
    collection.create_index([("pinyin_prefix", ASCENDING)], name="idx_region_pinyin_prefix")


def main() -> None:
    """执行地区数据导入。

    Returns:
        None。
    """

    data_file = Path(env("REGION_DATA_FILE", str(DEFAULT_DATA_FILE)))
    target_db = env("TARGET_DB", DEFAULT_TARGET_DB)
    target_collection = env("TARGET_COLLECTION", DEFAULT_TARGET_COLLECTION)

    if not data_file.exists():
        raise FileNotFoundError(f"地区数据文件不存在：{data_file}")

    client = MongoClient(build_mongodb_uri(), serverSelectionTimeoutMS=8000)
    client.admin.command("ping")
    collection = client[target_db][target_collection]

    documents = load_documents(data_file)
    collection.drop()
    if documents:
        collection.insert_many(documents, ordered=False)
    create_basic_indexes(collection)
    client.close()

    print(f"导入完成：{target_db}.{target_collection}，记录数：{len(documents)}")


if __name__ == "__main__":
    main()
