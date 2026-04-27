/**
 * 认证相关 API
 */

import baseRequest from '@/request/baseRequestClient'
import requestClient from '@/request/requestClient'
import type {
  ChangePhoneParams,
  ChangePasswordParams,
  LoginParams,
  SendPhoneVerificationCodeParams,
  RefreshTokenParams,
  LoginResponse,
  RefreshTokenResponse
} from '@/types/api'

/**
 * 用户登录
 * @param params 登录参数
 * @returns 原始响应数据，由调用方处理
 */
export const login = async (params: LoginParams) => {
  return baseRequest.post<LoginResponse>('/auth/login', params)
}

/**
 * 刷新 Token
 * @param params 刷新 Token 参数
 * @returns 原始响应数据，由调用方处理
 */
export const refreshToken = async (params: RefreshTokenParams) => {
  return baseRequest.post<RefreshTokenResponse>('/auth/refresh', params)
}

/**
 * 修改当前登录用户密码
 * @param params 修改密码参数
 * @returns 修改结果
 */
export const changePassword = async (params: ChangePasswordParams): Promise<void> => {
  return requestClient.put<void>('/auth/password', params)
}

/**
 * 发送修改手机号验证码
 * @param params 发送验证码参数
 * @returns 发送结果
 */
export const sendPhoneVerificationCode = async (params: SendPhoneVerificationCodeParams): Promise<void> => {
  return requestClient.post<void>('/auth/phone/verification-code', params)
}

/**
 * 修改当前登录用户手机号
 * @param params 修改手机号参数
 * @returns 修改结果
 */
export const changePhone = async (params: ChangePhoneParams): Promise<void> => {
  return requestClient.put<void>('/auth/phone', params)
}
