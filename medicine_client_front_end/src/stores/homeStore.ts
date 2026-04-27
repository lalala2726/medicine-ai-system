import { create } from 'zustand'
import type { ProductTypes } from '@/api/product'
import { toProductDrugType } from '@/constants/drugCategory'

// 商品数据类型
export interface Product {
  id: string
  title: string
  src: string
  sale: string
  price: string
  type?: 'OTC绿' | 'OTC红' | 'Rx' | '器械'
}

interface HomeStore {
  // 商品列表
  products: Product[]
  // 是否已加载过数据
  hasLoaded: boolean
  // 设置商品列表
  setProducts: (_products: Product[]) => void
  // 追加商品列表
  appendProducts: (_products: Product[]) => void
  // 标记已加载
  markAsLoaded: () => void
  // 清空数据(可选,用于登出等场景)
  clearProducts: () => void
}

export const useHomeStore = create<HomeStore>(set => ({
  products: [],
  hasLoaded: false,

  setProducts: products => {
    set({
      products,
      hasLoaded: true
    })
  },

  appendProducts: products => {
    set(state => ({
      products: [...state.products, ...products]
    }))
  },

  markAsLoaded: () => {
    set({ hasLoaded: true })
  },

  clearProducts: () => {
    set({
      products: [],
      hasLoaded: false
    })
  }
}))

// 格式化商品数据的工具函数
export const formatProducts = (data: ProductTypes.RecommendListVo[]): Product[] => {
  return data.map((item: ProductTypes.RecommendListVo) => ({
    id: item.productId || '',
    title: item.productName || '',
    src: item.cover || '/images/1.jpg',
    sale: item.sales ? `已售 ${item.sales}` : '已售 0',
    price: item.price ? `¥${item.price}` : '¥0',
    type: toProductDrugType(item.drugCategory)
  }))
}
