package com.zhangyichuang.medicine.model.enums;

import lombok.Getter;

/**
 * 消息角色枚举
 */
@Getter
public enum MessageRoleEnum {

    USER("USER", "用户"),
    ASSISTANT("ASSISTANT", "助手"),
    SYSTEM("SYSTEM", "系统");

    private final String type;
    private final String description;

    MessageRoleEnum(String type, String description) {
        this.type = type;
        this.description = description;
    }

    public static MessageRoleEnum fromCode(String type) {
        for (MessageRoleEnum role : values()) {
            if (role.type.equals(type)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown message role: " + type);
    }

}
