package com.zhangyichuang.medicine.client.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 激活码兑换请求。
 */
@Data
@Schema(description = "激活码兑换请求")
public class ActivationCodeRedeemRequest {

    /**
     * 激活码明文。
     */
    @NotBlank(message = "激活码不能为空")
    @Schema(description = "激活码明文", example = "ABCD1234EFGH5678JKLM")
    private String code;

    /**
     * 登录前置消费的验证码校验凭证。
     */
    @Schema(
            description = "登录前置消费的验证码校验凭证",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "captcha-verification-id"
    )
    @NotBlank(message = "验证码校验凭证不能为空")
    private String captchaVerificationId;
}
