package com.zhangyichuang.medicine.model.vo.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 履约时效汇总。
 */
@Data
@Schema(description = "履约时效汇总")
public class AnalyticsFulfillmentSummaryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "平均发货耗时（小时）")
    private BigDecimal averageShipmentHours;

    @Schema(description = "平均收货耗时（小时）")
    private BigDecimal averageReceiptHours;

    @Schema(description = "超24小时未发货订单数")
    private Long overdueShipmentOrderCount;

    @Schema(description = "超7天未收货订单数")
    private Long overdueReceiptOrderCount;
}
