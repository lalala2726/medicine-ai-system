package com.zhangyichuang.medicine.common.security.login;

import com.zhangyichuang.medicine.common.core.constants.RedisConstants;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.redis.core.RedisCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 登录安全服务，提供失败计数、锁定判定与配置管理能力。
 */
@Service
@RequiredArgsConstructor
public class LoginSecurityService {

    /**
     * 管理端登录来源标识。
     */
    public static final String LOGIN_SOURCE_ADMIN = "admin";

    /**
     * 客户端登录来源标识。
     */
    public static final String LOGIN_SOURCE_CLIENT = "client";

    /**
     * 默认连续失败阈值。
     */
    private static final int DEFAULT_MAX_RETRY_COUNT = 3;

    /**
     * 默认锁定分钟数。
     */
    private static final int DEFAULT_LOCK_MINUTES = 10;

    /**
     * 策略配置最小值。
     */
    private static final int MIN_POLICY_VALUE = 1;

    /**
     * 管理端水印默认启用状态。
     */
    private static final boolean DEFAULT_ADMIN_WATERMARK_ENABLED = false;

    /**
     * 管理端水印默认用户名展示状态。
     */
    private static final boolean DEFAULT_ADMIN_WATERMARK_SHOW_USERNAME = true;

    /**
     * 管理端水印默认用户ID展示状态。
     */
    private static final boolean DEFAULT_ADMIN_WATERMARK_SHOW_USER_ID = true;

    /**
     * 锁定标记值。
     */
    private static final String LOCKED_VALUE = "1";

    private final RedisCache redisCache;

    /**
     * 读取登录安全配置；首次不存在时写入默认配置。
     *
     * @return 登录安全配置
     */
    public LoginSecurityConfig getSecurityConfig() {
        LoginSecurityConfig config = redisCache.getCacheObject(RedisConstants.Auth.LOGIN_SECURITY_CONFIG_KEY);
        if (config == null) {
            config = buildDefaultSecurityConfig();
            redisCache.setCacheObject(RedisConstants.Auth.LOGIN_SECURITY_CONFIG_KEY, config);
        }
        config = normalizeSecurityConfig(config);
        validateSecurityConfig(config);
        return config;
    }

    /**
     * 更新登录安全配置。
     *
     * @param config 登录安全配置
     * @return 无返回值
     */
    public void updateSecurityConfig(LoginSecurityConfig config) {
        Assert.notNull(config, "登录安全配置不能为空");
        LoginSecurityConfig normalizedConfig = normalizeSecurityConfig(config);
        validateSecurityConfig(normalizedConfig);
        redisCache.setCacheObject(RedisConstants.Auth.LOGIN_SECURITY_CONFIG_KEY, normalizedConfig);
    }

    /**
     * 归一化登录安全配置中的可选字段。
     *
     * @param config 登录安全配置
     * @return 归一化后的登录安全配置
     */
    private LoginSecurityConfig normalizeSecurityConfig(LoginSecurityConfig config) {
        Assert.notNull(config, "登录安全配置不能为空");
        if (config.getAdminWatermark() == null) {
            config.setAdminWatermark(buildDefaultAdminWatermarkConfig());
        } else {
            AdminWatermarkConfig watermarkConfig = config.getAdminWatermark();
            if (watermarkConfig.getEnabled() == null) {
                watermarkConfig.setEnabled(DEFAULT_ADMIN_WATERMARK_ENABLED);
            }
            if (watermarkConfig.getShowUsername() == null) {
                watermarkConfig.setShowUsername(DEFAULT_ADMIN_WATERMARK_SHOW_USERNAME);
            }
            if (watermarkConfig.getShowUserId() == null) {
                watermarkConfig.setShowUserId(DEFAULT_ADMIN_WATERMARK_SHOW_USER_ID);
            }
        }
        return config;
    }

    /**
     * 查询管理端水印配置。
     *
     * @return 管理端水印配置
     */
    public AdminWatermarkConfig getAdminWatermarkConfig() {
        return getSecurityConfig().getAdminWatermark();
    }

