# 网络关系图（Network Graph）

展示节点之间的关系和连接，适合复杂关系网络可视化。数据包含节点列表（`nodes`）和边列表（`edges`）。

## 字段说明

| 字段                    | 必填 | 类型     | 说明                    |
|-----------------------|:--:|--------|-----------------------|
| `data`                |    | 对象     | 图表数据，包含 nodes 和 edges |
| `data.nodes`          |    | 数组对象   | 节点列表                  |
| `data.nodes[].name`   |    | 文本     | 节点名称                  |
| `data.edges`          |    | 数组对象   | 边列表                   |
| `data.edges[].source` |    | 文本     | 源节点名称                 |
| `data.edges[].target` |    | 文本     | 目标节点名称                |
| `data.edges[].name`   |    | 文本     | 边名称/关系描述              |
| `title`               |    | 文本     | 图表标题                  |
| `height`              |    | 数值（像素） | 图表高度                  |

## 输出格式

````
```networkgraph
{JSON配置}
```
````

## 示例

```networkgraph
{
  "height": 600,
  "data": {
    "nodes": [
      { "name": "用户" },
      { "name": "订单" },
      { "name": "商品" },
      { "name": "仓库" }
    ],
    "edges": [
      { "source": "用户", "target": "订单", "name": "创建" },
      { "source": "订单", "target": "商品", "name": "包含" },
      { "source": "订单", "target": "仓库", "name": "发货" }
    ]
  }
}
```
