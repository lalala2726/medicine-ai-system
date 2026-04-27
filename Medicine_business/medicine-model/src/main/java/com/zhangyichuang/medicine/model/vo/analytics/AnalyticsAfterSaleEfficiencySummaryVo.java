package com.zhangyichuang.medicine.model.vo.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 售后处理时效汇总。
 */
@Data
@Schema(description = "售后处理时效汇总")
public class AnalyticsAfterSaleEfficiencySummaryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "平均审核耗时（小时）")
    private BigDecimal averageAuditHours;

    @Schema(description = "平均完结耗时（小时）")
    private BigDecimal averageCompleteHours;

    @Schema(description = "超24小时未审核售后数")
    private Long overdueAuditCount;

    @Schema(description = "超72小时未完结售后数")
    private Long overdueCompleteCount;
}
