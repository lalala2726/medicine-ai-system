package com.zhangyichuang.medicine.model.coupon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 优惠券结算分摊结果对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponSettlementAllocationDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 商品项业务键。
     */
    private String itemKey;

    /**
     * 分摊优惠金额。
     */
    private BigDecimal couponDeductAmount;

    /**
     * 分摊后应付金额。
     */
    private BigDecimal payableAmount;
}
