package com.zhangyichuang.medicine.model.coupon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 单张优惠券应用明细。
 *
 * <p>
 * 该对象用于承载“某一张券在当前订单上的最终生效结果”，
 * 包含抵扣金额、消耗金额、浪费金额以及叠加/续用等关键规则快照，
 * 便于下单锁券、支付消耗、取消释放和审计日志保持一致。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponAppliedDetailDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户优惠券ID。
     */
    private Long couponId;

    /**
     * 优惠券名称快照。
     */
    private String couponName;

    /**
     * 该券在本订单上的抵扣金额。
     */
    private BigDecimal couponDeductAmount;

    /**
     * 该券在本订单上的实际消耗金额。
     */
    private BigDecimal couponConsumeAmount;

    /**
     * 该券在本订单上的浪费金额。
     */
    private BigDecimal couponWasteAmount;

    /**
     * 是否允许继续使用：1-允许，0-不允许。
     */
    private Integer continueUseEnabled;

    /**
     * 是否允许叠加：1-允许，0-不允许。
     */
    private Integer stackableEnabled;
}
