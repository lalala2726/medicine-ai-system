import requestClient from '@/request/requestClient.ts'
import type { PageRequest, TableDataResult } from '@/types/api.d.ts'

export namespace UserTypes {
  export interface CurrentUserInfoVo {
    /** 用户ID */
    id?: string
    /** 昵称 */
    nickname?: string
    /** 头像 */
    avatar?: string
    /** 用户名 */
    username?: string
    /** 角色标识 */
    roles?: string
  }

  export interface UserBriefVo {
    /** 头像 */
    avatarUrl?: string
    /** 昵称 */
    nickName?: string
    /** 手机号 */
    phoneNumber?: string
    /** 账户余额 */
    balance?: string
    /** 优惠券数量 */
    couponCount?: number
    /** 待支付订单数量 */
    payOrderCount?: number
    /** 待发货订单数量 */
    deliverOrderCount?: number
    /** 待收货订单数量 */
    receiveOrderCount?: number
    /** 已完成订单数量 */
    completeOrderCount?: number
    /** 退货/售后订单数量 */
    afterSaleOrderCount?: number
  }

  export interface UserWalletBillVo {
    /** 流水主键ID */
    id?: number
    /** 流水索引 */
    index?: number
    /** 流水标题 */
    title?: string
    /** 流水时间 */
    time?: string
    /** 流水金额 */
    amount?: string
    /** 是否充值 */
    isRecharge?: boolean
  }

  export interface UserWalletBillDetailVo {
    /** 流水主键ID */
    id?: number
    /** 流水编号 */
    flowNo?: string
    /** 流水标题 */
    title?: string
    /** 业务关联单号 */
    bizId?: string
    /** 变动类型：1收入、2支出、3冻结、4解冻 */
    changeType?: number
    /** 变动金额 */
    amount?: string
    /** 变动前余额 */
    beforeBalance?: string
    /** 变动后余额 */
    afterBalance?: string
    /** 备注说明 */
    remark?: string
    /** 流水时间 */
    time?: string
  }

  export interface UserWalletBillRequest extends PageRequest {
    /** 开始时间 */
    startTime?: string
    /** 结束时间 */
    endTime?: string
  }

  export interface UserProfileDto {
    /** 头像 */
    avatar?: string
    /** 昵称 */
    nickname?: string
    /** 真实姓名 */
    realName?: string
    /** 手机号 */
    phoneNumber?: string
    /** 生日 */
    birthday?: string
  }
}

/**
 * 获取当前的用户信息(在登录后调用)
 */
export async function getCurrentUserInfo(): Promise<UserTypes.CurrentUserInfoVo> {
  return requestClient.get<UserTypes.CurrentUserInfoVo>('/user/currentUserInfo')
}

/**
 * 获取用户的简要信息(主要用于个人中心展示)
 */
export async function getUserBrief(): Promise<UserTypes.UserBriefVo> {
  return requestClient.get<UserTypes.UserBriefVo>('/user/brief')
}

/**
 * 获取用户钱包余额
 */
export async function getUserWalletBalance(): Promise<string> {
  return requestClient.get<string>('/user/wallet/balance')
}

/**
 * 获取用户钱包流水列表
 */
export async function getBillList(params: UserTypes.UserWalletBillRequest) {
  return requestClient.get<TableDataResult<UserTypes.UserWalletBillVo[]>>('/user/wallet/bill', { params })
}

/**
 * 获取用户钱包流水详情
 *
 * @param billId - 钱包流水主键ID
 * @returns 钱包流水详情
 */
export async function getBillDetail(billId: string | number): Promise<UserTypes.UserWalletBillDetailVo> {
  return requestClient.get<UserTypes.UserWalletBillDetailVo>(`/user/wallet/bill/${billId}`)
}

/**
 * 获取用户个人资料
 */
export async function getUserProfile(): Promise<UserTypes.UserProfileDto> {
  return requestClient.get<UserTypes.UserProfileDto>('/user/profile')
}

/**
 * 修改用户个人资料
 */
export async function updateUserProfile(data: UserTypes.UserProfileDto) {
  return requestClient.put<void>('/user/profile', data)
}
