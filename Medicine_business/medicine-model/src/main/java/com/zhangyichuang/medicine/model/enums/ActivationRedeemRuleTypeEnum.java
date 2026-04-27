package com.zhangyichuang.medicine.model.enums;

import lombok.Getter;

/**
 * 激活码兑换规则类型枚举。
 */
@Getter
public enum ActivationRedeemRuleTypeEnum {

    /**
     * 同一码每个用户仅可成功兑换一次。
     */
    SHARED_PER_USER_ONCE("SHARED_PER_USER_ONCE", "共享码（每用户一次）"),

    /**
     * 同一码全局仅可成功兑换一次。
     */
    UNIQUE_SINGLE_USE("UNIQUE_SINGLE_USE", "唯一码（全局一次）");

    /**
     * 规则编码。
     */
    private final String type;

    /**
     * 规则名称。
     */
    private final String name;

    /**
     * 构造兑换规则类型枚举。
     *
     * @param type 规则编码
     * @param name 规则名称
     */
    ActivationRedeemRuleTypeEnum(String type, String name) {
        this.type = type;
        this.name = name;
    }

    /**
     * 根据规则编码解析枚举。
     *
     * @param type 规则编码
     * @return 兑换规则类型枚举
     */
    public static ActivationRedeemRuleTypeEnum fromCode(String type) {
        if (type == null) {
            return null;
        }
        for (ActivationRedeemRuleTypeEnum value : values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        return null;
    }
}
