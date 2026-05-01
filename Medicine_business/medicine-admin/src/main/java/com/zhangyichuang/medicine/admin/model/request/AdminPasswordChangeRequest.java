package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端当前登录用户修改密码请求。
 */
@Data
@Schema(description = "管理端当前登录用户修改密码请求")
public class AdminPasswordChangeRequest {

    /**
     * 原登录密码。
     */
    @NotBlank(message = "原密码不能为空")
    @Size(min = 6, max = 20, message = "原密码长度为6-20个字符")
    @Schema(description = "原登录密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "oldPassword123")
    private String oldPassword;

    /**
     * 新登录密码。
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "新密码长度为6-20个字符")
    @Schema(description = "新登录密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "newPassword123")
    private String newPassword;

    /**
     * 滑动验证码校验凭证。
     */
    @NotBlank(message = "验证码校验凭证不能为空")
    @Schema(description = "滑动验证码校验凭证", requiredMode = Schema.RequiredMode.REQUIRED, example = "captcha-verification-id")
    private String captchaVerificationId;
}
