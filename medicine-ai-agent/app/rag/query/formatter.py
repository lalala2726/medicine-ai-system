from __future__ import annotations

from app.rag.query.constants import RAG_MAX_CONTEXT_CHARS
from app.rag.query.types import KnowledgeSearchHit


def trim_content_to_budget(*, content: str, remaining_chars: int) -> str:
    """将片段内容裁剪到剩余预算以内。

    Args:
        content: 原始片段文本。
        remaining_chars: 当前剩余的字符预算。

    Returns:
        裁剪后的片段文本。
    """

    if remaining_chars <= 0:
        return ""
    if len(content) <= remaining_chars:
        return content
    if remaining_chars <= 3:
        return content[:remaining_chars]
    return f"{content[:remaining_chars - 3]}..."


def format_knowledge_search_hits(hits: list[KnowledgeSearchHit]) -> str:
    """将结构化命中结果渲染为 Agent 可消费的文本块。

    Args:
        hits: 已排序的知识片段命中列表。

    Returns:
        适合直接拼接到 Agent 上下文中的文本块；未命中时返回固定提示语。
    """

    if not hits:
        return "未检索到相关知识。"

    lines = ["已检索到以下知识片段："]
    consumed_chars = 0
    rendered_count = 0
    for hit in hits:
        remaining_chars = RAG_MAX_CONTEXT_CHARS - consumed_chars
        if remaining_chars <= 0:
            break

        rendered_content = trim_content_to_budget(
            content=hit.content,
            remaining_chars=remaining_chars,
        ).strip()
        if not rendered_content:
            break

        rendered_count += 1
        consumed_chars += len(rendered_content)
        meta_parts = [
            f"knowledge_name={hit.knowledge_name}",
            f"score={hit.score:.4f}",
        ]
        if hit.document_id is not None:
            meta_parts.append(f"document_id={hit.document_id}")
        if hit.chunk_index is not None:
            meta_parts.append(f"chunk_index={hit.chunk_index}")
        if hit.char_count is not None:
            meta_parts.append(f"char_count={hit.char_count}")
        lines.append(f"[片段{rendered_count}] {', '.join(meta_parts)}\n{rendered_content}")

    if rendered_count == 0:
        return "未检索到相关知识。"
    return "\n\n".join(lines)
