import React, { useCallback, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { Loading, Dialog } from '@nutui/nutui-react'
import { ChevronLeft, Trash2 } from 'lucide-react'
import { getViewHistoryList, deleteViewHistory, clearAllViewHistory } from '@/api/viewHistor'
import type { ViewHistoryTypes } from '@/api/viewHistor'
import ProductCard from '@/components/ProductCard'
import type { ProductCardData } from '@/components/ProductCard'
import { toProductDrugType } from '@/constants/drugCategory'
import { showNotify, showSuccessNotify } from '@/utils/notify'
import { useInfiniteScroll } from '@/hooks/useInfiniteScroll'
import PullRefresh from '@/components/PullRefresh'
import styles from './index.module.less'
import Empty from '@/components/Empty'

const ViewHistory: React.FC = () => {
  const navigate = useNavigate()
  /** 浏览历史页的滚动容器引用。 */
  const scrollRef = useRef<HTMLDivElement>(null)

  /**
   * 获取浏览历史分页数据。
   *
   * @param pageNum 当前页码。
   * @param pageSize 每页条数。
   * @returns Promise<{ rows: ViewHistoryTypes.ViewHistoryVo[]; total: number }> 浏览历史分页结果。
   */
  const fetchData = useCallback(async (pageNum: number, pageSize: number) => {
    const result = await getViewHistoryList({ pageNum, pageSize })
    return { rows: result?.rows || [], total: result?.total || 0 }
  }, [])

  const { list, loading, loadingMore, hasMore, refresh, setList } = useInfiniteScroll<ViewHistoryTypes.ViewHistoryVo>({
    fetchData,
    pageSize: 20,
    getKey: item => item.productId,
    scrollRef
  })

  /**
   * 将浏览历史记录转换为商品卡片数据。
   *
   * @param item 浏览历史记录。
   * @returns ProductCardData 商品卡片数据。
   */
  const toProductCardData = (item: ViewHistoryTypes.ViewHistoryVo): ProductCardData => ({
    productId: item.productId,
    productName: item.productName,
    coverImage: item.coverImage,
    price: item.price,
    sales: item.sales,
    type: toProductDrugType(item.drugCategory),
    viewCount: item.viewCount,
    lastViewTime: item.lastViewTime
  })

  /**
   * 处理浏览历史页下拉刷新。
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
   * 删除单条浏览记录。
   *
   * @param productId 商品 ID。
   * @returns Promise<void> 无返回值。
   */
  const handleDelete = async (productId: string) => {
    try {
      await deleteViewHistory(productId)
      setList(prev => prev.filter(item => item.productId !== productId))
      showSuccessNotify('删除成功')
    } catch (error) {
      console.error('删除浏览记录失败:', error)
    }
  }

  /**
   * 清空全部浏览记录。
   *
   * @returns void 无返回值。
   */
  const handleClearAll = () => {
    if (list.length === 0) {
      showNotify('暂无浏览记录')
      return
    }

    Dialog.confirm({
      title: '清空浏览记录',
      content: '确定要清空所有浏览记录吗？此操作不可恢复。',
      onConfirm: async () => {
        try {
          await clearAllViewHistory()
          setList([])
          showSuccessNotify('已清空浏览记录')
        } catch (error) {
          console.error('清空浏览记录失败:', error)
        }
      }
    })
  }

  /**
   * 返回上一页。
   *
   * @returns void 无返回值。
   */
  const handleBack = () => {
    navigate(-1)
  }

  return (
    <div className={styles.page} ref={scrollRef}>
      {/* 顶部导航栏 */}
      <div className={styles.navbar}>
        <div className={styles.navLeft} onClick={handleBack}>
          <ChevronLeft size={24} />
        </div>
        <div className={styles.navTitle}>浏览历史</div>
        <div className={styles.navRight} onClick={handleClearAll}>
          <Trash2 size={20} color='#667069' />
        </div>
      </div>

      <PullRefresh
        mode='external'
        scrollTargetRef={scrollRef}
        style={{
          backgroundColor: 'var(--background-light)'
        }}
        onRefresh={handleRefresh}
      >
        <div className={styles.list}>
          {loading && list.length === 0 ? (
            <div className={styles.loadingWrapper}>
              <Loading />
              <div className={styles.loadingText}>加载中...</div>
            </div>
          ) : list.length === 0 ? (
            <div className={styles.emptyWrapper}>
              <Empty description='暂无浏览记录' />
            </div>
          ) : (
            <>
              {list.map(item => (
                <ProductCard
                  key={item.productId}
                  data={toProductCardData(item)}
                  showViewInfo
                  swipeable
                  onDelete={handleDelete}
                  layout='horizontal'
                />
              ))}
              {loadingMore && <div className={styles.loadMore}>加载中...</div>}
              {!hasMore && list.length > 0 && <div className={styles.noMore}>没有更多了</div>}
            </>
          )}
        </div>
      </PullRefresh>
    </div>
  )
}

export default ViewHistory
