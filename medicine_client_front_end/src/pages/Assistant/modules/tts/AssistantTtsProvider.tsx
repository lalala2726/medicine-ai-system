import { useCallback, useEffect, useMemo, useRef, useState, type PropsWithChildren } from 'react'
import { streamAssistantMessageTts } from '@/api/assistant'
import { useChatStore } from '@/stores/chatStore'
import { showErrorNotify, showNotify } from '@/utils/notify'
import type { ChatMessage } from '../messages/chatTypes'
import { AssistantTtsContext, type AssistantTtsContextValue } from './assistantTtsContext'

/** TTS 播放状态值。 */
type AssistantTtsPlaybackStatus = 'idle' | 'loading' | 'playing' | 'error'

/** 页面级 TTS 播放状态。 */
interface AssistantTtsPlaybackState {
  /** 当前正在处理的消息 ID。 */
  activeMessageId?: string
  /** 当前播放状态。 */
  playbackStatus: AssistantTtsPlaybackStatus
}

/** Assistant TTS Provider 组件属性。 */
interface AssistantTtsProviderProps extends PropsWithChildren {
  /** 当前页面消息列表，用于在会话切换或消息移除时自动停止播放。 */
  messages: ChatMessage[]
}

/** 不支持朗读时的兜底提示文案。 */
const ASSISTANT_TTS_UNSUPPORTED_TEXT = '当前消息暂不支持朗读'

/** 语音播放的通用失败文案。 */
const ASSISTANT_TTS_PLAYBACK_ERROR_TEXT = '语音播放失败，请稍后重试'

/** 音频流默认使用的 MIME 类型。 */
const ASSISTANT_TTS_DEFAULT_CONTENT_TYPE = 'audio/mpeg'

/** Assistant TTS 初始状态。 */
const INITIAL_TTS_PLAYBACK_STATE: AssistantTtsPlaybackState = {
  activeMessageId: undefined,
  playbackStatus: 'idle'
}

/**
 * 规范化消息 ID，避免空字符串和纯空白字符串进入播放链路。
 *
 * @param messageId - 原始消息 ID
 * @returns 归一化后的消息 ID
 */
const normalizeMessageId = (messageId?: string) => {
  return messageId?.trim() || ''
}

/**
 * 判断当前错误是否为浏览器的主动中断错误。
 *
 * @param error - 捕获到的异常对象
 * @returns 是否为 AbortError
 */
const isAbortError = (error: unknown) => {
  return error instanceof DOMException && error.name === 'AbortError'
}

/**
 * 将 `Uint8Array` 转成独立可追加的 `ArrayBuffer`。
 *
 * @param chunk - 待读取的二进制片段
 * @returns 独立的 `ArrayBuffer`
 */
const toArrayBuffer = (chunk: Uint8Array) => {
  return chunk.buffer.slice(chunk.byteOffset, chunk.byteOffset + chunk.byteLength)
}

/**
 * 统一等待音频元素开始播放。
 *
 * @param audioElement - 目标音频元素
 * @returns 播放开始后的 Promise
 */
const startAudioPlayback = async (audioElement: HTMLAudioElement) => {
  const playResult = audioElement.play()

  if (playResult instanceof Promise) {
    await playResult
  }
}

/**
 * 等待 `MediaSource` 进入可用状态。
 *
 * @param mediaSource - 待监听的 `MediaSource`
 * @param signal - 当前播放请求的中断信号
 * @returns `sourceopen` 就绪后的 Promise
 */
