/**
 * 应用主布局
 * 使用 ProLayout 替代 UmiJS 的 layout 插件
 */
import type { ProLayoutProps } from '@ant-design/pro-components';
import { ProLayout } from '@ant-design/pro-components';
import React from 'react';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  AdminWatermark,
  AppIcon,
  AvatarDropdown,
  AvatarName,
  Footer,
  Question,
  ThemeToggle,
} from '@/components';
import DisclaimerModal from '@/components/DisclaimerModal';
import {
  LayoutSecondaryMenuContext,
  type LayoutSecondaryMenuConfig,
} from '@/layouts/LayoutSecondaryMenuContext';
import { routePaths } from '@/router/paths';
import { ThemeContext } from '@/contexts/ThemeContext';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import { canAccessByPermissions } from '@/utils/permission';
import { getAuthState } from './AuthGuard';
import { Avatar } from 'antd';
import { UserOutlined } from '@ant-design/icons';
import SideMenu, { getMenuMatchState } from '../components/SideMenu';
import defaultSettings, {
  applyThemeSettings,
  type AppThemeMode,
} from '../../config/defaultSettings';
import { menuRoutes, type MenuRoute } from '../router';

const LAYOUT_BODY_CLASS = 'app-layout-body';
const LAYOUT_HTML_CLASS = 'app-layout-html';
const THEME_CHANGE_EVENT = 'app-theme-change';

/**
 * 将菜单路由配置转换为 ProLayout 接受的 route 格式
 */
function convertMenuRoutes(routes: MenuRoute[]): any[] {
  return routes.map((route) => {
    const hasChildren = route.children && route.children.length > 0;
    const item: any = {
      path: route.path,
      name: route.name,
      // 有子菜单的一级菜单不显示 icon
      icon: hasChildren ? undefined : route.icon ? <AppIcon name={route.icon} /> : undefined,
      hideInMenu: route.hideInMenu,
    };
    if (route.children) {
      item.routes = convertMenuRoutes(route.children);
    }
    return item;
  });
}

const THEME_STORAGE_KEY = 'app-nav-theme';

const LAST_CONSOLE_PATH_KEY = 'app-last-console-path';
const DEFAULT_EXPANDED_MENU_KEYS = [routePaths.mall, routePaths.system, routePaths.llmManage];
/** 一级菜单列宽度。 */
const PRIMARY_SIDER_MENU_WIDTH = 210;
/** 二级菜单列默认宽度。 */
const DEFAULT_SECONDARY_MENU_WIDTH = 136;

type RouteMenuState = {
  defaultMenuCollapsed?: boolean;
  menuCollapsed?: boolean;
  defaultMenuOpenKeys?: string[] | string;
  menuOpenKeys?: string[] | string;
};

function normalizeMenuKey(key: string): string {
  if (!key || key === '/') {
    return '/';
  }

  return key.replace(/\/+$/, '') || '/';
}

function parseBooleanFlag(value: unknown): boolean | undefined {
  if (typeof value === 'boolean') {
    return value;
  }

  if (typeof value === 'number') {
    return value !== 0;
  }

  if (typeof value !== 'string') {
    return undefined;
  }

  const normalizedValue = value.trim().toLowerCase();

  if (['1', 'true', 'yes', 'collapsed'].includes(normalizedValue)) {
    return true;
  }

  if (['0', 'false', 'no', 'expanded'].includes(normalizedValue)) {
    return false;
  }

  return undefined;
}

function parseMenuOpenKeys(value: unknown): string[] | undefined {
  if (Array.isArray(value)) {
    return value.filter((item): item is string => typeof item === 'string');
  }

  if (typeof value === 'string') {
    return value
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean);
  }

  return undefined;
}

function collectExpandableMenuKeys(routes: MenuRoute[]): string[] {
  return routes.flatMap((route) => {
    if (!route.children?.length) {
      return [];
    }

    return route.hideInMenu
      ? collectExpandableMenuKeys(route.children)
      : [route.path, ...collectExpandableMenuKeys(route.children)];
  });
}

/**
 * 根据当前用户权限过滤菜单。
 * @param routes 原始菜单列表。
 * @param currentUser 当前用户信息。
 * @returns 有权限展示的菜单列表。
 */
function filterMenuRoutesByPermission(
  routes: MenuRoute[],
  currentUser: API.CurrentUser | undefined,
): MenuRoute[] {
  return routes.flatMap((route) => {
    if (route.hideInMenu) {
      return [];
    }

    const visibleChildren = route.children?.length
      ? filterMenuRoutesByPermission(route.children, currentUser)
      : [];

    if (route.children?.length) {
      return visibleChildren.length > 0 ? [{ ...route, children: visibleChildren }] : [];
    }

    return canAccessByPermissions(currentUser, route.access) ? [route] : [];
  });
}

