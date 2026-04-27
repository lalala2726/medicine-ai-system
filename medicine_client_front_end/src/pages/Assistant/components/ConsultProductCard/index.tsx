import React from 'react'
import { MessageSquare } from 'lucide-react'
import { AssistantBubble, AssistantCardShell } from '../MessagePrimitives'
import type { ConsultProductDisplayItem } from '../../modules/messages/chatTypes'
import styles from './index.module.less'

/** 咨询商品卡标签文案。 */
const CONSULT_TAG_TEXT = '咨询商品'

/** 咨询商品卡布局语义。 */
type ConsultProductCardTone = 'assistant' | 'user'

interface ConsultProductCardProps {
  /** @deprecated 不再展示标题，保留字段避免外部调用报错。 */
  title?: string
  /** 当前咨询商品。 */
  product: ConsultProductDisplayItem
  /** 卡片布局语义。 */
  tone?: ConsultProductCardTone
  /** 点击咨询商品回调。 */
  onProductClick?: (product: ConsultProductDisplayItem) => void
}

/**
 * 咨询商品卡组件。
 *
 * @param props - 组件属性
 * @returns 咨询商品卡节点
 */
const ConsultProductCard: React.FC<ConsultProductCardProps> = ({ product, tone = 'assistant', onProductClick }) => {
  /**
   * 触发咨询商品点击回调。
   *
   * @returns 无返回值
   */
  const handleProductClick = () => {
    onProductClick?.(product)
  }

  const cardClassName = [styles.consultCard, tone === 'user' ? styles.consultCardUser : styles.consultCardAssistant]
    .filter(Boolean)
    .join(' ')

  const cardContent = (
    <button
      type='button'
      className={cardClassName}
      onClick={handleProductClick}
      aria-label={`查看咨询商品${product.name}`}
    >
      <span className={styles.header}>
        <span className={styles.headerLeft}>
          <MessageSquare size={14} className={styles.headerIcon} />
          <span className={styles.tagText}>{CONSULT_TAG_TEXT}</span>
        </span>
      </span>

      <span className={styles.content}>
        <img src={product.image} alt={product.name} className={styles.productImage} />
        <span className={styles.productInfo}>
          <span className={styles.productName}>{product.name}</span>
          <span className={styles.productPrice}>
            <span className={styles.priceSymbol}>¥</span>
            <span className={styles.priceValue}>{product.price}</span>
          </span>
        </span>
      </span>
    </button>
  )

  if (tone === 'user') {
    return (
      <AssistantBubble tone='user' className={styles.userBubble}>
        {cardContent}
      </AssistantBubble>
    )
  }

  return <AssistantCardShell className={styles.assistantCardShell}>{cardContent}</AssistantCardShell>
}

export default ConsultProductCard
