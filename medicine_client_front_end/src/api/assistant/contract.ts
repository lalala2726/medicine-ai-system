/** 提取对象 value 联合类型的辅助类型。 */
type ValueOf<T> = T[keyof T]

/** Assistant 协议中的消息发送方角色常量。 */
export const ASSISTANT_ROLES = {
  /** 当前登录用户发送的消息。 */
  USER: 'user',
  /** AI 助手发送的消息。 */
  AI: 'ai'
} as const

/** Assistant 协议中的消息类型常量。 */
export const ASSISTANT_MESSAGE_TYPES = {
  /** 纯文本消息。 */
  TEXT: 'text',
  /** 卡片消息。 */
  CARD: 'card'
} as const

/** Assistant 协议中的消息状态常量。 */
export const ASSISTANT_MESSAGE_STATUSES = {
  /** 消息仍在流式生成中。 */
  STREAMING: 'streaming',
  /** 消息已成功完成。 */
  SUCCESS: 'success',
  /** 消息被用户主动停止。 */
  CANCELLED: 'cancelled',
  /** 消息处理失败。 */
  ERROR: 'error'
} as const

/** Assistant 协议中的卡片类型常量。 */
export const ASSISTANT_CARD_TYPES = {
  /** 商品推荐卡片。 */
  PRODUCT_CARD: 'product-card',
  /** 商品咨询卡片。 */
  CONSULT_PRODUCT_CARD: 'consult-product-card',
  /** 商品购买确认卡片。 */
  PRODUCT_PURCHASE_CARD: 'product-purchase-card',
  /** 订单卡片。 */
  ORDER_CARD: 'order-card',
  /** 售后卡片。 */
  AFTER_SALE_CARD: 'after-sale-card',
  /** 就诊人卡片。 */
  PATIENT_CARD: 'patient-card',
  /** 同意/拒绝卡片。 */
  CONSENT_CARD: 'consent-card',
  /** consultation 专用问卷卡片。 */
  CONSULTATION_QUESTIONNAIRE_CARD: 'consultation-questionnaire-card',
  /** 选项选择卡片。 */
  SELECTION_CARD: 'selection-card'
} as const

/** Assistant SSE 流式事件类型常量。 */
export const ASSISTANT_STREAM_EVENT_TYPES = {
  /** 通知类事件，通常携带会话初始化元信息。 */
  NOTICE: 'notice',
  /** 思考过程增量事件。 */
  THINKING: 'thinking',
  /** 最终回答增量事件。 */
  ANSWER: 'answer',
  /** 要求前端执行动作的事件。 */
  ACTION: 'action',
  /** 独立卡片事件。 */
  CARD: 'card',
  /** 状态同步事件。 */
  STATUS: 'status',
  /** 工具调用事件。 */
  FUNCTION_CALL: 'function_call',
  /** 工具结果事件。 */
  TOOL_RESPONSE: 'tool_response',
  /** 当前前端未识别的兜底事件。 */
  UNKNOWN: 'unknown'
} as const

/**
 * Assistant 模块协议层类型定义
 * 描述后端接口、SSE 事件和归一化后的领域对象
 */
export namespace AssistantTypes {
  /** 当前支持的卡片类型。 */
  export type CardType = ValueOf<typeof ASSISTANT_CARD_TYPES>

  /** 消息类型：纯文本或卡片 */
  export type MessageType = ValueOf<typeof ASSISTANT_MESSAGE_TYPES>

  /** 消息状态 */
  export type MessageStatus = ValueOf<typeof ASSISTANT_MESSAGE_STATUSES>

  /** 消息发送方角色 */
  export type Role = ValueOf<typeof ASSISTANT_ROLES>

  /** 已知 SSE 事件类型 */
  export type KnownStreamEventType = ValueOf<typeof ASSISTANT_STREAM_EVENT_TYPES>

  /** SSE 事件类型，保留对未来新增事件的兼容 */
  export type StreamEventType = KnownStreamEventType | (string & {})

