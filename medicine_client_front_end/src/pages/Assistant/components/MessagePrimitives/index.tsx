import { ChevronDown, ChevronUp } from 'lucide-react'
import { useEffect, useRef, useState, type ReactNode } from 'react'
import styles from './index.module.less'

/** Assistant 气泡类型。 */
type AssistantBubbleVariant = 'text' | 'image'

/** Assistant 气泡语义类型。 */
type AssistantBubbleTone = 'assistant' | 'user'

/** Assistant 通用消息气泡组件属性。 */
interface AssistantBubbleProps {
  /** 气泡内容节点。 */
  children: ReactNode
  /** 气泡类型。 */
  variant?: AssistantBubbleVariant
  /** 气泡语义类型。 */
  tone?: AssistantBubbleTone
  /** 附加样式类名。 */
  className?: string
}

/** Assistant 思考区组件属性。 */
interface AssistantThinkingProps {
  /** 思考内容节点。 */
  children: ReactNode
  /** 当前思考是否已完成。 */
  isDone?: boolean
  /** 附加样式类名。 */
  className?: string
}

/** Assistant 通用卡片容器组件属性。 */
interface AssistantCardShellProps {
  /** 卡片内容节点。 */
  children: ReactNode
  /** 附加样式类名。 */
  className?: string
  /** 卡片当前是否可见。 */
  visible?: boolean
  /** 卡片退出动画完成后的回调。 */
  onExitComplete?: () => void
}

/** Assistant 系统消息组件属性。 */
interface AssistantSystemMessageProps {
  /** 系统消息文本。 */
  text: string
}

/** Assistant 卡片退出动画时长，单位为毫秒。 */
const ASSISTANT_CARD_EXIT_DURATION_MS = 180

/**
 * Assistant 通用气泡容器。
 *
 * @param props - 组件属性
 * @returns 气泡容器节点
 */
export function AssistantBubble({ children, variant = 'text', tone = 'assistant', className }: AssistantBubbleProps) {
  const bubbleClassName = [
    styles.bubble,
    tone === 'user' ? styles.bubbleUser : styles.bubbleAssistant,
    variant === 'image' ? styles.bubbleImage : '',
    className ?? ''
  ]
    .filter(Boolean)
    .join(' ')

  return <div className={bubbleClassName}>{children}</div>
}

/** 用于判断滚动条是否处于贴底状态的容差（单位：px）。 */
const ASSISTANT_THINKING_SCROLL_BOTTOM_THRESHOLD = 24

/**
 * Assistant 思考区容器。
 * 根据状态自动折叠/展开，也支持用户自行切换。
 * 内部内容超过最大高度时出现滚动条，流式输出会自动贴底，
 * 用户手动向上滚动后则停止自动贴底，避免打断阅读。
 *
 * @param props - 组件属性
 * @returns 思考区节点
 */
