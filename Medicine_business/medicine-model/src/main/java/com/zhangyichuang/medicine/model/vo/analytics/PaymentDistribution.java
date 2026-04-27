package com.zhangyichuang.medicine.model.vo.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(description = "支付方式分布")
public class PaymentDistribution implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "支付方式编码")
    private String payType;

    @Schema(description = "支付方式名称")
    private String payTypeName;

    @Schema(description = "订单数量")
    private Long count;

    @Schema(description = "金额合计")
    private BigDecimal amount;
}
