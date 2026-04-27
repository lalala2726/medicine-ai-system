# Analytics Tools Reference

## 领域说明

运营分析领域工具用于查询实时看板、汇总指标、分布、排行和趋势数据。  
返回结构以 Python 工具实际返回的 `data` 为准，不包含 Java 原始响应外层的 `code/message/timestamp`。  
其中趋势、分布、排行结果通常很适合继续交给 `chart` skill 做图表。

## 工具清单

| 工具名                                        | 适用场景         | 关键参数           |
|--------------------------------------------|--------------|----------------|
| `analytics_realtime_overview`              | 查看实时运营看板     | 无              |
| `analytics_range_summary`                  | 查看经营结果汇总     | `days`         |
| `analytics_conversion_summary`             | 查看支付转化汇总     | `days`         |
| `analytics_fulfillment_summary`            | 查看履约时效汇总     | `days`         |
| `analytics_after_sale_efficiency_summary`  | 查看售后处理时效汇总   | `days`         |
| `analytics_after_sale_status_distribution` | 查看售后状态分布     | `days`         |
| `analytics_after_sale_reason_distribution` | 查看售后原因分布     | `days`         |
| `analytics_top_selling_products`           | 查看热销商品排行     | `days`、`limit` |
| `analytics_return_refund_risk_products`    | 查看退货退款风险商品排行 | `days`、`limit` |
| `analytics_sales_trend`                    | 查看成交趋势       | `days`         |
| `analytics_after_sale_trend`               | 查看售后趋势       | `days`         |

## 汇总类工具

### analytics_realtime_overview

返回单个对象，常见字段：

- `cumulativePaidAmount`
- `cumulativePaidOrderCount`
- `todayPaidAmount`
- `todayPaidOrderCount`
- `pendingShipmentOrderCount`
- `pendingReceiptOrderCount`
- `pendingAfterSaleCount`
- `processingAfterSaleCount`

适合回答：

- 当前实时成交情况
- 当前待发货、待收货、待处理售后数量

图表建议：

- 更适合做指标卡，不一定需要图表。

### analytics_range_summary

返回单个对象，常见字段：

- `paidAmount`
- `paidOrderCount`
- `averageOrderAmount`
- `refundAmount`
- `netPaidAmount`
- `refundRate`
- `afterSaleApplyCount`
- `returnRefundQuantity`

适合回答：

- 最近若干天经营结果如何
- 成交额、退款额、净成交额、退款率是多少

图表建议：

- 适合指标卡、分组柱状图。

### analytics_conversion_summary

返回单个对象，常见字段：

- `createdOrderCount`
- `paidOrderCount`
- `pendingPaymentOrderCount`
- `closedOrderCount`
- `paymentConversionRate`

适合回答：

- 下单到支付的转化情况
- 待支付和关闭订单规模

图表建议：

- 适合漏斗图、指标卡、柱状图。

### analytics_fulfillment_summary

返回单个对象，常见字段：

- `averageShipmentHours`
- `averageReceiptHours`
- `overdueShipmentOrderCount`
- `overdueReceiptOrderCount`

适合回答：

- 发货和收货时效表现
- 超时未发货、超时未收货订单规模

图表建议：

- 适合指标卡、柱状图。

### analytics_after_sale_efficiency_summary

返回单个对象，常见字段：

- `averageAuditHours`
- `averageCompleteHours`
- `overdueAuditCount`
- `overdueCompleteCount`

适合回答：

- 售后审核和完结时效
- 超时未审核、超时未完结规模

图表建议：

- 适合指标卡、柱状图。

## 分布类工具

### analytics_after_sale_status_distribution

返回数组 `list[object]`，每项通常包含：

- `status`
- `statusName`
- `count`

适合回答：

- 不同售后状态的数量分布

图表建议：

- 适合饼图、条形图、柱状图。

### analytics_after_sale_reason_distribution

返回数组 `list[object]`，每项通常包含：

- `reason`
- `reasonName`
- `count`

适合回答：

- 售后主要原因有哪些
- 各原因占比和数量如何

图表建议：

- 适合饼图、条形图、柱状图。

## 排行类工具

### analytics_top_selling_products

返回数组 `list[object]`，每项通常包含：

- `productId`
- `productName`
- `productImage`
- `soldQuantity`
- `paidAmount`

适合回答：

- 哪些商品卖得最好
- 热销商品的销量和成交金额

图表建议：

- 适合条形图、柱状图。

### analytics_return_refund_risk_products

返回数组 `list[object]`，每项通常包含：

- `productId`
- `productName`
- `productImage`
- `soldQuantity`
- `returnRefundQuantity`
- `returnRefundRate`
- `refundAmount`

适合回答：

- 哪些商品退款风险高
- 哪些商品退款率高、退款金额高

图表建议：

- 适合条形图、柱状图、散点图。

## 趋势类工具

### analytics_sales_trend

返回单个趋势对象，常见字段：

- `days`
- `granularity`
- `points`

`points[]` 中每项通常包含：

- `label`
- `paidOrderCount`
- `paidAmount`

适合回答：

- 最近若干天成交金额趋势
- 最近若干天支付订单数趋势

图表建议：

- 强烈适合折线图、面积图、双轴图。

### analytics_after_sale_trend

返回单个趋势对象，常见字段：

- `days`
- `granularity`
- `points`

`points[]` 中每项通常包含：

- `label`
- `refundAmount`
- `afterSaleApplyCount`

适合回答：

- 最近若干天退款金额趋势
- 最近若干天售后申请数趋势

图表建议：

- 强烈适合折线图、面积图、双轴图。

## 使用建议

1. 如果只需要几个核心指标，优先用汇总类工具。
2. 如果要看结构占比，用分布类工具。
3. 如果要看表现最强或风险最高的商品，用排行类工具。
4. 如果要做图表或看时间变化，用趋势类工具，并继续结合 `chart` skill 输出图表配置。
