package com.zhangyichuang.medicine.client.model.vo.coupon;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 激活码兑换结果视图对象。
 */
@Data
@Builder
@Schema(description = "激活码兑换结果")
public class ActivationCodeRedeemVo {

    /**
     * 兑换日志ID。
     */
    @Schema(description = "兑换日志ID", example = "30001")
    private Long redeemLogId;

    /**
     * 用户优惠券ID。
     */
    @Schema(description = "用户优惠券ID", example = "20001")
    private Long couponId;

    /**
     * 优惠券模板ID。
     */
    @Schema(description = "优惠券模板ID", example = "1")
    private Long templateId;

    /**
     * 优惠券名称。
     */
    @Schema(description = "优惠券名称", example = "新人100元券")
    private String couponName;

    /**
     * 使用门槛金额。
     */
    @Schema(description = "使用门槛金额", example = "100.00")
    private BigDecimal thresholdAmount;

    /**
     * 优惠券总金额。
     */
    @Schema(description = "优惠券总金额", example = "100.00")
    private BigDecimal totalAmount;

    /**
     * 当前可用金额。
     */
    @Schema(description = "当前可用金额", example = "100.00")
    private BigDecimal availableAmount;

    /**
     * 生效时间。
     */
    @Schema(description = "生效时间")
    private Date effectiveTime;

    /**
     * 失效时间。
     */
    @Schema(description = "失效时间")
    private Date expireTime;

    /**
     * 优惠券状态。
     */
    @Schema(description = "优惠券状态", example = "AVAILABLE")
    private String couponStatus;
}
