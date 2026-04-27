package com.zhangyichuang.medicine.model.enums;

import lombok.Getter;

/**
 * 激活码日志操作类型枚举。
 */
@Getter
public enum ActivationLogActionTypeEnum {

    /**
     * 生成激活码。
     */
    CREATE("CREATE", "生成"),

    /**
     * 兑换激活码。
     */
    REDEEM("REDEEM", "兑换");

    /**
     * 操作类型编码。
     */
    private final String type;

    /**
     * 操作类型名称。
     */
    private final String name;

    /**
     * 构造激活码日志操作类型枚举。
     *
     * @param type 操作类型编码
     * @param name 操作类型名称
     */
    ActivationLogActionTypeEnum(String type, String name) {
        this.type = type;
        this.name = name;
    }

    /**
     * 根据操作类型编码解析枚举。
     *
     * @param type 操作类型编码
     * @return 激活码日志操作类型枚举
     */
    public static ActivationLogActionTypeEnum fromCode(String type) {
        if (type == null) {
            return null;
        }
        for (ActivationLogActionTypeEnum value : values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        return null;
    }
}
