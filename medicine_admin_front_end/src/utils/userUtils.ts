/**
 * 用户相关工具函数
 */

/**
 * 安全解析角色数据
 * @param roles 角色数据，可能是字符串、数组或其他类型
 * @returns 角色数组
 */
export const parseUserRoles = (roles: any): string[] => {
  try {
    if (!roles) return [];

    if (Array.isArray(roles)) {
      return roles;
    }

    if (typeof roles === 'string') {
      // 首先尝试标准 JSON 解析
      try {
        const parsed = JSON.parse(roles);
        return Array.isArray(parsed) ? parsed : [parsed];
      } catch (_parseError) {
        console.warn('角色数据不是有效的 JSON，尝试手动解析:', roles);

        // 处理类似 "[user]" 的格式
        if (roles.startsWith('[') && roles.endsWith(']')) {
          const content = roles.slice(1, -1).trim();
          if (content) {
            // 尝试按逗号分割处理多个角色
            return content
              .split(',')
              .map((role) => role.trim())
              .filter((role) => role);
          }
          return [];
        }

        // 直接返回字符串作为单个角色
        return [roles];
      }
    }

    return Array.isArray(roles) ? roles : [roles];
  } catch (error) {
    console.error('解析角色数据时出错:', error, '原始数据:', roles);
    return [];
  }
};

/**
 * 规范化用户数据
 * @param userData 原始用户数据
 * @returns 规范化的用户数据
 */
export const normalizeUserData = (userData: any) => {
  if (!userData) return undefined;

  // 确保至少有一个有效的显示名称
  // 优先使用 nickname，然后是 username，最后是 name
  const displayName = userData.nickname || userData.username || userData.name || '用户';

  // 处理头像，如果为 null 或空字符串，使用默认头像
  let avatarUrl = userData.avatar;
  if (!avatarUrl || avatarUrl === '') {
    avatarUrl = '/default_avatar.png';
  }

  return {
    ...userData,
    // 确保 name 字段存在，用于显示
    name: displayName,
    // 确保头像存在
    avatar: avatarUrl,
    // 解析角色数据
    roles: parseUserRoles(userData.roles),
    // 规范化权限数据
    permissions: parseUserRoles(userData.permissions),
  };
};