    /**
     * 查询指定来源与账号是否处于锁定状态。
     *
     * @param loginSource 登录来源：admin/client
     * @param username    登录用户名
     * @return 锁定状态与剩余秒数
     */
    public LoginLockStatus checkLockStatus(String loginSource, String username) {
        String lockKey = buildLockKey(loginSource, username);
        if (!redisCache.exists(lockKey)) {
            return new LoginLockStatus(false, 0L);
        }
        long remainingSeconds = redisCache.getExpire(lockKey, TimeUnit.SECONDS);
        if (remainingSeconds <= 0L) {
            redisCache.deleteObject(lockKey);
            return new LoginLockStatus(false, 0L);
        }
        return new LoginLockStatus(true, remainingSeconds);
    }

    /**
     * 记录一次登录失败。
     * <p>
     * 若达到阈值，会立即进入锁定并清空失败计数。
     * </p>
     *
     * @param loginSource 登录来源：admin/client
     * @param username    登录用户名
     * @return 失败处理结果
     */
    public LoginFailureResult recordLoginFailure(String loginSource, String username) {
        String normalizedSource = normalizeLoginSource(loginSource);
        String normalizedUsername = normalizeUsername(username);
        LoginLockStatus lockStatus = checkLockStatus(normalizedSource, normalizedUsername);
        if (lockStatus.isLocked()) {
            return new LoginFailureResult(true, 0, lockStatus.getRemainingSeconds());
        }

        LoginSecurityPolicyConfig policyConfig = resolvePolicyConfig(getSecurityConfig(), normalizedSource);
        String retryKey = String.format(RedisConstants.Auth.PASSWORD_RETRY_COUNT_KEY, normalizedSource, normalizedUsername);
        long retryCount = redisCache.increment(retryKey);
        if (retryCount >= policyConfig.getMaxRetryCount()) {
            long lockSeconds = TimeUnit.MINUTES.toSeconds(policyConfig.getLockMinutes());
            String lockKey = String.format(RedisConstants.Auth.PASSWORD_LOCK_KEY, normalizedSource, normalizedUsername);
            redisCache.setCacheObject(lockKey, LOCKED_VALUE, lockSeconds, TimeUnit.SECONDS);
            redisCache.deleteObject(retryKey);
            return new LoginFailureResult(true, policyConfig.getMaxRetryCount(), lockSeconds);
        }
        return new LoginFailureResult(false, (int) retryCount, 0L);
    }

    /**
     * 清空指定账号的登录失败与锁定状态。
     *
     * @param loginSource 登录来源：admin/client
     * @param username    登录用户名
     * @return 无返回值
     */
    public void clearLoginState(String loginSource, String username) {
        String normalizedSource = normalizeLoginSource(loginSource);
        String normalizedUsername = normalizeUsername(username);
        redisCache.deleteObject(String.format(RedisConstants.Auth.PASSWORD_RETRY_COUNT_KEY, normalizedSource, normalizedUsername));
        redisCache.deleteObject(String.format(RedisConstants.Auth.PASSWORD_LOCK_KEY, normalizedSource, normalizedUsername));
    }

    /**
     * 根据锁定剩余秒数构建统一提示文案。
     *
     * @param remainingSeconds 剩余锁定秒数
     * @return 锁定提示文案
     */
    public String buildLockMessage(long remainingSeconds) {
        long remainingMinutes = Math.max(1L, (remainingSeconds + 59L) / 60L);
        return String.format("账号已锁定，请%d分钟后重试", remainingMinutes);
    }

    /**
     * 构建锁定缓存Key。
     *
     * @param loginSource 登录来源：admin/client
     * @param username    登录用户名
     * @return 锁定缓存Key
     */
    private String buildLockKey(String loginSource, String username) {
        String normalizedSource = normalizeLoginSource(loginSource);
        String normalizedUsername = normalizeUsername(username);
        return String.format(RedisConstants.Auth.PASSWORD_LOCK_KEY, normalizedSource, normalizedUsername);
    }

    /**
     * 解析来源对应的策略配置。
     *
     * @param config      登录安全总配置
     * @param loginSource 登录来源：admin/client
     * @return 来源对应策略配置
     */
    private LoginSecurityPolicyConfig resolvePolicyConfig(LoginSecurityConfig config, String loginSource) {
        if (LOGIN_SOURCE_ADMIN.equals(loginSource)) {
            return config.getAdmin();
        }
        return config.getClient();
    }

