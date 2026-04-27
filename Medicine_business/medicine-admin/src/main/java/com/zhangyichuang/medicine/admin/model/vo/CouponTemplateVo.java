package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 优惠券模板视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "优惠券模板")
public class CouponTemplateVo {

    /**
     * 模板ID。
     */
    @Schema(description = "模板ID", example = "1")
    private Long id;

    /**
     * 优惠券类型。
     */
    @Schema(description = "优惠券类型", example = "FULL_REDUCTION")
    private String couponType;

    /**
     * 优惠券名称。
     */
    @Schema(description = "优惠券名称", example = "新人100元券")
    private String name;

    /**
     * 使用门槛金额。
     */
    @Schema(description = "使用门槛金额", example = "300.00")
    private BigDecimal thresholdAmount;

    /**
     * 优惠券面额。
     */
    @Schema(description = "优惠券面额", example = "100.00")
    private BigDecimal faceAmount;

    /**
     * 是否允许继续使用。
     */
    @Schema(description = "是否允许继续使用（1-允许，0-不允许）", example = "1")
    private Integer continueUseEnabled;

    /**
     * 是否允许叠加。
     */
    @Schema(description = "是否允许叠加（1-允许，0-不允许）", example = "0")
    private Integer stackableEnabled;

    /**
     * 模板状态。
     */
    @Schema(description = "模板状态", example = "ACTIVE")
    private String status;

    /**
     * 模板备注。
     */
    @Schema(description = "模板备注", example = "新客拉新使用")
    private String remark;

    /**
     * 创建时间。
     */
    @Schema(description = "创建时间", example = "2026-04-06 12:00:00")
    private Date createTime;
}
