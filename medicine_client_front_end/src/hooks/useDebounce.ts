/**
 * 防抖相关 hooks
 */

import { useState, useEffect, useCallback, useRef } from 'react'

/**
 * 防抖值 hook
 * @param value 需要防抖的值
 * @param delay 防抖延迟时间（毫秒）
 * @returns 防抖后的值
 */
export const useDebounceValue = <T>(value: T, delay: number = 300): T => {
  const [debouncedValue, setDebouncedValue] = useState<T>(value)

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedValue(value)
    }, delay)

    return () => {
      clearTimeout(timer)
    }
  }, [value, delay])

  return debouncedValue
}

/**
 * 防抖函数 hook
 * @param callback 需要防抖的回调函数
 * @param delay 防抖延迟时间（毫秒）
 * @returns 防抖后的函数
 */
export const useDebounceCallback = <T extends (...args: unknown[]) => unknown>(callback: T, delay: number = 300): T => {
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const debouncedCallback = useCallback(
    (...args: Parameters<T>) => {
      if (timerRef.current) {
        clearTimeout(timerRef.current)
      }

      timerRef.current = setTimeout(() => {
        callback(...args)
      }, delay)
    },
    [callback, delay]
  ) as T

  // 清理定时器
  useEffect(() => {
    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current)
      }
    }
  }, [])

  return debouncedCallback
}

export default useDebounceValue
