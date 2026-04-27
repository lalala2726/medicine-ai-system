package com.zhangyichuang.medicine.model.enums;

import lombok.Getter;

/**
 * 激活码券有效期类型枚举。
 */
@Getter
public enum ActivationCodeValidityTypeEnum {

    /**
     * 一次性固定时间窗口。
     */
    ONCE("ONCE", "一次性"),

    /**
     * 激活后按天计算有效期。
     */
    AFTER_ACTIVATION("AFTER_ACTIVATION", "激活后计算");

    /**
     * 类型编码。
     */
    private final String type;

    /**
     * 类型名称。
     */
    private final String name;

    /**
     * 构造激活码有效期类型枚举。
     *
     * @param type 类型编码
     * @param name 类型名称
     */
    ActivationCodeValidityTypeEnum(String type, String name) {
        this.type = type;
        this.name = name;
    }

    /**
     * 根据类型编码解析枚举。
     *
     * @param type 类型编码
     * @return 激活码有效期类型枚举
     */
    public static ActivationCodeValidityTypeEnum fromCode(String type) {
        if (type == null) {
            return null;
        }
        for (ActivationCodeValidityTypeEnum value : values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        return null;
    }
}
