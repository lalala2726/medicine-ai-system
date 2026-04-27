package com.zhangyichuang.medicine.model.enums;

import lombok.Getter;

/**
 * 优惠券来源类型枚举。
 */
@Getter
public enum CouponSourceTypeEnum {

    /**
     * 管理端发券。
     */
    ADMIN_GRANT("ADMIN_GRANT", "管理端发券"),

    /**
     * 订单锁券。
     */
    ORDER("ORDER", "订单"),

    /**
     * 系统过期。
     */
    SYSTEM_EXPIRE("SYSTEM_EXPIRE", "系统过期"),

    /**
     * 手工调整。
     */
    MANUAL("MANUAL", "手工调整"),

    /**
     * 激活码兑换。
     */
    ACTIVATION_CODE("ACTIVATION_CODE", "激活码兑换");

    /**
     * 来源编码。
     */
    private final String type;

    /**
     * 来源名称。
     */
    private final String name;

    /**
     * 构造优惠券来源类型枚举。
     *
     * @param type 来源编码
     * @param name 来源名称
     */
    CouponSourceTypeEnum(String type, String name) {
        this.type = type;
        this.name = name;
    }

    /**
     * 根据编码解析来源类型。
     *
     * @param type 来源编码
     * @return 来源类型枚举
     */
    public static CouponSourceTypeEnum fromCode(String type) {
        if (type == null) {
            return null;
        }
        for (CouponSourceTypeEnum value : values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        return null;
    }
}
