package com.zhangyichuang.medicine.model.coupon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券结算结果对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponSettlementResultDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 商品原始总金额。
     */
    private BigDecimal itemsAmount;

    /**
     * 允许使用优惠券的商品总金额。
     */
    private BigDecimal eligibleItemsAmount;

    /**
     * 订单抵扣金额。
     */
    private BigDecimal couponDeductAmount;

    /**
     * 优惠券消耗金额。
     */
    private BigDecimal couponConsumeAmount;

    /**
     * 优惠券浪费金额。
     */
    private BigDecimal couponWasteAmount;

    /**
     * 商品项分摊明细。
     */
    private List<CouponSettlementAllocationDto> allocations;
}
