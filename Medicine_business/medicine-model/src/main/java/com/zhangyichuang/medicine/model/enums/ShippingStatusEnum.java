package com.zhangyichuang.medicine.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 物流状态枚举
 *
 * @author Chuang
 * created 2025/11/08
 */
@Getter
public enum ShippingStatusEnum {

    /**
     * 未发货
     */
    NOT_SHIPPED("NOT_SHIPPED", "未发货", "订单尚未发货"),

    /**
     * 运输中
     */
    IN_TRANSIT("IN_TRANSIT", "运输中", "商品正在配送途中"),

    /**
     * 已签收
     */
    DELIVERED("DELIVERED", "已签收", "用户已确认收货"),

    /**
     * 异常
     */
    EXCEPTION("EXCEPTION", "异常", "物流配送出现异常");

    /**
     * 枚举值
     */
    @EnumValue
    private final String type;

    /**
     * 中文名称
     */
    private final String name;

    /**
     * 描述信息
     */
    private final String description;

    ShippingStatusEnum(String type, String name, String description) {
        this.type = type;
        this.name = name;
        this.description = description;
    }

    /**
     * 根据 type 获取枚举
     *
     * @param type 状态类型
     * @return 枚举对象
     */
    public static ShippingStatusEnum fromCode(String type) {
        if (type == null) {
            return null;
        }
        for (ShippingStatusEnum status : values()) {
            if (status.type.equals(type)) {
                return status;
            }
        }
        return null;
    }
}

