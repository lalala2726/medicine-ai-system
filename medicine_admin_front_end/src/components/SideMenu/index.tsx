import React, { useMemo } from 'react';
import AppIcon from '@/components/AppIcon';
import SecondaryMenu from '@/components/SecondaryMenu';
import type { LayoutSecondaryMenuConfig } from '@/layouts/LayoutSecondaryMenuContext';
import type { MenuRoute } from '@/router';
import useStyles from './style';

/**
 * 侧边菜单组件属性。
 */
export interface SideMenuProps {
  /** 菜单路由数据 */
  menuRoutes: MenuRoute[];
  /** 当前路径 */
  pathname: string;
  /** 是否折叠 */
  collapsed: boolean;
  /** 点击菜单项回调 */
  onMenuClick: (path: string) => void;
  /** 当前展开的菜单 keys */
  openKeys: string[];
  /** 菜单展开变化回调 */
  onOpenChange: (openKeys: string[]) => void;
  /** 一级菜单列宽度 */
  primaryMenuWidth: number;
  /** 二级菜单配置 */
  secondaryMenuConfig?: LayoutSecondaryMenuConfig | null;
}

/**
 * 可渲染的可见菜单节点。
 */
interface VisibleMenuRoute {
  /** 菜单路径 */
  path: string;
  /** 菜单名称 */
  name?: string;
  /** 菜单图标 */
  icon?: string;
  /** 子菜单列表 */
  children?: VisibleMenuRoute[];
}

/**
 * 展开状态匹配结果。
 */
type OpenKeyMatch = {
  /** 需要展开的父级 keys */
  openKeys: string[];
  /** 当前匹配分数 */
  score: number;
};

/**
 * 选中状态匹配结果。
 */
type SelectedKeyMatch = {
  /** 当前选中的菜单 key */
  key: string;
  /** 当前匹配分数 */
  score: number;
};

/**
 * 规范化菜单路径，统一去掉末尾多余斜杠。
 * @param path 原始路径。
 * @returns 规范化后的路径。
 */
function normalizePath(path: string): string {
  if (!path || path === '/') {
    return '/';
  }
  return path.replace(/\/+$/, '') || '/';
}

/**
 * 将路径拆分为片段，便于逐段匹配。
 * @param path 原始路径。
 * @returns 拆分后的路径片段列表。
 */
function getPathSegments(path: string): string[] {
  return normalizePath(path).split('/').filter(Boolean);
}

/**
 * 计算路由匹配分数，静态段越多、路径越深优先级越高。
 * @param path 路由路径。
 * @returns 当前路由分数。
 */
function getRouteScore(path: string): number {
  const segments = getPathSegments(path);
  const staticSegmentCount = segments.filter((segment) => !segment.startsWith(':')).length;
  return staticSegmentCount * 100 + segments.length;
}

/**
 * 判断当前 pathname 是否命中某个路由模式。
 * @param pattern 路由模式。
 * @param pathname 当前路径。
 * @returns 是否匹配成功。
 */
function matchesRoutePath(pattern: string, pathname: string): boolean {
  const patternSegments = getPathSegments(pattern);
  const pathnameSegments = getPathSegments(pathname);

  if (patternSegments.length > pathnameSegments.length) {
    return false;
  }

  return patternSegments.every((segment, index) => {
    if (segment.startsWith(':')) {
      return pathnameSegments[index] !== undefined;
    }

    return segment === pathnameSegments[index];
  });
}

/**
 * 选择分数更高的匹配结果。
 * @param current 当前匹配结果。
 * @param candidate 新的候选匹配结果。
 * @returns 更优的匹配结果。
 */
function pickBetterMatch<T extends { score: number }>(
  current: T | null,
  candidate: T | null,
): T | null {
  if (!candidate) {
    return current;
  }

  if (!current || candidate.score >= current.score) {
    return candidate;
  }

  return current;
}

/**
 * 递归查找当前路径需要展开的父级菜单。
 * @param pathname 当前路径。
 * @param routes 菜单路由列表。
 * @param visibleParents 当前可见父级路径列表。
 * @returns 最佳展开匹配结果。
 */
