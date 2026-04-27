import requestClient from '@/request/requestClient'
import type { PageRequest, TableDataResult } from '@/types/api'

export namespace ProductTypes {
  export interface MallProductSearchTagFilterOptionVo {
    /** 标签ID */
    tagId?: string
    /** 标签名称 */
    tagName?: string
    /** 当前筛选命中数量 */
    count?: number
  }

  export interface MallProductSearchTagFilterVo {
    /** 标签类型ID */
    typeId?: string
    /** 标签类型编码 */
    typeCode?: string
    /** 标签类型名称 */
    typeName?: string
    /** 当前类型下的筛选项列表 */
    options?: MallProductSearchTagFilterOptionVo[]
  }

  export interface MallProductSearchExtra {
    /** 商品标签筛选分组 */
    tagFilters?: MallProductSearchTagFilterVo[]
  }

  export interface MallProductSearchTableDataResult extends TableDataResult<ProductTypes.MallProductSearchVo[]> {
    /** 搜索结果额外数据 */
    extra?: MallProductSearchExtra
  }

  export interface RecommendListVo {
    /** 商品ID */
    productId?: string
    /** 商品名称 */
    productName?: string
    /** 商品封面 */
    cover?: string
    /** 商品价格 */
    price?: string
    /** 商品销量 */
    sales?: number
    /** 药品分类编码（0-OTC绿，1-Rx，2-OTC红） */
    drugCategory?: number
  }

  export interface MallProductVo {
    /** 商品ID */
    id?: string
    /** 商品名称 */
    name?: string
    /** 商品分类ID列表 */
    categoryIds?: string[]
    /** 商品分类名称列表 */
    categoryNames?: string[]
    /** 商品单位（件、盒、瓶等） */
    unit?: string
    /** 展示价/兜底价：单规格=唯一SKU价，多规格=最小SKU价；结算以SKU价为准 */
    price?: string
    /** 销量 */
    sales?: number
    /** 库存 */
    stock?: number
    /** 商品图片列表 */
    images?: string[]
    /** 药品说明信息 */
    drugDetail?: DrugDetailDto
  }

  export interface DrugDetailDto {
    /** 药品通用名 */
    commonName?: string
    /** 品牌 */
    brand?: string
    /** 成分 */
    composition?: string
    /** 性状 */
    characteristics?: string
    /** 功能主治 */
    efficacy?: string
    /** 用法用量 */
    usageMethod?: string
    /** 不良反应 */
    adverseReactions?: string
    /** 注意事项 */
    precautions?: string
    /** 禁忌 */
    taboo?: string
    /** 包装规格 */
    packaging?: string
    /** 有效期 */
    validityPeriod?: string
    /** 贮藏条件 */
    storageConditions?: string
    /** 产地类型 */
    originType?: string
    /** 批准文号 */
    approvalNumber?: string
    /** 生产单位 */
    productionUnit?: string
    /** 执行标准 */
    executiveStandard?: string
    /** 药品分类编码（0-OTC绿，1-Rx，2-OTC红） */
    drugCategory?: number
    /** 是否外用药 */
    isOutpatientMedicine?: boolean
    /** 药品说明书 */
    instruction?: string
    /** 温馨提示 */
    warmTips?: string
  }

  export interface MallProductSearchVo {
    /** 商品ID */
    productId?: string
    /** 商品名称 */
    productName?: string
    /** 商品封面 */
    cover?: string
    /** 商品价格 */
    price?: string
    /** 销量 */
    sales?: number
    /** 药品分类编码（0-OTC绿，1-Rx，2-OTC红） */
    drugCategory?: number
  }
  /**
   * 商品搜索请求参数
   * 对应后端 MallProductSearchRequest
   */
  export interface MallProductSearchRequest {
    /** 搜索关键字,匹配商品名称、商品分类名称、商品描述、厂商名称、药品通用名、功效/主治 */
    keyword?: string
    /** 商品名称 */
    name?: string
    /** 商品分类名称 */
    categoryName?: string
    /** 商品价格 */
    price?: string
    /** 最低价格 */
    minPrice?: number | string
    /** 最高价格 */
    maxPrice?: number | string
    /** 价格排序方向（asc/desc） */
    priceSort?: 'asc' | 'desc'
    /** 销量排序方向（asc/desc） */
    salesSort?: 'asc' | 'desc'
    /** 商品状态 */
    status?: number
    /** 厂商名称 */
    brand?: string
    /** 药品通用名 */
    commonName?: string
    /** 功效/主治 */
    efficacy?: string
    /** 商品标签ID列表 */
    tagIds?: Array<number | string>
  }
}

/**
 * 商品推荐列表
 */
export const recommend = () => {
  return requestClient.get<ProductTypes.RecommendListVo[]>('/mall/product/recommend')
}

/**
 * 商品详情
 * @param productId 商品ID
 */
export const detail = (productId: string) => {
  return requestClient.get<ProductTypes.MallProductVo>(`/mall/product/${productId}`)
}

/**
 * 商品搜索列表
 * @param params 搜索请求参数
 */
export const search = (params: ProductTypes.MallProductSearchRequest & PageRequest) => {
  return requestClient.get<ProductTypes.MallProductSearchTableDataResult>('/mall/product/search', {
    params,
    paramsSerializer: {
      serialize: searchParams => {
        const urlSearchParams = new URLSearchParams()
        Object.entries(searchParams).forEach(([key, value]) => {
          if (value === undefined || value === null || value === '') {
            return
          }
          if (Array.isArray(value)) {
            value.forEach(item => {
              if (item === undefined || item === null || item === '') {
                return
              }
              urlSearchParams.append(key, String(item))
            })
            return
          }
          urlSearchParams.append(key, String(value))
        })
        return urlSearchParams.toString()
      }
    }
  })
}

/**
 * 查询搜索筛选弹窗的全量商品标签（数据库启用标签）。
 * @returns 按类型分组后的商品标签筛选项
 */
export const listSearchTagFilters = () => {
  return requestClient.get<ProductTypes.MallProductSearchTagFilterVo[]>('/mall/product/search/tag-filters')
}

/**
 * 搜索建议
 * @param keyword 搜索关键字
 */
export const suggest = (keyword: string) => {
  return requestClient.get<string[]>(`/mall/product/search/suggest`, {
    params: {
      keyword
    }
  })
}
