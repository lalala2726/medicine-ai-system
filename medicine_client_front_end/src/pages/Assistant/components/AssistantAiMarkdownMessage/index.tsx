import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { LucideIcon } from 'lucide-react'
import { Copy, RotateCcw, Share2, ThumbsDown, ThumbsUp, Volume2 } from 'lucide-react'
import MarkdownRenderer from '@/components/MarkdownRenderer'
import { showNotify, showSuccessNotify } from '@/utils/notify'
import type { ChatMessageToolStatusContent } from '../../modules/messages/chatTypes'
import { useAssistantTtsPlayback } from '../../modules/tts/assistantTtsContext'
import { AssistantThinking } from '../MessagePrimitives'
import styles from './index.module.less'

/** Assistant AI markdown 消息组件属性。 */
export interface AssistantAiMarkdownMessageProps {
  /** 当前 AI 消息对应的 message_uuid。 */
  messageId?: string
  /** AI 文本正文内容。 */
  content: string
  /** AI 思考过程文本。 */
  thinking?: string
  /** 思考过程是否结束。 */
  thinkingDone?: boolean
  /** 当前工具运行中状态。 */
  toolStatus?: ChatMessageToolStatusContent
  /** 回复状态提示文案。 */
  statusText?: string
}

/** AI markdown 组件使用的 Streamdown 渲染模式。 */
const ASSISTANT_AI_MARKDOWN_MODE = 'streaming' as const

/** AI 消息操作类型。 */
type AssistantAiMessageActionType = 'retry' | 'copy' | 'like' | 'dislike' | 'speak' | 'share'

/** AI 消息操作项配置。 */
interface AssistantAiMessageActionItem {
  /** 操作类型。 */
  type: AssistantAiMessageActionType
  /** 操作图标。 */
  icon: LucideIcon
  /** 操作文案。 */
  label: string
}

/** AI 消息复制成功提示文案。 */
const ASSISTANT_AI_COPY_SUCCESS_TEXT = '已复制文本'

/** AI 消息复制失败提示文案。 */
const ASSISTANT_AI_COPY_ERROR_TEXT = '复制失败，请稍后重试'

/** AI 消息占位操作提示文案。 */
const ASSISTANT_AI_ACTION_PENDING_TEXT = '功能开发中'
/** 工具状态文案切换的过渡时长。 */
const ASSISTANT_TOOL_STATUS_TRANSITION_MS = 180

/** Assistant AI 消息操作栏配置。 */
const ASSISTANT_AI_MESSAGE_ACTIONS: AssistantAiMessageActionItem[] = [
  { type: 'retry', icon: RotateCcw, label: '重新生成' },
  { type: 'copy', icon: Copy, label: '复制文本' },
  { type: 'like', icon: ThumbsUp, label: '点赞' },
  { type: 'dislike', icon: ThumbsDown, label: '点踩' },
  { type: 'speak', icon: Volume2, label: '朗读' },
  { type: 'share', icon: Share2, label: '分享' }
]

/**
 * 从 AI markdown 正文节点里提取可复制的可见文本。
 *
 * @param markdownElement - AI markdown 容器节点
 * @param fallbackContent - 节点为空时的兜底原始文本
 * @returns 可复制的纯文本内容
 */
const resolveCopyText = (markdownElement: HTMLDivElement | null, fallbackContent: string) => {
  const visibleText = markdownElement?.innerText.replace(/\n{3,}/g, '\n\n').trim() ?? ''

  return visibleText || fallbackContent.trim()
}

/**
 * 复制文本到系统剪贴板。
 * 优先使用现代 Clipboard API，不可用时回退到隐藏文本域方案。
 *
 * @param text - 待复制文本
 * @returns 复制是否成功
 */
