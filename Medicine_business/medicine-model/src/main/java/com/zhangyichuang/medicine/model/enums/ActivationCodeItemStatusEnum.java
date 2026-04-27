package com.zhangyichuang.medicine.model.enums;

import lombok.Getter;

/**
 * 激活码单码状态枚举。
 */
@Getter
public enum ActivationCodeItemStatusEnum {

    /**
     * 可用。
     */
    ACTIVE("ACTIVE", "可用"),

    /**
     * 已使用。
     */
    USED("USED", "已使用"),

    /**
     * 已停用。
     */
    DISABLED("DISABLED", "已停用");

    /**
     * 状态编码。
     */
    private final String type;

    /**
     * 状态名称。
     */
    private final String name;

    /**
     * 构造激活码单码状态枚举。
     *
     * @param type 状态编码
     * @param name 状态名称
     */
    ActivationCodeItemStatusEnum(String type, String name) {
        this.type = type;
        this.name = name;
    }

    /**
     * 根据状态编码解析枚举。
     *
     * @param type 状态编码
     * @return 激活码单码状态枚举
     */
    public static ActivationCodeItemStatusEnum fromCode(String type) {
        if (type == null) {
            return null;
        }
        for (ActivationCodeItemStatusEnum value : values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        return null;
    }
}
