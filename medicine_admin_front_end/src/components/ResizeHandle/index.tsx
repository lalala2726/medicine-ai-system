import React from 'react';
import styles from './index.module.less';

export interface ResizeHandleProps {
  /** 无障碍标签。 */
  ariaLabel: string;
  /** 鼠标悬浮提示。 */
  title?: string;
  /** 外部样式类名。 */
  className?: string;
  /** 是否处于拖拽激活状态。 */
  active?: boolean;
  /** 是否禁用拖拽。 */
  disabled?: boolean;
  /** 蓝色提示线位置。 */
  linePlacement?: 'edge' | 'center';
  /** 开始拖拽回调。 */
  onResizeStart: (event: React.PointerEvent<HTMLDivElement>) => void;
  /** 双击重置回调。 */
  onReset?: () => void;
}

/**
 * 合并 CSS 类名。
 *
 * @param classNames CSS 类名列表。
 * @returns 合并后的 CSS 类名。
 */
function joinClassNames(...classNames: Array<string | false | undefined>) {
  return classNames.filter(Boolean).join(' ');
}

/**
 * 通用拖拽边缘组件。
 *
 * @param props 拖拽边缘属性。
 * @returns 拖拽边缘节点。
 */
const ResizeHandle = React.forwardRef<HTMLDivElement, ResizeHandleProps>(
  (
    {
      ariaLabel,
      title,
      className,
      active = false,
      disabled = false,
      linePlacement = 'center',
      onResizeStart,
      onReset,
    },
    ref,
  ) => (
    <div
      ref={ref}
      aria-label={ariaLabel}
      aria-disabled={disabled}
      className={joinClassNames(
        styles.resizeHandle,
        styles[linePlacement],
        active && styles.active,
        disabled && styles.disabled,
        className,
      )}
      onDoubleClick={disabled ? undefined : onReset}
      onPointerDown={disabled ? undefined : onResizeStart}
      role="separator"
      title={title}
    />
  ),
);

ResizeHandle.displayName = 'ResizeHandle';

export default ResizeHandle;
