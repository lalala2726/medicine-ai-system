/**
 * 商品卡片可展示的药品分类缩写类型。
 */
export type ProductDrugType = 'OTC绿' | 'OTC红' | 'Rx'

/**
 * 药品分类元数据。
 */
export interface DrugCategoryMeta {
  /** 分类编码。 */
  code: number
  /** 卡片缩写。 */
  shortLabel: ProductDrugType
  /** 分类名称。 */
  name: string
  /** 分类说明。 */
  description: string
}

/**
 * 药品分类元数据列表。
 */
export const DRUG_CATEGORY_META_LIST: DrugCategoryMeta[] = [
  {
    code: 0,
    shortLabel: 'OTC绿',
    name: '乙类非处方药',
    description: '安全性高，用户可直接购买。'
  },
  {
    code: 1,
    shortLabel: 'Rx',
    name: '处方药',
    description: '必须上传医生处方，经药师审核后发货。'
  },
  {
    code: 2,
    shortLabel: 'OTC红',
    name: '甲类非处方药',
    description: '需在药师指导下购买。'
  }
]

/**
 * 根据分类编码获取元数据。
 *
 * @param code 分类编码
 * @returns 分类元数据，未命中时返回 undefined
 */
export const getDrugCategoryMeta = (code?: number): DrugCategoryMeta | undefined => {
  return DRUG_CATEGORY_META_LIST.find(item => item.code === code)
}

/**
 * 根据分类编码转换商品卡片缩写。
 *
 * @param code 分类编码
 * @returns 商品卡片缩写，未命中时返回 undefined
 */
export const toProductDrugType = (code?: number): ProductDrugType | undefined => {
  return getDrugCategoryMeta(code)?.shortLabel
}
