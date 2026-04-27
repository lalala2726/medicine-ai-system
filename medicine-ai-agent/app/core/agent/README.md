# Agent Core 模块说明

## 1. 模块定位

`app/core/agent` 是助手运行时的核心基础层，负责承载：

- agent 中间件体系
- 运行时调用与消息提取
- SSE 事件总线与流式编排
- 工具调用追踪
- 工具缓存
- LangGraph Redis checkpoint
- 运行事件存储
- skill 发现、提示词与资源加载

这里不放具体业务 domain 的工具实现与节点路由，业务代码只应复用这里提供的通用能力。

## 2. 目录结构

```text
agent/
├── __init__.py
├── README.md
├── agent_event_bus.py
├── agent_orchestrator.py
├── agent_runtime.py
├── agent_tool_trace.py
├── langgraph_redis_checkpoint.py
├── middleware/
│   ├── __init__.py
│   ├── base_prompt.py
│   ├── dynamic_tool.py
│   ├── skill.py
│   ├── tool_call_limit.py
│   └── tool_status.py
├── run_event_store.py
├── skill/
│   ├── __init__.py
│   ├── discovery/
│   │   ├── __init__.py
│   │   ├── metadata.py
│   │   └── scope.py
│   ├── prompt/
│   │   ├── __init__.py
│   │   └── templates.py
│   ├── tool/
│   │   ├── __init__.py
│   │   ├── list_skill_resources.py
│   │   └── load_skill.py
│   └── types/
│       ├── __init__.py
│       └── models.py
└── tool_cache.py
```

## 3. 顶层文件职责

- `__init__.py`：`app.core.agent` 包入口，不再负责 middleware re-export。
- `README.md`：说明 `app/core/agent` 的最新目录结构、导入约定与文件职责。
- `agent_event_bus.py`：SSE 事件发射、状态节点上下文与最终响应队列管理。
- `agent_orchestrator.py`：管理助手流式输出编排层，负责 workflow 事件消费、SSE 封包与收尾流程。
- `agent_runtime.py`：对 agent invoke/ainvoke/stream 做统一运行时封装与消息标准化提取。
- `agent_tool_trace.py`：从 agent/LLM 响应中提取纯文本结果，供运行时与节点复用。
- `langgraph_redis_checkpoint.py`：LangGraph checkpoint 的 Redis 落盘与读取实现。
- `run_event_store.py`：助手运行元数据、事件流、快照与单活锁的 Redis 存储层。
- `tool_cache.py`：工具缓存协议、缓存 profile、prompt 渲染与 `tool_cacheable` 装饰器实现。

## 4. `middleware/` 目录职责

- `middleware/`：统一存放 repo 自有的 agent middleware 相关实现与导出入口。
- `middleware/__init__.py`：middleware 统一导入入口；业务代码统一从这里拿 middleware 相关类、函数和动态工具协议模型。
- `middleware/base_prompt.py`：`BasePromptMiddleware`，负责在模型调用前注入基础系统提示词。
- `middleware/dynamic_tool.py`：动态工具加载协议主体，包含动态工具请求模型、注册中心基类、动态工具中间件、工具目录与加载工具工厂。
- `middleware/skill.py`：`SkillMiddleware`，负责 skill 元数据预加载、技能提示词注入和 skill 工具注册。
- `middleware/tool_call_limit.py`：本地统一导出 `ToolCallLimitMiddleware`，避免业务代码直接依赖第三方导入路径。
- `middleware/tool_status.py`：工具状态装饰器和工具状态中间件实现，负责工具开始、成功、失败、持续处理中事件透传。

## 5. `skill/` 目录职责

- `skill/`：skill 能力子系统，负责 skill 发现、提示词渲染、资源加载和结构类型定义。
- `skill/__init__.py`：skill 子系统公共导出入口，导出 discovery、prompt 和 tool 能力，不再导出 middleware。

### 5.1 `skill/discovery/`

- `skill/discovery/`：skill 目录扫描与作用域解析模块。
- `skill/discovery/__init__.py`：导出 skill discovery 的公共函数和根目录常量。
- `skill/discovery/metadata.py`：扫描 skill 文件系统并组装 `SkillMetadata` / `SkillFileIndex`。
- `skill/discovery/scope.py`：解析和校验 skill scope，统一 skill 根路径约束。

### 5.2 `skill/prompt/`

- `skill/prompt/`：skill 提示词模板与渲染逻辑。
- `skill/prompt/__init__.py`：导出 skill 系统提示词模板和构建函数。
- `skill/prompt/templates.py`：维护 `SKILLS_SYSTEM_PROMPT` 模板并提供 prompt 渲染函数。

### 5.3 `skill/tool/`

- `skill/tool/`：skill 相关工具函数实现。
- `skill/tool/__init__.py`：导出 skill 工具创建函数。
- `skill/tool/list_skill_resources.py`：列出 skill 资源目录树，供模型先发现可读资源路径。
- `skill/tool/load_skill.py`：按 skill 名或资源路径加载 `SKILL.md` / 资源文件内容。

### 5.4 `skill/types/`

- `skill/types/`：skill 子系统的数据结构定义。
- `skill/types/__init__.py`：导出 skill 相关类型模型。
- `skill/types/models.py`：定义 `SkillMetadata`、`SkillFileIndex`、资源树响应等结构。

## 6. 导入约定

- 所有 middleware 统一从 `app.core.agent.middleware` 导入。
- 不再在业务 domain 目录下放 middleware wrapper。
- `skill` 目录只负责 skill 子系统本身，不再承担 middleware 导出职责。
