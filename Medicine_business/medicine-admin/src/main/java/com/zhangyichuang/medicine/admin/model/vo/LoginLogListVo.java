package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 登录日志列表。
 */
@Data
@Schema(description = "登录日志列表")
public class LoginLogListVo {

    @Schema(description = "日志ID", example = "1")
    private long id = 0;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "登录账号", example = "admin")
    private String username;

    @Schema(description = "登录来源：admin/client", example = "admin")
    private String loginSource;

    @Schema(description = "登录状态：1成功 0失败", example = "1")
    private Integer loginStatus;

    @Schema(description = "登录方式", example = "password")
    private String loginType;

    @Schema(description = "IP地址", example = "localhost")
    private String ipAddress;

    @Schema(description = "登录时间", example = "2026-02-12 10:00:00")
    private Date loginTime;
}
