package com.zhangyichuang.medicine.model.enums;

import lombok.Getter;

/**
 * 激活码发券方式枚举。
 */
@Getter
public enum ActivationGrantModeEnum {

    /**
     * 通过优惠券核心发券服务发放。
     */
    COUPON_GRANT_CORE("COUPON_GRANT_CORE", "优惠券核心发券服务");

    /**
     * 方式编码。
     */
    private final String type;

    /**
     * 方式名称。
     */
    private final String name;

    /**
     * 构造激活码发券方式枚举。
     *
     * @param type 方式编码
     * @param name 方式名称
     */
    ActivationGrantModeEnum(String type, String name) {
        this.type = type;
        this.name = name;
    }
}
