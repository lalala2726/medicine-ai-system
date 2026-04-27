import React, { useState, useCallback, useRef, useEffect } from 'react'
import { useNavigate, useSearchParams, useLocation } from 'react-router-dom'
import { Tabs, Dialog } from '@nutui/nutui-react'
import { ArrowLeft } from '@nutui/icons-react'
import { newOrderTypes } from '@/api/order.ts'
import { OrderStatus } from '@/types/orderStatus'
import PullRefresh from '@/components/PullRefresh'
import OrderCard from './components/OrderCard'
import CancelOrderPopup from './components/CancelOrderPopup'
import { showNotify, showSuccessNotify } from '@/utils/notify'
import { useInfiniteScroll } from '@/hooks/useInfiniteScroll'
import styles from './index.module.less'
import Empty from '@/components/Empty'
import SkeletonBlock from '@/components/SkeletonBlock'

// Tab 配置 - 放在组件外避免每次渲染重建
const tabs = [
  { key: '0', title: '全部订单', status: undefined },
  { key: '1', title: '待支付', status: OrderStatus.PENDING_PAYMENT },
  { key: '2', title: '待发货', status: OrderStatus.PENDING_SHIPMENT },
  { key: '3', title: '待收货', status: OrderStatus.PENDING_RECEIPT },
  { key: '4', title: '已完成', status: OrderStatus.COMPLETED },
  { key: '5', title: '已取消', status: OrderStatus.CANCELLED }
]

/**
 * 订单列表首屏骨架屏数量。
 */
const ORDER_SKELETON_COUNT = 3

const getTabKeyByStatus = (status?: string | null) => tabs.find(tab => tab.status === status)?.key ?? '0'

/**
 * 渲染订单列表骨架屏卡片。
 *
 * @param index - 骨架屏卡片序号。
 * @returns 订单卡片骨架节点。
 */
const renderOrderSkeletonCard = (index: number) => (
  <div key={`order-skeleton-${index}`} className={styles.orderSkeletonCard} aria-hidden='true'>
    <div className={styles.orderSkeletonHeader}>
      <SkeletonBlock className={styles.orderSkeletonNo} />
      <SkeletonBlock className={styles.orderSkeletonStatus} />
    </div>
    <div className={styles.orderSkeletonAddress}>
      <SkeletonBlock className={styles.orderSkeletonAddressIcon} />
      <div className={styles.orderSkeletonAddressContent}>
        <SkeletonBlock className={styles.orderSkeletonAddressName} />
        <SkeletonBlock className={styles.orderSkeletonAddressText} />
      </div>
    </div>
    <div className={styles.orderSkeletonProduct}>
      <SkeletonBlock className={styles.orderSkeletonImage} />
      <div className={styles.orderSkeletonProductInfo}>
        <SkeletonBlock className={styles.orderSkeletonProductName} />
        <SkeletonBlock className={styles.orderSkeletonProductNameShort} />
        <div className={styles.orderSkeletonProductBottom}>
          <SkeletonBlock className={styles.orderSkeletonPrice} />
          <SkeletonBlock className={styles.orderSkeletonQuantity} />
        </div>
      </div>
    </div>
    <div className={styles.orderSkeletonTotal}>
      <SkeletonBlock className={styles.orderSkeletonTotalLabel} />
      <SkeletonBlock className={styles.orderSkeletonTotalAmount} />
    </div>
    <div className={styles.orderSkeletonActions}>
      <SkeletonBlock className={styles.orderSkeletonAction} />
      <SkeletonBlock className={styles.orderSkeletonActionPrimary} />
    </div>
  </div>
)

