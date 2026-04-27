import { useCallback, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { ASSISTANT_MESSAGE_TYPES, type AssistantTypes } from '@/api/assistant/contract'
import type { AssistantChatSubmitCardPayload } from '@/api/assistant/agent'
import { ASSISTANT_REQUEST_START_RESULT_TYPES, type AssistantRequestStartResult } from '../stream/useAssistantStream'
import {
  type ChatMessage,
  type ConsultProductDisplayItem,
  type ConsultationQuestionnaireCardSubmitPayload,
  type ConsentCardSubmitPayload,
  type MessageContentCallbacks,
  type ProductDisplayItem,
  type PurchaseProductDisplayItem,
  type SelectionCardSubmitPayload
} from './chatTypes'
import {
  buildConsentCardRequestPayload,
  buildConsultationQuestionnaireRequestPayload,
  buildSelectionCardRequestPayload
} from './interactiveCardPayloads'

/** useMessageCallbacks Hook 的配置项 */
interface UseMessageCallbacksOptions {
  /** 交互卡点击成功后，从当前消息列表中移除对应卡片。 */
  removeInteractiveCardLocally: (localMessageId: string, cardUuid: string) => void
  /** 页面层统一发送入口 */
  startAssistantRequest: (options: {
    messageType: typeof ASSISTANT_MESSAGE_TYPES.TEXT | typeof ASSISTANT_MESSAGE_TYPES.CARD
    content?: string
    card?: AssistantChatSubmitCardPayload
    userMessage?: ChatMessage
    cardAction?: AssistantTypes.CardActionPayload
    conversationTitle?: string
  }) => Promise<AssistantRequestStartResult>
  /** 当前用户头像 */
  userAvatar: string
  /** 卡片退出动画完成后的清理回调。 */
  onCardExitComplete?: (localMessageId: string) => void
}

/**
 * 封装消息渲染层的交互回调。
 * 当前负责商品卡片的点击反馈与跳转逻辑。
 *
 * @param options - 配置项，需传入 appendMsg 方法
 * @returns MessageContentCallbacks 回调对象
 */
export function useMessageCallbacks({
  removeInteractiveCardLocally,
  startAssistantRequest,
  userAvatar,
  onCardExitComplete
}: UseMessageCallbacksOptions) {
  const navigate = useNavigate()

  /**
   * 处理商品点击事件
   * 导航到对应商品详情页。
   *
   * @param product - 被点击的商品数据
   */
  const handleProductClick = useCallback(
    (product: ProductDisplayItem) => {
      navigate(`/product/${product.id}`)
    },
    [navigate]
  )

  /**
   * 处理咨询商品卡点击事件。
   * 导航到对应商品详情页。
   *
   * @param product - 当前咨询商品数据
   * @returns 无返回值
   */
  const handleConsultProductClick = useCallback(
    (product: ConsultProductDisplayItem) => {
      navigate(`/product/${product.id}`)
    },
    [navigate]
  )

  /**
   * 处理同意卡点击事件。
   * 成功发起请求后，移除当前卡片消息。
   */
  const handleConsentSubmit = useCallback(
    (payload: ConsentCardSubmitPayload) => {
      void (async () => {
        const requestPayload = buildConsentCardRequestPayload(payload, { userAvatar })
        const result = await startAssistantRequest(requestPayload)

        if (result.type === ASSISTANT_REQUEST_START_RESULT_TYPES.SUBMITTED) {
          removeInteractiveCardLocally(payload.localMessageId, payload.cardUuid)
        }
      })()
    },
    [removeInteractiveCardLocally, startAssistantRequest, userAvatar]
  )

  /**
   * 处理 consultation 问卷卡发送事件。
   * 成功发起请求后，移除当前问卷卡消息。
   */
  const handleConsultationQuestionnaireSubmit = useCallback(
    (payload: ConsultationQuestionnaireCardSubmitPayload) => {
      void (async () => {
        const requestPayload = buildConsultationQuestionnaireRequestPayload(payload, { userAvatar })
        const result = await startAssistantRequest(requestPayload)

        if (result.type === ASSISTANT_REQUEST_START_RESULT_TYPES.SUBMITTED) {
          removeInteractiveCardLocally(payload.localMessageId, payload.cardUuid)
        }
      })()
    },
    [removeInteractiveCardLocally, startAssistantRequest, userAvatar]
  )

  /**
   * 处理选择卡发送事件。
   * 成功发起请求后，移除当前卡片消息。
   */
  const handleSelectionSubmit = useCallback(
    (payload: SelectionCardSubmitPayload) => {
      void (async () => {
        const requestPayload = buildSelectionCardRequestPayload(payload, { userAvatar })
        const result = await startAssistantRequest(requestPayload)

        if (result.type === ASSISTANT_REQUEST_START_RESULT_TYPES.SUBMITTED) {
          removeInteractiveCardLocally(payload.localMessageId, payload.cardUuid)
        }
      })()
    },
    [removeInteractiveCardLocally, startAssistantRequest, userAvatar]
  )

  /**
   * 处理商品购买卡"立即购买"点击事件。
   * 将商品数据转换为 CartItem 格式后跳转到结算页。
   *
   * @param products - 购买商品列表
   */
  const handlePurchaseClick = useCallback(
    (products: PurchaseProductDisplayItem[]) => {
      if (products.length === 0) return

      const cartItems = products.map(product => ({
        id: 0,
        productId: Number(product.id),
        name: product.name,
        price: Number(product.price),
        image: product.image,
        spec: '',
        quantity: product.quantity
      }))

      navigate('/checkout', {
        state: {
          isBuyNow: true,
          products: cartItems
        }
      })
    },
    [navigate]
  )

  /** 聚合消息渲染器需要的回调集合，避免每次渲染都创建新对象。 */
  const messageCallbacks: MessageContentCallbacks = useMemo(
    () => ({
      onProductClick: handleProductClick,
      onBuyClick: handleProductClick,
      onConsultProductClick: handleConsultProductClick,
      onPurchaseClick: handlePurchaseClick,
      onConsentSubmit: handleConsentSubmit,
      onConsultationQuestionnaireSubmit: handleConsultationQuestionnaireSubmit,
      onSelectionSubmit: handleSelectionSubmit,
      onCardExitComplete
    }),
    [
      handleConsentSubmit,
      handleConsultProductClick,
      handleConsultationQuestionnaireSubmit,
      handleProductClick,
      handlePurchaseClick,
      handleSelectionSubmit,
      onCardExitComplete
    ]
  )

  return messageCallbacks
}
