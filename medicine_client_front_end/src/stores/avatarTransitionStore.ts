import { create } from 'zustand'

interface AvatarRect {
  top: number
  left: number
  width: number
  height: number
}

type AnimationDirection = 'forward' | 'backward'

interface AvatarTransitionStore {
  sourceRect: AvatarRect | null
  avatarUrl: string | null
  isAnimating: boolean
  direction: AnimationDirection
  setSourceRect: (rect: AvatarRect, url: string, direction?: AnimationDirection) => void
  startAnimation: () => void
  endAnimation: () => void
  clear: () => void
}

export const useAvatarTransitionStore = create<AvatarTransitionStore>(set => ({
  sourceRect: null,
  avatarUrl: null,
  isAnimating: false,
  direction: 'forward',
  setSourceRect: (rect, url, direction = 'forward') => set({ sourceRect: rect, avatarUrl: url, direction }),
  startAnimation: () => set({ isAnimating: true }),
  endAnimation: () => set({ isAnimating: false }),
  clear: () => set({ sourceRect: null, avatarUrl: null, isAnimating: false, direction: 'forward' })
}))
