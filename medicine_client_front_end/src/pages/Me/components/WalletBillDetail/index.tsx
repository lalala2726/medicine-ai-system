import React, { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Loading } from '@nutui/nutui-react'
import { ChevronLeft } from 'lucide-react'
import { getBillDetail, type UserTypes } from '@/api/user'
import { showNotify } from '@/utils/notify'
import styles from './index.module.less'

/** 钱包账单变动类型文案映射。 */
const WALLET_BILL_CHANGE_TYPE_TEXT_MAP: Record<number, string> = {
  1: '收入',
  2: '支出',
  3: '冻结',
  4: '解冻'
}

/** 视为正向金额展示的账单类型集合。 */
const POSITIVE_WALLET_BILL_CHANGE_TYPES = new Set<number>([1, 4])

/**
 * 获取账单变动类型文案。
 *
 * @param changeType - 账单变动类型
 * @returns 账单变动类型文案
 */
const getBillChangeTypeText = (changeType?: number) => {
  if (!changeType) {
    return '账单明细'
  }

  return WALLET_BILL_CHANGE_TYPE_TEXT_MAP[changeType] || '账单明细'
}

/**
 * 格式化金额为两位小数字符串。
 *
 * @param amount - 原始金额
 * @returns 两位小数金额文案
 */
const formatMoneyValue = (amount?: string) => {
  if (!amount) {
    return '0.00'
  }

  const numericAmount = Number(amount)
  if (Number.isNaN(numericAmount)) {
    return amount
  }

  return numericAmount.toFixed(2)
}

/**
 * 格式化绝对值金额为两位小数字符串。
 *
 * @param amount - 原始金额
 * @returns 去除正负号后的两位小数金额文案
 */
const formatAbsoluteMoneyValue = (amount?: string) => {
  if (!amount) {
    return '0.00'
  }

  const numericAmount = Number(amount)
  if (Number.isNaN(numericAmount)) {
    return amount.replace(/^[+-]/, '')
  }

  return Math.abs(numericAmount).toFixed(2)
}

/**
 * 格式化账单金额摘要。
 *
 * @param amount - 原始金额
 * @param changeType - 账单变动类型
 * @returns 带正负号的金额文案
 */
const formatBillSummaryAmount = (amount?: string, changeType?: number) => {
  const sign = POSITIVE_WALLET_BILL_CHANGE_TYPES.has(changeType || 0) ? '+' : '-'
  return `${sign}${formatAbsoluteMoneyValue(amount)}`
}

/**
 * 获取账单金额颜色。
 *
 * @param changeType - 账单变动类型
 * @returns 金额展示颜色
 */
const getBillAmountColor = (changeType?: number) => {
  return POSITIVE_WALLET_BILL_CHANGE_TYPES.has(changeType || 0) ? '#ef4444' : '#0d1b12'
}

/** 钱包账单详情页组件。 */
const WalletBillDetail: React.FC = () => {
  const navigate = useNavigate()
  const { billId } = useParams<{ billId: string }>()
  const [loading, setLoading] = useState(false)
  const [detail, setDetail] = useState<UserTypes.UserWalletBillDetailVo | null>(null)

  /**
   * 获取钱包账单详情。
   *
   * @returns 无返回值
   */
  const fetchDetail = async () => {
    if (!billId) {
      return
    }

    setLoading(true)
    try {
      const result = await getBillDetail(billId)
      setDetail(result)
    } catch (error) {
      console.error('获取账单详情失败:', error)
      showNotify('获取账单详情失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void fetchDetail()
  }, [billId])

  if (loading && !detail) {
    return (
      <div className={styles.page}>
        <div className={styles.navbar}>
          <div className={styles.navLeft} onClick={() => navigate(-1)}>
            <ChevronLeft size={24} />
          </div>
          <div className={styles.navTitle}>账单详情</div>
          <div className={styles.navRight} />
        </div>
        <div className={styles.loadingWrapper}>
          <Loading />
          <div className={styles.loadingText}>加载中...</div>
        </div>
      </div>
    )
  }

  if (!detail) {
    return null
  }

  return (
    <div className={styles.page}>
      <div className={styles.navbar}>
        <div className={styles.navLeft} onClick={() => navigate(-1)}>
          <ChevronLeft size={24} />
        </div>
        <div className={styles.navTitle}>账单详情</div>
        <div className={styles.navRight} />
      </div>

      <div className={styles.content}>
        <div className={styles.summaryCard}>
          <div className={styles.summaryType}>{detail.title || getBillChangeTypeText(detail.changeType)}</div>
          <div className={styles.summaryAmount} style={{ color: getBillAmountColor(detail.changeType) }}>
            {formatBillSummaryAmount(detail.amount, detail.changeType)}
          </div>
          <div className={styles.summaryTime}>{detail.time || ''}</div>
        </div>

        <div className={styles.card}>
          <div className={styles.cardTitle}>账单信息</div>
          <div className={styles.infoRow}>
            <span className={styles.label}>账单类型</span>
            <span className={styles.value}>{getBillChangeTypeText(detail.changeType)}</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.label}>流水标题</span>
            <span className={styles.value}>{detail.title || '钱包流水'}</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.label}>流水编号</span>
            <span className={styles.value}>{detail.flowNo || '--'}</span>
          </div>
          {detail.bizId ? (
            <div className={styles.infoRow}>
              <span className={styles.label}>业务单号</span>
              <span className={styles.value}>{detail.bizId}</span>
            </div>
          ) : null}
          <div className={styles.infoRow}>
            <span className={styles.label}>账单时间</span>
            <span className={styles.value}>{detail.time || '--'}</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.label}>变动金额</span>
            <span className={styles.valueAmount} style={{ color: getBillAmountColor(detail.changeType) }}>
              {formatBillSummaryAmount(detail.amount, detail.changeType)}
            </span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.label}>变动前余额</span>
            <span className={styles.value}>¥{formatMoneyValue(detail.beforeBalance)}</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.label}>变动后余额</span>
            <span className={styles.value}>¥{formatMoneyValue(detail.afterBalance)}</span>
          </div>
          {detail.remark ? (
            <div className={styles.infoRow}>
              <span className={styles.label}>备注说明</span>
              <span className={styles.value}>{detail.remark}</span>
            </div>
          ) : null}
        </div>
      </div>
    </div>
  )
}

export default WalletBillDetail
