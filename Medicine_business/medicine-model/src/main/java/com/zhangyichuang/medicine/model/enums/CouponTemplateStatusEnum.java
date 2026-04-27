package com.zhangyichuang.medicine.model.enums;

import lombok.Getter;

/**
 * 优惠券模板状态枚举。
 */
@Getter
public enum CouponTemplateStatusEnum {

    /**
     * 草稿状态。
     */
    DRAFT("DRAFT", "草稿"),

    /**
     * 启用状态。
     */
    ACTIVE("ACTIVE", "启用"),

    /**
     * 停用状态。
     */
    DISABLED("DISABLED", "停用");

    /**
     * 状态编码。
     */
    private final String type;

    /**
     * 状态名称。
     */
    private final String name;

    /**
     * 构造模板状态枚举。
     *
     * @param type 状态编码
     * @param name 状态名称
     */
    CouponTemplateStatusEnum(String type, String name) {
        this.type = type;
        this.name = name;
    }

    /**
     * 根据编码解析模板状态。
     *
     * @param type 状态编码
     * @return 模板状态枚举
     */
    public static CouponTemplateStatusEnum fromCode(String type) {
        if (type == null) {
            return null;
        }
        for (CouponTemplateStatusEnum value : values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        return null;
    }
}
