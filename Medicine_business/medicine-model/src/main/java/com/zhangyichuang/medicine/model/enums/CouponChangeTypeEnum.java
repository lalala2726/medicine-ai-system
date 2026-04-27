package com.zhangyichuang.medicine.model.enums;

import lombok.Getter;

/**
 * 优惠券日志变更类型枚举。
 */
@Getter
public enum CouponChangeTypeEnum {

    /**
     * 发券。
     */
    GRANT("GRANT", "发券"),

    /**
     * 锁券。
     */
    LOCK("LOCK", "锁券"),

    /**
     * 消耗。
     */
    CONSUME("CONSUME", "消耗"),

    /**
     * 释放锁券。
     */
    RELEASE("RELEASE", "释放锁券"),

    /**
     * 返还券金额。
     */
    RETURN("RETURN", "返还"),

    /**
     * 过期。
     */
    EXPIRE("EXPIRE", "过期"),

    /**
     * 手工调整。
     */
    MANUAL_ADJUST("MANUAL_ADJUST", "手工调整");

    /**
     * 变更类型编码。
     */
    private final String type;

    /**
     * 变更类型名称。
     */
    private final String name;

    /**
     * 构造优惠券变更类型枚举。
     *
     * @param type 编码
     * @param name 名称
     */
    CouponChangeTypeEnum(String type, String name) {
        this.type = type;
        this.name = name;
    }

    /**
     * 根据编码解析优惠券变更类型。
     *
     * @param type 变更类型编码
     * @return 变更类型枚举
     */
    public static CouponChangeTypeEnum fromCode(String type) {
        if (type == null) {
            return null;
        }
        for (CouponChangeTypeEnum value : values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        return null;
    }
}
