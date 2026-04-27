package com.zhangyichuang.medicine.common.core.annotation;

import com.zhangyichuang.medicine.common.core.validation.PowerOfTwoValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记数值字段必须为 2 的次方。
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PowerOfTwoValidator.class)
public @interface PowerOfTwo {

    String message() default "数值必须是2的次方";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
