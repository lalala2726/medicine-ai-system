import React, { useEffect, useCallback } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import { ArrowLeft, Search } from 'lucide-react'
import { search, type ProductTypes } from '@/api/product'
import { useSearchStore } from '@/stores/searchStore'
import ProductListLayout, { type ProductListParams, type ProductListResponse } from '@/components/ProductListLayout'
import styles from './index.module.less'

const SearchResult: React.FC = () => {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const keyword = searchParams.get('keyword') || ''
  const { setSearchState } = useSearchStore()

  // 将 URL 中的关键词同步到 store，确保返回搜索页时能读取
  useEffect(() => {
    if (keyword) {
      setSearchState({ keyword })
    }
  }, [keyword, setSearchState])

  /**
   * 将前端排序参数映射到后端搜索请求参数
   * 前端: sort + order (如 'price' + 'asc')
   * 后端: priceSort/salesSort (如 'asc'/'desc')
   */
  const buildSearchParams = useCallback(
    (params: ProductListParams): ProductTypes.MallProductSearchRequest & { pageNum: number; pageSize: number } => {
      const searchParams: ProductTypes.MallProductSearchRequest & { pageNum: number; pageSize: number } = {
        keyword: params.keyword!,
        pageNum: params.pageNum,
        pageSize: params.pageSize
      }

      // 处理价格筛选
      if (params.minPrice && params.minPrice > 0) {
        searchParams.minPrice = params.minPrice
      }
      if (params.maxPrice && params.maxPrice > 0) {
        searchParams.maxPrice = params.maxPrice
      }

      // 处理排序：将前端 sort/order 映射到后端的 priceSort/salesSort
      if (params.sort === 'sales') {
        if (params.order === 'asc') {
          searchParams.salesSort = 'asc'
        } else if (params.order === 'desc') {
          searchParams.salesSort = 'desc'
        }
      } else if (params.sort === 'price') {
        if (params.order === 'asc') {
          searchParams.priceSort = 'asc'
        } else if (params.order === 'desc') {
          searchParams.priceSort = 'desc'
        }
      }
      if (params.tagIds && params.tagIds.length > 0) {
        searchParams.tagIds = params.tagIds
      }
      // 如果 sort 是 'default' 或其他值，不传排序参数，使用后端默认排序

      return searchParams
    },
    []
  )

  // 适配搜索 API
  const requestFn = useCallback(
    async (params: ProductListParams): Promise<ProductListResponse> => {
      // 搜索页面必须有 keyword
      if (!params.keyword) {
        return { rows: [], total: 0 }
      }

      const searchParams = buildSearchParams(params)
      const res = await search(searchParams)

      return {
        rows: res.rows || [],
        total: res.total || 0,
        tagFilters: res.extra?.tagFilters || []
      }
    },
    [buildSearchParams]
  )

  // 点击头部搜索框，跳转到搜索页
  const handleHeaderSearchClick = () => {
    navigate('/search')
  }

  const headerSlot = (
    <div className={styles.header}>
      <div className={styles.backBtn} onClick={() => navigate(-1)}>
        <ArrowLeft size={20} />
      </div>
      <div className={styles.inputContainer} onClick={handleHeaderSearchClick}>
        <div className={styles.inputWrapper}>
          <Search size={20} className={styles.inputIcon} />
          <div className={styles.fakeInput}>{keyword}</div>
        </div>
      </div>
      <div className={styles.searchAction} onClick={handleHeaderSearchClick}>
        搜索
      </div>
    </div>
  )

  return (
    <div className={styles.pageWrapper}>
      <ProductListLayout
        key={keyword}
        requestFn={requestFn}
        initialParams={{ keyword }}
        headerSlot={headerSlot}
        enableTagFilters
      />
    </div>
  )
}

export default SearchResult
