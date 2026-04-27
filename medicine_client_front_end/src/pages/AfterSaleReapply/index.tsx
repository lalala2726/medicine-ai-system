import React, { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { ArrowLeft, ArrowRight } from '@nutui/icons-react'
import { Button, Popup, TextArea } from '@nutui/nutui-react'
import Upload from '@/components/Upload'
import { reapplyAfterSale, OrderAfterSaleTypes } from '@/api/orderAfterSale'
import { showNotify, showSuccessNotify } from '@/utils/notify'
import styles from './index.module.less'

interface LocationState {
  afterSaleNo: string
}

const AfterSaleReapply: React.FC = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const state = location.state as LocationState

  const [loading, setLoading] = useState(false)

  // Form State
  const [applyReason, setApplyReason] = useState<OrderAfterSaleTypes.AfterSaleReasonEnum | ''>('')
  const [applyDescription, setApplyDescription] = useState('')
  const [evidenceImages, setEvidenceImages] = useState<string[]>([])

  // Popup visibility
  const [showReasonPopup, setShowReasonPopup] = useState(false)

  if (!state) {
    return null
  }

  const reasonOptions = [
    { text: '收货地址填错了', value: OrderAfterSaleTypes.AfterSaleReasonEnum.ADDRESS_ERROR },
    { text: '与描述不符', value: OrderAfterSaleTypes.AfterSaleReasonEnum.NOT_AS_DESCRIBED },
    { text: '信息填错了，重新拍', value: OrderAfterSaleTypes.AfterSaleReasonEnum.INFO_ERROR },
    { text: '收到商品损坏了', value: OrderAfterSaleTypes.AfterSaleReasonEnum.DAMAGED },
    { text: '未按预定时间发货', value: OrderAfterSaleTypes.AfterSaleReasonEnum.DELAYED },
    { text: '其它原因', value: OrderAfterSaleTypes.AfterSaleReasonEnum.OTHER }
  ]

  const getReasonName = (val: string) => reasonOptions.find(o => o.value === val)?.text

  const handleSubmit = async () => {
    if (!applyReason) {
      showNotify('请选择申请原因')
      return
    }

    setLoading(true)
    try {
      await reapplyAfterSale({
        afterSaleNo: state.afterSaleNo,
        applyReason,
        applyDescription,
        evidenceImages
      })

      showSuccessNotify('重新申请成功')
      navigate(-1)
    } catch (error) {
      console.error(error)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className={styles.page}>
      <div className={styles.navbar}>
        <div className={styles.navLeft} onClick={() => navigate(-1)}>
          <ArrowLeft width={20} height={20} />
        </div>
        <div className={styles.navTitle}>重新申请售后</div>
        <div className={styles.navRight} />
      </div>

      <div className={styles.card}>
        <div className={styles.formItem} onClick={() => setShowReasonPopup(true)}>
          <span className={styles.label}>申请原因</span>
          <div className={styles.content}>
            <span style={{ color: applyReason ? '#333' : '#ccc' }}>
              {applyReason ? getReasonName(applyReason) : '请选择申请原因'}
            </span>
            <ArrowRight width={14} height={14} className={styles.arrow} />
          </div>
        </div>

        <div className={styles.formItem} style={{ display: 'block', borderBottom: 'none' }}>
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
        <Button block type='primary' onClick={handleSubmit} loading={loading}>
          提交申请
        </Button>
      </div>

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

export default AfterSaleReapply
