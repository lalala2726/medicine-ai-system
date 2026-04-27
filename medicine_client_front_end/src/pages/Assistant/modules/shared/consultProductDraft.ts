import { ASSISTANT_CARD_TYPES } from '@/api/assistant/contract'
import type { AssistantChatSubmitCardPayload } from '@/api/assistant/agent'

/** 商品咨询草稿卡片默认标题文案。 */
export const ASSISTANT_CONSULT_PRODUCT_CARD_TITLE = '咨询商品'

/** Assistant 页面待发送的商品咨询草稿。 */
export interface AssistantConsultProductDraft {
  /** 商品 ID。 */
  productId: number
  /** 商品名称。 */
  name: string
  /** 商品图片。 */
  image: string
  /** 商品价格展示文案。 */
  price: string
  /** 商品单位。 */
  unit: string
}

/** 从商品详情进入 Assistant 时携带的路由状态。 */
export interface AssistantConsultRouteState {
  /** 当前待发送的商品咨询草稿。 */
  consultProductDraft?: AssistantConsultProductDraft
}

/**
 * 将商品咨询草稿转换为 Assistant submit 使用的商品卡载荷。
 *
 * @param draft - 当前待发送的商品咨询草稿
 * @returns submit 接口所需的商品卡载荷
 */
export const buildConsultProductSubmitCardPayload = (
  draft: AssistantConsultProductDraft
): AssistantChatSubmitCardPayload => {
  return {
    type: ASSISTANT_CARD_TYPES.CONSULT_PRODUCT_CARD,
    data: {
      product_id: draft.productId
    }
  }
}

/**
 * 将商品咨询草稿转换为页面渲染使用的咨询商品卡数据。
 *
 * @param draft - 当前待发送的商品咨询草稿
 * @returns 页面层咨询商品卡数据
 */
export const buildConsultProductCardViewData = (draft: AssistantConsultProductDraft) => {
  return {
    title: ASSISTANT_CONSULT_PRODUCT_CARD_TITLE,
    product: {
      id: String(draft.productId),
      name: draft.name,
      image: draft.image,
      price: draft.price
    }
  }
}

/**
 * 根据商品咨询草稿生成会话标题。
 *
 * @param draft - 当前待发送的商品咨询草稿
 * @returns 商品咨询会话标题
 */
export const buildConsultProductConversationTitle = (draft: AssistantConsultProductDraft) => {
  return `商品咨询 ${draft.name}`
}
