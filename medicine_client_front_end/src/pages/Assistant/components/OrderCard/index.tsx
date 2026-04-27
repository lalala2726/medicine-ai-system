import React from 'react'
import { useNavigate } from 'react-router-dom'
import type { OrderCardContent } from '../../modules/messages/chatTypes'
import { AssistantBubble, AssistantCardShell } from '../MessagePrimitives'
import { getOrderStatusText } from '@/types/orderStatus'
import styles from './index.module.less'

type OrderCardTone = 'assistant' | 'user'

interface OrderCardProps {
  content: OrderCardContent
  tone?: OrderCardTone
}

const OrderCard: React.FC<OrderCardProps> = ({ content, tone = 'assistant' }) => {
  const { previewProduct, productCount, orderNo, orderStatus, payAmount } = content
  const navigate = useNavigate()

  const cardClassName = [styles.orderCard, tone === 'user' ? styles.orderCardUser : styles.orderCardAssistant]
    .filter(Boolean)
    .join(' ')

  const getStatusColor = (status?: string) => {
    switch (status) {
      case 'PENDING_PAYMENT':
      case 'PENDING_SHIPMENT':
      case 'PENDING_RECEIPT':
        return 'var(--nutui-color-primary)'
      case 'COMPLETED':
        return '#9ca3af'
      default:
        return '#9ca3af'
    }
  }

  const handleClick = () => {
    if (orderNo) {
      navigate(`/orders/${orderNo}`)
    }
  }

  const cardContent = (
    <div className={cardClassName} onClick={handleClick}>
      {/* 订单头部 */}
      <div className={styles.orderHeader}>
        <div className={styles.orderNo}>订单编号: {orderNo}</div>
        <div className={styles.orderStatus} style={{ color: getStatusColor(orderStatus) }}>
          {content.orderStatusText || getOrderStatusText(orderStatus)}
        </div>
      </div>

      {/* 订单商品列表 */}
      <div className={styles.orderItems}>
        <div className={styles.orderItem}>
          <img src={previewProduct?.imageUrl} alt={previewProduct?.productName} className={styles.productImage} />
          <div className={styles.productInfo}>
            <div className={styles.productNameRow}>
              <div className={styles.productName}>{previewProduct?.productName}</div>
            </div>
            <div className={styles.productMeta}>
              <span className={styles.productPrice}></span>
              <span className={styles.productQuantity}>共{productCount}件</span>
            </div>
          </div>
        </div>
      </div>

      {/* 订单金额 */}
      <div className={styles.orderTotal}>
        <span className={styles.totalLabel}>实付款</span>
        <span className={styles.totalAmount}>¥{payAmount}</span>
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

export default OrderCard
