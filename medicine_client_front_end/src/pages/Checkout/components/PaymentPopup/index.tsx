import React, { useState } from 'react'
import { Popup, Button, Radio } from '@nutui/nutui-react'
import { Wallet } from '@nutui/icons-react'
import styles from './index.module.less'

// 支付方式枚举
type PayTypeEnum = 'WALLET'

interface PaymentPopupProps {
  visible: boolean
  totalAmount: number
  onConfirm: (payMethod: PayTypeEnum) => void
  onClose: () => void
}

const PaymentPopup: React.FC<PaymentPopupProps> = ({ visible, totalAmount, onConfirm, onClose }) => {
  const [selectedMethod, setSelectedMethod] = useState<PayTypeEnum>('WALLET')

  const paymentMethods = [
    {
      value: 'WALLET' as PayTypeEnum,
      label: '钱包支付',
      icon: <Wallet width={24} height={24} />,
      description: '使用账户余额支付'
    }
  ]

  const handleConfirm = () => {
    onConfirm(selectedMethod)
  }

  return (
    <Popup
      visible={visible}
      position='bottom'
      round
      closeable
      onClose={onClose}
      style={{ height: 'auto', maxHeight: '70vh' }}
    >
      <div className={styles.paymentPopup}>
        <div className={styles.popupHeader}>
          <div className={styles.headerTitle}>选择支付方式</div>
          <div className={styles.headerAmount}>
            <span className={styles.amountLabel}>需支付:</span>
            <span className={styles.amountValue}>
              <span className={styles.amountSymbol}>¥</span>
              {totalAmount.toFixed(2)}
            </span>
          </div>
        </div>

        <div className={styles.popupContent}>
          <div className={styles.paymentMethods}>
            {paymentMethods.map(method => (
              <div
                key={method.value}
                className={`${styles.paymentMethod} ${selectedMethod === method.value ? styles.selected : ''}`}
                onClick={() => setSelectedMethod(method.value)}
              >
                <div className={styles.methodLeft}>
                  <div className={styles.methodIcon}>{method.icon}</div>
                  <div className={styles.methodInfo}>
                    <div className={styles.methodLabel}>{method.label}</div>
                    <div className={styles.methodDescription}>{method.description}</div>
                  </div>
                </div>
                <Radio checked={selectedMethod === method.value} />
              </div>
            ))}
          </div>
        </div>

        <div className={styles.popupFooter}>
          <Button type='primary' fill='solid' className={styles.confirmBtn} onClick={handleConfirm} block>
            确认支付
          </Button>
        </div>
      </div>
    </Popup>
  )
}

export default PaymentPopup
