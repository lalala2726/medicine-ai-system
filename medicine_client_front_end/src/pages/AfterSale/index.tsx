import React, { useCallback, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronLeft } from 'lucide-react'
import { getAfterSaleList, OrderAfterSaleTypes } from '@/api/orderAfterSale'
import PullRefresh from '@/components/PullRefresh'
import AfterSaleCard from './components/AfterSaleCard'
import { useInfiniteScroll } from '@/hooks/useInfiniteScroll'
import styles from './index.module.less'
import Empty from '@/components/Empty'
import SkeletonBlock from '@/components/SkeletonBlock'

/**
 * 售后列表首屏骨架屏数量。
 */
const AFTER_SALE_SKELETON_COUNT = 3

/**
 * 渲染售后列表骨架屏卡片。
 *
 * @param index - 骨架屏卡片序号。
 * @returns 售后卡片骨架节点。
 */
const renderAfterSaleSkeletonCard = (index: number) => (
  <div key={`after-sale-skeleton-${index}`} className={styles.afterSaleSkeletonCard} aria-hidden='true'>
    <div className={styles.afterSaleSkeletonHeader}>
      <div className={styles.afterSaleSkeletonHeaderLeft}>
        <SkeletonBlock className={styles.afterSaleSkeletonTag} />
        <SkeletonBlock className={styles.afterSaleSkeletonNo} />
      </div>
      <SkeletonBlock className={styles.afterSaleSkeletonStatus} />
    </div>
    <div className={styles.afterSaleSkeletonContent}>
      <SkeletonBlock className={styles.afterSaleSkeletonImage} />
      <div className={styles.afterSaleSkeletonInfo}>
        <SkeletonBlock className={styles.afterSaleSkeletonName} />
        <SkeletonBlock className={styles.afterSaleSkeletonNameShort} />
      </div>
    </div>
    <div className={styles.afterSaleSkeletonMetaBox}>
      <div className={styles.afterSaleSkeletonMetaRow}>
        <SkeletonBlock className={styles.afterSaleSkeletonMetaLabel} />
        <SkeletonBlock className={styles.afterSaleSkeletonMetaValue} />
      </div>
      <div className={styles.afterSaleSkeletonMetaRow}>
        <SkeletonBlock className={styles.afterSaleSkeletonMetaLabel} />
        <SkeletonBlock className={styles.afterSaleSkeletonMetaAmount} />
      </div>
      <div className={styles.afterSaleSkeletonMetaRow}>
        <SkeletonBlock className={styles.afterSaleSkeletonMetaLabel} />
        <SkeletonBlock className={styles.afterSaleSkeletonMetaTime} />
      </div>
    </div>
    <div className={styles.afterSaleSkeletonFooter}>
      <SkeletonBlock className={styles.afterSaleSkeletonAction} />
    </div>
  </div>
)

const AfterSale: React.FC = () => {
  const navigate = useNavigate()
  const scrollRef = useRef<HTMLDivElement>(null)

  const fetchData = useCallback(async (pageNum: number, pageSize: number) => {
    const result = await getAfterSaleList({ pageNum, pageSize })
    return { rows: result?.rows || [], total: result?.total || 0 }
  }, [])

  const { list, loading, loadingMore, hasMore, refresh } = useInfiniteScroll<OrderAfterSaleTypes.AfterSaleListVo>({
    fetchData,
    pageSize: 10,
    getKey: item => item.id,
    scrollRef
  })

  /**
   * 处理售后页下拉刷新。
   *
   * @returns Promise<void> 无返回值。
   */
  const handleRefresh = async (): Promise<void> => {
    await refresh()
  }

  return (
    <div className={styles.page} ref={scrollRef}>
      <div className={styles.navbar}>
        <div className={styles.navLeft} onClick={() => navigate(-1)}>
          <ChevronLeft size={24} color='#0d1b12' />
        </div>
        <div className={styles.navTitle}>售后/退款</div>
        <div className={styles.navRight} />
      </div>

      <PullRefresh mode='external' scrollTargetRef={scrollRef} onRefresh={handleRefresh}>
        <div className={styles.list}>
          {loading && list.length === 0 ? (
            Array.from({ length: AFTER_SALE_SKELETON_COUNT }).map((_, index) => renderAfterSaleSkeletonCard(index))
          ) : list.length > 0 ? (
            <>
              {list.map(item => (
                <AfterSaleCard
                  key={item.id}
                  data={item}
                  onClick={() => navigate(`/after-sale/detail/${item.afterSaleNo}`)}
                />
              ))}
              {loadingMore && <div className={styles.loadingMore}>加载中...</div>}
              {!hasMore && list.length > 0 && <div className={styles.noMore}>没有更多了</div>}
            </>
          ) : (
            <Empty description='暂无售后记录' />
          )}
        </div>
      </PullRefresh>
    </div>
  )
}

export default AfterSale
