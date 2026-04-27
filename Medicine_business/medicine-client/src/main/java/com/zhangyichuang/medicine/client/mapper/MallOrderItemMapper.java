package com.zhangyichuang.medicine.client.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhangyichuang.medicine.client.model.dto.ProductSalesDto;
import com.zhangyichuang.medicine.model.entity.MallOrderItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Chuang
 */
public interface MallOrderItemMapper extends BaseMapper<MallOrderItem> {

    /**
     * 查询已完成订单的商品销量
     */
    List<ProductSalesDto> listCompletedSalesByProductIds(@Param("productIds") List<Long> productIds);

}
