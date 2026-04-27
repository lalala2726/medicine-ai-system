import type { PageRequest, TableDataResult } from '@/types';
import { requestClient } from '@/utils/request';

export namespace MallProductTagTypeTypes {
  /**
   * 商品标签类型下拉视图对象。
   */
  export interface MallProductTagTypeVo {
    /** 标签类型 ID */
    id?: string;
    /** 标签类型编码 */
    code?: string;
    /** 标签类型名称 */
    name?: string;
  }

  /**
   * 商品标签类型管理视图对象。
   */
  export interface MallProductTagTypeAdminVo extends MallProductTagTypeVo {
    /** 排序值 */
    sort?: number;
    /** 状态 */
    status?: number;
    /** 创建时间 */
    createTime?: string;
    /** 更新时间 */
    updateTime?: string;
    /** 创建人 */
    createBy?: string;
    /** 更新人 */
    updateBy?: string;
  }

  /**
   * 商品标签类型列表查询请求。
   */
  export interface MallProductTagTypeListQueryRequest extends PageRequest {
    /** 标签类型名称 */
    name?: string;
    /** 状态 */
    status?: number;
  }

  /**
   * 商品标签类型新增请求。
   */
  export interface MallProductTagTypeAddRequest {
    /** 标签类型编码 */
    code: string;
    /** 标签类型名称 */
    name: string;
    /** 排序值 */
    sort?: number;
    /** 状态 */
    status: number;
  }

  /**
   * 商品标签类型修改请求。
   */
  export interface MallProductTagTypeUpdateRequest {
    /** 标签类型 ID */
    id: string;
    /** 标签类型名称 */
    name: string;
    /** 排序值 */
    sort?: number;
  }

  /**
   * 商品标签类型状态修改请求。
   */
  export interface MallProductTagTypeStatusUpdateRequest {
    /** 标签类型 ID */
    id: string;
    /** 状态 */
    status: number;
  }
}

/**
 * 查询商品标签类型分页列表。
 *
 * @param params 查询参数
 * @returns 标签类型分页数据
 */
export async function listProductTagTypes(
  params: MallProductTagTypeTypes.MallProductTagTypeListQueryRequest,
) {
  return requestClient.get<TableDataResult<MallProductTagTypeTypes.MallProductTagTypeAdminVo>>(
    '/mall/product/tag-type/list',
    {
      params,
    },
  );
}

/**
 * 查询启用的商品标签类型下拉列表。
 *
 * @returns 标签类型下拉数据
 */
export async function listProductTagTypeOptions() {
  return requestClient.get<MallProductTagTypeTypes.MallProductTagTypeVo[]>(
    '/mall/product/tag-type/option',
  );
}

/**
 * 根据 ID 查询商品标签类型详情。
 *
 * @param id 标签类型 ID
 * @returns 标签类型详情
 */
export async function getProductTagTypeById(id: string) {
  return requestClient.get<MallProductTagTypeTypes.MallProductTagTypeAdminVo>(
    `/mall/product/tag-type/${id}`,
  );
}

/**
 * 新增商品标签类型。
 *
 * @param data 标签类型数据
 * @returns 新增结果
 */
export async function addProductTagType(
  data: MallProductTagTypeTypes.MallProductTagTypeAddRequest,
) {
  return requestClient.post<void>('/mall/product/tag-type', data);
}

/**
 * 修改商品标签类型。
 *
 * @param data 标签类型数据
 * @returns 修改结果
 */
export async function updateProductTagType(
  data: MallProductTagTypeTypes.MallProductTagTypeUpdateRequest,
) {
  return requestClient.put<void>('/mall/product/tag-type', data);
}

/**
 * 修改商品标签类型状态。
 *
 * @param data 状态数据
 * @returns 修改结果
 */
export async function updateProductTagTypeStatus(
  data: MallProductTagTypeTypes.MallProductTagTypeStatusUpdateRequest,
) {
  return requestClient.put<void>('/mall/product/tag-type/status', data);
}

/**
 * 删除商品标签类型。
 *
 * @param id 标签类型 ID
 * @returns 删除结果
 */
export async function deleteProductTagType(id: string) {
  return requestClient.delete<void>(`/mall/product/tag-type/${id}`);
}
