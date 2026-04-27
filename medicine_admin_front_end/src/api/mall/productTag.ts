import type { PageRequest, TableDataResult } from '@/types';
import { requestClient } from '@/utils/request';

export namespace MallProductTagTypes {
  /**
   * 商品标签简要视图对象。
   */
  export interface MallProductTagVo {
    /** 标签 ID */
    id?: string;
    /** 标签名称 */
    name?: string;
    /** 标签类型 ID */
    typeId?: string;
    /** 标签类型编码 */
    typeCode?: string;
    /** 标签类型名称 */
    typeName?: string;
  }

  /**
   * 商品标签管理视图对象。
   */
  export interface MallProductTagAdminVo extends MallProductTagVo {
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
   * 商品标签列表查询请求。
   */
  export interface MallProductTagListQueryRequest extends PageRequest {
    /** 标签名称 */
    name?: string;
    /** 标签类型 ID */
    typeId?: string;
    /** 状态 */
    status?: number;
  }

  /**
   * 商品标签新增请求。
   */
  export interface MallProductTagAddRequest {
    /** 标签名称 */
    name: string;
    /** 标签类型 ID */
    typeId: string;
    /** 排序值 */
    sort?: number;
    /** 状态 */
    status: number;
  }

  /**
   * 商品标签修改请求。
   */
  export interface MallProductTagUpdateRequest {
    /** 标签 ID */
    id: string;
    /** 标签名称 */
    name: string;
    /** 标签类型 ID */
    typeId: string;
    /** 排序值 */
    sort?: number;
  }

  /**
   * 商品标签状态修改请求。
   */
  export interface MallProductTagStatusUpdateRequest {
    /** 标签 ID */
    id: string;
    /** 状态 */
    status: number;
  }
}

/**
 * 查询商品标签分页列表。
 *
 * @param params 查询参数
 * @returns 标签分页数据
 */
export async function listProductTags(params: MallProductTagTypes.MallProductTagListQueryRequest) {
  return requestClient.get<TableDataResult<MallProductTagTypes.MallProductTagAdminVo>>(
    '/mall/product/tag/list',
    {
      params,
    },
  );
}

/**
 * 查询商品标签下拉列表。
 *
 * @param typeCode 标签类型编码
 * @returns 标签下拉数据
 */
export async function listProductTagOptions(typeCode?: string) {
  return requestClient.get<MallProductTagTypes.MallProductTagVo[]>('/mall/product/tag/option', {
    params: {
      typeCode,
    },
  });
}

/**
 * 根据 ID 查询商品标签详情。
 *
 * @param id 标签 ID
 * @returns 标签详情
 */
export async function getProductTagById(id: string) {
  return requestClient.get<MallProductTagTypes.MallProductTagAdminVo>(`/mall/product/tag/${id}`);
}

/**
 * 新增商品标签。
 *
 * @param data 标签数据
 * @returns 新增结果
 */
export async function addProductTag(data: MallProductTagTypes.MallProductTagAddRequest) {
  return requestClient.post<void>('/mall/product/tag', data);
}

/**
 * 修改商品标签。
 *
 * @param data 标签数据
 * @returns 修改结果
 */
export async function updateProductTag(data: MallProductTagTypes.MallProductTagUpdateRequest) {
  return requestClient.put<void>('/mall/product/tag', data);
}

/**
 * 修改商品标签状态。
 *
 * @param data 状态数据
 * @returns 修改结果
 */
export async function updateProductTagStatus(
  data: MallProductTagTypes.MallProductTagStatusUpdateRequest,
) {
  return requestClient.put<void>('/mall/product/tag/status', data);
}

/**
 * 删除商品标签。
 *
 * @param id 标签 ID
 * @returns 删除结果
 */
export async function deleteProductTag(id: string) {
  return requestClient.delete<void>(`/mall/product/tag/${id}`);
}
