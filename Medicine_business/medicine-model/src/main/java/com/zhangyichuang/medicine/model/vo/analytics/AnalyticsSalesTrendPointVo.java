package com.zhangyichuang.medicine.model.vo.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * AI 成交趋势点。
 */
@Data
@Schema(description = "AI 成交趋势点")
public class AnalyticsSalesTrendPointVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "标签")
    private String label;

    @Schema(description = "支付订单数")
    private Long paidOrderCount;

    @Schema(description = "成交金额")
    private BigDecimal paidAmount;
}
