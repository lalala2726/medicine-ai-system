import { Typography } from 'antd';
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import styles from '../index.module.less';

const { Text } = Typography;

/**
 * 锚点详情底部最小留白高度。
 */
const ANCHOR_MIN_BOTTOM_SPACER = 16;

/** 锚点详情区块定义。 */
export interface TraceAnchorSection {
  /** 区块唯一键。 */
  key: string;
  /** 区块导航标题。 */
  label: string;
  /** 是否展示区块。 */
  visible?: boolean;
  /** 渲染区块内容。 */
  render: () => React.ReactNode;
}

export interface TraceAnchorDetailLayoutProps {
  /** 顶部概览内容。 */
  header: React.ReactNode;
  /** 锚点区块列表。 */
  sections: TraceAnchorSection[];
  /** 内容重置键，切换不同 Span 或 Trace 时用于重置滚动位置。 */
  resetKey?: string | number | null;
}

/**
 * 过滤可见区块。
 *
 * @param sections 原始区块列表。
 * @returns 可见区块列表。
 */
function resolveVisibleSections(sections: TraceAnchorSection[]) {
  return sections.filter((section) => section.visible !== false);
}

/**
 * 计算区块在内部滚动容器中的原始位置。
 *
 * @param scrollElement 内部滚动容器。
 * @param sectionElement 目标区块元素。
 * @returns 区块相对内部滚动内容顶部的位置。
 */
function resolveSectionRawTop(scrollElement: HTMLDivElement, sectionElement: HTMLElement) {
  const scrollRect = scrollElement.getBoundingClientRect();
  const sectionRect = sectionElement.getBoundingClientRect();
  return sectionRect.top - scrollRect.top + scrollElement.scrollTop;
}

/**
 * 计算区块在内部滚动容器中的目标滚动位置。
 *
 * @param scrollElement 内部滚动容器。
 * @param sectionElement 目标区块元素。
 * @returns 目标 scrollTop。
 */
function resolveSectionScrollTop(scrollElement: HTMLDivElement, sectionElement: HTMLElement) {
  const rawTop = resolveSectionRawTop(scrollElement, sectionElement);
  const maxTop = Math.max(scrollElement.scrollHeight - scrollElement.clientHeight, 0);
  return Math.min(Math.max(rawTop - 2, 0), maxTop);
}

/**
 * 精确设置区块滚动位置。
 *
 * @param scrollElement 内部滚动容器。
 * @param sectionElement 目标区块元素。
 * @returns 无返回值。
 */
function applySectionScrollTop(scrollElement: HTMLDivElement, sectionElement: HTMLElement) {
  scrollElement.scrollTop = resolveSectionScrollTop(scrollElement, sectionElement);
}

/**
 * Trace 锚点式详情布局。
 *
 * @param props 组件属性。
 * @returns Trace 锚点式详情节点。
 */
