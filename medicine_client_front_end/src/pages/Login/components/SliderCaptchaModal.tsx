import React, { useEffect, useMemo, useRef, useState } from 'react'
import { ArrowRight, RefreshCw, ShieldCheck, X } from 'lucide-react'
import {
  checkSliderCaptcha,
  getSliderCaptcha,
  type CaptchaVerificationResult,
  type SliderCaptchaChallenge,
  type SliderCaptchaCheckPayload,
  type SliderCaptchaTrackPayload,
  type SliderCaptchaTrackPoint
} from '@/api/captcha'
import styles from './SliderCaptchaModal.module.less'

/**
 * 滑块按钮宽度。
 */
const HANDLE_WIDTH = 56

/**
 * 滑块按钮高度。
 */
const HANDLE_HEIGHT = 42

/**
 * 验证码预览最大宽度。
 */
const PREVIEW_WIDTH = 360

/**
 * 校验失败后刷新前的等待时间（毫秒）。
 */
const RELOAD_DELAY_MS = 680

/**
 * 组件状态枚举。
 */
type SliderCaptchaStage = 'idle' | 'loading' | 'checking' | 'error' | 'success'

/**
 * 拖动上下文。
 */
interface DragContext {
  /** 指针 ID */
  pointerId: number
  /** 拖动开始时的页面 X 坐标 */
  startPageX: number
  /** 拖动开始时的时间戳 */
  startTime: number
  /** 拖动起始时的滑块偏移 */
  startHandleOffset: number
  /** 已采集的轨迹点 */
  trackList: SliderCaptchaTrackPoint[]
}

/**
 * 尺寸测量结果。
 */
interface CaptchaMetrics {
  /** 背景图渲染宽度 */
  previewWidth: number
  /** 背景图渲染高度 */
  previewHeight: number
  /** 拼图块渲染宽度 */
  pieceWidth: number
  /** 拼图块渲染高度 */
  pieceHeight: number
  /** 拼图块最大位移 */
  maxPieceOffset: number
}

/**
 * 组件属性。
 */
interface SliderCaptchaModalProps {
  /** 是否打开弹层 */
  open: boolean
  /** 关闭弹层回调 */
  onCancel: () => void
  /** 校验成功回调 */
  onVerified: (result: CaptchaVerificationResult) => void
  /** 校验成功后的动作描述，默认 "登录" */
  actionLabel?: string
}

/**
 * 页面滚动样式快照。
 */
interface ScrollLockSnapshot {
  /** html 原始 overflow */
  htmlOverflow: string
  /** body 原始 overflow */
  bodyOverflow: string
  /** body 原始 touch-action */
  bodyTouchAction: string
  /** body 原始 overscroll-behavior */
  bodyOverscrollBehavior: string
}

/**
 * 限制数值在指定区间内。
 *
 * @param value 当前值
 * @param min 最小值
 * @param max 最大值
 * @returns 限制后的值
 */
function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max)
}

/**
 * 读取事件页面坐标。
 *
 * @param event 指针事件
 * @returns 页面坐标
 */
function getPagePoint(event: PointerEvent | React.PointerEvent<HTMLElement>): { x: number; y: number } {
  return {
    x: window.scrollX + event.clientX,
    y: window.scrollY + event.clientY
  }
}

/**
 * 构建滑块轨迹点。
 *
 * @param event 指针事件
 * @param startTime 拖动开始时间
 * @param type 轨迹点类型
 * @returns 轨迹点
 */
function buildTrackPoint(
  event: PointerEvent | React.PointerEvent<HTMLElement>,
  startTime: number,
  type: SliderCaptchaTrackPoint['type']
): SliderCaptchaTrackPoint {
  const pagePoint = getPagePoint(event)
  return {
    x: pagePoint.x,
    y: pagePoint.y,
    t: Date.now() - startTime,
    type
  }
}

/**
 * 锁定页面滚动，避免弹层交互时带动背景页面。
 *
 * @returns 解锁页面滚动函数
 */
function lockPageScroll(): () => void {
  const htmlElement = document.documentElement
  const bodyElement = document.body
  const snapshot: ScrollLockSnapshot = {
    htmlOverflow: htmlElement.style.overflow,
    bodyOverflow: bodyElement.style.overflow,
    bodyTouchAction: bodyElement.style.touchAction,
    bodyOverscrollBehavior: bodyElement.style.overscrollBehavior
  }
  htmlElement.style.overflow = 'hidden'
  bodyElement.style.overflow = 'hidden'
  bodyElement.style.touchAction = 'none'
  bodyElement.style.overscrollBehavior = 'none'
  return () => {
    htmlElement.style.overflow = snapshot.htmlOverflow
    bodyElement.style.overflow = snapshot.bodyOverflow
    bodyElement.style.touchAction = snapshot.bodyTouchAction
    bodyElement.style.overscrollBehavior = snapshot.bodyOverscrollBehavior
  }
}

