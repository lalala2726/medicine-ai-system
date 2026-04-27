/**
 * 标准请求客户端
 * 包含请求拦截器和响应拦截器
 * 自动处理 token 认证和刷新
 */

import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig
} from 'axios'
import { getBaseURL } from '@/utils/env'
import { useAuthStore } from '@/store/auth'
import type { ApiResponse } from '@/types/api'
import { showErrorNotify } from '@/utils/notify'
import { logServerError, normalizeServerErrorMessage, resolveServerErrorSourceByUrl } from '@/utils/serverError'

/**
 * 业务成功状态码
 */
const SUCCESS_CODE = 200

/**
 * Token 过期状态码
 */
const TOKEN_EXPIRED_CODE = 4011

/**
 * 从 axios 错误对象中提取优先展示的后端错误文案。
 *
 * @param error - axios 响应拦截器捕获到的错误对象
 * @returns 标准化后的错误展示文案
 */
const resolveAxiosErrorMessage = (error: any): string => {
  /** 响应体中的错误提示。 */
  const responseMessage = error?.response?.data?.message

  if (typeof responseMessage === 'string' && responseMessage.trim()) {
    return normalizeServerErrorMessage(responseMessage)
  }

  return normalizeServerErrorMessage(error?.message, '请求失败')
}

/**
 * Token 刷新状态
 */
let isRefreshing = false
let failedQueue: Array<{
  resolve: (_value?: any) => void
  reject: (_reason?: any) => void
}> = []

/**
 * 处理队列中的请求
 */
const processQueue = (error: any = null) => {
  failedQueue.forEach(promise => {
    if (error) {
      promise.reject(error)
    } else {
      promise.resolve(undefined)
    }
  })
  failedQueue = []
}

/**
 * 刷新 token
 */
const refreshAccessToken = async (): Promise<boolean> => {
  try {
    const refreshToken = useAuthStore.getState().getRefreshToken()

    if (!refreshToken) {
      return false
    }

    // 使用 baseRequestClient 刷新 token
    const baseRequest = await import('./baseRequestClient')
    const response = await baseRequest.default.post('/auth/refresh', { refreshToken })

    const { code, data, message } = response.data

    // 刷新成功
    if (code === SUCCESS_CODE && data) {
      useAuthStore.getState().setTokens(data.accessToken, data.refreshToken)
      return true
    }

    // 刷新失败
    console.error('刷新 token 失败:', message)
    showErrorNotify('登录已过期，请重新登录')

    // 清空本地 token 并跳转登录页
    useAuthStore.getState().clearTokens()
    setTimeout(() => {
      window.location.href = '/login'
    }, 1000)

    return false
  } catch (error: any) {
    console.error('刷新 token 请求失败:', error)

    // 网络错误或其他异常
    showErrorNotify('网络异常，请重新登录')

    // 清空本地 token 并跳转登录页
    useAuthStore.getState().clearTokens()
    setTimeout(() => {
      window.location.href = '/login'
    }, 1000)

    return false
  }
}

/**
 * 创建标准 axios 实例
 */
const requestClient: AxiosInstance = axios.create({
  baseURL: getBaseURL(),
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

/**
 * 请求拦截器
 * 自动添加 Authorization 头
 */
requestClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const accessToken = useAuthStore.getState().getAccessToken()

    if (accessToken && config.headers) {
      config.headers.Authorization = `Bearer ${accessToken}`
    }

    return config
  },
  error => {
    console.error('请求拦截器错误:', error)
    return Promise.reject(error)
  }
)

/**
 * 响应拦截器
 * 统一处理响应数据和错误
 */
