package com.zhangyichuang.medicine.model.enums;

import lombok.Getter;

/**
 * 运营趋势粒度枚举。
 */
@Getter
public enum AnalyticsTrendGranularityEnum {

    DAY("DAY", "按日"),
    WEEK("WEEK", "按周"),
    MONTH("MONTH", "按月");

    private final String code;
    private final String label;

    AnalyticsTrendGranularityEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
