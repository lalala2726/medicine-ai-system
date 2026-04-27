# 折线图（Line）

用于展示数值随连续时间间隔或有序类别的变化趋势。适合时间序列数据，支持多系列对比（通过 `group` 字段）。

## 字段说明

| 字段             | 必填 | 类型     | 说明                                   |
|----------------|:--:|--------|--------------------------------------|
| `data`         |    | 数组对象   | 图表数据                                 |
| `data[].time`  |    | 文本/数值  | 时序名称（X轴）                             |
| `data[].value` |    | 数值     | 数值（Y轴）                               |
| `data[].group` |    | 文本     | 分组名称，用于多系列对比                         |
| `axisXTitle`   |    | 文本     | X轴标题                                 |
| `axisYTitle`   |    | 文本     | Y轴标题                                 |
| `title`        |    | 文本     | 图表标题                                 |
| `theme`        |    | 文本     | 主题，可选 `default` / `dark` / `academy` |
| `lineWidth`    |    | 数值     | 线条宽度                                 |
| `height`       |    | 数值（像素） | 图表高度                                 |

## 输出格式

````
```line
{JSON配置}
```
````

## 示例

```line
{
  "axisXTitle": "日期",
  "axisYTitle": "订单量",
  "theme": "default",
  "lineWidth": 2,
  "height": 500,
  "data": [
    { "time": "02-01", "value": 120, "group": "门店A" },
    { "time": "02-02", "value": 132, "group": "门店A" },
    { "time": "02-01", "value": 98, "group": "门店B" },
    { "time": "02-02", "value": 105, "group": "门店B" }
  ]
}
```
