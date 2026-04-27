import { requestClient } from '@/utils/request';

/** 权限树 */
export interface PermissionTreeVo {
  /** 权限ID */
  id?: string;
  /** 父权限ID */
  parentId?: string;
  /** 权限标识 */
  permissionCode?: string;
  /** 权限名称 */
  permissionName?: string;
  /** 排序 */
  sortOrder?: number;
  /** 状态（0启用 1禁用） */
  status?: number;
  /** 备注 */
  remark?: string;
  /** 子节点 */
  children?: PermissionTreeVo[];
}

/** 权限详情 */
export interface PermissionVo {
  /** 权限ID */
  id?: string;
  /** 父权限ID */
  parentId?: string;
  /** 权限标识 */
  permissionCode?: string;
  /** 权限名称 */
  permissionName?: string;
  /** 排序 */
  sortOrder?: number;
  /** 状态（0启用 1禁用） */
  status?: number;
  /** 备注 */
  remark?: string;
  /** 创建时间 */
  createTime?: string;
}

/** 权限添加参数 */
export interface PermissionAddRequest {
  /** 父权限ID */
  parentId?: string;
  /** 权限标识 */
  permissionCode: string;
  /** 权限名称 */
  permissionName: string;
  /** 排序 */
  sortOrder?: number;
  /** 状态（0启用 1禁用） */
  status?: number;
  /** 备注 */
  remark?: string;
}

/** 权限修改参数 */
export interface PermissionUpdateRequest extends PermissionAddRequest {
  /** 权限ID */
  id?: string;
}

/**
 * 查询权限树
 * @returns 权限树列表
 */
export async function listPermission() {
  return requestClient.get<PermissionTreeVo[]>('/system/permission/list');
}

/**
 * 根据权限ID获取权限详情
 * @param id 权限ID
 */
export async function getPermission(id: string | number) {
  return requestClient.get<PermissionVo>(`/system/permission/${id}`);
}

/**
 * 添加新权限
 * @param data 权限添加参数
 */
export async function addPermission(data: PermissionAddRequest) {
  return requestClient.post<void>('/system/permission', data);
}

/**
 * 修改权限信息
 * @param data 权限修改参数
 */
export async function updatePermission(data: PermissionUpdateRequest) {
  return requestClient.put<void>('/system/permission', data);
}

/**
 * 批量删除权限
 * @param ids 权限ID列表，逗号分隔或数组
 */
export async function deletePermission(ids: string | number | (string | number)[]) {
  const idStr = Array.isArray(ids) ? ids.join(',') : ids;
  return requestClient.delete<void>(`/system/permission/${idStr}`);
}
