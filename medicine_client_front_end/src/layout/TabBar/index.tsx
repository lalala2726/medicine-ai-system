import { memo, useEffect, useRef, useCallback, useMemo, useState } from 'react'
import type { FC } from 'react'
import { Tabbar } from '@nutui/nutui-react'
import { Home, Service, User, Category } from '@nutui/icons-react'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import SideDrawer from '@/components/SideDrawer'
import ConversationList from '@/pages/Assistant/components/ConversationList'
import { useAssistantDrawerStore } from '@/stores/assistantDrawerStore'
import { useAuthStore } from '@/store/auth'
import { useSpring, animated } from '@react-spring/web'
import './index.css'

/** 软键盘触发底部导航隐藏的最小视口压缩像素。 */
const TABBAR_KEYBOARD_HIDE_MIN_PX = 160

/** 软键盘触发底部导航隐藏的最小视口压缩比例。 */
const TABBAR_KEYBOARD_HIDE_MIN_RATIO = 0.18

/** 软键盘开始进入隐藏过渡态的最小视口压缩像素。 */
const TABBAR_KEYBOARD_PREPARE_MIN_PX = 48

/** 视口宽度变化达到该阈值时，视为发生横竖屏或布局模式切换。 */
const TABBAR_VIEWPORT_WIDTH_CHANGE_THRESHOLD = 96

/** 视口恢复时允许的基准高度回正误差。 */
const TABBAR_VIEWPORT_BASELINE_SETTLE_THRESHOLD = 24

/** 粗指针设备检测媒体查询。 */
const COARSE_POINTER_MEDIA_QUERY = '(pointer: coarse)'

/** 会话抽屉手势需要忽略的交互区域选择器。 */
const ASSISTANT_DRAWER_GESTURE_LOCK_SELECTOR = [
  '[data-assistant-drawer-swipe-lock="true"]',
  '[data-streamdown="mermaid-block"]',
  '[data-streamdown="mermaid"]',
  '[data-streamdown="table-wrapper"]',
  '[data-streamdown="code-block"]',
  '[data-streamdown="image-wrapper"]',
  'input',
  'textarea',
  'select'
].join(', ')

/** 不应触发文本键盘判定的 input 类型集合。 */
const NON_TEXT_INPUT_TYPES = new Set([
  'button',
  'checkbox',
  'color',
  'file',
  'hidden',
  'image',
  'radio',
  'range',
  'reset',
  'submit'
])

/** 受登录态保护的底部导航路径集合。 */
const TABBAR_PROTECTED_PATH_SET = new Set(['/assistant', '/me'])

/** 底部导航项结构。 */
interface TabItem {
  /** 导航路径。 */
  key: string
  /** 导航标题。 */
  title: string
  /** 导航图标。 */
  icon: React.ReactNode
}

/** 软键盘可见性判断参数。 */
interface SoftwareKeyboardVisibilityOptions {
  /** 当前稳定状态下记录的基准视口高度。 */
  baselineHeight: number
  /** 当前实时视口高度。 */
  viewportHeight: number
  /** 当前是否为粗指针设备。 */
  isCoarsePointerDevice: boolean
  /** 当前是否存在文本输入控件聚焦。 */
  isTextInputFocused: boolean
}

/**
 * 读取当前浏览器可用的可视视口尺寸。
 *
 * @returns 当前视口宽高，浏览器不支持 `visualViewport` 时返回 `null`
 */
const resolveVisualViewportMetrics = (): { width: number; height: number } | null => {
  if (typeof window === 'undefined' || !window.visualViewport) {
    return null
  }

  return {
    width: window.visualViewport.width,
    height: window.visualViewport.height
  }
}

/**
 * 计算当前可视视口相对稳定态视口缩小的高度。
 *
 * @param baselineHeight - 当前稳定态记录的基准视口高度
 * @param viewportHeight - 当前实时视口高度
 * @returns 当前视口缩小的高度值
 */
const resolveViewportHeightDelta = (baselineHeight: number, viewportHeight: number): number => {
  return baselineHeight - viewportHeight
}

/**
 * 判断当前是否应视为“软件键盘已真实弹起”。
 *
 * @param options - 键盘可见性判断参数
 * @returns 当前是否应隐藏底部导航
 */
