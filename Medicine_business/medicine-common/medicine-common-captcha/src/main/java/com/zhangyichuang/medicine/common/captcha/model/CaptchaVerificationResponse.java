package com.zhangyichuang.medicine.common.captcha.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码二次校验结果。
 *
 * @author Chuang
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "验证码二次校验结果")
public class CaptchaVerificationResponse {

    /**
     * 登录阶段消费的验证码校验凭证。
     */
    @Schema(description = "登录阶段消费的验证码校验凭证", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;
}
