# User Tools Reference

## 领域说明

用户领域工具用于查询用户列表、用户详情、钱包信息、钱包流水和消费信息。  
返回结构以 Python 工具实际返回的 `data` 为准，不包含 Java 原始响应外层的 `code/message/timestamp`。

## 工具清单

| 工具名                 | 适用场景              | 关键参数                                                                                    |
|---------------------|-------------------|-----------------------------------------------------------------------------------------|
| `user_list`         | 按用户名、昵称、角色、状态筛选用户 | `page_num`、`page_size`、`id`、`username`、`nickname`、`avatar`、`roles`、`status`、`create_by` |
| `user_context`      | 查用户整体情况、钱包状态、账号异常 | `user_ids: list[int]`，最多 20 个                                                           |
| `user_detail`       | 查看用户综合详情          | `user_ids: list[int]`                                                                   |
| `user_wallet`       | 查看钱包余额、累计收支、冻结信息  | `user_ids: list[int]`                                                                   |
| `user_wallet_flow`  | 查看钱包流水分页明细        | `user_id`、`page_num`、`page_size`                                                        |
| `user_consume_info` | 查看消费分页明细          | `user_id`、`page_num`、`page_size`                                                        |

## 优先调用策略

- 用户问“这个用户整体情况”“钱包状态”“账号是否异常”“能不能操作钱包”，优先调用 `user_context`。
- 多个用户 ID 必须一次性放进 `user_ids` 调用 `user_context`，不要逐个调用。
- 只有用户明确要求完整用户资料、钱包详情、钱包流水或消费分页时，才调用 `user_detail`、`user_wallet`、`user_wallet_flow`、
  `user_consume_info`。
- 如果用户只给昵称、用户名或手机号等模糊条件，先用 `user_list` 定位用户 ID，再调用 `user_context`。

## user_list

### 实际返回结构

分页对象：

- `total`
- `pageNum`
- `pageSize`
- `rows`

`rows[]` 中每项通常包含：

- `id`
- `username`
- `nickname`
- `avatar`
- `roles`
- `status`
- `createTime`

### 关键字段解释

- `roles` 是角色标识文本。
- `status` 是用户状态编码字段。
- 列表适合定位用户，不包含钱包余额、安全信息等深层详情。

### 常见可回答问题

- 某昵称或用户名对应哪些用户
- 某状态下有哪些用户
- 某个角色下有哪些用户

## user_detail

### 实际返回结构

返回用户详情数组 `list[object]`，每项顶层常见字段：

- `avatar`
- `nickName`
- `walletBalance`
- `totalOrders`
- `totalConsume`
- `basicInfo`
- `securityInfo`

`basicInfo` 常见字段：

- `userId`
- `realName`
- `phoneNumber`
- `email`
- `gender`
- `idCard`

`securityInfo` 常见字段：

- `registerTime`
- `lastLoginTime`
- `lastLoginIp`
- `status`

### 关键字段解释

- 用户详情支持一次传多个用户 ID，必须使用 `user_ids` 数组。
- `walletBalance`、`totalOrders`、`totalConsume` 是顶层摘要指标。
- `basicInfo` 提供用户身份资料。
- `securityInfo` 提供注册、登录和状态信息。
- `gender`、`status` 是编码字段。

### 常见可回答问题

- 某用户最近账号状态如何
- 某用户累计消费和订单数是多少
- 某用户的基础身份资料和安全信息是什么

## user_context

### 实际返回结构

返回按用户 ID 分组的对象，结构为 `dict[str, object]`。每个用户 ID 对应一个上下文对象，常见字段：

- `userId`
- `basicSummary`
- `orderSummary`
- `walletSummary`
- `riskSummary`
- `aiHints`

`basicSummary` 常见字段：

- `nickName`
- `phoneNumber`
- `status`
- `registerTime`
- `lastLoginTime`

`orderSummary` 常见字段：

- `totalOrders`
- `totalConsume`
- `recentOrderOverview`

`walletSummary` 常见字段：

- `balance`
- `status`
- `freezeReason`
- `freezeTime`
- `totalIncome`
- `totalExpend`

`riskSummary` 常见字段：

- `accountDisabled`
- `walletFrozen`
- `missingBasicProfile`

`aiHints` 常见字段：

- `canQueryOrders`
- `canOperateWallet`
- `walletFrozen`
- `accountDisabled`

### 关键字段解释

- 外层 key 是用户 ID。
- 回答时先看 `aiHints` 和 `riskSummary` 给结论，再引用 `walletSummary`、`orderSummary` 和登录状态。
- `user_context` 不默认查询钱包流水或消费分页；用户明确要求明细时再调用细工具。
- `recentOrderOverview` 是摘要字段，不等同消费分页明细。

### 常见可回答问题

- 用户账号是否禁用、钱包是否冻结
- 用户余额、累计收入支出和总消费情况
- 多个用户谁存在账号或钱包风险

## user_wallet

### 实际返回结构

返回钱包对象数组 `list[object]`，每项常见字段：

- `userId`
- `walletNo`
- `balance`
- `totalIncome`
- `totalExpend`
- `currency`
- `status`
- `freezeReason`
- `freezeTime`
- `updatedAt`

### 关键字段解释

- 用户钱包支持一次传多个用户 ID，必须使用 `user_ids` 数组。
- `balance`：可用余额。
- `totalIncome` / `totalExpend`：累计入账和累计支出。
- `status`：钱包状态编码。
- `freezeReason`、`freezeTime`：冻结原因和冻结时间。

### 常见可回答问题

- 用户当前钱包余额是多少
- 钱包是否被冻结
- 累计收入和支出分别是多少

## user_wallet_flow

### 实际返回结构

分页对象：

- `total`
- `pageNum`
- `pageSize`
- `rows`

`rows[]` 中每项通常包含：

- `index`
- `changeType`
- `amount`
- `amountDirection`
- `isIncome`
- `beforeBalance`
- `afterBalance`
- `changeTime`

### 关键字段解释

- `changeType`：面向业务的变动类型说明。
- `amountDirection`：金额方向编码。
- `isIncome`：是否收入。
- `beforeBalance` / `afterBalance`：变动前后余额。

### 常见可回答问题

- 最近几笔钱包变动是什么
- 某笔流水是收入还是支出
- 钱包余额是在什么时间发生变化的

## user_consume_info

### 实际返回结构

分页对象：

- `total`
- `pageNum`
- `pageSize`
- `rows`

`rows[]` 中每项通常包含：

- `index`
- `userId`
- `orderNo`
- `totalPrice`
- `payPrice`
- `finishTime`

### 关键字段解释

- `totalPrice`：商品总价。
- `payPrice`：实付金额。
- `finishTime`：订单完成时间，这里的完成是用户确认收货时间。

### 常见可回答问题

- 某用户最近消费了哪些订单
- 某用户某段时间内的实付金额情况
- 某用户最近完成收货的订单有哪些
