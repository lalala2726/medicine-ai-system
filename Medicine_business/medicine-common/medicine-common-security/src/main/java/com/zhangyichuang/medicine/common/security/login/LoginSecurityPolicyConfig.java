package com.zhangyichuang.medicine.common.security.login;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录安全策略配置。
 */
@Data
public class LoginSecurityPolicyConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 连续失败阈值，达到后触发锁定。
     */
    private Integer maxRetryCount;

    /**
     * 锁定时长（分钟）。
     */
    private Integer lockMinutes;
}
