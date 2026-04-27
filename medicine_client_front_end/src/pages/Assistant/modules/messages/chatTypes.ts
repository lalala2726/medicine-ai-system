/** 提取对象值联合类型的辅助类型。 */
type ValueOf<T> = T[keyof T]

/** Assistant 页面支持的消息类型常量。 */
export const CHAT_MESSAGE_TYPES = {
  /** 系统提示消息。 */
  SYSTEM: 'system',
  /** 纯文本消息。 */
  TEXT: 'text',
  /** AI 正在输入中的临时占位消息。 */
  TYPING: 'typing',
  /** 图片消息。 */
  IMAGE: 'image',
  /** 商品推荐卡消息。 */
  PRODUCT_CARD: 'product-card',
  /** 商品咨询卡消息。 */
  CONSULT_PRODUCT_CARD: 'consult-product-card',
  /** 商品购买确认卡消息。 */
  PRODUCT_PURCHASE_CARD: 'product-purchase-card',
  /** 同意/拒绝卡消息。 */
  CONSENT_CARD: 'consent-card',
  /** consultation 专用问卷卡消息。 */
  CONSULTATION_QUESTIONNAIRE_CARD: 'consultation-questionnaire-card',
  /** 选项选择卡消息。 */
  SELECTION_CARD: 'selection-card',
  /** 订单卡消息。 */
  ORDER_CARD: 'order-card',
  /** 售后卡消息。 */
  AFTER_SALE_CARD: 'after-sale-card',
  /** 就诊人卡消息。 */
  PATIENT_CARD: 'patient-card'
} as const

/** Assistant 页面支持的回复状态常量。 */
export const CHAT_RESPONSE_STATUSES = {
  /** 回复仍在流式生成中。 */
  STREAMING: 'streaming',
  /** 回复已成功完成。 */
  SUCCESS: 'success',
  /** 回复已被用户主动停止。 */
  CANCELLED: 'cancelled',
  /** 回复以错误状态结束。 */
  ERROR: 'error',
  /** 本地 attach 连接异常中断。 */
  INTERRUPTED: 'interrupted'
} as const

/** Assistant 消息在页面中的布局位置常量。 */
export const CHAT_MESSAGE_POSITIONS = {
  /** 左侧消息布局。 */
  LEFT: 'left',
  /** 右侧消息布局。 */
  RIGHT: 'right',
  /** 居中消息布局。 */
  CENTER: 'center'
} as const

/** 聊天消息类型联合类型。 */
export type ChatMessageType = ValueOf<typeof CHAT_MESSAGE_TYPES>
/** 聊天回复状态联合类型。 */
export type ChatResponseStatus = ValueOf<typeof CHAT_RESPONSE_STATUSES>
/** 聊天消息位置联合类型。 */
export type ChatMessagePosition = ValueOf<typeof CHAT_MESSAGE_POSITIONS>

/**
 * Assistant 聊天渲染层共享类型
 */
export interface ProductDisplayItem {
  /** 商品ID */
  id: string
  /** 商品名称 */
  name: string
  /** 商品图片 */
  image: string
  /** 商品价格 */
  price: string
  /** 商品标签 */
  tags?: string[]
}

/**
 * 商品咨询卡中的商品展示类型
 */
export interface ConsultProductDisplayItem {
  /** 商品ID */
  id: string
  /** 商品名称 */
  name: string
  /** 商品图片 */
  image: string
  /** 商品价格 */
  price: string
}

/**
 * 商品咨询卡片内容视图模型
 */
export interface ConsultProductCardContent {
  title?: string
  product: ConsultProductDisplayItem
}

/**
 * 购买确认卡片中的商品展示类型
 */
export interface PurchaseProductDisplayItem {
  /** 商品ID */
  id: string
  /** 商品名称 */
  name: string
  /** 商品图片 */
  image: string
  /** 商品价格 */
  price: string
  /** 购买数量 */
  quantity: number
}

/**
 * 商品购买卡片内容视图模型
 */
export interface ProductPurchaseCardContent {
  title?: string
  products: PurchaseProductDisplayItem[]
  totalPrice: string
}

/**
 * 同意卡动作视图模型
 */
export interface ConsentActionContent {
  action: 'confirm' | 'reject'
  label: string
  value: string
}

/**
 * 同意卡内容视图模型
 */
export interface ConsentCardContent {
  scene?: string
  title?: string
  description?: string
  confirm: ConsentActionContent
  reject: ConsentActionContent
}

/**
 * 选择卡中的选项视图模型
 */
export interface SelectionOptionContent {
  id: string
  label: string
  value: string
}

/**
 * 选择卡内容视图模型
 */
export interface SelectionCardContent {
  scene?: string
  title?: string
  description?: string
  selectionMode: 'single' | 'multiple'
  submitText?: string
  allowCustomInput: boolean
  customInputPlaceholder?: string
  options: SelectionOptionContent[]
}

/**
 * consultation 问卷卡中的单个问题视图模型
 */
export interface ConsultationQuestionnaireQuestionContent {
  question: string
  options: SelectionOptionContent[]
}

/**
 * consultation 问卷卡内容视图模型
 */
export interface ConsultationQuestionnaireCardContent {
  questions: ConsultationQuestionnaireQuestionContent[]
}

/**
 * 订单卡首个商品预览视图模型
 */
export interface OrderCardPreviewProductContent {
  /** 商品 ID */
  productId: string
  /** 商品名称 */
  productName: string
  /** 商品图片 */
  imageUrl: string
}

/**
 * 订单卡内容视图模型
 */