const isSoftwareKeyboardVisible = ({
  baselineHeight,
  viewportHeight,
  isCoarsePointerDevice,
  isTextInputFocused
}: SoftwareKeyboardVisibilityOptions): boolean => {
  if (!isCoarsePointerDevice || !isTextInputFocused || baselineHeight <= 0 || viewportHeight <= 0) {
    return false
  }

  /** 当前视口相对基准视口缩小的高度。 */
  const heightDelta = resolveViewportHeightDelta(baselineHeight, viewportHeight)

  return heightDelta >= TABBAR_KEYBOARD_HIDE_MIN_PX || heightDelta / baselineHeight >= TABBAR_KEYBOARD_HIDE_MIN_RATIO
}

/**
 * 判断当前元素是否为会触发文本输入键盘的可编辑控件。
 *
 * @param element - 当前聚焦元素
 * @returns 当前元素是否属于文本输入控件
 */
const isTextEntryElement = (element: Element | null): boolean => {
  if (!(element instanceof HTMLElement)) {
    return false
  }

  if (element instanceof HTMLTextAreaElement) {
    return !element.disabled && !element.readOnly
  }

  if (element instanceof HTMLInputElement) {
    /** 当前 input 的类型。 */
    const inputType = (element.type || 'text').toLowerCase()

    return !element.disabled && !element.readOnly && !NON_TEXT_INPUT_TYPES.has(inputType)
  }

  return element.isContentEditable
}

/**
 * 判断当前触摸起点是否位于会话抽屉手势应忽略的交互区域内。
 *
 * @param target - 当前触摸事件命中的原始节点
 * @returns 当前触摸是否应跳过抽屉侧滑处理
 */
const isAssistantDrawerGestureLockedTarget = (target: EventTarget | null): boolean => {
  if (!(target instanceof Element)) {
    return false
  }

  return Boolean(target.closest(ASSISTANT_DRAWER_GESTURE_LOCK_SELECTOR))
}

