import { requestClient } from '@/utils/request';

export namespace ImageParseTypes {
  export interface DrugInfoDto {
    /** 药品通用名 */
    commonName?: string;
    /** 成分 */
    composition?: string;
    /** 性状 */
    characteristics?: string;
    /** 包装规格 */
    packaging?: string;
    /** 有效期 */
    validityPeriod?: string;
    /** 贮藏条件 */
    storageConditions?: string;
    /** 生产单位 */
    productionUnit?: string;
    /** 批准文号 */
    approvalNumber?: string;
    /** 执行标准 */
    executiveStandard?: string;
    /** 产地类型 */
    originType?: string;
    /** 是否外用药 */
    isOutpatientMedicine?: boolean;
    /** 温馨提示 */
    warmTips?: string;
    /** 品牌名称 */
    brand?: string;
    /** 药品分类编码（0-OTC绿，1-Rx，2-OTC红） */
    drugCategory?: number;
    /** 功能主治 */
    efficacy?: string;
    /** 用法用量 */
    usageMethod?: string;
    /** 不良反应 */
    adverseReactions?: string;
    /** 注意事项 */
    precautions?: string;
    /** 禁忌 */
    taboo?: string;
    /** 药品说明书全文 */
    instruction?: string;
  }

  export interface ParseDrugParams {
    /** 图像 URL 列表 */
    image_urls: string[];
  }

  /** 标签项 */
  export interface TagGroupItem {
    /** 标签 ID */
    id: string;
    /** 标签名称 */
    name: string;
  }

  /** 标签分组 */
  export interface TagGroup {
    /** 标签类型名称 */
    typeName: string;
    /** 当前类型下的标签列表 */
    tags: TagGroupItem[];
  }

  /** 商品标签图片识别请求参数 */
  export interface ParseProductTagParams {
    /** 图像 URL 列表 */
    image_urls: string[];
    /** 按标签类型分组的所有可用标签列表 */
    tag_groups: TagGroup[];
  }

  /** 商品标签图片识别结果 */
  export interface ProductTagParseResult {
    /** 匹配的标签 ID 列表 */
    matchedTagIds: string[];
    /** 整体匹配置信度 */
    confidence?: string;
    /** 匹配理由简述 */
    reasoning?: string;
  }
}

/**
 * 解析药品图片信息
 * @param data 图片信息
 */
export async function parseDrug(data: ImageParseTypes.ParseDrugParams) {
  return requestClient.post<ImageParseTypes.DrugInfoDto>(`/ai_api/image_parse/drug`, data, {
    timeout: 120000,
  });
}

/**
 * 识别商品标签（通过药品图片匹配标签）
 *
 * @param data 图片URL与标签分组数据
 * @returns 匹配的标签ID列表与置信度
 */
export async function parseProductTags(data: ImageParseTypes.ParseProductTagParams) {
  return requestClient.post<ImageParseTypes.ProductTagParseResult>(
    `/ai_api/image_parse/product_tag`,
    data,
    {
      timeout: 120000,
    },
  );
}
