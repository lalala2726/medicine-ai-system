"""
Client LangGraph 运行入口。

该模块仅负责暴露编译后的 `graph` 变量，供 `langgraph.json` 或运行时
通过 `module:path` 方式动态加载。
"""

from app.agent.client.workflow import build_graph

graph = build_graph()