const waitForMediaSourceOpen = (mediaSource: MediaSource, signal: AbortSignal) => {
  return new Promise<void>((resolve, reject) => {
    /**
     * 清理当前监听器。
     *
     * @returns 无返回值
     */
    const cleanup = () => {
      mediaSource.removeEventListener('sourceopen', handleSourceOpen)
      signal.removeEventListener('abort', handleAbort)
    }

    /**
     * 处理 `MediaSource` 就绪。
     *
     * @returns 无返回值
     */
    const handleSourceOpen = () => {
      cleanup()
      resolve()
    }

    /**
     * 处理中断事件。
     *
     * @returns 无返回值
     */
    const handleAbort = () => {
      cleanup()
      reject(new DOMException('The request was aborted.', 'AbortError'))
    }

    mediaSource.addEventListener('sourceopen', handleSourceOpen, { once: true })
    signal.addEventListener('abort', handleAbort, { once: true })
  })
}

/**
 * 将一个二进制片段追加到 `SourceBuffer`。
 *
 * @param sourceBuffer - 目标 `SourceBuffer`
 * @param chunk - 待追加的音频片段
 * @param signal - 当前播放请求的中断信号
 * @returns 当前片段追加完成后的 Promise
 */
const appendChunkToSourceBuffer = (sourceBuffer: SourceBuffer, chunk: Uint8Array, signal: AbortSignal) => {
  return new Promise<void>((resolve, reject) => {
    /**
     * 清理当前事件监听。
     *
     * @returns 无返回值
     */
    const cleanup = () => {
      sourceBuffer.removeEventListener('updateend', handleUpdateEnd)
      sourceBuffer.removeEventListener('error', handleError)
      signal.removeEventListener('abort', handleAbort)
    }

    /**
     * 处理当前片段追加完成事件。
     *
     * @returns 无返回值
     */
    const handleUpdateEnd = () => {
      cleanup()
      resolve()
    }

    /**
     * 处理 `SourceBuffer` 错误事件。
     *
     * @returns 无返回值
     */
    const handleError = () => {
      cleanup()
      reject(new Error('音频流追加失败'))
    }

    /**
     * 处理中断事件。
     *
     * @returns 无返回值
     */
    const handleAbort = () => {
      cleanup()
      reject(new DOMException('The request was aborted.', 'AbortError'))
    }

    sourceBuffer.addEventListener('updateend', handleUpdateEnd, { once: true })
    sourceBuffer.addEventListener('error', handleError, { once: true })
    signal.addEventListener('abort', handleAbort, { once: true })

    try {
      sourceBuffer.appendBuffer(toArrayBuffer(chunk))
    } catch (error) {
      cleanup()
      reject(error)
    }
  })
}

/**
 * Assistant 页面级 TTS Provider。
 * 负责管理全页唯一的音频实例、播放状态以及朗读按钮的交互控制。
 *
 * @param props - Provider 属性
 * @returns Provider 节点
 */
