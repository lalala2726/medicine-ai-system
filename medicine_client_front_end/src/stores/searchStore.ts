import { create } from 'zustand'
import type { ProductCardData } from '@/components/ProductCard'

interface SearchState {
  // 搜索关键词
  keyword: string
  // 搜索结果
  results: ProductCardData[]
  // 是否已搜索
  hasSearched: boolean
  // 当前页码
  pageNum: number
  // 是否有更多
  hasMore: boolean
  // 滚动位置
  scrollTop: number

  // Actions
  setSearchState: (state: Partial<Omit<SearchState, 'setSearchState' | 'clearSearchState'>>) => void
  clearSearchState: () => void
}

const initialState = {
  keyword: '',
  results: [],
  hasSearched: false,
  pageNum: 1,
  hasMore: true,
  scrollTop: 0
}

export const useSearchStore = create<SearchState>(set => ({
  ...initialState,

  setSearchState: state => set(prev => ({ ...prev, ...state })),

  clearSearchState: () => set(initialState)
}))
