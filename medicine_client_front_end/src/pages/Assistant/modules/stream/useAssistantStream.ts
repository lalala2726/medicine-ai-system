import { useCallback, useEffect, useRef, useState } from 'react'
import {
  attachAssistantChatStream,
  stopAssistantChat,
  submitAssistantChat,
  ASSISTANT_CARD_TYPES,
  ASSISTANT_MESSAGE_STATUSES,
  ASSISTANT_MESSAGE_TYPES,
  ASSISTANT_ROLES,
  ASSISTANT_STREAM_EVENT_TYPES,
  type AssistantChatSubmitCardPayload,
  type AssistantChatSubmitResult
} from '@/api/assistant'
import { SSEHttpError } from '@/utils/sse'
import type { AssistantTypes } from '@/api/assistant/contract'
import {
  CHAT_MESSAGE_TYPES,
  CHAT_RESPONSE_STATUSES,
  type ChatMessageToolStatusContent,
  type ChatMessage,
  type ChatResponseStatus
} from '../messages/chatTypes'
import { mapAssistantMessageToChatMessage } from '../messages/messageAdapters'
import { createClientId } from '@/utils/createClientId'
import { normalizeServerErrorMessage } from '@/utils/serverError'

/** 会话标题最大字符数。 */
const CONVERSATION_TITLE_MAX_LENGTH = 18

/** 本地 attach 连接中断时的默认提示文案。 */
const ASSISTANT_STREAM_INTERRUPTED_TEXT = '回复连接已中断，请稍后重试'

/** 流式回复被停止时的默认提示文案。 */
const ASSISTANT_STREAM_CANCELLED_TEXT = '已停止生成'

/** 流式回复异常结束时的默认提示文案。 */
const ASSISTANT_STREAM_ERROR_TEXT = '这条回复未完整生成'
/** 工具状态缺失文案时的前端默认提示。 */
const ASSISTANT_TOOL_STATUS_FALLBACK_TEXT = '正在处理中...'
/** 工具状态结束后的等待关闭窗口时长。 */
const ASSISTANT_TOOL_STATUS_IDLE_CLOSE_MS = 8000

/** 开始发送请求的结果类型常量。 */
export const ASSISTANT_REQUEST_START_RESULT_TYPES = {
  /** submit 成功并已开始 attach。 */
  SUBMITTED: 'submitted',
  /** submit 命中 409 冲突并已恢复 attach。 */
  CONFLICT: 'conflict',
  /** 当前页面已有运行中的 attach，请求被拦截。 */
  BLOCKED: 'blocked',
  /** submit 请求失败。 */
  FAILED: 'failed'
} as const

/** 开始发送请求的结果类型联合。 */
export type AssistantRequestStartResultType =
  (typeof ASSISTANT_REQUEST_START_RESULT_TYPES)[keyof typeof ASSISTANT_REQUEST_START_RESULT_TYPES]

/** 页面层开始发送请求的结果。 */
export type AssistantRequestStartResult =
  | {
      /** 结果类型。 */
      type: typeof ASSISTANT_REQUEST_START_RESULT_TYPES.SUBMITTED
      /** 当前会话 UUID。 */
      conversationUuid: string
      /** 当前 AI 消息 UUID。 */
      messageUuid: string
    }
  | {
      /** 结果类型。 */
      type: typeof ASSISTANT_REQUEST_START_RESULT_TYPES.CONFLICT
      /** 当前会话 UUID。 */
      conversationUuid: string
      /** 当前 AI 消息 UUID。 */
      messageUuid: string
      /** 后端返回的冲突提示文案。 */
      message: string
    }
  | {
      /** 结果类型。 */
      type: typeof ASSISTANT_REQUEST_START_RESULT_TYPES.BLOCKED
    }
  | {
      /** 结果类型。 */
      type: typeof ASSISTANT_REQUEST_START_RESULT_TYPES.FAILED
      /** 失败提示文案。 */
      errorMessage: string
    }

/** 停止当前请求的结果。 */
export interface StopCurrentRequestResult {
  /** 当前停止请求是否成功提交。 */
  success: boolean
  /** 停止失败时的提示文案。 */
  errorMessage?: string
}

/** 对当前消息模型进行合并时使用的扩展类型。 */
type AssistantBaseMessage = ChatMessage & Record<string, unknown>

/** 流式回复的页面状态。 */
type ResponseStatus = ChatResponseStatus

/** 流式回复的最终状态。 */
type FinalResponseStatus = Exclude<ChatResponseStatus, typeof CHAT_RESPONSE_STATUSES.STREAMING>

/** Assistant 流式聊天 Hook 的配置项。 */
interface UseAssistantStreamOptions {
  /** AI 助手头像 URL。 */
  assistantAvatar: string
  /** 用户头像 URL。 */
  userAvatar: string
  /** 当前活跃的会话 UUID。 */
  activeConversationUuid: string | null
  /** 页面消息列表追加方法。 */
  appendMsg: (msg: ChatMessage) => void
  /** 页面消息列表更新方法。 */
  updateMsg: (id: string, msg: ChatMessage) => void
  /** 页面消息列表删除方法。 */
  deleteMsg: (id: string) => void
  /** 根据后端消息 ID 查找当前页面上的消息。 */
  findMessageByServerId: (messageId: string) => ChatMessage | undefined
  /** 设置当前活跃会话 UUID。 */
  setActiveConversationUuid: (conversationUuid: string) => void
  /** 新增或更新会话列表中的会话条目。 */
  upsertConversation: (conversation: AssistantTypes.ConversationItem) => void
  /** 设置是否正在回复中。 */
  setIsReplying: (replying: boolean) => void
  /** 会话就绪回调（新会话被后端创建后触发）。 */
  onConversationReady: (conversationUuid: string) => void
  /** 会话列表刷新回调。 */
  onConversationListRefresh: () => Promise<unknown> | void
  /** SSE action 事件回调。 */
  onAction?: (event: AssistantTypes.StreamEvent) => void
}

