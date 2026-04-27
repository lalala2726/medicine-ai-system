# 柱状图（Column）

用于比较不同类别的数值，柱子垂直排列。支持分组对比和堆叠展示（通过 `group`、`stack` 字段）。

## 字段说明

| 字段                | 必填 | 类型     | 说明                                   |
|-------------------|:--:|--------|--------------------------------------|
| `data`            |    | 数组对象   | 图表数据                                 |
| `data[].category` |    | 文本     | 分类名称（X轴）                             |
| `data[].value`    |    | 数值     | 数值（Y轴）                               |
| `data[].group`    |    | 文本     | 分组名称，用于分组或堆叠柱状图                      |
| `group`           |    | 布尔     | 是否开启分组（需 data 含 group 字段）            |
| `stack`           |    | 布尔     | 是否开启堆叠（需 data 含 group 字段）            |
| `axisXTitle`      |    | 文本     | X轴标题                                 |
| `axisYTitle`      |    | 文本     | Y轴标题                                 |
| `title`           |    | 文本     | 图表标题                                 |
| `theme`           |    | 文本     | 主题，可选 `default` / `dark` / `academy` |
| `height`          |    | 数值（像素） | 图表高度                                 |

## 输出格式

````
```column
{JSON配置}
```
````

## 示例

```column
{
  "axisXTitle": "品类",
  "axisYTitle": "销量",
  "height": 500,
  "data": [
    { "category": "感冒药", "value": 320 },
    { "category": "维生素", "value": 280 },
    { "category": "止咳药", "value": 210 }
  ]
}
```
