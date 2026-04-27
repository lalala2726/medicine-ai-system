import { authTokenStore } from '@/store';

/**
 * 认证相关工具函数
 */

/**
 * 获取访问令牌
 */
export function getAccessToken(): string | null {
  return authTokenStore.getState().accessToken ?? null;
}

/**
 * 获取刷新令牌
 */
export function getRefreshToken(): string | null {
  return authTokenStore.getState().refreshToken ?? null;
}

/**
 * 设置访问令牌
 */
export function setAccessToken(token: string): void {
  authTokenStore.getState().setAccessToken(token);
}

/**
 * 设置刷新令牌
 */
export function setRefreshToken(token: string): void {
  authTokenStore.getState().setRefreshToken(token);
}

/**
 * 清除所有令牌
 */
export function clearTokens(): void {
  authTokenStore.getState().clearTokens();
}

/**
 * 检查是否已登录
 */
export function isLoggedIn(): boolean {
  return !!getAccessToken();
}

/**
 * 检查令牌是否即将过期（可选）
 * 这里可以根据 JWT 的 exp 字段来判断
 */
export function isTokenExpiringSoon(): boolean {
  const token = getAccessToken();
  if (!token) return true;

  try {
    // 解析 JWT token（不验证签名）
    const payload = JSON.parse(atob(token.split('.')[1]));
    const exp = payload.exp * 1000; // 转换为毫秒
    const now = Date.now();
    const fiveMinutes = 5 * 60 * 1000; // 5分钟

    return exp - now < fiveMinutes;
  } catch (_error) {
    // 如果解析失败，认为令牌无效
    return true;
  }
}

/**
 * 从 JWT token 中提取用户信息
 */
export function getUserInfoFromToken(): any {
  const token = getAccessToken();
  if (!token) return null;

  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload;
  } catch (_error) {
    return null;
  }
}
