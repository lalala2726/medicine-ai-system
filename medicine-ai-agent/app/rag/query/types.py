from __future__ import annotations

from dataclasses import dataclass
from typing import Any


@dataclass(frozen=True)
class KnowledgeSearchRuntimeConfig:
    """表示一次知识库查询链路运行所需的完整配置。

    Attributes:
        provider_type: 当前请求使用的模型提供商类型。
        llm_base_url: 当前请求使用的模型服务基础地址。
        llm_api_key: 当前请求使用的模型服务密钥。
        knowledge_names: 当前请求允许访问的知识库名称列表。
        embedding_model_name: 当前请求使用的向量模型名称。
        embedding_dim: 当前请求使用的向量维度。
        ranking_enabled: 当前请求是否启用重排。
        ranking_model_name: 当前请求使用的重排模型名称。
        configured_top_k: 当前配置中心设置的默认返回条数。
    """

    provider_type: str
    llm_base_url: str
    llm_api_key: str
    knowledge_names: list[str]
    embedding_model_name: str
    embedding_dim: int
    ranking_enabled: bool
    ranking_model_name: str | None
    configured_top_k: int | None


@dataclass(frozen=True)
class KnowledgeSearchHit:
    """表示一条规范化后的知识库检索命中。

    Attributes:
        knowledge_name: 命中结果所在的知识库名称。
        content: 向量库返回的切片文本内容。
        score: 与该切片关联的相似度分数。
        document_id: 可选的业务文档 ID。
        chunk_index: 可选的文档内切片序号。
        char_count: 可选的切片字符数元信息。
    """

    knowledge_name: str
    content: str
    score: float
    document_id: int | None = None
    chunk_index: int | None = None
    char_count: int | None = None

    def to_dict(self) -> dict[str, Any]:
        """将命中结果序列化为普通字典。

        Returns:
            适合 JSON 序列化的字典视图。
        """

        return {
            "knowledge_name": self.knowledge_name,
            "content": self.content,
            "score": self.score,
            "document_id": self.document_id,
            "chunk_index": self.chunk_index,
            "char_count": self.char_count,
        }