/**
 * 获取第一个可见菜单路径。
 * @param routes 菜单列表。
 * @returns 第一个可见菜单路径。
 */
function getFirstVisibleMenuPath(routes: MenuRoute[]): string | null {
  for (const route of routes) {
    if (route.children?.length) {
      const childPath = getFirstVisibleMenuPath(route.children);
      if (childPath) {
        return childPath;
      }
    }

    if (!route.children?.length) {
      return route.path;
    }
  }

  return null;
}

function isSameStringArray(current: string[], next: string[]): boolean {
  if (current.length !== next.length) {
    return false;
  }
  const sortedCurrent = [...current].sort();
  const sortedNext = [...next].sort();
  return sortedCurrent.every((item, index) => item === sortedNext[index]);
}

const AppLayout: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const authState = getAuthState();
  const { currentUser } = authState;
  const pathname = location.pathname;
  const search = location.search;
  const fullPath = pathname + search;
  const routeMenuState = (location.state as RouteMenuState | null) ?? null;
  const permissionMenuRoutes = React.useMemo(
    () => filterMenuRoutesByPermission(menuRoutes, currentUser),
    [currentUser],
  );
  const firstVisibleMenuPath = React.useMemo(
    () => getFirstVisibleMenuPath(permissionMenuRoutes),
    [permissionMenuRoutes],
  );
  /** 当前用户是否可以进入顶部智能助手模式。 */
  const canAccessSmartAssistant = React.useMemo(
    () => canAccessByPermissions(currentUser, ADMIN_PERMISSIONS.smartAssistant.access),
    [currentUser],
  );
  const isAssistantMode =
    pathname === routePaths.smartAssistant || pathname.startsWith(`${routePaths.smartAssistant}/`);
  const menuMatchState = React.useMemo(
    () => getMenuMatchState(pathname, permissionMenuRoutes),
    [pathname, permissionMenuRoutes],
  );
  const expandableMenuKeys = React.useMemo(
    () => collectExpandableMenuKeys(permissionMenuRoutes),
    [permissionMenuRoutes],
  );
  const expandableMenuKeySet = React.useMemo(
    () => new Set(expandableMenuKeys.map((key) => normalizeMenuKey(key))),
    [expandableMenuKeys],
  );
  const explicitMenuCollapsed = React.useMemo(() => {
    const stateValue = parseBooleanFlag(
      routeMenuState?.defaultMenuCollapsed ?? routeMenuState?.menuCollapsed,
    );

    if (stateValue !== undefined) {
      return stateValue;
    }

    const searchParams = new URLSearchParams(search);
    return parseBooleanFlag(
      searchParams.get('defaultMenuCollapsed') ?? searchParams.get('menuCollapsed'),
    );
  }, [search, routeMenuState]);
  const explicitMenuOpenKeys = React.useMemo(() => {
    const stateValue = parseMenuOpenKeys(
      routeMenuState?.defaultMenuOpenKeys ?? routeMenuState?.menuOpenKeys,
    );

    const queryValue = parseMenuOpenKeys(
      new URLSearchParams(search).get('defaultMenuOpenKeys') ??
        new URLSearchParams(search).get('menuOpenKeys'),
    );

    const rawKeys = stateValue ?? queryValue;

    if (!rawKeys) {
      return undefined;
    }

    return Array.from(
      new Set(
        rawKeys.map((key) => normalizeMenuKey(key)).filter((key) => expandableMenuKeySet.has(key)),
      ),
    );
  }, [expandableMenuKeySet, search, routeMenuState]);
  const defaultExpandedMenuKeys = React.useMemo(
    () =>
      Array.from(
        new Set([...DEFAULT_EXPANDED_MENU_KEYS.map(normalizeMenuKey), ...menuMatchState.openKeys]),
      ).filter((key) => expandableMenuKeySet.has(key)),
    [expandableMenuKeySet, menuMatchState.openKeys],
  );
  const [openMenuKeys, setOpenMenuKeys] = React.useState<string[]>(() => {
    if (explicitMenuCollapsed) {
      return [];
    }

    return explicitMenuOpenKeys ?? defaultExpandedMenuKeys;
  });
  const [secondaryMenuConfig, setSecondaryMenuConfig] =
    React.useState<LayoutSecondaryMenuConfig | null>(null);
  const explicitMenuOpenKeysKey =
    explicitMenuOpenKeys === undefined ? '__unset__' : explicitMenuOpenKeys.join('|');
  const matchedOpenKeysKey = menuMatchState.openKeys.join('|');

  // 记住最后的控制台路由，从智能助手切换回来时恢复
  const lastConsolePathRef = React.useRef<string>(
    (() => {
      try {
        return (
          sessionStorage.getItem(LAST_CONSOLE_PATH_KEY) ||
          firstVisibleMenuPath ||
          routePaths.analytics
        );
      } catch {
        return firstVisibleMenuPath || routePaths.analytics;
      }
    })(),
  );

  React.useEffect(() => {
    if (!isAssistantMode && pathname && pathname !== '/') {
      lastConsolePathRef.current = fullPath;
      try {
        sessionStorage.setItem(LAST_CONSOLE_PATH_KEY, fullPath);
      } catch {
        // Ignore sessionStorage write failures in restricted environments.
      }
    }
  }, [pathname, search, fullPath, isAssistantMode]);

  const consoleEntryPath = isAssistantMode
    ? lastConsolePathRef.current
    : fullPath || firstVisibleMenuPath || routePaths.analytics;
  const assistantEntryPath = isAssistantMode ? fullPath : routePaths.smartAssistant;

  const [settings, setSettings] = React.useState<Partial<ProLayoutProps>>(() => {
    const savedTheme = localStorage.getItem(THEME_STORAGE_KEY);
    const navTheme: AppThemeMode = savedTheme === 'realDark' ? 'realDark' : 'light';

    return {
      ...applyThemeSettings(defaultSettings as Partial<ProLayoutProps>, navTheme),
    };
  });

  const currentTheme: AppThemeMode = settings.navTheme === 'realDark' ? 'realDark' : 'light';
  const isDark = currentTheme === 'realDark';
  const resolvedSecondaryMenuConfig = secondaryMenuConfig
    ? {
        ...secondaryMenuConfig,
        width: secondaryMenuConfig.width ?? DEFAULT_SECONDARY_MENU_WIDTH,
      }
    : null;
  const currentSiderWidth = resolvedSecondaryMenuConfig
    ? PRIMARY_SIDER_MENU_WIDTH + resolvedSecondaryMenuConfig.width + 1
    : PRIMARY_SIDER_MENU_WIDTH;
  const layoutClassName = [
    'app-pro-layout',
    isAssistantMode ? 'app-pro-layout--assistant' : '',
    isDark ? 'app-pro-layout--dark' : '',
  ]
    .filter(Boolean)
    .join(' ');

  const persistTheme = React.useCallback((nextTheme: 'light' | 'realDark') => {
    localStorage.setItem(THEME_STORAGE_KEY, nextTheme);
    window.dispatchEvent(new Event(THEME_CHANGE_EVENT));
  }, []);

  // 同步多标签页的暗色模式状态
  React.useEffect(() => {
    const handleThemeSync = () => {
      const savedTheme = localStorage.getItem(THEME_STORAGE_KEY);
      const nextTheme: AppThemeMode = savedTheme === 'realDark' ? 'realDark' : 'light';

      setSettings((prev) => {
        if (prev.navTheme === nextTheme) return prev;
        return applyThemeSettings(prev, nextTheme);
      });
    };

    window.addEventListener('storage', handleThemeSync);
    window.addEventListener(THEME_CHANGE_EVENT, handleThemeSync);

    return () => {
      window.removeEventListener('storage', handleThemeSync);
      window.removeEventListener(THEME_CHANGE_EVENT, handleThemeSync);
    };
  }, []);

  React.useEffect(() => {
    if (explicitMenuCollapsed) {
      setOpenMenuKeys((prev) => (prev.length === 0 ? prev : []));
      return;
    }

    if (explicitMenuOpenKeys !== undefined) {
      setOpenMenuKeys((prev) =>
        isSameStringArray(prev, explicitMenuOpenKeys) ? prev : explicitMenuOpenKeys,
      );
      return;
    }

    if (menuMatchState.openKeys.length === 0) {
      return;
    }

    setOpenMenuKeys((prev) => {
      const nextKeys = Array.from(new Set([...prev, ...menuMatchState.openKeys]));
      return isSameStringArray(prev, nextKeys) ? prev : nextKeys;
    });
  }, [
    explicitMenuCollapsed,
    explicitMenuOpenKeys,
    explicitMenuOpenKeysKey,
    matchedOpenKeysKey,
    menuMatchState.openKeys,
  ]);

  const handleMenuClick = React.useCallback(
    (path: string) => {
      if (path !== pathname) {
        navigate(path);
      }
    },
    [navigate, pathname],
  );

  const handleMenuOpenChange = React.useCallback(
    (nextOpenKeys: string[]) => {
      const normalizedOpenKeys = Array.from(
        new Set(
          nextOpenKeys
            .map((key) => normalizeMenuKey(key))
            .filter((key) => expandableMenuKeySet.has(key)),
        ),
      );

      setOpenMenuKeys((prev) =>
        isSameStringArray(prev, normalizedOpenKeys) ? prev : normalizedOpenKeys,
      );
    },
    [expandableMenuKeySet],
  );

  React.useEffect(() => {
    document.body.classList.add(LAYOUT_BODY_CLASS);
    document.documentElement.classList.add(LAYOUT_HTML_CLASS);

    return () => {
      document.body.classList.remove(LAYOUT_BODY_CLASS);
      document.documentElement.classList.remove(LAYOUT_HTML_CLASS);
    };
  }, []);

  const toggleTheme = React.useCallback(() => {
    setSettings((prev) => {
      const nextTheme: AppThemeMode = prev.navTheme === 'realDark' ? 'light' : 'realDark';
      persistTheme(nextTheme);
      return applyThemeSettings(prev, nextTheme);
    });
  }, [persistTheme]);

  return (
    <ThemeContext.Provider value={{ isDark }}>
      <LayoutSecondaryMenuContext.Provider value={{ setSecondaryMenuConfig }}>
        <AdminWatermark currentUser={currentUser}>
          <DisclaimerModal />
          <ProLayout
            title="药智通后台管理系统"
            logo="https://gw.alipayobjects.com/zos/rmsportal/KDpgvguMpGfqaHPjicRK.svg"
            layout="mix"
            location={{ pathname: location.pathname }}
            route={{
              path: '/',
              routes: convertMenuRoutes(permissionMenuRoutes),
            }}
            headerTitleRender={(logo, title) => (
              <div className="app-layout-header-brand">
                <Link to={firstVisibleMenuPath || '/'} className="app-layout-header-home">
                  {logo ? <span className="app-layout-header-logo">{logo}</span> : null}
                  {title ? <span className="app-layout-header-title">{title}</span> : null}
                </Link>
                <nav className="app-layout-mode-switcher" aria-label="模式切换">
                  <Link
                    to={consoleEntryPath}
                    aria-current={isAssistantMode ? undefined : 'page'}
                    className={`app-layout-mode-link ${!isAssistantMode ? 'is-active' : ''}`}
                  >
                    控制台
                  </Link>
                  {canAccessSmartAssistant && (
                    <Link
                      to={assistantEntryPath}
                      aria-current={isAssistantMode ? 'page' : undefined}
                      className={`app-layout-mode-link ${isAssistantMode ? 'is-active' : ''}`}
                    >
                      智能助手
                    </Link>
                  )}
                </nav>
              </div>
            )}
            actionsRender={() => [
              <ThemeToggle key="theme" isDark={isDark} onToggle={toggleTheme} />,
              <Question key="doc" />,
              <AvatarDropdown key="avatar">
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 8,
                    padding: '0 8px',
                    cursor: 'pointer',
                  }}
                >
                  <Avatar
                    size="small"
                    src={currentUser?.avatar}
                    icon={!currentUser?.avatar && <UserOutlined />}
                  />
                  <AvatarName />
                </div>
              </AvatarDropdown>,
            ]}
            avatarProps={false as any}
            footerRender={isAssistantMode ? false : () => <Footer />}
            siderWidth={currentSiderWidth}
            onPageChange={() => {
              // 路由变化时的处理
            }}
            menuItemRender={(item: any, dom: React.ReactNode) => (
              <Link to={item.path || '/'}>{dom}</Link>
            )}
            menuContentRender={
              isAssistantMode
                ? false
                : (props) => (
                    <SideMenu
                      menuRoutes={permissionMenuRoutes}
                      pathname={pathname}
                      collapsed={Boolean(props.collapsed)}
                      openKeys={openMenuKeys}
                      onOpenChange={handleMenuOpenChange}
                      onMenuClick={handleMenuClick}
                      primaryMenuWidth={PRIMARY_SIDER_MENU_WIDTH}
                      secondaryMenuConfig={resolvedSecondaryMenuConfig}
                    />
                  )
            }
            collapsed={false}
            collapsedButtonRender={false}
            onCollapse={() => {}}
            menuRender={isAssistantMode ? false : undefined}
            menuHeaderRender={undefined}
            {...settings}
            className={layoutClassName}
            style={{ height: '100%' }}
            contentStyle={{ minHeight: 0 }}
          >
            <div className="app-layout-content-shell">
              <div className="app-layout-content-wrapper">
                <div className="app-layout-content-page">
                  <Outlet />
                </div>
              </div>
            </div>
          </ProLayout>
        </AdminWatermark>
      </LayoutSecondaryMenuContext.Provider>
    </ThemeContext.Provider>
  );
};

export default AppLayout;
