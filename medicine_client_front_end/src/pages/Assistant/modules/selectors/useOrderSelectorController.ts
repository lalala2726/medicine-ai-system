import { useCallback, useEffect, useRef, useState } from 'react'
import { newOrderTypes } from '@/api/order'
import { OrderStatus } from '@/types/orderStatus'
import styles from '../../components/OrderSelector/index.module.less'

/** 订单选择器单页请求条数。 */
const ORDER_SELECTOR_PAGE_SIZE = 10

/** 订单选择器顶部 Tab 配置。 */
export const ORDER_SELECTOR_TABS = [
  { key: 'ALL', title: '全部', status: undefined },
  { key: 'PENDING_PAYMENT', title: '待支付', status: OrderStatus.PENDING_PAYMENT },
  { key: 'PENDING_SHIPMENT', title: '待发货', status: OrderStatus.PENDING_SHIPMENT },
  { key: 'PENDING_RECEIPT', title: '待收货', status: OrderStatus.PENDING_RECEIPT },
  { key: 'COMPLETED', title: '已完成', status: OrderStatus.COMPLETED },
  { key: 'CANCELLED', title: '已取消', status: OrderStatus.CANCELLED }
] as const

/** OrderSelector 业务控制器配置。 */
interface UseOrderSelectorControllerOptions {
  visible: boolean
  initialStatus?: string
}

/** OrderSelector 组件消费的控制器结果。 */
export interface OrderSelectorControllerResult {
  loading: boolean
  orders: newOrderTypes.OrderListVo[]
  keyword: string
  tabValue: string | number
  hasMore: boolean
  tabs: typeof ORDER_SELECTOR_TABS
  setKeyword: (value: string) => void
  handleSearch: (value: string) => void
  handleTabChange: (value: string | number) => void
  handleScroll: (event: React.UIEvent<HTMLDivElement>) => void
  getStatusText: (status?: string) => string
  getStatusClass: (status?: string) => string
  getTotalQuantity: (order: newOrderTypes.OrderListVo) => number
}

/**
 * 管理订单选择器的分页、搜索和状态筛选逻辑。
 * 组件本身只消费状态与事件处理器，不再直接发请求。
 */
