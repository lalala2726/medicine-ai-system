package com.zhangyichuang.medicine.client.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.model.entity.MallProductCategoryRel;

import java.util.List;
import java.util.Map;

/**
 * 商品分类关联服务（客户端）。
 *
 * @author Chuang
 */
public interface MallProductCategoryRelService extends IService<MallProductCategoryRel> {

    /**
     * 按商品ID列表查询分类关联。
     *
     * @param productIds 商品ID列表
     * @return 分类关联列表
     */
    List<MallProductCategoryRel> listByProductIds(List<Long> productIds);

    /**
     * 按商品ID列表查询分类ID映射。
     *
     * @param productIds 商品ID列表
     * @return 商品ID到分类ID列表的映射
     */
    Map<Long, List<Long>> listCategoryIdsMapByProductIds(List<Long> productIds);
}
