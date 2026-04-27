import React, { useState, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { Del } from '@nutui/icons-react'
import { Plus } from 'lucide-react'
import styles from './index.module.less'

/**
 * 商品卡片数据接口
 */
export interface ProductCardData {
  /** 商品ID */
  productId?: string
  /** 商品名称 */
  productName?: string
  /** 商品封面图 */
  coverImage?: string
  /** 商品价格 */
  price?: string
  /** 商品销量 */
  sales?: number
  /** 商品类型（OTC绿/OTC红/Rx/器械） */
  type?: 'OTC绿' | 'OTC红' | 'Rx' | '器械'
  /** 浏览次数（可选，用于浏览历史） */
  viewCount?: number
  /** 最后浏览时间（可选，用于浏览历史） */
  lastViewTime?: string
}

interface ProductCardProps {
  /** 商品数据 */
  data: ProductCardData
  /** 是否显示浏览信息（次数和时间） */
  showViewInfo?: boolean
  /** 是否支持滑动操作 */
  swipeable?: boolean
  /** 删除回调 */
  onDelete?: (productId: string) => void
  /** 点击加入购物车回调 */
  onAddToCart?: (data: ProductCardData, e: React.MouseEvent) => void
  /** 布局模式 */
  layout?: 'horizontal' | 'vertical'
}

const ProductCard: React.FC<ProductCardProps> = ({
  data,
  showViewInfo = false,
  swipeable = false,
  onDelete,
  onAddToCart,
  layout = 'horizontal'
}) => {
  const navigate = useNavigate()
  const [showActions, setShowActions] = useState(false)
  const startXRef = useRef(0)
  const startYRef = useRef(0)
  const isMovingRef = useRef(false)

  // 处理价格显示，移除可能存在的￥符号
  const displayPrice = String(data.price ?? '').replace(/^￥|^¥/, '') || '0.00'

  // 点击跳转到商品详情
  const handleClick = () => {
    if (isMovingRef.current) return
    if (showActions) {
      setShowActions(false)
      return
    }
    if (data.productId) {
      navigate(`/product/${data.productId}`)
    }
  }

  // 删除操作
  const handleDelete = (e: React.MouseEvent) => {
    e.stopPropagation()
    if (data.productId && onDelete) {
      onDelete(data.productId)
    }
  }

  // 去下单操作
  const handleBuyNow = (e: React.MouseEvent) => {
    e.stopPropagation()
    if (data.productId) {
      navigate(`/product/${data.productId}`)
    }
  }

  // 加入购物车操作
  const handleAddToCartClick = (e: React.MouseEvent) => {
    e.stopPropagation()
    if (onAddToCart) {
      onAddToCart(data, e)
    }
  }

  // 格式化时间显示
  const formatTime = (timeStr?: string) => {
    if (!timeStr) return ''
    const date = new Date(timeStr)
    const now = new Date()
    const diffTime = now.getTime() - date.getTime()
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24))

    if (diffDays === 0) {
      return '今天'
    } else if (diffDays === 1) {
      return '昨天'
    } else if (diffDays < 7) {
      return `${diffDays}天前`
    } else {
      const month = date.getMonth() + 1
      const day = date.getDate()
      return `${month}月${day}日`
    }
  }

  // 触摸事件处理
  const handleTouchStart = (e: React.TouchEvent) => {
    if (!swipeable) return
    startXRef.current = e.touches[0].clientX
    startYRef.current = e.touches[0].clientY
    isMovingRef.current = false
  }

  const handleTouchMove = (e: React.TouchEvent) => {
    if (!swipeable) return
    const deltaX = e.touches[0].clientX - startXRef.current
    const deltaY = e.touches[0].clientY - startYRef.current

    if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 10) {
      isMovingRef.current = true
    }
  }

  const handleTouchEnd = (e: React.TouchEvent) => {
    if (!swipeable) return
    const deltaX = e.changedTouches[0].clientX - startXRef.current
    const deltaY = e.changedTouches[0].clientY - startYRef.current

    if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 50) {
      if (deltaX < 0) {
        setShowActions(true)
      } else {
        setShowActions(false)
      }
    }

    setTimeout(() => {
      isMovingRef.current = false
    }, 100)
  }

  // 获取徽章样式
  const getBadgeStyle = () => {
    switch (data.type) {
      case 'OTC绿':
        return { text: 'OTC绿', color: '#0f8a3a', bg: '#e8f8ed' }
      case 'OTC红':
        return { text: 'OTC红', color: '#d9363e', bg: '#ffeceb' }
      case 'Rx':
        return { text: 'Rx', color: '#ff3b30', bg: '#ffeaea' }
      case '器械':
        return { text: '器械', color: '#8e8e93', bg: '#f2f2f7' }
      default:
        return null
    }
  }

  const badge = getBadgeStyle()

  return (
    <div className={`${styles.cardWrapper} ${layout === 'vertical' ? styles.verticalWrapper : ''}`}>
      <div
        className={`${styles.card} ${layout === 'vertical' ? styles.verticalCard : ''} ${showActions ? styles.cardSlided : ''}`}
        onClick={handleClick}
        onTouchStart={handleTouchStart}
        onTouchMove={handleTouchMove}
        onTouchEnd={handleTouchEnd}
      >
        <div className={`${styles.imageWrapper} ${layout === 'vertical' ? styles.verticalImageWrapper : ''}`}>
          <img
            src={
              data.coverImage ||
              'https://bkimg.cdn.bcebos.com/pic/b3b7d0a20cf431adcbef4a1d566fbbaf2edda3cc32c4?x-bce-process=image/format,f_auto/quality,Q_70/resize,m_lfit,limit_1,w_536'
            }
            alt={data.productName}
            className={styles.image}
          />
          {badge && (
            <span className={styles.badge} style={{ color: badge.color, backgroundColor: badge.bg }}>
              {badge.text}
            </span>
          )}
        </div>
        <div className={`${styles.content} ${layout === 'vertical' ? styles.verticalContent : ''}`}>
          <h3 className={styles.title}>{data.productName || '商品名称'}</h3>
          {showViewInfo && (
            <div className={styles.meta}>
              <span className={styles.viewCount}>浏览{data.viewCount || 1}次</span>
              <span className={styles.time}>{formatTime(data.lastViewTime)}</span>
            </div>
          )}
          <div className={styles.footer}>
            <div className={styles.priceWrapper}>
              <div className={styles.price}>
                <span className={styles.priceSymbol}>¥</span>
                <span className={styles.priceValue}>{displayPrice}</span>
              </div>
              {data.sales !== undefined && data.sales > 0 && (
                <span className={styles.sales}>已售{data.sales > 9999 ? '9999+' : data.sales}</span>
              )}
            </div>
            {layout === 'vertical' ? (
              <button className={styles.addBtn} onClick={handleAddToCartClick}>
                <Plus size={16} />
              </button>
            ) : (
              <div className={styles.buyBtn} onClick={handleBuyNow}>
                去下单
              </div>
            )}
          </div>
        </div>
      </div>
      {swipeable && (
        <div className={`${styles.actions} ${showActions ? styles.actionsVisible : ''}`}>
          <div className={styles.buyAction} onClick={handleBuyNow}>
            <span>去下单</span>
          </div>
          <div className={styles.deleteAction} onClick={handleDelete}>
            <Del color='#fff' width={18} height={18} />
            <span>删除</span>
          </div>
        </div>
      )}
    </div>
  )
}

export default ProductCard
