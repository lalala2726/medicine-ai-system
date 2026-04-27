import type { RefObject } from 'react'
import MessageContentRenderer from '../MessageContentRenderer'
import HistoryMessageContentRenderer from '../HistoryMessageContentRenderer'
import { AssistantSystemMessage } from '../MessagePrimitives'
import {
  CHAT_MESSAGE_POSITIONS,
  CHAT_MESSAGE_TYPES,
  type MessageContentCallbacks
} from '../../modules/messages/chatTypes'
import { isHistoryRenderMessage, type HistoryRenderMessage } from '../../modules/session/historyMessageAdapters'
import type { ChatMessage } from '../../modules/messages/chatTypes'
import styles from './index.module.less'

/** Assistant 消息列表组件属性。 */
interface AssistantMessageListProps {
  /** 当前消息数组。 */
  messages: ChatMessage[]
  /** 消息滚动容器引用。 */
  containerRef: RefObject<HTMLDivElement>
  /** 消息内容交互回调集合。 */
  callbacks: MessageContentCallbacks
}

/**
 * 计算某条消息在页面中的布局位置。
 *
 * @param message - 页面消息对象
 * @returns 布局位置字符串
 */
const resolveMessagePosition = (message: ChatMessage) => {
  if (message.type === CHAT_MESSAGE_TYPES.SYSTEM) {
    return CHAT_MESSAGE_POSITIONS.CENTER
  }

  return message.position ?? CHAT_MESSAGE_POSITIONS.LEFT
}

/**
 * Assistant 页面自研消息列表。
 *
 * @param props - 组件属性
 * @returns 消息列表节点
 */
const AssistantMessageList = ({ messages, containerRef, callbacks }: AssistantMessageListProps) => {
  if (messages.length === 0) {
    return <div ref={containerRef} className={styles.list} />
  }

  return (
    <div ref={containerRef} className={styles.list}>
      {messages.map(message => {
        const messagePosition = resolveMessagePosition(message)

        if (message.type === CHAT_MESSAGE_TYPES.SYSTEM) {
          return <AssistantSystemMessage key={message._id} text={message.content.text || ''} />
        }

        return (
          <div
            key={message._id}
            className={`${styles.messageRow} ${styles[`messageRow${messagePosition}`]}`}
            data-type={message.type}
          >
            <div className={`${styles.messageBody} ${styles[`messageBody${messagePosition}`]}`}>
              {isHistoryRenderMessage(message) ? (
                <HistoryMessageContentRenderer message={message as HistoryRenderMessage} callbacks={callbacks} />
              ) : (
                <MessageContentRenderer message={message} callbacks={callbacks} />
              )}
            </div>
          </div>
        )
      })}
    </div>
  )
}

export default AssistantMessageList
