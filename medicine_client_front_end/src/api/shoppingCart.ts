import requestClient from '@/request/requestClient'

export namespace ShoppingCartTypes {
  export interface CartItemVo {
    /** 购物车ID */
    id?: string
    /** 商品ID */
    productId?: string
    /** 商品名称 */
    productName?: string
    /** 商品图片 */
    productImage?: string
    /** 商品单价 */
    price?: string
    /** 购买数量 */
    cartNum?: number
    /** 小计金额 */
    subtotal?: string
    /** 库存 */
    stock?: number
  }
}

/**
 * 添加商品到购物车（默认数量为1）
 * 注意：requestClient 会自动提取 response.data.data，所以这里返回的是业务数据
 *
 * @param productId 商品ID
 * @returns 添加结果
 */
export const addToCart = (productId: number | string) => {
  return requestClient.post(`/mall/cart/${productId}`)
}

/**
 * 获取购物车列表
 * 注意：requestClient 会自动提取 response.data.data，所以这里返回的是业务数据数组
 *
 * @returns 购物车商品列表
 */
export const getCartList = () => {
  return requestClient.get<ShoppingCartTypes.CartItemVo[]>('/mall/cart/list')
}

/**
 * 删除购物车商品
 * 注意：requestClient 会自动提取 response.data.data，所以这里返回的是业务数据
 *
 * @param cartIds 购物车ID列表
 * @returns 删除结果
 */
export const removeCartItems = (cartIds: (number | string)[]) => {
  return requestClient.delete('/mall/cart/remove', {
    data: cartIds
  })
}

/**
 * 获取购物车商品数量
 */
export const getCartProductCount = () => {
  return requestClient.get<string>('/mall/cart/count')
}

/**
 * 更新购物车商品数量
 * 注意：requestClient 会自动提取 response.data.data，所以这里返回的是业务数据
 *
 * @param cartId 购物车ID
 * @param quantity 新的数量
 * @returns 更新结果
 */
export const updateCartQuantity = (cartId: number | string, quantity: number) => {
  return requestClient.put('/mall/cart/update', {
    cartId,
    quantity
  })
}
