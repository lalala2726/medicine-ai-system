package com.zhangyichuang.medicine.model.dto;

import com.zhangyichuang.medicine.model.entity.MallOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * @author Chuang
 * <p>
 * created on 2025/11/4
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderWithProductDto extends MallOrder {

    /**
     * 关联商品ID（首个订单项）
     */
    private Long productId;

    /**
     * 商品名称（优先取实时商品名，回退至订单项快照）
     */
    private String productName;

    /**
     * 购买数量（首个订单项）
     */
    private Integer productQuantity;

    /**
     * 商品首图
     */
    private String productImage;

    /**
     * 商品价格
     */
    private BigDecimal productPrice;

    /**
     * 商品分类
     */
    private String productCategory;
}
