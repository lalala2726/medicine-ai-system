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
 * 自动选券计算结果。
 *
 * <p>
 * 该对象用于统一承载自动选券算法输出，
 * 包括最终选中的优惠券集合、每张券应用明细、商品维度分摊结果以及订单聚合金额。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponAutoSelectResultDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 最终选中的优惠券快照集合（按应用顺序）。
     */
    private List<OrderCouponSnapshotDto> selectedCoupons;

    /**
     * 最终选中的优惠券应用明细集合（按应用顺序）。
     */
    private List<CouponAppliedDetailDto> appliedCoupons;

    /**
     * 按商品项汇总后的优惠分摊结果集合。
     */
    private List<CouponSettlementAllocationDto> allocations;

    /**
     * 订单层总抵扣金额。
     */
    private BigDecimal couponDeductAmount;

    /**
     * 订单层总消耗金额。
     */
    private BigDecimal couponConsumeAmount;

    /**
     * 订单层总浪费金额。
     */
    private BigDecimal couponWasteAmount;

    /**
     * 是否由自动选券算法产生。
     */
    private Boolean autoSelected;
}
