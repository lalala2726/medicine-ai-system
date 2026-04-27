# Order Tools Reference

## 领域说明

订单领域工具用于查询订单列表、订单聚合上下文和必要的订单明细。
返回结构以 Python 工具实际返回的 `data` 为准，不包含 Java 原始响应外层的 `code/message/timestamp`。

## 工具清单

| 工具名             | 适用场景                               | 关键参数                                                                                                         |
|-----------------|------------------------------------|--------------------------------------------------------------------------------------------------------------|
| `order_list`    | 查订单范围、筛选订单、做订单汇总前取样                | `page_num`、`page_size`、`order_no`、`pay_type`、`order_status`、`delivery_type`、`receiver_name`、`receiver_phone` |
| `order_context` | 查订单当前情况、收货地址、完整时间线、发货物流、原因判断、下一步动作 | `order_nos: list[str]`，最多 20 个                                                                               |
| `order_detail`  | 明确要完整商品明细或支付明细时使用                  | `order_nos: list[str]`                                                                                       |

## 优先调用策略

- 用户问“订单现在什么情况”“帮我看这些订单”“为什么没发货”“物流到哪”“收货地址是什么”“能不能退款”“下一步怎么处理”，优先调用
  `order_context`。
- 多个订单号必须一次性放进 `order_nos` 调用 `order_context`，不要逐个调用。
- 订单完整时间线、完整收货地址、发货物流信息已经包含在 `order_context`，不要寻找单独时间线或发货记录工具。
- 只有用户明确要求完整商品明细或支付明细时，才额外调用 `order_detail`。
- 如果用户只给模糊条件、没有订单号，先用 `order_list` 定位订单号，再调用 `order_context`。

## order_list

### 实际返回结构

分页对象：

- `total`
- `pageNum`
- `pageSize`
- `rows`

`rows[]` 中每一项是订单列表对象，通常包含：

- `id`：订单 ID
- `orderNo`：订单编号
- `totalAmount`：订单总金额
- `payType`：支付方式编码
- `orderStatus`：订单状态编码
- `payTime`：支付时间
- `createTime`：创建时间
- `productInfo`：首个商品信息对象

`productInfo` 常见字段：

- `productName`：商品名称
- `productImage`：商品图片
- `productPrice`：商品价格
- `productCategory`：商品分类
- `productId`：商品 ID
- `quantity`：商品数量

### 关键字段解释

- 这是订单分页列表，不是完整详情。
- `productInfo` 是订单列表里携带的首个商品摘要，适合快速浏览，不等于完整商品明细数组。
- `payType`、`orderStatus` 是业务编码字段。

### 常见可回答问题

- 某个状态下有多少订单
- 某个收货人或订单号相关的订单有哪些
- 某段时间最近创建或支付的订单概览

## order_detail

### 实际返回结构

返回订单详情数组 `list[object]`，每项通常包含：

- `userInfo`
- `deliveryInfo`
- `orderInfo`
- `productInfo`

`userInfo` 常见字段：

- `userId`
- `nickname`
- `phoneNumber`

`deliveryInfo` 常见字段：

- `receiverName`
- `receiverAddress`
- `receiverPhone`
- `deliveryMethod`

`orderInfo` 常见字段：

- `orderNo`
- `orderStatus`
- `payType`
- `totalAmount`
- `payAmount`
- `freightAmount`

`productInfo[]` 中每项常见字段：

- `productId`
- `productName`
- `productImage`
- `productPrice`
- `productQuantity`
- `productTotalAmount`

### 关键字段解释

- 订单详情统一按订单编号查询，禁止传订单 ID。
- 这是完整订单详情数组，即使只查一个订单，也要按数组理解。
- `productInfo` 是商品明细数组，不是订单列表里的单个摘要对象。
- `orderStatus`、`payType` 仍然是编码字段。
- 订单状态判断、收货地址、时间线、发货物流信息优先看 `order_context`。

### 常见可回答问题

- 某订单买了哪些商品、数量多少、金额多少
- 某订单实际支付金额和运费是多少

## order_context

### 实际返回结构

返回按订单编号分组的对象，结构为 `dict[str, object]`。每个订单编号对应一个上下文对象，常见字段：

- `orderNo`
- `statusCode`
- `statusText`
- `payType`
- `amountSummary`
- `receiverSummary`
- `productSummary`
- `shippingSummary`
- `timeline`
- `aiHints`

`amountSummary` 常见字段：

- `totalAmount`
- `payAmount`
- `freightAmount`

`receiverSummary` 常见字段：

- `receiverName`
- `receiverPhone`
- `deliveryMethod`
- `receiverAddress`

`productSummary` 常见字段：

- `productCount`
- `productLineCount`
- `topProductNames`
- `hasAfterSale`

`shippingSummary` 常见字段：

- `shipped`
- `logisticsCompany`
- `trackingNumber`
- `shipmentNote`
- `statusCode`
- `statusText`
- `deliverTime`
- `receiveTime`
- `nodes`

`shippingSummary.nodes[]` 常见字段：

- `time`
- `content`
- `location`

`timeline[]` 常见字段：

- `id`
- `eventType`
- `eventStatus`
- `operatorType`
- `description`
- `eventTime`

`aiHints` 常见字段：

- `canCancel`
- `canApplyAfterSale`
- `needsPayment`
- `needsShipment`
- `needsReceipt`
- `hasAfterSale`

### 关键字段解释

- 外层 key 是订单编号，不是订单 ID。
- 回答时先看 `aiHints` 给结论，再引用 `statusText`、`receiverSummary`、`shippingSummary`、`timeline` 和金额摘要。
- `timeline` 是完整订单时间线。
- `shippingSummary.nodes` 是完整物流轨迹节点数组。
- 收货地址直接使用 `receiverSummary.receiverAddress`，不要再调用订单详情只为获取地址。

### 常见可回答问题

- 订单当前该付款、发货、收货还是已完成
- 订单为什么没发货或为什么还不能退款
- 多个订单现在分别卡在哪一步
- 某订单的收货地址和联系人是谁
- 订单是否已发货、何时发货、当前物流走到了哪里