/** 发起一次 Assistant 消息发送所需的参数。 */
interface SendMessageOptions {
  /** 本次发送的消息类型。 */
  messageType: AssistantTypes.MessageType
  /** 文本消息正文，仅 text 类型需要传递。 */
  content?: string
  /** 本次发送附带的图片 URL 列表。 */
  imageUrls?: string[]
  /** 本次发送附带的卡片消息载荷。 */
  card?: AssistantChatSubmitCardPayload
  /** 可选的卡片点击动作数据。 */
  cardAction?: AssistantTypes.CardActionPayload
  /** 当前轮是否开启深度思考。 */
  reasoningEnabled?: boolean
  /** 可选的用户消息对象（追加到聊天列表展示）。 */
  userMessage?: ChatMessage
  /** 当前会话标题。 */
  conversationTitle?: string
}

/** attach 已存在会话流所需的参数。 */
export interface AttachConversationStreamOptions {
  /** 需要 attach 的会话 UUID。 */
  conversationUuid: string
  /** 当前运行中的 AI 消息 UUID。 */
  messageUuid: string
  /** 当前页面中已存在的消息对象。 */
  existingMessage?: ChatMessage | null
}

/**
 * 截断文本到指定长度，超出时追加后缀。
 *
 * @param text - 原始文本
 * @param maxLength - 最大字符数
 * @param suffix - 超出时追加的后缀
 * @returns 归一化并截断后的文本
 */
const truncateText = (text: string, maxLength: number, suffix = '...') => {
  const normalizedText = text.trim().replace(/\s+/g, ' ')

  return normalizedText.length > maxLength ? `${normalizedText.slice(0, maxLength)}${suffix}` : normalizedText
}

/**
 * 根据本次发送内容生成会话标题。
 *
 * @param options - 当前发送选项
 * @returns 适合作为会话标题的文本
 */
const resolveConversationTitle = ({
  messageType,
  content,
  imageUrls,
  card,
  userMessage,
  conversationTitle
}: SendMessageOptions) => {
  if (conversationTitle?.trim()) {
    return truncateText(conversationTitle, CONVERSATION_TITLE_MAX_LENGTH)
  }

  if (messageType === ASSISTANT_MESSAGE_TYPES.TEXT) {
    const normalizedContent = content?.trim() || ''
    if (normalizedContent) {
      return truncateText(normalizedContent, CONVERSATION_TITLE_MAX_LENGTH)
    }

    if ((imageUrls || []).length > 0) {
      return '图片咨询'
    }

    return '新对话'
  }

  if (card?.type === ASSISTANT_CARD_TYPES.ORDER_CARD) {
    const previewProductName = userMessage?.content.orderCard?.previewProduct.productName?.trim() || ''
    const orderCardTitle = previewProductName ? `订单咨询 ${previewProductName}` : '订单咨询'
    return truncateText(orderCardTitle, CONVERSATION_TITLE_MAX_LENGTH)
  }

  if (card?.type === ASSISTANT_CARD_TYPES.AFTER_SALE_CARD) {
    const productName = userMessage?.content.afterSaleCard?.productInfo.productName?.trim() || ''
    const afterSaleCardTitle = productName ? `售后咨询 ${productName}` : '售后咨询'
    return truncateText(afterSaleCardTitle, CONVERSATION_TITLE_MAX_LENGTH)
  }

  if (card?.type === ASSISTANT_CARD_TYPES.PATIENT_CARD) {
    const patientName = userMessage?.content.patientCard?.name?.trim() || ''
    const patientCardTitle = patientName ? `问诊资料 ${patientName}` : '问诊资料'
    return truncateText(patientCardTitle, CONVERSATION_TITLE_MAX_LENGTH)
  }

  return '新对话'
}

/**
 * 判断当前事件是否为 replace 覆盖事件。
 *
 * @param event - 当前 SSE 事件
 * @returns 当前事件是否应该覆盖已有内容
 */
const isReplaceStreamEvent = (event: AssistantTypes.StreamEvent) => {
  return event.meta?.replace === true || event.content?.state === 'replace'
}

/**
 * 将后端最终状态转换为页面层响应状态。
 *
 * @param rawState - 后端返回的最终状态
 * @returns 页面层最终状态，无法识别时返回 undefined
 */
const resolveStatusFromRawState = (rawState?: string | null): FinalResponseStatus | undefined => {
  if (rawState === ASSISTANT_MESSAGE_STATUSES.CANCELLED) {
    return CHAT_RESPONSE_STATUSES.CANCELLED
  }

  if (rawState === ASSISTANT_MESSAGE_STATUSES.ERROR) {
    return CHAT_RESPONSE_STATUSES.ERROR
  }

  if (rawState === ASSISTANT_MESSAGE_STATUSES.SUCCESS) {
    return CHAT_RESPONSE_STATUSES.SUCCESS
  }

  return undefined
}

