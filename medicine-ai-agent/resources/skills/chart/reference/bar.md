# 条形图（Bar）

用于比较不同类别的数值，柱子水平排列。适合类别名称较长或类别较多的场景。支持分组和堆叠。

## 字段说明

| 字段                | 必填 | 类型     | 说明                                   |
|-------------------|:--:|--------|--------------------------------------|
| `data`            |    | 数组对象   | 图表数据                                 |
| `data[].category` |    | 文本     | 分类名称（Y轴）                             |
| `data[].value`    |    | 数值     | 数值（X轴）                               |
| `data[].group`    |    | 文本     | 分组名称，用于分组或堆叠条形图                      |
| `group`           |    | 布尔     | 是否开启分组（需 data 含 group 字段）            |
| `stack`           |    | 布尔     | 是否开启堆叠（需 data 含 group 字段）            |
| `axisXTitle`      |    | 文本     | X轴标题                                 |
| `axisYTitle`      |    | 文本     | Y轴标题                                 |
| `title`           |    | 文本     | 图表标题                                 |
| `theme`           |    | 文本     | 主题，可选 `default` / `dark` / `academy` |
| `height`          |    | 数值（像素） | 图表高度                                 |

## 输出格式

````
```bar
{JSON配置}
```
````

## 示例

```bar
{
  "axisXTitle": "销量",
  "axisYTitle": "区域",
  "height": 500,
  "data": [
    { "category": "华东", "value": 450 },
    { "category": "华南", "value": 390 },
    { "category": "华北", "value": 310 }
  ]
}
```
