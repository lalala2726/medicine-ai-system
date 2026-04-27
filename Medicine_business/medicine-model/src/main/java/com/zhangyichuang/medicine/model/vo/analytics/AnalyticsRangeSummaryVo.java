package com.zhangyichuang.medicine.model.vo.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 选定时间范围汇总。
 */
@Data
@Schema(description = "选定时间范围汇总")
public class AnalyticsRangeSummaryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "成交金额")
    private BigDecimal paidAmount;

    @Schema(description = "支付订单数")
    private Long paidOrderCount;

    @Schema(description = "平均订单金额")
    private BigDecimal averageOrderAmount;

    @Schema(description = "退款金额")
    private BigDecimal refundAmount;

    @Schema(description = "净成交金额")
    private BigDecimal netPaidAmount;

    @Schema(description = "退款率")
    private BigDecimal refundRate;

    @Schema(description = "售后申请数")
    private Long afterSaleApplyCount;

    @Schema(description = "退货退款件数")
    private Long returnRefundQuantity;
}
