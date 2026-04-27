import { newOrderTypes } from '@/api/order'
import { ASSISTANT_CARD_TYPES, ASSISTANT_MESSAGE_TYPES } from '@/api/assistant/contract'
import { getOrderStatusText } from '@/types/orderStatus'
import { CHAT_MESSAGE_POSITIONS, CHAT_MESSAGE_TYPES, type ChatMessage } from '../messages/chatTypes'
import type { AssistantChatSubmitCardPayload } from '@/api/assistant/agent'

/** 订单选择后，发送给流式会话层的完整载荷。 */
export interface OrderSelectionPayload {
  messageType: typeof ASSISTANT_MESSAGE_TYPES.CARD
  card: AssistantChatSubmitCardPayload
  userMessage: ChatMessage
}

/** 订单选择载荷构建所需的参数。 */
interface BuildOrderSelectionPayloadOptions {
  order: newOrderTypes.OrderListVo
  userAvatar: string
}

/**
 * 将订单数据整理成聊天问题和用户回显消息。
 * 这样页面 controller 不需要关心订单卡提交协议和回显结构。
 */
export function buildOrderSelectionPayload({
  order,
  userAvatar
}: BuildOrderSelectionPayloadOptions): OrderSelectionPayload {
  /** 订单里的商品列表。 */
  const orderItems = order.items || []
  /** 当前订单号。 */
  const orderNo = order.orderNo || ''
  /** 当前订单状态枚举值。 */
  const orderStatus = order.orderStatus || ''
  /** 当前订单状态展示文案。 */
  const orderStatusText = getOrderStatusText(order.orderStatus)
  /** 当前订单首个商品，用于订单卡精简预览。 */
  const previewItem = orderItems[0]
  /** 优先取实付金额，没有时回退总金额。 */
  const payAmount = String(order.payAmount || order.totalAmount || '0')
  /** 当前订单总金额。 */
  const totalAmount = String(order.totalAmount || order.payAmount || '0')
  /** 当前订单的下单时间。 */
  const createTime = order.createTime || ''
  /** 当前订单商品总数（按商品行计数）。 */
  const productCount = orderItems.length
  /** 提交给后端的完整订单卡消息。 */
  const card: AssistantChatSubmitCardPayload = {
    type: ASSISTANT_CARD_TYPES.ORDER_CARD,
    data: {
      order_no: orderNo,
      order_status: orderStatus,
      order_status_text: orderStatusText,
      preview_product: {
        product_id: String(previewItem?.productId || ''),
        product_name: previewItem?.productName || '',
        image_url: previewItem?.imageUrl || ''
      },
      product_count: productCount,
      pay_amount: payAmount,
      total_amount: totalAmount,
      create_time: createTime
    }
  }

  /** 用户发送到聊天区的本地订单卡回显消息。 */
  const userMessage: ChatMessage = {
    type: CHAT_MESSAGE_TYPES.ORDER_CARD,
    content: {
      orderCard: {
        orderNo,
        orderStatus,
        orderStatusText,
        previewProduct: {
          productId: String(previewItem?.productId || ''),
          productName: previewItem?.productName || '',
          imageUrl: previewItem?.imageUrl || ''
        },
        productCount,
        payAmount,
        totalAmount,
        createTime
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
