# 药智通

药智通是一个面向医药零售和智能问诊场景的全栈项目，包含管理端前端、客户端前端、Spring Boot 业务后端和 Python AI Agent 服务。系统围绕药品商品、商城交易、售后履约、用户钱包、运营分析、知识库检索、图片识别和智能助手等功能构建，适合用于医药电商、互联网问诊、AI 导购和后台运营管理等学习与二次开发场景。

本仓库是开源发布副本

## 在线体验

建议先通过线上地址快速访问体验，再按下方文档进行本地部署和二次开发。

| 入口 | 地址 |
| --- | --- |
| 项目介绍网站 | [https://medicine.zhangyichuang.com/](https://medicine.zhangyichuang.com/) |
| 管理端 | [https://medicine-admin.zhangyichuang.com/](https://medicine-admin.zhangyichuang.com/) |
| 客户端 | [https://medicine-client.zhangyichuang.com/](https://medicine-client.zhangyichuang.com/) |

体验账号：

```text
账号：admin
密码：admin123
```

## 项目背景

这个项目的起点，其实是我自己一次很普通的买药经历。

在做这个项目之前，我在 2025 年 8 月底刚完成了第一个真正由我自己独立开发完的项目。那个项目更像一个脚手架系统，业务逻辑不算多，但对当时的我来说意义挺大。Spring Boot、Spring Security 这些东西我之前都学过，可“学过”和“自己完整集成进一个项目里”完全不是一回事。所以那段时间我下载了不少开源项目，看别人怎么写，再一点点照着敲、照着理解，断断续续做了五六个月。项目做完之后，技术能力确实提升了，但回头看也会觉得，它更像是在补工程基础，业务本身没有太多可以展开的地方。

后来有一次我生病了，是上呼吸道感染，喉咙特别难受，当时就急着去买药。但问题是我自己也不知道该买什么药，就在线问了一个问诊医生。那次挂号原本是 20 元，医生最后只收了我 10 元，还给我推荐了药。也是那次经历之后，我突然想到：能不能用 AI 做一个可以一步步追问症状、辅助判断情况，再推荐合适药品的系统？

这个想法后来就慢慢变成了现在的药智通。它比我之前那个脚手架项目更重业务逻辑，也更贴近真实场景：有商城、有订单、有售后、有用户钱包，也有 AI 问诊、药品推荐、图片识别和后台运营配置。这个项目里我也用了更多 AI 辅助开发，和第一个项目大部分代码自己手敲的状态不太一样。对我来说，它更像是一次把业务、工程和 AI 能力放在一起的完整练习，也会作为我的毕业设计继续完善。

项目最近这几天基本做完了，我也刚好开始找工作。如果你看到这里，并且贵公司正在招聘，觉得这个项目还不错，欢迎通过邮箱联系我：`zhangchuang2726@gmail.com`。很荣幸，也很期待有机会与你共事。

如果这段话还在，说明我还在找工作。

## 项目特性

- 管理后台：提供商品、分类、标签、订单、售后、优惠券、用户、钱包、日志、权限、系统模型和智能助手配置等后台能力。
- 客户端商城：提供首页、分类、搜索、商品详情、购物车、订单结算、优惠券、地址、个人中心和 AI 助手入口。
- 业务后端：采用 Spring Boot 多模块工程，按管理端、客户端、AI 调用、公共能力、模型和 RPC 契约拆分。
- AI Agent：基于 FastAPI、LangChain 和 LangGraph 构建，支持智能问诊、商品推荐、图片理解、知识库检索和工具调用。
- 数据能力：使用 MySQL 保存核心业务数据，MongoDB 保存地区数据，Elasticsearch 提供商品检索，Redis 用于缓存和会话，RabbitMQ 用于异步消息。
- 文件能力：使用 MinIO 管理商品图、头像、分类封面等对象资源。
- 检索增强：支持 Milvus、Neo4j 等知识库和图谱组件，可用于医学知识问答和推荐链路扩展。

## 系统截图

### 管理端

![售后总览](./images/admin/after-sales-overview.png)

![运营分析](./images/admin/operations-analytics.png)

![AI 助手演示](./images/admin/ai-assistant-demo.png)

### 客户端

<table>
  <tr>
    <td><img src="./images/client/client-interface-01.png" alt="客户端界面 01" width="180" /></td>
    <td><img src="./images/client/client-interface-02.png" alt="客户端界面 02" width="180" /></td>
    <td><img src="./images/client/client-interface-03.png" alt="客户端界面 03" width="180" /></td>
    <td><img src="./images/client/client-interface-04.png" alt="客户端界面 04" width="180" /></td>
    <td><img src="./images/client/client-interface-05.png" alt="客户端界面 05" width="180" /></td>
  </tr>
  <tr>
    <td><img src="./images/client/client-interface-06.png" alt="客户端界面 06" width="180" /></td>
    <td><img src="./images/client/client-interface-07.png" alt="客户端界面 07" width="180" /></td>
    <td><img src="./images/client/client-interface-08.png" alt="客户端界面 08" width="180" /></td>
    <td><img src="./images/client/client-interface-09.png" alt="客户端界面 09" width="180" /></td>
    <td><img src="./images/client/client-interface-10.png" alt="客户端界面 10" width="180" /></td>
  </tr>
</table>

## 目录结构

```text
.
├── Medicine_business/          Spring Boot 业务后端多模块工程
├── medicine_admin_front_end/   管理端前端，Vite + React + TypeScript
├── medicine_client_front_end/  客户端前端，Vite + React + TypeScript
├── medicine-ai-agent/          Python FastAPI AI Agent 服务
└── database/                   MySQL 与 MongoDB 初始化数据
```

### 后端模块

```text
Medicine_business/
├── medicine-admin/          管理端接口服务
├── medicine-client/         客户端接口服务
├── medicine-agent/          业务后端与 AI Agent 的接口服务
├── medicine-common/         通用工具、统一响应、基础配置
├── medicine-model/          领域模型、DTO、VO、实体对象
├── medicine-rpc-contract/   Dubbo RPC 契约
└── medicine-shared/         跨模块共享能力
```

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 业务后端 | Spring Boot、Java、Maven、Dubbo、MyBatis-Plus、Sa-Token、RabbitMQ |
| 管理端前端 | React、TypeScript、Vite、Ant Design、Ant Design Pro Components |
| 客户端前端 | React、TypeScript、Vite、NutUI、Zustand |
| AI Agent | Python、FastAPI、LangChain、LangGraph、Pydantic |
| 数据存储 | MySQL、MongoDB、Redis、Elasticsearch、Milvus、Neo4j |
| 文件服务 | MinIO |

## 功能模块

### 管理后台

- 数据看板：运营分析、售后总览、交易指标和趋势统计。
- 商品管理：商品基础信息、药品属性、分类、标签、上下架和库存相关信息。
- 订单管理：订单列表、订单详情、支付状态、履约流程和售后关联。
- 售后管理：退款、退货、售后进度、售后原因和处理结果。
- 优惠券管理：优惠券模板、发放、领取、使用记录和活动配置。
- 用户管理：用户资料、角色权限、登录日志、操作日志和钱包信息。
- AI 配置：模型配置、提示词配置、图片识别配置和智能助手管理。

### 客户端

- 商城首页：药品推荐、分类入口、营销区和商品瀑布流。
- 商品浏览：分类筛选、搜索、商品详情、标签和药品信息展示。
- 交易链路：购物车、订单确认、地址选择、优惠券使用和支付结果展示。
- 用户中心：个人资料、收货地址、订单记录、优惠券和钱包信息。
- 智能助手：支持医疗问答、药品推荐、订单卡片和上下文交互。

### AI Agent

- 多轮问诊：根据用户症状、就诊人信息和上下文逐步追问。
- 商品推荐：结合症状、药品知识和商城商品生成推荐卡片。
- 图片理解：支持药品图片识别和结构化解析。
- 工具调用：通过工具协议调用后端商品、订单、用户和知识库能力。
- 知识库检索：可接入向量数据库和知识图谱增强医学问答。

## AI 与检索能力

这个项目里实现了 RAG 检索链路，使用 Milvus 完成知识库的创建、文档切片、向量化存储和相似度检索。聊天过程中也会结合检索结果，让模型不是只靠自己的参数回答，而是能基于项目里的知识库内容进行回复。

为了让模型在复杂场景下更稳定，我也做了 Skill 能力。不同业务能力会拆成不同的 Skill，让模型在问诊、商品推荐、订单卡片、图片理解等场景里有更明确的能力边界和工具说明，不用一次性把所有提示词和工具都塞给模型。

项目里还接入了 Neo4j 图数据库，用医疗知识图谱给模型提供一定的诊断依据。它不是为了替代医生，而是希望在 AI 追问症状、分析可能方向、推荐药品时，能有更清楚的知识结构作为参考。

语音方面接入了豆包语音能力，支持语音播放和语音识别。也就是说，智能助手不只是文字聊天，也可以把回答播放出来，或者通过语音输入继续对话。

最近临近毕业季，一边要写论文，一边也在找工作，所以很多更具体的实现步骤和细节文档后续会慢慢补上。现在更建议先通过上面的在线地址实际体验一下项目。

## 数据来源与致谢

本项目 Neo4j 医疗知识图谱使用的数据来自 [QASystemOnMedicalKG](https://github.com/liuhuanyong/QASystemOnMedicalKG)。图谱数据的具体导入方式请参考该仓库中的说明文档。感谢原作者和相关贡献者的开源工作。

## 启动要求

- JDK：25。
- Maven：3.9+。
- Node.js：20+。
- pnpm：9+。
- Python：3.12+。
- MySQL：建议 8.x，默认连接 `localhost:3306`，库名 `medicine`，账号示例 `root/123456`。
- Redis：默认连接 `localhost:6379`，密码示例 `123456`。
- MongoDB：默认连接 `localhost:27017`，库名 `medicine`，账号示例 `root/123456`。
- Elasticsearch：默认连接 `http://localhost:9200`，账号示例 `elastic/123456`。
- RabbitMQ：默认连接 `localhost:5672`，账号示例 `guest/123456`。
- MinIO：默认连接 `http://localhost:9000`，Key 示例 `123456/123456`。
- AI 相关依赖：Milvus、Neo4j、可用的大模型供应商 API Key。

## Elasticsearch 插件

商品搜索使用了中文分词和拼音检索能力，启动后端前需要根据自己的 Elasticsearch 版本安装对应版本插件。当前导出环境插件如下：

| component | version |
| --- | --- |
| analysis-ik | 8.19.7 |
| analysis-pinyin | 8.19.7 |

如果你的 ES 版本不是 `8.19.7`，请安装与 ES 主版本完全一致的 `analysis-ik` 和 `analysis-pinyin` 插件。离线安装示例：

```bash
./bin/elasticsearch-plugin install file:///path/to/elasticsearch-analysis-ik-8.19.7.zip
./bin/elasticsearch-plugin install file:///path/to/elasticsearch-analysis-pinyin-8.19.7.zip
```

Docker 环境可进入容器执行同类命令，插件安装完成后需要重启 Elasticsearch。

## 数据库初始化

数据库初始化文件位于 [database](./database)：

- [database/MySQL/medicine.sql](./database/MySQL/medicine.sql)：MySQL 业务库脚本。
- [database/MongoDB/regions.jsonl.gz](./database/MongoDB/regions.jsonl.gz)：MongoDB 地区数据。
- [database/MongoDB/import_regions.py](./database/MongoDB/import_regions.py)：MongoDB 地区数据导入脚本。

### 导入 MySQL

```bash
cd database/MySQL
mysql -h localhost -P 3306 -u root -p123456 -e "CREATE DATABASE IF NOT EXISTS medicine DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
mysql -h localhost -P 3306 -u root -p123456 medicine < medicine.sql
```

### 导入 MongoDB 地区数据

```bash
cd database/MongoDB
python3 -m pip install pymongo
python3 import_regions.py
```

默认导入到 `mongodb://root:123456@localhost:27017/medicine?authSource=admin` 的 `regions` 集合。更多参数见 [database/README.md](./database/README.md)。

## 本地启动

建议启动顺序：

1. 启动 MySQL、Redis、MongoDB、Elasticsearch、RabbitMQ、MinIO。
2. 安装并重启 Elasticsearch 插件。
3. 导入 `database/MySQL/medicine.sql` 和 `database/MongoDB/regions.jsonl.gz`。
4. 启动业务后端。
5. 启动 AI Agent。
6. 启动管理端前端和客户端前端。

### 业务后端

```bash
cd Medicine_business
mvn -DskipTests package
mvn -pl medicine-client spring-boot:run
mvn -pl medicine-admin spring-boot:run
mvn -pl medicine-agent spring-boot:run
```

默认端口：

- `medicine-client`: `http://localhost:8081`
- `medicine-admin`: `http://localhost:8082`
- `medicine-agent`: `http://localhost:8083`

### AI Agent

```bash
cd medicine-ai-agent
python -m venv .venv
source .venv/bin/activate
pip install -e .
cp .env.example .env
uvicorn app.main:app --host localhost --port 8000 --reload
```

### 管理端前端

```bash
cd medicine_admin_front_end
pnpm install
pnpm dev
```

管理端默认访问：`http://localhost:3000`。

### 客户端前端

```bash
cd medicine_client_front_end
pnpm install
cp .env.example .env
pnpm dev
```

客户端前端使用 Vite 默认开发端口，通常为 `http://localhost:5173`。
