import React from 'react'
import { Button } from '@nutui/nutui-react'
import { MapPin } from 'lucide-react'
import { newOrderTypes } from '@/api/order.ts'
import { OrderStatus, getOrderStatusText } from '@/types/orderStatus'
import styles from './index.module.less'

interface OrderCardProps {
  order: newOrderTypes.OrderListVo
  onViewDetail: (orderNo: string) => void
  onPay: (orderNo: string) => void
  onCancel: (orderNo: string) => void
  onConfirmReceipt: (orderNo: string) => void
  onAfterSale: (orderNo: string) => void
}

const OrderCard: React.FC<OrderCardProps> = ({
  order,
  onViewDetail,
  onPay,
  onCancel,
  onConfirmReceipt,
  onAfterSale
}) => {
  // 获取订单状态颜色
  const getStatusColor = (status?: string) => {
    switch (status) {
      case OrderStatus.PENDING_PAYMENT:
      case OrderStatus.PENDING_SHIPMENT:
      case OrderStatus.PENDING_RECEIPT:
        return 'var(--nutui-color-primary)'
      case OrderStatus.COMPLETED:
        return '#9ca3af'
      default:
        return '#9ca3af'
    }
  }

  // 渲染订单操作按钮
  const renderActions = () => {
    const actions = []

    // 待支付：支付、取消
    if (order.orderStatus === OrderStatus.PENDING_PAYMENT) {
      actions.push(
        <Button
          key='cancel'
          size='small'
          fill='outline'
          type='default'
          className={styles.actionBtn}
          onClick={e => {
            e.stopPropagation()
            onCancel(order.orderNo!)
          }}
        >
          取消订单
        </Button>
      )
      actions.push(
        <Button
          key='pay'
          size='small'
          type='primary'
          fill='outline'
          className={`${styles.actionBtn} ${styles.primaryBtn}`}
          onClick={e => {
            e.stopPropagation()
            onPay(order.orderNo!)
          }}
        >
          去支付
        </Button>
      )
    }

    // 待发货：申请退款
    if (order.orderStatus === OrderStatus.PENDING_SHIPMENT) {
      actions.push(
        <Button
          key='refund'
          size='small'
          fill='outline'
          type='default'
          className={styles.actionBtn}
          onClick={e => {
            e.stopPropagation()
            onAfterSale(order.orderNo!)
          }}
        >
          申请退款
        </Button>
      )
    }

    // 待收货：查看物流、确认收货
    if (order.orderStatus === OrderStatus.PENDING_RECEIPT) {
      actions.push(
        <Button
          key='logistics'
          size='small'
          fill='outline'
          type='default'
          className={styles.actionBtn}
          onClick={e => {
            e.stopPropagation()
            // TODO: 查看物流
          }}
        >
          查看物流
        </Button>
      )
      actions.push(
        <Button
          key='confirm'
          size='small'
          type='primary'
          fill='outline'
          className={`${styles.actionBtn} ${styles.primaryBtn}`}
          onClick={e => {
            e.stopPropagation()
            onConfirmReceipt(order.orderNo!)
          }}
        >
          确认收货
        </Button>
      )
    }

    // 已完成：查看详情
    if (order.orderStatus === OrderStatus.COMPLETED) {
      actions.push(
        <Button
          key='detail'
          size='small'
          fill='outline'
          type='default'
          className={styles.actionBtn}
          onClick={e => {
            e.stopPropagation()
            onViewDetail(order.orderNo!)
          }}
        >
          查看详情
        </Button>
      )
    }

    return actions
  }

  return (
    <div className={styles.orderCard} onClick={() => onViewDetail(order.orderNo!)}>
      {/* 订单头部 */}
      <div className={styles.orderHeader}>
        <div className={styles.orderNo}>订单编号: {order.orderNo}</div>
        <div className={styles.orderStatus} style={{ color: getStatusColor(order.orderStatus) }}>
          {order.afterSaleFlag === 'COMPLETED' && <span className={styles.completedTag}>售后完成</span>}
          {getOrderStatusText(order.orderStatus)}
        </div>
      </div>

      {/* 地址信息 - 优化排版 */}
      <div className={styles.addressInfo}>
        <div className={styles.addressIcon}>
          <MapPin size={16} />
        </div>
        <div className={styles.addressContent}>
          <div className={styles.userInfo}>
            <span className={styles.userName}>{order.receiverInfo?.name}</span>
            <span className={styles.userPhone}>{order.receiverInfo?.phone}</span>
          </div>
          <div className={styles.addressDetail}>{order.receiverInfo?.address}</div>
        </div>
      </div>

      {/* 订单商品列表 */}
      <div className={styles.orderItems}>
        {order.items?.map(item => (
          <div key={item.id} className={styles.orderItem}>
            <img src={item.imageUrl} alt={item.productName} className={styles.productImage} />
            <div className={styles.productInfo}>
              <div className={styles.productNameRow}>
                <div className={styles.productName}>{item.productName}</div>
                {item.afterSaleStatus === 'IN_PROGRESS' && <span className={styles.itemAfterSaleTag}>售后中</span>}
                {item.afterSaleStatus === 'COMPLETED' && (
                  <span className={`${styles.itemAfterSaleTag} ${styles.itemAfterSaleTagCompleted}`}>售后完成</span>
                )}
              </div>
              <div className={styles.productMeta}>
                <span className={styles.productPrice}>¥{item.price}</span>
                <span className={styles.productQuantity}>×{item.quantity}</span>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* 订单金额 */}
      <div className={styles.orderTotal}>
        <span className={styles.totalLabel}>实付款</span>
        <span className={styles.totalAmount}>¥{order.totalAmount}</span>
      </div>

      {/* 订单底部操作按钮 */}
      <div className={styles.orderFooter}>
        <div className={styles.orderActions}>{renderActions()}</div>
      </div>
    </div>
  )
}

export default OrderCard
