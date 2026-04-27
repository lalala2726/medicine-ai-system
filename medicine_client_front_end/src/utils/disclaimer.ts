/**
 * localStorage 存储键名。
 */
const DISCLAIMER_AGREED_KEY = 'disclaimer_agreed'

/**
 * 检查用户是否已同意免责声明。
 *
 * @returns 是否已同意
 */
export function isDisclaimerAgreed(): boolean {
  try {
    return localStorage.getItem(DISCLAIMER_AGREED_KEY) === '1'
  } catch {
    return false
  }
}

/**
 * 将同意状态持久化到 localStorage。
 *
 * @returns 无返回值
 */
export function markDisclaimerAgreed(): void {
  try {
    localStorage.setItem(DISCLAIMER_AGREED_KEY, '1')
  } catch {
    // 静默失败
  }
}
