from __future__ import annotations

from app.repositories import vector_repository

#: 知识库切片检索时只命中启用状态数据。
RAG_FILTER_EXPR = "status == 0"
#: 复用项目当前 Milvus 向量索引的默认检索参数。
RAG_SEARCH_PARAMS = {"metric_type": vector_repository.DEFAULT_VECTOR_METRIC_TYPE, "params": {}}
#: 知识检索返回给查询层的标准输出字段。
RAG_OUTPUT_FIELDS = ["document_id", "chunk_index", "char_count", "content"]
#: 当显式参数与 Redis 都未提供时的默认最终返回条数。
RAG_DEFAULT_FINAL_TOP_K = 10
#: 最终返回条数允许的最大值，避免上下文被无限放大。
RAG_MAX_FINAL_TOP_K = 100
#: 输出给 Agent 的知识上下文最大字符预算。
RAG_MAX_CONTEXT_CHARS = 12000
#: 单次最多允许同时查询的知识库数量。
RAG_MAX_KNOWLEDGE_NAMES = 10
#: 启用排序时的候选池最大规模。
RAG_MAX_CANDIDATE_POOL = 100
#: 检索问题改写提示词业务键。
RAG_REWRITE_PROMPT_KEY = "system_rewrite_rag_query_prompt"
#: 检索问题改写提示词本地回退路径。
RAG_REWRITE_PROMPT_LOCAL_PATH = "_system/rewrite_rag_query.md"
#: 当前唯一支持的重排模型名称。
QWEN3_RERANK_MODEL_NAME = "qwen3-rerank"
#: 当前重排能力允许使用的模型提供商类型。
RAG_RERANK_PROVIDER_TYPE = "aliyun"
#: `qwen3-rerank` 使用的 DashScope 兼容接口地址。
RAG_QWEN3_RERANK_ENDPOINT = "https://dashscope.aliyuncs.com/compatible-api/v1/reranks"
#: 重排 HTTP 请求超时时间（秒）。
RAG_RERANK_REQUEST_TIMEOUT_SECONDS = 20.0
