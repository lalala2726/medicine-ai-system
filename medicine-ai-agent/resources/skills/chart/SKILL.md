---
name: chart
description: 图表模板技能，提供 18 种图表类型的模板规范、字段说明与标准输出格式，基于 GPT-Vis (AntV) 规范。
---

# Chart Skill（supervisor）

## 技能目标

为 `supervisor_agent` 提供图表生成所需的模板规范与输出格式要求。当用户需要数据可视化时，
根据数据特征选择合适的图表类型，严格按照模板规范输出图表配置代码块。

## 支持的图表类型（18 种）

| 类别  | 图表类型  | 代码块标识             | 说明           | 模板文件                                                   |
|-----|-------|-------------------|--------------|--------------------------------------------------------|
| 趋势类 | 折线图   | line              | 展示数据随时间的变化趋势 | [line.md](reference/line.md)                           |
| 趋势类 | 面积图   | area              | 趋势展示 + 总量对比  | [area.md](reference/area.md)                           |
| 对比类 | 柱状图   | column            | 分类数据的垂直柱形对比  | [column.md](reference/column.md)                       |
| 对比类 | 条形图   | bar               | 分类数据的水平条形对比  | [bar.md](reference/bar.md)                             |
| 对比类 | 雷达图   | radar             | 多维度综合评价对比    | [radar.md](reference/radar.md)                         |
| 占比类 | 饼图    | pie               | 各部分占整体的比例    | [pie.md](reference/pie.md)                             |
| 占比类 | 矩形树图  | treemap           | 层级占比数据可视化    | [treemap.md](reference/treemap.md)                     |
| 占比类 | 漏斗图   | funnel            | 多阶段转化或流失分析   | [funnel.md](reference/funnel.md)                       |
| 分布类 | 直方图   | histogram         | 连续数据的频率分布    | [histogram.md](reference/histogram.md)                 |
| 分布类 | 散点图   | scatter           | 双变量关系分析      | [scatter.md](reference/scatter.md)                     |
| 分布类 | 词云图   | wordcloud         | 词频/热度权重展示    | [wordcloud.md](reference/wordcloud.md)                 |
| 组合类 | 双轴图   | dualaxes          | 不同量纲数据同图对比   | [dualaxes.md](reference/dualaxes.md)                   |
| 结构类 | 思维导图  | mindmap           | 思维发散/知识结构展示  | [mindmap.md](reference/mindmap.md)                     |
| 结构类 | 网络关系图 | networkgraph      | 节点间关系连接展示    | [networkgraph.md](reference/networkgraph.md)           |
| 结构类 | 流程图   | flowdiagram       | 流程步骤与流向可视化   | [flowdiagram.md](reference/flowdiagram.md)             |
| 组织类 | 组织架构图 | organizationchart | 组织层级结构展示     | [organizationchart.md](reference/organizationchart.md) |
| 组织类 | 缩进树图  | indentedtree      | 目录/分类树形结构    | [indentedtree.md](reference/indentedtree.md)           |
| 组织类 | 鱼骨图   | fishbonediagram   | 因果分析/问题根因追溯  | [fishbonediagram.md](reference/fishbonediagram.md)     |

## 图表选型指南

- **展示趋势变化** → `line`（折线图）或 `area`（面积图）
- **分类数值对比** → `column`（柱状图）或 `bar`（条形图，类别名较长时更适合）
- **多维度综合评价** → `radar`（雷达图）
- **占比分析** → `pie`（饼图）、`treemap`（矩形树图）或 `funnel`（漏斗图）
- **数据分布** → `histogram`（直方图）或 `scatter`（散点图）
- **词频/热度** → `wordcloud`（词云图）
- **双量纲对比** → `dualaxes`（双轴图）
- **思维/知识结构** → `mindmap`（思维导图）
- **关系网络** → `networkgraph`（网络关系图）
- **流程步骤** → `flowdiagram`（流程图）
- **组织层级** → `organizationchart`（组织架构图）或 `indentedtree`（缩进树图）
- **因果分析** → `fishbonediagram`（鱼骨图）

## 输出规范（严格遵守）

### 代码块格式

- 代码块的语言标识**必须精确匹配**图表类型名称（如 `line`、`pie`、`fishbonediagram`）
- 禁止使用 `chart`、`json`、`图表` 等非标准标识
- 代码块内容必须是**合法 JSON**

### 字段约束

- 必填字段不能省略
- 数值字段使用纯数字类型（禁止包含 `%`、`+`、`k`、`万` 等符号）
- 文本字段使用字符串类型
- 字段名大小写必须精确匹配模板定义
- 禁止添加模板中不存在的自定义字段
- **所有字段均为顶层字段，严禁使用 `config`、`options`、`settings` 等嵌套包装对象**（例如 `title` 必须写在顶层，不能写成
  `config.title`）

### 生成流程

1. 根据数据特征和用户需求选择合适的图表类型
2. 参照对应图表的模板规范填充业务数据
3. 保持字段结构不变，仅替换示例数据为真实数据
4. 一个回复中可包含多个图表代码块
