/**
 * 商品浏览历史 API
 */

import requestClient from '@/request/requestClient'
import type { PageRequest, TableDataResult } from '@/types/api'

export namespace ViewHistoryTypes {
  export interface ViewHistoryVo {
    /** 商品ID */
    productId?: string
    /** 商品名称 */
    productName?: string
    /** 商品封面图 */
    coverImage?: string
    /** 商品价格 */
    price?: string
    /** 商品销量 */
    sales?: number
    /** 药品分类编码（0-OTC绿，1-Rx，2-OTC红） */
    drugCategory?: number
    /** 累计浏览次数 */
    viewCount?: number
    /** 首次浏览时间 */
    firstViewTime?: string
    /** 最后浏览时间 */
    lastViewTime?: string
  }

  export type ViewHistoryListRequest = PageRequest
}

/**
 * 分页查询当前用户的商品浏览历史
 * @param request 分页参数
 * @returns 浏览历史分页数据
 */
export const getViewHistoryList = (request: ViewHistoryTypes.ViewHistoryListRequest) => {
  return requestClient.get<TableDataResult<ViewHistoryTypes.ViewHistoryVo[]>>('/mall/product/history/list', {
    params: request
  })
}

/**
 * 记录一次商品浏览
 * @param productId 商品ID
 * @returns 操作结果
 */
export const recordViewHistory = (productId: string | number) => {
  return requestClient.post(`/mall/product/history/${productId}`)
}

/**
 * 删除指定商品的浏览记录
 * @param productId 商品ID
 * @returns 操作结果
 */
export const deleteViewHistory = (productId: string | number) => {
  return requestClient.delete(`/mall/product/history/${productId}`)
}

/**
 * 清空当前用户的浏览记录
 * @returns 操作结果
 */
export const clearAllViewHistory = () => {
  return requestClient.delete('/mall/product/history/clear')
}
