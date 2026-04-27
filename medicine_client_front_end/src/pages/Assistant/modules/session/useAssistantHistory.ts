import { useCallback, useRef, useState } from 'react'
import { getAssistantConversationHistory } from '@/api/assistant'
import type { AssistantTypes } from '@/api/assistant/contract'
import { showErrorNotify } from '@/utils/notify'
import { initialMessages } from '@/stores/chatStore'
import type { ChatMessage } from '../messages/chatTypes'
import { ASSISTANT_HISTORY_LOAD_ERROR_TEXT } from '../page/assistantPage.constants'
import { mapHistoryMessageToRenderMessage, type HistoryRenderMessage } from './historyMessageAdapters'

/** useAssistantHistory 的输入参数。 */
interface UseAssistantHistoryOptions {
  assistantAvatar: string
  userAvatar: string
  resetList: (messages?: ChatMessage[]) => void
  setHistoryLoading: (loading: boolean) => void
  setHistoryError: (error: string | null) => void
  startNewConversation: () => void
  loadConversationList: () => Promise<unknown>
}

/** useAssistantHistory 对外暴露的能力。 */
export interface AssistantHistoryControllerResult {
  /** 当前已经渲染完成的会话 UUID。 */
  renderedConversationUuid: string | null
  /** 每次历史成功加载并完成渲染后递增一次的版本号。 */
  historyLoadVersion: number
  setRenderedConversationUuid: (conversationUuid: string | null) => void
  resetToWelcome: () => void
  loadHistory: (conversationUuid: string) => Promise<void>
}

/**
 * 从错误对象中提取可展示的历史加载错误信息。
 */
const resolveHistoryErrorMessage = (error: unknown) => {
  if (error instanceof Error && error.message.trim()) {
    return error.message
  }

  return ASSISTANT_HISTORY_LOAD_ERROR_TEXT
}

/**
 * 管理 Assistant 页面中的历史记录加载、欢迎态重置和竞态保护。
 */
export function useAssistantHistory({
  assistantAvatar,
  userAvatar,
  resetList,
  setHistoryLoading,
  setHistoryError,
  startNewConversation,
  loadConversationList
}: UseAssistantHistoryOptions): AssistantHistoryControllerResult {
  /** 当前已经渲染到页面上的会话 UUID。 */
  const [renderedConversationUuid, setRenderedConversationUuid] = useState<string | null>(null)
  /** 历史消息成功渲染到聊天区后的递增版本号。 */
  const [historyLoadVersion, setHistoryLoadVersion] = useState(0)
  /** 用于规避历史请求竞态的自增 requestId。 */
  const historyRequestIdRef = useRef(0)

  /** 将历史消息转换成页面层历史专用视图模型。 */
  const mapHistoryMessageToChatMessage = useCallback(
    (message: AssistantTypes.HistoryMessage) =>
      mapHistoryMessageToRenderMessage({
        message,
        assistantAvatar,
        userAvatar
      }),
    [assistantAvatar, userAvatar]
  )

  /**
   * 将聊天区恢复到欢迎态，并中断未完成的历史请求。
   */
  const resetToWelcome = useCallback(() => {
    historyRequestIdRef.current += 1
    resetList(initialMessages)
    setRenderedConversationUuid(null)
    setHistoryError(null)
    setHistoryLoading(false)
  }, [resetList, setHistoryError, setHistoryLoading])

  /**
   * 加载某个会话的完整历史消息。
   * 通过 requestId 机制避免切换过快时旧请求覆盖新请求。
   */
  const loadHistory = useCallback(
    async (conversationUuid: string) => {
      const requestId = historyRequestIdRef.current + 1
      historyRequestIdRef.current = requestId

      setHistoryLoading(true)
      setHistoryError(null)

      try {
        const historyMessages = await getAssistantConversationHistory(conversationUuid)

        if (historyRequestIdRef.current !== requestId) {
          return
        }

        const chatMessages = historyMessages
          .map(mapHistoryMessageToChatMessage)
          .filter((message): message is HistoryRenderMessage => message !== null)
        resetList(chatMessages.length > 0 ? chatMessages : initialMessages)
        setRenderedConversationUuid(conversationUuid)
        setHistoryLoadVersion(version => version + 1)
      } catch (error) {
        if (historyRequestIdRef.current !== requestId) {
          return
        }

        const errorMessage = resolveHistoryErrorMessage(error)
        setHistoryError(errorMessage)
        showErrorNotify(errorMessage)
        startNewConversation()
        resetToWelcome()
        void loadConversationList().catch(() => undefined)
      } finally {
        if (historyRequestIdRef.current === requestId) {
          setHistoryLoading(false)
        }
      }
    },
    [
      loadConversationList,
      mapHistoryMessageToChatMessage,
      resetList,
      resetToWelcome,
      setHistoryError,
      setHistoryLoading,
      startNewConversation
    ]
  )

  return {
    renderedConversationUuid,
    historyLoadVersion,
    setRenderedConversationUuid,
    resetToWelcome,
    loadHistory
  }
}
