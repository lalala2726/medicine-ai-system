package com.zhangyichuang.medicine.client.model.vo.coupon;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单结算优惠券选项对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单结算优惠券选项")
public class OrderCouponOptionVo {

    /**
     * 用户优惠券ID。
     */
    @Schema(description = "用户优惠券ID", example = "10001")
    private Long couponId;

    /**
     * 优惠券名称。
     */
    @Schema(description = "优惠券名称", example = "新人100元券")
    private String couponName;

    /**
     * 使用门槛金额。
     */
    @Schema(description = "使用门槛金额", example = "300.00")
    private BigDecimal thresholdAmount;

    /**
     * 当前可用金额。
     */
    @Schema(description = "当前可用金额", example = "100.00")
    private BigDecimal availableAmount;

    /**
     * 是否允许继续使用。
     */
    @Schema(description = "是否允许继续使用（1-允许，0-不允许）", example = "1")
    private Integer continueUseEnabled;

    /**
     * 优惠券状态。
     */
    @Schema(description = "优惠券状态", example = "AVAILABLE")
    private String couponStatus;

    /**
     * 生效时间。
     */
    @Schema(description = "生效时间", example = "2026-04-06 12:00:00")
    private Date effectiveTime;

    /**
     * 失效时间。
     */
    @Schema(description = "失效时间", example = "2026-05-06 12:00:00")
    private Date expireTime;

    /**
     * 当前订单是否命中。
     */
    @Schema(description = "当前订单是否命中", example = "true")
    private Boolean matched;

    /**
     * 当前订单不可用原因。
     */
    @Schema(description = "当前订单不可用原因", example = "未达到使用门槛")
    private String unusableReason;

    /**
     * 订单抵扣金额。
     */
    @Schema(description = "订单抵扣金额", example = "30.00")
    private BigDecimal couponDeductAmount;

    /**
     * 优惠券消耗金额。
     */
    @Schema(description = "优惠券消耗金额", example = "30.00")
    private BigDecimal couponConsumeAmount;

    /**
     * 优惠券浪费金额。
     */
    @Schema(description = "优惠券浪费金额", example = "0.00")
    private BigDecimal couponWasteAmount;
}
