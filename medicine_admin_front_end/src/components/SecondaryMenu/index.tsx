import React from 'react';
import { usePermission } from '@/hooks/usePermission';
import styles from './index.module.less';

/**
 * 二级菜单项定义。
 */
export interface SecondaryMenuItem {
  /** 菜单唯一标识 */
  key: string;
  /** 菜单展示文案 */
  label: React.ReactNode;
  /** 是否禁用 */
  disabled?: boolean;
  /** 菜单需要的权限编码 */
  access?: string | string[];
}

/**
 * 二级菜单组件属性。
 */
export interface SecondaryMenuProps {
  /** 当前激活菜单标识 */
  activeKey: string;
  /** 菜单项列表 */
  items: SecondaryMenuItem[];
  /** 菜单切换回调 */
  onChange: (key: string) => void;
  /** 自定义容器类名 */
  className?: string;
}

/**
 * 通用二级菜单组件。
 * @param props 组件属性。
 * @returns 二级菜单节点。
 */
const SecondaryMenu: React.FC<SecondaryMenuProps> = ({ activeKey, items, onChange, className }) => {
  const { canAccess } = usePermission();
  const accessibleItems = React.useMemo(
    () => items.filter((item) => canAccess(item.access)),
    [canAccess, items],
  );

  return (
    <div className={`${styles.secondaryMenu} ${className || ''}`.trim()}>
      {accessibleItems.map((item) => {
        const isActive = item.key === activeKey;
        const itemClassName = [
          styles.menuItem,
          isActive ? styles.menuItemActive : '',
          item.disabled ? styles.menuItemDisabled : '',
        ]
          .filter(Boolean)
          .join(' ');

        return (
          <button
            key={item.key}
            type="button"
            className={itemClassName}
            disabled={item.disabled}
            onClick={() => {
              onChange(item.key);
            }}
          >
            <span className={styles.menuItemLabel}>{item.label}</span>
          </button>
        );
      })}
    </div>
  );
};

export default SecondaryMenu;
