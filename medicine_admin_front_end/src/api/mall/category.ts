import { requestClient } from '@/utils/request';

export namespace MallCategoryTypes {
  export interface MallCategoryVo {
    /** 分类ID */
    id?: number;
    /** 分类名称 */
    name?: string;
    /** 父分类ID */
    parentId?: number | string;
    /** 分类描述 */
    description?: string;
    /** 排序值 */
    sort?: number;
    /** 封面 */
    cover?: string;
    /** 创建时间 */
    createTime?: string;
    /** 更新时间 */
    updateTime?: string;
    /** 创建人 */
    createBy?: string;
    /** 更新人 */
    updateBy?: string;
  }

  export interface MallCategoryTree {
    /** 分类ID */
    id?: number | string;
    /** 分类名称 */
    name?: string;
    /** 封面 */
    cover?: string;
    /** 父分类ID，0表示顶级分类 */
    parentId?: number | string;
    /** 分类描述 */
    description?: string;
    /** 子分类列表 */
    children?: MallCategoryTree[];
  }

  export interface MallCategoryAddRequest {
    /** 分类名称 */
    name: string;
    /** 父分类ID，0表示顶级分类 */
    parentId?: number | string;
    /** 封面 */
    cover: string;
    /** 分类描述 */
    description?: string;
    /** 排序值，越小越靠前 */
    sort?: number;
  }

  export interface MallCategoryUpdateRequest {
    /** 分类ID */
    id: number;
    /** 分类名称 */
    name: string;
    /** 父分类ID，0表示顶级分类 */
    parentId?: number | string;
    /** 封面 */
    cover?: string;
    /** 分类描述 */
    description?: string;
    /** 排序值，越小越靠前 */
    sort?: number;
  }
}

/**
 * 商品分类下拉菜单
 */
export async function option() {
  return requestClient.get('/mall/category/option');
}

/**
 * 获取商品分类树
 */
export async function tree() {
  return requestClient.get('/mall/category/tree');
}

/**
 * 根据ID获取商品分类详情
 * @param id 分类ID
 */
export async function getCategoryById(id: string) {
  return requestClient.get(`/mall/category/${id}`);
}

/**
 * 添加商品分类
 * @param data 分类数据
 */
export async function addCategory(data: MallCategoryTypes.MallCategoryAddRequest) {
  return requestClient.post('/mall/category', data);
}

/**
 * 修改商品分类
 * @param data 分类数据
 */
export async function updateCategory(data: MallCategoryTypes.MallCategoryUpdateRequest) {
  return requestClient.put('/mall/category', data);
}

/**
 * 删除商品分类
 * @param ids 分类ID列表
 */
export async function deleteCategory(ids: Array<string>) {
  return requestClient.delete(`/mall/category/${ids.join(',')}`);
}
