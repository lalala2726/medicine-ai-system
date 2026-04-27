package com.zhangyichuang.medicine.model.vo.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "退款/售后统计")
public class RefundStats {

    @Schema(description = "售后/退款单数量")
    private Long refundCount;

    @Schema(description = "退款金额合计")
    private BigDecimal refundAmount;
}
