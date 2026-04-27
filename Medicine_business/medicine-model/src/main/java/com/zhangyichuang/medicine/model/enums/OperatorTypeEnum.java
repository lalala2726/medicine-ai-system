package com.zhangyichuang.medicine.model.enums;

import lombok.Getter;

/**
 * 操作方类型枚举
 *
 * @author Chuang
 * created 2025/11/08
 */
@Getter
public enum OperatorTypeEnum {

    /**
     * 用户操作
     */
    USER("USER", "用户"),

    /**
     * 管理员操作
     */
    ADMIN("ADMIN", "管理员"),

    /**
     * 系统操作
     */
    SYSTEM("SYSTEM", "系统");

    /**
     * 枚举值
     */
    private final String type;

    /**
     * 中文名称
     */
    private final String name;

    OperatorTypeEnum(String type, String name) {
        this.type = type;
        this.name = name;
    }

    /**
     * 根据 type 获取枚举
     *
     * @param type 操作方类型
     * @return 枚举对象
     */
    public static OperatorTypeEnum fromCode(String type) {
        if (type == null) {
            return null;
        }
        for (OperatorTypeEnum operatorType : values()) {
            if (operatorType.type.equals(type)) {
                return operatorType;
            }
        }
        return null;
    }
}

