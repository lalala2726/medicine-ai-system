import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type FocusEvent,
  type MouseEvent,
  type RefObject,
  type SetStateAction
} from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import type { OrderAfterSaleTypes } from '@/api/orderAfterSale'
import type { newOrderTypes } from '@/api/order'
import type { patientProfileTypes } from '@/api/patientProfile'
import type { AssistantChatSubmitCardPayload } from '@/api/assistant'
import { getAssistantChatCapability } from '@/api/assistant'
import { ASSISTANT_CARD_TYPES, ASSISTANT_MESSAGE_TYPES, type AssistantTypes } from '@/api/assistant/contract'
import { initialMessages, useChatStore } from '@/stores/chatStore'
import { useAssistantDrawerStore } from '@/stores/assistantDrawerStore'
import { useUserStore } from '@/stores/userStore'
import { showErrorNotify, showSuccessNotify, showWarningNotify } from '@/utils/notify'
import type { AssistantComposerProps } from '../../components/AssistantComposer'
import { useMessageCallbacks } from '../messages/useMessageCallbacks'
import { useAssistantMessages } from '../messages/useAssistantMessages'
import {
  CHAT_MESSAGE_POSITIONS,
  CHAT_RESPONSE_STATUSES,
  CHAT_MESSAGE_TYPES,
  type ChatMessage,
  type MessageContentCallbacks
} from '../messages/chatTypes'
import {
  ASSISTANT_HISTORY_LOADING_WARNING,
  ASSISTANT_INPUT_PLACEHOLDER,
  ASSISTANT_NEW_SESSION_SUCCESS_TEXT,
  ASSISTANT_REPLY_PENDING_WARNING,
  ASSISTANT_REPLY_RESUME_WARNING,
  ASSISTANT_TRANSIENT_MESSAGE_TYPES
} from './assistantPage.constants'
import {
  ASSISTANT_AVATAR,
  ASSISTANT_INPUT_MODES,
  ASSISTANT_TOOLBAR_ITEMS,
  ASSISTANT_TOOLBAR_TYPES,
  DEFAULT_USER_AVATAR,
  type AssistantInputMode,
  type AssistantToolbarItem
} from '../shared/assistantUiConfig'
import { useAssistantSelectorState } from '../selectors/useAssistantSelectorState'
import { useAssistantHistory } from '../session/useAssistantHistory'
import {
  ASSISTANT_REQUEST_START_RESULT_TYPES,
  useAssistantStream,
  type AssistantRequestStartResult
} from '../stream/useAssistantStream'
import { routeAssistantStreamAction } from '../stream/assistantStreamActionRouter'
import { useConversationBootstrap } from '../session/useConversationBootstrap'
import { buildOrderSelectionPayload } from '../selectors/orderSelection'
import { buildAfterSaleSelectionPayload } from '../selectors/afterSaleSelection'
import { buildPatientSelectionPayload } from '../selectors/patientSelection'
import { isHistoryRenderMessage, type HistoryRenderMessage } from '../session/historyMessageAdapters'
import {
  buildConsultProductCardViewData,
  buildConsultProductConversationTitle,
  buildConsultProductSubmitCardPayload,
  type AssistantConsultProductDraft,
  type AssistantConsultRouteState
} from '../shared/consultProductDraft'

/** Assistant 页面总控 Hook 的返回值。 */
export interface AssistantPageControllerResult {
  historyLoading: boolean
  historyError: string | null
  /** 消息滚动容器 ref，用于精确控制滚动位置。 */
  messageListRef: RefObject<HTMLDivElement>
  /** 当前页面消息数组。 */
  messages: ChatMessage[]
  /** 消息渲染交互回调集合。 */
  messageCallbacks: MessageContentCallbacks
  /** 输入区组件属性。 */
  composerProps: AssistantComposerProps
  orderSelectorProps: {
    visible: boolean
    initialStatus?: string
    onClose: () => void
    onSelect: (order: newOrderTypes.OrderListVo) => void
  }
  afterSaleSelectorProps: {
    visible: boolean
    initialStatus?: string
    onClose: () => void
    onSelect: (item: OrderAfterSaleTypes.AfterSaleListVo) => void
  }
  patientSelectorProps: {
    visible: boolean
    onClose: () => void
    onSelect: (patient: patientProfileTypes.PatientProfileListVo) => void
  }
  handleMenuButtonClick: (event: MouseEvent<HTMLButtonElement>) => void
  handleNewSessionClick: () => void
  handleQuickAction: (text: string) => void
}

/** 模型不支持图片理解时的统一提示文案。 */
const ASSISTANT_UNSUPPORTED_IMAGE_UNDERSTANDING_TEXT = '此模型不支持图片理解'
/** 模型不支持深度思考时的统一提示文案。 */
const ASSISTANT_UNSUPPORTED_REASONING_TEXT = '当前客户端助手暂未开启深度思考'

