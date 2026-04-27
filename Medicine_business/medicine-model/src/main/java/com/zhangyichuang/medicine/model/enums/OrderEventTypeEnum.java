package com.zhangyichuang.medicine.model.enums;

import lombok.Getter;

/**
 * 订单事件类型枚举
 *
 * @author Chuang
 * created 2025/11/08
 */
@Getter
public enum OrderEventTypeEnum {

    /**
     * 订单创建
     */
    ORDER_CREATED("ORDER_CREATED", "订单创建", "用户成功创建订单"),

    /**
     * 订单支付
     */
    ORDER_PAID("ORDER_PAID", "订单支付", "订单支付成功"),

    /**
     * 订单发货
     */
    ORDER_SHIPPED("ORDER_SHIPPED", "订单发货", "订单已发货"),

    /**
     * 确认收货
     */
    ORDER_RECEIVED("ORDER_RECEIVED", "确认收货", "用户确认收货"),

    /**
     * 订单完成
     */
    ORDER_COMPLETED("ORDER_COMPLETED", "订单完成", "订单已完成"),

    /**
     * 订单退款
     */
    ORDER_REFUNDED("ORDER_REFUNDED", "订单退款", "订单已退款"),

    /**
     * 订单取消
     */
    ORDER_CANCELLED("ORDER_CANCELLED", "订单取消", "订单已取消"),

    /**
     * 订单过期
     */
    ORDER_EXPIRED("ORDER_EXPIRED", "订单过期", "订单支付超时已过期"),

    /**
     * 申请售后
     */
    AFTER_SALE_APPLIED("AFTER_SALE_APPLIED", "申请售后", "用户申请售后服务"),

    /**
     * 售后审核通过
     */
    AFTER_SALE_APPROVED("AFTER_SALE_APPROVED", "售后审核通过", "管理员审核通过售后申请"),

    /**
     * 售后审核拒绝
     */
    AFTER_SALE_REJECTED("AFTER_SALE_REJECTED", "售后审核拒绝", "管理员拒绝售后申请"),

    /**
     * 售后完成
     */
    AFTER_SALE_COMPLETED("AFTER_SALE_COMPLETED", "售后完成", "售后处理已完成"),

    /**
     * 其他
     */
    OTHER("OTHER", "其他", "其他事件");

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

    OrderEventTypeEnum(String type, String name, String description) {
        this.type = type;
        this.name = name;
        this.description = description;
    }

    /**
     * 根据 type 获取枚举
     *
     * @param type 事件类型
     * @return 枚举对象
     */
    public static OrderEventTypeEnum fromCode(String type) {
        if (type == null) {
            return null;
        }
        for (OrderEventTypeEnum eventType : values()) {
            if (eventType.type.equals(type)) {
                return eventType;
            }
        }
        return null;
    }
}

