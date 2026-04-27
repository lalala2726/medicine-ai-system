"""
兼容层：Milvus 数据库客户端入口已迁移至 `app.core.database.milvus`。

保留本模块是为了避免历史导入路径在重构阶段立即失效。
"""

from app.core.database.milvus import get_milvus_client

__all__ = ["get_milvus_client"]
