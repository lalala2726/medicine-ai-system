import React from 'react';

export interface TableActionGroupProps {
  /** 表格操作按钮节点。 */
  children: React.ReactNode;
  /** 操作按钮之间的间距。 */
  gap?: React.CSSProperties['gap'];
  /** 操作按钮是否允许换行。 */
  wrap?: boolean;
  /** 自定义样式。 */
  style?: React.CSSProperties;
}

/**
 * 表格操作列按钮容器，统一让操作按钮在单元格内居中展示。
 * @param props 表格操作按钮容器属性。
 * @returns 居中展示的操作按钮组节点。
 */
const TableActionGroup: React.FC<TableActionGroupProps> = ({
  children,
  gap = 0,
  wrap = false,
  style,
}) => {
  return (
    <div
      style={{
        alignItems: 'center',
        display: 'flex',
        flexWrap: wrap ? 'wrap' : 'nowrap',
        gap,
        justifyContent: 'center',
        whiteSpace: wrap ? undefined : 'nowrap',
        ...style,
      }}
    >
      {children}
    </div>
  );
};

export default TableActionGroup;
