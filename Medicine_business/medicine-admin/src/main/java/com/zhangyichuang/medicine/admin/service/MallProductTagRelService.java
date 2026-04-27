package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.model.entity.MallProductTagRel;

import java.util.List;

/**
 * 商品标签关联服务。
 *
 * @author Chuang
 */
public interface MallProductTagRelService extends IService<MallProductTagRel> {

    /**
     * 替换商品绑定的标签集合。
     *
     * @param productId 商品ID
     * @param tagIds    标签ID集合
     */
    void replaceProductTags(Long productId, List<Long> tagIds);

    /**
     * 按商品ID列表删除关联。
     *
     * @param productIds 商品ID列表
     */
    void removeByProductIds(List<Long> productIds);

    /**
     * 按商品ID列表查询标签关联。
     *
     * @param productIds 商品ID列表
     * @return 标签关联列表
     */
    List<MallProductTagRel> listByProductIds(List<Long> productIds);

    /**
     * 按标签ID列表查询已绑定的商品ID列表。
     *
     * @param tagIds 标签ID列表
     * @return 商品ID列表
     */
    List<Long> listProductIdsByTagIds(List<Long> tagIds);

    /**
     * 判断标签是否已绑定商品。
     *
     * @param tagId 标签ID
     * @return true-已绑定，false-未绑定
     */
    boolean existsByTagId(Long tagId);
}