  /** 会话列表条目（已规范化） */
  export interface ConversationItem {
    /** 会话唯一标识。 */
    conversationUuid: string
    /** 会话标题。 */
    title: string
  }

  /** 商品卡片中单个商品的展示信息（已规范化） */
  export interface ProductCardItem {
    /** 商品 ID。 */
    id: string
    /** 商品名称。 */
    name: string
    /** 商品主图地址。 */
    image: string
    /** 商品价格展示文案。 */
    price: string
  }

  /** 商品卡片业务数据（已规范化） */
  export interface ProductCardData {
    /** 卡片标题。 */
    title?: string
    /** 商品列表。 */
    products: ProductCardItem[]
  }

  /** 商品卡片载荷（已规范化） */
  export interface ProductCardPayload {
    /** 卡片类型标识。 */
    type: typeof ASSISTANT_CARD_TYPES.PRODUCT_CARD
    /** 商品卡片业务数据。 */
    data: ProductCardData
  }

  /** 商品咨询卡片中单个商品的展示信息（已规范化）。 */
  export interface ConsultProductCardItem {
    /** 商品 ID。 */
    id: string
    /** 商品名称。 */
    name: string
    /** 商品主图地址。 */
    image: string
    /** 商品价格展示文案。 */
    price: string
  }

  /** 商品咨询卡片业务数据（已规范化）。 */
  export interface ConsultProductCardData {
    /** 卡片标题。 */
    title?: string
    /** 当前咨询商品。 */
    product: ConsultProductCardItem
  }

  /** 商品咨询卡片载荷（已规范化）。 */
  export interface ConsultProductCardPayload {
    /** 卡片类型标识。 */
    type: typeof ASSISTANT_CARD_TYPES.CONSULT_PRODUCT_CARD
    /** 商品咨询卡片业务数据。 */
    data: ConsultProductCardData
  }

  /** 商品购买卡中单个商品的展示信息（已规范化） */
  export interface ProductPurchaseCardItem {
    /** 商品 ID。 */
    id: string
    /** 商品名称。 */
    name: string
    /** 商品主图地址。 */
    image: string
    /** 商品价格展示文案。 */
    price: string
    /** 当前商品购买数量。 */
    quantity: number
  }

  /** 商品购买卡业务数据（已规范化） */
  export interface ProductPurchaseCardData {
    /** 卡片标题。 */
    title?: string
    /** 商品列表。 */
    products: ProductPurchaseCardItem[]
    /** 后端计算后的总价展示文案。 */
    totalPrice: string
  }

  /** 商品购买卡载荷（已规范化） */
  export interface ProductPurchaseCardPayload {
    /** 卡片类型标识。 */
    type: typeof ASSISTANT_CARD_TYPES.PRODUCT_PURCHASE_CARD
    /** 商品购买卡业务数据。 */
    data: ProductPurchaseCardData
  }

  /** 订单卡中的首个商品预览信息（已规范化）。 */
  export interface OrderCardPreviewProductData {
    /** 商品 ID。 */
    productId: string
    /** 商品名称。 */
    productName: string
    /** 商品图片地址。 */
    imageUrl: string
  }

  /** 订单卡业务数据（已规范化）。 */
  export interface OrderCardData {
    /** 订单号。 */
    orderNo: string
    /** 订单状态枚举值。 */
    orderStatus: string
    /** 订单状态展示文案。 */
    orderStatusText: string
    /** 首个商品预览信息。 */
    previewProduct: OrderCardPreviewProductData
    /** 商品总数。 */
    productCount: number
    /** 实付金额。 */
    payAmount: string
    /** 订单总金额。 */
    totalAmount: string
    /** 下单时间。 */
    createTime: string
  }

  /** 订单卡载荷（已规范化）。 */
  export interface OrderCardPayload {
    /** 卡片类型标识。 */
    type: typeof ASSISTANT_CARD_TYPES.ORDER_CARD
    /** 订单卡业务数据。 */
    data: OrderCardData
  }

