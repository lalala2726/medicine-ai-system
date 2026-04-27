package com.zhangyichuang.medicine.common.redis.annotation;

import com.zhangyichuang.medicine.common.core.constants.RedisConstants;

import java.lang.annotation.*;

/**
 * 防重复提交注解。
 * <p>
 * 用于限制同一用户在指定时间窗口内对同一写请求重复提交。
 * 判定规则固定为：{@code userId + HTTP方法 + 请求URI + 参数指纹}。
 * </p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface PreventDuplicateSubmit {

    /**
     * 防重复提交时间窗口（毫秒）。
     *
     * @return 时间窗口（毫秒）
     */
    long intervalMillis() default RedisConstants.DuplicateSubmit.DEFAULT_INTERVAL_MILLIS;

    /**
     * 命中重复提交时返回的提示语。
     *
     * @return 提示语
     */
    String failMessage() default "请勿重复提交";
}
