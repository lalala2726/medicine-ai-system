"""
Agent 中间件统一导出入口。
"""

from app.core.agent.middleware.base_prompt import BasePromptMiddleware
from app.core.agent.middleware.context_cache import (
    DashScopeExplicitCacheMiddleware,
    has_dashscope_explicit_cache_control,
)
from app.core.agent.middleware.dynamic_tool import (
    DynamicToolMiddleware,
    DynamicToolRegistryProtocol,
    DynamicToolingTextConfig,
    LoadToolsRequest,
    LoadableToolsCatalog,
    ManagedDynamicToolRegistry,
    create_list_loadable_tools_tool,
    create_load_tools_tool,
    extract_loaded_tool_keys_from_stream_result,
    merge_unique_loaded_tool_keys,
    normalize_loaded_tool_keys,
)
from app.core.agent.middleware.skill import SkillMiddleware
from app.core.agent.middleware.tool_call_limit import ToolCallLimitMiddleware
from app.core.agent.middleware.tool_trace_prompt import ToolTracePromptMiddleware
from app.core.agent.middleware.tool_trace_record import tool_trace_record
from app.core.agent.middleware.tool_thinking_redaction import (
    tool_thinking_redaction,
)
from app.core.agent.middleware.tool_status import (
    build_tool_status_middleware,
    tool_call_status,
)

__all__ = [
    "BasePromptMiddleware",
    "DashScopeExplicitCacheMiddleware",
    "DynamicToolMiddleware",
    "DynamicToolRegistryProtocol",
    "DynamicToolingTextConfig",
    "LoadToolsRequest",
    "LoadableToolsCatalog",
    "ManagedDynamicToolRegistry",
    "SkillMiddleware",
    "ToolCallLimitMiddleware",
    "ToolTracePromptMiddleware",
    "build_tool_status_middleware",
    "create_list_loadable_tools_tool",
    "create_load_tools_tool",
    "extract_loaded_tool_keys_from_stream_result",
    "has_dashscope_explicit_cache_control",
    "merge_unique_loaded_tool_keys",
    "normalize_loaded_tool_keys",
    "tool_call_status",
    "tool_trace_record",
    "tool_thinking_redaction",
]
