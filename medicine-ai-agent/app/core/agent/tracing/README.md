# Agent Tracing 目录说明

`app/core/agent/tracing/` 是智能体运行链路的 Trace 采集包，用来记录一次助手调用中的 graph、node、middleware、model、tool
等关键执行步骤，并将数据异步写入 MongoDB。这个包只负责采集和保存，不提供查询 API；Java 业务端后续直接读取 MongoDB 集合。

第一版的核心目标是：

- 不阻塞主对话链路，所有 Mongo 写入都通过后台队列异步完成。
- 统一维护 trace 上下文、span 层级、输入输出序列化、token 提取、Mongo 写入和生命周期管理。
- 固定写入 `agent_trace_runs` 和 `agent_trace_spans` 两个集合，避免和业务工具结果复用数据混在一起。

## 运行链路

一次助手调用开始时，流式编排层创建 run 和 root graph span，并把 `trace_id`、当前 `span_id`、span 栈写入 `ContextVar`
。后续节点装饰器、模型 middleware、工具 middleware 都从上下文中读取当前 trace 信息，创建自己的 span，并在结束时把 span
事件投递到异步 writer。

整体流程如下：

1. `iterate_assistant_responses(...)` 创建 trace run 和 graph span。
2. `@agent_trace(...)` 记录 agent node 的输入、输出、异常和耗时。
3. `TraceModelMiddleware` 记录模型可见工具、输入消息、输出消息、tool calls、finish reason 和 token usage。
4. `build_trace_tool_middleware()` 记录工具调用参数、返回结果、异常和耗时。
5. `TracedToolCallLimitMiddleware` 记录第三方工具调用限制 middleware 的执行阶段和耗时。
6. `writer.py` 将 run/span 操作放入队列，后台线程批量写入 MongoDB。
7. 服务关闭时，`lifecycle.py` flush 队列并停止 writer。

## Mongo 集合

Trace 数据固定写入两个集合：

- `agent_trace_runs`：一次完整助手调用的总览数据，例如会话、用户、入口、状态、开始结束时间、总耗时和 token 汇总。
- `agent_trace_spans`：一次调用中的具体执行步骤，例如 graph、node、middleware、model、tool 的输入输出、耗时、异常和属性。

集合名常量定义在 `app/core/database/mongodb/config.py`，不通过环境变量修改。

## 文件职责

### `__init__.py`

导出 tracing 包对外使用的主要入口，避免业务代码直接感知内部文件拆分。外部接入时优先从这里导入装饰器、middleware 和生命周期函数。

### `config.py`

读取 trace 相关环境变量，并提供默认配置。

主要配置包括：

- `AGENT_TRACE_ENABLED`：是否启用 trace 采集，默认启用。
- `AGENT_TRACE_QUEUE_MAX_SIZE`：异步队列最大长度。
- `AGENT_TRACE_BATCH_SIZE`：后台批量写入条数。
- `AGENT_TRACE_FLUSH_INTERVAL_MS`：后台定时 flush 间隔。
- `AGENT_TRACE_PAYLOAD_MAX_CHARS`：输入输出 payload 最大保留字符数。

### `context.py`

维护当前请求内的 trace 上下文。

主要职责：

- 使用 `ContextVar` 保存当前 `trace_id`、当前 `span_id` 和 span 栈。
- 管理 span 的父子关系。
- 生成同一个 trace 内单调递增的 `sequence`，用于前端按执行顺序展示 waterfall。
- 汇总模型 span 的 token usage，最终写回 run。

### `decorators.py`

提供 trace run 和普通 span 的核心封装能力。

主要职责：

- `start_trace_run(...)`：创建一次 trace run，并初始化 root graph span。
- `finish_trace_run(...)`：结束 run，写入最终状态、耗时、异常和 token 汇总。
- `AgentTraceSpan`：通用 span 上下文管理器，负责开始、结束、异常记录和异步投递。
- `agent_trace(...)`：节点装饰器，用于采集 agent node 的输入、输出、异常和耗时。

### `ids.py`

集中生成 trace 标识。

主要职责：

- 生成 `trace_id`。
- 生成 `span_id`。

单独拆分这个文件是为了让 ID 规则集中维护，后续如果要改成业务前缀、雪花 ID 或跨系统 ID，也只需要改这里。

### `lifecycle.py`

管理 trace writer 的服务生命周期。

主要职责：

- 服务启动时初始化 Mongo 索引并启动后台 writer。
- 服务关闭时 flush 队列并停止后台 writer。

这个文件由 `app/main.py` 调用，保证 trace 组件跟随 FastAPI 生命周期启动和关闭。

### `middleware.py`

提供和 LangGraph/LangChain agent 执行链路相关的 trace middleware。

主要职责：

- `TraceModelMiddleware`：采集模型调用 span，包括模型参数、可见工具、输入消息、输出消息、tool calls、finish reason 和 token
  usage。
- `build_trace_tool_middleware()`：构建工具调用 middleware，统一采集工具输入、输出、异常和耗时。
- `TracedToolCallLimitMiddleware`：包裹工具调用限制 middleware，采集 `after_model` 阶段的耗时。

业务侧不需要给每个工具单独加装饰器，工具 span 统一由 middleware 采集。

### `serializer.py`

负责把复杂 Python 对象转换成可安全写入 Mongo 的结构，并对超长 payload 做截断。

主要职责：

- 序列化 LangChain message、tool、Pydantic model、异常和普通 Python 对象。
- 清理不可 JSON 化的数据。
- 控制输入输出 payload 的最大长度，避免 trace 文档过大。

### `storage.py`

封装 MongoDB trace 集合的写入和索引初始化。

主要职责：

- 初始化 `agent_trace_runs` 和 `agent_trace_spans` 的查询索引。
- 插入 run。
- 更新 run 终态。
- 批量插入 span。

这里是 tracing 包唯一直接接触 Mongo 集合的文件。

### `token_usage.py`

从模型返回对象中提取 token 和工具调用信息。

主要职责：

- 提取 `input_tokens`、`output_tokens`、`total_tokens`。
- 提取 `finish_reason`。
- 提取模型返回中的 `tool_calls`。

不同模型供应商的返回结构可能不完全一致，后续供应商差异优先集中在这个文件维护。

### `writer.py`

提供非阻塞异步写入能力。

主要职责：

- 使用 `queue.Queue` 保存待写入的 trace 操作。
- 主线程只执行 `put_nowait()`，队列满时丢弃新事件并记录 warning。
- 后台线程按批量大小或 flush 间隔写入 MongoDB。
- Mongo 写入失败只记录日志，不把异常抛回主聊天链路。

这个文件保证 trace 不会因为 Mongo 慢、Mongo 不可用或队列积压而影响助手正常回答。

## 扩展注意事项

- 新增采集点时，优先复用 `AgentTraceSpan` 或 `agent_trace(...)`，不要在业务代码里直接拼 Mongo 文档。
- 新增模型供应商返回字段解析时，优先改 `token_usage.py`，不要把供应商差异散落在 middleware 里。
- 新增 payload 类型序列化时，优先改 `serializer.py`，保持写入 Mongo 的结构稳定。
- 新增 Mongo 字段时，同步更新 Java 端读取合同和 `AGENTS.md` 中的配置说明。
- 不要在主对话链路里等待 Mongo 写入结果，trace 写失败不能影响智能体主流程。
