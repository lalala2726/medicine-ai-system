package com.zhangyichuang.medicine.client.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.model.entity.MallCategory;
import com.zhangyichuang.medicine.model.vo.MallCategoryTree;

import java.util.List;

/**
 * 商城商品分类服务接口（客户端）
 *
 * @author Chuang
 */
public interface MallCategoryService extends IService<MallCategory> {

    /**
     * 获取启用的商品分类树
     *
     * @return 分类树
     */
    List<MallCategoryTree> categoryTree();

    /**
     * 获取指定父分类下的子分类树（仅启用分类）
     *
     * @param parentId 父分类ID
     * @return 子分类树
     */
    List<MallCategoryTree> categoryChildren(Long parentId);

    /**
     * 获取指定父分类下的同级分类（不包含子级）
     *
     * @param parentId 父分类ID
     * @return 同级分类列表
     */
    List<MallCategoryTree> categorySiblings(Long parentId);
}
