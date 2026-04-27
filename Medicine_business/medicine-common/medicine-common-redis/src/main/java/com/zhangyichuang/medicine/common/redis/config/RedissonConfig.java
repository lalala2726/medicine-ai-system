package com.zhangyichuang.medicine.common.redis.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.EqualJitterDelay;
import org.redisson.config.SingleServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Redisson 配置。
 * <p>
 * 当前项目统一复用现有 {@code spring.data.redis.*} 配置创建单机版 {@link RedissonClient}，
 * 避免引入第二套 Redis 连接参数。
 * </p>
 *
 * @author Chuang
 */
@Configuration
@EnableConfigurationProperties(MedicineRedissonProperties.class)
public class RedissonConfig {

    /**
     * 日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(RedissonConfig.class);

    /**
     * Redis 单机地址模板。
     */
    private static final String SINGLE_SERVER_ADDRESS_TEMPLATE = "%s://%s:%d";

    /**
     * 创建 Redisson 客户端。
     *
     * @param redisProperties    Spring Data Redis 配置
     * @param redissonProperties Redisson 配置
     * @return Redisson 客户端
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(DataRedisProperties redisProperties,
                                         MedicineRedissonProperties redissonProperties) {
        if (!redissonProperties.isEnabled()) {
            throw new IllegalStateException("medicine.redisson.enabled 当前版本必须保持为 true");
        }

        Config config = new Config();
        config.setThreads(redissonProperties.getThreads());
        config.setNettyThreads(redissonProperties.getNettyThreads());
        config.setLockWatchdogTimeout(redissonProperties.getLockWatchdogTimeoutMillis());
        config.setUsername(resolveText(redisProperties.getUsername()));
        config.setPassword(resolveText(redisProperties.getPassword()));

        SingleServerConfig singleServerConfig = config.useSingleServer();
        singleServerConfig.setAddress(buildRedisAddress(redisProperties));
        singleServerConfig.setDatabase(redisProperties.getDatabase());
        singleServerConfig.setConnectTimeout(redissonProperties.getConnectTimeoutMillis());
        singleServerConfig.setTimeout(redissonProperties.getTimeoutMillis());
        singleServerConfig.setRetryAttempts(redissonProperties.getRetryAttempts());
        singleServerConfig.setRetryDelay(new EqualJitterDelay(
                Duration.ofMillis(redissonProperties.getRetryIntervalMillis()),
                Duration.ofMillis(redissonProperties.getRetryIntervalMillis())
        ));
        if (StringUtils.hasText(redisProperties.getClientName())) {
            singleServerConfig.setClientName(redisProperties.getClientName());
        }

        log.info("初始化 Redisson 客户端，host={}，port={}，database={}",
                redisProperties.getHost(),
                redisProperties.getPort(),
                redisProperties.getDatabase());
        return Redisson.create(config);
    }

    /**
     * 构建 Redis 连接地址。
     *
     * @param redisProperties Spring Data Redis 配置
     * @return Redisson 使用的连接地址
     */
    private String buildRedisAddress(DataRedisProperties redisProperties) {
        String protocol = redisProperties.getSsl() != null && redisProperties.getSsl().isEnabled() ? "rediss" : "redis";
        String host = StringUtils.hasText(redisProperties.getHost()) ? redisProperties.getHost() : "localhost";
        int port = redisProperties.getPort() > 0 ? redisProperties.getPort() : 6379;
        return String.format(SINGLE_SERVER_ADDRESS_TEMPLATE, protocol, host, port);
    }

    /**
     * 归一化文本配置项。
     *
     * @param value 原始配置值
     * @return 非空白时返回原值，否则返回 null
     */
    private String resolveText(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
