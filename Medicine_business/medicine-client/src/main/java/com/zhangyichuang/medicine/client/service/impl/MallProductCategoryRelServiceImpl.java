package com.zhangyichuang.medicine.client.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.client.mapper.MallProductCategoryRelMapper;
import com.zhangyichuang.medicine.client.service.MallProductCategoryRelService;
import com.zhangyichuang.medicine.model.entity.MallProductCategoryRel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品分类关联服务实现（客户端）。
 *
 * @author Chuang
 */
@Service
public class MallProductCategoryRelServiceImpl
        extends ServiceImpl<MallProductCategoryRelMapper, MallProductCategoryRel>
        implements MallProductCategoryRelService {

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
}
