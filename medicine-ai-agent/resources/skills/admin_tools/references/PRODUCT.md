# Product Tools Reference

## 领域说明

商品领域工具用于查询商品列表、商品详情和药品说明书信息。  
返回结构以 Python 工具实际返回的 `data` 为准，不包含 Java 原始响应外层的 `code/message/timestamp`。

## 工具清单

| 工具名              | 适用场景                | 关键参数                                                                              |
|------------------|---------------------|-----------------------------------------------------------------------------------|
| `product_list`   | 按关键词、分类、价格区间、状态筛选商品 | `page_num`、`page_size`、`id`、`name`、`category_id`、`status`、`min_price`、`max_price` |
| `product_detail` | 查看商品完整详情            | `product_id: list[str]`                                                           |
| `drug_detail`    | 查看药品说明书与药学信息        | `product_id: list[str]`                                                           |

## product_list

### 实际返回结构

分页对象：

- `total`
- `pageNum`
- `pageSize`
- `rows`

`rows[]` 中每项通常包含：

- `id`
- `name`
- `categoryId`
- `categoryName`
- `unit`
- `price`
- `stock`
- `sales`
- `sort`
- `status`
- `deliveryType`
- `createTime`
- `updateTime`
- `createBy`
- `updateBy`
- `coverImage`

### 关键字段解释

- `status`：商品状态编码。
- `deliveryType`：配送方式编码。
- `coverImage`：商品展示图，适合列表摘要展示。
- 这是列表摘要，不包含图片数组和药品说明书详情。

### 常见可回答问题

- 某分类下有哪些商品
- 某个价格区间里有哪些商品
- 在售商品、下架商品、库存较低商品有哪些

## product_detail

### 实际返回结构

返回商品详情数组 `list[object]`，每项通常包含：

- `id`
- `name`
- `categoryId`
- `categoryName`
- `unit`
- `price`
- `stock`
- `sort`
- `status`
- `deliveryType`
- `createTime`
- `updateTime`
- `createBy`
- `updateBy`
- `images`

### 关键字段解释

- `images`：商品图片数组，适合查看完整商品素材。
- `status`、`deliveryType` 是编码字段。
- 商品详情工具不提供药品说明书字段，只提供商品本身详情。

### 常见可回答问题

- 商品当前库存、价格、状态是什么
- 商品有哪些图片
- 商品属于哪个分类、由谁创建和更新

## drug_detail

### 实际返回结构

返回药品详情数组 `list[object]`，每项通常包含：

- `productId`
- `productName`
- `drugDetail`

`drugDetail` 是药品说明信息对象，常见字段包括：

- `commonName`：药品通用名
- `composition`：成分
- `characteristics`：性状
- `packaging`：包装规格
- `validityPeriod`：有效期
- `storageConditions`：贮藏条件
- `productionUnit`：生产单位
- `approvalNumber`：批准文号
- `executiveStandard`：执行标准
- `originType`：产地类型
- `isOutpatientMedicine`：是否外用药
- `warmTips`：温馨提示
- `brand`：品牌
- `prescription`：是否处方药
- `efficacy`：功能主治
- `usageMethod`：用法用量
- `adverseReactions`：不良反应
- `precautions`：注意事项
- `taboo`：禁忌
- `instruction`：药品说明书全文

### 关键字段解释

- 这是药品说明书级别的信息，不只是商品销售字段。
- `prescription` 可用于区分是否处方药。
- `instruction` 是完整说明书全文；如果只需要结构化要点，优先从其他字段提炼，不必整段复述。

### 常见可回答问题

- 某药品的适应症、用法用量、不良反应是什么
- 某药品是否处方药、批准文号是什么
- 某药品的生产单位、有效期、贮藏条件是什么
