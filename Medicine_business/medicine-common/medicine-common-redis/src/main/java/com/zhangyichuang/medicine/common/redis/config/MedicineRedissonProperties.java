package com.zhangyichuang.medicine.common.redis.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redisson 配置属性。
 * <p>
 * 当前项目首版仅支持基于现有 {@code spring.data.redis.*} 的单机 Redis 配置，
 * 这里补充 Redisson 线程池、超时和看门狗等专属参数。
 * </p>
 *
 * @author Chuang
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "medicine.redisson")
public class MedicineRedissonProperties {

    /**
     * Redisson 总开关。
     * <p>
     * 当前版本要求固定开启，若配置为 false 会在启动时直接失败。
     * </p>
     * -- GETTER --
     * 获取是否启用 Redisson。
     * <p>
     * <p>
     * -- SETTER --
     * 设置是否启用 Redisson。
     *
     */
    private boolean enabled = true;

    /**
     * Redisson 业务线程数。
     * -- GETTER --
     * 获取 Redisson 业务线程数。
     * <p>
     * <p>
     * -- SETTER --
     * 设置 Redisson 业务线程数。
     *
     * @return 业务线程数
     * @param threads 业务线程数
     */
    private int threads = 16;

    /**
     * Redisson Netty 线程数。
     * -- GETTER --
     * 获取 Redisson Netty 线程数。
     * <p>
     * <p>
     * -- SETTER --
     * 设置 Redisson Netty 线程数。
     *
     * @return Netty 线程数
     * @param nettyThreads Netty 线程数
     */
    private int nettyThreads = 32;

    /**
     * 锁看门狗超时时间（毫秒）。
     * -- GETTER --
     * 获取锁看门狗超时时间。
     * <p>
     * <p>
     * -- SETTER --
     * 设置锁看门狗超时时间。
     *
     */
    private long lockWatchdogTimeoutMillis = 30000L;

    /**
     * Redis 连接超时时间（毫秒）。
     * -- GETTER --
     * 获取 Redis 连接超时时间。
     * <p>
     * <p>
     * -- SETTER --
     * 设置 Redis 连接超时时间。
     *
     */
    private int connectTimeoutMillis = 10000;

    /**
     * Redis 命令超时时间（毫秒）。
     * -- GETTER --
     * 获取 Redis 命令超时时间。
     * <p>
     * <p>
     * -- SETTER --
     * 设置 Redis 命令超时时间。
     *
     */
    private int timeoutMillis = 3000;

    /**
     * Redis 命令重试次数。
     * -- GETTER --
     * 获取 Redis 命令重试次数。
     * <p>
     * <p>
     * -- SETTER --
     * 设置 Redis 命令重试次数。
     *
     */
    private int retryAttempts = 3;

    /**
     * Redis 命令重试间隔（毫秒）。
     * -- GETTER --
     * 获取 Redis 命令重试间隔。
     * <p>
     * <p>
     * -- SETTER --
     * 设置 Redis 命令重试间隔。
     *
     */
    private int retryIntervalMillis = 1500;

}
