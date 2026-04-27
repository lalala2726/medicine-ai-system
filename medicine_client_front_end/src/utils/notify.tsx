/**
 * 统一的通知提示工具
 * 基于 NutUI 的 Notify 组件
 */

import { Toast } from '@nutui/nutui-react'

/** Toast 文案兜底，避免传入空值时 NutUI 抛出告警。 */
const resolveToastMessage = (message?: string | null) => {
  return typeof message === 'string' && message.trim() ? message : '操作提示'
}

/**
 * 显示文本通知
 * @param message 通知消息
 */
export const showNotify = (message: string) => {
  Toast.show({
    title: resolveToastMessage(message),
    closeOnOverlayClick: true
  })
}

/**
 * 显示成功通知
 * @param message 通知消息
 */
export const showSuccessNotify = (message: string) => {
  Toast.show({
    title: resolveToastMessage(message),
    icon: 'success',
    closeOnOverlayClick: true
  })
}

/**
 * 显示错误通知
 * @param message 通知消息
 */
export const showErrorNotify = (message: string) => {
  Toast.show({
    title: resolveToastMessage(message),
    icon: 'warn',
    closeOnOverlayClick: true
  })
}

/**
 * 显示警告通知
 * @param message 通知消息
 */
export const showWarningNotify = (message: string) => {
  Toast.show({
    title: resolveToastMessage(message),
    icon: 'warn',
    closeOnOverlayClick: true
  })
}

/**
 * 显示主要通知
 * @param message 通知消息
 */
export const showPrimaryNotify = (message: string) => {
  Toast.show({
    title: resolveToastMessage(message),
    icon: 'warn',
    closeOnOverlayClick: true
  })
}