/**
 * 解析流式结束事件对应的最终状态。
 *
 * @param event - 当前 SSE 事件
 * @returns 规范化后的最终状态
 */
const resolveFinalResponseStatus = (event: AssistantTypes.StreamEvent): FinalResponseStatus => {
  const statusFromMeta = resolveStatusFromRawState(event.meta?.runStatus)

  if (statusFromMeta) {
    return statusFromMeta
  }

  const statusFromContent = resolveStatusFromRawState(event.content?.state)

  return statusFromContent ?? CHAT_RESPONSE_STATUSES.SUCCESS
}

/**
 * 根据最终状态生成可展示的状态文案。
 *
 * @param status - 最终状态
 * @param backendMessage - 后端事件中附带的提示文案
 * @returns 页面层使用的状态文案
 */
const resolveFinalStatusText = (status: FinalResponseStatus, backendMessage?: string | null) => {
  const normalizedBackendMessage = typeof backendMessage === 'string' && backendMessage.trim() ? backendMessage : ''

  if (status === CHAT_RESPONSE_STATUSES.CANCELLED) {
    return normalizedBackendMessage || ASSISTANT_STREAM_CANCELLED_TEXT
  }

  if (status === CHAT_RESPONSE_STATUSES.ERROR) {
    return normalizedBackendMessage || ASSISTANT_STREAM_ERROR_TEXT
  }

  if (status === CHAT_RESPONSE_STATUSES.INTERRUPTED) {
    return normalizedBackendMessage || ASSISTANT_STREAM_INTERRUPTED_TEXT
  }

  return undefined
}

/**
 * 解析任意错误对象上的可展示提示文案。
 *
 * @param error - 原始错误对象
 * @param fallbackMessage - 默认兜底文案
 * @returns 可展示的错误文案
 */
const resolveErrorMessage = (error: unknown, fallbackMessage: string) => {
  if (error instanceof Error && error.message.trim()) {
    return normalizeServerErrorMessage(error.message, fallbackMessage)
  }

  return normalizeServerErrorMessage(fallbackMessage, fallbackMessage)
}

/**
 * Assistant SSE 流式聊天核心 Hook。
 * 负责维护 submit / attach / stop 生命周期、增量文本拼接、快照 replace 与本地状态结算。
 *
 * @param options - Hook 配置项
 * @returns 发送、attach、停止与断开连接的方法
 */
