import { useCallback, useState } from 'react'
import { createClientId } from '@/utils/createClientId'
import type { ChatMessage } from './chatTypes'

/**
 * 构建一条本地消息的稳定唯一标识。
 *
 * @returns 本地消息唯一标识
 */
const createLocalMessageId = (): string => {
  return createClientId()
}

/**
 * 规范化消息对象，确保每条消息都有本地 `_id`。
 *
 * @param message - 原始消息对象
 * @returns 补全 `_id` 后的消息对象
 */
const normalizeMessage = <T extends ChatMessage>(message: T): T => {
  if (message._id?.trim()) {
    return message
  }

  return {
    ...message,
    _id: createLocalMessageId()
  }
}

/** Assistant 页面消息状态 Hook 的返回值。 */
export interface AssistantMessagesControllerResult {
  /** 当前消息列表。 */
  messages: ChatMessage[]
  /** 向消息列表尾部追加一条消息。 */
  appendMessage: (message: ChatMessage) => void
  /** 更新某条指定消息。 */
  updateMessageById: (messageId: string, message: ChatMessage) => void
  /** 删除某条指定消息。 */
  deleteMessageById: (messageId: string) => void
  /** 重置整段消息列表。 */
  resetMessages: (messages?: ChatMessage[]) => void
}

/**
 * 管理 Assistant 页面内的消息数组。
 * 语义对齐原有消息增删改重置能力，但完全由页面自身维护。
 *
 * @param initialMessages - 初始消息数组
 * @returns 消息状态和增删改重置方法
 */
export function useAssistantMessages(initialMessages: ChatMessage[]): AssistantMessagesControllerResult {
  /** 当前页面消息数组。 */
  const [messages, setMessages] = useState<ChatMessage[]>(() => initialMessages.map(normalizeMessage))

  /**
   * 向消息列表尾部追加一条消息。
   *
   * @param message - 待追加的消息对象
   * @returns 无返回值
   */
  const appendMessage = useCallback((message: ChatMessage) => {
    setMessages(currentMessages => [...currentMessages, normalizeMessage(message)])
  }, [])

  /**
   * 根据消息 `_id` 更新指定消息。
   * 若消息不存在，则保持当前列表不变。
   *
   * @param messageId - 目标消息 `_id`
   * @param message - 新的消息对象
   * @returns 无返回值
   */
  const updateMessageById = useCallback((messageId: string, message: ChatMessage) => {
    setMessages(currentMessages =>
      currentMessages.map(currentMessage =>
        currentMessage._id === messageId
          ? normalizeMessage({
              ...message,
              _id: messageId
            })
          : currentMessage
      )
    )
  }, [])

  /**
   * 根据消息 `_id` 删除指定消息。
   *
   * @param messageId - 目标消息 `_id`
   * @returns 无返回值
   */
  const deleteMessageById = useCallback((messageId: string) => {
    setMessages(currentMessages => currentMessages.filter(message => message._id !== messageId))
  }, [])

  /**
   * 重置整段消息列表。
   *
   * @param nextMessages - 新的消息数组
   * @returns 无返回值
   */
  const resetMessages = useCallback((nextMessages: ChatMessage[] = []) => {
    setMessages(nextMessages.map(normalizeMessage))
  }, [])

  return {
    messages,
    appendMessage,
    updateMessageById,
    deleteMessageById,
    resetMessages
  }
}