function findOpenKeyMatch(
  pathname: string,
  routes: MenuRoute[],
  visibleParents: string[] = [],
): OpenKeyMatch | null {
  let bestMatch: OpenKeyMatch | null = null;

  routes.forEach((route) => {
    const nextVisibleParents =
      route.children?.length && !route.hideInMenu
        ? [...visibleParents, route.path]
        : visibleParents;

    if (matchesRoutePath(route.path, pathname)) {
      bestMatch = pickBetterMatch(bestMatch, {
        openKeys: visibleParents,
        score: getRouteScore(route.path),
      });
    }

    if (route.children?.length) {
      bestMatch = pickBetterMatch(
        bestMatch,
        findOpenKeyMatch(pathname, route.children, nextVisibleParents),
      );
    }
  });

  return bestMatch;
}

/**
 * 递归查找当前路径对应的叶子菜单项。
 * @param pathname 当前路径。
 * @param routes 菜单路由列表。
 * @returns 最佳选中匹配结果。
 */
function findSelectedKeyMatch(pathname: string, routes: MenuRoute[]): SelectedKeyMatch | null {
  let bestMatch: SelectedKeyMatch | null = null;

  routes.forEach((route) => {
    if (route.children?.length) {
      bestMatch = pickBetterMatch(bestMatch, findSelectedKeyMatch(pathname, route.children));
      return;
    }

    if (route.hideInMenu || !matchesRoutePath(route.path, pathname)) {
      return;
    }

    bestMatch = pickBetterMatch(bestMatch, {
      key: route.path,
      score: getRouteScore(route.path),
    });
  });

  return bestMatch;
}

/**
 * 计算菜单的展开 keys 与选中 keys。
 * @param pathname 当前路径。
 * @param routes 菜单路由列表。
 * @returns 菜单匹配状态。
 */
export function getMenuMatchState(pathname: string, routes: MenuRoute[]) {
  const normalizedPathname = normalizePath(pathname);
  const openKeyMatch = findOpenKeyMatch(normalizedPathname, routes);
  const selectedKeyMatch = findSelectedKeyMatch(normalizedPathname, routes);

  return {
    openKeys: Array.from(new Set(openKeyMatch?.openKeys ?? [])),
    selectedKeys: selectedKeyMatch ? [selectedKeyMatch.key] : [],
  };
}

/**
 * 构建真正用于渲染的可见菜单树。
 * @param routes 原始菜单路由列表。
 * @returns 过滤 hideInMenu 后的可见菜单树。
 */
function buildVisibleMenuRoutes(routes: MenuRoute[]): VisibleMenuRoute[] {
  return routes
    .filter((route) => !route.hideInMenu)
    .map((route) => {
      const visibleChildren = route.children?.length
        ? buildVisibleMenuRoutes(route.children)
        : undefined;

      return {
        path: route.path,
        name: route.name,
        icon: route.icon,
        children: visibleChildren?.length ? visibleChildren : undefined,
      };
    });
}

/**
 * 自定义分组折叠菜单组件。
 * @param props 组件属性。
 * @returns 自定义侧边菜单节点。
 */
