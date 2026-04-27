import React, { useMemo } from 'react'
import styles from './index.module.less'

export interface WaterfallItem {
  id: string
  src: string
  title: string
  sale: string
  price: string
  type?: 'OTC绿' | 'OTC红' | 'Rx' | '器械'
}

interface WaterfallProps {
  /** 瀑布流商品列表。 */
  items: WaterfallItem[]
  /** 商品卡片点击事件。 */
  onItemClick?: (item: WaterfallItem) => void
  /** 商品加入购物车事件。 */
  onAddToCart?: (item: WaterfallItem, triggerEl: HTMLButtonElement) => void
  /** 两列之间的间距，单位 px。 */
  columnGap?: number
  /** 是否展示商品加载骨架屏。 */
  loading?: boolean
  /** 骨架屏商品卡片数量。 */
  skeletonCount?: number
}

/**
 * 旧版双列卡片使用的预设图片高度。
 * 通过固定序列制造上下错落的瀑布流视觉。
 */
const PREDEFINED_HEIGHTS = [140, 130, 160, 200, 190, 240, 170, 210, 185, 230, 175, 195, 225, 165, 215]

/**
 * 瀑布流默认骨架屏卡片数量。
 */
const DEFAULT_WATERFALL_SKELETON_COUNT = 6

/**
 * 根据商品类型生成徽章样式。
 *
 * @param type 商品类型
 * @returns 徽章样式，不需要展示时返回 undefined
 */
const getBadgeStyle = (type?: WaterfallItem['type']) => {
  switch (type) {
    case 'OTC绿':
      return { text: 'OTC绿', color: '#0f8a3a', bg: '#e8f8ed' }
    case 'OTC红':
      return { text: 'OTC红', color: '#d9363e', bg: '#ffeceb' }
    case 'Rx':
      return { text: 'Rx', color: '#ff3b30', bg: '#ffeaea' }
    case '器械':
      return { text: '器械', color: '#8e8e93', bg: '#f2f2f7' }
    default:
      return undefined
  }
}

/**
 * 旧版双列卡片渲染态数据。
 * 在原始商品数据上补充图片展示高度。
 */
interface WaterfallRenderItem extends WaterfallItem {
  /** 当前卡片图片区域高度。 */
  height: number
  /** 徽章文本。 */
  badge?: string
  /** 徽章颜色。 */
  badgeColor?: string
  /** 徽章背景色。 */
  badgeBg?: string
}

/**
 * 瀑布流骨架屏渲染数据。
 */
interface WaterfallSkeletonItem {
  /** 骨架屏唯一标识。 */
  id: string
  /** 骨架屏图片区域高度。 */
  height: number
}

/**
 * 根据数量生成左右两列骨架屏数据。
 *
 * @param count - 骨架屏卡片数量
 * @returns 左右两列骨架屏数据
 */
const buildSkeletonColumns = (
  count: number
): { leftColumn: WaterfallSkeletonItem[]; rightColumn: WaterfallSkeletonItem[] } => {
  const leftColumn: WaterfallSkeletonItem[] = []
  const rightColumn: WaterfallSkeletonItem[] = []

  Array.from({ length: count }).forEach((_, index) => {
    const skeletonItem: WaterfallSkeletonItem = {
      id: `waterfall-skeleton-${index}`,
      height: PREDEFINED_HEIGHTS[index % PREDEFINED_HEIGHTS.length]
    }

    if (index % 2 === 0) {
      leftColumn.push(skeletonItem)
    } else {
      rightColumn.push(skeletonItem)
    }
  })

  return { leftColumn, rightColumn }
}

