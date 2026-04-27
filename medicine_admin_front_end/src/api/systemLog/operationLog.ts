import { requestClient } from '@/utils/request';
import type { PageRequest, TableDataResult } from '@/types';

/**
 * 操作日志列表查询参数。
 */
export interface OperationLogQueryRequest extends PageRequest {
  /** 业务模块 */
  module?: string;
  /** 操作说明 */
  action?: string;
  /** 操作人账号 */
  username?: string;
  /** 是否成功：1成功 0失败 */
  success?: number;
  /** 开始时间 */
  startTime?: string;
  /** 结束时间 */
  endTime?: string;
}

/**
 * 操作日志列表。
 */
export interface OperationLogListVo {
  /** 日志ID */
  id?: string;
  /** 业务模块 */
  module?: string;
  /** 操作说明 */
  action?: string;
  /** 操作人账号 */
  username?: string;
  /** 请求IP */
  ip?: string;
  /** 是否成功：1成功 0失败 */
  success?: number;
  /** 耗时(ms) */
  costTime?: string;
  /** 创建时间 */
  createTime?: string;
}

/**
 * 操作日志详情。
 */
export interface OperationLogVo {
  /** 日志ID */
  id?: string;
  /** 业务模块 */
  module?: string;
  /** 操作说明 */
  action?: string;
  /** 请求URI */
  requestUri?: string;
  /** HTTP方法 */
  httpMethod?: string;
  /** 方法名 */
  methodName?: string;
  /** 操作人ID */
  userId?: string;
  /** 操作人账号 */
  username?: string;
  /** 请求IP */
  ip?: string;
  /** User-Agent */
  userAgent?: string;
  /** 请求参数 */
  requestParams?: string;
  /** 返回结果 */
  responseResult?: string;
  /** 耗时(ms) */
  costTime?: string;
  /** 是否成功：1成功 0失败 */
  success?: number;
  /** 错误消息 */
  errorMsg?: string;
  /** 创建时间 */
  createTime?: string;
}

/**
 * 分页查询操作日志列表
 */
export async function listOperationLogs(params?: OperationLogQueryRequest) {
  return requestClient.get<TableDataResult<OperationLogListVo>>('/system/operation_log/list', {
    params,
  });
}

/**
 * 获取操作日志详情
 */
export async function getOperationLogDetail(id: string | number) {
  return requestClient.get<OperationLogVo>(`/system/operation_log/${id}`);
}

/**
 * 清空操作日志
 */
export async function clearOperationLogs() {
  return requestClient.delete('/system/operation_log');
}
