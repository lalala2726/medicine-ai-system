import type { SecondaryMenuItem } from '@/components/SecondaryMenu';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import { usePermission } from '@/hooks/usePermission';
import {
  type LayoutSecondaryMenuConfig,
  useLayoutSecondaryMenu,
} from '@/layouts/LayoutSecondaryMenuContext';
import {
  buildLlmAgentObservabilitySecondaryRoutePath,
  isSecondaryMenuRouteEnabled,
  routePaths,
  type LlmAgentObservabilitySecondaryRouteKey,
} from '@/router/paths';
import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

/** 智能体观测默认二级菜单 key。 */
export const DEFAULT_LLM_AGENT_OBSERVABILITY_SECONDARY_MENU_KEY: LlmAgentObservabilitySecondaryRouteKey =
  'monitor';

/** 智能体观测二级菜单宽度。 */
export const LLM_AGENT_OBSERVABILITY_SECONDARY_MENU_WIDTH = 136;

/** 智能体观测二级菜单项。 */
export const LLM_AGENT_OBSERVABILITY_SECONDARY_MENU_ITEMS: SecondaryMenuItem[] = [
  {
    key: 'monitor',
    label: '监控面板',
    access: ADMIN_PERMISSIONS.agentTrace.monitor,
  },
  {
    key: 'trace',
    label: '智能体跟踪',
    access: ADMIN_PERMISSIONS.agentTrace.list,
  },
];

/** 智能体观测路由与二级菜单 key 的映射。 */
const LLM_AGENT_OBSERVABILITY_ROUTE_TAB_MAP: Record<
  string,
  LlmAgentObservabilitySecondaryRouteKey
> = {
  [routePaths.llmAgentObservability]: DEFAULT_LLM_AGENT_OBSERVABILITY_SECONDARY_MENU_KEY,
  [routePaths.llmAgentTrace]: 'trace',
  [routePaths.llmAgentMonitor]: 'monitor',
  [routePaths.llmAgentMonitorModelDetail]: 'monitor',
};

/**
 * 根据当前路径解析智能体观测二级菜单 key。
 * @param pathname 当前页面路径。
 * @returns 当前命中的二级菜单 key。
 */
export function agentObservabilityTabFromPathname(
  pathname: string,
): LlmAgentObservabilitySecondaryRouteKey {
  return (
    LLM_AGENT_OBSERVABILITY_ROUTE_TAB_MAP[pathname] ||
    DEFAULT_LLM_AGENT_OBSERVABILITY_SECONDARY_MENU_KEY
  );
}

/**
 * 注册智能体观测页面使用的布局级二级菜单。
 * @returns 当前激活的智能体观测二级菜单 key。
 */
export function useAgentObservabilitySecondaryMenu(): LlmAgentObservabilitySecondaryRouteKey {
  const location = useLocation();
  const navigate = useNavigate();
  const pathname = location.pathname;
  const [manualActiveTab, setManualActiveTab] =
    React.useState<LlmAgentObservabilitySecondaryRouteKey>(
      DEFAULT_LLM_AGENT_OBSERVABILITY_SECONDARY_MENU_KEY,
    );
  const secondaryRouteEnabled = isSecondaryMenuRouteEnabled(routePaths.llmAgentObservability);
  const activeTab = secondaryRouteEnabled
    ? agentObservabilityTabFromPathname(pathname)
    : manualActiveTab;
  const { canAccess } = usePermission();

  React.useEffect(() => {
    const activeItem = LLM_AGENT_OBSERVABILITY_SECONDARY_MENU_ITEMS.find(
      (item) => item.key === activeTab,
    );
    if (activeItem && canAccess(activeItem.access)) {
      return;
    }

    const nextItem = LLM_AGENT_OBSERVABILITY_SECONDARY_MENU_ITEMS.find((item) =>
      canAccess(item.access),
    );
    if (!nextItem) {
      return;
    }

    const nextTab = nextItem.key as LlmAgentObservabilitySecondaryRouteKey;
    if (secondaryRouteEnabled) {
      const nextPath = buildLlmAgentObservabilitySecondaryRoutePath(nextTab);
      if (nextPath !== pathname) {
        navigate(nextPath, { replace: true });
      }
      return;
    }

    setManualActiveTab(nextTab);
  }, [activeTab, canAccess, navigate, pathname, secondaryRouteEnabled]);

  /**
   * 处理布局二级菜单切换。
   * @param key 二级菜单 key。
   * @returns 无返回值。
   */
  const handleSecondaryMenuChange = React.useCallback(
    (key: string) => {
      const nextTab = key as LlmAgentObservabilitySecondaryRouteKey;
      if (secondaryRouteEnabled) {
        const nextPath = buildLlmAgentObservabilitySecondaryRoutePath(nextTab);
        if (nextPath !== pathname) {
          navigate(nextPath);
        }
        return;
      }
      setManualActiveTab(nextTab);
    },
    [navigate, pathname, secondaryRouteEnabled],
  );

  /** 智能体观测布局二级菜单配置。 */
  const secondaryMenuConfig = React.useMemo<LayoutSecondaryMenuConfig>(
    () => ({
      items: LLM_AGENT_OBSERVABILITY_SECONDARY_MENU_ITEMS,
      activeKey: activeTab,
      onChange: handleSecondaryMenuChange,
      width: LLM_AGENT_OBSERVABILITY_SECONDARY_MENU_WIDTH,
    }),
    [activeTab, handleSecondaryMenuChange],
  );

  useLayoutSecondaryMenu(secondaryMenuConfig);

  return activeTab;
}
