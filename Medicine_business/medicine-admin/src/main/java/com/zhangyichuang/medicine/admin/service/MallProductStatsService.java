package com.zhangyichuang.medicine.admin.service;

import com.zhangyichuang.medicine.admin.model.dto.ProductSalesDto;

import java.util.List;

/**
 * 商品统计服务
 * <p>
 * 提供商品相关的统计分析功能，包括销量统计等数据。
 * 独立的服务避免与其他服务产生循环依赖。
 *
 * @author Chuang
 * created on 2025/11/25
 */
public interface MallProductStatsService {

    /**
     * 获取商品销量统计
     *
     * @return 商品销量统计列表
     */
    List<ProductSalesDto> getProductSales();

    /**
     * 获取指定商品ID的销量统计
     *
     * @param productIds 商品ID列表
     * @return 商品销量统计列表
     */
    List<ProductSalesDto> getProductSalesByIds(List<Long> productIds);
}