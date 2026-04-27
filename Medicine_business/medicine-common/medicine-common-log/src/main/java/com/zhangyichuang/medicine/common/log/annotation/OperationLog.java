package com.zhangyichuang.medicine.common.log.annotation;

import com.zhangyichuang.medicine.common.log.enums.OperationType;

import java.lang.annotation.*;

/**
 * 操作日志注解。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 业务模块。
     */
    String module() default "";

    /**
     * 操作说明。
     */
    String action() default "";

    /**
     * 操作类型。
     */
    OperationType type() default OperationType.OTHER;

    /**
     * 是否记录请求参数。
     */
    boolean recordParams() default true;

    /**
     * 是否记录返回结果。
     */
    boolean recordResult() default false;
}
