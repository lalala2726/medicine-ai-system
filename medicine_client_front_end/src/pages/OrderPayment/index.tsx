import React, { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Button } from '@nutui/nutui-react'
import { ArrowLeft, Checked, Wallet } from '@nutui/icons-react'
import { newOrderTypes, type PayTypeEnum } from '@/api/order.ts'
import { OrderStatus } from '@/types/orderStatus'
import { getPayTypeText } from '@/types/payType'
import { showNotify } from '@/utils/notify'
import styles from './index.module.less'

/**
 * 支付方式选项定义。
 */
interface PaymentOption {
  value: PayTypeEnum
  label: string
  icon: React.ReactNode
  description: string
  recommend?: boolean
}

const OrderPayment: React.FC = () => {
  const navigate = useNavigate()
  const { orderNo } = useParams<{ orderNo: string }>()

  const [loading, setLoading] = useState(false)
  const [fetching, setFetching] = useState(true)
  const [payMethod, setPayMethod] = useState<PayTypeEnum>('WALLET')
  const [payInfo, setPayInfo] = useState<newOrderTypes.OrderDetailVo.OrderPayInfoVo | null>(null)

  useEffect(() => {
    if (!orderNo) {
      showNotify('订单信息不存在')
      navigate('/orders')
      return
    }

    const fetchPayInfo = async () => {
      try {
        setFetching(true)
        const res = await newOrderTypes.getOrderPayInfo(orderNo)
        if (res) {
          setPayInfo(res)
        } else {
          showNotify('获取订单支付信息失败')
          navigate('/orders')
        }
      } catch (error) {
        console.error('获取订单支付信息失败:', error)
        navigate('/orders')
      } finally {
        setFetching(false)
      }
    }

    fetchPayInfo()
  }, [orderNo, navigate])

  // 返回上一页
  const handleBack = () => {
    navigate(-1)
  }

  /**
   * 跳转订单详情页。
   * @returns 无返回值。
   */
  const handleViewOrderDetail = () => {
    if (!orderNo) {
      navigate('/orders')
      return
    }
    navigate(`/orders/${orderNo}`, { replace: true })
  }

  /**
   * 判断当前订单是否允许继续支付。
   * @returns boolean 是否允许继续支付。
   */
  const canPay = payInfo?.orderStatus === OrderStatus.PENDING_PAYMENT && payInfo?.paid !== 1

  /**
   * 判断当前订单是否已经支付完成。
   * @returns boolean 是否已支付完成。
   */
  const paidCompleted = payInfo?.paid === 1

  // 立即支付
  const handlePay = async () => {
    if (!orderNo) {
      showNotify('订单信息不存在')
      return
    }
    if (!canPay) {
      showNotify('当前订单无需支付')
      return
    }

    try {
      setLoading(true)

      const result = await newOrderTypes.payOrder({
        orderNo: orderNo,
        payMethod
      })

      // 根据支付状态跳转
      if (result?.paymentStatus === 'SUCCESS') {
        // 钱包支付成功，直接跳转到支付成功页面
        navigate('/payment/result', {
          state: {
            success: true,
            orderNo: result.orderNo,
            totalAmount: result.payAmount,
            paymentMethod: result.paymentMethod,
            orderStatus: OrderStatus.PENDING_SHIPMENT
          },
          replace: true
        })
      } else {
        showNotify('支付失败，请重试')
      }
    } catch (error) {
      console.error('支付失败:', error)
      // Notify 已在 requestClient 中处理
    } finally {
      setLoading(false)
    }
  }

  // 支付方式选项
  const paymentOptions: PaymentOption[] = [
    {
      value: 'WALLET',
      label: '钱包支付',
      icon: <Wallet />,
      description: '使用账户余额支付'
    }
  ]

  if (fetching) {
    return (
      <div className={styles.orderPayment}>
        <div className={styles.paymentHeader}>
          <div className={styles.headerLeft} onClick={handleBack}>
            <ArrowLeft width={20} height={20} />
          </div>
          <div className={styles.headerTitle}>收银台</div>
          <div className={styles.headerRight}></div>
        </div>
        <div style={{ padding: '20px', textAlign: 'center' }}>加载中...</div>
      </div>
    )
  }

  return (
    <div className={styles.orderPayment}>
      {/* 顶部导航栏 */}
      <div className={styles.paymentHeader}>
        <div className={styles.headerLeft} onClick={handleBack}>
          <ArrowLeft width={20} height={20} />
        </div>
        <div className={styles.headerTitle}>收银台</div>
        <div className={styles.headerRight}></div>
      </div>

      {/* 支付内容 */}
      <div className={styles.paymentContent}>
        {/* 金额区域 */}
        <div className={styles.amountSection}>
          <span className={styles.currency}>¥</span>
          <span className={styles.amount}>{payInfo?.totalAmount || '0.00'}</span>
        </div>

        {/* 支付完成提示 */}
        {paidCompleted && (
          <div className={styles.countdownSection}>
            <div className={`${styles.countdownPill} ${styles.paidPill}`}>支付完成</div>
          </div>
        )}

        {/* 待支付倒计时 */}
        {canPay && payInfo?.payExpireTime && (
          <div className={styles.countdownSection}>
            <div className={styles.countdownPill}>请在 {payInfo.payExpireTime} 前完成支付</div>
          </div>
        )}

        {/* 非待支付状态提示 */}
        {!canPay && !paidCompleted && payInfo?.orderStatusName && (
          <div className={styles.countdownSection}>
            <div className={styles.countdownPill}>当前订单状态：{payInfo.orderStatusName}</div>
          </div>
        )}

        {/* 订单信息卡片 */}
        <div className={styles.orderSummarySection}>
          <div className={styles.summaryItem}>
            <span className={styles.label}>订单编号</span>
            <span className={styles.value}>{orderNo}</span>
          </div>
          {payInfo?.productSummary && (
            <div className={styles.summaryItem}>
              <span className={styles.label}>商品摘要</span>
              <span className={styles.value}>{payInfo.productSummary}</span>
            </div>
          )}
          {paidCompleted && (
            <div className={styles.summaryItem}>
              <span className={styles.label}>支付方式</span>
              <span className={styles.value}>{getPayTypeText(payInfo?.payType)}</span>
            </div>
          )}
          {paidCompleted && payInfo?.payTime && (
            <div className={styles.summaryItem}>
              <span className={styles.label}>支付时间</span>
              <span className={styles.value}>{payInfo.payTime}</span>
            </div>
          )}
        </div>

        {/* 支付工具卡片 */}
        {canPay && (
          <div className={styles.paymentToolsCard}>
            <div className={styles.cardTitle}>支付工具</div>
            <div className={styles.paymentList}>
              {paymentOptions.map(option => (
                <div key={option.value} className={styles.paymentItem} onClick={() => setPayMethod(option.value)}>
                  <div className={styles.iconWrapper}>{React.isValidElement(option.icon) ? option.icon : null}</div>
                  <div className={styles.info}>
                    <div className={styles.labelRow}>
                      <span className={styles.label}>{option.label}</span>
                      {option.recommend && <span className={styles.recommendTag}>推荐</span>}
                    </div>
                    <span className={styles.desc}>{option.description}</span>
                  </div>
                  <div className={styles.checkIcon}>
                    {payMethod === option.value ? (
                      <Checked width={22} height={22} color='var(--nutui-color-primary, #ff6b00)' />
                    ) : (
                      <div className={styles.radioUnchecked} />
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* 底部支付按钮 */}
      <div className={styles.paymentFooter}>
        {canPay ? (
          <Button
            block
            type='primary'
            fill='solid'
            className={styles.confirmBtn}
            onClick={handlePay}
            loading={loading}
            disabled={loading}
          >
            {loading ? '支付中...' : `确认支付 ¥${payInfo?.totalAmount || '0.00'}`}
          </Button>
        ) : (
          <Button block type='primary' fill='solid' className={styles.confirmBtn} onClick={handleViewOrderDetail}>
            查看订单详情
          </Button>
        )}
      </div>
    </div>
  )
}

export default OrderPayment
