package com.zhangyichuang.medicine.model.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 登录日志消息体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginLogMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID（失败时允许为空）。
     */
    private Long userId;

    /**
     * 用户名。
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
