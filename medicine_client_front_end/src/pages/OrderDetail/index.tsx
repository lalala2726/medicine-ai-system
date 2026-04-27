import React, { useEffect, useState } from 'react'
import { useParams, useNavigate, useLocation } from 'react-router-dom'
import { ArrowLeft, MapPin, Clock, Truck, ReceiptText } from 'lucide-react'
import { Loading, Dialog, Button } from '@nutui/nutui-react'
import { newOrderTypes } from '@/api/order'
import { OrderStatus } from '@/types/orderStatus'
import { getPayTypeText } from '@/types/payType'
import CancelOrderPopup from '../Orders/components/CancelOrderPopup'
import { showNotify, showSuccessNotify } from '@/utils/notify'
import styles from './index.module.less'

/** 未使用优惠券时的展示文案。 */
const UNUSED_COUPON_TEXT = '未使用优惠券'

const OrderDetail: React.FC = () => {
  const { orderNo } = useParams<{ orderNo: string }>()
  const navigate = useNavigate()
  const location = useLocation()
  const [loading, setLoading] = useState(false)
  const [detail, setDetail] = useState<newOrderTypes.OrderDetailVo | null>(null)
  const [showCancelPopup, setShowCancelPopup] = useState(false)

  /**
   * 获取订单详情。
   *
   * @returns 无返回值。
   */
  const fetchDetail = async () => {
    if (!orderNo) return
    setLoading(true)
    try {
      const res = await newOrderTypes.getOrderDetail(orderNo)
      setDetail(res)
    } catch (error) {
      console.error('获取订单详情失败:', error)
      showNotify('获取订单详情失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchDetail()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orderNo])

  /**
   * 处理返回操作。
   *
   * @returns 无返回值。
   */
  const handleBack = () => {
    if ((location.state as any)?.fromPayment) {
      navigate('/', { replace: true })
    } else {
      navigate(-1)
    }
  }

  /**
   * 跳转支付页。
   *
   * @returns 无返回值。
   */
  const handlePay = async () => {
    if (!detail?.orderNo) return
    navigate(`/order/payment/${detail.orderNo}`)
  }

  /**
   * 打开取消订单弹窗。
   *
   * @returns 无返回值。
   */
  const handleCancel = () => {
    setShowCancelPopup(true)
  }

  /**
   * 确认取消订单。
   *
   * @param orderNo - 当前订单编号。
   * @param reason - 用户填写的取消原因。
   * @returns 无返回值。
   */
  const handleConfirmCancel = async (orderNo: string, reason: string) => {
    try {
      setLoading(true)
      await newOrderTypes.cancelOrder({
        orderNo,
        cancelReason: reason
      })
      showSuccessNotify('订单已取消')
      setShowCancelPopup(false)
      fetchDetail()
    } catch (error) {
      console.error('取消订单失败:', error)
    } finally {
      setLoading(false)
    }
  }

  /**
   * 确认收货。
   *
   * @returns 无返回值。
   */
  const handleConfirmReceipt = () => {
    Dialog.confirm({
      title: '确认收货',
      content: '请确认您已收到商品，确认后订单将完成。',
      onConfirm: async () => {
        try {
          if (!orderNo) return
          setLoading(true)
          await newOrderTypes.confirmReceipt({ orderNo })
          showSuccessNotify('已确认收货')
          navigate('/orders', { replace: true })
        } catch (error) {
          console.error('确认收货失败:', error)
        } finally {
          setLoading(false)
        }
      },
      onCancel: () => {
        console.log('取消确认收货')
      }
    })
  }

  /**
   * 将订单金额格式化为两位小数。
   *
   * @param amount - 后端返回的金额字符串。
   * @returns 两位小数金额文本。
   */
  const formatAmount = (amount?: string) => {
    if (!amount) {
      return '0.00'
    }

    const parsedAmount = Number(amount)
    if (Number.isNaN(parsedAmount)) {
      return '0.00'
    }

    return parsedAmount.toFixed(2)
  }

  if (loading) {
    return (
      <div className={styles.page}>
        <div className={styles.navbar}>
          <div className={styles.navLeft} onClick={handleBack}>
            <ArrowLeft size={24} />
          </div>
          <div className={styles.navTitle}>订单详情</div>
          <div className={styles.navRight} />
        </div>
        <div className={styles.loadingWrapper}>
          <Loading />
          <div style={{ marginTop: 10 }}>加载中...</div>
        </div>
      </div>
    )
  }

  if (!detail) {
    return null
  }

  // 获取状态对应的文本和描述
  const getStatusInfo = () => {
    switch (detail.orderStatus) {
      case OrderStatus.PENDING_PAYMENT:
        return {
          title: '等待支付',
          desc: detail.payExpireTime ? `请在 ${detail.payExpireTime} 前完成支付` : '请尽快完成支付',
          icon: <Clock size={28} className={styles.statusIcon} />
        }
      case OrderStatus.PENDING_SHIPMENT:
        return {
          title: '等待发货',
          desc: '卖家正在为您准备商品，请耐心等待',
          icon: <Truck size={28} className={styles.statusIcon} />
        }
      case OrderStatus.PENDING_RECEIPT:
        return {
          title: '卖家已发货',
          desc: '商品已在路上，请保持电话畅通',
          icon: <Truck size={28} className={styles.statusIcon} />
        }
      case OrderStatus.COMPLETED:
        return {
          title: '订单已完成',
          desc: '感谢您的支持，期待再次为您服务',
          icon: <ReceiptText size={28} className={styles.statusIcon} />
        }
      case OrderStatus.REFUNDED:
        return {
          title: '已退款',
          desc: '订单款项已原路退回',
          icon: <ReceiptText size={28} className={styles.statusIcon} />
        }
      case OrderStatus.CANCELLED:
        return {
          title: '订单已取消',
          desc: '订单已关闭，如有需要可重新下单',
          icon: <Clock size={28} className={styles.statusIcon} />
        }
      default:
        return {
          title: detail.orderStatusName || '订单详情',
          desc: '',
          icon: null
        }
    }
  }

  const statusInfo = getStatusInfo()
  const receiver = detail.receiverInfo || {
    receiverName: detail.receiverName,
    receiverPhone: detail.receiverPhone,
    receiverDetail: detail.receiverDetail
  }
  /** 当前订单是否使用了优惠券。 */
  const hasCoupon = Boolean(detail.couponId || detail.couponName)
  /** 当前优惠券信息展示文案。 */
  const couponDisplayText = detail.couponName || UNUSED_COUPON_TEXT
  /** 当前金额汇总区域的末行标题。 */
  const settlementAmountLabel = detail.paid === 1 ? '实付款' : '应付款'
  /** 当前金额汇总区域的末行金额。 */
  const settlementAmount =
    detail.paid === 1 ? detail.payAmount || detail.totalAmount : detail.totalAmount || detail.payAmount

  return (
    <div className={styles.page}>
      <div className={styles.navbar}>
        <div className={styles.navLeft} onClick={handleBack}>
          <ArrowLeft size={24} />
        </div>
        <div className={styles.navTitle}>订单详情</div>
        <div className={styles.navRight} />
      </div>

      <div className={styles.content}>
        {/* 状态区域 */}
        <div className={styles.statusSection}>
          <div className={styles.statusContent}>
            <div className={styles.statusTextWrapper}>
              <div className={styles.statusText}>{statusInfo.title}</div>
              {detail.afterSaleFlag === 'COMPLETED' && (
                <span className={styles.mainAfterSaleTagCompleted}>售后完成</span>
              )}
            </div>
            <div className={styles.statusDesc}>{statusInfo.desc}</div>
          </div>
          {statusInfo.icon}
        </div>

        {/* 地址信息 */}
        <div className={styles.addressSection}>
          <div className={styles.addressIcon}>
            <MapPin size={20} />
          </div>
          <div className={styles.addressContent}>
            <div className={styles.userInfo}>
              <span className={styles.userName}>{receiver.receiverName}</span>
              <span className={styles.userPhone}>{receiver.receiverPhone}</span>
            </div>
            <div className={styles.addressText}>{receiver.receiverDetail}</div>
          </div>
        </div>

        {/* 商品列表 */}
        <div className={styles.goodsSection}>
          <div className={styles.sectionTitle}>商品信息</div>
          <div className={styles.goodsList}>
            {(detail.items || []).map((item, index) => (
              <div key={index} className={styles.goodsItem}>
                <img src={item.imageUrl} alt={item.productName} className={styles.goodsImage} />
                <div className={styles.goodsInfo}>
                  <div className={styles.goodsNameRow}>
                    <div className={styles.goodsName}>{item.productName}</div>
                    {item.afterSaleStatus === 'IN_PROGRESS' && <span className={styles.afterSaleTag}>售后中</span>}
                    {item.afterSaleStatus === 'COMPLETED' && (
                      <span className={`${styles.afterSaleTag} ${styles.afterSaleTagCompleted}`}>售后完成</span>
                    )}
                  </div>
                  <div className={styles.goodsMeta}>
                    <span className={styles.goodsPrice}>¥{item.price}</span>
                    <span className={styles.goodsCount}>x{item.quantity}</span>
                  </div>
                  {(detail.orderStatus === OrderStatus.PENDING_SHIPMENT ||
                    detail.orderStatus === OrderStatus.PENDING_RECEIPT ||
                    detail.orderStatus === OrderStatus.COMPLETED) && (
                    <div
                      className={styles.itemAfterSaleBtn}
                      onClick={() =>
                        navigate('/after-sale/apply', {
                          state: {
                            orderNo: detail.orderNo,
                            orderItemId: item.id,
                            productImage: item.imageUrl,
                            productName: item.productName,
                            price: item.price,
                            quantity: item.quantity,
                            maxRefundAmount: item.totalPrice || item.price
                          }
                        })
                      }
                    >
                      申请售后
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* 订单信息 */}
        <div className={styles.infoSection}>
          <div className={styles.sectionTitle}>订单信息</div>
          <div className={styles.infoContent}>
            <div className={styles.infoItem}>
              <span className={styles.label}>订单编号</span>
              <span className={styles.value}>{detail.orderNo}</span>
            </div>
            <div className={styles.infoItem}>
              <span className={styles.label}>下单时间</span>
              <span className={styles.value}>{detail.createTime}</span>
            </div>
            <div className={styles.infoItem}>
              <span className={styles.label}>支付方式</span>
              <span className={styles.value}>{getPayTypeText(detail.payType || detail.payTypeName)}</span>
            </div>
            {detail.payTime && (
              <div className={styles.infoItem}>
                <span className={styles.label}>支付时间</span>
                <span className={styles.value}>{detail.payTime}</span>
              </div>
            )}
            <div className={styles.infoItem}>
              <span className={styles.label}>配送方式</span>
              <span className={styles.value}>{detail.deliveryTypeName || '快递配送'}</span>
            </div>
            <div className={styles.infoItem}>
              <span className={styles.label}>优惠券信息</span>
              <span className={`${styles.value} ${hasCoupon ? styles.valueHighlight : ''}`}>{couponDisplayText}</span>
            </div>
          </div>
        </div>

        {/* 金额明细 */}
        <div className={styles.priceSection}>
          <div className={styles.priceContent}>
            <div className={styles.priceItem}>
              <span className={styles.label}>商品总额</span>
              <span className={styles.value}>¥{formatAmount(detail.itemsAmount || detail.totalAmount)}</span>
            </div>
            <div className={styles.priceItem}>
              <span className={styles.label}>优惠券抵扣</span>
              <span className={`${styles.value} ${styles.discountValue}`}>
                -¥{formatAmount(detail.couponDeductAmount)}
              </span>
            </div>
            {hasCoupon && (
              <div className={styles.priceItem}>
                <span className={styles.label}>优惠券消耗</span>
                <span className={styles.value}>¥{formatAmount(detail.couponConsumeAmount)}</span>
              </div>
            )}
            <div className={`${styles.priceItem} ${styles.totalPrice}`}>
              <span className={styles.label}>{settlementAmountLabel}</span>
              <span className={styles.value}>¥{formatAmount(settlementAmount)}</span>
            </div>
          </div>
        </div>
      </div>

      {/* 底部操作栏 */}
      {(detail.orderStatus === OrderStatus.PENDING_PAYMENT ||
        detail.orderStatus === OrderStatus.PENDING_SHIPMENT ||
        detail.orderStatus === OrderStatus.PENDING_RECEIPT ||
        detail.orderStatus === OrderStatus.COMPLETED) && (
        <div className={styles.footer}>
          {detail.orderStatus === OrderStatus.PENDING_PAYMENT && (
            <>
              <Button className={styles.footerBtn} onClick={handleCancel}>
                取消订单
              </Button>
              <Button type='primary' className={styles.footerBtn} onClick={handlePay}>
                去支付
              </Button>
            </>
          )}
          {(detail.orderStatus === OrderStatus.PENDING_SHIPMENT || detail.orderStatus === OrderStatus.COMPLETED) && (
            <Button
              className={styles.footerBtn}
              onClick={() =>
                navigate('/after-sale/apply', {
                  state: {
                    orderNo: detail.orderNo,
                    scope: 'ORDER'
                  }
                })
              }
            >
              申请退款
            </Button>
          )}
          {detail.orderStatus === OrderStatus.PENDING_RECEIPT && (
            <Button type='primary' className={styles.footerBtn} onClick={handleConfirmReceipt}>
              确认收货
            </Button>
          )}
        </div>
      )}

      {/* 取消订单弹窗 */}
      <CancelOrderPopup
        visible={showCancelPopup}
        orderNo={detail.orderNo || ''}
        onClose={() => setShowCancelPopup(false)}
        onConfirm={reason => handleConfirmCancel(detail.orderNo!, reason)}
      />
    </div>
  )
}

export default OrderDetail
