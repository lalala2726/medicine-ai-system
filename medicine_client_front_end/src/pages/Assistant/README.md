# Assistant 页面说明

本目录承载“智能客服 / Assistant”页面的前端实现。

当前实现已经按“页面装配 / UI 组件 / 页面逻辑模块”拆分，目标是让：

- `index.tsx` 只负责页面骨架和组件装配
- `components/` 只负责展示
- `modules/` 负责页面状态、SSE 流、会话历史、消息适配等业务逻辑

## 目录结构

```text
src/pages/Assistant/
  index.tsx
  index.module.less
  README.md
  components/
    AfterSaleSelector/
    AssistantComposer/
    AssistantMessageList/
    ConversationList/
    MessagePrimitives/
    MessageContentRenderer/
    OrderSelector/
    ProductCard/
  modules/
    conversation/
    messages/
    page/
    selectors/
    session/
    shared/
    stream/
```

## 根目录文件

### `index.tsx`

Assistant 页面入口。

职责：

- 渲染页面头部
- 渲染消息列表
- 渲染自研输入区
- 渲染订单/售后弹层
- 调用 `useAssistantPageController` 获取页面所需全部状态和事件

这份文件不应该继续堆业务逻辑，新增页面行为优先下沉到 `modules/`。

### `index.module.less`

Assistant 页面容器、头部、聊天区等页面级样式。

### `ORDER_CARD_PROTOCOL.md`

订单卡协议对接文档。

职责：

- 说明文本消息与订单卡消息的 submit 请求结构
- 说明订单卡的实时消息回传结构
- 说明订单卡的历史消息 `cards[]` 回传结构
- 约定 submit 阶段订单卡使用 `order_no`，售后卡使用 `after_sale_no`

## components

`components/` 只放展示组件，不直接处理复杂业务编排。

### `components/AfterSaleSelector`

售后单选择弹层。

职责：

- 展示售后列表
- 触发用户选择

数据请求、分页、搜索、Tab 切换逻辑在 `modules/selectors/useAfterSaleSelectorController.ts`。

### `components/ConversationList`

左侧抽屉里的历史会话列表。

职责：

- 展示会话列表
- 展示长按菜单
- 展示重命名弹层

会话加载、切换、重命名、删除逻辑在 `modules/conversation/useConversationListController.ts`。

### `components/MessageContentRenderer`

聊天消息内容渲染入口。

职责：

- 根据 `ChatMessage.type` 选择具体渲染方式
- 渲染文本、思考态、图片、商品卡片

它只消费 UI 视图模型，不处理后端协议适配。

### `components/OrderSelector`

订单选择弹层。

职责：

- 展示订单列表
- 触发用户选择订单

请求、分页、搜索、Tab 切换逻辑在 `modules/selectors/useOrderSelectorController.ts`。

### `components/ProductCard`

商品卡片纯展示组件，负责展示商品列表和“查看”按钮。

## modules

`modules/` 是 Assistant 页面真正的业务逻辑层。

### `modules/page`

页面级编排。

#### `useAssistantPageController.ts`

页面总控 Hook，是 Assistant 页面最重要的入口。

职责：

- 管理页面消息列表
- 管理历史会话初始化和切换
- 管理 SSE 流式消息发送
- 管理订单/售后选择弹层
- 组装消息列表和输入区所需 props
- 处理新建会话、菜单按钮、工具栏点击

如果要新增页面级交互，优先从这里接入。

#### `assistantPage.constants.ts`

页面级常量。

例如：

- 页面文案
- 提示文案
- 工具栏演示图片地址
- 临时消息类型集合

### `modules/session`

历史会话和页面启动流程。

#### `useAssistantHistory.ts`

负责会话历史消息加载、欢迎态恢复和请求竞态保护。

#### `useConversationBootstrap.ts`

负责页面首次进入时的初始化流程。

例如：

- 拉取会话列表
- 恢复上次活跃会话
- 页面卸载时清理流式请求

### `modules/stream`

SSE 流式消息处理。

#### `useAssistantStream.ts`

负责一次流式对话请求的完整生命周期。

包括：

- 发起 SSE 请求
- 处理 `notice / thinking / answer / action / card`
- 维护流式中间态消息
- 最终结算成功/失败/中断状态

