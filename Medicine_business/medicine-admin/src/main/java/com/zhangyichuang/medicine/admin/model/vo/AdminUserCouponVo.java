package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 管理端用户优惠券视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端用户优惠券")
public class AdminUserCouponVo {

    /**
     * 用户优惠券ID。
     */
    @Schema(description = "用户优惠券ID", example = "10001")
    private Long couponId;

    /**
     * 优惠券模板ID。
     */
    @Schema(description = "优惠券模板ID", example = "1")
    private Long templateId;

    /**
     * 用户ID。
     */
    @Schema(description = "用户ID", example = "1001")
    private Long userId;

    /**
     * 用户昵称。
     */
    @Schema(description = "用户昵称", example = "张三")
    private String userNickname;

    /**
     * 用户手机号。
     */
    @Schema(description = "用户手机号", example = "13800000000")
    private String userPhoneNumber;

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
     * 优惠券总金额。
     */
    @Schema(description = "优惠券总金额", example = "100.00")
    private BigDecimal totalAmount;

    /**
     * 当前可用金额。
     */
    @Schema(description = "当前可用金额", example = "80.00")
    private BigDecimal availableAmount;

    /**
     * 当前锁定消耗金额。
     */
    @Schema(description = "当前锁定消耗金额", example = "20.00")
    private BigDecimal lockedConsumeAmount;

    /**
     * 优惠券状态。
     */
    @Schema(description = "优惠券状态", example = "AVAILABLE")
    private String couponStatus;

    /**
     * 是否允许继续使用。
     */
    @Schema(description = "是否允许继续使用（1-允许，0-不允许）", example = "1")
    private Integer continueUseEnabled;

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
     * 锁定订单号。
     */
    @Schema(description = "锁定订单号", example = "O202604061230000001")
    private String lockOrderNo;

    /**
     * 锁定时间。
     */
    @Schema(description = "锁定时间", example = "2026-04-06 12:30:00")
    private Date lockTime;

    /**
     * 来源类型。
     */
    @Schema(description = "来源类型", example = "ADMIN_GRANT")
    private String sourceType;

    /**
     * 创建时间。
     */
    @Schema(description = "创建时间", example = "2026-04-06 12:00:00")
    private Date createTime;
}
