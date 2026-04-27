import { EventStreamContentType, fetchEventSource } from '@microsoft/fetch-event-source'
import { useAuthStore } from '@/store/auth'
import { getBaseURL } from '@/utils/env'
import { logServerError, normalizeServerErrorMessage, resolveServerErrorSourceByUrl } from '@/utils/serverError'

/** 工具调用事件 */
export interface ToolCallEvent {
  eventType: 'tool_call_start' | 'tool_call_end'
  description: string
}

/** SSE 消息体（兼容旧协议） */
export interface SSEMessage {
  role: string
  content: string | null
  type: 'TEXT' | 'EVENT'
  eventData?: ToolCallEvent
  action: unknown
  card: unknown
  isFinish: boolean | null
}

/** SSE 回调集合 */
export interface SSECallbacks<T = SSEMessage> {
  onChunk?: (content: string) => void
  onMessage?: (data: T) => void
  onToolCall?: (event: ToolCallEvent) => void
  onMeta?: (meta: { isFinish: boolean }) => void
  onFinish?: () => void
  onClose?: () => void
  onError?: (error: unknown) => void
}

/** SSE 请求选项 */
export interface SSERequestOptions<T = SSEMessage> extends SSECallbacks<T> {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: Record<string, unknown>
  headers?: Record<string, string>
  parseMessage?: (raw: string) => T | null
  signal?: AbortSignal
  openWhenHidden?: boolean
  onOpen?: (response: Response) => void | Promise<void>
  /**
   * 对 5xx / 网络错误的最大重试次数。
   * 4xx 客户端错误始终不重试。
   * 默认 `0`（不重试），保持向后兼容。
   */
  maxRetries?: number
}

/** SSEClient 全局配置 */
export interface SSEClientConfig {
  baseURL?: string
  getToken?: () => string | null
  defaultHeaders?: Record<string, string>
}

/**
 * 携带 HTTP 状态码的 SSE 请求错误。
 * 消费者可通过 `instanceof SSEHttpError` 和 `status` 字段区分错误类型：
 * - 4xx → 客户端错误（如 404 无 active run），不可重试
 * - 5xx → 服务端临时故障，可做有限重试
 */
export class SSEHttpError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'SSEHttpError'
    this.status = status
  }
}

const resolveErrorMessage = async (response: Response): Promise<string> => {
  try {
    const data = (await response.clone().json()) as { message?: string }
    if (typeof data.message === 'string' && data.message.trim()) {
      return normalizeServerErrorMessage(data.message)
    }
  } catch {
    // Ignore JSON parse errors and fallback below.
  }

  try {
    const text = await response.clone().text()
    if (text.trim()) {
      return normalizeServerErrorMessage(text.trim())
    }
  } catch {
    // Ignore text parse errors and fallback below.
  }

  return normalizeServerErrorMessage(response.statusText, 'SSE 请求失败')
}

class SSEClient {
  private config: Required<SSEClientConfig>

  constructor(config?: SSEClientConfig) {
    this.config = {
      baseURL: config?.baseURL ?? getBaseURL(),
      getToken: config?.getToken ?? (() => useAuthStore.getState().getAccessToken()),
      defaultHeaders: config?.defaultHeaders ?? {}
    }
  }

  setConfig(config: Partial<SSEClientConfig>) {
    if (config.baseURL !== undefined) {
      this.config.baseURL = config.baseURL
    }
    if (config.getToken !== undefined) {
      this.config.getToken = config.getToken
    }
    if (config.defaultHeaders !== undefined) {
      this.config.defaultHeaders = config.defaultHeaders
    }
  }

  private resolveURL(url: string): string {
    if (/^https?:\/\//.test(url)) {
      return url
    }

    const base = this.config.baseURL.replace(/\/+$/, '')
    const path = url.startsWith('/') ? url : `/${url}`
    return `${base}${path}`
  }

  private buildHeaders(extra?: Record<string, string>): Record<string, string> {
    const token = this.config.getToken()

    return {
      Accept: EventStreamContentType,
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...this.config.defaultHeaders,
      ...extra
    }
  }

  request<T = SSEMessage>(options: SSERequestOptions<T>): AbortController {
    const {
      url,
      method = 'POST',
      body,
      headers,
      parseMessage,
      signal: externalSignal,
      onChunk,
      onMessage,
      onToolCall,
      onMeta,
      onFinish,
      onClose,
      onError,
      onOpen,
      openWhenHidden = true,
      maxRetries = 0
    } = options

    const ctrl = new AbortController()
    let isClosed = false
    let hasErrored = false
    /** 当前已重试次数（仅对 5xx / 网络错误累加）。 */
    let retryCount = 0

    const finish = () => {
      if (isClosed) {
        return
      }

      isClosed = true
      onClose?.()
      onFinish?.()
    }

    if (externalSignal) {
      externalSignal.addEventListener('abort', () => ctrl.abort(), { once: true })
    }

    fetchEventSource(this.resolveURL(url), {
      method,
      headers: this.buildHeaders(headers),
      body: body ? JSON.stringify(body) : undefined,
      signal: ctrl.signal,
      openWhenHidden,
      async onopen(response) {
        if (!response.ok) {
          const message = await resolveErrorMessage(response)
          logServerError(resolveServerErrorSourceByUrl(url), `${method} ${url}`, {
            status: response.status,
            message
          })
          throw new SSEHttpError(response.status, message)
        }

        // 连接成功，重置重试计数。
        retryCount = 0

        const contentType = response.headers.get('content-type') ?? ''
        if (!contentType.includes(EventStreamContentType)) {
          throw new Error(`响应不是有效的 SSE 流: ${contentType || 'unknown content-type'}`)
        }

        await onOpen?.(response)
      },
      onmessage(event) {
        const dataStr = event.data?.trim()
        if (!dataStr || dataStr === '[DONE]') {
          return
        }

        try {
          if (parseMessage) {
            const parsed = parseMessage(dataStr)
            if (parsed !== null) {
              onMessage?.(parsed)
            }
            return
          }

          const data = JSON.parse(dataStr) as SSEMessage
          onMessage?.(data as unknown as T)

          if (data.isFinish !== undefined && data.isFinish !== null) {
            onMeta?.({ isFinish: data.isFinish })
          }

          if (data.type === 'EVENT' && data.eventData) {
            onToolCall?.(data.eventData)
            return
          }

          if (data.content) {
            onChunk?.(data.content)
          }
        } catch (error) {
          logServerError(resolveServerErrorSourceByUrl(url), `${method} ${url}`, error)
          onError?.(error)
        }
      },
      onclose() {
        finish()
      },
      onerror(error) {
        // 4xx 客户端错误（如 404 无 active run）不可恢复，立即终止。
        const isFatal = error instanceof SSEHttpError && error.status >= 400 && error.status < 500

        if (isFatal || retryCount >= maxRetries) {
          hasErrored = true
          onError?.(error)
          throw error
        }

        // 5xx / 网络错误：在 maxRetries 限制内做指数退避重试。
        retryCount += 1
        return Math.min(1000 * Math.pow(2, retryCount - 1), 30000)
      }
    }).catch(error => {
      if (ctrl.signal.aborted) {
        finish()
        return
      }

      if (!hasErrored) {
        logServerError(resolveServerErrorSourceByUrl(url), `${method} ${url}`, error)
        onError?.(error)
      }
    })

    return ctrl
  }
}

export const sseClient = new SSEClient()

export function createSSEClient(config?: SSEClientConfig): SSEClient {
  return new SSEClient(config)
}

export function sseRequest<T = SSEMessage>(options: SSERequestOptions<T>): AbortController {
  return sseClient.request(options)
}
