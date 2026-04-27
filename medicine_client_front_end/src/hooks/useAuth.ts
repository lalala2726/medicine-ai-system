/**
 * 认证相关 hooks
 */

import { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/store/auth'

/**
 * 判断用户是否已登录的 hook
 * @returns isLoggedIn - 是否已登录
 */
export const useIsLoggedIn = (): boolean => {
  const accessToken = useAuthStore(state => state.accessToken)
  return !!accessToken
}

/**
 * 认证相关操作的 hook
 * 提供登录状态检查和需要登录时的跳转功能
 */
export const useAuth = () => {
  const navigate = useNavigate()
  const accessToken = useAuthStore(state => state.accessToken)
  const isLoggedIn = !!accessToken

  /**
   * 检查是否需要登录，如果未登录则跳转到登录页
   * @returns 是否已登录
   */
  const requireLogin = useCallback((): boolean => {
    if (!isLoggedIn) {
      navigate('/login', { replace: true })
      return false
    }
    return true
  }, [isLoggedIn, navigate])

  /**
   * 执行需要登录的操作
   * 如果未登录，跳转到登录页；已登录则执行回调
   * @param callback 需要执行的回调函数
   */
  const withAuth = useCallback(
    <T extends (...args: unknown[]) => unknown>(callback: T) => {
      return ((...args: Parameters<T>) => {
        if (!isLoggedIn) {
          navigate('/login', { replace: true })
          return
        }
        return callback(...args)
      }) as T
    },
    [isLoggedIn, navigate]
  )

  return {
    isLoggedIn,
    requireLogin,
    withAuth
  }
}

export default useAuth
