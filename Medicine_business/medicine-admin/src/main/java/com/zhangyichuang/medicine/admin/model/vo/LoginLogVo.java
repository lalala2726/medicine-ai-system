package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 登录日志详情。
 */
@Data
@Schema(description = "登录日志详情")
public class LoginLogVo {

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

    @Schema(description = "失败原因")
    private String failReason;

    @Schema(description = "登录方式", example = "password")
    private String loginType;

    @Schema(description = "IP地址", example = "localhost")
    private String ipAddress;

    @Schema(description = "User-Agent")
    private String userAgent;

    @Schema(description = "设备类型", example = "PC")
    private String deviceType;

    @Schema(description = "操作系统", example = "Windows")
    private String os;

    @Schema(description = "浏览器", example = "Chrome")
    private String browser;

    @Schema(description = "登录时间", example = "2026-02-12 10:00:00")
    private Date loginTime;
}
