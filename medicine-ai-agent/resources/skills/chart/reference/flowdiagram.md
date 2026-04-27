# 流程图（Flow Diagram）

展示流程的步骤和顺序，节点通过箭头连接表示流向。适合业务流程、审批流程、操作步骤等可视化。

## 字段说明

| 字段                    | 必填 | 类型     | 说明                    |
|-----------------------|:--:|--------|-----------------------|
| `data`                |    | 对象     | 图表数据，包含 nodes 和 edges |
| `data.nodes`          |    | 数组对象   | 节点列表                  |
| `data.nodes[].name`   |    | 文本     | 步骤名称                  |
| `data.edges`          |    | 数组对象   | 边列表                   |
| `data.edges[].source` |    | 文本     | 起始节点名称                |
| `data.edges[].target` |    | 文本     | 目标节点名称                |
| `title`               |    | 文本     | 图表标题                  |
| `height`              |    | 数值（像素） | 图表高度                  |

## 输出格式

````
```flowdiagram
{JSON配置}
```
````

## 示例

```flowdiagram
{
  "height": 600,
  "data": {
    "nodes": [
      { "name": "创建订单" },
      { "name": "库存校验" },
      { "name": "支付成功" },
      { "name": "出库发货" }
    ],
    "edges": [
      { "source": "创建订单", "target": "库存校验" },
      { "source": "库存校验", "target": "支付成功" },
      { "source": "支付成功", "target": "出库发货" }
    ]
  }
}
```
