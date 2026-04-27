import { ReloadOutlined } from '@ant-design/icons';
import { Spin } from 'antd';
import React, { useCallback, useEffect, useImperativeHandle, useRef, useState } from 'react';
import styles from './index.module.less';

export interface PullToRefreshProps {
  /** 刷新回调函数 */
  onRefresh: () => Promise<void>;
  /** 子元素 */
  children: React.ReactNode;
  /** 下拉触发刷新的阈值，默认 60px */
  threshold?: number;
  /** 是否禁用下拉刷新 */
  disabled?: boolean;
  /** 自定义样式 */
  style?: React.CSSProperties;
  /** 自定义类名 */
  className?: string;
  /** 下拉提示文字 */
  pullDownText?: string;
  /** 释放提示文字 */
  releaseText?: string;
  /** 刷新中提示文字 */
  refreshingText?: string;
}

export interface PullToRefreshRef {
  /** 手动触发刷新 */
  refresh: () => Promise<void>;
}

type RefreshState = 'idle' | 'pulling' | 'ready' | 'refreshing';

const PullToRefresh = React.forwardRef<PullToRefreshRef, PullToRefreshProps>(
  (
    {
      onRefresh,
      children,
      threshold = 60,
      disabled = false,
      style,
      className,
      pullDownText = '下拉刷新',
      releaseText = '释放刷新',
      refreshingText = '刷新中...',
    },
    ref,
  ) => {
    const containerRef = useRef<HTMLDivElement>(null);
    const contentRef = useRef<HTMLDivElement>(null);
    const [refreshState, setRefreshState] = useState<RefreshState>('idle');
    const [pullDistance, setPullDistance] = useState(0);
    const startY = useRef(0);
    const isDragging = useRef(false);

    // 暴露方法给父组件
    useImperativeHandle(ref, () => ({
      refresh: doRefresh,
    }));

    const doRefresh = useCallback(async () => {
      if (refreshState === 'refreshing') return;

      setRefreshState('refreshing');
      setPullDistance(threshold);

      try {
        await onRefresh();
      } finally {
        setRefreshState('idle');
        setPullDistance(0);
      }
    }, [onRefresh, refreshState, threshold]);

    const handleTouchStart = useCallback(
      (e: React.TouchEvent | TouchEvent) => {
        if (disabled || refreshState === 'refreshing') return;

        const container = containerRef.current;
        if (!container) return;

        // 只有在滚动到顶部时才能下拉刷新
        if (container.scrollTop <= 0) {
          startY.current = e.touches[0].clientY;
          isDragging.current = true;
        }
      },
      [disabled, refreshState],
    );

    const handleTouchMove = useCallback(
      (e: React.TouchEvent | TouchEvent) => {
        if (!isDragging.current || disabled || refreshState === 'refreshing') return;

        const container = containerRef.current;
        if (!container) return;

        // 检查是否在顶部
        if (container.scrollTop > 0) {
          isDragging.current = false;
          setPullDistance(0);
          setRefreshState('idle');
          return;
        }

        const currentY = e.touches[0].clientY;
        const diff = currentY - startY.current;

        // 只处理下拉
        if (diff > 0) {
          e.preventDefault();
          // 添加阻尼效果
          const distance = Math.min(diff * 0.5, threshold * 1.5);
          setPullDistance(distance);

          if (distance >= threshold) {
            setRefreshState('ready');
          } else {
            setRefreshState('pulling');
          }
        }
      },
      [disabled, refreshState, threshold],
    );

    const handleTouchEnd = useCallback(() => {
      if (!isDragging.current) return;

      isDragging.current = false;

      if (refreshState === 'ready') {
        doRefresh();
      } else {
        setRefreshState('idle');
        setPullDistance(0);
      }
    }, [refreshState, doRefresh]);

    // 处理鼠标事件（PC端）
    const handleMouseDown = useCallback(
      (e: React.MouseEvent) => {
        if (disabled || refreshState === 'refreshing') return;

        const container = containerRef.current;
        if (!container) return;

        if (container.scrollTop <= 0) {
          startY.current = e.clientY;
          isDragging.current = true;
        }
      },
      [disabled, refreshState],
    );

    const handleMouseMove = useCallback(
      (e: React.MouseEvent) => {
        if (!isDragging.current || disabled || refreshState === 'refreshing') return;

        const container = containerRef.current;
        if (!container) return;

        if (container.scrollTop > 0) {
          isDragging.current = false;
          setPullDistance(0);
          setRefreshState('idle');
          return;
        }

        const diff = e.clientY - startY.current;

        if (diff > 0) {
          e.preventDefault();
          const distance = Math.min(diff * 0.5, threshold * 1.5);
          setPullDistance(distance);

          if (distance >= threshold) {
            setRefreshState('ready');
          } else {
            setRefreshState('pulling');
          }
        }
      },
      [disabled, refreshState, threshold],
    );

    const handleMouseUp = useCallback(() => {
      if (!isDragging.current) return;

      isDragging.current = false;

      if (refreshState === 'ready') {
        doRefresh();
      } else {
        setRefreshState('idle');
        setPullDistance(0);
      }
    }, [refreshState, doRefresh]);

    const handleMouseLeave = useCallback(() => {
      if (isDragging.current && refreshState !== 'refreshing') {
        isDragging.current = false;
        setRefreshState('idle');
        setPullDistance(0);
      }
    }, [refreshState]);

    // 添加全局鼠标事件监听，防止鼠标移出组件后无法触发mouseup
    useEffect(() => {
      const handleGlobalMouseUp = () => {
        if (isDragging.current) {
          handleMouseUp();
        }
      };

      window.addEventListener('mouseup', handleGlobalMouseUp);
      return () => {
        window.removeEventListener('mouseup', handleGlobalMouseUp);
      };
    }, [handleMouseUp]);

    // 获取提示文字
    const getTipText = () => {
      switch (refreshState) {
        case 'pulling':
          return pullDownText;
        case 'ready':
          return releaseText;
        case 'refreshing':
          return refreshingText;
        default:
          return '';
      }
    };

    return (
      <div
        ref={containerRef}
        className={`${styles.pullToRefreshContainer} ${className || ''}`}
        style={style}
        onTouchStart={handleTouchStart}
        onTouchMove={handleTouchMove}
        onTouchEnd={handleTouchEnd}
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onMouseLeave={handleMouseLeave}
      >
        {/* 刷新指示器 */}
        <div
          className={styles.refreshIndicator}
          style={{
            height: pullDistance,
            opacity: Math.min(pullDistance / threshold, 1),
          }}
        >
          <div className={styles.refreshContent}>
            {refreshState === 'refreshing' ? (
              <Spin size="small" />
            ) : (
              <ReloadOutlined
                className={`${styles.refreshIcon} ${refreshState === 'ready' ? styles.ready : ''}`}
              />
            )}
            <span className={styles.refreshText}>{getTipText()}</span>
          </div>
        </div>

        {/* 内容区域 */}
        <div
          ref={contentRef}
          className={styles.content}
          style={{
            transform: `translateY(${pullDistance}px)`,
            transition:
              refreshState === 'idle' || refreshState === 'refreshing'
                ? 'transform 0.3s ease'
                : 'none',
          }}
        >
          {children}
        </div>
      </div>
    );
  },
);

PullToRefresh.displayName = 'PullToRefresh';

export default PullToRefresh;
