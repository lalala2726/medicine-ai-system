package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 优惠券模板新增请求。
 */
@Data
@Schema(description = "优惠券模板新增请求")
public class CouponTemplateAddRequest {

    /**
     * 优惠券名称。
     */
    @NotBlank(message = "优惠券名称不能为空")
    @Schema(description = "优惠券名称", example = "新人100元券")
    private String name;

    /**
     * 使用门槛金额。
     */
    @NotNull(message = "使用门槛金额不能为空")
    @Schema(description = "使用门槛金额", example = "300.00")
    private BigDecimal thresholdAmount;

    /**
     * 优惠券面额。
     */
    @NotNull(message = "优惠券面额不能为空")
    @Schema(description = "优惠券面额", example = "100.00")
    private BigDecimal faceAmount;

    /**
     * 是否允许继续使用。
     */
    @NotNull(message = "是否允许继续使用不能为空")
    @Schema(description = "是否允许继续使用（1-允许，0-不允许）", example = "1")
    private Integer continueUseEnabled;

    /**
     * 是否允许叠加。
     */
    @NotNull(message = "是否允许叠加不能为空")
    @Schema(description = "是否允许叠加（1-允许，0-不允许）", example = "0")
    private Integer stackableEnabled;

    /**
     * 模板状态。
     */
    @NotBlank(message = "模板状态不能为空")
    @Schema(description = "模板状态", example = "ACTIVE")
    private String status;

    /**
     * 模板备注。
     */
    @Schema(description = "模板备注", example = "新客拉新使用")
    private String remark;
}