/**
 * 客户端登录滑块验证码弹层。
 *
 * @param props 组件属性
 * @returns React 节点
 */
const SliderCaptchaModal: React.FC<SliderCaptchaModalProps> = ({
  open,
  onCancel,
  onVerified,
  actionLabel = '登录'
}) => {
  /**
   * 弹层遮罩节点引用。
   */
  const maskRef = useRef<HTMLDivElement | null>(null)
  const previewRef = useRef<HTMLDivElement | null>(null)
  const pieceImageRef = useRef<HTMLImageElement | null>(null)
  const dragContextRef = useRef<DragContext | null>(null)
  const reloadTimerRef = useRef<number | null>(null)
  const pieceOffsetRef = useRef(0)
  const handleOffsetRef = useRef(0)
  const [challenge, setChallenge] = useState<SliderCaptchaChallenge | null>(null)
  const [metrics, setMetrics] = useState<CaptchaMetrics | null>(null)
  const [loadedImageCount, setLoadedImageCount] = useState(0)
  const [pieceOffset, setPieceOffset] = useState(0)
  const [handleOffset, setHandleOffset] = useState(0)
  const [stage, setStage] = useState<SliderCaptchaStage>('idle')
  const [statusMessage, setStatusMessage] = useState('按住滑块完成拼图验证')

  /**
   * 当前是否处于加载或校验状态。
   */
  const blocked = stage === 'loading' || stage === 'checking'

  /**
   * 当前背景图纵横比。
   */
  const previewAspectRatio = useMemo(() => {
    if (!challenge?.backgroundImageWidth || !challenge?.backgroundImageHeight) {
      return 5 / 3
    }
    return challenge.backgroundImageWidth / challenge.backgroundImageHeight
  }, [challenge?.backgroundImageWidth, challenge?.backgroundImageHeight])

  /**
   * 清空拖动状态。
   *
   * @returns void
   */
  const resetDragState = (): void => {
    dragContextRef.current = null
    pieceOffsetRef.current = 0
    handleOffsetRef.current = 0
    setPieceOffset(0)
    setHandleOffset(0)
  }

  /**
   * 清理自动刷新定时器。
   *
   * @returns void
   */
  const clearReloadTimer = (): void => {
    if (reloadTimerRef.current !== null) {
      window.clearTimeout(reloadTimerRef.current)
      reloadTimerRef.current = null
    }
  }

  /**
   * 解除拖动监听器。
   *
   * @returns void
   */
  const detachPointerListeners = (): void => {
    document.removeEventListener('pointermove', handlePointerMove)
    document.removeEventListener('pointerup', handlePointerUp)
    document.removeEventListener('pointercancel', handlePointerUp)
  }

  /**
   * 重新测量验证码区域尺寸。
   *
   * @returns void
   */
  const measureCaptchaMetrics = (): void => {
    const previewElement = previewRef.current
    const pieceImageElement = pieceImageRef.current
    if (!previewElement || !pieceImageElement) {
      return
    }
    const nextPreviewWidth = previewElement.clientWidth
    const nextPreviewHeight = previewElement.clientHeight
    const nextPieceWidth = pieceImageElement.clientWidth
    const nextPieceHeight = pieceImageElement.clientHeight
    if (!nextPreviewWidth || !nextPreviewHeight || !nextPieceWidth || !nextPieceHeight) {
      return
    }
    setMetrics({
      previewWidth: nextPreviewWidth,
      previewHeight: nextPreviewHeight,
      pieceWidth: nextPieceWidth,
      pieceHeight: nextPieceHeight,
      maxPieceOffset: Math.max(nextPreviewWidth - nextPieceWidth, 0)
    })
  }

  /**
   * 拉取新的验证码 challenge。
   *
   * @returns Promise<void>
   */
  const loadChallenge = async (): Promise<void> => {
    clearReloadTimer()
    detachPointerListeners()
    resetDragState()
    setChallenge(null)
    setMetrics(null)
    setLoadedImageCount(0)
    setStage('loading')
    setStatusMessage('正在加载验证码...')
    const response = await getSliderCaptcha()
    if (response.code !== 200 || !response.data) {
      setStage('error')
      setStatusMessage(response.msg || '验证码加载失败，请刷新重试')
      return
    }
    setChallenge(response.data)
    setStage('idle')
    setStatusMessage('按住滑块完成拼图验证')
  }

  /**
   * 提交滑块轨迹到后端。
   *
   * @param trackPayload 轨迹数据
   * @returns Promise<void>
   */
  const submitTrack = async (trackPayload: SliderCaptchaTrackPayload): Promise<void> => {
    if (!challenge) {
      return
    }
    setStage('checking')
    setStatusMessage('正在校验拖动轨迹...')
    const response = await checkSliderCaptcha({
      id: challenge.id,
      data: trackPayload
    } satisfies SliderCaptchaCheckPayload)
    if (response.code === 200 && response.data?.id) {
      setStage('success')
      setStatusMessage(`验证成功，正在继续${actionLabel}...`)
      onVerified(response.data)
      return
    }
    setStage('error')
    setStatusMessage(response.msg || '验证失败，正在刷新新拼图...')
    clearReloadTimer()
    reloadTimerRef.current = window.setTimeout(() => {
      void loadChallenge()
    }, RELOAD_DELAY_MS)
  }

  /**
   * 处理拖动过程。
   *
   * @param event 指针移动事件
   * @returns void
   */
  function handlePointerMove(event: PointerEvent): void {
    const dragContext = dragContextRef.current
    if (!dragContext || !metrics) {
      return
    }
    if (event.pointerId !== dragContext.pointerId) {
      return
    }
    if (event.cancelable) {
      event.preventDefault()
    }
    const pagePoint = getPagePoint(event)
    const deltaX = pagePoint.x - dragContext.startPageX
    const nextHandleOffset = clamp(dragContext.startHandleOffset + deltaX, 0, metrics.maxPieceOffset)
    const nextPieceOffset = nextHandleOffset
    handleOffsetRef.current = nextHandleOffset
    pieceOffsetRef.current = nextPieceOffset
    setHandleOffset(nextHandleOffset)
    setPieceOffset(nextPieceOffset)
    dragContext.trackList.push(buildTrackPoint(event, dragContext.startTime, 'move'))
  }

  /**
   * 处理拖动结束。
   *
   * @param event 指针抬起事件
   * @returns Promise<void>
   */
  async function handlePointerUp(event: PointerEvent): Promise<void> {
    const dragContext = dragContextRef.current
    if (!dragContext || !metrics || !challenge) {
      return
    }
    if (event.pointerId !== dragContext.pointerId) {
      return
    }
    if (event.cancelable) {
      event.preventDefault()
    }
    dragContext.trackList.push(buildTrackPoint(event, dragContext.startTime, 'up'))
    dragContextRef.current = null
    detachPointerListeners()
    const trackPayload: SliderCaptchaTrackPayload = {
      bgImageWidth: Math.round(metrics.previewWidth),
      bgImageHeight: Math.round(metrics.previewHeight),
      templateImageWidth: Math.round(metrics.pieceWidth),
      templateImageHeight: Math.round(metrics.pieceHeight),
      startTime: dragContext.startTime,
      stopTime: Date.now(),
      left: Math.round(pieceOffsetRef.current),
      top: 0,
      trackList: dragContext.trackList,
      data: challenge.data
    }
    await submitTrack(trackPayload)
  }

  /**
   * 处理滑块开始拖动。
   *
   * @param event 指针按下事件
   * @returns void
   */
  const handlePointerDown = (event: React.PointerEvent<HTMLButtonElement>): void => {
    if (!challenge || !metrics || blocked) {
      return
    }
    event.preventDefault()
    try {
      event.currentTarget.setPointerCapture(event.pointerId)
    } catch (error) {
      console.debug('setPointerCapture failed', error)
    }
    clearReloadTimer()
    const startTime = Date.now()
    dragContextRef.current = {
      pointerId: event.pointerId,
      startPageX: getPagePoint(event).x,
      startTime,
      startHandleOffset: handleOffsetRef.current,
      trackList: [buildTrackPoint(event, startTime, 'down')]
    }
    setStage('idle')
    setStatusMessage('继续拖动，松开后自动校验')
    document.addEventListener('pointermove', handlePointerMove, { passive: false })
    document.addEventListener('pointerup', handlePointerUp)
    document.addEventListener('pointercancel', handlePointerUp)
  }

  /**
   * 处理图片加载完成。
   *
   * @returns void
   */
  const handleImageLoaded = (): void => {
    setLoadedImageCount(currentCount => currentCount + 1)
  }

  /**
   * 刷新验证码 challenge。
   *
   * @returns void
   */
  const handleRefresh = (): void => {
    void loadChallenge()
  }

  /**
   * 关闭验证码弹层。
   *
   * @returns void
   */
  const handleClose = (): void => {
    clearReloadTimer()
    detachPointerListeners()
    resetDragState()
    onCancel()
  }

  useEffect(() => {
    if (!open) {
      clearReloadTimer()
      detachPointerListeners()
      resetDragState()
      setChallenge(null)
      setMetrics(null)
      setLoadedImageCount(0)
      setStage('idle')
      setStatusMessage('按住滑块完成拼图验证')
      return
    }
    void loadChallenge()
    return () => {
      clearReloadTimer()
      detachPointerListeners()
    }
  }, [open])

  useEffect(() => {
    if (!open) {
      return
    }
    return lockPageScroll()
  }, [open])

  useEffect(() => {
    if (!open) {
      return
    }
    const maskElement = maskRef.current
    if (!maskElement) {
      return
    }

    /**
     * 阻止遮罩层触摸滑动透传到背景页面。
     *
     * @param event 原生触摸移动事件
     * @returns void
     */
    const handleMaskNativeTouchMove = (event: TouchEvent): void => {
      if (event.cancelable) {
        event.preventDefault()
      }
    }

    maskElement.addEventListener('touchmove', handleMaskNativeTouchMove, { passive: false })
    return () => {
      maskElement.removeEventListener('touchmove', handleMaskNativeTouchMove)
    }
  }, [open])

  useEffect(() => {
    if (!open || loadedImageCount < 2) {
      return
    }
    measureCaptchaMetrics()
    const previewElement = previewRef.current
    if (!previewElement || typeof ResizeObserver === 'undefined') {
      return
    }
    const resizeObserver = new ResizeObserver(() => {
      measureCaptchaMetrics()
    })
    resizeObserver.observe(previewElement)
    return () => {
      resizeObserver.disconnect()
    }
  }, [loadedImageCount, open])

  if (!open) {
    return null
  }

  return (
    <div className={styles.mask} onClick={handleClose} ref={maskRef}>
      <div className={styles.panel} onClick={event => event.stopPropagation()}>
        <div className={styles.header}>
          <div className={styles.headerLeft}>
            <div className={styles.badge}>
              <ShieldCheck size={18} />
            </div>
            <div>
              <h3 className={styles.title}>安全验证</h3>
              <p className={styles.subtitle}>{`拖动滑块完成拼图后继续${actionLabel}`}</p>
            </div>
          </div>
          <div className={styles.headerActions}>
            <button aria-label='刷新验证码' className={styles.iconButton} onClick={handleRefresh} type='button'>
              <RefreshCw size={16} />
            </button>
            <button aria-label='关闭验证码' className={styles.iconButton} onClick={handleClose} type='button'>
              <X size={16} />
            </button>
          </div>
        </div>

        <div className={styles.body}>
          <p className={styles.statusText}>{statusMessage}</p>
          <div
            className={styles.previewFrame}
            ref={previewRef}
            style={{
              aspectRatio: `${previewAspectRatio}`,
              maxWidth: PREVIEW_WIDTH
            }}
          >
            {challenge ? (
              <>
                <img
                  alt='验证码背景图'
                  className={styles.previewImage}
                  onLoad={handleImageLoaded}
                  src={challenge.backgroundImage}
                />
                <div
                  className={styles.pieceLayer}
                  style={{
                    transform: `translate3d(${pieceOffset}px, 0, 0)`
                  }}
                >
                  <img
                    alt='验证码拼图块'
                    className={styles.pieceImage}
                    onLoad={handleImageLoaded}
                    ref={pieceImageRef}
                    src={challenge.templateImage}
                  />
                </div>
              </>
            ) : null}

            {stage === 'loading' ? <div className={styles.loadingCover}>加载中...</div> : null}
          </div>

          <div className={styles.sliderTrack}>
            <div
              className={styles.sliderFill}
              style={{
                width: `${handleOffset + HANDLE_WIDTH * 0.78}px`
              }}
            />
            <div className={styles.sliderHint}>按住滑块并平滑拖动</div>
            <button
              className={styles.sliderHandle}
              disabled={blocked || !challenge || !metrics}
              onPointerDown={handlePointerDown}
              style={{
                height: HANDLE_HEIGHT,
                transform: `translate3d(${handleOffset}px, 0, 0)`,
                width: HANDLE_WIDTH
              }}
              type='button'
            >
              <ArrowRight size={18} />
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

export default SliderCaptchaModal
