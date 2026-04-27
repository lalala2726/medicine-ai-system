import {
  ASSISTANT_CARD_TYPES,
  ASSISTANT_MESSAGE_STATUSES,
  ASSISTANT_ROLES,
  type AssistantTypes
} from '@/api/assistant/contract'
import {
  CHAT_MESSAGE_POSITIONS,
  CHAT_MESSAGE_TYPES,
  CHAT_RESPONSE_STATUSES,
  type AfterSaleCardContent,
  type ChatMessage,
  type ConsultProductDisplayItem,
  type ConsentActionContent,
  type OrderCardContent,
  type PatientCardContent,
  type ProductDisplayItem,
  type PurchaseProductDisplayItem,
  type SelectionOptionContent
} from '../messages/chatTypes'
import { ASSISTANT_UNSUPPORTED_MESSAGE_TEXT } from '../page/assistantPage.constants'

/** 历史专用消息来源标识，用于页面层分流渲染器。 */
export const HISTORY_RENDER_SOURCE = 'assistant-history' as const

/** 当前历史渲染支持的卡片类型集合。 */
const SUPPORTED_HISTORY_CARD_TYPES = new Set<string>([
  ASSISTANT_CARD_TYPES.PRODUCT_CARD,
  ASSISTANT_CARD_TYPES.CONSULT_PRODUCT_CARD,
  ASSISTANT_CARD_TYPES.PRODUCT_PURCHASE_CARD,
  ASSISTANT_CARD_TYPES.ORDER_CARD,
  ASSISTANT_CARD_TYPES.AFTER_SALE_CARD,
  ASSISTANT_CARD_TYPES.PATIENT_CARD,
  ASSISTANT_CARD_TYPES.CONSENT_CARD,
  ASSISTANT_CARD_TYPES.CONSULTATION_QUESTIONNAIRE_CARD,
  ASSISTANT_CARD_TYPES.SELECTION_CARD
])

/** 历史消息里可渲染的商品卡视图模型。 */
export interface HistoryRenderProductCard {
  /** 历史卡片唯一标识。 */
  cardUuid: string
  /** 卡片类型标识。 */
  type: typeof ASSISTANT_CARD_TYPES.PRODUCT_CARD
  /** 卡片标题。 */
  title?: string
  /** 商品列表。 */
  products: ProductDisplayItem[]
}

/** 历史消息里可渲染的商品咨询卡视图模型。 */
export interface HistoryRenderConsultProductCard {
  /** 历史卡片唯一标识。 */
  cardUuid: string
  /** 卡片类型标识。 */
  type: typeof ASSISTANT_CARD_TYPES.CONSULT_PRODUCT_CARD
  /** 卡片标题。 */
  title?: string
  /** 当前咨询商品。 */
  product: ConsultProductDisplayItem
}

/** 历史消息里可渲染的商品购买卡视图模型。 */
export interface HistoryRenderProductPurchaseCard {
  /** 历史卡片唯一标识。 */
  cardUuid: string
  /** 卡片类型标识。 */
  type: typeof ASSISTANT_CARD_TYPES.PRODUCT_PURCHASE_CARD
  /** 卡片标题。 */
  title?: string
  /** 商品列表。 */
  products: PurchaseProductDisplayItem[]
  /** 后端计算后的总价展示文案。 */
  totalPrice: string
}

/** 历史消息里可渲染的订单卡视图模型。 */
export interface HistoryRenderOrderCard {
  /** 历史卡片唯一标识。 */
  cardUuid: string
  /** 卡片类型标识。 */
  type: typeof ASSISTANT_CARD_TYPES.ORDER_CARD
  /** 订单卡内容。 */
  content: OrderCardContent
}

/** 历史消息里可渲染的售后卡视图模型。 */
export interface HistoryRenderAfterSaleCard {
  /** 历史卡片唯一标识。 */
  cardUuid: string
  /** 卡片类型标识。 */
  type: typeof ASSISTANT_CARD_TYPES.AFTER_SALE_CARD
  /** 售后卡内容。 */
  content: AfterSaleCardContent
}

/** 历史消息里可渲染的就诊人卡视图模型。 */
export interface HistoryRenderPatientCard {
  /** 历史卡片唯一标识。 */
  cardUuid: string
  /** 卡片类型标识。 */
  type: typeof ASSISTANT_CARD_TYPES.PATIENT_CARD
  /** 就诊人卡内容。 */
  content: PatientCardContent
}

