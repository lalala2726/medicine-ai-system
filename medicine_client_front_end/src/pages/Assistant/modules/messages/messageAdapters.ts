import {
  ASSISTANT_CARD_TYPES,
  ASSISTANT_MESSAGE_STATUSES,
  ASSISTANT_MESSAGE_TYPES,
  ASSISTANT_ROLES,
  type AssistantTypes
} from '@/api/assistant/contract'
import {
  CHAT_MESSAGE_POSITIONS,
  CHAT_MESSAGE_TYPES,
  CHAT_RESPONSE_STATUSES,
  type ChatMessage,
  type ChatMessageContent,
  type ChatMessageType
} from './chatTypes'

/** 当前支持渲染的卡片类型集合 */
const SUPPORTED_CARD_TYPES = new Set<AssistantTypes.CardType>([
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

/**
 * 构建推荐商品卡的聊天内容。
 *
 * @param card - 商品推荐卡载荷
 * @returns 构建后的聊天内容
 */
const buildProductCardContent = (card: AssistantTypes.ProductCardPayload): ChatMessageContent => {
  return {
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
 * 构建商品咨询卡的聊天内容。
 *
 * @param card - 商品咨询卡载荷
 * @returns 构建后的聊天内容
 */
const buildConsultProductCardContent = (card: AssistantTypes.ConsultProductCardPayload): ChatMessageContent => {
  return {
    consultProductCard: {
      title: card.data.title,
      product: {
        id: card.data.product.id,
        name: card.data.product.name,
        image: card.data.product.image,
        price: card.data.product.price
      }
    }
  }
}

/**
 * 构建商品购买卡的聊天内容。
 *
 * @param card - 商品购买卡载荷
 * @returns 构建后的聊天内容
 */
const buildProductPurchaseCardContent = (card: AssistantTypes.ProductPurchaseCardPayload): ChatMessageContent => {
  return {
    purchaseCard: {
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
}

/**
 * 构建订单卡的聊天内容。
 *
 * @param card - 订单卡载荷
 * @returns 构建后的聊天内容
 */
const buildOrderCardContent = (card: AssistantTypes.OrderCardPayload): ChatMessageContent => {
  return {
    orderCard: {
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
 * 构建售后卡的聊天内容。
 *
 * @param card - 售后卡载荷
 * @returns 构建后的聊天内容
 */
const buildAfterSaleCardContent = (card: AssistantTypes.AfterSaleCardPayload): ChatMessageContent => {
  return {
    afterSaleCard: {
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
 * 构建就诊人卡的聊天内容。
 *
 * @param card - 就诊人卡载荷
 * @returns 构建后的聊天内容
 */
const buildPatientCardContent = (card: AssistantTypes.PatientCardPayload): ChatMessageContent => {
  return {
    patientCard: {
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
 * 构建同意卡的聊天内容。
 *
 * @param card - 同意卡载荷
 * @returns 构建后的聊天内容
 */
const buildConsentCardContent = (card: AssistantTypes.ConsentCardPayload): ChatMessageContent => {
  return {
    consentCard: {
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
}

/**
 * 构建选择卡的聊天内容。
 *
 * @param card - 选择卡载荷
 * @returns 构建后的聊天内容
 */
const buildSelectionCardContent = (card: AssistantTypes.SelectionCardPayload): ChatMessageContent => {
  return {
    selectionCard: {
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
}

/**
 * 构建 consultation 问卷卡的聊天内容。
 *
 * @param card - consultation 问卷卡载荷
 * @returns 构建后的聊天内容
 */
const buildConsultationQuestionnaireCardContent = (
  card: AssistantTypes.ConsultationQuestionnaireCardPayload
): ChatMessageContent => {
  return {
    consultationQuestionnaireCard: {
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
}

/**
 * 根据卡片类型和数据构建聊天内容。
 * 当前支持商品、同意、consultation 追问以及通用选择卡协议。
 *
 * @param card - 卡片载荷数据
 * @returns 构建后的聊天内容，不支持的类型返回 null
 */
const buildCardContent = (card: AssistantTypes.CardPayload): ChatMessageContent | null => {
  switch (card.type) {
    case ASSISTANT_CARD_TYPES.PRODUCT_CARD:
      if (card.data.products.length === 0) {
        return null
      }

      return buildProductCardContent(card)
    case ASSISTANT_CARD_TYPES.CONSULT_PRODUCT_CARD:
      return buildConsultProductCardContent(card)
    case ASSISTANT_CARD_TYPES.PRODUCT_PURCHASE_CARD:
      if (card.data.products.length === 0 || !card.data.totalPrice.trim()) {
        return null
      }

      return buildProductPurchaseCardContent(card)
    case ASSISTANT_CARD_TYPES.ORDER_CARD:
      return buildOrderCardContent(card)
    case ASSISTANT_CARD_TYPES.AFTER_SALE_CARD:
      return buildAfterSaleCardContent(card)
    case ASSISTANT_CARD_TYPES.PATIENT_CARD:
      return buildPatientCardContent(card)
    case ASSISTANT_CARD_TYPES.CONSENT_CARD:
      return buildConsentCardContent(card)
    case ASSISTANT_CARD_TYPES.CONSULTATION_QUESTIONNAIRE_CARD:
      if (card.data.questions.length === 0) {
        return null
      }

      return buildConsultationQuestionnaireCardContent(card)
    case ASSISTANT_CARD_TYPES.SELECTION_CARD:
      if (card.data.options.length === 0) {
        return null
      }

      return buildSelectionCardContent(card)
    default:
      return null
  }
}

/**
 * 将后端卡片类型映射为聊天层的消息类型。
 *
 * @param card - 卡片载荷数据
 * @returns 聊天消息类型，不支持时返回 null
 */
const resolveCardMessageType = (card: AssistantTypes.CardPayload): ChatMessageType | null => {
  switch (card.type) {
    case ASSISTANT_CARD_TYPES.PRODUCT_CARD:
      return CHAT_MESSAGE_TYPES.PRODUCT_CARD
    case ASSISTANT_CARD_TYPES.CONSULT_PRODUCT_CARD:
      return CHAT_MESSAGE_TYPES.CONSULT_PRODUCT_CARD
    case ASSISTANT_CARD_TYPES.PRODUCT_PURCHASE_CARD:
      return CHAT_MESSAGE_TYPES.PRODUCT_PURCHASE_CARD
    case ASSISTANT_CARD_TYPES.ORDER_CARD:
      return CHAT_MESSAGE_TYPES.ORDER_CARD
    case ASSISTANT_CARD_TYPES.AFTER_SALE_CARD:
      return CHAT_MESSAGE_TYPES.AFTER_SALE_CARD
    case ASSISTANT_CARD_TYPES.PATIENT_CARD:
      return CHAT_MESSAGE_TYPES.PATIENT_CARD
    case ASSISTANT_CARD_TYPES.CONSENT_CARD:
      return CHAT_MESSAGE_TYPES.CONSENT_CARD
    case ASSISTANT_CARD_TYPES.CONSULTATION_QUESTIONNAIRE_CARD:
      return CHAT_MESSAGE_TYPES.CONSULTATION_QUESTIONNAIRE_CARD
    case ASSISTANT_CARD_TYPES.SELECTION_CARD:
      return CHAT_MESSAGE_TYPES.SELECTION_CARD
    default:
      return null
  }
}

/**
 * 将后端消息状态映射为前端响应状态。
 *
 * @param status - 后端消息状态
 * @returns 前端响应状态字符串
 */
const resolveAssistantResponseStatus = (status?: AssistantTypes.Message['status']) => {
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
 * 将后端消息状态转换成可展示的状态文案。
 *
 * @param status - 后端消息状态
 * @returns 可展示的状态文案，无需展示时返回 undefined
 */
const resolveAssistantResponseStatusText = (status?: AssistantTypes.Message['status']) => {
  if (status === ASSISTANT_MESSAGE_STATUSES.CANCELLED) {
    return '已停止生成'
  }

  if (status === ASSISTANT_MESSAGE_STATUSES.ERROR) {
    return '这条回复未完整生成'
  }

  return undefined
}

/**
 * 判断卡片类型是否在当前支持的渲染范围内。
 *
 * @param cardType - 卡片类型字符串
 * @returns 是否支持渲染
 */
export const isSupportedAssistantCardType = (cardType?: string | null) => {
  return Boolean(cardType && SUPPORTED_CARD_TYPES.has(cardType as AssistantTypes.CardType))
}

/**
 * 将后端 Assistant 消息转换为页面渲染层的视图模型。
 * 这是协议层到渲染层的唯一翻译边界。
 *
 * @param options.message - 后端消息对象
 * @param options.assistantAvatar - AI 助手头像 URL
 * @param options.userAvatar - 用户头像 URL
 * @returns 转换后的 ChatMessage，不支持的卡片类型返回 null
 */
export const mapAssistantMessageToChatMessage = ({
  message,
  assistantAvatar,
  userAvatar
}: {
  message: AssistantTypes.Message
  assistantAvatar: string
  userAvatar: string
}): ChatMessage | null => {
  const isUser = message.role === ASSISTANT_ROLES.USER

  if (message.messageType === ASSISTANT_MESSAGE_TYPES.CARD) {
    if (!message.card || !isSupportedAssistantCardType(message.card.type)) {
      return null
    }

    const messageType = resolveCardMessageType(message.card)
    const content = buildCardContent(message.card)
    if (!messageType || !content) {
      return null
    }

    const messageId = message.sourceMessageId || message.id
    const localMessageId = message.cardUuid ? `card-${message.cardUuid}` : message.id

    return {
      _id: localMessageId,
      messageId,
      cardUuid: message.cardUuid,
      type: messageType,
      content,
      position: isUser ? CHAT_MESSAGE_POSITIONS.RIGHT : undefined,
      user: {
        avatar: isUser ? userAvatar : assistantAvatar
      }
    }
  }

  return {
    _id: message.id,
    messageId: message.id,
    type: CHAT_MESSAGE_TYPES.TEXT,
    content: {
      text: message.content ?? '',
      thinking: !isUser ? message.thinking : undefined,
      thinkingDone: !isUser ? message.status !== ASSISTANT_MESSAGE_STATUSES.STREAMING : undefined,
      responseStatus: !isUser ? resolveAssistantResponseStatus(message.status) : undefined,
      responseStatusText: !isUser ? resolveAssistantResponseStatusText(message.status) : undefined
    },
    position: isUser ? CHAT_MESSAGE_POSITIONS.RIGHT : undefined,
    user: {
      avatar: isUser ? userAvatar : assistantAvatar
    }
  }
}
