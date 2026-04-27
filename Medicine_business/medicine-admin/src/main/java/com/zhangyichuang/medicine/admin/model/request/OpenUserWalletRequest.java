package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 开通用户钱包请求对象。
 *
 * @author Chuang
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "开通用户钱包请求对象")
public class OpenUserWalletRequest {

    /**
     * 用户ID。
     */
    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 滑动验证码校验凭证。
     */
    @Schema(description = "滑动验证码校验凭证", requiredMode = Schema.RequiredMode.REQUIRED, example = "captcha-verification-id")
    @NotBlank(message = "验证码校验凭证不能为空")
    private String captchaVerificationId;

}
