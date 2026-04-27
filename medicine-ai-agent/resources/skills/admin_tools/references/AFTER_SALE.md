# After Sale Tools Reference

## 领域说明

售后领域工具用于查询售后申请列表和售后详情。  
返回结构以 Python 工具实际返回的 `data` 为准，不包含 Java 原始响应外层的 `code/message/timestamp`。

## 工具清单

| 工具名                  | 适用场景                   | 关键参数                                                                                             |
|----------------------|------------------------|--------------------------------------------------------------------------------------------------|
| `after_sale_list`    | 查询待处理售后、定位某订单或某用户的售后范围 | `page_num`、`page_size`、`after_sale_type`、`after_sale_status`、`order_no`、`user_id`、`apply_reason` |
| `after_sale_context` | 查售后进度、拒绝原因、下一步动作       | `after_sale_nos: list[str]`，最多 20 个                                                              |
| `after_sale_detail`  | 查看售后申请完整详情             | `after_sale_nos: list[str]`                                                                      |

## 优先调用策略

- 用户问“售后进度”“为什么拒绝”“下一步怎么处理”“售后当前状态”，优先调用 `after_sale_context`。
- 多个售后单号必须一次性放进 `after_sale_nos` 调用 `after_sale_context`，不要逐个调用。
- 只有用户明确要求完整售后详情、全部凭证图片或完整处理时间线时，才调用 `after_sale_detail`。
- 如果用户只给订单号或用户条件、没有售后单号，先用 `after_sale_list` 定位售后单号，再调用 `after_sale_context`。

## after_sale_list

### 实际返回结构

分页对象：

- `total`
- `pageNum`
- `pageSize`
- `rows`

`rows[]` 中每项通常包含：

- `id`
- `afterSaleNo`
- `orderNo`
- `userId`
- `userNickname`
- `productName`
- `productImage`
- `afterSaleType`
- `afterSaleStatus`
- `refundAmount`
- `applyReason`
- `applyTime`
- `auditTime`

### 关键字段解释

- `afterSaleType`、`afterSaleStatus`、`applyReason` 是业务编码字段。
- 列表页适合快速定位售后单，不包含完整时间线和管理员备注。

### 常见可回答问题

- 当前有哪些待审核或处理中售后
- 某个订单是否有售后申请
- 某个用户近期有哪些售后单

## after_sale_detail

### 实际返回结构

返回售后详情数组 `list[object]`，每项常见字段：

- `id`
- `afterSaleNo`
- `orderNo`
- `orderItemId`
- `userId`
- `userNickname`
- `afterSaleType`
- `afterSaleTypeName`
- `afterSaleStatus`
- `afterSaleStatusName`
- `refundAmount`
- `applyReason`
- `applyReasonName`
- `applyDescription`
- `evidenceImages`
- `receiveStatus`
- `receiveStatusName`
- `rejectReason`
- `adminRemark`
- `applyTime`
- `auditTime`
- `completeTime`
- `productInfo`
- `timeline`

`productInfo` 常见字段：

- `productId`
- `productName`
- `productImage`
- `productPrice`
- `quantity`
- `totalPrice`

`timeline[]` 中每项常见字段：

- `id`
- `eventType`
- `eventTypeName`
- `eventStatus`
- `operatorType`
- `operatorTypeName`
- `description`
- `createTime`

### 关键字段解释

- 售后详情统一按售后单号查询，禁止传售后申请 ID。
- `afterSaleTypeName`、`afterSaleStatusName`、`applyReasonName`、`receiveStatusName` 是更适合直接面向用户说明的中文名称。
- `evidenceImages` 是凭证图片数组。
- `rejectReason`、`adminRemark` 适合说明后台处理结论和原因。
- `timeline` 适合整理成售后处理流程。

### 常见可回答问题

- 某个售后单当前是什么状态
- 用户为什么申请售后、提交了哪些凭证
- 后台为什么拒绝或通过该售后
- 这个售后处理到哪一步了

## after_sale_context

### 实际返回结构

返回按售后单号分组的对象，结构为 `dict[str, object]`。每个售后单号对应一个上下文对象，常见字段：

- `afterSaleNo`
- `orderNo`
- `statusCode`
- `statusText`
- `typeCode`
- `typeText`
- `refundAmount`
- `reasonText`
- `productSummary`
- `evidenceSummary`
- `timelineSummary`
- `aiHints`

`productSummary` 常见字段：

- `productId`
- `productName`
- `productImage`
- `quantity`
- `totalPrice`

`evidenceSummary` 常见字段：

- `evidenceCount`
- `firstEvidenceImage`

`timelineSummary[]` 最多 5 条，常见字段：

- `eventType`
- `eventTypeName`
- `eventStatus`
- `operatorType`
- `operatorTypeName`
- `description`
- `eventTime`

`aiHints` 常见字段：

- `waitingAudit`
- `processing`
- `completed`
- `rejected`
- `canCancel`
- `canReapply`

### 关键字段解释

- 外层 key 是售后单号，不是售后申请 ID。
- 回答时先看 `aiHints` 给处理结论，再引用 `statusText`、`reasonText`、`refundAmount`、最近处理节点。
- `evidenceSummary` 只返回凭证数量和首图，不默认展开全部凭证。
- `timelineSummary` 只保留最近 5 条处理节点；完整时间线必须调用 `after_sale_detail`。

### 常见可回答问题

- 售后现在是否待审核、处理中、已完成或已拒绝
- 售后为什么被拒绝或下一步该怎么处理
- 多个售后单分别处于什么进度
