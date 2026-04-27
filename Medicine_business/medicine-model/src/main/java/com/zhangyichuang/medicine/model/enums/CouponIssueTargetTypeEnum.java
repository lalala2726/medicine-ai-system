package com.zhangyichuang.medicine.model.enums;

import lombok.Getter;

/**
 * 优惠券发券目标类型枚举。
 */
@Getter
public enum CouponIssueTargetTypeEnum {

    /**
     * 发给全部启用用户。
     */
    ALL("ALL", "全部用户"),

    /**
     * 发给指定用户。
     */
    SPECIFIED("SPECIFIED", "指定用户");

    /**
     * 目标类型编码。
     */
    private final String type;

    /**
     * 目标类型名称。
     */
    private final String name;

    /**
     * 构造发券目标类型枚举。
     *
     * @param type 目标类型编码
     * @param name 目标类型名称
     */
    CouponIssueTargetTypeEnum(String type, String name) {
        this.type = type;
        this.name = name;
    }

    /**
     * 根据编码解析目标类型。
     *
     * @param type 目标类型编码
     * @return 目标类型枚举
     */
    public static CouponIssueTargetTypeEnum fromCode(String type) {
        if (type == null) {
            return null;
        }
        for (CouponIssueTargetTypeEnum value : values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        return null;
    }
}
