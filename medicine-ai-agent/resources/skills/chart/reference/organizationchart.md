# 组织架构图（Organization Chart）

展示组织结构和人员层级关系。数据为递归树形结构，根节点为最高层级。

## 字段说明

| 字段              | 必填 | 类型     | 说明            |
|-----------------|:--:|--------|---------------|
| `data`          |    | 对象     | 图表数据，递归树形结构   |
| `data.name`     |    | 文本     | 节点名称（人员/部门名称） |
| `data.children` |    | 数组对象   | 下属节点列表（递归结构）  |
| `title`         |    | 文本     | 图表标题          |
| `height`        |    | 数值（像素） | 图表高度          |

## 输出格式

````
```organizationchart
{JSON配置}
```
````

## 示例

```organizationchart
{
  "height": 600,
  "data": {
    "name": "总经理",
    "children": [
      {
        "name": "运营部",
        "children": [
          { "name": "运营一组" },
          { "name": "运营二组" }
        ]
      },
      {
        "name": "技术部",
        "children": [
          { "name": "后端组" },
          { "name": "前端组" }
        ]
      }
    ]
  }
}
```