export interface OrderCardContent {
  /** 订单号 */
  orderNo: string
  /** 订单状态枚举值 */
  orderStatus: string
  /** 订单状态展示文案 */
  orderStatusText: string
  /** 首个商品预览 */
  previewProduct: OrderCardPreviewProductContent
  /** 商品总数 */
  productCount: number
  /** 实付金额 */
  payAmount: string
  /** 订单总金额 */
  totalAmount: string
  /** 下单时间 */
  createTime: string
}

/**
 * 售后卡商品信息视图模型
 */
export interface AfterSaleCardProductInfoContent {
  /** 商品名称 */
  productName: string
  /** 商品图片 */
  productImage: string
}

/**
 * 售后卡内容视图模型
 */
export interface AfterSaleCardContent {
  /** 售后单号 */
  afterSaleNo: string
  /** 订单编号 */
  orderNo: string
  /** 售后类型编码 */
  afterSaleType: string
  /** 售后类型展示文案 */
  afterSaleTypeText: string
  /** 售后状态编码 */
  afterSaleStatus: string
  /** 售后状态展示文案 */
  afterSaleStatusText: string
  /** 退款金额 */
  refundAmount: string
  /** 申请原因名称 */
  applyReasonName: string
  /** 申请时间 */
  applyTime: string
  /** 售后商品信息 */
  productInfo: AfterSaleCardProductInfoContent
}

/**
 * 就诊人卡内容视图模型
 */
export interface PatientCardContent {
  /** 就诊人 ID */
  patientId: string
  /** 就诊人姓名 */
  name: string
  /** 性别编码 */
  gender: number
  /** 性别展示文案 */
  genderText: string
  /** 出生日期 */
  birthDate: string
  /** 与账户关系 */
  relationship: string
  /** 是否默认就诊人 */
  isDefault: boolean
  /** 过敏史 */
  allergy?: string
  /** 既往病史 */
  pastMedicalHistory?: string
  /** 慢性病信息 */
  chronicDisease?: string
  /** 长期用药 */
  longTermMedications?: string
}

/**
 * 工具运行中状态视图模型。
 */
export interface ChatMessageToolStatusContent {
  /** 当前展示给用户的中文状态文案。 */
  text: string
  /** 当前工具状态阶段。 */
  phase: 'running'
}

/**
 * 聊天消息内容视图模型
 */
export interface ChatMessageContent {
  text?: string
  thinking?: string
  thinkingDone?: boolean
  responseStatus?: ChatResponseStatus
  responseStatusText?: string
  toolStatus?: ChatMessageToolStatusContent
  picUrl?: string
  /** 用户发送的图片 URL 列表。 */
  imageUrls?: string[]
  title?: string
  products?: ProductDisplayItem[]
  consultProductCard?: ConsultProductCardContent
  purchaseCard?: ProductPurchaseCardContent
  consentCard?: ConsentCardContent
  consultationQuestionnaireCard?: ConsultationQuestionnaireCardContent
  selectionCard?: SelectionCardContent
  orderCard?: OrderCardContent
  afterSaleCard?: AfterSaleCardContent
  patientCard?: PatientCardContent
}

/**
 * Assistant 页面消息数据模型。
 */
export interface ChatMessage {
  _id?: string
  messageId?: string
  cardUuid?: string
  type: ChatMessageType
  content: ChatMessageContent
  position?: ChatMessagePosition
  user?: {
    avatar?: string
    name?: string
  }
  /** 标记当前卡片消息正在播放退出动画。 */
  _exiting?: boolean
}

/**
 * 同意卡提交时回调给页面层的载荷。
 */
export interface ConsentCardSubmitPayload {
  localMessageId: string
  messageId: string
  cardUuid: string
  cardType: typeof CHAT_MESSAGE_TYPES.CONSENT_CARD
  cardScene?: string
  cardTitle?: string
  action: 'confirm' | 'reject'
  label: string
  value: string
}

/**
 * 选择卡提交时回调给页面层的载荷。
 */
export interface SelectionCardSubmitPayload {
  localMessageId: string
  messageId: string
  cardUuid: string
  cardType: typeof CHAT_MESSAGE_TYPES.SELECTION_CARD
  cardScene?: string
  cardTitle?: string
  selectionMode: 'single' | 'multiple'
  selectedOptions: SelectionOptionContent[]
  customInput: string
}

/**
 * consultation 问卷卡单题答案载荷。
 */
export interface ConsultationQuestionnaireAnswerPayload {
  question: string
  selectedOptions: SelectionOptionContent[]
}

/**
 * consultation 问卷卡提交时回调给页面层的载荷。
 */
export interface ConsultationQuestionnaireCardSubmitPayload {
  localMessageId: string
  messageId: string
  cardUuid: string
  answers: ConsultationQuestionnaireAnswerPayload[]
}

/**
 * 消息内容渲染器的回调函数接口
 */
export interface MessageContentCallbacks {
  /** 推荐商品卡点击回调。 */
  onProductClick?: (product: ProductDisplayItem) => void
  /** 推荐商品卡“查看”按钮点击回调。 */
  onBuyClick?: (product: ProductDisplayItem) => void
  /** 商品咨询卡点击回调。 */
  onConsultProductClick?: (product: ConsultProductDisplayItem) => void
  onConsentSubmit?: (payload: ConsentCardSubmitPayload) => void
  onConsultationQuestionnaireSubmit?: (payload: ConsultationQuestionnaireCardSubmitPayload) => void
  onSelectionSubmit?: (payload: SelectionCardSubmitPayload) => void
  /** 商品购买卡"立即购买"按钮点击回调。 */
  onPurchaseClick?: (products: PurchaseProductDisplayItem[]) => void
  /** 卡片退出动画完成后的清理回调。 */
  onCardExitComplete?: (localMessageId: string) => void
}
