/**
 * 智能助手聊天 API
 *
 * 管理端助手改为 submit / stream / stop 三段式协议，
 * 不再使用旧的 POST 直连 SSE 接口。
 */
import { fetchEventSource } from '@microsoft/fetch-event-source';
import type { ApiResponse } from '@/utils/request';
import { getServiceLabel } from '@/utils/request/config';
import {
  expireAuthSession,
  getActiveAccessToken,
  isAccessTokenExpiredCode,
  isRefreshTokenExpiredCode,
  refreshAccessToken,
} from '@/utils/request/authRefresh';

// ======================== 常量 ========================

/** 管理端助手提交接口路径 */
const ADMIN_ASSISTANT_SUBMIT_URL = '/ai_api/admin/assistant/chat/submit';
/** 管理端助手流式接口路径 */
const ADMIN_ASSISTANT_STREAM_URL = '/ai_api/admin/assistant/chat/stream';
/** 管理端助手停止接口路径 */
const ADMIN_ASSISTANT_STOP_URL = '/ai_api/admin/assistant/chat/stop';

// ======================== 类型定义 ========================

export namespace ChatTypes {
  /** 运行状态 */
  export type RunStatus = 'running' | 'success' | 'cancelled' | 'error';

  /** 提交请求参数 */
  export interface SubmitRequest {
    /** 用户问题 */
    question: string;
    /** 用户上传图片 URL 列表，最多 5 张 */
    image_urls?: string[];
    /** 会话 UUID，不传表示创建新会话 */
    conversation_uuid?: string;
    /** 用户手动选择的前端自定义模型名，后端会映射为真实模型名 */
    model_name: string;
    /** 是否开启深度思考，由前端按当前选中模型显式传入 */
    reasoning_enabled: boolean;
  }

  /** 提交 / 冲突返回数据 */
  export interface SubmitResponseData {
    /** 会话 UUID */
    conversation_uuid: string;
    /** 消息 UUID */
    message_uuid: string;
    /** 当前运行状态 */
    run_status: RunStatus;
  }

  /** 停止请求参数 */
  export interface StopRequest {
    /** 会话 UUID */
    conversation_uuid: string;
  }

  /** 停止返回数据 */
  export interface StopResponseData extends SubmitResponseData {
    /** 是否已接受停止请求 */
    stop_requested: boolean;
  }

  /** 事件内容 */
  export interface Content {
    /** 文本内容 */
    text?: string | null;
    /** 内容状态，如 replace / success / cancelled / error */
    state?: string | null;
    /** 附加提示消息 */
    message?: string | null;
    /** 思维链节点名称 */
    node?: string | null;
    /** 工具或函数名称 */
    name?: string | null;
    /** 工具调用参数 */
    arguments?: string | null;
    /** 工具返回结果 */
    result?: string | null;
    /** 父节点标识 */
    parent_node?: string | null;
    /** 透传其它字段 */
    [key: string]: unknown;
  }

  /** 事件类型 */
  export enum MessageType {
    NOTICE = 'notice',
    ANSWER = 'answer',
    THINKING = 'thinking',
    CARD = 'card',
    ACTION = 'action',
    STATUS = 'status',
    FUNCTION_CALL = 'function_call',
    TOOL_RESPONSE = 'tool_response',
  }

  /** 事件元数据 */
  export interface StreamMeta {
    /** 会话 UUID */
    conversation_uuid?: string;
    /** 消息 UUID */
    message_uuid?: string;
    /** 最终运行状态 */
    run_status?: Exclude<RunStatus, 'running'>;
    /** 是否为快照事件 */
    snapshot?: boolean;
    /** 是否为 replace 快照 */
    replace?: boolean;
    /** 透传其它字段 */
    [key: string]: unknown;
  }

  /** 流式事件 */
  export interface AssistantStreamMessage {
    /** 事件内容 */
    content: Content;
    /** 事件类型 */
    type: MessageType | string;
    /** 元数据 */
    meta?: StreamMeta;
    /** 是否为结束事件 */
    is_end: boolean;
    /** 时间戳 */
    timestamp?: number;
  }
}

/**
 * 智能助手业务错误。
 *
 * @template TData 错误响应中的 data 结构。
 */