const Waterfall: React.FC<WaterfallProps> = ({
  items,
  onItemClick,
  columnGap = 8,
  loading = false,
  skeletonCount = DEFAULT_WATERFALL_SKELETON_COUNT
}) => {
  /**
   * 处理价格显示，移除可能存在的货币符号。
   *
   * @param price - 原始价格字符串
   * @returns 纯数字价格文本
   */
  const formatPrice = (price: string) => {
    return price?.replace(/^￥|^¥/, '') || '0.00'
  }

  /**
   * 根据索引分配商品到左右列。
   * 偶数索引放左列，奇数索引放右列，并结合预设高度还原旧版双列瀑布流效果。
   *
   * @returns 左右两列的商品数据
   */
  const { leftColumn, rightColumn } = useMemo(() => {
    const left: WaterfallRenderItem[] = []
    const right: WaterfallRenderItem[] = []

    items.forEach((item, index) => {
      const badgeData = getBadgeStyle(item.type)
      const renderItem: WaterfallRenderItem = {
        ...item,
        height: PREDEFINED_HEIGHTS[index % PREDEFINED_HEIGHTS.length],
        badge: badgeData?.text,
        badgeColor: badgeData?.color,
        badgeBg: badgeData?.bg
      }

      if (index % 2 === 0) {
        left.push(renderItem)
      } else {
        right.push(renderItem)
      }
    })

    return { leftColumn: left, rightColumn: right }
  }, [items])

  /**
   * 首页商品加载时使用的骨架屏双列数据。
   *
   * @returns 左右两列骨架屏数据
   */
  const { leftColumn: leftSkeletonColumn, rightColumn: rightSkeletonColumn } = useMemo(() => {
    return buildSkeletonColumns(skeletonCount)
  }, [skeletonCount])

  /**
   * 渲染单个商品骨架屏卡片。
   *
   * @param item - 骨架屏卡片渲染数据
   * @returns 商品骨架屏卡片节点
   */
  const renderSkeletonCard = (item: WaterfallSkeletonItem) => (
    <div key={item.id} className={`${styles.card} ${styles.skeletonCard}`} aria-hidden='true'>
      <div className={`${styles.imageWrapper} ${styles.skeletonImage}`} style={{ height: item.height }}></div>
      <div className={styles.content}>
        <div className={`${styles.skeletonBlock} ${styles.skeletonTitle}`}></div>
        <div className={`${styles.skeletonBlock} ${styles.skeletonTitleShort}`}></div>
        <div className={`${styles.skeletonBlock} ${styles.skeletonSale}`}></div>
        <div className={`${styles.skeletonBlock} ${styles.skeletonPrice}`}></div>
      </div>
    </div>
  )

  /**
   * 渲染单个商品推荐卡片。
   *
   * @param item - 卡片渲染数据
   * @returns 商品卡片节点
   */
  const renderCard = (item: WaterfallRenderItem) => (
    <div key={item.id} className={styles.card} onClick={() => onItemClick?.(item)}>
      <div className={styles.imageWrapper} style={{ height: item.height }}>
        <img src={item.src} alt={item.title} loading='lazy' />
        {item.badge && (
          <span className={styles.badge} style={{ color: item.badgeColor, backgroundColor: item.badgeBg }}>
            {item.badge}
          </span>
        )}
      </div>
      <div className={styles.content}>
        <h3 className={styles.title}>{item.title}</h3>
        <p className={styles.sale}>{item.sale}</p>
        <div className={styles.footer}>
          <div className={styles.price}>
            <span className={styles.priceSymbol}>¥</span>
            <span className={styles.priceValue}>{formatPrice(item.price)}</span>
          </div>
        </div>
      </div>
    </div>
  )

  if (loading) {
    return (
      <div className={styles.waterfall} style={{ gap: columnGap }}>
        <div className={styles.column}>{leftSkeletonColumn.map(renderSkeletonCard)}</div>
        <div className={styles.column}>{rightSkeletonColumn.map(renderSkeletonCard)}</div>
      </div>
    )
  }

  return (
    <div className={styles.waterfall} style={{ gap: columnGap }}>
      <div className={styles.column}>{leftColumn.map(renderCard)}</div>
      <div className={styles.column}>{rightColumn.map(renderCard)}</div>
    </div>
  )
}

export default Waterfall
