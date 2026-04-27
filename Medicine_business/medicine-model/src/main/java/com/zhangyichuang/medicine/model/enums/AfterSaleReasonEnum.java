package com.zhangyichuang.medicine.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 售后申请原因枚举
 *
 * @author Chuang
 * created 2025/11/08
 */
@Getter
public enum AfterSaleReasonEnum {

    /**
     * 收货地址填错了
     */
    ADDRESS_ERROR("ADDRESS_ERROR", "收货地址填错了"),

    /**
     * 与描述不符
     */
    NOT_AS_DESCRIBED("NOT_AS_DESCRIBED", "与描述不符"),

    /**
     * 信息填错了，重新拍
     */
    INFO_ERROR("INFO_ERROR", "信息填错了，重新拍"),

    /**
     * 收到商品损坏了
     */
    DAMAGED("DAMAGED", "收到商品损坏了"),

    /**
     * 未按预定时间发货
     */
    DELAYED("DELAYED", "未按预定时间发货"),

    /**
     * 其它原因
     */
    OTHER("OTHER", "其它原因");

    /**
     * 枚举值(存储到数据库的值)
     */
    @EnumValue
    @JsonValue
    private final String reason;

    /**
     * 中文名称
     */
    private final String name;

    AfterSaleReasonEnum(String reason, String name) {
        this.reason = reason;
        this.name = name;
    }

    /**
     * 根据 reason 获取枚举
     *
     * @param reason 售后原因
     * @return 枚举对象
     */
    public static AfterSaleReasonEnum fromCode(String reason) {
        if (reason == null) {
            return null;
        }
        for (AfterSaleReasonEnum afterSaleReason : values()) {
            if (afterSaleReason.reason.equals(reason)) {
                return afterSaleReason;
            }
        }
        return null;
    }
}

