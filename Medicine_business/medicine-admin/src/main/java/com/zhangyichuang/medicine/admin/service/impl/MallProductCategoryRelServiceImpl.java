package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.admin.mapper.MallProductCategoryRelMapper;
import com.zhangyichuang.medicine.admin.service.MallProductCategoryRelService;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.security.base.BaseService;
import com.zhangyichuang.medicine.model.entity.MallProductCategoryRel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 商品分类关联服务实现。
 *
 * @author Chuang
 */
@Service
public class MallProductCategoryRelServiceImpl
        extends ServiceImpl<MallProductCategoryRelMapper, MallProductCategoryRel>
        implements MallProductCategoryRelService, BaseService {

    /**
     * 替换商品分类关联。
     *
     * @param productId   商品ID
     * @param categoryIds 分类ID集合
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceProductCategories(Long productId, List<Long> categoryIds) {
        Assert.isPositive(productId, "商品ID不能为空");
        baseMapper.physicalDeleteByProductId(productId);
        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }
        Date now = new Date();
        List<MallProductCategoryRel> relations = categoryIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(categoryId -> {
                    MallProductCategoryRel relation = new MallProductCategoryRel();
                    relation.setProductId(productId);
                    relation.setCategoryId(categoryId);
                    relation.setCreateTime(now);
                    relation.setCreateBy(getUsername());
                    return relation;
                })
                .toList();
        if (!relations.isEmpty()) {
            saveBatch(relations);
        }
    }

    /**
     * 批量删除商品分类关联。
     *
     * @param productIds 商品ID列表
     */
    @Override
    public void removeByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        baseMapper.physicalDeleteByProductIds(productIds);
    }

    /**
     * 按商品ID列表查询分类关联。
     *
     * @param productIds 商品ID列表
     * @return 分类关联列表
     */
    @Override
    public List<MallProductCategoryRel> listByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return lambdaQuery()
                .in(MallProductCategoryRel::getProductId, productIds)
                .list();
    }

    /**
     * 按商品ID列表查询分类ID映射。
     *
     * @param productIds 商品ID列表
     * @return 商品ID到分类ID列表的映射
     */
    @Override
    public Map<Long, List<Long>> listCategoryIdsMapByProductIds(List<Long> productIds) {
        List<MallProductCategoryRel> relations = listByProductIds(productIds);
        if (relations.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<Long>> result = new LinkedHashMap<>();
        for (MallProductCategoryRel relation : relations) {
            if (relation == null || relation.getProductId() == null || relation.getCategoryId() == null) {
                continue;
            }
            List<Long> categoryIds = result.computeIfAbsent(relation.getProductId(), key -> new ArrayList<>());
            if (!categoryIds.contains(relation.getCategoryId())) {
                categoryIds.add(relation.getCategoryId());
            }
        }
        return result;
    }

    /**
     * 按分类ID列表查询商品ID列表。
     *
     * @param categoryIds 分类ID列表
     * @return 商品ID列表
     */
    @Override
    public List<Long> listProductIdsByCategoryIds(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return List.of();
        }
        return lambdaQuery()
                .in(MallProductCategoryRel::getCategoryId, categoryIds)
                .list()
                .stream()
                .map(MallProductCategoryRel::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
