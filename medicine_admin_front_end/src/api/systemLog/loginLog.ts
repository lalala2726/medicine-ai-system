import { requestClient } from '@/utils/request';
import type { PageRequest, TableDataResult } from '@/types';

/**
 * 登录日志列表查询参数。
 */
export interface LoginLogQueryRequest extends PageRequest {
  /** 登录账号 */
  username?: string;
  /** 登录来源：admin/client */
  loginSource?: string;
  /** 登录状态：1成功 0失败 */
  loginStatus?: number;
  /** 登录方式 */
  loginType?: string;
  /** IP地址 */
  ipAddress?: string;
  /** 开始时间 */
  startTime?: string;
  /** 结束时间 */
  endTime?: string;
}

/**
 * 登录日志列表。
 */
export interface LoginLogListVo {
  /** 日志ID */
  id?: string;
  /** 用户ID */
  userId?: string;
  /** 登录账号 */
  username?: string;
  /** 登录来源：admin/client */
  loginSource?: string;
  /** 登录状态：1成功 0失败 */
  loginStatus?: number;
  /** 登录方式 */
  loginType?: string;
  /** IP地址 */
  ipAddress?: string;
  /** 登录时间 */
  loginTime?: string;
}

/**
 * 登录日志详情。
 */
export interface LoginLogVo {
  /** 日志ID */
  id?: string;
  /** 用户ID */
  userId?: string;
  /** 登录账号 */
  username?: string;
  /** 登录来源：admin/client */
  loginSource?: string;
  /** 登录状态：1成功 0失败 */
  loginStatus?: number;
  /** 失败原因 */
  failReason?: string;
  /** 登录方式 */
  loginType?: string;
  /** IP地址 */
  ipAddress?: string;
  /** User-Agent */
  userAgent?: string;
  /** 设备类型 */
  deviceType?: string;
  /** 操作系统 */
  os?: string;
  /** 浏览器 */
  browser?: string;
  /** 登录时间 */
  loginTime?: string;
}

/**
 * 分页查询登录日志列表
 */
export async function listLoginLogs(params?: LoginLogQueryRequest) {
  return requestClient.get<TableDataResult<LoginLogListVo>>('/system/login_log/list', {
    params,
  });
}

/**
 * 获取登录日志详情
 */
export async function getLoginLogDetail(id: string | number) {
  return requestClient.get<LoginLogVo>(`/system/login_log/${id}`);
}

/**
 * 清空登录日志
 */
export async function clearLoginLogs() {
  return requestClient.delete('/system/login_log');
}
