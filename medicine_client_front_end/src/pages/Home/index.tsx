import React, { useEffect, useCallback, useMemo, useRef, useState } from 'react'
import { Badge } from '@nutui/nutui-react'
import { useNavigate } from 'react-router-dom'
import { ShoppingCart, Search, ScanLine, Plus } from 'lucide-react'
import AuthImage from '@/components/AuthImage'
import Waterfall from '@/components/Waterfall'
import type { WaterfallItem } from '@/components/Waterfall'
import { recommend } from '@/api/product'
import { useHomeStore, formatProducts } from '@/stores/homeStore'
import { useCartStore } from '@/stores/cartStore'
import { useCategoryStore } from '@/stores/categoryStore'
import { useAuth, useIsLoggedIn } from '@/hooks/useAuth'
import { getCategoryIconText } from '@/utils/category'
import { showNotify } from '@/utils/notify'
import DisclaimerModal from '@/components/DisclaimerModal'
import { isDisclaimerAgreed } from '@/utils/disclaimer'
import PullRefresh from '@/components/PullRefresh'
import styles from './index.module.less'

/**
 * 首页金刚区骨架屏数量。
 */
const CATEGORY_SKELETON_COUNT = 10

/**
 * 首页推荐商品骨架屏数量。
 */
const RECOMMEND_PRODUCT_SKELETON_COUNT = 6

/**
 * 飞入购物车动画元素尺寸。
 */
const FLYING_ITEM_SIZE = 36

/**
 * 飞入购物车动画时长，单位毫秒。
 */
const FLY_ANIMATION_DURATION = 680

interface FlyingCartItem {
  id: string
  imageSrc?: string
  startX: number
  startY: number
  midX: number
  midY: number
  endX: number
  endY: number
}

