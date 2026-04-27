package com.zhangyichuang.medicine.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author Chuang
 * <p>
 * created on 2025/8/28
 */
@Schema(description = "登录请求参数")
@Data
public class LoginRequest {

    /**
     * 登录用户名。
     */
    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin")
    private String username;

    /**
     * 登录密码。
     */
    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    private String password;

    /**
     * 登录前置消费的验证码校验凭证。
     */
    @NotBlank(message = "验证码不能为空")
    @Schema(description = "登录前置消费的验证码校验凭证", example = "captcha-verification-id")
    private String captchaVerificationId;
}
