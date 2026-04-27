package com.zhangyichuang.medicine.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 售后类型枚举
 *
 * @author Chuang
 * created 2025/11/08
 */
@Getter
public enum AfterSaleTypeEnum {

    /**
     * 仅退款
     */
    REFUND_ONLY("REFUND_ONLY", "仅退款", "不需要退货，直接退款"),

    /**
     * 退货退款
     */
    RETURN_REFUND("RETURN_REFUND", "退货退款", "需要退货后才能退款"),

    /**
     * 换货
     */
    EXCHANGE("EXCHANGE", "换货", "更换商品");

    /**
     * 枚举值(存储到数据库的值)
     */
    @EnumValue
    @JsonValue
    private final String type;

    /**
     * 中文名称
     */
    private final String name;

    /**
     * 描述信息
     */
    private final String description;

    AfterSaleTypeEnum(String type, String name, String description) {
        this.type = type;
        this.name = name;
        this.description = description;
    }

    /**
     * 根据 type 获取枚举
     *
     * @param type 售后类型
     * @return 枚举对象
     */
    public static AfterSaleTypeEnum fromCode(String type) {
        if (type == null) {
            return null;
        }
        for (AfterSaleTypeEnum afterSaleType : values()) {
            if (afterSaleType.type.equals(type)) {
                return afterSaleType;
            }
        }
        return null;
    }
}

