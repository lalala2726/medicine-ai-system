import { Loading } from '@nutui/nutui-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { ArrowLeft, Search as SearchIcon } from '@nutui/icons-react'
import { X, Search as LucideSearch, ShieldCheck, SlidersHorizontal, ArrowUpDown } from 'lucide-react'
import { search, suggest } from '@/api/product'
import type { ProductTypes } from '@/api/product'
import ProductCard from '@/components/ProductCard'
import type { ProductCardData } from '@/components/ProductCard'
import { toProductDrugType } from '@/constants/drugCategory'
import { useDebounceValue } from '@/hooks/useDebounce'
import { useSearchStore } from '@/stores/searchStore'
import styles from './index.module.less'
import Empty from '@/components/Empty'

// 搜索历史本地存储 key
const SEARCH_HISTORY_KEY = 'searchHistory'

// 搜索发现固定数据
const searchDiscoverData = ['止咳糖浆', '阿司匹林', '布洛芬', '感冒灵', '创可贴', '口罩']

const Search = () => {
  const inputRef = useRef<HTMLInputElement>(null)
  const resultsRef = useRef<HTMLDivElement>(null)
  const location = useLocation()
  const navigate = useNavigate()

  // 从 store 获取搜索状态
  const {
    keyword: storeKeyword,
    results: storeResults,
    hasSearched: storeHasSearched,
    pageNum: storePageNum,
    hasMore: storeHasMore,
    scrollTop: storeScrollTop,
    setSearchState,
    clearSearchState
  } = useSearchStore()

  // 本地状态
  const [searchValue, setSearchValue] = useState(storeKeyword)
  const [searchResults, setSearchResults] = useState<ProductCardData[]>(storeResults)
  const [loading, setLoading] = useState(false)
  const [hasSearched, setHasSearched] = useState(storeHasSearched)
  const [pageNum, setPageNum] = useState(storePageNum)
  const [hasMore, setHasMore] = useState(storeHasMore)
  const [currentKeyword, setCurrentKeyword] = useState(storeKeyword)
  const [sortType, setSortType] = useState<'all' | 'sales' | 'price'>('all')
  const pageSize = 20

  // 搜索建议相关状态
  const [suggestions, setSuggestions] = useState<string[]>([])
  const [showSuggestions, setShowSuggestions] = useState(false)
  const [suggestLoading, setSuggestLoading] = useState(false)

  // 搜索建议缓存（避免重复请求）
  const suggestCacheRef = useRef<Map<string, string[]>>(new Map())

  // 是否显示搜索输入界面（用于在搜索结果页点击搜索框时显示历史和建议）
  const [isSearchInputMode, setIsSearchInputMode] = useState(!storeHasSearched)

  // 搜索历史
  const [recentSearchData, setRecentSearchData] = useState<string[]>(() => {
    try {
      return JSON.parse(localStorage.getItem(SEARCH_HISTORY_KEY) || '[]')
    } catch {
      return []
    }
  })

  // 使用防抖处理搜索输入
  const debouncedSearchValue = useDebounceValue(searchValue, 300)

  // 恢复滚动位置
  useEffect(() => {
    if (storeHasSearched && storeScrollTop > 0 && resultsRef.current && !isSearchInputMode) {
      setTimeout(() => {
        if (resultsRef.current) {
          resultsRef.current.scrollTop = storeScrollTop
        }
      }, 100)
    }
  }, [storeHasSearched, storeScrollTop, isSearchInputMode])

  // 保存搜索历史
  const saveSearchHistory = useCallback(
    (keyword: string) => {
      if (keyword.trim() === '') return

      let arr = [...recentSearchData]
      // 去重：如果已存在，先移除
      arr = arr.filter(item => item !== keyword)
      // 添加到开头
      arr.unshift(keyword)
      // 限制最多保存 20 条
      if (arr.length > 20) {
        arr = arr.slice(0, 20)
      }

      localStorage.setItem(SEARCH_HISTORY_KEY, JSON.stringify(arr))
      setRecentSearchData(arr)
    },
    [recentSearchData]
  )

  // 删除搜索历史
  const handleDeleteHistory = () => {
    localStorage.removeItem(SEARCH_HISTORY_KEY)
    setRecentSearchData([])
  }

  // 获取搜索建议（带缓存）
  const fetchSuggestions = useCallback(async (keyword: string) => {
    if (!keyword.trim()) {
      setSuggestions([])
      setShowSuggestions(false)
      return
    }

    const trimmedKeyword = keyword.trim()

    // 检查缓存
    const cached = suggestCacheRef.current.get(trimmedKeyword)
    if (cached) {
      setSuggestions(cached)
      setShowSuggestions(cached.length > 0)
      return
    }

    setSuggestLoading(true)
    try {
      const result = await suggest(trimmedKeyword)
      if (result && result.length > 0) {
        // 保存到缓存
        suggestCacheRef.current.set(trimmedKeyword, result)
        setSuggestions(result)
        setShowSuggestions(true)
      } else {
        // 空结果也缓存，避免重复请求
        suggestCacheRef.current.set(trimmedKeyword, [])
        setSuggestions([])
        setShowSuggestions(false)
      }
    } catch (error) {
      console.error('获取搜索建议失败:', error)
      setSuggestions([])
      setShowSuggestions(false)
    } finally {
      setSuggestLoading(false)
    }
  }, [])

  // 监听防抖后的搜索值，获取搜索建议（只在搜索输入模式下）
  useEffect(() => {
    if (debouncedSearchValue && isSearchInputMode) {
      fetchSuggestions(debouncedSearchValue)
    }
  }, [debouncedSearchValue, isSearchInputMode, fetchSuggestions])

  // 执行搜索
  const handleSearch = useCallback(
    async (keyword: string, page: number = 1, refresh: boolean = true) => {
      const trimmedKeyword = keyword.trim()
      if (!trimmedKeyword) {
        return
      }

      // 隐藏搜索建议，退出搜索输入模式
      setShowSuggestions(false)
      setSuggestions([])
      setIsSearchInputMode(false)
      setCurrentKeyword(trimmedKeyword)
      setLoading(true)
      setHasSearched(true)

      // 让输入框失去焦点
      inputRef.current?.blur()

      try {
        const result = await search({
          keyword: trimmedKeyword,
          pageNum: page,
          pageSize
        })

        if (result && result.rows) {
          // 转换数据格式
          const products: ProductCardData[] = result.rows.map((item: ProductTypes.MallProductSearchVo) => ({
            productId: item.productId,
            productName: item.productName,
            coverImage: item.cover,
            price: item.price,
            sales: item.sales,
            type: toProductDrugType(item.drugCategory)
          }))

          let newResults: ProductCardData[]
          if (refresh) {
            newResults = products
            setSearchResults(products)
          } else {
            newResults = [...searchResults, ...products]
            setSearchResults(newResults)
          }

          const newHasMore = result.rows.length >= pageSize
          setHasMore(newHasMore)
          setPageNum(page)

          // 保存到 store
          setSearchState({
            keyword: trimmedKeyword,
            results: newResults,
            hasSearched: true,
            pageNum: page,
            hasMore: newHasMore
          })
        } else {
          if (refresh) {
            setSearchResults([])
            setSearchState({
              keyword: trimmedKeyword,
              results: [],
              hasSearched: true,
              pageNum: page,
              hasMore: false
            })
          }
          setHasMore(false)
        }
      } catch (error) {
        console.error('搜索失败:', error)
        if (refresh) {
          setSearchResults([])
        }
      } finally {
        setLoading(false)
      }
    },
    [pageSize, searchResults, setSearchState]
  )

  // 页面初始化
  useEffect(() => {
    // 获取从 Home 页面传递过来的关键字
    const keyword = (location.state as { keyword?: string })?.keyword
    if (keyword) {
      setSearchValue(keyword)
      saveSearchHistory(keyword)
      handleSearch(keyword)
    } else if (!storeHasSearched) {
      // 页面加载后自动聚焦到搜索框（仅在没有搜索结果时）
      const timer = setTimeout(() => {
        if (inputRef.current) {
          inputRef.current.focus()
        }
      }, 300)
      return () => clearTimeout(timer)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [location.state])

  // 点击搜索按钮
  const handleClickSearch = () => {
    if (searchValue.trim()) {
      saveSearchHistory(searchValue.trim())
      handleSearch(searchValue)
    }
  }

  // 搜索框获得焦点（进入搜索输入模式）
  const handleSearchFocus = () => {
    // 只有在有搜索结果时才切换模式，避免不必要的状态变化
    if (hasSearched && !isSearchInputMode) {
      setIsSearchInputMode(true)
    } else if (!hasSearched) {
      setIsSearchInputMode(true)
    }
    // 如果当前有搜索值，获取建议（会使用缓存）
    if (searchValue.trim()) {
      fetchSuggestions(searchValue.trim())
    }
  }

  // 输入框变化
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value
    setSearchValue(value)
    if (!value.trim()) {
      setSuggestions([])
      setShowSuggestions(false)
    }
  }

  // 清空输入框
  const handleClearInput = () => {
    setSearchValue('')
    setSuggestions([])
    setShowSuggestions(false)
    inputRef.current?.focus()
  }

  // 键盘事件
  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      handleClickSearch()
    }
  }

  // 点击搜索建议
  const handleClickSuggestion = (item: string) => {
    setSearchValue(item)
    saveSearchHistory(item)
    handleSearch(item)
  }

  // 点击历史记录或热门搜索
  const handleClickHistoryOrHot = (item: string) => {
    setSearchValue(item)
    saveSearchHistory(item)
    handleSearch(item)
  }

  // 加载更多
  const handleLoadMore = () => {
    if (!loading && hasMore && currentKeyword) {
      handleSearch(currentKeyword, pageNum + 1, false)
    }
  }

  // 返回按钮点击处理 - 始终返回首页
  const handleBackClick = () => {
    // 清空 store 状态
    clearSearchState()
    navigate(-1)
  }

  // 点击商品卡片，保存滚动位置
  const handleProductClick = (item: ProductCardData) => {
    // 保存当前滚动位置
    if (resultsRef.current) {
      setSearchState({ scrollTop: resultsRef.current.scrollTop })
    }
    // 跳转到商品详情
    navigate(`/product/${item.productId}`)
  }

  // 渲染搜索历史和热门搜索
  const renderSearchHistory = () => (
    <div className={styles.historySection}>
      {/* 搜索历史 */}
      <div className={styles.historyBlock}>
        <div className={styles.historyHeader}>
          <span className={styles.historyTitle}>搜索历史</span>
          {recentSearchData.length > 0 && (
            <span className={styles.historyDelete} onClick={handleDeleteHistory}>
              清除全部
            </span>
          )}
        </div>
        <div className={styles.historyTags}>
          {recentSearchData.length > 0 ? (
            recentSearchData.map((item, index) => (
              <span key={index} className={styles.historyTag} onClick={() => handleClickHistoryOrHot(item)}>
                {item}
              </span>
            ))
          ) : (
            <span className={styles.suggestSubText}>暂无搜索历史</span>
          )}
        </div>
      </div>

      {/* 热门搜索 */}
      <div className={styles.historyBlock}>
        <div className={styles.historyHeader}>
          <span className={styles.historyTitle}>热门搜索</span>
        </div>
        <div className={styles.historyTags}>
          {searchDiscoverData.map((item, index) => (
            <span key={index} className={styles.historyTag} onClick={() => handleClickHistoryOrHot(item)}>
              {item}
            </span>
          ))}
        </div>
      </div>

      {/* Footer Branding */}
      <div className={styles.searchFooter}>
        <ShieldCheck className={styles.footerIcon} />
        <p className={styles.footerText}>正品保障 药监认证</p>
      </div>
    </div>
  )

  // 渲染搜索建议列表
  const renderSuggestions = () => (
    <div className={styles.suggestList}>
      {suggestLoading ? (
        <div className={styles.suggestLoading}>
          <Loading type='spinner' />
          <span>正在搜索...</span>
        </div>
      ) : (
        suggestions.map((item, index) => {
          // 高亮匹配文字
          const parts = item.split(new RegExp(`(${searchValue})`, 'gi'))
          return (
            <div key={index} className={styles.suggestItem} onClick={() => handleClickSuggestion(item)}>
              <LucideSearch size={18} className={styles.suggestIcon} />
              <div className={styles.suggestContent}>
                <p className={styles.suggestMainText}>
                  {parts.map((part, i) =>
                    part.toLowerCase() === searchValue.toLowerCase() ? (
                      <span key={i} className={styles.highlight}>
                        {part}
                      </span>
                    ) : (
                      part
                    )
                  )}
                </p>
                <p className={styles.suggestSubText}>所属分类：药房</p>
              </div>
            </div>
          )
        })
      )}
    </div>
  )

  // 渲染搜索结果
  const renderSearchResults = () => (
    <div className={styles.results} ref={resultsRef}>
      {loading && searchResults.length === 0 ? (
        <div className={styles.loadingWrapper}>
          <Loading />
          <div className={styles.loadingText}>搜索中...</div>
        </div>
      ) : searchResults.length === 0 ? (
        <div className={styles.emptyWrapper}>
          <LucideSearch className={styles.emptyIcon} size={48} />
          <Empty description='暂无搜索结果' />
        </div>
      ) : (
        <div className={styles.productGrid}>
          {searchResults.map(item => (
            <div key={item.productId} onClick={() => handleProductClick(item)}>
              <ProductCard data={item} layout='vertical' />
            </div>
          ))}
          {hasMore && searchResults.length >= pageSize && (
            <div className={styles.loadMore} onClick={handleLoadMore}>
              {loading ? '加载中...' : '加载更多'}
            </div>
          )}
        </div>
      )}
      {!hasMore && searchResults.length > 0 && <div className={styles.noMore}>没有更多了</div>}
    </div>
  )

  return (
    <div className={styles.searchPage}>
      {/* 顶部搜索栏 */}
      <div className={styles.header}>
        <div className={styles.headerTop}>
          <div className={styles.backBtn} onClick={handleBackClick}>
            <ArrowLeft width={24} height={24} />
          </div>
          <div className={styles.searchInputWrapper}>
            <SearchIcon className={styles.inputIcon} width={18} height={18} />
            <input
              ref={inputRef}
              className={styles.input}
              type='text'
              placeholder='感冒灵'
              value={searchValue}
              onChange={handleInputChange}
              onFocus={handleSearchFocus}
              onKeyDown={handleKeyDown}
            />
            {searchValue && (
              <div className={styles.clearBtn} onClick={handleClearInput}>
                <X size={14} />
              </div>
            )}
          </div>
          <div className={styles.searchAction} onClick={handleClickSearch}>
            搜索
          </div>
        </div>

        {/* 筛选栏 - 仅在搜索结果模式下显示 */}
        {!isSearchInputMode && hasSearched && (
          <div className={styles.filterBar}>
            <div
              className={`${styles.filterItem} ${sortType === 'all' ? styles.active : ''}`}
              onClick={() => setSortType('all')}
            >
              <span>综合</span>
            </div>
            <div
              className={`${styles.filterItem} ${sortType === 'sales' ? styles.active : ''}`}
              onClick={() => setSortType('sales')}
            >
              <span>销量</span>
              <ArrowUpDown size={14} />
            </div>
            <div
              className={`${styles.filterItem} ${sortType === 'price' ? styles.active : ''}`}
              onClick={() => setSortType('price')}
            >
              <span>价格</span>
              <ArrowUpDown size={14} />
            </div>
            <div className={styles.filterDivider} />
            <div className={styles.filterItem}>
              <span>筛选</span>
              <SlidersHorizontal size={14} />
            </div>
          </div>
        )}
      </div>

      {/* 搜索输入模式：显示建议或历史 */}
      {isSearchInputMode && (
        <>
          {/* 搜索建议（有输入且有建议时显示） */}
          {showSuggestions && suggestions.length > 0 && renderSuggestions()}

          {/* 搜索历史和热门搜索（无建议时显示） */}
          {!showSuggestions && renderSearchHistory()}
        </>
      )}

      {/* 搜索结果（非输入模式且有搜索结果时显示） */}
      {!isSearchInputMode && hasSearched && renderSearchResults()}
    </div>
  )
}

export default Search
