import type { ApiResponse } from '@/utils/request';
import {
  expireAuthSession,
  getActiveAccessToken,
  isAccessTokenExpiredCode,
  isRefreshTokenExpiredCode,
  refreshAccessToken,
} from '@/utils/request/authRefresh';
import { getServiceLabel } from '@/utils/request/config';

/** 助手消息语音流式播放接口地址。 */
const ASSISTANT_TTS_STREAM_URL = '/ai_api/admin/assistant/message/tts/stream';

/**
 * 记录语音播放请求错误来源。
 *
 * @param url 请求地址。
 * @param detail 错误诊断详情。
 */
function logTtsRequestError(url: string, detail: Record<string, unknown>): void {
  console.error(`[请求错误][${getServiceLabel(url)}]`, {
    url,
    ...detail,
  });
}

/**
 * 获取 TTS 流式请求鉴权请求头。
 *
 * @param refreshWhenExpired 本地 JWT 已过期时是否先刷新。
 * @returns 带 Authorization 的请求头。
 */
async function getTtsAuthHeaders(refreshWhenExpired: boolean): Promise<Record<string, string>> {
  const token = await getActiveAccessToken({ refreshWhenExpired });
  if (!token) {
    throw new Error('未授权，请登录后再试');
  }
  return { Authorization: token };
}

/**
 * 请求助手消息语音流。
 *
 * @param messageUuid 消息 UUID。
 * @param signal 外部中断信号。
 * @param refreshWhenExpired 本地 JWT 已过期时是否先刷新。
 * @returns TTS 流式响应。
 */
async function fetchAssistantMessageTts(
  messageUuid: string,
  signal: AbortSignal | undefined,
  refreshWhenExpired: boolean,
): Promise<Response> {
  try {
    return await fetch(ASSISTANT_TTS_STREAM_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(await getTtsAuthHeaders(refreshWhenExpired)),
      },
      body: JSON.stringify({ message_uuid: messageUuid }),
      signal,
    });
  } catch (error) {
    logTtsRequestError(ASSISTANT_TTS_STREAM_URL, {
      stage: 'fetch',
      error,
    });
    throw error;
  }
}

/**
 * 解析 TTS 错误响应。
 *
 * @param response Fetch 响应对象。
 * @returns 错误消息与标准响应体。
 */
async function parseTtsErrorResponse(
  response: Response,
): Promise<{ message: string; payload: ApiResponse<unknown> | null }> {
  let errorMessage = `TTS 请求失败: ${response.status}`;
  let errorPayload: ApiResponse<unknown> | null = null;
  try {
    errorPayload = (await response.json()) as ApiResponse<unknown>;
    errorMessage = errorPayload.message || errorMessage;
  } catch {
    // Ignore
  }
  return { message: errorMessage, payload: errorPayload };
}

/**
 * 判断 TTS 响应是否需要刷新访问令牌后重试。
 *
 * @param response Fetch 响应对象。
 * @param payload 标准错误响应体。
 * @returns 是否需要刷新访问令牌。
 */
function shouldRefreshTtsAuth(response: Response, payload: ApiResponse<unknown> | null): boolean {
  if (isAccessTokenExpiredCode(payload?.code)) {
    return true;
  }
  return response.status === 401 && !isRefreshTokenExpiredCode(payload?.code);
}

/**
 * 确保 TTS 响应已通过鉴权，必要时刷新 token 后重试一次。
 *
 * @param response 原始响应对象。
 * @param messageUuid 消息 UUID。
 * @param signal 外部中断信号。
 * @returns 可读取音频流的响应对象。
 */
async function ensureTtsAuthorizedResponse(
  response: Response,
  messageUuid: string,
  signal: AbortSignal | undefined,
): Promise<Response> {
  if (response.ok && response.body) {
    return response;
  }

  const errorInfo = await parseTtsErrorResponse(response);
  if (isRefreshTokenExpiredCode(errorInfo.payload?.code)) {
    expireAuthSession();
  }
  if (shouldRefreshTtsAuth(response, errorInfo.payload)) {
    await refreshAccessToken();
    const retryResponse = await fetchAssistantMessageTts(messageUuid, signal, false);
    if (retryResponse.ok && retryResponse.body) {
      return retryResponse;
    }
    const retryErrorInfo = await parseTtsErrorResponse(retryResponse);
    if (isRefreshTokenExpiredCode(retryErrorInfo.payload?.code)) {
      expireAuthSession();
    }
    logTtsRequestError(ASSISTANT_TTS_STREAM_URL, {
      stage: 'http',
      status: retryResponse.status,
      statusText: retryResponse.statusText,
      message: retryErrorInfo.message,
      response: retryErrorInfo.payload,
    });
    throw new Error(retryErrorInfo.message);
  }
  logTtsRequestError(ASSISTANT_TTS_STREAM_URL, {
    stage: 'http',
    status: response.status,
    statusText: response.statusText,
    message: errorInfo.message,
    response: errorInfo.payload,
  });
  throw new Error(errorInfo.message);
}

