package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 发券请求。
 */
@Data
@Schema(description = "发券请求")
public class CouponIssueRequest {

    /**
     * 模板ID。
     */
    @NotNull(message = "优惠券ID不能为空")
    @Schema(description = "优惠券ID", example = "1")
    private Long templateId;

    /**
     * 发券目标类型。
     */
    @NotBlank(message = "发券目标类型不能为空")
    @Schema(description = "发券目标类型（ALL-全部用户，SPECIFIED-指定用户）", example = "SPECIFIED")
    private String issueTargetType;

    /**
     * 指定用户ID列表。
     */
    @Schema(description = "指定用户ID列表", example = "[1001,1002,1003]")
    private List<Long> userIds;

    /**
     * 生效时间。
     */
    @NotNull(message = "生效时间不能为空")
    @Schema(description = "生效时间", example = "2026-04-06 12:00:00")
    private Date effectiveTime;

    /**
     * 失效时间。
     */
    @NotNull(message = "失效时间不能为空")
    @Schema(description = "失效时间", example = "2026-05-06 12:00:00")
    private Date expireTime;

    /**
     * 发券备注。
     */
    @Schema(description = "发券备注", example = "后台活动补发")
    private String remark;

    /**
     * 验证码校验凭证。
     */
    @NotBlank(message = "验证码校验凭证不能为空")
    @Schema(description = "验证码校验凭证", example = "captcha-verification-id")
    private String captchaVerificationId;
}
