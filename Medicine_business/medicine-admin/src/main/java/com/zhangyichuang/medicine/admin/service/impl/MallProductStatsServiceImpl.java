package com.zhangyichuang.medicine.admin.service.impl;

import com.zhangyichuang.medicine.admin.mapper.MallOrderItemMapper;
import com.zhangyichuang.medicine.admin.model.dto.ProductSalesDto;
import com.zhangyichuang.medicine.admin.service.MallProductStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品统计服务实现类
 * <p>
 * 实现商品相关的统计分析功能，从底层直接查询数据，
 * 避免与高层业务服务产生循环依赖。
 *
 * @author Chuang
 * created on 2025/11/25
 */
@Service
@RequiredArgsConstructor
public class MallProductStatsServiceImpl implements MallProductStatsService {

    private final MallOrderItemMapper mallOrderItemMapper;

    @Override
    public List<ProductSalesDto> getProductSales() {
        return mallOrderItemMapper.getProductSales();
    }

    @Override
    public List<ProductSalesDto> getProductSalesByIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return mallOrderItemMapper.getProductSalesByIds(productIds);
    }
}
