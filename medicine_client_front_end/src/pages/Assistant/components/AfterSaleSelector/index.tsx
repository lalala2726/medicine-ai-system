import React from 'react'
import { Popup, Button, Loading, SearchBar } from '@nutui/nutui-react'
import { Close } from '@nutui/icons-react'
import type { OrderAfterSaleTypes } from '@/api/orderAfterSale'
import { useAfterSaleSelectorController } from '../../modules/selectors/useAfterSaleSelectorController'
import SelectorTabs from '../SelectorTabs'
import styles from './index.module.less'

interface AfterSaleSelectorProps {
  /** 是否显示 */
  visible: boolean
  /** 初始筛选状态 */
  initialStatus?: string
  /** 关闭回调 */
  onClose: () => void
  /** 选中售后单回调 */
  onSelect: (item: OrderAfterSaleTypes.AfterSaleListVo) => void
}

const AfterSaleSelector: React.FC<AfterSaleSelectorProps> = ({ visible, initialStatus, onClose, onSelect }) => {
  const {
    loading,
    list,
    keyword,
    tabValue,
    hasMore,
    tabs,
    setKeyword,
    handleSearch,
    handleTabChange,
    handleScroll,
    getStatusClass
  } = useAfterSaleSelectorController({
    visible,
    initialStatus
  })

  return (
    <Popup visible={visible} position='bottom' round onClose={onClose} style={{ height: '80vh' }}>
      <div className={styles.container} data-assistant-drawer-swipe-lock='true'>
        <div className={styles.topSection}>
          <div className={styles.header}>
            <span className={styles.title}>选择售后单</span>
            <Close className={styles.close} onClick={onClose} />
          </div>
          <div className={styles.searchBar}>
            <SearchBar
              placeholder='搜索订单编号'
              value={keyword}
              onChange={val => setKeyword(val)}
              onSearch={handleSearch}
              onClear={() => handleSearch('')}
            />
          </div>
          <div className={styles.tabsWrapper}>
            <SelectorTabs value={tabValue} tabs={[...tabs]} onChange={handleTabChange} ariaLabel='售后状态筛选' />
          </div>
        </div>

        <div className={styles.list} onScroll={handleScroll}>
          {list.map(item => (
            <div key={item.id} className={styles.card}>
              <div className={styles.cardHeader}>
                <span className={styles.afterSaleNo}>售后单号: {item.afterSaleNo}</span>
                <span className={`${styles.status} ${getStatusClass(item.afterSaleStatus)}`}>
                  {item.afterSaleStatusName}
                </span>
              </div>

              <div className={styles.product}>
                {item.productImage && <img src={item.productImage} alt={item.productName} className={styles.image} />}
                <div className={styles.info}>
                  <div className={styles.productTitle}>{item.productName}</div>
                  <div className={styles.meta}>
                    <span className={styles.type}>{item.afterSaleTypeName}</span>
                    {item.refundAmount && <span className={styles.refundAmount}>退款: ¥{item.refundAmount}</span>}
                  </div>
                </div>
                <div className={styles.action}>
                  <Button
                    type='primary'
                    size='small'
                    className={styles.sendBtn}
                    onClick={e => {
                      e.stopPropagation()
                      onSelect(item)
                    }}
                  >
                    发送
                  </Button>
                </div>
              </div>

              <div className={styles.cardFooter}>
                <span className={styles.orderNo}>订单: {item.orderNo}</span>
                <span className={styles.time}>{item.applyTime}</span>
              </div>
            </div>
          ))}

          {loading && (
            <div className={styles.loading}>
              <Loading />
            </div>
          )}
          {!loading && list.length === 0 && (
            <div className={styles.empty}>
              <div className={styles.emptyIcon}>🔧</div>
              <div>暂无售后记录</div>
            </div>
          )}
          {!hasMore && list.length > 0 && <div className={styles.noMore}>没有更多了</div>}
        </div>
      </div>
    </Popup>
  )
}

export default AfterSaleSelector
