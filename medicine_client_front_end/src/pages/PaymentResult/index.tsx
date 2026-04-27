import React from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { Button } from '@nutui/nutui-react'
import { CheckCircle2, XCircle } from 'lucide-react'
import { OrderStatus } from '@/types/orderStatus'
import { getPayTypeText } from '@/types/payType'
import styles from './index.module.less'

// 支付方式枚举
type PayTypeEnum = 'WALLET' | 'COUPON'

interface LocationState {
  success: boolean
  orderNo?: string
  totalAmount?: string
  paymentMethod?: PayTypeEnum
  orderStatus?: OrderStatus
  message?: string
}

const PaymentResult: React.FC = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const state = (location.state as LocationState) || { success: false }

  const handleViewOrder = () => {
    // 支付成功后跳转到订单页面的"待发货"Tab，并替换当前历史记录，带上来源标记
    if (state.success) {
      navigate(`/orders?status=${OrderStatus.PENDING_SHIPMENT}`, {
        state: { fromPayment: true },
        replace: true
      })
    } else if (state.orderNo) {
      navigate(`/orders/${state.orderNo}`, {
        state: { fromPayment: true },
        replace: true
      })
    } else {
      navigate('/orders', {
        state: { fromPayment: true },
        replace: true
      })
    }
  }

  const handleBackHome = () => {
    navigate('/', { replace: true })
  }

  return (
    <div className={styles.paymentResult}>
      <div className={styles.resultContent}>
        {/* 结果图标 */}
        <div className={styles.resultIcon}>
          {state.success ? (
            <div className={styles.successIcon}>
              <CheckCircle2 size={60} strokeWidth={2.5} />
            </div>
          ) : (
            <div className={styles.failureIcon}>
              <XCircle size={60} strokeWidth={2.5} />
            </div>
          )}
        </div>

        {/* 结果标题 */}
        <div className={styles.resultTitle}>{state.success ? '支付成功' : '支付失败'}</div>

        {/* 结果描述 */}
        <div className={styles.resultDescription}>
          {state.success ? '您的订单已支付成功，我们会尽快为您发货' : state.message || '支付过程中出现问题，请稍后重试'}
        </div>

        {/* 订单信息卡片 */}
        {state.success && (
          <div className={styles.orderInfo}>
            {state.orderNo && (
              <div className={styles.infoItem}>
                <span className={styles.infoLabel}>订单编号</span>
                <span className={styles.infoValue}>{state.orderNo}</span>
              </div>
            )}
            {state.totalAmount && (
              <div className={styles.infoItem}>
                <span className={styles.infoLabel}>实付金额</span>
                <span className={styles.infoValue}>¥ {state.totalAmount}</span>
              </div>
            )}
            {state.paymentMethod && (
              <div className={styles.infoItem}>
                <span className={styles.infoLabel}>支付方式</span>
                <span className={styles.infoValue}>{getPayTypeText(state.paymentMethod)}</span>
              </div>
            )}
          </div>
        )}

        {/* 操作按钮 */}
        <div className={styles.resultActions}>
          {state.success ? (
            <>
              <Button type='primary' className={styles.actionBtn} onClick={handleViewOrder}>
                查看订单
              </Button>
              <Button type='default' className={styles.actionBtn} onClick={handleBackHome}>
                返回首页
              </Button>
            </>
          ) : (
            <>
              <Button type='primary' className={styles.actionBtn} onClick={() => navigate(-1)}>
                重新支付
              </Button>
              <Button type='default' className={styles.actionBtn} onClick={handleBackHome}>
                返回首页
              </Button>
            </>
          )}
        </div>
      </div>
    </div>
  )
}

export default PaymentResult
