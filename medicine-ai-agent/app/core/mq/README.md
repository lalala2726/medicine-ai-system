# MQ 模块（`app/core/mq`）

基于 **FastStream\[rabbit\]** 的 RabbitMQ 消息队列模块，负责知识库异步任务的消费与结果回调。

## 目录结构

```
app/core/mq/
├── broker.py           # Broker 单例 & 连接配置
├── topology.py         # Exchange / Queue / RoutingKey 拓扑定义
├── publishers.py       # 结果消息发布函数
├── version_store.py    # Redis 版本检查（过期消息判定）
├── log.py              # 统一结构化日志 & 阶段枚举
├── models/
│   ├── stages.py           # 结果事件阶段枚举
│   ├── import_msgs.py      # 导入命令 / 结果消息模型
│   ├── chunk_rebuild_msgs.py  # 切片重建命令 / 结果消息模型
│   └── chunk_add_msgs.py     # 手工新增切片命令 / 结果消息模型
├── handlers/
│   ├── import_handler.py       # 导入命令消费者
│   ├── chunk_rebuild_handler.py # 切片重建命令消费者
│   └── chunk_add_handler.py     # 手工新增切片命令消费者
└── __init__.py         # 模块聚合导出
```

## 快速接入

### 1. 环境变量

在 `.env` 中配置以下变量即可启用 MQ：

```dotenv
RABBITMQ_HOST=192.168.10.110
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_VHOST=/
```

> `RABBITMQ_HOST` 存在时模块自动启用，不设置则跳过 MQ 初始化。

### 2. 生命周期

`app/main.py` 的 lifespan 中自动管理：

```python
from app.core.mq import get_broker, is_mq_configured

if is_mq_configured():
    import app.core.mq.handlers  # 触发 subscriber 注册
    broker = get_broker()
    await broker.start()
    # ... yield ...
    await broker.close()
```

## 三条链路

| 链路     | Exchange                  | Command Queue                       | Result RoutingKey                |
|--------|---------------------------|-------------------------------------|----------------------------------|
| 知识库导入  | `knowledge.import`        | `knowledge.import.command.q`        | `knowledge.import.result`        |
| 切片重建   | `knowledge.chunk_rebuild` | `knowledge.chunk_rebuild.command.q` | `knowledge.chunk_rebuild.result` |
| 手工新增切片 | `knowledge.chunk_add`     | `knowledge.chunk_add.command.q`     | `knowledge.chunk_add.result`     |

所有 Exchange 均为 **DIRECT** + **durable**，Queue 均为 **durable**。

## ACK 策略

- **导入**：始终 ACK——handler 内部 try/except 兜底，不重投。
- **切片重建 / 新增**：`REJECT_ON_ERROR`——结果消息投递失败时 raise，触发 NACK 重投。

## 版本检查

通过 Redis 实现"最新版本"语义，丢弃过期消息：

- 导入链路：key = `kb:latest:{biz_key}`
- 切片重建：key = `kb:chunk_edit:latest_version:{vector_id}`

版本号由业务服务写入 Redis，本模块只读。

## 日志

统一使用 `mq_log(pipeline, stage, task_uuid, **metrics)` 输出结构化日志：

```
[import] [task_uuid=abc123] [download_done] filename=a.pdf size=10240
```

日志级别自动映射：`FAILED` → error，`STALE` → warning，其余 → info。

## 新增链路

1. 在 `topology.py` 添加 Exchange / Queue / RoutingKey
2. 在 `models/` 新建 command + result 消息模型
3. 在 `handlers/` 新建 handler 并用 `@broker.subscriber(...)` 装饰
4. 在 `handlers/__init__.py` 导入新 handler
5. 在 `publishers.py` 添加发布函数
6. 如需版本检查，在 `version_store.py` 添加对应方法
