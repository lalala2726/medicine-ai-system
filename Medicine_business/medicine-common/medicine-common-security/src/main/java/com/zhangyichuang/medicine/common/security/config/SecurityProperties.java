package com.zhangyichuang.medicine.common.security.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Chuang
 * <p>
 * created on 2025/8/28
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    /**
     * token标识符
     */
    private String header = "Authorization";

    /**
     * 密钥
     */
    private String secret;

    /**
     * 访问令牌有效期（单位：秒），-1 表示永不过期
     * 默认值：1800 秒（30分钟）
     */
    @Min(-1)
    private long accessTokenExpireTime = 1800;

    /**
     * 刷新令牌有效期（单位：秒），-1 表示永不过期
     * 默认值：259200 秒（3天）
     */
    @Min(-1)
    private long refreshTokenExpireTime = 259200;

}
