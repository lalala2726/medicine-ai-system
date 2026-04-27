import React, { useMemo, useState } from 'react'
import { ImagePreview } from '@nutui/nutui-react'
import MarkdownRenderer from '@/components/MarkdownRenderer'
import AssistantAiMarkdownMessage from '../AssistantAiMarkdownMessage'
import AfterSaleCard from '../AfterSaleCard'
import ConsultProductCard from '../ConsultProductCard'
import ConsultationQuestionnaireCard from '../ConsultationQuestionnaireCard'
import ConsentCard from '../ConsentCard'
import { AssistantBubble, AssistantCardShell, AssistantThinking } from '../MessagePrimitives'
import OrderCard from '../OrderCard'
import PatientCard from '../PatientCard'
import ProductCard from '../ProductCard'
import ProductPurchaseCard from '../ProductPurchaseCard'
import SelectionCard from '../SelectionCard'
import { ASSISTANT_CARD_TYPES } from '@/api/assistant/contract'
import { CHAT_MESSAGE_POSITIONS, type MessageContentCallbacks } from '../../modules/messages/chatTypes'
import type {
  HistoryRenderCard,
  HistoryRenderAfterSaleCard,
  HistoryRenderConsultProductCard,
  HistoryRenderConsultationQuestionnaireCard,
  HistoryRenderConsentCard,
  HistoryRenderMessage,
  HistoryRenderOrderCard,
  HistoryRenderPatientCard,
  HistoryRenderProductCard,
  HistoryRenderProductPurchaseCard,
  HistoryRenderSelectionCard
} from '../../modules/session/historyMessageAdapters'
import styles from './index.module.less'

/** HistoryMessageContentRenderer 组件的 Props。 */
interface HistoryMessageContentRendererProps {
  /** 历史专用消息对象。 */
  message: HistoryRenderMessage
  /** 消息交互回调集合。 */
  callbacks?: MessageContentCallbacks
}

/**
 * 判断历史消息是否需要展示文本区域。
 *
 * @param message - 历史专用消息对象
 * @returns 是否存在可展示文本
 */
const hasTextSection = (message: HistoryRenderMessage) => {
  return Boolean(
    message.content.text?.trim() || message.content.thinking?.trim() || message.content.responseStatusText?.trim()
  )
}

/**
 * 渲染单张历史卡片。
 * 商品推荐卡为空时展示一个简短空态，避免纯卡片消息完全不可见。
 *
 * @param card - 历史卡片对象
 * @param messagePosition - 当前消息的布局位置
 * @param localMessageId - 当前历史消息在本地列表中的消息 ID
 * @param callbacks - 交互回调集合
 * @returns 渲染后的卡片节点
 */
const renderHistoryCard = (
  card: HistoryRenderCard,
  messagePosition: HistoryRenderMessage['position'],
  localMessageId: string,
  callbacks: MessageContentCallbacks
) => {
  if (card.type === ASSISTANT_CARD_TYPES.PRODUCT_CARD && card.products.length > 0) {
    const productCard = card as HistoryRenderProductCard
    return (
      <ProductCard
        title={productCard.title}
        products={productCard.products}
        tone={messagePosition === CHAT_MESSAGE_POSITIONS.RIGHT ? 'user' : 'assistant'}
        onProductClick={messagePosition === CHAT_MESSAGE_POSITIONS.RIGHT ? undefined : callbacks.onProductClick}
      />
    )
  }

  if (card.type === ASSISTANT_CARD_TYPES.CONSULT_PRODUCT_CARD) {
    const consultProductCard = card as HistoryRenderConsultProductCard

    return (
      <ConsultProductCard
        title={consultProductCard.title}
        product={consultProductCard.product}
        tone={messagePosition === CHAT_MESSAGE_POSITIONS.RIGHT ? 'user' : 'assistant'}
        onProductClick={callbacks.onConsultProductClick}
      />
    )
  }

  if (card.type === ASSISTANT_CARD_TYPES.PRODUCT_PURCHASE_CARD) {
    const purchaseCard = card as HistoryRenderProductPurchaseCard

    if (purchaseCard.products.length > 0 && purchaseCard.totalPrice.trim()) {
      return (
        <ProductPurchaseCard
          title={purchaseCard.title}
          products={purchaseCard.products}
          totalPrice={purchaseCard.totalPrice}
          onPurchase={callbacks.onPurchaseClick}
        />
      )
    }
  }

  if (card.type === ASSISTANT_CARD_TYPES.ORDER_CARD) {
    const orderCard = card as HistoryRenderOrderCard

    return (
      <OrderCard
        content={orderCard.content}
        tone={messagePosition === CHAT_MESSAGE_POSITIONS.RIGHT ? 'user' : 'assistant'}
      />
    )
  }

  if (card.type === ASSISTANT_CARD_TYPES.AFTER_SALE_CARD) {
    const afterSaleCard = card as HistoryRenderAfterSaleCard

    return (
      <AfterSaleCard
        content={afterSaleCard.content}
        tone={messagePosition === CHAT_MESSAGE_POSITIONS.RIGHT ? 'user' : 'assistant'}
      />
    )
  }

  if (card.type === ASSISTANT_CARD_TYPES.PATIENT_CARD) {
    const patientCard = card as HistoryRenderPatientCard

    return (
      <PatientCard
        content={patientCard.content}
        tone={messagePosition === CHAT_MESSAGE_POSITIONS.RIGHT ? 'user' : 'assistant'}
      />
    )
  }

  if (card.type === ASSISTANT_CARD_TYPES.CONSENT_CARD) {
    const consentCard = card as HistoryRenderConsentCard

    return (
      <ConsentCard
        localMessageId={localMessageId}
        messageId={consentCard.messageId}
        cardUuid={consentCard.cardUuid}
        title={consentCard.title}
        description={consentCard.description}
        confirm={consentCard.confirm}
        reject={consentCard.reject}
        onSubmit={callbacks.onConsentSubmit}
      />
    )
  }

  if (card.type === ASSISTANT_CARD_TYPES.CONSULTATION_QUESTIONNAIRE_CARD) {
    const consultationQuestionnaireCard = card as HistoryRenderConsultationQuestionnaireCard

    return (
      <ConsultationQuestionnaireCard
        localMessageId={localMessageId}
        messageId={consultationQuestionnaireCard.messageId}
        cardUuid={consultationQuestionnaireCard.cardUuid}
        questions={consultationQuestionnaireCard.questions}
        onSubmit={callbacks.onConsultationQuestionnaireSubmit}
      />
    )
  }

  if (card.type === ASSISTANT_CARD_TYPES.SELECTION_CARD) {
    const selectionCard = card as HistoryRenderSelectionCard

    return (
      <SelectionCard
        localMessageId={localMessageId}
        messageId={selectionCard.messageId}
        cardUuid={selectionCard.cardUuid}
        scene={selectionCard.scene}
        title={selectionCard.title}
        description={selectionCard.description}
        selectionMode={selectionCard.selectionMode}
        submitText={selectionCard.submitText}
        allowCustomInput={selectionCard.allowCustomInput}
        customInputPlaceholder={selectionCard.customInputPlaceholder}
        options={selectionCard.options}
        onSubmit={callbacks.onSelectionSubmit}
      />
    )
  }

  return (
    <AssistantCardShell className={styles.emptyCard}>
      <div className={styles.emptyCardHeader}>
        {card.title || (card.type === ASSISTANT_CARD_TYPES.PRODUCT_CARD ? '为您推荐以下商品' : '请确认要购买的商品')}
      </div>
      <div className={styles.emptyCardBody}>暂无可展示的商品信息</div>
    </AssistantCardShell>
  )
}