#### `assistantStreamActionRouter.ts`

负责把后端 `action` 事件分发成页面行为。

当前支持：

- 打开订单列表
- 打开售后列表

### `modules/messages`

消息 UI 模型与协议适配层。

#### `chatTypes.ts`

定义页面渲染层使用的消息类型。

例如：

- `ChatMessage`
- `ChatMessageContent`
- `ProductDisplayItem`
- `CHAT_MESSAGE_TYPES`

这里只放 UI 视图模型，不放后端 Assistant 协议定义。

#### `messageAdapters.ts`

协议层到 UI 层的唯一翻译边界。

职责：

- `AssistantTypes.Message -> ChatMessage`
- 把后端商品卡片协议适配成页面可直接渲染结构

如果后端消息结构变化，优先修改这里。

#### `useMessageCallbacks.ts`

消息交互回调。

例如：

- 点击商品后的附加消息
- 点击“查看”后的页面跳转

### `modules/selectors`

订单/售后相关选择器逻辑。

#### `useAssistantSelectorState.ts`

统一管理订单选择器、售后选择器的显隐状态和初始筛选状态。

#### `orderSelection.ts`

把订单对象整理成：

- 发给 AI 的问题文本
- 用户侧回显消息

#### `afterSaleSelection.ts`

把售后单对象整理成：

- 发给 AI 的问题文本
- 用户侧回显消息

#### `useOrderSelectorController.ts`

订单选择器的分页、搜索、状态筛选逻辑。

#### `useAfterSaleSelectorController.ts`

售后选择器的分页、搜索、状态筛选逻辑。

### `modules/conversation`

历史会话抽屉相关逻辑。

#### `useConversationListController.ts`

负责：

- 会话列表首次加载和分页加载
- 新建会话
- 切换会话
- 长按菜单
- 重命名
- 删除会话

### `modules/shared`

共享 UI 配置。

#### `assistantUiConfig.ts`

负责：

- 助手头像
- 默认用户头像
- 快捷回复配置
- 底部工具栏配置

## 页面运行主链路

可以把 Assistant 页面理解成下面这条链路：

1. `index.tsx` 调用 `useAssistantPageController`
2. `useAssistantPageController` 组合 session / stream / selectors / messages 等模块
3. `Chat` 收到 `chatProps`
4. 消息通过 `MessageContentRenderer` 渲染
5. 后端协议消息先经过 `messageAdapters.ts` 适配，再进入 UI

## 常见维护入口

### 1. 想改页面整体交互

优先看：

- `modules/page/useAssistantPageController.ts`

### 2. 想改历史会话加载逻辑

优先看：

- `modules/session/useAssistantHistory.ts`
- `modules/session/useConversationBootstrap.ts`
- `modules/conversation/useConversationListController.ts`

### 3. 想改 SSE 流式回复逻辑

优先看：

- `modules/stream/useAssistantStream.ts`
- `modules/stream/assistantStreamActionRouter.ts`

### 4. 想改消息展示结构

优先看：

- `modules/messages/chatTypes.ts`
- `modules/messages/messageAdapters.ts`
- `components/MessageContentRenderer/index.tsx`

### 5. 想新增一种卡片类型

优先看：

- `src/api/assistant/contract.ts`
- `modules/messages/messageAdapters.ts`
- `components/MessageContentRenderer/index.tsx`

### 6. 想改订单/售后选择后的发送内容

优先看：

- `modules/selectors/orderSelection.ts`
- `modules/selectors/afterSaleSelection.ts`

## 分层约束

为避免目录再次变乱，建议继续遵守这些约束：

- `index.tsx` 不直接堆复杂业务逻辑
- `components/` 不直接请求接口、不直接操作 store
- `modules/messages/` 只处理 UI 消息模型和协议适配
- 后端协议定义统一放 `src/api/assistant/contract.ts`
- AI Agent 侧接口实现放 `src/api/assistant/agent.ts`
- 8081 业务侧接口实现放 `src/api/assistant/business.ts`
- 页面和 store 统一从 `src/api/assistant/index.ts` 导出入口引用 API
- Assistant 链路不再依赖 `src/utils/normalize.ts`，局部规范化逻辑应内聚到实际使用文件

如果后面继续扩展 Assistant 页面，优先保持这套边界不变。
