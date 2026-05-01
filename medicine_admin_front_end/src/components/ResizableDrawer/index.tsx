import type { DrawerProps } from 'antd';
import { Drawer } from 'antd';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import ResizeHandle from '@/components/ResizeHandle';
import styles from './index.module.less';

/** 默认抽屉宽度。 */
const DEFAULT_DRAWER_WIDTH = 1280;

/** 默认抽屉最小宽度。 */
const DEFAULT_MIN_DRAWER_WIDTH = 960;

/** 默认抽屉右侧保留视口宽度。 */
const DEFAULT_VIEWPORT_REMAIN_WIDTH = 80;

/** 小屏断点宽度。 */
const MOBILE_BREAKPOINT_WIDTH = 768;

export interface ResizableDrawerProps extends Omit<
  DrawerProps,
  'width' | 'placement' | 'children'
> {
  /** 默认宽度。 */
  defaultWidth?: number;
  /** 最小宽度。 */
  minWidth?: number;
  /** 最大宽度；不传时使用视口宽度减去默认保留宽度。 */
  maxWidth?: number;
  /** 本地存储宽度的 key。 */
  storageKey?: string;
  /** 抽屉弹出方向，第一版只支持右侧抽屉。 */
  placement?: 'right';
  /** 抽屉内容。 */
  children?: React.ReactNode;
}

/**
 * 判断当前环境是否可以访问浏览器窗口。
 *
 * @returns 可以访问 window 时返回 true。
 */
function canUseWindow() {
  return typeof window !== 'undefined';
}

/**
 * 计算抽屉最大宽度。
 *
 * @param maxWidth 外部传入最大宽度。
 * @returns 抽屉最大宽度。
 */
function resolveMaxWidth(maxWidth?: number) {
  if (!canUseWindow()) {
    return maxWidth || DEFAULT_DRAWER_WIDTH;
  }
  if (window.innerWidth <= MOBILE_BREAKPOINT_WIDTH) {
    return window.innerWidth;
  }
  return maxWidth || Math.max(window.innerWidth - DEFAULT_VIEWPORT_REMAIN_WIDTH, 1);
}

/**
 * 将宽度限制在允许范围内。
 *
 * @param width 原始宽度。
 * @param minWidth 最小宽度。
 * @param maxWidth 最大宽度。
 * @returns 处理后的宽度。
 */
function clampDrawerWidth(width: number, minWidth: number, maxWidth: number) {
  return Math.min(Math.max(width, minWidth), maxWidth);
}

/**
 * 从本地存储读取抽屉宽度。
 *
 * @param storageKey 本地存储 key。
 * @returns 宽度；不存在或非法时返回 null。
 */
function readStoredWidth(storageKey?: string) {
  if (!storageKey || !canUseWindow()) {
    return null;
  }
  const rawValue = window.localStorage.getItem(storageKey);
  if (!rawValue) {
    return null;
  }
  const parsedValue = Number(rawValue);
  return Number.isFinite(parsedValue) && parsedValue > 0 ? parsedValue : null;
}

/**
 * 写入抽屉宽度到本地存储。
 *
 * @param storageKey 本地存储 key。
 * @param width 抽屉宽度。
 * @returns 无返回值。
 */
function writeStoredWidth(storageKey: string | undefined, width: number) {
  if (!storageKey || !canUseWindow()) {
    return;
  }
  window.localStorage.setItem(storageKey, String(Math.round(width)));
}

/**
 * 合并 CSS 类名。
 *
 * @param classNames CSS 类名列表。
 * @returns 合并后的 CSS 类名。
 */
function joinClassNames(...classNames: Array<string | undefined>) {
  return classNames.filter(Boolean).join(' ');
}

/**
 * 可拖拽宽度抽屉组件。
 *
 * @param props 抽屉属性。
 * @returns 可拖拽宽度抽屉节点。
 */
