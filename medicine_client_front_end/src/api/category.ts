import requestClient from '@/request/requestClient'

export namespace CategoryTypes {
  export interface MallCategoryTree {
    /** ID */
    id?: string | number
    /** 父ID */
    parentId?: string | number
    /** 分类名称 */
    name?: string
    /** 分类描述 */
    description?: string
    /** 封面 */
    cover?: string
    /** 排序 */
    sort?: number
    /** 状态 */
    status?: number
    /** 子分类 */
    children?: MallCategoryTree[]
  }
}

/**
 * 获取商品分类树（仅启用分类）
 */
export const getCategoryTree = () => {
  return requestClient.get<CategoryTypes.MallCategoryTree[]>('/mall/category/tree')
}

/**
 * 获取指定父分类下的同级分类（不包含子级）
 */
export const getCategorySiblings = (parentId: number | string) => {
  return requestClient.get<CategoryTypes.MallCategoryTree[]>('/mall/category/siblings', { params: { parentId } })
}
