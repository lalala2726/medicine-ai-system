# 双轴图（Dual Axes）

组合图表，通常将柱状图与折线图结合，使用两个 Y 轴展示不同量纲的数据。适合同时展示数量与金额、销量与增长率等场景。

## 字段说明

| 字段                    | 必填 | 类型     | 说明                                   |
|-----------------------|:--:|--------|--------------------------------------|
| `categories`          |    | 数组     | X轴分类数组                               |
| `series`              |    | 数组对象   | 图表组合配置                               |
| `series[].type`       |    | 文本     | 基础图表类型，`column` 或 `line`             |
| `series[].data`       |    | 数值数组   | 数据数组                                 |
| `series[].axisYTitle` |    | 文本     | Y轴标题                                 |
| `title`               |    | 文本     | 图表标题                                 |
| `axisXTitle`          |    | 文本     | X轴标题                                 |
| `theme`               |    | 文本     | 主题，可选 `default` / `dark` / `academy` |
| `height`              |    | 数值（像素） | 图表高度                                 |

## 输出格式

````
```dualaxes
{JSON配置}
```
````

## 示例

```dualaxes
{
  "categories": ["周一", "周二", "周三", "周四", "周五"],
  "series": [
    { "type": "column", "axisYTitle": "单量", "data": [120, 132, 101, 134, 90] },
    { "type": "line", "axisYTitle": "金额", "data": [3200, 3600, 2800, 4100, 3000] }
  ],
  "theme": "default",
  "height": 400
}
```
