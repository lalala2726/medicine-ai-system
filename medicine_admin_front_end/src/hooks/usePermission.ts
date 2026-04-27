import { useMemo } from 'react';
import { useUserStore } from '@/store';
import {
  canAccessByPermissions,
  hasAllPermissions as hasAllPermissionsForUser,
  hasAnyPermission as hasAnyPermissionForUser,
  hasPermission as hasPermissionForUser,
  isSuperAdmin as isSuperAdminUser,
  type PermissionMatchMode,
} from '@/utils/permission';

/**
 * 当前用户权限 Hook。
 * @returns 当前用户权限判断方法。
 */
export function usePermission() {
  const user = useUserStore((state) => state.user);

  return useMemo(
    () => ({
      user,
      isSuperAdmin: () => isSuperAdminUser(user),
      hasPermission: (permissionCode?: string) => hasPermissionForUser(user, permissionCode),
      hasAnyPermission: (permissionCodes?: string | string[]) =>
        hasAnyPermissionForUser(user, permissionCodes),
      hasAllPermissions: (permissionCodes?: string | string[]) =>
        hasAllPermissionsForUser(user, permissionCodes),
      canAccess: (permissionCodes?: string | string[], mode: PermissionMatchMode = 'any') =>
        canAccessByPermissions(user, permissionCodes, mode),
    }),
    [user],
  );
}
