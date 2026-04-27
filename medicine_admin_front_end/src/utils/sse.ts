import { EventStreamContentType, fetchEventSource } from '@microsoft/fetch-event-source';
import { authTokenStore } from '@/store/authTokenStore';

// ======================== 类型定义 ========================

/** 工具调用事件 */
export interface ToolCallEvent {
  eventType: 'tool_call_start' | 'tool_call_end';
  description: string;
}

/** SSE 消息体（与后端约定格式） */
export interface SSEMessage {
  role: string;
  content: string | null;
  type: 'TEXT' | 'EVENT';
  eventData?: ToolCallEvent;
  action: any;
  card: any;
  isFinish: boolean | null;
}

/** SSE 回调集合 */
export interface SSECallbacks<T = SSEMessage> {
  /** 收到文本内容时触发（增量） */
  onChunk?: (content: string) => void;
  /** 收到完整消息时触发（可自行解析） */
  onMessage?: (data: T) => void;
  /** 收到工具调用事件 */
  onToolCall?: (event: ToolCallEvent) => void;
  /** 收到元数据（如 isFinish） */
  onMeta?: (meta: { isFinish: boolean }) => void;
  /** 流正常结束 */
  onFinish?: () => void;
  /** 出错时触发 */
  onError?: (error: any) => void;
}

/** SSE 请求选项 */
export interface SSERequestOptions<T = SSEMessage> extends SSECallbacks<T> {
  /** 请求地址（绝对路径或相对路径，相对路径会拼接 baseURL） */
  url: string;
  /** HTTP 方法，默认 POST */
  method?: string;
  /** 请求体（对象，会被 JSON.stringify） */
  body?: Record<string, any>;
  /** 额外请求头 */
  headers?: Record<string, string>;
  /** 自定义消息解析器，默认使用内置的 SSEMessage 解析 */
  parseMessage?: (raw: string) => T | null;
  /** 请求信号，用于外部中断 */
  signal?: AbortSignal;
}

/** SSEClient 全局配置 */
export interface SSEClientConfig {
  /** 基础URL，默认 'http://localhost:8000' */
  baseURL?: string;
  /** 获取 token 方法，默认从 authTokenStore 读取 */
  getToken?: () => string | null;
  /** 默认请求头 */
  defaultHeaders?: Record<string, string>;
}

// ======================== SSEClient 类 ========================

class SSEClient {
  private config: Required<SSEClientConfig>;

  constructor(config?: SSEClientConfig) {
    this.config = {
      baseURL: config?.baseURL ?? 'http://localhost:8000',
      getToken: config?.getToken ?? (() => authTokenStore.getState().accessToken),
      defaultHeaders: config?.defaultHeaders ?? {},
    };
  }

  /** 更新配置 */
  setConfig(config: Partial<SSEClientConfig>) {
    if (config.baseURL !== undefined) this.config.baseURL = config.baseURL;
    if (config.getToken !== undefined) this.config.getToken = config.getToken;
    if (config.defaultHeaders !== undefined) this.config.defaultHeaders = config.defaultHeaders;
  }

  /** 构建完整 URL */
  private resolveURL(url: string): string {
    // 绝对路径（http/https）直接返回
    if (/^https?:\/\//.test(url)) return url;
    // 已包含 baseURL 前缀则直接返回
    if (url.startsWith(this.config.baseURL)) return url;
    // 拼接 baseURL
    const base = this.config.baseURL.replace(/\/+$/, '');
    const path = url.startsWith('/') ? url : `/${url}`;
    return `${base}${path}`;
  }

  /** 构建请求头 */
  private buildHeaders(extra?: Record<string, string>): Record<string, string> {
    const token = this.config.getToken();
    return {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: token } : {}),
      ...this.config.defaultHeaders,
      ...extra,
    };
  }

  /**
   * 发起 SSE 请求
   * @returns AbortController —— 调用 .abort() 可手动中断流
   */
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
      onError,
    } = options;

    const ctrl = new AbortController();

    // 如果外部传了 signal，监听其 abort 事件
    if (externalSignal) {
      externalSignal.addEventListener('abort', () => ctrl.abort());
    }

    const fullURL = this.resolveURL(url);

    fetchEventSource(fullURL, {
      method,
      headers: this.buildHeaders(headers),
      body: body ? JSON.stringify(body) : undefined,
      signal: ctrl.signal,
      openWhenHidden: true, // 页面切到后台时也保持连接

      async onopen(response) {
        if (response.ok) {
          return; // 正常连接
        }
        throw new Error(`SSE 连接失败: ${response.status} ${response.statusText}`);
      },

      onmessage(event) {
        const dataStr = event.data?.trim();
        if (!dataStr || dataStr === '[DONE]') return;

        try {
          // 支持自定义解析器
          if (parseMessage) {
            const parsed = parseMessage(dataStr);
            if (parsed && onMessage) onMessage(parsed);
            return;
          }

          // 默认按 SSEMessage 解析
          const data = JSON.parse(dataStr) as SSEMessage;

          // 完整消息回调
          if (onMessage) onMessage(data as unknown as T);

          // 元数据
          if (data.isFinish !== undefined && data.isFinish !== null && onMeta) {
            onMeta({ isFinish: data.isFinish });
          }

          // 工具调用事件
          if (data.type === 'EVENT' && data.eventData && onToolCall) {
            onToolCall(data.eventData);
            return;
          }

          // 文本内容
          if (data.content && onChunk) {
            onChunk(data.content);
          }
        } catch (e) {
          console.error('SSE 消息解析失败:', e);
        }
      },

      onclose() {
        onFinish?.();
      },

      onerror(err) {
        onError?.(err);
        // 返回 undefined 让库不再自动重试
        throw err;
      },
    }).catch((err) => {
      // fetchEventSource 的 promise rejection（如手动 abort）
      if (ctrl.signal.aborted) {
        onFinish?.();
      } else {
        onError?.(err);
      }
    });

    return ctrl;
  }
}

// ======================== 默认单例 ========================

/** 默认 SSE 客户端实例（baseURL = 'http://localhost:8000'，直连 AI 服务） */
export const sseClient = new SSEClient();

/**
 * 创建自定义配置的 SSE 客户端
 * @example
 * const customClient = createSSEClient({ baseURL: 'http://localhost:8000' });
 * customClient.request({ url: '/chat', body: { message: 'hi' }, onChunk: console.log });
 */
export function createSSEClient(config?: SSEClientConfig): SSEClient {
  return new SSEClient(config);
}

// ======================== 便捷方法 ========================

/**
 * 快捷 SSE 请求（使用默认客户端）
 * @returns AbortController
 */
export function sseRequest<T = SSEMessage>(options: SSERequestOptions<T>): AbortController {
  return sseClient.request(options);
}
