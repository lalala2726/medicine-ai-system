package com.zhangyichuang.medicine.common.core.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.zhangyichuang.medicine.common.core.enums.MaskingType;
import com.zhangyichuang.medicine.common.core.json.DataMaskingSerializer;
import tools.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据脱敏注解，用于标记需要进行数据脱敏的字段。
 * <p>
 * 示例：
 * <pre>
 *     手机号脱敏前：18800000000，脱敏后：188****0000
 *     身份证号脱敏前：123456789012345678，脱敏后：123456****5678
 *     邮箱脱敏前：test@example.com，脱敏后：t***@example.com
 *     姓名脱敏前：张三丰，脱敏后：张**
 *     银行卡号脱敏前：6222021234567890，脱敏后：6222****7890
 * </pre>
 * </p>
 *
 * @author Chuang
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = DataMaskingSerializer.class)
public @interface DataMasking {

    /**
     * 脱敏类型，默认为自定义类型
     */
    MaskingType type() default MaskingType.CUSTOM;

    /**
     * 自定义正则表达式，当 type 为 CUSTOM 时使用
     * 格式：原始字符串的正则表达式
     */
    String regex() default "";

    /**
     * 替换字符串，当 type 为 CUSTOM 时使用
     * 格式：替换后的字符串，使用 $1、$2 等表示分组
     */
    String replacement() default "";

    /**
     * 脱敏字符，默认为 *
     */
    String maskChar() default "*";

    /**
     * 保留前面字符数量（仅对部分类型有效）
     */
    int prefixKeep() default -1;

    /**
     * 保留后面字符数量（仅对部分类型有效）
     */
    int suffixKeep() default -1;

    /**
     * 是否保留原始字符串长度，默认为 true
     * 当为 true 时，脱敏后的字符串长度与原始字符串相同
     * 当为 false 时，可能会使用固定长度的脱敏字符
     * 注意：预定义类型在模板脱敏场景下可能优先使用类型模板；
     * 如需严格按前后保留字符策略控制长度，请使用 CUSTOM + prefixKeep/suffixKeep。
     */
    boolean preserveLength() default true;

    /**
     * 指定脱敏字符的长度（当 preserveLength 为 false 时使用）
     * 默认为 -1，表示自动计算
     * 当指定具体数值时，脱敏部分将使用指定长度的脱敏字符
     */
    int maskLength() default -1;
}
