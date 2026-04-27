import type { MallProductTypes } from '@/api/mall/product';
import { extractProductTagIds } from './productTagUtils';

/**
 * 商品表单回显值。
 */
export interface MallProductFormValues extends Partial<MallProductTypes.MallProductVo> {
  /** 商品分类 ID 列表 */
  categoryIds?: string[];
  /** 标签 ID 列表 */
  tagIds?: string[];
  /** 药品详情 */
  drugDetail?: MallProductTypes.DrugDetailDto;
}

/**
 * 将商品详情转换为表单回显结构。
 *
 * @param detail 商品详情
 * @returns 表单可直接使用的回显值
 */
export function buildMallProductFormValues(
  detail: MallProductTypes.MallProductVo,
): MallProductFormValues {
  return {
    ...detail,
    categoryIds: (detail.categoryIds || []).map(String).filter(Boolean),
    tagIds: extractProductTagIds(detail.tags),
    drugDetail: detail.drugDetail || {},
  };
}
