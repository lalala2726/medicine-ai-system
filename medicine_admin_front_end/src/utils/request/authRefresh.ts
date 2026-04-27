import { message } from 'antd';
import { authTokenStore, clearAuthState, hydrateAuthTokenStore } from '@/store';
import { type ApiResponse, normalizeRequestUrl } from './config';

/** 业务端约定的访问令牌过期错误码。 */
export const ACCESS_TOKEN_EXPIRED_CODE = 4011;
/** 业务端约定的刷新令牌失效错误码。 */
export const REFRESH_TOKEN_EXPIRED_CODE = 4012;
/** 管理端刷新访问令牌接口地址。 */
const AUTH_REFRESH_URL = '/auth/refresh';

/**
 * 刷新访问令牌接口返回数据。
 */
interface RefreshTokenResponse {
  /** 新访问令牌。 */
  accessToken: string;
  /** 新刷新令牌；为空表示沿用当前刷新令牌。 */
  refreshToken?: string | null;
}

/**
 * 等待刷新令牌完成的请求回调。
 */
interface RefreshWaiter {
  /** 刷新成功回调。 */
  resolve: (accessToken: string) => void;
  /** 刷新失败回调。 */
  reject: (error: unknown) => void;
}

/** 当前是否存在进行中的 token 刷新请求。 */
let isRefreshing = false;
/** 等待同一次 token 刷新完成的请求队列。 */
let refreshWaiters: RefreshWaiter[] = [];
/** token 持久化状态是否已经完成 hydration。 */
let hasHydratedTokenStore = false;

/**
 * 判断错误码是否表示访问令牌过期。
 *
 * @param code 后端返回的业务错误码。
 * @returns 是否为访问令牌过期。
 */
export const isAccessTokenExpiredCode = (code: unknown): boolean => {
  return Number(code) === ACCESS_TOKEN_EXPIRED_CODE;
};

/**
 * 判断错误码是否表示刷新令牌失效。
 *
 * @param code 后端返回的业务错误码。
 * @returns 是否为刷新令牌失效。
 */
export const isRefreshTokenExpiredCode = (code: unknown): boolean => {
  return Number(code) === REFRESH_TOKEN_EXPIRED_CODE;
};

/**
 * 确保持久化 token 已经完成 hydration。
 *
 * @returns hydration 完成后的 Promise。
 */
export const ensureTokenStoreHydrated = async (): Promise<void> => {
  if (hasHydratedTokenStore || typeof window === 'undefined') {
    return;
  }
  await hydrateAuthTokenStore();
  hasHydratedTokenStore = true;
};

/**
 * 统一处理登录态彻底失效。
 */
export const expireAuthSession = (): void => {
  clearAuthState();
  message.error('登录已过期，请重新登录');
  if (typeof window === 'undefined') {
    return;
  }
  const currentPath = window.location.pathname;
  if (currentPath !== '/user/login') {
    window.location.href = '/user/login';
  }
};

/**
 * 通知所有等待中的请求 token 刷新成功。
 *
 * @param accessToken 新访问令牌。
 */
const resolveRefreshWaiters = (accessToken: string): void => {
  for (const waiter of refreshWaiters) {
    waiter.resolve(accessToken);
  }
  refreshWaiters = [];
};

/**
 * 通知所有等待中的请求 token 刷新失败。
 *
 * @param error 刷新失败原因。
 */
const rejectRefreshWaiters = (error: unknown): void => {
  for (const waiter of refreshWaiters) {
    waiter.reject(error);
  }
  refreshWaiters = [];
};

/**
 * 解析刷新 token 接口响应体。
 *
 * @param response Fetch 响应对象。
 * @returns 标准响应结构；响应体不是 JSON 时返回 null。
 */
const parseRefreshResponse = async (
  response: Response,
): Promise<ApiResponse<RefreshTokenResponse> | null> => {
  try {
    return (await response.json()) as ApiResponse<RefreshTokenResponse>;
  } catch {
    return null;
  }
};

