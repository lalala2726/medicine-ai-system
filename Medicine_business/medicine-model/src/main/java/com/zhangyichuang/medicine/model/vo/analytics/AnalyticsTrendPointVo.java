package com.zhangyichuang.medicine.model.vo.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 趋势点。
 */
@Data
@Schema(description = "运营趋势点")
public class AnalyticsTrendPointVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "标签")
    private String label;

    @Schema(description = "支付订单数")
    private Long paidOrderCount;

    @Schema(description = "成交金额")
    private BigDecimal paidAmount;

    @Schema(description = "退款金额")
    private BigDecimal refundAmount;

    @Schema(description = "售后申请数")
    private Long afterSaleApplyCount;
}
