package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Chuang
 * <p>
 * created on 2025/11/7
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreezeOrUnUserWalletRequest {
    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "冻结或解冻原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "风控处理")
    @NotBlank(message = "冻结或解冻原因不能为空")
    private String reason;

    @Schema(description = "滑动验证码校验凭证", requiredMode = Schema.RequiredMode.REQUIRED, example = "captcha-verification-id")
    @NotBlank(message = "验证码校验凭证不能为空")
    private String captchaVerificationId;

}
