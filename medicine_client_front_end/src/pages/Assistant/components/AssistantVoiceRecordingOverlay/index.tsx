import { useEffect, useState, type CSSProperties } from 'react'
import { createPortal } from 'react-dom'
import styles from './index.module.less'

/** 录音波形柱子的高度配置。 */
const VOICE_WAVE_BAR_HEIGHTS = Array.from({ length: 21 }, () => 8)

/** 录音准备中的提示文案。 */
const VOICE_PREPARING_TEXT = '准备中...'

/** 录音态提示文案。 */
const VOICE_HINT_TEXT = '松开完成识别 上滑取消'

/** 取消录音态提示文案。 */
const VOICE_CANCEL_HINT_TEXT = '松开取消 下滑恢复识别'

/** 语音录音浮层组件属性。 */
export interface AssistantVoiceRecordingOverlayProps {
  /** 当前录音浮层是否显示。 */
  visible: boolean
  /** 当前是否仍在等待录音链路准备完成。 */
  isPreparing: boolean
  /** 当前是否处于松手取消状态。 */
  isCancelArmed: boolean
  /** 当前录音浮层需要展示的波形高度数组。 */
  waveHeights: number[]
}

/**
 * Assistant 语音录音占位浮层。
 * 组件处理基于真实声音的波纹和匹配输入框位置的展示逻辑。
 *
 * @param props - 组件属性
 * @returns 录音浮层节点
 */
const AssistantVoiceRecordingOverlay = ({
  visible,
  isPreparing,
  isCancelArmed,
  waveHeights
}: AssistantVoiceRecordingOverlayProps) => {
  const [triggerRect, setTriggerRect] = useState<DOMRect | null>(null)

  useEffect(() => {
    if (visible) {
      const el = document.getElementById('assistant-composer-bar')
      if (el) {
        const rect = el.getBoundingClientRect()
        // 始终使浮层贴合底部的输入操作行，限制最大高度为默认操作行高，避免被图片预览撑得过高
        const rowHeight = 44
        const height = Math.min(rect.height, rowHeight)
        const triggerRectOverride = {
          left: rect.left,
          top: rect.bottom - height,
          right: rect.right,
          bottom: rect.bottom,
          width: rect.width,
          height: height
        }
        setTriggerRect(triggerRectOverride as DOMRect)
      }
    }
  }, [visible])

  if (!visible || typeof document === 'undefined') {
    return null
  }

  /** 当前浮层实际要渲染的波形高度数组。 */
  const renderedWaveHeights = waveHeights.length > 0 ? waveHeights : VOICE_WAVE_BAR_HEIGHTS
  /** 当前录音浮层展示的提示文案。 */
  const currentHintText =
    isPreparing && !isCancelArmed ? VOICE_PREPARING_TEXT : isCancelArmed ? VOICE_CANCEL_HINT_TEXT : VOICE_HINT_TEXT

  return createPortal(
    <div className={styles.overlay} aria-hidden='true'>
      {triggerRect && (
        <div
          className={styles.dynamicContainer}
          style={{
            left: triggerRect.left,
            top: triggerRect.top,
            width: triggerRect.width,
            height: triggerRect.height
          }}
        >
          <div className={styles.overlayContent}>
            <div className={styles.waveContainer}>
              {isPreparing && !isCancelArmed ? (
                <div className={styles.spinner} />
              ) : (
                <div className={styles.waveform}>
                  {renderedWaveHeights.map((waveHeight, index) => {
                    const waveBarStyle: CSSProperties = {
                      height: `${waveHeight}px`
                    }

                    return (
                      <span
                        key={index}
                        className={`${styles.waveBar} ${isCancelArmed ? styles.waveBarCancel : ''}`}
                        style={waveBarStyle}
                      />
                    )
                  })}
                </div>
              )}
            </div>

            <p className={`${styles.hintText} ${isCancelArmed ? styles.hintTextCancel : ''}`}>{currentHintText}</p>
          </div>

          <div className={`${styles.sliderTrack} ${isCancelArmed ? styles.sliderTrackCancel : ''}`} />
        </div>
      )}
    </div>,
    document.body
  )
}

export default AssistantVoiceRecordingOverlay
