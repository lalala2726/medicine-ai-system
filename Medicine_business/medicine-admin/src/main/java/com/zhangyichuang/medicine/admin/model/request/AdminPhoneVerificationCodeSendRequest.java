package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 管理端发送手机号修改验证码请求。
 */
@Data
@Schema(description = "管理端发送手机号修改验证码请求")
public class AdminPhoneVerificationCodeSendRequest {

    /**
     * 新手机号。
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "新手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800000000")
    private String phoneNumber;

    /**
     * 滑动验证码校验凭证。
     */
    @NotBlank(message = "验证码校验凭证不能为空")
    @Schema(description = "滑动验证码校验凭证", requiredMode = Schema.RequiredMode.REQUIRED, example = "captcha-verification-id")
    private String captchaVerificationId;
}
