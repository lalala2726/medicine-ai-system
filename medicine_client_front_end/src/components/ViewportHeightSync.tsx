import { useLayoutEffect } from 'react'
import type { FC } from 'react'

/** 动态视口高度写入到根节点时使用的 CSS 变量名。 */
const APP_VIEWPORT_HEIGHT_CSS_VARIABLE = '--app-viewport-height'

/**
 * 读取当前浏览器真实可用的可视高度。
 *
 * @returns 当前应写入 CSS 变量的视口高度
 */
const resolveViewportHeight = (): number => {
  if (typeof window === 'undefined') {
    return 0
  }

  /** 当前浏览器暴露的 VisualViewport 高度。 */
  const visualViewportHeight = window.visualViewport?.height ?? 0

  if (visualViewportHeight > 0) {
    return visualViewportHeight
  }

  return window.innerHeight
}

/**
 * 将数值型视口高度转换为 CSS 可用的像素字符串。
 *
 * @param viewportHeight - 当前需要写入的视口高度
 * @returns 格式化后的像素字符串
 */
const formatViewportHeight = (viewportHeight: number): string => {
  return `${Math.round(viewportHeight)}px`
}

/**
 * 全局同步移动端真实可视高度，避免 `100vh` 在浏览器地址栏伸缩时出现高度误差。
 *
 * @returns 不渲染可见节点
 */
const ViewportHeightSync: FC = () => {
  useLayoutEffect(() => {
    /** 当前文档根节点。 */
    const rootElement = document.documentElement
    /** 当前注册到下一帧的同步任务 id。 */
    let animationFrameId = 0
    /** 当前最后一次写入根节点的高度值。 */
    let lastViewportHeight = ''
    /** 当前浏览器暴露的 VisualViewport 实例。 */
    const visualViewport = window.visualViewport

    /**
     * 将最新可视高度写入根节点 CSS 变量。
     *
     * @returns 无返回值
     */
    const syncViewportHeight = (): void => {
      window.cancelAnimationFrame(animationFrameId)
      animationFrameId = window.requestAnimationFrame(() => {
        /** 当前应使用的真实可视高度。 */
        const viewportHeight = resolveViewportHeight()

        if (viewportHeight <= 0) {
          return
        }

        /** 当前格式化后的视口高度字符串。 */
        const nextViewportHeight = formatViewportHeight(viewportHeight)

        if (nextViewportHeight === lastViewportHeight) {
          return
        }

        rootElement.style.setProperty(APP_VIEWPORT_HEIGHT_CSS_VARIABLE, nextViewportHeight)
        lastViewportHeight = nextViewportHeight
      })
    }

    syncViewportHeight()
    window.addEventListener('resize', syncViewportHeight)
    window.addEventListener('orientationchange', syncViewportHeight)
    visualViewport?.addEventListener('resize', syncViewportHeight)
    visualViewport?.addEventListener('scroll', syncViewportHeight)

    return () => {
      window.cancelAnimationFrame(animationFrameId)
      window.removeEventListener('resize', syncViewportHeight)
      window.removeEventListener('orientationchange', syncViewportHeight)
      visualViewport?.removeEventListener('resize', syncViewportHeight)
      visualViewport?.removeEventListener('scroll', syncViewportHeight)
    }
  }, [])

  return null
}

export default ViewportHeightSync
