package com.zhangyichuang.medicine.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 售后申请范围枚举。
 */
@Getter
public enum AfterSaleScopeEnum {

    /**
     * 整单。
     */
    ORDER("ORDER", "整单"),

    /**
     * 单个商品。
     */
    ITEM("ITEM", "商品");

    @JsonValue
    private final String scope;

    private final String name;

    AfterSaleScopeEnum(String scope, String name) {
        this.scope = scope;
        this.name = name;
    }

    public static AfterSaleScopeEnum fromCode(String scope) {
        if (scope == null) {
            return null;
        }
        for (AfterSaleScopeEnum value : values()) {
            if (value.scope.equals(scope)) {
                return value;
            }
        }
        return null;
    }
}
