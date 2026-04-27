import React, { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronLeft, ChevronRight, Eye, EyeOff } from 'lucide-react'
import { useUserStore } from '@/stores/userStore'
import { getUserWalletBalance } from '@/api/user'
import { showNotify } from '@/utils/notify'
import PullRefresh from '@/components/PullRefresh'
import styles from './index.module.less'

const Wallet: React.FC = () => {
  const navigate = useNavigate()
  const { user, updateUser } = useUserStore()
  const walletPageRef = useRef<HTMLDivElement>(null)
  const [showBalance, setShowBalance] = useState(true)

  // 获取钱包余额
  const fetchBalance = async () => {
    try {
      const balance = await getUserWalletBalance()
      updateUser({ balance })
    } catch (error) {
      console.error('获取钱包余额失败:', error)
      showNotify('获取钱包余额失败')
    }
  }

  // 页面加载时获取余额
  useEffect(() => {
    fetchBalance()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // 下拉刷新
  /**
   * 处理钱包页下拉刷新。
   *
   * @returns Promise<void> 无返回值。
   */
  const handleRefresh = async (): Promise<void> => {
    try {
      await fetchBalance()
    } catch {
      showNotify('刷新失败')
    }
  }

  // 返回上一页
  const handleBack = () => {
    navigate(-1)
  }

  // 常用功能
  const commonFeatures = [
    {
      label: '账单明细',
      desc: '查看所有收支记录',
      onClick: () => navigate('/wallet/bill')
    }
  ]

  return (
    <div className={styles.walletPage} ref={walletPageRef}>
      {/* 顶部导航栏 */}
      <div className={styles.navbar}>
        <div className={styles.navLeft} onClick={handleBack}>
          <ChevronLeft size={24} />
        </div>
        <div className={styles.navTitle}>我的钱包</div>
        <div className={styles.navRight} />
      </div>

      <PullRefresh mode='external' scrollTargetRef={walletPageRef} onRefresh={handleRefresh}>
        {/* 余额卡片 */}
        <div className={styles.balanceCard}>
          <div className={styles.balanceHeader}>
            <div className={styles.balanceLabel}>账户余额 (元)</div>
            <div className={styles.eyeIcon} onClick={() => setShowBalance(!showBalance)}>
              {showBalance ? <Eye size={18} /> : <EyeOff size={18} />}
            </div>
          </div>
          <div className={styles.balanceAmount}>{showBalance ? user.balance || '0.00' : '****'}</div>
          <div className={styles.balanceHint}>可用余额</div>
        </div>

        {/* 常用功能 */}
        <div className={styles.featuresContainer}>
          <div className={styles.featuresHeader}>
            <div className={styles.featuresTitle}>常用功能</div>
          </div>
          <div className={styles.featuresList}>
            {commonFeatures.map((feature, index) => (
              <div key={index} className={styles.featureItem} onClick={feature.onClick}>
                <div className={styles.featureLeft}>
                  <div className={styles.featureLabel}>{feature.label}</div>
                  <div className={styles.featureDesc}>{feature.desc}</div>
                </div>
                <div className={styles.featureRight}>
                  <ChevronRight size={18} />
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* 底部占位 */}
        <div className={styles.bottomPlaceholder} />
      </PullRefresh>
    </div>
  )
}

export default Wallet
