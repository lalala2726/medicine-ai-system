package com.zhangyichuang.medicine.model.vo.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * AI 售后趋势点。
 */
@Data
@Schema(description = "AI 售后趋势点")
public class AnalyticsAfterSaleTrendPointVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "标签")
    private String label;

    @Schema(description = "退款金额")
    private BigDecimal refundAmount;

    @Schema(description = "售后申请数")
    private Long afterSaleApplyCount;
}
