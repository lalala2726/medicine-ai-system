import { createContext, useContext } from 'react'

/** Assistant TTS Context 暴露的能力集合。 */
export interface AssistantTtsContextValue {
  /** 切换某条消息的语音播放状态。 */
  toggleMessagePlayback: (messageId?: string) => Promise<void>
  /** 判断某条消息当前是否正在播放。 */
  isMessagePlaying: (messageId?: string) => boolean
  /** 判断某条消息当前是否正在加载。 */
  isMessageLoading: (messageId?: string) => boolean
  /** 停止当前语音播放。 */
  stopPlayback: () => void
}

/** Assistant TTS 播放上下文。 */
export const AssistantTtsContext = createContext<AssistantTtsContextValue | null>(null)

/**
 * 读取 Assistant 页面级 TTS 控制上下文。
 *
 * @returns TTS 控制能力集合
 */
export const useAssistantTtsPlayback = () => {
  const context = useContext(AssistantTtsContext)

  if (!context) {
    throw new Error('useAssistantTtsPlayback must be used within AssistantTtsProvider')
  }

  return context
}
