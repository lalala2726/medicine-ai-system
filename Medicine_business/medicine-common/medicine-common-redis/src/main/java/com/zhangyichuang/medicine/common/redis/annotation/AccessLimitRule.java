package com.zhangyichuang.medicine.common.redis.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 限流时间窗口规则。
 * <p>
 * 用于声明单个滑动窗口中的请求上限与窗口时长。
 * </p>
 *
 * @author Chuang
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface AccessLimitRule {

    /**
     * 时间窗口内允许的最大请求次数。
     *
     * @return 最大请求次数
     */
    long limit();

    /**
     * 时间窗口长度。
     *
     * @return 时间窗口长度
     */
    long interval();

    /**
     * 时间窗口单位。
     *
     * @return 时间窗口单位
     */
    TimeUnit unit() default TimeUnit.SECONDS;
}
