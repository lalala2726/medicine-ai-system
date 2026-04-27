package com.zhangyichuang.medicine.model.enums;

import lombok.Getter;

/**
 * 激活码日志结果状态枚举。
 */
@Getter
public enum ActivationLogResultStatusEnum {

    /**
     * 成功。
     */
    SUCCESS("SUCCESS", "成功"),

    /**
     * 失败。
     */
    FAIL("FAIL", "失败");

    /**
     * 结果状态编码。
     */
    private final String type;

    /**
     * 结果状态名称。
     */
    private final String name;

    /**
     * 构造激活码日志结果状态枚举。
     *
     * @param type 结果状态编码
     * @param name 结果状态名称
     */
    ActivationLogResultStatusEnum(String type, String name) {
        this.type = type;
        this.name = name;
    }

    /**
     * 根据结果状态编码解析枚举。
     *
     * @param type 结果状态编码
     * @return 激活码日志结果状态枚举
     */
    public static ActivationLogResultStatusEnum fromCode(String type) {
        if (type == null) {
            return null;
        }
        for (ActivationLogResultStatusEnum value : values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        return null;
    }
}
