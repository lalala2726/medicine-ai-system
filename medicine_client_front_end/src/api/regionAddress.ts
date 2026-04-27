import requestClient from '@/request/requestClient'

export namespace AddressTypes {
  export interface RegionVo {
    /** 行政区划唯一编码, 国家标准12位或15位行政代码 */
    id?: string
    /** 上级行政区划编码, 省级为"0",市级为省id,区县级为市id,街道为区id */
    parentId?: string
    /** 行政区划中文名称, 例如: "北京市"、"朝阳区"、"东花市街道" */
    name?: string
    /** 行政区划层级, 1=省级, 2=市级, 3=区县级, 5=街道/村级 */
    level?: number
  }
}

/**
 * 获取所有省份
 * @returns 省级行政区列表
 */
export const getProvinces = () => {
  return requestClient.get<AddressTypes.RegionVo[]>('/common/regions/provinces')
}

/**
 * 根据省份ID获取城市列表
 * @param provinceId 省份ID
 * @returns 市级行政区列表
 */
export const getCities = (provinceId: string) => {
  return requestClient.get<AddressTypes.RegionVo[]>(`/common/regions/cities/${provinceId}`)
}

/**
 * 根据城市ID获取区县列表
 * @param cityId 城市ID
 * @returns 区县级行政区列表
 */
export const getDistricts = (cityId: string) => {
  return requestClient.get<AddressTypes.RegionVo[]>(`/common/regions/districts/${cityId}`)
}

/**
 * 根据区县ID获取街道列表
 * @param districtId 区县ID
 * @returns 街道列表
 */
export const getStreets = (districtId: string) => {
  return requestClient.get<AddressTypes.RegionVo[]>(`/common/regions/streets/${districtId}`)
}

/**
 * 根据父ID获取子级区域(通用接口)
 * @param parentId 父级ID,"0"表示获取省份
 * @returns 子级行政区列表
 */
export const getChildren = (parentId: string) => {
  return requestClient.get<AddressTypes.RegionVo[]>('/common/regions/children', {
    params: { parentId }
  })
}

/**
 * 根据ID获取单个区域详情
 * @param id 区域ID
 * @returns 区域详情
 */
export const getRegionById = (id: string) => {
  return requestClient.get<AddressTypes.RegionVo>(`/common/regions/${id}`)
}

/**
 * 获取从省到当前区域的完整路径
 * @param id 区域ID
 * @returns 完整路径字符串数组（如: ["北京市", "朝阳区", "建外街道"]）
 */
export const getFullPath = (id: string) => {
  return requestClient.get<string[]>(`/common/regions/${id}/path`)
}

/**
 * 搜索地址（支持按名称、拼音、拼音首字母搜索）
 * @param params 搜索参数（name/pinyin/prefix 三选一）
 * @returns 搜索结果列表
 */
export const searchRegions = (params: {
  /** 名称关键词 */
  name?: string
  /** 拼音关键词 */
  pinyin?: string
  /** 拼音首字母 */
  prefix?: string
}) => {
  return requestClient.get<AddressTypes.RegionVo[]>('/common/regions/search', {
    params
  })
}
