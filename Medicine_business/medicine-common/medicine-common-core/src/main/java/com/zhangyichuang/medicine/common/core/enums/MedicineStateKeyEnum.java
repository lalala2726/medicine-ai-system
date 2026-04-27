package com.zhangyichuang.medicine.common.core.enums;

import lombok.Getter;

/**
 * 医疗工作流状态键枚举
 *
 * @author Chuang
 * created 2025/9/10
 */
@Getter
public enum MedicineStateKeyEnum {

    USER_MESSAGE("USER_MESSAGE", "用户消息"),
    USER_INTENT("USER_INTENT", "用户意图"),
    SYSTEM_RESPONSE("SYSTEM_RESPONSE", "系统响应");

    private final String key;
    private final String description;

    MedicineStateKeyEnum(String key, String description) {
        this.key = key;
        this.description = description;
    }

    @Override
    public String toString() {
        return key;
    }
}