const Orders: React.FC = () => {
  const navigate = useNavigate()
  const scrollRef = useRef<HTMLDivElement>(null)
  const [searchParams, setSearchParams] = useSearchParams()
  const statusParam = searchParams.get('status')

  const [tabValue, setTabValue] = useState<string>(() => getTabKeyByStatus(statusParam))
  const [showCancelPopup, setShowCancelPopup] = useState(false)
  const [cancellingOrderNo, setCancellingOrderNo] = useState('')

  const tabValueRef = useRef(tabValue)
  tabValueRef.current = tabValue

  const fetchData = useCallback(async (pageNum: number, pageSize: number) => {
    const currentTab = tabs.find(tab => tab.key === tabValueRef.current)
    const result = await newOrderTypes.getOrderList({
      orderStatus: currentTab?.status,
      pageNum,
      pageSize
    })
    return { rows: result?.rows || [], total: result?.total || 0 }
  }, [])

  const {
    list: orderList,
    loading,
    loadingMore,
    hasMore,
    refresh,
    reset
  } = useInfiniteScroll<newOrderTypes.OrderListVo>({
    fetchData,
    pageSize: 10,
    getKey: item => item.id,
    scrollRef
  })

  // Tab 切换
  const handleTabChange = (value: string | number) => {
    const nextTab = String(value)
    const nextStatus = tabs.find(tab => tab.key === nextTab)?.status
    const nextSearchParams = new URLSearchParams(searchParams)

    if (nextStatus) {
      nextSearchParams.set('status', nextStatus)
    } else {
      nextSearchParams.delete('status')
    }

    setTabValue(nextTab)
    setSearchParams(nextSearchParams, { replace: true })
  }

  // Tab 切换时重新加载
  useEffect(() => {
    reset()
  }, [tabValue, reset])

  // 浏览器返回或外部带参进入时，同步 URL 到当前 tab
  useEffect(() => {
    const nextTab = getTabKeyByStatus(statusParam)
    setTabValue(prev => (prev === nextTab ? prev : nextTab))
  }, [statusParam])

  // 下拉刷新
  /**
   * 处理订单页下拉刷新。
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

  const location = useLocation()

  // 返回上一页或主页
  const handleBack = () => {
    // 如果是从支付流程过来的，根据要求回到主界面
    if ((location.state as any)?.fromPayment) {
      navigate('/', { replace: true })
    } else {
      // 否则正常返回（比如从个人中心进入，则返回个人中心）
      navigate(-1)
    }
  }

  // 查看订单详情
  const handleViewDetail = (orderNo: string) => {
    navigate(`/orders/${orderNo}`)
  }

  // 支付订单
  const handlePayOrder = async (orderNo: string) => {
    navigate(`/order/payment/${orderNo}`)
  }

  // 取消订单 - 显示弹窗
  const handleCancelOrder = (orderNo: string) => {
    setCancellingOrderNo(orderNo)
    setShowCancelPopup(true)
  }

  // 确认取消订单
  const handleConfirmCancel = async (orderNo: string, reason: string) => {
    try {
      await newOrderTypes.cancelOrder({
        orderNo,
        cancelReason: reason
      })
      showSuccessNotify('订单已取消')
      setShowCancelPopup(false)
      // 刷新订单列表
      await refresh()
    } catch (error) {
      console.error('取消订单失败:', error)
      // Notify 已在 requestClient 中处理
    }
  }

  // 确认收货
  const handleConfirmReceipt = (orderNo: string) => {
    Dialog.confirm({
      title: '确认收货',
      content: '请确认您已收到商品，确认后订单将完成。',
      onConfirm: async () => {
        try {
          await newOrderTypes.confirmReceipt({ orderNo })
          showSuccessNotify('已确认收货')
          // 重新加载当前tab的订单列表，确保显示最新状态
          await refresh()
        } catch (error) {
          console.error('确认收货失败:', error)
          // Notify 已在 requestClient 中处理
        }
      },
      onCancel: () => {
        console.log('取消确认收货')
      }
    })
  }

  // 申请售后/退款
  const handleAfterSale = (orderNo: string) => {
    // 如果是待发货状态，则是整单退款
    const order = orderList.find(o => o.orderNo === orderNo)
    if (order?.orderStatus === OrderStatus.PENDING_SHIPMENT) {
      navigate('/after-sale/apply', {
        state: {
          orderNo: order.orderNo,
          scope: 'ORDER'
        }
      })
    } else {
      // 其他状态（如已完成）跳转到售后申请页面（通常在详情页操作，这里可能不需要处理，或者跳转到详情页）
      // 根据之前的修改，OrderCard 在已完成状态下点击的是“查看详情”，所以这里主要是处理待发货的退款
      navigate(`/orders/${orderNo}`)
    }
  }

  return (
    <div className={styles.ordersPage} ref={scrollRef}>
      {/* 顶部导航栏 */}
      <div className={styles.navbar}>
        <div className={styles.navLeft} onClick={handleBack}>
          <ArrowLeft width={20} height={20} />
        </div>
        <div className={styles.navTitle}>我的订单</div>
        <div className={styles.navRight} />
      </div>

      {/* Tabs 切换 */}
      <div className={styles.tabsWrapper}>
        <Tabs value={tabValue} onChange={handleTabChange}>
          {tabs.map(tab => (
            <Tabs.TabPane key={tab.key} title={tab.title} value={tab.key}>
              <PullRefresh mode='external' scrollTargetRef={scrollRef} onRefresh={handleRefresh}>
                <div className={styles.orderList}>
                  {loading && orderList.length === 0 ? (
                    Array.from({ length: ORDER_SKELETON_COUNT }).map((_, index) => renderOrderSkeletonCard(index))
                  ) : orderList.length === 0 ? (
                    <div className={styles.emptyWrapper}>
                      <Empty description='暂无订单' />
                    </div>
                  ) : (
                    <>
                      {orderList.map(order => (
                        <OrderCard
                          key={order.id}
                          order={order}
                          onViewDetail={handleViewDetail}
                          onPay={handlePayOrder}
                          onCancel={handleCancelOrder}
                          onConfirmReceipt={handleConfirmReceipt}
                          onAfterSale={handleAfterSale}
                        />
                      ))}
                      {loadingMore && <div className={styles.loadMore}>加载中...</div>}
                      {!hasMore && orderList.length > 0 && (
                        <div className={styles.loadMore} style={{ cursor: 'default' }}>
                          没有更多了
                        </div>
                      )}
                    </>
                  )}
                </div>
              </PullRefresh>
            </Tabs.TabPane>
          ))}
        </Tabs>
      </div>

      {/* 取消订单弹窗 */}
      <CancelOrderPopup
        visible={showCancelPopup}
        orderNo={cancellingOrderNo}
        onConfirm={handleConfirmCancel}
        onClose={() => setShowCancelPopup(false)}
      />
    </div>
  )
}

export default Orders