const SideMenu: React.FC<SideMenuProps> = ({
  menuRoutes,
  pathname,
  collapsed,
  onMenuClick,
  openKeys,
  onOpenChange,
  primaryMenuWidth,
  secondaryMenuConfig,
}) => {
  const { styles, cx } = useStyles();
  /** 当前是否展示右侧二级菜单列。 */
  const hasVisibleSecondaryMenu = Boolean(secondaryMenuConfig && !collapsed);
  /** 当前实际可见的二级菜单配置。 */
  const visibleSecondaryMenuConfig = hasVisibleSecondaryMenu ? secondaryMenuConfig : null;
  const visibleMenuRoutes = useMemo(() => buildVisibleMenuRoutes(menuRoutes), [menuRoutes]);
  const selectedKeys = useMemo(
    () => getMenuMatchState(pathname, menuRoutes).selectedKeys,
    [menuRoutes, pathname],
  );
  const selectedKey = selectedKeys[0] ?? null;
  const openKeySet = useMemo(() => new Set(openKeys), [openKeys]);

  /**
   * 切换某个分组的展开状态。
   * @param path 当前分组路径。
   * @returns 无返回值。
   */
  function toggleGroup(path: string): void {
    const nextOpenKeys = openKeySet.has(path)
      ? openKeys.filter((key) => key !== path)
      : [...openKeys, path];

    onOpenChange(nextOpenKeys);
  }

  /**
   * 递归渲染菜单树。
   * @param routes 当前层级的菜单节点列表。
   * @param depth 当前层级深度。
   * @returns 菜单节点集合。
   */
  function renderMenuNodes(routes: VisibleMenuRoute[], depth = 0): React.ReactNode {
    return routes.map((route) => {
      const hasChildren = Boolean(route.children?.length);
      const isOpen = openKeySet.has(route.path);
      const isSelected = selectedKey === route.path;
      const isNestedItem = depth > 0;
      const buttonTitle = collapsed ? route.name : undefined;

      if (hasChildren) {
        return (
          <div key={route.path} className={styles.menuGroup}>
            <button
              type="button"
              title={buttonTitle}
              aria-expanded={isOpen}
              aria-label={route.name}
              className={cx(styles.groupButton, collapsed ? styles.groupButtonCollapsed : '')}
              onClick={() => {
                toggleGroup(route.path);
              }}
            >
              {collapsed && route.icon ? (
                <span className={styles.groupButtonIcon} data-side-menu-icon="true">
                  <AppIcon name={route.icon} size={16} />
                </span>
              ) : null}
              {!collapsed ? <span className={styles.groupLabelText}>{route.name}</span> : null}
              {!collapsed ? (
                <span
                  aria-hidden="true"
                  className={cx(
                    styles.expandArrow,
                    isOpen ? styles.expandArrowOpen : styles.expandArrowClosed,
                  )}
                />
              ) : null}
            </button>
            <div
              className={cx(
                styles.submenuList,
                isOpen ? styles.submenuListOpen : styles.submenuListClosed,
                collapsed ? styles.submenuListCollapsed : '',
              )}
            >
              {renderMenuNodes(route.children ?? [], depth + 1)}
            </div>
          </div>
        );
      }

      return (
        <button
          key={route.path}
          type="button"
          title={buttonTitle}
          data-current={isSelected ? 'true' : undefined}
          aria-current={isSelected ? 'page' : undefined}
          aria-label={route.name}
          className={cx(
            styles.menuItemButton,
            isSelected ? styles.menuItemButtonSelected : '',
            collapsed ? styles.menuItemButtonCollapsed : '',
            isNestedItem ? styles.menuItemButtonNested : '',
          )}
          onClick={() => {
            onMenuClick(route.path);
          }}
        >
          {route.icon ? (
            <span
              className={cx(styles.menuItemIcon, isNestedItem ? styles.menuItemIconNested : '')}
              data-side-menu-icon="true"
            >
              <AppIcon name={route.icon} size={isNestedItem ? 15 : 16} />
            </span>
          ) : null}
          {!collapsed ? <span className={styles.menuItemLabel}>{route.name}</span> : null}
        </button>
      );
    });
  }

  return (
    <div className={styles.sideMenuLayout}>
      <div className={styles.primaryMenuColumn} style={{ width: primaryMenuWidth }}>
        <div className={styles.sideMenuWrapper}>
          <nav
            className={cx(
              styles.menu,
              collapsed ? styles.menuCollapsed : '',
              !hasVisibleSecondaryMenu ? styles.menuStandalone : '',
            )}
            aria-label="主导航菜单"
          >
            {renderMenuNodes(visibleMenuRoutes)}
          </nav>
        </div>
      </div>

      {visibleSecondaryMenuConfig ? (
        <>
          <div className={styles.secondaryMenuDivider} />
          <div
            className={styles.secondaryMenuColumn}
            style={{ width: visibleSecondaryMenuConfig.width }}
          >
            <SecondaryMenu
              activeKey={visibleSecondaryMenuConfig.activeKey}
              items={visibleSecondaryMenuConfig.items}
              onChange={visibleSecondaryMenuConfig.onChange}
            />
          </div>
        </>
      ) : null}
    </div>
  );
};

export default SideMenu;
