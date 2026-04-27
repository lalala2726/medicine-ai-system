package com.zhangyichuang.medicine.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Set;

/**
 * 用户
 */
@Schema(description = "用户修改请求对象")
@Data
public class UserUpdateRequest {

    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    @Schema(description = "昵称", example = "张三")
    private String nickname;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "密码", example = "123456")
    private String password;

    @Schema(description = "角色ID集合", example = "[1,2]")
    private Set<Long> roles;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "身份证号码", example = "123456789012345678")
    private String idCard;

    @Schema(description = "手机号码", example = "13800000000")
    private String phoneNumber;

    @Schema(description = "性别", example = "1")
    private Integer gender;

    @Schema(description = "真实姓名", example = "张三")
    private String realName;
}
