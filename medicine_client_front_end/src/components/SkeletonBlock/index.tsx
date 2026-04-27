import React from 'react'
import styles from './index.module.less'

interface SkeletonBlockProps {
  /** 额外样式类名，用于控制占位块尺寸和布局。 */
  className?: string
  /** 行内样式，用于少量动态尺寸场景。 */
  style?: React.CSSProperties
  /** 是否渲染为行内容器，用于替换文本字段。 */
  inline?: boolean
}

/**
 * 通用骨架屏占位块。
 *
 * @param props - 占位块样式参数。
 * @returns 骨架屏占位节点。
 */
const SkeletonBlock: React.FC<SkeletonBlockProps> = ({ className = '', style, inline = false }) => {
  if (inline) {
    return <span className={`${styles.skeletonBlock} ${className}`} style={style} aria-hidden='true' />
  }

  return <div className={`${styles.skeletonBlock} ${className}`} style={style} aria-hidden='true' />
}

export default SkeletonBlock