const TabLayout: FC = () => {
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const isAuthenticated = useAuthStore(state => state.isAuthenticated())
  const isDrawerOpen = useAssistantDrawerStore(state => state.isOpen)
  const openDrawer = useAssistantDrawerStore(state => state.openDrawer)
  const closeDrawer = useAssistantDrawerStore(state => state.closeDrawer)
  const isAssistantRoute = isAuthenticated && pathname.startsWith('/assistant')
  /** 当前底部导航是否因软件键盘弹起而隐藏。 */
  const [isTabbarHiddenByKeyboard, setIsTabbarHiddenByKeyboard] = useState(false)
  /** 当前底部导航是否处于键盘弹起前的过渡态。 */
  const [isTabbarPreparingToHide, setIsTabbarPreparingToHide] = useState(false)

  // 获取抽屉宽度（同步 CSS 中的逻辑）
  const drawerWidth = useMemo(() => {
    return Math.min(window.innerWidth * 0.72, 300)
  }, [])

  // 使用 react-spring 控制位移，调整参数消除弹跳感
  const [{ x }, api] = useSpring(() => ({
    x: 0,
    config: {
      tension: 280,
      friction: 30,
      clamp: true, // 禁止过冲回弹
      precision: 0.1
    }
  }))

  const touchStartX = useRef<number>(0)
  const touchStartY = useRef<number>(0)
  const lastX = useRef<number>(0)
  const isDragging = useRef<boolean>(false)
  const dragDirection = useRef<'none' | 'horizontal' | 'vertical'>('none')
  /** 当前稳定态下的视口基准高度。 */
  const viewportBaselineHeightRef = useRef(0)
  /** 当前稳定态下的视口基准宽度。 */
  const viewportBaselineWidthRef = useRef(0)
  /** 当前是否存在文本输入控件聚焦。 */
  const isTextInputFocusedRef = useRef(false)

  // 同步外部状态到动画
  useEffect(() => {
    api.start({ x: isDrawerOpen ? drawerWidth : 0 })
  }, [isDrawerOpen, api, drawerWidth])

  /**
   * 重置当前路由下的键盘视口基准值。
   *
   * @returns 无返回值
   */
  const resetViewportBaseline = useCallback(() => {
    /** 当前浏览器支持的可视视口尺寸。 */
    const viewportMetrics = resolveVisualViewportMetrics()

    if (!viewportMetrics) {
      setIsTabbarPreparingToHide(false)
      setIsTabbarHiddenByKeyboard(false)
      return
    }

    viewportBaselineHeightRef.current = viewportMetrics.height
    viewportBaselineWidthRef.current = viewportMetrics.width
    setIsTabbarPreparingToHide(false)
    setIsTabbarHiddenByKeyboard(false)
  }, [])

  /**
   * 根据当前可视视口变化同步底部导航显隐状态。
   *
   * @returns 无返回值
   */
  const syncTabbarKeyboardVisibility = useCallback(() => {
    /** 当前浏览器支持的可视视口尺寸。 */
    const viewportMetrics = resolveVisualViewportMetrics()

    if (!viewportMetrics) {
      setIsTabbarPreparingToHide(false)
      setIsTabbarHiddenByKeyboard(false)
      return
    }

    /** 当前视口宽度。 */
    const viewportWidth = viewportMetrics.width
    /** 当前视口高度。 */
    const viewportHeight = viewportMetrics.height
    /** 当前设备是否属于粗指针触控环境。 */
    const isCoarsePointerDevice = typeof window !== 'undefined' && window.matchMedia(COARSE_POINTER_MEDIA_QUERY).matches

    if (viewportBaselineHeightRef.current <= 0 || viewportBaselineWidthRef.current <= 0) {
      viewportBaselineHeightRef.current = viewportHeight
      viewportBaselineWidthRef.current = viewportWidth
      setIsTabbarPreparingToHide(false)
      setIsTabbarHiddenByKeyboard(false)
      return
    }

    if (Math.abs(viewportWidth - viewportBaselineWidthRef.current) >= TABBAR_VIEWPORT_WIDTH_CHANGE_THRESHOLD) {
      viewportBaselineHeightRef.current = viewportHeight
      viewportBaselineWidthRef.current = viewportWidth
      setIsTabbarPreparingToHide(false)
      setIsTabbarHiddenByKeyboard(false)
      return
    }

    /** 当前是否应视为软件键盘已真实弹起。 */
    const shouldHideTabbar = isSoftwareKeyboardVisible({
      baselineHeight: viewportBaselineHeightRef.current,
      viewportHeight,
      isCoarsePointerDevice,
      isTextInputFocused: isTextInputFocusedRef.current
    })
    /** 当前视口相对稳定态视口缩小的高度。 */
    const viewportHeightDelta = resolveViewportHeightDelta(viewportBaselineHeightRef.current, viewportHeight)
    /** 当前底部导航是否应先进入轻量过渡态。 */
    const shouldPrepareTabbar =
      isCoarsePointerDevice &&
      isTextInputFocusedRef.current &&
      viewportHeightDelta >= TABBAR_KEYBOARD_PREPARE_MIN_PX &&
      !shouldHideTabbar

    setIsTabbarPreparingToHide(shouldPrepareTabbar)
    setIsTabbarHiddenByKeyboard(shouldHideTabbar)

    if (!shouldHideTabbar) {
      /** 当前视口是否已经恢复到接近稳定态。 */
      const hasViewportRecovered =
        viewportHeight >= viewportBaselineHeightRef.current - TABBAR_VIEWPORT_BASELINE_SETTLE_THRESHOLD

      if (hasViewportRecovered || viewportHeight > viewportBaselineHeightRef.current) {
        viewportBaselineHeightRef.current = viewportHeight
      }

      viewportBaselineWidthRef.current = viewportWidth
    }
  }, [])

  const handleTouchStart = useCallback(
    (e: TouchEvent) => {
      if (!isAssistantRoute) return
      /** 当前触摸点。 */
      const touch = e.touches[0]

      if (!touch) {
        return
      }

      /** 当前触摸起点是否位于抽屉手势豁免区域。 */
      const isGestureLockedTarget = isAssistantDrawerGestureLockedTarget(e.target)

      if (isGestureLockedTarget) {
        return
      }

      touchStartX.current = touch.clientX
      touchStartY.current = touch.clientY
      lastX.current = isDrawerOpen ? drawerWidth : 0
      isDragging.current = true
      dragDirection.current = 'none'
    },
    [isAssistantRoute, isDrawerOpen, drawerWidth]
  )

  const handleTouchMove = useCallback(
    (e: TouchEvent) => {
      if (!isDragging.current) return
      const touch = e.touches[0]
      const deltaX = touch.clientX - touchStartX.current
      const deltaY = touch.clientY - touchStartY.current

      // 首次移动时判定方向
      if (dragDirection.current === 'none') {
        if (Math.abs(deltaX) > Math.abs(deltaY)) {
          dragDirection.current = 'horizontal'
        } else if (Math.abs(deltaY) > 10) {
          dragDirection.current = 'vertical'
          isDragging.current = false
          return
        }
      }

      if (dragDirection.current === 'horizontal') {
        // 只有水平滑动时才阻止默认行为（防止页面滚动干扰）
        if (e.cancelable) e.preventDefault()

        let newX = lastX.current + deltaX
        // 限制滑动范围 [0, drawerWidth]
        newX = Math.max(0, Math.min(drawerWidth, newX))

        api.start({ x: newX, immediate: true })
      }
    },
    [api, drawerWidth]
  )

  const handleTouchEnd = useCallback(
    (e: TouchEvent) => {
      if (!isDragging.current) return
      isDragging.current = false

      if (dragDirection.current === 'horizontal') {
        const touch = e.changedTouches[0]
        const deltaX = touch.clientX - touchStartX.current

        // 判定阈值：滑动超过 1/4 宽度则切换
        const threshold = drawerWidth / 4
        if (isDrawerOpen) {
          if (deltaX < -threshold) {
            closeDrawer()
          } else {
            api.start({ x: drawerWidth, immediate: false })
          }
        } else {
          if (deltaX > threshold) {
            openDrawer()
          } else {
            api.start({ x: 0, immediate: false })
          }
        }
      }
      dragDirection.current = 'none'
    },
    [closeDrawer, openDrawer, drawerWidth, isDrawerOpen, api]
  )

  useEffect(() => {
    window.addEventListener('touchstart', handleTouchStart, { passive: true })
    window.addEventListener('touchmove', handleTouchMove, { passive: false })
    window.addEventListener('touchend', handleTouchEnd)

    return () => {
      window.removeEventListener('touchstart', handleTouchStart)
      window.removeEventListener('touchmove', handleTouchMove)
      window.removeEventListener('touchend', handleTouchEnd)
    }
  }, [handleTouchStart, handleTouchMove, handleTouchEnd])

  /** 当前底部导航配置。 */
  const tabs: TabItem[] = [
    { key: '/home', title: '首页', icon: <Home /> },
    { key: '/category', title: '分类', icon: <Category /> },
    { key: '/assistant', title: '咨询', icon: <Service /> },
    { key: '/me', title: '我的', icon: <User /> }
  ]

  /**
   * 处理底部导航切换。
   *
   * @param value 当前 Tabbar 返回的目标索引。
   * @returns 无返回值。
   */
  const handleTabSwitch = useCallback(
    (value: string | number): void => {
      /** 当前目标导航索引。 */
      const nextTabIndex = Number(value)
      /** 当前目标导航项。 */
      const nextTab = tabs[nextTabIndex]

      if (!nextTab) {
        return
      }

      if (TABBAR_PROTECTED_PATH_SET.has(nextTab.key) && !isAuthenticated) {
        navigate('/login', { replace: true })
        return
      }

      navigate(nextTab.key)
    },
    [isAuthenticated, navigate, tabs]
  )

  useEffect(() => {
    if (!isAssistantRoute && isDrawerOpen) {
      closeDrawer()
    }
  }, [closeDrawer, isAssistantRoute, isDrawerOpen])

  /**
   * 根据当前文档聚焦元素同步“文本输入控件是否聚焦”状态。
   *
   * @returns 无返回值
   */
  const syncTextInputFocusState = useCallback(() => {
    /** 当前文档上的激活元素。 */
    const activeElement = typeof document === 'undefined' ? null : document.activeElement
    /** 当前是否存在文本输入控件聚焦。 */
    const isTextInputFocused = isTextEntryElement(activeElement)

    isTextInputFocusedRef.current = isTextInputFocused

    if (!isTextInputFocused) {
      setIsTabbarPreparingToHide(false)
      setIsTabbarHiddenByKeyboard(false)
      return
    }

    syncTabbarKeyboardVisibility()
  }, [syncTabbarKeyboardVisibility])

  /**
   * 页面初次挂载后监听可视视口变化，仅在软件键盘真实挤压视口时隐藏底栏。
   */
  useEffect(() => {
    /** 当前浏览器的 VisualViewport 实例。 */
    const visualViewport = window.visualViewport

    if (!visualViewport) {
      setIsTabbarHiddenByKeyboard(false)
      return
    }

    resetViewportBaseline()
    visualViewport.addEventListener('resize', syncTabbarKeyboardVisibility)
    visualViewport.addEventListener('scroll', syncTabbarKeyboardVisibility)
    window.addEventListener('orientationchange', resetViewportBaseline)

    return () => {
      visualViewport.removeEventListener('resize', syncTabbarKeyboardVisibility)
      visualViewport.removeEventListener('scroll', syncTabbarKeyboardVisibility)
      window.removeEventListener('orientationchange', resetViewportBaseline)
    }
  }, [resetViewportBaseline, syncTabbarKeyboardVisibility])

  /**
   * 监听全局聚焦元素变化，仅当文本输入控件聚焦时才允许底栏进入隐藏态。
   */
  useEffect(() => {
    /** 延后一帧读取当前激活元素，确保 focusout 后的 activeElement 已经稳定。 */
    let focusFrameId = 0

    /**
     * 处理文档 focusin 事件。
     *
     * @returns 无返回值
     */
    const handleDocumentFocusIn = () => {
      window.cancelAnimationFrame(focusFrameId)
      syncTextInputFocusState()
    }

    /**
     * 处理文档 focusout 事件。
     *
     * @returns 无返回值
     */
    const handleDocumentFocusOut = () => {
      window.cancelAnimationFrame(focusFrameId)
      focusFrameId = window.requestAnimationFrame(() => {
        syncTextInputFocusState()
      })
    }

    document.addEventListener('focusin', handleDocumentFocusIn)
    document.addEventListener('focusout', handleDocumentFocusOut)
    syncTextInputFocusState()

    return () => {
      window.cancelAnimationFrame(focusFrameId)
      document.removeEventListener('focusin', handleDocumentFocusIn)
      document.removeEventListener('focusout', handleDocumentFocusOut)
    }
  }, [syncTextInputFocusState])

  /**
   * 路由切换时重新校准稳定态视口基准，避免页面切换后的误判。
   */
  useEffect(() => {
    if (isTabbarHiddenByKeyboard) {
      return
    }

    resetViewportBaseline()
  }, [isTabbarHiddenByKeyboard, pathname, resetViewportBaseline])

  const activeIndex = Math.max(
    0,
    tabs.findIndex(t => (pathname === '/' ? t.key === '/home' : pathname.startsWith(t.key)))
  )
  /** 当前底部导航最终使用的 className。 */
  const bottomClassName = [
    'bottom',
    isTabbarPreparingToHide ? 'bottomPreparingToHide' : '',
    isTabbarHiddenByKeyboard ? 'bottomHidden' : ''
  ]
    .filter(Boolean)
    .join(' ')

  return (
    <div className='app'>
      {isAssistantRoute && (
        <animated.div
          style={{
            position: 'absolute',
            inset: 0,
            zIndex: 1,
            // 当位移接近0时完全隐藏，并向左多偏移一些，彻底消除阴影泄露
            visibility: x.to(v => (v <= 0.5 ? 'hidden' : 'visible')),
            transform: x.to(v => `translate3d(${v - drawerWidth - (v <= 1 ? 50 : 0)}px, 0, 0)`)
          }}
        >
          <SideDrawer open={isDrawerOpen}>
            <ConversationList />
          </SideDrawer>
        </animated.div>
      )}
      <animated.div
        className='shell'
        style={{
          transform: x.to(v => `translate3d(${v}px, 0, 0)`)
        }}
      >
        <div className='body'>
          <Outlet />
        </div>
        <div className={bottomClassName}>
          <Tabbar value={activeIndex} activeColor='rgb(21, 190, 81)' onSwitch={handleTabSwitch}>
            {tabs.map(item => (
              <Tabbar.Item key={item.key} title={item.title} icon={item.icon} />
            ))}
          </Tabbar>
        </div>
        {isAssistantRoute && (
          <animated.button
            type='button'
            className='shellMask'
            onClick={closeDrawer}
            style={{
              opacity: x.to([0, drawerWidth], [0, 1]),
              // 实时计算模糊程度：从 0px 渐变到 2px
              backdropFilter: x.to(v => `blur(${(v / drawerWidth) * 2}px)`),
              WebkitBackdropFilter: x.to(v => `blur(${(v / drawerWidth) * 2}px)`),
              // 背景色透明度也实时渐变
              backgroundColor: x.to(v => `rgba(255, 255, 255, ${(v / drawerWidth) * 0.05})`),
              // 当位移为0时，禁用交互和渲染，防止点击不到下方内容
              pointerEvents: x.to(v => (v > 0.5 ? 'auto' : 'none')),
              visibility: x.to(v => (v > 0.1 ? 'visible' : 'hidden'))
            }}
            aria-label='关闭侧边抽屉遮罩'
          />
        )}
      </animated.div>
    </div>
  )
}

export default memo(TabLayout)
