package com.zhangyichuang.medicine.model.enums;

import lombok.Getter;

/**
 * 药品分类枚举。
 */
@Getter
public enum DrugCategoryEnum {

    /**
     * OTC（绿）/ 乙类非处方药。
     */
    OTC_GREEN(0, "OTC绿", "乙类非处方药", "安全性高，用户可直接购买。"),

    /**
     * OTC（红）/ 甲类非处方药。
     */
    OTC_RED(2, "OTC红", "甲类非处方药", "需在药师指导下购买。"),

    /**
     * Rx / 处方药。
     */
    RX(1, "Rx", "处方药", "必须上传医生处方，经药师审核后发货。");

    /**
     * 分类编码。
     */
    private final Integer code;

    /**
     * 卡片缩写文案。
     */
    private final String shortLabel;

    /**
     * 分类名称。
     */
    private final String name;

    /**
     * 分类说明。
     */
    private final String description;

    DrugCategoryEnum(Integer code, String shortLabel, String name, String description) {
        this.code = code;
        this.shortLabel = shortLabel;
        this.name = name;
        this.description = description;
    }

    /**
     * 根据分类编码获取枚举。
     *
     * @param code 分类编码
     * @return 药品分类枚举，未匹配时返回 null
     */
    public static DrugCategoryEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DrugCategoryEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
