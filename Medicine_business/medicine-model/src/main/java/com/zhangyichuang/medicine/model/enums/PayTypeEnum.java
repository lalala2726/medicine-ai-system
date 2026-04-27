package com.zhangyichuang.medicine.model.enums;

import lombok.Getter;

/**
 * 支付方式枚举
 *
 * @author Chuang
 * created 2025/11/01
 */
@Getter
public enum PayTypeEnum {

    /**
     * 钱包
     */
    WALLET("WALLET", "使用钱包余额进行支付"),

    /**
     * 待支付
     */
    WAIT_PAY("WAIT_PAY", "待支付"),

    /**
     * 优惠券零元支付
     */
    COUPON("COUPON", "使用优惠券零元支付"),

    /**
     * 订单已取消
     */
    CANCELLED("CANCELLED", "订单已取消");

    /**
     * 枚举值
     */
    private final String type;


    /**
     * 描述信息
     */
    private final String description;

    PayTypeEnum(String type, String description) {
        this.type = type;
        this.description = description;
    }

    /**
     * 根据 type 获取枚举
     */
    public static PayTypeEnum fromCode(String type) {
        if (type == null) {
            return null;
        }
        for (PayTypeEnum payType : values()) {
            if (payType.type.equals(type)) {
                return payType;
            }
        }
        return null;
    }

}
