package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.model.entity.MallProductCategoryRel;

import java.util.List;
import java.util.Map;

/**
 * 商品分类关联服务。
 *
 * @author Chuang
 */
public interface MallProductCategoryRelService extends IService<MallProductCategoryRel> {

    /**
     * 替换商品绑定的分类集合。
     *
     * @param productId   商品ID
     * @param categoryIds 分类ID集合
     */
    void replaceProductCategories(Long productId, List<Long> categoryIds);

    /**
     * 按商品ID列表删除分类关联。
     *
     * @param productIds 商品ID列表
     */
    void removeByProductIds(List<Long> productIds);

    /**
     * 按商品ID列表查询分类关联。
     *
     * @param productIds 商品ID列表
     * @return 分类关联列表
     */
    List<MallProductCategoryRel> listByProductIds(List<Long> productIds);

    /**
     * 按商品ID列表查询去重后的分类ID映射。
     *
     * @param productIds 商品ID列表
     * @return 商品ID到分类ID列表的映射
     */
    Map<Long, List<Long>> listCategoryIdsMapByProductIds(List<Long> productIds);

    /**
     * 按分类ID列表查询已绑定的商品ID列表。
     *
     * @param categoryIds 分类ID列表
     * @return 商品ID列表
     */
    List<Long> listProductIdsByCategoryIds(List<Long> categoryIds);
}
