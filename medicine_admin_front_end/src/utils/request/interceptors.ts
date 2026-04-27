import type { AxiosError, AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import { message } from 'antd';
import { authTokenStore } from '@/store';
import {
  type ApiResponse,
  type RequestConfig,
  getServiceLabel,
  normalizeRequestUrl,
} from './config';
import {
  ensureTokenStoreHydrated,
  expireAuthSession,
  isAccessTokenExpiredCode,
  refreshAccessToken,
} from './authRefresh';

const HTTP_STATUS_MSG: Record<number, string> = {
  400: '请求参数错误（400）',
  401: '未授权，请重新登录（401）',
  403: '无权限访问该资源（403）',
  404: '请求的资源不存在（404）',
  405: '请求方法不被允许（405）',
  408: '请求超时（408）',
  429: '请求过于频繁，请稍后再试（429）',
  500: '内部服务器错误（500）',
  502: '网关错误（502）',
  503: '服务不可用（503）',
  504: '网关超时（504）',
};

/**
 * 可被 token 刷新重试标记的请求配置。
 */
type RetriableRequestConfig = InternalAxiosRequestConfig & {
  /** 是否已经因为访问令牌过期重试过。 */
  __tokenRefreshRetried?: boolean;
};

/**
 * 记录请求错误来源，便于控制台区分业务端和 AI 端。
 *
 * @param serviceLabel 服务来源标签。
 * @param detail 错误诊断详情。
 */
const logRequestErrorSource = (serviceLabel: string, detail: Record<string, unknown>): void => {
  console.error(`[请求错误][${serviceLabel}]`, detail);
};

/**
 * 解析展示给用户的业务错误文案。
 *
 * @param data 标准响应结构。
 * @returns 后端业务错误文案或错误码。
 */
const resolveBusinessErrorMessage = (data: ApiResponse): string => {
  return data.message || `错误码 ${data.code}`;
};

/**
 * 解析 HTTP 错误展示文案，优先展示后端返回的业务说明。
 *
 * @param statusMsg HTTP 状态文案。
 * @param errorDetail 后端返回的错误详情。
 * @returns 展示给用户的错误文案。
 */
const resolveHttpErrorMessage = (statusMsg: string, errorDetail: string): string => {
  return errorDetail || statusMsg;
};

/**
 * 刷新 token 后重新发起原始 axios 请求。
 *
 * @param interceptedAxios 已挂载拦截器的 axios 实例。
 * @param config 原始请求配置。
 * @returns 重试后的 axios 响应。
 */
const retryRequestAfterTokenRefresh = async (
  interceptedAxios: AxiosInstance,
  config?: InternalAxiosRequestConfig,
): Promise<AxiosResponse> => {
  const requestConfig = config as RetriableRequestConfig | undefined;
  if (!requestConfig || requestConfig.__tokenRefreshRetried) {
    expireAuthSession();
    return Promise.reject(new Error('Token 刷新后请求重试失败'));
  }

  const accessToken = await refreshAccessToken();
  requestConfig.__tokenRefreshRetried = true;
  requestConfig.headers = requestConfig.headers || {};
  requestConfig.headers.Authorization = accessToken;
  return interceptedAxios(requestConfig);
};

/**
 * 挂载 requestClient 使用的请求/响应拦截器。
 */
export const applyInterceptors = (interceptedAxios: AxiosInstance): void => {
  interceptedAxios.interceptors.request.use(
    async (config: InternalAxiosRequestConfig) => {
      if (config.url) {
        config.url = normalizeRequestUrl(config.url) ?? config.url;
      }

      const url = config.url || '';
      if (url.includes('/auth/login')) {
        console.log('登录请求，不添加 token');
        return config;
      }

      await ensureTokenStoreHydrated();
      const { accessToken } = authTokenStore.getState();
      if (accessToken) {
        config.headers = config.headers || {};
        config.headers.Authorization = accessToken;
        console.log('请求拦截器添加 token:', config.url);
      } else {
        console.log('请求拦截器未找到 token:', config.url);
      }
      return config;
    },
    (error) => Promise.reject(error),
  );

  interceptedAxios.interceptors.response.use(
    (response: AxiosResponse) => {
      const data = response.data as ApiResponse;
      const requestUrl = response.config?.url;
      const serviceLabel = getServiceLabel(requestUrl);

      if (data && data.code === 200) {
        response.data = data.data ?? data.result ?? data;
        return response;
      }

      if (data?.code && data.code !== 200) {
        logRequestErrorSource(serviceLabel, {
          url: requestUrl,
          code: data.code,
          message: data.message,
          response: data,
        });

        switch (data.code) {
          case 4011:
            return retryRequestAfterTokenRefresh(interceptedAxios, response.config);
          case 4012:
            expireAuthSession();
            return Promise.reject(data);
          case 401:
            expireAuthSession();
            return Promise.reject(data);
          case 403:
            message.error(data.message || '没有权限访问该资源');
            break;
          case 404:
            message.error(data.message || '请求的资源不存在');
            break;
          case 500:
            message.error(data.message || '内部服务器错误');
            return Promise.reject(data);
          default:
            message.error(resolveBusinessErrorMessage(data));
        }
        return Promise.reject(data);
      }

      return response;
    },
    (error: AxiosError) => {
      const response = error.response;
      const requestUrl = error.config?.url;
      const serviceLabel = getServiceLabel(requestUrl);

      const errorPayload = response?.data as ApiResponse | undefined;
      if (isAccessTokenExpiredCode(errorPayload?.code)) {
        return retryRequestAfterTokenRefresh(
          interceptedAxios,
          error.config as InternalAxiosRequestConfig | undefined,
        );
      }

      if ((error.config as RequestConfig | undefined)?.skipErrorHandler) throw error;

      if (response) {
        const status = response.status;
        const statusMsg = HTTP_STATUS_MSG[status] ?? `服务器错误（${status}）`;
        const errorDetail: string =
          (response.data as any)?.message ?? (response.data as any)?.msg ?? error.message ?? '';
        const displayMessage = resolveHttpErrorMessage(statusMsg, errorDetail);

        logRequestErrorSource(serviceLabel, {
          url: requestUrl,
          status,
          statusText: response.statusText,
          message: errorDetail,
          response: response.data,
        });

        if (status >= 500) {
          message.error({
            content: displayMessage,
            duration: 5,
          });
        } else if (status === 401) {
          expireAuthSession();
        } else if (status === 403) {
          message.error({
            content: displayMessage,
            duration: 4,
          });
        } else if (status >= 400) {
          message.error({
            content: displayMessage,
            duration: 4,
          });
        }
      } else if (error.request) {
        logRequestErrorSource(serviceLabel, {
          url: requestUrl,
          message: error.message,
          request: error.request,
        });
        message.error({
          content: '无法连接服务器，请检查网络或联系管理员',
          duration: 5,
        });
      } else {
        logRequestErrorSource(serviceLabel, {
          url: requestUrl,
          message: error.message,
        });
        message.error({
          content: `请求发起失败：${error?.message ?? '未知错误'}`,
          duration: 4,
        });
      }

      throw error;
    },
  );
};
