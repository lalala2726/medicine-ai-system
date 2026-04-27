/**
 * 认证状态管理
 * 使用 zustand 管理全局认证状态
 */

import { create } from 'zustand'

/**
 * Token 存储的 localStorage key
 */
const ACCESS_TOKEN_KEY = 'access_token'
const REFRESH_TOKEN_KEY = 'refresh_token'

/**
 * 认证状态接口
 */
interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  setTokens: (_accessToken: string, _refreshToken: string) => void
  clearTokens: () => void
  getAccessToken: () => string | null
  getRefreshToken: () => string | null
  isAuthenticated: () => boolean
}

/**
 * 从 localStorage 获取 token
 */
const getTokenFromStorage = (key: string): string | null => {
  try {
    return localStorage.getItem(key)
  } catch (err) {
    console.error(`Failed to get ${key} from localStorage:`, err)
    return null
  }
}

/**
 * 保存 token 到 localStorage
 */
const setTokenToStorage = (key: string, value: string): void => {
  try {
    localStorage.setItem(key, value)
  } catch (err) {
    console.error(`Failed to set ${key} to localStorage:`, err)
  }
}

/**
 * 从 localStorage 删除 token
 */
const removeTokenFromStorage = (key: string): void => {
  try {
    localStorage.removeItem(key)
  } catch (err) {
    console.error(`Failed to remove ${key} from localStorage:`, err)
  }
}

/**
 * 创建认证 store
 */
export const useAuthStore = create<AuthState>((set, get) => ({
  // 初始化时从 localStorage 恢复 token
  accessToken: getTokenFromStorage(ACCESS_TOKEN_KEY),
  refreshToken: getTokenFromStorage(REFRESH_TOKEN_KEY),

  /**
   * 设置 tokens
   * 同时保存到 localStorage
   */
  setTokens: (accessToken, refreshToken) => {
    set({ accessToken, refreshToken })
    setTokenToStorage(ACCESS_TOKEN_KEY, accessToken)
    setTokenToStorage(REFRESH_TOKEN_KEY, refreshToken)
  },

  /**
   * 清空 tokens
   * 同时清除 localStorage 中的数据
   */
  clearTokens: () => {
    set({ accessToken: null, refreshToken: null })
    removeTokenFromStorage(ACCESS_TOKEN_KEY)
    removeTokenFromStorage(REFRESH_TOKEN_KEY)
  },

  /**
   * 获取 access token
   */
  getAccessToken: () => {
    return get().accessToken
  },

  /**
   * 获取 refresh token
   */
  getRefreshToken: () => {
    return get().refreshToken
  },

  /**
   * 检查是否已认证
   */
  isAuthenticated: () => {
    return !!get().accessToken
  }
}))
