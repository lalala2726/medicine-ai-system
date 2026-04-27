package com.zhangyichuang.medicine.model.coupon;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 优惠券发放结果。
 */
@Data
@Builder
public class CouponGrantResultDto {

    /**
     * 用户优惠券ID。
     */
    private Long couponId;

    /**
     * 优惠券模板ID。
     */
    private Long templateId;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 优惠券名称。
     */
    private String couponName;

    /**
     * 使用门槛金额。
     */
    private BigDecimal thresholdAmount;

    /**
     * 优惠券总金额。
     */
    private BigDecimal totalAmount;

    /**
     * 当前可用金额。
     */
    private BigDecimal availableAmount;

    /**
     * 生效时间。
     */
    private Date effectiveTime;

    /**
     * 失效时间。
     */
    private Date expireTime;

    /**
     * 优惠券状态。
     */
    private String couponStatus;
}
