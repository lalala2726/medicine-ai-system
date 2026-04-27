package com.zhangyichuang.medicine.client.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.client.mapper.MallOrderItemMapper;
import com.zhangyichuang.medicine.client.model.dto.ProductSalesDto;
import com.zhangyichuang.medicine.client.service.MallOrderItemService;
import com.zhangyichuang.medicine.model.entity.MallOrderItem;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Chuang
 */
@Service
public class MallOrderItemServiceImpl extends ServiceImpl<MallOrderItemMapper, MallOrderItem>
        implements MallOrderItemService {


    @Override
    public List<MallOrderItem> getOrderItemByOrderId(Long orderId) {
        return Optional.ofNullable(orderId)
                .map(id -> lambdaQuery()
                        .eq(MallOrderItem::getOrderId, id)
                        .list())
                .orElse(List.of());
    }

    @Override
    public java.util.Map<Long, Integer> getCompletedSalesByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ProductSalesDto> stats = baseMapper.listCompletedSalesByProductIds(productIds);
        if (stats == null || stats.isEmpty()) {
            return Collections.emptyMap();
        }
        return stats.stream().collect(Collectors.toMap(ProductSalesDto::getProductId, ProductSalesDto::getSales));
    }

    @Override
    public Integer getCompletedSalesByProductId(Long productId) {
        if (productId == null) {
            return 0;
        }
        return getCompletedSalesByProductIds(List.of(productId)).getOrDefault(productId, 0);
    }
}

