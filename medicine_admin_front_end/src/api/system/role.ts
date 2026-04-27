import { requestClient } from '@/utils/request';
import type { Option, PageRequest, TableDataResult } from '@/types';

/** 角色详情 */
export interface RoleVo {
  /** 角色ID */
  id?: string;
  /** 角色标识 */
  roleCode?: string;
  /** 角色名称 */
  roleName?: string;
  /** 备注 */
  remark?: string;
  /** 状态：0启用 1禁用 */
  status?: number;
  /** 创建时间 */
  createTime?: string;
}

/** 角色列表 */
export interface RoleListVo {
  /** 角色ID */
  id?: string;
  /** 角色标识 */
  roleCode?: string;
  /** 角色名称 */
  roleName?: string;
  /** 备注 */
  remark?: string;
  /** 状态：0启用 1禁用 */
  status?: number;
  /** 创建时间 */
  createTime?: string;
}

/** 角色添加参数 */
export interface RoleAddRequest {
  /** 角色标识 */
  roleCode: string;
  /** 角色名称 */
  roleName: string;
  /** 备注 */
  remark?: string;
  /** 状态：0启用 1禁用 */
  status?: number;
}

/** 角色修改参数 */
export interface RoleUpdateRequest extends RoleAddRequest {
  /** 角色ID */
  id: string;
}

/** 角色列表查询参数 */
export interface RoleListQuery extends PageRequest {
  /** 角色名称 */
  roleName?: string;
  /** 角色标识 */
  roleCode?: string;
  /** 状态：0启用 1禁用 */
  status?: number;
}

/** 角色权限详情 */
export interface RolePermissionVo {
  /** 权限选项 */
  permissionOption?: Option<string | number>[];
  /** 角色已有权限ID */
  rolePermission?: (string | number)[];
}

/** 角色权限修改参数 */
export interface RolePermissionUpdateRequest {
  /** 角色ID */
  roleId: string;
  /** 权限ID列表 */
  permissionIds?: string[];
}

/**
 * 分页查询角色列表
 * @param params 查询条件
 */
export async function listRole(params: RoleListQuery) {
  return requestClient.get<TableDataResult<RoleListVo>>('/system/role/list', {
    params,
  });
}

/**
 * 根据角色ID获取角色详情
 * @param id 角色ID
 */
export async function getRole(id: string | number) {
  return requestClient.get<RoleVo>(`/system/role/${id}`);
}

/**
 * 添加新角色
 * @param data 角色添加参数
 */
export async function addRole(data: RoleAddRequest) {
  return requestClient.post<void>('/system/role', data);
}

/**
 * 修改角色信息
 * @param data 角色修改参数
 */
export async function updateRole(data: RoleUpdateRequest) {
  return requestClient.put<void>('/system/role', data);
}

/**
 * 批量删除角色
 * @param ids 角色ID列表，逗号分隔或数组
 */
export async function deleteRole(ids: string | number | (string | number)[]) {
  const idStr = Array.isArray(ids) ? ids.join(',') : ids;
  return requestClient.delete<void>(`/system/role/${idStr}`);
}

/**
 * 获取角色权限
 * @param id 角色ID
 */
export async function getRolePermission(id: string | number) {
  return requestClient.get<RolePermissionVo>(`/system/role/permission/${id}`);
}

/**
 * 修改角色权限
 * @param data 角色权限修改参数
 */
export async function updateRolePermission(data: RolePermissionUpdateRequest) {
  return requestClient.put<void>('/system/role/permission', data);
}

/**
 * 获取启用状态的角色选项。
 * @returns 后端角色选项列表，value 为角色ID。
 */
export async function roleOption() {
  return requestClient.get<Option<number>[]>('/system/role/option');
}