const Home: React.FC = () => {
  const navigate = useNavigate()
  const { products, hasLoaded, setProducts } = useHomeStore()
  const { addItem, cartCount, fetchCartCount } = useCartStore()
  const { categories, loading: categoryLoading, fetchCategoryTree } = useCategoryStore()
  const isLoggedIn = useIsLoggedIn()
  const { requireLogin } = useAuth()
  const homePageRef = useRef<HTMLDivElement>(null)
  const cartRef = useRef<HTMLDivElement>(null)
  const timerRefs = useRef<number[]>([])
  const [recommendLoading, setRecommendLoading] = useState(() => !hasLoaded && products.length === 0)
  const homeCategories = useMemo(() => categories.slice(0, CATEGORY_SKELETON_COUNT), [categories])
  const showCategorySkeleton = categoryLoading && homeCategories.length === 0
  /** 首页推荐商品是否展示骨架屏。 */
  const showProductSkeleton = recommendLoading && products.length === 0
  /** 首页是否需要显示金刚区。 */
  const shouldShowCategorySection = showCategorySkeleton || homeCategories.length > 0
  const [flyingItems, setFlyingItems] = useState<FlyingCartItem[]>([])
  const [cartBump, setCartBump] = useState(false)
  const [disclaimerOpen, setDisclaimerOpen] = useState(() => !isDisclaimerAgreed())

  /**
   * 将首页滚动容器重置到顶部，保证首次进入时下拉刷新能正确识别顶部状态。
   *
   * @returns void
   */
  const resetHomeScrollPosition = useCallback((): void => {
    if (homePageRef.current) {
      homePageRef.current.scrollTop = 0
    }

    const layoutBodyElement = document.querySelector('.body')
    if (layoutBodyElement instanceof HTMLElement) {
      layoutBodyElement.scrollTop = 0
    }

    window.scrollTo(0, 0)
  }, [])

  /**
   * 拉取首页推荐商品并写入首页缓存。
   *
   * @returns Promise<void> 无返回值。
   */
  const fetchRecommendProducts = useCallback(async (): Promise<void> => {
    setRecommendLoading(true)
    try {
      const data = await recommend()
      const formattedProducts = formatProducts(data)
      setProducts(formattedProducts)
    } catch (error) {
      console.error('获取商品推荐失败:', error)
      throw error
    } finally {
      setRecommendLoading(false)
    }
  }, [setProducts])

  useEffect(() => {
    if (!hasLoaded) {
      void fetchRecommendProducts()
    }
    void fetchCategoryTree()
  }, [hasLoaded, fetchRecommendProducts, fetchCategoryTree])

  useEffect(() => {
    const rafId = window.requestAnimationFrame(resetHomeScrollPosition)
    return () => window.cancelAnimationFrame(rafId)
  }, [resetHomeScrollPosition])

  useEffect(() => {
    if (disclaimerOpen) {
      return undefined
    }

    const rafId = window.requestAnimationFrame(resetHomeScrollPosition)
    return () => window.cancelAnimationFrame(rafId)
  }, [disclaimerOpen, resetHomeScrollPosition])

  useEffect(() => {
    if (isLoggedIn) {
      fetchCartCount()
    }
  }, [isLoggedIn, fetchCartCount])

  useEffect(() => {
    return () => {
      timerRefs.current.forEach(timer => window.clearTimeout(timer))
      timerRefs.current = []
    }
  }, [])

  const handleSearchClick = () => {
    navigate('/search', { state: { fromHomeSearch: true } })
  }

  const handleToCart = () => {
    navigate('/cart')
  }

  const handleProductClick = (item: WaterfallItem) => {
    navigate(`/product/${item.id}`)
  }

  const handleCategoryClick = (categoryId: string | number) => {
    navigate('/category', { state: { activeId: categoryId } })
  }

  const triggerCartBump = useCallback(() => {
    setCartBump(false)
    window.requestAnimationFrame(() => {
      setCartBump(true)
    })
    const timer = window.setTimeout(() => {
      setCartBump(false)
    }, 360)
    timerRefs.current.push(timer)
  }, [])

  const launchFlyToCart = useCallback(
    (triggerEl: HTMLButtonElement, imageSrc?: string) => {
      if (!cartRef.current) return

      const triggerRect = triggerEl.getBoundingClientRect()
      const cartRect = cartRef.current.getBoundingClientRect()
      const startX = triggerRect.left + triggerRect.width / 2 - FLYING_ITEM_SIZE / 2
      const startY = triggerRect.top + triggerRect.height / 2 - FLYING_ITEM_SIZE / 2
      const targetX = cartRect.left + cartRect.width / 2 - FLYING_ITEM_SIZE / 2
      const targetY = cartRect.top + cartRect.height / 2 - FLYING_ITEM_SIZE / 2
      const endX = targetX - startX
      const endY = targetY - startY
      const midX = endX * 0.56
      const midY = endY * 0.32 - 96
      const id = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`

      setFlyingItems(prev => [...prev, { id, imageSrc, startX, startY, midX, midY, endX, endY }])

      const removeTimer = window.setTimeout(() => {
        setFlyingItems(prev => prev.filter(item => item.id !== id))
      }, FLY_ANIMATION_DURATION)
      const bumpTimer = window.setTimeout(() => {
        triggerCartBump()
      }, FLY_ANIMATION_DURATION - 180)

      timerRefs.current.push(removeTimer, bumpTimer)
    },
    [triggerCartBump]
  )

  const handleAddToCart = useCallback(
    async (item: WaterfallItem, triggerEl: HTMLButtonElement) => {
      if (!requireLogin()) return
      if (!item.id) return

      launchFlyToCart(triggerEl, item.src)

      await addItem({ productId: Number(item.id) || 0 }, { silentSuccess: true })
    },
    [addItem, launchFlyToCart, requireLogin]
  )

  /**
   * 处理首页下拉刷新。
   *
   * @returns Promise<void> 无返回值。
   */
  const handleRefresh = async (): Promise<void> => {
    const results = await Promise.allSettled([fetchRecommendProducts(), fetchCategoryTree(true)])

    if (results.some(result => result.status === 'rejected')) {
      showNotify('刷新失败,请稍后重试')
    }
  }

  const renderCategoryGrid = () => {
    if (showCategorySkeleton) {
      return Array.from({ length: CATEGORY_SKELETON_COUNT }).map((_, index) => (
        <div key={`skeleton-${index}`} className={`${styles.categoryItem} ${styles.categoryItemSkeleton}`}>
          <div className={`${styles.categoryIcon} ${styles.categoryIconSkeleton}`}></div>
          <span className={styles.categoryTitleSkeleton}></span>
        </div>
      ))
    }

    return homeCategories.map((item, index) => {
      return (
        <div
          key={item.id || `category-${index}`}
          className={styles.categoryItem}
          onClick={() => handleCategoryClick(item.id || '')}
        >
          <div className={styles.categoryIcon}>
            {item.cover ? (
              <AuthImage src={item.cover} alt={item.name} className={styles.categoryIconImage} />
            ) : (
              <span className={styles.categoryIconText}>{getCategoryIconText(item.name)}</span>
            )}
          </div>
          <span className={styles.categoryTitle}>{item.name}</span>
        </div>
      )
    })
  }

  return (
    <div ref={homePageRef} className={styles.homePage}>
      <PullRefresh
        mode='external'
        scrollTargetRef={homePageRef}
        style={{ backgroundColor: 'var(--background-light)', minHeight: '100%' }}
        onRefresh={handleRefresh}
      >
        <div className={styles.pageContainer}>
          <header className={styles.header}>
            <div className={styles.searchSection} onClick={handleSearchClick}>
              <div className={styles.searchBar}>
                <Search className={styles.searchIcon} size={20} />
                <span className={styles.searchPlaceholder}>搜索药品、品牌或症状</span>
                <ScanLine className={styles.scanIcon} size={20} />
              </div>
            </div>
          </header>

          <div className={styles.mainBody}>
            <div className={styles.content}>
              {shouldShowCategorySection && (
                <div className={styles.categorySection}>
                  <div className={styles.categoryGrid}>{renderCategoryGrid()}</div>
                </div>
              )}

              <div className={styles.sectionHeader}>
                <span className={styles.sectionTitle}>为您推荐</span>
              </div>

              <Waterfall
                items={products}
                loading={showProductSkeleton}
                skeletonCount={RECOMMEND_PRODUCT_SKELETON_COUNT}
                onItemClick={handleProductClick}
                onAddToCart={handleAddToCart}
                columnGap={8}
              />
            </div>
          </div>
        </div>
      </PullRefresh>

      {flyingItems.map(item => (
        <div
          key={item.id}
          className={styles.flyingItem}
          style={
            {
              left: `${item.startX}px`,
              top: `${item.startY}px`,
              '--fly-mid-x': `${item.midX}px`,
              '--fly-mid-y': `${item.midY}px`,
              '--fly-end-x': `${item.endX}px`,
              '--fly-end-y': `${item.endY}px`
            } as React.CSSProperties
          }
        >
          {item.imageSrc ? <img src={item.imageSrc} alt='' /> : <Plus size={16} />}
        </div>
      ))}

      <div ref={cartRef} className={`${styles.fixedCart} ${cartBump ? styles.cartBump : ''}`} onClick={handleToCart}>
        {cartCount > 0 ? (
          <Badge value={cartCount} max={99}>
            <ShoppingCart size={24} />
          </Badge>
        ) : (
          <ShoppingCart size={24} />
        )}
      </div>

      <DisclaimerModal open={disclaimerOpen} onAgree={() => setDisclaimerOpen(false)} />
    </div>
  )
}

export default Home
