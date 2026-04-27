package com.zhangyichuang.medicine.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Set;

/**
 * 用户信息视图对象
 */
@Data
@Schema(name = "CurrentUserInfoVo", description = "当前登录用户信息")
public class CurrentUserInfoVo {

    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "昵称", example = "张三")
    private String nickname;

    @Schema(description = "头像", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "手机号", example = "13800000000")
    private String phoneNumber;

    @Schema(description = "角色标识集合", example = "[\"admin\",\"super_admin\"]")
    private Set<String> roles;

    @Schema(description = "权限编码集合", example = "[\"system:user:list\",\"mall:order:list\"]")
    private Set<String> permissions;
}
