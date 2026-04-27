import React from 'react'
import { Popup, Button, Loading, SearchBar } from '@nutui/nutui-react'
import { Close } from '@nutui/icons-react'
import type { newOrderTypes } from '@/api/order'
import { useOrderSelectorController } from '../../modules/selectors/useOrderSelectorController'
import SelectorTabs from '../SelectorTabs'
import styles from './index.module.less'

interface OrderSelectorProps {
  visible: boolean
  initialStatus?: string
  onClose: () => void
  onSelect: (order: newOrderTypes.OrderListVo) => void
}

/**
 * 订单选择器展示组件。
 * 分页、搜索和筛选逻辑统一由 controller hook 承担。
 */
const OrderSelector: React.FC<OrderSelectorProps> = ({ visible, initialStatus, onClose, onSelect }) => {
  const [expandedOrderIds, setExpandedOrderIds] = React.useState<Set<string>>(new Set())

  const {
    loading,
    orders,
    keyword,
    tabValue,
    hasMore,
    tabs,
    setKeyword,
    handleSearch,
    handleTabChange,
    handleScroll,
    getStatusText,
    getStatusClass,
    getTotalQuantity
  } = useOrderSelectorController({
    visible,
    initialStatus
  })

  return (
    <Popup visible={visible} position='bottom' round onClose={onClose} style={{ height: '80vh' }}>
      <div className={styles.container} data-assistant-drawer-swipe-lock='true'>
        <div className={styles.topSection}>
          <div className={styles.header}>
            <span className={styles.title}>选择订单</span>
            <Close className={styles.close} onClick={onClose} />
          </div>
          <div className={styles.searchBar}>
            <SearchBar
              placeholder='搜索订单商品名称'
              value={keyword}
              onChange={val => setKeyword(val)}
              onSearch={handleSearch}
              onClear={() => handleSearch('')}
            />
          </div>
          <div className={styles.tabsWrapper}>
            <SelectorTabs value={tabValue} tabs={[...tabs]} onChange={handleTabChange} ariaLabel='订单状态筛选' />
          </div>
        </div>

        <div className={styles.list} onScroll={handleScroll}>
          {orders.map(order => (
            <div key={order.id} className={styles.card}>
              <div className={styles.cardHeader}>
                <span className={styles.orderNo}>订单号: {order.orderNo}</span>
                <span className={`${styles.status} ${getStatusClass(order.orderStatus)}`}>
                  {getStatusText(order.orderStatus)}
                </span>
              </div>

              {(() => {
                const isExpanded = order.id ? expandedOrderIds.has(order.id) : false
                const displayedItems = isExpanded ? order.items : order.items?.slice(0, 2)
                return (
                  <>
                    {displayedItems?.map((item, index) => (
                      <div key={index} className={styles.product}>
                        <img src={item.imageUrl} alt={item.productName} className={styles.image} />
                        <div className={styles.info}>
                          <div className={styles.productTitle}>{item.productName}</div>
                          <div className={styles.meta}>
                            <span className={styles.price}>{item.price}</span>
                            <span className={styles.quantity}>x{item.quantity}</span>
                          </div>
                        </div>
                      </div>
                    ))}
                    {!isExpanded && (order.items?.length || 0) > 2 && (
                      <div
                        className={styles.moreItems}
                        onClick={e => {
                          e.stopPropagation()
                          const next = new Set(expandedOrderIds)
                          next.add(order.id!)
                          setExpandedOrderIds(next)
                        }}
                      >
                        查看全部 {order.items?.length || 0} 件商品
                      </div>
                    )}
                  </>
                )
              })()}
              <div className={styles.cardFooter}>
                <div className={styles.orderInfo}>
                  <div className={styles.total}>
                    共{getTotalQuantity(order)}件 实付款
                    <span className={styles.amount}>{order.payAmount || order.totalAmount}</span>
                  </div>
                </div>
                <div className={styles.actionRow}>
                  <Button
                    type='primary'
                    size='small'
                    className={styles.sendBtn}
                    style={{ color: '#fff' }}
                    onClick={e => {
                      e.stopPropagation()
                      onSelect(order)
                    }}
                  >
                    发送此单
                  </Button>
                </div>
              </div>
            </div>
          ))}

          {loading && (
            <div className={styles.loading}>
              <Loading />
            </div>
          )}
          {!loading && orders.length === 0 && (
            <div className={styles.empty}>
              <div className={styles.emptyIcon}>📦</div>
              <div>暂无相关订单</div>
            </div>
          )}
          {!hasMore && orders.length > 0 && <div className={styles.noMore}>没有更多了</div>}
        </div>
      </div>
    </Popup>
  )
}

export default OrderSelector
