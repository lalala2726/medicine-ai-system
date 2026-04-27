import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'
import { getCategoryTree, type CategoryTypes } from '@/api/category'
import { sortCategoriesBySortDesc } from '@/utils/category'

/**
 * 分类树缓存有效期，单位毫秒。
 */
const CATEGORY_CACHE_TTL = 60 * 60 * 1000

interface CategoryStore {
  categories: CategoryTypes.MallCategoryTree[]
  loading: boolean
  hasLoaded: boolean
  lastFetchedAt: number | null
  fetchCategoryTree: (_force?: boolean) => Promise<CategoryTypes.MallCategoryTree[]>
}

let pendingRequest: Promise<CategoryTypes.MallCategoryTree[]> | null = null

/**
 * 判断分类缓存是否仍然有效。
 *
 * @param lastFetchedAt 最近一次拉取时间
 * @returns 缓存是否仍在有效期内
 */
const isCacheFresh = (lastFetchedAt: number | null) => {
  if (!lastFetchedAt) return false
  return Date.now() - lastFetchedAt < CATEGORY_CACHE_TTL
}

export const useCategoryStore = create<CategoryStore>()(
  persist(
    (set, get) => {
      /**
       * 发起分类树请求并同步缓存。
       *
       * @param keepCurrentData 是否保留当前分类展示态
       * @returns 最新分类树
       */
      const requestCategoryTree = (keepCurrentData: boolean): Promise<CategoryTypes.MallCategoryTree[]> => {
        if (!keepCurrentData) {
          set({ loading: true })
        }

        pendingRequest = getCategoryTree()
          .then(data => {
            const nextCategories = Array.isArray(data) ? sortCategoriesBySortDesc(data) : []

            set({
              categories: nextCategories,
              loading: false,
              hasLoaded: true,
              lastFetchedAt: Date.now()
            })

            return nextCategories
          })
          .catch(error => {
            console.error('获取分类树失败:', error)
            set(state => ({
              loading: false,
              hasLoaded: state.hasLoaded || state.categories.length > 0
            }))
            throw error
          })
          .finally(() => {
            pendingRequest = null
          })

        return pendingRequest
      }

      return {
        categories: [],
        loading: false,
        hasLoaded: false,
        lastFetchedAt: null,

        fetchCategoryTree: async force => {
          const { categories, hasLoaded, lastFetchedAt } = get()
          const hasCache = categories.length > 0

          if (!force && isCacheFresh(lastFetchedAt) && (hasCache || hasLoaded)) {
            if (!pendingRequest) {
              void requestCategoryTree(true)
            }

            return categories
          }

          if (pendingRequest) {
            return pendingRequest
          }

          return requestCategoryTree(hasCache)
        }
      }
    },
    {
      name: 'category-tree-cache',
      storage: createJSONStorage(() => localStorage),
      merge: (persistedState, currentState) => {
        const normalizedState = persistedState as Partial<CategoryStore> | undefined

        return {
          ...currentState,
          ...normalizedState,
          categories: Array.isArray(normalizedState?.categories)
            ? sortCategoriesBySortDesc(normalizedState.categories)
            : currentState.categories
        }
      },
      partialize: state => ({
        categories: state.categories,
        hasLoaded: state.hasLoaded,
        lastFetchedAt: state.lastFetchedAt
      })
    }
  )
)
