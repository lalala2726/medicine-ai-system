package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.common.core.base.Option;
import com.zhangyichuang.medicine.model.entity.MallCategory;
import com.zhangyichuang.medicine.model.request.MallCategoryAddRequest;
import com.zhangyichuang.medicine.model.request.MallCategoryUpdateRequest;
import com.zhangyichuang.medicine.model.vo.MallCategoryTree;

import java.util.List;

/**
 * 商城商品分类服务接口
 * <p>
 * 提供商城商品分类的业务逻辑处理，包括分类的增删改查、
 * 分类树构建、分类选项获取等功能。
 *
 * @author Chuang
 * created on 2025/10/4
 */
public interface MallCategoryService extends IService<MallCategory> {


    /**
     * 商品分类树
     *
     * @return 商品分类树
     */
    List<MallCategoryTree> categoryTree();

    /**
     * 获取商品下拉选项
     *
     * @return 商品分类选项列表
     */
    List<Option<Long>> option();

    /**
     * 根据ID获取商城商品分类
     *
     * @param id 分类ID
     * @return 商城商品分类信息
     */
    MallCategory getCategoryById(Long id);

    /**
     * 添加商城商品分类
     *
     * @param request 添加参数
     * @return 添加结果
     */
    boolean addCategory(MallCategoryAddRequest request);

    /**
     * 修改商城商品分类
     *
     * @param request 修改参数
     * @return 修改结果
     */
    boolean updateCategory(MallCategoryUpdateRequest request);

    /**
     * 删除商城商品分类
     *
     * @param ids 分类ID列表
     * @return 删除结果
     */
    boolean deleteCategory(List<Long> ids);

    /**
     * 判断商品分类是否存在
     *
     * @param categoryId 商品分类ID
     * @return 是否存在
     */
    boolean isProductCategoryExist(Long categoryId);

}
