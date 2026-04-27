package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @author Chuang
 * <p>
 * created on 2025/11/7
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "钱包金额修改请求对象")
public class WalletChangeRequest {

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "10.00")
    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    @Schema(description = "操作类型 (1-充值, 2-扣款)", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "操作类型不能为空")
    private Integer operationType;

    @Schema(description = "修改原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "充值")
    @NotBlank(message = "修改原因不能为空")
    private String reason;

    @Schema(description = "滑动验证码校验凭证", requiredMode = Schema.RequiredMode.REQUIRED, example = "captcha-verification-id")
    @NotBlank(message = "验证码校验凭证不能为空")
    private String captchaVerificationId;

}
