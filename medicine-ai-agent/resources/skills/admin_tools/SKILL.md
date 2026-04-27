---
name: admin_tools
description: 管理端单 Agent 工具总目录，提供通用返回规则、领域入口与子技能资源索引。
---

# Admin Tools Skill

## 使用原则

1. 默认只有基础工具可直接调用；订单、商品、售后、用户、分析等业务工具都需要先通过 `load_tools` 加载。
2. `load_tools` 是加载步骤，不是说明性工具，也不是向用户申请；加载时必须传精确的 `tool_keys`，不要传模糊领域名。
3. `load_tools` 支持一次同时加载多个工具；如果一个问题要查订单和用户，就把多个工具名一起放进 `tool_keys`。
4. 如果你不确定精确工具名，先调用 `list_loadable_tools` 拿完整工具目录，再调用 `load_tools`。
5. 加载工具是你自己的内部步骤，不需要等待用户确认；只要任务需要，就直接自行调用。
6. 工具名统一使用精确的 `snake_case`，不要自造工具名。
7. 主 skill 只负责告诉你“有哪些工具、每个领域大概能拿到什么”；需要字段级返回结构时，继续读取 `references/` 下面对应领域说明文件。
8. 回答制度、字段定义、规则说明、配置说明等知识型问题时，优先调用 `search_knowledge_context`，不要把业务工具当知识库。
9. 订单上下文和订单详情统一传 `order_nos` 数组；售后详情统一传 `after_sale_nos` 数组；用户详情和用户钱包统一传 `user_ids`
   数组。
10. 订单、售后、用户查询默认先用 `order_context`、`after_sale_context`、`user_context`；订单收货地址、完整时间线、发货物流信息都在
    `order_context` 中，不再存在单独订单时间线或发货记录工具。

## 通用返回规则

1. 管理端 Python 工具最终返回给模型的是业务 `data`，不是 Java 原始 `AjaxResult` 整包，所以不要期待 `code`、`message`、
   `timestamp` 这类外层字段。
2. 分页类工具通常返回：
   - `total`：总记录数
   - `pageNum`：当前页码
   - `pageSize`：每页条数
   - `rows`：当前页数据数组
3. 详情类工具通常返回对象或对象数组，不带分页壳。
4. 趋势类工具通常返回：
   - `days`：统计天数
   - `granularity`：时间粒度
   - `points`：趋势点数组
5. 某些字段是业务编码值；如果返回结构里没有对应的 `xxxName` 字段，就把它当作编码本身使用，不要擅自推断完整字典。

## 基础工具

| 工具名                        | 能拿到什么             | 何时使用           |
|----------------------------|-------------------|----------------|
| `list_loadable_tools`      | 可加载业务工具完整目录       | 不确定精确工具名时      |
| `load_tools`               | 业务工具加载结果          | 需要调用未暴露业务工具时   |
| `search_knowledge_context` | 知识库检索片段           | 制度、规则、FAQ、字段解释 |
| `get_safe_user_info`       | 当前登录人的用户名、昵称等基础信息 | 需要当前操作人上下文时    |

## 领域入口

### 订单工具

工具：

- `order_list`
- `order_context`
- `order_detail`

能拿到什么：

- 订单分页列表、订单编号、金额、支付方式、订单状态、时间
- 订单聚合上下文、完整收货地址、完整时间线、发货物流完整信息、金额摘要、商品摘要、AI 决策提示
- 订单商品明细、支付信息

完整字段说明：

- [references/ORDER.md](references/ORDER.md)

### 商品工具

工具：

- `product_list`
- `product_detail`
- `drug_detail`

能拿到什么：

- 商品分页列表、分类、价格、库存、销量、状态、展示图
- 商品详情、图片列表
- 药品说明书、适应症、用法用量、不良反应、禁忌等说明书字段

完整字段说明：

- [references/PRODUCT.md](references/PRODUCT.md)

### 售后工具

工具：

- `after_sale_list`
- `after_sale_context`
- `after_sale_detail`

能拿到什么：

- 售后分页列表、售后单号、订单号、用户、商品、退款金额、申请原因、状态
- 售后聚合上下文、商品摘要、凭证数量、最近 5 条处理节点、AI 决策提示
- 售后详情、凭证图片、管理员备注、商品信息、处理时间线

完整字段说明：

- [references/AFTER_SALE.md](references/AFTER_SALE.md)

### 用户工具

工具：

- `user_list`
- `user_context`
- `user_detail`
- `user_wallet`
- `user_wallet_flow`
- `user_consume_info`

能拿到什么：

- 用户分页列表、角色、状态、创建时间
- 用户聚合上下文、基础摘要、订单摘要、钱包摘要、风险摘要、AI 决策提示
- 用户详情、基础资料、安全信息、钱包余额、总订单数、总消费金额
- 钱包余额、累计收支、冻结信息、流水、消费记录

完整字段说明：

- [references/USER.md](references/USER.md)

### 运营分析工具

工具：

- `analytics_realtime_overview`
- `analytics_range_summary`
- `analytics_conversion_summary`
- `analytics_fulfillment_summary`
- `analytics_after_sale_efficiency_summary`
- `analytics_after_sale_status_distribution`
- `analytics_after_sale_reason_distribution`
- `analytics_top_selling_products`
- `analytics_return_refund_risk_products`
- `analytics_sales_trend`
- `analytics_after_sale_trend`

能拿到什么：

- 实时运营看板指标
- 经营、转化、履约、售后时效等汇总指标
- 售后状态/原因分布
- 热销商品排行、退款风险商品排行
- 成交趋势、售后趋势，适合配合 `chart` skill 做图表

完整字段说明：

- [references/ANALYTICS.md](references/ANALYTICS.md)

## 使用建议

1. 先用本主 skill 找到正确领域和工具名。
2. 如果你不确定精确工具名，先调用 `list_loadable_tools` 获取完整工具目录。
3. 确认工具名后，如果当前工具列表里看不到该业务工具，先调用 `load_tools` 加载该工具；需要多个工具时，一次传多个 `tool_keys`。
4. 明确订单号、售后单号或用户 ID 时，先加载并调用对应 context 工具；多个主键必须一次性批量传入。
5. 如果只需要知道“这个工具能查什么”，主 skill 已经足够。
6. 如果要精确理解返回字段、嵌套对象、分页结构或趋势点结构，再读取 `references/` 下对应领域文件。
7. 如果要做图表，优先读取 [references/ANALYTICS.md](references/ANALYTICS.md) 里的“适合图表”说明，再结合 `chart` skill
   输出图表方案。
