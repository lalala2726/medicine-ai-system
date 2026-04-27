import React, { useEffect, useRef, useState } from 'react';
import { useThemeContext } from '@/contexts/ThemeContext';

interface ChartContainerProps {
  /** 图表高度 */
  height?: number;
  /** 额外 className */
  className?: string;
  children: React.ReactNode;
}

/**
 * 图表容器 — 解决从历史记录加载时图表宽度塌陷的问题。
 *
 * 原因：@antv/g2 在首次渲染时测量容器像素宽度来设置 canvas 尺寸，
 * 但从会话列表加载历史消息时，消息一次性渲染，容器尚未完成布局计算，
 * 实际像素宽度为 0 或极小值，导致图表被压缩。
 *
 * 方案：使用 ResizeObserver 监听容器宽度，当宽度发生变化时派发
 * window resize 事件，触发 @antv/g2 重新计算尺寸。同时延迟首次
 * 渲染图表子组件，等待容器获得有效宽度后再挂载。
 */
const ChartContainer: React.FC<ChartContainerProps> = ({ height = 500, className, children }) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const [ready, setReady] = useState(false);
  const prevWidthRef = useRef(0);
  const { isDark } = useThemeContext();

  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;

    const ro = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const width = entry.contentRect.width;
        // 容器首次获得有效宽度时标记 ready
        if (width > 0 && !ready) {
          setReady(true);
        }
        // 宽度变化超过 1px 时触发 resize，让 g2 重新计算
        if (Math.abs(width - prevWidthRef.current) > 1) {
          prevWidthRef.current = width;
          window.dispatchEvent(new Event('resize'));
        }
      }
    });

    ro.observe(el);
    return () => ro.disconnect();
  }, [ready]);

  return (
    <div
      ref={containerRef}
      className={className}
      style={{
        width: '100%',
        minHeight: height,
        display: 'block',
        backgroundColor: 'transparent',
        transition: 'background-color 0.3s ease',
      }}
    >
      {ready ? children : null}
    </div>
  );
};

export default ChartContainer;
