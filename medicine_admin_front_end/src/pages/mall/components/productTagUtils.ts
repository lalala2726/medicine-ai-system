import { listProductTagOptions, type MallProductTagTypes } from '@/api/mall/productTag';
import { listProductTagTypeOptions, type MallProductTagTypeTypes } from '@/api/mall/productTagType';

/**
 * 商品标签基础结构。
 */
export interface ProductTagLike {
  /** 标签 ID */
  id?: string | number;
  /** 标签名称 */
  name?: string;
  /** 标签类型 ID */
  typeId?: string | number;
  /** 标签类型编码 */
  typeCode?: string;
  /** 标签类型名称 */
  typeName?: string;
}

/**
 * 商品标签类型基础结构。
 */
export interface ProductTagTypeLike {
  /** 标签类型 ID */
  id?: string | number;
  /** 标签类型编码 */
  code?: string;
  /** 标签类型名称 */
  name?: string;
}

/**
 * 商品标签选择分组。
 */
export interface ProductTagSelectGroup {
  /** 标签类型 ID */
  typeId: string;
  /** 标签类型编码 */
  typeCode?: string;
  /** 标签类型名称 */
  typeName: string;
  /** 当前类型下的标签选项 */
  options: Array<{
    /** 显示名称 */
    label: string;
    /** 标签值 */
    value: string;
  }>;
}

/**
 * 商品标签展示分组。
 */
export interface GroupedProductTags {
  /** 标签类型 ID */
  typeId: string;
  /** 标签类型编码 */
  typeCode?: string;
  /** 标签类型名称 */
  typeName: string;
  /** 当前类型下的标签集合 */
  tags: Array<{
    /** 标签 ID */
    id: string;
    /** 标签名称 */
    name: string;
    /** 标签类型 ID */
    typeId: string;
    /** 标签类型编码 */
    typeCode?: string;
    /** 标签类型名称 */
    typeName: string;
  }>;
}

/**
 * 将标签 ID 统一转换为字符串。
 *
 * @param id 原始标签 ID
 * @returns 字符串类型的标签 ID
 */
export function normalizeTagId(id?: string | number): string {
  return id === undefined || id === null ? '' : String(id);
}

/**
 * 提取商品详情中的标签 ID 列表。
 *
 * @param tags 商品标签列表
 * @returns 扁平标签 ID 列表
 */
export function extractProductTagIds(tags?: ProductTagLike[]): string[] {
  return (tags || []).map((tag) => normalizeTagId(tag.id)).filter((id) => id.length > 0);
}

/**
 * 构建商品标签多选分组数据。
 *
 * @param tagTypes 标签类型列表
 * @param tags 标签列表
 * @returns 按类型分组后的选择数据
 */
export function buildProductTagSelectGroups(
  tagTypes: ProductTagTypeLike[],
  tags: ProductTagLike[],
): ProductTagSelectGroup[] {
  const groupMap = new Map<string, ProductTagSelectGroup>();
  const groupOrder: string[] = [];

  tagTypes.forEach((tagType) => {
    const typeId = normalizeTagId(tagType.id);
    if (!typeId) {
      return;
    }
    groupMap.set(typeId, {
      typeId,
      typeCode: tagType.code,
      typeName: tagType.name || tagType.code || `类型 ${typeId}`,
      options: [],
    });
    groupOrder.push(typeId);
  });

  tags.forEach((tag) => {
    const typeId = normalizeTagId(tag.typeId);
    const tagId = normalizeTagId(tag.id);
    if (!typeId || !tagId || !tag.name) {
      return;
    }

    if (!groupMap.has(typeId)) {
      groupMap.set(typeId, {
        typeId,
        typeCode: tag.typeCode,
        typeName: tag.typeName || tag.typeCode || `类型 ${typeId}`,
        options: [],
      });
      groupOrder.push(typeId);
    }

    groupMap.get(typeId)?.options.push({
      label: tag.name,
      value: tagId,
    });
  });

  return groupOrder.map((typeId) => ({
    ...groupMap.get(typeId)!,
    options: groupMap.get(typeId)!.options,
  }));
}

/**
 * 将标签列表按类型分组，供详情展示使用。
 *
 * @param tags 商品标签列表
 * @returns 按类型分组后的标签数据
 */
export function groupProductTagsByType(tags?: ProductTagLike[]): GroupedProductTags[] {
  const groupMap = new Map<string, GroupedProductTags>();
  const groupOrder: string[] = [];

  (tags || []).forEach((tag) => {
    const typeId = normalizeTagId(tag.typeId);
    const tagId = normalizeTagId(tag.id);
    if (!typeId || !tagId || !tag.name) {
      return;
    }

    if (!groupMap.has(typeId)) {
      groupMap.set(typeId, {
        typeId,
        typeCode: tag.typeCode,
        typeName: tag.typeName || tag.typeCode || `类型 ${typeId}`,
        tags: [],
      });
      groupOrder.push(typeId);
    }

    groupMap.get(typeId)?.tags.push({
      id: tagId,
      name: tag.name,
      typeId,
      typeCode: tag.typeCode,
      typeName: tag.typeName || tag.typeCode || `类型 ${typeId}`,
    });
  });

  return groupOrder.map((typeId) => groupMap.get(typeId)!);
}

/**
 * 加载启用的标签类型及其标签选项。
 *
 * @returns 按类型分组后的标签选择数据
 */
export async function loadEnabledProductTagSelectGroups(): Promise<ProductTagSelectGroup[]> {
  const tagTypes = await listProductTagTypeOptions();
  if (!tagTypes?.length) {
    return [];
  }

  const tagGroupList = await Promise.all(
    tagTypes.map(async (tagType) => {
      const tagList = await listProductTagOptions(tagType.code);
      return {
        tagType,
        tagList,
      };
    }),
  );

  const allTags = tagGroupList.flatMap((item) => item.tagList || []);

  return buildProductTagSelectGroups(
    tagTypes as ProductTagTypeLike[],
    allTags as MallProductTagTypes.MallProductTagVo[],
  );
}
