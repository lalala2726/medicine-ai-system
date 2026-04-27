import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Dialog, Swipe } from '@nutui/nutui-react'
import { ChevronLeft, TicketPercent, Clock3, Trash2 } from 'lucide-react'
import { deleteUserCoupon, getUserCouponList, type CouponTypes } from '@/api/coupon'
import { showNotify, showSuccessNotify } from '@/utils/notify'
import Empty from '@/components/Empty'
import PullRefresh from '@/components/PullRefresh'
import SkeletonBlock from '@/components/SkeletonBlock'
import styles from './index.module.less'

/** 每页查询数量。 */
const PAGE_SIZE = 20

/** 优惠券首屏骨架屏数量。 */
const COUPON_SKELETON_COUNT = 3

/** 优惠券状态标签配置。 */
const STATUS_TABS = [
  { label: '可使用', value: 'AVAILABLE' },
  { label: '已使用', value: 'USED' },
  { label: '已过期', value: 'EXPIRED' }
] as const

type CouponStatus = (typeof STATUS_TABS)[number]['value']

/**
 * 渲染优惠券骨架屏卡片。
 *
 * @param index 骨架屏卡片序号。
 * @returns 优惠券骨架节点。
 */
const renderCouponSkeletonCard = (index: number) => (
  <div key={`coupon-skeleton-${index}`} className={styles.couponSkeletonCard} aria-hidden='true'>
    <div className={styles.couponSkeletonLeft}>
      <SkeletonBlock className={styles.couponSkeletonAmount} />
      <SkeletonBlock className={styles.couponSkeletonAmountHint} />
    </div>
    <div className={styles.couponSkeletonRight}>
      <div className={styles.couponSkeletonHeader}>
        <SkeletonBlock className={styles.couponSkeletonName} />
        <SkeletonBlock className={styles.couponSkeletonStatus} />
      </div>
      <SkeletonBlock className={styles.couponSkeletonMeta} />
      <SkeletonBlock className={styles.couponSkeletonMetaShort} />
      <SkeletonBlock className={styles.couponSkeletonTime} />
    </div>
  </div>
)

/**
 * 优惠券列表页面。
 */
