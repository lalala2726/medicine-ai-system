package com.zhangyichuang.medicine.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 系统登录日志表。
 */
@TableName(value = "sys_login_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysLoginLog {

    /**
     * 主键ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID（失败时允许为空）。
     */
    private Long userId;

    /**
     * 登录账号。
     */
    private String username;

    /**
     * 登录来源：admin/client。
     */
    private String loginSource;

    /**
     * 登录状态：0失败 1成功。
     */
    private Integer loginStatus;

    /**
     * 失败原因。
     */
    private String failReason;

    /**
     * 登录方式。
     */
    private String loginType;

    /**
     * IP地址。
     */
    private String ipAddress;

    /**
     * User-Agent。
     */
    private String userAgent;

    /**
     * 设备类型。
     */
    private String deviceType;

    /**
     * 操作系统。
     */
    private String os;

    /**
     * 浏览器。
     */
    private String browser;

    /**
     * 登录时间。
     */
    private Date loginTime;
}