const copyTextToClipboard = async (text: string) => {
  if (!text.trim()) {
    return false
  }

  if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text)
    return true
  }

  if (typeof document === 'undefined') {
    return false
  }

  const textareaElement = document.createElement('textarea')
  textareaElement.value = text
  textareaElement.setAttribute('readonly', 'true')
  textareaElement.style.position = 'fixed'
  textareaElement.style.opacity = '0'
  textareaElement.style.pointerEvents = 'none'
  document.body.appendChild(textareaElement)
  textareaElement.focus()
  textareaElement.select()

  try {
    return document.execCommand('copy')
  } finally {
    document.body.removeChild(textareaElement)
  }
}

/**
 * Assistant 页面专用 AI markdown 消息组件。
 * 仅用于渲染 AI 文本正文、思考区和状态提示文案。
 *
 * @param props - 组件属性
 * @returns AI markdown 消息节点
 */
const AssistantAiMarkdownMessage = ({
  messageId,
  content,
  thinking,
  thinkingDone = false,
  toolStatus,
  statusText
}: AssistantAiMarkdownMessageProps) => {
  /** AI markdown 正文容器引用，用于复制可见文本。 */
  const markdownRef = useRef<HTMLDivElement>(null)
  /** 工具状态文案切换定时器引用。 */
  const toolStatusTransitionTimerRef = useRef<number | null>(null)
  /** 工具状态离场卸载定时器引用。 */
  const toolStatusHideTimerRef = useRef<number | null>(null)
  /** 是否存在可展示的 AI 正文。 */
  const hasContent = content.trim().length > 0
  /** Assistant 页面级 TTS 播放控制器。 */
  const { toggleMessagePlayback, isMessageLoading, isMessagePlaying } = useAssistantTtsPlayback()
  /** 当前实际渲染中的工具状态。 */
  const [renderedToolStatus, setRenderedToolStatus] = useState<ChatMessageToolStatusContent | undefined>(toolStatus)
  /** 当前工具状态是否处于离场过渡。 */
  const [isToolStatusLeaving, setIsToolStatusLeaving] = useState(false)
  /** 当前工具状态文案是否处于切换过渡。 */
  const [isToolStatusTextTransitioning, setIsToolStatusTextTransitioning] = useState(false)

  /**
   * 清理工具状态过渡定时器。
   *
   * @returns 无返回值
   */
  const clearToolStatusTransitionTimer = useCallback(() => {
    if (toolStatusTransitionTimerRef.current === null) {
      return
    }

    window.clearTimeout(toolStatusTransitionTimerRef.current)
    toolStatusTransitionTimerRef.current = null
  }, [])

  /**
   * 清理工具状态离场定时器。
   *
   * @returns 无返回值
   */
  const clearToolStatusHideTimer = useCallback(() => {
    if (toolStatusHideTimerRef.current === null) {
      return
    }

    window.clearTimeout(toolStatusHideTimerRef.current)
    toolStatusHideTimerRef.current = null
  }, [])

  useEffect(() => {
    return () => {
      clearToolStatusTransitionTimer()
      clearToolStatusHideTimer()
    }
  }, [clearToolStatusHideTimer, clearToolStatusTransitionTimer])

  /**
   * 处理工具状态条的显隐和文案切换动画。
   *
   * @returns 无返回值
   */
  useEffect(() => {
    clearToolStatusTransitionTimer()
    clearToolStatusHideTimer()

    if (toolStatus?.phase === 'running') {
      setIsToolStatusLeaving(false)

      if (!renderedToolStatus) {
        setRenderedToolStatus(toolStatus)
        setIsToolStatusTextTransitioning(false)
        return
      }

      if (renderedToolStatus.text === toolStatus.text) {
        setIsToolStatusTextTransitioning(false)
        return
      }

      setIsToolStatusTextTransitioning(true)
      toolStatusTransitionTimerRef.current = window.setTimeout(() => {
        setRenderedToolStatus(toolStatus)
        setIsToolStatusTextTransitioning(false)
        toolStatusTransitionTimerRef.current = null
      }, ASSISTANT_TOOL_STATUS_TRANSITION_MS)
      return
    }

    if (!renderedToolStatus) {
      setIsToolStatusLeaving(false)
      setIsToolStatusTextTransitioning(false)
      return
    }

    setIsToolStatusLeaving(true)
    setIsToolStatusTextTransitioning(false)
    toolStatusHideTimerRef.current = window.setTimeout(() => {
      setRenderedToolStatus(undefined)
      setIsToolStatusLeaving(false)
      toolStatusHideTimerRef.current = null
    }, ASSISTANT_TOOL_STATUS_TRANSITION_MS)
  }, [clearToolStatusHideTimer, clearToolStatusTransitionTimer, renderedToolStatus, toolStatus])

  /**
   * 处理操作栏按钮点击。
   *
   * @param actionType - 当前触发的操作类型
   * @returns 无返回值
   */
  const handleActionClick = useCallback(
    async (actionType: AssistantAiMessageActionType) => {
      if (actionType === 'copy') {
        try {
          const copyText = resolveCopyText(markdownRef.current, content)
          const copied = await copyTextToClipboard(copyText)

          if (copied) {
            showSuccessNotify(ASSISTANT_AI_COPY_SUCCESS_TEXT)
            return
          }
        } catch {
          // ignore and fall through to unified error notify
        }

        showNotify(ASSISTANT_AI_COPY_ERROR_TEXT)
        return
      }

      if (actionType === 'like') {
        console.info('[AssistantAiMarkdownMessage] like', { content })
        return
      }

      if (actionType === 'speak') {
        await toggleMessagePlayback(messageId)
        return
      }

      showNotify(ASSISTANT_AI_ACTION_PENDING_TEXT)
    },
    [content, messageId, toggleMessagePlayback]
  )

  /** 当前消息需要展示的操作栏配置。 */
  const visibleActions = useMemo(() => {
    return hasContent ? ASSISTANT_AI_MESSAGE_ACTIONS : []
  }, [hasContent])

  return (
    <div className={styles.container}>
      {thinking ? (
        <AssistantThinking className={styles.thinking} isDone={thinkingDone}>
          <MarkdownRenderer content={thinking} mode={ASSISTANT_AI_MARKDOWN_MODE} />
        </AssistantThinking>
      ) : null}

      {renderedToolStatus?.phase === 'running' ? (
        <div className={`${styles.toolStatus} ${isToolStatusLeaving ? styles.toolStatusLeaving : ''}`}>
          <div className={styles.toolStatusDots} aria-hidden='true'>
            <span className={styles.toolStatusDot} />
            <span className={styles.toolStatusDot} />
            <span className={styles.toolStatusDot} />
          </div>
          <div
            className={`${styles.toolStatusText} ${isToolStatusTextTransitioning ? styles.toolStatusTextTransitioning : ''}`}
          >
            {renderedToolStatus.text}
          </div>
        </div>
      ) : null}

      {hasContent ? (
        <div ref={markdownRef} className={styles.markdown}>
          <MarkdownRenderer content={content} mode={ASSISTANT_AI_MARKDOWN_MODE} />
        </div>
      ) : null}

      {statusText ? <div className={styles.statusText}>{statusText}</div> : null}

      {visibleActions.length > 0 ? (
        <div className={styles.actions}>
          {visibleActions.map(action => {
            const ActionIcon = action.icon
            /** 当前操作按钮是否处于播放中。 */
            const isPlaying = action.type === 'speak' ? isMessagePlaying(messageId) : false
            /** 当前操作按钮是否处于加载中。 */
            const isLoading = action.type === 'speak' ? isMessageLoading(messageId) : false

            return (
              <button
                key={action.type}
                type='button'
                className={`${styles.actionButton} ${isPlaying ? styles.actionButtonPlaying : ''} ${isLoading ? styles.actionButtonLoading : ''}`}
                aria-label={action.label}
                title={action.label}
                aria-pressed={isPlaying}
                onClick={() => {
                  void handleActionClick(action.type)
                }}
              >
                <ActionIcon size={16} strokeWidth={1.9} />
              </button>
            )
          })}
        </div>
      ) : null}
    </div>
  )
}

export default AssistantAiMarkdownMessage
