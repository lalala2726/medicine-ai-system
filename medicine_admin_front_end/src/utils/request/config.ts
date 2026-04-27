import type { AxiosRequestConfig } from 'axios';

export const BASE_URL = '/api';
export const AI_BASE_URL = '/ai_api';

/**
 * 与后端约定的标准响应结构。
 */
export interface ApiResponse<TData = any> {
  code: number;
  message: string;
  timestamp: number;
  data: TData;
  result?: TData;
  [key: string]: any;
}

/**
 * 请求可选项。
 * - `skipErrorHandler`: 跳过全局错误提示
 * - `requestType=form`: 自动设置 multipart 头
 */
export interface RequestOptions extends AxiosRequestConfig {
  skipErrorHandler?: boolean;
  requestType?: 'json' | 'form';
  [key: string]: any;
}

export type RequestConfig = AxiosRequestConfig & { skipErrorHandler?: boolean };
export type RequestMethodOptions = Omit<RequestOptions, 'method' | 'data'>;

/**
 * 业务请求客户端：走拦截器并返回解包后的 data。
 */
export interface RequestClient {
  request<TData = any>(url: string, options?: RequestOptions): Promise<TData>;
  get<TData = any>(url: string, options?: RequestMethodOptions): Promise<TData>;
  delete<TData = any>(url: string, options?: RequestMethodOptions): Promise<TData>;
  post<TData = any, TBody = any>(
    url: string,
    data?: TBody,
    options?: RequestMethodOptions,
  ): Promise<TData>;
  put<TData = any, TBody = any>(
    url: string,
    data?: TBody,
    options?: RequestMethodOptions,
  ): Promise<TData>;
  patch<TData = any, TBody = any>(
    url: string,
    data?: TBody,
    options?: RequestMethodOptions,
  ): Promise<TData>;
}

/**
 * 基础请求客户端：不走拦截器，返回完整 code/message/data。
 */
export interface BaseRequestClient {
  request<TData = any>(url: string, options?: RequestOptions): Promise<ApiResponse<TData>>;
  get<TData = any>(url: string, options?: RequestMethodOptions): Promise<ApiResponse<TData>>;
  delete<TData = any>(url: string, options?: RequestMethodOptions): Promise<ApiResponse<TData>>;
  post<TData = any, TBody = any>(
    url: string,
    data?: TBody,
    options?: RequestMethodOptions,
  ): Promise<ApiResponse<TData>>;
  put<TData = any, TBody = any>(
    url: string,
    data?: TBody,
    options?: RequestMethodOptions,
  ): Promise<ApiResponse<TData>>;
  patch<TData = any, TBody = any>(
    url: string,
    data?: TBody,
    options?: RequestMethodOptions,
  ): Promise<ApiResponse<TData>>;
}

/**
 * 判断是否为绝对 HTTP 地址。
 */
export const isAbsoluteHttpUrl = (url: string): boolean => /^https?:\/\//i.test(url);

/**
 * 统一规范化请求地址。
 * - http/https 原样返回
 * - /ai_api 与 /api 原样返回
 * - 其他相对地址自动补 /api 前缀
 */
export const normalizeRequestUrl = (url?: string): string | undefined => {
  if (!url || isAbsoluteHttpUrl(url)) {
    return url;
  }

  if (url.startsWith(BASE_URL) || url.startsWith(AI_BASE_URL)) {
    return url;
  }

  return url.startsWith('/') ? `${BASE_URL}${url}` : `${BASE_URL}/${url}`;
};

/**
 * 构造 axios request config，并在 form 请求时补充请求头。
 */
export const createRequestConfig = (url: string, options: RequestOptions = {}): RequestConfig => {
  const { requestType, ...rest } = options;
  const config: RequestConfig = {
    ...rest,
    url: normalizeRequestUrl(url),
  };

  if (requestType === 'form') {
    config.headers = {
      ...config.headers,
      'Content-Type': 'multipart/form-data',
    };
  }

  return config;
};

/**
 * 判断是否是 AI 服务请求。
 */
export const isAiApiRequest = (url?: string): boolean => {
  if (!url) return false;
  return url.startsWith(AI_BASE_URL) || url.includes('/ai_api');
};

/**
 * 获取请求来源标签（用于控制台诊断）。
 *
 * @param url 请求地址。
 * @returns 请求对应的服务来源标签。
 */
export const getServiceLabel = (url?: string): string => {
  return isAiApiRequest(url) ? 'AI 服务器' : '业务服务器';
};
