package com.zhangyichuang.medicine.client.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 当前用户修改密码请求。
 */
@Data
@Schema(description = "当前用户修改密码请求")
public class UserPasswordChangeRequest {

    /**
     * 原密码。
     */
    @NotBlank(message = "原密码不能为空")
    @Schema(description = "原密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "oldPassword123")
    private String oldPassword;

    /**
     * 新密码。
     */
    @NotBlank(message = "新密码不能为空")
    @Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "newPassword123")
    private String newPassword;

    /**
     * 登录前置消费的验证码校验凭证。
     */
    @NotBlank(message = "验证码校验凭证不能为空")
    @Schema(
            description = "登录前置消费的验证码校验凭证",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "captcha-verification-id"
    )
    private String captchaVerificationId;
}