const ResizableDrawer: React.FC<ResizableDrawerProps> = ({
  defaultWidth = DEFAULT_DRAWER_WIDTH,
  minWidth = DEFAULT_MIN_DRAWER_WIDTH,
  maxWidth,
  storageKey,
  placement = 'right',
  open,
  children,
  rootClassName,
  className,
  ...drawerProps
}) => {
  /** 拖拽起点信息。 */
  const dragStartRef = useRef<{ startX: number; startWidth: number } | null>(null);
  /** 拖拽边缘元素。 */
  const resizeHandleRef = useRef<HTMLDivElement | null>(null);
  /** 当前抽屉宽度。 */
  const drawerWidthRef = useRef<number>(0);
  /** 下一帧要应用的抽屉宽度。 */
  const pendingWidthRef = useRef<number | null>(null);
  /** requestAnimationFrame 任务 ID。 */
  const animationFrameRef = useRef<number | null>(null);
  /** 拖拽前 body 光标。 */
  const bodyCursorRef = useRef<string>('');
  /** 拖拽前 body 选中样式。 */
  const bodyUserSelectRef = useRef<string>('');
  /** 是否处于拖拽中。 */
  const [resizing, setResizing] = useState(false);
  /** 当前抽屉宽度。 */
  const [drawerWidth, setDrawerWidth] = useState(() => {
    const resolvedMaxWidth = resolveMaxWidth(maxWidth);
    const storedWidth = readStoredWidth(storageKey);
    return clampDrawerWidth(storedWidth || defaultWidth, minWidth, resolvedMaxWidth);
  });

  /**
   * 同步最新抽屉宽度给拖拽事件使用。
   *
   * @returns 无返回值。
   */
  useEffect(() => {
    drawerWidthRef.current = drawerWidth;
  }, [drawerWidth]);

  /**
   * 读取 Ant Design Drawer 的内容包裹元素。
   *
   * @returns 内容包裹元素；未挂载时返回 null。
   */
  const resolveContentWrapper = useCallback(() => {
    return resizeHandleRef.current?.closest('.ant-drawer-content-wrapper') as HTMLElement | null;
  }, []);

  /**
   * 直接应用抽屉宽度到 DOM，避免拖动时触发 React 子树重渲染。
   *
   * @param width 抽屉宽度。
   * @returns 无返回值。
   */
  const applyDrawerWidth = useCallback(
    (width: number) => {
      const nextWidth = clampDrawerWidth(width, minWidth, resolveMaxWidth(maxWidth));
      drawerWidthRef.current = nextWidth;
      const contentWrapper = resolveContentWrapper();
      if (contentWrapper) {
        contentWrapper.style.width = `${Math.round(nextWidth)}px`;
      }
    },
    [maxWidth, minWidth, resolveContentWrapper],
  );

  /**
   * 按浏览器帧率批量应用抽屉宽度。
   *
   * @param width 抽屉宽度。
   * @returns 无返回值。
   */
  const scheduleDrawerWidth = useCallback(
    (width: number) => {
      pendingWidthRef.current = width;
      if (animationFrameRef.current !== null || !canUseWindow()) {
        return;
      }
      animationFrameRef.current = window.requestAnimationFrame(() => {
        animationFrameRef.current = null;
        if (pendingWidthRef.current === null) {
          return;
        }
        applyDrawerWidth(pendingWidthRef.current);
      });
    },
    [applyDrawerWidth],
  );

  /**
   * 取消待执行的宽度更新帧。
   *
   * @returns 无返回值。
   */
  const cancelScheduledWidth = useCallback(() => {
    if (animationFrameRef.current !== null && canUseWindow()) {
      window.cancelAnimationFrame(animationFrameRef.current);
    }
    animationFrameRef.current = null;
  }, []);

  /**
   * 记录拖拽时的 body 交互状态。
   *
   * @returns 无返回值。
   */
  const lockBodyInteraction = useCallback(() => {
    if (!canUseWindow()) {
      return;
    }
    bodyCursorRef.current = document.body.style.cursor;
    bodyUserSelectRef.current = document.body.style.userSelect;
    document.body.style.cursor = 'col-resize';
    document.body.style.userSelect = 'none';
  }, []);

  /**
   * 恢复拖拽前的 body 交互状态。
   *
   * @returns 无返回值。
   */
  const unlockBodyInteraction = useCallback(() => {
    if (!canUseWindow()) {
      return;
    }
    document.body.style.cursor = bodyCursorRef.current;
    document.body.style.userSelect = bodyUserSelectRef.current;
  }, []);

  /**
   * 结束拖拽。
   *
   * @returns 无返回值。
   */
  const stopResize = useCallback(() => {
    const finalWidth = pendingWidthRef.current ?? drawerWidthRef.current;
    cancelScheduledWidth();
    applyDrawerWidth(finalWidth);
    setDrawerWidth(drawerWidthRef.current);
    writeStoredWidth(storageKey, drawerWidthRef.current);
    pendingWidthRef.current = null;
    dragStartRef.current = null;
    setResizing(false);
    unlockBodyInteraction();
  }, [applyDrawerWidth, cancelScheduledWidth, storageKey, unlockBodyInteraction]);

  /**
   * 拖拽过程中更新宽度。
   *
   * @param event 指针移动事件。
   * @returns 无返回值。
   */
  const handlePointerMove = useCallback(
    (event: PointerEvent) => {
      const dragStart = dragStartRef.current;
      if (!dragStart) {
        return;
      }
      const nextWidth = clampDrawerWidth(
        dragStart.startWidth + dragStart.startX - event.clientX,
        minWidth,
        resolveMaxWidth(maxWidth),
      );
      scheduleDrawerWidth(nextWidth);
    },
    [maxWidth, minWidth, scheduleDrawerWidth],
  );

  /**
   * 开始拖拽。
   *
   * @param event 指针按下事件。
   * @returns 无返回值。
   */
  const handleResizeStart = useCallback(
    (event: React.PointerEvent<HTMLDivElement>) => {
      if (event.pointerType === 'mouse' && event.button !== 0) {
        return;
      }
      event.preventDefault();
      dragStartRef.current = {
        startX: event.clientX,
        startWidth: drawerWidthRef.current,
      };
      setResizing(true);
      lockBodyInteraction();
    },
    [lockBodyInteraction],
  );

  /**
   * 恢复默认宽度。
   *
   * @returns 无返回值。
   */
  const handleResetWidth = useCallback(() => {
    const nextWidth = clampDrawerWidth(defaultWidth, minWidth, resolveMaxWidth(maxWidth));
    applyDrawerWidth(nextWidth);
    setDrawerWidth(nextWidth);
    writeStoredWidth(storageKey, nextWidth);
  }, [applyDrawerWidth, defaultWidth, maxWidth, minWidth, storageKey]);

  /**
   * 绑定拖拽全局事件。
   *
   * @returns 清理函数。
   */
  useEffect(() => {
    if (!resizing || !canUseWindow()) {
      return undefined;
    }
    window.addEventListener('pointermove', handlePointerMove);
    window.addEventListener('pointerup', stopResize);
    window.addEventListener('pointercancel', stopResize);
    return () => {
      window.removeEventListener('pointermove', handlePointerMove);
      window.removeEventListener('pointerup', stopResize);
      window.removeEventListener('pointercancel', stopResize);
    };
  }, [handlePointerMove, resizing, stopResize]);

  /**
   * 视口变化时重新限制宽度范围。
   *
   * @returns 清理函数。
   */
  useEffect(() => {
    if (!canUseWindow()) {
      return undefined;
    }
    const handleWindowResize = () => {
      setDrawerWidth((currentWidth) => {
        const nextWidth = clampDrawerWidth(currentWidth, minWidth, resolveMaxWidth(maxWidth));
        applyDrawerWidth(nextWidth);
        writeStoredWidth(storageKey, nextWidth);
        return nextWidth;
      });
    };
    window.addEventListener('resize', handleWindowResize);
    return () => window.removeEventListener('resize', handleWindowResize);
  }, [applyDrawerWidth, maxWidth, minWidth, storageKey]);

  /**
   * 打开时同步本地存储宽度。
   *
   * @returns 无返回值。
   */
  useEffect(() => {
    if (!open) {
      return;
    }
    const storedWidth = readStoredWidth(storageKey);
    if (storedWidth) {
      const nextWidth = clampDrawerWidth(storedWidth, minWidth, resolveMaxWidth(maxWidth));
      applyDrawerWidth(nextWidth);
      setDrawerWidth(nextWidth);
    }
  }, [applyDrawerWidth, maxWidth, minWidth, open, storageKey]);

  /**
   * 组件卸载时恢复 body 交互状态。
   *
   * @returns 清理函数。
   */
  useEffect(
    () => () => {
      cancelScheduledWidth();
      unlockBodyInteraction();
    },
    [cancelScheduledWidth, unlockBodyInteraction],
  );

  return (
    <Drawer
      {...drawerProps}
      open={open}
      width={drawerWidth}
      placement={placement}
      rootClassName={joinClassNames(
        styles.resizableDrawerRoot,
        resizing ? styles.resizingRoot : undefined,
        rootClassName,
      )}
      className={joinClassNames(styles.resizableDrawer, className)}
    >
      <ResizeHandle
        ref={resizeHandleRef}
        ariaLabel="调整抽屉宽度"
        active={resizing}
        className={styles.resizeHandle}
        linePlacement="edge"
        onReset={handleResetWidth}
        onResizeStart={handleResizeStart}
        title="拖拽调整宽度，双击恢复默认宽度"
      />
      {children}
    </Drawer>
  );
};

export default ResizableDrawer;
