import React, { useState, useEffect, useCallback } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { getCategorySiblings, type CategoryTypes } from '@/api/category'
import { search, type ProductTypes } from '@/api/product'
import ProductListLayout, { type ProductListParams, type ProductListResponse } from '@/components/ProductListLayout'
import { sortCategoriesBySortDesc } from '@/utils/category'
import styles from './index.module.less'

const CategoryResult: React.FC = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const state = location.state as { categoryId: number | string; parentId: number | string; name: string } | null

  // 状态
  const [currentId, setCurrentId] = useState<string | number>(state?.categoryId || '')
  const [currentName, setCurrentName] = useState<string>(state?.name || '商品列表')
  const [siblings, setSiblings] = useState<CategoryTypes.MallCategoryTree[]>([])

  // 获取同级分类
  useEffect(() => {
    if (state?.parentId) {
      const fetchSiblings = async () => {
        try {
          const data = await getCategorySiblings(state.parentId)
          setSiblings(Array.isArray(data) ? sortCategoriesBySortDesc(data) : [])
        } catch (error) {
          console.error('获取同级分类失败:', error)
        }
      }
      fetchSiblings()
    }
  }, [state?.parentId])

  // 切换分类
  const handleCategoryClick = (item: CategoryTypes.MallCategoryTree) => {
    if (item.id === currentId) return
    setCurrentId(item.id!)
    setCurrentName(item.name!)
    // ProductListLayout 会监听 initialParams 变化重新请求
  }

  /**
   * 将前端排序参数映射到后端搜索请求参数
   * 前端: sort + order (如 'price' + 'asc')
   * 后端: priceSort/salesSort (如 'asc'/'desc')
   */
  const buildSearchParams = useCallback(
    (params: ProductListParams): ProductTypes.MallProductSearchRequest & { pageNum: number; pageSize: number } => {
      const searchParams: ProductTypes.MallProductSearchRequest & { pageNum: number; pageSize: number } = {
        categoryName: params.categoryName!,
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
      // 如果 sort 是 'default' 或其他值，不传排序参数，使用后端默认排序

      return searchParams
    },
    []
  )

  // 适配搜索 API
  const requestFn = useCallback(
    async (params: ProductListParams): Promise<ProductListResponse> => {
      if (!params.categoryName) {
        return { rows: [], total: 0 }
      }

      const searchParams = buildSearchParams(params)
      const res = await search(searchParams)

      return res as unknown as ProductListResponse
    },
    [buildSearchParams]
  )

  // 头部 Slot
  const headerSlot = (
    <div className={styles.header}>
      <div className={styles.backBtn} onClick={() => navigate(-1)}>
        <ArrowLeft size={20} />
      </div>
      <div className={styles.title}>{currentName}</div>
    </div>
  )

  // 次级头部 (同级分类)
  const subHeaderSlot =
    siblings.length > 0 ? (
      <div className={styles.siblingContainer}>
        <div className={styles.siblingList}>
          {siblings.map(item => (
            <div
              key={item.id}
              className={`${styles.siblingItem} ${item.id === currentId ? styles.active : ''}`}
              onClick={() => handleCategoryClick(item)}
            >
              {item.name}
            </div>
          ))}
        </div>
      </div>
    ) : null

  return (
    <div className={styles.pageWrapper}>
      <ProductListLayout
        requestFn={requestFn}
        initialParams={{ categoryId: currentId, categoryName: currentName }}
        headerSlot={headerSlot}
        subHeaderSlot={subHeaderSlot}
      />
    </div>
  )
}

export default CategoryResult
