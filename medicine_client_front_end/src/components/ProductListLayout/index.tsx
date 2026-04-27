import React, { useState, useEffect, useRef, useCallback } from 'react'
import { ChevronDown, ChevronUp, SlidersHorizontal, X } from 'lucide-react'
import ProductCard from '@/components/ProductCard'
import type { ProductCardData } from '@/components/ProductCard'
import { listSearchTagFilters, type ProductTypes } from '@/api/product'
import { toProductDrugType } from '@/constants/drugCategory'
import { useCartStore } from '@/stores/cartStore'
import { useAuth } from '@/hooks/useAuth'
import SkeletonBlock from '@/components/SkeletonBlock'
import styles from './index.module.less'

type SortType = 'default' | 'sales-asc' | 'sales-desc' | 'price-asc' | 'price-desc'

const PAGE_SIZE = 20

/**
 * 商品搜索/分类首屏骨架屏数量。
 */
const PRODUCT_LIST_SKELETON_COUNT = 6

// 搜索结果类型 (适配 ProductCardData)
export interface ProductListResponse {
  rows: Array<{
    productId?: string
    productName?: string
    cover?: string
    price?: number | string
    sales?: number
    drugCategory?: number
  }>
  total: number
  tagFilters?: ProductTypes.MallProductSearchTagFilterVo[]
}

// 请求参数接口
export interface ProductListParams {
  keyword?: string
  categoryId?: number | string
  categoryName?: string
  pageNum: number
  pageSize: number
  sort?: string // 后端排序字段
  order?: string // 后端排序方向
  minPrice?: number
  maxPrice?: number
  tagIds?: string[]
}

interface ProductListLayoutProps {
  // 请求函数
  requestFn: (params: ProductListParams) => Promise<ProductListResponse>
  // 初始参数 (keyword 或 categoryId)
  initialParams?: Partial<ProductListParams>
  // 自定义头部
  headerSlot?: React.ReactNode
  // 自定义次级头部 (如分类同级列表)
  subHeaderSlot?: React.ReactNode
  // 是否启用商品标签筛选
  enableTagFilters?: boolean
}

/**
 * 渲染商品列表卡片骨架屏。
 *
 * @param index - 骨架屏卡片序号。
 * @returns 商品卡片骨架节点。
 */
const renderProductSkeletonCard = (index: number) => (
  <div key={`product-skeleton-${index}`} className={styles.skeletonCard} aria-hidden='true'>
    <SkeletonBlock className={styles.skeletonImage} />
    <div className={styles.skeletonContent}>
      <SkeletonBlock className={styles.skeletonTitle} />
      <SkeletonBlock className={styles.skeletonTitleShort} />
      <div className={styles.skeletonFooter}>
        <div className={styles.skeletonPriceGroup}>
          <SkeletonBlock className={styles.skeletonPrice} />
          <SkeletonBlock className={styles.skeletonSale} />
        </div>
        <SkeletonBlock className={styles.skeletonButton} />
      </div>
    </div>
  </div>
)

