# 矩形树图（Treemap）

展示层级结构数据，矩形大小表示数值大小，支持嵌套子分类（通过 `children` 字段）。

## 字段说明

| 字段                | 必填 | 类型     | 说明                                   |
|-------------------|:--:|--------|--------------------------------------|
| `data`            |    | 数组对象   | 图表数据，支持嵌套结构                          |
| `data[].name`     |    | 文本     | 分类名称                                 |
| `data[].value`    |    | 数值     | 数值大小                                 |
| `data[].children` |    | 数组对象   | 子分类列表（递归结构）                          |
| `title`           |    | 文本     | 图表标题                                 |
| `theme`           |    | 文本     | 主题，可选 `default` / `dark` / `academy` |
| `height`          |    | 数值（像素） | 图表高度                                 |

## 输出格式

````
```treemap
{JSON配置}
```
````

## 示例

```treemap
{
  "theme": "academy",
  "height": 400,
  "data": [
    {
      "name": "药品",
      "value": 680,
      "children": [
        { "name": "感冒药", "value": 220 },
        { "name": "消炎药", "value": 180 },
        { "name": "维生素", "value": 280 }
      ]
    },
    {
      "name": "器械",
      "value": 320,
      "children": [
        { "name": "血压计", "value": 130 },
        { "name": "体温计", "value": 190 }
      ]
    }
  ]
}
```
