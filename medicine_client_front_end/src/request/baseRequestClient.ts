/**
 * 基础请求客户端
 * 仅携带 BASE_URL，不添加任何认证参数，不经过响应拦截器处理
 * 返回原始的 axios response 数据，由调用方自行处理业务逻辑
 * 用于登录和刷新 token 等不需要认证的接口
 */

import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { getBaseURL } from '@/utils/env'

/**
 * 创建基础 axios 实例
 */
const baseRequestClient: AxiosInstance = axios.create({
  baseURL: getBaseURL(),
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

/**
 * 基础请求方法封装
 *
 * 注意: 此客户端不经过响应拦截器处理,返回完整的 AxiosResponse
 * 泛型 T 是 response.data 的类型,通常是 ApiResponse<业务数据>
 *
 * 例如: post<ApiResponse<TokenInfo>>('/auth/login', data)
 *       返回 Promise<AxiosResponse<ApiResponse<TokenInfo>>>
 *       需要通过 response.data 访问 ApiResponse,再通过 response.data.data 访问业务数据
 */
class BaseRequest {
  /**
   * GET 请求
   * @param url 请求地址
   * @param config axios 配置
   * @returns Promise<AxiosResponse<T>> 返回完整的 axios 响应对象
   */
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return baseRequestClient.get<T>(url, config)
  }

  /**
   * POST 请求
   * @param url 请求地址
   * @param data 请求数据
   * @param config axios 配置
   * @returns Promise<AxiosResponse<T>> 返回完整的 axios 响应对象
   */
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return baseRequestClient.post<T>(url, data, config)
  }

  /**
   * PUT 请求
   * @param url 请求地址
   * @param data 请求数据
   * @param config axios 配置
   * @returns Promise<AxiosResponse<T>> 返回完整的 axios 响应对象
   */
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return baseRequestClient.put<T>(url, data, config)
  }

  /**
   * DELETE 请求
   * @param url 请求地址
   * @param config axios 配置
   * @returns Promise<AxiosResponse<T>> 返回完整的 axios 响应对象
   */
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return baseRequestClient.delete<T>(url, config)
  }

  /**
   * PATCH 请求
   * @param url 请求地址
   * @param data 请求数据
   * @param config axios 配置
   * @returns Promise<AxiosResponse<T>> 返回完整的 axios 响应对象
   */
  patch<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return baseRequestClient.patch<T>(url, data, config)
  }
}

export default new BaseRequest()
export { baseRequestClient }
