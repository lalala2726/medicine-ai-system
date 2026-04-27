package com.zhangyichuang.medicine.common.redis.annotation;

import java.lang.annotation.*;

/**
 * 分布式锁注解。
 * <p>
 * 用于声明业务方法在执行前需要获取的 Redisson 分布式锁。
 * {@code prefix} 通常使用 Redis 常量中的 key 模板，
 * {@code key} 通过 SpEL 解析实际业务键。
 * </p>
 *
 * @author Chuang
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DistributedLock {

    /**
     * 分布式锁 key 前缀或模板。
     *
     * @return 锁 key 前缀或模板
     */
    String prefix();

    /**
     * SpEL 表达式，用于解析业务键。
     *
     * @return SpEL 表达式
     */
    String key();

    /**
     * 获取锁时的等待时间（毫秒）。
     *
     * @return 等待时间（毫秒）
     */
    long waitTimeMillis() default 0L;

    /**
     * 锁租约时间（毫秒）。
     * <p>
     * 当值小于等于 0 时，使用 Redisson watchdog 自动续期。
     * </p>
     *
     * @return 锁租约时间（毫秒）
     */
    long leaseTimeMillis() default -1L;

    /**
     * 获取锁失败时返回给业务层的提示语。
     *
     * @return 失败提示语
     */
    String failMessage() default "操作处理中，请勿重复提交";
}
