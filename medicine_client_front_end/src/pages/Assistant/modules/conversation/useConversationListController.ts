import { useCallback, useEffect, useRef, useState } from 'react'
import { Dialog } from '@nutui/nutui-react'
import { useChatStore } from '@/stores/chatStore'
import { useAssistantDrawerStore } from '@/stores/assistantDrawerStore'
import { showErrorNotify, showSuccessNotify } from '@/utils/notify'

/** 会话列表长按菜单的触发时长。 */
const CONVERSATION_LONG_PRESS_DURATION_MS = 500

/** 会话列表控制器对外暴露的数据与交互方法。 */
export interface ConversationListControllerResult {
  conversations: ReturnType<typeof useChatStore.getState>['conversations']
  activeConversationUuid: string | null
  conversationListError: string | null
  conversationListLoading: boolean
  conversationHasMore: boolean
  historyLoading: boolean
  isReplying: boolean
  activeActionId: string | null
  renameDialogOpen: boolean
  renameValue: string
  setRenameValue: (value: string) => void
  closeActionMenu: () => void
  closeRenameDialog: () => void
  handleNewConversation: () => void
  handleSelectConversation: (conversationUuid: string) => void
  handleLoadMore: () => Promise<void>
  handleRetryLoad: () => Promise<void>
  handlePressStart: (event: React.TouchEvent | React.MouseEvent, conversationUuid: string) => void
  handlePressEnd: () => void
  handlePressMove: () => void
  handleRenameClick: (event: React.MouseEvent, conversationUuid: string, currentTitle: string) => void
  handleDeleteClick: (event: React.MouseEvent, conversationUuid: string) => void
  confirmRename: () => Promise<void>
}

/**
 * 管理历史会话抽屉里的列表交互逻辑。
 * 组件层只负责渲染列表和弹层，不直接操作 store 或对话框副作用。
 */