/** 历史消息里可渲染的同意卡视图模型。 */
export interface HistoryRenderConsentCard {
  /** 历史卡片唯一标识。 */
  cardUuid: string
  /** 卡片类型标识。 */
  type: typeof ASSISTANT_CARD_TYPES.CONSENT_CARD
  /** 卡片所属消息 ID。 */
  messageId: string
  /** 卡片场景标识。 */
  scene?: string
  /** 卡片标题。 */
  title?: string
  /** 卡片说明。 */
  description?: string
  /** 同意动作。 */
  confirm: ConsentActionContent
  /** 拒绝动作。 */
  reject: ConsentActionContent
}

/** 历史消息里可渲染的选择卡视图模型。 */
export interface HistoryRenderSelectionCard {
  /** 历史卡片唯一标识。 */
  cardUuid: string
  /** 卡片类型标识。 */
  type: typeof ASSISTANT_CARD_TYPES.SELECTION_CARD
  /** 卡片所属消息 ID。 */
  messageId: string
  /** 卡片场景标识。 */
  scene?: string
  /** 卡片标题。 */
  title?: string
  /** 卡片说明。 */
  description?: string
  /** 单选/多选模式。 */
  selectionMode: 'single' | 'multiple'
  /** 提交按钮文案。 */
  submitText?: string
  /** 是否允许用户补充自定义输入。 */
  allowCustomInput: boolean
  /** 自定义输入框占位文案。 */
  customInputPlaceholder?: string
  /** 可选项列表。 */
  options: SelectionOptionContent[]
}

/** 历史消息里可渲染的 consultation 问卷卡视图模型。 */
export interface HistoryRenderConsultationQuestionnaireCard {
  /** 历史卡片唯一标识。 */
  cardUuid: string
  /** 卡片类型标识。 */
  type: typeof ASSISTANT_CARD_TYPES.CONSULTATION_QUESTIONNAIRE_CARD
  /** 卡片所属消息 ID。 */
  messageId: string
  /** 问卷问题列表。 */
  questions: Array<{
    question: string
    options: SelectionOptionContent[]
  }>
}

/** 历史消息中支持渲染的卡片视图模型联合类型。 */
export type HistoryRenderCard =
  | HistoryRenderProductCard
  | HistoryRenderConsultProductCard
  | HistoryRenderProductPurchaseCard
  | HistoryRenderOrderCard
  | HistoryRenderAfterSaleCard
  | HistoryRenderPatientCard
  | HistoryRenderConsentCard
  | HistoryRenderConsultationQuestionnaireCard
  | HistoryRenderSelectionCard

/** 历史消息专用视图模型。 */
export interface HistoryRenderMessage extends ChatMessage {
  /** 标记该消息需要走历史专用渲染器。 */
  renderSource: typeof HISTORY_RENDER_SOURCE
  /** 历史消息里的卡片列表。 */
  historyCards?: HistoryRenderCard[]
}

/** 当前历史消息里支持渲染的卡片类型。 */
type SupportedHistoryCard =
  | AssistantTypes.HistoryProductCard
  | AssistantTypes.HistoryConsultProductCard
  | AssistantTypes.HistoryProductPurchaseCard
  | AssistantTypes.HistoryOrderCard
  | AssistantTypes.HistoryAfterSaleCard
  | AssistantTypes.HistoryPatientCard
  | AssistantTypes.HistoryConsentCard
  | AssistantTypes.HistoryConsultationQuestionnaireCard
  | AssistantTypes.HistorySelectionCard

/**
 * 将后端历史消息状态映射为页面层响应状态。
 *
 * @param status - 历史消息状态
 * @returns 页面层响应状态
 */
const resolveHistoryResponseStatus = (status?: AssistantTypes.HistoryMessage['status']) => {
  if (status === ASSISTANT_MESSAGE_STATUSES.STREAMING) {
    return CHAT_RESPONSE_STATUSES.STREAMING
  }

  if (status === ASSISTANT_MESSAGE_STATUSES.CANCELLED) {
    return CHAT_RESPONSE_STATUSES.CANCELLED
  }

  if (status === ASSISTANT_MESSAGE_STATUSES.ERROR) {
    return CHAT_RESPONSE_STATUSES.ERROR
  }

  return CHAT_RESPONSE_STATUSES.SUCCESS
}

/**
 * 将历史消息状态转换成可展示的状态文案。
 *
 * @param status - 历史消息状态
 * @returns 可展示文案，无需展示时返回 undefined
 */
