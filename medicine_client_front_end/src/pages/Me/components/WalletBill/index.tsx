import React, { useCallback, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { Loading } from '@nutui/nutui-react'
import { ChevronLeft } from 'lucide-react'
import { getBillList, type UserTypes } from '@/api/user'
import { showNotify } from '@/utils/notify'
import { useInfiniteScroll } from '@/hooks/useInfiniteScroll'
import PullRefresh from '@/components/PullRefresh'
import styles from './index.module.less'
import Empty from '@/components/Empty'

const WalletBill: React.FC = () => {
  const navigate = useNavigate()
  const scrollRef = useRef<HTMLDivElement>(null)

  /**
   * 获取钱包账单分页数据。
   *
   * @param pageNum - 当前页码
   * @param pageSize - 每页大小
   * @returns 钱包账单分页结果
   */
  const fetchData = useCallback(async (pageNum: number, pageSize: number) => {
    const result = await getBillList({
      pageNum,
      pageSize
    })

    return {
      rows: result?.rows || [],
      total: result?.total || 0
    }
  }, [])

  const {
    list: billList,
    loading,
    loadingMore,
    hasMore,
    refresh
  } = useInfiniteScroll<UserTypes.UserWalletBillVo>({
    fetchData,
    pageSize: 20,
    getKey: item => item.id ?? item.index,
    scrollRef
  })

  /**
   * 处理账单页下拉刷新。
   *
   * @returns Promise<void> 无返回值。
   */
  const handleRefresh = async (): Promise<void> => {
    try {
      await refresh()
    } catch {
      showNotify('刷新失败')
    }
  }

  /**
   * 返回上一页。
   *
   * @returns 无返回值
   */
  const handleBack = () => {
    navigate(-1)
  }

  /**
   * 打开账单详情页。
   *
   * @param bill - 当前点击的账单记录
   * @returns 无返回值
   */
  const handleBillClick = (bill: UserTypes.UserWalletBillVo) => {
    if (!bill.id) {
      return
    }

    navigate(`/wallet/bill/${bill.id}`)
  }

  /**
   * 格式化账单金额显示。
   *
   * @param amount - 原始金额
   * @param isRecharge - 是否为收入
   * @returns 带正负号的金额文案
   */
  const formatAmount = (amount: string | undefined, isRecharge: boolean | undefined) => {
    if (!amount) return '0.00'
    const value = Math.abs(parseFloat(amount))
    if (isRecharge) {
      return `+${value.toFixed(2)}`
    } else {
      return `-${value.toFixed(2)}`
    }
  }

  return (
    <div className={styles.billPage} ref={scrollRef}>
      {/* 顶部导航栏 */}
      <div className={styles.navbar}>
        <div className={styles.navLeft} onClick={handleBack}>
          <ChevronLeft size={24} />
        </div>
        <div className={styles.navTitle}>账单明细</div>
        <div className={styles.navRight} />
      </div>

      <PullRefresh mode='external' scrollTargetRef={scrollRef} onRefresh={handleRefresh}>
        <div className={styles.billList}>
          {loading && billList.length === 0 ? (
            <div className={styles.loadingWrapper}>
              <Loading />
              <div className={styles.loadingText}>加载中...</div>
            </div>
          ) : billList.length === 0 ? (
            <div className={styles.emptyWrapper}>
              <Empty description='暂无账单记录' />
            </div>
          ) : (
            <>
              {billList.map((bill, index) => (
                <div
                  key={bill.id ?? bill.index ?? index}
                  className={styles.billItem}
                  onClick={() => handleBillClick(bill)}
                >
                  <div className={styles.billContent}>
                    <div className={styles.billTitle}>{bill.title || '交易记录'}</div>
                    <div className={styles.billTime}>{bill.time || ''}</div>
                  </div>
                  <div className={styles.billAmount}>
                    <div className={styles.amountValue} style={{ color: bill.isRecharge ? '#ef4444' : '#0d1b12' }}>
                      {formatAmount(bill.amount, bill.isRecharge)}
                    </div>
                  </div>
                </div>
              ))}
              {loadingMore && <div className={styles.loadMore}>加载中...</div>}
              {!hasMore && billList.length > 0 && <div className={styles.noMore}>没有更多了</div>}
            </>
          )}
        </div>
      </PullRefresh>
    </div>
  )
}

export default WalletBill
