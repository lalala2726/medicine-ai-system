import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import * as cartApi from '@/api/shoppingCart'
import type { ShoppingCartTypes } from '@/api/shoppingCart'
import { showSuccessNotify } from '@/utils/notify'

export interface CartItem {
  id: number // 对应后端的 cartId
  productId: number // 商品ID
  name: string
  price: number
  image: string
  spec: string
  quantity: number
  selected?: boolean
  stock?: number // 库存
}

interface AddCartItemPayload {
  productId: number | string
}

interface CartStore {
  items: CartItem[]
  loading: boolean
  cartCount: number // 购物车商品总数量（从后端获取）
  // 从后端获取购物车商品数量
  fetchCartCount: () => Promise<void>
  // 从后端加载购物车列表
  fetchCartList: () => Promise<void>
  // 添加商品到购物车（调用后端接口）
  addItem: (
    _item: AddCartItemPayload,
    _options?: {
      silentSuccess?: boolean
    }
  ) => Promise<void>
  // 删除购物车商品（调用后端接口）
  removeItem: (_id: number) => Promise<boolean>
  // 批量删除购物车商品（调用后端接口）
  removeItems: (_ids: number[]) => Promise<void>
  // 更新商品数量（调用后端接口）
  updateQuantity: (_id: number, _quantity: number) => Promise<boolean>
  // 本地操作：切换选中状态
  toggleSelect: (_id: number) => void
  toggleSelectAll: () => void
  clearCart: () => void
  getTotalPrice: () => number
  getTotalQuantity: () => number
  getSelectedItems: () => CartItem[]
  isAllSelected: () => boolean
}

/**
 * 将后端数据转换为前端 CartItem 格式
 */
const convertToCartItem = (item: ShoppingCartTypes.CartItemVo): CartItem => {
  return {
    id: Number(item.id) || 0,
    productId: Number(item.productId) || 0,
    name: item.productName || '未知商品',
    price: Number(item.price) || 0,
    image: item.productImage || '/images/1.jpg',
    spec: '件', // 后端数据没有规格，使用默认值
    quantity: item.cartNum || 1,
    selected: true,
    stock: item.stock || 0
  }
}

export const useCartStore = create<CartStore>()(
  persist(
    (set, get) => ({
      items: [],
      loading: false,
      cartCount: 0,

      // 从后端获取购物车商品数量
      fetchCartCount: async () => {
        try {
          // requestClient 会自动提取 data，失败会自动提示
          const count = await cartApi.getCartProductCount()
          set({ cartCount: Number(count) || 0 })
        } catch (error) {
          console.error('获取购物车数量失败:', error)
          set({ cartCount: 0 })
        }
      },

      // 从后端加载购物车列表
      fetchCartList: async () => {
        try {
          set({ loading: true })
          // requestClient 会自动提取 data，失败会自动提示
          const data = (await cartApi.getCartList()) as unknown as ShoppingCartTypes.CartItemVo[]

          // 无论后端返回什么数据（包括空数组），都更新本地状态
          const items = data && Array.isArray(data) ? data.map(convertToCartItem) : []
          set({ items, loading: false })
        } catch (error) {
          // 拦截器已经处理了错误提示，这里只需要清空购物车
          set({ items: [], loading: false })
          console.error('获取购物车列表失败:', error)
        }
      },

      // 添加商品到购物车（调用后端接口）
      // 注意：只调用后端接口，不刷新购物车列表（进入购物车页面时才刷新）
      addItem: async (itemToAdd, options) => {
        try {
          set(state => ({ cartCount: state.cartCount + 1 }))
          // requestClient 会自动处理错误，失败会自动提示
          await cartApi.addToCart(itemToAdd.productId)
          if (!options?.silentSuccess) {
            showSuccessNotify('已添加到购物车')
          }
          // 添加成功后异步同步购物车数量，保证和服务端最终一致
          void get().fetchCartCount()
        } catch (error) {
          set(state => ({ cartCount: Math.max(state.cartCount - 1, 0) }))
          // 拦截器已经处理了错误提示
          console.error('添加购物车失败:', error)
        }
      },

      // 删除单个购物车商品（调用后端接口）
      removeItem: async itemId => {
        try {
          set({ loading: true })
          // requestClient 会自动处理错误，失败会自动提示
          await cartApi.removeCartItems([itemId])

          // 删除成功后重新从后端获取购物车列表和数量
          await get().fetchCartList()
          await get().fetchCartCount()
          showSuccessNotify('删除成功')
          return true
        } catch (error) {
          // 拦截器已经处理了错误提示
          set({ loading: false })
          console.error('删除购物车商品失败:', error)
          return false
        }
      },

      // 批量删除购物车商品（调用后端接口）
      removeItems: async itemIds => {
        try {
          set({ loading: true })
          // requestClient 会自动处理错误，失败会自动提示
          await cartApi.removeCartItems(itemIds)

          // 删除成功后重新从后端获取购物车列表和数量
          await get().fetchCartList()
          await get().fetchCartCount()
          showSuccessNotify('删除成功')
        } catch (error) {
          // 拦截器已经处理了错误提示
          set({ loading: false })
          console.error('批量删除购物车商品失败:', error)
        }
      },

      // 更新商品数量（调用后端接口）
      // 注意：先更新本地状态（乐观更新），失败时回滚
      updateQuantity: async (itemId, newQuantity) => {
        // 如果数量无效（undefined、null、NaN），不做任何处理
        if (newQuantity === undefined || newQuantity === null || isNaN(newQuantity)) {
          return false
        }

        // 如果数量小于1，设置为1（最小值）
        if (newQuantity < 1) {
          newQuantity = 1
        }

        // 保存旧数据用于回滚
        const oldItems = get().items
        const oldItem = oldItems.find(item => item.id === itemId)

        if (!oldItem) return false

        // 如果数量没有变化，不调用接口
        if (oldItem.quantity === newQuantity) return true

        try {
          // 先更新本地状态（乐观更新）
          set({
            items: oldItems.map(item => (item.id === itemId ? { ...item, quantity: newQuantity } : item))
          })

          // 调用后端接口
          await cartApi.updateCartQuantity(itemId, newQuantity)
          // 更新成功后刷新购物车数量
          await get().fetchCartCount()
          return true
        } catch (error) {
          // 失败时回滚到旧数据
          set({ items: oldItems })
          console.error('更新购物车商品数量失败:', error)
          return false
        }
      },

      // 本地操作：切换选中状态
      toggleSelect: itemId => {
        set({
          items: get().items.map(item => (item.id === itemId ? { ...item, selected: !item.selected } : item))
        })
      },

      toggleSelectAll: () => {
        const allSelected = get().isAllSelected()
        set({
          items: get().items.map(item => ({ ...item, selected: !allSelected }))
        })
      },

      clearCart: () => {
        set({ items: [] })
      },

      getTotalPrice: () => {
        return get()
          .items.filter(item => item.selected)
          .reduce((total, item) => total + item.price * item.quantity, 0)
      },

      getTotalQuantity: () => {
        return get().items.reduce((total, item) => total + item.quantity, 0)
      },

      getSelectedItems: () => {
        return get().items.filter(item => item.selected)
      },

      isAllSelected: () => {
        const items = get().items
        return items.length > 0 && items.every(item => item.selected)
      }
    }),
    {
      name: 'cart-storage'
    }
  )
)