const resolveHistoryResponseStatusText = (status?: AssistantTypes.HistoryMessage['status']) => {
  if (status === ASSISTANT_MESSAGE_STATUSES.CANCELLED) {
    return '已停止生成'
  }

  if (status === ASSISTANT_MESSAGE_STATUSES.ERROR) {
    return '这条回复未完整生成'
  }

  return undefined
}

/**
 * 判断某张历史卡片是否为当前版本支持的卡片类型。
 *
 * @param card - 历史卡片对象
 * @returns 是否为支持渲染的卡片
 */
const isSupportedHistoryCard = (card: AssistantTypes.HistoryCard): card is SupportedHistoryCard => {
  return SUPPORTED_HISTORY_CARD_TYPES.has(card.type)
}

/**
 * 将历史商品卡转换成页面渲染使用的结构。
 * 这里只保留当前商品卡组件真正需要的字段。
 *
 * @param card - 历史商品卡对象
 * @returns 可渲染的商品卡，无效时返回 null
 */
const mapHistoryProductCardToRenderCard = (
  card: AssistantTypes.HistoryProductCard
): HistoryRenderProductCard | null => {
  return {
    cardUuid: card.cardUuid,
    type: ASSISTANT_CARD_TYPES.PRODUCT_CARD,
    title: card.data.title,
    products: card.data.products.map(product => ({
      id: product.id,
      name: product.name,
      image: product.image,
      price: product.price
    }))
  }
}

/**
 * 将历史商品咨询卡转换成页面渲染使用的结构。
 *
 * @param card - 历史商品咨询卡对象
 * @returns 可渲染的商品咨询卡，无效时返回 null
 */
const mapHistoryConsultProductCardToRenderCard = (
  card: AssistantTypes.HistoryConsultProductCard
): HistoryRenderConsultProductCard | null => {
  return {
    cardUuid: card.cardUuid,
    type: ASSISTANT_CARD_TYPES.CONSULT_PRODUCT_CARD,
    title: card.data.title,
    product: {
      id: card.data.product.id,
      name: card.data.product.name,
      image: card.data.product.image,
      price: card.data.product.price
    }
  }
}

/**
 * 将历史商品购买卡转换成页面渲染使用的结构。
 * 总价和数量均直接展示服务端下发值，不做本地计算。
 *
 * @param card - 历史商品购买卡对象
 * @returns 可渲染的商品购买卡，无效时返回 null
 */
const mapHistoryProductPurchaseCardToRenderCard = (
  card: AssistantTypes.HistoryProductPurchaseCard
): HistoryRenderProductPurchaseCard | null => {
  if (card.data.products.length === 0 || !card.data.totalPrice.trim()) {
    return null
  }

  return {
    cardUuid: card.cardUuid,
    type: ASSISTANT_CARD_TYPES.PRODUCT_PURCHASE_CARD,
    title: card.data.title,
    totalPrice: card.data.totalPrice,
    products: card.data.products.map(product => ({
      id: product.id,
      name: product.name,
      image: product.image,
      price: product.price,
      quantity: product.quantity
    }))
  }
}

/**
 * 将历史订单卡转换成页面渲染使用的结构。
 *
 * @param card - 历史订单卡对象
 * @returns 可渲染的订单卡，无效时返回 null
 */
const mapHistoryOrderCardToRenderCard = (card: AssistantTypes.HistoryOrderCard): HistoryRenderOrderCard | null => {
  return {
    cardUuid: card.cardUuid,
    type: ASSISTANT_CARD_TYPES.ORDER_CARD,
    content: {
      orderNo: card.data.orderNo,
      orderStatus: card.data.orderStatus,
      orderStatusText: card.data.orderStatusText,
      previewProduct: {
        productId: card.data.previewProduct.productId,
        productName: card.data.previewProduct.productName,
        imageUrl: card.data.previewProduct.imageUrl
      },
      productCount: card.data.productCount,
      payAmount: card.data.payAmount,
      totalAmount: card.data.totalAmount,
      createTime: card.data.createTime
    }
  }
}

/**
 * 将历史售后卡转换成页面渲染使用的结构。
 *
 * @param card - 历史售后卡对象
 * @returns 可渲染的售后卡，无效时返回 null
 */
