import React from 'react';
import ForbiddenPage from '@/pages/403';
import { usePermission } from '@/hooks/usePermission';
import type { PermissionMatchMode } from '@/utils/permission';

export interface PermissionRouteProps {
  /** 需要的权限编码。 */
  access?: string | string[];
  /** 多权限编码的匹配模式。 */
  accessMode?: PermissionMatchMode;
  /** 路由内容。 */
  children: React.ReactNode;
}

/**
 * 路由权限守卫。
 * @param props 路由权限配置。
 * @returns 有权限时返回页面内容，无权限时返回 403。
 */
const PermissionRoute: React.FC<PermissionRouteProps> = ({
  access,
  accessMode = 'any',
  children,
}) => {
  const { canAccess } = usePermission();
  return canAccess(access, accessMode) ? <>{children}</> : <ForbiddenPage />;
};

export default PermissionRoute;
