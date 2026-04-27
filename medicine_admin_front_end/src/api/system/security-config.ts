import { requestClient } from '@/utils/request';

export namespace SecurityConfigTypes {
  /** 管理端水印配置。 */
  export interface AdminWatermarkConfig {
    /** 是否启用管理端水印。 */
    enabled?: boolean;
    /** 是否展示用户名。 */
    showUsername?: boolean;
    /** 是否展示用户ID。 */
    showUserId?: boolean;
  }

  /** 单端安全策略配置。 */
  export interface SecurityPolicyConfig {
    /** 连续失败阈值。 */
    maxRetryCount?: number;
    /** 锁定时长（分钟）。 */
    lockMinutes?: number;
  }

  /** 安全配置详情。 */
  export interface SecurityConfigVo {
    /** 管理端安全策略。 */
    admin?: SecurityPolicyConfig;
    /** 客户端安全策略。 */
    client?: SecurityPolicyConfig;
    /** 管理端水印配置。 */
    adminWatermark?: AdminWatermarkConfig;
  }

  /** 安全配置更新请求。 */
  export type SecurityConfigUpdateRequest = SecurityConfigVo;
}

/**
 * 查询安全配置。
 * @returns 安全配置详情。
 */
export async function getSecurityConfig() {
  return requestClient.get<SecurityConfigTypes.SecurityConfigVo>('/system/config/security');
}

/**
 * 查询管理端水印配置。
 * @returns 管理端水印配置。
 */
export async function getAdminWatermarkConfig(options?: { skipErrorHandler?: boolean }) {
  return requestClient.get<SecurityConfigTypes.AdminWatermarkConfig>(
    '/system/config/security/admin-watermark',
    options,
  );
}

/**
 * 更新安全配置。
 * @param data 安全配置更新请求。
 * @returns 更新结果。
 */
export async function updateSecurityConfig(data: SecurityConfigTypes.SecurityConfigUpdateRequest) {
  return requestClient.put<void, SecurityConfigTypes.SecurityConfigUpdateRequest>(
    '/system/config/security',
    data,
  );
}
