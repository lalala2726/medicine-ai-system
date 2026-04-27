"""
通用 RAG 查询工具。

说明：
1. 本模块统一收敛管理端与客户端的知识库查询工具；
2. 两端共享同一套入参校验与查询实现；
3. 管理端与客户端仅在知识库作用域和工具描述文案上存在差异。
"""

from __future__ import annotations

from langchain.tools import tool as admin_tool
from langchain_core.tools import tool as client_tool
from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.core.agent.middleware import (
    tool_call_status,
    tool_thinking_redaction,
)
from app.core.config_sync import KnowledgeBaseScope
from app.rag import format_knowledge_search_hits, query_knowledge_by_rewritten_question


class KnowledgeSearchToolRequest(BaseModel):
    """
    功能描述：
        通用知识库检索工具入参模型。

    参数说明：
        query (str): 用户原始问题。

    返回值：
        无（数据模型定义）。

    异常说明：
        ValueError: 当 query 去空白后为空时抛出。
    """

    model_config = ConfigDict(extra="forbid")

    query: str = Field(..., min_length=1, description="用户原始问题")

    @field_validator("query")
    @classmethod
    def normalize_query(cls, value: str) -> str:
        """
        功能描述：
            规范化知识库检索问题文本。

        参数说明：
            value (str): 原始问题文本。

        返回值：
            str: 去除首尾空白后的问题文本。

        异常说明：
            ValueError: 当文本为空时抛出。
        """

        normalized = value.strip()
        if not normalized:
            raise ValueError("query 不能为空")
        return normalized


def _search_knowledge_context(*, query: str, scope: KnowledgeBaseScope) -> str:
    """
    功能描述：
        按指定知识库作用域执行问题改写检索，并返回格式化后的知识片段文本。

    参数说明：
        query (str): 用户原始问题。
        scope (KnowledgeBaseScope): 当前检索使用的知识库作用域。

    返回值：
        str: 格式化后的知识检索结果文本。

    异常说明：
        无。
    """

    hits = query_knowledge_by_rewritten_question(
        question=query,
        top_k=None,
        scope=scope,
    )
    return format_knowledge_search_hits(hits)


@admin_tool(
    args_schema=KnowledgeSearchToolRequest,
    description=(
            "检索固定知识库中的相关文档片段。"
            "适用于制度说明、文档知识、产品资料、FAQ、规则解释等问题。"
            "直接传入用户原始问题，不要拼接额外提示语。"
    ),
)
@tool_thinking_redaction(display_name="知识库检索")
@tool_call_status(
    tool_name="知识库检索",
    start_message="正在检索知识库",
    error_message="知识库检索失败",
    timely_message="知识库检索正在持续处理中",
)
def search_knowledge_context(query: str) -> str:
    """
    功能描述：
        对管理端知识库执行问题改写检索，并返回格式化后的知识片段文本。

    参数说明：
        query (str): 用户原始问题。

    返回值：
        str: 管理端知识检索结果的格式化文本。

    异常说明：
        无。
    """

    return _search_knowledge_context(
        query=query,
        scope=KnowledgeBaseScope.ADMIN,
    )


@client_tool(
    args_schema=KnowledgeSearchToolRequest,
    description=(
            "检索客户端聊天节点使用的固定知识库文档片段。"
            "适用于用药说明、产品资料、规则说明、FAQ、科普知识等问题。"
            "直接传入用户原始问题，不要拼接额外提示语。"
    ),
)
@tool_thinking_redaction(display_name="知识库检索")
def search_client_knowledge_context(query: str) -> str:
    """
    功能描述：
        对客户端知识库执行问题改写检索，并返回格式化后的知识片段文本。

    参数说明：
        query (str): 用户原始问题。

    返回值：
        str: 客户端知识检索结果的格式化文本。

    异常说明：
        无。
    """

    return _search_knowledge_context(
        query=query,
        scope=KnowledgeBaseScope.CLIENT,
    )


__all__ = [
    "KnowledgeSearchToolRequest",
    "search_client_knowledge_context",
    "search_knowledge_context",
]