export const AssistantTtsProvider = ({ messages, children }: AssistantTtsProviderProps) => {
  /** 当前活跃会话 UUID。 */
  const activeConversationUuid = useChatStore(state => state.activeConversationUuid)
  /** 当前播放状态。 */
  const [playbackState, setPlaybackState] = useState<AssistantTtsPlaybackState>(INITIAL_TTS_PLAYBACK_STATE)
  /** 单例音频元素引用。 */
  const audioElementRef = useRef<HTMLAudioElement | null>(null)
  /** 当前播放请求的中断控制器。 */
  const abortControllerRef = useRef<AbortController | null>(null)
  /** 当前读取中的流式 reader。 */
  const responseReaderRef = useRef<ReadableStreamDefaultReader<Uint8Array> | null>(null)
  /** 当前 object URL。 */
  const objectUrlRef = useRef<string | null>(null)
  /** 当前 MediaSource 实例。 */
  const mediaSourceRef = useRef<MediaSource | null>(null)
  /** 当前播放状态引用，避免异步闭包读取过期值。 */
  const playbackStateRef = useRef<AssistantTtsPlaybackState>(INITIAL_TTS_PLAYBACK_STATE)
  /** 记录上一次会话 UUID，用于在切换会话时自动停止播放。 */
  const previousConversationUuidRef = useRef<string | null>(activeConversationUuid)

  /**
   * 同步更新播放状态及其引用。
   *
   * @param nextState - 下一个播放状态
   * @returns 无返回值
   */
  const updatePlaybackState = useCallback((nextState: AssistantTtsPlaybackState) => {
    playbackStateRef.current = nextState
    setPlaybackState(nextState)
  }, [])

  /**
   * 获取全局唯一的音频元素实例。
   *
   * @returns 音频元素实例
   */
  const ensureAudioElement = useCallback(() => {
    if (!audioElementRef.current) {
      const audioElement = new Audio()
      audioElement.preload = 'auto'
      audioElementRef.current = audioElement
    }

    return audioElementRef.current
  }, [])

  /**
   * 释放当前播放过程中持有的外部资源。
   *
   * @returns 无返回值
   */
  const releasePlaybackResources = useCallback((abortRequest: boolean = true) => {
    if (responseReaderRef.current) {
      responseReaderRef.current.cancel().catch(() => undefined)
      responseReaderRef.current = null
    }

    if (abortRequest && abortControllerRef.current) {
      abortControllerRef.current.abort()
      abortControllerRef.current = null
    }

    if (objectUrlRef.current) {
      URL.revokeObjectURL(objectUrlRef.current)
      objectUrlRef.current = null
    }

    mediaSourceRef.current = null
  }, [])

  /**
   * 将当前音频元素恢复为空闲状态。
   *
   * @returns 无返回值
   */
  const resetAudioElement = useCallback(() => {
    if (!audioElementRef.current) {
      return
    }

    audioElementRef.current.pause()
    audioElementRef.current.removeAttribute('src')
    audioElementRef.current.load()
  }, [])

  /**
   * 停止当前音频播放并重置状态。
   *
   * @returns 无返回值
   */
  const stopPlayback = useCallback(() => {
    updatePlaybackState(INITIAL_TTS_PLAYBACK_STATE)
    releasePlaybackResources()
    resetAudioElement()
  }, [releasePlaybackResources, resetAudioElement, updatePlaybackState])

  /**
   * 以完整 Blob 的方式播放音频。
   *
   * @param response - TTS 接口的成功响应
   * @param audioElement - 当前音频元素
   * @param signal - 当前播放请求的中断信号
   * @returns 播放开始后的 Promise
   */
  const playResponseAsBlob = useCallback(
    async (response: Response, audioElement: HTMLAudioElement, signal: AbortSignal) => {
      const audioBlob = await response.blob()

      if (signal.aborted) {
        throw new DOMException('The request was aborted.', 'AbortError')
      }

      objectUrlRef.current = URL.createObjectURL(audioBlob)
      audioElement.src = objectUrlRef.current
      audioElement.load()
      await startAudioPlayback(audioElement)
    },
    []
  )

  /**
   * 优先按流式方式播放音频。
   * 当浏览器能力不足或初始化失败时，调用方可降级到 Blob 播放。
   *
   * @param response - TTS 接口的成功响应
   * @param audioElement - 当前音频元素
   * @param contentType - 音频 MIME 类型
   * @param signal - 当前播放请求的中断信号
   * @returns 播放开始后的 Promise
   */
  const playResponseAsMediaSource = useCallback(
    async (response: Response, audioElement: HTMLAudioElement, contentType: string, signal: AbortSignal) => {
      if (typeof window === 'undefined' || !window.MediaSource || !response.body) {
        throw new Error('当前环境不支持流式音频播放')
      }

      /** 浏览器内置的 `MediaSource` 构造函数。 */
      const MediaSourceConstructor = window.MediaSource

      if (!MediaSourceConstructor.isTypeSupported(contentType)) {
        throw new Error(`当前浏览器不支持 ${contentType} 的流式播放`)
      }

      const mediaSource = new MediaSource()
      mediaSourceRef.current = mediaSource
      objectUrlRef.current = URL.createObjectURL(mediaSource)
      audioElement.src = objectUrlRef.current
      audioElement.load()

      await waitForMediaSourceOpen(mediaSource, signal)

      const sourceBuffer = mediaSource.addSourceBuffer(contentType)
      const responseReader = response.body.getReader()
      responseReaderRef.current = responseReader

      /** 是否已经启动过音频播放。 */
      let hasStartedPlayback = false

      await new Promise<void>((resolve, reject) => {
        /**
         * 后台持续读取音频流并追加到 `SourceBuffer`。
         *
         * @returns 无返回值
         */
        const pumpAudioStream = async () => {
          try {
            while (true) {
              if (signal.aborted) {
                throw new DOMException('The request was aborted.', 'AbortError')
              }

              const { done, value } = await responseReader.read()

              if (done) {
                break
              }

              if (!value) {
                continue
              }

              await appendChunkToSourceBuffer(sourceBuffer, value, signal)

              if (!hasStartedPlayback) {
                hasStartedPlayback = true
                await startAudioPlayback(audioElement)
                resolve()
              }
            }

            responseReaderRef.current = null

            if (!hasStartedPlayback) {
              await startAudioPlayback(audioElement)
              resolve()
            }

            if (mediaSource.readyState === 'open') {
              mediaSource.endOfStream()
            }
          } catch (error) {
            responseReaderRef.current = null

            if (!hasStartedPlayback) {
              reject(error)
              return
            }

            if (!isAbortError(error)) {
              showErrorNotify(ASSISTANT_TTS_PLAYBACK_ERROR_TEXT)
              stopPlayback()
            }
          }
        }

        void pumpAudioStream()
      })
    },
    [stopPlayback]
  )

  /**
   * 启动某条 AI 消息的 TTS 播放。
   *
   * @param messageId - 目标消息 UUID
   * @returns 播放流程的 Promise
   */
  const startMessagePlayback = useCallback(
    async (messageId: string) => {
      stopPlayback()

      /** 当前音频元素。 */
      const audioElement = ensureAudioElement()
      /** 当前播放请求的中断控制器。 */
      const abortController = new AbortController()

      abortControllerRef.current = abortController
      updatePlaybackState({
        activeMessageId: messageId,
        playbackStatus: 'loading'
      })

      /**
       * 统一将播放状态收敛为出错状态，并给出错误提示。
       *
       * @param error - 当前播放错误
       * @returns 无返回值
       */
      const handlePlaybackFailure = (error: unknown) => {
        if (isAbortError(error)) {
          return
        }

        const errorMessage =
          error instanceof Error && error.message.trim() ? error.message : ASSISTANT_TTS_PLAYBACK_ERROR_TEXT
        updatePlaybackState({
          activeMessageId: messageId,
          playbackStatus: 'error'
        })
        showErrorNotify(errorMessage)
        stopPlayback()
      }

      try {
        const response = await streamAssistantMessageTts({
          messageUuid: messageId,
          signal: abortController.signal
        })

        if (abortController.signal.aborted) {
          throw new DOMException('The request was aborted.', 'AbortError')
        }

        /** 当前响应头中的音频类型。 */
        const contentType =
          response.headers.get('content-type')?.split(';')[0].trim() || ASSISTANT_TTS_DEFAULT_CONTENT_TYPE
        /** 流式播放失败时用于兜底的响应副本。 */
        const fallbackResponse = response.clone()

        try {
          await playResponseAsMediaSource(response, audioElement, contentType, abortController.signal)
        } catch (error) {
          if (isAbortError(error)) {
            throw error
          }

          releasePlaybackResources(false)
          resetAudioElement()
          await playResponseAsBlob(fallbackResponse, audioElement, abortController.signal)
        }

        updatePlaybackState({
          activeMessageId: messageId,
          playbackStatus: 'playing'
        })
      } catch (error) {
        handlePlaybackFailure(error)
      }
    },
    [
      ensureAudioElement,
      playResponseAsBlob,
      playResponseAsMediaSource,
      releasePlaybackResources,
      resetAudioElement,
      stopPlayback,
      updatePlaybackState
    ]
  )

  /**
   * 切换某条消息的播放状态。
   *
   * @param messageId - 目标消息 UUID
   * @returns 无返回值
   */
  const toggleMessagePlayback = useCallback(
    async (messageId?: string) => {
      const normalizedMessageId = normalizeMessageId(messageId)

      if (!normalizedMessageId) {
        showNotify(ASSISTANT_TTS_UNSUPPORTED_TEXT)
        return
      }

      if (
        playbackStateRef.current.activeMessageId === normalizedMessageId &&
        playbackStateRef.current.playbackStatus !== 'idle'
      ) {
        stopPlayback()
        return
      }

      await startMessagePlayback(normalizedMessageId)
    },
    [startMessagePlayback, stopPlayback]
  )

  /**
   * 判断某条消息是否正在播放。
   *
   * @param messageId - 待检测的消息 UUID
   * @returns 是否正在播放
   */
  const isMessagePlaying = useCallback(
    (messageId?: string) => {
      const normalizedMessageId = normalizeMessageId(messageId)

      return (
        Boolean(normalizedMessageId) &&
        playbackState.activeMessageId === normalizedMessageId &&
        playbackState.playbackStatus === 'playing'
      )
    },
    [playbackState.activeMessageId, playbackState.playbackStatus]
  )

  /**
   * 判断某条消息是否正在加载中。
   *
   * @param messageId - 待检测的消息 UUID
   * @returns 是否正在加载
   */
  const isMessageLoading = useCallback(
    (messageId?: string) => {
      const normalizedMessageId = normalizeMessageId(messageId)

      return (
        Boolean(normalizedMessageId) &&
        playbackState.activeMessageId === normalizedMessageId &&
        playbackState.playbackStatus === 'loading'
      )
    },
    [playbackState.activeMessageId, playbackState.playbackStatus]
  )

  useEffect(() => {
    /** 当前音频元素实例。 */
    const audioElement = ensureAudioElement()

    /**
     * 处理音频自然播放结束事件。
     *
     * @returns 无返回值
     */
    const handleEnded = () => {
      stopPlayback()
    }

    /**
     * 处理音频元素运行时错误。
     *
     * @returns 无返回值
     */
    const handleError = () => {
      if (playbackStateRef.current.playbackStatus === 'idle') {
        return
      }

      showErrorNotify(ASSISTANT_TTS_PLAYBACK_ERROR_TEXT)
      stopPlayback()
    }

    audioElement.addEventListener('ended', handleEnded)
    audioElement.addEventListener('error', handleError)

    return () => {
      audioElement.removeEventListener('ended', handleEnded)
      audioElement.removeEventListener('error', handleError)
      stopPlayback()
    }
  }, [ensureAudioElement, stopPlayback])

  useEffect(() => {
    if (!playbackState.activeMessageId) {
      return
    }

    /** 当前消息列表里可播放的 messageId 集合。 */
    const availableMessageIds = new Set(messages.map(message => normalizeMessageId(message.messageId)).filter(Boolean))

    if (!availableMessageIds.has(playbackState.activeMessageId)) {
      stopPlayback()
    }
  }, [messages, playbackState.activeMessageId, stopPlayback])

  useEffect(() => {
    if (previousConversationUuidRef.current !== activeConversationUuid && playbackStateRef.current.activeMessageId) {
      stopPlayback()
    }

    previousConversationUuidRef.current = activeConversationUuid
  }, [activeConversationUuid, stopPlayback])

  /** Provider 暴露给下游组件的上下文值。 */
  const contextValue = useMemo<AssistantTtsContextValue>(() => {
    return {
      toggleMessagePlayback,
      isMessagePlaying,
      isMessageLoading,
      stopPlayback
    }
  }, [isMessageLoading, isMessagePlaying, stopPlayback, toggleMessagePlayback])

  return <AssistantTtsContext.Provider value={contextValue}>{children}</AssistantTtsContext.Provider>
}