export function useAssistantStream({
  assistantAvatar,
  userAvatar,
  activeConversationUuid,
  appendMsg,
  updateMsg,
  deleteMsg,
  findMessageByServerId,
  setActiveConversationUuid,
  upsertConversation,
  setIsReplying,
  onConversationReady,
  onConversationListRefresh,
  onAction
}: UseAssistantStreamOptions) {
  /** 当前正在运行的 SSE attach 控制器。 */
  const currentRequestRef = useRef<AbortController | null>(null)
  /** 当前流所属会话 UUID。 */
  const currentConversationUuidRef = useRef('')
  /** 当前 AI 回复消息在页面消息列表中的本地 ID。 */
  const currentMsgIdRef = useRef('')
  /** 当前流式累积的回答文本。 */
  const currentTextRef = useRef('')
  /** 当前流式累积的思考文本。 */
  const currentThinkingRef = useRef('')
  /** 当前流式展示中的工具运行状态。 */
  const currentToolStatusRef = useRef<ChatMessageToolStatusContent | undefined>(undefined)
  /** 当前工具状态清理延迟定时器。 */
  const toolStatusClearTimerRef = useRef<number | null>(null)
  /** 后端下发的 AI 消息 UUID。 */
  const assistantMessageUuidRef = useRef<string | undefined>(undefined)
  /** 当前正在更新的基准消息。 */
  const currentBaseMessageRef = useRef<AssistantBaseMessage | null>(null)
  /** 当前临时回显的用户消息本地 ID。 */
  const optimisticUserMessageIdRef = useRef<string | null>(null)
  /** 等待刷入聊天列表的持久化消息缓冲队列。 */
  const pendingMessagesRef = useRef<AssistantTypes.Message[]>([])
  /** 已入队的持久化消息 ID 集合，用于去重。 */
  const pendingMessageIdsRef = useRef<Set<string>>(new Set())
  /** 标记本次流式请求是否已经结算。 */
  const hasSettledRef = useRef(false)
  /** 标记当前关闭是否由本地主动 detach 触发。 */
  const detachAbortRef = useRef(false)
  /** 标记当前是否已经提交 stop 请求。 */
  const stopRequestedRef = useRef(false)
  /** 当前是否正在提交 stop 请求。 */
  const [isStopping, setIsStopping] = useState(false)

  /**
   * 清空待刷入的持久化消息缓冲队列。
   *
   * @returns 无返回值
   */
  const resetPendingMessages = useCallback(() => {
    pendingMessagesRef.current = []
    pendingMessageIdsRef.current.clear()
  }, [])

  /**
   * 清理当前挂起的工具状态延迟清除定时器。
   *
   * @returns 无返回值
   */
  const clearPendingToolStatusTimer = useCallback(() => {
    if (toolStatusClearTimerRef.current === null) {
      return
    }

    window.clearTimeout(toolStatusClearTimerRef.current)
    toolStatusClearTimerRef.current = null
  }, [])

  useEffect(() => {
    return () => {
      clearPendingToolStatusTimer()
    }
  }, [clearPendingToolStatusTimer])

  /**
   * 清理本轮流式请求的运行时引用。
   *
   * @returns 无返回值
   */
  const resetStreamRuntimeRefs = useCallback(() => {
    clearPendingToolStatusTimer()
    currentRequestRef.current = null
    currentConversationUuidRef.current = ''
    currentMsgIdRef.current = ''
    currentTextRef.current = ''
    currentThinkingRef.current = ''
    currentToolStatusRef.current = undefined
    assistantMessageUuidRef.current = undefined
    currentBaseMessageRef.current = null
    optimisticUserMessageIdRef.current = null
    stopRequestedRef.current = false
  }, [clearPendingToolStatusTimer])

  /**
   * 回滚当前轮次临时回显的用户消息。
   *
   * @returns 无返回值
   */
  const rollbackOptimisticUserMessage = useCallback(() => {
    const optimisticMessageId = optimisticUserMessageIdRef.current

    if (!optimisticMessageId) {
      return
    }

    deleteMsg(optimisticMessageId)
    optimisticUserMessageIdRef.current = null
  }, [deleteMsg])

  /**
   * 构建当前 AI 回复的视图模型。
   *
   * @param status - 响应状态
   * @param statusText - 状态描述文本
   * @returns ChatMessage 对象
   */
  const buildAssistantMessage = useCallback(
    (status: ResponseStatus, statusText?: string): ChatMessage => {
      /** 当前消息的基准快照。 */
      const baseMessage = currentBaseMessageRef.current

      return {
        ...(baseMessage ?? {}),
        _id: currentMsgIdRef.current,
        messageId: assistantMessageUuidRef.current,
        type: CHAT_MESSAGE_TYPES.TEXT,
        content: {
          ...(baseMessage?.content ?? {}),
          text: currentTextRef.current,
          thinking: currentThinkingRef.current || undefined,
          thinkingDone: status !== CHAT_RESPONSE_STATUSES.STREAMING,
          responseStatus: status,
          responseStatusText: statusText,
          toolStatus: currentToolStatusRef.current
        },
        user: baseMessage?.user ?? { avatar: assistantAvatar }
      } as ChatMessage
    },
    [assistantAvatar]
  )

  /**
   * 构建当前轮次的 typing 占位消息。
   *
   * @returns typing 占位消息对象
   */
  const buildAssistantTypingMessage = useCallback((): ChatMessage => {
    /** 当前消息的基准快照。 */
    const baseMessage = currentBaseMessageRef.current

    return {
      ...(baseMessage ?? {}),
      _id: currentMsgIdRef.current,
      messageId: assistantMessageUuidRef.current,
      type: CHAT_MESSAGE_TYPES.TYPING,
      content: {},
      user: baseMessage?.user ?? { avatar: assistantAvatar }
    } as ChatMessage
  }, [assistantAvatar])

  /**
   * 判断当前 AI 回复是否已有可渲染的正文、思考或工具状态。
   *
   * @returns 当前是否具备可渲染内容
   */
  const hasRenderableAssistantMessage = useCallback(() => {
    return Boolean(
      currentTextRef.current.trim() ||
        currentThinkingRef.current.trim() ||
        (currentToolStatusRef.current?.text ?? '').trim()
    )
  }, [])

  /**
   * 更新聊天列表中当前 AI 回复消息的状态。
   *
   * @param status - 响应状态
   * @param statusText - 状态描述文本
   * @returns 无返回值
   */
  const syncAssistantMessage = useCallback(
    (status: ResponseStatus, statusText?: string) => {
      updateMsg(currentMsgIdRef.current, buildAssistantMessage(status, statusText))
    },
    [buildAssistantMessage, updateMsg]
  )

  /**
   * 根据当前流式内容自动在 typing 占位与文本消息之间切换。
   *
   * @returns 无返回值
   */
  const syncAssistantStreamingMessage = useCallback(() => {
    if (hasRenderableAssistantMessage()) {
      syncAssistantMessage(CHAT_RESPONSE_STATUSES.STREAMING)
      return
    }

    updateMsg(currentMsgIdRef.current, buildAssistantTypingMessage())
  }, [buildAssistantTypingMessage, hasRenderableAssistantMessage, syncAssistantMessage, updateMsg])

  /**
   * 检查当前流式回复是否已经生成了任何可展示内容。
   *
   * @returns 当前是否已有可展示正文或思考内容
   */
  const hasAssistantStreamContent = useCallback(() => {
    return Boolean(currentTextRef.current.trim() || currentThinkingRef.current.trim())
  }, [])

  /**
   * 将工具事件中的文案解析为可展示的运行中状态。
   *
   * @param event - 当前工具调用事件
   * @returns 规范化后的工具状态
   */
  const resolveRunningToolStatus = useCallback((event: AssistantTypes.StreamEvent): ChatMessageToolStatusContent => {
    /** 当前工具事件携带的原始提示文案。 */
    const rawMessage = event.content?.message?.trim()

    return {
      text: rawMessage || ASSISTANT_TOOL_STATUS_FALLBACK_TEXT,
      phase: 'running'
    }
  }, [])

  /**
   * 应用当前运行中的工具状态，并取消已有的延迟关闭任务。
   *
   * @param toolStatus - 当前即将展示的工具状态
   * @returns 无返回值
   */
  const applyRunningToolStatus = useCallback(
    (toolStatus: ChatMessageToolStatusContent) => {
      clearPendingToolStatusTimer()
      currentToolStatusRef.current = toolStatus
    },
    [clearPendingToolStatusTimer]
  )

  /**
   * 立即清空当前工具状态。
   *
   * @param options - 清理附加选项
   * @returns 无返回值
   */
  const clearToolStatusImmediately = useCallback(
    (options?: { syncAfterClear?: boolean; onCleared?: () => void }) => {
      const { syncAfterClear = true, onCleared } = options || {}

      clearPendingToolStatusTimer()
      if (!currentToolStatusRef.current) {
        onCleared?.()
        return
      }

      currentToolStatusRef.current = undefined

      if (syncAfterClear) {
        syncAssistantStreamingMessage()
      }

      onCleared?.()
    },
    [clearPendingToolStatusTimer, syncAssistantStreamingMessage]
  )

  /**
   * 在工具结束后启动 5 秒等待窗口，超时后自动关闭工具状态条。
   *
   * @returns 无返回值
   */
  const scheduleToolStatusClose = useCallback(() => {
    if (!currentToolStatusRef.current) {
      return
    }

    clearPendingToolStatusTimer()
    toolStatusClearTimerRef.current = window.setTimeout(() => {
      toolStatusClearTimerRef.current = null
      currentToolStatusRef.current = undefined
      syncAssistantStreamingMessage()
    }, ASSISTANT_TOOL_STATUS_IDLE_CLOSE_MS)
  }, [clearPendingToolStatusTimer, syncAssistantStreamingMessage])

  /**
   * 将后端持久化消息加入缓冲队列，并自动跳过当前占位消息和已存在消息。
   *
   * @param messages - 持久化消息数组
   * @returns 无返回值
   */
  const queueAssistantMessages = useCallback(
    (messages: AssistantTypes.Message[]) => {
      messages.forEach(message => {
        if (message.role === ASSISTANT_ROLES.USER) {
          return
        }

        if (
          message.messageType === ASSISTANT_MESSAGE_TYPES.TEXT &&
          message.id === assistantMessageUuidRef.current &&
          currentMsgIdRef.current
        ) {
          return
        }

        if (pendingMessageIdsRef.current.has(message.id) || findMessageByServerId(message.id)) {
          return
        }

        pendingMessageIdsRef.current.add(message.id)
        pendingMessagesRef.current.push(message)
      })
    },
    [findMessageByServerId]
  )

  /**
   * 将缓冲队列中的持久化消息逐条追加到聊天列表。
   *
   * @returns 无返回值
   */
  const flushPendingMessages = useCallback(() => {
    pendingMessagesRef.current.forEach(message => {
      const chatMessage = mapAssistantMessageToChatMessage({
        message,
        assistantAvatar,
        userAvatar
      })

      if (chatMessage && !findMessageByServerId(message.id)) {
        appendMsg(chatMessage)
      }
    })

    resetPendingMessages()
  }, [appendMsg, assistantAvatar, findMessageByServerId, resetPendingMessages, userAvatar])

  /**
   * 将单条可渲染的 Assistant 消息立即追加到页面列表。
   *
   * @param message - 已规范化的 Assistant 消息
   * @returns 无返回值
   */
  const appendAssistantMessageImmediately = useCallback(
    (message: AssistantTypes.Message) => {
      if (findMessageByServerId(message.id)) {
        return
      }

      const chatMessage = mapAssistantMessageToChatMessage({
        message,
        assistantAvatar,
        userAvatar
      })

      if (!chatMessage) {
        return
      }

      appendMsg(chatMessage)
    },
    [appendMsg, assistantAvatar, findMessageByServerId, userAvatar]
  )

  /**
   * 结算当前流式请求，并根据最终状态刷新 UI。
   *
   * @param status - 最终结算状态
   * @param statusText - 状态描述文本
   * @returns 无返回值
   */
  const settleStream = useCallback(
    (status: FinalResponseStatus, statusText?: string) => {
      if (hasSettledRef.current) {
        return
      }

      hasSettledRef.current = true
      currentRequestRef.current = null

      const finalizeSettledStream = () => {
        setIsReplying(false)
        setIsStopping(false)

        const shouldRefreshConversationList = status !== CHAT_RESPONSE_STATUSES.INTERRUPTED

        if (status === CHAT_RESPONSE_STATUSES.SUCCESS) {
          if (hasAssistantStreamContent()) {
            syncAssistantMessage(CHAT_RESPONSE_STATUSES.SUCCESS, statusText)
          } else {
            deleteMsg(currentMsgIdRef.current)
          }

          // Delay card flush to wait for Streamdown typing animation to finish.
          // Note: When the stream settles, most text is already animated. We only await the final chunk.
          const hasPendingCards = pendingMessagesRef.current.length > 0
          const textLength = currentTextRef.current.length
          const estimatedDelay = hasPendingCards && textLength > 0 ? Math.min(Math.max(textLength * 5, 150), 300) : 0

          if (estimatedDelay > 0) {
            window.setTimeout(() => {
              flushPendingMessages()
            }, estimatedDelay)
          } else {
            flushPendingMessages()
          }

          if (shouldRefreshConversationList) {
            void Promise.resolve(onConversationListRefresh()).catch(() => undefined)
          }

          resetStreamRuntimeRefs()
          return
        }

        resetPendingMessages()
        syncAssistantMessage(status, statusText)

        if (shouldRefreshConversationList) {
          void Promise.resolve(onConversationListRefresh()).catch(() => undefined)
        }

        resetStreamRuntimeRefs()
      }

      clearToolStatusImmediately({
        syncAfterClear: false
      })

      finalizeSettledStream()
    },
    [
      clearToolStatusImmediately,
      deleteMsg,
      flushPendingMessages,
      hasAssistantStreamContent,
      onConversationListRefresh,
      resetPendingMessages,
      resetStreamRuntimeRefs,
      setIsReplying,
      setIsStopping,
      syncAssistantMessage
    ]
  )

  /**
   * 处理 SSE `notice` 事件，提取会话与消息元数据。
   *
   * @param event - SSE 事件对象
   * @returns 无返回值
   */
  const handleNoticeEvent = useCallback(
    (event: AssistantTypes.StreamEvent) => {
      if (event.meta?.messageUuid) {
        assistantMessageUuidRef.current = event.meta.messageUuid
      }

      if (event.meta?.conversationUuid) {
        currentConversationUuidRef.current = event.meta.conversationUuid
        setActiveConversationUuid(event.meta.conversationUuid)
      }
    },
    [setActiveConversationUuid]
  )

  /**
   * 处理 SSE 持久化消息事件，并将消息加入缓冲队列。
   *
   * @param event - SSE 事件对象
   * @returns 无返回值
   */
  const handlePersistentMessageEvent = useCallback(
    (event: AssistantTypes.StreamEvent) => {
      const messages = event.messages?.length ? event.messages : event.message ? [event.message] : []

      if (messages.length === 0) {
        return
      }

      queueAssistantMessages(messages)
    },
    [queueAssistantMessages]
  )

  /**
   * 处理 SSE action 事件。
   *
   * @param event - SSE 事件对象
   * @returns 无返回值
   */
  const handleActionEvent = useCallback(
    (event: AssistantTypes.StreamEvent) => {
      /** 补齐 action 事件所属的会话与消息标识，供页面层做一次性消费去重。 */
      const normalizedActionEvent: AssistantTypes.StreamEvent = {
        ...event,
        meta: {
          ...event.meta,
          conversationUuid: event.meta?.conversationUuid ?? currentConversationUuidRef.current ?? undefined,
          messageUuid: event.meta?.messageUuid ?? assistantMessageUuidRef.current ?? undefined
        }
      }

      onAction?.(normalizedActionEvent)
    },
    [onAction]
  )

  /**
   * 处理 SSE 独立卡片事件，并转成持久化消息缓冲到队列中。
   *
   * @param event - SSE 事件对象
   * @returns 无返回值
   */
  const handleCardEvent = useCallback(
    (event: AssistantTypes.StreamEvent) => {
      if (!event.card?.type) {
        return
      }

      const sourceMessageId = event.meta?.messageUuid?.trim() || assistantMessageUuidRef.current?.trim()
      const cardUuid = event.meta?.cardUuid?.trim()
      const isInteractiveCard =
        event.card.type === ASSISTANT_CARD_TYPES.CONSENT_CARD ||
        event.card.type === ASSISTANT_CARD_TYPES.CONSULTATION_QUESTIONNAIRE_CARD ||
        event.card.type === ASSISTANT_CARD_TYPES.SELECTION_CARD

      if (isInteractiveCard && (!sourceMessageId || !cardUuid)) {
        console.error('Interactive assistant card is missing required identifiers', {
          messageId: sourceMessageId,
          cardUuid,
          cardType: event.card.type,
          event
        })
      }

      /** 独立 card 事件优先使用后端 card_uuid 作为稳定消息 ID。 */
      const cardMessageId = cardUuid ? `card-${cardUuid}` : `card-${createClientId()}`

      appendAssistantMessageImmediately({
        id: cardMessageId,
        sourceMessageId,
        cardUuid,
        role: ASSISTANT_ROLES.AI,
        messageType: ASSISTANT_MESSAGE_TYPES.CARD,
        card: event.card
      })
    },
    [appendAssistantMessageImmediately]
  )

  /**
   * 处理 SSE function_call 工具状态事件。
   *
   * @param event - SSE 事件对象
   * @returns 无返回值
   */
  const handleFunctionCallEvent = useCallback(
    (event: AssistantTypes.StreamEvent) => {
      /** 当前工具事件状态。 */
      const toolState = event.content?.state
      /** 当前是否已有工具状态显示。 */
      const hasToolStatus = Boolean(currentToolStatusRef.current)

      if (toolState === 'start' || toolState === 'timely') {
        applyRunningToolStatus(resolveRunningToolStatus(event))
        syncAssistantStreamingMessage()
        return
      }

      if (toolState === 'end' && hasToolStatus) {
        scheduleToolStatusClose()
      }
    },
    [applyRunningToolStatus, resolveRunningToolStatus, scheduleToolStatusClose, syncAssistantStreamingMessage]
  )

  /**
   * 处理 answer / thinking 文本事件。
   *
   * @param event - SSE 事件对象
   * @param target - 当前需要更新的字段类型
   * @returns 无返回值
   */
  const handleTextStreamEvent = useCallback(
    (event: AssistantTypes.StreamEvent, target: 'text' | 'thinking') => {
      /** 当前事件是否应覆盖已有内容。 */
      const shouldReplace = isReplaceStreamEvent(event)
      /** 当前事件携带的文本内容。 */
      const eventText = event.content?.text ?? ''
      /** 当前文本事件是否会让正文进入页面。 */
      const shouldHideToolStatus =
        target === 'text' && Boolean(currentToolStatusRef.current) && Boolean(shouldReplace || eventText)

      if (shouldHideToolStatus) {
        clearToolStatusImmediately({
          syncAfterClear: false
        })
      }

      if (target === 'text') {
        currentTextRef.current = shouldReplace ? eventText : `${currentTextRef.current}${eventText}`
      } else {
        currentThinkingRef.current = shouldReplace ? eventText : `${currentThinkingRef.current}${eventText}`
      }

      if (shouldReplace || eventText) {
        syncAssistantStreamingMessage()
      }
    },
    [clearToolStatusImmediately, syncAssistantStreamingMessage]
  )

  /**
   * 启动当前会话的流式 attach 连接。
   *
   * @param options - attach 所需的会话参数
   * @returns 当前是否已成功启动 attach
   */
  const attachConversationStream = useCallback(
    ({ conversationUuid, messageUuid, existingMessage }: AttachConversationStreamOptions) => {
      if (currentRequestRef.current) {
        return false
      }

      /** 规整后的会话 UUID。 */
      const normalizedConversationUuid = conversationUuid.trim()
      /** 规整后的消息 UUID。 */
      const normalizedMessageUuid = messageUuid.trim()
      /** 当前页面中与目标消息对应的现有消息。 */
      const resolvedExistingMessage = existingMessage ?? findMessageByServerId(normalizedMessageUuid) ?? null

      if (!normalizedConversationUuid || !normalizedMessageUuid) {
        return false
      }

      currentConversationUuidRef.current = normalizedConversationUuid
      assistantMessageUuidRef.current = normalizedMessageUuid
      currentBaseMessageRef.current = resolvedExistingMessage as AssistantBaseMessage | null
      currentMsgIdRef.current = resolvedExistingMessage?._id?.trim() || normalizedMessageUuid
      currentTextRef.current = resolvedExistingMessage?.content.text ?? ''
      currentThinkingRef.current = resolvedExistingMessage?.content.thinking ?? ''
      currentToolStatusRef.current = resolvedExistingMessage?.content.toolStatus
      hasSettledRef.current = false
      detachAbortRef.current = false
      stopRequestedRef.current = false
      setIsStopping(false)
      setIsReplying(true)
      setActiveConversationUuid(normalizedConversationUuid)
      resetPendingMessages()

      if (resolvedExistingMessage?._id) {
        syncAssistantMessage(CHAT_RESPONSE_STATUSES.STREAMING)
      } else {
        appendMsg({
          _id: normalizedMessageUuid,
          messageId: normalizedMessageUuid,
          type: CHAT_MESSAGE_TYPES.TYPING,
          content: {},
          user: { avatar: assistantAvatar }
        })
      }

      currentRequestRef.current = attachAssistantChatStream({
        conversationUuid: normalizedConversationUuid,
        onEvent: event => {
          handlePersistentMessageEvent(event)

          switch (event.type) {
            case ASSISTANT_STREAM_EVENT_TYPES.NOTICE:
              handleNoticeEvent(event)
              break
            case ASSISTANT_STREAM_EVENT_TYPES.THINKING:
              handleTextStreamEvent(event, 'thinking')
              break
            case ASSISTANT_STREAM_EVENT_TYPES.ANSWER:
              handleTextStreamEvent(event, 'text')
              break
            case ASSISTANT_STREAM_EVENT_TYPES.FUNCTION_CALL:
              handleFunctionCallEvent(event)
              break
            case ASSISTANT_STREAM_EVENT_TYPES.ACTION:
              handleActionEvent(event)
              break
            case ASSISTANT_STREAM_EVENT_TYPES.CARD:
              handleCardEvent(event)
              break
            default:
              break
          }

          if (event.isEnd) {
            const finalStatus = resolveFinalResponseStatus(event)
            settleStream(finalStatus, resolveFinalStatusText(finalStatus, event.content?.message))
          }
        },
        onClose: () => {
          if (detachAbortRef.current || hasSettledRef.current) {
            return
          }

          settleStream(CHAT_RESPONSE_STATUSES.INTERRUPTED, resolveFinalStatusText(CHAT_RESPONSE_STATUSES.INTERRUPTED))
        },
        onError: error => {
          if (detachAbortRef.current || hasSettledRef.current) {
            return
          }

          // 404 表示后端已无 active run，该回复已经完成。
          // 以 SUCCESS 结算以避免“连接中断”的错误提示。
          if (error instanceof SSEHttpError && error.status === 404) {
            settleStream(CHAT_RESPONSE_STATUSES.SUCCESS)
            return
          }

          settleStream(
            CHAT_RESPONSE_STATUSES.INTERRUPTED,
            resolveErrorMessage(error, ASSISTANT_STREAM_INTERRUPTED_TEXT)
          )
        }
      })

      return true
    },
    [
      appendMsg,
      assistantAvatar,
      findMessageByServerId,
      handleActionEvent,
      handleCardEvent,
      handleFunctionCallEvent,
      handleNoticeEvent,
      handlePersistentMessageEvent,
      handleTextStreamEvent,
      resetPendingMessages,
      setActiveConversationUuid,
      setIsReplying,
      settleStream,
      syncAssistantMessage
    ]
  )

  /**
   * 发起一轮新的 Assistant submit 请求，并在成功后自动 attach 到 stream。
   *
   * @param options - 发送选项
   * @returns 当前轮次的启动结果
   */
  const sendMessage = useCallback(
    async ({
      messageType,
      content,
      imageUrls,
      card,
      cardAction,
      reasoningEnabled,
      userMessage,
      conversationTitle
    }: SendMessageOptions): Promise<AssistantRequestStartResult> => {
      if (currentRequestRef.current) {
        return {
          type: ASSISTANT_REQUEST_START_RESULT_TYPES.BLOCKED
        }
      }

      /** 当前用户消息的本地回显对象。 */
      const optimisticUserMessage = userMessage
        ? {
            ...userMessage,
            _id: userMessage._id?.trim() || createClientId()
          }
        : null

      if (optimisticUserMessage?._id) {
        optimisticUserMessageIdRef.current = optimisticUserMessage._id
        appendMsg(optimisticUserMessage)
      }

      try {
        /** submit 接口返回结果。 */
        const submitResult = await submitAssistantChat({
          messageType,
          content,
          imageUrls,
          card,
          conversationUuid: activeConversationUuid,
          cardAction,
          reasoningEnabled
        })

        if (submitResult.type === 'conflict') {
          rollbackOptimisticUserMessage()

          const started = attachConversationStream({
            conversationUuid: submitResult.conversationUuid,
            messageUuid: submitResult.messageUuid,
            existingMessage: findMessageByServerId(submitResult.messageUuid) ?? null
          })

          if (!started) {
            return {
              type: ASSISTANT_REQUEST_START_RESULT_TYPES.FAILED,
              errorMessage: submitResult.message
            }
          }

          return {
            type: ASSISTANT_REQUEST_START_RESULT_TYPES.CONFLICT,
            conversationUuid: submitResult.conversationUuid,
            messageUuid: submitResult.messageUuid,
            message: submitResult.message
          }
        }

        const submittedResult = submitResult as Extract<AssistantChatSubmitResult, { type: 'submitted' }>
        /** 当前会话标题。 */
        const resolvedConversationTitle = resolveConversationTitle({
          messageType,
          content,
          imageUrls,
          card,
          cardAction,
          userMessage,
          conversationTitle
        })

        setActiveConversationUuid(submittedResult.conversationUuid)
        upsertConversation({
          conversationUuid: submittedResult.conversationUuid,
          title: resolvedConversationTitle || '新对话'
        })
        onConversationReady(submittedResult.conversationUuid)

        const started = attachConversationStream({
          conversationUuid: submittedResult.conversationUuid,
          messageUuid: submittedResult.messageUuid
        })

        if (!started) {
          return {
            type: ASSISTANT_REQUEST_START_RESULT_TYPES.FAILED,
            errorMessage: ASSISTANT_STREAM_INTERRUPTED_TEXT
          }
        }

        return {
          type: ASSISTANT_REQUEST_START_RESULT_TYPES.SUBMITTED,
          conversationUuid: submittedResult.conversationUuid,
          messageUuid: submittedResult.messageUuid
        }
      } catch (error) {
        rollbackOptimisticUserMessage()

        return {
          type: ASSISTANT_REQUEST_START_RESULT_TYPES.FAILED,
          errorMessage: resolveErrorMessage(error, '发送消息失败，请稍后重试')
        }
      }
    },
    [
      activeConversationUuid,
      appendMsg,
      attachConversationStream,
      findMessageByServerId,
      onConversationReady,
      rollbackOptimisticUserMessage,
      setActiveConversationUuid,
      upsertConversation
    ]
  )

  /**
   * 提交 stop 请求，并等待当前 attach 流的结束事件完成最终结算。
   *
   * @returns stop 请求是否已成功提交
   */
  const stopCurrentRequest = useCallback(async (): Promise<StopCurrentRequestResult> => {
    if (!currentRequestRef.current || !currentConversationUuidRef.current.trim() || isStopping) {
      return {
        success: false,
        errorMessage: '当前没有可停止的回复'
      }
    }

    try {
      setIsStopping(true)
      stopRequestedRef.current = true
      await stopAssistantChat({
        conversationUuid: currentConversationUuidRef.current
      })

      detachAbortRef.current = true
      currentRequestRef.current.abort()
      settleStream(CHAT_RESPONSE_STATUSES.CANCELLED, ASSISTANT_STREAM_CANCELLED_TEXT)

      return {
        success: true
      }
    } catch (error) {
      stopRequestedRef.current = false
      setIsStopping(false)

      return {
        success: false,
        errorMessage: resolveErrorMessage(error, '停止回复失败，请稍后重试')
      }
    }
  }, [isStopping, settleStream])

  /**
   * 断开当前 attach 连接，但不向后端发送 stop。
   * 该方法只用于页面卸载或本地清理场景。
   *
   * @returns 无返回值
   */
  const detachCurrentRequest = useCallback(() => {
    if (!currentRequestRef.current) {
      return
    }

    detachAbortRef.current = true
    currentRequestRef.current.abort()
    setIsReplying(false)
    setIsStopping(false)
    resetPendingMessages()
    resetStreamRuntimeRefs()
  }, [resetPendingMessages, resetStreamRuntimeRefs, setIsReplying])

  /**
   * 组件卸载时自动断开尚未结束的 attach 连接。
   *
   * @returns 清理函数
   */
  useEffect(() => {
    return () => {
      detachCurrentRequest()
    }
  }, [detachCurrentRequest])

  return {
    sendMessage,
    attachConversationStream,
    stopCurrentRequest,
    detachCurrentRequest,
    isStopping
  }
}
