package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端当前登录用户资料修改请求。
 */
@Data
@Schema(description = "管理端当前登录用户资料修改请求")
public class AdminProfileUpdateRequest {

    /**
     * 头像地址。
     */
    @Schema(description = "头像地址", example = "https://example.com/avatar.jpg")
    private String avatar;

    /**
     * 昵称。
     */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    @Schema(description = "昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "药智通管理员")
    private String nickname;

    /**
     * 真实姓名。
     */
    @Size(max = 50, message = "真实姓名长度不能超过50个字符")
    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    /**
     * 邮箱。
     */
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    @Schema(description = "邮箱", example = "admin@example.com")
    private String email;
}
