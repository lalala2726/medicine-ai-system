package com.zhangyichuang.medicine.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 收货状态枚举
 *
 * @author Chuang
 * created 2025/11/08
 */
@Getter
public enum ReceiveStatusEnum {

    /**
     * 已收到货
     */
    RECEIVED("RECEIVED", "已收到货"),

    /**
     * 未收到货
     */
    NOT_RECEIVED("NOT_RECEIVED", "未收到货");

    /**
     * 枚举值(存储到数据库的值)
     */
    @EnumValue
    @JsonValue
    private final String status;

    /**
     * 中文名称
     */
    private final String name;

    ReceiveStatusEnum(String status, String name) {
        this.status = status;
        this.name = name;
    }

    /**
     * 根据 status 获取枚举
     *
     * @param status 收货状态
     * @return 枚举对象
     */
    public static ReceiveStatusEnum fromCode(String status) {
        if (status == null) {
            return null;
        }
        for (ReceiveStatusEnum receiveStatus : values()) {
            if (receiveStatus.status.equals(status)) {
                return receiveStatus;
            }
        }
        return null;
    }
}