  /** 售后卡中的商品信息（已规范化）。 */
  export interface AfterSaleCardProductInfoData {
    /** 商品名称。 */
    productName: string
    /** 商品图片地址。 */
    productImage: string
  }

  /** 售后卡业务数据（已规范化）。 */
  export interface AfterSaleCardData {
    /** 售后单号。 */
    afterSaleNo: string
    /** 订单编号。 */
    orderNo: string
    /** 售后类型编码。 */
    afterSaleType: string
    /** 售后类型展示文案。 */
    afterSaleTypeText: string
    /** 售后状态编码。 */
    afterSaleStatus: string
    /** 售后状态展示文案。 */
    afterSaleStatusText: string
    /** 退款金额。 */
    refundAmount: string
    /** 申请原因名称。 */
    applyReasonName: string
    /** 申请时间。 */
    applyTime: string
    /** 售后商品信息。 */
    productInfo: AfterSaleCardProductInfoData
  }

  /** 售后卡载荷（已规范化）。 */
  export interface AfterSaleCardPayload {
    /** 卡片类型标识。 */
    type: typeof ASSISTANT_CARD_TYPES.AFTER_SALE_CARD
    /** 售后卡业务数据。 */
    data: AfterSaleCardData
  }

  /** 就诊人卡业务数据（已规范化）。 */
  export interface PatientCardData {
    /** 就诊人 ID。 */
    patientId: string
    /** 就诊人姓名。 */
    name: string
    /** 性别编码。 */
    gender: number
    /** 性别展示文案。 */
    genderText: string
    /** 出生日期。 */
    birthDate: string
    /** 与账户关系。 */
    relationship: string
    /** 是否默认就诊人。 */
    isDefault: boolean
    /** 过敏史。 */
    allergy?: string
    /** 既往病史。 */
    pastMedicalHistory?: string
    /** 慢性病信息。 */
    chronicDisease?: string
    /** 长期用药。 */
    longTermMedications?: string
  }

  /** 就诊人卡载荷（已规范化）。 */
  export interface PatientCardPayload {
    /** 卡片类型标识。 */
    type: typeof ASSISTANT_CARD_TYPES.PATIENT_CARD
    /** 就诊人卡业务数据。 */
    data: PatientCardData
  }

  /** 同意卡中的按钮动作。 */
  export interface ConsentAction {
    /** 按钮展示文案。 */
    label: string
    /** 发送给后端的语义值。 */
    value: string
  }

  /** 同意卡业务数据（已规范化） */
  export interface ConsentCardData {
    /** 卡片场景标识。 */
    scene?: string
    /** 卡片标题。 */
    title?: string
    /** 卡片说明。 */
    description?: string
    /** 同意动作。 */
    confirm: ConsentAction
    /** 拒绝动作。 */
    reject: ConsentAction
  }

  /** 同意卡载荷（已规范化） */
  export interface ConsentCardPayload {
    /** 卡片类型标识。 */
    type: typeof ASSISTANT_CARD_TYPES.CONSENT_CARD
    /** 同意卡业务数据。 */
    data: ConsentCardData
  }

  /** 选择卡业务数据（已规范化） */
  export interface SelectionCardData {
    /** 卡片场景标识。 */
    scene?: string
    /** 卡片标题。 */
    title?: string
    /** 可选项列表。 */
    options: string[]
  }

  /** 选择卡载荷（已规范化） */
  export interface SelectionCardPayload {
    /** 卡片类型标识。 */
    type: typeof ASSISTANT_CARD_TYPES.SELECTION_CARD
    /** 选择卡业务数据。 */
    data: SelectionCardData
  }

  /** consultation 问卷卡中的单个问题结构。 */
  export interface ConsultationQuestionnaireQuestionData {
    /** 问题文本。 */
    question: string
    /** 当前问题的可选项列表。 */
    options: string[]
  }

  /** consultation 问卷卡业务数据（已规范化） */
  export interface ConsultationQuestionnaireCardData {
    /** 问卷问题列表。 */
    questions: ConsultationQuestionnaireQuestionData[]
  }