/**
 * 播放助手消息语音（流式）
 *
 * 对接接口：POST /admin/assistant/message/tts/stream
 * 使用 MediaSource 播放 HTTP chunked 流式音频。
 *
 * @param messageUuid 消息 UUID。
 * @param options 播放控制选项。
 * @returns 播放完成时的 Promise。
 */
export async function playAssistantMessageTts(
  messageUuid: string,
  options?: { signal?: AbortSignal },
): Promise<void> {
  let resp = await fetchAssistantMessageTts(messageUuid, options?.signal, true);
  resp = await ensureTtsAuthorizedResponse(resp, messageUuid, options?.signal);

  const audio = new Audio();
  audio.autoplay = true;

  const mediaSource = new MediaSource();
  audio.src = URL.createObjectURL(mediaSource);

  // 如果传入了 signal，监听 abort 事件以停止播放
  if (options?.signal) {
    options.signal.addEventListener('abort', () => {
      audio.pause();
      audio.removeAttribute('src');
      audio.load();
      if (mediaSource.readyState === 'open') {
        try {
          mediaSource.endOfStream();
        } catch {
          // ignore
        }
      }
    });
  }

  // 创建一个 Promise 用于追踪音频播放结束
  const playbackPromise = new Promise<void>((resolve, reject) => {
    audio.onended = () => {
      URL.revokeObjectURL(audio.src);
      resolve();
    };
    audio.onerror = (_e) => {
      URL.revokeObjectURL(audio.src);
      reject(new Error('音频播放失败'));
    };
    if (options?.signal) {
      options.signal.addEventListener('abort', () => {
        URL.revokeObjectURL(audio.src);
        reject(new DOMException('Aborted', 'AbortError'));
      });
    }
  });

  // 当 MediaSource 打开后开始准备写入 Buffer
  await new Promise<void>((resolve, reject) => {
    const onOpen = () => resolve();
    mediaSource.addEventListener('sourceopen', onOpen, { once: true });
    if (options?.signal) {
      options.signal.addEventListener('abort', () => {
        mediaSource.removeEventListener('sourceopen', onOpen);
        reject(new DOMException('Aborted', 'AbortError'));
      });
    }
  });

  // mp3 格式对应的 mime
  const sourceBuffer = mediaSource.addSourceBuffer('audio/mpeg');
  const responseBody = resp.body;
  if (!responseBody) {
    throw new Error('TTS 响应缺少音频流');
  }
  const reader = responseBody.getReader();

  try {
    while (true) {
      if (options?.signal?.aborted) {
        break;
      }

      const { done, value } = await reader.read();
      if (done) break;
      if (!value) continue;

      // 串行写入 Buffer，确保顺序正确
      await new Promise<void>((resolve, reject) => {
        const append = () => {
          sourceBuffer.removeEventListener('error', onError);
          sourceBuffer.appendBuffer(value);
        };

        const onUpdateEnd = () => {
          sourceBuffer.removeEventListener('updateend', onUpdateEnd);
          resolve();
        };

        const onError = (e: Event) => {
          sourceBuffer.removeEventListener('updateend', onUpdateEnd);
          reject(e);
        };

        sourceBuffer.addEventListener('updateend', onUpdateEnd, { once: true });
        sourceBuffer.addEventListener('error', onError, { once: true });

        if (options?.signal) {
          options.signal.addEventListener('abort', () => {
            sourceBuffer.removeEventListener('updateend', onUpdateEnd);
            sourceBuffer.removeEventListener('error', onError);
            reject(new DOMException('Aborted', 'AbortError'));
          });
        }

        if (sourceBuffer.updating) {
          sourceBuffer.addEventListener('updateend', append, { once: true });
        } else {
          append();
        }
      });
    }
  } catch (error: any) {
    if (error.name !== 'AbortError') {
      console.error('音频流读取或写入失败:', error);
    }
  } finally {
    if (mediaSource.readyState === 'open') {
      try {
        mediaSource.endOfStream();
      } catch {
        // ignore
      }
    }
    // 释放 reader
    reader.cancel().catch(() => {});
  }

  // 等待音频真正播放完毕
  return playbackPromise;
}
