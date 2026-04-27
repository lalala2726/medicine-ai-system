# 缩进树图（Indented Tree）

以缩进方式展示层级结构，适合文件目录、分类体系、商品目录等场景。数据为递归树形结构。

## 字段说明

| 字段              | 必填 | 类型     | 说明                                   |
|-----------------|:--:|--------|--------------------------------------|
| `data`          |    | 对象     | 图表数据，递归树形结构                          |
| `data.name`     |    | 文本     | 节点名称                                 |
| `data.children` |    | 数组对象   | 子节点列表（递归结构）                          |
| `title`         |    | 文本     | 图表标题                                 |
| `theme`         |    | 文本     | 主题，可选 `default` / `dark` / `academy` |
| `height`        |    | 数值（像素） | 图表高度                                 |

## 输出格式

````
```indentedtree
{JSON配置}
```
````

## 示例

```indentedtree
{
  "theme": "default",
  "height": 600,
  "data": {
    "name": "商品目录",
    "children": [
      {
        "name": "药品",
        "children": [
          { "name": "感冒药" },
          { "name": "消炎药" }
        ]
      },
      {
        "name": "保健品",
        "children": [
          { "name": "维生素" },
          { "name": "鱼油" }
        ]
      }
    ]
  }
}
```