  /** consultation 问卷卡载荷（已规范化） */
  export interface ConsultationQuestionnaireCardPayload {
    /** 卡片类型标识。 */
    type: typeof ASSISTANT_CARD_TYPES.CONSULTATION_QUESTIONNAIRE_CARD
    /** consultation 问卷卡业务数据。 */
    data: ConsultationQuestionnaireCardData
  }

  /** 卡片消息的载荷数据（已规范化） */
  export type CardPayload =
    | ProductCardPayload
    | ConsultProductCardPayload
    | ProductPurchaseCardPayload
    | OrderCardPayload
    | AfterSaleCardPayload
    | PatientCardPayload
    | ConsentCardPayload
    | ConsultationQuestionnaireCardPayload
    | SelectionCardPayload

  /** 历史消息中的通用卡片结构。 */
  export interface HistoryCard {
    /** 卡片唯一标识。 */
    cardUuid: string
    /** 卡片类型标识。 */
    type: string
    /** 卡片业务数据。 */
    data: unknown
  }

  /** 历史消息中的商品卡片结构。 */
  export interface HistoryProductCard extends HistoryCard {
    /** 卡片类型标识。 */
    type: typeof ASSISTANT_CARD_TYPES.PRODUCT_CARD
    /** 商品卡片业务数据。 */
    data: ProductCardData
  }

  /** 历史消息中的商品咨询卡片结构。 */
  export interface HistoryConsultProductCard extends HistoryCard {
    /** 卡片类型标识。 */
    type: typeof ASSISTANT_CARD_TYPES.CONSULT_PRODUCT_CARD
    /** 商品咨询卡片业务数据。 */
    data: ConsultProductCardData
  }

  /** 历史消息中的商品购买卡片结构。 */
  export interface HistoryProductPurchaseCard extends HistoryCard {
    /** 卡片类型标识。 */
    type: typeof ASSISTANT_CARD_TYPES.PRODUCT_PURCHASE_CARD
    /** 商品购买卡业务数据。 */
    data: ProductPurchaseCardData
  }

  /** 历史消息中的订单卡片结构。 */
  export interface HistoryOrderCard extends HistoryCard {
    /** 卡片类型标识。 */
    type: typeof ASSISTANT_CARD_TYPES.ORDER_CARD
    /** 订单卡业务数据。 */
    data: OrderCardData
  }

  /** 历史消息中的售后卡片结构。 */
  export interface HistoryAfterSaleCard extends HistoryCard {
    /** 卡片类型标识。 */
    type: typeof ASSISTANT_CARD_TYPES.AFTER_SALE_CARD
    /** 售后卡业务数据。 */
    data: AfterSaleCardData
  }

  /** 历史消息中的就诊人卡片结构。 */
  export interface HistoryPatientCard extends HistoryCard {
    /** 卡片类型标识。 */
    type: typeof ASSISTANT_CARD_TYPES.PATIENT_CARD
    /** 就诊人卡业务数据。 */
    data: PatientCardData
  }

  /** 历史消息中的同意卡片结构。 */
  export interface HistoryConsentCard extends HistoryCard {
    /** 卡片类型标识。 */
    type: typeof ASSISTANT_CARD_TYPES.CONSENT_CARD
    /** 同意卡业务数据。 */
    data: ConsentCardData
  }

  /** 历史消息中的选择卡片结构。 */
  export interface HistorySelectionCard extends HistoryCard {
    /** 卡片类型标识。 */
    type: typeof ASSISTANT_CARD_TYPES.SELECTION_CARD
    /** 选择卡业务数据。 */
    data: SelectionCardData
  }

  /** 历史消息中的 consultation 问卷卡片结构。 */
  export interface HistoryConsultationQuestionnaireCard extends HistoryCard {
    /** 卡片类型标识。 */
    type: typeof ASSISTANT_CARD_TYPES.CONSULTATION_QUESTIONNAIRE_CARD
    /** consultation 问卷卡业务数据。 */
    data: ConsultationQuestionnaireCardData
  }

