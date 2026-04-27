import React from 'react'
import { useNavigate } from 'react-router-dom'
import type { AfterSaleCardContent } from '../../modules/messages/chatTypes'
import { AssistantBubble, AssistantCardShell } from '../MessagePrimitives'
import styles from './index.module.less'

/** 售后卡气泡语义类型。 */
type AfterSaleCardTone = 'assistant' | 'user'

/** 售后卡组件属性。 */
interface AfterSaleCardProps {
  /** 售后卡内容。 */
  content: AfterSaleCardContent
  /** 卡片布局语义。 */
  tone?: AfterSaleCardTone
}

/**
 * Assistant 售后卡组件。
 *
 * @param props - 组件属性
 * @returns 售后卡节点
 */
const AfterSaleCard: React.FC<AfterSaleCardProps> = ({ content, tone = 'assistant' }) => {
  const navigate = useNavigate()

  /** 当前卡片容器样式。 */
  const cardClassName = [
    styles.afterSaleCard,
    tone === 'user' ? styles.afterSaleCardUser : styles.afterSaleCardAssistant
  ]
    .filter(Boolean)
    .join(' ')

  /**
   * 根据售后状态解析展示颜色。
   *
   * @param status - 售后状态编码
   * @returns 展示颜色值
   */
  const getStatusColor = (status?: string) => {
    switch (status) {
      case 'PENDING':
      case 'APPROVED':
      case 'PROCESSING':
        return 'var(--nutui-color-primary)'
      case 'COMPLETED':
        return '#16a34a'
      default:
        return '#9ca3af'
    }
  }

  /**
   * 处理卡片点击，跳转到售后详情页。
   *
   * @returns 无返回值
   */
  const handleClick = () => {
    if (content.afterSaleNo) {
      navigate(`/after-sale/detail/${content.afterSaleNo}`)
    }
  }

  /** 当前卡片主体。 */
  const cardContent = (
    <div className={cardClassName} onClick={handleClick}>
      <div className={styles.header}>
        <div className={styles.headerLeft}>
          <span className={styles.typeTag}>{content.afterSaleTypeText}</span>
          <span className={styles.afterSaleNo}>{content.afterSaleNo}</span>
        </div>
        <span className={styles.status} style={{ color: getStatusColor(content.afterSaleStatus) }}>
          {content.afterSaleStatusText}
        </span>
      </div>

      <div className={styles.content}>
        <img src={content.productInfo.productImage} className={styles.image} alt={content.productInfo.productName} />
        <div className={styles.info}>
          <div className={styles.name}>{content.productInfo.productName}</div>
          <div className={styles.orderNo}>订单号: {content.orderNo}</div>
        </div>
      </div>

      <div className={styles.metaBox}>
        <div className={styles.metaRow}>
          <span className={styles.metaLabel}>申请原因</span>
          <span className={styles.metaValue}>{content.applyReasonName}</span>
        </div>
        <div className={styles.metaRow}>
          <span className={styles.metaLabel}>退款金额</span>
          <span className={styles.metaAmount}>¥{content.refundAmount}</span>
        </div>
        <div className={styles.metaRow}>
          <span className={styles.metaLabel}>申请时间</span>
          <span className={styles.metaTime}>{content.applyTime}</span>
        </div>
      </div>
    </div>
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

export default AfterSaleCard
