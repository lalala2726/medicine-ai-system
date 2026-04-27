# 面积图（Area）

展示数值随有序变量变化的趋势，折线与 X 轴之间的区域填充颜色。支持堆叠面积图（通过 `stack` 和 `group` 字段），适合展示总量与各部分的变化趋势。

## 字段说明

| 字段             | 必填 | 类型     | 说明                                   |
|----------------|:--:|--------|--------------------------------------|
| `data`         |    | 数组对象   | 图表数据                                 |
| `data[].time`  |    | 文本/数值  | 时序名称（X轴）                             |
| `data[].value` |    | 数值     | 数值（Y轴）                               |
| `data[].group` |    | 文本     | 分组名称，用于堆叠面积图                         |
| `stack`        |    | 布尔     | 是否开启堆叠（需 data 含 group 字段）            |
| `axisXTitle`   |    | 文本     | X轴标题                                 |
| `axisYTitle`   |    | 文本     | Y轴标题                                 |
| `title`        |    | 文本     | 图表标题                                 |
| `theme`        |    | 文本     | 主题，可选 `default` / `dark` / `academy` |
| `height`       |    | 数值（像素） | 图表高度                                 |

## 输出格式

````
```area
{JSON配置}
```
````

## 示例

```area
{
  "axisXTitle": "日期",
  "axisYTitle": "销售额",
  "theme": "academy",
  "lineWidth": 2,
  "height": 500,
  "data": [
    { "time": "02-01", "value": 3200 },
    { "time": "02-02", "value": 4100 },
    { "time": "02-03", "value": 3800 }
  ]
}
```
