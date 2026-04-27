package com.zhangyichuang.medicine.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhangyichuang.medicine.admin.model.dto.ProductSalesDto;
import com.zhangyichuang.medicine.model.entity.MallOrderItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Chuang
 */
public interface MallOrderItemMapper extends BaseMapper<MallOrderItem> {


    /**
     * 获取商品销售信息
     *
     * @return 商品销售信息
     */
    List<ProductSalesDto> getProductSales();

    /**
     * 根据商品ID列表获取商品销售信息
     *
     * @param productIds 商品ID列表
     * @return 商品销售信息
     */
    List<ProductSalesDto> getProductSalesByIds(@Param("productIds") List<Long> productIds);

    /**
     * 查询存在指定订单状态的商品ID
     *
     * @param productIds    商品ID列表
     * @param orderStatuses 订单状态列表
     * @return 存在指定状态订单的商品ID列表
     */
    List<Long> findProductIdsWithOrderStatuses(@Param("productIds") List<Long> productIds,
                                               @Param("orderStatuses") List<String> orderStatuses);
}