export function useOrderSelectorController({
  visible,
  initialStatus
}: UseOrderSelectorControllerOptions): OrderSelectorControllerResult {
  /** 当前是否正在加载订单列表。 */
  const [loading, setLoading] = useState(false)
  /** 使用 ref 避免滚动加载时重复触发请求。 */
  const loadingRef = useRef(false)
  /** 当前已加载的订单列表。 */
  const [orders, setOrders] = useState<newOrderTypes.OrderListVo[]>([])
  /** 当前页码。 */
  const [page, setPage] = useState(1)
  /** 是否还存在下一页数据。 */
  const [hasMore, setHasMore] = useState(true)
  /** 当前搜索关键字。 */
  const [keyword, setKeyword] = useState('')
  /** 当前选中的 Tab。 */
  const [tabValue, setTabValue] = useState<string | number>('ALL')

  /**
   * 请求订单列表。
   * page > 1 时会自动防止重复并发请求。
   */
  const fetchOrders = useCallback(async (pageNum: number, searchKeyword: string = '', status?: string) => {
    if (loadingRef.current && pageNum > 1) {
      return
    }

    loadingRef.current = true
    setLoading(true)

    try {
      const response = await newOrderTypes.getOrderList({
        pageNum,
        pageSize: ORDER_SELECTOR_PAGE_SIZE,
        productName: searchKeyword,
        orderStatus: status
      })

      const nextRows = response?.rows ?? []

      if (pageNum === 1) {
        setOrders(nextRows)
      } else {
        setOrders(previousOrders => {
          const uniqueOrders = nextRows.filter(
            nextOrder => !previousOrders.some(previousOrder => previousOrder.id === nextOrder.id)
          )

          return [...previousOrders, ...uniqueOrders]
        })
      }

      setHasMore(nextRows.length >= ORDER_SELECTOR_PAGE_SIZE)
      setPage(pageNum)
    } catch (error) {
      console.error('Fetch orders failed', error)
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

    const matchedTab = initialStatus ? ORDER_SELECTOR_TABS.find(tab => tab.status === initialStatus) : undefined
    const startTab = matchedTab?.key ?? 'ALL'
    const startStatus = matchedTab?.status

    setOrders([])
    setPage(1)
    setHasMore(true)
    setKeyword('')
    setTabValue(startTab)
    void fetchOrders(1, '', startStatus)
  }, [fetchOrders, initialStatus, visible])

  /** 根据搜索词重新拉取第一页数据。 */
  const handleSearch = useCallback(
    (value: string) => {
      setOrders([])
      setPage(1)
      setHasMore(true)

      const currentStatus = ORDER_SELECTOR_TABS.find(tab => tab.key === tabValue)?.status
      void fetchOrders(1, value, currentStatus)
    },
    [fetchOrders, tabValue]
  )

  /** 切换订单状态 Tab，并用新的筛选条件重新拉取第一页数据。 */
  const handleTabChange = useCallback(
    (value: string | number) => {
      setTabValue(value)
      setOrders([])
      setPage(1)
      setHasMore(true)

      const currentStatus = ORDER_SELECTOR_TABS.find(tab => tab.key === value)?.status
      void fetchOrders(1, keyword, currentStatus)
    },
    [fetchOrders, keyword]
  )

  /** 滚动接近底部时加载下一页。 */
  const handleScroll = useCallback(
    (event: React.UIEvent<HTMLDivElement>) => {
      const { scrollTop, clientHeight, scrollHeight } = event.currentTarget

      if (scrollHeight - scrollTop - clientHeight >= 50 || !hasMore || loadingRef.current) {
        return
      }

      const currentStatus = ORDER_SELECTOR_TABS.find(tab => tab.key === tabValue)?.status
      void fetchOrders(page + 1, keyword, currentStatus)
    },
    [fetchOrders, hasMore, keyword, page, tabValue]
  )

  /** 将订单状态值映射成人类可读的中文文案。 */
  const getStatusText = useCallback((status?: string) => {
    switch (status) {
      case OrderStatus.PENDING_PAYMENT:
        return '待支付'
      case OrderStatus.PENDING_SHIPMENT:
        return '待发货'
      case OrderStatus.PENDING_RECEIPT:
        return '待收货'
      case OrderStatus.COMPLETED:
        return '已完成'
      case OrderStatus.CANCELLED:
        return '已取消'
      case OrderStatus.REFUNDED:
        return '已退款'
      case OrderStatus.AFTER_SALE:
        return '售后中'
      case OrderStatus.EXPIRED:
        return '已过期'
      default:
        return status || '未知状态'
    }
  }, [])

  /** 根据订单状态返回对应的样式类。 */
  const getStatusClass = useCallback((status?: string) => {
    if (status === OrderStatus.COMPLETED) {
      return styles.success
    }

    if (status === OrderStatus.CANCELLED || status === OrderStatus.REFUNDED || status === OrderStatus.EXPIRED) {
      return styles.closed
    }

    if (status === OrderStatus.AFTER_SALE) {
      return styles.warning
    }

    if (status === OrderStatus.PENDING_PAYMENT) {
      return styles.warning
    }

    return ''
  }, [])

  /** 统计订单中商品的总数量。 */
  const getTotalQuantity = useCallback((order: newOrderTypes.OrderListVo) => {
    return order.items?.reduce((total, currentItem) => total + (currentItem.quantity || 0), 0) || 0
  }, [])

  return {
    loading,
    orders,
    keyword,
    tabValue,
    hasMore,
    tabs: ORDER_SELECTOR_TABS,
    setKeyword,
    handleSearch,
    handleTabChange,
    handleScroll,
    getStatusText,
    getStatusClass,
    getTotalQuantity
  }
}