export class AssistantApiError<TData = unknown> extends Error {
  /** 业务错误码 */
  public code: number;
  /** 错误响应数据 */
  public data: TData | undefined;
  /** 错误响应时间戳 */
  public timestamp: number | undefined;

  /**
   * @param code 错误码。
   * @param message 错误消息。
   * @param data 错误数据。
   * @param timestamp 错误时间戳。
   */
  constructor(code: number, message: string, data?: TData, timestamp?: number) {
    super(message);
    this.name = 'AssistantApiError';
    this.code = code;
    this.data = data;
    this.timestamp = timestamp;
  }
}

/** attach 流式事件的回调 */
export interface AttachAssistantStreamCallbacks {
  /** 收到事件时触发 */
  onMessage?: (message: ChatTypes.AssistantStreamMessage) => void;
  /** 流结束时触发 */
  onFinish?: () => void;
  /** 流异常时触发 */
  onError?: (error: unknown) => void;
}

/** attach 流式请求的附加选项 */
export interface AttachAssistantStreamOptions {
  /** 可选的断点续传事件 ID */
  lastEventId?: string;
}

/**
 * 智能助手 SSE 鉴权刷新后需要重连的内部信号。
 */
class AssistantStreamAuthRetryError extends Error {
  constructor() {
    super('智能助手访问令牌已刷新，正在重连');
    this.name = 'AssistantStreamAuthRetryError';
  }
}

// ======================== 内部工具函数 ========================

/**
 * 获取当前鉴权请求头。
 *
 * @param options.refreshWhenExpired 本地 JWT 已过期时是否先刷新。
 * @returns 带 Authorization 的请求头对象。
 */
async function getAssistantAuthHeaders(
  options: { refreshWhenExpired?: boolean } = {},
): Promise<Record<string, string>> {
  const accessToken = await getActiveAccessToken({
    refreshWhenExpired: options.refreshWhenExpired,
  });
  return accessToken ? { Authorization: accessToken } : {};
}

/**
 * 记录智能助手请求错误来源。
 *
 * @param url 请求地址。
 * @param detail 错误诊断详情。
 */
function logAssistantRequestError(url: string, detail: Record<string, unknown>): void {
  console.error(`[请求错误][${getServiceLabel(url)}]`, {
    url,
    ...detail,
  });
}

/**
 * 解析标准 JSON 响应体。
 *
 * @template TData 业务 data 结构。
 * @param response fetch 响应对象。
 * @returns 完整标准响应包裹。
 */
async function parseAssistantJsonResponse<TData>(response: Response): Promise<ApiResponse<TData>> {
  const json = (await response.json()) as ApiResponse<TData>;
  return json;
}

/**
 * 判断智能助手响应是否需要刷新访问令牌后重试。
 *
 * @param response Fetch 响应对象。
 * @param payload 标准响应体。
 * @returns 是否需要刷新访问令牌。
 */
function shouldRefreshAssistantAuth(
  response: Response,
  payload: ApiResponse<unknown> | null,
): boolean {
  if (isAccessTokenExpiredCode(payload?.code)) {
    return true;
  }
  return response.status === 401 && !isRefreshTokenExpiredCode(payload?.code);
}

/**
 * 处理刷新令牌失效响应。
 *
 * @param payload 标准响应体。
 */
function handleRefreshTokenExpiredPayload(payload: ApiResponse<unknown> | null): void {
  if (isRefreshTokenExpiredCode(payload?.code)) {
    expireAuthSession();
  }
}

/**
 * 发起一次智能助手 JSON fetch 请求。
 *
 * @template TBody 请求体结构。
 * @param url 请求路径。
 * @param body 请求体。
 * @param refreshWhenExpired 本地 JWT 已过期时是否先刷新。
 * @returns Fetch 响应对象。
 */
