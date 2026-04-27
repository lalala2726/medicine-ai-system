package com.zhangyichuang.medicine.common.security.login;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录安全总配置，管理端与客户端独立生效。
 */
@Data
public class LoginSecurityConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 管理端登录安全策略。
     */
    private LoginSecurityPolicyConfig admin;

    /**
     * 客户端登录安全策略。
     */
    private LoginSecurityPolicyConfig client;

    /**
     * 管理端水印配置。
     */
    private AdminWatermarkConfig adminWatermark;
}
