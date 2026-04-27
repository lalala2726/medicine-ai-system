/**
 * 药品分类元数据定义。
 */
export interface DrugCategoryMeta {
  /** 分类编码。 */
  code: number;
  /** 卡片与标签缩写。 */
  shortLabel: string;
  /** 分类名称。 */
  name: string;
  /** 分类说明。 */
  description: string;
  /** Ant Design 标签颜色。 */
  tagColor: 'success' | 'warning' | 'error';
}

/**
 * 药品分类元数据列表。
 */
export const DRUG_CATEGORY_META_LIST: DrugCategoryMeta[] = [
  {
    code: 0,
    shortLabel: 'OTC绿',
    name: '乙类非处方药',
    description: '安全性高，用户可直接购买。',
    tagColor: 'success',
  },
  {
    code: 1,
    shortLabel: 'Rx',
    name: '处方药',
    description: '必须上传医生处方，经药师审核后发货。',
    tagColor: 'error',
  },
  {
    code: 2,
    shortLabel: 'OTC红',
    name: '甲类非处方药',
    description: '需在药师指导下购买。',
    tagColor: 'warning',
  },
];

/**
 * 根据分类编码查询元数据。
 *
 * @param code 分类编码
 * @returns 分类元数据，未命中时返回 undefined
 */
export function getDrugCategoryMeta(code?: number): DrugCategoryMeta | undefined {
  return DRUG_CATEGORY_META_LIST.find((item) => item.code === code);
}
