import { useEffect, useState } from 'react'
import { showErrorNotify } from '@/utils/notify'
import { useChatStore } from '@/stores/chatStore'
import { ASSISTANT_HISTORY_LOAD_ERROR_TEXT } from '../page/assistantPage.constants'

/** useConversationBootstrap 的输入参数。 */
interface UseConversationBootstrapOptions {
  activeConversationUuid: string | null
  renderedConversationUuid: string | null
  isReplying: boolean
  detachCurrentRequest: () => void
  closeDrawer: () => void
  setIsReplying: (replying: boolean) => void
  loadConversationList: () => Promise<Array<{ conversationUuid: string }>>
  loadHistory: (conversationUuid: string) => Promise<void>
  startNewConversation: () => void
  resetToWelcome: () => void
}

/** useConversationBootstrap 的返回值。 */
export interface ConversationBootstrapResult {
  isBootstrapped: boolean
}

/**
 * 将错误对象转换为启动流程可展示的错误文案。
 */
const resolveBootstrapErrorMessage = (error: unknown) => {
  if (error instanceof Error && error.message.trim()) {
    return error.message
  }

  return ASSISTANT_HISTORY_LOAD_ERROR_TEXT
}

/**
 * 管理 Assistant 页面初始化与会话切换同步。
 */
export function useConversationBootstrap({
  activeConversationUuid,
  renderedConversationUuid,
  isReplying,
  detachCurrentRequest,
  closeDrawer,
  setIsReplying,
  loadConversationList,
  loadHistory,
  startNewConversation,
  resetToWelcome
}: UseConversationBootstrapOptions): ConversationBootstrapResult {
  /** 标记初始化流程是否已经完成。 */
  const [isBootstrapped, setIsBootstrapped] = useState(false)

  /**
   * 页面首次进入时拉取会话列表，并尽量恢复上一次活跃会话。
   */
  useEffect(() => {
    let isCancelled = false

    const bootstrap = async () => {
      try {
        const conversations = await loadConversationList()

        if (isCancelled) {
          return
        }

        const nextConversationUuid = useChatStore.getState().activeConversationUuid
        const hasActiveConversation =
          !!nextConversationUuid && conversations.some(item => item.conversationUuid === nextConversationUuid)

        if (nextConversationUuid && hasActiveConversation) {
          await loadHistory(nextConversationUuid)
        } else {
          startNewConversation()
          resetToWelcome()
        }
      } catch (error) {
        if (!isCancelled) {
          startNewConversation()
          resetToWelcome()
          showErrorNotify(resolveBootstrapErrorMessage(error))
        }
      } finally {
        if (!isCancelled) {
          setIsBootstrapped(true)
        }
      }
    }

    void bootstrap()

    return () => {
      isCancelled = true
      detachCurrentRequest()
      closeDrawer()
      setIsReplying(false)
    }
  }, [
    detachCurrentRequest,
    closeDrawer,
    loadConversationList,
    loadHistory,
    resetToWelcome,
    setIsReplying,
    startNewConversation
  ])

  /**
   * 当活跃会话切换时，同步加载对应历史消息。
   * bootstrap 未完成或正在回复时不会触发切换。
   */
  useEffect(() => {
    if (!isBootstrapped || isReplying) {
      return
    }

    if (!activeConversationUuid) {
      if (renderedConversationUuid !== null) {
        resetToWelcome()
      }

      return
    }

    if (activeConversationUuid === renderedConversationUuid) {
      return
    }

    void loadHistory(activeConversationUuid)
  }, [activeConversationUuid, isBootstrapped, isReplying, loadHistory, renderedConversationUuid, resetToWelcome])

  return {
    isBootstrapped
  }
}
