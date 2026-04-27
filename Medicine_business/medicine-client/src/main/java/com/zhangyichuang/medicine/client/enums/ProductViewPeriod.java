package com.zhangyichuang.medicine.client.enums;

import java.util.Arrays;

/**
 * 商品浏览量查询周期枚举
 *
 * <p>支持总量、按小时、按天、按周、按月统计。</p>
 *
 * @author Chuang
 */
public enum ProductViewPeriod {

    /**
     * 累计总浏览次数
     */
    TOTAL("TOTAL"),
    /**
     * 当前小时
     */
    HOUR("HOUR"),
    /**
     * 当天
     */
    DAY("DAY"),
    /**
     * 当周（周一 00:00 起）
     */
    WEEK("WEEK"),
    /**
     * 当月（1 号 00:00 起）
     */
    MONTH("MONTH");

    private final String code;

    ProductViewPeriod(String code) {
        this.code = code;
    }

    /**
     * 根据传入编码解析周期，默认返回 TOTAL
     *
     * @param code 周期编码
     * @return 匹配的枚举
     */
    public static ProductViewPeriod fromCode(String code) {
        if (code == null || code.isBlank()) {
            return TOTAL;
        }
        String normalized = code.trim().toLowerCase();
        return Arrays.stream(values())
                .filter(item -> item.code.equals(normalized))
                .findFirst()
                .orElse(TOTAL);
    }

    public String getCode() {
        return code;
    }
}
