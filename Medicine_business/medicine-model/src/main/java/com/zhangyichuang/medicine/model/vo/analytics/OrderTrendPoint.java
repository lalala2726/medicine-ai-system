package com.zhangyichuang.medicine.model.vo.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(description = "订单趋势点")
public class OrderTrendPoint implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "分组标签：按日/周/月")
    private String label;

    @Schema(description = "订单数")
    private Long orderCount;

    @Schema(description = "订单金额合计")
    private BigDecimal orderAmount;
}