  /** 发送消息时携带的卡片动作载荷。 */
  export interface CardActionPayload {
    /** 动作类型。 */
    type: string
    /** 卡片所属消息 ID。 */
    messageId: string
    /** 被点击卡片 ID。 */
    cardUuid: string
    /** 被点击卡片类型。 */
    cardType?: string
    /** 被点击卡片场景标识。 */
    cardScene?: string
    /** 被点击卡片标题。 */
    cardTitle?: string
    /** 被点击按钮的结构化动作编码。 */
    action?: string
  }

  /** SSE/实时消息使用的规范化消息结构。 */
  export interface Message {
    /** 消息唯一标识。 */
    id: string
    /** 独立卡片事件所属消息 ID。 */
    sourceMessageId?: string
    /** 卡片唯一标识。 */
    cardUuid?: string
    /** 消息发送方角色。 */
    role: Role
    /** 消息类型。 */
    messageType: MessageType
    /** 文本消息正文。 */
    content?: string
    /** 卡片消息载荷。 */
    card?: CardPayload
    /** AI 思考过程文本。 */
    thinking?: string
    /** 消息最终状态。 */
    status?: MessageStatus
    /** 消息所属会话 UUID。 */
    conversationUuid?: string
    /** 消息创建时间。 */
    createdAt?: string
  }

  /** 历史接口使用的规范化消息结构。 */
  export interface HistoryMessage {
    /** 消息唯一标识。 */
    id: string
    /** 消息发送方角色。 */
    role: Role
    /** 文本消息正文。 */
    content: string
    /** AI 思考过程文本。 */
    thinking?: string
    /** 消息最终状态。 */
    status?: MessageStatus
    /** 历史消息中的卡片数组。 */
    cards?: HistoryCard[]
    /** 消息所属会话 UUID。 */
    conversationUuid?: string
    /** 消息创建时间。 */
    createdAt?: string
  }

  /** SSE 流式事件中的内容数据（已规范化） */
  export interface StreamContent {
    /** 本次增量文本内容。 */
    text?: string
    /** 当前节点标识。 */
    node?: string
    /** 父节点标识。 */
    parentNode?: string | null
    /** 当前节点状态。 */
    state?: string | null
    /** 后端附带的提示消息。 */
    message?: string | null
    /** 工具执行或节点结果。 */
    result?: string | null
    /** 工具名或节点名。 */
    name?: string | null
    /** 工具调用参数。 */
    arguments?: string | null
  }

  /** SSE 流式事件中的元数据（已规范化） */
  export interface StreamMeta {
    /** 当前流所属会话 UUID。 */
    conversationUuid?: string
    /** 当前 AI 消息 UUID。 */
    messageUuid?: string
    /** 独立卡片事件对应的卡片 UUID。 */
    cardUuid?: string
    /** 当前运行状态。 */
    runStatus?: MessageStatus | string
    /** 当前事件是否属于快照恢复。 */
    snapshot?: boolean
    /** 当前事件是否要求前端执行 replace 覆盖。 */
    replace?: boolean
  }

  /** SSE 流式事件中的 action 指令（已规范化） */
  export interface StreamAction {
    /** 动作类型。 */
    type: string
    /** 动作目标。 */
    target: string
    /** 动作业务参数。 */
    payload?: Record<string, unknown>
    /** 动作优先级，数值越小优先级越高。 */
    priority?: number
  }

  /** SSE 流式推送的完整事件（已规范化） */
  export interface StreamEvent {
    /** 事件类型。 */
    type: StreamEventType
    /** 文本增量或工具节点内容。 */
    content?: StreamContent
    /** 事件元信息。 */
    meta?: StreamMeta
    /** 需要前端执行的动作。 */
    action?: StreamAction
    /** 直接下发的卡片数据。 */
    card?: CardPayload
    /** 单条持久化消息。 */
    message?: Message
    /** 多条持久化消息。 */
    messages?: Message[]
    /** 当前事件是否为本次流的结束事件。 */
    isEnd: boolean
    /** 事件时间戳。 */
    timestamp?: number
  }
}
