package com.zhangyichuang.medicine.client.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 当前用户修改手机号请求。
 */
@Data
@Schema(description = "当前用户修改手机号请求")
public class UserPhoneChangeRequest {

    /**
     * 新手机号。
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "新手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800000000")
    private String phoneNumber;

    /**
     * 手机验证码。
     */
    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码格式不正确")
    @Schema(description = "手机验证码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    private String verificationCode;
}
