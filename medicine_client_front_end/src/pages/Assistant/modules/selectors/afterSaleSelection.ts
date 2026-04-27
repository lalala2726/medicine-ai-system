import { OrderAfterSaleTypes } from '@/api/orderAfterSale'
import { ASSISTANT_CARD_TYPES, ASSISTANT_MESSAGE_TYPES } from '@/api/assistant/contract'
import { CHAT_MESSAGE_POSITIONS, CHAT_MESSAGE_TYPES, type ChatMessage } from '../messages/chatTypes'
import type { AssistantChatSubmitCardPayload } from '@/api/assistant/agent'

/** 售后单选择后，发送给流式会话层的完整载荷。 */
export interface AfterSaleSelectionPayload {
  messageType: typeof ASSISTANT_MESSAGE_TYPES.CARD
  card: AssistantChatSubmitCardPayload
  userMessage: ChatMessage
}

/** 售后单选择载荷构建所需的参数。 */
interface BuildAfterSaleSelectionPayloadOptions {
  item: OrderAfterSaleTypes.AfterSaleListVo
  userAvatar: string
}

/**
 * 将售后单数据整理成聊天问题和用户回显消息。
 * 页面 controller 不再直接拼接售后文本。
 */
export function buildAfterSaleSelectionPayload({
  item,
  userAvatar
}: BuildAfterSaleSelectionPayloadOptions): AfterSaleSelectionPayload {
  /** 当前售后单号。 */
  const afterSaleNo = item.afterSaleNo || ''
  /** 当前订单编号。 */
  const orderNo = item.orderNo || ''
  /** 当前售后类型编码。 */
  const afterSaleType = item.afterSaleType || ''
  /** 当前售后类型展示文案。 */
  const afterSaleTypeText = item.afterSaleTypeName || ''
  /** 当前售后状态编码。 */
  const afterSaleStatus = item.afterSaleStatus || ''
  /** 当前售后状态展示文案。 */
  const afterSaleStatusText = item.afterSaleStatusName || ''
  /** 当前退款金额。 */
  const refundAmount = String(item.refundAmount || '')
  /** 当前申请原因名称。 */
  const applyReasonName = item.applyReasonName || ''
  /** 当前申请时间。 */
  const applyTime = item.applyTime || ''
  /** 当前售后卡完整提交载荷。 */
  const card: AssistantChatSubmitCardPayload = {
    type: ASSISTANT_CARD_TYPES.AFTER_SALE_CARD,
    data: {
      after_sale_no: afterSaleNo,
      order_no: orderNo,
      after_sale_type: afterSaleType,
      after_sale_type_text: afterSaleTypeText,
      after_sale_status: afterSaleStatus,
      after_sale_status_text: afterSaleStatusText,
      refund_amount: refundAmount,
      apply_reason_name: applyReasonName,
      apply_time: applyTime,
      product_info: {
        product_name: item.productName || '',
        product_image: item.productImage || ''
      }
    }
  }

  /** 用户发送到聊天区的本地售后卡回显消息。 */
  const userMessage: ChatMessage = {
    type: CHAT_MESSAGE_TYPES.AFTER_SALE_CARD,
    content: {
      afterSaleCard: {
        afterSaleNo,
        orderNo,
        afterSaleType,
        afterSaleTypeText,
        afterSaleStatus,
        afterSaleStatusText,
        refundAmount,
        applyReasonName,
        applyTime,
        productInfo: {
          productName: item.productName || '',
          productImage: item.productImage || ''
        }
      }
    },
    position: CHAT_MESSAGE_POSITIONS.RIGHT,
    user: { avatar: userAvatar }
  }

  return {
    messageType: ASSISTANT_MESSAGE_TYPES.CARD,
    card,
    userMessage
  }
}
