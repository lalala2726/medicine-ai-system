import React, { useState } from 'react'
import { Popup, Button, TextArea, Uploader } from '@nutui/nutui-react'
import { ArrowRight } from '@nutui/icons-react'
import { OrderAfterSaleTypes, applyAfterSale } from '@/api/orderAfterSale'
import { showNotify, showSuccessNotify } from '@/utils/notify'
import styles from './index.module.less'

interface RefundOrderPopupProps {
  visible: boolean
  orderNo: string
  onClose: () => void
  onSuccess: () => void
}

const RefundOrderPopup: React.FC<RefundOrderPopupProps> = ({ visible, orderNo, onClose, onSuccess }) => {
  const [loading, setLoading] = useState(false)
  const [applyReason, setApplyReason] = useState<OrderAfterSaleTypes.AfterSaleReasonEnum | ''>('')
  const [applyDescription, setApplyDescription] = useState('')
  const [evidenceImages, setEvidenceImages] = useState<any[]>([])
  const [showReasonPopup, setShowReasonPopup] = useState(false)

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
      showNotify('请选择退款原因')
      return
    }

    setLoading(true)
    try {
      // 模拟图片 URL (如果组件支持自动上传)
      const imageUrls = evidenceImages.map(item => item.url || item.response?.url).filter(Boolean)

      await applyAfterSale({
        orderNo,
        scope: OrderAfterSaleTypes.AfterSaleScopeEnum.ORDER,
        afterSaleType: OrderAfterSaleTypes.AfterSaleTypeEnum.REFUND_ONLY,
        applyReason,
        applyDescription: applyDescription || undefined,
        evidenceImages: imageUrls.length > 0 ? imageUrls : undefined
      })

      showSuccessNotify('申请退款成功')
      onSuccess()
      onClose()
    } catch (error) {
      console.error(error)
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <Popup visible={visible} position='bottom' onClose={onClose} round style={{ height: '70%' }}>
        <div className={styles.popupContent} style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
          <div style={{ textAlign: 'center', padding: '16px', fontWeight: 'bold', fontSize: '16px' }}>申请整单退款</div>

          <div className={styles.formContent} style={{ flex: 1, overflowY: 'auto' }}>
            <div className={styles.formItem} onClick={() => setShowReasonPopup(true)}>
              <span className={styles.label}>退款原因</span>
              <div className={styles.reasonSelect}>
                <span style={{ color: applyReason ? '#333' : '#ccc' }}>
                  {applyReason ? getReasonName(applyReason) : '请选择退款原因'}
                </span>
                <ArrowRight width={14} height={14} color='#ccc' />
              </div>
            </div>

            <div className={styles.formItem}>
              <span className={styles.label}>补充描述</span>
              <TextArea
                value={applyDescription}
                onChange={val => setApplyDescription(val)}
                placeholder='补充描述，有助于商家更好的处理售后问题'
                maxLength={200}
                className={styles.textarea}
              />
            </div>

            <div className={styles.formItem}>
              <span className={styles.label}>凭证图片</span>
              <div className={styles.uploadWrapper}>
                <Uploader
                  // url="YOUR_UPLOAD_API_URL"
                  // fileList={evidenceImages}
                  onChange={files => setEvidenceImages(files)}
                />
              </div>
            </div>
          </div>

          <div className={styles.footer}>
            <Button block type='primary' onClick={handleSubmit} loading={loading}>
              提交申请
            </Button>
          </div>
        </div>
      </Popup>

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
    </>
  )
}

export default RefundOrderPopup
