/**
 * API 通用类型定义
 */

// 支付方式枚举
export type PayTypeEnum = 'WALLET' | 'WAIT_PAY' | 'COUPON'

/**
 * 通用 API 响应接口
 */
export interface ApiResponse<T = any> {
  code: number
  message: string
  timestamp: string
  data: T
}

export interface PageRequest {
  /** 当前页码 */
  pageNum?: number
  /** 每页数量 */
  pageSize?: number
}

/**
 * 分页数据结果
 */
export interface TableDataResult<T = any> {
  /** 总记录数 */
  total: number
  /** 数据列表 */
  rows: T
  /** 当前页码 */
  pageNum: number
  /** 每页大小 */
  pageSize: number
  /** 额外数据 */
  extra?: Record<string, any>
}

/**
 * Token 信息
 */
export interface TokenInfo {
  accessToken: string
  refreshToken: string
}

/**
 * 登录响应类型
 */
export type LoginResponse = ApiResponse<TokenInfo>

/**
 * 刷新 Token 响应类型
 */
export type RefreshTokenResponse = ApiResponse<TokenInfo>

/**
 * 登录请求参数
 */
export interface LoginParams {
  username: string
  password: string
  /** 登录前置消费的验证码校验凭证 */
  captchaVerificationId?: string
}

/**
 * 修改密码请求参数
 */
export interface ChangePasswordParams {
  /** 原密码 */
  oldPassword: string
  /** 新密码 */
  newPassword: string
  /** 登录前置消费的验证码校验凭证 */
  captchaVerificationId: string
}

/**
 * 发送手机号验证码请求参数
 */
export interface SendPhoneVerificationCodeParams {
  /** 新手机号 */
  phoneNumber: string
  /** 登录前置消费的验证码校验凭证 */
  captchaVerificationId: string
}

/**
 * 修改手机号请求参数
 */
export interface ChangePhoneParams {
  /** 新手机号 */
  phoneNumber: string
  /** 手机验证码 */
  verificationCode: string
}

/**
 * 刷新 Token 请求参数
 */
export interface RefreshTokenParams {
  refreshToken: string
}
