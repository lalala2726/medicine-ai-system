import React, { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Loading, Button, Dialog, ImagePreview, Popup, Step, Steps, NavBar } from '@nutui/nutui-react'
import { getAfterSaleDetail, cancelAfterSale, OrderAfterSaleTypes } from '@/api/orderAfterSale'
import { showNotify, showSuccessNotify } from '@/utils/notify'
import styles from './index.module.less'

const AfterSaleDetail: React.FC = () => {
  const { afterSaleNo } = useParams<{ afterSaleNo: string }>()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [detail, setDetail] = useState<OrderAfterSaleTypes.AfterSaleDetailVo | null>(null)
  const [showPreview, setShowPreview] = useState(false)
  const [previewIndex, setPreviewIndex] = useState(0)
  const [showTimeline, setShowTimeline] = useState(false)

  const fetchDetail = async () => {
    if (!afterSaleNo) return
    setLoading(true)
    try {
      const res = await getAfterSaleDetail(afterSaleNo)
      setDetail(res)
    } catch (error) {
      console.error('获取售后详情失败:', error)
      showNotify('获取售后详情失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchDetail()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [afterSaleNo])

  const handleCancel = () => {
    Dialog.confirm({
      title: '提示',
      content: '确定要取消售后申请吗？',
      onConfirm: async () => {
        try {
          if (!detail?.id) return
          await cancelAfterSale({ afterSaleId: detail.id })
          showSuccessNotify('取消成功')
          fetchDetail()
        } catch (error) {
          console.error(error)
        }
      }
    })
  }

  const handleReapply = () => {
    if (!detail?.afterSaleNo) return
    navigate('/after-sale/reapply', {
      state: {
        afterSaleNo: detail.afterSaleNo
      }
    })
  }

  const handleImageClick = (index: number) => {
    setPreviewIndex(index)
    setShowPreview(true)
  }

  if (loading && !detail) {
    return (
      <div className={styles.page}>
        <div className={styles.topBar}>
          <NavBar back={<ChevronLeft size={24} color='#0d1b12' />} onBackClick={() => navigate(-1)}>
            <div className='title'>售后详情</div>
          </NavBar>
        </div>
        <div className={styles.loadingWrapper}>
          <Loading />
          <div style={{ marginTop: 10 }}>加载中...</div>
        </div>
      </div>
    )
  }

  if (!detail) return null

  return (
    <div className={styles.page}>
      <div className={styles.topBar}>
        <NavBar back={<ChevronLeft size={24} color='#0d1b12' />} onBackClick={() => navigate(-1)}>
          <div className='title'>售后详情</div>
        </NavBar>
      </div>

      <div className={styles.content}>
        {/* 状态信息 */}
        <div className={styles.statusCard} onClick={() => setShowTimeline(true)}>
          <div>
            <div className={styles.statusText}>{detail.afterSaleStatusName}</div>
            <div className={styles.statusDesc}>
              {detail.rejectReason ? `拒绝原因: ${detail.rejectReason}` : detail.applyTime}
            </div>
          </div>
          <div className={styles.viewProgress}>
            进度详情 <ChevronRight size={14} style={{ marginLeft: 4 }} />
          </div>
        </div>

        {/* 商品信息 */}
        <div className={styles.card}>
          <div className={styles.cardTitle}>退款商品</div>
          <div className={styles.productItem}>
            <img src={detail.productInfo?.productImage} className={styles.productImage} alt='' />
            <div className={styles.productInfo}>
              <div className={styles.productName}>{detail.productInfo?.productName}</div>
              <div className={styles.productPrice}>
                ¥ {detail.productInfo?.productPrice} x {detail.productInfo?.quantity}
              </div>
            </div>
          </div>
        </div>

        {/* 售后信息 */}
        <div className={styles.card}>
          <div className={styles.cardTitle}>售后信息</div>
          <div className={styles.infoItem}>
            <span className={styles.label}>售后类型</span>
            <span className={styles.value}>{detail.afterSaleTypeName}</span>
          </div>
          <div className={styles.infoItem}>
            <span className={styles.label}>退款金额</span>
            <span className={styles.value} style={{ color: 'var(--nutui-color-primary)', fontSize: '16px' }}>
              ¥ {detail.refundAmount}
            </span>
          </div>
          <div className={styles.infoItem}>
            <span className={styles.label}>申请原因</span>
            <span className={styles.value}>{detail.applyReasonName}</span>
          </div>
          <div className={styles.infoItem}>
            <span className={styles.label}>收货状态</span>
            <span className={styles.value}>{detail.receiveStatusName}</span>
          </div>
          <div className={styles.infoItem}>
            <span className={styles.label}>售后单号</span>
            <span className={styles.value}>{detail.afterSaleNo}</span>
          </div>
          <div className={styles.infoItem}>
            <span className={styles.label}>申请时间</span>
            <span className={styles.value}>{detail.applyTime}</span>
          </div>
          {detail.auditTime && (
            <div className={styles.infoItem}>
              <span className={styles.label}>审核时间</span>
              <span className={styles.value}>{detail.auditTime}</span>
            </div>
          )}
          {detail.applyDescription && (
            <div className={styles.infoItem}>
              <span className={styles.label}>详细说明</span>
              <span className={styles.value}>{detail.applyDescription}</span>
            </div>
          )}
          {detail.evidenceImages && detail.evidenceImages.length > 0 && (
            <div className={styles.infoItem} style={{ display: 'block' }}>
              <div className={styles.label} style={{ marginBottom: 12 }}>
                凭证图片
              </div>
              <div className={styles.evidenceImages}>
                {detail.evidenceImages.map((img, index) => (
                  <img key={index} src={img} alt='' onClick={() => handleImageClick(index)} />
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* 底部操作栏 */}
      {['PENDING', 'REJECTED'].includes(detail.afterSaleStatus || '') && (
        <div className={styles.bottomBar}>
          {detail.afterSaleStatus === 'PENDING' && <Button onClick={handleCancel}>取消申请</Button>}
          {detail.afterSaleStatus === 'REJECTED' && (
            <Button type='primary' onClick={handleReapply}>
              重新申请
            </Button>
          )}
        </div>
      )}

      <ImagePreview
        images={detail.evidenceImages?.map(src => ({ src })) || []}
        visible={showPreview}
        defaultValue={previewIndex}
        onClose={() => setShowPreview(false)}
      />

      {/* 进度弹窗 */}
      <Popup
        visible={showTimeline}
        position='bottom'
        onClose={() => setShowTimeline(false)}
        round
        style={{ height: '60%' }}
      >
        <div className={styles.popupContent}>
          <div className={styles.popupTitle}>售后进度</div>
          <div className={styles.timelineWrapper}>
            <Steps direction='vertical' value={detail.timeline?.length || 1}>
              {detail.timeline?.map((item, index) => (
                <Step
                  key={item.id || index}
                  title={item.eventTypeName}
                  description={
                    <div>
                      <div>{item.description}</div>
                      <div style={{ fontSize: '12px', color: '#9ca3af', marginTop: '4px' }}>{item.createTime}</div>
                    </div>
                  }
                  value={index + 1}
                />
              ))}
            </Steps>
          </div>
        </div>
      </Popup>
    </div>
  )
}

export default AfterSaleDetail
