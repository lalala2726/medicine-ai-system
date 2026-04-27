import React, { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { ArrowLeft, ArrowRight } from '@nutui/icons-react'
import { Button, Popup, TextArea, Loading } from '@nutui/nutui-react'
import Upload from '@/components/Upload'
import { applyAfterSale, checkAfterSaleEligibility, OrderAfterSaleTypes } from '@/api/orderAfterSale'
import { showNotify, showSuccessNotify } from '@/utils/notify'
import styles from './index.module.less'

/**
 * 统一售后申请页
 * - scope=ITEM: 单商品售后（从商品卡片"申请售后"进入）
 * - scope=ORDER: 整单退款（从底部"申请退款"进入）
 */

interface LocationState {
  orderNo: string
  /** scope=ITEM 时传入 */
  orderItemId?: string
  productImage?: string
  productName?: string
  price?: string
  quantity?: number
  maxRefundAmount?: string
  /** 申请范围：ORDER-整单 / ITEM-单商品，默认 ITEM */
  scope?: 'ORDER' | 'ITEM'
}

const AfterSaleApply: React.FC = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const state = location.state as LocationState

  const scope =
    state?.scope === 'ORDER'
      ? OrderAfterSaleTypes.AfterSaleScopeEnum.ORDER
      : OrderAfterSaleTypes.AfterSaleScopeEnum.ITEM
  const isOrderScope = scope === OrderAfterSaleTypes.AfterSaleScopeEnum.ORDER

  const [loading, setLoading] = useState(false)
  const [eligibilityLoading, setEligibilityLoading] = useState(true)
  const [eligibility, setEligibility] = useState<OrderAfterSaleTypes.EligibilityResponse | null>(null)

  // Form State
  const [afterSaleType, setAfterSaleType] = useState<OrderAfterSaleTypes.AfterSaleTypeEnum>(
    OrderAfterSaleTypes.AfterSaleTypeEnum.REFUND_ONLY
  )
  const [applyReason, setApplyReason] = useState<OrderAfterSaleTypes.AfterSaleReasonEnum | ''>('')
  const [refundAmount, setRefundAmount] = useState('')
  const [applyDescription, setApplyDescription] = useState('')
  const [evidenceImages, setEvidenceImages] = useState<string[]>([])

  // Popup visibility
  const [showTypePopup, setShowTypePopup] = useState(false)
  const [showReasonPopup, setShowReasonPopup] = useState(false)

  // 资格校验
  useEffect(() => {
    if (!state?.orderNo) return
    const fetchEligibility = async () => {
      setEligibilityLoading(true)
      try {
        const res = await checkAfterSaleEligibility({
          orderNo: state.orderNo,
          scope,
          ...(scope === OrderAfterSaleTypes.AfterSaleScopeEnum.ITEM && state.orderItemId
            ? { orderItemId: Number(state.orderItemId) }
            : {})
        })
        setEligibility(res)
        // 单商品模式下设置退款金额默认值
        if (!isOrderScope) {
          if (res?.selectedRefundableAmount != null) {
            setRefundAmount(String(res.selectedRefundableAmount))
          } else if (state.maxRefundAmount) {
            setRefundAmount(state.maxRefundAmount)
          }
        }
      } catch (error) {
        console.error('资格校验失败:', error)
        if (!isOrderScope && state.maxRefundAmount) {
          setRefundAmount(state.maxRefundAmount)
        }
      } finally {
        setEligibilityLoading(false)
      }
    }
    fetchEligibility()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  if (!state) {
    return null
  }

  const maxRefund = !isOrderScope
    ? eligibility?.selectedRefundableAmount != null
      ? eligibility.selectedRefundableAmount
      : Number(state.maxRefundAmount || 0)
    : 0

  const typeOptions = [
    { text: '仅退款', value: OrderAfterSaleTypes.AfterSaleTypeEnum.REFUND_ONLY },
    { text: '退货退款', value: OrderAfterSaleTypes.AfterSaleTypeEnum.RETURN_REFUND },
    { text: '换货', value: OrderAfterSaleTypes.AfterSaleTypeEnum.EXCHANGE }
  ]

  const reasonOptions = [
    { text: '收货地址填错了', value: OrderAfterSaleTypes.AfterSaleReasonEnum.ADDRESS_ERROR },
    { text: '与描述不符', value: OrderAfterSaleTypes.AfterSaleReasonEnum.NOT_AS_DESCRIBED },
    { text: '信息填错了，重新拍', value: OrderAfterSaleTypes.AfterSaleReasonEnum.INFO_ERROR },
    { text: '收到商品损坏了', value: OrderAfterSaleTypes.AfterSaleReasonEnum.DAMAGED },
    { text: '未按预定时间发货', value: OrderAfterSaleTypes.AfterSaleReasonEnum.DELAYED },
    { text: '其它原因', value: OrderAfterSaleTypes.AfterSaleReasonEnum.OTHER }
  ]

  const getTypeName = (val: string) => typeOptions.find(o => o.value === val)?.text
  const getReasonName = (val: string) => reasonOptions.find(o => o.value === val)?.text

  const handleSubmit = async () => {
    if (!applyReason) {
      showNotify(isOrderScope ? '请选择退款原因' : '请选择申请原因')
      return
    }
    if (!isOrderScope) {
      if (!refundAmount) {
        showNotify('请输入退款金额')
        return
      }
      if (Number(refundAmount) > maxRefund) {
        showNotify(`退款金额不能超过¥${maxRefund}`)
        return
      }
    }

    setLoading(true)
    try {
      const res = await applyAfterSale({
        orderNo: state.orderNo,
        scope,
        ...(isOrderScope
          ? { afterSaleType: OrderAfterSaleTypes.AfterSaleTypeEnum.REFUND_ONLY }
          : {
              orderItemId: state.orderItemId,
              afterSaleType,
              refundAmount
            }),
        applyReason,
        applyDescription: applyDescription || undefined,
        evidenceImages: evidenceImages.length > 0 ? evidenceImages : undefined
      })

      if (res.requestedScope !== res.resolvedScope) {
        showSuccessNotify('已自动按整单发起售后')
      } else {
        showSuccessNotify(isOrderScope ? '申请退款成功' : '申请提交成功')
      }
      navigate(-1)
    } catch (error) {
      console.error(error)
    } finally {
      setLoading(false)
    }
  }

  const isEligible = eligibility ? eligibility.eligible : true
  const reasonPlaceholder = isOrderScope ? '请选择退款原因' : '请选择申请原因'
  const amountPlaceholder = maxRefund > 0 ? `最多可退${maxRefund}` : '请输入退款金额'

  return (
    <div className={styles.page}>
      <div className={styles.navbar}>
        <div className={styles.navLeft} onClick={() => navigate(-1)}>
          <ArrowLeft width={20} height={20} />
        </div>
        <div className={styles.navTitle}>{isOrderScope ? '申请整单退款' : '申请售后'}</div>
        <div className={styles.navRight} />
      </div>

      {eligibilityLoading ? (
        <div className={styles.loadingWrapper}>
          <Loading />
        </div>
      ) : (
        <>
          {/* 不可申请提示 */}
          {!isEligible && eligibility && (
            <div className={styles.card}>
              <div className={styles.warningText}>{eligibility.reasonMessage}</div>
            </div>
          )}

          {/* 售后截止时间 */}
          {eligibility?.afterSaleDeadlineTime && (
            <div className={styles.card}>
              <div className={styles.deadlineText}>售后截止时间：{eligibility.afterSaleDeadlineTime}</div>
            </div>
          )}

          {/* 整单模式：展示商品列表 */}
          {isOrderScope && eligibility?.items && eligibility.items.length > 0 && (
            <div className={styles.card}>
              <div className={styles.sectionTitle}>退款商品</div>
              {eligibility.items.map(item => (
                <div key={item.orderItemId} className={styles.productItem}>
                  <img src={item.imageUrl} className={styles.productImage} alt='' />
                  <div className={styles.productInfo}>
                    <div className={styles.productName}>{item.productName}</div>
                    <div className={styles.productPrice}>
                      <span>
                        ¥{item.price} x {item.quantity}
                      </span>
                      <span className={styles.productHint}>可退¥{item.refundableAmount}</span>
                    </div>
                    {!item.eligible && <div className={styles.productWarning}>{item.reasonMessage}</div>}
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* 单商品模式：展示单个商品 */}
          {!isOrderScope && (
            <div className={styles.card}>
              <div className={styles.productItem}>
                <img src={state.productImage} className={styles.productImage} alt='' />
                <div className={styles.productInfo}>
                  <div className={styles.productName}>{state.productName}</div>
                  <div className={styles.productPrice}>
                    ¥{state.price} x {state.quantity}
                  </div>
                </div>
              </div>
            </div>
          )}

          <div className={styles.card}>
            {/* 单商品模式：售后类型选择 */}
            {!isOrderScope && (
              <div className={styles.formItem} onClick={() => setShowTypePopup(true)}>
                <span className={styles.label}>售后类型</span>
                <div className={styles.content}>
                  <span>{getTypeName(afterSaleType)}</span>
                  <ArrowRight width={14} height={14} className={styles.arrow} />
                </div>
              </div>
            )}

            <div className={styles.formItem} onClick={() => setShowReasonPopup(true)}>
              <span className={styles.label}>{isOrderScope ? '退款原因' : '申请原因'}</span>
              <div className={styles.content}>
                <span className={applyReason ? styles.valueText : styles.placeholderText}>
                  {applyReason ? getReasonName(applyReason) : reasonPlaceholder}
                </span>
                <ArrowRight width={14} height={14} className={styles.arrow} />
              </div>
            </div>

            <div className={styles.formItem}>
              <span className={styles.label}>退款金额</span>
              <div className={`${styles.content} ${styles.amountContent}`}>
                {isOrderScope ? (
                  <span className={styles.amountValue}>¥{eligibility?.totalRefundableAmount ?? '--'}</span>
                ) : (
                  <label className={styles.amountInputWrapper}>
                    <input
                      className={styles.amountField}
                      type='number'
                      value={refundAmount}
                      onChange={e => setRefundAmount(e.target.value)}
                      placeholder={amountPlaceholder}
                      inputMode='decimal'
                    />
                    <span className={styles.currencySymbol}>¥</span>
                  </label>
                )}
              </div>
            </div>

            <div className={`${styles.formItem} ${styles.formItemBlock}`}>
              <span className={styles.label}>补充描述和凭证</span>
              <div className={styles.textareaWrapper}>
                <TextArea
                  value={applyDescription}
                  onChange={val => setApplyDescription(val)}
                  placeholder='补充描述，有助于商家更好的处理售后问题'
                  maxLength={200}
                />
                <div className={styles.uploadWrapper}>
                  <Upload value={evidenceImages} onChange={setEvidenceImages} maxCount={5} uploadLabel='上传凭证' />
                </div>
              </div>
            </div>
          </div>

          <div className={styles.bottomBar}>
            <Button
              block
              type='primary'
              onClick={handleSubmit}
              loading={loading}
              disabled={!isEligible}
              className={styles.submitButton}
            >
              提交申请
            </Button>
          </div>
        </>
      )}

      {/* Popups */}
      {!isOrderScope && (
        <Popup visible={showTypePopup} position='bottom' onClose={() => setShowTypePopup(false)} round>
          <div className={styles.popupContent}>
            {typeOptions.map(opt => (
              <div
                key={opt.value}
                className={`${styles.popupItem} ${afterSaleType === opt.value ? styles.active : ''}`}
                onClick={() => {
                  setAfterSaleType(opt.value)
                  setShowTypePopup(false)
                }}
              >
                {opt.text}
              </div>
            ))}
          </div>
        </Popup>
      )}

      <Popup visible={showReasonPopup} position='bottom' onClose={() => setShowReasonPopup(false)} round>
        <div className={styles.popupContent}>
          {reasonOptions.map(opt => (
            <div
              key={opt.value}
              className={`${styles.popupItem} ${applyReason === opt.value ? styles.active : ''}`}
              onClick={() => {
                setApplyReason(opt.value)
                setShowReasonPopup(false)
              }}
            >
              {opt.text}
            </div>
          ))}
        </div>
      </Popup>
    </div>
  )
}

export default AfterSaleApply
