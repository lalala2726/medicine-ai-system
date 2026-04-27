import React from 'react'
import { Button } from '@nutui/nutui-react'
import { OrderAfterSaleTypes } from '@/api/orderAfterSale'
import styles from './index.module.less'

interface AfterSaleCardProps {
  data: OrderAfterSaleTypes.AfterSaleListVo
  onClick: () => void
}

const AfterSaleCard: React.FC<AfterSaleCardProps> = ({ data, onClick }) => {
  return (
    <div className={styles.card} onClick={onClick}>
      <div className={styles.header}>
        <div className={styles.headerLeft}>
          {data.afterSaleTypeName && <span className={styles.typeTag}>{data.afterSaleTypeName}</span>}
          <span className={styles.no}>{data.afterSaleNo}</span>
        </div>
        <span className={styles.status}>{data.afterSaleStatusName}</span>
      </div>

      <div className={styles.content}>
        <img src={data.productImage} className={styles.image} alt={data.productName} />
        <div className={styles.info}>
          <div className={styles.name}>{data.productName}</div>
        </div>
      </div>

      <div className={styles.metaBox}>
        {data.applyReasonName && (
          <div className={styles.metaRow}>
            <span className={styles.metaLabel}>申请原因</span>
            <span className={styles.metaValue}>{data.applyReasonName}</span>
          </div>
        )}
        {data.refundAmount && (
          <div className={styles.metaRow}>
            <span className={styles.metaLabel}>退款金额</span>
            <span className={styles.metaAmount}>¥{data.refundAmount}</span>
          </div>
        )}
        {data.applyTime && (
          <div className={styles.metaRow}>
            <span className={styles.metaLabel}>申请时间</span>
            <span className={styles.metaTime}>{data.applyTime}</span>
          </div>
        )}
      </div>

      <div className={styles.footer}>
        <Button
          size='small'
          fill='outline'
          className={styles.actionBtn}
          onClick={e => {
            e.stopPropagation()
            onClick()
          }}
        >
          查看详情
        </Button>
      </div>
    </div>
  )
}

export default AfterSaleCard
