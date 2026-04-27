package com.zhangyichuang.medicine.model.enums;

import lombok.Getter;

/**
 * 优惠券类型枚举。
 */
@Getter
public enum CouponTypeEnum {

    /**
     * 满减券。
     */
    FULL_REDUCTION("FULL_REDUCTION", "满减券");

    /**
     * 枚举编码。
     */
    private final String type;

    /**
     * 枚举名称。
     */
    private final String name;

    /**
     * 构造优惠券类型枚举。
     *
     * @param type 编码
     * @param name 名称
     */
    CouponTypeEnum(String type, String name) {
        this.type = type;
        this.name = name;
    }

    /**
     * 根据编码解析优惠券类型枚举。
     *
     * @param type 枚举编码
     * @return 优惠券类型枚举
     */
    public static CouponTypeEnum fromCode(String type) {
        if (type == null) {
            return null;
        }
        for (CouponTypeEnum value : values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        return null;
    }
}
