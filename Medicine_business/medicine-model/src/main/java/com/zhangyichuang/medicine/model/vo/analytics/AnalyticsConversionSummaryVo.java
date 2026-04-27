package com.zhangyichuang.medicine.model.vo.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单支付转化汇总。
 */
@Data
@Schema(description = "订单支付转化汇总")
public class AnalyticsConversionSummaryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "下单订单数")
    private Long createdOrderCount;

    @Schema(description = "已支付订单数")
    private Long paidOrderCount;

    @Schema(description = "待支付订单数")
    private Long pendingPaymentOrderCount;

    @Schema(description = "已关闭订单数")
    private Long closedOrderCount;

    @Schema(description = "支付转化率")
    private BigDecimal paymentConversionRate;
}
