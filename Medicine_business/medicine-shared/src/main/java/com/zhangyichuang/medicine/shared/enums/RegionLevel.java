package com.zhangyichuang.medicine.shared.enums;

import lombok.Getter;

/**
 * 行政区划层级枚举
 *
 * @author Chuang
 */
@Getter
public enum RegionLevel {

    /**
     * 省级
     */
    PROVINCE(1, "省级"),

    /**
     * 市级
     */
    CITY(2, "市级"),

    /**
     * 区县级
     */
    DISTRICT(3, "区县级"),

    /**
     * 街道/村级
     */
    STREET(5, "街道/村级");

    private final Integer code;
    private final String description;

    RegionLevel(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     *
     * @param code 层级代码
     * @return 对应的枚举,不存在返回null
     */
    public static RegionLevel getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (RegionLevel level : values()) {
            if (level.getCode().equals(code)) {
                return level;
            }
        }
        return null;
    }
}
