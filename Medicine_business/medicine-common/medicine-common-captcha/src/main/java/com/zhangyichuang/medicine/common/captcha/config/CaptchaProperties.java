package com.zhangyichuang.medicine.common.captcha.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 验证码配置属性。
 *
 * @author Chuang
 */
@Data
@ConfigurationProperties(prefix = "medicine.captcha")
public class CaptchaProperties {

    /**
     * 默认 Redis 键前缀。
     */
    private static final String DEFAULT_REDIS_KEY_PREFIX = "medicine:captcha";

    /**
     * 默认 challenge 过期时间（毫秒）。
     */
    private static final long DEFAULT_CHALLENGE_EXPIRE_MS = 120000L;

    /**
     * 默认登录校验令牌过期时间（毫秒）。
     */
    private static final long DEFAULT_VERIFICATION_EXPIRE_MS = 120000L;

    /**
     * 默认背景图资源目录。
     */
    private static final String DEFAULT_RESOURCE_PREFIX = "classpath:captcha/backgrounds";

    /**
     * 验证码功能总开关。
     */
    private boolean enabled = true;

    /**
     * 是否强制登录时校验验证码。
     */
    private boolean loginRequired = false;

    /**
     * challenge 过期时间（毫秒）。
     */
    private long challengeExpireMs = DEFAULT_CHALLENGE_EXPIRE_MS;

    /**
     * 登录消费凭证过期时间（毫秒）。
     */
    private long verificationExpireMs = DEFAULT_VERIFICATION_EXPIRE_MS;

    /**
     * Redis 键前缀。
     */
    private String redisKeyPrefix = DEFAULT_REDIS_KEY_PREFIX;

    /**
     * 是否启用 tianai 默认模板。
     */
    private boolean initDefaultTemplate = true;

    /**
     * 背景图资源目录前缀。
     */
    private String resourcePrefix = DEFAULT_RESOURCE_PREFIX;
}
