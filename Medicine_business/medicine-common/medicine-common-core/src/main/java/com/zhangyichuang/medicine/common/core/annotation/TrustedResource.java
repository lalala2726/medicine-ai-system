package com.zhangyichuang.medicine.common.core.annotation;

import com.zhangyichuang.medicine.common.core.validation.TrustedResourceValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记可信资源地址字段，限制必须来自本系统可信域名并支持文件名白名单。
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TrustedResourceValidator.class)
public @interface TrustedResource {

    String message() default "资源地址不可信";

    /**
     * 校验分组，用于在不同业务场景下启用/禁用该校验。
     */
    Class<?>[] groups() default {};

    /**
     * 负载信息，通常用于携带元数据给校验框架或审计处理。
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * 可信域名白名单（优先于配置）。
     */
    String[] trustedDomains() default {};

    /**
     * 允许子域名匹配（如 img.example.com）。
     */
    boolean allowSubdomains() default false;

    /**
     * 允许相对路径地址（无域名时）。
     */
    boolean allowRelative() default false;

    /**
     * 允许空值或空字符串。
     */
    boolean allowBlank() default true;

    /**
     * 允许的文件名列表（精确匹配）。
     */
    String[] allowedFileNames() default {};

    /**
     * 文件名匹配正则（可选）。
     */
    String fileNamePattern() default "";
}
