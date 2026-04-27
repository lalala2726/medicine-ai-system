import requestClient from '@/request/requestClient.ts'
import type { PageRequest, TableDataResult } from '@/types/api.d.ts'

export namespace CouponTypes {
  export interface ActivationCodeRedeemRequest {
    /** 激活码明文 */
    code: string
    /** 登录前置消费的验证码校验凭证 */
    captchaVerificationId: string
  }

  export interface UserCouponListRequest extends PageRequest {
    /** 优惠券状态，仅支持 AVAILABLE、USED、EXPIRED */
    couponStatus?: string
  }

  export interface UserCouponVo {
    /** 用户优惠券ID */
    couponId?: number
    /** 优惠券模板ID */
    templateId?: number
    /** 优惠券名称 */
    couponName?: string
    /** 使用门槛金额 */
    thresholdAmount?: string
    /** 优惠券总金额 */
    totalAmount?: string
    /** 当前可用金额 */
    availableAmount?: string
    /** 当前锁定消耗金额 */
    lockedConsumeAmount?: string
    /** 是否允许继续使用（1-允许，0-不允许） */
    continueUseEnabled?: number
    /** 优惠券状态 */
    couponStatus?: string
    /** 生效时间 */
    effectiveTime?: string
    /** 失效时间 */
    expireTime?: string
  }

  export interface ActivationCodeRedeemVo {
    /** 兑换日志ID */
    redeemLogId?: number
    /** 用户优惠券ID */
    couponId?: number
    /** 优惠券模板ID */
    templateId?: number
    /** 优惠券名称 */
    couponName?: string
    /** 使用门槛金额 */
    thresholdAmount?: string
    /** 优惠券总金额 */
    totalAmount?: string
    /** 当前可用金额 */
    availableAmount?: string
    /** 生效时间 */
    effectiveTime?: string
    /** 失效时间 */
    expireTime?: string
    /** 优惠券状态 */
    couponStatus?: string
  }
}

/**
 * 查询当前用户优惠券列表。
 * @param params 查询参数。
 * @returns 当前用户优惠券分页数据。
 */
export async function getUserCouponList(params: CouponTypes.UserCouponListRequest) {
  return requestClient.get<TableDataResult<CouponTypes.UserCouponVo[]>>('/coupon/list', { params })
}

/**
 * 删除当前用户优惠券。
 * @param couponId 用户优惠券ID。
 * @returns 删除结果。
 */
export async function deleteUserCoupon(couponId: number) {
  return requestClient.delete<void>(`/coupon/${couponId}`)
}

/**
 * 兑换激活码。
 * @param data 激活码兑换参数。
 * @returns 激活码兑换结果。
 */
export async function redeemActivationCode(data: CouponTypes.ActivationCodeRedeemRequest) {
  return requestClient.post<CouponTypes.ActivationCodeRedeemVo>('/coupon/activation-code/redeem', data)
}
