package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 优惠券日志视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "优惠券日志")
public class CouponLogVo {

    /**
     * 日志ID。
     */
    @Schema(description = "日志ID", example = "1")
    private Long id;

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
     * 用户ID。
     */
    @Schema(description = "用户ID", example = "1001")
    private Long userId;

    /**
     * 用户名。
     */
    @Schema(description = "用户名", example = "zhangsan")
    private String userName;

    /**
     * 订单号。
     */
    @Schema(description = "订单号", example = "O202604061230000001")
    private String orderNo;

    /**
     * 变更类型。
     */
    @Schema(description = "变更类型", example = "LOCK")
    private String changeType;

    /**
     * 变更金额。
     */
    @Schema(description = "变更金额", example = "30.00")
    private BigDecimal changeAmount;

    /**
     * 抵扣金额。
     */
    @Schema(description = "抵扣金额", example = "30.00")
    private BigDecimal deductAmount;

    /**
     * 浪费金额。
     */
    @Schema(description = "浪费金额", example = "0.00")
    private BigDecimal wasteAmount;

    /**
     * 变更前可用金额。
     */
    @Schema(description = "变更前可用金额", example = "100.00")
    private BigDecimal beforeAvailableAmount;

    /**
     * 变更后可用金额。
     */
    @Schema(description = "变更后可用金额", example = "70.00")
    private BigDecimal afterAvailableAmount;

    /**
     * 来源类型。
     */
    @Schema(description = "来源类型", example = "ORDER")
    private String sourceType;

    /**
     * 来源业务号。
     */
    @Schema(description = "来源业务号", example = "O202604061230000001")
    private String sourceBizNo;

    /**
     * 备注。
     */
    @Schema(description = "备注", example = "订单锁券")
    private String remark;

    /**
     * 操作人标识。
     */
    @Schema(description = "操作人标识", example = "admin")
    private String operatorId;

    /**
     * 操作人用户名。
     */
    @Schema(description = "操作人用户名", example = "admin")
    private String operatorName;

    /**
     * 创建时间。
     */
    @Schema(description = "创建时间", example = "2026-04-06 12:00:00")
    private Date createTime;
}
