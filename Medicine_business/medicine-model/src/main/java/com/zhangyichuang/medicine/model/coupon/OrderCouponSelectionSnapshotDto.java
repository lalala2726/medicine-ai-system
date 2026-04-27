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
 * 订单多券应用快照。
 *
 * <p>
 * 该对象用于持久化到订单的优惠券快照 JSON 字段，
 * 用于支付成功消耗、订单取消释放、后台重算等全链路一致性场景。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCouponSelectionSnapshotDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 选中的优惠券快照集合（按应用顺序）。
     */
    private List<OrderCouponSnapshotDto> selectedCoupons;

    /**
     * 选中的优惠券应用明细集合（按应用顺序）。
     */
    private List<CouponAppliedDetailDto> appliedCoupons;

    /**
     * 按商品项汇总后的分摊集合。
     */
    private List<CouponSettlementAllocationDto> allocations;

    /**
     * 聚合抵扣金额。
     */
    private BigDecimal couponDeductAmount;

    /**
     * 聚合消耗金额。
     */
    private BigDecimal couponConsumeAmount;

    /**
     * 聚合浪费金额。
     */
    private BigDecimal couponWasteAmount;

    /**
     * 是否自动选券。
     */
    private Boolean autoSelected;
}
