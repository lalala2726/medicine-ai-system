import React, { useEffect, useMemo, useRef, useState } from 'react'
import type { CSSProperties, ReactNode, TouchEvent as ReactTouchEvent } from 'react'
import { ArrowDown, Check, LoaderCircle } from 'lucide-react'
import styles from './index.module.less'

/**
 * 下拉刷新工作模式。
 */
export type PullRefreshMode = 'self' | 'external'

/**
 * 下拉刷新状态。
 */
type PullRefreshStatus = 'idle' | 'pulling' | 'ready' | 'refreshing' | 'complete'

/**
 * 手势状态快照。
 */
interface PullRefreshGestureState {
  /** 手势起始横坐标。 */
  startX: number
  /** 手势起始纵坐标。 */
  startY: number
  /** 当前是否已经进入下拉刷新拖拽态。 */
  active: boolean
  /** 当前手势是否已被判定为不可触发下拉刷新。 */
  blocked: boolean
}

/**
 * 下拉刷新组件属性。
 */
export interface PullRefreshProps {
  /** 下拉刷新时执行的异步回调。 */
  onRefresh: () => Promise<void>
  /** 刷新区域内容。 */
  children: ReactNode
  /** 工作模式。 */
  mode?: PullRefreshMode
  /** 是否禁用下拉刷新。 */
  disabled?: boolean
  /** 外层自定义类名。 */
  className?: string
  /** 外层内联样式。 */
  style?: CSSProperties
  /** 外部滚动容器引用，仅在 external 模式下使用。 */
  scrollTargetRef?: React.RefObject<HTMLElement | null>
  /** 触发刷新的阈值。 */
  threshold?: number
  /** 最大可下拉距离。 */
  maxPullDistance?: number
  /** 刷新完成态保留时长。 */
  completeDelay?: number
  /** 需要忽略下拉刷新的选择器。 */
  ignoreSelector?: string
}

/**
 * 默认触发刷新阈值。
 */
const DEFAULT_THRESHOLD = 72

/**
 * 默认最大下拉距离。
 */
const DEFAULT_MAX_PULL_DISTANCE = 120

/**
 * 默认完成态保留时长，单位毫秒。
 */
const DEFAULT_COMPLETE_DELAY = 300

/**
 * 下拉头部区域高度。
 */
const HEAD_HEIGHT = 56

/**
 * 手势判定最小位移。
 */
const DIRECTION_LOCK_DISTANCE = 6

/**
 * 默认忽略下拉刷新的节点选择器。
 */
const DEFAULT_IGNORE_SELECTOR = [
  'input',
  'textarea',
  'select',
  '[data-pull-refresh-lock="true"]',
  '.nut-swipe',
  '.nut-swipe-wrapper',
  '.nut-swipe-content',
  '.nut-swipe-left',
  '.nut-swipe-right'
].join(', ')

/**
 * 生成带阻尼的下拉位移。
 *
 * @param distance 原始下拉距离。
 * @param threshold 触发刷新的阈值。
 * @param maxPullDistance 最大允许下拉距离。
 * @returns 带阻尼的下拉位移。
 */
const resolveDampedOffset = (distance: number, threshold: number, maxPullDistance: number): number => {
  if (distance <= 0) {
    return 0
  }

  if (distance <= threshold) {
    return Math.min(distance, maxPullDistance)
  }

  const overshootDistance = distance - threshold
  const dampedDistance = threshold + overshootDistance * 0.35

  return Math.min(dampedDistance, maxPullDistance)
}

/**
 * 生成当前状态对应的提示文案。
 *
 * @param status 当前刷新状态。
 * @returns 对应的提示文案。
 */
const resolveStatusText = (status: PullRefreshStatus): string => {
  switch (status) {
    case 'ready':
      return '松开立即刷新'
    case 'refreshing':
      return '刷新中...'
    case 'complete':
      return '刷新完成'
    case 'pulling':
      return '下拉即可刷新'
    default:
      return '下拉即可刷新'
  }
}

