package com.zhangyichuang.medicine.model.enums;

import lombok.Getter;

/**
 * 优惠券模板删除模式枚举。
 */
@Getter
public enum CouponTemplateDeleteModeEnum {

    /**
     * 仅隐藏模板。
     */
    HIDE_ONLY("HIDE_ONLY", "仅删除模板"),

    /**
     * 隐藏模板并使已发可用券失效。
     */
    HIDE_AND_EXPIRE_ISSUED("HIDE_AND_EXPIRE_ISSUED", "删除模板并使已发可用券失效");

    /**
     * 删除模式编码。
     */
    private final String type;

    /**
     * 删除模式名称。
     */
    private final String name;

    /**
     * 构造优惠券模板删除模式枚举。
     *
     * @param type 删除模式编码
     * @param name 删除模式名称
     */
    CouponTemplateDeleteModeEnum(String type, String name) {
        this.type = type;
        this.name = name;
    }

    /**
     * 根据编码解析删除模式枚举。
     *
     * @param type 删除模式编码
     * @return 删除模式枚举
     */
    public static CouponTemplateDeleteModeEnum fromCode(String type) {
        if (type == null) {
            return null;
        }
        for (CouponTemplateDeleteModeEnum value : values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        return null;
    }
}