/**
 * 历史消息专用内容渲染器。
 * 负责在同一条历史消息中组合渲染文本区和 cards[] 区域。
 */
const HistoryMessageContentRenderer: React.FC<HistoryMessageContentRendererProps> = ({ message, callbacks = {} }) => {
  const historyCards = message.historyCards ?? []
  /** 当前历史文本气泡语义类型。 */
  const bubbleTone = message.position === CHAT_MESSAGE_POSITIONS.RIGHT ? 'user' : 'assistant'

  /** 当前图片预览是否可见。 */
  const [imagePreviewVisible, setImagePreviewVisible] = useState(false)
  /** 当前图片预览的初始索引。 */
  const [imagePreviewIndex, setImagePreviewIndex] = useState(0)

  // 提取历史用户消息中的文本和内嵌的图片链接
  const { displayText, imageUrls } = useMemo(() => {
    const rawText = message.content.text || ''
    if (message.position !== CHAT_MESSAGE_POSITIONS.RIGHT) {
      return { displayText: rawText, imageUrls: [] }
    }

    const images: string[] = []
    // 匹配如 ![用户上传图片1](https://xxx.jpg) 的 Markdown 图片语法
    const imageRegex = /!\[.*?\]\((.*?)\)/g
    let match
    while ((match = imageRegex.exec(rawText)) !== null) {
      images.push(match[1])
    }

    // 去除原文本中的这些图片声明，保留干净的文本气泡
    const cleanText = rawText.replace(/!\[.*?\]\((.*?)\)/g, '').trim()

    return { displayText: cleanText, imageUrls: images }
  }, [message.content.text, message.position])

  return (
    <div className={styles.container}>
      {hasTextSection(message) || imageUrls.length > 0 ? (
        message.position !== CHAT_MESSAGE_POSITIONS.RIGHT ? (
          <AssistantAiMarkdownMessage
            messageId={message.messageId}
            content={message.content.text || ''}
            thinking={message.content.thinking}
            thinkingDone={message.content.thinkingDone}
            statusText={message.content.responseStatusText}
          />
        ) : (
          <div className={styles.userMessageContainer}>
            {imageUrls.length > 0 ? (
              <>
                <div className={styles.userImageGrid}>
                  {imageUrls.map((imageUrl, imageIndex) => (
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
                  images={imageUrls.map(src => ({ src }))}
                  defaultValue={imagePreviewIndex + 1}
                  onClose={() => setImagePreviewVisible(false)}
                />
              </>
            ) : null}
            {displayText || message.content.thinking || message.content.responseStatusText ? (
              <AssistantBubble tone={bubbleTone}>
                <div className={styles.textBody}>
                  {message.content.thinking ? (
                    <AssistantThinking className={styles.thinking} isDone={message.content.thinkingDone}>
                      <MarkdownRenderer content={message.content.thinking} />
                    </AssistantThinking>
                  ) : null}
                  {displayText ? <MarkdownRenderer content={displayText} /> : null}
                  {message.content.responseStatusText ? (
                    <div className={styles.statusText}>{message.content.responseStatusText}</div>
                  ) : null}
                </div>
              </AssistantBubble>
            ) : null}
          </div>
        )
      ) : null}

      {historyCards.length > 0 ? (
        <div className={styles.cardList}>
          {historyCards.map(card => (
            <React.Fragment key={card.cardUuid}>
              {renderHistoryCard(card, message.position, message._id || '', callbacks)}
            </React.Fragment>
          ))}
        </div>
      ) : null}
    </div>
  )
}

export default HistoryMessageContentRenderer
