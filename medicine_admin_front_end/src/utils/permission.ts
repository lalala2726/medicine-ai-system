/** 超级管理员角色编码。 */
export const SUPER_ADMIN_ROLE_CODE = 'super_admin';

/** 权限匹配模式。 */
export type PermissionMatchMode = 'any' | 'all';

/**
 * 将原始集合规范化为字符串数组。
 * @param value 原始集合值。
 * @returns 去空格后的字符串数组。
 */
export function normalizeStringList(value: unknown): string[] {
  if (!value) {
    return [];
  }

  if (Array.isArray(value)) {
    return value
      .map((item) => (typeof item === 'string' ? item.trim() : String(item).trim()))
      .filter(Boolean);
  }

  if (typeof value === 'string') {
    return value
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean);
  }

  return [String(value).trim()].filter(Boolean);
}

/**
 * 判断当前用户是否为超级管理员。
 * @param user 当前用户信息。
 * @returns 是否拥有超级管理员角色。
 */
export function isSuperAdmin(user?: API.CurrentUser | null): boolean {
  return normalizeStringList(user?.roles).some(
    (role) => role.toLowerCase() === SUPER_ADMIN_ROLE_CODE,
  );
}

/**
 * 判断当前用户是否拥有指定权限。
 * @param user 当前用户信息。
 * @param permissionCode 权限编码。
 * @returns 是否拥有权限。
 */
export function hasPermission(
  user: API.CurrentUser | null | undefined,
  permissionCode?: string,
): boolean {
  const normalizedPermissionCode = permissionCode?.trim();
  if (!normalizedPermissionCode) {
    return true;
  }

  if (isSuperAdmin(user)) {
    return true;
  }

  return normalizeStringList(user?.permissions).includes(normalizedPermissionCode);
}

/**
 * 判断当前用户是否拥有任一权限。
 * @param user 当前用户信息。
 * @param permissionCodes 权限编码集合。
 * @returns 是否拥有任一权限。
 */
export function hasAnyPermission(
  user: API.CurrentUser | null | undefined,
  permissionCodes?: string | string[],
): boolean {
  const normalizedPermissionCodes = normalizeStringList(permissionCodes);
  if (normalizedPermissionCodes.length === 0) {
    return true;
  }

  return normalizedPermissionCodes.some((permissionCode) => hasPermission(user, permissionCode));
}

/**
 * 判断当前用户是否拥有全部权限。
 * @param user 当前用户信息。
 * @param permissionCodes 权限编码集合。
 * @returns 是否拥有全部权限。
 */
export function hasAllPermissions(
  user: API.CurrentUser | null | undefined,
  permissionCodes?: string | string[],
): boolean {
  const normalizedPermissionCodes = normalizeStringList(permissionCodes);
  if (normalizedPermissionCodes.length === 0) {
    return true;
  }

  return normalizedPermissionCodes.every((permissionCode) => hasPermission(user, permissionCode));
}

/**
 * 根据匹配模式判断当前用户是否满足权限。
 * @param user 当前用户信息。
 * @param permissionCodes 权限编码集合。
 * @param mode 权限匹配模式。
 * @returns 是否满足权限。
 */
export function canAccessByPermissions(
  user: API.CurrentUser | null | undefined,
  permissionCodes?: string | string[],
  mode: PermissionMatchMode = 'any',
): boolean {
  return mode === 'all'
    ? hasAllPermissions(user, permissionCodes)
    : hasAnyPermission(user, permissionCodes);
}
