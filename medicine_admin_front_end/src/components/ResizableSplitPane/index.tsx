import React, { useCallback, useEffect, useRef, useState } from 'react';
import ResizeHandle from '@/components/ResizeHandle';
import styles from './index.module.less';

/** 默认左侧面板宽度。 */
const DEFAULT_START_PANE_WIDTH = 470;

/** 默认左侧面板最小宽度。 */
const DEFAULT_MIN_START_PANE_WIDTH = 360;

/** 默认右侧面板最小宽度。 */
const DEFAULT_MIN_END_PANE_WIDTH = 420;

export interface ResizableSplitPaneProps {
  /** 外层样式类名。 */
  className?: string;
  /** 左侧面板样式类名。 */
  startClassName?: string;
  /** 右侧面板样式类名。 */
  endClassName?: string;
  /** 默认左侧面板宽度。 */
  defaultStartWidth?: number;
  /** 左侧面板最小宽度。 */
  minStartWidth?: number;
  /** 左侧面板最大宽度；不传时根据容器宽度和右侧最小宽度计算。 */
  maxStartWidth?: number;
  /** 右侧面板最小宽度。 */
  minEndWidth?: number;
  /** 本地存储宽度的 key。 */
  storageKey?: string;
  /** 左侧内容。 */
  startPane: React.ReactNode;
  /** 右侧内容。 */
  endPane: React.ReactNode;
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
 * 合并 CSS 类名。
 *
 * @param classNames CSS 类名列表。
 * @returns 合并后的 CSS 类名。
 */
function joinClassNames(...classNames: Array<string | undefined>) {
  return classNames.filter(Boolean).join(' ');
}

/**
 * 从本地存储读取宽度。
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
 * 写入宽度到本地存储。
 *
 * @param storageKey 本地存储 key。
 * @param width 面板宽度。
 * @returns 无返回值。
 */
function writeStoredWidth(storageKey: string | undefined, width: number) {
  if (!storageKey || !canUseWindow()) {
    return;
  }
  window.localStorage.setItem(storageKey, String(Math.round(width)));
}

/**
 * 将宽度限制在允许范围内。
 *
 * @param width 原始宽度。
 * @param minWidth 最小宽度。
 * @param maxWidth 最大宽度。
 * @returns 处理后的宽度。
 */
function clampPaneWidth(width: number, minWidth: number, maxWidth: number) {
  return Math.min(Math.max(width, minWidth), maxWidth);
}

/**
 * 可拖拽左右分栏组件。
 *
 * @param props 分栏属性。
 * @returns 可拖拽左右分栏节点。
 */
const ResizableSplitPane: React.FC<ResizableSplitPaneProps> = ({
  className,
  startClassName,
  endClassName,
  defaultStartWidth = DEFAULT_START_PANE_WIDTH,
  minStartWidth = DEFAULT_MIN_START_PANE_WIDTH,
  maxStartWidth,
  minEndWidth = DEFAULT_MIN_END_PANE_WIDTH,
  storageKey,
  startPane,
  endPane,
}) => {
  /** 分栏根节点。 */
  const containerRef = useRef<HTMLDivElement | null>(null);
  /** 拖拽起点信息。 */
  const dragStartRef = useRef<{ startX: number; startWidth: number } | null>(null);
  /** 当前左侧面板宽度。 */
  const startWidthRef = useRef<number>(0);
  /** 下一帧要应用的左侧面板宽度。 */
  const pendingWidthRef = useRef<number | null>(null);
  /** requestAnimationFrame 任务 ID。 */
  const animationFrameRef = useRef<number | null>(null);
  /** 拖拽前 body 光标。 */
  const bodyCursorRef = useRef<string>('');
  /** 拖拽前 body 选中样式。 */
  const bodyUserSelectRef = useRef<string>('');
  /** 是否处于拖拽中。 */
  const [resizing, setResizing] = useState(false);
  /** 当前左侧面板宽度。 */
  const [startWidth, setStartWidth] = useState(
    () => readStoredWidth(storageKey) || defaultStartWidth,
  );

  /**
   * 同步最新左侧面板宽度给拖拽事件使用。
   *
   * @returns 无返回值。
   */
  useEffect(() => {
    startWidthRef.current = startWidth;
  }, [startWidth]);

  /**
   * 计算左侧面板最大宽度。
   *
   * @returns 左侧面板最大宽度。
   */
  const resolveMaxStartWidth = useCallback(() => {
    const containerWidth = containerRef.current?.clientWidth || 0;
    const containerMaxWidth = containerWidth
      ? Math.max(containerWidth - minEndWidth, minStartWidth)
      : null;
    if (typeof maxStartWidth === 'number' && Number.isFinite(maxStartWidth) && maxStartWidth > 0) {
      const configuredMaxWidth = Math.max(maxStartWidth, minStartWidth);
      return containerMaxWidth === null
        ? configuredMaxWidth
        : Math.min(configuredMaxWidth, containerMaxWidth);
    }
    return containerMaxWidth ?? Math.max(defaultStartWidth, minStartWidth);
  }, [defaultStartWidth, maxStartWidth, minEndWidth, minStartWidth]);

  /**
   * 直接应用左侧面板宽度到 DOM。
   *
   * @param width 左侧面板宽度。
   * @returns 无返回值。
   */
  const applyStartWidth = useCallback(
    (width: number) => {
      const nextWidth = clampPaneWidth(width, minStartWidth, resolveMaxStartWidth());
      startWidthRef.current = nextWidth;
      containerRef.current?.style.setProperty(
        '--resizable-start-width',
        `${Math.round(nextWidth)}px`,
      );
    },
    [minStartWidth, resolveMaxStartWidth],
  );

  /**
   * 按浏览器帧率批量应用左侧面板宽度。
   *
   * @param width 左侧面板宽度。
   * @returns 无返回值。
   */
  const scheduleStartWidth = useCallback(
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
        applyStartWidth(pendingWidthRef.current);
      });
    },
    [applyStartWidth],
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
    const finalWidth = pendingWidthRef.current ?? startWidthRef.current;
    cancelScheduledWidth();
    applyStartWidth(finalWidth);
    setStartWidth(startWidthRef.current);
    writeStoredWidth(storageKey, startWidthRef.current);
    pendingWidthRef.current = null;
    dragStartRef.current = null;
    setResizing(false);
    unlockBodyInteraction();
  }, [applyStartWidth, cancelScheduledWidth, storageKey, unlockBodyInteraction]);

  /**
   * 拖拽过程中更新左侧面板宽度。
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
      scheduleStartWidth(dragStart.startWidth + event.clientX - dragStart.startX);
    },
    [scheduleStartWidth],
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
        startWidth: startWidthRef.current,
      };
      setResizing(true);
      lockBodyInteraction();
    },
    [lockBodyInteraction],
  );

  /**
   * 恢复默认左侧面板宽度。
   *
   * @returns 无返回值。
   */
  const handleResetWidth = useCallback(() => {
    applyStartWidth(defaultStartWidth);
    setStartWidth(startWidthRef.current);
    writeStoredWidth(storageKey, startWidthRef.current);
  }, [applyStartWidth, defaultStartWidth, storageKey]);

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
   * 视口变化时重新限制面板宽度。
   *
   * @returns 清理函数。
   */
  useEffect(() => {
    if (!canUseWindow()) {
      return undefined;
    }
    const handleWindowResize = () => {
      setStartWidth((currentWidth) => {
        applyStartWidth(currentWidth);
        writeStoredWidth(storageKey, startWidthRef.current);
        return startWidthRef.current;
      });
    };
    window.addEventListener('resize', handleWindowResize);
    return () => window.removeEventListener('resize', handleWindowResize);
  }, [applyStartWidth, storageKey]);

  /**
   * 首次挂载时应用初始宽度。
   *
   * @returns 无返回值。
   */
  useEffect(() => {
    applyStartWidth(startWidth);
  }, [applyStartWidth, startWidth]);

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
    <div
      ref={containerRef}
      className={joinClassNames(
        styles.resizableSplitPane,
        resizing ? styles.resizing : undefined,
        className,
      )}
      style={{ '--resizable-start-width': `${Math.round(startWidth)}px` } as React.CSSProperties}
    >
      <div className={joinClassNames(styles.startPane, startClassName)}>{startPane}</div>
      <ResizeHandle
        ariaLabel="调整面板宽度"
        active={resizing}
        className={styles.splitHandle}
        linePlacement="center"
        onReset={handleResetWidth}
        onResizeStart={handleResizeStart}
        title="拖拽调整面板宽度，双击恢复默认宽度"
      />
      <div className={joinClassNames(styles.endPane, endClassName)}>{endPane}</div>
    </div>
  );
};

export default ResizableSplitPane;
