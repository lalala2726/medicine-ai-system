package com.zhangyichuang.medicine.model.enums;

import lombok.Getter;

/**
 * 订单状态枚举
 *
 * @author Chuang
 * created 2025/11/01
 */
@Getter
public enum OrderStatusEnum {

    /**
     * 待支付
     */
    PENDING_PAYMENT("PENDING_PAYMENT", "待支付", "订单已创建，等待用户支付"),

    /**
     * 待发货
     */
    PENDING_SHIPMENT("PENDING_SHIPMENT", "待发货", "用户已支付，商家准备发货"),

    /**
     * 待收货
     */
    PENDING_RECEIPT("PENDING_RECEIPT", "待收货", "商品已发货，等待用户确认收货"),

    /**
     * 已完成
     */
    COMPLETED("COMPLETED", "已完成", "用户已确认收货，订单完成"),

    /**
     * 已退款
     */
    REFUNDED("REFUNDED", "已退款", "订单已退款处理"),

    /**
     * 售后中
     */
    AFTER_SALE("AFTER_SALE", "售后中", "订单正在进行售后处理"),

    /**
     * 已过期
     */
    EXPIRED("EXPIRED", "已过期", "订单已过期，请重新下单"),

    /**
     * 已取消
     */
    CANCELLED("CANCELLED", "已取消", "订单已取消");

    /**
     * 枚举值
     */
    private final String type;

    /**
     * 中文名称
     */
    private final String name;

    /**
     * 描述信息
     */
    private final String description;

    OrderStatusEnum(String type, String name, String description) {
        this.type = type;
        this.name = name;
        this.description = description;
    }

    /**
     * 根据 type 获取枚举
     */
    public static OrderStatusEnum fromCode(String type) {
        if (type == null) {
            return null;
        }
        for (OrderStatusEnum status : values()) {
            if (status.type.equals(type)) {
                return status;
            }
        }
        return null;
    }

}
