package com.zhangyichuang.medicine.model.vo.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 实时运营总览。
 */
@Data
@Schema(description = "实时运营总览")
public class AnalyticsRealtimeOverviewVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "累计成交金额")
    private BigDecimal cumulativePaidAmount;

    @Schema(description = "累计支付订单数")
    private Long cumulativePaidOrderCount;

    @Schema(description = "今日成交金额")
    private BigDecimal todayPaidAmount;

    @Schema(description = "今日支付订单数")
    private Long todayPaidOrderCount;

    @Schema(description = "待发货订单数")
    private Long pendingShipmentOrderCount;

    @Schema(description = "待收货订单数")
    private Long pendingReceiptOrderCount;

    @Schema(description = "待处理售后数")
    private Long pendingAfterSaleCount;

    @Schema(description = "处理中售后数")
    private Long processingAfterSaleCount;
}
