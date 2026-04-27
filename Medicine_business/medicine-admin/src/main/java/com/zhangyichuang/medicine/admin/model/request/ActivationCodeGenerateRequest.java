package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

/**
 * 激活码生成请求。
 */
@Data
@Schema(description = "激活码生成请求")
public class ActivationCodeGenerateRequest {

    /**
     * 优惠券模板ID。
     */
    @NotNull(message = "优惠券模板ID不能为空")
    @Schema(description = "优惠券模板ID", example = "1")
    private Long templateId;

    /**
     * 兑换规则类型。
     */
    @NotBlank(message = "兑换规则类型不能为空")
    @Schema(
            description = "兑换规则类型（SHARED_PER_USER_ONCE-共享码每用户一次，UNIQUE_SINGLE_USE-唯一码全局一次）",
            example = "SHARED_PER_USER_ONCE"
    )
    private String redeemRuleType;

    /**
     * 生成数量。
     */
    @NotNull(message = "生成数量不能为空")
    @Min(value = 1, message = "生成数量必须大于0")
    @Schema(description = "生成数量", example = "20")
    @Max(value = 1000, message = "生成数量必须小于1000")
    private Integer generateCount;

    /**
     * 有效期类型。
     */
    @NotBlank(message = "有效期类型不能为空")
    @Schema(description = "有效期类型（ONCE-一次性，AFTER_ACTIVATION-激活后计算）", example = "ONCE")
    private String validityType;

    /**
     * 固定生效时间。
     */
    @Schema(description = "固定生效时间", example = "2026-04-09 12:00:00")
    private Date fixedEffectiveTime;

    /**
     * 固定失效时间。
     */
    @Schema(description = "固定失效时间", example = "2026-05-09 12:00:00")
    private Date fixedExpireTime;

    /**
     * 激活后有效天数。
     */
    @Schema(description = "激活后有效天数", example = "30")
    private Integer relativeValidDays;

    /**
     * 备注。
     */
    @Schema(description = "备注", example = "四月活动激活码")
    private String remark;

    /**
     * 验证码校验凭证。
     */
    @NotBlank(message = "验证码校验凭证不能为空")
    @Schema(description = "验证码校验凭证", example = "captcha-verification-id")
    private String captchaVerificationId;
}