requestClient.interceptors.response.use(
  async (response: AxiosResponse<ApiResponse>) => {
    const { code, data, message } = response.data

    // 业务请求成功，直接返回 data
    if (code === SUCCESS_CODE) {
      return data as any // 返回业务数据,类型由调用方的泛型决定
    }

    // Token 过期，尝试刷新
    if (code === TOKEN_EXPIRED_CODE) {
      const originalRequest = response.config as InternalAxiosRequestConfig & {
        _retry?: boolean
      }

      // 避免死循环
      if (originalRequest._retry) {
        showErrorNotify('认证失败，请重新登录')
        useAuthStore.getState().clearTokens()
        setTimeout(() => {
          window.location.href = '/login'
        }, 1000)
        return Promise.reject(new Error('Token 刷新失败'))
      }

      // 如果正在刷新 token，将请求加入队列
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        })
          .then(() => {
            // Token 刷新成功后重新发起请求
            const accessToken = useAuthStore.getState().getAccessToken()
            if (originalRequest.headers && accessToken) {
              originalRequest.headers.Authorization = `Bearer ${accessToken}`
            }
            return requestClient(originalRequest)
          })
          .catch(err => {
            return Promise.reject(err)
          })
      }

      // 标记正在刷新
      originalRequest._retry = true
      isRefreshing = true

      try {
        const refreshSuccess = await refreshAccessToken()

        if (refreshSuccess) {
          // 刷新成功，处理队列中的请求
          processQueue()
          isRefreshing = false

          // 重新发起原始请求
          const accessToken = useAuthStore.getState().getAccessToken()
          if (originalRequest.headers && accessToken) {
            originalRequest.headers.Authorization = `Bearer ${accessToken}`
          }
          return requestClient(originalRequest)
        } else {
          // 刷新失败
          processQueue(new Error('Token 刷新失败'))
          isRefreshing = false
          return Promise.reject(new Error('Token 刷新失败'))
        }
      } catch (error) {
        processQueue(error)
        isRefreshing = false
        return Promise.reject(error)
      }
    }

    // 其他业务错误，使用 Notify 提示
    const source = resolveServerErrorSourceByUrl(response.config.url)
    const normalizedMessage = normalizeServerErrorMessage(message, '请求失败')
    logServerError(
      source,
      `${response.config.method?.toUpperCase() || 'REQUEST'} ${response.config.url || ''}`,
      response.data
    )
    showErrorNotify(normalizedMessage)
    return Promise.reject(new Error(normalizedMessage))
  },
  error => {
    // 网络错误或其他错误
    const source = resolveServerErrorSourceByUrl(error?.config?.url)
    logServerError(source, `${error?.config?.method?.toUpperCase() || 'REQUEST'} ${error?.config?.url || ''}`, error)

    if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
      showErrorNotify('请求超时，请稍后重试')
    } else if (!error.response) {
      showErrorNotify('网络连接失败，请检查网络')
    } else {
      showErrorNotify(resolveAxiosErrorMessage(error))
    }

    return Promise.reject(error)
  }
)

/**
 * 标准请求方法封装
 *
 * 注意: 由于响应拦截器会自动提取 response.data.data,
 * 所以这里的泛型 T 是业务数据的类型,而不是 ApiResponse<T>
 *
 * 例如: get<User>('/user') 返回 Promise<User>,而不是 Promise<ApiResponse<User>>
 */
class Request {
  /**
   * GET 请求
   * @param url 请求地址
   * @param config axios 配置
   * @returns Promise<T> 返回业务数据(已通过拦截器提取)
   */
  async get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return requestClient.get(url, config) as any
  }

  /**
   * POST 请求
   * @param url 请求地址
   * @param data 请求数据
   * @param config axios 配置
   * @returns Promise<T> 返回业务数据(已通过拦截器提取)
   */
  async post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return requestClient.post(url, data, config) as any
  }

  /**
   * PUT 请求
   * @param url 请求地址
   * @param data 请求数据
   * @param config axios 配置
   * @returns Promise<T> 返回业务数据(已通过拦截器提取)
   */
  async put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return requestClient.put(url, data, config) as any
  }

  /**
   * DELETE 请求
   * @param url 请求地址
   * @param config axios 配置
   * @returns Promise<T> 返回业务数据(已通过拦截器提取)
   */
  async delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return requestClient.delete(url, config) as any
  }

  /**
   * PATCH 请求
   * @param url 请求地址
   * @param data 请求数据
   * @param config axios 配置
   * @returns Promise<T> 返回业务数据(已通过拦截器提取)
   */
  async patch<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return requestClient.patch(url, data, config) as any
  }
}

export default new Request()
export { requestClient }