export function useConversationListController(): ConversationListControllerResult {
  /** 历史会话列表数据。 */
  const conversations = useChatStore(state => state.conversations)
  /** 当前激活的会话 UUID。 */
  const activeConversationUuid = useChatStore(state => state.activeConversationUuid)
  /** 会话列表自身的加载状态。 */
  const conversationListLoading = useChatStore(state => state.conversationListLoading)
  /** 会话列表最近一次加载失败文案。 */
  const conversationListError = useChatStore(state => state.conversationListError)
  /** 是否还有更多历史会话。 */
  const conversationHasMore = useChatStore(state => state.conversationHasMore)
  /** 当前分页页码。 */
  const conversationPage = useChatStore(state => state.conversationPage)
  /** 当前页面生命周期内是否已经尝试过初始化会话列表。 */
  const conversationListInitialized = useChatStore(state => state.conversationListInitialized)
  /** 历史消息加载状态。 */
  const historyLoading = useChatStore(state => state.historyLoading)
  /** 是否正在生成回复。 */
  const isReplying = useChatStore(state => state.isReplying)

  /** 切换当前会话。 */
  const setActiveConversationUuid = useChatStore(state => state.setActiveConversationUuid)
  /** 开启新会话。 */
  const startNewConversation = useChatStore(state => state.startNewConversation)
  /** 加载会话列表。 */
  const loadConversationList = useChatStore(state => state.loadConversationList)
  /** 重命名会话。 */
  const renameConversationItem = useChatStore(state => state.renameConversationItem)
  /** 删除会话。 */
  const deleteConversationItem = useChatStore(state => state.deleteConversationItem)

  /** 关闭抽屉。 */
  const closeDrawer = useAssistantDrawerStore(state => state.closeDrawer)

  /** 当前展开长按菜单的会话 ID。 */
  const [activeActionId, setActiveActionId] = useState<string | null>(null)
  /** 重命名对话框是否打开。 */
  const [renameDialogOpen, setRenameDialogOpen] = useState(false)
  /** 重命名输入框当前值。 */
  const [renameValue, setRenameValue] = useState('')
  /** 当前正在被重命名的会话 ID。 */
  const [renamingId, setRenamingId] = useState<string | null>(null)

  /** 长按计时器。 */
  const pressTimer = useRef<number | null>(null)

  /**
   * 判断当前是否需要暂时锁定会话切换相关交互。
   *
   * @returns 当前是否应禁止切换、新建或长按操作
   */
  const isConversationInteractionLocked = useCallback(() => {
    if (!isReplying && !historyLoading) {
      return false
    }

    return true
  }, [historyLoading, isReplying])

  /** 关闭当前长按菜单。 */
  const closeActionMenu = useCallback(() => {
    setActiveActionId(null)
  }, [])

  /** 关闭重命名对话框。 */
  const closeRenameDialog = useCallback(() => {
    setRenameDialogOpen(false)
    setRenamingId(null)
    setRenameValue('')
  }, [])

  /** 新建会话并关闭抽屉。 */
  const handleNewConversation = useCallback(() => {
    if (isConversationInteractionLocked()) {
      return
    }

    startNewConversation()
    closeDrawer()
  }, [closeDrawer, isConversationInteractionLocked, startNewConversation])

  /** 选择某一条历史会话。 */
  const handleSelectConversation = useCallback(
    (conversationUuid: string) => {
      if (activeActionId) {
        setActiveActionId(null)
        return
      }

      if (isConversationInteractionLocked()) {
        return
      }

      setActiveConversationUuid(conversationUuid)
      closeDrawer()
    },
    [activeActionId, closeDrawer, isConversationInteractionLocked, setActiveConversationUuid]
  )

  /** 加载更多历史会话。 */
  const handleLoadMore = useCallback(async () => {
    try {
      if (conversationHasMore && !conversationListLoading) {
        await loadConversationList(false)
      }
    } catch (error) {
      console.error(error)
    }
  }, [conversationHasMore, conversationListLoading, loadConversationList])

  /** 手动重试拉取第一页历史会话。 */
  const handleRetryLoad = useCallback(async () => {
    try {
      await loadConversationList(true)
    } catch (error) {
      console.error(error)
      showErrorNotify('历史会话加载失败，请稍后重试')
    }
  }, [loadConversationList])

  /**
   * 当会话列表为空时，自动触发首次加载。
   * 失败后不再自动重试，避免接口未部署时持续刷请求。
   */
  useEffect(() => {
    if (
      !conversationListInitialized &&
      conversations.length === 0 &&
      conversationPage === 1 &&
      !conversationListLoading
    ) {
      void loadConversationList(true).catch(() => undefined)
    }
  }, [
    conversationListInitialized,
    conversationListLoading,
    conversationPage,
    conversations.length,
    loadConversationList
  ])

  /** 开始长按计时。 */
  const handlePressStart = useCallback(
    (_: React.TouchEvent | React.MouseEvent, conversationUuid: string) => {
      if (isConversationInteractionLocked()) {
        return
      }

      if (pressTimer.current) {
        window.clearTimeout(pressTimer.current)
      }

      pressTimer.current = window.setTimeout(() => {
        setActiveActionId(conversationUuid)
      }, CONVERSATION_LONG_PRESS_DURATION_MS)
    },
    [isConversationInteractionLocked]
  )

  /** 结束长按计时。 */
  const handlePressEnd = useCallback(() => {
    if (pressTimer.current) {
      window.clearTimeout(pressTimer.current)
      pressTimer.current = null
    }
  }, [])

  /** 滑动时取消长按计时。 */
  const handlePressMove = useCallback(() => {
    if (pressTimer.current) {
      window.clearTimeout(pressTimer.current)
      pressTimer.current = null
    }
  }, [])

  /** 打开重命名对话框。 */
  const handleRenameClick = useCallback((event: React.MouseEvent, conversationUuid: string, currentTitle: string) => {
    event.stopPropagation()
    setRenamingId(conversationUuid)
    setRenameValue(currentTitle)
    setRenameDialogOpen(true)
    setActiveActionId(null)
  }, [])

  /** 删除指定会话。 */
  const handleDeleteClick = useCallback(
    (event: React.MouseEvent, conversationUuid: string) => {
      event.stopPropagation()
      setActiveActionId(null)

      Dialog.confirm({
        title: '确认删除',
        content: '确定要删除这条会话记录吗？',
        className: 'delete-confirm-dialog',
        onConfirm: async () => {
          const success = await deleteConversationItem(conversationUuid)

          if (success) {
            showSuccessNotify('删除成功')
          } else {
            showErrorNotify('删除失败，请重试')
          }
        }
      })
    },
    [deleteConversationItem]
  )

  /** 提交会话重命名。 */
  const confirmRename = useCallback(async () => {
    if (!renamingId || !renameValue.trim()) {
      return
    }

    const success = await renameConversationItem(renamingId, renameValue.trim())

    if (success) {
      showSuccessNotify('重命名成功')
    } else {
      showErrorNotify('重命名失败，请重试')
    }

    closeRenameDialog()
  }, [closeRenameDialog, renameConversationItem, renameValue, renamingId])

  return {
    conversations,
    activeConversationUuid,
    conversationListError,
    conversationListLoading,
    conversationHasMore,
    historyLoading,
    isReplying,
    activeActionId,
    renameDialogOpen,
    renameValue,
    setRenameValue,
    closeActionMenu,
    closeRenameDialog,
    handleNewConversation,
    handleSelectConversation,
    handleLoadMore,
    handleRetryLoad,
    handlePressStart,
    handlePressEnd,
    handlePressMove,
    handleRenameClick,
    handleDeleteClick,
    confirmRename
  }
}
