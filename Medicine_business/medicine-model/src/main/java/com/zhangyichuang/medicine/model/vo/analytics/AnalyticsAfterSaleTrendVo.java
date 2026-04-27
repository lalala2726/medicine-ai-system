package com.zhangyichuang.medicine.model.vo.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * AI 售后趋势响应。
 */
@Data
@Schema(description = "AI 售后趋势响应")
public class AnalyticsAfterSaleTrendVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "最近天数")
    private Integer days;

    @Schema(description = "粒度")
    private String granularity;

    @Schema(description = "趋势点")
    private List<AnalyticsAfterSaleTrendPointVo> points;
}
