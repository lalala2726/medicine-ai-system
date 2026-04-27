package com.zhangyichuang.medicine.common.systemauth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 系统签名认证配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "system-auth")
public class SystemAuthProperties {

    /**
     * 系统鉴权总开关。
     */
    private boolean enabled = true;

    /**
     * 时间戳容差（秒）。
     */
    private long maxSkewSeconds = 300;

    /**
     * nonce 过期时间（秒）。
     */
    private long nonceTtlSeconds = 600;

    /**
     * nonce Redis key 前缀。
     */
    private String nonceKeyPrefix = "system_auth:nonce";

    /**
     * 客户端注册配置 JSON（app_id/secret/enabled）。
     */
    private String clientsJson = "[]";

    /**
     * 默认签名版本。
     */
    private String defaultSignVersion = "v1";

    /**
     * 本方出站 app_id。
     */
    private String xAgentKey;

    /**
     * 本方出站签名密钥。
     */
    private String localSecret;

}