/**
 * 自研下拉刷新组件。
 *
 * @param props 组件属性。
 * @returns React 节点。
 */
const PullRefresh: React.FC<PullRefreshProps> = ({
  onRefresh,
  children,
  mode = 'self',
  disabled = false,
  className,
  style,
  scrollTargetRef,
  threshold = DEFAULT_THRESHOLD,
  maxPullDistance = DEFAULT_MAX_PULL_DISTANCE,
  completeDelay = DEFAULT_COMPLETE_DELAY,
  ignoreSelector = DEFAULT_IGNORE_SELECTOR
}) => {
  const rootRef = useRef<HTMLDivElement>(null)
  const completeTimerRef = useRef<number | null>(null)
  const gestureStateRef = useRef<PullRefreshGestureState>({
    startX: 0,
    startY: 0,
    active: false,
    blocked: false
  })
  const refreshingRef = useRef(false)
  const offsetRef = useRef(0)
  const [offset, setOffset] = useState(0)
  const [status, setStatus] = useState<PullRefreshStatus>('idle')

  /**
   * 清理完成态定时器。
   *
   * @returns 无返回值。
   */
  const clearCompleteTimer = (): void => {
    if (completeTimerRef.current !== null) {
      window.clearTimeout(completeTimerRef.current)
      completeTimerRef.current = null
    }
  }

  /**
   * 同步刷新当前位移状态。
   *
   * @param nextOffset 下一次位移值。
   * @returns 无返回值。
   */
  const updateOffset = (nextOffset: number): void => {
    offsetRef.current = nextOffset
    setOffset(nextOffset)
  }

  /**
   * 读取当前生效的滚动容器。
   *
   * @returns 当前滚动容器。
   */
  const resolveScrollElement = (): HTMLElement | null => {
    if (mode === 'self') {
      return rootRef.current
    }

    return scrollTargetRef?.current ?? null
  }

  /**
   * 判断当前节点是否需要忽略下拉刷新。
   *
   * @param target 当前事件触发节点。
   * @returns 当前节点是否需要忽略。
   */
  const shouldIgnoreTarget = (target: EventTarget | null): boolean => {
    if (!(target instanceof Element)) {
      return false
    }

    return Boolean(target.closest(ignoreSelector))
  }

  /**
   * 重置当前手势状态。
   *
   * @returns 无返回值。
   */
  const resetGestureState = (): void => {
    gestureStateRef.current = {
      startX: 0,
      startY: 0,
      active: false,
      blocked: false
    }
  }

  /**
   * 结束当前交互并回弹。
   *
   * @returns 无返回值。
   */
  const resetToIdle = (): void => {
    setStatus('idle')
    updateOffset(0)
  }

  /**
   * 完整执行一次刷新流程。
   *
   * @returns 无返回值。
   */
  const runRefresh = async (): Promise<void> => {
    if (refreshingRef.current) {
      return
    }

    clearCompleteTimer()
    refreshingRef.current = true
    setStatus('refreshing')
    updateOffset(HEAD_HEIGHT)

    try {
      await onRefresh()
    } finally {
      setStatus('complete')
      updateOffset(HEAD_HEIGHT)
      completeTimerRef.current = window.setTimeout(() => {
        refreshingRef.current = false
        resetToIdle()
      }, completeDelay)
    }
  }

  /**
   * 处理触摸开始。
   *
   * @param event 触摸事件。
   * @returns 无返回值。
   */
  const handleTouchStart = (event: ReactTouchEvent<HTMLDivElement>): void => {
    if (disabled || refreshingRef.current) {
      return
    }

    const touch = event.touches[0]
    const scrollElement = resolveScrollElement()

    if (!touch || !scrollElement) {
      return
    }

    gestureStateRef.current = {
      startX: touch.clientX,
      startY: touch.clientY,
      active: false,
      blocked: scrollElement.scrollTop > 0 || shouldIgnoreTarget(event.target)
    }
  }

  /**
   * 处理触摸移动。
   *
   * @param event 触摸事件。
   * @returns 无返回值。
   */
  const handleTouchMove = (event: ReactTouchEvent<HTMLDivElement>): void => {
    if (disabled || refreshingRef.current) {
      return
    }

    const touch = event.touches[0]
    const scrollElement = resolveScrollElement()

    if (!touch || !scrollElement) {
      return
    }

    const gestureState = gestureStateRef.current

    if (gestureState.blocked) {
      return
    }

    const deltaX = touch.clientX - gestureState.startX
    const deltaY = touch.clientY - gestureState.startY

    if (!gestureState.active) {
      if (Math.abs(deltaX) < DIRECTION_LOCK_DISTANCE && Math.abs(deltaY) < DIRECTION_LOCK_DISTANCE) {
        return
      }

      if (Math.abs(deltaX) > Math.abs(deltaY) || deltaY <= 0 || scrollElement.scrollTop > 0) {
        gestureState.blocked = true
        return
      }

      gestureState.active = true
    }

    event.preventDefault()
    const nextOffset = resolveDampedOffset(deltaY, threshold, maxPullDistance)
    updateOffset(nextOffset)
    setStatus(nextOffset >= threshold ? 'ready' : 'pulling')
  }

  /**
   * 处理触摸结束。
   *
   * @returns 无返回值。
   */
  const handleTouchEnd = (): void => {
    const gestureState = gestureStateRef.current

    if (!gestureState.active) {
      resetGestureState()
      return
    }

    const shouldTriggerRefresh = offsetRef.current >= threshold
    resetGestureState()

    if (shouldTriggerRefresh) {
      void runRefresh()
      return
    }

    resetToIdle()
  }

  /**
   * 处理触摸取消。
   *
   * @returns 无返回值。
   */
  const handleTouchCancel = (): void => {
    resetGestureState()

    if (!refreshingRef.current) {
      resetToIdle()
    }
  }

  /**
   * 计算当前进度。
   */
  const progress = useMemo(() => {
    if (threshold <= 0) {
      return 0
    }

    return Math.min(offset / threshold, 1)
  }, [offset, threshold])

  /**
   * 生成当前状态对应的图标节点。
   *
   * @returns 图标节点。
   */
  const renderIcon = (): ReactNode => {
    if (status === 'refreshing') {
      return <LoaderCircle className={`${styles.icon} ${styles.iconRefreshing}`} size={16} />
    }

    if (status === 'complete') {
      return <Check className={`${styles.icon} ${styles.iconComplete}`} size={16} />
    }

    return <ArrowDown className={`${styles.icon} ${status === 'ready' ? styles.iconReady : ''}`} size={16} />
  }

  /**
   * 组件卸载时清理资源。
   *
   * @returns 无返回值。
   */
  useEffect(() => {
    return () => {
      clearCompleteTimer()
    }
  }, [])

  const rootClassName = [styles.root, mode === 'self' ? styles.selfMode : '', className].filter(Boolean).join(' ')
  const trackClassName = [styles.track, status === 'pulling' || status === 'ready' ? '' : styles.trackAnimating]
    .filter(Boolean)
    .join(' ')
  const rootStyle = {
    ...style,
    '--pull-refresh-head-height': `${HEAD_HEIGHT}px`
  } as CSSProperties

  return (
    <div
      ref={rootRef}
      className={rootClassName}
      style={rootStyle}
      onTouchStart={handleTouchStart}
      onTouchMove={handleTouchMove}
      onTouchEnd={handleTouchEnd}
      onTouchCancel={handleTouchCancel}
    >
      <div className={trackClassName} style={{ transform: `translate3d(0, ${offset}px, 0)` }}>
        <div className={styles.head} style={{ opacity: status === 'idle' ? progress : 1 }}>
          <span className={styles.iconWrap}>{renderIcon()}</span>
          <span className={styles.text}>{resolveStatusText(status)}</span>
        </div>
        <div className={styles.content}>{children}</div>
      </div>
    </div>
  )
}

export default PullRefresh
