import React, { useCallback, useState } from 'react'
import { ImagePreview } from '@nutui/nutui-react'
import MarkdownRenderer from '@/components/MarkdownRenderer'
import AssistantAiMarkdownMessage from '../AssistantAiMarkdownMessage'
import AfterSaleCard from '../AfterSaleCard'
import ConsultationQuestionnaireCard from '../ConsultationQuestionnaireCard'
import ConsentCard from '../ConsentCard'
import { AssistantBubble, AssistantThinking, AssistantTyping } from '../MessagePrimitives'
import ProductCard from '../ProductCard'
import ProductPurchaseCard from '../ProductPurchaseCard'
import SelectionCard from '../SelectionCard'
import OrderCard from '../OrderCard'
import PatientCard from '../PatientCard'
import {
  CHAT_MESSAGE_POSITIONS,
  CHAT_MESSAGE_TYPES,
  type ChatMessage,
  type MessageContentCallbacks,
  type ProductDisplayItem
} from '../../modules/messages/chatTypes'
import styles from './index.module.less'

export type { ChatMessage, MessageContentCallbacks }

/** MessageContentRenderer 组件的 Props */
interface MessageContentRendererProps {
  /** 消息对象 */
  message: ChatMessage
  /** 回调函数集合 */
  callbacks?: MessageContentCallbacks
}

/**
 * 消息内容渲染器组件
 * 根据消息类型（typing / text / image / product-card）渲染对应的 UI 组件
 * - typing: 显示打字动画
 * - text: 渲染 Markdown 文本，支持思考过程展示和状态提示
 * - image: 渲染图片气泡
 * - product-card: 渲染商品卡片列表
 * - product-purchase-card: 渲染商品购买确认卡
 * @param props.message - 聊天消息对象
 * @param props.callbacks - 消息交互回调（点击商品、购买等）
 */