export function AssistantThinking({ children, isDone = false, className }: AssistantThinkingProps) {
  // 仅在初始挂载时，根据 isDone 决定是否展开。历史记录默认折叠，正在进行默认展开。
  const [expanded, setExpanded] = useState(!isDone)
  /** 滚动容器 ref，用于读取滚动状态并触发自动贴底。 */
  const bodyRef = useRef<HTMLDivElement>(null)
  /** 当前滚动条是否仍贴在底部，用户上滑后置为 false。 */
  const stickToBottomRef = useRef(true)

  /** 思考区标题文案。 */
  const thinkingTitle = isDone ? '已深度思考' : '深度思考中...'
  const thinkingClassName = [
    styles.thinking,
    isDone ? styles.thinkingDone : styles.thinkingActive,
    expanded ? styles.thinkingExpanded : '',
    className ?? ''
  ]
    .filter(Boolean)
    .join(' ')

  /**
   * 监听用户在思考区内的滚动行为，记录当前是否贴底。
   *
   * @returns 无返回值
   */
  const handleBodyScroll = () => {
    const bodyElement = bodyRef.current
    if (!bodyElement) {
      return
    }
    const distanceFromBottom = bodyElement.scrollHeight - bodyElement.scrollTop - bodyElement.clientHeight
    stickToBottomRef.current = distanceFromBottom <= ASSISTANT_THINKING_SCROLL_BOTTOM_THRESHOLD
  }

  /**
   * 流式输出新增内容时，若用户仍处于贴底状态，则保持滚动条贴底。
   */
  useEffect(() => {
    if (!expanded) {
      return
    }
    const bodyElement = bodyRef.current
    if (!bodyElement) {
      return
    }

    bodyElement.scrollTop = bodyElement.scrollHeight
    stickToBottomRef.current = true

    if (typeof ResizeObserver === 'undefined') {
      return
    }

    const resizeObserver = new ResizeObserver(() => {
      if (!stickToBottomRef.current) {
        return
      }
      bodyElement.scrollTop = bodyElement.scrollHeight
    })

    resizeObserver.observe(bodyElement)
    Array.from(bodyElement.children).forEach(child => resizeObserver.observe(child))

    return () => {
      resizeObserver.disconnect()
    }
  }, [expanded])

  return (
    <div className={thinkingClassName}>
      <button type='button' className={styles.thinkingHeader} onClick={() => setExpanded(!expanded)}>
        <div className={styles.thinkingHeaderLeft}>
          <span className={styles.thinkingTitle}>{thinkingTitle}</span>
        </div>
        {expanded ? (
          <ChevronUp className={styles.thinkingArrow} size={14} />
        ) : (
          <ChevronDown className={styles.thinkingArrow} size={14} />
        )}
      </button>
      {expanded ? (
        <div ref={bodyRef} className={styles.thinkingBody} onScroll={handleBodyScroll}>
          {children}
        </div>
      ) : null}
    </div>
  )
}

/**
 * Assistant 打字中动画。
 *
 * @returns 打字中动画节点
 */
export function AssistantTyping() {
  return (
    <div className={styles.typing}>
      <span className={styles.typingDot} />
      <span className={styles.typingDot} />
      <span className={styles.typingDot} />
    </div>
  )
}

/**
 * Assistant 通用卡片容器。
 *
 * @param props - 组件属性
 * @returns 卡片容器节点
 */
export function AssistantCardShell({ children, className, visible = true, onExitComplete }: AssistantCardShellProps) {
  /** 当前退出动画定时器句柄。 */
  const exitTimerRef = useRef<number | null>(null)
  /** 当前这次隐藏流程是否已经完成退出回调。 */
  const exitCompletedRef = useRef(false)

  useEffect(() => {
    if (visible) {
      exitCompletedRef.current = false

      if (exitTimerRef.current !== null) {
        window.clearTimeout(exitTimerRef.current)
        exitTimerRef.current = null
      }

      return
    }

    if (!onExitComplete || exitCompletedRef.current) {
      return
    }

    exitTimerRef.current = window.setTimeout(() => {
      exitCompletedRef.current = true
      exitTimerRef.current = null
      onExitComplete()
    }, ASSISTANT_CARD_EXIT_DURATION_MS)

    return () => {
      if (exitTimerRef.current !== null) {
        window.clearTimeout(exitTimerRef.current)
        exitTimerRef.current = null
      }
    }
  }, [onExitComplete, visible])

  const cardClassName = [styles.cardShell, !visible ? styles.cardShellExiting : '', className ?? '']
    .filter(Boolean)
    .join(' ')

  return <div className={cardClassName}>{children}</div>
}

/**
 * Assistant 系统消息容器。
 *
 * @param props - 组件属性
 * @returns 系统消息节点
 */
export function AssistantSystemMessage({ text }: AssistantSystemMessageProps) {
  return (
    <div className={styles.systemMessage}>
      <div className={styles.systemMessageInner}>{text}</div>
    </div>
  )
}
