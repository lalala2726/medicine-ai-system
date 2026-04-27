import axios from 'axios';
import { applyInterceptors } from './interceptors';
import {
  type ApiResponse,
  type BaseRequestClient,
  type RequestClient,
  type RequestConfig,
  type RequestMethodOptions,
  type RequestOptions,
  createRequestConfig,
} from './config';

export type {
  ApiResponse,
  BaseRequestClient,
  RequestClient,
  RequestConfig,
  RequestMethodOptions,
  RequestOptions,
} from './config';

const interceptedAxios = axios.create({
  timeout: 30000,
});

const plainAxios = axios.create({
  timeout: 30000,
});

applyInterceptors(interceptedAxios);

/**
 * 执行带拦截器请求，返回解包后的业务 data。
 */
const executeInterceptedRequest = <TData = any>(config: RequestConfig): Promise<TData> => {
  return interceptedAxios(config).then((res) => res.data as TData);
};

/**
 * 执行基础请求，返回完整响应结构。
 */
const executePlainRequest = <TData = any>(config: RequestConfig): Promise<ApiResponse<TData>> => {
  return plainAxios(config).then((res) => res.data as ApiResponse<TData>);
};

/**
 * 业务请求客户端：会自动注入 token、处理错误并解包 data。
 */
export const requestClient: RequestClient = {
  request<TData = any>(url: string, options: RequestOptions = {}) {
    const { method = 'GET', ...rest } = options;
    return executeInterceptedRequest<TData>(
      createRequestConfig(url, {
        ...rest,
        method,
      }),
    );
  },
  get<TData = any>(url: string, options: RequestMethodOptions = {}) {
    return executeInterceptedRequest<TData>(
      createRequestConfig(url, {
        ...options,
        method: 'GET',
      }),
    );
  },
  delete<TData = any>(url: string, options: RequestMethodOptions = {}) {
    return executeInterceptedRequest<TData>(
      createRequestConfig(url, {
        ...options,
        method: 'DELETE',
      }),
    );
  },
  post<TData = any, TBody = any>(url: string, data?: TBody, options: RequestMethodOptions = {}) {
    return executeInterceptedRequest<TData>(
      createRequestConfig(url, {
        ...options,
        method: 'POST',
        data,
      }),
    );
  },
  put<TData = any, TBody = any>(url: string, data?: TBody, options: RequestMethodOptions = {}) {
    return executeInterceptedRequest<TData>(
      createRequestConfig(url, {
        ...options,
        method: 'PUT',
        data,
      }),
    );
  },
  patch<TData = any, TBody = any>(
    url: string,
    data?: TBody,
    options: RequestMethodOptions = {},
  ) {
    return executeInterceptedRequest<TData>(
      createRequestConfig(url, {
        ...options,
        method: 'PATCH',
        data,
      }),
    );
  },
};

/**
 * 基础请求客户端：不走拦截器，调用方自行处理 code/message/data。
 */
export const baseRequestClient: BaseRequestClient = {
  request<TData = any>(url: string, options: RequestOptions = {}) {
    const { method = 'GET', ...rest } = options;
    return executePlainRequest<TData>(
      createRequestConfig(url, {
        ...rest,
        method,
      }),
    );
  },
  get<TData = any>(url: string, options: RequestMethodOptions = {}) {
    return executePlainRequest<TData>(
      createRequestConfig(url, {
        ...options,
        method: 'GET',
      }),
    );
  },
  delete<TData = any>(url: string, options: RequestMethodOptions = {}) {
    return executePlainRequest<TData>(
      createRequestConfig(url, {
        ...options,
        method: 'DELETE',
      }),
    );
  },
  post<TData = any, TBody = any>(url: string, data?: TBody, options: RequestMethodOptions = {}) {
    return executePlainRequest<TData>(
      createRequestConfig(url, {
        ...options,
        method: 'POST',
        data,
      }),
    );
  },
  put<TData = any, TBody = any>(url: string, data?: TBody, options: RequestMethodOptions = {}) {
    return executePlainRequest<TData>(
      createRequestConfig(url, {
        ...options,
        method: 'PUT',
        data,
      }),
    );
  },
  patch<TData = any, TBody = any>(
    url: string,
    data?: TBody,
    options: RequestMethodOptions = {},
  ) {
    return executePlainRequest<TData>(
      createRequestConfig(url, {
        ...options,
        method: 'PATCH',
        data,
      }),
    );
  },
};