const MessageContentRenderer: React.FC<MessageContentRendererProps> = ({ message, callbacks = {} }) => {
  const { type, content } = message
  const {
    onProductClick,
    onConsentSubmit,
    onConsultationQuestionnaireSubmit,
    onSelectionSubmit,
    onPurchaseClick,
    onCardExitComplete
  } = callbacks
  const localMessageId = message._id || ''
  const sourceMessageId = message.messageId || ''
  const sourceCardUuid = message.cardUuid || ''
  /** 当前文本气泡语义类型。 */
  const bubbleTone = message.position === CHAT_MESSAGE_POSITIONS.RIGHT ? 'user' : 'assistant'
  /** 当前消息是否正在退出。 */
  const isExiting = message._exiting === true
  /** 当前图片预览是否可见。 */
  const [imagePreviewVisible, setImagePreviewVisible] = useState(false)
  /** 当前图片预览的初始索引。 */
  const [imagePreviewIndex, setImagePreviewIndex] = useState(0)

  /** 当前消息退出动画完成时的回调。 */
  const handleExitComplete = useCallback(() => {
    if (localMessageId) {
      onCardExitComplete?.(localMessageId)
    }
  }, [localMessageId, onCardExitComplete])

  switch (type) {
    case CHAT_MESSAGE_TYPES.TYPING:
      return <AssistantTyping />

    case CHAT_MESSAGE_TYPES.TEXT:
      if (message.position !== CHAT_MESSAGE_POSITIONS.RIGHT) {
        return (
          <AssistantAiMarkdownMessage
            messageId={message.messageId}
            content={content.text || ''}
            thinking={content.thinking}
            thinkingDone={content.thinkingDone}
            toolStatus={content.toolStatus}
            statusText={content.responseStatusText}
          />
        )
      }

      return (
        <div className={styles.userMessageContainer}>
          {content.imageUrls && content.imageUrls.length > 0 ? (
            <>
              <div className={styles.userImageGrid}>
                {content.imageUrls.map((imageUrl, imageIndex) => (
                  <img
                    key={`${imageUrl}-${imageIndex}`}
                    src={imageUrl}
                    alt={`图片${imageIndex + 1}`}
                    className={styles.userImage}
                    onClick={() => {
                      setImagePreviewIndex(imageIndex)
                      setImagePreviewVisible(true)
                    }}
                  />
                ))}
              </div>
              <ImagePreview
                visible={imagePreviewVisible}
                images={content.imageUrls.map(src => ({ src }))}
                defaultValue={imagePreviewIndex + 1}
                onClose={() => setImagePreviewVisible(false)}
              />
            </>
          ) : null}
          {content.text ? (
            <AssistantBubble tone={bubbleTone}>
              <div className={styles.textBody}>
                {content.thinking ? (
                  <AssistantThinking className={styles.thinking} isDone={content.thinkingDone}>
                    <MarkdownRenderer content={content.thinking} />
                  </AssistantThinking>
                ) : null}
                <MarkdownRenderer content={content.text} />
                {content.responseStatusText ? (
                  <div className={styles.statusText}>{content.responseStatusText}</div>
                ) : null}
              </div>
            </AssistantBubble>
          ) : null}
        </div>
      )

    case CHAT_MESSAGE_TYPES.IMAGE:
      return (
        <AssistantBubble variant='image'>
          <img src={content.picUrl} alt='' />
        </AssistantBubble>
      )

    case CHAT_MESSAGE_TYPES.PRODUCT_CARD: {
      const productList: ProductDisplayItem[] = (content.products || []).map(item => ({
        id: item.id,
        name: item.name,
        image: item.image,
        price: item.price
      }))
      return (
        <ProductCard title={content.title} products={productList} tone={bubbleTone} onProductClick={onProductClick} />
      )
    }

    case CHAT_MESSAGE_TYPES.PRODUCT_PURCHASE_CARD:
      return content.purchaseCard ? (
        <ProductPurchaseCard
          title={content.purchaseCard.title}
          products={content.purchaseCard.products}
          totalPrice={content.purchaseCard.totalPrice}
          onPurchase={onPurchaseClick}
        />
      ) : null

    case CHAT_MESSAGE_TYPES.CONSENT_CARD:
      return content.consentCard ? (
        <ConsentCard
          localMessageId={localMessageId}
          messageId={sourceMessageId}
          cardUuid={sourceCardUuid}
          title={content.consentCard.title}
          description={content.consentCard.description}
          confirm={content.consentCard.confirm}
          reject={content.consentCard.reject}
          onSubmit={onConsentSubmit}
          visible={!isExiting}
          onExitComplete={handleExitComplete}
        />
      ) : null

    case CHAT_MESSAGE_TYPES.CONSULTATION_QUESTIONNAIRE_CARD:
      return content.consultationQuestionnaireCard ? (
        <ConsultationQuestionnaireCard
          localMessageId={localMessageId}
          messageId={sourceMessageId}
          cardUuid={sourceCardUuid}
          questions={content.consultationQuestionnaireCard.questions}
          onSubmit={onConsultationQuestionnaireSubmit}
          visible={!isExiting}
          onExitComplete={handleExitComplete}
        />
      ) : null

    case CHAT_MESSAGE_TYPES.SELECTION_CARD:
      return content.selectionCard ? (
        <SelectionCard
          localMessageId={localMessageId}
          messageId={sourceMessageId}
          cardUuid={sourceCardUuid}
          scene={content.selectionCard.scene}
          title={content.selectionCard.title}
          description={content.selectionCard.description}
          selectionMode={content.selectionCard.selectionMode}
          submitText={content.selectionCard.submitText}
          allowCustomInput={content.selectionCard.allowCustomInput}
          customInputPlaceholder={content.selectionCard.customInputPlaceholder}
          options={content.selectionCard.options}
          onSubmit={onSelectionSubmit}
          visible={!isExiting}
          onExitComplete={handleExitComplete}
        />
      ) : null

    case CHAT_MESSAGE_TYPES.ORDER_CARD:
      return content.orderCard ? <OrderCard content={content.orderCard} tone={bubbleTone} /> : null

    case CHAT_MESSAGE_TYPES.AFTER_SALE_CARD:
      return content.afterSaleCard ? <AfterSaleCard content={content.afterSaleCard} tone={bubbleTone} /> : null

    case CHAT_MESSAGE_TYPES.PATIENT_CARD:
      return content.patientCard ? <PatientCard content={content.patientCard} tone={bubbleTone} /> : null

    default:
      return null
  }
}

export default MessageContentRenderer