/**
 * 将聊天区直接定位到最新一条消息。
 *
 * @param messageListRef - 页面层消息滚动容器 ref
 */
const scrollChatToBottom = (messageListRef: RefObject<HTMLDivElement>) => {
  const container = messageListRef.current
  if (!container) {
    return
  }

  container.scrollTop = container.scrollHeight
}

/**
 * 编排 Assistant 页面所需的所有状态、事件和副作用。
 * `index.tsx` 只负责消费这个 controller 的结果并渲染页面骨架。
 */
export function useAssistantPageController(): AssistantPageControllerResult {
  /** 当前路由对象，用于消费详情页带来的商品咨询草稿。 */
  const location = useLocation()
  /** 当前路由导航方法，用于单次消费路由 state。 */
  const navigate = useNavigate()
  /** 页面自有消息列表与增删改能力。 */
  const { messages, appendMessage, updateMessageById, deleteMessageById, resetMessages } =
    useAssistantMessages(initialMessages)
  /** Assistant 页面消息滚动容器，用于精确控制历史加载后的滚动位置。 */
  const messageListRef = useRef<HTMLDivElement>(null)
  /** 记录已经处理过的历史版本，避免重复执行到底部逻辑。 */
  const lastHandledHistoryLoadVersionRef = useRef(0)
  /** 已尝试过自动 re-attach 的会话 UUID，防止 404 后无限重试。 */
  const autoReattachAttemptedRef = useRef<string | null>(null)
  /** 当前输入框文本内容。 */
  const [composerValue, setComposerValue] = useState('')
  /** 当前输入框交互模式。 */
  const [composerInputMode, setComposerInputMode] = useState<AssistantInputMode>(ASSISTANT_INPUT_MODES.TEXT)
  /** 当前输入区挂载的商品咨询草稿。 */
  const [attachedProductDraft, setAttachedProductDraft] = useState<AssistantConsultProductDraft | null>(null)
  /** 当前输入区已上传图片 URL 列表。 */
  const [composerImageUrls, setComposerImageUrls] = useState<string[]>([])
  /** 当前是否允许上传图片。 */
  const [imageUploadEnabled, setImageUploadEnabled] = useState(true)
  /** 图片上传禁用时的提示文案。 */
  const [imageUploadDisabledText, setImageUploadDisabledText] = useState(ASSISTANT_UNSUPPORTED_IMAGE_UNDERSTANDING_TEXT)
  /** 当前是否允许开启深度思考。 */
  const [reasoningToggleEnabled, setReasoningToggleEnabled] = useState(false)
  /** 深度思考禁用时的提示文案。 */
  const [reasoningToggleDisabledText, setReasoningToggleDisabledText] = useState(ASSISTANT_UNSUPPORTED_REASONING_TEXT)
  /** 当前输入是否开启深度思考。 */
  const [deepThinking, setDeepThinking] = useState(false)

  /** 当前登录用户信息。 */
  const { user } = useUserStore()
  /** 当前激活的会话 UUID。 */
  const activeConversationUuid = useChatStore(state => state.activeConversationUuid)
  /** 历史消息加载状态。 */
  const historyLoading = useChatStore(state => state.historyLoading)
  /** 历史消息错误信息。 */
  const historyError = useChatStore(state => state.historyError)
  /** 当前是否存在流式回复。 */
  const isReplying = useChatStore(state => state.isReplying)
  /** 设置激活会话。 */
  const setActiveConversationUuid = useChatStore(state => state.setActiveConversationUuid)
  /** 更新历史加载状态。 */
  const setHistoryLoading = useChatStore(state => state.setHistoryLoading)
  /** 更新历史错误信息。 */
  const setHistoryError = useChatStore(state => state.setHistoryError)
  /** 更新流式回复状态。 */
  const setIsReplying = useChatStore(state => state.setIsReplying)
  /** 将新会话或已存在会话顶到列表顶部。 */
  const upsertConversation = useChatStore(state => state.upsertConversation)
  /** 开启新会话。 */
  const startNewConversation = useChatStore(state => state.startNewConversation)
  /** 刷新历史会话列表。 */
  const loadConversationList = useChatStore(state => state.loadConversationList)
  /** 稳定的“刷新第一页会话列表”回调，避免 effect 因匿名函数反复重跑。 */
  const refreshConversationList = useCallback(() => loadConversationList(true), [loadConversationList])

  /** 侧边历史抽屉切换函数。 */
  const toggleDrawer = useAssistantDrawerStore(state => state.toggleDrawer)
  /** 关闭侧边历史抽屉。 */
  const closeDrawer = useAssistantDrawerStore(state => state.closeDrawer)

  /** 页面内的选择器可见性与筛选状态。 */
  const {
    orderSelectorVisible,
    orderSelectorInitialStatus,
    afterSaleSelectorVisible,
    afterSaleSelectorInitialStatus,
    patientSelectorVisible,
    openOrderSelector,
    closeOrderSelector,
    openAfterSaleSelector,
    closeAfterSaleSelector,
    openPatientSelector,
    closePatientSelector
  } = useAssistantSelectorState()

  /** 助手头像。 */
  const assistantAvatar = ASSISTANT_AVATAR
  /** 用户头像，未登录或未设置时使用默认头像。 */
  const userAvatar = user?.avatar || DEFAULT_USER_AVATAR

  /**
   * 构造“文本 + 图片”用户消息 Markdown。
   *
   * @param question - 用户问题文本
   * @param imageUrls - 用户上传图片 URL 列表
   * @returns 可用于本地回显与历史回放的 Markdown 文本
   */
  const buildUserMessageMarkdownWithImages = useCallback((question: string, imageUrls: string[]) => {
    const normalizedQuestion = question.trim()
    if (!imageUrls.length) {
      return normalizedQuestion
    }

    const imageMarkdown = imageUrls.map((url, index) => `![用户上传图片${index + 1}](${url})`).join('\n')
    if (normalizedQuestion) {
      return `${normalizedQuestion}\n\n${imageMarkdown}`
    }
    return imageMarkdown
  }, [])

  /**
   * 将当前商品咨询草稿转换为用户本地回显消息。
   *
   * @param question - 用户当前输入的问题文本
   * @param productDraft - 当前待发送的商品咨询草稿
   * @returns 复用历史渲染器的本地用户消息
   */
  const buildConsultProductUserMessage = useCallback(
    (question: string, productDraft: AssistantConsultProductDraft): HistoryRenderMessage => {
      return {
        type: CHAT_MESSAGE_TYPES.TEXT,
        content: {
          text: question
        },
        position: CHAT_MESSAGE_POSITIONS.RIGHT,
        user: {
          avatar: userAvatar
        },
        renderSource: 'assistant-history',
        historyCards: [
          {
            cardUuid: `draft-product-${productDraft.productId}`,
            type: ASSISTANT_CARD_TYPES.CONSULT_PRODUCT_CARD,
            ...buildConsultProductCardViewData(productDraft)
          }
        ]
      }
    },
    [userAvatar]
  )

  /**
   * 单次消费详情页带入的商品咨询草稿，并立即清理路由 state。
   *
   * @returns 无返回值
   */
  useEffect(() => {
    const routeState = location.state as AssistantConsultRouteState | null
    const consultProductDraft = routeState?.consultProductDraft

    if (!consultProductDraft) {
      return
    }

    setAttachedProductDraft(consultProductDraft)
    setComposerImageUrls([])
    navigate(location.pathname, { replace: true, state: null })
  }, [location.pathname, location.state, navigate])

  /**
   * 加载客户端聊天输入区能力配置。
   */
  useEffect(() => {
    let cancelled = false

    void getAssistantChatCapability()
      .then(capability => {
        if (cancelled) {
          return
        }
        setImageUploadEnabled(capability.imageUploadEnabled)
        setImageUploadDisabledText(
          capability.imageUploadDisabledMessage || ASSISTANT_UNSUPPORTED_IMAGE_UNDERSTANDING_TEXT
        )
        setReasoningToggleEnabled(capability.reasoningToggleEnabled)
        setReasoningToggleDisabledText(
          capability.reasoningToggleDisabledMessage || ASSISTANT_UNSUPPORTED_REASONING_TEXT
        )
        if (!capability.reasoningToggleEnabled) {
          setDeepThinking(false)
        }
      })
      .catch(() => {
        // 能力加载失败时保持当前页面默认行为，并继续依赖提交接口校验
      })

    return () => {
      cancelled = true
    }
  }, [])

  /**
   * 当图片能力被禁用时，主动清空当前已上传图片，避免继续带图发送。
   */
  useEffect(() => {
    if (imageUploadEnabled || composerImageUrls.length === 0) {
      return
    }
    setComposerImageUrls([])
    showWarningNotify(imageUploadDisabledText)
  }, [composerImageUrls.length, imageUploadDisabledText, imageUploadEnabled])

  /**
   * 当深度思考能力被禁用时，主动关闭当前输入态开关。
   */
  useEffect(() => {
    if (reasoningToggleEnabled || !deepThinking) {
      return
    }
    setDeepThinking(false)
    showWarningNotify(reasoningToggleDisabledText)
  }, [deepThinking, reasoningToggleDisabledText, reasoningToggleEnabled])

  /**
   * 会话切换时重置深度思考开关，避免跨会话残留。
   */
  useEffect(() => {
    setDeepThinking(false)
  }, [activeConversationUuid])

  /** 历史会话加载与欢迎态重置能力。 */
  const { renderedConversationUuid, historyLoadVersion, setRenderedConversationUuid, resetToWelcome, loadHistory } =
    useAssistantHistory({
      assistantAvatar,
      userAvatar,
      resetList: resetMessages,
      setHistoryLoading,
      setHistoryError,
      startNewConversation,
      loadConversationList: refreshConversationList
    })

  /**
   * 根据后端消息 ID 在当前页面消息列表中查找对应消息。
   *
   * @param messageId - 后端消息 UUID
   * @returns 命中的页面消息对象
   */
  const findMessageByServerId = useCallback(
    (messageId: string) => {
      return messages.find(message => message.messageId === messageId || message._id === messageId)
    },
    [messages]
  )

  /**
   * 历史消息加载完成后，将聊天区瞬间定位到最新消息。
   * 只对新的历史加载成功版本执行一次，避免普通重渲染反复抢滚动。
   */
  useEffect(() => {
    if (historyLoading || historyLoadVersion === 0 || historyLoadVersion <= lastHandledHistoryLoadVersionRef.current) {
      return
    }

    let frameId = 0
    let nestedFrameId = 0

    frameId = window.requestAnimationFrame(() => {
      nestedFrameId = window.requestAnimationFrame(() => {
        scrollChatToBottom(messageListRef)
        lastHandledHistoryLoadVersionRef.current = historyLoadVersion
      })
    })

    return () => {
      window.cancelAnimationFrame(frameId)
      window.cancelAnimationFrame(nestedFrameId)
    }
  }, [historyLoading, historyLoadVersion])

  /** 最后一条消息的滚动观测快照，用于在新增或流式更新后自动滚到底部。 */
  const lastMessageSnapshot = useMemo(() => {
    const lastMessage = messages[messages.length - 1]

    if (!lastMessage) {
      return 'empty'
    }

    return [
      messages.length,
      lastMessage._id || '',
      lastMessage.type,
      lastMessage.content.text?.length || 0,
      lastMessage.content.thinking?.length || 0,
      lastMessage.content.toolStatus?.text || '',
      lastMessage.content.responseStatus || '',
      lastMessage.content.responseStatusText || '',
      isHistoryRenderMessage(lastMessage) ? lastMessage.historyCards?.length || 0 : 0
    ].join(':')
  }, [messages])

  /** 当前会话中最后一条仍处于 streaming 状态的历史 AI 消息。 */
  const lastStreamingHistoryAssistantMessage = useMemo(() => {
    for (let index = messages.length - 1; index >= 0; index -= 1) {
      const currentMessage = messages[index]

      if (!isHistoryRenderMessage(currentMessage) || currentMessage.position === CHAT_MESSAGE_POSITIONS.RIGHT) {
        continue
      }

      if (
        currentMessage.messageId?.trim() &&
        currentMessage.content.responseStatus === CHAT_RESPONSE_STATUSES.STREAMING
      ) {
        return currentMessage
      }
    }

    return null
  }, [messages])

  /**
   * 只要消息尾部发生变化，就把消息区滚到最新位置。
   */
  useEffect(() => {
    if (messages.length === 0) {
      return
    }

    const frameId = window.requestAnimationFrame(() => {
      scrollChatToBottom(messageListRef)
    })

    return () => {
      window.cancelAnimationFrame(frameId)
    }
  }, [lastMessageSnapshot, messages.length])

  /**
   * 路由后端下发的 SSE action。
   * 这层只做“协议动作 -> 页面弹层”的映射。
   */
  const handleStreamAction = useCallback(
    (event: AssistantTypes.StreamEvent) => {
      routeAssistantStreamAction(event, {
        onOpenOrderSelector: openOrderSelector,
        onOpenAfterSaleSelector: openAfterSaleSelector,
        onOpenPatientSelector: openPatientSelector
      })
    },
    [openAfterSaleSelector, openOrderSelector, openPatientSelector]
  )

  /** SSE 流式发送与结算能力。 */
  const { sendMessage, attachConversationStream, stopCurrentRequest, detachCurrentRequest, isStopping } =
    useAssistantStream({
      assistantAvatar,
      userAvatar,
      activeConversationUuid,
      appendMsg: appendMessage,
      updateMsg: updateMessageById,
      deleteMsg: deleteMessageById,
      findMessageByServerId,
      setActiveConversationUuid,
      upsertConversation,
      setIsReplying,
      onConversationReady: setRenderedConversationUuid,
      onConversationListRefresh: refreshConversationList,
      onAction: handleStreamAction
    })

  /**
   * 历史消息恢复后，如果最后一条 AI 仍在 streaming，则自动重新 attach 到当前会话。
   */
  useEffect(() => {
    if (
      historyLoading ||
      isReplying ||
      !activeConversationUuid ||
      activeConversationUuid !== renderedConversationUuid ||
      !lastStreamingHistoryAssistantMessage?.messageId
    ) {
      return
    }

    /**
     * 防止同一会话的 auto-re-attach 被无限触发。
     * 如果本轮已经对当前会话尝试过 attach（无论成功或失败），
     * 跳过后续由 attachConversationStream 引用变化引起的重复调用。
     */
    if (autoReattachAttemptedRef.current === activeConversationUuid) {
      return
    }

    autoReattachAttemptedRef.current = activeConversationUuid

    attachConversationStream({
      conversationUuid: activeConversationUuid,
      messageUuid: lastStreamingHistoryAssistantMessage.messageId,
      existingMessage: lastStreamingHistoryAssistantMessage
    })
  }, [
    activeConversationUuid,
    attachConversationStream,
    historyLoading,
    isReplying,
    lastStreamingHistoryAssistantMessage,
    renderedConversationUuid
  ])

  /**
   * 页面初始化时恢复会话列表与激活会话。
   * 同时负责组件卸载时的流式请求清理。
   */
  useConversationBootstrap({
    activeConversationUuid,
    renderedConversationUuid,
    isReplying,
    detachCurrentRequest,
    closeDrawer,
    setIsReplying,
    loadConversationList: refreshConversationList,
    loadHistory,
    startNewConversation,
    resetToWelcome
  })

  /**
   * 当历史消息加载或流式回复进行中时，阻止发送新请求。
   *
   * @returns 是否应当阻止当前交互
   */
  const guardBusyState = useCallback(() => {
    if (historyLoading) {
      showWarningNotify(ASSISTANT_HISTORY_LOADING_WARNING)
      return true
    }

    if (isReplying) {
      showWarningNotify(ASSISTANT_REPLY_PENDING_WARNING)
      return true
    }

    return false
  }, [historyLoading, isReplying])

  /**
   * 清理掉历史切换或重新发送前遗留的临时消息，例如 typing 占位。
   */
  const removeTransientMessages = useCallback(() => {
    messages.forEach(message => {
      const isTransientMessage = ASSISTANT_TRANSIENT_MESSAGE_TYPES.some(type => type === message.type)

      if (message._id && isTransientMessage) {
        deleteMessageById(message._id)
      }
    })
  }, [deleteMessageById, messages])

  /**
   * 从当前消息列表里移除一张已经成功发起请求的交互卡。
   * 历史消息可能同时包含文本和多张卡片，因此不能直接删除整条消息。
   */
  const removeInteractiveCardLocally = useCallback(
    (localMessageId: string, cardUuid: string) => {
      const targetMessage = messages.find(message => message._id === localMessageId)

      if (!targetMessage?._id) {
        return
      }

      if (!isHistoryRenderMessage(targetMessage)) {
        updateMessageById(targetMessage._id, {
          ...targetMessage,
          _exiting: true
        })
        return
      }

      const currentHistoryCards = targetMessage.historyCards ?? []
      const remainingHistoryCards = currentHistoryCards.filter(card => card.cardUuid !== cardUuid)

      if (remainingHistoryCards.length === currentHistoryCards.length) {
        return
      }

      const hasRenderableText = Boolean(
        targetMessage.content.text?.trim() ||
          targetMessage.content.thinking?.trim() ||
          targetMessage.content.responseStatusText?.trim()
      )

      if (remainingHistoryCards.length === 0 && !hasRenderableText) {
        deleteMessageById(targetMessage._id)
        return
      }

      const nextHistoryMessage: HistoryRenderMessage = {
        ...targetMessage,
        historyCards: remainingHistoryCards.length > 0 ? remainingHistoryCards : undefined
      }

      updateMessageById(targetMessage._id, nextHistoryMessage as unknown as ChatMessage)
    },
    [deleteMessageById, messages, updateMessageById]
  )

  /**
   * 卡片退出动画完成后的清理回调。
   */
  const handleCardExitComplete = useCallback(
    (localMessageId: string) => {
      deleteMessageById(localMessageId)
    },
    [deleteMessageById]
  )

  /**
   * 统一封装“用户发起一次 Assistant 请求”的启动流程。
   *
   * @param options.messageType - 发送给后端的消息类型
   * @param options.content - 文本消息正文
   * @param options.imageUrls - 图片 URL 列表
   * @param options.card - 卡片消息载荷
   * @param options.userMessage - 需要回显到聊天区的用户消息
   * @param options.cardAction - 附带给后端的卡片点击动作
   * @param options.conversationTitle - 本次提交使用的会话标题
   * @returns 本次请求的启动结果
   */
  const startAssistantRequest = useCallback(
    async ({
      messageType,
      content,
      imageUrls,
      card,
      userMessage,
      cardAction,
      conversationTitle
    }: {
      messageType: typeof ASSISTANT_MESSAGE_TYPES.TEXT | typeof ASSISTANT_MESSAGE_TYPES.CARD
      content?: string
      imageUrls?: string[]
      card?: AssistantChatSubmitCardPayload
      userMessage?: ChatMessage
      cardAction?: AssistantTypes.CardActionPayload
      conversationTitle?: string
    }): Promise<AssistantRequestStartResult> => {
      if (guardBusyState()) {
        return {
          type: ASSISTANT_REQUEST_START_RESULT_TYPES.BLOCKED
        }
      }

      removeTransientMessages()

      const result = await sendMessage({
        messageType,
        content,
        imageUrls,
        card,
        userMessage,
        cardAction,
        conversationTitle,
        reasoningEnabled: reasoningToggleEnabled && deepThinking
      })

      if (result.type === ASSISTANT_REQUEST_START_RESULT_TYPES.BLOCKED) {
        showWarningNotify(ASSISTANT_REPLY_PENDING_WARNING)
      }

      if (result.type === ASSISTANT_REQUEST_START_RESULT_TYPES.CONFLICT) {
        showWarningNotify(ASSISTANT_REPLY_RESUME_WARNING)
      }

      if (result.type === ASSISTANT_REQUEST_START_RESULT_TYPES.FAILED) {
        if (
          result.errorMessage.trim() === ASSISTANT_UNSUPPORTED_IMAGE_UNDERSTANDING_TEXT ||
          result.errorMessage.trim() === ASSISTANT_UNSUPPORTED_REASONING_TEXT
        ) {
          showWarningNotify(result.errorMessage)
        } else {
          showErrorNotify(result.errorMessage)
        }
      }

      return result
    },
    [deepThinking, guardBusyState, reasoningToggleEnabled, removeTransientMessages, sendMessage]
  )

  /** 消息内容组件使用的交互回调。 */
  const messageCallbacks = useMessageCallbacks({
    removeInteractiveCardLocally,
    startAssistantRequest,
    userAvatar,
    onCardExitComplete: handleCardExitComplete
  })

  /**
   * 处理用户发送的纯文本消息。
   */
  const handleSend = useCallback(() => {
    void (async () => {
      const question = composerValue.trim()
      const consultProductDraft = attachedProductDraft
      const imageUrls = composerImageUrls

      if (!question && !consultProductDraft && imageUrls.length === 0) {
        return
      }

      const result = await startAssistantRequest({
        messageType: ASSISTANT_MESSAGE_TYPES.TEXT,
        content: question || undefined,
        imageUrls: consultProductDraft ? undefined : imageUrls,
        card: consultProductDraft ? buildConsultProductSubmitCardPayload(consultProductDraft) : undefined,
        userMessage: consultProductDraft
          ? buildConsultProductUserMessage(question, consultProductDraft)
          : {
              type: CHAT_MESSAGE_TYPES.TEXT,
              content: {
                text: question || undefined,
                imageUrls: imageUrls.length > 0 ? imageUrls : undefined
              },
              position: CHAT_MESSAGE_POSITIONS.RIGHT,
              user: { avatar: userAvatar }
            },
        conversationTitle: consultProductDraft
          ? buildConsultProductConversationTitle(consultProductDraft)
          : question || (imageUrls.length > 0 ? '图片咨询' : undefined)
      })

      if (result.type === ASSISTANT_REQUEST_START_RESULT_TYPES.SUBMITTED) {
        setComposerValue('')
        setComposerImageUrls([])
        if (consultProductDraft) {
          setAttachedProductDraft(null)
        }
      }
    })()
  }, [
    attachedProductDraft,
    buildConsultProductUserMessage,
    composerImageUrls,
    composerValue,
    startAssistantRequest,
    userAvatar
  ])

  /**
   * 处理“停止生成”按钮点击。
   *
   * @returns 无返回值
   */
  const handleStopReply = useCallback(() => {
    void (async () => {
      const result = await stopCurrentRequest()

      if (!result.success && result.errorMessage) {
        showErrorNotify(result.errorMessage)
      }
    })()
  }, [stopCurrentRequest])

  /**
   * 处理输入框内容变化。
   *
   * @param value - 最新输入文本
   * @returns 无返回值
   */
  const handleComposerValueChange = useCallback((value: string) => {
    setComposerValue(value)
  }, [])

  /**
   * 处理输入区图片 URL 列表变化。
   *
   * @param imageUrls - 最新图片 URL 列表或基于当前列表的更新函数
   * @returns 无返回值
   */
  const handleComposerImageUrlsChange = useCallback((imageUrls: SetStateAction<string[]>) => {
    setComposerImageUrls(imageUrls)
  }, [])

  /**
   * 切换输入区交互模式。
   *
   * @param inputMode - 目标输入模式
   * @returns 无返回值
   */
  const handleComposerInputModeChange = useCallback((inputMode: AssistantInputMode) => {
    setComposerInputMode(inputMode)
  }, [])

  /**
   * 在启动语音识别前，统一检查当前页面是否允许继续交互。
   *
   * @returns 当前是否允许启动语音识别
   */
  const handleBeforeVoiceStart = useCallback(() => {
    return !guardBusyState()
  }, [guardBusyState])

  /**
   * 处理语音识别完成后的最终文本，并直接走现有发送链路。
   *
   * @param text - STT 最终识别得到的文本
   * @returns 无返回值
   */
  const handleVoiceTranscriptSubmit = useCallback(
    (text: string) => {
      void (async () => {
        /** 规整后的最终问题文本。 */
        const question = text.trim()

        if (!question) {
          return
        }

        const consultProductDraft = attachedProductDraft
        const imageUrls = composerImageUrls
        const result = await startAssistantRequest({
          messageType: ASSISTANT_MESSAGE_TYPES.TEXT,
          content: question,
          imageUrls: consultProductDraft ? undefined : imageUrls,
          card: consultProductDraft ? buildConsultProductSubmitCardPayload(consultProductDraft) : undefined,
          userMessage: consultProductDraft
            ? buildConsultProductUserMessage(question, consultProductDraft)
            : {
                type: CHAT_MESSAGE_TYPES.TEXT,
                content: { text: buildUserMessageMarkdownWithImages(question, imageUrls) },
                position: CHAT_MESSAGE_POSITIONS.RIGHT,
                user: { avatar: userAvatar }
              },
          conversationTitle: consultProductDraft
            ? buildConsultProductConversationTitle(consultProductDraft)
            : question || (imageUrls.length > 0 ? '图片咨询' : undefined)
        })

        if (result.type === ASSISTANT_REQUEST_START_RESULT_TYPES.SUBMITTED) {
          setComposerImageUrls([])
          if (consultProductDraft) {
            setAttachedProductDraft(null)
          }
        } else {
          setComposerInputMode(ASSISTANT_INPUT_MODES.TEXT)
          setComposerValue(question)
        }
      })()
    },
    [
      attachedProductDraft,
      buildConsultProductUserMessage,
      buildUserMessageMarkdownWithImages,
      composerImageUrls,
      startAssistantRequest,
      userAvatar
    ]
  )

  /**
   * 输入框聚焦时，将消息区滚到最新位置。
   *
   * @param _event - 输入框焦点事件
   * @returns 无返回值
   */
  const handleComposerFocus = useCallback((_event: FocusEvent<HTMLTextAreaElement>) => {
    scrollChatToBottom(messageListRef)
  }, [])

  /**
   * 移除当前输入区挂载的商品咨询草稿。
   *
   * @returns 无返回值
   */
  const handleRemoveAttachedProductDraft = useCallback(() => {
    setAttachedProductDraft(null)
  }, [])

  /**
   * 处理底部工具栏点击。
   * 图片项由 AssistantComposer 内部处理上传；此处只处理业务工具入口。
   */
  const handleToolbarClick = useCallback(
    (item: AssistantToolbarItem) => {
      if (item.type === ASSISTANT_TOOLBAR_TYPES.ORDER) {
        openOrderSelector()
        return
      }

      if (item.type === ASSISTANT_TOOLBAR_TYPES.AFTER_SALE) {
        openAfterSaleSelector()
        return
      }

      if (item.type === ASSISTANT_TOOLBAR_TYPES.PATIENT) {
        openPatientSelector()
        return
      }
    },
    [openAfterSaleSelector, openOrderSelector, openPatientSelector]
  )

  /**
   * 将选中的订单转换成问题文本和用户消息，然后发起流式请求。
   */
  const handleOrderSelect = useCallback(
    (order: newOrderTypes.OrderListVo) => {
      const payload = buildOrderSelectionPayload({
        order,
        userAvatar
      })

      closeOrderSelector()

      void (async () => {
        await startAssistantRequest(payload)
      })()
    },
    [closeOrderSelector, startAssistantRequest, userAvatar]
  )

  /**
   * 将选中的售后单摘要整理为文本消息并发起流式请求。
   */
  const handleAfterSaleSelect = useCallback(
    (item: OrderAfterSaleTypes.AfterSaleListVo) => {
      const payload = buildAfterSaleSelectionPayload({
        item,
        userAvatar
      })

      closeAfterSaleSelector()

      void (async () => {
        await startAssistantRequest(payload)
      })()
    },
    [closeAfterSaleSelector, startAssistantRequest, userAvatar]
  )

  /**
   * 将选中的就诊人资料整理成就诊人卡并发起流式请求。
   *
   * @param patient - 选中的就诊人信息
   * @returns 无返回值
   */
  const handlePatientSelect = useCallback(
    (patient: patientProfileTypes.PatientProfileListVo) => {
      const payload = buildPatientSelectionPayload({
        patient,
        userAvatar
      })

      closePatientSelector()

      void (async () => {
        await startAssistantRequest(payload)
      })()
    },
    [closePatientSelector, startAssistantRequest, userAvatar]
  )

  /**
   * 切换左侧历史会话抽屉。
   */
  const handleMenuButtonClick = useCallback(
    (event: MouseEvent<HTMLButtonElement>) => {
      event.stopPropagation()
      toggleDrawer()
    },
    [toggleDrawer]
  )

  /**
   * 开启一个全新会话，并把聊天区恢复成欢迎态。
   */
  const handleNewSessionClick = useCallback(() => {
    if (guardBusyState()) {
      return
    }

    startNewConversation()
    resetToWelcome()
    setComposerValue('')
    setComposerImageUrls([])
    setDeepThinking(false)
    setComposerInputMode(ASSISTANT_INPUT_MODES.TEXT)
    void refreshConversationList().catch(() => undefined)
    showSuccessNotify(ASSISTANT_NEW_SESSION_SUCCESS_TEXT)
  }, [guardBusyState, refreshConversationList, resetToWelcome, startNewConversation])

  /**
   * 欢迎屏快捷操作点击：将操作文本填入输入框并直接发送。
   */
  const handleQuickAction = useCallback(
    (text: string) => {
      setComposerValue(text)
      // 延迟一帧确保 composerValue 已更新后触发发送
      requestAnimationFrame(() => {
        void (async () => {
          const result = await startAssistantRequest({
            messageType: ASSISTANT_MESSAGE_TYPES.TEXT,
            content: text,
            userMessage: {
              type: CHAT_MESSAGE_TYPES.TEXT,
              content: { text },
              position: CHAT_MESSAGE_POSITIONS.RIGHT,
              user: { avatar: userAvatar }
            },
            conversationTitle: text
          })

          if (result.type === ASSISTANT_REQUEST_START_RESULT_TYPES.SUBMITTED) {
            setComposerValue('')
          }
        })()
      })
    },
    [startAssistantRequest, userAvatar]
  )

  /** 页面输入区属性集合。 */
  const composerProps: AssistantComposerProps = useMemo(
    () => ({
      value: composerValue,
      inputMode: composerInputMode,
      isReplying,
      isStopping,
      placeholder: ASSISTANT_INPUT_PLACEHOLDER,
      toolbarItems: ASSISTANT_TOOLBAR_ITEMS,
      onValueChange: handleComposerValueChange,
      imageUrls: composerImageUrls,
      imageUploadEnabled,
      imageUploadDisabledText,
      showDeepThinking: reasoningToggleEnabled,
      deepThinking,
      onImageUrlsChange: handleComposerImageUrlsChange,
      onSend: handleSend,
      onStop: handleStopReply,
      onToggleInputMode: handleComposerInputModeChange,
      onDeepThinkingChange: setDeepThinking,
      onBeforeVoiceStart: handleBeforeVoiceStart,
      onVoiceTranscriptSubmit: handleVoiceTranscriptSubmit,
      onToolbarItemClick: handleToolbarClick,
      attachedProductDraft,
      onRemoveAttachedProductDraft: handleRemoveAttachedProductDraft,
      onFocus: handleComposerFocus
    }),
    [
      attachedProductDraft,
      composerInputMode,
      composerImageUrls,
      composerValue,
      handleBeforeVoiceStart,
      handleComposerFocus,
      handleComposerInputModeChange,
      handleComposerImageUrlsChange,
      handleRemoveAttachedProductDraft,
      handleComposerValueChange,
      deepThinking,
      handleSend,
      handleStopReply,
      handleVoiceTranscriptSubmit,
      handleToolbarClick,
      imageUploadDisabledText,
      imageUploadEnabled,
      isReplying,
      isStopping,
      reasoningToggleEnabled
    ]
  )

  return {
    historyLoading,
    historyError,
    messageListRef,
    messages,
    messageCallbacks,
    composerProps,
    orderSelectorProps: {
      visible: orderSelectorVisible,
      initialStatus: orderSelectorInitialStatus,
      onClose: closeOrderSelector,
      onSelect: handleOrderSelect
    },
    afterSaleSelectorProps: {
      visible: afterSaleSelectorVisible,
      initialStatus: afterSaleSelectorInitialStatus,
      onClose: closeAfterSaleSelector,
      onSelect: handleAfterSaleSelect
    },
    patientSelectorProps: {
      visible: patientSelectorVisible,
      onClose: closePatientSelector,
      onSelect: handlePatientSelect
    },
    handleMenuButtonClick,
    handleNewSessionClick,
    handleQuickAction
  }
}
