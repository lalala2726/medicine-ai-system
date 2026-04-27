package com.zhangyichuang.medicine.model.coupon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 优惠券结算商品项对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponSettlementItemDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 商品项业务键。
     */
    private String itemKey;

    /**
     * 商品ID。
     */
    private Long productId;

    /**
     * 商品原始金额。
     */
    private BigDecimal totalAmount;

    /**
     * 是否允许使用优惠券：1-允许，0-不允许。
     */
    private Integer couponEnabled;
}
