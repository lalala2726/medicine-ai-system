import { requestClient } from '@/utils/request';
import type { TableDataResult } from '@/types';

export namespace MallProductTypes {
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

  export interface MallProductListQueryRequest {
    /** 商品ID */
    id?: string;
    /** 商品名称 */
    name?: string;
    /** 商品分类ID，关联 mall_category */
    categoryId?: string;
    /** 状态（1-上架，0-下架） */
    status?: number;
    /** 最低价格 */
    minPrice?: string;
    /** 最高价格 */
    maxPrice?: string;
    /** 商品标签 ID 列表 */
    tagIds?: string[];
  }

  export interface MallProductListVo {
    /** 商品ID */
    id?: string;
    /** 商品名称 */
    name?: string;
    /** 商品分类ID列表 */
    categoryIds?: string[];
    /** 商品分类名称列表 */
    categoryNames?: string[];
    /** 商品单位 */
    unit?: string;
    /** 基础售价 */
    price?: string;
    /** 销量 */
    salesVolume?: string;
    /** 商品库存数量 */
    stock?: number;
    /** 排序值，越小越靠前 */
    sort?: number;
    /** 状态（1-上架，0-下架） */
    status?: number;
    /** 销量 */
    sales: number;
    /** 配送方式 */
    deliveryType?: number;
    /** 是否允许使用优惠券 */
    couponEnabled?: number;
    /** 商品标签列表 */
    tags?: MallProductTagVo[];
    /** 商品标签名称列表 */
    tagNames?: string[];
    /** 创建时间 */
    createTime?: string;
    /** 更新时间 */
    updateTime?: string;
    /** 创建者 */
    createBy?: string;
    /** 更新者 */
    updateBy?: string;
    /** 商品展示图 */
    coverImage?: string;
  }

  export interface MallProductVo {
    /** 商品ID */
    id?: string;
    /** 商品名称 */
    name?: string;
    /** 商品分类ID列表 */
    categoryIds?: string[];
    /** 商品分类名称列表 */
    categoryNames?: string[];
    /** 商品单位 */
    unit?: string;
    /** 基础售价 */
    price?: string;
    /** 销量 */
    salesVolume?: string;
    /** 商品库存数量 */
    stock?: number;
    /** 排序值，越小越靠前 */
    sort?: number;
    /** 状态（1-上架，0-下架） */
    status?: number;
    /** 配送方式 */
    deliveryType?: number;
    /** 是否允许使用优惠券 */
    couponEnabled?: number;
    /** 创建时间 */
    createTime?: string;
    /** 更新时间 */
    updateTime?: string;
    /** 创建者 */
    createBy?: string;
    /** 更新者 */
    updateBy?: string;
    /** 商品图片列表 */
    images?: string[];
    /** 商品标签列表 */
    tags?: MallProductTagVo[];
    /** 药品详情 */
    drugDetail?: DrugDetailDto;
    /** 销量 */
    sales?: number;
  }
  export interface DrugDetailDto {
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
    /** 品牌 */
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
    /** 温馨提示  */
    warmTips?: string;
  }

  export interface MallProductAddRequest {
    /** 商品名称 */
    name: string;
    /** 商品分类ID列表，关联 mall_category */
    categoryIds: string[];
    /** 商品单位（件、盒、瓶等） */
    unit: string;
    /** 基础售价 */
    price: string;
    /** 库存数量 */
    stock?: number;
    /** 排序值，越小越靠前 */
    sort?: number;
    /** 状态（1-上架，0-下架） */
    status?: number;
    /** 配送方式（快递、自提、同城配送等） */
    deliveryType?: string;
    /** 是否允许使用优惠券（1-允许，0-不允许） */
    couponEnabled: number;
    /** 运费模板ID，关联 mall_product_shipping */
    shippingId?: string;
    /** 图片列表 */
    images: string[];
    /** 商品标签 ID 列表 */
    tagIds: string[];
    /** 药品详情 */
    drugDetail?: DrugDetailDto;
  }

  export interface MallProductUpdateRequest {
    /** 商品ID */
    id: string;
    /** 商品名称 */
    name: string;
    /** 商品分类ID列表，关联 mall_category */
    categoryIds: string[];
    /** 商品单位（件、盒、瓶等） */
    unit: string;
    /** 基础售价 */
    price: string;
    /** 库存数量 */
    stock?: number;
    /** 排序值，越小越靠前 */
    sort?: number;
    /** 状态（1-上架，0-下架） */
    status?: number;
    /** 配送方式（快递、自提、同城配送等） */
    deliveryType?: string;
    /** 是否允许使用优惠券（1-允许，0-不允许） */
    couponEnabled: number;
    /** 运费模板ID，关联 mall_product_shipping */
    shippingId?: string;
    /** 图片列表 */
    images: string[];
    /** 商品标签 ID 列表 */
    tagIds: string[];
    /** 药品详情 */
    drugDetail?: DrugDetailDto;
  }
}

/**
 * 获取商城商品列表
 */
export async function listMallProduct(params: MallProductTypes.MallProductListQueryRequest) {
  return requestClient.get<TableDataResult<MallProductTypes.MallProductListVo>>(
    '/mall/product/list',
    {
      params: params,
    },
  );
}

/**
 * 获取商城商品详情
 * @param id 商品ID
 */
export async function getMallProductById(id: string) {
  return requestClient.get<MallProductTypes.MallProductVo>(`/mall/product/${id}`);
}

/**
 * 添加商城商品
 * @param data 商品数据
 */
export async function addMallProduct(data: MallProductTypes.MallProductAddRequest) {
  return requestClient.post<{ id: string }>('/mall/product', data);
}

/**
 * 修改商城商品
 * @param data 商品数据
 */
export async function updateMallProduct(data: MallProductTypes.MallProductUpdateRequest) {
  return requestClient.put<void>('/mall/product', data);
}

/**
 * 删除商城商品
 * @param ids 商品ID列表
 */
export async function deleteMallProduct(ids: string[]) {
  return requestClient.delete<void>(`/mall/product/${ids.join(',')}`);
}
