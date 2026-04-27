package com.zhangyichuang.medicine.model.enums;

import lombok.Getter;

/**
 * 用户优惠券状态枚举。
 */
@Getter
public enum UserCouponStatusEnum {

    /**
     * 可使用状态。
     */
    AVAILABLE("AVAILABLE", "可使用"),

    /**
     * 锁定状态。
     */
    LOCKED("LOCKED", "已锁定"),

    /**
     * 已使用状态。
     */
    USED("USED", "已使用"),

    /**
     * 已过期状态。
     */
    EXPIRED("EXPIRED", "已过期");

    /**
     * 状态编码。
     */
    private final String type;

    /**
     * 状态名称。
     */
    private final String name;

    /**
     * 构造用户优惠券状态枚举。
     *
     * @param type 状态编码
     * @param name 状态名称
     */
    UserCouponStatusEnum(String type, String name) {
        this.type = type;
        this.name = name;
    }

    /**
     * 根据编码解析用户优惠券状态。
     *
     * @param type 状态编码
     * @return 用户优惠券状态枚举
     */
    public static UserCouponStatusEnum fromCode(String type) {
        if (type == null) {
            return null;
        }
        for (UserCouponStatusEnum value : values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        return null;
    }
}
