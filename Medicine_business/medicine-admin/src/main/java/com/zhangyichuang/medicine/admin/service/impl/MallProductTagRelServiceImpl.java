package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.admin.mapper.MallProductTagRelMapper;
import com.zhangyichuang.medicine.admin.service.MallProductTagRelService;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.security.base.BaseService;
import com.zhangyichuang.medicine.model.entity.MallProductTagRel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 商品标签关联服务实现。
 *
 * @author Chuang
 */
@Service
public class MallProductTagRelServiceImpl extends ServiceImpl<MallProductTagRelMapper, MallProductTagRel>
        implements MallProductTagRelService, BaseService {

    /**
     * 替换商品标签关联。
     *
     * @param productId 商品ID
     * @param tagIds    标签ID集合
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceProductTags(Long productId, List<Long> tagIds) {
        Assert.isPositive(productId, "商品ID不能为空");
        remove(new LambdaQueryWrapper<MallProductTagRel>()
                .eq(MallProductTagRel::getProductId, productId));
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        Date now = new Date();
        List<MallProductTagRel> relations = tagIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(tagId -> {
                    MallProductTagRel relation = new MallProductTagRel();
                    relation.setProductId(productId);
                    relation.setTagId(tagId);
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
     * 批量删除商品标签关联。
     *
     * @param productIds 商品ID列表
     */
    @Override
    public void removeByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        remove(new LambdaQueryWrapper<MallProductTagRel>()
                .in(MallProductTagRel::getProductId, productIds));
    }

    /**
     * 按商品ID列表查询标签关联。
     *
     * @param productIds 商品ID列表
     * @return 标签关联列表
     */
    @Override
    public List<MallProductTagRel> listByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return lambdaQuery()
                .in(MallProductTagRel::getProductId, productIds)
                .list();
    }

    /**
     * 按标签ID列表查询已绑定的商品ID列表。
     *
     * @param tagIds 标签ID列表
     * @return 商品ID列表
     */
    @Override
    public List<Long> listProductIdsByTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        return lambdaQuery()
                .in(MallProductTagRel::getTagId, tagIds)
                .list()
                .stream()
                .map(MallProductTagRel::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * 判断标签是否已被商品绑定。
     *
     * @param tagId 标签ID
     * @return true-已绑定
     */
    @Override
    public boolean existsByTagId(Long tagId) {
        if (tagId == null) {
            return false;
        }
        return lambdaQuery()
                .eq(MallProductTagRel::getTagId, tagId)
                .count() > 0;
    }
}
