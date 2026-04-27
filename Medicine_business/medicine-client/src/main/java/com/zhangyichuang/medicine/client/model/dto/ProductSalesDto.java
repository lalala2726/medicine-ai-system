package com.zhangyichuang.medicine.client.model.dto;

import lombok.Data;

/**
 * 商品销量统计
 */
@Data
public class ProductSalesDto {

    private Long productId;

    private Integer sales;
}