const mapHistoryAfterSaleCardToRenderCard = (
  card: AssistantTypes.HistoryAfterSaleCard
): HistoryRenderAfterSaleCard | null => {
  return {
    cardUuid: card.cardUuid,
    type: ASSISTANT_CARD_TYPES.AFTER_SALE_CARD,
    content: {
      afterSaleNo: card.data.afterSaleNo,
      orderNo: card.data.orderNo,
      afterSaleType: card.data.afterSaleType,
      afterSaleTypeText: card.data.afterSaleTypeText,
      afterSaleStatus: card.data.afterSaleStatus,
      afterSaleStatusText: card.data.afterSaleStatusText,
      refundAmount: card.data.refundAmount,
      applyReasonName: card.data.applyReasonName,
      applyTime: card.data.applyTime,
      productInfo: {
        productName: card.data.productInfo.productName,
        productImage: card.data.productInfo.productImage
      }
    }
  }
}

/**
 * 将历史就诊人卡转换成页面渲染使用的结构。
 *
 * @param card - 历史就诊人卡对象
 * @returns 可渲染的就诊人卡，无效时返回 null
 */
const mapHistoryPatientCardToRenderCard = (
  card: AssistantTypes.HistoryPatientCard
): HistoryRenderPatientCard | null => {
  return {
    cardUuid: card.cardUuid,
    type: ASSISTANT_CARD_TYPES.PATIENT_CARD,
    content: {
      patientId: card.data.patientId,
      name: card.data.name,
      gender: card.data.gender,
      genderText: card.data.genderText,
      birthDate: card.data.birthDate,
      relationship: card.data.relationship,
      isDefault: card.data.isDefault,
      allergy: card.data.allergy,
      pastMedicalHistory: card.data.pastMedicalHistory,
      chronicDisease: card.data.chronicDisease,
      longTermMedications: card.data.longTermMedications
    }
  }
}

/**
 * 将历史同意卡转换成页面渲染使用的结构。
 *
 * @param card - 历史同意卡对象
 * @param messageId - 卡片所属消息 ID
 * @returns 可渲染的同意卡
 */
const mapHistoryConsentCardToRenderCard = (
  card: AssistantTypes.HistoryConsentCard,
  messageId: string
): HistoryRenderConsentCard => {
  return {
    cardUuid: card.cardUuid,
    type: ASSISTANT_CARD_TYPES.CONSENT_CARD,
    messageId,
    scene: card.data.scene,
    title: card.data.title,
    description: card.data.description,
    confirm: {
      action: 'confirm',
      label: card.data.confirm.label,
      value: card.data.confirm.value
    },
    reject: {
      action: 'reject',
      label: card.data.reject.label,
      value: card.data.reject.value
    }
  }
}

/**
 * 将历史选择卡转换成页面渲染使用的结构。
 *
 * @param card - 历史选择卡对象
 * @param messageId - 卡片所属消息 ID
 * @returns 可渲染的选择卡，无效时返回 null
 */
const mapHistorySelectionCardToRenderCard = (
  card: AssistantTypes.HistorySelectionCard,
  messageId: string
): HistoryRenderSelectionCard | null => {
  if (card.data.options.length === 0) {
    return null
  }

  return {
    cardUuid: card.cardUuid,
    type: ASSISTANT_CARD_TYPES.SELECTION_CARD,
    messageId,
    scene: card.data.scene,
    title: card.data.title,
    selectionMode: 'multiple',
    submitText: '发送',
    allowCustomInput: true,
    options: card.data.options.map(option => ({
      id: option,
      label: option,
      value: option
    }))
  }
}

/**
 * 将历史 consultation 问卷卡转换成页面渲染使用的结构。
 *
 * @param card - 历史 consultation 问卷卡对象
 * @param messageId - 卡片所属消息 ID
 * @returns 可渲染的 consultation 问卷卡，无效时返回 null
 */
const mapHistoryConsultationQuestionnaireCardToRenderCard = (
  card: AssistantTypes.HistoryConsultationQuestionnaireCard,
  messageId: string
): HistoryRenderConsultationQuestionnaireCard | null => {
  if (card.data.questions.length === 0) {
    return null
  }

  return {
    cardUuid: card.cardUuid,
    type: ASSISTANT_CARD_TYPES.CONSULTATION_QUESTIONNAIRE_CARD,
    messageId,
    questions: card.data.questions.map((questionItem, index) => ({
      question: questionItem.question,
      options: questionItem.options.map(option => ({
        id: `${index}-${option}`,
        label: option,
        value: option
      }))
    }))
  }
}

/**
 * 将支持的历史卡片映射为页面层可渲染的卡片视图模型。
 *
 * @param card - 支持的历史卡片对象
 * @returns 历史卡片视图模型，无效时返回 null
 */
