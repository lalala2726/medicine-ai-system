package com.zhangyichuang.medicine.model.enums;

import lombok.Getter;

/**
 * 激活码发券状态枚举。
 */
@Getter
public enum ActivationGrantStatusEnum {

    /**
     * 发券成功。
     */
    SUCCESS("SUCCESS", "成功"),

    /**
     * 发券失败。
     */
    FAIL("FAIL", "失败");

    /**
     * 状态编码。
     */
    private final String type;

    /**
     * 状态名称。
     */
    private final String name;

    /**
     * 构造激活码发券状态枚举。
     *
     * @param type 状态编码
     * @param name 状态名称
     */
    ActivationGrantStatusEnum(String type, String name) {
        this.type = type;
        this.name = name;
    }
}
