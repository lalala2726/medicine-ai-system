# RAG Query 模块说明

## 模块目标

`app/rag/query` 负责“知识库查询”这条完整链路，目标是把查询过程拆成稳定、清晰、低耦合的职责层，方便后续维护、排错和扩展。

当前模块只负责：

- 查询入口编排
- 运行时配置解析
- 问题改写
- 向量召回
- 重排
- 结果格式化

当前模块不负责：

- Agent 工具封装
- 知识库导入、切片、写库
- 后台配置合法性校验逻辑

## 文件职责图

- `__init__.py`
    - 包级导出入口。
- `constants.py`
    - 静态常量定义。
- `types.py`
    - 查询命中对象与运行时配置对象。
- `runtime.py`
    - `top_k` 规范化与运行时配置解析。
- `rewrite.py`
    - 检索问题改写。
- `clients.py`
    - embedding client、Milvus client 与查询配置日志。
- `retriever.py`
    - 向量召回、候选池计算、命中结构转换、去重排序。
- `ranking.py`
    - 重排请求与结果解析。
- `formatter.py`
    - 查询结果格式化输出。
- `service.py`
    - 查询编排层，对外提供查询入口。
- `utils.py`
    - 纯工具函数。

## 查询执行流程

1. `service.py` 接收查询请求。
2. `runtime.py` 解析知识库作用域下的运行时配置。
3. `rewrite.py` 在需要时先把原始问题改写成更适合向量召回的问题。
4. `retriever.py` 使用 embedding + Milvus 完成多知识库召回。
5. 如果开启重排，则由 `ranking.py` 调用重排模型完成精排。
6. `formatter.py` 把最终命中结果渲染成可直接给 Agent 使用的文本。

## raw / rewritten 两条入口区别

- `query_knowledge_by_raw_question`
    - 原始问题做向量召回。
    - 原始问题做重排。

- `query_knowledge_by_rewritten_question`
    - 改写后的问题做向量召回。
    - 原始规范化问题做重排。

这样做的目的是让召回更偏向检索表达，而重排仍然对齐用户原始语义。

## 重排限制

当前重排能力只支持：

- `qwen3-rerank`

当前不支持：

- 其他历史文本重排模型
- 其他历史多模态重排模型

如果 `rankingEnabled=true` 但 `rankingModel` 不是 `qwen3-rerank`，查询链路会直接报错，不做兼容、不做降级。

## 当前业务调用入口

业务代码应优先使用包级导出，而不是直接依赖内部文件：

- `app.rag.query.query_knowledge_by_raw_question`
- `app.rag.query.query_knowledge_by_rewritten_question`
- `app.rag.query.format_knowledge_search_hits`
- `app.rag.query.KnowledgeSearchHit`

当前 Agent 工具层主要通过：

- `app.agent.tools.rag_query`

间接调用本模块。
