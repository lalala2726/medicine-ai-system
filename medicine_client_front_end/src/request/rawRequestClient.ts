import { useAuthStore } from '@/store/auth'
import { logServerError, resolveServerErrorSourceByUrl } from '@/utils/serverError'

/** 原始请求支持的 HTTP 方法类型。 */
type RawRequestMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

/** 原始请求允许的结构化 JSON 数据类型。 */
type RawRequestJsonBody = Record<string, unknown> | Array<unknown>

/** 原始请求配置。 */
export interface RawRequestOptions {
  /** 请求地址。 */
  url: string
  /** 请求方法。 */
  method?: RawRequestMethod
  /** 请求体。 */
  body?: BodyInit | RawRequestJsonBody
  /** 自定义请求头。 */
  headers?: HeadersInit
  /** 中断信号。 */
  signal?: AbortSignal
}

/**
 * 判断当前请求体是否应该按 JSON 序列化。
 *
 * @param body - 原始请求体
 * @returns 是否为结构化 JSON 数据
 */
const isJsonBody = (body: RawRequestOptions['body']): body is RawRequestJsonBody => {
  if (!body) {
    return false
  }

  return (
    typeof body === 'object' &&
    !(body instanceof FormData) &&
    !(body instanceof Blob) &&
    !(body instanceof URLSearchParams) &&
    !(body instanceof ArrayBuffer)
  )
}

/**
 * 发起一个返回原始 `Response` 的请求。
 * 该方法不会经过当前项目的 JSON 业务拦截器，适合音频流、文件流等非标准 JSON 响应。
 *
 * @param options - 原始请求配置
 * @returns 原始 `fetch` 响应对象
 */
export const rawRequest = async ({
  url,
  method = 'GET',
  body,
  headers,
  signal
}: RawRequestOptions): Promise<Response> => {
  /** 当前请求头集合。 */
  const requestHeaders = new Headers(headers)
  /** 当前登录态 access token。 */
  const accessToken = useAuthStore.getState().getAccessToken()
  /** 最终发送给 `fetch` 的请求体。 */
  let requestBody: BodyInit | undefined

  if (accessToken && !requestHeaders.has('Authorization')) {
    requestHeaders.set('Authorization', `Bearer ${accessToken}`)
  }

  if (isJsonBody(body)) {
    if (!requestHeaders.has('Content-Type')) {
      requestHeaders.set('Content-Type', 'application/json')
    }

    requestBody = JSON.stringify(body)
  } else {
    requestBody = body
  }

  try {
    return await fetch(url, {
      method,
      headers: requestHeaders,
      body: requestBody,
      signal
    })
  } catch (error) {
    logServerError(resolveServerErrorSourceByUrl(url), `${method} ${url}`, error)
    throw error
  }
}
