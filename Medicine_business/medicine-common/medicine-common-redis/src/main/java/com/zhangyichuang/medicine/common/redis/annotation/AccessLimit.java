package com.zhangyichuang.medicine.common.redis.annotation;

import com.zhangyichuang.medicine.common.core.constants.RedisConstants;
import com.zhangyichuang.medicine.common.redis.enums.AccessLimitDimension;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 滑动窗口限流注解。
 * <p>
 * 用于在控制器或业务方法上声明多时间窗口限流规则，支持用户与 IP 维度。
 * </p>
 *
 * @author Chuang
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AccessLimit {

    /**
     * 限流主体维度。
     *
     * @return 限流主体维度
     */
    AccessLimitDimension dimension() default AccessLimitDimension.USER_OR_IP;

    /**
     * 限流窗口规则集合。
     *
     * @return 限流窗口规则集合
     */
    AccessLimitRule[] rules() default {
            @AccessLimitRule(limit = 60, interval = 1, unit = TimeUnit.MINUTES),
            @AccessLimitRule(limit = 200, interval = 5, unit = TimeUnit.MINUTES)
    };

    /**
     * 业务资源标识。
     * <p>
     * 为空时使用 HTTP_METHOD + ":" + REQUEST_URI。
     * </p>
     *
     * @return 业务资源标识
     */
    String resource() default "";

    /**
     * 业务键 SpEL 表达式。
     * <p>
     * 用于在资源标识后追加细粒度业务维度，支持跨服务共享限流桶。
     * </p>
     *
     * @return 业务键 SpEL 表达式
     */
    String key() default "";

    /**
     * 命中限流时返回的提示语。
     *
     * @return 限流提示语
     */
    String failMessage() default RedisConstants.AccessLimit.DEFAULT_FAIL_MESSAGE;
}
