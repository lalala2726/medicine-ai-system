package com.zhangyichuang.medicine.model.vo.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "订单金额统计")
public class OrderAmountStats {

    @Schema(description = "订单总数（全部）")
    private Long totalOrders;

    @Schema(description = "已支付订单数")
    private Long paidOrders;

    @Schema(description = "订单金额合计（已支付）")
    private BigDecimal totalAmount;

    @Schema(description = "平均订单金额（已支付）")
    private BigDecimal averageAmount;
}
