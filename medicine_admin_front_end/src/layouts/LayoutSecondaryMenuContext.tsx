import React from 'react';
import type { SecondaryMenuItem } from '@/components/SecondaryMenu';

/**
 * 布局级二级菜单配置。
 */
export interface LayoutSecondaryMenuConfig {
  /** 菜单项列表 */
  items: SecondaryMenuItem[];
  /** 当前激活菜单 key */
  activeKey: string;
  /** 菜单点击回调 */
  onChange: (key: string) => void;
  /** 二级菜单列宽度 */
  width?: number;
}

/**
 * 布局级二级菜单上下文数据。
 */
interface LayoutSecondaryMenuContextValue {
  /** 更新布局级二级菜单配置 */
  setSecondaryMenuConfig: (config: LayoutSecondaryMenuConfig | null) => void;
}

/**
 * 布局级二级菜单上下文。
 */
export const LayoutSecondaryMenuContext =
  React.createContext<LayoutSecondaryMenuContextValue | null>(null);

/**
 * 将页面级二级菜单注册到布局侧边栏。
 * @param config 布局级二级菜单配置。
 * @returns 无返回值。
 */
export function useLayoutSecondaryMenu(config: LayoutSecondaryMenuConfig | null): void {
  const context = React.useContext(LayoutSecondaryMenuContext);

  React.useEffect(() => {
    context?.setSecondaryMenuConfig(config);
  }, [config, context]);

  React.useEffect(() => {
    return () => {
      context?.setSecondaryMenuConfig(null);
    };
  }, [context]);
}
