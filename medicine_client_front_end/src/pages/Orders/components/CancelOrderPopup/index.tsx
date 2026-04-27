import React from 'react'
import { Popup } from '@nutui/nutui-react'
import styles from './index.module.less'

interface CancelOrderPopupProps {
  visible: boolean
  orderNo: string
  onConfirm: (orderNo: string, reason: string) => void
  onClose: () => void
}

const CancelOrderPopup: React.FC<CancelOrderPopupProps> = ({ visible, orderNo, onConfirm, onClose }) => {
  const cancelReasons = [
    '不想买了',
    '商品信息填写错误',
    '重复下单',
    '商品降价了',
    '配送时间太长',
    '找到更便宜的了',
    '其他原因'
  ]

  const handleSelect = (reason: string) => {
    onConfirm(orderNo, reason)
  }

  return (
    <Popup visible={visible} position='bottom' onClose={onClose} round>
      <div className={styles.popupContent}>
        <div className={styles.title}>请选择取消原因</div>
        {cancelReasons.map(reason => (
          <div key={reason} className={styles.popupItem} onClick={() => handleSelect(reason)}>
            {reason}
          </div>
        ))}
        <div className={`${styles.popupItem} ${styles.cancelBtn}`} onClick={onClose}>
          取消
        </div>
      </div>
    </Popup>
  )
}

export default CancelOrderPopup