const TraceAnchorDetailLayout: React.FC<TraceAnchorDetailLayoutProps> = ({
  header,
  sections,
  resetKey,
}) => {
  const visibleSections = useMemo(() => resolveVisibleSections(sections), [sections]);
  const visibleSectionKeyText = useMemo(
    () => visibleSections.map((section) => section.key).join('|'),
    [visibleSections],
  );
  const firstVisibleSectionKey = visibleSections[0]?.key;
  const [activeSectionKey, setActiveSectionKey] = useState(firstVisibleSectionKey);
  const [bottomSpacerHeight, setBottomSpacerHeight] = useState(ANCHOR_MIN_BOTTOM_SPACER);
  const scrollRef = useRef<HTMLDivElement | null>(null);
  const spacerRef = useRef<HTMLDivElement | null>(null);
  const sectionRefs = useRef<Record<string, HTMLElement | null>>({});
  const clickLockTimerRef = useRef<number | null>(null);
  const clickLockedSectionKeyRef = useRef<string | null>(null);
  const alignFrameRef = useRef<number | null>(null);

  /**
   * 计算当前内容需要的动态底部留白。
   *
   * @returns 底部留白高度。
   */
  const resolveBottomSpacerHeight = useCallback(() => {
    const scrollElement = scrollRef.current;
    if (!scrollElement || !visibleSections.length) {
      return ANCHOR_MIN_BOTTOM_SPACER;
    }
    const lastSection = visibleSections[visibleSections.length - 1];
    const lastSectionElement = sectionRefs.current[lastSection.key];
    if (!lastSectionElement) {
      return ANCHOR_MIN_BOTTOM_SPACER;
    }
    const currentSpacerHeight = spacerRef.current?.offsetHeight || 0;
    const contentHeightWithoutSpacer = scrollElement.scrollHeight - currentSpacerHeight;
    const lastSectionTop = resolveSectionRawTop(scrollElement, lastSectionElement);
    const requiredHeight =
      lastSectionTop - (contentHeightWithoutSpacer - scrollElement.clientHeight) + 2;
    return Math.max(ANCHOR_MIN_BOTTOM_SPACER, Math.ceil(requiredHeight));
  }, [visibleSections]);

  /**
   * 刷新动态底部留白。
   *
   * @returns 无返回值。
   */
  const refreshBottomSpacerHeight = useCallback(() => {
    const nextHeight = resolveBottomSpacerHeight();
    setBottomSpacerHeight((currentHeight) =>
      Math.abs(currentHeight - nextHeight) > 1 ? nextHeight : currentHeight,
    );
  }, [resolveBottomSpacerHeight]);

  /**
   * 滚动时同步当前激活锚点。
   *
   * @returns 无返回值。
   */
  const handleScroll = useCallback(() => {
    const scrollElement = scrollRef.current;
    if (!scrollElement) {
      return;
    }
    if (clickLockedSectionKeyRef.current) {
      setActiveSectionKey(clickLockedSectionKeyRef.current);
      return;
    }
    const scrollTop = scrollElement.scrollTop;
    const activationOffset = Math.min(Math.max(scrollElement.clientHeight * 0.22, 80), 180);
    let nextActiveKey = firstVisibleSectionKey;
    for (const section of visibleSections) {
      const sectionElement = sectionRefs.current[section.key];
      if (!sectionElement) {
        continue;
      }
      if (resolveSectionRawTop(scrollElement, sectionElement) - scrollTop <= activationOffset) {
        nextActiveKey = section.key;
      }
    }
    setActiveSectionKey(nextActiveKey);
  }, [firstVisibleSectionKey, visibleSections]);

  /**
   * 滚动到指定区块。
   *
   * @param sectionKey 区块 key。
   * @returns 无返回值。
   */
  const scrollToSection = useCallback(
    (sectionKey: string) => {
      const scrollElement = scrollRef.current;
      const sectionElement = sectionRefs.current[sectionKey];
      if (!scrollElement || !sectionElement) {
        return;
      }
      if (clickLockTimerRef.current !== null) {
        window.clearTimeout(clickLockTimerRef.current);
      }
      clickLockedSectionKeyRef.current = sectionKey;
      setActiveSectionKey(sectionKey);
      if (alignFrameRef.current !== null) {
        window.cancelAnimationFrame(alignFrameRef.current);
      }
      refreshBottomSpacerHeight();
      applySectionScrollTop(scrollElement, sectionElement);
      alignFrameRef.current = window.requestAnimationFrame(() => {
        applySectionScrollTop(scrollElement, sectionElement);
        alignFrameRef.current = window.requestAnimationFrame(() => {
          applySectionScrollTop(scrollElement, sectionElement);
          alignFrameRef.current = null;
        });
      });
      clickLockTimerRef.current = window.setTimeout(() => {
        clickLockedSectionKeyRef.current = null;
        clickLockTimerRef.current = null;
        handleScroll();
      }, 300);
    },
    [handleScroll, refreshBottomSpacerHeight],
  );

  /**
   * 区块变化时重置激活项。
   *
   * @returns 无返回值。
   */
  useEffect(() => {
    setActiveSectionKey(firstVisibleSectionKey);
    setBottomSpacerHeight(ANCHOR_MIN_BOTTOM_SPACER);
    clickLockedSectionKeyRef.current = null;
    if (clickLockTimerRef.current !== null) {
      window.clearTimeout(clickLockTimerRef.current);
      clickLockTimerRef.current = null;
    }
    if (scrollRef.current) {
      scrollRef.current.scrollTop = 0;
    }
  }, [firstVisibleSectionKey, visibleSectionKeyText, resetKey]);

  /**
   * 内容高度或容器高度变化时重新计算底部留白。
   *
   * @returns 清理函数。
   */
  useEffect(() => {
    const scrollElement = scrollRef.current;
    if (!scrollElement) {
      return undefined;
    }
    refreshBottomSpacerHeight();
    const resizeObserver = new ResizeObserver(() => {
      refreshBottomSpacerHeight();
    });
    resizeObserver.observe(scrollElement);
    visibleSections.forEach((section) => {
      const sectionElement = sectionRefs.current[section.key];
      if (sectionElement) {
        resizeObserver.observe(sectionElement);
      }
    });
    window.addEventListener('resize', refreshBottomSpacerHeight);
    return () => {
      resizeObserver.disconnect();
      window.removeEventListener('resize', refreshBottomSpacerHeight);
    };
  }, [refreshBottomSpacerHeight, visibleSectionKeyText, visibleSections]);

  /**
   * 组件卸载时清理点击锁定定时器。
   *
   * @returns 清理函数。
   */
  useEffect(
    () => () => {
      if (clickLockTimerRef.current !== null) {
        window.clearTimeout(clickLockTimerRef.current);
      }
      if (alignFrameRef.current !== null) {
        window.cancelAnimationFrame(alignFrameRef.current);
      }
    },
    [],
  );

  return (
    <div className={styles.anchorDetailLayout}>
      <div className={styles.anchorDetailHeader}>{header}</div>
      <nav className={styles.anchorNav}>
        {visibleSections.map((section) => (
          <button
            key={section.key}
            type="button"
            className={[
              styles.anchorNavItem,
              activeSectionKey === section.key ? styles.anchorNavItemActive : '',
            ]
              .filter(Boolean)
              .join(' ')}
            onClick={(event) => {
              event.preventDefault();
              scrollToSection(section.key);
            }}
          >
            {section.label}
          </button>
        ))}
      </nav>
      <div ref={scrollRef} className={styles.anchorScrollBody} onScroll={handleScroll}>
        {visibleSections.map((section) => (
          <section
            key={section.key}
            ref={(element) => {
              sectionRefs.current[section.key] = element;
            }}
            className={styles.anchorSection}
          >
            <div className={styles.anchorSectionTitle}>
              <Text strong>{section.label}</Text>
            </div>
            {section.render()}
          </section>
        ))}
        <div
          ref={spacerRef}
          aria-hidden
          className={styles.anchorScrollSpacer}
          style={{ height: bottomSpacerHeight }}
        />
      </div>
    </div>
  );
};

export default TraceAnchorDetailLayout;