async function fetchAssistantJsonResponse<TBody>(
  url: string,
  body: TBody,
  refreshWhenExpired: boolean,
): Promise<Response> {
  try {
    return await fetch(url, {
      method: 'POST',
      headers: {
        ...(await getAssistantAuthHeaders({ refreshWhenExpired })),
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    });
  } catch (error) {
    logAssistantRequestError(url, {
      stage: 'fetch',
      error,
    });
    throw error;
  }
}

/**
 * 解析智能助手 JSON 请求结果。
 *
 * @template TData 响应 data 结构。
 * @param url 请求路径。
 * @param response Fetch 响应对象。
 * @param payload 标准响应体。
 * @returns 标准响应中的 data 字段。
 * @throws AssistantApiError 当 code 非 200 或 HTTP 失败时抛出。
 */
function resolveAssistantJsonResult<TData>(
  url: string,
  response: Response,
  payload: ApiResponse<TData> | null,
): TData {
  handleRefreshTokenExpiredPayload(payload as ApiResponse<unknown> | null);
  if (!response.ok) {
    const message = payload?.message || `请求失败（HTTP ${response.status}）`;
    logAssistantRequestError(url, {
      stage: 'http',
      status: response.status,
      statusText: response.statusText,
      message,
      response: payload,
    });
    throw new AssistantApiError(response.status, message, payload?.data, payload?.timestamp);
  }

  if (!payload) {
    logAssistantRequestError(url, {
      stage: 'parse',
      status: response.status,
      statusText: response.statusText,
    });
    throw new AssistantApiError(500, '智能助手响应解析失败');
  }

  if (payload.code !== 200) {
    logAssistantRequestError(url, {
      stage: 'business',
      code: payload.code,
      message: payload.message,
      response: payload,
    });
    throw new AssistantApiError(payload.code, payload.message, payload.data, payload.timestamp);
  }

  return payload.data;
}

/**
 * 发送智能助手 JSON 请求。
 *
 * @template TData 响应 data 结构。
 * @template TBody 请求体结构。
 * @param url 请求路径。
 * @param body 请求体。
 * @returns 标准响应中的 data 字段。
 * @throws AssistantApiError 当 code 非 200 或 HTTP 失败时抛出。
 */
async function requestAssistantJson<TData, TBody>(url: string, body: TBody): Promise<TData> {
  let response = await fetchAssistantJsonResponse(url, body, true);
  let payload: ApiResponse<TData> | null;
  try {
    payload = await parseAssistantJsonResponse<TData>(response);
  } catch {
    payload = null;
  }

  if (shouldRefreshAssistantAuth(response, payload as ApiResponse<unknown> | null)) {
    await refreshAccessToken();
    response = await fetchAssistantJsonResponse(url, body, false);
    try {
      payload = await parseAssistantJsonResponse<TData>(response);
    } catch {
      payload = null;
    }
  }

  return resolveAssistantJsonResult(url, response, payload);
}

/**
 * 解析智能助手 SSE 建连失败响应。
 *
 * @param response Fetch 响应对象。
 * @param streamUrl SSE 地址。
 * @param hasRetried 是否已经刷新 token 并重连过。
 * @returns 需要抛出的错误对象。
 */
async function resolveAssistantStreamOpenError(
  response: Response,
  streamUrl: string,
  hasRetried: boolean,
): Promise<Error> {
  let errorMessage = `SSE 连接失败: ${response.status} ${response.statusText}`;
  let errorPayload: ApiResponse<unknown> | null = null;
  try {
    errorPayload = await parseAssistantJsonResponse<unknown>(response);
    if (errorPayload?.message) {
      errorMessage = errorPayload.message;
    }
  } catch {
    // ignore parse error
  }

  handleRefreshTokenExpiredPayload(errorPayload);
  if (!hasRetried && shouldRefreshAssistantAuth(response, errorPayload)) {
    await refreshAccessToken();
    logAssistantRequestError(streamUrl, {
      stage: 'sse-open',
      status: response.status,
      statusText: response.statusText,
      message: errorMessage,
      response: errorPayload,
      authRetry: true,
    });
    return new AssistantStreamAuthRetryError();
  }

  logAssistantRequestError(streamUrl, {
    stage: 'sse-open',
    status: response.status,
    statusText: response.statusText,
    message: errorMessage,
    response: errorPayload,
  });
  return new Error(errorMessage);
}

// ======================== 导出方法 ========================

/**
 * 提交一条智能助手消息。
 *
 * @param payload 提交参数。
 * @returns 后端返回的会话 UUID、消息 UUID 和运行状态。
 */
export async function submitAssistantMessage(
  payload: ChatTypes.SubmitRequest,
): Promise<ChatTypes.SubmitResponseData> {
  return requestAssistantJson<ChatTypes.SubmitResponseData, ChatTypes.SubmitRequest>(
    ADMIN_ASSISTANT_SUBMIT_URL,
    payload,
  );
}

/**
 * 发送停止生成请求。
 *
 * @param payload 停止请求参数。
 * @returns 停止请求受理结果。
 */
export async function stopAssistantMessage(
  payload: ChatTypes.StopRequest,
): Promise<ChatTypes.StopResponseData> {
  return requestAssistantJson<ChatTypes.StopResponseData, ChatTypes.StopRequest>(
    ADMIN_ASSISTANT_STOP_URL,
    payload,
  );
}

/**
 * attach 到指定会话的流式输出。
 *
 * @param conversationUuid 会话 UUID。
 * @param callbacks 流式回调集合。
 * @param options 附加选项。
 * @returns 可中断当前 attach 的控制器。
 */
export function attachAssistantStream(
  conversationUuid: string,
  callbacks: AttachAssistantStreamCallbacks,
  options: AttachAssistantStreamOptions = {},
): AbortController {
  const controller = new AbortController();
  const searchParams = new URLSearchParams({
    conversation_uuid: conversationUuid,
  });
  const streamUrl = `${ADMIN_ASSISTANT_STREAM_URL}?${searchParams.toString()}`;

  /**
   * 建立 SSE 连接。
   *
   * @param hasRetried 是否已经刷新 token 并重连过。
   */
  const openStream = async (hasRetried: boolean): Promise<void> => {
    let headers: Record<string, string>;
    try {
      headers = {
        ...(await getAssistantAuthHeaders({ refreshWhenExpired: !hasRetried })),
        ...(options.lastEventId ? { 'Last-Event-ID': options.lastEventId } : {}),
      };
    } catch (error) {
      if (!controller.signal.aborted) {
        callbacks.onError?.(error);
      }
      return;
    }

    fetchEventSource(streamUrl, {
      method: 'GET',
      headers,
      signal: controller.signal,
      openWhenHidden: true,
      async onopen(response) {
        if (response.ok) {
          return;
        }
        throw await resolveAssistantStreamOpenError(response, streamUrl, hasRetried);
      },
      onmessage(event) {
        const rawData = event.data?.trim();
        if (!rawData) {
          return;
        }

        try {
          const message = JSON.parse(rawData) as ChatTypes.AssistantStreamMessage;
          callbacks.onMessage?.(message);
        } catch (error) {
          callbacks.onError?.(error);
        }
      },
      onclose() {
        callbacks.onFinish?.();
      },
      onerror(error) {
        throw error;
      },
    }).catch((error) => {
      if (controller.signal.aborted) {
        callbacks.onFinish?.();
        return;
      }
      if (error instanceof AssistantStreamAuthRetryError && !hasRetried) {
        void openStream(true);
        return;
      }
      callbacks.onError?.(error);
    });
  };

  void openStream(false);

  return controller;
}

/**
 * 解析结束事件对应的最终状态。
 *
 * @param message SSE 结束事件。
 * @returns 标准化后的最终状态。
 */
export function resolveAssistantFinalStatus(
  message: ChatTypes.AssistantStreamMessage,
): Exclude<ChatTypes.RunStatus, 'running'> {
  const metaStatus = message.meta?.run_status;
  if (metaStatus === 'success' || metaStatus === 'cancelled' || metaStatus === 'error') {
    return metaStatus;
  }

  const contentState = message.content.state;
  if (contentState === 'success' || contentState === 'cancelled' || contentState === 'error') {
    return contentState;
  }

  return 'success';
}

/**
 * 判断当前事件是否为 replace 快照。
 *
 * @param message SSE 事件。
 * @returns 是否为 replace 快照事件。
 */
export function isAssistantReplaceMessage(message: ChatTypes.AssistantStreamMessage): boolean {
  return message.content.state === 'replace' || Boolean(message.meta?.replace);
}