const Coupon: React.FC = () => {
  const navigate = useNavigate()
  const [activeStatus, setActiveStatus] = useState<CouponStatus>('AVAILABLE')
  const [couponList, setCouponList] = useState<CouponTypes.UserCouponVo[]>([])
  const [pageNum, setPageNum] = useState(1)
  const [hasMore, setHasMore] = useState(false)
  const [loading, setLoading] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)

  /**
   * 查询优惠券列表。
   * @param status 状态筛选。
   * @param nextPage 目标页码。
   * @param append 是否追加列表。
   * @returns 无返回值。
   */
  const fetchCouponList = useCallback(async (status: CouponStatus, nextPage: number, append: boolean) => {
    if (append) {
      setLoadingMore(true)
    } else {
      setLoading(true)
    }

    try {
      const result = await getUserCouponList({
        couponStatus: status,
        pageNum: nextPage,
        pageSize: PAGE_SIZE
      })

      const rows = (result?.rows || []) as CouponTypes.UserCouponVo[]
      const total = Number(result?.total || 0)
      setCouponList(previousList => {
        const mergedList = append ? [...previousList, ...rows] : rows
        setHasMore(mergedList.length < total)
        return mergedList
      })
      setPageNum(nextPage)
    } catch (error) {
      console.error('查询优惠券列表失败:', error)
      showNotify('查询优惠券列表失败')
    } finally {
      setLoading(false)
      setLoadingMore(false)
    }
  }, [])

  /**
   * 初次加载与状态切换时刷新列表。
   */
  useEffect(() => {
    fetchCouponList(activeStatus, 1, false)
  }, [activeStatus, fetchCouponList])

  /**
   * 返回上一页。
   * @returns 无返回值。
   */
  const handleBack = () => {
    navigate(-1)
  }

  /**
   * 跳转到激活码激活页。
   * @returns 无返回值。
   */
  const handleGoActivationPage = () => {
    navigate('/coupon/activation')
  }

  /**
   * 处理下拉刷新。
   * @returns Promise<void> 无返回值。
   */
  const handleRefresh = async (): Promise<void> => {
    await fetchCouponList(activeStatus, 1, false)
  }

  /**
   * 处理加载更多。
   * @returns 无返回值。
   */
  const handleLoadMore = () => {
    if (loadingMore || !hasMore) {
      return
    }
    void fetchCouponList(activeStatus, pageNum + 1, true)
  }

  /**
   * 删除优惠券。
   * @param coupon 优惠券数据。
   * @returns 无返回值。
   */
  const handleDeleteCoupon = (coupon: CouponTypes.UserCouponVo) => {
    if (!coupon.couponId) {
      showNotify('优惠券不存在')
      return
    }
    const couponName = coupon.couponName || '这张优惠券'
    Dialog.confirm({
      title: '确认删除',
      content: `确定要删除“${couponName}”吗？`,
      onConfirm: async () => {
        try {
          await deleteUserCoupon(coupon.couponId as number)
          await fetchCouponList(activeStatus, 1, false)
          showSuccessNotify('删除成功')
        } catch (error: any) {
          console.error('删除优惠券失败:', error)
          showNotify(error?.message || '删除优惠券失败')
        }
      }
    })
  }

  /**
   * 格式化金额显示。
   * @param amount 金额文本。
   * @returns 人民币金额字符串。
   */
  const formatAmount = (amount?: string) => {
    if (!amount) {
      return '0.00'
    }
    return Number(amount).toFixed(2)
  }

  /**
   * 格式化时间显示。
   * @param time 时间字符串。
   * @returns 日期字符串。
   */
  const formatTime = (time?: string) => {
    if (!time) return '--'
    return time.split(' ')[0]
  }

  /**
   * 状态文本映射。
   */
  const statusLabelMap = useMemo<Record<string, string>>(
    () => ({
      AVAILABLE: '可使用',
      USED: '已使用',
      EXPIRED: '已过期'
    }),
    []
  )

  return (
    <div className={styles.couponPage}>
      <div className={styles.pageHeader}>
        <div className={styles.headerLeft} onClick={handleBack}>
          <ChevronLeft size={24} />
        </div>
        <div className={styles.headerTitle}>我的优惠券</div>
        <div className={styles.headerRight} onClick={handleGoActivationPage}>
          <span className={styles.activationEntry}>激活码</span>
        </div>
      </div>

      <div className={styles.statusTabs}>
        {STATUS_TABS.map(tab => (
          <div
            key={tab.value}
            className={`${styles.tabItem} ${activeStatus === tab.value ? styles.active : ''}`}
            onClick={() => setActiveStatus(tab.value)}
          >
            {tab.label}
          </div>
        ))}
      </div>

      <PullRefresh mode='self' style={{ backgroundColor: 'var(--background-light)' }} onRefresh={handleRefresh}>
        <div className={styles.pageContent}>
          {loading && couponList.length === 0 ? (
            <div className={styles.couponList}>
              {Array.from({ length: COUPON_SKELETON_COUNT }).map((_, index) => renderCouponSkeletonCard(index))}
            </div>
          ) : couponList.length === 0 ? (
            <div className={styles.emptyWrapper}>
              <Empty
                image={<TicketPercent size={64} color='var(--nutui-color-primary)' opacity={0.65} />}
                description='暂无优惠券'
              />
            </div>
          ) : (
            <>
              <div className={styles.couponList}>
                {couponList.map(coupon => {
                  const statusText = statusLabelMap[coupon.couponStatus || ''] || '未知状态'
                  const isAvailable = coupon.couponStatus === 'AVAILABLE'

                  return (
                    <div
                      className={styles.couponItemWrapper}
                      key={`${coupon.couponId}-${coupon.templateId}-${coupon.expireTime}`}
                    >
                      <Swipe
                        rightAction={
                          <div className={styles.deleteButton} onClick={() => handleDeleteCoupon(coupon)}>
                            <div className={styles.deleteIcon}>
                              <Trash2 size={20} color='#fff' />
                            </div>
                            <div className={styles.deleteText}>删除</div>
                          </div>
                        }
                      >
                        <div className={`${styles.couponCard} ${isAvailable ? styles.available : styles.disabled}`}>
                          <div className={styles.cardLeft}>
                            <div className={styles.amountLine}>
                              <span className={styles.symbol}>¥</span>
                              <span className={styles.amount}>
                                {formatAmount(coupon.availableAmount || coupon.totalAmount)}
                              </span>
                            </div>
                            <div className={styles.amountHint}>可抵扣金额</div>
                          </div>

                          <div className={styles.cardRight}>
                            <div className={styles.cardHeader}>
                              <div className={styles.couponName}>{coupon.couponName || '优惠券'}</div>
                              <div className={styles.couponStatus}>{statusText}</div>
                            </div>

                            <div className={styles.cardMeta}>满 {formatAmount(coupon.thresholdAmount)} 元可用</div>

                            <div className={styles.cardMeta}>总面额 {formatAmount(coupon.totalAmount)} 元</div>

                            <div className={styles.timeLine}>
                              <Clock3 size={12} />
                              <span>
                                {formatTime(coupon.effectiveTime)} 至 {formatTime(coupon.expireTime)}
                              </span>
                            </div>
                          </div>
                        </div>
                      </Swipe>
                    </div>
                  )
                })}
              </div>

              {hasMore && (
                <div className={styles.loadMoreWrapper}>
                  <Button size='small' fill='outline' loading={loadingMore} onClick={handleLoadMore}>
                    {loadingMore ? '加载中...' : '加载更多'}
                  </Button>
                </div>
              )}
            </>
          )}
        </div>
      </PullRefresh>
    </div>
  )
}

export default Coupon