const mapHistoryCardToRenderCard = (card: SupportedHistoryCard, messageId: string): HistoryRenderCard | null => {
  switch (card.type) {
    case ASSISTANT_CARD_TYPES.PRODUCT_CARD:
      return mapHistoryProductCardToRenderCard(card)
    case ASSISTANT_CARD_TYPES.CONSULT_PRODUCT_CARD:
      return mapHistoryConsultProductCardToRenderCard(card)
    case ASSISTANT_CARD_TYPES.PRODUCT_PURCHASE_CARD:
      return mapHistoryProductPurchaseCardToRenderCard(card)
    case ASSISTANT_CARD_TYPES.ORDER_CARD:
      return mapHistoryOrderCardToRenderCard(card)
    case ASSISTANT_CARD_TYPES.AFTER_SALE_CARD:
      return mapHistoryAfterSaleCardToRenderCard(card)
    case ASSISTANT_CARD_TYPES.PATIENT_CARD:
      return mapHistoryPatientCardToRenderCard(card)
    case ASSISTANT_CARD_TYPES.CONSENT_CARD:
      return mapHistoryConsentCardToRenderCard(card, messageId)
    case ASSISTANT_CARD_TYPES.CONSULTATION_QUESTIONNAIRE_CARD:
      return mapHistoryConsultationQuestionnaireCardToRenderCard(card, messageId)
    case ASSISTANT_CARD_TYPES.SELECTION_CARD:
      return mapHistorySelectionCardToRenderCard(card, messageId)
    default:
      return null
  }
}

/**
 * 判断历史消息是否已经具备可展示的文本区域。
 *
 * @param message - 历史专用视图模型
 * @returns 是否存在可展示文本
 */
const hasRenderableHistoryText = (message: HistoryRenderMessage) => {
  return Boolean(
    message.content.text?.trim() || message.content.thinking?.trim() || message.content.responseStatusText?.trim()
  )
}

/**
 * 将规范化后的历史消息转换成页面层历史专用视图模型。
 *
 * @param options.message - 历史消息对象
 * @param options.assistantAvatar - 助手头像
 * @param options.userAvatar - 用户头像
 * @returns 历史专用消息模型
 */
export const mapHistoryMessageToRenderMessage = ({
  message,
  assistantAvatar,
  userAvatar
}: {
  message: AssistantTypes.HistoryMessage
  assistantAvatar: string
  userAvatar: string
}): HistoryRenderMessage | null => {
  const isUser = message.role === ASSISTANT_ROLES.USER
  const originalCards = message.cards ?? []
  const historyCards = originalCards
    .filter(isSupportedHistoryCard)
    .map(card => mapHistoryCardToRenderCard(card, message.id))
    .filter((item): item is HistoryRenderCard => item !== null)

  const historyMessage: HistoryRenderMessage = {
    _id: message.id,
    messageId: message.id,
    type: CHAT_MESSAGE_TYPES.TEXT,
    content: {
      text: message.content,
      thinking: !isUser ? message.thinking : undefined,
      thinkingDone: !isUser ? message.status !== ASSISTANT_MESSAGE_STATUSES.STREAMING : undefined,
      responseStatus: !isUser ? resolveHistoryResponseStatus(message.status) : undefined,
      responseStatusText: !isUser ? resolveHistoryResponseStatusText(message.status) : undefined
    },
    position: isUser ? CHAT_MESSAGE_POSITIONS.RIGHT : undefined,
    user: {
      avatar: isUser ? userAvatar : assistantAvatar
    },
    renderSource: HISTORY_RENDER_SOURCE,
    historyCards: historyCards.length > 0 ? historyCards : undefined
  }

  if (hasRenderableHistoryText(historyMessage) || historyCards.length > 0) {
    return historyMessage
  }

  return {
    ...historyMessage,
    content: {
      text: ASSISTANT_UNSUPPORTED_MESSAGE_TEXT
    }
  }
}

/**
 * 判断某条消息是否应交给历史专用渲染器处理。
 *
 * @param message - 待检测的消息对象
 * @returns 是否为历史专用消息
 */
export const isHistoryRenderMessage = (message: unknown): message is HistoryRenderMessage => {
  return Boolean(
    message &&
      typeof message === 'object' &&
      'renderSource' in message &&
      (message as { renderSource?: string }).renderSource === HISTORY_RENDER_SOURCE
  )
}
