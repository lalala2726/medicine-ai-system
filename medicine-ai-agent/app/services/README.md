# Services README

本目录存放应用层 Service，负责把「工作流、数据库、外部接口、消息落库、摘要、运行态」这些能力编排起来，对上层路由提供稳定入口。

## 使用约定

- `services/` 主要放应用层编排，不放纯工具函数。
- 纯消息工具、纯文本处理、纯格式转换，优先放到 `app/utils/`。
- 通用消息落库、消息快照、消息回调这类“非业务但和消息强相关”的逻辑，优先放到独立公共 service，例如
  `assistant_message_service.py`。
- `admin_*` / `client_*` 文件优先只关注各自端的业务差异，不要继续堆积可复用的基础设施逻辑。

## 文件说明

### `__init__.py`

- 包初始化文件。
- 当前不承载业务逻辑。

### `admin_assistant_service.py`

- 管理端 Assistant 主入口。
- 负责管理端会话创建、历史加载、后台 run 提交、attach 流式输出、停止输出、会话列表、标题修改、TTS 入口。
- 当前仍承载一部分 admin/client 共享的运行态能力，例如 `RUN_EVENT_STORE`、attach streaming 响应构造、后台 run 调度。

### `assistant_message_service.py`

- Assistant 消息公共 service。
- 负责用户消息持久化、AI 占位消息创建、流式快照落库、最终 AI 消息持久化、消息回调构造。
- 这类逻辑属于消息层能力，不应该散落在 `admin_assistant_service.py` / `client_assistant_service.py` 中。

### `auth_service.py`

- 认证上下文拉取与校验。
- 负责调用 `/agent/authorization`，把外部鉴权结果转换成内部 `AuthUser`。
- 统一处理认证失败、服务不可用、协议异常等场景。

### `client_assistant_service.py`

- 客户端 Assistant 主入口。
- 负责客户端会话创建、历史加载、后台 run 提交、attach 流式输出、停止输出、会话列表、标题修改、TTS 入口。
- 当前会处理客户端订单卡、售后卡 submit 的业务规则，并把最小号值文本喂给 workflow。
- 目前仍复用 `admin_assistant_service.py` 的部分共享运行态能力，后续适合继续下沉到公共 runtime 模块。

### `conversation_service.py`

- 会话持久化 service。
- 负责 admin/client 会话的查询、新增、列表、标题更新、软删除、存在性判断。
- 统一封装 conversations 集合的 MongoDB 读写。

### `document_chunk_service.py`

- 知识库文档切片与向量化 service。
- 负责文件下载、解析、切片、embedding、Milvus 入库、切片重建、切片追加。
- 也承接导入链路中的版本控制和消息队列日志。

### `image_parse_service.py`

- 图片结构化解析 service。
- 当前主要用于药品图片识别，把图片内容解析成结构化字段。
- 内部会调用图像理解模型并按 schema 校验返回结果。

### `knowledge_base_service.py`

- 知识库集合管理 service。
- 负责 Milvus collection 的创建、删除、加载、释放、文档删除、状态更新、切片查询。
- 更偏知识库资源层管理，不负责文档解析和切片生成。

### `memory_service.py`

- 会话记忆加载 service。
- 负责 window/summary 两种记忆模式的配置解析、消息提取、系统摘要注入、会话记忆组装。
- 供 admin/client Assistant workflow 在运行前加载上下文。

### `memory_summary_service.py`

- 会话摘要生成与刷新 service。
- 负责把可总结消息整理成摘要输入，调用摘要模型，控制 token 预算，并在达到阈值时刷新会话摘要。
- 更偏“摘要生成策略”和“摘要更新时机”。

### `message_service.py`

- 消息主表 service。
- 负责消息新增、更新、查询、计数、可总结消息筛选、卡片可见性处理等。
- 是所有 Assistant 消息读写的底层主入口。

### `message_tts_usage_service.py`

- 消息 TTS 使用记录 service。
- 负责记录消息转语音的调用情况、状态、提供方等统计信息。
- 更偏语音播放行为统计，而不是语音生成本身。

### `speech_stt_service.py`

- 实时语音转文本 service。
- 负责 websocket STT 会话生命周期，接收前端音频流并转发到 STT 供应商。
- 当前主要服务管理端实时语音识别。

### `summary_service.py`

- 会话摘要持久化 service。
- 负责 conversation summaries 集合的读写。
- 与 `memory_summary_service.py` 的区别是：这里管“摘要存哪里、怎么存”，那边管“摘要怎么生成、什么时候刷新”。

## 当前分层建议

- 会话层：`conversation_service.py`
- 消息层：`message_service.py`、`assistant_message_service.py`
- 记忆与摘要层：`memory_service.py`、`memory_summary_service.py`、`summary_service.py`
- Assistant 业务入口：`admin_assistant_service.py`、`client_assistant_service.py`
- 语音能力：`speech_stt_service.py`、`message_tts_usage_service.py`
- 知识库与切片：`knowledge_base_service.py`、`document_chunk_service.py`
- 认证与通用外部能力：`auth_service.py`、`image_parse_service.py`

## 维护建议

- 如果一个函数主要处理“消息格式转换、卡片提取、SSE notice 构造”，优先考虑放到 `app/utils/assistant_message_utils.py`。
- 如果一个函数主要处理“消息落库、消息快照、消息状态收尾、消息回调”，优先考虑放到 `assistant_message_service.py`。
- 如果一个函数明显只服务 admin 或 client 的业务规则，再放回各自的 `*_assistant_service.py`。
