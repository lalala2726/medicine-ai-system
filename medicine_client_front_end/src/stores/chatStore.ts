import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { ChatMessage } from '@/pages/Assistant/modules/messages/chatTypes'
import {
  fetchConversationListPage,
  renameConversation,
  deleteConversation,
  CONVERSATION_PAGE_SIZE
} from '@/api/assistant'
import type { AssistantTypes } from '@/api/assistant/contract'

interface ChatStore {
  activeConversationUuid: string | null
  conversations: AssistantTypes.ConversationItem[]
  conversationPage: number
  conversationHasMore: boolean
  /** 当前页面生命周期内是否已经主动尝试过加载会话列表。 */
  conversationListInitialized: boolean
  conversationListLoading: boolean
  conversationListError: string | null
  historyLoading: boolean
  historyError: string | null
  isReplying: boolean
  setActiveConversationUuid: (conversationUuid: string | null) => void
  setHistoryLoading: (loading: boolean) => void
  setHistoryError: (error: string | null) => void
  setIsReplying: (replying: boolean) => void
  upsertConversation: (conversation: AssistantTypes.ConversationItem) => void
  startNewConversation: () => void
  loadConversationList: (reset?: boolean) => Promise<AssistantTypes.ConversationItem[]>
  renameConversationItem: (uuid: string, newTitle: string) => Promise<boolean>
  deleteConversationItem: (uuid: string) => Promise<boolean>
}

const resolveErrorMessage = (error: unknown) => {
  if (error instanceof Error && error.message.trim()) {
    return error.message
  }

  return '加载历史对话失败，请稍后重试'
}

export const initialMessages: ChatMessage[] = []

export const useChatStore = create<ChatStore>()(
  persist(
    (set, get) => ({
      activeConversationUuid: null,
      conversations: [],
      conversationPage: 1,
      conversationHasMore: true,
      conversationListInitialized: false,
      conversationListLoading: false,
      conversationListError: null,
      historyLoading: false,
      historyError: null,
      isReplying: false,
      setActiveConversationUuid: conversationUuid => set({ activeConversationUuid: conversationUuid }),
      setHistoryLoading: loading => set({ historyLoading: loading }),
      setHistoryError: error => set({ historyError: error }),
      setIsReplying: replying => set({ isReplying: replying }),
      upsertConversation: conversation =>
        set(state => ({
          activeConversationUuid: conversation.conversationUuid,
          conversations: [
            conversation,
            ...state.conversations.filter(item => item.conversationUuid !== conversation.conversationUuid)
          ]
        })),
      startNewConversation: () =>
        set({
          activeConversationUuid: null,
          historyError: null,
          historyLoading: false
        }),
      loadConversationList: async (reset = false) => {
        const state = get()
        if (state.conversationListLoading || (!reset && !state.conversationHasMore)) {
          return state.conversations
        }

        const targetPage = reset ? 1 : state.conversationPage

        set({
          conversationListInitialized: true,
          conversationListLoading: true,
          conversationListError: null
        })

        try {
          const { items, hasMore } = await fetchConversationListPage(targetPage, CONVERSATION_PAGE_SIZE)
          const activeConversationUuid = get().activeConversationUuid

          set(prev => {
            const newConversations = reset ? items : [...prev.conversations, ...items]
            // remove duplicates if any
            const uniqueConversations = Array.from(new Map(newConversations.map(c => [c.conversationUuid, c])).values())

            return {
              conversations: uniqueConversations,
              activeConversationUuid: activeConversationUuid ?? null,
              conversationListInitialized: true,
              conversationListLoading: false,
              conversationListError: null,
              conversationHasMore: hasMore,
              conversationPage: targetPage + 1
            }
          })

          return get().conversations
        } catch (error) {
          set({
            conversationListInitialized: true,
            conversationListLoading: false,
            conversationListError: resolveErrorMessage(error)
          })
          throw error
        }
      },
      renameConversationItem: async (uuid: string, newTitle: string) => {
        try {
          const updatedConversation = await renameConversation(uuid, newTitle)

          set(state => ({
            conversations: state.conversations.map(conversation =>
              conversation.conversationUuid === updatedConversation.conversationUuid
                ? { ...conversation, title: updatedConversation.title }
                : conversation
            )
          }))

          return true
        } catch (error) {
          console.error('Failed to rename conversation', error)
          return false
        }
      },
      deleteConversationItem: async (uuid: string) => {
        try {
          const deletedConversationUuid = await deleteConversation(uuid)

          set(state => ({
            conversations: state.conversations.filter(
              conversation => conversation.conversationUuid !== deletedConversationUuid
            ),
            activeConversationUuid:
              state.activeConversationUuid === deletedConversationUuid ? null : state.activeConversationUuid
          }))

          return true
        } catch (error) {
          console.error('Failed to delete conversation', error)
          return false
        }
      }
    }),
    {
      name: 'assistant-session-storage',
      partialize: state => ({
        activeConversationUuid: state.activeConversationUuid,
        conversations: state.conversations
      })
    }
  )
)
