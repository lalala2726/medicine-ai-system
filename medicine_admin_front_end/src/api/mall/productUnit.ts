import { requestClient } from '@/utils/request';

export namespace MallProductUnitTypes {
  /**
   * 商品单位视图对象。
   */
  export interface MallProductUnitVo {
    /** 单位 ID */
    id?: string;
    /** 单位名称 */
    name?: string;
    /** 排序值 */
    sort?: number;
  }

  /**
   * 商品单位新增请求。
   */
  export interface MallProductUnitAddRequest {
    /** 单位名称 */
    name: string;
  }
}

/**
 * 查询商品单位下拉选项。
 *
 * @returns 商品单位下拉选项列表
 */
export async function listProductUnitOptions() {
  return requestClient.get<MallProductUnitTypes.MallProductUnitVo[]>('/mall/product/unit/option');
}

/**
 * 新增商品单位。
 *
 * @param data 商品单位新增请求
 * @returns 新增后的商品单位
 */
export async function addProductUnit(data: MallProductUnitTypes.MallProductUnitAddRequest) {
  return requestClient.post<MallProductUnitTypes.MallProductUnitVo>('/mall/product/unit', data);
}

/**
 * 删除商品单位。
 *
 * @param id 商品单位 ID
 * @returns 删除结果
 */
export async function deleteProductUnit(id: string) {
  return requestClient.delete<void>(`/mall/product/unit/${id}`);
}
