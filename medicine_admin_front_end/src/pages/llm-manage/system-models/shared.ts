import type { SecondaryMenuItem } from '@/components/SecondaryMenu';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import { usePermission } from '@/hooks/usePermission';
import {
  type LayoutSecondaryMenuConfig,
  useLayoutSecondaryMenu,
} from '@/layouts/LayoutSecondaryMenuContext';
import {
  buildLlmSystemModelsSecondaryRoutePath,
  isSecondaryMenuRouteEnabled,
  routePaths,
  type LlmSystemModelsSecondaryRouteKey,
} from '@/router/paths';
import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

/** 系统模型配置页面标题。 */
export const SYSTEM_MODELS_PAGE_TITLE = '系统模型配置';

export interface ConfigSectionRef {
  reload: () => Promise<void>;
  save: () => Promise<void>;
}

/** 系统模型配置默认二级菜单 key。 */
export const DEFAULT_LLM_SYSTEM_MODELS_SECONDARY_MENU_KEY: LlmSystemModelsSecondaryRouteKey =
  'adminConfig';

/** 系统模型配置二级菜单宽度。 */
export const LLM_SYSTEM_MODELS_SECONDARY_MENU_WIDTH = 136;

/** 系统模型配置二级菜单项。 */
export const LLM_SYSTEM_MODELS_SECONDARY_MENU_ITEMS: SecondaryMenuItem[] = [
  {
    key: 'adminConfig',
    label: '管理端配置',
    access: ADMIN_PERMISSIONS.agentConfig.adminQuery,
  },
  {
    key: 'clientConfig',
    label: '客户端配置',
    access: ADMIN_PERMISSIONS.agentConfig.clientQuery,
  },
  {
    key: 'commonCapability',
    label: '通用能力',
    access: ADMIN_PERMISSIONS.agentConfig.commonQuery,
  },
];

/** 系统模型配置路由与二级菜单 key 的映射。 */
const LLM_SYSTEM_MODELS_ROUTE_TAB_MAP: Record<string, LlmSystemModelsSecondaryRouteKey> = {
  [routePaths.llmSystemModels]: DEFAULT_LLM_SYSTEM_MODELS_SECONDARY_MENU_KEY,
  [routePaths.llmSystemModelsAdminConfig]: 'adminConfig',
  [routePaths.llmSystemModelsClientConfig]: 'clientConfig',
  [routePaths.llmSystemModelsCommonCapability]: 'commonCapability',
};

/**
 * 统一提取系统模型配置页面错误文案。
 *
 * @param error 原始异常对象。
 * @param fallback 默认错误提示。
 * @returns 可展示给用户的错误文案。
 */
export function getSystemModelsErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
}

/**
 * 根据当前路径解析系统模型配置二级菜单 key。
 *
 * @param pathname 当前页面路径。
 * @returns 当前命中的二级菜单 key。
 */
export function systemModelsTabFromPathname(pathname: string): LlmSystemModelsSecondaryRouteKey {
  return LLM_SYSTEM_MODELS_ROUTE_TAB_MAP[pathname] || DEFAULT_LLM_SYSTEM_MODELS_SECONDARY_MENU_KEY;
}

/**
 * 注册系统模型配置页面使用的布局级二级菜单。
 *
 * @returns 当前激活的系统模型配置二级菜单 key。
 */
export function useSystemModelsSecondaryMenu(): LlmSystemModelsSecondaryRouteKey {
  const location = useLocation();
  const navigate = useNavigate();
  const pathname = location.pathname;
  const [manualActiveTab, setManualActiveTab] = React.useState<LlmSystemModelsSecondaryRouteKey>(
    DEFAULT_LLM_SYSTEM_MODELS_SECONDARY_MENU_KEY,
  );
  const secondaryRouteEnabled = isSecondaryMenuRouteEnabled(routePaths.llmSystemModels);
  const activeTab = secondaryRouteEnabled ? systemModelsTabFromPathname(pathname) : manualActiveTab;
  const { canAccess } = usePermission();

  React.useEffect(() => {
    const activeItem = LLM_SYSTEM_MODELS_SECONDARY_MENU_ITEMS.find(
      (item) => item.key === activeTab,
    );
    if (activeItem && canAccess(activeItem.access)) {
      return;
    }

    const nextItem = LLM_SYSTEM_MODELS_SECONDARY_MENU_ITEMS.find((item) => canAccess(item.access));
    if (!nextItem) {
      return;
    }

    const nextTab = nextItem.key as LlmSystemModelsSecondaryRouteKey;
    if (secondaryRouteEnabled) {
      const nextPath = buildLlmSystemModelsSecondaryRoutePath(nextTab);
      if (nextPath !== pathname) {
        navigate(nextPath, { replace: true });
      }
      return;
    }

    setManualActiveTab(nextTab);
  }, [activeTab, canAccess, navigate, pathname, secondaryRouteEnabled]);

  /**
   * 处理布局二级菜单切换。
   *
   * @param key 二级菜单 key。
   * @returns 无返回值。
   */
  const handleSecondaryMenuChange = React.useCallback(
    (key: string) => {
      const nextTab = key as LlmSystemModelsSecondaryRouteKey;
      if (secondaryRouteEnabled) {
        const nextPath = buildLlmSystemModelsSecondaryRoutePath(nextTab);
        if (nextPath !== pathname) {
          navigate(nextPath);
        }
        return;
      }
      setManualActiveTab(nextTab);
    },
    [navigate, pathname, secondaryRouteEnabled],
  );

  /**
   * 系统模型配置布局二级菜单配置。
   */
  const secondaryMenuConfig = React.useMemo<LayoutSecondaryMenuConfig>(
    () => ({
      items: LLM_SYSTEM_MODELS_SECONDARY_MENU_ITEMS,
      activeKey: activeTab,
      onChange: handleSecondaryMenuChange,
      width: LLM_SYSTEM_MODELS_SECONDARY_MENU_WIDTH,
    }),
    [activeTab, handleSecondaryMenuChange],
  );

  useLayoutSecondaryMenu(secondaryMenuConfig);

  return activeTab;
}
