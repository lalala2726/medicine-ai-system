package com.zhangyichuang.medicine.model.coupon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 订单优惠券快照对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCouponSnapshotDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户优惠券ID。
     */
    private Long couponId;

    /**
     * 优惠券模板ID。
     */
    private Long templateId;

    /**
     * 优惠券名称。
     */
    private String couponName;

    /**
     * 优惠券类型。
     */
    private String couponType;

    /**
     * 使用门槛金额。
     */
    private BigDecimal thresholdAmount;

    /**
     * 锁定时可用金额快照。
     */
    private BigDecimal lockedAvailableAmount;

    /**
     * 是否允许继续使用：1-允许，0-不允许。
     */
    private Integer continueUseEnabled;

    /**
     * 是否允许叠加：1-允许，0-不允许。
     */
    private Integer stackableEnabled;

    /**
     * 生效时间快照。
     */
    private Date effectiveTime;

    /**
     * 失效时间快照。
     */
    private Date expireTime;

    /**
     * 锁定时允许使用优惠券的商品ID列表。
     */
    private List<Long> eligibleProductIds;
}
