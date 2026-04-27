import { useCallback, useEffect, useRef, useState } from 'react'
import { getAfterSaleList, OrderAfterSaleTypes } from '@/api/orderAfterSale'
import styles from '../../components/AfterSaleSelector/index.module.less'

/** 售后状态常量。 */
export const AFTER_SALE_SELECTOR_STATUSES = {
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
  PROCESSING: 'PROCESSING',
  COMPLETED: 'COMPLETED',
  CANCELLED: 'CANCELLED'
} as const

/** 售后选择器单页请求条数。 */
const AFTER_SALE_SELECTOR_PAGE_SIZE = 10

/** 售后选择器顶部 Tab 配置。 */
export const AFTER_SALE_SELECTOR_TABS = [
  { key: 'ALL', title: '全部', status: undefined },
  { key: AFTER_SALE_SELECTOR_STATUSES.PENDING, title: '待审核', status: AFTER_SALE_SELECTOR_STATUSES.PENDING },
  { key: AFTER_SALE_SELECTOR_STATUSES.APPROVED, title: '已通过', status: AFTER_SALE_SELECTOR_STATUSES.APPROVED },
  { key: AFTER_SALE_SELECTOR_STATUSES.REJECTED, title: '已拒绝', status: AFTER_SALE_SELECTOR_STATUSES.REJECTED },
  { key: AFTER_SALE_SELECTOR_STATUSES.PROCESSING, title: '处理中', status: AFTER_SALE_SELECTOR_STATUSES.PROCESSING },
  { key: AFTER_SALE_SELECTOR_STATUSES.COMPLETED, title: '已完成', status: AFTER_SALE_SELECTOR_STATUSES.COMPLETED },
  { key: AFTER_SALE_SELECTOR_STATUSES.CANCELLED, title: '已取消', status: AFTER_SALE_SELECTOR_STATUSES.CANCELLED }
] as const

/** AfterSaleSelector 业务控制器配置。 */
interface UseAfterSaleSelectorControllerOptions {
  visible: boolean
  initialStatus?: string
}

/** AfterSaleSelector 组件消费的控制器结果。 */
export interface AfterSaleSelectorControllerResult {
  loading: boolean
  list: OrderAfterSaleTypes.AfterSaleListVo[]
  keyword: string
  tabValue: string | number
  hasMore: boolean
  tabs: typeof AFTER_SALE_SELECTOR_TABS
  setKeyword: (value: string) => void
  handleSearch: (value: string) => void
  handleTabChange: (value: string | number) => void
  handleScroll: (event: React.UIEvent<HTMLDivElement>) => void
  getStatusClass: (status?: string) => string
}

/**
 * 管理售后选择器的分页、搜索和状态筛选逻辑。
 * 组件本身只消费状态与事件处理器，不再直接发请求。
 */
