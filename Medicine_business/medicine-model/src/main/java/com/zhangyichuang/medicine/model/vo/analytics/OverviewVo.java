package com.zhangyichuang.medicine.model.vo.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(description = "运营总览")
public class OverviewVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户总数")
    private Long totalUsers;

    @Schema(description = "订单总数")
    private Long totalOrders;

    @Schema(description = "已支付订单数")
    private Long paidOrders;

    @Schema(description = "退款订单数（售后单数）")
    private Long refundCount;

    @Schema(description = "订单总金额")
    private BigDecimal totalAmount;

    @Schema(description = "平均订单金额")
    private BigDecimal averageAmount;

    @Schema(description = "累计退款金额")
    private BigDecimal refundAmount;
}