/**
 * 请求业务端刷新访问令牌。
 *
 * @param currentRefreshToken 当前刷新令牌。
 * @returns 标准刷新响应。
 */
const requestRefreshToken = async (
  currentRefreshToken: string,
): Promise<ApiResponse<RefreshTokenResponse> | null> => {
  const refreshUrl = normalizeRequestUrl(AUTH_REFRESH_URL) ?? AUTH_REFRESH_URL;
  const response = await fetch(refreshUrl, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ refreshToken: currentRefreshToken }),
  });

  return parseRefreshResponse(response);
};

/**
 * 刷新 access token，并同步到本地 store。
 *
 * @returns 新访问令牌。
 */
export const refreshAccessToken = async (): Promise<string> => {
  await ensureTokenStoreHydrated();

  if (isRefreshing) {
    return new Promise((resolve, reject) => {
      refreshWaiters.push({ resolve, reject });
    });
  }

  isRefreshing = true;

  try {
    const { refreshToken: currentRefreshToken } = authTokenStore.getState();
    if (!currentRefreshToken) {
      throw new Error('Token 刷新失败');
    }

    const resData = await requestRefreshToken(currentRefreshToken);
    if (resData?.code === 200 && resData.data?.accessToken) {
      const { accessToken, refreshToken: nextRefreshToken } = resData.data;
      authTokenStore.getState().setAuthTokens({
        accessToken,
        refreshToken:
          typeof nextRefreshToken === 'string'
            ? nextRefreshToken
            : nextRefreshToken === null
              ? null
              : undefined,
      });

      console.log('Token 刷新成功');
      resolveRefreshWaiters(accessToken);
      return accessToken;
    }

    throw new Error(resData?.message || 'Token 刷新失败');
  } catch (error) {
    console.error('Token 刷新失败:', error);
    rejectRefreshWaiters(error);
    expireAuthSession();
    throw error;
  } finally {
    isRefreshing = false;
  }
};

/**
 * 去掉 Authorization 头中的 Bearer 前缀。
 *
 * @param accessToken 访问令牌。
 * @returns 可解析 JWT payload 的令牌主体。
 */
const stripBearerPrefix = (accessToken: string): string => {
  return accessToken.replace(/^Bearer\s+/i, '');
};

/**
 * 解析 JWT payload 的 base64url 内容。
 *
 * @param payloadSegment JWT 第二段 payload。
 * @returns JSON 字符串。
 */
const decodeJwtPayloadSegment = (payloadSegment: string): string => {
  const base64 = payloadSegment.replace(/-/g, '+').replace(/_/g, '/');
  const paddedBase64 = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=');
  return atob(paddedBase64);
};

/**
 * 判断本地 JWT access token 是否已经过期。
 *
 * @param accessToken 访问令牌。
 * @returns JWT 明确过期时返回 true，无法解析时返回 false。
 */
const isLocalAccessTokenExpired = (accessToken: string): boolean => {
  try {
    const tokenBody = stripBearerPrefix(accessToken);
    const payloadSegment = tokenBody.split('.')[1];
    if (!payloadSegment) {
      return false;
    }
    const payload = JSON.parse(decodeJwtPayloadSegment(payloadSegment)) as { exp?: number };
    if (typeof payload.exp !== 'number') {
      return false;
    }
    return payload.exp * 1000 <= Date.now();
  } catch {
    return false;
  }
};

/**
 * 获取当前可用的访问令牌。
 *
 * @param options.refreshWhenExpired 本地 JWT 已过期时是否先刷新。
 * @returns 当前访问令牌；未登录时返回 null。
 */
export const getActiveAccessToken = async (
  options: { refreshWhenExpired?: boolean } = {},
): Promise<string | null> => {
  await ensureTokenStoreHydrated();
  const accessToken = authTokenStore.getState().accessToken;
  if (!accessToken) {
    return null;
  }
  if (options.refreshWhenExpired && isLocalAccessTokenExpired(accessToken)) {
    return refreshAccessToken();
  }
  return accessToken;
};
