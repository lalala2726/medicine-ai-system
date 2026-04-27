import requestClient from '@/request/requestClient'

export namespace UserAddressTypes {
  export interface UserAddressVo {
    /** 地址ID */
    id?: string
    /** 收货人姓名 */
    receiverName?: string
    /** 收货人手机号 */
    receiverPhone?: string
    /** 地址(省市区县街道等) */
    address?: string
    /** 详细地址(如小区名、栋号、门牌) */
    detailAddress?: string
    /** 是否默认地址 1是 0否 */
    isDefault?: number
    /** 创建时间 */
    createTime?: string
    /** 更新时间 */
    updateTime?: string
  }

  export interface UserAddressRequest {
    /** 地址ID，更新时必填 */
    id?: string
    /** 收货人姓名 */
    receiverName: string
    /** 收货人手机号 */
    receiverPhone: string
    /** 地址(省市区县街道等) */
    address: string
    /** 详细地址(如小区名、栋号、门牌) */
    detailAddress: string
    /** 是否默认地址 1是 0否 */
    isDefault?: number
  }
}

/**
 * 获取地址列表
 * @returns 地址列表
 */
export const getAddressList = () => {
  return requestClient.get<UserAddressTypes.UserAddressVo[]>('/user/address/list')
}

/**
 * 获取地址详情
 * @param id 地址ID
 * @returns 地址详情
 */
export const getAddressById = (id: string | number) => {
  return requestClient.get<UserAddressTypes.UserAddressVo>(`/user/address/${id}`)
}

/**
 * 新增地址
 * @param request 地址信息
 * @returns 操作结果
 */
export const addAddress = (request: UserAddressTypes.UserAddressRequest) => {
  return requestClient.post('/user/address', request)
}

/**
 * 更新地址
 * @param request 地址信息
 * @returns 操作结果
 */
export const updateAddress = (request: UserAddressTypes.UserAddressRequest) => {
  return requestClient.put('/user/address', request)
}

/**
 * 删除地址
 * @param id 地址ID
 * @returns 操作结果
 */
export const deleteAddress = (id: string | number) => {
  return requestClient.delete(`/user/address/${id}`)
}

/**
 * 设置默认地址
 * @param id 地址ID
 * @returns 操作结果
 */
export const setDefaultAddress = (id: string | number) => {
  return requestClient.put(`/user/address/default/${id}`)
}

/**
 * 获取默认地址
 * @returns 默认地址
 */
export const getDefaultAddress = () => {
  return requestClient.get<UserAddressTypes.UserAddressVo | null>('/user/address/default')
}