    /**
     * 校验登录安全总配置是否合法。
     *
     * @param config 登录安全总配置
     * @return 无返回值
     */
    private void validateSecurityConfig(LoginSecurityConfig config) {
        Assert.notNull(config.getAdmin(), "管理端登录安全配置不能为空");
        Assert.notNull(config.getClient(), "客户端登录安全配置不能为空");
        Assert.notNull(config.getAdminWatermark(), "管理端水印配置不能为空");
        validatePolicyConfig(config.getAdmin(), "管理端");
        validatePolicyConfig(config.getClient(), "客户端");
        validateAdminWatermarkConfig(config.getAdminWatermark());
    }

    /**
     * 校验单端策略配置是否合法。
     *
     * @param policyConfig 策略配置
     * @param sourceLabel  来源说明
     * @return 无返回值
     */
    private void validatePolicyConfig(LoginSecurityPolicyConfig policyConfig, String sourceLabel) {
        Assert.notNull(policyConfig.getMaxRetryCount(), sourceLabel + "连续失败阈值不能为空");
        Assert.notNull(policyConfig.getLockMinutes(), sourceLabel + "锁定分钟数不能为空");
        Assert.isParamTrue(policyConfig.getMaxRetryCount() >= MIN_POLICY_VALUE, sourceLabel + "连续失败阈值必须大于等于1");
        Assert.isParamTrue(policyConfig.getLockMinutes() >= MIN_POLICY_VALUE, sourceLabel + "锁定分钟数必须大于等于1");
    }

    /**
     * 校验管理端水印配置是否合法。
     *
     * @param config 管理端水印配置
     */
    private void validateAdminWatermarkConfig(AdminWatermarkConfig config) {
        Assert.notNull(config.getEnabled(), "管理端水印启用状态不能为空");
        Assert.notNull(config.getShowUsername(), "管理端水印用户名展示状态不能为空");
        Assert.notNull(config.getShowUserId(), "管理端水印用户ID展示状态不能为空");
        if (Boolean.TRUE.equals(config.getEnabled())) {
            Assert.isParamTrue(
                    Boolean.TRUE.equals(config.getShowUsername()) || Boolean.TRUE.equals(config.getShowUserId()),
                    "管理端水印至少需要选择一种显示内容"
            );
        }
    }

    /**
     * 归一化登录来源。
     *
     * @param loginSource 登录来源：admin/client
     * @return 归一化后的登录来源
     */
    private String normalizeLoginSource(String loginSource) {
        Assert.hasText(loginSource, "登录来源不能为空");
        String normalizedSource = loginSource.trim().toLowerCase(Locale.ROOT);
        Assert.isParamTrue(LOGIN_SOURCE_ADMIN.equals(normalizedSource) || LOGIN_SOURCE_CLIENT.equals(normalizedSource),
                "登录来源不合法");
        return normalizedSource;
    }

    /**
     * 归一化登录用户名。
     *
     * @param username 登录用户名
     * @return 归一化后的用户名
     */
    private String normalizeUsername(String username) {
        Assert.hasText(username, "登录用户名不能为空");
        return username.trim();
    }

    /**
     * 构建默认登录安全配置。
     *
     * @return 默认登录安全配置
     */
    private LoginSecurityConfig buildDefaultSecurityConfig() {
        LoginSecurityConfig config = new LoginSecurityConfig();
        config.setAdmin(buildDefaultPolicyConfig());
        config.setClient(buildDefaultPolicyConfig());
        config.setAdminWatermark(buildDefaultAdminWatermarkConfig());
        return config;
    }

    /**
     * 构建默认单端登录策略。
     *
     * @return 默认单端登录策略
     */
    private LoginSecurityPolicyConfig buildDefaultPolicyConfig() {
        LoginSecurityPolicyConfig config = new LoginSecurityPolicyConfig();
        config.setMaxRetryCount(DEFAULT_MAX_RETRY_COUNT);
        config.setLockMinutes(DEFAULT_LOCK_MINUTES);
        return config;
    }

    /**
     * 构建默认管理端水印配置。
     *
     * @return 默认管理端水印配置
     */
    private AdminWatermarkConfig buildDefaultAdminWatermarkConfig() {
        AdminWatermarkConfig config = new AdminWatermarkConfig();
        config.setEnabled(DEFAULT_ADMIN_WATERMARK_ENABLED);
        config.setShowUsername(DEFAULT_ADMIN_WATERMARK_SHOW_USERNAME);
        config.setShowUserId(DEFAULT_ADMIN_WATERMARK_SHOW_USER_ID);
        return config;
    }
}
