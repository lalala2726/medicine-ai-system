import { create } from 'zustand'

interface AssistantDrawerStore {
  isOpen: boolean
  openDrawer: () => void
  closeDrawer: () => void
  toggleDrawer: () => void
}

export const useAssistantDrawerStore = create<AssistantDrawerStore>(set => ({
  isOpen: false,
  openDrawer: () => set({ isOpen: true }),
  closeDrawer: () => set({ isOpen: false }),
  toggleDrawer: () => set(state => ({ isOpen: !state.isOpen }))
}))