export function useAfterSaleSelectorController({
  visible,
  initialStatus
}: UseAfterSaleSelectorControllerOptions): AfterSaleSelectorControllerResult {
  /** 当前是否正在加载售后列表。 */
  const [loading, setLoading] = useState(false)
  /** 使用 ref 避免滚动加载时重复触发请求。 */
  const loadingRef = useRef(false)
  /** 当前已加载的售后列表。 */
  const [list, setList] = useState<OrderAfterSaleTypes.AfterSaleListVo[]>([])
  /** 当前页码。 */
  const [page, setPage] = useState(1)
  /** 是否还存在下一页数据。 */
  const [hasMore, setHasMore] = useState(true)
  /** 当前搜索关键字。 */
  const [keyword, setKeyword] = useState('')
  /** 当前选中的 Tab。 */
  const [tabValue, setTabValue] = useState<string | number>('ALL')

  /**
   * 请求售后列表。
   * page > 1 时会自动防止重复并发请求。
   */
  const fetchList = useCallback(async (pageNum: number, searchKeyword: string = '', status?: string) => {
    if (loadingRef.current && pageNum > 1) {
      return
    }

    loadingRef.current = true
    setLoading(true)

    try {
      const response = await getAfterSaleList({
        pageNum,
        pageSize: AFTER_SALE_SELECTOR_PAGE_SIZE,
        orderNo: searchKeyword || undefined,
        afterSaleStatus: status
      })

      const nextRows = response?.rows ?? []

      if (pageNum === 1) {
        setList(nextRows)
      } else {
        setList(previousItems => {
          const uniqueItems = nextRows.filter(
            nextItem => !previousItems.some(previousItem => previousItem.id === nextItem.id)
          )
          return [...previousItems, ...uniqueItems]
        })
      }

      setHasMore(nextRows.length >= AFTER_SALE_SELECTOR_PAGE_SIZE)
      setPage(pageNum)
    } catch (error) {
      console.error('Fetch after-sale list failed', error)
    } finally {
      setLoading(false)
      loadingRef.current = false
    }
  }, [])

  /**
   * 选择器弹层打开时，按初始状态重置并拉取第一页数据。
   */
  useEffect(() => {
    if (!visible) {
      return
    }

    const matchedTab = initialStatus ? AFTER_SALE_SELECTOR_TABS.find(tab => tab.status === initialStatus) : undefined
    const startTab = matchedTab?.key ?? 'ALL'
    const startStatus = matchedTab?.status

    setList([])
    setPage(1)
    setHasMore(true)
    setKeyword('')
    setTabValue(startTab)
    void fetchList(1, '', startStatus)
  }, [fetchList, initialStatus, visible])

  /** 根据搜索词重新拉取第一页数据。 */
  const handleSearch = useCallback(
    (value: string) => {
      setList([])
      setPage(1)
      setHasMore(true)

      const currentStatus = AFTER_SALE_SELECTOR_TABS.find(tab => tab.key === tabValue)?.status
      void fetchList(1, value, currentStatus)
    },
    [fetchList, tabValue]
  )

  /** 切换售后状态 Tab，并用新的筛选条件重新拉取第一页数据。 */
  const handleTabChange = useCallback(
    (value: string | number) => {
      setTabValue(value)
      setList([])
      setPage(1)
      setHasMore(true)

      const currentStatus = AFTER_SALE_SELECTOR_TABS.find(tab => tab.key === value)?.status
      void fetchList(1, keyword, currentStatus)
    },
    [fetchList, keyword]
  )

  /** 滚动接近底部时加载下一页。 */
  const handleScroll = useCallback(
    (event: React.UIEvent<HTMLDivElement>) => {
      const { scrollTop, clientHeight, scrollHeight } = event.currentTarget

      if (scrollHeight - scrollTop - clientHeight >= 50 || !hasMore || loadingRef.current) {
        return
      }

      const currentStatus = AFTER_SALE_SELECTOR_TABS.find(tab => tab.key === tabValue)?.status
      void fetchList(page + 1, keyword, currentStatus)
    },
    [fetchList, hasMore, keyword, page, tabValue]
  )

  /** 根据售后状态返回对应的样式类。 */
  const getStatusClass = useCallback((status?: string) => {
    if (status === AFTER_SALE_SELECTOR_STATUSES.COMPLETED) {
      return styles.success
    }

    if (status === AFTER_SALE_SELECTOR_STATUSES.CANCELLED || status === AFTER_SALE_SELECTOR_STATUSES.REJECTED) {
      return styles.closed
    }

    if (status === AFTER_SALE_SELECTOR_STATUSES.PENDING) {
      return styles.warning
    }

    return ''
  }, [])

  return {
    loading,
    list,
    keyword,
    tabValue,
    hasMore,
    tabs: AFTER_SALE_SELECTOR_TABS,
    setKeyword,
    handleSearch,
    handleTabChange,
    handleScroll,
    getStatusClass
  }
}
