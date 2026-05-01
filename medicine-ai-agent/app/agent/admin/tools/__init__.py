"""
管理端单 Agent 工具包。

说明：
1. 该包集中管理 admin 单 Agent 的基础工具、业务工具与注册中心；
2. 业务工具按领域拆分到独立模块，避免再次回到多 domain 节点结构；
3. 通用 middleware 统一收口到 `app.core.agent.middleware`。
"""

from app.agent.admin.tools.registry import ADMIN_TOOL_REGISTRY, AdminToolRegistry

__all__ = [
    "ADMIN_TOOL_REGISTRY",
    "AdminToolRegistry",
]