const ProductListLayout: React.FC<ProductListLayoutProps> = ({
  requestFn,
  initialParams = {},
  headerSlot,
  subHeaderSlot,
  enableTagFilters = false
}) => {
  const [results, setResults] = useState<ProductCardData[]>([])
  const [loading, setLoading] = useState(false)
  const [pageNum, setPageNum] = useState(1)
  const [hasMore, setHasMore] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)

  const { addItem } = useCartStore()
  const { requireLogin } = useAuth()

  // 排序和筛选状态
  const [sortType, setSortType] = useState<SortType>('default')
  const [showFilterDrawer, setShowFilterDrawer] = useState(false)
  const [priceRange, setPriceRange] = useState<[number, number]>([0, 0])
  const [tempPriceRange, setTempPriceRange] = useState<[number, number]>([0, 0])
  const [tagFilters, setTagFilters] = useState<ProductTypes.MallProductSearchTagFilterVo[]>([])
  const [selectedTagIds, setSelectedTagIds] = useState<string[]>([])
  const [tempSelectedTagIds, setTempSelectedTagIds] = useState<string[]>([])
  const [expandedTagGroups, setExpandedTagGroups] = useState<Record<string, boolean>>({})

  const resultsRef = useRef<HTMLDivElement>(null)

  const hasAppliedFilters = priceRange[0] > 0 || priceRange[1] > 0 || selectedTagIds.length > 0

  // 构建请求参数
  const buildParams = useCallback(
    (page: number): ProductListParams => {
      const params: ProductListParams = {
        ...initialParams,
        pageNum: page,
        pageSize: PAGE_SIZE
      }

      // 处理排序
      if (sortType === 'sales-asc') {
        params.sort = 'sales'
        params.order = 'asc'
      } else if (sortType === 'sales-desc') {
        params.sort = 'sales'
        params.order = 'desc'
      } else if (sortType === 'price-asc') {
        params.sort = 'price'
        params.order = 'asc'
      } else if (sortType === 'price-desc') {
        params.sort = 'price'
        params.order = 'desc'
      }

      // 处理价格筛选
      if (priceRange[0] > 0) params.minPrice = priceRange[0]
      if (priceRange[1] > 0) params.maxPrice = priceRange[1]
      if (enableTagFilters && selectedTagIds.length > 0) params.tagIds = selectedTagIds

      return params
    },
    [enableTagFilters, initialParams, sortType, priceRange, selectedTagIds]
  )

  // 执行搜索
  const fetchData = useCallback(
    async (page: number = 1, append: boolean = false) => {
      if (page === 1) {
        setLoading(true)
      } else {
        setLoadingMore(true)
      }

      try {
        const params = buildParams(page)
        const response = await requestFn(params)

        const data = response.rows || []
        const formattedData: ProductCardData[] = data.map(item => ({
          productId: item.productId,
          productName: item.productName,
          coverImage: item.cover,
          price: String(item.price),
          sales: item.sales,
          type: toProductDrugType(item.drugCategory)
        }))

        if (append) {
          setResults(prev => [...prev, ...formattedData])
        } else {
          setResults(formattedData)
        }

        setHasMore(page * PAGE_SIZE < (response.total || 0))
        setPageNum(page)
      } catch (error) {
        console.error('加载列表失败:', error)
      } finally {
        setLoading(false)
        setLoadingMore(false)
      }
    },
    [buildParams, requestFn]
  )

  /**
   * 加载搜索筛选弹窗使用的全量商品标签。
   * @returns Promise<void> 加载完成后更新标签分组状态
   */
  const fetchSearchTagFilters = useCallback(async (): Promise<void> => {
    if (!enableTagFilters) {
      return
    }
    try {
      const filterGroups = await listSearchTagFilters()
      setTagFilters(filterGroups)
    } catch (error) {
      console.error('加载商品标签筛选失败:', error)
    }
  }, [enableTagFilters])

  // 初始加载 & 监听参数变化
  useEffect(() => {
    const hasKey = initialParams.keyword || initialParams.categoryId || initialParams.categoryName
    if (hasKey) {
      fetchData(1, false)
    }
  }, [fetchData, initialParams.keyword, initialParams.categoryId, initialParams.categoryName])

  // 初始化商品标签筛选数据
  useEffect(() => {
    if (!enableTagFilters) {
      setTagFilters([])
      return
    }
    fetchSearchTagFilters()
  }, [enableTagFilters, fetchSearchTagFilters])

  // 处理排序点击
  const handleSortClick = (type: SortType) => {
    if (type === 'price-asc' || type === 'price-desc') {
      if (sortType === 'price-asc') {
        setSortType('price-desc')
      } else if (sortType === 'price-desc') {
        setSortType('price-asc')
      } else {
        setSortType('price-asc')
      }
    } else if (type === 'sales-asc' || type === 'sales-desc') {
      if (sortType === 'sales-desc') {
        setSortType('sales-asc')
      } else if (sortType === 'sales-asc') {
        setSortType('sales-desc')
      } else {
        setSortType('sales-desc')
      }
    } else {
      setSortType(type)
    }
  }

  // 加入购物车逻辑
  const handleAddToCart = async (data: ProductCardData) => {
    if (!requireLogin()) return

    try {
      await addItem({ productId: Number(data.productId) })
      // addItem 内部已经调用了 showSuccessNotify('已添加到购物车')
      // 且调用了 fetchCartCount()，所以这里可以简化
    } catch (error) {
      console.error('加购失败:', error)
    }
  }

  // 打开筛选抽屉
  const handleOpenFilter = () => {
    setTempPriceRange(priceRange)
    setTempSelectedTagIds(selectedTagIds)
    setShowFilterDrawer(true)
  }

  // 关闭筛选抽屉
  const handleCloseFilter = () => {
    setShowFilterDrawer(false)
  }

  // 重置筛选
  const handleResetFilter = () => {
    setTempPriceRange([0, 0])
    setTempSelectedTagIds([])
  }

  // 应用筛选
  const handleApplyFilter = () => {
    setPriceRange(tempPriceRange)
    setSelectedTagIds(tempSelectedTagIds)
    setShowFilterDrawer(false)
  }

  // 切换临时标签选中状态
  const handleToggleTempTag = (tagId?: string) => {
    if (!tagId) return
    setTempSelectedTagIds(prev =>
      prev.includes(tagId) ? prev.filter(currentTagId => currentTagId !== tagId) : [...prev, tagId]
    )
  }

  // 展开/收起标签组
  const handleToggleGroupExpand = (groupId: string) => {
    setExpandedTagGroups(prev => ({
      ...prev,
      [groupId]: !prev[groupId]
    }))
  }

  // 加载更多
  const handleLoadMore = () => {
    if (!loadingMore && hasMore) {
      fetchData(pageNum + 1, true)
    }
  }

  // 监听滚动加载更多
  const handleScroll = (e: React.UIEvent<HTMLDivElement>) => {
    const target = e.target as HTMLDivElement
    const { scrollTop, scrollHeight, clientHeight } = target
    if (scrollHeight - scrollTop - clientHeight < 100 && !loadingMore && hasMore) {
      handleLoadMore()
    }
  }

  return (
    <div className={styles.layoutContainer}>
      {/* 顶部插槽 */}
      {headerSlot}

      {/* 次级顶部插槽 (如分类同级列表) */}
      {subHeaderSlot}

      {/* 排序栏 */}
      <div className={styles.sortBar}>
        <div
          className={`${styles.sortItem} ${sortType === 'default' ? styles.active : ''}`}
          onClick={() => handleSortClick('default')}
        >
          综合
        </div>
        <div
          className={`${styles.sortItem} ${sortType === 'sales-asc' || sortType === 'sales-desc' ? styles.active : ''}`}
          onClick={() => handleSortClick('sales-asc')}
        >
          销量
          <div className={styles.sortIcons}>
            <ChevronUp
              size={12}
              className={`${styles.sortIcon} ${styles.sortIconUp} ${sortType === 'sales-asc' ? styles.activeIcon : ''}`}
            />
            <ChevronDown
              size={12}
              className={`${styles.sortIcon} ${styles.sortIconDown} ${sortType === 'sales-desc' ? styles.activeIcon : ''}`}
            />
          </div>
        </div>
        <div
          className={`${styles.sortItem} ${sortType === 'price-asc' || sortType === 'price-desc' ? styles.active : ''}`}
          onClick={() => handleSortClick('price-asc')}
        >
          价格
          <div className={styles.sortIcons}>
            <ChevronUp
              size={12}
              className={`${styles.sortIcon} ${styles.sortIconUp} ${sortType === 'price-asc' ? styles.activeIcon : ''}`}
            />
            <ChevronDown
              size={12}
              className={`${styles.sortIcon} ${styles.sortIconDown} ${sortType === 'price-desc' ? styles.activeIcon : ''}`}
            />
          </div>
        </div>
        <div
          className={`${styles.filterTrigger} ${hasAppliedFilters ? styles.filterTriggerActive : ''}`}
          onClick={handleOpenFilter}
        >
          筛选
          {hasAppliedFilters && <span className={styles.filterBadge}>{selectedTagIds.length || '1'}</span>}
          <SlidersHorizontal size={14} className={styles.filterIcon} />
        </div>
      </div>

      {/* 结果列表 */}
      <div className={styles.results} ref={resultsRef} onScroll={handleScroll}>
        {loading && pageNum === 1 ? (
          <div className={styles.productList}>
            {Array.from({ length: PRODUCT_LIST_SKELETON_COUNT }).map((_, index) => renderProductSkeletonCard(index))}
          </div>
        ) : results.length === 0 ? (
          <div className={styles.emptyWrapper}>
            <span className={styles.emptyIcon}>🔍</span>
            <span className={styles.emptyText}>未找到相关商品</span>
          </div>
        ) : (
          <div className={styles.productList}>
            {results.map(item => (
              <ProductCard key={item.productId} data={item} layout='vertical' onAddToCart={handleAddToCart} />
            ))}
            {loadingMore && (
              <div className={styles.loadMore}>
                <div className={styles.spinner} />
                加载中...
              </div>
            )}
            {!hasMore && results.length > 0 && <div className={styles.noMore}>已经到底啦</div>}
          </div>
        )}
      </div>

      {/* 筛选抽屉 */}
      {showFilterDrawer && (
        <>
          <div className={styles.drawerMask} onClick={handleCloseFilter}></div>
          <div className={styles.filterDrawer}>
            <div className={styles.drawerHeader}>
              <span className={styles.drawerTitle}>筛选</span>
              <X size={20} onClick={handleCloseFilter} style={{ cursor: 'pointer' }} />
            </div>

            <div className={styles.drawerContent}>
              {/* 价格区间 */}
              <div className={styles.filterSection}>
                <div className={styles.filterLabel}>价格区间</div>
                <div className={styles.priceInputs}>
                  <input
                    type='number'
                    className={styles.priceInput}
                    placeholder='最低价'
                    value={tempPriceRange[0] || ''}
                    onChange={e => setTempPriceRange([Number(e.target.value) || 0, tempPriceRange[1]])}
                  />
                  <span className={styles.priceSeparator}>-</span>
                  <input
                    type='number'
                    className={styles.priceInput}
                    placeholder='最高价'
                    value={tempPriceRange[1] || ''}
                    onChange={e => setTempPriceRange([tempPriceRange[0], Number(e.target.value) || 0])}
                  />
                </div>
              </div>

              {/* 快捷价格选项 */}
              <div className={styles.filterSection}>
                <div className={styles.filterLabel}>快捷选择</div>
                <div className={styles.quickPrices}>
                  <div
                    className={`${styles.quickPrice} ${tempPriceRange[0] === 0 && tempPriceRange[1] === 50 ? styles.active : ''}`}
                    onClick={() => setTempPriceRange([0, 50])}
                  >
                    0-50元
                  </div>
                  <div
                    className={`${styles.quickPrice} ${tempPriceRange[0] === 50 && tempPriceRange[1] === 100 ? styles.active : ''}`}
                    onClick={() => setTempPriceRange([50, 100])}
                  >
                    50-100元
                  </div>
                  <div
                    className={`${styles.quickPrice} ${tempPriceRange[0] === 100 && tempPriceRange[1] === 200 ? styles.active : ''}`}
                    onClick={() => setTempPriceRange([100, 200])}
                  >
                    100-200元
                  </div>
                  <div
                    className={`${styles.quickPrice} ${tempPriceRange[0] === 200 && tempPriceRange[1] === 0 ? styles.active : ''}`}
                    onClick={() => setTempPriceRange([200, 0])}
                  >
                    200元以上
                  </div>
                </div>
              </div>

              {enableTagFilters && tagFilters.length > 0 && (
                <div className={styles.filterSection}>
                  <div className={styles.filterLabel}>商品标签</div>
                  <div className={styles.tagFilterGroups}>
                    {tagFilters.map(group => {
                      const groupId = String(group.typeId || group.typeCode || '')
                      const isExpanded = !!expandedTagGroups[groupId]
                      const allOptions = group.options || []

                      const DEFAULT_SHOW_COUNT = 6
                      const hasMore = allOptions.length > DEFAULT_SHOW_COUNT
                      const showOptions = isExpanded ? allOptions : allOptions.slice(0, DEFAULT_SHOW_COUNT)

                      return (
                        <div key={groupId} className={styles.tagFilterGroup}>
                          <div className={styles.tagFilterGroupTitle}>
                            <span>{group.typeName || '标签'}</span>
                          </div>
                          <div className={styles.tagFilterOptions}>
                            {showOptions.map(option => {
                              const tagId = option.tagId ? String(option.tagId) : ''
                              const isSelected = tagId ? tempSelectedTagIds.includes(tagId) : false
                              return (
                                <button
                                  key={tagId || `${group.typeCode}-${option.tagName}`}
                                  type='button'
                                  className={`${styles.tagFilterOption} ${isSelected ? styles.tagFilterOptionActive : ''}`}
                                  onClick={() => handleToggleTempTag(tagId)}
                                >
                                  <span className={styles.tagFilterOptionText}>{option.tagName || '标签'}</span>
                                  {typeof option.count === 'number' && (
                                    <span className={styles.tagFilterOptionCount}>（{option.count}）</span>
                                  )}
                                </button>
                              )
                            })}
                            {hasMore && (
                              <button
                                type='button'
                                className={`${styles.tagFilterOption} ${styles.tagFilterToggleOption}`}
                                onClick={() => handleToggleGroupExpand(groupId)}
                              >
                                <span className={styles.tagFilterOptionText}>{isExpanded ? '收起' : '更多'}</span>
                                {isExpanded ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                              </button>
                            )}
                          </div>
                        </div>
                      )
                    })}
                  </div>
                </div>
              )}
            </div>

            <div className={styles.drawerFooter}>
              <div className={styles.resetBtn} onClick={handleResetFilter}>
                重置
              </div>
              <div className={styles.confirmBtn} onClick={handleApplyFilter}>
                确定
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  )
}

export default ProductListLayout
