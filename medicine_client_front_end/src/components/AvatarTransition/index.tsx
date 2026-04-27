import React, { useEffect, useRef, useState } from 'react'
import { useAvatarTransitionStore } from '@/stores/avatarTransitionStore'
import styles from './AvatarTransition.module.less'

interface AvatarTransitionProps {
  targetRef: React.RefObject<HTMLElement | null>
  /** 期望的动画方向，只有匹配时才播放 */
  expectedDirection?: 'forward' | 'backward'
}

const AvatarTransition: React.FC<AvatarTransitionProps> = ({ targetRef, expectedDirection = 'forward' }) => {
  const { sourceRect, avatarUrl, isAnimating, direction, endAnimation, clear } = useAvatarTransitionStore()
  const [style, setStyle] = useState<React.CSSProperties | null>(null)
  const animatingRef = useRef(false)

  useEffect(() => {
    // 只有方向匹配时才播放动画
    if (!sourceRect || !avatarUrl || !targetRef.current || animatingRef.current) return
    if (direction !== expectedDirection) return

    animatingRef.current = true

    // 获取目标位置
    const targetElement = targetRef.current
    const targetRect = targetElement.getBoundingClientRect()

    // 设置初始位置（源位置）- 这次同步设置
    const initialStyle: React.CSSProperties = {
      position: 'fixed',
      top: sourceRect.top,
      left: sourceRect.left,
      width: sourceRect.width,
      height: sourceRect.height,
      borderRadius: '50%',
      zIndex: 9999,
      transition: 'none',
      opacity: 1
    }
    setStyle(initialStyle)

    // 开始动画
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        setStyle({
          position: 'fixed',
          top: targetRect.top,
          left: targetRect.left,
          width: targetRect.width,
          height: targetRect.height,
          borderRadius: '50%',
          zIndex: 9999,
          transition: 'all 0.35s cubic-bezier(0.4, 0, 0.2, 1)',
          opacity: 1
        })

        // 动画结束后清理
        setTimeout(() => {
          endAnimation()
          clear()
          setStyle(null)
          animatingRef.current = false
        }, 350)
      })
    })
  }, [sourceRect, avatarUrl, targetRef, direction, expectedDirection, endAnimation, clear])

  // 只有方向匹配、正在动画、且样式已计算完成时才渲染
  if (!sourceRect || !avatarUrl || !isAnimating || direction !== expectedDirection || !style) {
    return null
  }

  return <img src={avatarUrl} alt='avatar transition' className={styles.transitionAvatar} style={style} />
}

export default AvatarTransition
