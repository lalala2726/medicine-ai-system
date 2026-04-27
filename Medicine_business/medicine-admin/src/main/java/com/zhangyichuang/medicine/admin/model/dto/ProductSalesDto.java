package com.zhangyichuang.medicine.admin.model.dto;

import lombok.Data;

/**
 * @author Chuang
 * <p>
 * created on 2025/11/25
 */
@Data
public class ProductSalesDto {

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 销量
     */
    private Integer sales;
}
