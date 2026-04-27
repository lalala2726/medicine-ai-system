package com.zhangyichuang.medicine.agent.annotation;

import java.lang.annotation.*;

/**
 * 通用描述注解。
 * <p>
 * 标注在类上表示对象整体描述，标注在字段上表示字段语义描述。
 */
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FieldDescription {

    /**
     * 描述文本。
     */
    String description();
}
